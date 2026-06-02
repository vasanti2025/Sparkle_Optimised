package com.loyalstring.rfid.ui.screens
import android.os.Looper
import android.os.Handler
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority

import android.content.IntentSender


import com.google.android.gms.common.api.ResolvableApiException

import com.google.android.gms.location.LocationSettingsRequest

import com.google.android.gms.location.SettingsClient

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.location.Geocoder
import android.net.Uri

import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource

import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.local.db.AppDatabase
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.navigation.Screens
import com.loyalstring.rfid.ui.utils.AutoSyncSetting
import com.loyalstring.rfid.ui.utils.GradientButton
import com.loyalstring.rfid.ui.utils.ToastUtils
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.utils.BackupUtils
import com.loyalstring.rfid.viewmodel.SettingsViewModel
import com.loyalstring.rfid.viewmodel.UiState1
import com.loyalstring.rfid.worker.EmailSender
import com.loyalstring.rfid.worker.LocaleHelper
import com.loyalstring.rfid.worker.SyncDataWorker
import com.loyalstring.rfid.worker.cancelPeriodicSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit


// ---------------- MENU ITEM TYPES ----------------
sealed class SettingType {
    object Counter : SettingType()
    object Action : SettingType()
}

data class SettingsMenuItem(

    val key: String,
    val title: String,
    val icon: ImageVector,
    val type: SettingType,
    val defaultValue: Int? = null,
    val subtitle: String? = null,
    val onClick: (() -> Unit)? = null,
    val hasToggle: Boolean = false,              // ✅ new
    val isToggled: Boolean = false,              // ✅ new
    val onToggleChange: ((Boolean) -> Unit)? = null, // ✅ new

)

