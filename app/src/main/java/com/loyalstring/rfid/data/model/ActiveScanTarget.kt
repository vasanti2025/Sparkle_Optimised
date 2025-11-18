package com.loyalstring.rfid.data.model

object ActiveScanTarget {
    @Volatile
    var currentIndex: Int? = null
}