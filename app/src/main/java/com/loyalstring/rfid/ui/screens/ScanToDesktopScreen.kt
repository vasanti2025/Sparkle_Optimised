package com.loyalstring.rfid.ui.screens

import android.annotation.SuppressLint
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.loyalstring.rfid.MainActivity
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.reader.ScanKeyListener
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.ui.utils.ToastUtils
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.BulkViewModel
import com.loyalstring.rfid.worker.LocaleHelper

@SuppressLint("HardwareIds")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanToDesktopScreen(onBack: () -> Unit, navController: NavHostController) {
    val viewModel: BulkViewModel = hiltViewModel()
    val context = LocalContext.current

    val tags by viewModel.scannedTags.collectAsState()
    val items by viewModel.scannedItems.collectAsState()
    val rfidMap by viewModel.rfidMap.collectAsState()
    val itemCodeMap by viewModel.itemCodeMap.collectAsState()

    var firstPress by remember { mutableStateOf(false) }

    var selectedPower by remember { mutableIntStateOf(5) }
    var showExportPopup by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        selectedPower = UserPreferences.getInstance(context).getInt(
            UserPreferences.KEY_PRODUCT_COUNT,
            5
        )
    }
    var shouldNavigateBack by remember { mutableStateOf(false) }

    LaunchedEffect(shouldNavigateBack) {
        if (shouldNavigateBack) {
            kotlinx.coroutines.delay(50)
            onBack()
        }
    }

    var clickedIndex by remember { mutableStateOf<Int?>(null) }
    val activity = LocalContext.current as MainActivity
    var isScanning by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }

    val loading by viewModel.clearLoading.collectAsState()
    val success by viewModel.clearSuccess.collectAsState()
    val deleted by viewModel.deletedRecords.collectAsState()
    val error by viewModel.clearError.collectAsState()
    val employee = UserPreferences.getInstance(context).getEmployee(Employee::class.java)
    var showClearDialog by remember { mutableStateOf(false) }

    val userPreferences = UserPreferences.getInstance(context)
    val savedLang = userPreferences.getAppLanguage().ifBlank { "en" }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = currentLocales[0]?.language ?: savedLang
    val localizedContext = LocaleHelper.applyLocale(context, currentLang)

    // ✅ IMPORTANT: whenever tags change → auto fill RFID from DB
    LaunchedEffect(tags) {
        if (tags.isNotEmpty()) {
            viewModel.autoFillRfidFromDb(tags)
        }
    }

    DisposableEffect(Unit) {
        val listener = object : ScanKeyListener {
            override fun onBarcodeKeyPressed() {
                viewModel.startBarcodeScanning(context)
            }

            override fun onRfidKeyPressed() {
                if (isScanning) {
                    viewModel.stopScanning()
                    isScanning = false
                } else {
                    viewModel.startScanning(selectedPower)
                    isScanning = true
                }
            }
        }
        activity.registerScanKeyListener(listener)

        onDispose {
            activity.unregisterScanKeyListener()
            viewModel.stopScanning()
        }
    }

    // ✅ Barcode scan callback
    LaunchedEffect(Unit) {
        viewModel.barcodeReader.openIfNeeded()
        viewModel.barcodeReader.setOnBarcodeScanned { scanned ->
            viewModel.onBarcodeScanned(scanned)
            clickedIndex?.let { index ->
                viewModel.assignRfidCode(index, scanned) // manual override
                clickedIndex = null
            }
        }
    }

    LaunchedEffect(tags) {
        tags.forEach { tag ->
            val epc = tag.epc.trim().uppercase()

            // avoid duplicate DB calls
            if (!itemCodeMap.containsKey(epc)) {
                viewModel.loadItemCodeForEpc(epc)
            }
        }
    }



    // ✅ success / error message show once
    LaunchedEffect(success, error) {
        when {
            success -> {
                ToastUtils.showToast(context, "✅ Cleared ${deleted} records successfully")
                viewModel.clearClearStockResult()  // reset so it won’t re-toast
            }
            error != null -> {
                ToastUtils.showToast(context, "❌ ${error}")
                viewModel.clearClearStockResult()
            }
        }
    }

    val androidId = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID
    )

    userPreferences.saveDeviceId(androidId)

    Scaffold(
        topBar = {
            GradientTopBar(
                title =  localizedContext.getString(R.string.scan_to_desktop_title),

                navigationIcon = {
                    IconButton(onClick = { shouldNavigateBack = true }) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {},
                showCounter = true,
                selectedCount = selectedPower,

                onCountSelected = { selectedPower = it },
                titleTextSize = 20.sp


            )
        },
        bottomBar = {
            ScanBottomBarDesktop(
                onSave = {
                    viewModel.barcodeReader.close()
                    Log.d("save scanned items", "CLICKED"+tags.size)



                    // ✅ Better check: tags exist + at least one RFID mapped
                    if (tags.isNotEmpty()) {
                        viewModel.sendScannedData(tags, shortSerial(userPreferences.getDeviceId().toString()), context)
                        viewModel.resetScanResults()
                        viewModel.stopBarcodeScanner()
                        viewModel.resetProductScanResults()
                    } else {
                        ToastUtils.showToast(context, "Please scan RFID tag / RFID not found in DB")
                    }
                },
                onClear = {
                    showClearDialog = true
                    viewModel.resetScanResults()
                    viewModel.stopBarcodeScanner()
                    viewModel.resetProductScanResults()
                },
                onScan = { viewModel.startSingleScan(20) },
                onGscan = {
                    if (isScanning) {
                        viewModel.stopScanning()
                        isScanning = false
                    } else {
                        viewModel.startScanning(selectedPower)
                        isScanning = true
                    }
                },
                onReset = {
                    firstPress = false
                    isScanning = false
                    viewModel.resetScanResults()
                    viewModel.stopBarcodeScanner()
                    viewModel.resetProductScanResults()
                },
                isScanning = isScanning,
                isEditMode = isEditMode,
                isScreen = false
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.DarkGray)
                    .padding(vertical = 8.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    localizedContext.getString(R.string.sr_header),
                    Modifier.weight(0.8f),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontFamily = poppins,
                    fontSize = 13.sp
                )
                Text(
                    localizedContext.getString(R.string.lbl_epc),
                    Modifier.weight(2.2f),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontFamily = poppins,
                    fontSize = 13.sp
                )
                Text(
                    localizedContext.getString(R.string.rfid_code),
                    Modifier.weight(2f),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    fontFamily = poppins,
                    fontSize = 13.sp
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFFF0F0F0))
            ) {
                itemsIndexed(tags) { index, item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${index + 1}",
                                Modifier.width(100.dp).background(Color.Transparent),
                                color = Color.DarkGray,
                                fontFamily = poppins,
                                fontSize = 11.sp
                            )

                            Text(
                                item.epc,
                                Modifier.width(100.dp).background(Color.Transparent),
                                color = Color.DarkGray,
                                fontFamily = poppins,
                                fontSize = 11.sp
                            )

                            // ✅ AUTO-FILLED FROM DB (via viewModel.autoFillRfidFromDb)
                            val rfid = rfidMap[index]
                            val itemCode = hexToAscii(item.epc) ?: ""
                            Log.d("itemCode","itemCode"+itemCode)

                            val displayText =
                                if (!rfid.isNullOrBlank()) rfid
                                else itemCode.toString().ifBlank { "scan here" }
                            val isScanned = !rfid.isNullOrBlank() || !itemCode.isNullOrBlank()
                           // val displayText = if (isScanned) rfid!! else itemCode.ifBlank { "scan here" }
                            val textColor = if (!isScanned) Color.Blue else Color.DarkGray
                            val style = if (!isScanned) TextDecoration.Underline else TextDecoration.None

                            Text(
                                " $displayText",
                                Modifier
                                    .width(100.dp)
                                    .clickable {
                                        // manual override
                                        clickedIndex = index
                                        viewModel.startBarcodeScanning(context)
                                    },
                                color = textColor,
                                textDecoration = style,
                                fontFamily = poppins,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(
                            modifier = Modifier
                                .height(1.dp)
                                .fillMaxWidth()
                                .background(Color.LightGray)
                                .align(Alignment.BottomCenter)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.DarkGray)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = localizedContext.getString(R.string.total_items, tags.size),
                    color = Color.White,
                    fontFamily = poppins,
                    fontSize = 12.sp
                )

                Text(
                    text = "Export Data",
                    color = Color.White,
                    fontFamily = poppins,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        showExportPopup = true
                    }
                )
            }
        }
    }

    if (showExportPopup) {
        Dialog(onDismissRequest = { showExportPopup = false }) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF3A3A3A))
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Export Data",
                            fontSize = 18.sp,
                            color = Color.White,
                            fontFamily = poppins,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Export Excel",
                            fontFamily = poppins,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showExportPopup = false
                                    exportExcel()
                                }
                                .padding(vertical = 12.dp)
                        )

                        Text(
                            text = "Email",
                            fontFamily = poppins,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showExportPopup = false
                                    sendEmail()
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            }
        }
    }

    // ✅ Confirm Popup
    if (showClearDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Confirm") },
            text = { Text("Are you sure you want to clear/delete the stock data from server?") },
            confirmButton = {
                Text(
                    "OK",
                    modifier = Modifier
                        .padding(12.dp)
                        .clickable {
                            showClearDialog = false
                            val clientCode = employee?.clientCode ?: return@clickable

                            val deviceId = shortSerial(
                                userPreferences.getDeviceId()?.toString()
                            )

                            viewModel.clearStockData(clientCode, deviceId)
                        },
                    color = Color.Red
                )
            },
            dismissButton = {
                Text(
                    "Cancel",
                    modifier = Modifier
                        .padding(12.dp)
                        .clickable { showClearDialog = false }
                )
            }
        )
    }
}

private fun sendEmail() {
    TODO("Not yet implemented")
}

private fun exportExcel() {
    TODO("Not yet implemented")
}

fun hexToAscii(hex: String): String {
    val cleanHex = hex.replace(" ", "").uppercase()

    if (cleanHex.length % 2 != 0) return ""

    return try {
        buildString {
            for (i in cleanHex.indices step 2) {
                val part = cleanHex.substring(i, i + 2)
                val char = part.toInt(16).toChar()
                if (char.code in 32..126) { // printable ASCII only
                    append(char)
                }
            }
        }
    } catch (e: Exception) {
        ""
    }
}




fun shortSerial(serial: String?): String {
    if (serial.isNullOrBlank()) return "A"
    if (serial.length < 2) return "A$serial"
    val lastTwo = serial.takeLast(2)
    return "A$lastTwo"
}


