package com.loyalstring.rfid.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.setting.UpdateDailyRatesReq
import com.loyalstring.rfid.data.model.setting.UpdateDailyRatesResponse
import com.loyalstring.rfid.data.remote.data.DailyRateResponse
import com.loyalstring.rfid.repository.OrderRepository
import com.loyalstring.rfid.repository.SettingRepository
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.loyalstring.rfid.data.local.db.AppDatabase
import com.loyalstring.rfid.data.model.setting.LocationGetRequest
import com.loyalstring.rfid.data.model.setting.LocationGetResponse
import com.loyalstring.rfid.data.model.setting.LocationItem
import com.loyalstring.rfid.data.model.setting.LocationSyncResponse
import com.loyalstring.rfid.repository.SettingRepositoryImpl
import com.loyalstring.rfid.ui.utils.ToastUtils
import com.loyalstring.rfid.ui.utils.UserPreferences
import dagger.hilt.android.internal.Contexts.getApplication
import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.bouncycastle.oer.its.EndEntityType.app
import javax.inject.Inject

// ✅ Use sealed class + rename Error -> Failure to avoid KAPT stub error
sealed class UiState1<out T> {
    object Idle : UiState1<Nothing>()
    object Loading : UiState1<Nothing>()
    data class Success<T>(val data: T) : UiState1<T>()
    data class Failure(val message: String, val code: Int? = null) : UiState1<Nothing>()
}
@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val userPreferences: UserPreferences,
    private val orderRepository: OrderRepository,
    private val settingRepository: SettingRepositoryImpl
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "SettingsViewModel"
    }

    private val _getAllDailyRate = MutableStateFlow<List<DailyRateResponse>>(emptyList())
    val getAllDailyRate: StateFlow<List<DailyRateResponse>> = _getAllDailyRate

    private val _updateDailyRatesState =
        MutableStateFlow<UiState1<List<UpdateDailyRatesResponse>>>(UiState1.Idle)
    val updateDailyRatesState: StateFlow<UiState1<List<UpdateDailyRatesResponse>>> =
        _updateDailyRatesState.asStateFlow()

    var sheetUrl by mutableStateOf(userPreferences.getSheetUrl().orEmpty())
        private set

    private val _locationResponse = MutableStateFlow<List<LocationItem>>(emptyList())
    val locationResponse: StateFlow<List<LocationItem>> = _locationResponse
    private val _locationError = MutableStateFlow<String?>(null)
    val locationError: StateFlow<String?> = _locationError

    private val _localLocations = MutableStateFlow<List<LocationItem>>(emptyList())
    val localLocations: StateFlow<List<LocationItem>> = _localLocations

    fun updateSheetUrl(newUrl: String) {
        sheetUrl = newUrl
        userPreferences.saveSheetUrl(newUrl)
        Log.d(TAG, "SHEET URL: ${userPreferences.getSheetUrl()}")
    }

    fun getDailyRate(request: ClientCodeRequest) {
        viewModelScope.launch {
            try {
                val response: Response<List<DailyRateResponse>> =
                    withContext(Dispatchers.IO) { orderRepository.dailyRate(request) }

                if (response.isSuccessful) {
                    _getAllDailyRate.value = response.body().orEmpty()
                    Log.d(TAG, "DailyRate loaded: ${_getAllDailyRate.value.size} rows")
                } else {
                    _getAllDailyRate.value = emptyList()
                    val err =
                        withContext(Dispatchers.IO) { response.errorBody()?.string() }.orEmpty()
                    Log.e(TAG, "DailyRate error ${response.code()}: $err")
                }
            } catch (e: Exception) {
                _getAllDailyRate.value = emptyList()
                Log.e(TAG, "DailyRate exception: ${e.message}", e)
            }
        }
    }

    fun updateDailyRates(req: List<UpdateDailyRatesReq>) {
        viewModelScope.launch {
            _updateDailyRatesState.value = UiState1.Loading
            try {
                val res: Response<List<UpdateDailyRatesResponse>> =
                    withContext(Dispatchers.IO) { settingRepository.updateDailyRates(req) }

                if (res.isSuccessful) {
                    _updateDailyRatesState.value = UiState1.Success(res.body().orEmpty())
                    Log.d(TAG, "updateDailyRates success: ${res.body()?.size ?: 0} records")
                } else {
                    val msg = withContext(Dispatchers.IO) { res.errorBody()?.string() }.orEmpty()
                    _updateDailyRatesState.value = UiState1.Failure(
                        message = if (msg.isNotBlank()) msg else "Request failed",
                        code = res.code()
                    )
                    Log.e(TAG, "updateDailyRates error ${res.code()}: $msg")
                }
            } catch (e: Exception) {
                _updateDailyRatesState.value = UiState1.Failure(e.message ?: "Something went wrong")
                Log.e(TAG, "updateDailyRates exception: ${e.message}", e)
            }
        }
    }

    fun resetUpdateState() {
        _updateDailyRatesState.value = UiState1.Idle
    }

    fun clearAllData(context: Context, navController: NavHostController) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Clear Room Database
                AppDatabase.getDatabase(context).clearAllTables()

                // 2. Clear SharedPreferences
                context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    .edit { clear() }

                // 3. Clear cache
                context.cacheDir.deleteRecursively()

                withContext(Dispatchers.Main) {
                    ToastUtils.showToast(context, "All data cleared. Please login again.")

                    // 4. Navigate to Login Screen
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true } // ✅ clears entire backstack
                        launchSingleTop = true          // prevent multiple copies
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    ToastUtils.showToast(context, "Error clearing data: ${e.message}")
                }
            }
        }
    }

    fun fetchLocation(locationRequest: LocationGetRequest) {
        viewModelScope.launch {
            try {
                delay(1000)
                val response = settingRepository.getLocation(locationRequest)
                if (response.isSuccessful) {
                    // Flatten and remove nulls
                    _locationResponse.value = response.body().orEmpty()
                    _locationError.value = null
                    Log.d("@@","@@"+ _locationResponse.value)
                } else {
                    _locationError.value =
                        "Error ${response.code()}: ${response.errorBody()?.string().orEmpty()}"
                    _locationResponse.value = emptyList()
                }
            } catch (e: Exception) {
                _locationError.value = e.message
                _locationResponse.value = emptyList()
            }
        }
    }

    fun fetchLocationsFromDb() {
        viewModelScope.launch {
            val db = AppDatabase.getDatabase(getApplication()) // make sure ViewModel has Application context if needed
            val list = db.locationDao().getAll() // DAO function to get all locations
            _localLocations.value = list
        }
    }


}




