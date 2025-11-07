package com.loyalstring.rfid

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.loyalstring.rfid.ui.utils.UserPreferences
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
                if (reader != null && reader.init(this@SparkleRFIDApplication)) {
                    mReader = reader
                    Log.d("SparkleRFID", "RFID Reader initialized successfully")
                } else {
                    Log.e("SparkleRFID", "Failed to initialize RFID Reader")
                }
            } catch (ex: Exception) {
                Log.e("SparkleRFID", "Exception initializing RFID: ${ex.message}")
            }
        }
        val userPrefs = UserPreferences.getInstance(this)
        ensureDefaultCounters(userPrefs)
        Log.d("StartupTrace", "Application.onCreate end")
    }


    private fun ensureDefaultCounters(userPrefs: UserPreferences) {
        val defaults = mapOf(
            UserPreferences.KEY_PRODUCT_COUNT to 5,
            UserPreferences.KEY_INVENTORY_COUNT to 30,
            UserPreferences.KEY_SEARCH_COUNT to 30,
            UserPreferences.KEY_ORDER_COUNT to 10,
            UserPreferences.KEY_STOCK_TRANSFER_COUNT to 10
        )

        defaults.forEach { (key, defaultValue) ->
            if (!userPrefs.contains(key)) {
                userPrefs.saveInt(key, defaultValue)
                Log.d("AppInit", "Default value set for $key = $defaultValue")
            }
        }
    }
}
