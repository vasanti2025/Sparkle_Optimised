package com.loyalstring.rfid.data.remote.data

import android.adservices.ondevicepersonalization.UserData
import com.google.gson.annotations.SerializedName

data class DeviceIdResponseData(
    @SerializedName("Message")
    val message: String? = null,

    @SerializedName("Data")
    val data: List<Data>)
