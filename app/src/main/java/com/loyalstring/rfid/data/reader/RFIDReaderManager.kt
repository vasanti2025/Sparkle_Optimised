package com.loyalstring.rfid.data.reader

import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import com.loyalstring.rfid.R
import com.rscja.deviceapi.RFIDWithUHFUART
import com.rscja.deviceapi.entity.UHFTAGInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RFIDReaderManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val soundPlayer = SoundPlayer(context)

    private var _reader: RFIDWithUHFUART? = null
    val reader: RFIDWithUHFUART?
        get() = _reader

    // Skips re-running _reader?.init(context) + initSounds() on every initReader() call.
    // Without this, SearchViewModel rebuilds SoundPool and reinitializes hardware on every
    // startSearch(), adding significant latency each time the SearchScreen auto-scan fires.
    @Volatile private var isReaderReady = false

    // Tracks whether startInventoryTag() was called and succeeded, so stopInventory() can skip
    // the 2.5-second hardware timeout (5 retries × 500ms) when the hardware isn't scanning.
    @Volatile private var isInventoryActive = false

    var soundMap: HashMap<Int?, Int?> = HashMap()
    private var soundPool: SoundPool? = null
    private var volumeRatio = 0f
    private var am: AudioManager? = null
    private val soundStreamIds = mutableMapOf<Int, Int>()
    @Volatile private var soundsInitialized = false

    fun initReader(): Boolean {
        if (isReaderReady && _reader != null) return true
        return try {
            if (_reader == null) {
                _reader = RFIDWithUHFUART.getInstance()
            }
            initSounds()
            val success = _reader?.init(context) ?: false
            if (success) {
                isReaderReady = true
                Log.d("RFID", "Reader initialized successfully")
            } else {
                Log.e("RFID", "Reader initialization failed")
            }
            success
        } catch (e: Exception) {
            Log.e("RFID", "Exception initializing reader: ${e.message}", e)
            false
        }
    }

    fun readTagFromBuffer(): UHFTAGInfo? {
        return try {
            _reader?.readTagFromBuffer()
        } catch (e: Exception) {
            Log.w("RFID", "readTagFromBuffer failed: ${e.message}")
            null
        }
    }

    /** Discard stale tags left in the hardware buffer before a new inventory session. */
    fun drainStaleBuffer(maxDrain: Int = 64) {
        var drained = 0
        while (drained < maxDrain) {
            val tag = readTagFromBuffer() ?: break
            if (tag.epc.isNullOrBlank()) break
            drained++
        }
        if (drained > 0) {
            Log.d("RFID", "Drained $drained stale tag(s) from buffer")
        }
    }

    /** Read up to [maxTags] tags in one pass to reduce JNI round-trips during high-volume scans. */
    fun readTagsFromBuffer(maxTags: Int = 32): List<UHFTAGInfo> {
        val tags = ArrayList<UHFTAGInfo>(maxTags)
        repeat(maxTags) {
            val tag = readTagFromBuffer() ?: return tags
            if (tag.epc.isNullOrBlank()) return tags
            tags.add(tag)
        }
        return tags
    }

    /** Pre-create looping scan audio so the first inventory start is not blocked on MediaPlayer.create(). */
    fun prepareInventorySound() {
        soundPlayer.prepareLoopingSound()
    }

    private fun configureReaderForInventory() {
        try {
            _reader?.apply {
                setTagFocus(false)
                setFastID(false)
                setDynamicDistance(0)
            }
        } catch (e: Exception) {
            Log.w("RFID", "configureReaderForInventory failed: ${e.message}")
        }
    }

    fun startInventoryTag(selectedPower: Int, search: Boolean): Boolean {
        _reader?.setPower(selectedPower)
        if (!search) {
            configureReaderForInventory()
            soundPlayer.startLoopingSound()
        }
        val started = _reader?.startInventoryTag() ?: false
        if (started) isInventoryActive = true
        Log.d("RFID", "startInventoryTag: $started")
        return started
    }


    fun stopInventory() {
        // Always attempt the hardware stop. The isInventoryActive guard was removed because
        // releaseScanning() sets it to false without sending a hardware command, which caused
        // subsequent stopInventory() calls (from resetForDisplay, onScanStopped, scanSingleTagRaw)
        // to silently skip the hardware stop — leaving the device scanning indefinitely.
        // releaseScanning() already handles the fast no-hardware-call path for navigation;
        // stopInventory() is only called for genuine explicit stops where we must try hardware.
        isInventoryActive = false
        _reader?.stopInventory()
        soundPlayer.stopSound()
        Log.d("RFID", "Inventory stopped")
    }

    // Fast release used when navigating away from a scan screen to a different scan screen.
    // Resets the inventory flag and stops sound WITHOUT sending the stopInventory() hardware
    // command — that command takes 2.5s (5 retries × 500ms) and always returns error -1 on this
    // device. The next startInventoryTag() call naturally restarts a clean inventory session.
    fun releaseScanning() {
        isInventoryActive = false
        soundPlayer.stopSound()
        Log.d("RFID", "Inventory released (no hardware stop)")
    }

    fun release() {
        _reader?.free()
        _reader = null
        isReaderReady = false
        isInventoryActive = false
    }

    fun initSounds() {
        if (soundsInitialized) return
        soundsInitialized = true
        soundPool = SoundPool(10, AudioManager.STREAM_MUSIC, 5)
        soundMap[1] = soundPool?.load(context, R.raw.barcodebeep, 1)
        soundMap[2] = soundPool?.load(context, R.raw.sixty, 1)
        soundMap[3] = soundPool?.load(context, R.raw.seventy, 1)
        soundMap[4] = soundPool?.load(context, R.raw.fourty, 1)
        soundMap[5] = soundPool?.load(context, R.raw.found2, 1)
        am = context.getSystemService(AUDIO_SERVICE) as AudioManager
    }

   /* fun playSound(type: Int = 1, loop: Int = 1) {
        val maxVolume = am?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.toFloat() ?: 1f
        val currentVolume = am?.getStreamVolume(AudioManager.STREAM_MUSIC)?.toFloat() ?: 1f
        volumeRatio = currentVolume / maxVolume
        soundMap[type]?.let {
            soundPool?.play(it, volumeRatio, volumeRatio, 1, loop, 1f)
        }
    }*/
   fun playSound(id: Int, loop: Int = 0) {
       try {
           // Stop all active sound streams before starting a new one.
           // SoundPool plays multiple streams simultaneously; without this, a previous
           // sound (e.g. id=2 at 55 RSSI) keeps playing when RSSI shifts to a new bucket
           // (e.g. id=4), causing two sounds to overlap and distort.
           soundStreamIds.values.forEach { streamId -> soundPool?.stop(streamId) }
           soundStreamIds.clear()

           val audioMaxVolume =
               am?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.toFloat() ?: 1f

           val audioCurrentVolume =
               am?.getStreamVolume(AudioManager.STREAM_MUSIC)?.toFloat() ?: 1f

           volumeRatio = audioCurrentVolume / audioMaxVolume

           val soundId = soundMap[id] ?: return

           val streamId = soundPool?.play(
               soundId,
               volumeRatio,   // left volume
               volumeRatio,   // right volume
               1,             // priority
               loop,          // loop count (0 = no loop, -1 = infinite)
               1f             // playback rate
           ) ?: return

           soundStreamIds[id] = streamId
       } catch (e: Exception) {
           e.printStackTrace()
       }
   }


    fun stopSound(id: Int) {
        // SoundPool.stop() requires the stream ID returned by play(), not the sound asset ID.
        // Previously this passed `id` (1-5) directly, which targeted the wrong stream and
        // left audio playing indefinitely.
        val streamId = soundStreamIds[id] ?: return
        soundPool?.stop(streamId)
        soundStreamIds.remove(id)
    }
}

class SoundPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    fun prepareLoopingSound() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context, R.raw.barcodebeep)
            mediaPlayer?.isLooping = true
        }
    }

    fun startLoopingSound() {
        prepareLoopingSound()
        if (mediaPlayer?.isPlaying != true) {
            mediaPlayer?.start()
        }
    }

    fun stopSound() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