// ---------------- SETTINGS SCREEN ----------------
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    navController: NavHostController,
    userPreferences: UserPreferences
) {
    val viewModel: SettingsViewModel = hiltViewModel()

    var showSheetInput by remember { mutableStateOf(false) }
    var showAutoSyncDialog by remember { mutableStateOf(false) }
    var sheetUrl by remember { mutableStateOf(userPreferences.getSheetUrl()) }

    var showRatesEditor by remember { mutableStateOf(false) }


    val LOCATION_SYNC_DATA_WORKER = "loaction_sync_data_worker"


    val updateState = viewModel.updateDailyRatesState.collectAsState()

    var showCustomApiDialog by remember { mutableStateOf(false) }
    var customApi by remember { mutableStateOf("") }

    var showBackupDialog by remember { mutableStateOf(false) }

    var showClearDataConfirm by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    val context: Context = LocalContext.current

    var locationAutoSyncEnabled by remember {
        mutableStateOf(userPreferences.isAutoSyncEnabled() ?: true)
    }

    var localWifiModeEnabled by remember {
        mutableStateOf(userPreferences.isLocalWifiModeEnabled())
    }

    var webReusableTagEnabled by remember {
        mutableStateOf(userPreferences.isWebReusableTagEnabled())
    }

    var androidDeviceIp by remember {
        mutableStateOf(getAndroidDeviceIp())
    }
    var showLocationList by remember { mutableStateOf(false) }


    var showLanguageDialog by remember { mutableStateOf(false) }
    /* val currentLocales = AppCompatDelegate.getApplicationLocales()
     val currentLang = if (currentLocales.isEmpty) "en" else currentLocales[0]?.language
    // val context = LocaleHelper.applyLocale(context, currentLang ?: "en")
 */
    /*val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = if (currentLocales.isEmpty) "en" else currentLocales[0]?.language
    val localizedContext = LocaleHelper.applyLocale(context, currentLang ?: "en")*/

    val savedLang = userPreferences.getAppLanguage().ifBlank { "en" }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = currentLocales[0]?.language ?: savedLang
    val localizedContext = LocaleHelper.applyLocale(context, currentLang)

    // val savedLang = userPreferences.getAppLanguage()
    val delegateLang = currentLocales[0]?.language
    val delegateTags = currentLocales.toLanguageTags()
    val configLocales = ConfigurationCompat.getLocales(localizedContext.resources.configuration)
    val configLang = configLocales[0]?.language
    val configTags = configLocales.toLanguageTags()

    Log.d("LANG_DEBUG", "----------------------------------")
    Log.d("LANG_DEBUG", "savedLang from prefs = $savedLang")
    Log.d("LANG_DEBUG", "AppCompatDelegate locales = $delegateTags")
    Log.d("LANG_DEBUG", "delegateLang = $delegateLang")
    Log.d("LANG_DEBUG", "currentLang = $currentLang")
    Log.d("LANG_DEBUG", "localizedContext config language = $configLang")
    Log.d("LANG_DEBUG", "localizedContext config tags = $configTags")
    Log.d("LANG_DEBUG", "test string settings = ${localizedContext.getString(R.string.settings)}")
    Log.d("LANG_DEBUG", "----------------------------------")
    LaunchedEffect(Unit) {
        // Read the active locale from AppCompatDelegate
        //val currentLocales = AppCompatDelegate.getApplicationLocales()
        //val currentLang = if (currentLocales.isEmpty) "en" else currentLocales[0]?.language

        // Wrap Compose context using your LocaleHelper
        //val context = LocaleHelper.applyLocale(context, currentLang.toString())

        // val cfg = context.resources.configuration


        // Log.d("LangCheck", "SettingsScreen cfg.locales[0] = ${cfg.locales[0].toLanguageTag()}")

        val s = localizedContext.getString(R.string.menu_rates_title)
        Log.d("LangCheck", "menu_rates_title = $s")
    }

    // ✅ Ensure default counter values are stored in SharedPreferences
    LaunchedEffect(Unit) {
        val defaults = mapOf(
            UserPreferences.KEY_PRODUCT_COUNT to 5,
            UserPreferences.KEY_INVENTORY_COUNT to 30,
            UserPreferences.KEY_SEARCH_COUNT to 30,
            UserPreferences.KEY_ORDER_COUNT to 10,
            UserPreferences.KEY_STOCK_TRANSFER_COUNT to 10
        )

        defaults.forEach { (key, defaultValue) ->
            if (!userPreferences.contains(key)) {
                userPreferences.saveInt(key, defaultValue)
                Log.d("Settings", "✅ Default value set for $key = $defaultValue")
            }
        }
    }



    LaunchedEffect(updateState.value) {
        when (val s = updateState.value) {
            is UiState1.Success -> {
                ToastUtils.showToast(
                    context,
                    localizedContext.getString(R.string.toast_rate_updated)
                )
                // refresh list
                val emp = UserPreferences.getInstance(context)
                    .getEmployee(Employee::class.java)
                emp?.clientCode?.let { cc ->
                    viewModel.getDailyRate(ClientCodeRequest(cc))
                }
                // close dialog & reset state
                showRatesEditor = false
                viewModel.resetUpdateState()
            }

            is UiState1.Failure -> { // If your UiState still uses Error, change to UiState.Error
                ToastUtils.showToast(context, s.message)
                viewModel.resetUpdateState()
            }

            UiState1.Loading, UiState1.Idle -> Unit
            else -> {}
        }
    }


    val scope = rememberCoroutineScope()
    val employee =
        remember { UserPreferences.getInstance(context).getEmployee(Employee::class.java) }

    Log.d("EMPLOYEE", employee.toString())
    employee?.empEmail?.let { Log.d("EMAIL ", it) }

    LaunchedEffect(locationAutoSyncEnabled) {

        Log.d("WORKER_TEST", "Scheduling worker1")

        if (locationAutoSyncEnabled) {

            Log.d("WORKER_TEST", "Scheduling worker2")
          //  val activity = context as Activity
          //  checkLocationSettings(activity)

            getCurrentLocation(context, context) { latitude, longitude, address ->

                val locationData = Data.Builder()
                    .putString("task_type", SyncDataWorker.LOCATION_SYNC_DATA_WORKER)
                    .putString("latitude", latitude)
                    .putString("longitude", longitude)
                    .putString("address", address)
                    .build()

                Log.d("WORKER_TEST", "Scheduling worker3")

                val periodicRequest =
                    PeriodicWorkRequestBuilder<SyncDataWorker>(
                        15, TimeUnit.MINUTES
                    )
                        .setInputData(locationData)
                        .addTag(SyncDataWorker.LOCATION_SYNC_DATA_WORKER)
                        .build()

                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    SyncDataWorker.LOCATION_SYNC_DATA_WORKER,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    periodicRequest
                )
            }

        } else {
            cancelPeriodicSync(context, LOCATION_SYNC_DATA_WORKER)
        }
    }
    /* LaunchedEffect(locationAutoSyncEnabled) {
         Log.d("WORKER_TEST", "Scheduling worker1")
         if (locationAutoSyncEnabled) {
             Log.d("WORKER_TEST", "Scheduling worker2")
             getCurrentLocation(context) { latitude, longitude, address ->
                 val locationData = Data.Builder()
                     .putString("task_type", SyncDataWorker.LOCATION_SYNC_DATA_WORKER)
                     .putString("latitude", latitude)
                     .putString("longitude", longitude)
                     .putString("address", address)
                     .build()

                 *//*  schedulePeriodicSync(
                  context,
                  SyncDataWorker.LOCATION_SYNC_DATA_WORKER,
                  2,
                  locationData
              )*//*
                Log.d("WORKER_TEST", "Scheduling worker3")

                val request = OneTimeWorkRequestBuilder<SyncDataWorker>()
                    .setInputData(locationData)
                    // .setInitialDelay(1, TimeUnit.MINUTES) // repeat every 1 minute
                    .addTag(SyncDataWorker.LOCATION_SYNC_DATA_WORKER)
                    .build()

                WorkManager.getInstance(context)
                    .enqueueUniqueWork(
                        SyncDataWorker.LOCATION_SYNC_DATA_WORKER,
                        ExistingWorkPolicy.REPLACE,
                        request
                    )
            }
        } else {
            cancelPeriodicSync(context, LOCATION_SYNC_DATA_WORKER)
        }
    }*/


    val menuItems = listOf(
        // Counters (first 5)
        SettingsMenuItem(
            UserPreferences.KEY_PRODUCT_COUNT,
            localizedContext.getString(R.string.product),
            Icons.Default.Settings,
            SettingType.Counter,
            5
        ),
        SettingsMenuItem(
            UserPreferences.KEY_INVENTORY_COUNT,
            localizedContext.getString(R.string.inventory),
            Icons.Default.Settings,
            SettingType.Counter,
            30
        ),
        SettingsMenuItem(
            UserPreferences.KEY_SEARCH_COUNT,
            localizedContext.getString(R.string.search),
            Icons.Default.Settings,
            SettingType.Counter,
            30
        ),
        SettingsMenuItem(
            UserPreferences.KEY_ORDER_COUNT,
            localizedContext.getString(R.string.order),
            Icons.Default.Settings,
            SettingType.Counter,
            10
        ),
        SettingsMenuItem(
            UserPreferences.KEY_STOCK_TRANSFER_COUNT,
            localizedContext.getString(R.string.stock_transfer),
            Icons.Default.Settings,
            SettingType.Counter,
            10
        ),

   /*     // Actions
        SettingsMenuItem(
            "rates",
            localizedContext.getString(R.string.menu_rates_title),
            Icons.Default.Settings,
            SettingType.Action,
            subtitle = localizedContext.getString(R.string.menu_rates_subtitle)
        ) {
            ///  showRatesEditor  = true
            //  navController.navigate(Screens.DailyRatesEditorScreen.route)
        },*/
        SettingsMenuItem(
            "account",
            localizedContext.getString(R.string.account),
            Icons.Default.Settings,
            SettingType.Action,
            subtitle = localizedContext.getString(R.string.username_password)
        ) {
            /// navController.navigate(Screens.Account.route)
        },
        SettingsMenuItem(
            "permissions",
            localizedContext.getString(R.string.user_permissions),
            Icons.Default.Settings,
            SettingType.Action,
            subtitle = localizedContext.getString(R.string.manage_permissions)
        ),
        SettingsMenuItem(
            "email",
            localizedContext.getString(R.string.hint_email),
            Icons.Default.Settings,
            SettingType.Action,
            subtitle = employee?.empEmail


        ),
        SettingsMenuItem(
            "backup",
            localizedContext.getString(R.string.backup),
            Icons.Default.Settings,
            SettingType.Action,
            subtitle = localizedContext.getString(R.string.data_backup)
        ) {

            // showBackupDialog = true
            /*  scope.launch {
                try {
                    val dbFile = context.getDatabasePath("app_db")
                    val db = SQLiteDatabase.openDatabase(
                        dbFile.absolutePath,
                        null,
                        SQLiteDatabase.OPEN_READONLY
                    )

                    BackupUtils.exportRoomDatabaseToCsv(context, db)
                    db.close()

                } catch (e: Exception) {
                    ToastUtils.showToast(context, "Backup failed: ${e.message}")
                }
            }*/
        },
        SettingsMenuItem(
            "autosync",
            localizedContext.getString(R.string.auto_sync),
            Icons.Default.Settings,
            SettingType.Action,
            subtitle = localizedContext.getString(R.string.enable_automatic_sync)
        ),
        SettingsMenuItem(
            "notifications",
            localizedContext.getString(R.string.notifications),
            Icons.Default.Settings,
            SettingType.Action,
            subtitle = localizedContext.getString(R.string.notification_settings)
        ),
        SettingsMenuItem(
            "branches",
            localizedContext.getString(R.string.branches),
            Icons.Default.Settings,
            SettingType.Action,
            subtitle = localizedContext.getString(R.string.branch_management)
        ),
        SettingsMenuItem(
            key = "apis",
            title = localizedContext.getString(R.string.menu_apis_title),
            icon = Icons.Default.Settings,
            type = SettingType.Action,
            subtitle = localizedContext.getString(R.string.menu_apis_subtitle)
        ) /*{
            showCustomApiDialog = true
        }*/,
        SettingsMenuItem(
            "sheet_url",
            localizedContext.getString(R.string.sheet_url),
            Icons.Default.Settings,
            SettingType.Action,
            subtitle = localizedContext.getString(R.string.set_google_sheet_url)
        ),
        SettingsMenuItem(
            "stock_transfer_url",
            localizedContext.getString(R.string.stock_transfer_url),
            Icons.Default.Settings,
            SettingType.Action,
            subtitle = localizedContext.getString(R.string.stock_transfer_api_url)
        ),
        SettingsMenuItem(
            "clear_data",
            localizedContext.getString(R.string.clear_data),
            Icons.Default.Settings,
            SettingType.Action,
            subtitle = localizedContext.getString(R.string.clear_data)
        ),
        SettingsMenuItem(
            key = "language",
            localizedContext.getString(R.string.language),
            icon = Icons.Default.Settings,
            type = SettingType.Action,
            /* subtitle = when (AppCompatDelegate.getApplicationLocales()[0]?.language
                 ?: userPreferences.getAppLanguage().ifBlank { "en" }) {
                 "hi" -> "Hindi"
                 "ar" -> "Arabic"
                 else -> "English"
             },*/
            subtitle = userPreferences.getAppLanguage()
                .uppercase(Locale.getDefault()), // show selected
            onClick = { /* will handle in MenuItemRow */ }
        ),
        SettingsMenuItem(
            key = "Location",
            title = localizedContext.getString(R.string.location),
            icon = Icons.Default.Settings,
            type = SettingType.Action,
            hasToggle = true,
            isToggled = locationAutoSyncEnabled,
            onToggleChange = { newValue ->
                locationAutoSyncEnabled = newValue
                userPreferences.setAutoSyncEnabled(newValue)
                // ToastUtils.showToast(context, if (newValue) "Auto Sync Enabled" else "Auto Sync Disabled")
            },
            onClick = { navController.navigate(Screens.LocationListScreen.route) }
        ),
        SettingsMenuItem(
            key = "privacy_policy",
            title = localizedContext.getString(R.string.privacy_policy),
            icon = Icons.Default.Settings,
            type = SettingType.Action,
            subtitle = localizedContext.getString(R.string.view_our_privacy_policy),
            onClick = {
                Log.d("PRIVACY_POLICY", "Navigating to privacy_policy")
                navController.navigate(Screens.PrivacyPolicyScreen.route)
            }
        ),
                SettingsMenuItem(
                key = "face_data",
        title = localizedContext.getString(R.string.face_data),
        icon = Icons.Default.Settings,
        type = SettingType.Action,
        subtitle = localizedContext.getString(R.string.add_face_login_data),
        onClick = {
            navController.navigate(Screens.AddFaceScreen.route)
            //navController.navigate(Screens.FaceManagement.route)
        }
    ),
        SettingsMenuItem(
            key = "local_wifi_mode",
            title = "Local WiFi Mode",
            icon = Icons.Default.Settings,
            type = SettingType.Action,
            subtitle = if (localWifiModeEnabled) {
                if (androidDeviceIp.isNotBlank()) {
                    "http://$androidDeviceIp:8080/rfid-data"
                } else {
                    "Device IP not found"
                }
            } else {
                "Using internet connection"
            },
            hasToggle = true,
            isToggled = localWifiModeEnabled,
            onToggleChange = { newValue ->

                if (newValue) {
                    val latestIp = getAndroidDeviceIp()

                    if (isConnectedToWifi(context) && latestIp.isNotBlank()) {
                        androidDeviceIp = latestIp
                        localWifiModeEnabled = true
                        userPreferences.setLocalWifiMode(true)

                        ToastUtils.showToast(
                            context,
                            "Local WiFi Mode Enabled: http://$latestIp:8080/rfid-data"
                        )
                    } else {
                        androidDeviceIp = ""
                        localWifiModeEnabled = false
                        userPreferences.setLocalWifiMode(false)

                        ToastUtils.showToast(
                            context,
                            "Please connect to WiFi for Local Mode"
                        )
                    }
                } else {
                    localWifiModeEnabled = false
                    userPreferences.setLocalWifiMode(false)

                    val internetAvailable = hasInternetConnection(context)

                    ToastUtils.showToast(
                        context,
                        if (internetAvailable) {
                            "Internet Mode Enabled"
                        } else {
                            "Internet Mode Enabled, but no internet found"
                        }
                    )
                }
            }
        )
    ,
    SettingsMenuItem(
        key = "web_reusable_tag",
        title = "Reusable Tags",
        icon = Icons.Default.Settings,
        type = SettingType.Action,
        subtitle = if (webReusableTagEnabled)
            "Single + Reusable enabled"
        else
            "Only WebReusable enabled", hasToggle = true,
        isToggled = webReusableTagEnabled,
        onToggleChange = { newValue ->
            webReusableTagEnabled = newValue
            userPreferences.setWebReusableTagEnabled(newValue)

            ToastUtils.showToast(
                context,
                if (newValue)
                    "Single + Reusable enabled"
                else
                    "Only Reusable enabled"    )
        }
    )
    )

    Scaffold(
        topBar = {
            GradientTopBar(
                title = localizedContext.getString(R.string.settings),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = localizedContext.getString(R.string.back),
                            tint = Color.White
                        )
                    }
                },
                selectedCount = 0,
                titleTextSize = 20.sp
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
        ) {
            items(menuItems) { item ->
                MenuItemRow(
                    item = item,
                    userPreferences = userPreferences,
                    onAutoSyncClick = { showAutoSyncDialog = true },
                    onSheetUrlClick = { showSheetInput = true },
                    onCustomApiClick = { showCustomApiDialog = true },
                    onClearDataClick = { showClearDataConfirm = true },
                    onRatesClick = { showRatesEditor = true },
                    onLanguageClick = { showLanguageDialog = true },
                    onBackupClick = { showBackupDialog = true }
                )
            }
        }
    }

    if (showSheetInput) {
        sheetUrl?.let {
            SheetInputDialog(
                localizedContext = context,
                sheetUrl = it,
                onValueChange = { sheetUrl = it },
                onDismiss = { showSheetInput = false },
                onSetClick = {
                    viewModel.updateSheetUrl(sheetUrl!!)
                    userPreferences.saveSheetUrl(sheetUrl!!)
                    showSheetInput = false
                    ToastUtils.showToast(
                        context,
                        localizedContext.getString(R.string.toast_sheet_url_updated)
                    )
                }
            )
        }
    }
    if (showRatesEditor) {
        navController.navigate(Screens.DailyRatesEditorScreen.route)
    }

    if (showCustomApiDialog) {

        CustomApiDialog(
            localizedContext = context,
            onDismiss = { showCustomApiDialog = false },
            onSave = { newApi ->
                customApi = newApi
                userPreferences.saveCustomApi(newApi)
                ToastUtils.showToast(context, localizedContext.getString(R.string.custom_api_saved))
                showCustomApiDialog = false
            }
        )
    }

    if (showBackupDialog) {
        BackupDialogExample(
            onDismiss = { showBackupDialog = false },
            scope = scope,
            userPreferences = userPreferences,
            localizedContext = localizedContext
        )
    }

    // ✅ Auto Sync Dialog
    if (showAutoSyncDialog) {
        AlertDialog(
            onDismissRequest = { showAutoSyncDialog = false },
            title = { Text(localizedContext.getString(R.string.auto_sync_settings)) },
            text = {
                AutoSyncSetting(userPref = userPreferences)
            },
            confirmButton = {}
        )
    }

    // ✅ Clear Data Confirmation Dialog
    if (showClearDataConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirm = false },
            title = {
                Text(
                    localizedContext.getString(R.string.confirm_clear_data),
                    fontFamily = poppins
                )
            },
            text = {
                Text(
                    localizedContext.getString(R.string.this_will_permanently_delete_all_app_data_from_this_device_continue),
                    fontFamily = poppins
                )
            },
            confirmButton = {
                GradientButton(
                    text = localizedContext.getString(R.string.yes_clear_data),
                    onClick = {
                        showClearDataConfirm = false
                        showPasswordDialog = true
                    },
                )
            },
            dismissButton = {
                GradientButton(
                    text = localizedContext.getString(R.string.cancel),
                    onClick = {
                        showClearDataConfirm = false
                    },
                )
            }
        )
    }
    if (showPasswordDialog) {
        var password by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }
        val correctPassword =
            userPreferences.getSavedPassword() // You can define this in UserPreferences

        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Password Verification", fontFamily = poppins) },
            text = {
                Column {
                    Text("Enter your password to confirm data wipe:")
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password", fontFamily = poppins) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    painter = painterResource(id = if (passwordVisible) R.drawable.ic_action_eye else R.drawable.ic_action_eye_off),
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = Color.DarkGray
                                )
                            }
                        }
                    )
                }
            },
            confirmButton = {

                GradientButton("Confirm", onClick = {
                    if (password == correctPassword) {
                        showPasswordDialog = false
                        viewModel.clearAllData(context, navController)
                    } else {
                        ToastUtils.showToast(context, "Incorrect password")
                    }
                })
            },
            dismissButton = {
                GradientButton(
                    localizedContext.getString(R.string.cancel),
                    onClick = { showPasswordDialog = false })
            }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = {
                showLanguageDialog = false
            },  // Dismiss the dialog when clicked outside
            title = { Text("Select Language") },
            text = {
                Column {
                    // English Option
                    LanguageOption(
                        label = "English",
                        selected = userPreferences.getAppLanguage() == "en",
                        onSelect = {
                            // userPreferences.saveAppLanguage("en")
                            applyLocaleAndRestart(context, "en", userPreferences)
                            showLanguageDialog = false  // Close the dialog after selection
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Hindi Option
                    LanguageOption(
                        label = "Hindi (हिन्दी)",
                        selected = userPreferences.getAppLanguage() == "hi",
                        onSelect = {
                            //userPreferences.saveAppLanguage("hi")
                            applyLocaleAndRestart(context, "hi", userPreferences)
                            showLanguageDialog = false  // Close the dialog after selection
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Arabic Option
                    LanguageOption(
                        label = "Arabic (العربية)",
                        selected = userPreferences.getAppLanguage() == "ar",
                        onSelect = {
                            //userPreferences.saveAppLanguage("ar")
                            applyLocaleAndRestart(context, "ar", userPreferences)
                            showLanguageDialog = false  // Close the dialog after selection
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text("Cancel") }
            }
        )
    }
    /*  AlertDialog(
          onDismissRequest = { showLanguageDialog = false },
          title = { Text(localizedContext.getString( R.string.select_lagauge), fontFamily = poppins) },
          text = {
              Column(Modifier.fillMaxWidth()) {
                  LanguageOption(
                      label = "English",
                      selected = userPreferences.getAppLanguage() == "en",
                      onSelect = {
                          userPreferences.saveAppLanguage("en")

                          val appLocale = LocaleListCompat.forLanguageTags("en")
                          AppCompatDelegate.setApplicationLocales(appLocale)

                          ToastUtils.showToast(context, "Language changed to English")
                          showLanguageDialog = false
                          scope.launch {
                              delay(300)
                              restartApp(context)
                          }
                        //  restartApp(context)   // optional but tu already use kar raha hai
                      }
                  )

                  Spacer(modifier = Modifier.height(8.dp))

                  LanguageOption(
                      label = "Hindi (हिन्दी)",
                      selected = userPreferences.getAppLanguage() == "hi",
                      onSelect = {
                          userPreferences.saveAppLanguage("hi")

                          val appLocale = LocaleListCompat.forLanguageTags("hi")
                          AppCompatDelegate.setApplicationLocales(appLocale)

                          ToastUtils.showToast(context, "भाषा हिंदी में बदल दी गई है")
                          showLanguageDialog = false
                          scope.launch {
                              delay(300)
                              restartApp(context)
                          }
                         // restartApp(context)   // optional but safe
                      }
                  )

                  // Arabic
                  LanguageOption(
                      label = "Arabic (العربية)",
                      selected = userPreferences.getAppLanguage() == "ar",
                      onSelect = {
                          userPreferences.saveAppLanguage("ar")

                          val appLocale = LocaleListCompat.forLanguageTags("ar")
                          AppCompatDelegate.setApplicationLocales(appLocale)

                          ToastUtils.showToast(context, "تم تغيير اللغة إلى العربية")
                          showLanguageDialog = false

                          scope.launch {
                              delay(300)
                              restartApp(context)
                          }
                      }
                  )
              }
          },
          confirmButton = {},
          dismissButton = {
              TextButton(onClick = { showLanguageDialog = false }) { Text(localizedContext.getString(R.string.cancel)) }
          }
      )*/


}

fun getCurrentNetworkType(context: Context): String {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val network = connectivityManager.activeNetwork ?: return "No Connection"
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return "No Connection"

    return when {
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobile Internet"
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
        else -> "Unknown"
    }
}

fun isConnectedToWifi(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
}

fun hasInternetConnection(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

@Composable
fun LanguageOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, fontFamily = poppins, fontSize = 15.sp)
    }
}

fun checkLocationSettings(activity: Activity) {

    val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        1000
    ).build()

    val builder = LocationSettingsRequest.Builder()
        .addLocationRequest(locationRequest)
        .setAlwaysShow(true)

    val client: SettingsClient = LocationServices.getSettingsClient(activity)

    val task = client.checkLocationSettings(builder.build())

    task.addOnSuccessListener {
        Log.d("LOCATION_SETTINGS", "Location accuracy already enabled")
    }

    task.addOnFailureListener { exception ->
        if (exception is ResolvableApiException) {
            try {
                exception.startResolutionForResult(activity, 1001)
            } catch (sendEx: IntentSender.SendIntentException) {
                sendEx.printStackTrace()
            }
        }
    }
}

