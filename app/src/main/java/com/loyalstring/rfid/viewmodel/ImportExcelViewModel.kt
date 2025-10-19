package com.loyalstring.rfid.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.local.entity.EpcDto
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.repository.BulkRepository
import com.loyalstring.rfid.repository.BulkRepositoryImpl
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.opencsv.CSVReader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

data class ImportProgress(
    val totalFields: Int,
    val importedFields: Int,
    val failedFields: List<String>
)

@HiltViewModel
class ImportExcelViewModel @Inject constructor(
    private val bulkRepository: BulkRepository,
    private val repository: BulkRepositoryImpl,
    private val userPreferences: UserPreferences,
) : ViewModel() {

    val allTagsFlow: Flow<List<EpcDto>> = repository.getAllTagsFlow()
    private val _dbRFIDMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val dbRFIDMap: StateFlow<Map<String, String>> = _dbRFIDMap
    
    private var isRFIDMapLoaded = false

    init {
        // Lazy load RFID map - only start collecting when actually needed
        // This prevents blocking the UI on screen navigation
    }
    
    // Call this when RFID map is actually needed (e.g., during import operations)
    fun ensureRFIDMapLoaded() {
        if (!isRFIDMapLoaded) {
            isRFIDMapLoaded = true
            viewModelScope.launch(Dispatchers.IO) {
                allTagsFlow.collect { tags ->
                    _dbRFIDMap.value = tags.associate { dto ->
                        dto.BarcodeNumber.orEmpty().trim().uppercase() to dto.TidValue.orEmpty().trim().uppercase()
                    }
                }
            }
        }
    }

    private val _importProgress = MutableStateFlow(
        ImportProgress(0, 0, emptyList())
    )
    val importProgress: StateFlow<ImportProgress> = _importProgress
    private val _syncStatusText = MutableStateFlow("")
    val syncStatusText: StateFlow<String> = _syncStatusText

    private val _isImportDone = MutableStateFlow(false)
    val isImportDone: StateFlow<Boolean> = _isImportDone

    private var selectedUri: Uri? = null
    private var syncedRFIDMap: Map<String, String>? = null


    fun setSelectedFile(uri: Uri) {
        selectedUri = uri
    }

    fun parseExcelHeaders(context: Context, uri: Uri): List<String> {
        return try {
            context.contentResolver.openInputStream(uri).use { inputStream ->
                val workbook = WorkbookFactory.create(inputStream)
                val sheet = workbook.getSheetAt(0)
                val headerRow = sheet.getRow(0)
                if (headerRow == null) {
                    emptyList()
                } else {
                    (0 until headerRow.lastCellNum).map { colIndex ->
                        val cell = headerRow.getCell(colIndex)
                        getCellValue(cell).trim()  // <- reuse your existing getCellValue
                    }.filter { it.isNotBlank() }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }


    // ✅ Excel Import
    fun importMappedData(context: Context, fieldMapping: Map<String, String>) {
        val uri = selectedUri ?: return
        ensureRFIDMapLoaded() // Load RFID map only when import is triggered
        viewModelScope.launch(Dispatchers.IO) {
            val failed = mutableListOf<String>()
            var imported = 0
            var total = 0

            try {
                context.contentResolver.openInputStream(uri).use { inputStream ->
                    val workbook = WorkbookFactory.create(inputStream)
                    val sheet = workbook.getSheetAt(0)

                    val headerRow = sheet.getRow(0)
                    val rawHeaderIndexMap = mutableMapOf<String, Int>()
                    for (cell in headerRow) {
                        val name = getCellValue(cell).trim().lowercase()
                        rawHeaderIndexMap[name] = cell.columnIndex
                    }

                    val normalizedFieldMapping = fieldMapping
                        .mapKeys { it.key.trim().lowercase() }
                        .mapValues { it.value.trim().lowercase() }

                    val items = mutableListOf<BulkItem>()
                    total = sheet.lastRowNum

                    for (i in 1..sheet.lastRowNum) {
                        val row = sheet.getRow(i) ?: continue

                        val rfid =
                            getStringFromRow(row, rawHeaderIndexMap, normalizedFieldMapping["rfid"])
                        var epcVal = getStringFromRow(
                            row,
                            rawHeaderIndexMap,
                            normalizedFieldMapping["epc"]
                        ).trim()

                        // EPC not compulsory
                        if (epcVal.isBlank()) {
                            epcVal = syncAndMapRow(rfid)
                        }
                        if (epcVal.isBlank()) {
                            epcVal = "TEMP-${System.currentTimeMillis()}-${i}"
                        }

                        try {
                            val item = createBulkItemFromRow(
                                row,
                                rawHeaderIndexMap,
                                normalizedFieldMapping,
                                epcVal,
                                rfid
                            )
                            items.add(item)
                            imported++
                        } catch (e: Exception) {
                            failed.add("Row ${i + 1}")
                        }

                        _importProgress.value = ImportProgress(total, imported, failed.toList())
                    }

                    bulkRepository.clearAllItems()
                    bulkRepository.insertBulkItems(items)
                    _isImportDone.value = true
                    delay(200)
                }
            } catch (e: Exception) {
                Log.e("ImportExcel", "Excel import failed", e)
                _isImportDone.value = true
            }
        }
    }

    // ✅ Google Sheet Import
// ✅ Google Sheet Import
    fun importMappedDataFromSheet(
        sheetUrl: String,
        fieldMapping: Map<String, String>
    ) {
        ensureRFIDMapLoaded() // Load RFID map only when import is triggered
        viewModelScope.launch(Dispatchers.IO) {
            val failed = mutableListOf<String>()
            var imported = 0

            try {
                val rows = parseGoogleSheetRows(sheetUrl)
                if (rows.isEmpty()) {
                    Log.e("ImportSheet", "No rows parsed from sheet!")
                    _isImportDone.value = true
                    return@launch
                }

                val items = mutableListOf<BulkItem>()
                val total = rows.size

                for ((index, row) in rows.withIndex()) {
                    val rfid = row[fieldMapping["rfid"]].orEmpty()
                    var epcVal = row[fieldMapping["epc"]].orEmpty().trim()

                    if (epcVal.isBlank()) {
                        epcVal = syncAndMapRow(rfid)
                    }
                    if (epcVal.isBlank()) {
                        epcVal = "TEMP-${System.currentTimeMillis()}-${index}"
                    }

                    try {
                        val item = BulkItem(
                            id = 0,
                            productName = row[fieldMapping["productName"]],
                            itemCode = row[fieldMapping["itemCode"]],
                            rfid = rfid,
                            grossWeight = row[fieldMapping["grossWeight"]],
                            stoneWeight = row[fieldMapping["stoneWeight"]],
                            diamondWeight = row[fieldMapping["diamondWeight"]],
                            netWeight = row[fieldMapping["netWeight"]],
                            category = row[fieldMapping["category"]],
                            design = row[fieldMapping["design"]],
                            purity = row[fieldMapping["purity"]],
                            makingPerGram = row[fieldMapping["makingPerGram"]],
                            makingPercent = row[fieldMapping["makingPercent"]],
                            fixMaking = row[fieldMapping["fixMaking"]],
                            fixWastage = row[fieldMapping["fixWastage"]],
                            stoneAmount = row[fieldMapping["stoneAmount"]],
                            diamondAmount = row[fieldMapping["diamondAmount"]],
                            sku = row[fieldMapping["sku"]],
                            epc = epcVal,
                            vendor = row[fieldMapping["vendor"]],
                            tid = epcVal,
                            box = row[fieldMapping["box"]],
                            designCode = row[fieldMapping["designCode"]],
                            productCode = row[fieldMapping["productCode"]],
                            imageUrl = "",
                            totalQty = 0,
                            pcs = 0,
                            matchedPcs = 0,
                            totalGwt = 0.0,
                            matchGwt = 0.0,
                            totalStoneWt = 0.0,
                            matchStoneWt = 0.0,
                            totalNetWt = 0.0,
                            matchNetWt = 0.0,
                            unmatchedQty = 0,
                            matchedQty = 0,
                            unmatchedGrossWt = 0.0,
                            mrp = 0.0,
                            counterName = row[fieldMapping["counterName"]],
                            counterId = 0,
                            scannedStatus = "",
                            boxId = 0,
                            boxName = row[fieldMapping["boxName"]],
                            branchId = 0,
                            branchName = row[fieldMapping["branchName"]],
                            categoryId = 0,
                            productId = 0,
                            designId = 0,
                            packetId = 0,
                            packetName = "",
                            branchType = ""
                        )
                        items.add(item)
                        imported++
                    } catch (e: Exception) {
                        failed.add("Row ${index + 1}")
                    }

                    _importProgress.value = ImportProgress(total, imported, failed.toList())


                }

                bulkRepository.clearAllItems()
                bulkRepository.insertBulkItems(items)


                delay(200) // give UI time to catch progress
                _isImportDone.value = true
                Log.e("ImportSheet", "Successfully imported sheet!")
                _syncStatusText.value = "✅ Google Sheet import completed successfully!"

            } catch (e: Exception) {
                Log.e("ImportSheet", "Sheet import failed", e)
                _isImportDone.value = true
            }
        }
    }

    fun resetImportState() {
        _isImportDone.value = false
        _importProgress.value = ImportProgress(0, 0, emptyList())

    }

    // ✅ Parse Google Sheet CSV
    suspend fun parseGoogleSheetRows(sheetUrl: String): List<Map<String, String>> {
        return try {
            val url = URL(sheetUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connect()

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val csvReader = CSVReader(reader)

            val allRows = csvReader.readAll()
            if (allRows.isEmpty()) return emptyList()

            val headers = allRows.first().map { it.trim() }
            allRows.drop(1).mapNotNull { row ->
                if (row.isEmpty()) null else headers.zip(row).toMap()
            }
        } catch (e: Exception) {
            Log.e("ImportVM", "Failed to parse sheet rows", e)
            emptyList()
        }
    }


    private fun createBulkItemFromRow(
        row: org.apache.poi.ss.usermodel.Row,
        rawHeaderIndexMap: Map<String, Int>,
        normalizedFieldMapping: Map<String, String>,
        epcVal: String,
        rfid: String?
    ): BulkItem {
        return BulkItem(
            id = 0,
            productName = getStringFromRow(
                row,
                rawHeaderIndexMap,
                normalizedFieldMapping["productname"]
            ),
            itemCode = getStringFromRow(row, rawHeaderIndexMap, normalizedFieldMapping["itemcode"]),
            rfid = rfid,
            netWeight = getStringFromRow(
                row,
                rawHeaderIndexMap,
                normalizedFieldMapping["netweight"]
            ),
            category = getStringFromRow(row, rawHeaderIndexMap, normalizedFieldMapping["category"]),
            purity = getStringFromRow(row, rawHeaderIndexMap, normalizedFieldMapping["purity"]),
            design = getStringFromRow(row, rawHeaderIndexMap, normalizedFieldMapping["design"]),
            grossWeight = getStringFromRow(
                row,
                rawHeaderIndexMap,
                normalizedFieldMapping["grossweight"]
            ),
            stoneWeight = getStringFromRow(
                row,
                rawHeaderIndexMap,
                normalizedFieldMapping["stoneweight"]
            ),
            makingPerGram = getStringFromRow(
                row,
                rawHeaderIndexMap,
                normalizedFieldMapping["makingpergram"]
            ),
            makingPercent = getStringFromRow(
                row,
                rawHeaderIndexMap,
                normalizedFieldMapping["makingpercent"]
            ),
            fixMaking = getStringFromRow(
                row,
                rawHeaderIndexMap,
                normalizedFieldMapping["fixmaking"]
            ),
            fixWastage = getStringFromRow(
                row,
                rawHeaderIndexMap,
                normalizedFieldMapping["fixwastage"]
            ),
            stoneAmount = getStringFromRow(
                row,
                rawHeaderIndexMap,
                normalizedFieldMapping["stoneamount"]
            ),
            counterName = getStringFromRow(
                row,
                rawHeaderIndexMap,
                normalizedFieldMapping["countername"]
            ),
            branchName = getStringFromRow(
                row,
                rawHeaderIndexMap,
                normalizedFieldMapping["branchname"]
            ),
            diamondWeight = "",
            diamondAmount = "",
            sku = getStringFromRow(
                row,
                rawHeaderIndexMap,
                normalizedFieldMapping["sku"]
            ),
            epc = epcVal,
            vendor = getStringFromRow(
                row,
                rawHeaderIndexMap,
                normalizedFieldMapping["vendor"]
            ),
            tid = epcVal,
            box = "",
            designCode = "",
            productCode = "",
            imageUrl = "",
            totalQty = 0,
            pcs = 0,
            matchedPcs = 0,
            totalGwt = 0.0,
            matchGwt = 0.0,
            totalStoneWt = 0.0,
            matchStoneWt = 0.0,
            totalNetWt = 0.0,
            matchNetWt = 0.0,
            unmatchedQty = 0,
            matchedQty = 0,
            unmatchedGrossWt = 0.0,
            mrp = 0.0,
            counterId = 0,
            scannedStatus = "",
            boxId = 0,
            boxName = "",
            branchId = 0,
            categoryId = 0,
            productId = 0,
            designId = 0,
            packetId = 0,
            packetName = "",
            branchType = ""
        )
    }

  /*  fun syncAndMapRow(itemCode: String): String {
        return syncedRFIDMap?.get(itemCode) ?: ""
    }*/

    fun syncAndMapRow(itemCode: String): String {
        val key = itemCode.trim().uppercase()
        return syncedRFIDMap?.get(key)
            ?: dbRFIDMap.value[key]
            ?: ""
    }

    suspend fun syncRFIDDataIfNeeded(context: Context) {
        if (syncedRFIDMap != null) return // Already synced

        val employee = userPreferences.getEmployee(Employee::class.java)
        val clientCode = employee?.clientCode ?: return

        val response = bulkRepository.syncRFIDItemsFromServer(ClientCodeRequest(clientCode))
        bulkRepository.insertRFIDTags(response)

        // Cache mapping in memory (itemCode -> EPC)
        syncedRFIDMap = response.associateBy(
            { it.BarcodeNumber },
            { it.TidValue }
        )
    }

    private fun getCellValue(cell: org.apache.poi.ss.usermodel.Cell?): String {
        if (cell == null) return ""
        return when (cell.cellType) {
            org.apache.poi.ss.usermodel.CellType.STRING -> cell.stringCellValue
            org.apache.poi.ss.usermodel.CellType.NUMERIC -> cell.numericCellValue.toString()
            org.apache.poi.ss.usermodel.CellType.BOOLEAN -> cell.booleanCellValue.toString()
            else -> ""
        }
    }
    private fun getStringFromRow(
        row: org.apache.poi.ss.usermodel.Row,
        headerIndexMap: Map<String, Int>,
        columnName: String?
    ): String {
        if (columnName.isNullOrBlank()) return ""
        val index = headerIndexMap[columnName.lowercase()] ?: return ""
        val cell = row.getCell(index) ?: return ""
        return getCellValue(cell).trim()
    }



}
