package com.loyalstring.rfid.data.local.entity

import androidx.room.*

@Entity(
    tableName = "modules",
    foreignKeys = [
        ForeignKey(
            entity = UserPermissionEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userOwnerId")]
)
data class ModuleEntity(
    @PrimaryKey val id: Int,
    val userOwnerId: Int,
    val pageId: Int,
    val pageName: String,
    val pageDisplayName: String,
    val pagePermission: String
)