/*fun restartApp(context: Context) {

    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    val mainIntent = Intent.makeRestartActivityTask(intent?.component)
    context.startActivity(mainIntent)
    Runtime.getRuntime().exit(0)
}

@Composable
fun LanguageOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, fontFamily = poppins, fontSize = 15.sp)
    }
}*/

fun applyLocaleAndRestart(context: Context, langCode: String, userPreferences1: UserPreferences) {
    userPreferences1.saveAppLanguage(langCode)

    AppCompatDelegate.setApplicationLocales(
        LocaleListCompat.forLanguageTags(langCode)
    )

    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    val mainIntent = Intent.makeRestartActivityTask(intent?.component)
    context.startActivity(mainIntent)
    Runtime.getRuntime().exit(0)
}

fun restartApp(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
    val mainIntent = Intent.makeRestartActivityTask(intent?.component)
    context.startActivity(mainIntent)
    Runtime.getRuntime().exit(0)
}


/*
@SuppressLint("MissingPermission")
fun getCurrentLocation(activity: Context, onLocationFetched: (String, String, String) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)

    if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED &&
        ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        // Request permissions directly
        ActivityCompat.requestPermissions(
            activity as Activity,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            100
        )
        return
    }

    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        try {
            if (location != null) {
                val latitude = location.latitude.toString()
                val longitude = location.longitude.toString()

                val geocoder = Geocoder(activity, Locale.getDefault())
                val addressInfo = geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()

                val area = addressInfo?.subLocality ?: "" // Area or neighborhood
                val city = addressInfo?.locality
                    ?: addressInfo?.subAdminArea           // fallback (district/taluka level)
                    ?: addressInfo?.featureName            // sometimes contains village or town name
                    ?: ""
                val state = addressInfo?.adminArea ?: ""   // State
                val pinCode = addressInfo?.postalCode ?: "" // Pincode

                // Combine only non-empty parts
                val address = listOf(area, city, state, pinCode)
                    .filter { it.isNotEmpty() }
                    .joinToString(", ")

                onLocationFetched(latitude, longitude, address)
            } else {
                Toast.makeText(activity, "Failed to get location", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(activity, "⚠️ Error while fetching address: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

}*/

