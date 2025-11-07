package com.loyalstring.rfid.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class UserWithModules(
    @Embedded val user: UserPermissionEntity,
    @Relation(
        parentColumn = "userId",
        entityColumn = "userOwnerId",
        entity = ModuleEntity::class
    )
    val modules: List<ModuleWithControls>
)

data class ModuleWithControls(
    @Embedded val module: ModuleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "moduleId",
        entity = PageControlEntity::class
    )
    val controls: List<PageControlEntity>
)
