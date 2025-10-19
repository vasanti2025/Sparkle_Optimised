package com.loyalstring.rfid.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "uhf_tags")
data class UHFTAGEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tagJson: String,
    val timestamp: Long = System.currentTimeMillis()
)