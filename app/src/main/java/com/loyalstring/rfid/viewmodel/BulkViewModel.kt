package com.loyalstring.rfid.viewmodel

import ScannedDataToService
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Environment
import android.util.Log
import android.view.WindowManager
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
import com.loyalstring.rfid.data.model.login.SyncSkippedItem
import com.loyalstring.rfid.data.reader.BarcodeReader
import com.loyalstring.rfid.data.reader.RFIDReaderManager
import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import com.loyalstring.rfid.data.remote.data.ClearStockDataModelReq
import com.loyalstring.rfid.data.remote.response.AlllabelResponse
import com.loyalstring.rfid.di.SyncScope
import com.loyalstring.rfid.repository.BulkRepositoryImpl
import com.loyalstring.rfid.repository.DropdownRepository
import com.loyalstring.rfid.ui.screens.hexToAscii
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketException
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
    private val apiService: RetrofitInterface,
    private val syncScope: SyncScope
) : ViewModel() {
    @Volatile
    private var isSyncRunning = false
    @Volatile private var isFirstPageLoaded = false

    //private val success = readerManager.initReader()
    private var readerReady = false
    private val duplicateEpcCounter = HashMap<String, Int>(10_000)

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

    // PERF-FIX: Changed SharingStarted.Eagerly → WhileSubscribed(5000).
    // Eagerly keeps live DB cursors open even when no screen is observing these flows.
    // WhileSubscribed releases DB resources 5s after the last collector disappears,
    // lowering GC frequency on the Chainway device (75MB free RAM / 97.9% used).
    // val categories = repository.categories.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    // val products = repository.products.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    // val designs = repository.designs.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val categories =
        repository.categories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val products = repository.products.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val designs = repository.designs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
    // O(1) EPC lookup sets — avoid O(n) .any{} on every scanned tag
    private val existingEpcSet: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()
    private val allScannedTagEpcSet: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()
    private val duplicateEpcSet: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()

    private val _allScannedTags = mutableStateOf<List<UHFTAGInfo>>(emptyList())
    val allScannedTags: State<List<UHFTAGInfo>> = _allScannedTags

    private val _existingItems = mutableStateOf<List<UHFTAGInfo>>(emptyList())
    val existingItems: State<List<UHFTAGInfo>> = _existingItems

    private val _duplicateItems = mutableStateOf<List<UHFTAGInfo>>(emptyList())
    val duplicateItems: State<List<UHFTAGInfo>> = _duplicateItems
    val rfidInput = mutableStateOf("")

    // Thread-safe HashSet for concurrent access during high-volume scanning
    // Using ConcurrentHashMap.newKeySet() for thread safety
    // PERF-FIX: Reduced initial capacity from 300_000 → 10_000.
    // ConcurrentHashMap pre-allocates its internal table at creation time.
    // 300K buckets ≈ 24MB of heap allocated immediately on ViewModel init —
    // before any scanning even starts. On the Chainway device with 75MB free
    // RAM, this alone consumed ~32% of available memory. The map grows
    // automatically if more entries are needed, so 10K is a safe starting point.
    // val scannedEpcList: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet(300_000)
    val scannedEpcList: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet(10_000)

    // Persistent seen-EPC set for O(1) duplicate checks in addTagUnique and autoFillRfidFromDb
    private val seenTagEpcSet: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet(10_000)
    private val autoFillFetchedEpcs: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet(10_000)

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

    private val _isBulkMode = MutableStateFlow(false)
    val isBulkMode = _isBulkMode.asStateFlow()

    // ViewModel
    private val _syncTotalCount = MutableStateFlow(0)
    val syncTotalCount = _syncTotalCount.asStateFlow()



    private val _syncSyncedCount = MutableStateFlow(0)
    val syncSyncedCount = _syncSyncedCount.asStateFlow()

    private val _clearLoading = MutableStateFlow(false)
    val clearLoading: StateFlow<Boolean> = _clearLoading

    private val _clearSuccess = MutableStateFlow(false)
    val clearSuccess: StateFlow<Boolean> = _clearSuccess

    private val _deletedRecords = MutableStateFlow(0)
    val deletedRecords: StateFlow<Int> = _deletedRecords

    private val _clearError = MutableStateFlow<String?>(null)
    val clearError: StateFlow<String?> = _clearError

    fun setBulkMode(value: Boolean) {
        _isBulkMode.value = value
    }

    // PERF-FIX: Converted to a lazy StateFlow with WhileSubscribed so the Room
    // database query is NOT opened at BulkViewModel creation time. Previously,
    // bulkItemDao.getAllBulkItemsFlow() was a class-level property that executed
    // a Room Flow subscription immediately when BulkViewModel was injected into any
    // screen — even screens that never display the bulk item list. This caused Room
    // to hold an open cursor and deliver results on the main thread during navigation,
    // contributing to the "Skipped N frames" Choreographer warnings.
    // val savedBulkItemsFlow = bulkItemDao.getAllBulkItemsFlow()
    val savedBulkItemsFlow = bulkItemDao.getAllBulkItemsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _pendingSingleIndex = MutableStateFlow<Int?>(null)
    val pendingSingleIndex = _pendingSingleIndex.asStateFlow()

    private val _scannedKeySet = mutableStateOf<Set<String>>(emptySet())
    val scannedKeySet: State<Set<String>> = _scannedKeySet

    private fun String.normalizeTagKey(): String =
        trim().uppercase()

    private val _syncSkippedItems =
        MutableStateFlow<List<SyncSkippedItem>>(emptyList())

    val syncSkippedItems = _syncSkippedItems.asStateFlow()

    val syncSkippedItemCodes: StateFlow<List<String>> =
        syncSkippedItems
            .map { list -> list.map { it.itemCode } }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )


    // 🔸 add this
    @Volatile
    private var pendingIndexBackup: Int? = null

    fun prepareSingleRfidUpdate(index: Int) {
        Log.d("VM", "Setting pending index = $index")
        pendingIndexBackup = index        // immediate (no delay, thread-safe)
        _pendingSingleIndex.value = index // reactive (Compose)
    }

    fun getPendingSingleIndex(): Int? {
        val idx = _pendingSingleIndex.value ?: pendingIndexBackup
        Log.d("VM", "🔍 getPendingSingleIndex(): $idx")
        return idx
    }

    fun clearPendingSingleIndex() {
        Log.d("VM", "🧹 Clearing pending index")
        _pendingSingleIndex.value = null
        pendingIndexBackup = null
    }

    // ✅ function to update value at a specific index
    /*fun updateRfidForIndex(index: Int, newValue: String) {
         _rfidMap.value = _rfidMap.value.toMutableMap().apply {
             this[index] = newValue
         }
     }*/

    @Volatile
    private var _lastClickedIndex: Int? = null
    val lastClickedIndex: Int? get() = _lastClickedIndex

    fun setLastClickedIndex(index: Int) {
        _lastClickedIndex = index
        _pendingSingleIndex.value = index
        Log.d("VM", "✅ setLastClickedIndex = $index")
    }


    fun updateRfidForIndex(index: Int, value: String) {
        val newMap = _rfidMap.value.toMutableMap()
        newMap[index] = value
        _rfidMap.value = newMap
        Log.d(" _rfidMap.value @@",""+ _rfidMap.value
        )
    }



    fun preloadFilters(allItems: List<BulkItem>) {
        viewModelScope.launch(Dispatchers.Default) {
            // Process data on background thread
          val counters=bulkItemDao.getDistinctCounters()
          // val counters = allItems.mapNotNull { it.counterName }
           // val counters = allItems.mapNotNull { it.counterName?.takeIf { it.isNotBlank() } }.distinct()
            val branches=bulkItemDao.getDistinctBranch()
            //    val branches = allItems.mapNotNull { it.branchName?.takeIf { it.isNotBlank() } }.distinct()
          //  val boxes = allItems.mapNotNull { it.boxName?.takeIf { it.isNotBlank() } }.distinct()
            val boxes=bulkItemDao.getDistinctBoxes()
            val exhibitions = allItems
                .filter { it.branchType?.equals("Exhibition", ignoreCase = true) == true }
                .mapNotNull { it.branchName }
                .distinct()


            Log.d("FILTER_DEBUG", "Counters=$counters")
            Log.d("FILTER_DEBUG", "Branches=$branches")
            Log.d("FILTER_DEBUG", "Boxes=$boxes")
            Log.d("FILTER_DEBUG", "exhibitions=$exhibitions")


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

    /**
     * Expose a small API to toggle the existing loading indicator from callers.
     * Callers should use this to show the shared "please wait" overlay while
     * performing background computations (e.g., computing unmatched ids).
     */
    fun setLoading(flag: Boolean) {
        _isLoading.value = flag
    }

    fun setFilteredItems(filtered: List<BulkItem>) {
        _filteredSource = if (filtered.isEmpty()) _allItems.value else filtered
        // Precompute normalized EPC set for O(1) membership checks
        filteredDbEpcSet = _filteredSource.mapNotNull { it.epc?.trim()?.uppercase() }.toHashSet()
        // filteredDbTidSet = _filteredSource.mapNotNull { it.tid?.trim()?.uppercase() }.toHashSet() // TID matching disabled
    }


    private var scanJob: Job? = null

    private val _scanTrigger = MutableStateFlow<String?>(null)
    val scanTrigger: StateFlow<String?> = _scanTrigger

    private val _searchItems = mutableStateListOf<BulkItem>()
    val searchItems: SnapshotStateList<BulkItem> get() = _searchItems

    private val _allItems = MutableStateFlow<List<BulkItem>>(emptyList())
    val allItems: StateFlow<List<BulkItem>> = _allItems.asStateFlow()

    private val _filteredItems = mutableStateListOf<BulkItem>()
    val filteredItems: List<BulkItem> get() = _filteredItems
    // Backing set for O(1) membership checks to avoid O(n^2) behavior on large lists
    private val _filteredUnmatchedIds = MutableStateFlow<List<String>>(emptyList())
    val filteredUnmatchedIds: StateFlow<List<String>> = _filteredUnmatchedIds

    // Pagination support for large datasets
    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage

    private val _stickyUnmatchedIds = mutableStateListOf<String>()
    val stickyUnmatchedIds: List<String> get() = _stickyUnmatchedIds

    private val _pageSize = MutableStateFlow(100) // Default page size for UI
    val pageSize: StateFlow<Int> = _pageSize

    private val _totalItems = MutableStateFlow(0)
    val totalItems: StateFlow<Int> = _totalItems

    private val _isLoadingPage = MutableStateFlow(false)
    val isLoadingPage: StateFlow<Boolean> = _isLoadingPage

    fun rememberUnmatched(items: List<BulkItem>) {
        val ids = items.mapNotNull { it.epc?.trim()?.uppercase() }
        val toAdd = ids.filterNot { _filteredUnmatchedIds.value.contains(it) }
        if (toAdd.isNotEmpty()) {
            _filteredUnmatchedIds.value = _filteredUnmatchedIds.value.toMutableList().apply { addAll(toAdd) }
        }
    }

    fun clearStickyUnmatched() {
        _filteredUnmatchedIds.value = emptyList()
    }

    // Pagination methods for efficient large dataset handling
    fun setPageSize(size: Int) {
        _pageSize.value = size
        _currentPage.value = 0 // Reset to first page when page size changes
    }

    fun loadNextPage() {
        if (_isLoadingPage.value) return
        val nextPage = _currentPage.value + 1
        val maxPages = (_totalItems.value + _pageSize.value - 1) / _pageSize.value
        if (nextPage < maxPages) {
            loadPage(nextPage)
        }
    }

    fun loadPreviousPage() {
        if (_isLoadingPage.value) return
        val prevPage = _currentPage.value - 1
        if (prevPage >= 0) {
            loadPage(prevPage)
        }
    }

    fun loadPage(page: Int) {
        if (_isLoadingPage.value) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoadingPage.value = true
                val offset = page * _pageSize.value
                val items = bulkRepository.getMinimalItemsPaged(_pageSize.value, offset)
                withContext(Dispatchers.Main) {
                    _allItems.value = items
                    _currentPage.value = page
                    preloadFilters(items)
                }
            } finally {
                _isLoadingPage.value = false
            }
        }
    }

    suspend fun loadTotalCount() {
        _totalItems.value = bulkRepository.getTotalItemCount()
    }

    /**
     * Add a list of already-normalized EPC ids to the sticky unmatched set.
     * This is intended for callers that compute the ids off the main thread to avoid
     * performing large allocations on the UI thread.
     */
    fun rememberUnmatchedIds(ids: List<String>) {
        // Caller is expected to compute normalized ids off the main thread.
        // We still defensively normalize here, but avoid expensive contains checks
        // by using the backing set for O(1) membership tests.
        viewModelScope.launch(Dispatchers.Default) {
            val current = _filteredUnmatchedIds.value.toMutableSet()
            val toAdd = ids.filterNot { current.contains(it) }
            if (toAdd.isNotEmpty()) {
                current.addAll(toAdd)
                _filteredUnmatchedIds.value = current.toList()
            }
        }
    }

    private var isDataLoaded = false

    private var dataCollectorJob: Job? = null

    public fun startMinimalItemsCollector() {
        if (isFirstPageLoaded) return
        isFirstPageLoaded = true

        viewModelScope.launch(Dispatchers.IO) {
            val items = bulkRepository.getMinimalItemsPaged(100, 0)

            Log.e("PAGE_LOAD", "Loaded page size = ${items.size}")

            withContext(Dispatchers.Main) {
                _allItems.value = items
                _filteredSource = items        // 🔥 inventory uses this
                _filteredItems.clear()
                _filteredItems.addAll(items)
                _scannedFilteredItems.value = items
            }
          //  ensureFiltersLoaded()
            preloadFilters(items)
        }
    }
