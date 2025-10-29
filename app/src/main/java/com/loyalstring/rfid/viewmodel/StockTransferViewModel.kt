package com.loyalstring.rfid.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.stockTransfer.LabelledStockItems
import com.loyalstring.rfid.data.model.stockTransfer.STApproveRejectRequest
import com.loyalstring.rfid.data.model.stockTransfer.STApproveRejectResponse
import com.loyalstring.rfid.data.model.stockTransfer.StockInOutRequest
import com.loyalstring.rfid.data.model.stockTransfer.StockTransferInOutResponse
import com.loyalstring.rfid.data.remote.data.StockTransferItem
import com.loyalstring.rfid.data.remote.data.StockTransferRequest
import com.loyalstring.rfid.repository.BulkRepositoryImpl
import com.loyalstring.rfid.repository.TransferRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class StockTransferViewModel @Inject constructor(
    private val repository: TransferRepository,
    private val bulkRepository: BulkRepositoryImpl
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

    // Store all API responses with labelled items
    var allStockTransferResponseList: List<StockTransferInOutResponse> = emptyList()
        private set


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

    /** -------------------- Filter Local Bulk Items -------------------- **/
    fun filterBulkItemsByFrom(fromType: String, selectedValue: String) = viewModelScope.launch {
        val allItems = bulkRepository.getAllBulkItems().first()
        _filteredBulkItems.value = when (fromType.lowercase()) {
            "counter" -> allItems.filter { it.counterName.equals(selectedValue, true) }
            "branch" -> allItems.filter { it.branchName.equals(selectedValue, true) }
            "box" -> allItems.filter { it.boxName.equals(selectedValue, true) }
            "packet" -> allItems.filter { it.packetName.equals(selectedValue, true) }
            "display" -> allItems.filter { it.counterId == 0 || it.counterName.isNullOrEmpty() }
            else -> allItems
        }
    }

    /** -------------------- ID Fetch Helpers -------------------- **/
    suspend fun getEntityIdByName(type: String, name: String): Int {
        return when (type.lowercase()) {
            "counter" -> bulkRepository.getCounterIdFromName(name)
            "branch" -> bulkRepository.getBranchIdFromName(name)
            "box" -> bulkRepository.getBoxIdFromName(name)
            "packet" -> bulkRepository.getBoxIdFromName(name)
            else -> null
        } ?: 0
    }

    /** -------------------- Submit Stock Transfer -------------------- **/
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
    fun stApproveReject(request: STApproveRejectRequest) {
        viewModelScope.launch {
            val result = repository.stApproveReject(request)
            result.onFailure {
                _errorMessage.postValue(it.localizedMessage ?: "Failed to process stock transfer approval")
            }
            _stApproveRejectResponse.postValue(result)
        }
    }
    fun clearApproveResult() {
        _stApproveRejectResponse.postValue(null)
    }

    fun getLabelledStockByTransferId(
        clientCode: String,
        transferId: Int,
        requestType: String,
        userId: Int,
        branchId: Int,
        onResult: (Result<List<LabelledStockItems>>) -> Unit
    ) {
        // Reuse the same API call you already have for fetching all stock transfers
        val request = StockInOutRequest(
            ClientCode = clientCode,
            StockType = "labelled",
            TransferType = 0,
            BranchId = branchId,
            UserID = userId,
            RequestType = requestType
        )

        getAllStockTransfers(request) { result ->
            result.onSuccess { responseList ->
                // ✅ Find the correct transfer by Id and return its LabelledStockItems
                val matched = responseList
                    .firstOrNull { it.Id == transferId }
                    ?.LabelledStockItems ?: emptyList()
                onResult(Result.success(matched))
            }.onFailure { error ->
                onResult(Result.failure(error))
            }
        }
    }

}


