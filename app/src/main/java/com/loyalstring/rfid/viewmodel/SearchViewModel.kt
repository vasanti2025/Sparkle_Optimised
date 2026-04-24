package com.loyalstring.rfid.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
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

  /*  private var _searchItems = mutableStateListOf<SearchItem>()
    val searchItems: List<SearchItem> get() = _searchItems*/
  private val _searchItems = mutableStateListOf<SearchItem>()
    val searchItems: SnapshotStateList<SearchItem> = _searchItems

    // PERF-FIX: HashMap for O(1) EPC/RFID/ItemCode → list index lookup.
    // Previously, every RFID tag read from hardware triggered an indexOfFirst { } call
    // which is O(n) over the entire searchItems list. At high scan rates (100+ tags/sec)
    // this caused severe main-thread jank. The map is rebuilt whenever startSearch() is called.
    private val epcToIndex = HashMap<String, Int>()

    private var currentScanPower: Int = 10

    init {
        val unmatched = savedStateHandle.get<List<BulkItem>>("unmatchedItems") ?: emptyList()
        Log.d("SearchViewModel", "Received ${unmatched.size} items")
    }

    private var scanJob: Job? = null
    private var lastSoundId: Int? = null
    private var lastBlinkEpc: String? = null
    private var blinkingJob: Job? = null
    var lastSoundTime = 0L

    fun startSearch(unmatchedItems: List<BulkItem>, power: Int) {
        stopSearch() // stop old scan fully before starting new one
        currentScanPower = power
        _searchItems.clear()
        epcToIndex.clear() // PERF-FIX: Reset the O(1) lookup map

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

        // PERF-FIX: Build O(1) lookup map after list is populated.
        // All three identifier fields are indexed so any of them can be matched
        // in constant time without scanning the entire list.
        _searchItems.forEachIndexed { index, item ->
            if (item.epc.isNotBlank()) epcToIndex[item.epc.uppercase()] = index
            if (item.rfid.isNotBlank()) epcToIndex[item.rfid.uppercase()] = index
            if (item.itemCode.isNotBlank()) epcToIndex[item.itemCode.uppercase()] = index
        }

        if (readerManager.initReader()) {
            startTagScanning(power)
        }
    }

    fun startTagScanning(power: Int) {
        currentScanPower = power
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

                    // SOUND-CHANGE: Replaced proximity-based sound mapping with direct RSSI
                    // absolute-value mapping to match Searchfragment.java logic:
                    // |RSSI| < 50 → sound 4, 50-60 → sound 2, 60-70 → sound 5, >70 → sound 1
                    /*
                    val id = when {
                        proximity >= 70 -> 1
                        proximity in 61..69 -> 5
                        proximity in 51..59 -> 2
                        proximity in 1..49 -> 4
                        else -> -1
                    }
                    */
                    val rssiAbs = try { Math.abs(rssi.trim().toDouble()) } catch (e: Exception) { 0.0 }
                    val id = when {
                        rssiAbs > 0 && rssiAbs < 50 -> 4
                        rssiAbs > 50 && rssiAbs < 60 -> 2
                        rssiAbs > 60 && rssiAbs < 70 -> 5
                        rssiAbs > 70 -> 1
                        else -> -1
                    }

                    // PERF-FIX: O(1) HashMap lookup replaces O(n) indexOfFirst { } search.
                    // At high scan rates (100+ tags/sec) with large item lists, the old
                    // indexOfFirst caused the IO thread to iterate the full list for every
                    // tag read, creating CPU spikes and stalling the scan buffer reader.
                    // val index = _searchItems.indexOfFirst { it.epc.equals(epc, true) || it.rfid.equals(epc, true) || it.itemCode.equals(epc, true) }
                    val index = epcToIndex[epc.uppercase()]

                    if (index != null && index >= 0 && index < _searchItems.size) {
                        withContext(Dispatchers.Main) {
                            _searchItems[index] = _searchItems[index].copy(
                                rssi = rssi,
                                proximityPercent = proximity
                            )

                            val currentTime = System.currentTimeMillis()

                            if (id != -1 && (lastSoundId != id || currentTime - lastSoundTime > 300)) {
                                lastSoundId = id
                                readerManager.playSound(id)
                                lastSoundTime = currentTime
                            }

                            val searchedEpc = _searchItems[index].epc.trim()
                            val epcMatched = epc.equals(searchedEpc, ignoreCase = true)

                            if (epcMatched && proximity >= 40) {
                                if (lastBlinkEpc != epc || blinkingJob?.isActive != true) {
                                    startContinuousBlink(epc)
                                }
                            } else if (lastBlinkEpc == epc && proximity < 40) {
                                stopBlinkingEpc()
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
        epcToIndex.clear() // PERF-FIX: Clear lookup map together with the list
    }

    fun stopSearch() {
        scanJob?.cancel()
        scanJob = null

        blinkingJob?.cancel()
        blinkingJob = null

        readerManager.stopInventory()

        lastSoundId?.let { readerManager.stopSound(it) }
        lastSoundId = null
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

   /* private fun convertRssiToProximity(rssi: String): Int {
        return try {
            val cleanRssi = rssi
                .replace("dBm", "", ignoreCase = true)
                .trim()
                .toFloat()

            // RSSI range: -80 (far) to -40 (near)
            val normalized = ((cleanRssi + 80) / 40f) * 100f
            normalized.toInt().coerceIn(0, 100)

        } catch (e: Exception) {
            Log.e("RSSI", "Invalid RSSI: $rssi")
            0
        }
    }*/


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
            readerManager.startInventoryTag(currentScanPower, true)
        }
    }

  /*  private fun startBlinkingEpc(epc: String) {
        // Cancel any previous blinking
        blinkingJob?.cancel()

        blinkingJob = viewModelScope.launch(Dispatchers.IO) {
            val reader = readerManager.reader ?: return@launch
            val filterBank = RFIDWithUHFUART.Bank_EPC
            val filterPtr = 32
            val filterCnt = epc.length * 4

            while (isActive) {
                try {
                    // Stop inventory temporarily vasanti
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
                    // Restart inventory vasanti
                   readerManager.startInventoryTag(30, true)
                }
            }
        }
    }*/
  private fun startContinuousBlink(epc: String) {
      if (lastBlinkEpc == epc && blinkingJob?.isActive == true) return

      blinkingJob?.cancel()
      lastBlinkEpc = epc

      blinkingJob = viewModelScope.launch(Dispatchers.IO) {
          val reader = readerManager.reader ?: return@launch

          val filterBank = RFIDWithUHFUART.Bank_EPC
          val filterPtr = 32
          val filterCnt = epc.length * 4

          while (isActive && lastBlinkEpc == epc) {
              try {
                  readerManager.stopInventory()

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

                  delay(120) // LED blink visible

              } catch (e: Exception) {
                  Log.e("RFID", "Blink error: ${e.message}", e)
              } finally {
                  readerManager.startInventoryTag(currentScanPower, true)
              }

              delay(500)
          }
      }
  }



    private fun stopBlinkingEpc() {
        blinkingJob?.cancel()
        blinkingJob = null
        lastBlinkEpc = null
    }



}


/*
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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
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
    private var blinkingJob: Job? = null

    // ✅ NEW: store all nearby EPCs
    private val nearbyEpcs = mutableSetOf<String>()

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
            startBlinkingMultipleEpcs() // ✅ start LED job ONCE
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

                    val id = when {
                        proximity in 1..49 -> 4
                        proximity in 51..75 -> 2
                        proximity >= 76 -> 5
                        else -> -1
                    }

                    val index = _searchItems.indexOfFirst {
                        it.epc.equals(epc, true) ||
                                it.rfid.equals(epc, true) ||
                                it.itemCode.equals(epc, true)
                    }

                    if (index != -1 && _searchItems.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            _searchItems[index] = _searchItems[index].copy(
                                rssi = rssi,
                                proximityPercent = proximity
                            )

                            // 🔊 SOUND (UNCHANGED)
                            if (id != -1) {
                                lastSoundId?.let { readerManager.stopSound(it) }
                                lastSoundId = id
                                readerManager.playSound(id)
                            }

                            // 🔦 LED LOGIC (ONLY CHANGE)
                            if (proximity >= 40) {
                                nearbyEpcs.add(epc)
                            } else {
                                nearbyEpcs.remove(epc)
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
    }

    private fun convertRssiToProximity(rssi: String): Int {
        return try {
            val rssiValue = rssi.toFloat()
            ((rssiValue + 80).coerceAtLeast(0f) * 100f / 40f)
                .toInt()
                .coerceIn(0, 100)
        } catch (e: NumberFormatException) {
            0
        }
    }

    */
/* ================= MULTI TAG LED BLINK ================= *//*


    private fun startBlinkingMultipleEpcs() {
        blinkingJob?.cancel()

        blinkingJob = viewModelScope.launch(Dispatchers.IO) {
            val reader = readerManager.reader ?: return@launch

            while (isActive) {
                if (nearbyEpcs.isEmpty()) {
                    delay(100)
                    continue
                }

                for (epc in nearbyEpcs.toList()) {
                    if (!isActive) break

                    try {
                        readerManager.stopInventory()

                        val filterBank = RFIDWithUHFUART.Bank_EPC
                        val filterPtr = 32
                        val filterCnt = epc.length * 4

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

                        delay(80) // fast blink illusion

                    } catch (e: Exception) {
                        Log.e("RFID", "Error blinking EPC: $epc", e)
                    } finally {
                        readerManager.startInventoryTag(30, true)
                    }
                }
            }
        }
    }

    private fun stopBlinkingEpc() {
        blinkingJob?.cancel()
        blinkingJob = null
        nearbyEpcs.clear()
    }
}
*/
