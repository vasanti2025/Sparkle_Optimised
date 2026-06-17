package com.loyalstring.rfid.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.local.entity.SearchItem
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.order.CustomOrderItem
import com.loyalstring.rfid.data.model.order.CustomOrderResponse
import com.loyalstring.rfid.data.model.order.OrderSearchRequest
import com.loyalstring.rfid.data.model.box.BoxRfidBoxInfo
import com.loyalstring.rfid.data.model.box.BoxRfidBoxSummary
import com.loyalstring.rfid.data.model.box.BoxRfidDetailsResponse
import com.loyalstring.rfid.data.model.box.BoxRfidProduct
import com.loyalstring.rfid.data.model.box.BoxRfidSearchRequest
import com.loyalstring.rfid.data.reader.RFIDReaderManager
import com.loyalstring.rfid.repository.BoxRfidRepository
import com.loyalstring.rfid.repository.BulkRepositoryImpl
import com.loyalstring.rfid.repository.OrderRepository
import com.rscja.deviceapi.RFIDWithUHFUART
import com.rscja.deviceapi.interfaces.IUHF
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val readerManager: RFIDReaderManager,
    private val bulkRepositoryImpl: BulkRepositoryImpl,
    private val orderRepository: OrderRepository,
    private val boxRfidRepository: BoxRfidRepository
) : ViewModel() {

  /*  private var _searchItems = mutableStateListOf<SearchItem>()
    val searchItems: List<SearchItem> get() = _searchItems*/
  private val _searchItems = mutableStateListOf<SearchItem>()
    val searchItems: SnapshotStateList<SearchItem> = _searchItems

    // PERF-FIX: HashMap for O(1) EPC/RFID/ItemCode → list index lookup.
    // Previously, every RFID tag read from hardware triggered an indexOfFirst { } call
    // which is O(n) over the entire searchItems list. At high scan rates (100+ tags/sec)
    // this caused severe main-thread jank. The map is rebuilt whenever startSearch() is called.
    private val epcToIndex = HashMap<String, Int>()

    private var currentScanPower: Int = 10

    private var scanJob: Job? = null
    private var lastSoundId: Int? = null
    private var lastBlinkEpc: String? = null
    private var blinkingJob: Job? = null
    var lastSoundTime = 0L
    private var lastSearchUiUpdate = 0L
    private var lastSoundPlayAt = 0L

    // Aggressive tuning — same blink/sound/progress logic, lower latency.
    private val searchUiUpdateNs = 16_000_000L   // ~60fps progress
    private val soundMinIntervalMs = 15L
    private val blinkLedVisibleMs = 50L
    private val blinkCyclePauseMs = 200L
    private val maxStaleDrain = 5

    // Tracks whether this VM has actually started a scan.
    private var isScanActive = false

    init {
        viewModelScope.launch(Dispatchers.IO) {
            readerManager.initReader()
        }
    }

    fun preWarmReader() {
        readerManager.initReader()
    }

    fun startSearch(unmatchedItems: List<BulkItem>, power: Int) {
        if (isScanActive) stopSearch()
        currentScanPower = power
        epcToIndex.clear()

        // Wrap clear() + addAll() in a single snapshot transaction so Compose never
        // observes the intermediate empty state. Without this, clear() notifies Compose
        // immediately, causing itemsIndexed in SearchScreen to rebuild its key index map
        // while the list is still empty — accessing index 0 of size 0 → crash.
        Snapshot.withMutableSnapshot {
            _searchItems.clear()
            _searchItems.addAll(unmatchedItems.map { item ->
                val epcValue = when {
                    !item.epc.isNullOrBlank() -> item.epc!!
                    !item.rfid.isNullOrBlank() -> item.rfid!!
                    !item.itemCode.isNullOrBlank() -> item.itemCode!!
                    else -> ""
                }

                SearchItem(
                    epc = epcValue,
                    itemCode = item.itemCode ?: "",
                    productName = item.productName ?: "",
                    rfid = item.rfid ?: ""
                )
            })
        }

        // Index all identifiers from source items (box/order API uses TID + RFID + EPC).
        _searchItems.forEachIndexed { index, item ->
            if (item.epc.isNotBlank()) epcToIndex[item.epc.uppercase()] = index
            if (item.rfid.isNotBlank()) epcToIndex[item.rfid.uppercase()] = index
            if (item.itemCode.isNotBlank()) epcToIndex[item.itemCode.uppercase()] = index
        }
        unmatchedItems.forEachIndexed { index, item ->
            if (index >= _searchItems.size) return@forEachIndexed
            item.tid?.trim()?.takeIf { it.isNotBlank() }?.uppercase()?.let { epcToIndex[it] = index }
            item.epc?.trim()?.takeIf { it.isNotBlank() }?.uppercase()?.let { epcToIndex[it] = index }
            item.rfid?.trim()?.takeIf { it.isNotBlank() }?.uppercase()?.let { epcToIndex[it] = index }
            item.itemCode?.trim()?.takeIf { it.isNotBlank() }?.uppercase()?.let { epcToIndex[it] = index }
            item.boxName?.trim()?.takeIf { it.isNotBlank() }?.uppercase()?.let { epcToIndex[it] = index }
        }

        isScanActive = true
        startTagScanning(power)
    }

    fun startTagScanning(power: Int) {
        currentScanPower = power
        lastSearchUiUpdate = 0L
        lastSoundPlayAt = 0L
        scanJob?.cancel()
        scanJob = viewModelScope.launch(Dispatchers.IO) {
            if (!readerManager.initReader()) {
                isScanActive = false
                return@launch
            }

            readerManager.reader?.apply {
                setTagFocus(false)
                setFastID(false)
                setDynamicDistance(0)
            }
            readerManager.startInventoryTag(power, true)

            var drained = 0
            while (drained < maxStaleDrain && readerManager.readTagFromBuffer() != null) {
                drained++
            }

            while (isActive) {
                var tag = readerManager.readTagFromBuffer()
                if (tag?.epc == null) {
                    yield()
                    tag = readerManager.readTagFromBuffer()
                    if (tag?.epc == null) continue
                }

                do {
                    val epc = tag!!.epc?.trim()
                    val rssi = tag!!.rssi
                    val proximity = convertRssiToProximity(rssi)

                    val rssiAbs = try { Math.abs(rssi.trim().toDouble()) } catch (e: Exception) { 0.0 }
                    val id = when {
                        rssiAbs > 0 && rssiAbs < 50 -> 4
                        rssiAbs > 50 && rssiAbs < 60 -> 2
                        rssiAbs > 60 && rssiAbs < 70 -> 5
                        rssiAbs > 70 -> 1
                        else -> -1
                    }

                    val index = epcToIndex[epc?.uppercase()]

                    if (index != null && index >= 0 && index < _searchItems.size) {
                        if (id != -1) {
                            val soundNow = System.currentTimeMillis()
                            if (id != lastSoundId || soundNow - lastSoundPlayAt >= soundMinIntervalMs) {
                                lastSoundPlayAt = soundNow
                                lastSoundId?.let { readerManager.stopSound(it) }
                                lastSoundId = id
                                readerManager.playSound(id)
                            }
                        }

                        val blinkTarget = _searchItems[index].epc.trim()
                            .ifBlank { epc.orEmpty() }

                        if (blinkTarget.isNotBlank()) {
                            if (proximity > 0) {
                                if (lastBlinkEpc != blinkTarget || blinkingJob?.isActive != true) {
                                    startContinuousBlink(blinkTarget)
                                }
                            } else if (lastBlinkEpc == blinkTarget) {
                                stopBlinkingEpc()
                            }
                        }

                        val now = System.nanoTime()
                        if (lastSearchUiUpdate == 0L || now - lastSearchUiUpdate >= searchUiUpdateNs) {
                            lastSearchUiUpdate = now
                            Snapshot.withMutableSnapshot {
                                _searchItems[index] = _searchItems[index].copy(
                                    rssi = rssi,
                                    proximityPercent = proximity
                                )
                            }
                        }
                    }

                    tag = readerManager.readTagFromBuffer()
                } while (tag?.epc != null && isActive)
            }
        }
    }

    private fun startContinuousBlink(epc: String) {
        if (lastBlinkEpc == epc && blinkingJob?.isActive == true) return

        blinkingJob?.cancel()
        lastBlinkEpc = epc

        blinkingJob = viewModelScope.launch(Dispatchers.IO) {
            val reader = readerManager.reader ?: return@launch

            val filterBank = RFIDWithUHFUART.Bank_EPC
            val filterPtr = 32
            val filterCnt = epc.length * 4

            while (isActive && lastBlinkEpc == epc) {
                try {
                    readerManager.stopInventory()
                    if (!isActive) break

                    reader.readData(
                        "00000000",
                        filterBank,
                        filterPtr,
                        filterCnt,
                        epc,
                        IUHF.Bank_RESERVED,
                        4,
                        1
                    )

                    delay(blinkLedVisibleMs)

                    readerManager.startInventoryTag(currentScanPower, true)
                } catch (e: Exception) {
                    Log.e("RFID", "Blink error: ${e.message}", e)
                }

                delay(blinkCyclePauseMs)
            }
        }
    }

    private fun stopBlinkingEpc() {
        blinkingJob?.cancel()
        blinkingJob = null
        lastBlinkEpc = null
    }

    suspend fun getAllBulkItemsFromDb(): List<BulkItem> {
        return bulkRepositoryImpl.getAllBulkItems().first()
    }

    suspend fun searchOrdersByRfid(clientCode: String, rfidCode: String): List<BulkItem> {
        if (clientCode.isBlank() || rfidCode.isBlank()) return emptyList()

        val query = rfidCode.trim()
        val numericValue = query.toIntOrNull()
        return try {
            val orders = fetchOrdersForSearch(
                clientCode = clientCode,
                rfidCode = query,
                customOrderId = numericValue,
                orderId = numericValue,
                orderNo = query
            )
            orders.flatMap { order ->
                val items = order.CustomOrderItem.orEmpty()

                val isOrderLevelMatch =
                    order.CustomOrderId?.toString() == query ||
                            order.Id?.toString() == query ||
                            order.OrderNo.safeStr().equals(query, true) ||
                            order.RfidCode.safeStr().equals(query, true) ||
                            order.TidNumber.safeStr().equals(query, true)

                val matchedItems = if (isOrderLevelMatch) {
                    items
                } else {
                    items.filter { item ->
                        item.RFIDCode.safeStr().equals(query, true) ||
                                item.ItemCode.safeStr().equals(query, true) ||
                                item.TIDNumber.safeStr().equals(query, true)
                    }
                }

                matchedItems.mapNotNull { item ->
                    runCatching { item.toSearchBulkItem(order) }.getOrNull()
                }
            }.filter { it.hasSearchableIdentifier() }
        } catch (e: Exception) {
            Log.e("SearchViewModel", "Order search failed", e)
            emptyList()
        }
    }

    suspend fun searchBoxesByRfid(clientCode: String, rfidCode: String): List<BulkItem> {
        if (clientCode.isBlank() || rfidCode.isBlank()) return emptyList()

        val query = rfidCode.trim()
        return try {
            val response = boxRfidRepository.getDetailsByRfidCode(
                BoxRfidSearchRequest(clientCode = clientCode, rfidCode = query)
            )
            if (!response.isSuccessful) {
                Log.e("SearchViewModel", "Box RFID search failed: ${response.code()}")
                return emptyList()
            }

            val body = response.body()
            if (body?.success != true) {
                Log.w("SearchViewModel", "Box RFID search: ${body?.message.orEmpty()}")
                return emptyList()
            }

            body.toSearchBulkItems()
        } catch (e: Exception) {
            Log.e("SearchViewModel", "Box RFID search failed", e)
            emptyList()
        }
    }

    private fun BoxRfidDetailsResponse.toSearchBulkItems(): List<BulkItem> {
        val items = mutableListOf<BulkItem>()
        val seen = mutableSetOf<String>()

        fun addUnique(item: BulkItem?) {
            if (item == null || !item.hasSearchableIdentifier()) return
            val key = when {
                (item.productId ?: 0) > 0 -> "P:${item.productId}"
                (item.boxId ?: 0) > 0 -> "B:${item.boxId}"
                else -> listOfNotNull(item.tid, item.epc, item.rfid, item.itemCode)
                    .joinToString("|") { it.uppercase() }
                    .ifBlank { return }
            }
            if (seen.add(key)) items.add(item)
        }

        val matchType = matchType.safeStr()

        when {
            matchType.equals("Box", ignoreCase = true) -> {
                boxes.orEmpty().forEach { entry ->
                    addUnique(entry.box?.toSearchBulkItem(entry.summary))
                    entry.products.orEmpty().forEach { product ->
                        addUnique(product.toSearchBulkItem(this, entry.summary))
                    }
                }
                products.orEmpty().forEach { product ->
                    addUnique(product.toSearchBulkItem(this))
                }
            }
            else -> {
                // MatchType "Product" (or default): product first, then related box(es).
                products.orEmpty().forEach { product ->
                    addUnique(product.toSearchBulkItem(this))
                }
                boxes.orEmpty().forEach { entry ->
                    addUnique(entry.box?.toSearchBulkItem(entry.summary))
                    entry.products.orEmpty().forEach { product ->
                        addUnique(product.toSearchBulkItem(this, entry.summary))
                    }
                }
            }
        }

        return items
    }

    private suspend fun fetchOrdersForSearch(
        clientCode: String,
        rfidCode: String,
        customOrderId: Int?,
        orderId: Int?,
        orderNo: String?
    ): List<CustomOrderResponse> {
        try {
            val rfidResponse = orderRepository.searchOrdersByRfid(
                OrderSearchRequest(
                    clientCode = clientCode,
                    rfidCode = rfidCode,
                    customOrderId = customOrderId,
                    orderId = orderId,
                    orderNo = orderNo
                )
            )
            if (rfidResponse.isSuccessful && !rfidResponse.body().isNullOrEmpty()) {
                return rfidResponse.body()!!
            }
        } catch (e: Exception) {
            Log.e("SearchViewModel", "Order RFID API failed, trying full list", e)
        }

        try {
            val allResponse = orderRepository.getAllOrderList(ClientCodeRequest(clientCode))
            if (allResponse.isSuccessful && !allResponse.body().isNullOrEmpty()) {
                return allResponse.body()!!
            }
        } catch (e: Exception) {
            Log.e("SearchViewModel", "GetAllOrders API failed, trying cache", e)
        }

        return try {
            orderRepository.getOrderListCache(clientCode)
        } catch (e: Exception) {
            Log.e("SearchViewModel", "Order cache read failed", e)
            emptyList()
        }
    }

    private fun CustomOrderResponse.orderLevelMatches(query: String): Boolean {
        return matchesSearchQuery(RfidCode.safeStr(), query) ||
                matchesSearchQuery(TidNumber.safeStr(), query) ||
                matchesSearchQuery(CustomOrderId.toString(), query)
    }

    private fun CustomOrderItem.matchesOrderQuery(query: String): Boolean {
        return matchesSearchQuery(RFIDCode.safeStr(), query) ||
                matchesSearchQuery(ItemCode.safeStr(), query) ||
                matchesSearchQuery(TIDNumber.safeStr(), query)
    }

    private fun matchesSearchQuery(value: String, query: String): Boolean {
        if (value.isBlank() || query.isBlank()) return false
        return value.equals(query, true)
    }

    private fun BulkItem.hasSearchableIdentifier(): Boolean {
        return !rfid.isNullOrBlank() ||
                !itemCode.isNullOrBlank() ||
                !tid.isNullOrBlank() ||
                !epc.isNullOrBlank()
    }

    private fun String?.safeStr(): String = this?.trim().orEmpty()

    private fun CustomOrderResponse.toOrderLevelBulkItem(): BulkItem? {
        val rfid = RfidCode.safeStr()
        val tid = TidNumber.safeStr()
      //  val searchKey = rfid.ifBlank { tid }
        val searchKey = tid.ifBlank { rfid }
        if (searchKey.isBlank()) return null

        return BulkItem(
            productName = ProductName.safeStr().ifBlank { null },
            itemCode = null,
            rfid = rfid.ifBlank { null },
            epc = searchKey,
            grossWeight = null,
            stoneWeight = null,
            diamondWeight = null,
            netWeight = null,
            category = null,
            design = null,
            purity = null,
            makingPerGram = null,
            makingPercent = null,
            fixMaking = null,
            fixWastage = null,
            stoneAmount = null,
            diamondAmount = null,
            sku = null,
            tid = tid.ifBlank { null },
            box = null,
            designCode = null,
            productCode = null,
            imageUrl = null,
            totalQty = Qty.safeStr().toIntOrNull() ?: 1,
            pcs = null,
            matchedPcs = null,
            totalGwt = null,
            matchGwt = null,
            totalStoneWt = null,
            matchStoneWt = null,
            totalNetWt = null,
            matchNetWt = null,
            unmatchedQty = null,
            matchedQty = null,
            unmatchedGrossWt = null,
            mrp = MRP.safeStr().toDoubleOrNull() ?: 0.0,
            counterName = null,
            counterId = null,
            boxId = null,
            boxName = null,
            branchId = 0,
            branchName = null,
            packetId = null,
            packetName = null,
            scannedStatus = OrderStatus.safeStr().ifBlank { null },
            categoryId = CategoryId,
            productId = 0,
            branchType = null,
            designId = 0,
            vendor = null,
            totalWt = null,
            CategoryWt = null,
            SKUId = SKUId,
            purityId = 0,
            Status = null
        )
    }

    private fun CustomOrderItem.toSearchBulkItem(order: CustomOrderResponse): BulkItem {
       /* val rfid = RFIDCode.safeStr().ifBlank { order.RfidCode.safeStr() }
        val tid = TIDNumber.safeStr().ifBlank { order.TidNumber.safeStr() }
        val code = ItemCode.safeStr()
        val searchKey = rfid.ifBlank { tid }.ifBlank { code }*/

        val rfid = RFIDCode.safeStr().ifBlank { order.RfidCode.safeStr() }
        val tid = TIDNumber.safeStr().ifBlank { order.TidNumber.safeStr() }
        val code = ItemCode.safeStr()

// IMPORTANT: scanner reads EPC/TID, so keep TID first
        val searchKey = tid.ifBlank { rfid }.ifBlank { code }

        return BulkItem(
            productName = ProductName.safeStr().ifBlank { null },
            itemCode = code.ifBlank { null },
            rfid = rfid.ifBlank { code.ifBlank { null } },
            epc = searchKey.ifBlank { null },
            grossWeight = GrossWt.safeStr().ifBlank { null },
            stoneWeight = StoneWt.safeStr().ifBlank { null },
            diamondWeight = DiamondWt.safeStr().ifBlank { null },
            netWeight = NetWt.safeStr().ifBlank { null },
            category = CategoryName.safeStr().ifBlank { null },
            design = DesignName.safeStr().ifBlank { null },
            purity = PurityName.safeStr().ifBlank { null },
            makingPerGram = MakingPerGram.safeStr().ifBlank { null },
            makingPercent = MakingPercentage.safeStr().ifBlank { null },
            fixMaking = MakingFixed.safeStr().ifBlank { null },
            fixWastage = MakingFixedWastage.safeStr().ifBlank { null },
            stoneAmount = StoneAmount.safeStr().ifBlank { null },
            diamondAmount = DiamondAmount.safeStr().ifBlank { null },
            sku = SKU.safeStr().ifBlank { null },
            tid = tid.ifBlank { null },
            box = null,
            designCode = null,
            productCode = ProductCode.safeStr().ifBlank { null },
            imageUrl = Image.safeStr().ifBlank { null },
            totalQty = Quantity.safeStr().toIntOrNull() ?: 1,
            pcs = Quantity.safeStr().toIntOrNull(),
            matchedPcs = null,
            totalGwt = null,
            matchGwt = null,
            totalStoneWt = null,
            matchStoneWt = null,
            totalNetWt = null,
            matchNetWt = null,
            unmatchedQty = null,
            matchedQty = null,
            unmatchedGrossWt = null,
            mrp = MRP.safeStr().toDoubleOrNull() ?: 0.0,
            counterName = CounterId.safeStr().ifBlank { null },
            counterId = CounterId.safeStr().toIntOrNull(),
            boxId = null,
            boxName = null,
            branchId = BranchId,
            branchName = BranchName.safeStr().ifBlank { null },
            packetId = null,
            packetName = null,
            scannedStatus = OrderStatus.safeStr().ifBlank { null },
            categoryId = CategoryId ?: 0,
            productId = ProductId,
            branchType = null,
            designId = DesignId,
            vendor = VendorName.safeStr().ifBlank { null },
            totalWt = TotalWt.safeStr().toDoubleOrNull(),
            CategoryWt = null,
            SKUId = SKUId,
            purityId = PurityId,
            Status = Status.safeStr().ifBlank { null }
        )
    }

    private fun BoxRfidProduct.toSearchBulkItem(
        response: BoxRfidDetailsResponse? = null,
        boxSummary: BoxRfidBoxSummary? = null
    ): BulkItem {
        val rfid = rfidCode.safeStr().ifBlank { response?.rfidBarcode.safeStr() }
        val tid = tidNumber.safeStr().ifBlank { response?.tidValue.safeStr() }
        val hex = hexCode.safeStr()
        val code = itemCode.safeStr()
        val searchKey = tid.ifBlank { hex }.ifBlank { rfid }.ifBlank { code }
        val resolvedBoxName = boxName.safeStr()
            .ifBlank { box?.boxName.safeStr() }
            .ifBlank { null }
        val resolvedBoxId = boxId ?: box?.boxId

        return BulkItem(
            productName = productName.safeStr().ifBlank { productTitle.safeStr().ifBlank { null } },
            itemCode = code.ifBlank { null },
            rfid = rfid.ifBlank { null },
            epc = searchKey.ifBlank { null },
            grossWeight = grossWt.safeStr().ifBlank { null },
            stoneWeight = null,
            diamondWeight = null,
            netWeight = netWt.safeStr().ifBlank { null },
            category = categoryName.safeStr().ifBlank { null },
            design = designName.safeStr().ifBlank { null },
            purity = purityName.safeStr().ifBlank { null },
            makingPerGram = null,
            makingPercent = null,
            fixMaking = null,
            fixWastage = null,
            stoneAmount = null,
            diamondAmount = null,
            sku = sku.safeStr().ifBlank { null },
            tid = tid.ifBlank { null },
            box = resolvedBoxName,
            designCode = null,
            productCode = null,
            imageUrl = null,
            totalQty = pieces.safeStr().toIntOrNull() ?: 1,
            pcs = pieces.safeStr().toIntOrNull(),
            matchedPcs = null,
            totalGwt = boxSummary?.totalGrossWt,
            matchGwt = null,
            totalStoneWt = null,
            matchStoneWt = null,
            totalNetWt = boxSummary?.totalNetWt,
            matchNetWt = null,
            unmatchedQty = null,
            matchedQty = null,
            unmatchedGrossWt = null,
            mrp = mrp.safeStr().toDoubleOrNull() ?: 0.0,
            counterName = null,
            counterId = null,
            boxId = resolvedBoxId,
            boxName = resolvedBoxName,
            branchId = 0,
            branchName = null,
            packetId = null,
            packetName = null,
            scannedStatus = status.safeStr().ifBlank { null },
            categoryId = 0,
            productId = labelledStockId,
            branchType = null,
            designId = 0,
            vendor = vendorName.safeStr().ifBlank { null },
            totalWt = null,
            CategoryWt = null,
            SKUId = null,
            purityId = 0,
            Status = status.safeStr().ifBlank { null }
        )
    }

    private fun BoxRfidBoxInfo.toSearchBulkItem(summary: BoxRfidBoxSummary? = null): BulkItem {
        val rfid = rfidCode.safeStr()
        val tid = tidNumber.safeStr()
        val hex = hexCode.safeStr()
        val searchKey = tid.ifBlank { hex }.ifBlank { rfid }
        val grossWeight = summary?.totalGrossWt?.let { formatWeight(it) }
            ?: emptyWeight.safeStr().ifBlank { null }
        val netWeight = summary?.totalNetWt?.let { formatWeight(it) }
            ?: emptyWeight.safeStr().ifBlank { null }

        return BulkItem(
            productName = boxName.safeStr().ifBlank { null },
            itemCode = null,
            rfid = rfid.ifBlank { null },
            epc = searchKey.ifBlank { null },
            grossWeight = grossWeight,
            stoneWeight = null,
            diamondWeight = null,
            netWeight = netWeight,
            category = null,
            design = null,
            purity = null,
            makingPerGram = null,
            makingPercent = null,
            fixMaking = null,
            fixWastage = null,
            stoneAmount = null,
            diamondAmount = null,
            sku = null,
            tid = tid.ifBlank { hex.ifBlank { null } },
            box = boxName.safeStr().ifBlank { null },
            designCode = null,
            productCode = null,
            imageUrl = null,
            totalQty = summary?.totalProducts ?: 1,
            pcs = summary?.totalPieces,
            matchedPcs = null,
            totalGwt = summary?.totalGrossWt,
            matchGwt = null,
            totalStoneWt = null,
            matchStoneWt = null,
            totalNetWt = summary?.totalNetWt,
            matchNetWt = null,
            unmatchedQty = null,
            matchedQty = null,
            unmatchedGrossWt = null,
            mrp = 0.0,
            counterName = null,
            counterId = null,
            boxId = boxId,
            boxName = boxName.safeStr().ifBlank { null },
            branchId = 0,
            branchName = null,
            packetId = null,
            packetName = null,
            scannedStatus = null,
            categoryId = 0,
            productId = null,
            branchType = null,
            designId = 0,
            vendor = null,
            totalWt = summary?.grandTotalWeight ?: emptyWeight.safeStr().toDoubleOrNull(),
            CategoryWt = null,
            SKUId = null,
            purityId = 0,
            Status = null
        )
    }

    private fun formatWeight(value: Double): String =
        String.format(Locale.US, "%.3f", value)

    fun clearSearchItems() {
        _searchItems.clear()
        epcToIndex.clear() // PERF-FIX: Clear lookup map together with the list
    }

    fun stopSearch() {
        isScanActive = false
        scanJob?.cancel()
        scanJob = null

        blinkingJob?.cancel()
        blinkingJob = null

        readerManager.releaseScanning()

        lastSoundId?.let { readerManager.stopSound(it) }
        lastSoundId = null
        lastBlinkEpc = null
    }

  private fun convertRssiToProximity(rssi: String): Int {
        return try {
            val rssiValue = rssi.toFloat()
            ((rssiValue + 80).coerceAtLeast(0f) * 100f / 40f).toInt().coerceIn(0, 100)
        } catch (e: NumberFormatException) {
            0
        }
    }

   /* private fun convertRssiToProximity(rssi: String): Int {
        return try {
            val cleanRssi = rssi
                .replace("dBm", "", ignoreCase = true)
                .trim()
                .toFloat()

            // RSSI range: -80 (far) to -40 (near)
            val normalized = ((cleanRssi + 80) / 40f) * 100f
            normalized.toInt().coerceIn(0, 100)

        } catch (e: Exception) {
            Log.e("RSSI", "Invalid RSSI: $rssi")
            0
        }
    }*/


    /**
     * Updated lightTag method: only blink LED for matched EPC
     */
    private fun lightTag(scannedEpc: String, searchedEpc: String) {
        val reader = readerManager.reader ?: return
        if (!scannedEpc.equals(searchedEpc, ignoreCase = true)) return

        // Stop inventory temporarily
        readerManager.stopInventory()

        try {
            val filterBank = RFIDWithUHFUART.Bank_EPC
            val filterPtr = 32
            val filterCnt = searchedEpc.length * 4

            reader.readData(
                "00000000",
                filterBank,
                filterPtr,
                filterCnt,
                searchedEpc,
                IUHF.Bank_RESERVED,
                4,
                1
            )

            Log.d("RFID", "✅ LED triggered for EPC: $searchedEpc")

           Thread.sleep(100) // allow LED blink

        } catch (e: Exception) {
            Log.e("RFID", "Error lighting tag: ${e.message}", e)
        } finally {
            // Restart inventory
            readerManager.startInventoryTag(currentScanPower, true)
        }
    }

  /*  private fun startBlinkingEpc(epc: String) {
        // Cancel any previous blinking
        blinkingJob?.cancel()

        blinkingJob = viewModelScope.launch(Dispatchers.IO) {
            val reader = readerManager.reader ?: return@launch
            val filterBank = RFIDWithUHFUART.Bank_EPC
            val filterPtr = 32
            val filterCnt = epc.length * 4

            while (isActive) {
                try {
                    // Stop inventory temporarily vasanti
                   readerManager.stopInventory()

                    // Trigger LED blink for the EPC
                    reader.readData(
                        "00000000",
                        filterBank,
                        filterPtr,
                        filterCnt,
                        epc,
                        IUHF.Bank_RESERVED,
                        4,
                        1
                    )

                    // Small delay to allow LED to blink visually
                    delay(100) // adjust 50-150ms for blink speed

                } catch (e: Exception) {
                    Log.e("RFID", "Error blinking tag: ${e.message}", e)
                } finally {
                    // Restart inventory vasanti
                   readerManager.startInventoryTag(30, true)
                }
            }
        }
    }*/


}


