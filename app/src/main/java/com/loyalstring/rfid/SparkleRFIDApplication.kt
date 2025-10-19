package com.loyalstring.rfid

import android.app.Application
import android.util.Log
import com.rscja.deviceapi.RFIDWithUHFUART
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@HiltAndroidApp
class SparkleRFIDApplication : Application() {
    var mReader: RFIDWithUHFUART? = null

    override fun onCreate() {
        super.onCreate()
        val t0 = System.nanoTime()
        Log.d("StartupTrace", "Application.onCreate start")

        // Initialize RFID reader in background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reader = RFIDWithUHFUART.getInstance()
                if (reader != null && reader.init()) {
                    mReader = reader
                    Log.d("SparkleRFID", "RFID Reader initialized successfully")
                } else {
                    Log.e("SparkleRFID", "Failed to initialize RFID Reader")
                }
            } catch (ex: Exception) {
                Log.e("SparkleRFID", "Exception initializing RFID: ${ex.message}")
            }
        }
        val t1 = System.nanoTime()
        Log.d("StartupTrace", "Application.onCreate end ${(t1 - t0)/1_000_000} ms")
    }
}

/*@HiltAndroidApp
class SparkleRFIDApplication : Application() {
    var mReader: RFIDWithUHFUART? = null
    init {
        try {
            mReader = RFIDWithUHFUART.getInstance()
        } catch (ex: Exception) {
            println("exception : $ex")
        }

        mReader?.init()
    }

    override fun onCreate() {
        super.onCreate()
*//*
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("UncaughtException", "App crashed with: ${throwable.message}", throwable)
        }*//*
    }

}*/
