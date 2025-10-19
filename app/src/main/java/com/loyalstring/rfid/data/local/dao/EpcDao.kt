package com.loyalstring.rfid.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.loyalstring.rfid.data.local.entity.EpcDto
import kotlinx.coroutines.flow.Flow

@Dao
interface EpcDao {

    @Query("DELETE FROM rfid_tags")
    suspend fun clearAllTags()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRFIDTag(items: List<EpcDto>): List<Long>

    @Query("SELECT * FROM rfid_tags")
    fun getAllTagsFlow(): Flow<List<EpcDto>>

    @Query("SELECT * FROM rfid_tags")
    fun getAllItemsFlow(): Flow<List<EpcDto>>

    @Query("SELECT * FROM rfid_tags WHERE BarcodeNumber = :itemCode LIMIT 1")
    suspend fun getItemByRFIDCode(itemCode: String): EpcDto?

}