package com.loyalstring.rfid.data.local.entity

import androidx.room.*

@Entity(
    tableName = "page_controls",
    foreignKeys = [
        ForeignKey(
            entity = ModuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["moduleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("moduleId")]
)
data class PageControlEntity(
    @PrimaryKey val id: Int,
    val moduleId: Int,
    val key: String?,
    val label: String?,
    val type: String?,
    val visibility: String?,
    val place: String?
)
