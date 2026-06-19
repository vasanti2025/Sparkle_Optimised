package com.loyalstring.rfid.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomWarnings
import androidx.room.Transaction
import androidx.room.Update
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.model.order.BulkItemWithDetails
import com.loyalstring.rfid.data.model.order.Diamond
import com.loyalstring.rfid.data.model.order.Stone
import com.loyalstring.rfid.data.remote.data.IdNamePair
import kotlinx.coroutines.flow.Flow

@Dao
@SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
interface BulkItemDao {

    @Query("DELETE FROM bulk_items")
    suspend fun clearAllItems()

    /*@Insert(onConflict = OnConflictStrategy.IGNORE)
    //@Transaction
    suspend fun insertBulkItem(items: List<BulkItem>): List<Long>*/

    /*@Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBulkItem(items: List<BulkItem>)*/

    @Transaction
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBulkItem(items: List<BulkItem>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSingleItem(item: BulkItem): Long

    //@Query("SELECT * FROM bulk_items")
    @Query("SELECT  id,bulkItemId, productName, itemCode, rfid, epc, imageUrl, isScanned, counterName, branchName, boxName, branchType, totalQty, totalNetWt, mrp, categoryId, category, productName, design,pcs FROM bulk_items")
    fun getMinimalItemsFlow(): Flow<List<BulkItem>>

   /* @Query("SELECT * FROM bulk_items")
   // @Query("SELECT id, productName, itemCode, rfid, epc, imageUrl, isScanned, counterName, branchName, boxName, branchType, totalQty, totalNetWt, mrp, categoryId, category, productName, design, sku FROM bulk_items")
    fun getAllItemsFlow(): Flow<List<BulkItem>>
*/
    @Query("SELECT id, bulkItemId,productName, itemCode, rfid, epc, imageUrl, isScanned, counterName, branchName, boxName, branchType, totalQty, totalNetWt, mrp, categoryId, category, design,pcs FROM bulk_items")
    suspend fun getMinimalItemFlow(): List<BulkItem>

    @Query("SELECT * FROM bulk_items")
    // @Query("SELECT id, productName, itemCode, rfid, epc, imageUrl, isScanned, counterName, branchName, boxName, branchType, totalQty, totalNetWt, mrp, categoryId, category, design, sku FROM bulk_items")
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

    @Query("SELECT * FROM bulk_items WHERE bulkItemId = :id LIMIT 1")
    suspend fun getById(id: Int): BulkItem?

    @Query("UPDATE bulk_items SET scannedStatus = :status WHERE UPPER(TRIM(epc)) = :epc")
    suspend fun updateScannedStatus(epc: String, status: String)

    @Query("UPDATE bulk_items SET scannedStatus = ''")
    suspend fun resetAllScannedStatus()

    @Query("DELETE FROM bulk_items WHERE bulkItemId = :id")
    suspend fun deleteById(id: Int): Int   // ✅ rows deleted

    //@Query("DELETE FROM bulk_items WHERE id = :id")
    //suspend fun deleteById(id: Int): Int   // ✅ rows deleted
    // Pagination queries for efficient large dataset handling
   @Query("SELECT id, bulkItemId, productName, itemCode, epc, imageUrl, isScanned, counterName, branchName, boxName, branchType, totalQty, totalNetWt, mrp, categoryId,category, rfid, design ,grossWeight,netWeight,stoneWeight,diamondWeight ,makingPerGram,fixMaking,fixWastage,stoneAmount,diamondAmount,sku ,purity ,branchId ,productId,designId ,purityId ,makingPercent,pcs,Status FROM bulk_items ORDER BY bulkItemId LIMIT :limit OFFSET :offset")
    suspend fun getMinimalItemsPaged(limit: Int, offset: Int): List<BulkItem>

    @Query("SELECT COUNT(*) FROM bulk_items")
    suspend fun getTotalItemCount(): Int

    @Query("SELECT id,bulkItemId, productName, itemCode, epc, imageUrl, isScanned, counterName, branchName, boxName, branchType, totalQty, totalNetWt, mrp, categoryId, rfid, design,grossWeight,netWeight ,stoneWeight,diamondWeight,makingPerGram,fixMaking,fixWastage,stoneAmount,diamondAmount,sku FROM bulk_items WHERE scannedStatus = :status ORDER BY bulkItemId LIMIT :limit OFFSET :offset")
    suspend fun getItemsByStatusPaged(status: String, limit: Int, offset: Int): List<BulkItem>

    @Query("SELECT COUNT(*) FROM bulk_items WHERE scannedStatus = :status")
    suspend fun getItemCountByStatus(status: String): Int

    /** Fast EPC-only queries for inventory scan matching (avoids loading full BulkItem rows). */
    @Query("SELECT UPPER(TRIM(epc)) FROM bulk_items WHERE epc IS NOT NULL AND TRIM(epc) != ''")
    suspend fun getAllNormalizedEpcs(): List<String>

    @Query("SELECT UPPER(TRIM(epc)) FROM bulk_items WHERE boxName = :value AND epc IS NOT NULL AND TRIM(epc) != ''")
    suspend fun getEpcsByBox(value: String): List<String>

    @Query("SELECT UPPER(TRIM(epc)) FROM bulk_items WHERE branchName = :value AND epc IS NOT NULL AND TRIM(epc) != ''")
    suspend fun getEpcsByBranch(value: String): List<String>

    @Query("SELECT UPPER(TRIM(epc)) FROM bulk_items WHERE counterName = :value AND epc IS NOT NULL AND TRIM(epc) != ''")
    suspend fun getEpcsByCounter(value: String): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM bulk_items WHERE UPPER(TRIM(epc)) = :epc LIMIT 1)")
    suspend fun epcExists(epc: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM bulk_items WHERE UPPER(TRIM(epc)) = :epc AND boxName = :box LIMIT 1)")
    suspend fun epcExistsInBox(epc: String, box: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM bulk_items WHERE UPPER(TRIM(epc)) = :epc AND branchName = :branch LIMIT 1)")
    suspend fun epcExistsInBranch(epc: String, branch: String): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM bulk_items WHERE UPPER(TRIM(epc)) = :epc AND counterName = :counter LIMIT 1)")
    suspend fun epcExistsInCounter(epc: String, counter: String): Boolean


    /*@Query("DELETE FROM bulk_items WHERE id = :id")
    suspend fun deleteById(id: Int): Int   // ✅ rows deleted*/
    @Query("SELECT id,bulkItemId, productName, itemCode, epc, imageUrl, isScanned, counterName, branchName, boxName, branchType, totalQty, totalNetWt, mrp, categoryId FROM bulk_items WHERE UPPER(TRIM(epc)) IN (:epcs) ORDER BY bulkItemId LIMIT :limit OFFSET :offset")
    suspend fun getItemsByEpcsPaged(epcs: List<String>, limit: Int, offset: Int): List<BulkItem>

    @Query("SELECT COUNT(*) FROM bulk_items WHERE UPPER(TRIM(epc)) IN (:epcs)")
    suspend fun getItemCountByEpcs(epcs: List<String>): Int


   /* @Query("SELECT * FROM bulk_items WHERE UPPER(TRIM(epc)) = UPPER(TRIM(:epc)) LIMIT 1")
    suspend fun getItemByEpc(epc: String): BulkItem?*/

    // ✅ NEW: batch fetch (fast)
    @Query("SELECT * FROM bulk_items WHERE UPPER(TRIM(epc)) IN (:epcs)")
    suspend fun getItemsByEpcs(epcs: List<String>): List<BulkItem>

    @Insert
    suspend fun insertStones(stones: List<Stone>)

    @Insert
    suspend fun insertDiamonds(diamonds: List<Diamond>)

    @Transaction
    @Query("SELECT * FROM bulk_items")
    suspend fun getAllBulkItems(): List<BulkItemWithDetails>

    /*for single use tag*/

 /*   @Query("SELECT itemCode FROM bulk_items WHERE epc = :epc LIMIT 1")
    suspend fun getItemCodeByEpc(epc: String): String?*/

    @Query("""
    SELECT BarcodeNumber 
    FROM rfid_tags 
    WHERE UPPER(TRIM(TidValue)) = UPPER(TRIM(:epc))
    LIMIT 1
""")
    suspend fun getItemCodeByEpc(epc: String): String?

    @Update
    suspend fun updateBulkItems(items: List<BulkItem>)



    /*chetan changes*/

    @Query("""
    SELECT * FROM bulk_items
    WHERE scannedStatus IN ('Unmatched','Not Found')
    LIMIT :limit OFFSET :offset
""")
    /*@Query("""
    SELECT * FROM bulk_items
    WHERE
    scannedStatus IN ('Unmatched','Not Found')
    OR scannedStatus IS NULL
    OR TRIM(scannedStatus) = ''
""")*/
    suspend fun getUnmatchedPaged(limit: Int, offset: Int): List<BulkItem>


    @Query("""
    SELECT * FROM bulk_items
    WHERE scannedStatus IN ('Matched','Found')
    LIMIT :limit OFFSET :offset
""")
    suspend fun getMatchedPaged(limit: Int, offset: Int): List<BulkItem>

 @Query("SELECT * FROM bulk_items")
 fun getAllBulkItemsFlow(): Flow<List<BulkItem>>

 @Query("SELECT * FROM bulk_items")
 suspend fun getAllBulkItemsOnce(): List<BulkItem>

 @Query("DELETE FROM bulk_items")
 suspend fun deleteAll()


 @Query("""
    SELECT DISTINCT TRIM(counterName) 
    FROM bulk_items 
    WHERE counterName IS NOT NULL AND TRIM(counterName) != ''
""")
 suspend fun getDistinctCounters(): List<String>

 @Query("""
    SELECT DISTINCT TRIM(boxName) 
    FROM bulk_items 
    WHERE boxName IS NOT NULL AND TRIM(boxName) != ''
""")
 suspend fun getDistinctBoxes(): List<String>

 @Query("""
    SELECT DISTINCT TRIM(branchName) 
    FROM bulk_items 
    WHERE counterName IS NOT NULL AND TRIM(branchName) != ''
""")
 suspend fun getDistinctBranch(): List<String>

    @Query("UPDATE bulk_items SET Status = :status WHERE bulkItemId = :id")
    suspend fun updateStatusByBulkItemid(id: Int, status: String)

}
