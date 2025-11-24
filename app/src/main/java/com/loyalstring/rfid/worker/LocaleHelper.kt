
package com.loyalstring.rfid.worker

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.loyalstring.rfid.ui.utils.UserPreferences
import java.util.Locale
object LocaleHelper {
    fun applyLocale(context: Context, langCode: String): Context {
        val locale = Locale(langCode)
        Locale.setDefault(locale)

        val config = context.resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        return context.createConfigurationContext(config)
    }

    fun applySavedLocale(context: Context): Context {
        val userPrefs = UserPreferences.getInstance(context)
        val langCode = userPrefs.getAppLanguage().ifBlank { "en" }
        return applyLocale(context, langCode)
    }

    fun getLanguage(context: Context): String {
        return context.resources.configuration.locales[0].language
    }
}

