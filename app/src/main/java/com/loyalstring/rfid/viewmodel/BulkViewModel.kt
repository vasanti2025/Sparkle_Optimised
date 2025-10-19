package com.loyalstring.rfid.viewmodel

import ScannedDataToService
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loyalstring.rfid.data.local.dao.BulkItemDao
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.local.entity.EpcDto
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.ScannedItem
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.reader.BarcodeReader
import com.loyalstring.rfid.data.reader.RFIDReaderManager
import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import com.loyalstring.rfid.repository.BulkRepositoryImpl
import com.loyalstring.rfid.repository.DropdownRepository
import com.loyalstring.rfid.ui.utils.ToastUtils
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.toBulkItem
import com.rscja.deviceapi.entity.UHFTAGInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class BulkViewModel @Inject constructor(
    private val readerManager: RFIDReaderManager,
    internal val barcodeReader: BarcodeReader,
    private val repository: DropdownRepository,
    private val bulkItemDao: BulkItemDao,
    private val bulkRepository: BulkRepositoryImpl,
    private val userPreferences: UserPreferences,
    private val apiService: RetrofitInterface
) : ViewModel() {

    //private val success = readerManager.initReader()
    private var readerReady = false

    private suspend fun ensureReader(): Boolean = withContext(Dispatchers.IO) {
        if (!readerReady) {
            readerReady = readerManager.initReader()
        }
        readerReady
    }
    private val barcodeDecoder = barcodeReader.barcodeDecoder

    private val _scannedTags = MutableStateFlow<List<UHFTAGInfo>>(emptyList())
    val scannedTags: StateFlow<List<UHFTAGInfo>> = _scannedTags

    private val _scannedItems = MutableStateFlow<List<ScannedItem>>(emptyList())
    val scannedItems: StateFlow<List<ScannedItem>> = _scannedItems

    private val _rfidMap = MutableStateFlow<Map<Int, String>>(emptyMap())
    val rfidMap: StateFlow<Map<Int, String>> = _rfidMap


    val employee: Employee? = userPreferences.getEmployee(Employee::class.java)

    val categories =
        repository.categories.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val products = repository.products.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val designs = repository.designs.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _syncProgress = MutableStateFlow(0f) // 0.0 to 1.0
    val syncProgress: StateFlow<Float> = _syncProgress

    private val _syncStatusText = MutableStateFlow("")
    val syncStatusText: StateFlow<String> = _syncStatusText

    private val _syncCompleted = MutableStateFlow(false)
    var syncCompleted: StateFlow<Boolean> = _syncCompleted

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting

    private val _exportStatus = MutableStateFlow("")
    val exportStatus: StateFlow<String> = _exportStatus

    private val _reloadTrigger = MutableStateFlow(false)
    val reloadTrigger = _reloadTrigger.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage = _toastMessage

    private val existingTags = mutableListOf<UHFTAGInfo>()
    private val duplicateTags = mutableListOf<UHFTAGInfo>()

    private val _allScannedTags = mutableStateOf<List<UHFTAGInfo>>(emptyList())
    val allScannedTags: State<List<UHFTAGInfo>> = _allScannedTags

    private val _existingItems = mutableStateOf<List<UHFTAGInfo>>(emptyList())
    val existingItems: State<List<UHFTAGInfo>> = _existingItems

    private val _duplicateItems = mutableStateOf<List<UHFTAGInfo>>(emptyList())
    val duplicateItems: State<List<UHFTAGInfo>> = _duplicateItems
    val rfidInput = mutableStateOf("")

    val scannedEpcList = mutableStateListOf<String>()

    private val _matchedItems = mutableStateListOf<BulkItem>()
    val matchedItems: List<BulkItem> get() = _matchedItems

    private val _unmatchedItems = mutableStateListOf<BulkItem>() // real unmatched items
    val unmatchedItems: List<BulkItem> = _unmatchedItems

    // 👇 NEW: what the UI uses to render
    private val _visibleUnmatchedItems = mutableStateListOf<BulkItem>()
    val visibleUnmatchedItems: List<BulkItem> = _visibleUnmatchedItems

    private val _scannedFilteredItems = mutableStateOf<List<BulkItem>>(emptyList())
    val scannedFilteredItems: State<List<BulkItem>> = _scannedFilteredItems

    // ✅ New: normalized EPCs/TIDs present in the current scope
    private var filteredDbEpcSet: Set<String> = emptySet()
    // private var filteredDbTidSet: Set<String> = emptySet() // TID matching disabled

    // ✅ New: matched EPCs/TIDs sets to drive UI without remapping the whole list
    private val _matchedEpcSet = MutableStateFlow<Set<String>>(emptySet())
    val matchedEpcSet: StateFlow<Set<String>> = _matchedEpcSet
    // private val _matchedTidSet = MutableStateFlow<Set<String>>(emptySet()) // TID matching disabled
    // val matchedTidSet: StateFlow<Set<String>> = _matchedTidSet

    private var _filteredSource: List<BulkItem> = emptyList()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    private val _counters = MutableStateFlow<List<String>>(emptyList())
    val counters: StateFlow<List<String>> = _counters

    private val _branches = MutableStateFlow<List<String>>(emptyList())
    val branches: StateFlow<List<String>> = _branches

    private val _boxes = MutableStateFlow<List<String>>(emptyList())
    val boxes: StateFlow<List<String>> = _boxes

    private val _exhibitions = MutableStateFlow<List<String>>(emptyList())
    val exhibitions: StateFlow<List<String>> = _exhibitions

    private var syncedRFIDMap: Map<String, String>? = null



    // ✅ function to update value at a specific index
   fun updateRfidForIndex(index: Int, newValue: String) {
        _rfidMap.value = _rfidMap.value.toMutableMap().apply {
            this[index] = newValue
        }
    }



    fun preloadFilters(allItems: List<BulkItem>) {
        viewModelScope.launch(Dispatchers.Default) {
            // Process data on background thread
            val counters = allItems.mapNotNull { it.counterName?.takeIf { it.isNotBlank() } }.distinct()
            val branches = allItems.mapNotNull { it.branchName?.takeIf { it.isNotBlank() } }.distinct()
            val boxes = allItems.mapNotNull { it.boxName?.takeIf { it.isNotBlank() } }.distinct()
            val exhibitions = allItems
                .filter { it.branchType?.equals("Exhibition", ignoreCase = true) == true }
                .mapNotNull { it.branchName }
                .distinct()
            
            // Update StateFlows on main thread
            withContext(Dispatchers.Main) {
                _counters.value = counters
                _branches.value = branches
                _boxes.value = boxes
                _exhibitions.value = exhibitions
            }
        }
    }


    fun setSyncCompleted() {
        _syncStatusText.value = "completed"
    }

    // ← the function your UI calls
    fun clearSyncStatus() {
        _syncStatusText.value = ""
    }

    fun setFilteredItems(filtered: List<BulkItem>) {
        _filteredSource = if (filtered.isEmpty()) _allItems else filtered
        // Precompute normalized EPC set for O(1) membership checks
        filteredDbEpcSet = _filteredSource.mapNotNull { it.epc?.trim()?.uppercase() }.toHashSet()
        // filteredDbTidSet = _filteredSource.mapNotNull { it.tid?.trim()?.uppercase() }.toHashSet() // TID matching disabled
    }


    private var scanJob: Job? = null

    private val _scanTrigger = MutableStateFlow<String?>(null)
    val scanTrigger: StateFlow<String?> = _scanTrigger

    private val _searchItems = mutableStateListOf<BulkItem>()
    val searchItems: SnapshotStateList<BulkItem> get() = _searchItems

    private var _allItems: List<BulkItem> = emptyList()
    val allItems: List<BulkItem> get() = _allItems

    private val _filteredItems = mutableStateListOf<BulkItem>()
    val filteredItems: List<BulkItem> get() = _filteredItems

    private val _stickyUnmatchedIds = mutableStateListOf<String>()
    val stickyUnmatchedIds: List<String> get() = _stickyUnmatchedIds

    fun rememberUnmatched(items: List<BulkItem>) {
        val ids = items.mapNotNull { it.epc?.trim()?.uppercase() }
        _stickyUnmatchedIds.addAll(ids.filterNot { _stickyUnmatchedIds.contains(it) })
    }

    fun clearStickyUnmatched() {
        _stickyUnmatchedIds.clear()
    }

    private var isDataLoaded = false

    init {
        // Lazy load data only when needed instead of immediately
        viewModelScope.launch {
            bulkRepository.getAllBulkItems().collect { items ->
                _allItems = items
                if (isDataLoaded) {
                    // Only preload filters after first load to avoid blocking initial composition
                    preloadFilters(_allItems)
                }
                _scannedFilteredItems.value = items
                isDataLoaded = true
            }
        }
    }
    
    // Call this method when user actually needs the data (e.g., when navigating to list screen)
    fun ensureFiltersLoaded() {
        if (!isDataLoaded && _allItems.isNotEmpty()) {
            preloadFilters(_allItems)
            isDataLoaded = true
        }
    }
    fun toggleScanningInventory(selectedPower: Int) {
        if (_isScanning.value) {
            stopScanningAndCompute()
            _isScanning.value = false
            Log.d("RFID", "Scanning stopped by toggle")
        } else {
            _isScanning.value = true
            resetScanResults()  // 🔑 Always reset before scanning
            setFilteredItems(_allItems)
            startScanningInventory(selectedPower)
            Log.d("RFID", "Scanning started by toggle")
        }
    }


    fun toggleScanning(selectedPower: Int) {
        if (_isScanning.value) {
            stopScanning()
            _isScanning.value = false
            Log.d("RFID", "Scanning stopped by toggle")
        } else {
           // resetScanResults()
           // setFilteredItems(_allItems) // or _filteredSource depending on scope
            startScanning(selectedPower)
            _isScanning.value = true
            Log.d("RFID", "Scanning started by toggle")
        }
    }






    fun onScanKeyPressed(type: String) {
        _scanTrigger.value = type
    }

    fun clearScanTrigger() {
        _scanTrigger.value = null
    }

    fun startSearch(items: List<BulkItem>) {
        _searchItems.clear()
        _searchItems.addAll(items.filter { it.scannedStatus == "Unmatched" })
    }
    fun showUnmatchedTab() {
        _visibleUnmatchedItems.clear()
        _visibleUnmatchedItems.addAll(_unmatchedItems) // only real unmatched
    }

    fun startSingleScan(selectedPower: Int) {
        //if (!success) return
        scanJob?.cancel()

        scanJob = viewModelScope.launch(Dispatchers.IO) {
            if (!ensureReader()) return@launch
            readerManager.startInventoryTag(selectedPower, false)

            val timeoutMillis = 2000L
            val startTime = System.currentTimeMillis()
            var foundTag: UHFTAGInfo? = null

            while (isActive && (System.currentTimeMillis() - startTime < timeoutMillis)) {
                val tag = readerManager.readTagFromBuffer()
                if (tag != null && !tag.epc.isNullOrBlank()) {
                    foundTag = tag
                    break
                } else {
                    delay(100)
                }
            }

            readerManager.stopInventory()

            foundTag?.let {
                handleScannedTag(it)   // ✅ adds to the same lists as bulk
                readerManager.playSound(1)
            }
        }
    }


    suspend fun scanSingleTagRaw(
        selectedPower: Int,
        onResult: (String?) -> Unit
    ) {
        //if (!success) {
        if (!ensureReader()) {
            onResult(null)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            readerManager.startInventoryTag(selectedPower, false)
            val timeoutMillis = 2000L
            val startTime = System.currentTimeMillis()
            var epc: String? = null

            while (isActive && (System.currentTimeMillis() - startTime < timeoutMillis)) {
                val tag = readerManager.readTagFromBuffer()
                if (tag != null && !tag.epc.isNullOrBlank()) {
                    epc = tag.epc
                    break
                } else delay(100)
            }

            readerManager.stopInventory()

            withContext(Dispatchers.Main) {
                onResult(epc)
            }
        }
    }


    fun startScanningInventory(selectedPower: Int) {
        //if (!success || _isScanning.value) return
        scanJob?.cancel()
        _isScanning.value = true
        viewModelScope.launch(Dispatchers.IO) {
            if (!ensureReader()) {
                _isScanning.value = false
                return@launch
            }


        readerManager.startInventoryTag(selectedPower, false)
        readerManager.playSound(1)

        // Build EPC set if not already prepared
        // Build EPC set if not already prepared
        if (filteredDbEpcSet.isEmpty()) {
            filteredDbEpcSet = _filteredSource.mapNotNull { it.epc?.trim()?.uppercase() }.toHashSet()
            // filteredDbTidSet = _filteredSource.mapNotNull { it.tid?.trim()?.uppercase() }.toHashSet() // TID matching disabled
        }

        // Loop: only update matched set; avoid remapping list on main thread per tag
        scanJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val tag = readerManager.readTagFromBuffer()
                if (tag != null) {
                    val scannedEpc = tag.epc?.trim()?.uppercase()
                    // val scannedTid = tag.tid?.trim()?.uppercase() // TID matching disabled
                    // Track seen EPCs to avoid repeated processing
                    if (!scannedEpc.isNullOrBlank()) {
                        scannedEpcList.add(scannedEpc)
                    }

                    // EPC match
                    if (!scannedEpc.isNullOrBlank() && filteredDbEpcSet.contains(scannedEpc)) {
                        val currentE = _matchedEpcSet.value
                        if (!currentE.contains(scannedEpc)) {
                            _matchedEpcSet.value = currentE + scannedEpc
                        }
                    }

                    // TID match (disabled)
                    // if (!scannedTid.isNullOrBlank() && filteredDbTidSet.contains(scannedTid)) {
                    //     val currentT = _matchedTidSet.value
                    //     if (!currentT.contains(scannedTid)) {
                    //         _matchedTidSet.value = currentT + scannedTid
                    //     }
                    // }
                }
            }
        }
        }
    }

    fun computeScanResults(
        filteredItems: List<BulkItem>,
        stayVisibleInUnmatched: Boolean = false
    ) {
        val matched = mutableListOf<BulkItem>()
        val unmatched = mutableListOf<BulkItem>()
        val scannedEpcSet = scannedEpcList.map { it.trim().uppercase() }.toSet()
        _matchedEpcSet.value = scannedEpcSet
        // Compute matched TIDs too if needed (disabled)
        // val scannedTidSet = _allScannedTags.value.mapNotNull { it.tid?.trim()?.uppercase() }.toSet() // TID matching disabled
        // _matchedTidSet.value = scannedTidSet // TID matching disabled

        filteredItems.forEach { item ->
            val dbEpc = item.epc?.trim()?.uppercase()
            if (dbEpc != null && scannedEpcSet.contains(dbEpc)) {
                val updatedItem = item.copy(scannedStatus = "Matched")
                matched.add(updatedItem)
                if (stayVisibleInUnmatched) {
                    unmatched.add(updatedItem)
                }
            } else {
                val updatedItem = item.copy(scannedStatus = "Unmatched")
                unmatched.add(updatedItem)
            }
        }

        _matchedItems.clear()
        _matchedItems.addAll(matched)

        _unmatchedItems.clear()
        _unmatchedItems.addAll(unmatched)

        // Keep base list stable; do not remap entire list here
        _scannedFilteredItems.value = filteredItems
    }


    fun pauseScanning() {
        readerManager.stopInventory()
        readerManager.stopSound(1)
        _isScanning.value = false
        scanJob?.cancel()
        scanJob = null

        // ❌ DO NOT clear scannedEpcList or recompute here
    }


    fun startScanning(selectedPower: Int) {
        //if (success) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!ensureReader()) {
                Log.e("RFID", "Reader not connected.")
                return@launch
            }
            readerManager.startInventoryTag(selectedPower, false)
            readerManager.playSound(1, 0)
            scanJob?.cancel()
            if (scanJob?.isActive == true) return@launch

            scanJob = viewModelScope.launch(Dispatchers.IO) {

                while (isActive) {
                    val tag = readerManager.readTagFromBuffer()
                    if (tag != null) {
                        val epc = tag.epc ?: continue
                        // Avoid DB calls in the hot path; update UI immediately
                        handleScannedTag(tag)
                    }
                }
            }
        }
