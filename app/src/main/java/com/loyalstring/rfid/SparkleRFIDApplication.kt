package com.loyalstring.rfid

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.rscja.deviceapi.RFIDWithUHFUART
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class SparkleRFIDApplication : Application(), Configuration.Provider {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    var mReader: RFIDWithUHFUART? = null

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.DEBUG)
            .build()

    override fun onCreate() {
        super.onCreate()
        Log.d("StartupTrace", "Application.onCreate start")

        applicationScope.launch {
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

        Log.d("StartupTrace", "Application.onCreate end")
    }
}
