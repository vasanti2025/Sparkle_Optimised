package com.loyalstring.rfid.data.model.order
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Payment(val string: String) : Parcelable