/*@SuppressLint("MissingPermission")
fun getCurrentLocation(
    context: Context,
    onLocationFetched: (String, String, String) -> Unit
) {

    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    val finePermission = ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    val coarsePermission = ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    Log.d("LOCATION_DEBUG", "Fine permission = $finePermission")
    Log.d("LOCATION_DEBUG", "Coarse permission = $coarsePermission")

    if (finePermission != PackageManager.PERMISSION_GRANTED &&
        coarsePermission != PackageManager.PERMISSION_GRANTED
    ) {
        Log.d("LOCATION_DEBUG", "Location permission NOT granted")
        return
    }

    fusedLocationClient.getCurrentLocation(
        Priority.PRIORITY_HIGH_ACCURACY,
        null
    ).addOnSuccessListener { location ->

        Log.d("LOCATION_DEBUG", "Location result = $location")

        if (location != null) {

            val latitude = location.latitude
            val longitude = location.longitude

            Log.d("LOCATION_DEBUG", "Lat = $latitude , Lng = $longitude")

            val geocoder = Geocoder(context, Locale.getDefault())
            val addressInfo =
                geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()

            val address = listOf(
                addressInfo?.subLocality,
                addressInfo?.locality,
                addressInfo?.adminArea,
                addressInfo?.postalCode
            ).filterNotNull().joinToString(", ")

            Log.d("LOCATION_DEBUG", "Address = $address")

            onLocationFetched(latitude.toString(), longitude.toString(), address)

        } else {
            Log.d("LOCATION_DEBUG", "Location returned NULL")
        }
    }
}*/

