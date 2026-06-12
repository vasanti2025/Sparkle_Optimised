package com.loyalstring.rfid.data.model.order

import com.google.gson.annotations.SerializedName

data class OrderSearchRequest(
    @SerializedName("ClientCode") val clientCode: String?,
    @SerializedName("RfidCode") val rfidCode: String?,
    @SerializedName("CustomOrderId") val customOrderId: Int? = 0,
    @SerializedName("OrderId") val orderId: Int? = null,
    @SerializedName("OrderNo") val orderNo: String? = null
)
