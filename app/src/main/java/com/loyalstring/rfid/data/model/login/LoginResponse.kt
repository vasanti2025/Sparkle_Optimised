package com.loyalstring.rfid.data.model.login

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("Employee")
    val employee: Employee? = null,

    @SerializedName("token")
    val token: String? = null
)
