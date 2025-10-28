package com.loyalstring.rfid.repository


import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanRequestList
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanResponseList
import retrofit2.Response

interface DeliveryChallanRepository {
    suspend fun getAllDeliveryChallans(request: DeliveryChallanRequestList): Response<List<DeliveryChallanResponseList>>
}