//        else {
//            Log.e("RFID", "Reader not connected.")
//            return
//        }
    }

    fun stopScanningAndCompute() {
        stopScanning()
        computeScanResults(_filteredSource)
    }


    fun resetProductScanResults() {
        viewModelScope.launch(Dispatchers.Default) {
            _scannedTags.value = emptyList()
            _scannedItems.value = emptyList()
            _rfidMap.value = emptyMap()
            _allScannedTags.value = emptyList()
            _existingItems.value = emptyList()
            _duplicateItems.value = emptyList()
            _matchedItems.clear()
            _unmatchedItems.clear()
            scannedEpcList.clear()
            _matchedEpcSet.value = emptySet()
            // _matchedTidSet.value = emptySet() // TID matching disabled
            _scannedFilteredItems.value = _filteredSource
        }
    }


    fun resetScanResults() {
        viewModelScope.launch(Dispatchers.Default)  {
            _matchedItems.clear()
            _unmatchedItems.clear()
            scannedEpcList.clear()
            _matchedEpcSet.value = emptySet()
            // _matchedTidSet.value = emptySet() // TID matching disabled
            _scannedFilteredItems.value = _filteredSource
        }
    }

    //    fun scanSingleTagBlocking(onResult: (String?) -> Unit) {
//        viewModelScope.launch(Dispatchers.IO) {
//            val tag = readerManager.inventorySingleTag(se)
//            val epc = tag?.epc ?: ""
//
//            Log.d("RFID", "Blocking scan result: $epc")
//
//            withContext(Dispatchers.Main) {
//                onResult(epc.ifBlank { null })
//            }
//        }
//    }
    fun startBarcodeScanning(context: Context) {
        if (!barcodeDecoder.isOpen) {
            barcodeDecoder.open(context)
        }
        barcodeDecoder.startScan()

    }
    private suspend fun isTagExistsInDatabase(epc: String): Boolean {
        return bulkItemDao.getItemByEpc(epc) != null
    }

    private fun addTagUnique(tag: UHFTAGInfo) {
        val current = _scannedTags.value
        if (current.none { it.epc == tag.epc }) {
            // Defer emitting to reduce recompositions under rapid scans
            pendingTagsBuffer.add(tag)
            schedulePendingFlush()
        }
    }

    // Buffer to accumulate rapid incoming tags and emit in batches
    private val pendingTagsBuffer: MutableList<UHFTAGInfo> = mutableListOf()
    private var flushJob: Job? = null

    private fun schedulePendingFlush() {
        // Emit immediately to avoid visible buffering
        if (pendingTagsBuffer.isEmpty()) return
        val snapshot = pendingTagsBuffer.toList()
        pendingTagsBuffer.clear()
        val existing = _scannedTags.value
        val merged = buildList(existing.size + snapshot.size) {
            addAll(existing)
            snapshot.forEach { t -> if (existing.none { it.epc == t.epc }) add(t) }
        }
        _scannedTags.value = merged
    }

    private fun flushPendingTags() {
        if (pendingTagsBuffer.isEmpty()) return
        val snapshot = pendingTagsBuffer.toList()
        pendingTagsBuffer.clear()
        val existing = _scannedTags.value
        val merged = buildList(existing.size + snapshot.size) {
            addAll(existing)
            snapshot.forEach { t -> if (existing.none { it.epc == t.epc }) add(t) }
        }
        _scannedTags.value = merged
    }

    fun getLocalCounters(): List<String> =
        allItems.mapNotNull { it.counterName?.takeIf { it.isNotBlank() } }.distinct()

    fun getLocalBranches(): List<String> =
        allItems.mapNotNull { it.branchName?.takeIf { it.isNotBlank() } }.distinct()

    fun getLocalBoxes(): List<String> =
        allItems.mapNotNull { it.boxName?.takeIf { it.isNotBlank() } }.distinct()

    fun getLocalExhibitions(): List<String> =
        allItems
            .filter { it.branchType?.equals("Exhibition", ignoreCase = true) == true }
            .mapNotNull { it.branchName } // return the branch names
            .distinct()

    fun setFilteredItemsByType(type: String, value: String) {
        val filtered = when (type) {
            "scan display" -> allItems
            "counter" -> allItems.filter { it.counterName == value }
            "branch" -> allItems.filter { it.branchName == value }
            "box" -> allItems.filter { it.boxName == value }
            "exhibition" -> allItems.filter {
                it.branchName == value && it.branchType.equals(
                    "Exhibition",
                    true
                )
            }

            else -> allItems
        }
        _filteredItems.clear()
        _filteredItems.addAll(filtered)
    }



    fun assignRfidCode(index: Int, rfid: String) {
        val currentMap = _rfidMap.value

        // Skip if already assigned elsewhere
        if (currentMap.containsValue(rfid)) return

        _rfidMap.value = currentMap.toMutableMap().apply {
            put(index, rfid)
        }
    }


    fun onBarcodeScanned(barcode: String) {
        rfidInput.value = barcode
        if (_scannedItems.value.any { it.barcode == barcode }) return

        val nextIndex = _scannedItems.value.size + 1
        val itemCode = generateItemCode(nextIndex)
        val srNo = generateSerialNumber(nextIndex)

        val newItem = ScannedItem(id = srNo, itemCode = itemCode, barcode = barcode)
        _scannedItems.update { it + newItem }
        println("Scanned barcode: $barcode")
    }

    private fun generateItemCode(index: Int): String {
        return "ITEM" + index.toString().padStart(4, '0')
    }

    private fun generateSerialNumber(index: Int): String {
        return index.toString()
    }

    private suspend fun handleScannedTag(tag: UHFTAGInfo) {
        val epc = tag.epc ?: return
        // 1) Update UI list immediately
        addTagUnique(tag)

        // 2) Resolve duplicate/existing info off the critical path
        viewModelScope.launch(Dispatchers.IO) {
            val exists = isTagExistsInDatabase(epc)
            withContext(Dispatchers.Main) {
                val alreadyInExisting = existingTags.any { it.epc == epc }
                val alreadyInScanned = _allScannedTags.value.any { it.epc == epc }
                val alreadyInDuplicates = duplicateTags.any { it.epc == epc }

                if (!alreadyInExisting) {
                    if (alreadyInScanned) {
                        if (!alreadyInDuplicates) {
                            duplicateTags.add(tag)
                            _duplicateItems.value = duplicateTags.toList()
                        }
                    } else {
                        _allScannedTags.value += tag
                        if (exists && !alreadyInDuplicates) {
                            existingTags.add(tag)
                            _existingItems.value = existingTags.toList()
                        }
                    }
                }
                Log.d("RFID", "Processed EPC: $epc")
            }
        }
    }


    fun stopScanning() {
        // Attempt to drain remaining tags from device buffer quickly before stopping
        repeat(25) {
            val tag = readerManager.readTagFromBuffer()
            if (tag != null && !tag.epc.isNullOrBlank()) {
                // Fire-and-forget; UI list updates immediately
                viewModelScope.launch(Dispatchers.Default) {
                    handleScannedTag(tag)
                }
            }
        }

        // Stop reader
        readerManager.stopSound(1)
        readerManager.stopInventory()
        _isScanning.value = false

        // Ensure any buffered tags are emitted immediately
        flushPendingTags()
        scanJob?.cancel()
        scanJob = null
    }


    fun onScanStopped() {
        scanJob?.cancel()
        scanJob = null
        readerManager.stopInventory()
        readerManager.stopSound(1)
        scannedEpcList.clear()
        _allScannedTags.value.forEach { tag ->
            tag.epc?.let { epc ->
                if (!scannedEpcList.contains(epc)) {
                    scannedEpcList.add(epc)
                }
            }
        }
    }











    fun stopBarcodeScanner() {
        barcodeDecoder.close()
        readerManager.stopSound(2)
    }


    override fun onCleared() {
        super.onCleared()
        stopScanning()
    }

    fun saveDropdownCategory(name: String, type: String) {
        viewModelScope.launch {
            repository.addCategory(name)
        }
    }

    fun saveDropdownProduct(name: String, type: String) {
        viewModelScope.launch {
            repository.addProduct(name)
        }
    }

    fun saveDropdownDesign(name: String, type: String) {
        viewModelScope.launch {
            repository.addDesign(name)
        }
    }


    fun saveBulkItems(
        category: String,
        itemCode: String,
        product: String,
        design: String,
        scannedTags: List<UHFTAGInfo>,
        index: Int
    ) {
        viewModelScope.launch {
            val itemList = scannedTags.mapNotNull { tag ->
                val epc = tag.epc ?: return@mapNotNull null
                val tid = tag.tid ?: ""
                // val rfid = epc // or your display RFID if different

                BulkItem(
                    category = category,
                    productName = product,
                    design = design,
                    itemCode = itemCode,
                    rfid = rfidMap.value.get(index),
                    grossWeight = "",
                    stoneWeight = "",
                    diamondWeight = "",
                    netWeight = "",
                    purity = "",
                    makingPerGram = "",
                    makingPercent = "",
                    fixMaking = "",
                    fixWastage = "",
                    stoneAmount = "",
                    diamondAmount = "",
                    sku = "",
                    epc = epc,
                    vendor = "",
                    tid = tid,
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
                    unmatchedGrossWt = 0.0,
                    mrp = 0.0,
                    counterName = "",
                    matchedQty = 0,
                    counterId = 0,
                    scannedStatus = "",
                    boxId = 0,
                    boxName = "",
                    branchId = 0,
                    branchName = "",
                    categoryId = 0,
                    productId = 0,
                    designId = 0,
                    packetId = 0,
                    packetName = "",
                    branchType = "",
                ).apply {
                    uhfTagInfo = tag
                }
            }
            if (itemList.isNotEmpty()) {
                bulkRepository.clearAllItems()
                bulkRepository.insertBulkItems(itemList)
                println("SAVED: Saved ${itemList.size} items to DB successfully.")
                _toastMessage.emit("Saved ${itemList.size} items successfully!")
            } else {
                _toastMessage.emit("No items to save.")
            }
        }
    }

    suspend fun parseGoogleSheetHeaders(url: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connect()
            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val headersLine = reader.readLine()
            reader.close()
            println()
            headersLine.split(",").map {
                it.trim()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }


    private fun exportToExcel(context: Context, items: List<BulkItem>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isExporting.value = true
                _exportStatus.value = "Preparing export..."
                val workbook = XSSFWorkbook()
                val sheet = workbook.createSheet("all_sync_items")

                // Create header row
                val columns = listOf<(BulkItem) -> String>(
                    { it.category!! },
                    { it.productName!! },
                    { it.design!! },
                    { it.itemCode!! },
                    { it.rfid!! },
                    { it.grossWeight!! },
                    { it.stoneWeight!! },
                    { it.diamondWeight!! },
                    { it.netWeight!! },
                    { it.purity!! },
                    { it.makingPerGram!! },
                    { it.makingPercent!! },
                    { it.fixMaking!! },
                    { it.fixWastage!! },
                    { it.stoneAmount!! },
                    { it.diamondAmount!! },
                    { it.sku!! },
                    { it.epc!! },
                    { it.vendor!! },
                    { it.tid!! },
                    { it.productCode!! },
                    { it.box!! },
                    { it.designCode!! },
                )
                val headers = listOf(
                    "Category",
                    "Product Name",
                    "Design",
                    "Item Code",
                    "RFID",
                    "Gross Weight",
                    "Stone Weight",
                    "Dust Weight",
                    "Net Weight",
                    "Purity",
                    "Making/Gram",
                    "Making %",
                    "Fix Making",
                    "Fix Wastage",
                    "Stone Amount",
                    "Dust Amount",
                    "SKU",
                    "EPC",
                    "Vendor",
                    "TID",
                    "Box",
                    "Product Code",
                    "Design Code"
                )
                Log.e("HEADERS :", headers.toString())
                val headerRow = sheet.createRow(0)
                headers.forEachIndexed { colIndex, title ->
                    headerRow.createCell(colIndex).setCellValue(title)
                    sheet.setColumnWidth(colIndex, 4000)
                }

                // Add data rows
                items.forEachIndexed { rowIndex, item ->
                    val row = sheet.createRow(rowIndex + 1)
                    columns.forEachIndexed { colIndex, extractor ->
                        row.createCell(colIndex)
                            .setCellValue(extractor(item))
                    }
                }

                // Create file

                val downloads =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloads.exists()) downloads.mkdirs()
                val file = File(downloads, "all_items.xlsx")

// Optional: Delete existing file if you want to ensure it's removed before writing
                if (file.exists()) {
                    file.delete()
                }



                FileOutputStream(file).use { outputStream ->
                    workbook.write(outputStream)
                }

                workbook.close()

                // Media scan
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(file.absolutePath),
                    arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                    null
                )

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Exported to ${file.absolutePath}", Toast.LENGTH_SHORT)
                        .show()
                    openExcelFile(context, file)
                }
                _exportStatus.value = "Exported to ${file.absolutePath}"
            } catch (e: Exception) {
                _exportStatus.value = "Export failed: ${e.localizedMessage}"
            } finally {
                _isExporting.value = false
            }
        }
    }

    private fun openExcelFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No app to open Excel", Toast.LENGTH_SHORT).show()
        }
    }

    fun getAllItems() {
        viewModelScope.launch {
            bulkRepository.getAllBulkItems().collect { items ->
                _scannedFilteredItems.value = items // ✅ initialize display list
            }
        }
    }
    suspend fun uploadImage(clientCode: String, itemCode: String, imageUri: File) {

        val clientCodePart = clientCode.toRequestBody("text/plain".toMediaTypeOrNull())
        val itemCodePart = itemCode.toRequestBody("text/plain".toMediaTypeOrNull())

        val requestFile = imageUri.asRequestBody("image/*".toMediaTypeOrNull())
        val multipartBody = MultipartBody.Part.createFormData(
            name = "File",
            filename = imageUri.name,
            body = requestFile
        )

        apiService.uploadLabelStockImage(clientCodePart, itemCodePart, listOf(multipartBody))
    }

    fun getAllItems(context: Context) {
        viewModelScope.launch {
            bulkRepository.getAllBulkItems().collect { items ->
                _allItems = items
                _scannedFilteredItems.value = items
                exportToExcel(context, items)
                preloadFilters(_allItems)
            }
        }
    }

    fun syncAndMapRow(itemCode: String): String {
        return syncedRFIDMap?.get(itemCode) ?: ""
    }

    val rfidList: StateFlow<List<EpcDto>> =
        bulkRepository.getAllRFIDTags()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    suspend fun syncRFIDDataIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        if (syncedRFIDMap != null) return@withContext

        val employee = userPreferences.getEmployee(Employee::class.java)
        val clientCode = employee?.clientCode ?: return@withContext

        val response = bulkRepository.syncRFIDItemsFromServer(ClientCodeRequest(clientCode))

        // Save in DB
        bulkRepository.insertRFIDTags(response)

        // Build RFID → EPC map
        syncedRFIDMap = response.associateBy(
            { it.BarcodeNumber.orEmpty().trim().uppercase() },
            { it.TidValue.orEmpty().trim().uppercase() }
        )
    }




    // 📡 Utility function to check network availability
    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }




       fun syncItems() {
           viewModelScope.launch {
               try {
                   Log.d("Sync", "syncItems called")
                   _isLoading.value = true
                   _syncProgress.value = 0f
                   _syncStatusText.value = "Starting sync..."

                   val clientCode = employee?.clientCode ?: return@launch
                   val request = ClientCodeRequest(clientCode)

                   _syncStatusText.value = "Fetching data..."
                   val response = bulkRepository.syncBulkItemsFromServer(request)
                   val bulkItems = response
                       .filter { it.status == "ApiActive" || it.status == "Active" && !it.rfidCode.isNullOrBlank()  }
                       .map { it.toBulkItem() }

                   val total = bulkItems.size
                   bulkRepository.clearAllItems()

                   _scannedFilteredItems.value = bulkItems


                   bulkItems.forEachIndexed { index, item ->
                       val result = bulkRepository.insertSingleItem(item)
                       Log.d("Insert", "Inserted item with EPC ${item.epc}, result = $result")
                       _syncProgress.value = (index + 1f) / total
                       _syncStatusText.value = "Syncing... ${index + 1} of $total"
                       delay(100)
                   }

                   Log.d("ToastEmit", "Emitting toast")
                   _toastMessage.emit("Synced $total items successfully!")
                   _syncStatusText.value = "Sync completed successfully!"



               } catch (e: Exception) {
                   _syncStatusText.value = "Sync failed: ${e.localizedMessage}"
                   Log.d("ToastEmit", "Emitting toast")
                   _toastMessage.emit("Sync failed: ${e.localizedMessage}")
                   Log.e("Sync", "Error: ${e.localizedMessage}")

               }finally {
                   _isLoading.value = false
               }
           }
       }





    fun setRfidForAllTags(scanned: String) {
        val updatedMap = mutableMapOf<Int, String>()
        scannedTags.value.forEachIndexed { index, _ ->
            updatedMap[index] = scanned
        }
        _rfidMap.value = updatedMap
    }


    fun sendScannedData(tags: List<UHFTAGInfo>, androidId: String, context: Context) {
        Log.d("send scanned items", "CALLED")
        val currentDateTime = LocalDateTime.now()
        val formatted = currentDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))

        val clientCode = employee?.clientCode


        val data = _rfidMap.value.mapNotNull { (index, rfid) ->
            rfid.let {
                ScannedDataToService(
                    tIDValue = tags.get(index).tid,
                    rFIDCode = it,
                    createdOn = formatted,
                    lastUpdated = formatted,
                    id = 0,
                    clientCode = clientCode,
                    statusType = true,
                    deviceId = androidId

                )


            }
        }

        Log.d("DATA", data.toString())
        if (data.isNotEmpty()) {


            viewModelScope.launch {
                val response = apiService.addAllScannedData(data)
                if (response.isSuccessful) {
                    response.body() ?: emptyList()
                    ToastUtils.showToast(context, "Items scanned successfully")
                    _reloadTrigger.value = !_reloadTrigger.value // triggers recomposition
                    Log.d("API_SUCCESS", "Received response: ${response.body()}")

                } else {
                    Log.e("API_ERROR", "Error: ${response.code()}")
                    ToastUtils.showToast(context, "Failed to scan")
                }
            }


        }


    }



}
