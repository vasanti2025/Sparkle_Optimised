package com.loyalstring.rfid.data.model.setting

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "location_table")
data class LocationItem(    @PrimaryKey val Id: Int,
                           val ClientCode: String?,
                           val UserId: Int?,
                           val BranchId: Int?,
                           val Latitude: String?,
                           val Longitude: String?,
                           val Address: String?,
                           val CreatedOn: String?,
                           val LastUpdated: String?,
                           val StatusType: Boolean?)
