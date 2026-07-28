package com.loyalstring.rfid.viewmodel

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.local.entity.UserPermissionEntity
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.stockTransfer.CancelStockTransfer
import com.loyalstring.rfid.data.model.stockTransfer.CancelStockTransferResponse
import com.loyalstring.rfid.data.model.stockTransfer.LabelledStockItems
import com.loyalstring.rfid.data.model.stockTransfer.STApproveRejectRequest
import com.loyalstring.rfid.data.model.stockTransfer.STApproveRejectResponse
import com.loyalstring.rfid.data.model.stockTransfer.StockInOutRequest
import com.loyalstring.rfid.data.model.stockTransfer.StockTransferInOutResponse
import com.loyalstring.rfid.data.model.stockTransfer.StockTransferResponse
import com.loyalstring.rfid.data.model.stockVerification.AccessibleCompany

import com.loyalstring.rfid.data.remote.data.StockTransferRequest
import com.loyalstring.rfid.repository.BulkRepositoryImpl
import com.loyalstring.rfid.repository.SingleProductRepository
import com.loyalstring.rfid.repository.TransferRepository
import com.loyalstring.rfid.repository.UserPermissionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.loyalstring.rfid.data.model.stockTransfer.StockTransferLineItem
import com.loyalstring.rfid.ui.utils.TransferLocationLookups
import com.loyalstring.rfid.ui.utils.TransferMasterData
import com.loyalstring.rfid.ui.utils.bulkItemToLabelledStock
import com.loyalstring.rfid.ui.utils.resolveLabelledItemsFromTransfer
import com.loyalstring.rfid.ui.utils.resolveTransferFromToNamesAsync

data class TransferDetailSession(
    val transferId: Int = 0,
    val requestType: String = "In Request",
    val selectedTransferType: String = "Transfer Type",
    val isSelfApproval: Boolean = false
)


