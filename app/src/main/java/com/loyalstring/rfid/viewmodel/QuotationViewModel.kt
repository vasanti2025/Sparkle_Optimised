package com.loyalstring.rfid.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.quotation.AddQuotationRequest
import com.loyalstring.rfid.data.model.quotation.LastQuotationNoResponse
import com.loyalstring.rfid.data.model.quotation.QuotationListRequest
import com.loyalstring.rfid.data.model.quotation.QuotationListResponse
import com.loyalstring.rfid.data.model.quotation.UpdateQuotationRequest
import com.loyalstring.rfid.data.model.quotation.UpdateQuotationResponse
import com.loyalstring.rfid.data.model.sampleOut.SampleOutListResponse
import com.loyalstring.rfid.repository.QuotationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class QuotationViewModel  @Inject constructor(
    private val repository: QuotationRepository
) : ViewModel() {

    private val _quotationList = MutableStateFlow<List<QuotationListResponse>>(emptyList())
    val quotationList: StateFlow<List<QuotationListResponse>> = _quotationList.asStateFlow()

    private val _addResult = MutableStateFlow<QuotationListResponse?>(null)
    val addResult = _addResult.asStateFlow()

    private val _lastQuotationNo = MutableStateFlow<LastQuotationNoResponse?>(null)
    val lastQuotationNo: StateFlow<LastQuotationNoResponse?> = _lastQuotationNo.asStateFlow()

    private val _updateResult = MutableStateFlow<UpdateQuotationResponse?>(null)
    val updateResult = _updateResult.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedQuotation = MutableStateFlow<QuotationListResponse?>(null)
    val selectedQuotation: StateFlow<QuotationListResponse?> = _selectedQuotation

    fun setSelectedQuotation(challan: QuotationListResponse?) {
        _selectedQuotation.value = challan
    }

    fun loadQuotationList(clientCode: String) {
        viewModelScope.launch {
            val hasCached = _quotationList.value.isNotEmpty()
            if (!hasCached) {
                _loading.value = true
            }

            try {
                val request = QuotationListRequest(ClientCode = clientCode)
                val result = repository.getAllQuotationList(request)

                result.onSuccess { list ->
                    _quotationList.value = list
                    _error.value = null
                }

                result.onFailure { e ->
                    if (!hasCached) {
                        _quotationList.value = emptyList()
                    }
                    _error.value = e.message ?: "Something went wrong"
                }
            } catch (e: Exception) {
                if (!hasCached) {
                    _quotationList.value = emptyList()
                }
                _error.value = e.message ?: "Unexpected error"
            } finally {
                _loading.value = false
            }
        }
    }



    fun saveQuotation(request: AddQuotationRequest) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val result = repository.saveQuotation(request) // Result<AddQuotationResponse>

                result
                    .onSuccess { body ->
                        _addResult.value = body
                        _error.value = null
                    }
                    .onFailure { e ->
                        _addResult.value = null
                        _error.value = e.message ?: "Something went wrong"
                    }

            } finally {
                _loading.value = false
            }
        }
    }

    fun clearLastQuotationNo() {
        _lastQuotationNo.value = null
    }

    fun clearAddResult() {
        _addResult.value = null
    }

    fun clearUpdateResult() {
        _updateResult.value = null
    }

    fun clearError() {
        _error.value = null
    }

    fun loadLastQuotationNo(clientCode: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val result = repository.getLastQuotationNo(ClientCodeRequest(clientCode))

                result
                    .onSuccess { body ->
                        _lastQuotationNo.value = body
                        _error.value = null
                    }
                    .onFailure { e ->
                        _lastQuotationNo.value = null
                        _error.value = e.message ?: "Failed to fetch last quotation no"
                    }

            } finally {
                _loading.value = false
            }
        }
    }

    fun updateQuotation(request: UpdateQuotationRequest) {
        viewModelScope.launch {
            _loading.value = true
            try {
                repository.updateQuotation(request)
                    .onSuccess { body ->
                        _updateResult.value = body
                        _error.value = null
                    }
                    .onFailure { e ->
                        _updateResult.value = null
                        _error.value = e.message ?: "Something went wrong"
                    }
            } finally {
                _loading.value = false
            }
        }
    }


}