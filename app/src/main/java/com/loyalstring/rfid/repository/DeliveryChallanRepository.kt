package com.loyalstring.rfid.repository


import com.loyalstring.rfid.data.model.deliveryChallan.AddDeliveryChallanRequest
import com.loyalstring.rfid.data.model.deliveryChallan.AddDeliveryChallanResponse
import com.loyalstring.rfid.data.model.deliveryChallan.ChallanNoRequest
import com.loyalstring.rfid.data.model.deliveryChallan.ChallanNoResponse
import com.loyalstring.rfid.data.model.deliveryChallan.CustomerTunchRequest
import com.loyalstring.rfid.data.model.deliveryChallan.CustomerTunchResponse
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanRequestList
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanResponseList
import com.loyalstring.rfid.data.model.deliveryChallan.UpdateDeliveryChallanRequest
import retrofit2.Response

interface DeliveryChallanRepository {
    suspend fun getAllDeliveryChallans(request: DeliveryChallanRequestList): Response<List<DeliveryChallanResponseList>>
    suspend fun getLastChallanNo(request: ChallanNoRequest): Response<ChallanNoResponse>
    suspend fun addDeliveryChallan(request: AddDeliveryChallanRequest): Response<AddDeliveryChallanResponse>
    suspend fun updateDeliveryChallan(request: UpdateDeliveryChallanRequest): Response<AddDeliveryChallanResponse>
    suspend fun getAllCustomerTunch(request: CustomerTunchRequest): Response<List<CustomerTunchResponse>>

}