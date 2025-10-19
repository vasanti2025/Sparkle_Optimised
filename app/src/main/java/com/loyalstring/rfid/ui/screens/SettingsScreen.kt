package com.loyalstring.rfid.ui.screens

import android.app.Activity
import android.content.Context
import android.database.sqlite.SQLiteDatabase

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.loyalstring.rfid.R
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
import kotlinx.coroutines.launch

import android.os.Environment
import android.util.Log
import androidx.compose.material3.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import java.io.File

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent

import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.loyalstring.rfid.worker.EmailSender
import com.loyalstring.rfid.worker.SyncDataWorker
import com.loyalstring.rfid.worker.cancelPeriodicSync
import com.loyalstring.rfid.worker.schedulePeriodicSync
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import com.google.android.gms.location.LocationServices
import com.loyalstring.rfid.data.local.db.AppDatabase
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
    var showLocationList by remember { mutableStateOf(false) }

    LaunchedEffect(updateState.value) {
        when (val s = updateState.value) {
            is UiState1.Success -> {
                ToastUtils.showToast(context, "Rates updated successfully")
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

    if (locationAutoSyncEnabled) {
        getCurrentLocation(context) { latitude, longitude, address ->
            val locationData = Data.Builder()
                .putString("task_type", SyncDataWorker.LOCATION_SYNC_DATA_WORKER)
                .putString("latitude", latitude)
                .putString("longitude", longitude)
                .putString("address", address)
                .build()

          /*  schedulePeriodicSync(
                context,
                SyncDataWorker.LOCATION_SYNC_DATA_WORKER,
                2,
                locationData
            )*/

            val request = OneTimeWorkRequestBuilder<SyncDataWorker>()
                .setInputData(locationData)
                .setInitialDelay(1, TimeUnit.MINUTES) // repeat every 1 minute
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
        cancelPeriodicSync(context,LOCATION_SYNC_DATA_WORKER)
    }

    val menuItems = listOf(
        // Counters (first 5)
        SettingsMenuItem(
            "product_count",
            "Product",
            Icons.Default.Settings,
            SettingType.Counter,
            5
        ),
        SettingsMenuItem(
            "inventory_count",
            "Inventory",
            Icons.Default.Settings,
            SettingType.Counter,
            30
        ),
        SettingsMenuItem("search_count", "Search", Icons.Default.Settings, SettingType.Counter, 30),
        SettingsMenuItem("orders_count", "Orders", Icons.Default.Settings, SettingType.Counter, 10),
        SettingsMenuItem(
            "stock_transfer_count",
            "Stock transfer",
            Icons.Default.Settings,
            SettingType.Counter,
            10
        ),

        // Actions
        SettingsMenuItem(
            "rates",
            stringResource(id = R.string.menu_rates_title),
            Icons.Default.Settings,
            SettingType.Action,
            subtitle = stringResource(id = R.string.menu_rates_subtitle)
        ) {
          ///  showRatesEditor  = true
          //  navController.navigate(Screens.DailyRatesEditorScreen.route)
        },
        SettingsMenuItem(
            "account",
            "Account",
            Icons.Default.Settings,
            SettingType.Action,
            subtitle = "Username & Password"
        ) {
            /// navController.navigate(Screens.Account.route)
        },
        SettingsMenuItem(
            "permissions",
            "Users and permissions",
            Icons.Default.Settings,
            SettingType.Action,
            subtitle = "Manage permissions"
        ),
        SettingsMenuItem(
            "email",
            "Email",
            Icons.Default.Settings,
            SettingType.Action,
            subtitle = employee?.empEmail


        ),
        SettingsMenuItem(
            "backup",
            "Backup",
            Icons.Default.Settings,
            SettingType.Action,
            subtitle = "Data Backup"
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
            "Auto Sync",
            Icons.Default.Settings,
            SettingType.Action,
            subtitle = "Enable automatic sync"
        ),
        SettingsMenuItem(
            "notifications",
            "Notifications",
            Icons.Default.Settings,
            SettingType.Action,
            subtitle = "Notification settings"
        ),
        SettingsMenuItem(
            "branches",
            "Branches",
            Icons.Default.Settings,
            SettingType.Action,
            subtitle = "Branch management"
        ),
        SettingsMenuItem(
            key = "apis",
            title = stringResource(id = R.string.menu_apis_title),
            icon = Icons.Default.Settings,
            type = SettingType.Action,
            subtitle = stringResource(id = R.string.menu_apis_subtitle)
        ) /*{
            showCustomApiDialog = true
        }*/,
        SettingsMenuItem(
            "sheet_url",
            "Sheet URL",
            Icons.Default.Settings,
            SettingType.Action,
            subtitle = "Set Google Sheet URL"
        ),
        SettingsMenuItem(
            "stock_transfer_url",
            "Stock Transfer URL",
            Icons.Default.Settings,
            SettingType.Action,
            subtitle = "Stock Transfer API URL"
        ),
        SettingsMenuItem(
            "clear_data",
            "Clear Data",
            Icons.Default.Settings,
            SettingType.Action,
            subtitle = "Clear data"
        ),
        SettingsMenuItem(
            key = "Location",
            title = "Location",
            icon = Icons.Default.Settings,
            type = SettingType.Action,
            hasToggle = true,
            isToggled = locationAutoSyncEnabled,
            onToggleChange = { newValue ->
                locationAutoSyncEnabled = newValue
                userPreferences.setAutoSyncEnabled(newValue)
                ToastUtils.showToast(context, if (newValue) "Auto Sync Enabled" else "Auto Sync Disabled")
            },
            onClick = {  navController.navigate(Screens.LocationListScreen.route) }
        )
    )

    Scaffold(
        topBar = {
            GradientTopBar(
                title = "Settings",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                showCounter = false,
                selectedCount = 0,
                onCountSelected = {}
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
                    onRatesClick={showRatesEditor=true},
                    onBackupClick={showBackupDialog=true}
                )
            }
        }
    }

    if (showSheetInput) {
        sheetUrl?.let {
            SheetInputDialog(
                sheetUrl = it,
                onValueChange = { sheetUrl = it },
                onDismiss = { showSheetInput = false },
                onSetClick = {
                    viewModel.updateSheetUrl(sheetUrl!!)
                    userPreferences.saveSheetUrl(sheetUrl!!)
                    showSheetInput = false
                    ToastUtils.showToast(context, "Sheet URL updated successfully")
                }
            )
        }
    }
    if (showRatesEditor) {
        navController.navigate(Screens.DailyRatesEditorScreen.route)
    }

    if (showCustomApiDialog) {

        CustomApiDialog(
            onDismiss = { showCustomApiDialog = false },
            onSave = { newApi ->
                customApi = newApi
                userPreferences.saveCustomApi(newApi)
                ToastUtils.showToast(context, "Custom API saved!")
                showCustomApiDialog = false
            }
        )
    }

    if (showBackupDialog) {
        BackupDialogExample(
            onDismiss = { showBackupDialog = false },
            scope = scope,
             userPreferences=userPreferences
        )
    }

    // ✅ Auto Sync Dialog
    if (showAutoSyncDialog) {
        AlertDialog(
            onDismissRequest = { showAutoSyncDialog = false },
            title = { Text("Auto Sync Settings") },
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
            title = { Text("Confirm Clear Data", fontFamily = poppins) },
            text = {
                Text(
                    "This will permanently delete all app data from this device. Continue?",
                    fontFamily = poppins
                )
            },
            confirmButton = {
                GradientButton(
                    text = "Yes, Clear Data",
                    onClick = {
                        showClearDataConfirm = false
                        showPasswordDialog = true
                    },
                )
            },
            dismissButton = {
                GradientButton(
                    text = "Cancel",
                    onClick = {
                        showClearDataConfirm = false
                    },
                )
            }
        )
    }
    if (showPasswordDialog) {
        var password by remember { mutableStateOf("") }
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
                        modifier = Modifier.fillMaxWidth()
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
                GradientButton("Cancel", onClick = { showPasswordDialog = false })
            }
        )
    }
}

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

}

