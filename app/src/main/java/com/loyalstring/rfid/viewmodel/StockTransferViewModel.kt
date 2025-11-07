package com.loyalstring.rfid.viewmodel

import android.annotation.SuppressLint
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.stockTransfer.CancelStockTransfer
import com.loyalstring.rfid.data.model.stockTransfer.CancelStockTransferResponse
import com.loyalstring.rfid.data.model.stockTransfer.LabelledStockItems
import com.loyalstring.rfid.data.model.stockTransfer.STApproveRejectRequest
import com.loyalstring.rfid.data.model.stockTransfer.STApproveRejectResponse
import com.loyalstring.rfid.data.model.stockTransfer.StockInOutRequest
import com.loyalstring.rfid.data.model.stockTransfer.StockTransferInOutResponse
import com.loyalstring.rfid.data.model.stockTransfer.StockTransferResponse
import com.loyalstring.rfid.data.remote.data.StockTransferItem
import com.loyalstring.rfid.data.remote.data.StockTransferRequest
import com.loyalstring.rfid.repository.BulkRepositoryImpl
import com.loyalstring.rfid.repository.TransferRepository
import com.loyalstring.rfid.repository.UserPermissionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.firstOrNull


@HiltViewModel
class StockTransferViewModel @Inject constructor(
    private val repository: TransferRepository,
    private val bulkRepository: BulkRepositoryImpl,
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
        _allBulkItems.value = allItems // 🔥 Keep full list in memory

        _filteredBulkItems.value = when (fromType.lowercase()) {
            "counter" -> allItems.filter { it.counterName.equals(selectedValue, true) }
            "branch" -> allItems.filter { it.branchName.equals(selectedValue, true) }
            "box" -> allItems.filter { it.boxName.equals(selectedValue, true) }
            "packet" -> allItems.filter { it.packetName.equals(selectedValue, true) }
            "display" -> allItems.filter { it.counterId == 0 || it.counterName.isNullOrEmpty() }
            else -> allItems
        }
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
    fun submitStockTransfer(
        clientCode: String,
        stockIds: List<Int>,
        transferTypeId: Int,
        transferByEmployee: String,
        fromId: Int,
        toId: Int,
        onResult: (Boolean) -> Unit
    ) = viewModelScope.launch {
        try {
            val request = StockTransferRequest(
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
            )

            val result = repository.submitStockTransfer(request)
            _transferStatus.value = result.map { "Transfer successful" }
            onResult(result.isSuccess)
        } catch (e: Exception) {
            _transferStatus.value = Result.failure(e)
            onResult(false)
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


    fun getLabelledStockByTransferId(
        clientCode: String,
        mainObjectId: Int, // 👈 renamed for clarity
        requestType: String,
        userId: Int,
        branchId: Int
    ) {
        viewModelScope.launch {
            try {
                val request = StockInOutRequest(
                    ClientCode = clientCode,
                    StockType = "labelled",
                    TransferType = 0,
                    BranchId = branchId,
                    UserID = userId,
                    RequestType = requestType
                )

                val result = repository.getAllStockTransfers(request)
                val responseList = result.getOrNull() ?: emptyList()

                // ✅ match by main object Id, not TransferTypeId
                val matchedTransfer = responseList.firstOrNull { it.Id == mainObjectId }

                if (matchedTransfer != null) {
                    Log.d("DEBUG_LABELLED", "Matched transfer found: Id=${matchedTransfer.Id}")
                   // _stockTransferDetail.postValue(matchedTransfer)
                    _labelledStockItems.postValue(matchedTransfer.LabelledStockItems ?: emptyList())
                } else {
                    Log.d("DEBUG_LABELLED", "No transfer found for Id=$mainObjectId")
                    _stockTransferDetail.postValue(null)
                    _labelledStockItems.postValue(emptyList())
                }
            } catch (e: Exception) {
                Log.e("DEBUG_LABELLED", "Error fetching transfer details", e)
                _stockTransferDetail.postValue(null)
                _labelledStockItems.postValue(emptyList())
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


