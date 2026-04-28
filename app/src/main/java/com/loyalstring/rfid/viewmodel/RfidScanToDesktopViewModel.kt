package com.loyalstring.rfid.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.model.login.LoginRequest
import com.loyalstring.rfid.data.remote.data.DeviceIdData
import com.loyalstring.rfid.data.remote.data.DeviceIdResponseData
import com.loyalstring.rfid.data.remote.data.RfidItem
import com.loyalstring.rfid.repository.LoginRepository
import com.loyalstring.rfid.repository.RfidScanDesktopRepository
import com.loyalstring.rfid.ui.screens.shortSerial
import com.loyalstring.rfid.ui.utils.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class RfidScanToDesktopViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: RfidScanDesktopRepository,
    private val loginrepository: LoginRepository
) : ViewModel() {
    val employee = UserPreferences.getInstance(context).getEmployee(Employee::class.java)

    private val _rfidList = MutableStateFlow<List<RfidItem>>(emptyList())
    val rfidList: StateFlow<List<RfidItem>> = _rfidList.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _finalDeviceId = MutableStateFlow<String?>(null)
    val finalDeviceId: StateFlow<String?> = _finalDeviceId.asStateFlow()

    private val _addDeviceIdState =
        MutableStateFlow<Result<DeviceIdResponseData>?>(null)
    val addDeviceIdState: StateFlow<Result<DeviceIdResponseData>?> =
        _addDeviceIdState.asStateFlow()

    private val _getDeviceIdState =
        MutableStateFlow<Result<DeviceIdData>?>(null)
    val getDeviceIdState: StateFlow<Result<DeviceIdData>?> =
        _getDeviceIdState.asStateFlow()


    fun getAllScantoDesktop(clientCode: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val result = repository.getAllScantoDesktop(
                ClientCodeRequest(clientCode)
            )

            result.onSuccess { response ->
                _rfidList.value = response.data ?: emptyList()
            }.onFailure { error ->
                _errorMessage.value = error.message
            }

            _isLoading.value = false
        }
    }


    fun setupDeviceId(
        clientCode: String,
        androidId: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val getResponse = loginrepository.login(
                    LoginRequest(employee?.username.toString(),employee?.password.toString())
                )

                if (getResponse.isSuccessful && getResponse.body() != null) {
                    val dbData = getResponse.body()

                    val dbDeviceId = dbData?.employee?.DeviceId
                        ?.trim()
                        .orEmpty()

                    if (dbDeviceId.isNotBlank()) {
                        _finalDeviceId.value = dbDeviceId

                        val deviceData = DeviceIdData(
                            id = dbData!!.employee!!.id.toString(),
                            deviceId = dbDeviceId
                        )
                        _getDeviceIdState.value = Result.success(deviceData)
                    } else {
                        createAndSaveDeviceId(clientCode.toString(), androidId)
                    }

                } else {
                    createAndSaveDeviceId(clientCode.toString(), androidId)
                }

            } catch (e: Exception) {
                _errorMessage.value = e.message
                createAndSaveDeviceId(clientCode.toString(), androidId)
            }

            _isLoading.value = false
        }
    }


    private suspend fun createAndSaveDeviceId(
        clientCode: String,
        androidId: String
    ) {
        val newDeviceId = shortSerial(androidId)

        val request = listOf(
            DeviceIdData(
                id = clientCode,
                deviceId = newDeviceId
            )
        )

        val addResponse = repository.addDeviceId(request)

        if (addResponse.isSuccessful && addResponse.body() != null) {
            _finalDeviceId.value = newDeviceId
            _addDeviceIdState.value = Result.success(addResponse.body()!!)
        } else {
            _addDeviceIdState.value = Result.failure(
                Exception(addResponse.message().ifEmpty { "Device Id save failed" })
            )
            _errorMessage.value = addResponse.message().ifEmpty { "Device Id save failed" }
        }
    }

/*
    fun addDeviceId(request: DeviceIdData) {
        viewModelScope.launch {
            try {
                val response = repository.addDeviceId(request)

                if (response.isSuccessful && response.body() != null) {
                    _addDeviceIdState.value = Result.success(response.body()!!)
                } else {
                    _addDeviceIdState.value = Result.failure(
                        Exception(response.message().ifEmpty { "Something went wrong" })
                    )
                }

            } catch (e: Exception) {
                _addDeviceIdState.value = Result.failure(e)
            }
        }
    }*/





    fun clearAddDeviceIdState() {
        _addDeviceIdState.value = null
    }

    fun clearGetDeviceIdState() {
        _getDeviceIdState.value = null
    }
}