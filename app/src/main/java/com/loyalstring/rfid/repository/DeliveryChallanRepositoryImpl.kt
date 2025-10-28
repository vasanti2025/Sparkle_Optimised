package com.loyalstring.rfid.repository


import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanRequestList
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanResponseList
import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import retrofit2.Response
import javax.inject.Inject

class DeliveryChallanRepositoryImpl @Inject constructor(
    private val api: RetrofitInterface
) : DeliveryChallanRepository {

    override suspend fun getAllDeliveryChallans(
        request: DeliveryChallanRequestList
    ): Response<List<DeliveryChallanResponseList>> {
        return api.getAllChallanList(request)
    }
}