package com.loyalstring.rfid.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_permissions")
data class UserPermissionEntity(
    @PrimaryKey val userId: Int,
    val firstName: String,
    val lastName: String,
    val roleId: Int,
    val roleName: String,
    val clientCode: String,
    val branchSelectionJson: String
)

