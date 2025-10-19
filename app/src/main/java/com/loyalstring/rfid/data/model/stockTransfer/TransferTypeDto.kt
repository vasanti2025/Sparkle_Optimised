package com.loyalstring.rfid.data.model.stockTransfer

import androidx.room.Entity

@Entity(tableName = "transfer_types")
data class TransferTypeDto(
    val Id: Int,
    val TransferType: String,
    val ClientCode: String,
    val CreatedOn: String,
    val LastUpdated: String,
    val StatusType: Boolean
)
