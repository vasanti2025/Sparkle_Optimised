package com.loyalstring.rfid.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "design")
data class Design(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)