package com.loyalstring.rfid.repository

import com.loyalstring.rfid.data.model.box.BoxRfidDetailsResponse
import com.loyalstring.rfid.data.model.box.BoxRfidSearchRequest
import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import retrofit2.Response
import javax.inject.Inject

class BoxRfidRepository @Inject constructor(
    private val apiService: RetrofitInterface
) {
    suspend fun getDetailsByRfidCode(
        request: BoxRfidSearchRequest
    ): Response<BoxRfidDetailsResponse> {
        return apiService.getBoxDetailsByRfidCode(request)
    }
}