@HiltViewModel
class StockTransferViewModel @Inject constructor(
    private val repository: TransferRepository,
    private val bulkRepository: BulkRepositoryImpl,
    private val productRepository: SingleProductRepository,
    private val userPermissionRepository: UserPermissionRepository
) : ViewModel() {

    /** -------------------- State & UI data -------------------- **/
    val transferTypes = repository.transferTypes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTransferType = MutableStateFlow<String?>(null)
    val selectedTransferType: StateFlow<String?> = _selectedTransferType.asStateFlow()

    private val _counterNames = MutableStateFlow<List<String>>(emptyList())
    val counterNames: StateFlow<List<String>> = _counterNames

    private val _branchNames = MutableStateFlow<List<String>>(emptyList())
    val branchNames: StateFlow<List<String>> = _branchNames

    private val _boxNames = MutableStateFlow<List<String>>(emptyList())
    val boxNames: StateFlow<List<String>> = _boxNames

    private val _fromOptions = MutableStateFlow<List<String>>(emptyList())
    val fromOptions: StateFlow<List<String>> = _fromOptions

    private val _toOptions = MutableStateFlow<List<String>>(emptyList())
    val toOptions: StateFlow<List<String>> = _toOptions

    val currentFrom = MutableStateFlow("")
    val currentTo = MutableStateFlow("")

    private val _filteredBulkItems = MutableStateFlow<List<BulkItem>>(emptyList())
    val filteredBulkItems: StateFlow<List<BulkItem>> = _filteredBulkItems

    private val _transferStatus = MutableStateFlow<Result<String>?>(null)
    val transferStatus: StateFlow<Result<String>?> = _transferStatus

    private val _stApproveRejectResponse = MutableLiveData<Result<STApproveRejectResponse>>()
    val stApproveRejectResponse: LiveData<Result<STApproveRejectResponse>> = _stApproveRejectResponse

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    private val _allBulkItems = MutableStateFlow<List<BulkItem>>(emptyList())
    private val allBulkItems: StateFlow<List<BulkItem>> = _allBulkItems.asStateFlow()

    private val _categoryFilters = MutableStateFlow<List<String>>(emptyList())
    val categoryFilters: StateFlow<List<String>> = _categoryFilters

    private val _productFilters = MutableStateFlow<List<String>>(emptyList())
    val productFilters: StateFlow<List<String>> = _productFilters

    private val _designFilters = MutableStateFlow<List<String>>(emptyList())
    val designFilters: StateFlow<List<String>> = _designFilters


    val distinctCategories = MutableStateFlow<List<String>>(emptyList())
    val distinctProducts = MutableStateFlow<List<String>>(emptyList())
    val distinctDesigns = MutableStateFlow<List<String>>(emptyList())

    // Store all API responses with labelled items
    var allStockTransferResponseList: List<StockTransferInOutResponse> = emptyList()
        private set

    // In StockTransferViewModel.kt
    private val _labelledStockItems = MutableLiveData<List<LabelledStockItems>>()
    val labelledStockItems: LiveData<List<LabelledStockItems>> = _labelledStockItems

    private val _stockTransferDetail = MutableLiveData<StockTransferResponse?>()
    val stockTransferDetail: LiveData<StockTransferResponse?> = _stockTransferDetail

    private val _cancelResponse =
        MutableStateFlow<Result<CancelStockTransferResponse>?>(null)
    val cancelResponse: StateFlow<Result<CancelStockTransferResponse>?> = _cancelResponse

    private val _transferPreviewItems = MutableStateFlow<List<BulkItem>>(emptyList())
    val transferPreviewItems: StateFlow<List<BulkItem>> = _transferPreviewItems

    private val _transferDetailSession = MutableStateFlow(TransferDetailSession())
    val transferDetailSession: StateFlow<TransferDetailSession> = _transferDetailSession.asStateFlow()

    private val _detailLabelItems = MutableStateFlow<List<LabelledStockItems>>(emptyList())
    val detailLabelItems: StateFlow<List<LabelledStockItems>> = _detailLabelItems.asStateFlow()

    private val _transferCache = MutableStateFlow<Map<Int, StockTransferInOutResponse>>(emptyMap())

    private val _locationLookups = MutableStateFlow(TransferLocationLookups())
    val locationLookups: StateFlow<TransferLocationLookups> = _locationLookups.asStateFlow()

    private val _masterData = MutableStateFlow(TransferMasterData())
    val masterData: StateFlow<TransferMasterData> = _masterData.asStateFlow()

    private val _resolvedFromTo = MutableStateFlow<Map<Int, Pair<String, String>>>(emptyMap())
    val resolvedFromTo: StateFlow<Map<Int, Pair<String, String>>> = _resolvedFromTo.asStateFlow()

    private var pendingFromLocationName: String = ""
    private var pendingToLocationName: String = ""

    fun setPendingLocationNames(from: String, to: String) {
        pendingFromLocationName = from.trim()
        pendingToLocationName = to.trim()
    }

    fun getPendingFromLocationName(): String = pendingFromLocationName
    fun getPendingToLocationName(): String = pendingToLocationName

    fun loadLocationLookups() {
        viewModelScope.launch {
            try {
                _locationLookups.value = TransferLocationLookups(
                    counterPairs = bulkRepository.bulkItemDao.getCounterIdNamePairs(),
                    boxPairs = bulkRepository.bulkItemDao.getBoxIdNamePairs(),
                    branchPairs = bulkRepository.bulkItemDao.getBranchIdNamePairs(),
                    packetPairs = bulkRepository.bulkItemDao.getPacketIdNamePairs()
                )
            } catch (e: Exception) {
                Log.e("StockTransferVM", "Error loading location lookups: ${e.message}")
            }
        }
    }

    fun loadMasterLocationData(clientCode: String) {
        if (clientCode.isBlank()) return
        viewModelScope.launch {
            try {
                val request = ClientCodeRequest(clientCode)
                val counters = productRepository.getAllCounters(request).body().orEmpty()
                val branches = productRepository.getAllBranches(request).body().orEmpty()
                val boxes = productRepository.getAllBoxes(request).body().orEmpty()
                val packets = productRepository.getAllPackets(request).body().orEmpty()
                val lookups = TransferLocationLookups(
                    counterPairs = bulkRepository.bulkItemDao.getCounterIdNamePairs(),
                    boxPairs = bulkRepository.bulkItemDao.getBoxIdNamePairs(),
                    branchPairs = bulkRepository.bulkItemDao.getBranchIdNamePairs(),
                    packetPairs = bulkRepository.bulkItemDao.getPacketIdNamePairs()
                )
                _locationLookups.value = lookups
                _masterData.value = TransferMasterData(
                    counters = counters,
                    branches = branches,
                    boxes = boxes,
                    packets = packets,
                    lookups = lookups
                )
                refreshTransferDisplayNames()
            } catch (e: Exception) {
                Log.e("StockTransferVM", "Error loading master location data: ${e.message}")
            }
        }
    }

    fun refreshTransferDisplayNames() {
        viewModelScope.launch {
            val transfers = _transferCache.value.values.toList()
            if (transfers.isEmpty()) {
                _resolvedFromTo.value = emptyMap()
                return@launch
            }
            val master = _masterData.value
            val resolved = transfers.associate { transfer ->
                val items = resolveLabelledItemsFromTransfer(transfer)
                transfer.Id to resolveTransferFromToNamesAsync(
                    transfer = transfer,
                    masterData = master,
                    bulkRepository = bulkRepository,
                    labelledItems = items
                )
            }
            _resolvedFromTo.value = resolved
        }
    }

    fun cacheStockTransfers(transfers: List<StockTransferInOutResponse>) {
        if (transfers.isEmpty()) return
        _transferCache.value = _transferCache.value + transfers.associateBy { it.Id }
        refreshTransferDisplayNames()
    }

    private suspend fun enrichLabelledItemsFromDatabase(
        items: List<LabelledStockItems>,
        lineItems: List<StockTransferLineItem>
    ): List<LabelledStockItems> {
        if (items.isEmpty() && lineItems.isEmpty()) return items

        return if (items.isNotEmpty()) {
            items.map { item ->
                val stockId = item.Id?.takeIf { it > 0 } ?: return@map item
                val needsEnrichment = item.ItemCode.isNullOrBlank() && item.CategoryName.isNullOrBlank()
                if (!needsEnrichment) return@map item

                val bulkItem = bulkRepository.bulkItemDao.getById(stockId) ?: return@map item
                bulkItemToLabelledStock(
                    bulkItem = bulkItem,
                    lineItem = lineItems.firstOrNull { line ->
                        line.LabelledStockId == stockId || line.StockId == stockId
                    }
                ).copy(
                    TransferItemId = item.TransferItemId,
                    RequestStatus = item.RequestStatus
                )
            }
        } else {
            lineItems.mapNotNull { line ->
                val stockId = line.LabelledStockId?.takeIf { it > 0 }
                    ?: line.StockId?.takeIf { it > 0 }
                    ?: return@mapNotNull null
                val bulkItem = bulkRepository.bulkItemDao.getById(stockId)
                if (bulkItem != null) {
                    bulkItemToLabelledStock(bulkItem, line)
                } else {
                    com.loyalstring.rfid.ui.utils.lineItemToLabelledStock(line)
                }
            }
        }
    }

    private suspend fun resolveTransferDetailItems(
        transferId: Int,
        fallbackItems: List<LabelledStockItems> = emptyList()
    ): List<LabelledStockItems> {
        val transfer = _transferCache.value[transferId]
        val resolved = fallbackItems.takeIf { it.isNotEmpty() }
            ?: resolveLabelledItemsFromTransfer(transfer)
        val lineItems = transfer?.StockTransferItems.orEmpty()
        return enrichLabelledItemsFromDatabase(resolved, lineItems)
    }

    fun setTransferPreviewItems(items: List<BulkItem>) {
        _transferPreviewItems.value = items
    }

    fun clearTransferPreviewItems() {
        _transferPreviewItems.value = emptyList()
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private fun labelledTransferItems(items: List<BulkItem>): List<BulkItem> =
        items.filter { !it.itemCode.isNullOrBlank() || !it.rfid.isNullOrBlank() }

    fun loadAllLabelledStock() = viewModelScope.launch {
        try {
            _isLoading.value = true
            val allItems = bulkRepository.getAllBulkItems().first()
            _allBulkItems.value = allItems
            _filteredBulkItems.value = labelledTransferItems(allItems)
        } catch (e: Exception) {
            Log.e("StockTransferVM", "Error loading labelled stock: ${e.message}")
        } finally {
            _isLoading.value = false
        }
    }

/*    suspend fun loadAllLabelledStock() {

        val labelledItems = mutableListOf<BulkItem>()

        allBulkItems.collect { list ->
            list.forEach { item ->
                if (!item.itemCode.isNullOrBlank()) {
                    labelledItems.add(item)
                }
            }
        }

        _filteredBulkItems.value = labelledItems
    }*/


    /** -------------------- Load Transfer Types -------------------- **/
    fun loadTransferTypes(request: ClientCodeRequest) {
        viewModelScope.launch {
            try {
                repository.refreshTransferTypes(request)
            } catch (e: Exception) {
                Log.e("StockTransferVM", "Error loading transfer types: ${e.message}")
            }
        }
    }

    fun onTransferTypeSelected(type: String) {
        _selectedTransferType.value = type
    }

    /** -------------------- Local DB Fetch -------------------- **/
    fun fetchCounterNames() = viewModelScope.launch {
        _counterNames.value = bulkRepository.getDistinctCounterNames()
    }

    fun fetchBranchNames() = viewModelScope.launch {
        _branchNames.value = bulkRepository.getDistinctBranchNames()
    }

    fun fetchBoxNames() = viewModelScope.launch {
        _boxNames.value = bulkRepository.getDistinctBoxNames()
    }

    /** -------------------- Transfer Type Parsing -------------------- **/
    fun extractFromAndToOptions(transferType: String) {
        val parts = transferType.split(" to ", ignoreCase = true)
        if (parts.size != 2) return

        val from = parts[0].trim().lowercase()
        val to = parts[1].trim().lowercase()
        currentFrom.value = from
        currentTo.value = to

        viewModelScope.launch {
            _fromOptions.value = when (from) {
                "box" -> bulkRepository.getDistinctBoxNames()
                "branch" -> bulkRepository.getDistinctBranchNames()
                "counter" -> bulkRepository.getDistinctCounterNames()
                else -> emptyList()
            }

            _toOptions.value = when (to) {
                "box" -> bulkRepository.getDistinctBoxNames()
                "branch" -> bulkRepository.getDistinctBranchNames()
                "counter" -> bulkRepository.getDistinctCounterNames()
                else -> emptyList()
            }
        }
    }
    fun getTransferTypeId(transferTypeName: String): Int {
        return transferTypes.value
            .firstOrNull { it.TransferType.equals(transferTypeName, ignoreCase = true) }
            ?.Id ?: -1
    }

    private val _accessibleBranches = MutableStateFlow<List<String>>(emptyList())
    val accessibleBranches: StateFlow<List<String>> = _accessibleBranches

    fun loadAccessibleBranches(branches: List<String>) {
        _accessibleBranches.value = branches
    }

    private val _accessibleCompany = MutableStateFlow<List<UserPermissionEntity>>(emptyList())
    val accessibleCompany: StateFlow<List<UserPermissionEntity>> = _accessibleCompany

    fun loadAccessibleCompany(companyEmp: List<UserPermissionEntity>) {
        _accessibleCompany.value = companyEmp
    }

    private val _transferSourceBranchId = MutableStateFlow<Int?>(null)
    val transferSourceBranchId: StateFlow<Int?> = _transferSourceBranchId

    private val _transferDestinationBranchId = MutableStateFlow<Int?>(null)
    val transferDestinationBranchId: StateFlow<Int?> = _transferDestinationBranchId

    fun setTransferBranches(source: Int, destination: Int) {
        _transferSourceBranchId.value = source
        _transferDestinationBranchId.value = destination
    }




        fun extractCategoryProductDesignFromFiltered() {
        val currentFiltered = _filteredBulkItems.value

        _categoryFilters.value = currentFiltered
            .mapNotNull { it.category?.takeIf { name -> name.isNotBlank() } }
            .distinct()

        _productFilters.value = currentFiltered
            .mapNotNull { it.productName?.takeIf { name -> name.isNotBlank() } }
            .distinct()

        _designFilters.value = currentFiltered
            .mapNotNull { it.design?.takeIf { name -> name.isNotBlank() } }
            .distinct()

        Log.d("StockTransferVM", "Category filters=${_categoryFilters.value.size}, Product filters=${_productFilters.value.size}, Design filters=${_designFilters.value.size}")
    }

    /**-------------------- Filter Local Bulk Items --------------------**/

    fun filterBulkItemsByFrom(fromType: String, selectedValue: String) = viewModelScope.launch {
        val allItems = bulkRepository.getAllBulkItems().first()
        _allBulkItems.value = allItems

        val normalizedValue = selectedValue.trim()
        val filtered = when (fromType.lowercase()) {
            "counter" -> allItems.filter { it.counterName?.trim().equals(normalizedValue, true) }
            "branch" -> allItems.filter { it.branchName?.trim().equals(normalizedValue, true) }
            "box" -> allItems.filter { it.boxName?.trim().equals(normalizedValue, true) }
            "packet" -> allItems.filter { it.packetName?.trim().equals(normalizedValue, true) }
            "display" -> allItems.filter { it.counterId == 0 || it.counterName.isNullOrBlank() }
            else -> allItems
        }
        _filteredBulkItems.value = labelledTransferItems(filtered)
    }
    fun filterItemsByCategory(category: String) {
        viewModelScope.launch {
            _filteredBulkItems.value = _filteredBulkItems.value.filter {
                it.category.equals(category, ignoreCase = true)
            }
            extractCategoryProductDesignFromFiltered()
        }
    }

    fun filterItemsByProduct(product: String) {
        viewModelScope.launch {
            _filteredBulkItems.value = _filteredBulkItems.value.filter {
                it.productName.equals(product, ignoreCase = true)
            }
            extractCategoryProductDesignFromFiltered()
        }
    }

    fun filterItemsByDesign(design: String) {
        viewModelScope.launch {
            _filteredBulkItems.value = _filteredBulkItems.value.filter {
                it.design.equals(design, ignoreCase = true)
            }
            extractCategoryProductDesignFromFiltered()
        }
    }


    /** --------------------ID Fetch Helpers-------------------- **/
    suspend fun getEntityIdByName(type: String, name: String): Int {
        return when (type.lowercase()) {
            "counter" -> bulkRepository.getCounterIdFromName(name)
            "branch" -> bulkRepository.getBranchIdFromName(name)
            "box" -> bulkRepository.getBoxIdFromName(name)
            "packet" -> bulkRepository.getBoxIdFromName(name)
            else -> null
        } ?: 0
    }


    fun removeTransferredItems(items: List<BulkItem>) {
        val currentList = _filteredBulkItems.value.toMutableList()
        currentList.removeAll(items.toSet())
        _filteredBulkItems.value = currentList
    }

    fun addBackToFiltered(items: List<BulkItem>) {
        val currentList = _filteredBulkItems.value.toMutableList()
        currentList.addAll(items)
        _filteredBulkItems.value = currentList
    }



    /** --------------------Submit Stock Transfer-------------------- **/
    fun submitStockTransfer(request1: StockTransferRequest) = viewModelScope.launch {
        try {
          /*  val request = StockTransferRequest(
                ClientCode = clientCode,
                StockTransferItems = stockIds.map { StockTransferItem(it) },
                StockType = "labelled",
                TransferTypeId = transferTypeId,
                TransferByEmployee = transferByEmployee,
                TransferedToBranch = toId.toString(),
                Source = fromId,
                Destination = toId,
                Remarks = "",
                ReceivedByEmployee = ""
            )*/

            val result = repository.submitStockTransfer(request1)
            _transferStatus.value = result.map { "Transfer successful" }
           // onResult(result.isSuccess)
        } catch (e: Exception) {
            _transferStatus.value = Result.failure(e)
            //onResult(false)
        }
    }

    /** -------------------- Fetch All Stock Transfers -------------------- **/
    fun getAllStockTransfers(
        request: StockInOutRequest,
        onResult: (Result<List<StockTransferInOutResponse>>) -> Unit
    ) = viewModelScope.launch {
        try {
            val result = repository.getAllStockTransfers(request)
            onResult(result)
        } catch (e: Exception) {
            Log.e("StockTransferVM", "Error fetching stock transfers: ${e.message}")
            onResult(Result.failure(e))
        }
    }



    // ✅ Approve/Reject Stock Transfer
   /* fun stApproveReject(request: STApproveRejectRequest) {
        viewModelScope.launch {
            val result = repository.stApproveReject(request)
            result.onFailure {
                _errorMessage.postValue(it.localizedMessage ?: "Failed to process stock transfer approval")
            }
            _stApproveRejectResponse.postValue(result)
        }
    }*/
    fun stApproveReject(request: STApproveRejectRequest) {
        viewModelScope.launch {
            try {
                val result = repository.stApproveReject(request)
                _stApproveRejectResponse.value =result
            } catch (e: Exception) {
                _stApproveRejectResponse.value = Result.failure(e)
            }
        }
    }
    @SuppressLint("NullSafeMutableLiveData")
    fun clearApproveResult() {
        _stApproveRejectResponse.postValue(null)
    }

    fun clearTransferStatus() {
        _transferStatus.value = null
    }

    fun setLabelledStockItems(items: List<LabelledStockItems>) {
        if (items.isNotEmpty()) {
            _detailLabelItems.value = items
        }
        _labelledStockItems.postValue(items)
    }

    fun openTransferDetail(
        transferId: Int,
        requestType: String,
        selectedTransferType: String,
        isSelfApproval: Boolean,
        items: List<LabelledStockItems>
    ) {
        _transferDetailSession.value = TransferDetailSession(
            transferId = transferId,
            requestType = requestType,
            selectedTransferType = selectedTransferType,
            isSelfApproval = isSelfApproval
        )

        val immediateItems = items.takeIf { it.isNotEmpty() }
            ?: resolveLabelledItemsFromTransfer(_transferCache.value[transferId])
        _detailLabelItems.value = immediateItems
        _labelledStockItems.postValue(immediateItems)

        viewModelScope.launch {
            val resolvedItems = resolveTransferDetailItems(
                transferId = transferId,
                fallbackItems = immediateItems
            )
            if (resolvedItems.isNotEmpty()) {
                _detailLabelItems.value = resolvedItems
                _labelledStockItems.postValue(resolvedItems)
            }
            Log.d(
                "TransferDetail",
                "openTransferDetail id=$transferId requestType=$requestType selfApproval=$isSelfApproval items=${resolvedItems.size}"
            )
        }
    }

    private suspend fun fetchTransferLabelItems(
        clientCode: String,
        transferId: Int,
        requestType: String,
        userId: Int,
        branchId: Int
    ): List<LabelledStockItems> {
        suspend fun fetch(requestTypeValue: String): List<LabelledStockItems> {
            val request = StockInOutRequest(
                ClientCode = clientCode,
                StockType = "labelled",
                TransferType = null,
                BranchId = branchId,
                UserID = userId,
                RequestType = requestTypeValue
            )
            val responseList = repository.getAllStockTransfers(request).getOrNull().orEmpty()
            cacheStockTransfers(responseList)
            val transfer = responseList.firstOrNull { it.Id == transferId }
            return resolveLabelledItemsFromTransfer(transfer)
        }

        val primaryItems = fetch(requestType)
        val resolved = if (primaryItems.isNotEmpty()) {
            primaryItems
        } else if (requestType == "Out Request") {
            fetch("In Request")
        } else {
            emptyList()
        }

        return enrichLabelledItemsFromDatabase(
            items = resolved,
            lineItems = _transferCache.value[transferId]?.StockTransferItems.orEmpty()
        )
    }

    fun loadTransferDetailItems(
        clientCode: String,
        userId: Int,
        branchId: Int,
        forceRefresh: Boolean = false
    ) {
        val session = _transferDetailSession.value
        if (session.transferId <= 0) return

        viewModelScope.launch {
            val cachedItems = _detailLabelItems.value
            if (!forceRefresh && cachedItems.isNotEmpty()) {
                _labelledStockItems.postValue(cachedItems)
                return@launch
            }

            try {
                val fetchedItems = fetchTransferLabelItems(
                    clientCode = clientCode,
                    transferId = session.transferId,
                    requestType = session.requestType,
                    userId = userId,
                    branchId = branchId
                )

                val enrichedFetched = enrichLabelledItemsFromDatabase(
                    items = fetchedItems,
                    lineItems = _transferCache.value[session.transferId]?.StockTransferItems.orEmpty()
                )

                val resolvedItems = when {
                    enrichedFetched.isNotEmpty() -> enrichedFetched
                    cachedItems.isNotEmpty() -> cachedItems
                    else -> emptyList()
                }

                if (resolvedItems.isNotEmpty() || cachedItems.isEmpty()) {
                    _detailLabelItems.value = resolvedItems
                }
                _labelledStockItems.postValue(_detailLabelItems.value)
                Log.d(
                    "TransferDetail",
                    "loadTransferDetailItems id=${session.transferId} fetched=${fetchedItems.size} resolved=${resolvedItems.size}"
                )
            } catch (e: Exception) {
                Log.e("TransferDetail", "loadTransferDetailItems failed", e)
                if (cachedItems.isNotEmpty()) {
                    _labelledStockItems.postValue(cachedItems)
                }
            }
        }
    }

    fun getLabelledStockByTransferId(
        clientCode: String,
        mainObjectId: Int,
        requestType: String,
        userId: Int,
        branchId: Int,
        fallbackItems: List<LabelledStockItems> = emptyList()
    ) {
        viewModelScope.launch {
            try {
                val request = StockInOutRequest(
                    ClientCode = clientCode,
                    StockType = "labelled",
                    TransferType = null,
                    BranchId = branchId,
                    UserID = userId,
                    RequestType = requestType
                )

                val result = repository.getAllStockTransfers(request)
                val responseList = result.getOrNull() ?: emptyList()
                cacheStockTransfers(responseList)
                val matchedTransfer = responseList.firstOrNull { it.Id == mainObjectId }
                val apiItems = resolveLabelledItemsFromTransfer(matchedTransfer)
                var resolvedItems = when {
                    apiItems.isNotEmpty() -> apiItems
                    fallbackItems.isNotEmpty() -> fallbackItems
                    else -> emptyList()
                }

                if (resolvedItems.isEmpty() && requestType == "Out Request") {
                    val inRequestItems = fetchTransferLabelItems(
                        clientCode = clientCode,
                        transferId = mainObjectId,
                        requestType = "In Request",
                        userId = userId,
                        branchId = branchId
                    )
                    if (inRequestItems.isNotEmpty()) {
                        resolvedItems = inRequestItems
                    }
                }

                if (resolvedItems.isNotEmpty()) {
                    _detailLabelItems.value = resolvedItems
                }

                if (matchedTransfer != null) {
                    Log.d(
                        "DEBUG_LABELLED",
                        "Matched transfer Id=${matchedTransfer.Id}, apiItems=${apiItems.size}, resolved=${resolvedItems.size}"
                    )
                } else {
                    Log.d(
                        "DEBUG_LABELLED",
                        "No transfer found for Id=$mainObjectId, using fallback=${fallbackItems.size}"
                    )
                }

                _labelledStockItems.postValue(resolvedItems)
            } catch (e: Exception) {
                Log.e("DEBUG_LABELLED", "Error fetching transfer details", e)
                _labelledStockItems.postValue(fallbackItems)
            }
        }
    }

    fun cancelStockTransfer(id: Int, clientCode: String) {
        viewModelScope.launch {
            val request = CancelStockTransfer(Id = id, ClientCode = clientCode)
            val result = repository.cancelStockTransfer(request)
            _cancelResponse.value = result
        }
    }
    fun clearCancelResponse() {
        _cancelResponse.value = null
    }




}


