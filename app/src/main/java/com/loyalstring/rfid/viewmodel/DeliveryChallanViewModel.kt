package com.loyalstring.rfid.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loyalstring.rfid.data.model.deliveryChallan.AddDeliveryChallanRequest
import com.loyalstring.rfid.data.model.deliveryChallan.AddDeliveryChallanResponse
import com.loyalstring.rfid.data.model.deliveryChallan.ChallanNoRequest
import com.loyalstring.rfid.data.model.deliveryChallan.CustomerTunchRequest
import com.loyalstring.rfid.data.model.deliveryChallan.CustomerTunchResponse
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanRequestList
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanResponseList
import com.loyalstring.rfid.data.model.deliveryChallan.UpdateDeliveryChallanRequest
import com.loyalstring.rfid.repository.DeliveryChallanRepository

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeliveryChallanViewModel @Inject constructor(
    private val repository: DeliveryChallanRepository
) : ViewModel() {

    private val _challanList = MutableStateFlow<List<DeliveryChallanResponseList>>(emptyList())
    val challanList: StateFlow<List<DeliveryChallanResponseList>> = _challanList.asStateFlow()

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


    fun fetchAllChallans(clientCode: String, branchId: Any) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                val request = DeliveryChallanRequestList(clientCode, branchId as Int)
                val response = repository.getAllDeliveryChallans(request)
                if (response.isSuccessful && response.body() != null) {
                    _challanList.value = response.body()!!
                } else {
                    _error.value = response.message()
                }
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
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

    fun fetchCustomerTunch(clientCode: String, employeeId: Int) {
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



}