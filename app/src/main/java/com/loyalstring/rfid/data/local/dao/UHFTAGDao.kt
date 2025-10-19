package com.loyalstring.rfid.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.loyalstring.rfid.data.local.entity.UHFTAGEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UHFTAGDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: UHFTAGEntity)

    @Query("SELECT * FROM uhf_tags ORDER BY timestamp DESC")
    fun getAllTags(): Flow<List<UHFTAGEntity>>

    @Query("DELETE FROM uhf_tags")
    suspend fun deleteAll()
}
