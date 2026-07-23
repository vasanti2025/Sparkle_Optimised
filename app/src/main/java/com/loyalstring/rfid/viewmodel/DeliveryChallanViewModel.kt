package com.loyalstring.rfid.viewmodel
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loyalstring.rfid.data.model.deliveryChallan.AddDeliveryChallanRequest
import com.loyalstring.rfid.data.model.deliveryChallan.AddDeliveryChallanResponse
import com.loyalstring.rfid.data.model.deliveryChallan.ChallanNoRequest
import com.loyalstring.rfid.data.model.deliveryChallan.CustomerTunchRequest
import com.loyalstring.rfid.data.model.deliveryChallan.CustomerTunchResponse
import com.loyalstring.rfid.data.model.deliveryChallan.DeleteDeliveryChallanRequest
import com.loyalstring.rfid.data.model.deliveryChallan.DeleteDeliveryChallanResponse
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanItemPrint
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanPrintData
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanListRow
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanRequestList
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanResponseList
import com.loyalstring.rfid.data.model.deliveryChallan.UpdateDeliveryChallanRequest
import com.loyalstring.rfid.data.model.deliveryChallan.toListRow
import com.loyalstring.rfid.repository.DeliveryChallanRepository
import com.loyalstring.rfid.ui.screens.generateDeliveryChallanPdf
import com.loyalstring.rfid.ui.screens.openPdfPreview
import com.loyalstring.rfid.ui.utils.DeliveryChallanListCache

