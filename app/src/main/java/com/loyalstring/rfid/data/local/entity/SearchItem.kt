package com.loyalstring.rfid.data.local.entity

import java.io.Serializable

data class SearchItem(
    val epc: String,
    val itemCode: String,
    val rfid: String,
    val productName: String,
    val rssi: String? = null,
    val proximityPercent: Int = 0
) : Serializable