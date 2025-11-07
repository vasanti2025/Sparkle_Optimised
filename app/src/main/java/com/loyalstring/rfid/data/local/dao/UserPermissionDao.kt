package com.loyalstring.rfid.data.local.dao

import androidx.room.*
import com.loyalstring.rfid.data.local.entity.*

@Dao
interface UserPermissionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserPermission(user: UserPermissionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModules(modules: List<ModuleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPageControls(pageControls: List<PageControlEntity>)

    @Transaction
    @Query("SELECT * FROM user_permissions WHERE userId = :userId")
    suspend fun getUserWithModules(userId: Int): UserWithModules?

    @Query("SELECT * FROM user_permissions LIMIT 1")
    suspend fun getUserPermission(): UserPermissionEntity?

    @Query("DELETE FROM user_permissions")
    suspend fun clearAll()
}
