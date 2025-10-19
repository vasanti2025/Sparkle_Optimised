package com.loyalstring.rfid.repository

import com.loyalstring.rfid.data.model.setting.LocationGetRequest
import com.loyalstring.rfid.data.model.setting.LocationGetResponse
import com.loyalstring.rfid.data.model.setting.LocationItem
import com.loyalstring.rfid.data.model.setting.LocationSyncRequest
import com.loyalstring.rfid.data.model.setting.LocationSyncResponse
import com.loyalstring.rfid.data.model.setting.UpdateDailyRatesReq
import com.loyalstring.rfid.data.model.setting.UpdateDailyRatesResponse
import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of SettingRepository that calls the backend APIs.
 */
@Singleton
class SettingRepositoryImpl @Inject constructor(
    private val apiService: RetrofitInterface
) : SettingRepository {

    override suspend fun updateDailyRates(updateDailyRatesReq: List<UpdateDailyRatesReq>): Response<List<UpdateDailyRatesResponse>> {
        return apiService.updateDailyRate(updateDailyRatesReq) // actual API call
    }

    override suspend fun addLocation(locationSyncRequest: LocationSyncRequest): Response<LocationSyncResponse> {
        return apiService.addLocation(locationSyncRequest) // actual API call
    }

    override suspend fun getLocation(locationGetReq: LocationGetRequest): Response<List<LocationItem>> {
        return apiService.getLocation(locationGetReq) // actual API call
    }
}