/*

@SuppressLint("MissingPermission")
fun getCurrentLocation(context: Context,context: Context, onLocationFetched: (String, String, String) -> Unit) {

    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
        ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
    ) {
        ActivityCompat.requestPermissions(
            context as Activity,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            100
        )
        return
    }

    val cancellationTokenSource = CancellationTokenSource()

    fusedLocationClient.getCurrentLocation(
        Priority.PRIORITY_HIGH_ACCURACY,
        cancellationTokenSource.token
    ).addOnSuccessListener { location ->

        if (location != null) {

            val latitude = location.latitude.toString()
            val longitude = location.longitude.toString()

            val geocoder = Geocoder(context, Locale.getDefault())

         */
/*   val addressInfo =
                geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()

            val area = addressInfo?.subLocality ?: ""
            val city = addressInfo?.locality
                ?: addressInfo?.subAdminArea
                ?: addressInfo?.featureName
                ?: ""
            val state = addressInfo?.adminArea ?: ""
            val pinCode = addressInfo?.postalCode ?: ""

            val address = listOf(area, city, state, pinCode)
                .filter { it.isNotEmpty() }
                .joinToString(", ")*//*

            CoroutineScope(Dispatchers.IO).launch {

                try {

                    val geocoder = Geocoder(context, Locale.getDefault())

                    val addressList = geocoder.getFromLocation(
                        location.latitude,
                        location.longitude,
                        1
                    )

                    val addressInfo = addressList?.firstOrNull()

                    val address = addressInfo?.getAddressLine(0) ?: "Unknown location"

                    withContext(Dispatchers.Main) {
                        onLocationFetched(latitude, longitude, address)
                    }

                } catch (e: Exception) {

                    Log.e("LOCATION", "Geocoder error: ${e.message}")

                    withContext(Dispatchers.Main) {
                        onLocationFetched(latitude, longitude, "Address unavailable")
                    }
                }
            }

           // onLocationFetched(latitude, longitude, address)

        } else {
            Toast.makeText(context,
                localizedContext.getString(R.string.failed_to_get_location), Toast.LENGTH_SHORT).show()
        }
    }
}
*/