import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@HiltViewModel
class DeliveryChallanViewModel @Inject constructor(
    private val repository: DeliveryChallanRepository,
    @ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _challanList = MutableStateFlow<List<DeliveryChallanListRow>>(emptyList())
    val challanList: StateFlow<List<DeliveryChallanListRow>> = _challanList.asStateFlow()

    private val fullChallanById = ConcurrentHashMap<Int, DeliveryChallanResponseList>()
    private val _fullDataVersion = MutableStateFlow(0)
    val fullDataVersion: StateFlow<Int> = _fullDataVersion.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _lastChallanNo = MutableStateFlow<Int?>(null)
    val lastChallanNo: StateFlow<Int?> = _lastChallanNo

    private val _addChallanResponse = MutableStateFlow<AddDeliveryChallanResponse?>(null)
    val addChallanResponse: StateFlow<AddDeliveryChallanResponse?> = _addChallanResponse

    private val _updateChallanResponse = MutableStateFlow<AddDeliveryChallanResponse?>(null)
    val updateChallanResponse: StateFlow<AddDeliveryChallanResponse?> = _updateChallanResponse

    private val _customerTunchList = MutableStateFlow<List<CustomerTunchResponse>>(emptyList())
    val customerTunchList: StateFlow<List<CustomerTunchResponse>> = _customerTunchList
    private val _selectedChallan = MutableStateFlow<DeliveryChallanResponseList?>(null)
    val selectedChallan: StateFlow<DeliveryChallanResponseList?> = _selectedChallan

    fun setSelectedChallan(challan: DeliveryChallanResponseList?) {
        _selectedChallan.value = challan
    }


    fun getFullChallan(id: Int): DeliveryChallanResponseList? = fullChallanById[id]

    fun seedRowsIfEmpty(rows: List<DeliveryChallanListRow>) {
        if (_challanList.value.isEmpty() && rows.isNotEmpty()) {
            _challanList.value = rows.sortedByDescending { it.Id }
            _loading.value = false
        }
    }

    private fun indexFullChallans(list: List<DeliveryChallanResponseList>) {
        fullChallanById.clear()
        list.forEach { fullChallanById[it.Id] = it }
        _fullDataVersion.value++
    }

    private fun toSortedRows(list: List<DeliveryChallanResponseList>): List<DeliveryChallanListRow> =
        list.map { it.toListRow() }.sortedByDescending { it.Id }

    private fun rowsChanged(
        current: List<DeliveryChallanListRow>,
        updated: List<DeliveryChallanListRow>,
    ): Boolean {
        if (current.size != updated.size) return true
        return current.indices.any { current[it] != updated[it] }
    }

    fun fetchAllChallans(clientCode: String, branchId: Any) {
        val branch = branchId as Int

        DeliveryChallanListCache.readMemoryRows(clientCode, branch)?.let { memRows ->
            if (_challanList.value.isEmpty()) {
                _challanList.value = memRows
                _loading.value = false
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            if (_challanList.value.isEmpty()) {
                val cachedRows = DeliveryChallanListCache.loadRows(appContext, clientCode, branch)
                if (cachedRows.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        _challanList.value = cachedRows.sortedByDescending { it.Id }
                        _loading.value = false
                    }
                    launch {
                        indexFullChallans(
                            DeliveryChallanListCache.loadFull(appContext, clientCode, branch)
                        )
                    }
                } else {
                    withContext(Dispatchers.Main) { _loading.value = true }
                }
            }

            withContext(Dispatchers.Main) { _error.value = null }
            try {
                val request = DeliveryChallanRequestList(clientCode, branch)
                val response = repository.getAllDeliveryChallans(request)
                if (response.isSuccessful && response.body() != null) {
                    val fullList = response.body()!!
                    indexFullChallans(fullList)
                    val rows = toSortedRows(fullList)
                    withContext(Dispatchers.Main) {
                        if (_challanList.value.isEmpty() || rowsChanged(_challanList.value, rows)) {
                            _challanList.value = rows
                        }
                    }
                    DeliveryChallanListCache.saveRows(appContext, clientCode, branch, rows)
                    launch { DeliveryChallanListCache.saveFull(appContext, clientCode, branch, fullList) }
                } else if (_challanList.value.isEmpty()) {
                    withContext(Dispatchers.Main) { _error.value = response.message() }
                }
            } catch (e: Exception) {
                if (_challanList.value.isEmpty()) {
                    withContext(Dispatchers.Main) { _error.value = e.message }
                }
            } finally {
                withContext(Dispatchers.Main) {
                    if (_challanList.value.isEmpty()) {
                        _loading.value = false
                    }
                }
            }
        }
    }

    fun clearlastDeliveryChallanNo() {
        _lastChallanNo.value = null
    }

    /** ✅ Fetch last challan number */
    fun fetchLastChallanNo(clientCode: String, branchId: Any) {
        viewModelScope.launch {
            try {
                val request = ChallanNoRequest(clientCode, branchId as Int)
                val response = repository.getLastChallanNo(request)
                if (response.isSuccessful) {
                    _lastChallanNo.value = response.body()?.LastChallanNo
                } else {
                    _lastChallanNo.value = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _lastChallanNo.value = null
            }
        }
    }


    /** ✅ Add Delivery Challan */
    fun addDeliveryChallan(request: AddDeliveryChallanRequest) {
        viewModelScope.launch {
            try {
                _loading.value = true
                val response = repository.addDeliveryChallan(request)
                if (response.isSuccessful) {
                    _addChallanResponse.value = response.body()
                } else {
                    _error.value = "Failed: ${response.message()}"
                    _addChallanResponse.value = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = e.localizedMessage ?: "Unknown error"
                _addChallanResponse.value = null
            } finally {
                _loading.value = false
            }
        }
    }

    /** ✅ Update Delivery Challan */
    fun updateDeliveryChallan(request: UpdateDeliveryChallanRequest) {
        viewModelScope.launch {
            try {
                _loading.value = true
                val response = repository.updateDeliveryChallan(request)
                if (response.isSuccessful) {
                    _updateChallanResponse.value = response.body()
                    _error.value = null
                } else {
                    _error.value = "Failed: ${response.message()}"
                    _updateChallanResponse.value = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = e.localizedMessage ?: "Unknown error"
                _updateChallanResponse.value = null
            } finally {
                _loading.value = false
            }
        }
    }

    fun fetchCustomerTunch(clientCode: String, employeeId: Int?) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                val request = CustomerTunchRequest(clientCode)
                val response = repository.getAllCustomerTunch(request)

                if (response.isSuccessful && response.body() != null) {
                    _customerTunchList.value = response.body()!!
                } else {
                    _error.value = "Failed: ${response.message()}"
                    _customerTunchList.value = emptyList()
                }

            } catch (e: Exception) {
                _error.value = e.localizedMessage ?: "Something went wrong"
                _customerTunchList.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }

    fun deleteDeliveryChallan(clientCode: String, id: Int, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = DeleteDeliveryChallanRequest(ClientCode = clientCode, Id = id)
                val response = repository.deleteDeliveryChallan(request)
                if (response.isSuccessful) {
                    val branchId = fullChallanById[id]?.BranchId
                        ?: _challanList.value.firstOrNull { it.Id == id }?.BranchId
                        ?: 0
                    fullChallanById.remove(id)
                    val remainingRows = _challanList.value.filterNot { it.Id == id }
                    val remainingFull = fullChallanById.values.toList()
                    withContext(Dispatchers.Main) { _challanList.value = remainingRows }
                    DeliveryChallanListCache.saveRows(appContext, clientCode, branchId, remainingRows)
                    DeliveryChallanListCache.saveFull(appContext, clientCode, branchId, remainingFull)
                    withContext(Dispatchers.Main) { onResult(true) }
                } else {
                    withContext(Dispatchers.Main) {
                        _error.value = "Failed: ${response.message()}"
                        onResult(false)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _error.value = e.localizedMessage ?: "Unknown error"
                    onResult(false)
                }
            }
        }
    }

  /*  fun printDeliveryChallan(
        context: Context,
        challan: DeliveryChallanResponseList
    ) {
        viewModelScope.launch {
            try {
                // ✅ If list item already has full details, use it
                // ✅ Otherwise fetch full challan from API inside getChallanForPrint(...)
                val printData = getChallanForPrint(
                    clientCode = challan.ClientCode ?: "",
                    challanId = challan.Id ?: 0
                )

                val uri = generateDeliveryChallanPdf(context, printData)
                if (uri != null) openPdfPreview(context, uri)

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Print failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ✅ You already mentioned you have this kind of function
    suspend fun getChallanForPrint(clientCode: String, challanId: Int): DeliveryChallanPrintData {
        // call API + map to DeliveryChallanPrintData
        // return printData
        TODO("Implement")
    }*/



    fun clearLastChallanNo() { _lastChallanNo.value = null }
    fun clearAddChallanResponse() { _addChallanResponse.value = null }
    fun clearUpdateChallanResponse() { _updateChallanResponse.value = null }
    fun clearError() { _error.value = null }


}






