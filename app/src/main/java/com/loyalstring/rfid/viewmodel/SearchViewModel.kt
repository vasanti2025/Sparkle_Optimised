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
import com.rscja.deviceapi.RFIDWithUHFUART
import com.rscja.deviceapi.interfaces.IUHF
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

    private var _searchItems = mutableStateListOf<SearchItem>()
    val searchItems: List<SearchItem> get() = _searchItems

    init {
        val unmatched = savedStateHandle.get<List<BulkItem>>("unmatchedItems") ?: emptyList()
        Log.d("SearchViewModel", "Received ${unmatched.size} items")
    }

    private var scanJob: Job? = null
    private var lastSoundId: Int? = null
    private var lastBlinkEpc: String? = null
    private var blinkingJob: Job? = null

    fun startSearch(unmatchedItems: List<BulkItem>, power: Int) {
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
        readerManager.reader?.apply {
            setTagFocus(false)
            setFastID(false)
            setDynamicDistance(0)
        }
        

        readerManager.startInventoryTag(power, true)

        scanJob?.cancel()
        scanJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val tag = readerManager.readTagFromBuffer()

                if (tag?.epc != null) {
                    val epc = tag.epc.trim()
                    val rssi = tag.rssi
                    val proximity = convertRssiToProximity(rssi)

                   /* val id = when {
                        proximity >= 70 -> 1
                        proximity in 61..69 -> 5
                        proximity in 51..59 -> 2
                        proximity in 1..49 -> 4
                        else -> -1
                    }*/
                   val id = when {
                        proximity in 1..49 -> 4
                        proximity in 51..75 -> 2
                        proximity >= 76 -> 5
                        else -> -1
                    }

                    val index = _searchItems.indexOfFirst {
                        it.epc.equals(epc, true) || it.rfid.equals(epc, true) || it.itemCode.equals(epc, true)
                    }

                    if (index != -1 && _searchItems.isNotEmpty()) {
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

                            val searchedEpc = _searchItems[index].epc.trim()
                            val epcMatched = epc.equals(searchedEpc, ignoreCase = true)

                           /* if (epcMatched && proximity >= 40 && lastBlinkEpc != epc) {
                                lastBlinkEpc = epc
                                viewModelScope.launch {
                                    lightTag(epc, searchedEpc)
                                    lastBlinkEpc = null // reset after blink
                                }
                            }*/
                            if (epcMatched && proximity >= 1) {
                                if (lastBlinkEpc != epc) {
                                    lastBlinkEpc = epc
                                    startBlinkingEpc(epc)
                                }
                            } else {
                                stopBlinkingEpc()
                                lastBlinkEpc = null
                            }

                        }
                    }
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
        lastSoundId?.let { readerManager.stopSound(it) }
        lastSoundId = null
        stopBlinkingEpc()
        lastBlinkEpc = null
    }

    private fun convertRssiToProximity(rssi: String): Int {
        return try {
            val rssiValue = rssi.toFloat()
            ((rssiValue + 80).coerceAtLeast(0f) * 100f / 40f).toInt().coerceIn(0, 100)
        } catch (e: NumberFormatException) {
            0
        }
    }

    /**
     * Updated lightTag method: only blink LED for matched EPC
     */
    private fun lightTag(scannedEpc: String, searchedEpc: String) {
        val reader = readerManager.reader ?: return
        if (!scannedEpc.equals(searchedEpc, ignoreCase = true)) return

        // Stop inventory temporarily
        readerManager.stopInventory()

        try {
            val filterBank = RFIDWithUHFUART.Bank_EPC
            val filterPtr = 32
            val filterCnt = searchedEpc.length * 4

            reader.readData(
                "00000000",
                filterBank,
                filterPtr,
                filterCnt,
                searchedEpc,
                IUHF.Bank_RESERVED,
                4,
                1
            )

            Log.d("RFID", "✅ LED triggered for EPC: $searchedEpc")

           Thread.sleep(100) // allow LED blink

        } catch (e: Exception) {
            Log.e("RFID", "Error lighting tag: ${e.message}", e)
        } finally {
            // Restart inventory
            readerManager.startInventoryTag(30, true)
        }
    }

    private fun startBlinkingEpc(epc: String) {
        // Cancel any previous blinking
        blinkingJob?.cancel()

        blinkingJob = viewModelScope.launch(Dispatchers.IO) {
            val reader = readerManager.reader ?: return@launch
            val filterBank = RFIDWithUHFUART.Bank_EPC
            val filterPtr = 32
            val filterCnt = epc.length * 4

            while (isActive) {
                try {
                    // Stop inventory temporarily
                    readerManager.stopInventory()

                    // Trigger LED blink for the EPC
                    reader.readData(
                        "00000000",
                        filterBank,
                        filterPtr,
                        filterCnt,
                        epc,
                        IUHF.Bank_RESERVED,
                        4,
                        1
                    )

                    // Small delay to allow LED to blink visually
                    delay(100) // adjust 50-150ms for blink speed

                } catch (e: Exception) {
                    Log.e("RFID", "Error blinking tag: ${e.message}", e)
                } finally {
                    // Restart inventory
                    readerManager.startInventoryTag(30, true)
                }
            }
        }
    }

    private fun stopBlinkingEpc() {
        blinkingJob?.cancel()
        blinkingJob = null
    }



}