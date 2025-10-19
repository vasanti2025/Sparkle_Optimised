package com.loyalstring.rfid.repository

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.loyalstring.rfid.data.local.dao.BulkItemDao
import com.loyalstring.rfid.data.local.dao.EpcDao
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.local.entity.EpcDto
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import com.loyalstring.rfid.data.remote.response.AlllabelResponse
import kotlinx.coroutines.flow.Flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class BulkRepositoryImpl @Inject constructor(
    private val apiService: RetrofitInterface,
    override val bulkItemDao: BulkItemDao,
    private val epcDao: EpcDao
) : BulkRepository {

    override suspend fun insertBulkItems(items: List<BulkItem>) {
        bulkItemDao.insertBulkItem(items)
    }

    override suspend fun insertRFIDTags(items: List<EpcDto>) {
        epcDao.insertRFIDTag(items)
    }

    override suspend fun insertSingleItem(item: BulkItem) {
        bulkItemDao.insertSingleItem(item)

    }

    override fun getAllBulkItems(): Flow<List<BulkItem>> {
        return bulkItemDao.getAllItemsFlow()
    }

    override fun getAllRFIDTags(): Flow<List<EpcDto>> {
        return epcDao.getAllItemsFlow()
    }

    override suspend fun clearAllItems() {
        bulkItemDao.clearAllItems()
    }

    override suspend fun clearAllRFID() {
        epcDao.clearAllTags()
    }

    suspend fun getItemByItemCode(itemCode: String): BulkItem? {
        return bulkItemDao.getItemByEpc(itemCode)
    }

    override suspend fun getDistinctCounterNames(): List<String> {
        return bulkItemDao.getDistinctCounterNames()
    }

    override suspend fun getDistinctBranchNames(): List<String> {
        return bulkItemDao.getDistinctBranchNames()
    }

    override suspend fun getDistinctBoxNames(): List<String> {
        return bulkItemDao.getDistinctBoxNames()
    }

    override suspend fun getBranchIdFromName(name: String): Int? {
        return bulkItemDao.getBranchIdNamePairs().find { it.name.equals(name, true) }?.id
    }

    override suspend fun getCounterIdFromName(name: String): Int? {
        return bulkItemDao.getCounterIdNamePairs().find { it.name.equals(name, true) }?.id
    }

    override suspend fun getBoxIdFromName(name: String): Int? {
        return bulkItemDao.getBoxIdNamePairs().find { it.name.equals(name, true) }?.id
    }

    override suspend fun getPacketIdFromName(name: String): Int? {
        return bulkItemDao.getPacketIdNamePairs().find { it.name.equals(name, true) }?.id
    }


    override suspend fun syncBulkItemsFromServer(request: ClientCodeRequest): List<AlllabelResponse.LabelItem> {

        val jsonObject = JsonObject().apply {
            addProperty("ClientCode", request.clientcode)
        }

// Convert it to pretty JSON
        val gson = GsonBuilder().setPrettyPrinting().create()
        val prettyJson = gson.toJson(jsonObject)

// Convert string to RequestBody
        val requestBody = prettyJson.toRequestBody("application/json".toMediaType())

        return try {
            val response = apiService.getAllLabeledStock(requestBody)
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateScannedStatus(epc: String, status: String) {
        bulkItemDao.updateScannedStatus(epc, status)
    }

    // ✅ Reset all statuses
    suspend fun resetAllScannedStatus() {
        bulkItemDao.resetAllScannedStatus()
    }
    override suspend fun syncRFIDItemsFromServer(request: ClientCodeRequest): List<EpcDto> {

        val jsonObject = JsonObject().apply {
            addProperty("ClientCode", request.clientcode)
        }

// Convert it to pretty JSON
        val gson = GsonBuilder().setPrettyPrinting().create()
        val prettyJson = gson.toJson(jsonObject)

// Convert string to RequestBody
        val requestBody = prettyJson.toRequestBody("application/json".toMediaType())

        return try {
            val response = apiService.getAllRFID(requestBody)
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }


    fun getAllTagsFlow(): Flow<List<EpcDto>> {
        return epcDao.getAllTagsFlow()
    }
}