@Composable
fun BackupDialogExample(
    onDismiss: () -> Unit,
    scope: CoroutineScope,
    userPreferences: UserPreferences
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
                restoreBackupFromDb(context, tempFile)
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
        onDismiss: () -> Unit
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
                val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
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
                    ToastUtils.showToast(context, "✅ Backup email sent successfully to $recipientEmail")
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
        title = { Text("Backup Options") },
        text = { Text("Choose how you’d like to back up your data:") },
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
                                    "✅ Database backup saved at:\n${backupFile.absolutePath}"
                                )
                            } catch (e: Exception) {
                                ToastUtils.showToast(context, "❌ Backup failed: ${e.message}")
                            } finally {
                                onDismiss()
                            }
                        }
                    }
                ) { Text("📂 Save to Device") }



                // 📧 Send via Email
                TextButton(onClick = {
                    val activity = context.findActivity() ?: return@TextButton
                    if (!ensureStoragePermission(context, activity)) {
                        ToastUtils.showToast(context, "⚠️ Please grant storage permission to send backup.")
                        return@TextButton
                    }

                    // ✅ Always fetch fresh from SharedPreferences each time
                    inputEmail = UserPreferences.getInstance(context).getBackupEmail().orEmpty()
                    showEmailDialog = true
                }) {
                    Text("📧 Send via Email")
                }


                // 🔄 Restore Backup
                TextButton(onClick = {
                    restoreFileLauncher.launch("*/*")
                }) {
                    Text("🔄 Restore Backup")
                }

                // 📥 Email Input Dialog
                if (showEmailDialog) {
                    AlertDialog(
                        onDismissRequest = { showEmailDialog = false },
                        title = { Text("Enter Email Address") },
                        text = {
                            TextField(
                                value = inputEmail,
                                onValueChange = { inputEmail = it },
                                label = { Text("Email") },
                                singleLine = true
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                if (android.util.Patterns.EMAIL_ADDRESS.matcher(inputEmail).matches()) {
                                    val userPreferences = UserPreferences.getInstance(context)
                                    userPreferences.saveBackupEmail(inputEmail) // ✅ Save using your preference helper

                                    savedEmail = inputEmail
                                    showEmailDialog = false
                                    ToastUtils.showToast(context, "📤 Sending backup…")

                                    sendBackupEmail(context, scope, inputEmail, onDismiss)
                                } else {
                                    ToastUtils.showToast(context, "⚠️ Please enter a valid email address.")
                                }
                            }) { Text("Save & Send") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEmailDialog = false }) { Text("Cancel") }
                        }
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
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
fun restoreBackupFromDb(context: Context, backupFile: File) {

    try {
        if (!backupFile.exists()) {
            ToastUtils.showToast(context, "❌ Backup file not found!")
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

        ToastUtils.showToast(context, "✅ Database restored successfully!")

        // Restart app to reload DB
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val mainIntent = Intent.makeRestartActivityTask(intent?.component)
        context.startActivity(mainIntent)
        Runtime.getRuntime().exit(0)

    } catch (e: Exception) {
        Log.e("DB_RESTORE", "Restore failed", e)
        ToastUtils.showToast(context, "❌ Restore failed: ${e.message}")
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
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
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
                text = stringResource(R.string.dialog_custom_api_title),
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
                            text = stringResource(R.string.hint_custom_api),
                            fontSize = 14.sp,
                            fontFamily = poppins
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                GradientButton(
                    text = stringResource(R.string.button_save),
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
    onCustomApiClick:()-> Unit,
    onClearDataClick: () -> Unit,
    onRatesClick: () -> Unit,
    onBackupClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedValue by remember {
        try {
            mutableIntStateOf(userPreferences.getInt(item.key, item.defaultValue ?: 0))
        } catch (e: Exception) {
            e.printStackTrace()
            mutableIntStateOf(item.defaultValue ?: 0) // fallback to default
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable {
                when (item.key) {
                    "autosync" -> onAutoSyncClick()
                    "sheet_url" -> onSheetUrlClick()
                    "clear_data" -> onClearDataClick()
                    "apis"->onCustomApiClick()
                    "rates"->onRatesClick()
                    "backup"->onBackupClick()
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
                            .height(300.dp)
                            .width(60.dp)
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

                is SettingType.Action -> {
                    // ✅ If hasToggle is true → show switch instead of subtitle
                    if (item.hasToggle) {
                        Switch(
                            checked = item.isToggled,
                            onCheckedChange = { newValue -> item.onToggleChange?.invoke(newValue) }
                        )
                    } else {
                        Text(item.subtitle ?: "", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ---------------- SHEET URL DIALOG ----------------
@Composable
fun SheetInputDialog(
    sheetUrl: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSetClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text("Set Sheet URL", fontFamily = poppins, fontSize = 14.sp) },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                OutlinedTextField(
                    value = sheetUrl,
                    onValueChange = onValueChange,
                    label = { Text("Enter Sheet Url", fontFamily = poppins, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                GradientButton(
                    text = "Set Sheet Id",
                    onClick = onSetClick,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    )


}
