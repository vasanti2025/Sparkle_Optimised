package com.loyalstring.rfid.repository

import com.loyalstring.rfid.data.model.setting.LocationGetRequest
import com.loyalstring.rfid.data.model.setting.LocationGetResponse
import com.loyalstring.rfid.data.model.setting.LocationItem
import com.loyalstring.rfid.data.model.setting.LocationSyncRequest
import com.loyalstring.rfid.data.model.setting.LocationSyncResponse
import com.loyalstring.rfid.data.model.setting.UpdateDailyRatesReq
import com.loyalstring.rfid.data.model.setting.UpdateDailyRatesResponse
import retrofit2.Response

/**
 * Defines the contract for Settings-related API operations.
 */
interface SettingRepository {

    suspend fun updateDailyRates(updateDailyRatesReq: List<UpdateDailyRatesReq>): Response<List<UpdateDailyRatesResponse>>
    suspend fun addLocation(locationSyncRequest: LocationSyncRequest): Response<LocationSyncResponse>
    suspend fun getLocation(locationGetRequest: LocationGetRequest): Response<List<LocationItem>>

}
