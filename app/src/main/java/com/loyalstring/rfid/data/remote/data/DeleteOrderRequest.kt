package com.loyalstring.rfid.data.remote.data

data class DeleteOrderRequest(
    val clientCode: String,
    val CustomOrderId: Int
)