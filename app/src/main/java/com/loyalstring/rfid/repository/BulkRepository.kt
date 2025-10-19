package com.loyalstring.rfid.repository

import com.loyalstring.rfid.data.local.dao.BulkItemDao
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.local.entity.EpcDto
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.remote.response.AlllabelResponse
import kotlinx.coroutines.flow.Flow

interface BulkRepository {
     val bulkItemDao: BulkItemDao
    suspend fun insertBulkItems(items: List<BulkItem>)
    suspend fun insertRFIDTags(items: List<EpcDto>)
    fun getAllBulkItems(): Flow<List<BulkItem>>
    fun getAllRFIDTags(): Flow<List<EpcDto>>
    suspend fun clearAllItems()
    suspend fun clearAllRFID()
    suspend fun syncBulkItemsFromServer(request: ClientCodeRequest): List<AlllabelResponse.LabelItem>
    suspend fun insertSingleItem(item: BulkItem)
    suspend fun syncRFIDItemsFromServer(request: ClientCodeRequest): List<EpcDto>

    suspend fun getDistinctBranchNames(): List<String>
    suspend fun getDistinctCounterNames(): List<String>
    suspend fun getDistinctBoxNames(): List<String>

    suspend fun getBranchIdFromName(name: String): Int?
    suspend fun getCounterIdFromName(name: String): Int?
    suspend fun getBoxIdFromName(name: String): Int?
    suspend fun getPacketIdFromName(name: String): Int?

    suspend fun updateBulkItem(item: BulkItem) {
        val existing = bulkItemDao.getById(item.id)
        if (existing == null) {
           // bulkItemDao.insertBulkItem(List<BulkItem>)
        } else {
            bulkItemDao.updateBulkItem(item)
        }
    }

    suspend fun deleteBulkItemById(id: Int): Int {
        return bulkItemDao.deleteById(id)
    }


}
