package com.loyalstring.rfid.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.loyalstring.rfid.data.local.entity.TransferTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferTypeDao {
    @Query("SELECT * FROM transfer_types WHERE StatusType = 1")
    fun getActiveTransferTypes(): Flow<List<TransferTypeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(types: List<TransferTypeEntity>)

    @Query("DELETE FROM transfer_types")
    suspend fun clearAll()
}