@SuppressLint("MissingPermission")
/*fun getCurrentLocation(
    localizedContext: Context,
    context: Context,
    onLocationFetched: (String, String, String) -> Unit
) {
    if (context !is Activity) {
        Log.e("LOCATION_DEBUG", "Context is not Activity")
        return
    }

    val finePermission = ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    val coarsePermission = ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    if (finePermission != PackageManager.PERMISSION_GRANTED &&
        coarsePermission != PackageManager.PERMISSION_GRANTED
    ) {
        ActivityCompat.requestPermissions(
            context,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            100
        )
        return
    }

    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    fusedLocationClient.lastLocation
        .addOnSuccessListener { lastLocation ->
            if (lastLocation != null) {
                getAddressFromLatLong(
                    context,
                    lastLocation.latitude,
                    lastLocation.longitude,
                    onLocationFetched
                )
            } else {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    CancellationTokenSource().token
                ).addOnSuccessListener { currentLocation ->
                    if (currentLocation != null) {
                        getAddressFromLatLong(
                            context,
                            currentLocation.latitude,
                            currentLocation.longitude,
                            onLocationFetched
                        )
                    } else {
                        Log.e("LOCATION_DEBUG", "Location null")
                        Toast.makeText(
                            context,
                            "Location not found. Please turn on GPS and try again.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
}*/



fun getCurrentLocation(
    localizedContext: Context,
    context: Context,
    onLocationFetched: (String, String, String) -> Unit
) {
    if (context !is Activity) {
        Log.e("LOCATION_DEBUG", "Context is not Activity")
        return
    }

    val finePermission = ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    val coarsePermission = ActivityCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    if (finePermission != PackageManager.PERMISSION_GRANTED &&
        coarsePermission != PackageManager.PERMISSION_GRANTED
    ) {
        ActivityCompat.requestPermissions(
            context,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            100
        )
        return
    }

    getLocationUsingLocationManager(
        context = context,
        onLocationFetched = onLocationFetched
    )
}

@SuppressLint("MissingPermission")
fun getLocationUsingLocationManager(
    context: Context,
    onLocationFetched: (String, String, String) -> Unit
) {
    val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    val providers = mutableListOf<String>()

    if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
        providers.add(LocationManager.NETWORK_PROVIDER)
    }

    if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
        providers.add(LocationManager.GPS_PROVIDER)
    }

    providers.add(LocationManager.PASSIVE_PROVIDER)

    if (providers.isEmpty()) {
        Toast.makeText(context, "Please turn on device location.", Toast.LENGTH_LONG).show()
        return
    }

    Log.d("LOCATION_DEBUG", "Available providers = $providers")

    // 1. First try last known from all providers
    for (provider in providers) {
        val lastKnown = locationManager.getLastKnownLocation(provider)

        Log.d("LOCATION_DEBUG", "LastKnown $provider = $lastKnown")

        if (lastKnown != null) {
            getAddressFromLatLong(
                context,
                lastKnown.latitude,
                lastKnown.longitude,
                onLocationFetched
            )
            return
        }
    }

    var locationFound = false

    val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (locationFound) return

            locationFound = true
            locationManager.removeUpdates(this)

            Log.d(
                "LOCATION_DEBUG",
                "Location found provider=${location.provider}, lat=${location.latitude}, lng=${location.longitude}"
            )

            getAddressFromLatLong(
                context,
                location.latitude,
                location.longitude,
                onLocationFetched
            )
        }

        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    // 2. Request update from all available providers
    providers.forEach { provider ->
        try {
            locationManager.requestLocationUpdates(
                provider,
                1000L,
                0f,
                listener,
                android.os.Looper.getMainLooper()
            )
        } catch (e: Exception) {
            Log.e("LOCATION_DEBUG", "Provider failed: $provider ${e.message}")
        }
    }

    // 3. Timeout fallback
    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
        if (!locationFound) {
            locationManager.removeUpdates(listener)

            Log.e("LOCATION_DEBUG", "Location timeout. No provider returned location.")

            Toast.makeText(
                context,
                "Location not found. Please keep GPS ON and try outside/open area.",
                Toast.LENGTH_LONG
            ).show()
        }
    }, 60000L)
}

fun getAddressFromLatLong(
    context: Context,
    latitude: Double,
    longitude: Double,
    onLocationFetched: (String, String, String) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        val address = try {
            val geocoder = Geocoder(context, Locale.getDefault())
            geocoder.getFromLocation(latitude, longitude, 1)
                ?.firstOrNull()
                ?.getAddressLine(0)
                ?: "Address unavailable"
        } catch (e: Exception) {
            "Address unavailable"
        }

        withContext(Dispatchers.Main) {
            onLocationFetched(latitude.toString(), longitude.toString(), address)
        }
    }
}

