package com.loyalstring.rfid.ui.utils

import android.content.Context
import com.loyalstring.rfid.data.local.dao.BulkItemDao
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.model.sampleOut.IssueItemDto
import com.loyalstring.rfid.data.model.sampleOut.SampleOutListResponse
import com.loyalstring.rfid.data.model.sampleOut.SampleOutPrintData
import com.loyalstring.rfid.data.model.sampleOut.SampleOutPrintItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Toggle Sample Out item images in list (eye button) and PDF (View column).
 * Set to `true` when ready to re-enable.
 */
const val SAMPLE_OUT_ITEM_IMAGES_ENABLED = false

data class SampleOutItemImageUi(
    val title: String,
    val imageUrl: String?,
    val itemCode: String?,
    val designName: String?,
)

private fun normalizeCode(value: String?): String? =
    value?.trim()?.takeIf { it.isNotBlank() }

private fun buildBulkItemCodeMap(
    dbItems: List<BulkItem>,
    cachedBulkItems: List<BulkItem>,
): Map<String, BulkItem> {
    val map = linkedMapOf<String, BulkItem>()

    fun putBulk(bulk: BulkItem) {
        val code = normalizeCode(bulk.itemCode) ?: return
        val key = code.lowercase()
        val existing = map[key]
        when {
            existing == null -> map[key] = bulk
            existing.imageUrl.isNullOrBlank() && !bulk.imageUrl.isNullOrBlank() -> map[key] = bulk
        }
    }

    dbItems.forEach { putBulk(it) }
    cachedBulkItems.forEach { putBulk(it) }
    return map
}

suspend fun resolveBulkItemForIssue(
    issueItem: IssueItemDto,
    itemCodeMap: Map<String, BulkItem>,
    bulkItemDao: BulkItemDao?,
): BulkItem? {
    normalizeCode(issueItem.ItemCode)?.let { itemCode ->
        itemCodeMap[itemCode.lowercase()]?.let { return it }
        bulkItemDao?.getItemByItemCode(itemCode)?.let { return it }
        bulkItemDao?.getItemByItemCodeOrRfid(itemCode)?.let { return it }
    }

    if (issueItem.LabelledStockId > 0) {
        bulkItemDao?.getById(issueItem.LabelledStockId)?.let { return it }
    }

    listOfNotNull(issueItem.RFIDCode, issueItem.TIDNumber, issueItem.SKU)
        .mapNotNull { normalizeCode(it) }
        .distinct()
        .forEach { code ->
            bulkItemDao?.getItemByItemCodeOrRfid(code)?.let { return it }
            bulkItemDao?.getItemByEpc(code)?.let { return it }
        }

    return null
}

fun resolveSampleOutItemImageFields(
    context: Context,
    bulkItem: BulkItem?,
    issueItem: IssueItemDto,
): Triple<String?, String?, String?> {
    val itemCode = normalizeCode(bulkItem?.itemCode) ?: normalizeCode(issueItem.ItemCode)
    val designName = normalizeCode(bulkItem?.design) ?: normalizeCode(issueItem.DesignName)

    val imageValue = bulkItem?.imageUrl?.takeIf { it.isNotBlank() }
        ?: issueItem.Image?.takeIf { it.isNotBlank() }
        ?: issueItem.Images?.takeIf { it.isNotBlank() }
        ?: itemCode?.let { SampleOutImageCache.get(context, it) }

    if (!imageValue.isNullOrBlank() && !itemCode.isNullOrBlank()) {
        SampleOutImageCache.save(context, itemCode, imageValue)
    }

    return Triple(imageValue, itemCode, designName)
}

private fun extractImageUrlCandidates(imageUrl: String?): List<String> {
    if (imageUrl.isNullOrBlank()) return emptyList()
    return imageUrl
        .trim()
        .trimEnd(',')
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .flatMap { part ->
            listOfNotNull(
                resolveProductImageUrl(part),
                part.takeIf {
                    it.startsWith("http://", ignoreCase = true) ||
                        it.startsWith("https://", ignoreCase = true)
                },
            )
        }
        .distinctBy { it.lowercase() }
}

suspend fun loadImageBytesFromUrl(urlString: String): ByteArray? = withContext(Dispatchers.IO) {
    if (urlString.isBlank()) return@withContext null
    try {
        val connection = URL(urlString).openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.doInput = true
        connection.requestMethod = "GET"
        connection.connect()
        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            connection.disconnect()
            return@withContext null
        }
        connection.inputStream.use { it.readBytes() }.also {
            connection.disconnect()
        }
    } catch (_: Exception) {
        null
    }
}

