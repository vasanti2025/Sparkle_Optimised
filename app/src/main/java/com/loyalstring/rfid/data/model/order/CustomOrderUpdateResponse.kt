package com.loyalstring.rfid.data.model.order

data class CustomOrderUpdateResponse(
    val Success: Boolean,
    val Message: String,
    val Data: CustomOrderResponse
)