@Composable
fun BackupDialogExample(
    onDismiss: () -> Unit,
    scope: CoroutineScope,
    userPreferences: UserPreferences,
    localizedContext: Context
) {
    val context = LocalContext.current
    var showEmailDialog by remember { mutableStateOf(false) }
    var inputEmail by remember { mutableStateOf("") }
    var savedEmail by remember { mutableStateOf<String?>(null) }

    // ----------------------------------------
    // ✅ Declare launcher at top level
    // ----------------------------------------
    val restoreFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val tempFile = File(context.cacheDir, "restore_temp.db")
                inputStream?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }
                restoreBackupFromDb(context, tempFile, context)
            } catch (e: Exception) {
                ToastUtils.showToast(context, "❌ Restore failed: ${e.message}")
            }
        }
    }


    // ----------------------------------------
    // ✅ Email Sending Function
    // ----------------------------------------
    fun sendBackupEmail(
        context: Context,
        scope: CoroutineScope,
        recipientEmail: String,
        onDismiss: () -> Unit,
        localizedContext: Context
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                // 1️⃣ Prepare export directory
                val exportDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "DatabaseBackup"
                )
                if (!exportDir.exists()) exportDir.mkdirs()

                // 2️⃣ Export CSV
                val csvFile = File(exportDir, "Backup_All.csv")
                val dbFile = context.getDatabasePath("app_db")
                val db = SQLiteDatabase.openDatabase(
                    dbFile.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READONLY
                )
                BackupUtils.exportRoomDatabaseToCsv(context, db)
                db.close()

                // 3️⃣ Prepare DB file (copy original DB to export folder)
                val dbBackupFile = File(exportDir, "app_db_backup.db")
                dbFile.copyTo(dbBackupFile, overwrite = true)

                // 4️⃣ Check files exist
                if ((!csvFile.exists() || csvFile.length() == 0L) && !dbBackupFile.exists()) {
                    withContext(Dispatchers.Main) {
                        ToastUtils.showToast(context, "⚠️ Backup files not found or empty.")
                    }
                    return@launch
                }

                // 5️⃣ Send email with both attachments
                EmailSender.sendEmailWithAttachment(
                    toEmails = listOf(recipientEmail),
                    subject = "SparkleERP Backup",
                    body = "Here’s your latest backup files (CSV + Database).",
                    attachments = mapOf(
                        "Backup_All.csv" to csvFile,
                        "app_db_backup.db" to dbBackupFile
                    )
                )

                withContext(Dispatchers.Main) {
                    ToastUtils.showToast(
                        context,
                        localizedContext.getString(
                            R.string.backup_email_sent_success,
                            recipientEmail
                        )
                    )
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    ToastUtils.showToast(context, "❌ Failed: ${e.message}")
                }
            } finally {
                withContext(Dispatchers.Main) { onDismiss() }
            }
        }
    }


    // ----------------------------------------
    // ✅ UI
    // ----------------------------------------
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(localizedContext.getString(R.string.backup_options)) },
        text = { Text(localizedContext.getString(R.string.choose_how_you_d_like_to_back_up_your_data)) },
        confirmButton = {
            Column {
                // 📂 Save to Device
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                val dbFile = context.getDatabasePath("app_db")

                                // Create /backup directory inside app-specific external files
                                val backupDir = File(context.getExternalFilesDir(null), "backup")
                                if (!backupDir.exists()) backupDir.mkdirs()

                                // Copy the Room .db file to backup folder
                                val backupFile = File(backupDir, "app_db_backup.db")
                                dbFile.copyTo(backupFile, overwrite = true)

                                ToastUtils.showToast(
                                    context,
                                    localizedContext.getString(
                                        R.string.database_backup_saved,
                                        backupFile.absolutePath
                                    )
                                )
                            } catch (e: Exception) {
                                ToastUtils.showToast(
                                    context,
                                    localizedContext.getString(R.string.backup_failed, e.message)
                                )
                            } finally {
                                onDismiss()
                            }
                        }
                    }
                ) { Text(localizedContext.getString(R.string.save_to_device)) }


                // 📧 Send via Email
                TextButton(onClick = {
                    val activity = context.findActivity() ?: return@TextButton
                    if (!ensureStoragePermission(context, activity)) {
                        ToastUtils.showToast(
                            context,
                            localizedContext.getString(R.string.please_grant_storage_permission_to_send_backup)
                        )
                        return@TextButton
                    }

                    // ✅ Always fetch fresh from SharedPreferences each time
                    inputEmail = UserPreferences.getInstance(context).getBackupEmail().orEmpty()
                    showEmailDialog = true
                }) {
                    Text(localizedContext.getString(R.string.send_via_email))
                }


                // 🔄 Restore Backup
                TextButton(onClick = {
                    restoreFileLauncher.launch("*/*")
                }) {
                    Text(localizedContext.getString(R.string.restore_backup))
                }

                // 📥 Email Input Dialog
                if (showEmailDialog) {
                    AlertDialog(
                        onDismissRequest = { showEmailDialog = false },
                        title = { Text(localizedContext.getString(R.string.enter_email_address)) },
                        text = {
                            TextField(
                                value = inputEmail,
                                onValueChange = { inputEmail = it },
                                label = { Text(localizedContext.getString(R.string.hint_email)) },
                                singleLine = true
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                if (android.util.Patterns.EMAIL_ADDRESS.matcher(inputEmail)
                                        .matches()
                                ) {
                                    val userPreferences = UserPreferences.getInstance(context)
                                    userPreferences.saveBackupEmail(inputEmail) // ✅ Save using your preference helper

                                    savedEmail = inputEmail
                                    showEmailDialog = false
                                    ToastUtils.showToast(
                                        context,
                                        localizedContext.getString(R.string.sending_backup)
                                    )

                                    sendBackupEmail(context, scope, inputEmail, onDismiss, context)
                                } else {
                                    ToastUtils.showToast(
                                        context,
                                        localizedContext.getString(R.string.please_enter_a_valid_email_address)
                                    )
                                }
                            }) { Text(localizedContext.getString(R.string.save_send)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEmailDialog = false }) {
                                Text(
                                    localizedContext.getString(R.string.cancel)
                                )
                            }
                        }
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(localizedContext.getString(R.string.cancel)) }
        }
    )
}

