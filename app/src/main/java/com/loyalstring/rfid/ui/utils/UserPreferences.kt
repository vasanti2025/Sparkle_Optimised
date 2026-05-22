package com.loyalstring.rfid.ui.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.loyalstring.rfid.data.model.login.Clients
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val PREF_NAME = "user_prefs"
        private const val KEY_TOKEN = "token"
        private const val KEY_EMPLOYEE = "employee"

        private const val KEY_USERNAME = "remember_username"
        private const val KEY_PASSWORD = "remember_password"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_SHEET_URL = "sheet_url"
        private const val KEY_CLIENT = "client"
        private const val KEY_RFIDTYPE = "remember_rfidType"

        // ✅ New Keys for Counters
        const val KEY_PRODUCT_COUNT = "product_count"
        const val KEY_INVENTORY_COUNT = "inventory_count"
        const val KEY_SEARCH_COUNT = "search_count"
        const val KEY_ORDER_COUNT = "orders_count"
        const val KEY_STOCK_TRANSFER_COUNT = "stock_transfer_count"
        const val KEY_AUTOSYNC_ENABLED = "autosync_enabled"
        const val KEY_AUTOSYNC_INTERVAL_MIN = "autosync_interval_min"
        const val KEY_CUSTOM_API_URL="custom_api_url"
        const  val KEY_BACKUP_EMAIL = "backup_email"
        const  val KEY_LOCATION_SYNC = "location_sync"
        const val KEY_USER_ID="user_id"
        const val KEY_BRANCH_ID="branch_id"
        const val KEY_ORG="organisation_name"
        const val DEVICE_ID="device_id"
        private const val KEY_APP_LANGUAGE = "app_language"

        private  const val KEY_LOCAL_WIFI_MODE = "local_wifi_mode"
        private const val KEY_WEB_REUSABLE_TAG = "web_reusable_tag"

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

    // ---------------- TOKEN ----------------
    fun saveToken(token: String) {
        prefs.edit { putString(KEY_TOKEN, token) }
    }

    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    // ---------------- USERNAME ----------------
    fun saveUserName(username: String) {
        prefs.edit { putString(KEY_USERNAME, username) }
    }

    fun getSavedUsername(): String = prefs.getString(KEY_USERNAME, "") ?: ""

    // ---------------- PASSWORD ----------------
    fun getSavedPassword(): String = prefs.getString(KEY_PASSWORD, "") ?: ""

    // ---------------- EMPLOYEE ----------------
    fun <T> saveEmployee(employee: T) {
        val json = gson.toJson(employee)
        prefs.edit { putString(KEY_EMPLOYEE, json) }
    }

    fun <T> getEmployee(clazz: Class<T>): T? {
        val json = prefs.getString(KEY_EMPLOYEE, null)
        return if (json != null) gson.fromJson(json, clazz) else null
    }

    //---------------- LOGIN / LOGOUT ----------------
    fun saveLoginCredentials(username: String, password: String, rememberMe: Boolean,rfidtype:String, userId: Int, branchId: Int, organisationName: String) {
        prefs.edit().apply {
            putBoolean(KEY_REMEMBER_ME, rememberMe)
            if (rememberMe){
                putString(KEY_USERNAME, username)
                putString(KEY_PASSWORD, password)
                putString(KEY_RFIDTYPE,rfidtype)
                putInt(KEY_USER_ID,userId)
                putInt(KEY_BRANCH_ID,branchId)
                putString(KEY_ORG,organisationName)
            }else {
                remove(KEY_USERNAME)
                remove(KEY_PASSWORD)
                remove(KEY_RFIDTYPE)
                remove(KEY_USER_ID)
                remove(KEY_BRANCH_ID)
                remove(KEY_ORG)

            /*    putString(KEY_USERNAME, username)
                putString(KEY_PASSWORD, password)
                putString(KEY_RFIDTYPE,rfidtype)
                putInt(KEY_USER_ID,userId)
                putInt(KEY_BRANCH_ID,branchId)*/

            }
            apply()
        }
    }

    fun isRememberMe(): Boolean = prefs.getBoolean(KEY_REMEMBER_ME, false)

    fun getUserId(): Int? {
        return prefs.getInt(KEY_USER_ID, 0)
    }

    fun setUserId(userId: Int) {
        prefs.edit { putInt(KEY_USER_ID, userId) }
    }

    fun setLoggedIn(loggedIn: Boolean) {
        prefs.edit { putBoolean(KEY_LOGGED_IN, loggedIn) }
    }

    fun saveBranchId(branchId: Int) {
        prefs.edit { putInt(KEY_BRANCH_ID, branchId) }
    }

    fun getBranchID(): Int? {
        return prefs.getInt(KEY_BRANCH_ID, 0)
    }

    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_LOGGED_IN, false)

    fun logout() {
        //prefs.edit { clear() }
        prefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_USERNAME)
            .remove(KEY_PASSWORD)
            .remove(KEY_REMEMBER_ME)
            .remove(KEY_RFIDTYPE)
            .remove(KEY_CUSTOM_API_URL)
            .apply()
    }

    // ---------------- CLIENT ----------------
    fun saveClient(client: Clients) {
        val json = gson.toJson(client)
        prefs.edit { putString(KEY_CLIENT, json) }
    }

    fun getClient(): Clients? {
        val json = prefs.getString(KEY_CLIENT, null)
        return if (json != null) gson.fromJson(json, Clients::class.java) else null
    }

    fun saveBoolean(key: String, value: Boolean) {
        prefs.edit { putBoolean(key, value) }
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean {
        return prefs.getBoolean(key, default)
    }

    // ---------------- SHEET URL ----------------
    fun saveSheetUrl(url: String) {
        prefs.edit { putString(KEY_SHEET_URL, url) }
    }

    fun getSheetUrl(): String? {
        return prefs.getString(KEY_SHEET_URL, "")
    }

    // ---------------- GENERIC CLEAR ----------------
    fun clearAll() {
      //  prefs.edit { clear() }

        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_EMPLOYEE)
            .remove(KEY_USERNAME)
            .remove(KEY_PASSWORD)
            .remove(KEY_REMEMBER_ME)
            .remove(KEY_LOGGED_IN)
            .remove(KEY_CLIENT)
            .remove(KEY_RFIDTYPE)
            .remove(KEY_CUSTOM_API_URL)
            .remove(KEY_USER_ID)
            .remove(KEY_BRANCH_ID)
            .remove(KEY_ORG)
            .apply()
    }

    // ---------------- INT HELPERS (for counters) ----------------
    fun saveInt(key: String, value: Int) {
        prefs.edit { putInt(key, value) }
    }

    fun getInt(key: String, default: Int = 0): Int {
        try {
            return prefs.getInt(key, default)
        }catch (e : Exception)
        {
            return default
        }

    }

    fun saveCustomApi(url: String) {
        prefs.edit().putString(KEY_CUSTOM_API_URL, url).apply()
    }

    fun getCustomApi(): String? = prefs.getString(KEY_CUSTOM_API_URL, null)

    // ---------------- BACKUP EMAIL ----------------


    fun saveBackupEmail(email: String) {
        prefs.edit { putString(KEY_BACKUP_EMAIL, email) }
    }

    fun getBackupEmail(): String? {
        return prefs.getString(KEY_BACKUP_EMAIL, "")
    }

    fun isAutoSyncEnabled(): Boolean? {
        return if (prefs.contains(KEY_LOCATION_SYNC)) {
            prefs.getBoolean(KEY_LOCATION_SYNC, true)
        } else null
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOCATION_SYNC, enabled).apply()
    }
    fun contains(key: String): Boolean {
        return prefs.contains(key)
    }

    fun saveAppLanguage(languageCode: String) {
        Log.d("LocaleDebug", "saveAppLanguage called with -> $languageCode")
        val success = prefs.edit().putString(KEY_APP_LANGUAGE, languageCode).commit()
        Log.d("LocaleDebug", "save success -> $success")
        Log.d("LocaleDebug", "saved value now -> ${prefs.getString(KEY_APP_LANGUAGE, "en")}")
    }

    fun getAppLanguage(): String {
        val value = prefs.getString(KEY_APP_LANGUAGE, "en") ?: "en"
        Log.d("LocaleDebug", "getAppLanguage pref-> $value")
        return value
    }

    fun saveOrganization(org: String) {
        val json = gson.toJson(org)
        prefs.edit { putString(KEY_ORG, json) }
    }

    // ✅ Get Organization
    fun getOrganization(): String? {
        val json = prefs.getString(KEY_ORG, null)
        return if (json != null) gson.fromJson(json, String()::class.java) else null
    }

    // (optional) clear
    fun clearOrganization() {
        prefs.edit { remove(KEY_ORG) }
    }

    fun saveDeviceId(deviceId: String) {
        prefs.edit { putString(DEVICE_ID, deviceId) }
    }

    fun getDeviceId(): String? {
        return prefs.getString(DEVICE_ID, "")
    }

    fun saveBranchIds(branchIds: List<Int>) {
        val json = com.google.gson.Gson().toJson(branchIds)

        val saved = prefs.edit()
            .putString("branch_ids", json)
            .commit()

        Log.d("USER_PREF", "saveBranchIds saved=$saved json=$json")
    }

    fun getBranchIds(): List<Int> {
        val json = prefs.getString("branch_ids", "[]") ?: "[]"
        val type = object : com.google.gson.reflect.TypeToken<List<Int>>() {}.type
        return com.google.gson.Gson().fromJson(json, type)
    }


    fun setLocalWifiMode(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_LOCAL_WIFI_MODE, enabled)
            .apply()
    }

    fun isLocalWifiModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_LOCAL_WIFI_MODE, false)
    }

    fun setWebReusableTagEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_WEB_REUSABLE_TAG, value).apply()
    }

    fun isWebReusableTagEnabled(): Boolean {
        return prefs.getBoolean(KEY_WEB_REUSABLE_TAG, true)
    }
}

