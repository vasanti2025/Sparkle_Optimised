package com.loyalstring.rfid

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.Coil
import coil.ImageLoader
import coil.decode.SvgDecoder
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.worker.LocaleHelper
import com.rscja.deviceapi.RFIDWithUHFUART
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.posprinter.POSConnect
import javax.inject.Inject

@HiltAndroidApp
class SparkleRFIDApplication : Application(), Configuration.Provider {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/*    override fun attachBaseContext(base: Context?) {
        if (base == null) {
            super.attachBaseContext(base)
            return
        }

        try {
            val userPrefs = UserPreferences.getInstance(base)
            val langCode = userPrefs.getAppLanguage().ifBlank { "en" }

            // ✅ Properly wrap context
            val localizedContext = LocaleHelper.applyLocale(base, langCode)

            super.attachBaseContext(localizedContext)
            Log.d("AppLocale", "✅ Locale applied: $langCode")
        } catch (e: Exception) {
            super.attachBaseContext(base)
            Log.e("AppLocale", "⚠️ Locale setup failed: ${e.message}")
        }
    }*/


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

        // PERF-FIX: Load language preference once and set locale only once.
        // Previously setApplicationLocales() was called twice with two separate
        // UserPreferences.getInstance() calls, doubling the SharedPreferences I/O
        // and locale-rebuild cost on the main thread.
        /*
        POSConnect.init(this)
        val prefs = UserPreferences.getInstance(this)
        val rawLang = prefs.getAppLanguage()
        val langCode = rawLang?.ifBlank { "en" } ?: "en"
        Log.d("LocaleDebug", "prefs langCode = '$rawLang' -> using '$langCode'")
        val localeList = LocaleListCompat.forLanguageTags(langCode)
        AppCompatDelegate.setApplicationLocales(localeList)
        val userPrefs = UserPreferences.getInstance(this)
        val savedLang = userPrefs.getAppLanguage().ifBlank { "en" }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(savedLang))
        val cfg = resources.configuration
        Log.d("LocaleDebug", "after setApplicationLocales: cfg.locales[0] = ${cfg.locales[0].toLanguageTag()}")
        */
        val userPrefs = UserPreferences.getInstance(this)
        val langCode = userPrefs.getAppLanguage().ifBlank { "en" }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langCode))
        Log.d("LocaleDebug", "Locale applied once in Application: '$langCode'")

        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .components {
                    add(SvgDecoder.Factory())
                }
                .build()
        )

        // PERF-FIX: Move POSConnect.init() to background thread so it does not
        // block the main thread during app startup. It is a third-party library
        // that may perform I/O (socket/file) internally.
        // POSConnect.init(this)  // old: was blocking main thread
        applicationScope.launch {
            try {
                POSConnect.init(this@SparkleRFIDApplication)
                Log.d("SparkleRFID", "POSConnect initialized on background thread")
            } catch (ex: Exception) {
                Log.e("SparkleRFID", "POSConnect init failed: ${ex.message}")
            }
        }

        // RFID reader initialization runs on background thread (unchanged)
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
