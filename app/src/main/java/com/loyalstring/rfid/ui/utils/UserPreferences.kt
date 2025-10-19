package com.loyalstring.rfid.ui.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import androidx.core.content.edit
import com.loyalstring.rfid.data.model.login.Clients

class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {


    companion object {
        private const val PREF_NAME = "user_prefs"
        private const val KEY_TOKEN = "token"
        private const val KEY_EMPLOYEE = "employee"
        // private const val KEY_USERNAME = "username"

        private const val KEY_USERNAME = "remember_username"
        private const val KEY_PASSWORD = "remember_password"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_SHEET_URL = "sheet_url"
        private const val KEY_CLIENT = "client"

        private val gson = Gson()

        @Volatile
        private var instance: UserPreferences? = null

        fun getInstance(context: Context): UserPreferences {
            return instance ?: synchronized(this) {
                instance ?: UserPreferences(context.applicationContext).also { instance = it }
            }
        }

    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        prefs.edit() { putString(KEY_TOKEN, token) }
    }

    fun saveUserName(username: String) {
        prefs.edit() { putString(KEY_USERNAME, username) }

    }

    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }


    fun <T> saveEmployee(employee: T) {
        val json = gson.toJson(employee)
        prefs.edit() { putString(KEY_EMPLOYEE, json) }
    }

    fun <T> getEmployee(clazz: Class<T>): T? {
        val json = prefs.getString(KEY_EMPLOYEE, null)
        return if (json != null) gson.fromJson(json, clazz) else null
    }

    fun clearAll() {
        prefs.edit() { clear() }
    }

    fun saveSheetUrl(url: String) {
        prefs.edit() { putString(KEY_SHEET_URL, url) }
    }

    fun getSheetUrl(): String? {
        return prefs.getString(KEY_SHEET_URL, "")
    }

    fun saveLoginCredentials(username: String, password: String, rememberMe: Boolean) {
        prefs.edit().apply {
            putBoolean(KEY_REMEMBER_ME, rememberMe)
            if (rememberMe) {
                putString(KEY_USERNAME, username)
                putString(KEY_PASSWORD, password)
            } else {
                remove(KEY_USERNAME)
                remove(KEY_PASSWORD)
            }
            apply()
        }
    }

    fun getSavedUsername(): String = prefs.getString(KEY_USERNAME, "") ?: ""
    fun getSavedPassword(): String = prefs.getString(KEY_PASSWORD, "") ?: ""
    fun isRememberMe(): Boolean = prefs.getBoolean(KEY_REMEMBER_ME, false)

    fun setLoggedIn(loggedIn: Boolean) {
        prefs.edit() { putBoolean(KEY_LOGGED_IN, loggedIn) }
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_LOGGED_IN, false)

    fun logout() {
        prefs.edit() { clear() }
        prefs.edit()
            .remove(KEY_USERNAME)
            .remove(KEY_PASSWORD)
            .remove(KEY_REMEMBER_ME)
            .apply()
    }

    fun saveClient(client: Clients) {
        val json = gson.toJson(client)
        prefs.edit { putString(KEY_CLIENT, json) }
    }

    fun getClient(): Clients? {
        val json = prefs.getString(KEY_CLIENT, null)
        return if (json != null) gson.fromJson(json, Clients::class.java) else null
    }
}
