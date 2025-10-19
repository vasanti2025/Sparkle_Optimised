package com.loyalstring.rfid.data.model.addSingleItem

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "purity")
data class PurityModel(
    @PrimaryKey val Id: Int = 0,
    val PurityName: String = "",
    val CategoryId: Int = 0,
    val ShortName: String = "",
    val Description: String = "",
    val FinePercentage: String = "",
    val TodaysRate: String = "",
    val Status: String = "",
    val ClientCode: String = "",
    val EmployeeCode: String = "",
    val CategoryName: String = "",
    val LastUpdated: String = "",  // You can later convert to LocalDateTime if needed
    val CreatedOn: String = "",
    val StatusType: Boolean = true
)

