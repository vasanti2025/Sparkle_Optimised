package com.loyalstring.rfid.repository

import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import com.loyalstring.rfid.data.remote.data.DeviceIdData
import com.loyalstring.rfid.data.remote.data.DeviceIdResponseData
import com.loyalstring.rfid.data.remote.data.RfidScanToDesktopResponse
import jakarta.inject.Inject
import retrofit2.Response

class RfidScanDesktopRepositoryImpl  @Inject constructor(
    private val apiService: RetrofitInterface
) : RfidScanDesktopRepository {

    override suspend fun getAllScantoDesktop(
        request: ClientCodeRequest
    ): Result<RfidScanToDesktopResponse> {
        return try {
            val response = apiService.getAllScantoDesktop(request)

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception(response.message()))
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun addDeviceId(
        request: List<DeviceIdData>
    ): Response<DeviceIdResponseData> {
        return apiService.addDeviceId(request)
    }

   /* override suspend fun getDeviceId(
        request: ClientCodeRequest
    ): Response<DeviceIdData> {
        return apiService.getDeviceId(request)
    }*/
}