suspend fun loadProductImageBytes(
    context: Context,
    imageUrl: String?,
    itemCode: String?,
    designName: String?,
): ByteArray? = withContext(Dispatchers.IO) {
    extractImageUrlCandidates(imageUrl).forEach { url ->
        loadImageBytesFromUrl(url)?.let { return@withContext it }
    }

    val customApiUrl = UserPreferences.getInstance(context).getCustomApi()
    val candidates = buildProductImageLoadCandidates(
        context = context,
        imageUrl = imageUrl,
        itemCode = itemCode,
        designName = designName,
        customApiUrl = customApiUrl,
    )

    for (candidate in candidates) {
        when (candidate) {
            is File -> {
                if (candidate.exists() && candidate.isFile) {
                    runCatching { candidate.readBytes() }.getOrNull()?.let { return@withContext it }
                }
            }

            is String -> {
                loadImageBytesFromUrl(candidate)?.let { return@withContext it }
            }
        }
    }
    null
}

private suspend fun buildSampleOutLookup(
    bulkItemDao: BulkItemDao?,
    cachedBulkItems: List<BulkItem>,
): Pair<Map<String, BulkItem>, BulkItemDao?> {
    val dbItems = bulkItemDao?.getAllBulkItemsOnce().orEmpty()
    val itemCodeMap = buildBulkItemCodeMap(dbItems, cachedBulkItems)
    return itemCodeMap to bulkItemDao
}

private fun buildSampleOutItemTitle(issueItem: IssueItemDto, itemCode: String?): String {
    return listOfNotNull(
        issueItem.CategoryName,
        issueItem.ProductName,
        issueItem.DesignName,
        issueItem.PurityName,
        itemCode,
    )
        .filter { it.isNotBlank() }
        .joinToString(" - ")
}

suspend fun buildSampleOutItemImages(
    context: Context,
    bulkItemDao: BulkItemDao?,
    challan: SampleOutListResponse,
    cachedBulkItems: List<BulkItem> = emptyList(),
): List<SampleOutItemImageUi> {
    val (itemCodeMap, dao) = buildSampleOutLookup(bulkItemDao, cachedBulkItems)

    return (challan.IssueItems ?: emptyList()).map { issueItem ->
        val bulkItem = resolveBulkItemForIssue(issueItem, itemCodeMap, dao)
        val (imageUrl, itemCode, designName) = resolveSampleOutItemImageFields(context, bulkItem, issueItem)

        SampleOutItemImageUi(
            title = buildSampleOutItemTitle(issueItem, itemCode),
            imageUrl = imageUrl,
            itemCode = itemCode,
            designName = designName,
        )
    }
}

private fun formatSampleOutCreatedOn(createdOn: String?): String {
    if (createdOn.isNullOrBlank()) return ""
    return try {
        val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val output = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val date = input.parse(createdOn)
        if (date != null) output.format(date) else createdOn
    } catch (_: Exception) {
        createdOn
    }
}

private suspend fun buildPrintItem(
    context: Context,
    issueItem: IssueItemDto,
    itemCodeMap: Map<String, BulkItem>,
    bulkItemDao: BulkItemDao?,
): SampleOutPrintItem {
    val bulkItem = resolveBulkItemForIssue(issueItem, itemCodeMap, bulkItemDao)
    val (imageUrl, itemCode, designName) = resolveSampleOutItemImageFields(context, bulkItem, issueItem)

    return SampleOutPrintItem(
        itemDetails = listOfNotNull(
            issueItem.CategoryName,
            issueItem.ProductName,
            issueItem.DesignName,
            issueItem.PurityName
        )
            .filter { it.isNotBlank() }
            .joinToString(" - "),
        grossWt = issueItem.GrossWt ?: "0.000",
        stoneWt = issueItem.StoneWeight ?: "0.000",
        diamondWt = issueItem.DiamondWeight ?: "0.000",
        netWt = issueItem.NetWt ?: "0.000",
        pieces = issueItem.Pieces ?: "1",
        status = "Sample Out",
        imageUrl = imageUrl,
        itemCode = itemCode,
        designName = designName,
    )
}

suspend fun SampleOutListResponse.toSampleOutPrintData(
    context: Context,
    bulkItemDao: BulkItemDao?,
    cachedBulkItems: List<BulkItem> = emptyList(),
): SampleOutPrintData {
    val org = UserPreferences.getInstance(context).getOrganization()
    val companyName = org?.toString().orEmpty()
    val (itemCodeMap, dao) = buildSampleOutLookup(bulkItemDao, cachedBulkItems)

    val items = (this.IssueItems ?: emptyList()).map { issueItem ->
        buildPrintItem(context, issueItem, itemCodeMap, dao)
    }

    return SampleOutPrintData(
        companyName = companyName,
        customerName = listOfNotNull(this.Customer?.FirstName, this.Customer?.LastName)
            .joinToString(" ")
            .trim(),
        addressCity = this.Customer?.CurrAddTown.orEmpty(),
        contactNo = this.Customer?.Mobile.orEmpty(),
        sampleOutNo = this.SampleOutNo.orEmpty(),
        date = formatSampleOutCreatedOn(this.CreatedOn),
        returnDate = this.ReturnDate.orEmpty(),
        items = items,
    )
}
