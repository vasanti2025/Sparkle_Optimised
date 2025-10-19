package com.loyalstring.rfid.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rfid_tags",
    indices = [Index(value = ["TidValue"], unique = true)]
)
data class EpcDto(
    val BarcodeNumber: String,
    val TidValue: String,
    val ClientCode: String?,
    @PrimaryKey(autoGenerate = true) val Id: Int,
    val CreatedOn: String,
    val LastUpdated: String,
    val StatusType: Boolean
)
