package com.loyalstring.rfid.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.remote.data.IdNamePair
import kotlinx.coroutines.flow.Flow

@Dao
interface BulkItemDao {

    @Query("DELETE FROM bulk_items")
    suspend fun clearAllItems()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBulkItem(items: List<BulkItem>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSingleItem(item: BulkItem): Long

    @Query("SELECT * FROM bulk_items")
    fun getAllItemsFlow(): Flow<List<BulkItem>>

    @Query("SELECT * FROM bulk_items WHERE epc = :epc LIMIT 1")
    suspend fun getItemByEpc(epc: String): BulkItem?

    //  DISTINCT FIELD NAMES

    @Query("SELECT DISTINCT counterName FROM bulk_items WHERE counterName IS NOT NULL AND counterName != ''")
    suspend fun getDistinctCounterNames(): List<String>

    @Query("SELECT DISTINCT boxName FROM bulk_items WHERE boxName IS NOT NULL AND boxName != ''")
    suspend fun getDistinctBoxNames(): List<String>

    @Query("SELECT DISTINCT branchName FROM bulk_items WHERE branchName IS NOT NULL AND branchName != ''")
    suspend fun getDistinctBranchNames(): List<String>

    //  ID-NAME PAIRS (used to extract entity ID based on name)

    @Query("SELECT DISTINCT boxId AS id, boxName AS name FROM bulk_items WHERE boxId IS NOT NULL AND boxName IS NOT NULL")
    suspend fun getBoxIdNamePairs(): List<IdNamePair>

    @Query("SELECT DISTINCT branchId AS id, branchName AS name FROM bulk_items WHERE branchId IS NOT NULL AND branchName IS NOT NULL")
    suspend fun getBranchIdNamePairs(): List<IdNamePair>

    @Query("SELECT DISTINCT counterId AS id, counterName AS name FROM bulk_items WHERE counterId IS NOT NULL AND counterName IS NOT NULL")
    suspend fun getCounterIdNamePairs(): List<IdNamePair>

    @Query("SELECT DISTINCT packetId AS id, packetName AS name FROM bulk_items WHERE packetId IS NOT NULL AND packetName IS NOT NULL")
    suspend fun getPacketIdNamePairs(): List<IdNamePair>

    @Query("UPDATE bulk_items SET imageUrl = :newImageUrl WHERE itemCode = :itemCode")
    suspend fun updateImageUrl(itemCode: String, newImageUrl: String)

    @Update
    suspend fun updateBulkItem(item: BulkItem)

    @Query("SELECT * FROM bulk_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): BulkItem?

    @Query("UPDATE bulk_items SET scannedStatus = :status WHERE UPPER(TRIM(epc)) = :epc")
    suspend fun updateScannedStatus(epc: String, status: String)

    @Query("UPDATE bulk_items SET scannedStatus = ''")
    suspend fun resetAllScannedStatus()


        @Query("DELETE FROM bulk_items WHERE id = :id")
        suspend fun deleteById(id: Int): Int   // ✅ rows deleted


}
