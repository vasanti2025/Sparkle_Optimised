package com.loyalstring.rfid.repository

import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.remote.data.DeviceIdData
import com.loyalstring.rfid.data.remote.data.DeviceIdResponseData
import com.loyalstring.rfid.data.remote.data.RfidScanToDesktopResponse
import retrofit2.Response

interface RfidScanDesktopRepository {

    suspend fun getAllScantoDesktop(
        request: ClientCodeRequest
    ): Result<RfidScanToDesktopResponse>


    suspend fun addDeviceId(
        request: List<DeviceIdData>
    ): Response<DeviceIdResponseData>

  /*  suspend fun getDeviceId(
        request: ClientCodeRequest
    ): Response<DeviceIdData>*/
}