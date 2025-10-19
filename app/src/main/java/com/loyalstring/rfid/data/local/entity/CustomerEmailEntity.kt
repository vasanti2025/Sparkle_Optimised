package com.loyalstring.rfid.data.local.entity


import androidx.room.Entity

import androidx.room.PrimaryKey


@Entity(tableName = "customer_email")
data class CustomerEmailEntity(
    @PrimaryKey val id: Int = 1, // only one record
    val email: String
)

