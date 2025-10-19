package com.loyalstring.rfid.data.model.order

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "lastorderno")
data class LastOrderNoResponse(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val LastOrderNo: String="")
