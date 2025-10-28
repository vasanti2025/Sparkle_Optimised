package com.loyalstring.rfid.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.local.entity.SearchItem
import com.loyalstring.rfid.data.reader.RFIDReaderManager
import com.loyalstring.rfid.repository.BulkRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val readerManager: RFIDReaderManager,
    private val bulkRepositoryImpl: BulkRepositoryImpl,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /// private val _unmatchedItems = savedStateHandle.get<List<BulkItem>>("unmatchedItems") ?: emptyList()
    private var _searchItems = mutableStateListOf<SearchItem>()
    val searchItems: List<SearchItem> get() = _searchItems

    init {
        val unmatched = savedStateHandle.get<List<BulkItem>>("unmatchedItems") ?: emptyList()
        Log.d("SearchViewModel", "Received ${unmatched.size} items")
      //  startSearch(unmatched)
    }

    private var scanJob: Job? = null
    private var lastSoundId: Int? = null




    fun startSearch(unmatchedItems: List<BulkItem>,power: Int) {
        _searchItems.clear()
        _searchItems.addAll(unmatchedItems.map { item ->
            val epcValue = when {
                !item.epc.isNullOrBlank() -> item.epc!!
                !item.rfid.isNullOrBlank() -> item.rfid!!
                !item.itemCode.isNullOrBlank() -> item.itemCode!!
                else -> ""
            }

            SearchItem(
                epc = epcValue,
                itemCode = item.itemCode ?: "",
                productName = item.productName ?: "",
                rfid = item.rfid ?: ""
            )
        })


        if (readerManager.initReader()) {
            startTagScanning(power)

        }
    }

    fun startTagScanning(power: Int) {
        readerManager.startInventoryTag(power,true)

        scanJob?.cancel()
        scanJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val tag = readerManager.readTagFromBuffer()
                if (tag?.epc != null) {
                    val epc = tag.epc
                    val rssi = tag.rssi
                    val proximity = convertRssiToProximity(rssi)
                    var id = -1

                    // Assign ID based on proximity
                    id = when {
                        proximity in 1..49 -> 4
                        proximity in 51..59 -> 2
                        proximity in 61..69 -> 5
                        proximity >= 70 -> 1
                        else -> -1
                    }


                    // Update UI list
                    val index = _searchItems.indexOfFirst {
                        it.epc.trim().equals(epc.trim(), ignoreCase = true) ||
                                it.rfid.trim().equals(epc.trim(), ignoreCase = true) ||
                                it.itemCode.trim().equals(epc.trim(), ignoreCase = true)
                    }
                    if (index != -1) {
                        withContext(Dispatchers.Main) {
                            _searchItems[index] = _searchItems[index].copy(
                                rssi = rssi,
                                proximityPercent = proximity
                            )

                            if (id != -1) {
                                lastSoundId?.let { readerManager.stopSound(it) }
                                lastSoundId = id
                                readerManager.playSound(id)
                            }
                        }
                    } else {
                        Log.d("SEARCH_SCAN", "No match found for tag $epc")
                    }


                    /*
                                        // Update UI list
                                        val index = _searchItems.indexOfFirst { it.epc == epc }
                                        if (index != -1) {
                                            withContext(Dispatchers.Main) {
                                                _searchItems[index] = _searchItems[index].copy(
                                                    rssi = rssi,
                                                    proximityPercent = proximity
                                                )
                                            }
                                        }*/
                } else {
                    delay(100)
                }
            }
        }
    }
    suspend fun getAllBulkItemsFromDb(): List<BulkItem> {
        return bulkRepositoryImpl.getAllBulkItems().first()
    }

    fun clearSearchItems() {
        _searchItems.clear()
    }


    fun stopSearch() {
        scanJob?.cancel()
        readerManager.stopInventory()
        lastSoundId?.let { readerManager.stopSound(it) } // ✅ stop the same sound
        lastSoundId = null
    }

    private fun convertRssiToProximity(rssi: String): Int {
        return try {
            val rssiValue = rssi.toFloat()  // Change here
            ((rssiValue + 80).coerceAtLeast(0f) * 100f / 40f).toInt().coerceIn(0, 100)
        } catch (e: NumberFormatException) {
            0 // Default proximity if parsing fails
        }
    }
}
