package com.loyalstring.rfid.data.remote.data

data class RfidItem( val ClientCode: String,
                     val DeviceId: String,
                     val TIDValue: String,
                     val RFIDCode: String,
                     val Id: Int,
                     val CreatedOn: String,
                     val LastUpdated: String,
                     val StatusType: Boolean)