// ---------------------------------------------------------
// 🔹 Restore Function — Reads Backup CSV → Restores to DB
// ---------------------------------------------------------
/*fun restoreBackupFromCsv(context: Context, csvFile: File) {
    try {
        val dbFile = context.getDatabasePath("app_db")
        val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE)

        db.beginTransaction()
        val reader = csvFile.bufferedReader()
        val lines = reader.readLines()

        if (lines.isEmpty()) {
            ToastUtils.showToast(context, "⚠️ Backup file is empty.")
            db.close()
            return
        }

        val header = lines.first().split(",")
        val dataRows = lines.drop(1)

        // Example: Assume your table name is “items”
        db.delete("items", null, null) // clear table before restore

        val insertQuery =
            "INSERT INTO items (${header.joinToString(",")}) VALUES (${header.joinToString(",") { "?" }})"

        val stmt = db.compileStatement(insertQuery)
        dataRows.forEach { line ->
            val values = line.split(",")
            stmt.clearBindings()
            values.forEachIndexed { index, value ->
                stmt.bindString(index + 1, value.trim())
            }
            stmt.executeInsert()
        }

        db.setTransactionSuccessful()
        db.endTransaction()
        db.close()

        ToastUtils.showToast(context, "✅ Data restored successfully!")

    } catch (e: Exception) {
        ToastUtils.showToast(context, "❌ Restore failed: ${e.message}")
    }
}*/
fun restoreBackupFromDb(localizedContext: Context, backupFile: File, context: Context) {

    try {
        if (!backupFile.exists()) {
            ToastUtils.showToast(
                context,
                localizedContext.getString(R.string.backup_file_not_found)
            )
            return
        }

        val dbFile = context.getDatabasePath("app_db")
        AppDatabase.closeInstance()

        // Remove old DB + WAL/SHM files
        File(dbFile.absolutePath + "-shm").delete()
        File(dbFile.absolutePath + "-wal").delete()
        dbFile.delete()

        // Copy backup into place
        backupFile.copyTo(dbFile, overwrite = true)

        ToastUtils.showToast(
            context,
            localizedContext.getString(R.string.database_restored_successfully)
        )

        // Restart app to reload DB
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val mainIntent = Intent.makeRestartActivityTask(intent?.component)
        context.startActivity(mainIntent)
        Runtime.getRuntime().exit(0)

    } catch (e: Exception) {
        Log.e("DB_RESTORE", "Restore failed", e)
        ToastUtils.showToast(
            context,
            localizedContext.getString(R.string.restore_failed, e.message)
        )
    }
}


private fun ensureStoragePermission(context: Context, activity: Activity): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_MEDIA_IMAGES
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
                ),
                100
            )
        }
        hasPermission
    } else {
        val hasPermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                101
            )
        }
        hasPermission
    }
}


@Composable
fun CustomApiDialog(
    localizedContext: Context,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,

    ) {
    val context = LocalContext.current
    val userPrefs = remember { UserPreferences.getInstance(context) }

    // 🔹 Load the saved custom API from SharedPreferences
    val savedUrl = remember { mutableStateOf(userPrefs.getCustomApi().orEmpty()) }

    var input by remember { mutableStateOf(savedUrl.value) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = {
            Text(
                text = localizedContext.getString(R.string.dialog_custom_api_title),
                fontSize = 16.sp,
                fontFamily = poppins,
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = {
                        Text(
                            text = localizedContext.getString(R.string.hint_custom_api),
                            fontSize = 14.sp,
                            fontFamily = poppins
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                GradientButton(
                    text = localizedContext.getString(R.string.button_save),
                    onClick = {
                        if (input.isNotBlank()) {
                            userPrefs.saveCustomApi(input)  // ✅ Save in SharedPreferences
                            onSave(input)
                        }
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    )
}


// ---------------- MENU ROW ----------------
@Composable
fun MenuItemRow(
    item: SettingsMenuItem,
    userPreferences: UserPreferences,
    onAutoSyncClick: () -> Unit,
    onSheetUrlClick: () -> Unit,
    onCustomApiClick: () -> Unit,
    onClearDataClick: () -> Unit,
    onRatesClick: () -> Unit,
    onLanguageClick: () -> Unit,
    onBackupClick: () -> Unit,

    ) {
    var expanded by remember { mutableStateOf(false) }
    var selectedValue by remember {
        mutableIntStateOf(
            userPreferences.getInt(item.key, item.defaultValue ?: 0)
        )
    }


    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable {
                Log.d("SETTINGS_CLICK", "Clicked key = ${item.key}")
                when (item.key) {
                    "autosync" -> onAutoSyncClick()
                    "sheet_url" -> onSheetUrlClick()
                    "clear_data" -> onClearDataClick()
                    "apis" -> onCustomApiClick()
                    "rates" -> onRatesClick()
                    "backup" -> onBackupClick()
                    "language" -> onLanguageClick()
                    else -> item.onClick?.invoke()
                }
            },
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        color = Color(0xFFF7F7F7)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = item.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = poppins,
                modifier = Modifier.weight(1f),
                color = Color.Black
            )

            when (item.type) {
                is SettingType.Counter -> {
                    Box(
                        modifier = Modifier.wrapContentSize(Alignment.TopEnd) // ✅ anchor to right
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFFE0E0E0), RoundedCornerShape(6.dp))
                                .clickable { expanded = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                selectedValue.toString(),
                                fontWeight = FontWeight.Bold,
                                fontFamily = poppins
                            )
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier
                                .width(70.dp)
                                .background(Color.White),
                            offset = DpOffset(
                                x = (-20).dp,
                                y = 0.dp
                            ) // small tweak to match alignment
                        ) {
                            (1..30).forEach { count ->
                                DropdownMenuItem(
                                    text = { Text(count.toString(), fontFamily = poppins) },
                                    onClick = {
                                        selectedValue = count
                                        userPreferences.saveInt(item.key, count)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                is SettingType.Action -> {
                    if (item.hasToggle) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.subtitle ?: "",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                fontFamily = poppins,
                                modifier = Modifier.width(145.dp)
                            )
                            Switch(
                                checked = item.isToggled,
                                onCheckedChange = { newValue ->
                                    item.onToggleChange?.invoke(newValue)
                                },
                                modifier = Modifier.scale(0.75f)
                            )
                        }
                    } else {
                        Text(
                            item.subtitle ?: "",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            fontFamily = poppins
                        )
                    }
                }
            }


        }
    }
}

// ---------------- SHEET URL DIALOG ----------------
@Composable
fun SheetInputDialog(
    localizedContext: Context,
    sheetUrl: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSetClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = {
            Text(
                localizedContext.getString(R.string.set_sheet_url),
                fontFamily = poppins,
                fontSize = 14.sp
            )
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                OutlinedTextField(
                    value = sheetUrl,
                    onValueChange = onValueChange,
                    label = {
                        Text(
                            localizedContext.getString(R.string.enter_sheet_url),
                            fontFamily = poppins,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                GradientButton(
                    text = localizedContext.getString(R.string.set_sheet_id),
                    onClick = onSetClick,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    )



}