/*
package com.loyalstring.rfid.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.local.entity.SearchItem
import com.loyalstring.rfid.data.reader.RFIDReaderManager
import com.loyalstring.rfid.repository.BulkRepositoryImpl
import com.rscja.deviceapi.RFIDWithUHFUART
import com.rscja.deviceapi.interfaces.IUHF
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val readerManager: RFIDReaderManager,
    private val bulkRepositoryImpl: BulkRepositoryImpl,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private var _searchItems = mutableStateListOf<SearchItem>()
    val searchItems: List<SearchItem> get() = _searchItems

    init {
        val unmatched = savedStateHandle.get<List<BulkItem>>("unmatchedItems") ?: emptyList()
        Log.d("SearchViewModel", "Received ${unmatched.size} items")
    }

    private var scanJob: Job? = null
    private var lastSoundId: Int? = null
    private var blinkingJob: Job? = null

    // ✅ NEW: store all nearby EPCs
    private val nearbyEpcs = mutableSetOf<String>()

    fun startSearch(unmatchedItems: List<BulkItem>, power: Int) {
        _searchItems.clear()
        _searchItems.addAll(unmatchedItems.map { item ->
            val epcValue = when {
                !item.epc.isNullOrBlank() -> item.epc!!
                !item.rfid.isNullOrBlank() -> item.rfid!!
                !item.itemCode.isNullOrBlank() -> item.itemCode!!
                else -> ""
            }
            SearchItem(
                epc = epcValue,
                itemCode = item.itemCode ?: "",
                productName = item.productName ?: "",
                rfid = item.rfid ?: ""
            )
        })

        if (readerManager.initReader()) {
            startTagScanning(power)
            startBlinkingMultipleEpcs() // ✅ start LED job ONCE
        }
    }

    fun startTagScanning(power: Int) {
        readerManager.reader?.apply {
            setTagFocus(false)
            setFastID(false)
            setDynamicDistance(0)
        }

        readerManager.startInventoryTag(power, true)

        scanJob?.cancel()
        scanJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val tag = readerManager.readTagFromBuffer()

                if (tag?.epc != null) {
                    val epc = tag.epc.trim()
                    val rssi = tag.rssi
                    val proximity = convertRssiToProximity(rssi)

                    val id = when {
                        proximity in 1..49 -> 4
                        proximity in 51..75 -> 2
                        proximity >= 76 -> 5
                        else -> -1
                    }

                    val index = _searchItems.indexOfFirst {
                        it.epc.equals(epc, true) ||
                                it.rfid.equals(epc, true) ||
                                it.itemCode.equals(epc, true)
                    }

                    if (index != -1 && _searchItems.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            _searchItems[index] = _searchItems[index].copy(
                                rssi = rssi,
                                proximityPercent = proximity
                            )

                            // 🔊 SOUND (UNCHANGED)
                            if (id != -1) {
                                lastSoundId?.let { readerManager.stopSound(it) }
                                lastSoundId = id
                                readerManager.playSound(id)
                            }

                            // 🔦 LED LOGIC (ONLY CHANGE)
                            if (proximity >= 40) {
                                nearbyEpcs.add(epc)
                            } else {
                                nearbyEpcs.remove(epc)
                            }
                        }
                    }
                } else {
                    delay(100)
                }
            }
        }
    }

    suspend fun getAllBulkItemsFromDb(): List<BulkItem> {
        return bulkRepositoryImpl.getAllBulkItems().first()
    }

    fun clearSearchItems() {
        _searchItems.clear()
    }

    fun stopSearch() {
        scanJob?.cancel()
        readerManager.stopInventory()
        lastSoundId?.let { readerManager.stopSound(it) }
        lastSoundId = null
        stopBlinkingEpc()
    }

    private fun convertRssiToProximity(rssi: String): Int {
        return try {
            val rssiValue = rssi.toFloat()
            ((rssiValue + 80).coerceAtLeast(0f) * 100f / 40f)
                .toInt()
                .coerceIn(0, 100)
        } catch (e: NumberFormatException) {
            0
        }
    }

    */
/* ================= MULTI TAG LED BLINK ================= *//*


    private fun startBlinkingMultipleEpcs() {
        blinkingJob?.cancel()

        blinkingJob = viewModelScope.launch(Dispatchers.IO) {
            val reader = readerManager.reader ?: return@launch

            while (isActive) {
                if (nearbyEpcs.isEmpty()) {
                    delay(100)
                    continue
                }

                for (epc in nearbyEpcs.toList()) {
                    if (!isActive) break

                    try {
                        readerManager.stopInventory()

                        val filterBank = RFIDWithUHFUART.Bank_EPC
                        val filterPtr = 32
                        val filterCnt = epc.length * 4

                        reader.readData(
                            "00000000",
                            filterBank,
                            filterPtr,
                            filterCnt,
                            epc,
                            IUHF.Bank_RESERVED,
                            4,
                            1
                        )

                        delay(80) // fast blink illusion

                    } catch (e: Exception) {
                        Log.e("RFID", "Error blinking EPC: $epc", e)
                    } finally {
                        readerManager.startInventoryTag(30, true)
                    }
                }
            }
        }
    }

    private fun stopBlinkingEpc() {
        blinkingJob?.cancel()
        blinkingJob = null
        nearbyEpcs.clear()
    }
}
*/
