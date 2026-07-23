package com.loyalstring.rfid.ui.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanListRow
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanResponseList
import com.loyalstring.rfid.data.model.deliveryChallan.toListRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val gson = Gson()

object DeliveryChallanListCache {
    private const val PREFS_NAME = "delivery_challan_list_cache"

    private val memoryRows = mutableMapOf<String, List<DeliveryChallanListRow>>()

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun baseKey(clientCode: String, branchId: Int) =
        "${clientCode.trim().lowercase()}_$branchId"

    private fun rowsKey(clientCode: String, branchId: Int) = "rows_${baseKey(clientCode, branchId)}"
    private fun fullKey(clientCode: String, branchId: Int) = "full_${baseKey(clientCode, branchId)}"
    private fun legacyKey(clientCode: String, branchId: Int) = baseKey(clientCode, branchId)

    fun readMemoryRows(clientCode: String, branchId: Int): List<DeliveryChallanListRow>? =
        memoryRows[baseKey(clientCode, branchId)]

    private fun writeMemoryRows(clientCode: String, branchId: Int, rows: List<DeliveryChallanListRow>) {
        if (rows.isNotEmpty()) {
            memoryRows[baseKey(clientCode, branchId)] = rows
        }
    }

    suspend fun loadRows(
        context: Context,
        clientCode: String,
        branchId: Int,
    ): List<DeliveryChallanListRow> = withContext(Dispatchers.IO) {
        readMemoryRows(clientCode, branchId)?.let { return@withContext it }

        val key = rowsKey(clientCode, branchId)
        val json = prefs(context).getString(key, null)
            ?: migrateLegacyRows(context, clientCode, branchId)
            ?: return@withContext emptyList()
        parseRows(json).also { rows ->
            if (rows.isNotEmpty()) writeMemoryRows(clientCode, branchId, rows)
        }
    }

    suspend fun saveRows(
        context: Context,
        clientCode: String,
        branchId: Int,
        rows: List<DeliveryChallanListRow>,
    ) = withContext(Dispatchers.IO) {
        writeMemoryRows(clientCode, branchId, rows)
        val type = object : TypeToken<List<DeliveryChallanListRow>>() {}.type
        prefs(context).edit()
            .putString(rowsKey(clientCode, branchId), gson.toJson(rows, type))
            .apply()
    }

    suspend fun loadFull(
        context: Context,
        clientCode: String,
        branchId: Int,
    ): List<DeliveryChallanResponseList> = withContext(Dispatchers.IO) {
        val json = prefs(context).getString(fullKey(clientCode, branchId), null)
            ?: prefs(context).getString(legacyKey(clientCode, branchId), null)
            ?: return@withContext emptyList()
        parseFull(json)
    }

    suspend fun saveFull(
        context: Context,
        clientCode: String,
        branchId: Int,
        list: List<DeliveryChallanResponseList>,
    ) = withContext(Dispatchers.IO) {
        val type = object : TypeToken<List<DeliveryChallanResponseList>>() {}.type
        prefs(context).edit()
            .putString(fullKey(clientCode, branchId), gson.toJson(list, type))
            .remove(legacyKey(clientCode, branchId))
            .apply()
    }

    private fun migrateLegacyRows(
        context: Context,
        clientCode: String,
        branchId: Int,
    ): String? {
        val legacyJson = prefs(context).getString(legacyKey(clientCode, branchId), null) ?: return null
        val fullList = parseFull(legacyJson)
        if (fullList.isEmpty()) return null
        val rows = fullList.map { it.toListRow() }
        val rowsType = object : TypeToken<List<DeliveryChallanListRow>>() {}.type
        prefs(context).edit()
            .putString(rowsKey(clientCode, branchId), gson.toJson(rows, rowsType))
            .putString(fullKey(clientCode, branchId), legacyJson)
            .remove(legacyKey(clientCode, branchId))
            .apply()
        return prefs(context).getString(rowsKey(clientCode, branchId), null)
    }

    private fun parseRows(json: String): List<DeliveryChallanListRow> = runCatching {
        val type = object : TypeToken<List<DeliveryChallanListRow>>() {}.type
        gson.fromJson<List<DeliveryChallanListRow>>(json, type).orEmpty()
    }.getOrDefault(emptyList())

    private fun parseFull(json: String): List<DeliveryChallanResponseList> = runCatching {
        val type = object : TypeToken<List<DeliveryChallanResponseList>>() {}.type
        gson.fromJson<List<DeliveryChallanResponseList>>(json, type).orEmpty()
    }.getOrDefault(emptyList())
}
