package com.loyalstring.rfid.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.google.gson.stream.JsonReader
import com.loyalstring.rfid.data.local.dao.BulkItemDao
import com.loyalstring.rfid.data.local.dao.EpcDao
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.local.entity.EpcDto
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.order.Diamond
import com.loyalstring.rfid.data.model.order.Stone
import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import com.loyalstring.rfid.data.remote.data.ClearStockDataModelReq
import com.loyalstring.rfid.data.remote.response.AlllabelResponse
import com.loyalstring.rfid.data.remote.response.ClearStockDataModelResponse
import com.loyalstring.rfid.di.NormalRetrofit
import com.loyalstring.rfid.di.SyncRetrofit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.EOFException
import java.net.SocketException
import javax.inject.Inject

class BulkRepositoryImpl @Inject constructor(
    @SyncRetrofit private val syncApi: RetrofitInterface,
    @NormalRetrofit private val apiService: RetrofitInterface,
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

    override fun getMinimalItemsFlow(): Flow<List<BulkItem>> {
        return bulkItemDao.getMinimalItemsFlow()
    }

    override suspend fun getMinimalItemFlow(): List<BulkItem> {
        return bulkItemDao.getMinimalItemFlow()
    }

    override fun getAllItemsFlow(): Flow<List<BulkItem>> {
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

    override suspend fun clearStockData(req: ClearStockDataModelReq): ClearStockDataModelResponse {
        val res = apiService.clearStockData(req)
        val body = res.body()

        if (res.isSuccessful && body != null) return body

        // helpful debug
        val err = res.errorBody()?.string()
        throw Exception("API Error: ${res.code()} ${res.message()} | $err")
    }


    override suspend fun syncBulkItemsFromServer(request: ClientCodeRequest): List<AlllabelResponse.LabelItem> {
        Log.d("SYNC_ITEM", "Server API syncBulkItemsFromServer Called Repository")
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
               // Log.d("## response","response"+response)
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


    /*override suspend fun syncBulkItemsFromServer(
        request: ClientCodeRequest,
        tagType: String,
        mapItem: suspend (AlllabelResponse.LabelItem) -> BulkItem?,
        onProgress: suspend (processed: Int, synced: Int) -> Unit
    ) {
        val json = JsonObject().apply {
            addProperty("ClientCode", request.clientcode)
        }

        val requestBody = json
            .toString()
            .toRequestBody("application/json".toMediaType())

        val response = syncApi.getAllLabeledStockNew(requestBody)
        if (!response.isSuccessful || response.body() == null) return

        val reader = JsonReader(response.body()!!.charStream())
        val gson = Gson()

        val BATCH_SIZE = 2000
        val batch = ArrayList<BulkItem>(BATCH_SIZE)

        var processed = 0
        var synced = 0

        reader.beginArray()
        while (reader.hasNext()) {

            val serverItem: AlllabelResponse.LabelItem =
                gson.fromJson(reader, AlllabelResponse.LabelItem::class.java)

            processed++

            val bulkItem = mapItem(serverItem)
            if (bulkItem != null) {
                batch.add(bulkItem)
                synced++
            }

            if (batch.size == BATCH_SIZE) {
                bulkItemDao.insertBulkItem(batch)
                batch.clear()
            }

            onProgress(processed, synced)
        }

        reader.endArray()
        reader.close()

        if (batch.isNotEmpty()) {
            bulkItemDao.insertBulkItem(batch)
        }
    }*/

    /*override suspend fun syncBulkItemsFromServer(
        request: ClientCodeRequest,
        tagType: String,
        mapItem: suspend (AlllabelResponse.LabelItem) -> BulkItem?,
        onProgress: suspend (processed: Int, synced: Int) -> Unit
    ) {
        val requestBody = JsonObject().apply {
            addProperty("ClientCode", request.clientcode)
        }.toString().toRequestBody("application/json".toMediaType())

        val response = syncApi.getAllLabeledStockNew(requestBody)
        if (!response.isSuccessful || response.body() == null) return

        val reader = JsonReader(response.body()!!.charStream()).apply {
            isLenient = true
        }

        //val gson = Gson()
        val gson = GsonBuilder()
            .disableHtmlEscaping()
            .create()

        // 🔥 Bigger batch = faster Room inserts
        val BATCH_SIZE = 5_000
        val batch = ArrayList<BulkItem>(BATCH_SIZE)

        var processed = 0
        var synced = 0

        // 🔥 Throttle UI updates
        val PROGRESS_INTERVAL = 500L
        var lastProgressTime = System.currentTimeMillis()

        reader.beginArray()
        while (reader.hasNext()) {

            val serverItem: AlllabelResponse.LabelItem = gson.fromJson(
                reader,
                AlllabelResponse.LabelItem::class.java
            )

            processed++

            val bulkItem = mapItem(serverItem)
            if (bulkItem != null) {
                batch.add(bulkItem)
                synced++
            }

            // 🔥 Batch DB insert
            if (batch.size >= BATCH_SIZE) {
                bulkItemDao.insertBulkItem(batch)
                batch.clear()
            }

            // 🔥 Throttled progress callback
            val now = System.currentTimeMillis()
            if (now - lastProgressTime >= PROGRESS_INTERVAL) {
                onProgress(processed, synced)
                lastProgressTime = now
            }
        }

        reader.endArray()
        reader.close()

        // 🔥 Insert remaining
        if (batch.isNotEmpty()) {
            bulkItemDao.insertBulkItem(batch)
        }

        // 🔥 Final progress update
        onProgress(processed, synced)
    }*/



    /*override suspend fun syncBulkItemsFromServer(
        request: ClientCodeRequest,
        tagType: String,
        mapItem: suspend (AlllabelResponse.LabelItem) -> BulkItem?,
        onProgress: suspend (processed: Int, synced: Int) -> Unit
    ) {
        Log.d("SYNC_ITEM", "Server API New Called: " + request.clientcode)
        val requestBody = JsonObject().apply {
            addProperty("ClientCode", request.clientcode)
        }.toString().toRequestBody("application/json".toMediaType())

        val response = syncApi.getAllLabeledStockNew(requestBody)
        if (!response.isSuccessful || response.body() == null) return

        val reader = JsonReader(response.body()!!.charStream()).apply {
            isLenient = true
        }

        val gson = GsonBuilder()
            .disableHtmlEscaping()
            .create()

        val BATCH_SIZE = 1000   // 🔥 critical
        val batch = ArrayList<BulkItem>(BATCH_SIZE)

        var processed = 0
        var synced = 0

        val PROGRESS_INTERVAL = 1000L
        var lastProgressTime = System.currentTimeMillis()

        reader.beginArray()
        try {
            while (reader.hasNext()) {

                val serverItem: AlllabelResponse.LabelItem = gson.fromJson(
                    reader,
                    AlllabelResponse.LabelItem::class.java
                )

                processed++

                val bulkItem = mapItem(serverItem)
                if (bulkItem != null) {
                    batch.add(bulkItem)
                    synced++
                }

                if (batch.size >= BATCH_SIZE) {
                    bulkItemDao.insertBulkItem(batch)
                    batch.clear()

                    // 🔥 prevent heap buildup
                    System.gc()
                }

                val now = System.currentTimeMillis()
                if (now - lastProgressTime >= PROGRESS_INTERVAL) {
                    onProgress(processed, synced)
                    lastProgressTime = now
                }
            }
        } catch (e: SocketException) {
            // Network interrupted
            Log.e("SYNC_ITEM", "SocketException $e")
        } catch (e: EOFException) {
            // Partial response
            Log.e("SYNC_ITEM", "EOFException $e")
        } catch (e: JsonSyntaxException) {
            // Only real JSON corruption
            Log.e("SYNC_ITEM", "JsonSyntaxException $e")
        }


        reader.endArray()
        reader.close()

        if (batch.isNotEmpty()) {
            bulkItemDao.insertBulkItem(batch)
        }

        onProgress(processed, synced)
    }*/




    override suspend fun syncBulkItemsFromServer(
        request: ClientCodeRequest,
        tagType: String,
        mapItem: suspend (AlllabelResponse.LabelItem) -> BulkItem?,
        onProgress: suspend (processed: Int, synced: Int, totalCount: Int) -> Unit
    ) = withContext(Dispatchers.IO) {

        Log.d("SYNC_ITEM", "Server API New Called: ${request.clientcode}")
        var totalCount = 0;
        val requestBody = JsonObject().apply {
            addProperty("ClientCode", request.clientcode)
        }.toString().toRequestBody("application/json".toMediaType())

        val response = try {
            syncApi.getAllLabeledStockNew(requestBody)
        } catch (e: Exception) {
            Log.e("SYNC_ITEM", "API call failed", e)
            return@withContext
        }

        if (!response.isSuccessful) {
            Log.e("SYNC_ITEM", "API failed: ${response.code()}")
            return@withContext
        }

        val body = response.body() ?: return@withContext

        val gson = GsonBuilder()
            .disableHtmlEscaping()
            .create()

        val BATCH_SIZE = 500        // 🔥 safer for memory + DB
        val batch = ArrayList<BulkItem>(BATCH_SIZE)

        var processed = 0
        var synced = 0

        val PROGRESS_INTERVAL = 1000L
        var lastProgressTime = System.currentTimeMillis()

        body.charStream().use { stream ->
            val reader = JsonReader(stream).apply {
                isLenient = true
            }

            try {
                reader.beginArray()

                while (reader.hasNext()) {
                    val serverItem : AlllabelResponse.LabelItem = gson.fromJson(
                        reader,
                        AlllabelResponse.LabelItem::class.java
                    )

                    if (totalCount == 0) {
                        totalCount = serverItem.totalCount
                    }

                    processed++

                    val bulkItem = mapItem(serverItem)
                    if (bulkItem != null) {
                        batch.add(bulkItem)
                        synced++
                    }

                    if (batch.size == BATCH_SIZE) {
                        bulkItemDao.insertBulkItem(batch)
                        batch.clear()
                    }

                    val now = System.currentTimeMillis()
                    if (now - lastProgressTime >= PROGRESS_INTERVAL) {
                        onProgress(processed, synced, totalCount)
                        lastProgressTime = now
                    }
                }

                reader.endArray()

            } catch (e: SocketException) {
                Log.e("SYNC_ITEM", "Connection reset during sync", e)

            } catch (e: EOFException) {
                Log.e("SYNC_ITEM", "Partial response received", e)

            } catch (e: JsonSyntaxException) {
                Log.e("SYNC_ITEM", "Malformed JSON from server", e)

            } catch (e: Exception) {
                Log.e("SYNC_ITEM", "Unexpected sync error", e)
            }
        }

        // 🔥 insert remaining items
        if (batch.isNotEmpty()) {
            bulkItemDao.insertBulkItem(batch)
            batch.clear()
        }

        onProgress(processed, synced, totalCount)
    }



    fun getAllTagsFlow(): Flow<List<EpcDto>> {
        return epcDao.getAllTagsFlow()
    }

    // Lazy loading methods for efficient large dataset handling
    suspend fun getMinimalItemsPaged(limit: Int, offset: Int): List<BulkItem> {
        return bulkItemDao.getMinimalItemsPaged(limit, offset)
    }

    suspend fun getTotalItemCount(): Int {
        return bulkItemDao.getTotalItemCount()
    }

    suspend fun getItemsByStatusPaged(status: String, limit: Int, offset: Int): List<BulkItem> {
        return bulkItemDao.getItemsByStatusPaged(status, limit, offset)
    }

    suspend fun getItemCountByStatus(status: String): Int {
        return bulkItemDao.getItemCountByStatus(status)
    }

    suspend fun getItemsByEpcsPaged(epcs: List<String>, limit: Int, offset: Int): List<BulkItem> {
        return bulkItemDao.getItemsByEpcsPaged(epcs, limit, offset)
    }

    suspend fun getItemCountByEpcs(epcs: List<String>): Int {
        return bulkItemDao.getItemCountByEpcs(epcs)
    }

    suspend fun insertBulkItemWithDetails(
        bulkItem: BulkItem,
        stones: List<Stone>,
        diamonds: List<Diamond>
    ) {
        // 1️⃣ insert parent
        val bulkItemId = bulkItemDao.insertSingleItem(bulkItem).toInt()

        // 2️⃣ insert stones
        if (stones.isNotEmpty()) {
            bulkItemDao.insertStones(
                stones.map { it.copy(bulkItemId = bulkItemId) }
            )
        }

        // 3️⃣ insert diamonds
        if (diamonds.isNotEmpty()) {
            bulkItemDao.insertDiamonds(
                diamonds.map { it.copy(bulkItemId = bulkItemId) }
            )
        }
    }








}


