package com.loyalstring.rfid.data.model.setting

data class LocationSyncRequest(
    val ClientCode: String,
    val UserId: Int,
    val BranchId: Int,
    val Latitude: String,
    val Longitude: String,
    val Address: String
)
