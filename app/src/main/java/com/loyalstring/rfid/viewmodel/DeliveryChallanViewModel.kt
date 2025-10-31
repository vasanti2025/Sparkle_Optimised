package com.loyalstring.rfid.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanRequestList
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanResponseList
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
}