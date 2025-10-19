package com.loyalstring.rfid.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.remote.api.RetrofitInterface
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
    private val bulkRepository: BulkRepositoryImpl,
    private val apiService: RetrofitInterface
) : ViewModel() {

    val transferTypes = repository.transferTypes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTransferType = MutableStateFlow<String?>(null)
    val selectedTransferType = _selectedTransferType.asStateFlow()

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

    fun fetchCounterNames() {
        viewModelScope.launch {
            _counterNames.value = bulkRepository.getDistinctCounterNames()
        }
    }

    fun fetchBranchNames() {
        viewModelScope.launch {
            _branchNames.value = bulkRepository.getDistinctBranchNames()
        }
    }

    fun fetchBoxNames() {
        viewModelScope.launch {
            _boxNames.value = bulkRepository.getDistinctBoxNames()
        }
    }

    fun extractFromAndToOptions(transferType: String) {
        val parts = transferType.split(" to ", ignoreCase = true)
        if (parts.size == 2) {
            val from = parts[0].trim().lowercase()
            val to = parts[1].trim().lowercase()

            currentFrom.value = from
            currentTo.value = to

            viewModelScope.launch {
                val fromList = when (from) {
                    "box" -> bulkRepository.getDistinctBoxNames()
                    "branch" -> bulkRepository.getDistinctBranchNames()
                    "counter" -> bulkRepository.getDistinctCounterNames()
                    "counter" -> bulkRepository.getDistinctCounterNames()
                    else -> emptyList()
                }
                _fromOptions.value = fromList

                val toList = when (to) {
                    "box" -> bulkRepository.getDistinctBoxNames()
                    "branch" -> bulkRepository.getDistinctBranchNames()
                    "counter" -> bulkRepository.getDistinctCounterNames()
                    else -> emptyList()
                }
                _toOptions.value = toList
            }
        }
    }

    fun filterBulkItemsByFrom(fromType: String, selectedValue: String) {
        viewModelScope.launch {
            val allItems = bulkRepository.getAllBulkItems().first()
            val filtered = when (fromType.lowercase()) {
                "counter" -> allItems.filter {
                    it.counterName.equals(
                        selectedValue,
                        ignoreCase = true
                    )
                }

                "branch" -> allItems.filter {
                    it.branchName.equals(
                        selectedValue,
                        ignoreCase = true
                    )
                }

                "box" -> allItems.filter {
                    it.boxName.equals(
                        selectedValue,
                        ignoreCase = true
                    )
                }
                "packet" -> allItems.filter {
                    it.packetName.equals(
                        selectedValue,
                        ignoreCase = true
                    )
                }

                "display" -> allItems.filter { (it.counterId == 0 || it.counterName.isNullOrEmpty()) }
                else -> allItems
            }
            _filteredBulkItems.value = filtered
        }
    }

    suspend fun getEntityIdByName(type: String, name: String): Int {
        return when (type.lowercase()) {
            "counter" -> bulkRepository.getCounterIdFromName(name)
            "branch" -> bulkRepository.getBranchIdFromName(name)
            "box" -> bulkRepository.getBoxIdFromName(name)
            "packet" -> bulkRepository.getBoxIdFromName(name)
            else -> null
        } ?: 0
    }

    fun submitStockTransfer(
        clientCode: String,
        stockIds: List<Int>,
        transferTypeId: Int,
        transferByEmployee: String,
        fromId: Int,
        toId: Int,
        onResult: (Boolean) -> Unit // 🔹 <-- Add this callback
    ) {
        viewModelScope.launch {
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

                val response = apiService.postStockTransfer(request)

                if (response.isSuccessful) {
                    _transferStatus.value = Result.success("Transfer successful")
                    onResult(true)  // ✅ Notify success
                } else {
                    _transferStatus.value =
                        Result.failure(Exception("Transfer failed: ${response.code()}"))
                    onResult(false) // ❌ Notify failure
                }
            } catch (e: Exception) {
                _transferStatus.value = Result.failure(e)
                onResult(false) // ❌ Notify failure
            }
        }
    }

}
