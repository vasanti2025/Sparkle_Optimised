package com.loyalstring.rfid.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.repository.BulkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject



@HiltViewModel
class ProductListViewModel @Inject constructor(
    //repository: BulkRepository,
    private val repository: BulkRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isLoadingAllItems = MutableStateFlow(false)
    val isLoadingAllItems: StateFlow<Boolean> = _isLoadingAllItems

    private val _productList = MutableStateFlow<List<BulkItem>>(emptyList())
    val productList: StateFlow<List<BulkItem>> = _productList

    // ✅ Optimized: Use pagination instead of loading all items at once
    private val initialPageSize = 500 // Load first 500 items initially
    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage

    init {
       refrshProductList()
    }

    fun removeItemLocally(id: Int?) {
        _productList.value = _productList.value.filter {
            it.bulkItemId != id
        }
    }

    fun refrshProductList() {
        viewModelScope.launch {

            /*try {
                    _isLoading.value = true
                    repository.getAllBulkItems().collect { items ->
                        for (item in items) {
                            if (item.rfid == "SJ4227") {
                                val json = Gson().toJson(item)
                                Log.d("ProductListVM", "🎯 Matched BulkItem: $json")
                            }
                        }

                        _productList.value = items
                        _isLoading.value = false
                    }
                } catch (e: Exception) {
                    _isLoading.value = false
                }*/

            try {
                val startTime = System.currentTimeMillis()
                Log.d("ProductListVM", "🚀 [PERF] Starting initial load...")

                _isLoading.value = true

                // ✅ Load first 500 items quickly for immediate display (avoid OOM)
                withContext(Dispatchers.IO) {
                    val loadStartTime = System.currentTimeMillis()
                    val initialItems = repository.bulkItemDao.getMinimalItemsPaged(initialPageSize, 0)
                    val loadEndTime = System.currentTimeMillis()
                    Log.d("ProductListVM", "🚀 [PERF] Initial 500 items loaded in: ${loadEndTime - loadStartTime}ms")

                    withContext(Dispatchers.Main) {
                        // Debug logging for specific item
                        for (item in initialItems) {
                            if (item.rfid == "SJ4227") {
                                val json = Gson().toJson(item)
                                Log.d("ProductListVM", "🎯 Matched BulkItem: $json")
                            }
                        }

                        _productList.value = initialItems
                        _currentPage.value = 0

                        val totalTime = System.currentTimeMillis() - startTime
                        Log.d("ProductListVM", "🚀 [PERF] Initial 500 items displayed in: ${totalTime}ms")
                        //_isLoading.value = false
                    }
                }

                // ✅ For 500k+ items: Use Room Flow but limit to reasonable size to avoid OOM
                // Only load all items if total count is manageable, otherwise use pagination
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val allItemsStartTime = System.currentTimeMillis()
                        Log.d("ProductListVM", "🚀 [PERF] Checking total count...")

                        // Get total count first
                        val totalCount = repository.bulkItemDao.getTotalItemCount()
                        Log.d("ProductListVM", "🚀 [PERF] Total items in database: $totalCount")

                        // ✅ Load ALL items progressively: 25k chunks with delays between chunks
                        Log.d("ProductListVM", "🚀 [PERF] Loading ALL items progressively in 25k chunks...")

                        withContext(Dispatchers.Main) {
                            //_isLoadingAllItems.value = true
                            _isLoading.value = true
                        }

                        // ✅ Load ALL chunks first, then update UI once to avoid multiple recompositions
                        val chunkSize = 25000 // Load 25k items per chunk
                        val allItemsList = mutableListOf<BulkItem>()
                        allItemsList.addAll(_productList.value) // Start with initial 500 items
                        var currentOffset = initialPageSize // Start from 500 (after initial load)
                        var chunkNumber = 1

                        Log.d("ProductListVM", "🚀 [PERF] Starting to fetch all chunks from DB (no UI updates during fetch)...")

                        // ✅ Phase 1: Fetch all chunks from DB (no UI updates)
                        while (currentOffset < totalCount) {
                            val chunkStartTime = System.currentTimeMillis()

                            // Load chunk from database
                            val chunk = repository.bulkItemDao.getMinimalItemsPaged(chunkSize, currentOffset)
                            val chunkLoadTime = System.currentTimeMillis() - chunkStartTime

                            // Add chunk to list (in memory, no UI update yet)
                            allItemsList.addAll(chunk)
                            currentOffset += chunkSize

                            // Log progress but don't update UI
                            val progress = (currentOffset.toFloat() / totalCount * 100).toInt()
                            val itemsLoaded = allItemsList.size
                            Log.d("ProductListVM", "🚀 [PERF] Chunk $chunkNumber: Fetched $itemsLoaded/$totalCount items ($progress%) - Chunk took: ${chunkLoadTime}ms")

                            chunkNumber++

                            // ✅ Critical: Delay between chunks to allow GC and prevent OOM
                            // Wait 200ms between chunks to give system time to process and GC
                            if (currentOffset < totalCount) {
                                kotlinx.coroutines.delay(200)
                            }

                            // Yield to allow other coroutines to run
                            kotlinx.coroutines.yield()
                        }

                        val fetchTime = System.currentTimeMillis() - allItemsStartTime
                        Log.d("ProductListVM", "🚀 [PERF] ✅ All ${allItemsList.size} items fetched from DB in: ${fetchTime}ms (${chunkNumber - 1} chunks)")

                        // ✅ Phase 2: Update UI once with all items (single recomposition)
                        withContext(Dispatchers.Main) {
                            val uiUpdateStartTime = System.currentTimeMillis()
                            _productList.value = ArrayList(allItemsList) // Single UI update
                            _isLoadingAllItems.value = false
                            _isLoading.value = false
                            val uiUpdateTime = System.currentTimeMillis() - uiUpdateStartTime
                            val totalLoadTime = System.currentTimeMillis() - allItemsStartTime
                            Log.d("ProductListVM", "🚀 [PERF] ✅ UI updated with all ${allItemsList.size} items in: ${uiUpdateTime}ms (Total time: ${totalLoadTime}ms)")
                        }
                    } catch (e: OutOfMemoryError) {
                        Log.e("ProductListVM", "❌ [PERF] OutOfMemoryError: ${e.message}", e)
                        withContext(Dispatchers.Main) {
                            _isLoadingAllItems.value = false
                            _isLoading.value = false
                        }
                    } catch (e: Exception) {
                        Log.e("ProductListVM", "❌ [PERF] Error loading items: ${e.message}", e)
                        withContext(Dispatchers.Main) {
                            _isLoadingAllItems.value = false
                            _isLoading.value = false
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ProductListVM", "❌ [PERF] Error in initial load: ${e.message}", e)
                _isLoadingAllItems.value = false
                _isLoading.value = false
            }
        }
    }

    fun updateLocalStockStatus(tid: String, status: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.bulkItemDao.updateStatusByTid(tid, status)
        }
    }
}
