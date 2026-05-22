package com.loyalstring.rfid.ui.screens


import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Environment
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.loyalstring.rfid.MainActivity
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.reader.ScanKeyListener
import com.loyalstring.rfid.data.remote.data.RfidItem
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.ui.utils.ToastUtils
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.BulkViewModel
import com.loyalstring.rfid.viewmodel.RfidScanToDesktopViewModel
import com.loyalstring.rfid.worker.LocaleHelper
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import kotlin.collections.forEachIndexed

@SuppressLint("HardwareIds")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanToDesktopScreen(onBack: () -> Unit, navController: NavHostController) {
    val viewModel: BulkViewModel = hiltViewModel()
    val context = LocalContext.current
    val localServer = remember { RfidLocalServer(8080) }
    val rfidCodeByEpcMap by viewModel.rfidCodeByEpcMap.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.syncRFIDDataIfNeeded(context)
        viewModel.loadRfidTagMap()
    }
    DisposableEffect(Unit) {
        try {
            if (!localServer.isAlive) {
                localServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
                Log.d("LOCAL_SERVER", "Android local server started")
            }
        } catch (e: Exception) {
            Log.e("LOCAL_SERVER", "Server start failed", e)
        }

        onDispose {
            try {
                localServer.stop()
                Log.d("LOCAL_SERVER", "Android local server stopped")
            } catch (e: Exception) {
                Log.e("LOCAL_SERVER", "Server stop failed", e)
            }
        }
    }

    val tags by viewModel.scannedTags.collectAsState()
    val items by viewModel.scannedItems.collectAsState()
    val rfidMap by viewModel.rfidMap.collectAsState()
    val itemCodeMap by viewModel.itemCodeMap.collectAsState()

    var firstPress by remember { mutableStateOf(false) }

    var selectedPower by remember { mutableIntStateOf(5) }
    var showExportPopup by remember { mutableStateOf(false) }

    val exportScope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
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

    val rfidExportViewModel: RfidScanToDesktopViewModel = hiltViewModel()
    val rfidExportList by rfidExportViewModel.rfidList.collectAsState()
    val exportLoading by rfidExportViewModel.isLoading.collectAsState()


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
    val isLocalWifiMode = userPreferences.isLocalWifiModeEnabled()
    LaunchedEffect(employee?.clientCode) {
        val clientCode = employee?.clientCode
        val deviceId = shortSerial(
            userPreferences.getDeviceId()?.toString()
        )

        if (!clientCode.isNullOrBlank() && deviceId.isNotBlank()) {
            rfidExportViewModel.getAllScantoDesktop(clientCode, deviceId)
            val clientCode = employee?.clientCode
            val deviceId = shortSerial(
                userPreferences.getDeviceId()?.toString()
            )

            if (!clientCode.isNullOrBlank() && deviceId.isNotBlank()) {
                rfidExportViewModel.getAllScantoDesktop(clientCode, deviceId)
            }
        }
    }

        // Trigger only when new tags arrive (size change), not on every recompose
        LaunchedEffect(tags.size) {
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
                    title = localizedContext.getString(R.string.scan_to_desktop_title),

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

                  /*  onSave = {
                        viewModel.barcodeReader.close()
                        Log.d("save scanned items", "CLICKED" + tags.size)

                        val deviceId = shortSerial(
                            userPreferences.getDeviceId()?.toString()
                        )
                        val clientCode = employee?.clientCode.orEmpty()

                        if (tags.isEmpty() || deviceId.isNullOrBlank() || clientCode.isBlank()) {
                            ToastUtils.showToast(
                                context,
                                "Please scan RFID tag / Device Id not found"
                            )
                            return@ScanBottomBarDesktop
                        }



                        if (isLocalWifiMode) {
                            // ✅ LOCAL WIFI MODE
                            val itemsArray = JSONArray()

                            tags.forEachIndexed { index, tag ->

                                val epcValue = tag.epc
                                    .trim()
                                    .uppercase()
                                    .replace(" ", "")
                                    .replace("\n", "")
                                    .replace("\r", "")

                                val mappedRfidCode =
                                    if (epcValue.startsWith("E", ignoreCase = true)) {
                                        rfidCodeByEpcMap[epcValue].orEmpty()
                                    } else {
                                        hexToAscii(epcValue)
                                    }

                                val finalRfidCode = rfidMap[index].orEmpty().ifBlank {
                                    mappedRfidCode
                                }

// ✅ Skip scan here / blank rows
                                val cleanRfidCode = finalRfidCode.trim()

                                if (
                                    epcValue.isNotBlank() &&
                                    cleanRfidCode.isNotBlank() &&
                                    !cleanRfidCode.equals("scan here", ignoreCase = true)
                                ) {
                                    val itemObject = JSONObject().apply {
                                        put("EPC", epcValue)
                                        put("RFIDCode", cleanRfidCode)
                                    }

                                    itemsArray.put(itemObject)
                                }

                            *//*    val itemObject = JSONObject().apply {
                                    put("EPC", tag.epc.orEmpty())
                                    val epcValue = tag.epc
                                        .trim()
                                        .uppercase()
                                        .replace(" ", "")
                                        .replace("\n", "")
                                        .replace("\r", "")

                                    val finalRfidCode = rfidMap[index].orEmpty().ifBlank {
                                        rfidCodeByEpcMap[epcValue].orEmpty()
                                    }

                                    put("RFIDCode", finalRfidCode)
                                   // put("RFIDCode", rfidMap[index].orEmpty())
                                }

                                itemsArray.put(itemObject)*//*
                            }

                            val requestBody = JSONObject().apply {
                                put("ClientCode", clientCode)
                                put("DeviceId", deviceId)
                                put("Items", itemsArray)
                            }

                            localServer.latestJson = requestBody.toString()

                            val androidIp = getAndroidDeviceIp()

                            if (androidIp.isBlank()) {
                                ToastUtils.showToast(
                                    context,
                                    "Device IP not found. Connect RFID and desktop to same WiFi/hotspot."
                                )
                                return@ScanBottomBarDesktop
                            }

                            val desktopUrl = "http://$androidIp:8080/rfid-data"

                            Log.d("LOCAL_SERVER", "Open this URL on desktop: $desktopUrl")
                            Log.d("LOCAL_SERVER", "Data = $requestBody")

                            ToastUtils.showToast(
                                context,
                                "Local Mode: Open on desktop: $desktopUrl"
                            )

                        } else {
                            // ✅ ONLINE / INTERNET API MODE
                            viewModel.sendScannedData(tags, deviceId, context)

                            ToastUtils.showToast(
                                context,
                                "Internet Mode: Data sent to server"
                            )

                            viewModel.resetScanResults()
                            viewModel.stopBarcodeScanner()
                            viewModel.resetProductScanResults()
                        }
                    },*/
                    onSave = {
                        viewModel.barcodeReader.close()
                        Log.d("save scanned items", "CLICKED ${tags.size}")

                        val deviceId = shortSerial(userPreferences.getDeviceId()?.toString())
                        val clientCode = employee?.clientCode.orEmpty()

                        if (tags.isEmpty() || deviceId.isBlank() || clientCode.isBlank()) {
                            ToastUtils.showToast(
                                context,
                                "Please scan RFID tag / Device Id not found"
                            )
                            return@ScanBottomBarDesktop
                        }

                        val itemsArray = JSONArray()

                        tags.forEachIndexed { index, tag ->

                            val epcValue = tag.epc
                                .trim()
                                .uppercase()
                                .replace(" ", "")
                                .replace("\n", "")
                                .replace("\r", "")

                            val mappedRfidCode =
                                if (epcValue.startsWith("E", ignoreCase = true)) {
                                    rfidCodeByEpcMap[epcValue].orEmpty()
                                } else {
                                    hexToAscii(epcValue)
                                }

                            val cleanRfidCode = rfidMap[index]
                                .orEmpty()
                                .ifBlank { mappedRfidCode }
                                .trim()

                            if (
                                epcValue.isNotBlank() &&
                                cleanRfidCode.isNotBlank() &&
                                !cleanRfidCode.equals("scan here", ignoreCase = true)
                            ) {
                                val itemObject = JSONObject().apply {
                                    put("EPC", epcValue)
                                    put("RFIDCode", cleanRfidCode)
                                }

                                itemsArray.put(itemObject)
                            }
                        }

                        if (itemsArray.length() == 0) {
                            ToastUtils.showToast(context, "No valid EPC/RFID data found")
                            return@ScanBottomBarDesktop
                        }

                        val requestBody = JSONObject().apply {
                            put("ClientCode", clientCode)
                            put("DeviceId", deviceId)
                            put("Items", itemsArray)
                        }

                        if (isLocalWifiMode) {
                            localServer.latestJson = requestBody.toString()

                            val androidIp = getAndroidDeviceIp()

                            if (androidIp.isBlank()) {
                                ToastUtils.showToast(
                                    context,
                                    "Device IP not found. Connect RFID and desktop to same WiFi/hotspot."
                                )
                                return@ScanBottomBarDesktop
                            }

                            val desktopUrl = "http://$androidIp:8080/rfid-data"

                            Log.d("LOCAL_SERVER", "Open this URL on desktop: $desktopUrl")
                            Log.d("LOCAL_SERVER", "Data = $requestBody")

                            ToastUtils.showToast(
                                context,
                                "Local Mode: Open on desktop: $desktopUrl"
                            )

                        } else {
                            // ✅ ONLINE / INTERNET API MODE
                            viewModel.sendScannedData(tags, deviceId, context)

                            ToastUtils.showToast(
                                context,
                                "Internet Mode: Data sent to server"
                            )

                            viewModel.resetScanResults()
                            viewModel.stopBarcodeScanner()
                            viewModel.resetProductScanResults()
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
                                val epcValue = item.epc
                                    .trim()
                                    .uppercase()
                                    .replace(" ", "")
                                    .replace("\n", "")
                                    .replace("\r", "")

                                val manualRfidCode = rfidMap[index].orEmpty()

                               // val mappedRfidCode = rfidCodeByEpcMap[epcValue].orEmpty()
                                val mappedRfidCode =
                                    if (epcValue.startsWith("E", ignoreCase = true)) {
                                        rfidCodeByEpcMap[epcValue].orEmpty()
                                    } else {
                                        hexToAscii(epcValue)
                                    }
                                val finalRfidCode = manualRfidCode.ifBlank {
                                    mappedRfidCode
                                }

                                val displayText = finalRfidCode.ifBlank { "scan here" }
                                val isScanned = finalRfidCode.isNotBlank()
                              /* for testinmg  val rfid = rfidMap[index]
                                // val itemCode = hexToAscii(item.epc) ?: ""

                                val epcValue = item.epc
                                    .trim()
                                    .uppercase()
                                    .replace(" ", "")
                                    .replace("\n", "")
                                    .replace("\r", "")

                                val itemCode = if (epcValue.startsWith("E", ignoreCase = true)) {
                                    itemCodeMap[epcValue.uppercase()].orEmpty()
                                } else {
                                    hexToAscii(epcValue)
                                }
                                Log.d("itemCode", "itemCode" + itemCode)

                                val displayText =
                                    if (!rfid.isNullOrBlank()) rfid
                                    else itemCode.toString().ifBlank { "scan here" }
                                val isScanned = !rfid.isNullOrBlank() || !itemCode.isNullOrBlank()
                             */   // val displayText = if (isScanned) rfid!! else itemCode.ifBlank { "scan here" }
                                val textColor = if (!isScanned) Color.Blue else Color.DarkGray
                                val style =
                                    if (!isScanned) TextDecoration.Underline else TextDecoration.None

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
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ExportOptionRow(
                                title = if (isExporting) "Exporting..." else "Export Excel",
                                onClick = {
                                    if (isExporting) return@ExportOptionRow

                                    showExportPopup = false

                                    if (rfidExportList.isEmpty()) {
                                        ToastUtils.showToast(context, "No data found for export")
                                        return@ExportOptionRow
                                    }

                                    exportScope.launch {
                                        try {
                                            isExporting = true

                                            val file = withContext(Dispatchers.IO) {
                                                exportRfidExcel(context, rfidExportList)
                                            }

                                            ToastUtils.showToast(
                                                context,
                                                "Excel downloaded: ${file.name}"
                                            )

                                        } catch (e: Exception) {
                                            Log.e("EXPORT_EXCEL", "Export failed", e)
                                            ToastUtils.showToast(
                                                context,
                                                "Export failed: ${e.message ?: "Unknown error"}"
                                            )
                                        } finally {
                                            isExporting = false
                                        }
                                    }
                                }
                            )

                            ExportOptionRow(
                                title = if (isExporting) "Preparing..." else "Email",
                                onClick = {
                                    if (isExporting) return@ExportOptionRow

                                    showExportPopup = false

                                    if (rfidExportList.isEmpty()) {
                                        ToastUtils.showToast(context, "No data found for email")
                                        return@ExportOptionRow
                                    }

                                    exportScope.launch {
                                        try {
                                            isExporting = true

                                            val file = withContext(Dispatchers.IO) {
                                                exportRfidExcelForEmail(context, rfidExportList)
                                            }

                                            shareExcelByEmail(context, file)

                                        } catch (e: Exception) {
                                            Log.e("EMAIL_EXCEL", "Email failed", e)
                                            ToastUtils.showToast(
                                                context,
                                                "Email failed: ${e.message ?: "Unknown error"}"
                                            )
                                        } finally {
                                            isExporting = false
                                        }
                                    }
                                }
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

    fun getAndroidDeviceIp(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()

            for (networkInterface in interfaces) {
                val interfaceName = networkInterface.name ?: ""

                // wlan0 = Wi-Fi client
                // ap0 / swlan0 / wlan1 can appear in hotspot/tethering cases
                if (
                    !interfaceName.contains("wlan", ignoreCase = true) &&
                    !interfaceName.contains("ap", ignoreCase = true)
                ) {
                    continue
                }

                val addresses = networkInterface.inetAddresses

                for (address in addresses) {
                    if (
                        !address.isLoopbackAddress &&
                        address is Inet4Address
                    ) {
                        val ip = address.hostAddress ?: ""

                        if (
                            ip.isNotBlank() &&
                            ip != "0.0.0.0" &&
                            !ip.startsWith("127.")
                        ) {
                            Log.d("LOCAL_SERVER", "Found IP: $ip on interface=$interfaceName")
                            return ip
                        }
                    }
                }
            }

            ""
        } catch (e: Exception) {
            Log.e("LOCAL_SERVER", "IP fetch failed", e)
            ""
        }
    }

    private class RfidLocalServer(
        port: Int = 8080
    ) : NanoHTTPD(port) {

        var latestJson: String = "{}"

        override fun serve(session: IHTTPSession): Response {
            return when (session.uri) {
                "/rfid-data" -> {
                    newFixedLengthResponse(
                        Response.Status.OK,
                        "application/json",
                        latestJson
                    ).apply {
                        addHeader("Access-Control-Allow-Origin", "*")
                    }
                }

                "/" -> {
                    newFixedLengthResponse(
                        Response.Status.OK,
                        "text/html",
                        """
                    <html>
                        <body>
                            <h2>RFID Local Server Running</h2>
                            <p>Open <b>/rfid-data</b> to view scanned RFID data.</p>
                        </body>
                    </html>
                    """.trimIndent()
                    )
                }

                else -> {
                    newFixedLengthResponse(
                        Response.Status.NOT_FOUND,
                        "text/plain",
                        "Not Found"
                    )
                }
            }
        }


    }


    @Composable
    private fun ExportOptionRow(
        title: String,
        onClick: () -> Unit
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFF5F5F5),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontFamily = poppins,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF333333),
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = ">",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )
            }
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

    private fun exportRfidExcelForEmail(
        context: Context,
        list: List<RfidItem>
    ): File {
        val workbook = XSSFWorkbook()

        return try {
            val sheet = workbook.createSheet("RFID Data")

            val headers = listOf(
                "Sr No",
                "Client Code",
                "Device Id",
                "TID Value",
                "RFID Code",
                "Id",
                "Created On",
                "Last Updated",
                "Status"
            )

            val header = sheet.createRow(0)
            headers.forEachIndexed { index, title ->
                header.createCell(index).setCellValue(title)
            }

            list.forEachIndexed { index, item ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue((index + 1).toDouble())
                row.createCell(1).setCellValue(item.ClientCode)
                row.createCell(2).setCellValue(item.DeviceId)
                row.createCell(3).setCellValue(item.TIDValue)
                row.createCell(4).setCellValue(item.RFIDCode)
                row.createCell(5).setCellValue(item.Id.toDouble())
                row.createCell(6).setCellValue(item.CreatedOn)
                row.createCell(7).setCellValue(item.LastUpdated)
                row.createCell(8).setCellValue(item.StatusType.toString())
            }

            val file = File(
                context.cacheDir,
                "rfid_scan_to_desktop_${System.currentTimeMillis()}.xlsx"
            )

            FileOutputStream(file).use { outputStream ->
                workbook.write(outputStream)
            }

            file

        } finally {
            workbook.close()
        }
    }

    private fun exportRfidExcel(
        context: Context,
        list: List<RfidItem>
    ): File {
        val workbook = XSSFWorkbook()

        return try {
            val sheet = workbook.createSheet("RFID Data")

            val headers = listOf(
                "Sr No",
                "Client Code",
                "Device Id",
                "TID Value",
                "RFID Code",
                "Id",
                "Created On",
                "Last Updated",
                "Status"
            )

            val header = sheet.createRow(0)
            headers.forEachIndexed { index, title ->
                header.createCell(index).setCellValue(title)
            }

            list.forEachIndexed { index, item ->
                val row = sheet.createRow(index + 1)

                row.createCell(0).setCellValue((index + 1).toDouble())
                row.createCell(1).setCellValue(item.ClientCode)
                row.createCell(2).setCellValue(item.DeviceId)
                row.createCell(3).setCellValue(item.TIDValue)
                row.createCell(4).setCellValue(item.RFIDCode)
                row.createCell(5).setCellValue(item.Id.toDouble())
                row.createCell(6).setCellValue(item.CreatedOn)
                row.createCell(7).setCellValue(item.LastUpdated)
                row.createCell(8).setCellValue(item.StatusType.toString())
            }

            for (i in headers.indices) {
                sheet.setColumnWidth(i, 5000)
            }

            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir

            if (!dir.exists()) {
                dir.mkdirs()
            }

            val file = File(
                dir,
                "rfid_scan_to_desktop_${System.currentTimeMillis()}.xlsx"
            )

            FileOutputStream(file).use { outputStream ->
                workbook.write(outputStream)
            }

            file

        } finally {
            workbook.close()
        }
    }

    private fun shareExcelByEmail(
        context: Context,
        file: File
    ) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

            putExtra(Intent.EXTRA_SUBJECT, "RFID Scan To Desktop Excel")
            putExtra(Intent.EXTRA_TEXT, "Please find attached RFID scan data excel file.")
            putExtra(Intent.EXTRA_STREAM, uri)

            clipData = ClipData.newUri(
                context.contentResolver,
                "RFID Excel",
                uri
            )

            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(
            Intent.createChooser(intent, "Send Excel File").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        )
    }



