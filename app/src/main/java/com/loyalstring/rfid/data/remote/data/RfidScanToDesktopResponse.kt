package com.loyalstring.rfid.data.remote.data

data class RfidScanToDesktopResponse(  val success: Boolean,
                                       val data: List<RfidItem>)