/*init {
    startMinimalItemsCollector()
}*/

    init {
        /*// Lazy load data only when needed instead of immediately
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
        }*/
        /*viewModelScope.launch {
            bulkRepository.getMinimalItemsFlow().collect { items ->
                _allItems.value = items
                preloadFilters(items)
                _scannedFilteredItems.value = items
            }
        }*/
    }

    /*  it working all counter and all
       init {
            viewModelScope.launch {
                bulkRepository.getAllBulkItems().collect { items ->

                    // ✅ THIS WAS MISSING (CRITICAL)
                    _allItems.value = items
                    withContext(Dispatchers.Main) {
                        preloadFilters(items)
                    }

                    _scannedFilteredItems.value = items
                    isDataLoaded = true
                }
            }
        }*/

    /*   init {
           viewModelScope.launch {
               bulkRepository.getMinimalItemsFlow()   // 👈 NOT full table
                   .collectLatest { items ->
                       if (isSyncRunning) return@collectLatest   // ✅ STOP reading
                       _allItems.value = items
                   }
           }
       }*/


    // Call this method when user actually needs the data (e.g., when navigating to list screen)
    fun ensureFiltersLoaded() {
        if (!isDataLoaded && _allItems.value.isNotEmpty()) {
            preloadFilters(_allItems.value)
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
            setFilteredItems(_allItems.value)
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
        // Prevent multiple scan sessions
        if (_isScanning.value) return

        _scannedKeySet.value = emptySet()
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
            if (filteredDbEpcSet.isEmpty()) {
                filteredDbEpcSet = _filteredSource.mapNotNull { it.epc?.trim()?.uppercase() }.toHashSet()
            }

            // Loop: only update matched set; avoid remapping list on main thread per tag
            scanJob = viewModelScope.launch(Dispatchers.IO) {
                // Seed from previous session so stop→start preserves the existing count
                val localMatchedSet = HashSet<String>(_matchedEpcSet.value)
                var lastUiUpdate = System.currentTimeMillis()

                while (isActive) {
                    try {
                        val tag = readerManager.readTagFromBuffer()
                        if (tag != null) {
                            val scannedEpc = tag.epc?.trim()?.uppercase()

                            if (!scannedEpc.isNullOrBlank()) {
                                scannedEpcList.add(scannedEpc)

                                if (filteredDbEpcSet.contains(scannedEpc)) {
                                    localMatchedSet.add(scannedEpc)
                                }

                                // Throttle StateFlow updates to max once per 300ms
                                // This prevents O(n²) Set copies and recomposition storms
                                val now = System.currentTimeMillis()
                                if (now - lastUiUpdate >= 300) {
                                    _matchedEpcSet.value = localMatchedSet.toHashSet()
                                    lastUiUpdate = now
                                    yield()
                                }
                            }
                        } else {
                            delay(1)
                        }
                    } catch (e: Exception) {
                        Log.e("RFID", "Error in scan loop: ${e.message}")
                        delay(10)
                    }
                }

                // Push final state after scanning stops
                _matchedEpcSet.value = localMatchedSet.toHashSet()
            }
        }
    }

    suspend fun computeScanResults(
        filteredItems: List<BulkItem>,
        stayVisibleInUnmatched: Boolean = false
    ) = withContext(Dispatchers.Default) {
        if (filteredItems.isEmpty()) {
            // If the list is empty, there's nothing to compute.
            // This can happen after a reset.
            _matchedItems.clear()
            _unmatchedItems.clear()
            _scannedFilteredItems.value = emptyList()
            return@withContext
        }

        val currentScannedEpcList = scannedEpcList.toList() // Create an immutable copy

        val matched = mutableListOf<BulkItem>()
        val unmatched = mutableListOf<BulkItem>()
        val scannedEpcSet = if (currentScannedEpcList.isNotEmpty()) {
            currentScannedEpcList.map { it.trim().uppercase() }.toSet()
        } else {
            emptySet()
        }
        _matchedEpcSet.value = scannedEpcSet


        val safeList = filteredItems.toList()

        safeList.forEach { item ->
            val dbEpc = item.epc?.trim()?.uppercase()
            if (dbEpc != null && scannedEpcSet.contains(dbEpc)) {
                val updatedItem = item.copy(scannedStatus = "Matched")
                matched.add(updatedItem)
                if (stayVisibleInUnmatched) unmatched.add(updatedItem)
            } else {
                val updatedItem = item.copy(scannedStatus = "Unmatched")
                unmatched.add(updatedItem)
            }
        }


        withContext(Dispatchers.Main) {
            if (matched.isEmpty() && unmatched.isEmpty()) {
                _matchedItems.clear()
                _unmatchedItems.clear()
                _scannedFilteredItems.value = emptyList()
                return@withContext
            }

            if (matched.isNotEmpty()) {
                _matchedItems.clear()
                _matchedItems.addAll(matched)     // ← atomic update, no delays or chunks
            } else {
                _matchedItems.clear()
            }

            if (unmatched.isNotEmpty()) {
                _unmatchedItems.clear()
                _unmatchedItems.addAll(unmatched)
            } else {
                _unmatchedItems.clear()
            }
            _scannedFilteredItems.value = safeList
        }
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
                    try {
                        val tag = readerManager.readTagFromBuffer()
                        // Null-safe check for tag and epc
                        if (tag != null && !tag.epc.isNullOrBlank()) {
                            // Avoid DB calls in the hot path; update UI immediately
                            handleScannedTag(tag)
                        }
                    } catch (e: Exception) {
                        Log.e("RFID", "Error reading tag: ${e.message}")
                    }
                }
            }
        }
