package com.loyalstring.rfid.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transfer_types")
data class TransferTypeEntity(
    @PrimaryKey val Id: Int,
    val TransferType: String,
    val ClientCode: String,
    val CreatedOn: String,
    val LastUpdated: String,
    val StatusType: Boolean
)
