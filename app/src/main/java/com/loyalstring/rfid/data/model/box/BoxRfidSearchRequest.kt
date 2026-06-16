package com.loyalstring.rfid.data.model.box

import com.google.gson.annotations.SerializedName

data class BoxRfidSearchRequest(
    @SerializedName("clientCode") val clientCode: String,
    @SerializedName("rfidCode") val rfidCode: String
)
