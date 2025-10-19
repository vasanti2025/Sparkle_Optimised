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

    var soundMap: HashMap<Int?, Int?> = HashMap()
    private var soundPool: SoundPool? = null
    private var volumeRatio = 0f
    private var am: AudioManager? = null

    fun initReader(): Boolean {
        return try {
            if (_reader == null) {
                _reader = RFIDWithUHFUART.getInstance()
            }
            initSounds()
            val success = _reader?.init() ?: false
            if (success) {
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
        return _reader?.readTagFromBuffer()
    }

    fun startInventoryTag(selectedPower: Int, search: Boolean): Boolean {
        _reader?.setPower(selectedPower)
        if(!search) {
            soundPlayer.startLoopingSound()
        }
        val started = _reader?.startInventoryTag() ?: false
        Log.d("RFID", "startInventoryTag: $started")
        return started
    }


    fun stopInventory() {
        _reader?.stopInventory()
        soundPlayer.stopSound()
        Log.d("RFID", "Inventory stopped")
    }

    fun release() {
        _reader?.free()
        _reader = null
    }

    fun initSounds() {
        soundPool = SoundPool(10, AudioManager.STREAM_MUSIC, 5)
        soundMap[1] = soundPool?.load(context, R.raw.barcodebeep, 1)
        soundMap[2] = soundPool?.load(context, R.raw.sixty, 1)
        soundMap[3] = soundPool?.load(context, R.raw.seventy, 1)
        soundMap[4] = soundPool?.load(context, R.raw.fourty, 1)
        soundMap[5] = soundPool?.load(context, R.raw.found2, 1)
        am = context.getSystemService(AUDIO_SERVICE) as AudioManager
    }

    fun playSound(type: Int = 1, loop: Int = 1) {
        val maxVolume = am?.getStreamMaxVolume(AudioManager.STREAM_MUSIC)?.toFloat() ?: 1f
        val currentVolume = am?.getStreamVolume(AudioManager.STREAM_MUSIC)?.toFloat() ?: 1f
        volumeRatio = currentVolume / maxVolume
        soundMap[type]?.let {
            soundPool?.play(it, volumeRatio, volumeRatio, 1, loop, 1f)
        }
    }

    fun stopSound(id: Int) {
        soundPool?.stop(id)
    }
}

class SoundPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    fun startLoopingSound() {
        if (mediaPlayer == null) {
            mediaPlayer = MediaPlayer.create(context, R.raw.barcodebeep)
            mediaPlayer?.isLooping = true
        }
        mediaPlayer?.start()
    }

    fun stopSound() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