//        else {
//            Log.e("RFID", "Reader not connected.")
//            return
//        }
    }

    /*fun stopScanningAndCompute() {
    stopScanning()
    viewModelScope.launch {
        computeScanResults(_filteredSource)
    }
    }*/

    // ViewModel-level properties
    @Volatile
    private var isComputing = false

    private val computeMutex = Mutex() // optional extra safety

    fun stopScanningAndCompute() {
        stopScanning()
        return
// Quick guard: avoid re-entry early
        if (isComputing) return

        viewModelScope.launch(Dispatchers.Default) {
            // make sure only one compute runs at a time
            computeMutex.withLock {
                if (isComputing) return@withLock
                isComputing = true

                try {
                    // snapshot the source to avoid concurrent mutation issues
                    val snapshot = _filteredSource.toList()
                    computeScanResultsFast(snapshot)
                } finally {
                    // Make sure to reset the flag on Main so any UI consumers see it there
                    withContext(Dispatchers.Main) {
                        isComputing = false
                    }
                }
            }
        }
    }


    suspend fun computeScanResultsFast(
        filteredItems: List<BulkItem>,
        stayVisibleInUnmatched: Boolean = false
    ) = withContext(Dispatchers.Default) {

        if (filteredItems.isEmpty()) {
            _matchedItems.clear()
            _unmatchedItems.clear()
            _scannedFilteredItems.value = emptyList()
            return@withContext
        }

// Immutable copy to avoid ConcurrentModificationException
        val currentScannedList = scannedEpcList.toList()

// Build EPC set only once (O(n))
        val scannedEpcSet = currentScannedList.asSequence()
            .map { it.trim().uppercase() }
            .toHashSet()

        _matchedEpcSet.value = scannedEpcSet

        val matched = ArrayList<BulkItem>(filteredItems.size / 4) // pre-allocate some memory
        val unmatched = ArrayList<BulkItem>(filteredItems.size / 2)

// Avoid calling .copy() unless required → huge performance gain for 3.5 lakh items
        for (item in filteredItems) {
            val dbEpc = item.epc?.trim()?.uppercase()

            if (dbEpc != null && scannedEpcSet.contains(dbEpc)) {
                if (item.scannedStatus != "Matched") {
                    matched.add(item.copy(scannedStatus = "Matched", rfid = item.rfid ?: item.epc))
                } else {
                    matched.add(item.copy(rfid = item.rfid ?: item.epc))
                }
                if (stayVisibleInUnmatched) unmatched.add(item.copy(rfid = item.rfid ?: item.epc))
            } else {
                if (item.scannedStatus != "Unmatched") {
                    unmatched.add(item.copy(scannedStatus = "Unmatched", rfid = item.rfid ?: item.epc))
                } else {
                    unmatched.add(item.copy(rfid = item.rfid ?: item.epc))
                }
            }
        }

// Switch to Main thread only to push final results
        withContext(Dispatchers.Main) {

            _matchedItems.apply {
                clear()
                if (matched.isNotEmpty()) addAll(matched)
            }

            _unmatchedItems.apply {
                clear()
                if (unmatched.isNotEmpty()) addAll(unmatched)
            }

            // No need to recalculate safeList
            _scannedFilteredItems.value = filteredItems
        }
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
            seenTagEpcSet.clear()
            autoFillFetchedEpcs.clear()
            existingEpcSet.clear()
            allScannedTagEpcSet.clear()
            duplicateEpcSet.clear()
            delay(50) // Allow recomposition to process empty lists
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
            seenTagEpcSet.clear()
            autoFillFetchedEpcs.clear()
            _scannedKeySet.value = emptySet()
            delay(50) // Allow recomposition to process empty lists
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

    private fun addTagUnique(tag: UHFTAGInfo?) {
        if (tag == null || tag.epc.isNullOrBlank()) return
        val tagEpc = tag.epc?.trim()?.uppercase() ?: return
        // O(1) duplicate check — avoids linear scan over the growing list
        if (seenTagEpcSet.add(tagEpc)) {
            synchronized(pendingTagsBufferLock) {
                pendingTagsBuffer.add(tag)
            }
            schedulePendingFlush()
        }
    }

    // Buffer to accumulate rapid incoming tags and emit in batches
    // Using synchronized lock for thread safety when scanning large volumes
    private val pendingTagsBuffer: MutableList<UHFTAGInfo> = mutableListOf()
    private val pendingTagsBufferLock = Any()
    private var flushJob: Job? = null

    private fun schedulePendingFlush() {
        val snapshot: List<UHFTAGInfo>
        synchronized(pendingTagsBufferLock) {
            if (pendingTagsBuffer.isEmpty()) return
            snapshot = pendingTagsBuffer.toList()
            pendingTagsBuffer.clear()
        }
        // seenTagEpcSet already guaranteed uniqueness in addTagUnique — no need to rebuild set here
        val existing = _scannedTags.value
        _scannedTags.value = existing + snapshot
    }

    private fun flushPendingTags() {
        val snapshot: List<UHFTAGInfo>
        synchronized(pendingTagsBufferLock) {
            if (pendingTagsBuffer.isEmpty()) return
            snapshot = pendingTagsBuffer.toList()
            pendingTagsBuffer.clear()
        }
        val existing = _scannedTags.value
        _scannedTags.value = existing + snapshot
    }

    fun getLocalCounters(): List<String> =
        allItems.value.mapNotNull { it.counterName?.takeIf { it.isNotBlank() } }.distinct()

    fun getLocalBranches(): List<String> =
        allItems.value.mapNotNull { it.branchName?.takeIf { it.isNotBlank() } }.distinct()

    fun getLocalBoxes(): List<String> =
        allItems.value.mapNotNull { it.boxName?.takeIf { it.isNotBlank() } }.distinct()

    fun getLocalExhibitions(): List<String> =
        allItems.value
            .filter { it.branchType?.equals("Exhibition", ignoreCase = true) == true }
            .mapNotNull { it.branchName } // return the branch names
            .distinct()

    fun setFilteredItemsByType(type: String, value: String) {
        val filtered = when (type) {
            "scan display" -> allItems.value
            "counter" -> allItems.value.filter { it.counterName == value }
            "branch" -> allItems.value.filter { it.branchName == value }
            "box" -> allItems.value.filter { it.boxName == value }
            "exhibition" -> allItems.value.filter {
                it.branchName == value && it.branchType.equals(
                    "Exhibition",
                    true
                )
            }

            else -> allItems.value
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

    private suspend fun handleScannedTag(tag: UHFTAGInfo?) {
        if (tag == null) return
        val epc = tag.epc?.trim()?.uppercase()
        if (epc.isNullOrBlank()) return

        // 1) Update UI list immediately
        addTagUnique(tag)

        // 2) Resolve duplicate/existing info — O(1) HashSet checks, no per-tag DB query
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val alreadyInExisting = existingEpcSet.contains(epc)
                val alreadyInScanned = allScannedTagEpcSet.contains(epc)
                val alreadyInDuplicates = duplicateEpcSet.contains(epc)

                if (!alreadyInExisting) {
                    if (alreadyInScanned) {
                        if (duplicateEpcSet.add(epc)) {
                            withContext(Dispatchers.Main) {
                                duplicateTags.add(tag)
                                setLastEpc(epc)
                                _duplicateItems.value = duplicateTags.toList()
                            }
                        }
                    } else {
                        allScannedTagEpcSet.add(epc)
                        // Only hit DB for truly new tags; cache result in existingEpcSet
                        val exists = isTagExistsInDatabase(epc)
                        withContext(Dispatchers.Main) {
                            _allScannedTags.value += tag
                            if (exists && !alreadyInDuplicates) {
                                existingEpcSet.add(epc)
                                existingTags.add(tag)
                                setLastEpc(epc)
                                _existingItems.value = existingTags.toList()
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("RFID", "Error processing tag: ${e.message}")
            }
        }
    }


    fun stopScanning() {
        // Cancel scan job first to stop incoming tags
        scanJob?.cancel()
        scanJob = null

        // Stop reader hardware
        try {
            readerManager.stopSound(1)
            readerManager.stopInventory()
        } catch (e: Exception) {
            Log.e("RFID", "Error stopping reader: ${e.message}")
        }

        _isScanning.value = false

        // Attempt to drain remaining tags from device buffer quickly before stopping
        // Using try-catch to prevent crashes during buffer drain
        try {
            repeat(25) {
                val tag = readerManager.readTagFromBuffer()
                if (tag != null && !tag.epc.isNullOrBlank()) {
                    // Process synchronously on Default dispatcher to avoid race conditions
                    viewModelScope.launch(Dispatchers.Default) {
                        try {
                            handleScannedTag(tag)
                        } catch (e: Exception) {
                            Log.e("RFID", "Error handling tag during stop: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("RFID", "Error draining buffer: ${e.message}")
        }

        // Ensure any buffered tags are emitted immediately
        flushPendingTags()
    }


    // Fast reset for ScanDisplayScreen.
    // KEY ORDER: cancel job → clear all visible data immediately → fire hardware stop async.
    // stopInventory() can block 4-5s waiting for hardware; doing it first kept the display
    // populated until it returned. Now the UI clears instantly and hardware stops in background.
    fun resetForDisplay() {
        // 1. Cancel scan job so no new tags get processed
        scanJob?.cancel()
        scanJob = null
        _isScanning.value = false

        // 2. Clear all visible scan state immediately — UI updates on the next frame
        _matchedItems.clear()
        _unmatchedItems.clear()
        scannedEpcList.clear()
        synchronized(pendingTagsBufferLock) { pendingTagsBuffer.clear() }
        _scannedKeySet.value = emptySet()
        _matchedEpcSet.value = emptySet()

        // 3. Reset filter to full DB — direct reference, no copy
        _filteredSource = _allItems.value
        filteredDbEpcSet = _filteredSource.mapNotNull { it.epc?.trim()?.uppercase() }.toHashSet()
        _scannedFilteredItems.value = _filteredSource

        // 4. Stop hardware in a separate coroutine — blocking but UI is already cleared
        viewModelScope.launch(Dispatchers.IO) {
            try {
                readerManager.stopSound(1)
                readerManager.stopInventory()
            } catch (e: Exception) {
                Log.e("RFID", "Error stopping reader during reset: ${e.message}")
            }
        }
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


    fun saveAllBulkProductRows(
        category: String,
        product: String,
        design: String,
        scannedTags: List<UHFTAGInfo>,
        itemCodes: Map<Int, String>,
        rfidCodes: Map<Int, String>
    ) {
        viewModelScope.launch {
            val itemList = scannedTags.mapIndexedNotNull { index, tag ->
                val epc = tag.epc?.trim()?.uppercase().orEmpty()
                if (epc.isBlank()) return@mapIndexedNotNull null
                val itemCode = itemCodes[index]?.trim().orEmpty()
                if (itemCode.isBlank()) return@mapIndexedNotNull null
                val tid = tag.tid ?: ""

                BulkItem(
                    category = category,
                    productName = product,
                    design = design,
                    itemCode = itemCode,
                    rfid = rfidCodes[index]?.trim()?.takeIf { it.isNotBlank() },
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
                    totalWt = 0.0,
                    CategoryWt = "",
                    SKUId = 0,
                    bulkItemId = 0,
                    purityId = 0,
                    Status = ""

                ).apply {
                    uhfTagInfo = tag
                }
            }
            if (itemList.isNotEmpty()) {
                bulkRepository.clearAllItems()
                bulkRepository.insertBulkItems(itemList)
                Log.d("BulkVM", "Saved ${itemList.size} bulk rows with per-row itemCode/rfid")
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
            bulkRepository.getAllItemsFlow().collectLatest {
                _scannedFilteredItems.value = it // ✅ initialize display list
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
                _allItems.value = items
                _scannedFilteredItems.value = items
                exportToExcel(context, items)
                preloadFilters(_allItems.value)
            }
        }
    }

    fun syncAndMapRow(itemCode: String): String {
        return syncedRFIDMap?.get(itemCode) ?: ""
    }

    val rfidList: StateFlow<List<EpcDto>> =
        bulkRepository.getAllRFIDTags()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

   /* suspend fun syncRFIDDataIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        return@withContext //Unnecessary
        *//*Log.d("SYNC_ITEM", "Server API Called")
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
        Log.d("SYNC_ITEM", "Server API Called Finished")*//*
    }*/
   suspend fun syncRFIDDataIfNeeded(context: Context) = withContext(Dispatchers.IO) {

       if (syncedRFIDMap != null) return@withContext

       val employee = userPreferences.getEmployee(Employee::class.java)
       val clientCode = employee?.clientCode ?: return@withContext

       val response = bulkRepository.syncRFIDItemsFromServer(ClientCodeRequest(clientCode))

       bulkRepository.insertRFIDTags(response)

       syncedRFIDMap = response.associateBy(
           { it.BarcodeNumber.orEmpty().trim().uppercase() },
           { it.TidValue.orEmpty().trim().uppercase() }
       )

       Log.d("SYNC_ITEM", "RFID mapping loaded size=${syncedRFIDMap?.size}")
   }




    // 📡 Utility function to check network availability
    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /*fun syncItems() {
    val skippedItems = mutableListOf<String>()
    viewModelScope.launch(Dispatchers.IO) {
        try {
            withContext(Dispatchers.Main) {
                _isLoading.value = true
                _syncStatusText.value = "Downloading data from server..."
                _syncProgress.value = 0f
                _syncTotalCount.value = 0
                _syncSyncedCount.value = 0
            }
            val clientCode = employee?.clientCode ?: run {
                withContext(Dispatchers.Main) { _isLoading.value = false }
                return@launch
            }
            val request = ClientCodeRequest(clientCode)
            val tagType = userPreferences.getClient()?.rfidType?.trim()?.lowercase() ?: "webreusable"

            val response = bulkRepository.syncBulkItemsFromServer(request)

            val bulkItems = response.asSequence()
                .filter { (it.status == "ApiActive" || it.status == "Active") &&
                        (!it.rfidCode.isNullOrBlank() || !it.itemCode.isNullOrBlank()) }
                .map { it.toBulkItem() }
                .toList()

            val total = bulkItems.size

            withContext(Dispatchers.Main) {
                _syncTotalCount.value = total
                _syncSyncedCount.value = 0
            }

            bulkRepository.clearAllItems()

            if (total == 0) {
                withContext(Dispatchers.Main) {
                    _syncProgress.value = 1f
                    _syncStatusText.value = "No items to sync"
                    _isLoading.value = false
                }
                return@launch
            }

            val processedItems = mutableListOf<BulkItem>()
            var processed = 0
            var synced = 0   // ✅ NEW
            var lastUpdate = System.currentTimeMillis()

            for (item in bulkItems) {
                var updatedItem = if (tagType == "webreusable") {
                    if (!item.rfid.isNullOrBlank()) {
                        if (item.epc.isNullOrBlank()) item.epc = syncAndMapRow(item.rfid!!)
                        item
                    } else null
                } else {
                    if (!item.itemCode.isNullOrBlank()) {
                        val hexValue = item.itemCode.toByteArray()
                            .joinToString("") { String.format("%02X", it) }
                        item.copy(rfid = item.itemCode, epc = hexValue, tid = hexValue)
                    } else null
                }

                if (updatedItem != null) {
                    if (!item.rfid.isNullOrBlank() && item.tid.isNullOrBlank()) {

                        val info = "ItemCode=${item.itemCode} here RFID Wrong "
                        skippedItems.add(info)
                        Log.e("SYNC_NOT_SYNCED", info)

                        // skip (do not add)
                    }else {
                        processedItems.add(updatedItem)
                        synced++ // ✅ count only valid/synced rows
                    }
                } else {
                    // ✅ NOT SYNCED ITEM LOG + STORE
                    val reason = if (tagType == "webreusable") "RFID blank" else "ItemCode blank"
                    val info = "ItemCode=${item.itemCode}  here TID is NULL"
                    skippedItems.add(info)
                    Log.e("SYNC_NOT_SYNCED", info)
                }

                processed++

                val now = System.currentTimeMillis()
                if (now - lastUpdate > 500) {
                    val progress = processed.toFloat() / total
                    withContext(Dispatchers.Main) {
                        _syncProgress.value = progress
                        _syncSyncedCount.value = synced
                        _syncStatusText.value = "Processing $synced of $total"
                    }
                    lastUpdate = now
                }

                if (processedItems.size >= 100) {
                    bulkRepository.insertBulkItems(processedItems.toList())
                    processedItems.clear()
                }
            }

            Log.e("SYNC_NOT_SYNCED_SUMMARY", "Total Not Synced = ${skippedItems.size}")
            Log.e("SYNC_NOT_SYNCED_LIST", skippedItems.joinToString("\n"))

            if (processedItems.isNotEmpty()) {
                bulkRepository.insertBulkItems(processedItems.toList())
            }

            withContext(Dispatchers.Main) {
                _syncSkippedItemCodes.value = skippedItems.distinct()
                _syncSyncedCount.value = synced
                _toastMessage.emit("✅ Synced $synced of $total items successfully!")
                _syncStatusText.value = "Sync completed!"
            }

        } finally {
            withContext(Dispatchers.Main) { _isLoading.value = false }
        }
    }
    }*/
    private fun blockTouch(context: Context) {
        val window = (context as Activity).window
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    fun unblockTouch(context: Context) {
        val window = (context as Activity).window
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    fun clearSyncCompleted() {
        _syncCompleted.value = false
    }

    /*fun syncItems() {
    val skippedItems = mutableListOf<String>()

    viewModelScope.launch(Dispatchers.IO) {
        try {
            withContext(Dispatchers.Main) {
                _isLoading.value = true
                _syncStatusText.value = "Downloading data from server..."
                _syncProgress.value = 0f
                _syncTotalCount.value = 0
                _syncSyncedCount.value = 0
            }

            val clientCode = employee?.clientCode ?: run {
                withContext(Dispatchers.Main) { _isLoading.value = false }
                return@launch
            }

            Log.d("SYNC", "clientCode=$clientCode")

            val request = ClientCodeRequest(clientCode)
            val tagType = userPreferences.getClient()?.rfidType?.trim()?.lowercase() ?: "webreusable"
            delay(100)

            // ✅ API fetch
            val response = bulkRepository.syncBulkItemsFromServer(request)
            Log.d("response", "response=$response")
            val bulkItems = response.asSequence()
                .filter {
                    (it.status == "ApiActive" || it.status == "Active") &&
                            (!it.rfidCode.isNullOrBlank() || !it.itemCode.isNullOrBlank())
                }
                .map { it.toBulkItem() }
                .toList()

            val total = bulkItems.size

            withContext(Dispatchers.Main) {
                _syncTotalCount.value = total
                _syncSyncedCount.value = 0
            }

            // ✅ Clear old data
            bulkRepository.clearAllItems()

            if (total == 0) {
                withContext(Dispatchers.Main) {
                    _syncProgress.value = 1f
                    _syncStatusText.value = "No items to sync"
                }
                return@launch
            }

            val processedItems = mutableListOf<BulkItem>()
            var processed = 0
            var synced = 0
            var lastUpdate = System.currentTimeMillis()

            for (item in bulkItems) {

                // ✅ Build updatedItem
                val updatedItem: BulkItem? =
                    if (tagType == "webreusable") {
                        if (!item.rfid.isNullOrBlank()) {
                            val mapped = syncAndMapRow(item.rfid!!).trim().uppercase()
                            item.copy(
                                epc = item.epc.takeIf { !it.isNullOrBlank() } ?: mapped,
                                tid = item.tid.takeIf { !it.isNullOrBlank() } ?: mapped
                            )
                        }
                        // ✅ if RFID blank but EPC/TID exists, allow insert
                        else if (!item.epc.isNullOrBlank() || !item.tid.isNullOrBlank()) {
                            item
                        } else {
                            null
                        }
                    } else {
                        if (!item.itemCode.isNullOrBlank()) {
                            val hexValue = item.itemCode.toByteArray()
                                .joinToString("") { String.format("%02X", it) }
                            item.copy(rfid = "", epc = hexValue, tid = hexValue)
                        } else null
                    }

                // ✅ VALIDATION + INSERT LIST
                if (updatedItem != null) {

                    // ✅ IMPORTANT FIX:
                    // earlier you checked item.tid (old), now check updatedItem.tid (mapped)
                    if (!updatedItem.rfid.isNullOrBlank() && updatedItem.tid.isNullOrBlank()) {
                        val info = "ItemCode=${updatedItem.itemCode} here RFID Wrong"
                        skippedItems.add(info)
                        Log.e("SYNC_NOT_SYNCED", info)
                    } else {
                        processedItems.add(updatedItem)
                        synced++
                    }

                } else {
                    val info = "ItemCode=${item.itemCode} here TID/RFID is NULL"
                    skippedItems.add(info)
                    Log.e("SYNC_NOT_SYNCED", info)
                }

                processed++

                // ✅ Progress update throttled
                val now = System.currentTimeMillis()
                if (now - lastUpdate > 500) {
                    val progress = processed.toFloat() / total.toFloat()
                    withContext(Dispatchers.Main) {
                        _syncProgress.value = progress
                        _syncSyncedCount.value = synced
                        _syncStatusText.value = "Processing $synced of $total"
                    }
                    lastUpdate = now
                }

                // ✅ Batch insert (with protection)
                if (processedItems.size >= 100) {
                    try {
                        bulkRepository.insertBulkItems(processedItems.toList())
                    } catch (e: Exception) {
                        Log.e("SYNC_DB", "Insert chunk failed size=${processedItems.size}", e)
                        skippedItems.add("DB insert failed for chunk: ${e.message}")
                    } finally {
                        processedItems.clear()
                    }
                }
            }

            Log.e("SYNC_DB", "Insert remaining failed size=${processedItems.size}")
            // ✅ Insert remaining
            if (processedItems.isNotEmpty()) {
                try {
                    bulkRepository.insertBulkItems(processedItems.toList())
                } catch (e: Exception) {
                    Log.e("SYNC_DB", "Insert remaining failed size=${processedItems.size}", e)
                    skippedItems.add("DB insert failed for remaining: ${e.message}")
                } finally {
                    processedItems.clear()
                }
            }

            Log.e("SYNC_NOT_SYNCED_SUMMARY", "Total Not Synced = ${skippedItems.size}")
            Log.e("SYNC_NOT_SYNCED_LIST", skippedItems.joinToString("\n"))

            // ✅ Final UI update
            withContext(Dispatchers.Main) {
                _syncSkippedItemCodes.value = skippedItems.distinct()
                _syncSyncedCount.value = synced
                _syncProgress.value = 1f
                _syncStatusText.value = "Sync completed!"
            }

            // ✅ toast emit (no need to launch again)
            _toastMessage.emit("✅ Synced $synced of $total items successfully!")

        } catch (e: Exception) {
            Log.e("SYNC_FAILED", "syncItems failed", e)
            withContext(Dispatchers.Main) {
                _syncStatusText.value = "Sync failed: ${e.message}"
            }
        } finally {
            withContext(Dispatchers.Main) { _isLoading.value = false }
        }
    }
    }
    */
    val usedEpcSet = mutableSetOf<String>()
    fun syncItems(context: Context) {
        isFirstPageLoaded = false
        isSyncRunning = true

        //repository
        syncScope.scope.launch(Dispatchers.IO) {

            val UI_UPDATE_INTERVAL = 700L
            var lastUiUpdate = System.currentTimeMillis()
            _syncSkippedItems.value = emptyList()

            val skippedItems = mutableListOf<SyncSkippedItem>()
            try {
                withContext(Dispatchers.Main) {
                    blockTouch(context)
                    _isLoading.value = true
                    _syncStatusText.value = "Downloading data..."
                    _syncProgress.value = 0f
                    _syncSyncedCount.value = 0

                }

                var totalItemsCount = 0
                var totalSyncCount = 0
                val clientCode = employee?.clientCode ?: return@launch
                val tagType = userPreferences.getClient()?.rfidType
                    ?.trim()
                    ?.lowercase()
                    ?: "webreusable"

                // Clear old data first
                delay(300)
                usedEpcSet.clear()
                withContext(Dispatchers.IO) {
                    bulkRepository.clearAllItemsSafely()
                }


                bulkRepository.syncBulkItemsFromServer(
                    request = ClientCodeRequest(clientCode),
                    tagType = tagType,

                    // 🔹 Mapping stays in ViewModel (SAFE)
                    mapItem = { serverItem ->
                        try {
                            mapServerItemToBulkItem(serverItem, tagType,skippedItems,usedEpcSet)
                        } catch (e: Exception) {
                            if (skippedItems.size < 10_000) {
                                skippedItems.add(
                                    SyncSkippedItem(
                                        itemCode = "${serverItem.itemCode ?: "UNKNOWN"} and ${e.message ?: "UNKNOWN"}",
                                        rfid = serverItem.rfidCode,
                                        tid = serverItem.tidNumber,
                                        reason = e.message ?: "Mapping failed"
                                    )

                                )
                            }

                            null // IMPORTANT: skip this item
                        }
                    },


                    // 🔹 Progress callback (throttled)
                    onProgress = { processed, synced, totalCount ->
                        totalSyncCount = synced
                        val now = System.currentTimeMillis()
                        if (totalItemsCount == 0)
                            totalItemsCount = totalCount
                        if (now - lastUiUpdate > UI_UPDATE_INTERVAL) {
                            val progress = processed.toFloat() / totalCount
                            withContext(Dispatchers.Main) {
                                //_syncSyncedCount.value = synced
                                //_syncStatusText.value = "Processing $synced items"
                                _syncProgress.value = progress
                                _syncSyncedCount.value = synced
                                _syncStatusText.value = "Processing $synced of $totalCount"
                            }
                            lastUiUpdate = now
                        }
                    }
                )

                withContext(Dispatchers.Main) {
                    _syncSkippedItems.value = skippedItems
                    _syncTotalCount.value = totalItemsCount
                    _syncSyncedCount.value = totalSyncCount
                    _syncProgress.value = 1f
                    _syncCompleted.value = true
                    _syncStatusText.value = "Sync completed"
                    _toastMessage.emit("✅ Sync completed successfully")
                    Log.d("SKIPPED_VM", "Skipped size = ${_syncSkippedItems.value.size}")
                  //  loadFirstPageAfterSync()
                    startMinimalItemsCollector()
                    /* viewModelScope.launch {
                         bulkRepository.getAllBulkItems().collect { items ->
                             _allItems.value = items
                             preloadFilters(items)
                         }
                     }*/
                }

            } catch (e: SocketException) {
                Log.e("SYNC", "Stream broke", e)
                throw e // handled by retry layer

            } catch (e: Exception) {
                Log.e("SYNC_ERROR", "Sync failed", e)
            } finally {
                withContext(Dispatchers.Main) {
                    unblockTouch(context)
                    _isLoading.value = false
                    isSyncRunning = false
                }
            }
        }
    }

    fun loadFirstPageAfterSync() {
        if (isFirstPageLoaded) return
        isFirstPageLoaded = true

        viewModelScope.launch(Dispatchers.IO) {
            val items = bulkRepository.getMinimalItemsPaged(100, 0)

            Log.e("PAGE_LOAD", "Loaded page size = ${items.size}")

            withContext(Dispatchers.Main) {
                // 1️⃣ Update main list
                _allItems.value = items.toList()

                // 2️⃣ 🔥 RESET FILTER SOURCE (THIS WAS MISSING)
                _filteredSource = items
                _filteredItems.clear()
                _filteredItems.addAll(items)

                // 3️⃣ Update scanned view
                _scannedFilteredItems.value = items
            }

            // 4️⃣ Now preload filters
            preloadFilters(items)
        }
    }

    /*   private fun mapServerItemToBulkItem(
    serverItem: AlllabelResponse.LabelItem,
    tagType: String
    ): BulkItem? {

    val item = serverItem.takeIf {
        (it.status == "ApiActive" || it.status == "Active") &&
                (!it.rfidCode.isNullOrBlank() || !it.itemCode.isNullOrBlank())
    }?.toBulkItem() ?: return null

    return when {
        tagType == "webreusable" && !item.rfid.isNullOrBlank() -> {
            if (item.epc.isNullOrBlank()) {
                item.epc = syncAndMapRow(item.rfid!!)   // ✅ stays here
            }
            item
        }

        tagType != "webreusable" && !item.itemCode.isNullOrBlank() -> {
            val hex = item.itemCode.toByteArray()
                .joinToString("") { "%02X".format(it) }
            item.copy(rfid = item.itemCode, epc = hex, tid = hex)
        }

        else -> null
    }
    }*/

    private fun mapServerItemToBulkItem(
        serverItem: AlllabelResponse.LabelItem,
        tagType: String,
        skippedItems: MutableList<SyncSkippedItem>,
        usedEpcSet: MutableSet<String>
    ): BulkItem? {

// ❌ Status invalid
        if (
            serverItem.status != "ApiActive" &&
            serverItem.status != "Active"
        ) {
            if (skippedItems.size < 10_000) {
                skippedItems.add(
                    SyncSkippedItem(
                        itemCode = "${serverItem.itemCode ?: "UNKNOWN"} and ${serverItem.status ?: "UNKNOWN"}",
                        rfid = serverItem.rfidCode,
                        tid = serverItem.tidNumber,
                        reason = "Inactive status: ${serverItem.status ?: "UNKNOWN"}"
                    )
                )
            }
            return null
        }

        val missingReason = when {
            serverItem.itemCode.isNullOrBlank() ->
                "ItemCode is null or blank"

            serverItem.categoryId == null || serverItem.categoryName.isNullOrBlank() ->
                "Category is null"

            serverItem.productId == null || serverItem.productName.isNullOrBlank() ->
                "Product is null"

            else -> null
        }

        if (missingReason != null) {
            if (skippedItems.size < 10_000) {
                skippedItems.add(
                    SyncSkippedItem(
                        itemCode = serverItem.itemCode  +" - "+missingReason?: "UNKNOWN",
                        rfid = serverItem.rfidCode,
                        tid = serverItem.tidNumber,
                        reason = missingReason
                    )
                )
            }

            return null
        }

        val item = serverItem.toBulkItem()

        return when {
            /* tagType == "webreusable" -> {
                 if (item.rfid.isNullOrBlank()) {
                     skippedItems.add(
                         SyncSkippedItem(
                             itemCode = "${item.itemCode ?: "UNKNOWN"} - RFID blank",
                             rfid = item.rfid,
                             tid = item.tid,
                             reason = "RFID blank"
                         )
                     )
                     null
                 } else {
                     if (item.epc.isNullOrBlank()) {
                         item.epc = syncAndMapRow(item.rfid!!)
                     }
                     item
                 }
             }*/
            tagType == "webreusable" -> {
                val allowSingleAndWebReusable = userPreferences.isWebReusableTagEnabled()
                val hasItemCode = !item.itemCode.isNullOrBlank()
                val hasRfid = !item.rfid.isNullOrBlank()
                val hasEpcOrTid = !item.epc.isNullOrBlank() || !item.tid.isNullOrBlank()

                // ❌ Skip only if NOTHING usable
                if (
                    !hasItemCode ||
                    (!allowSingleAndWebReusable && !hasRfid) ||
                    (allowSingleAndWebReusable && !hasRfid && !hasEpcOrTid)
                ) {
                    if (skippedItems.size < 10_000) {
                        skippedItems.add(
                            SyncSkippedItem(
                                itemCode = "${item.itemCode ?: "UNKNOWN"} - No RFID/EPC/TID",
                                rfid = item.rfid,
                                tid = item.tid,
                                reason = "RFID, EPC and TID all blank"
                            )
                        )
                    }
                    null
                } else {
                    // ✅ If RFID present but EPC missing → generate EPC
                    if (!item.rfid.isNullOrBlank() && item.epc.isNullOrBlank()) {
                        item.epc = syncAndMapRow(item.rfid!!)
                    }
                 // vasanti   item.epc = syncAndMapRow(item.rfid!!)

                    // 🔥 DUPLICATE EPC CHECK
                    /* val epcKey = item.epc?.trim()?.uppercase()
                     if (!epcKey.isNullOrBlank()) {
                         if (usedEpcSet.contains(epcKey)) {
                             if (skippedItems.size < 10_000) {
                                 skippedItems.add(
                                     SyncSkippedItem(
                                         itemCode = "${item.itemCode ?: "UNKNOWN"} - Duplicate EPC",
                                         rfid = item.rfid,
                                         tid = item.tid,
                                         reason = "Duplicate EPC detected"
                                     )
                                 )
                             }
                             return null
                         }
                         usedEpcSet.add(epcKey)
                     }*/
                    /*    val epcKey = item.epc?.trim()?.uppercase()
                        if (!epcKey.isNullOrBlank()) {

                            // ✅ SAFETY GUARD — prevent memory blow-up
                            if (usedEpcSet.size > 300_000) {
                                usedEpcSet.clear()
                            }

                            // 🔁 DUPLICATE CHECK
                            if (usedEpcSet.contains(epcKey)) {
                                if (skippedItems.size < 10_000) {
                                    skippedItems.add(
                                        SyncSkippedItem(
                                            itemCode = "${item.itemCode ?: "UNKNOWN"} - Duplicate EPC",
                                            rfid = item.rfid,
                                            tid = item.tid,
                                            reason = "Duplicate EPC detected"
                                        )
                                    )
                                }
                                return null
                            }

                            // ✅ ADD EPC
                            usedEpcSet.add(epcKey)
                        }*/
                    /*   val epcKey = item.epc?.trim()?.uppercase()
                       if (!epcKey.isNullOrBlank()) {

                           if (usedEpcSet.contains(epcKey)) {

                               // 🔢 Count duplicate
                               val count = (duplicateEpcCounter[epcKey] ?: 1) + 1
                               duplicateEpcCounter[epcKey] = count

                               // 👁️ Show to user (limited)
                               if (skippedItems.size < 10_000) {
                                   skippedItems.add(
                                       SyncSkippedItem(
                                           itemCode = "${item.itemCode ?: "UNKNOWN"}",
                                           rfid = item.rfid,
                                           tid = item.tid,
                                           reason = "Duplicate EPC: $epcKey (occurrence #$count)"
                                       )
                                   )

                               }

                               return null
                           }

                           // ✅ First time EPC → store
                           usedEpcSet.add(epcKey)
                       }
           */
                    val epcKey = item.epc?.trim()?.uppercase()

                    if (!epcKey.isNullOrBlank()) {

                        // Memory guard
                        if (usedEpcSet.size > 300_000) {
                            usedEpcSet.clear()
                        }

                        if (usedEpcSet.contains(epcKey)) {
                            if (skippedItems.size < 10_000) {
                                skippedItems.add(
                                    SyncSkippedItem(
                                        itemCode = item.itemCode +" -"+"Duplicate EPC" ?: "UNKNOWN",
                                        rfid = item.rfid,
                                        tid = item.tid,
                                        reason = "Duplicate EPC"
                                    )
                                )
                            }
                            return null
                        }

                        usedEpcSet.add(epcKey)
                    }




                    // usedEpcSet.add(epcKey.toString())
                    // ✅ VALID ITEM → ADD
                    item
                }


            }


            tagType != "webreusable" -> {
                if (item.itemCode.isNullOrBlank()) {
                    if (skippedItems.size < 10_000) {
                        skippedItems.add(
                            SyncSkippedItem(
                                itemCode = "ItemCode blank - UNKNOWN",
                                rfid = item.rfid,
                                tid = item.tid,
                                reason = "ItemCode blank"
                            )
                        )
                    }
                    null
                } else {
                    val hex =convertToHex(item.itemCode)
                    item.copy(
                        rfid = null,     // ✅ KEEP BLANK
                        epc = hex,
                        tid = hex
                    )
                }
            }

            else -> {
                val reasonText = "Unknown mapping rule"
                if (skippedItems.size < 10_000) {
                    skippedItems.add(

                        SyncSkippedItem(
                            itemCode = "${item.itemCode ?: "UNKNOWN"} ($reasonText)",
                            rfid = item.rfid,
                            tid = item.tid,
                            reason = reasonText
                        )
                    )
                }
                null
            }
        }
    }

    fun convertToHex(input: String): String {
        val hexBuilder = StringBuilder()

// Step 1: Convert each character to 2-digit hex (ASCII)
        for (ch in input) {
            val hex = String.format("%02X", ch.code)
            hexBuilder.append(hex)
        }

// Step 2: Pad with "00" at the START until length % 4 == 0
        while (hexBuilder.length % 4 != 0) {
            hexBuilder.insert(0, "00")
        }

        return hexBuilder.toString()
    }



    fun setRfidForAllTags(scanned: String) {
        val updatedMap = mutableMapOf<Int, String>()
        scannedTags.value.forEachIndexed { index, _ ->
            updatedMap[index] = scanned
        }
        _rfidMap.value = updatedMap
    }


    /* fun sendScannedData(tags: List<UHFTAGInfo>, androidId: String, context: Context) {
    Log.d("send scanned items", "CALLED")
    val currentDateTime = LocalDateTime.now()
    val formatted = currentDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))

    val clientCode = employee?.clientCode

    if (tags.isEmpty()) {
        Log.e("SEND_DATA", "Tags list is empty, skipping sending data.")
        return
    }

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
                ToastUtils.showToast(context, "Items Saved successfully")
                _reloadTrigger.value = !_reloadTrigger.value // triggers recomposition
                Log.d("API_SUCCESS", "Received response: ${response.body()}")

            } else {
                Log.e("API_ERROR", "Error: ${response.code()}")
                ToastUtils.showToast(context, "Failed to scan")
            }
        }


    }


    }*/

    fun hexToAscii(hex: String): String {
        return try {
            hex.chunked(2)
                .map { it.toInt(16).toChar() }
                .joinToString("")
                .trim()
        } catch (e: Exception) {
            ""
        }
    }
    private fun normalizeEpc(raw: String): String =
        raw.trim()
            .uppercase()
            .replace(" ", "")
            .replace("\n", "")
            .replace("\r", "")

    private suspend fun resolveRfidCode(epc: String, index: Int): String {
        val manual = _rfidMap.value[index].orEmpty().trim()
        if (manual.isNotBlank() && !manual.equals("scan here", ignoreCase = true)) {
            return manual
        }

        val mapped = if (epc.startsWith("E", ignoreCase = true)) {
            _itemCodeMap.value[epc].orEmpty()
                .ifBlank { _rfidCodeByEpcMap.value[epc].orEmpty() }
                .ifBlank { bulkRepository.getItemCodeByEpc(epc) }
        } else {
            cleanRfid(hexToAscii(epc))
        }

        return mapped.trim()
    }

    fun sendScannedData(
        tags: List<UHFTAGInfo>,
        androidId: String,
        context: Context
    ) {
        Log.d("send scanned items", "CALLED tags=${tags.size}")

        if (tags.isEmpty()) {
            Log.e("SEND_DATA", "Tags list is empty")
            return
        }

        viewModelScope.launch {
            val formatted = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
            val clientCode = employee?.clientCode

            val data = tags.mapIndexedNotNull { index, tag ->
                val epc = normalizeEpc(tag.epc)
                val finalCode = resolveRfidCode(epc, index)

                if (
                    epc.isBlank() ||
                    finalCode.isBlank() ||
                    finalCode.equals("scan here", ignoreCase = true)
                ) {
                    Log.d("SKIP_SEND", "Skipped INDEX=$index EPC=$epc FINAL='$finalCode'")
                    return@mapIndexedNotNull null
                }

                Log.d("DEBUG_SEND", "INDEX=$index EPC=$epc FINAL='$finalCode'")

                ScannedDataToService(
                    tIDValue = epc,
                    rFIDCode = finalCode,
                    createdOn = formatted,
                    lastUpdated = formatted,
                    id = 0,
                    clientCode = clientCode,
                    statusType = true,
                    deviceId = androidId
                )
            }

            Log.e(
                "FINAL_SEND",
                "tags=${tags.size}, rfidMap=${_rfidMap.value.size}, data=${data.size}"
            )

            if (data.isEmpty()) {
                ToastUtils.showToast(context, "No valid RFID data found")
                return@launch
            }

            try {
                val response = apiService.addAllScannedData(data)

                if (response.isSuccessful) {
                    ToastUtils.showToast(context, "Items Saved successfully")
                    _reloadTrigger.value = !_reloadTrigger.value
                    Log.d("API_SUCCESS", "Saved ${data.size} items")
                } else {
                    Log.e("API_ERROR", "Error: ${response.code()}")
                    ToastUtils.showToast(context, "Failed to scan")
                }
            } catch (e: Exception) {
                Log.e("API_EXCEPTION", "Save failed", e)
                ToastUtils.showToast(context, "Failed to scan")
            }
        }
    }

    fun cleanRfid(value: String?): String {
        return value
            .orEmpty()
            .replace("\u0000", "")
            .filter { it.code in 32..126 }
            .trim()
    }

    /*fun loadUnmatchedFast(sourceItems: List<BulkItem>) {
    viewModelScope.launch(Dispatchers.Default) {

        if (sourceItems.isEmpty()) {
            withContext(Dispatchers.Main) {
                _filteredUnmatchedIds.value = emptyList()
                _unmatchedItems.clear()
            }
            return@launch
        }

        // Heavy processing off main thread
        val epcSet = sourceItems
            .asSequence()
            .mapNotNull { it.epc?.trim()?.uppercase() }
            .toSet()

        val scannedSet = scannedEpcList
            .asSequence()
            .mapNotNull { it.trim().uppercase() }
            .toSet()

        val unmatchedList = sourceItems
            .asSequence()
            .filterNot { item ->
                val epc = item.epc?.trim()?.uppercase()
                epc != null && scannedSet.contains(epc)
            }
            .map { it.copy(scannedStatus = "Unmatched") }
            .toList()

        // Only update UI thread with final result
        withContext(Dispatchers.Main) {

            // DO NOT push 350k IDs to StateFlow → limit to avoid ANR
            _filteredUnmatchedIds.value = epcSet
                .take(3000)              // safe cap to avoid recomposition storm
                .toList()

            _unmatchedItems.clear()
            _unmatchedItems.addAll(unmatchedList)
        }
    }
    }*/


    private val unmatchedMutex = Mutex()      // prevents parallel calls
    @Volatile private var unmatchedRunning = false

    fun loadUnmatchedFast(sourceItems: List<BulkItem>) {
        if (unmatchedRunning) return   // avoid multiple fast calls

        viewModelScope.launch(Dispatchers.Default) {
            unmatchedMutex.withLock {
                if (unmatchedRunning) return@launch
                unmatchedRunning = true
            }

            try {
                // Use pagination for unmatched items to avoid ANR
                loadUnmatchedPaged()
            } catch (t: Throwable) {
                withContext(Dispatchers.Main) {
                    setLoading(false)
                    Log.e("BulkVM", "loadUnmatchedFast error", t)
                }
            } finally {
                unmatchedRunning = false
            }
        }
    }

    private suspend fun loadUnmatchedPaged() {
        withContext(Dispatchers.IO) {
            try {
                // Get scanned EPCs for filtering
                val scannedEpcs = scannedEpcList.toList()
                val scannedSet = scannedEpcs.map { it.trim().uppercase() }.toHashSet()

                // Load first page of items and filter unmatched ones
                val pageSize = 500 // Smaller page size for better performance
                val allPaged = bulkRepository.getMinimalItemsPaged(pageSize * 3, 0) // Load more to have enough after filtering

                // Filter unmatched items efficiently
                val unmatchedItems = allPaged.filter { item ->
                    val epc = item.epc?.trim()?.uppercase()
                    epc == null || !scannedSet.contains(epc)
                }.take(pageSize)

                // Mark items as unmatched
                val processedItems = unmatchedItems.map { item ->
                    Log.d("UNMATCHED_ITEM_DEBUG", "ItemCode: ${item.itemCode}, RFID: ${item.rfid}, EPC: ${item.epc}")
                    if (item.scannedStatus != "Unmatched") {
                        item.copy(scannedStatus = "Unmatched", rfid = item.rfid )
                    } else {
                        item.copy(rfid = item.rfid)
                    }
                }

                // Calculate total unmatched count efficiently
                val totalItems = bulkRepository.getTotalItemCount()
                val estimatedUnmatched = maxOf(0, totalItems - scannedEpcs.size)

                withContext(Dispatchers.Main) {
                    _unmatchedItems.clear()
                    _unmatchedItems.addAll(processedItems)

                    // Set filtered IDs for UI (limited for performance)
                    val limitedIds = processedItems
                        .mapNotNull { it.epc?.trim()?.uppercase() }
                        .take(1000) // Limit for UI performance
                        .toList()
                    _filteredUnmatchedIds.value = limitedIds

                    _totalItems.value = estimatedUnmatched
                    setLoading(false)
                }
            } catch (e: Exception) {
                Log.e("BulkVM", "Error loading unmatched items", e)
                withContext(Dispatchers.Main) {
                    setLoading(false)
                }
            }
        }
    }


    fun findNextEmptyRow(): Int? {
        val map = rfidMap.value
        return map.entries.firstOrNull { it.value.isNullOrEmpty() }?.key
    }

    // ✅ NEW: auto-fill RFIDCode from local Room DB by EPC and update rfidMap(index->rfid)
    private val autoFillMutex = Mutex()

    fun autoFillRfidFromDb(tags: List<UHFTAGInfo>) {
        viewModelScope.launch(Dispatchers.IO) {
            autoFillMutex.withLock {
                if (tags.isEmpty()) {
                    _rfidMap.value = emptyMap()
                    return@withLock
                }

                val currentMap = _rfidMap.value.toMutableMap()

                // Only query EPCs we haven't fetched before — avoids re-querying all 15k tags each call
                val newEpcs = tags.mapNotNull { it.epc?.trim()?.uppercase() }
                    .filter { it.isNotBlank() && autoFillFetchedEpcs.add(it) }

                val byEpc = if (newEpcs.isNotEmpty()) {
                    // Chunk to stay under SQLite's 999-variable limit
                    newEpcs.chunked(500)
                        .flatMap { chunk -> bulkItemDao.getItemsByEpcs(chunk) }
                        .associateBy { it.epc?.trim()?.uppercase().orEmpty() }
                } else {
                    emptyMap()
                }

                if (byEpc.isEmpty() && currentMap.isNotEmpty()) return@withLock

                // Fill rfidMap only if index empty (do not override manual barcode edits)
                tags.forEachIndexed { index, tag ->
                    val already = currentMap[index]
                    if (!already.isNullOrBlank()) return@forEachIndexed

                    val key = tag.epc?.trim()?.uppercase().orEmpty()
                    if (key.isBlank()) return@forEachIndexed

                    val db = byEpc[key]
                    val rfid = db?.rfid?.trim().orEmpty()

                    if (rfid.isNotBlank()) {
                        // avoid duplicate assignment
                        if (!currentMap.containsValue(rfid)) {
                            currentMap[index] = rfid
                        }
                    }
                }

                _rfidMap.value = currentMap
            }
        }
    }

    fun clearStockData(clientCode: String, deviceId: String) {
        viewModelScope.launch {
            _clearLoading.value = true
            _clearSuccess.value = false
            _deletedRecords.value = 0
            _clearError.value = null

            try {
                val res = bulkRepository.clearStockData(
                    ClearStockDataModelReq(
                        clientCode = clientCode,
                        deviceId = deviceId
                    )
                )

                // response: {"success":true,"deletedRecords":11}
                _clearSuccess.value = res.success
                _deletedRecords.value = res.deletedRecords

                if (!res.success) {
                    _clearError.value = "Failed to clear stock data"
                }
            } catch (e: Exception) {
                _clearError.value = e.message ?: "API error"
            } finally {
                _clearLoading.value = false
            }
        }
    }

    fun clearClearStockResult() {
        _clearLoading.value = false
        _clearSuccess.value = false
        _deletedRecords.value = 0
        _clearError.value = null
    }

    private val _lastEpc = MutableStateFlow("")
    val lastEpc: StateFlow<String> = _lastEpc

    fun setLastEpc(epc: String) {
        _lastEpc.value = epc
    }


    /*added itemcode map for the single use tag*/
    private val _itemCodeMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val itemCodeMap: StateFlow<Map<String, String>> = _itemCodeMap

/*    fun loadItemCodeForEpc(epc: String) {
        viewModelScope.launch {
            val code = bulkRepository.getItemCodeByEpc(epc)
            if (code.isNotBlank()) {
                _itemCodeMap.value =
                    _itemCodeMap.value + (epc to code)
            }
        }
    }*/

    fun loadItemCodeForEpc(epc: String) {
        viewModelScope.launch {
            val cleanEpc = epc.trim().uppercase()
            val code = bulkRepository.getItemCodeByEpc(cleanEpc)

            if (code.isNotBlank()) {
                _itemCodeMap.value = _itemCodeMap.value + (cleanEpc to code)
            }
        }
    }

    private val _rfidCodeByEpcMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val rfidCodeByEpcMap = _rfidCodeByEpcMap.asStateFlow()

    fun loadRfidTagMap() {
        viewModelScope.launch(Dispatchers.IO) {
            bulkRepository.getAllRFIDTags().collect { list ->
                val map = list.associate { row ->
                    row.TidValue
                        .orEmpty()
                        .trim()
                        .uppercase()
                        .replace(" ", "")
                        .replace("\n", "")
                        .replace("\r", "") to row.BarcodeNumber.orEmpty()
                }

                _rfidCodeByEpcMap.value = map
            }
        }
    }



    /*   fun restoreScanFromSavedBulkItems(items: List<BulkItem>) {
    val restoredMatched = items
        .filter { it.scannedStatus.equals("Matched", true) }
        .mapNotNull { it.epc ?: it.itemCode }
        .map { it.trim().uppercase() }
        .toSet()

    _matchedEpcSet.value = restoredMatched
    }*/

    /* fun restoreScanFromSavedBulkItems(allItems: List<BulkItem>) {
    viewModelScope.launch {

        // 1️⃣ UI ke liye items restore
        _allItems.value = allItems

        // 2️⃣ matched EPC/RFID set rebuild
        val restoredMatchedKeys = allItems
            .filter { it.scannedStatus.equals("Matched", true) }
            .mapNotNull { it.epc ?: it.itemCode }
            .map { it.trim().uppercase() }
            .toSet()

        _matchedEpcSet.value = restoredMatchedKeys

        // 3️⃣ live scan temp states clear
        _scannedKeySet.value = emptySet()
       // _filteredUnmatchedIds.value = emptySet()

        Log.d("RESUME_SCAN", "Restored matched=${restoredMatchedKeys.size}")
    }
    }*/

    /*fun restoreScanFromSavedBulkItems(allItems: List<BulkItem>) {

    // 1️⃣ Extract matched EPCs from saved data
    val restoredMatchedEpcs = allItems
        .filter { it.isScanned }
        .mapNotNull { it.epc?.trim()?.uppercase() }
        .toSet()

    // 2️⃣ Restore EPC set (THIS DRIVES UI)
    _matchedEpcSet.value = restoredMatchedEpcs

    // 3️⃣ Clear any sticky unmatched state
    //  _filteredUnmatchedIds.value = emptySet()

    // 4️⃣ Stop scanning mode
    _isScanning.value = false
    }*/



    fun restoreScanFromDatabase() {
        viewModelScope.launch {
            val savedItems = bulkItemDao.getAllBulkItemsOnce()

            _matchedEpcSet.value = savedItems
                .filter { it.scannedStatus.equals("Matched", true) }
                .mapNotNull { it.epc ?: it.itemCode }
                .map { it.trim().uppercase() }
                .toSet()

            _scannedFilteredItems.value = savedItems
        }
    }

    fun saveScanResultLocally(items: List<BulkItem>) {
        viewModelScope.launch(Dispatchers.IO) {
            bulkRepository.updateScanStatus(items)
        }
    }

    /*remove the item from unmatch once scan and matched*/
    fun removeFromStickyUnmatched(matchedKeys: Set<String>) {
        _filteredUnmatchedIds.value =
            _filteredUnmatchedIds.value.filterNot { it in matchedKeys }
    }

    fun clearLabelStockOnLogout(onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                stopScanning()

                bulkRepository.clearAllItemsSafely()
                bulkRepository.clearAllRFID()

                usedEpcSet.clear()
                scannedEpcList.clear()
                seenTagEpcSet.clear()
                autoFillFetchedEpcs.clear()

                withContext(Dispatchers.Main) {
                    _allItems.value = emptyList()
                    _scannedFilteredItems.value = emptyList()
                    _matchedItems.clear()
                    _unmatchedItems.clear()
                    _filteredItems.clear()
                    _matchedEpcSet.value = emptySet()
                    onDone()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onDone()
                }
            }
        }
    }



}




