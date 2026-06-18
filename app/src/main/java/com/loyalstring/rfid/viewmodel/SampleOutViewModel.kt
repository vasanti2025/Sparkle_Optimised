package com.loyalstring.rfid.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanResponseList
import com.loyalstring.rfid.data.model.sampleOut.SampleOutAddRequest
import com.loyalstring.rfid.data.model.sampleOut.SampleOutAddResponse
import com.loyalstring.rfid.data.model.sampleOut.SampleOutLastNoReq
import com.loyalstring.rfid.data.model.sampleOut.SampleOutListRequest
import com.loyalstring.rfid.data.model.sampleOut.SampleOutListResponse
import com.loyalstring.rfid.data.model.sampleOut.SampleOutUpdateRequest
import com.loyalstring.rfid.repository.SampleOutRepositoty
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SampleOutViewModel @Inject constructor(
    private val repository: SampleOutRepositoty
) : ViewModel() {

    // Sirf list expose kar rahe hain
    private val _sampleOutList = MutableStateFlow<List<SampleOutListResponse>>(emptyList())
    val sampleOutList: StateFlow<List<SampleOutListResponse>> = _sampleOutList.asStateFlow()

    // ---------- ADD STATE ----------
    private val _addResult = MutableStateFlow<SampleOutAddResponse?>(null)
    val addResult: StateFlow<SampleOutAddResponse?> = _addResult.asStateFlow()


    //-------------------UPDATE SAMPLEOUT----------------------//
    private val _updateResult = MutableStateFlow<SampleOutAddResponse?>(null)
    val updateResult: StateFlow<SampleOutAddResponse?> = _updateResult.asStateFlow()


    // ---------- LAST NO STATE ----------
    private val _lastSampleOutNo = MutableStateFlow<String?>(null)
    val lastSampleOutNo: StateFlow<String?> = _lastSampleOutNo.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _selectedChallan = MutableStateFlow<SampleOutListResponse?>(null)
    val selectedSampleOut: StateFlow<SampleOutListResponse?> = _selectedChallan

    fun setSelectedSampleOut(challan: SampleOutListResponse?) {
        _selectedChallan.value = challan
    }


    fun loadSampleOut(
        clientCode: String,
        sampleStatus: String
    ) {
        viewModelScope.launch {
            val hasCached = _sampleOutList.value.isNotEmpty()
            if (!hasCached) {
                _loading.value = true
            }

            try {
                val request = SampleOutListRequest(
                    ClientCode = clientCode,
                    SampleStatus = sampleStatus
                )

                val result = repository.getAllSampleOut(request)

                result.onSuccess { list ->
                    _sampleOutList.value = list
                    _error.value = null
                }

                result.onFailure { e ->
                    if (!hasCached) {
                        _sampleOutList.value = emptyList()
                    }
                    _error.value = e.message ?: "Something went wrong"
                }

            } catch (e: Exception) {
                if (!hasCached) {
                    _sampleOutList.value = emptyList()
                }
                _error.value = e.message ?: "Unexpected error"

            } finally {
                _loading.value = false
            }
        }
    }

    // 🔹 2. ADD SAMPLE OUT (save)
    // 🔹 2. ADD SAMPLE OUT (save)
    fun addSampleOut(request: SampleOutAddRequest) {
        viewModelScope.launch {
            _loading.value = true

            try {
                val response = repository.addSampleOut(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d("SampleOutVM", "✅ addSampleOut body = $body")

                    if (body != null) {
                        _addResult.value = body      // 🔹 single object
                        _error.value = null
                    } else {
                        _addResult.value = null
                        _error.value = "Empty response body"
                    }
                } else {
                    Log.e(
                        "SampleOutVM",
                        "❌ addSampleOut failed: ${response.code()} - ${response.message()}"
                    )
                    _addResult.value = null
                    _error.value = "Failed: ${response.code()} - ${response.message()}"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _addResult.value = null
                _error.value = e.localizedMessage ?: "Unexpected error while adding Sample Out"
            } finally {
                _loading.value = false
            }
        }
    }

//update sample out
    fun updateSampleOut(request: SampleOutUpdateRequest) {
        viewModelScope.launch {
            _loading.value = true

            try {
                val response = repository.updateSampleOut(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        _updateResult.value = body
                        _error.value = null
                        Log.d("SampleOutVM", "✅ SampleOut updated: $body")
                    } else {
                        _error.value = "Empty response body"
                        Log.e("SampleOutVM", "❌ Update SampleOut: empty body")
                    }
                } else {
                    _error.value = "Failed: ${response.code()} - ${response.message()}"
                    Log.e(
                        "SampleOutVM",
                        "❌ Update SampleOut failed: code=${response.code()} msg=${response.message()}"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = e.localizedMessage ?: "Unexpected error while updating Sample Out"
                Log.e("SampleOutVM", "💥 Exception in updateSampleOut", e)
            } finally {
                _loading.value = false
            }
        }
    }






    // 🔹 3. GET LAST SAMPLE OUT NO (e.g. "C14")
    fun fetchLastSampleOutNo(
        clientCode: String,
        branchId: Int
    ) {
        viewModelScope.launch {
            Log.d("SampleOutVM", "🚀 fetchLastSampleOutNo CALLED with clientCode=$clientCode, branchId=$branchId")

            _loading.value = true

            try {
                val req = SampleOutLastNoReq(
                    ClientCode = clientCode,
                    BranchId = branchId
                )

                Log.d("SampleOutVM", "📤 Request object = $req")

                val result = repository.lastSampleOutNo(req)

                Log.d(
                    "SampleOutVM",
                    "📥 Result from repository: isSuccess=${result.isSuccess}, isFailure=${result.isFailure}"
                )

                result
                    .onSuccess { lastNo ->
                        Log.d("SampleOutVM", "✅ lastSampleOutNo from API = $lastNo")
                        _lastSampleOutNo.value = lastNo   // e.g. "C14"
                        _error.value = null
                    }
                    .onFailure { e ->
                        Log.e("SampleOutVM", "❌ lastSampleOutNo failed", e)
                        _lastSampleOutNo.value = null
                        _error.value = e.message ?: "Failed to load last Sample Out No"
                    }

            } catch (e: Exception) {
                Log.e("SampleOutVM", "💥 Exception in fetchLastSampleOutNo", e)
                _lastSampleOutNo.value = null
                _error.value = e.message ?: "Unexpected error while fetching last Sample Out No"
            } finally {
                _loading.value = false
            }
        }
    }


    // SampleOutViewModel ke andar

    fun getNextSampleOutNo(lastNo: String?): String {
        if (lastNo.isNullOrBlank()) return "C1"

        val prefix = lastNo.takeWhile { !it.isDigit() }    // "C"
        val numberPart = lastNo.drop(prefix.length)        // "12"

        val current = numberPart.toIntOrNull() ?: 0
        val next = current + 1

        Log.d("@@","lstNo"+prefix + next.toString()   )

        return prefix + next.toString()                    // "C13"
    }


    /**
     * Ye function pura flow handle karega:
     * 1) lastSampleOutNo API call
     * 2) usme +1 karke next no banayega
     * 3) addSampleOut API call karega
     */


    fun clearAddResult() {
        _addResult.value = null
    }

    fun clearError() {
        _error.value = null
    }
    fun clearLastSampleOutNo() {
        _lastSampleOutNo.value = null
    }

    fun clearUpdateResult() {
        _updateResult.value = null
    }





}
