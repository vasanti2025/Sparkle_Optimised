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
import com.loyalstring.rfid.data.reader.RFIDReaderManager
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
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val readerManager: RFIDReaderManager,
    private val bulkRepositoryImpl: BulkRepositoryImpl,
    private val orderRepository: OrderRepository
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

    // Tracks whether this VM has actually started a scan. Avoids calling stopSearch() (which
    // calls stopInventory() and blocks 2.5s on hardware timeout) when we haven't started anything
    // yet — e.g. on the very first call from ScanDisplayScreen navigation.
    private var isScanActive = false

    fun startSearch(unmatchedItems: List<BulkItem>, power: Int) {
        if (isScanActive) stopSearch() // only stop if we started a scan ourselves
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

        // PERF-FIX: Build O(1) lookup map after list is populated.
        // All three identifier fields are indexed so any of them can be matched
        // in constant time without scanning the entire list.
        _searchItems.forEachIndexed { index, item ->
            if (item.epc.isNotBlank()) epcToIndex[item.epc.uppercase()] = index
            if (item.rfid.isNotBlank()) epcToIndex[item.rfid.uppercase()] = index
            if (item.itemCode.isNotBlank()) epcToIndex[item.itemCode.uppercase()] = index
        }

        if (readerManager.initReader()) {
            startTagScanning(power)
            isScanActive = true
        }
    }

    fun startTagScanning(power: Int) {
        currentScanPower = power
        lastSearchUiUpdate = 0L
        scanJob?.cancel()
        scanJob = viewModelScope.launch(Dispatchers.IO) {
            // setTagFocus/setFastID/setDynamicDistance/startInventoryTag are blocking JNI calls.
            // They were previously before the coroutine launch, running on the calling thread
            // (main, via LaunchedEffect) and freezing the navigation transition animation.
            // Moving them here keeps everything in the single IO coroutine with no structural change.
            readerManager.reader?.apply {
                setTagFocus(false)
                setFastID(false)
                setDynamicDistance(0)
            }
            readerManager.startInventoryTag(power, true)

            // Drain tags that accumulated in the hardware buffer while the scanner was idle
            // (releaseScanning() doesn't send a hardware stop, so the device keeps scanning).
            // Without this drain, all backlogged tags are processed at once on resume, causing
            // rapid-fire playSound() + withContext(Main) calls that freeze the UI and stutter audio.
            while (readerManager.readTagFromBuffer() != null) { /* discard stale */ }

            while (isActive) {
                val tag = readerManager.readTagFromBuffer()

                if (tag?.epc != null) {
                    val epc = tag.epc.trim()
                    val rssi = tag.rssi
                    val proximity = convertRssiToProximity(rssi)

                    // SOUND-CHANGE: Replaced proximity-based sound mapping with direct RSSI
                    // absolute-value mapping to match Searchfragment.java logic:
                    // |RSSI| < 50 → sound 4, 50-60 → sound 2, 60-70 → sound 5, >70 → sound 1
                    /*
                    val id = when {
                        proximity >= 70 -> 1
                        proximity in 61..69 -> 5
                        proximity in 51..59 -> 2
                        proximity in 1..49 -> 4
                        else -> -1
                    }
                    */
                    val rssiAbs = try { Math.abs(rssi.trim().toDouble()) } catch (e: Exception) { 0.0 }
                    val id = when {
                        rssiAbs > 0 && rssiAbs < 50 -> 4
                        rssiAbs > 50 && rssiAbs < 60 -> 2
                        rssiAbs > 60 && rssiAbs < 70 -> 5
                        rssiAbs > 70 -> 1
                        else -> -1
                    }

                    // PERF-FIX: O(1) HashMap lookup replaces O(n) indexOfFirst { } search.
                    // At high scan rates (100+ tags/sec) with large item lists, the old
                    // indexOfFirst caused the IO thread to iterate the full list for every
                    // tag read, creating CPU spikes and stalling the scan buffer reader.
                    // val index = _searchItems.indexOfFirst { it.epc.equals(epc, true) || it.rfid.equals(epc, true) || it.itemCode.equals(epc, true) }
                    val index = epcToIndex[epc.uppercase()]

                    if (index != null && index >= 0 && index < _searchItems.size) {
                        // Throttle main-thread dispatches to at most once per 100ms.
                        // On inventory restart, all nearby tags respond at once (standard RFID
                        // behavior), flooding the scan loop. Without throttling, rapid-fire
                        // withContext(Main) calls overwhelm recomposition and distort audio.
                        val now = System.currentTimeMillis()
                        if (now - lastSearchUiUpdate >= 50) {
                            lastSearchUiUpdate = now
                            withContext(Dispatchers.Main) {
                                _searchItems[index] = _searchItems[index].copy(
                                    rssi = rssi,
                                    proximityPercent = proximity
                                )

                                /*
                                val currentTime = System.currentTimeMillis()

                                // Pure time-based sound throttle: fire only if 400ms have passed.
                                // The old condition (lastSoundId != id) also fires on every RSSI
                                // bucket change during a burst, causing rapid overlapping playback
                                // and audio distortion when many tags are read at once.
                                if (id != -1 && currentTime - lastSoundTime > 400) {
                                    lastSoundId = id
                                    readerManager.playSound(id)
                                    lastSoundTime = currentTime
                                }
                                */

                                // Sound logic matching Searchfragment.java:
                                // Stop the previous stream then play the new one on every tag read.
                                // playSound() in RFIDReaderManager now stops all active streams
                                // before starting a new one, so overlap/distortion is prevented
                                // at the hardware level without needing a time throttle here.
                                if (id != -1) {
                                    lastSoundId?.let { readerManager.stopSound(it) }
                                    lastSoundId = id
                                    readerManager.playSound(id)
                                }

                                val searchedEpc = _searchItems[index].epc.trim()
                                val epcMatched = epc.equals(searchedEpc, ignoreCase = true)

                                if (epcMatched && proximity >= 40) {
                                    if (lastBlinkEpc != epc || blinkingJob?.isActive != true) {
                                        startContinuousBlink(epc)
                                    }
                                } else if (lastBlinkEpc == epc && proximity < 40) {
                                    stopBlinkingEpc()
                                }
                            }
                        }
                    }
                } else {
                    delay(50)
                }
            }
        }
    }

    suspend fun getAllBulkItemsFromDb(): List<BulkItem> {
        return bulkRepositoryImpl.getAllBulkItems().first()
    }

    suspend fun searchOrdersByRfid(clientCode: String, rfidCode: String): List<BulkItem> {
        if (clientCode.isBlank() || rfidCode.isBlank()) return emptyList()

        val query = rfidCode.trim()
        return try {
            val orders = fetchOrdersForSearch(clientCode, query)
            orders.flatMap { order ->
                val parentMatches = order.orderLevelMatches(query)
                val items = order.CustomOrderItem.orEmpty()

                when {
                    parentMatches && items.isEmpty() -> {
                        listOfNotNull(
                            runCatching { order.toOrderLevelBulkItem() }
                                .onFailure { e ->
                                    Log.e("SearchViewModel", "Skip invalid order header", e)
                                }
                                .getOrNull()
                        )
                    }
                    parentMatches -> {
                        items.mapNotNull { item ->
                            runCatching { item.toSearchBulkItem(order) }
                                .onFailure { e ->
                                    Log.e("SearchViewModel", "Skip invalid order item", e)
                                }
                                .getOrNull()
                        }
                    }
                    else -> {
                        items
                            .filter { it.matchesOrderQuery(query) }
                            .mapNotNull { item ->
                                runCatching { item.toSearchBulkItem(order) }
                                    .onFailure { e ->
                                        Log.e("SearchViewModel", "Skip invalid order item", e)
                                    }
                                    .getOrNull()
                            }
                    }
                }
            }.filter { it.hasSearchableIdentifier() }
        } catch (e: Exception) {
            Log.e("SearchViewModel", "Order search failed", e)
            emptyList()
        }
    }

    private suspend fun fetchOrdersForSearch(clientCode: String, rfidCode: String): List<CustomOrderResponse> {
        try {
            val rfidResponse = orderRepository.searchOrdersByRfid(
                OrderSearchRequest(clientCode = clientCode, rfidCode = rfidCode)
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
                matchesSearchQuery(TidNumber.safeStr(), query)
    }

    private fun CustomOrderItem.matchesOrderQuery(query: String): Boolean {
        return matchesSearchQuery(RFIDCode.safeStr(), query) ||
                matchesSearchQuery(ItemCode.safeStr(), query) ||
                matchesSearchQuery(TIDNumber.safeStr(), query)
    }

    private fun matchesSearchQuery(value: String, query: String): Boolean {
        if (value.isBlank() || query.isBlank()) return false
        return value.equals(query, true) || value.contains(query, true)
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

        // releaseScanning() instead of stopInventory():
        // stopInventory() sends a hardware stop command that takes 2.5s (5 retries × 500ms,
        // always fails with -1 on this device). If the user restarts scanning quickly, the
        // concurrent stopInventory() + startInventoryTag() JNI calls race on the UART device,
        // causing tags to buffer and sound events to queue then fire all at once.
        // releaseScanning() resets the flag and stops sound instantly with no hardware call.
        // The next startInventoryTag() restarts the session cleanly.
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

                  // If cancelled while stopInventory() was blocking, don't restart inventory —
                  // the new scan's startInventoryTag() will handle it. Restarting here would
                  // race with the new scan and cause sound/UI bursts.
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

                  delay(120) // LED blink visible

                  readerManager.startInventoryTag(currentScanPower, true)

              } catch (e: Exception) {
                  Log.e("RFID", "Blink error: ${e.message}", e)
              }

              delay(500)
          }
      }
  }



    private fun stopBlinkingEpc() {
        blinkingJob?.cancel()
        blinkingJob = null
        lastBlinkEpc = null
    }



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
