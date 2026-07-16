package com.loyalstring.rfid.repository


import com.loyalstring.rfid.data.model.deliveryChallan.AddDeliveryChallanRequest
import com.loyalstring.rfid.data.model.deliveryChallan.AddDeliveryChallanResponse
import com.loyalstring.rfid.data.model.deliveryChallan.ChallanNoRequest
import com.loyalstring.rfid.data.model.deliveryChallan.ChallanNoResponse
import com.loyalstring.rfid.data.model.deliveryChallan.CustomerTunchRequest
import com.loyalstring.rfid.data.model.deliveryChallan.CustomerTunchResponse
import com.loyalstring.rfid.data.model.deliveryChallan.DeleteDeliveryChallanRequest
import com.loyalstring.rfid.data.model.deliveryChallan.DeleteDeliveryChallanResponse
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanRequestList
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanResponseList
import com.loyalstring.rfid.data.model.deliveryChallan.UpdateDeliveryChallanRequest
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

    override suspend fun getLastChallanNo(
        request: ChallanNoRequest
    ): Response<ChallanNoResponse> {
        return api.getLastChallanNo(request)
    }

    override suspend fun addDeliveryChallan(
        request: AddDeliveryChallanRequest
    ): Response<AddDeliveryChallanResponse> {
        return api.addDeliveryChallan(request)
    }

    override suspend fun updateDeliveryChallan(
        request: UpdateDeliveryChallanRequest
    ): Response<AddDeliveryChallanResponse> {
        return api.updateDeliveryChallan(request)
    }

    override suspend fun getAllCustomerTunch(
        request: CustomerTunchRequest
    ): Response<List<CustomerTunchResponse>> {
        return api.getAllCustomerTounch(request)
    }

    override suspend fun deleteDeliveryChallan(
        request: DeleteDeliveryChallanRequest
    ): Response<DeleteDeliveryChallanResponse> {
        return api.deleteDeliveryChallan(request)
    }

   /* override suspend fun getDeliveryChallanById(clientCode: String, challanId: Int) =
        api.getDeliveryChallanById(clientCode, challanId)*/


}