package com.loyalstring.rfid.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loyalstring.rfid.data.model.sampleIn.SampleInResponse
import com.loyalstring.rfid.data.model.sampleOut.SampleOutListRequest
import com.loyalstring.rfid.repository.SampleInRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SampleInViewModel @Inject constructor(
    private val repository: SampleInRepository
) : ViewModel() {

    // Sirf list expose kar rahe hain
    private val _sampleInList = MutableStateFlow<List<SampleInResponse>>(emptyList())
    val sampleInList: StateFlow<List<SampleInResponse>> = _sampleInList.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadSampleIn(
        clientCode: String,
        sampleStatus: String
    ) {
        viewModelScope.launch {
            val hasCached = _sampleInList.value.isNotEmpty()
            if (!hasCached) {
                _loading.value = true
            }

            try {
                val request = SampleOutListRequest(
                    ClientCode = clientCode,
                    SampleStatus = sampleStatus
                )

                val result = repository.getAllSampleIn(request)

                result.onSuccess { list ->
                    _sampleInList.value = list
                    _error.value = null
                }

                result.onFailure { e ->
                    if (!hasCached) {
                        _sampleInList.value = emptyList()
                    }
                    _error.value = e.message ?: "Something went wrong"
                }

            } catch (e: Exception) {
                if (!hasCached) {
                    _sampleInList.value = emptyList()
                }
                _error.value = e.message ?: "Unexpected error"

            } finally {
                _loading.value = false
            }
        }
    }
}