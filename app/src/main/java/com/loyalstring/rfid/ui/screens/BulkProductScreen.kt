package com.loyalstring.rfid.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope

import androidx.navigation.NavHostController
import com.loyalstring.rfid.MainActivity
import com.loyalstring.rfid.R

import com.loyalstring.rfid.data.reader.ScanKeyListener
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.navigation.Screens
import com.loyalstring.rfid.ui.utils.*
import com.loyalstring.rfid.viewmodel.BulkViewModel
import com.loyalstring.rfid.worker.LocaleHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@SuppressLint("RestrictedApi", "UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkProductScreen(
    onBack: () -> Unit,
    navController: NavHostController
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val viewModel: BulkViewModel = hiltViewModel()
    val context = LocalContext.current

    // Observers
    val tags by viewModel.scannedTags.collectAsState()


    // Dropdown data
    val categories by viewModel.categories.collectAsState()
    val products by viewModel.products.collectAsState()
    val designs by viewModel.designs.collectAsState()

    var selectedCategory by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf("") }
    var selectedDesign by remember { mutableStateOf("") }

    var showAddDialogFor by remember { mutableStateOf<String?>(null) }
    var shouldNavigateBack by remember { mutableStateOf(false) }

    var clickedIndex by remember { mutableStateOf<Int?>(null) }

    var isScanning by remember { mutableStateOf(false) }
    var selectedPower by remember { mutableIntStateOf(5) }

    var applyItemCodeToAll by remember { mutableStateOf(false) }
    var applyRfidToAll by remember { mutableStateOf(false) }
    val applyItemCodeToAllState by rememberUpdatedState(applyItemCodeToAll)
    val applyRfidToAllState by rememberUpdatedState(applyRfidToAll)

    // Per-row local state — avoids ViewModel updates on each keystroke (keeps keyboard open)
    val itemCodeList = remember { mutableStateMapOf<Int, String>() }
    val rfidCodeList = remember { mutableStateMapOf<Int, String>() }

    val userPreferences = UserPreferences.getInstance(context)
    val savedLang = userPreferences.getAppLanguage().ifBlank { "en" }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = currentLocales[0]?.language ?: savedLang
    val localizedContext = LocaleHelper.applyLocale(context, currentLang)

    LaunchedEffect(Unit) {
        selectedPower = UserPreferences.getInstance(context).getInt(
            UserPreferences.KEY_PRODUCT_COUNT,
            5
        )
    }


    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    viewModel.barcodeReader.openIfNeeded()
                    registerBulkBarcodeHandler(
                        viewModel = viewModel,
                        rfidCodeList = rfidCodeList,
                        applyRfidToAll = { applyRfidToAllState },
                        rowCount = { tags.size }
                    )
                }

                Lifecycle.Event.ON_PAUSE -> {
                    viewModel.stopScanning()
                    viewModel.stopBarcodeScanner()
                    viewModel.barcodeReader.close()
                    isScanning = false
                }

                Lifecycle.Event.ON_DESTROY -> {
                    viewModel.stopScanning()
                    viewModel.stopBarcodeScanner()
                    viewModel.barcodeReader.close()
                }

                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopScanning()
            viewModel.stopBarcodeScanner()
            viewModel.barcodeReader.close()
        }
    }

    val isBulkMode by viewModel.isBulkMode.collectAsState()

    LaunchedEffect(shouldNavigateBack) {
        if (shouldNavigateBack) {
            delay(80)
            onBack()
        }
    }

    // Register hardware key listener
    val activity = LocalContext.current as MainActivity

    DisposableEffect(Unit) {
        val listener = object : ScanKeyListener {

            override fun onBarcodeKeyPressed() {
                ensureBarcodeTargetRow(viewModel, rfidCodeList)
                viewModel.barcodeReader.openIfNeeded()
                registerBulkBarcodeHandler(
                    viewModel = viewModel,
                    rfidCodeList = rfidCodeList,
                    applyRfidToAll = { applyRfidToAllState },
                    rowCount = { tags.size }
                )
                viewModel.startBarcodeScanning(context)
            }

            override fun onRfidKeyPressed() {
                viewModel.setBulkMode(true)
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
        }
    }

    LaunchedEffect(Unit) {
        viewModel.barcodeReader.openIfNeeded()
        registerBulkBarcodeHandler(
            viewModel = viewModel,
            rfidCodeList = rfidCodeList,
            applyRfidToAll = { applyRfidToAllState },
            rowCount = { tags.size }
        )
    }


    Scaffold(
        topBar = {
            GradientTopBar(
                title =  localizedContext.getString(R.string.add_bulk_products),
                navigationIcon = {
                    IconButton(
                        onClick = { shouldNavigateBack = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                showCounter = true,
                selectedCount = selectedPower,
                onCountSelected = { selectedPower = it },
                titleTextSize = 20.sp
            )
        },

        bottomBar = {
            ScanBottomBar(
                onSave = {
                    viewModel.barcodeReader.close()

                    if (selectedCategory.isBlank() || selectedProduct.isBlank() || selectedDesign.isBlank()) {
                        ToastUtils.showToast(context, "Category/Product/Design cannot be empty")
                        return@ScanBottomBar
                    }

                    viewModel.saveAllBulkProductRows(
                        category = selectedCategory,
                        product = selectedProduct,
                        design = selectedDesign,
                        scannedTags = tags,
                        itemCodes = itemCodeList.toMap(),
                        rfidCodes = rfidCodeList.toMap()
                    )

                    ToastUtils.showToast(context, "Items saved successfully")
                    viewModel.resetScanResults()
                    navController.navigate(Screens.ProductManagementScreen.route)
                },

                onList = { navController.navigate(Screens.ProductListScreen.route) },

                onScan = {
                    viewModel.setBulkMode(false)
                    viewModel.barcodeReader.openIfNeeded()
                    val activeIndex = viewModel.lastClickedIndex
                    Log.d("UI", "Scan button pressed → lastClickedIndex = $activeIndex")
                    viewModel.startSingleScan(22)
                },

                onGscan = {
                    viewModel.setBulkMode(true)
                    viewModel.barcodeReader.openIfNeeded()
                    if (isScanning) {
                        viewModel.stopScanning()
                        isScanning = false
                    } else {
                        viewModel.startScanning(selectedPower)
                        isScanning = true
                    }
                },

                onReset = {
                    try {

                        viewModel.stopBarcodeScanner()

                        viewModel.resetProductScanResults()

                        itemCodeList.clear()
                        rfidCodeList.clear()
                        applyItemCodeToAll = false
                        applyRfidToAll = false

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },

                isScanning = isScanning,
                isEditMode = false,
                isScreen = false,
                isBulkScanning = false
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.White)
        ) {

            // ----------------------
            // FILTER ROW
            // ----------------------
            Row(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterDropdown(
                    label = "Category",
                    options = categories.map { it.name },
                    selectedOption = selectedCategory,
                    onOptionSelected = { selectedCategory = it },
                    onAddOption = { showAddDialogFor = "Category" },
                    modifier = Modifier.weight(1f)
                )

                FilterDropdown(
                    label = "Product",
                    options = products.map { it.name },
                    selectedOption = selectedProduct,
                    onOptionSelected = { selectedProduct = it },
                    onAddOption = { showAddDialogFor = "Product" },
                    modifier = Modifier.weight(1f)
                )

                FilterDropdown(
                    label = "Design",
                    options = designs.map { it.name },
                    selectedOption = selectedDesign,
                    onOptionSelected = { selectedDesign = it },
                    onAddOption = { showAddDialogFor = "Design" },
                    modifier = Modifier.weight(1f)
                )
            }

            if (showAddDialogFor != null) {
                AddItemDialog(
                    title = showAddDialogFor!!,
                    onAdd = { newVal ->
                        when (showAddDialogFor) {
                            "Category" -> {
                                selectedCategory = newVal
                                viewModel.saveDropdownCategory(newVal, "Category")
                            }

                            "Product" -> {
                                selectedProduct = newVal
                                viewModel.saveDropdownProduct(newVal, "Product")
                            }

                            "Design" -> {
                                selectedDesign = newVal
                                viewModel.saveDropdownDesign(newVal, "Design")
                            }
                        }
                        showAddDialogFor = null
                    },
                    onDismiss = { showAddDialogFor = null }
                )
            }

            // ----------------------
            // TABLE HEADER
            // ----------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.DarkGray)
                    .padding(vertical = 6.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Box(Modifier.width(44.dp), contentAlignment = Alignment.Center) {
                    Text(localizedContext.getString(R.string.header_sr), color = Color.White, fontSize = 12.sp, fontFamily = poppins)
                }

                Box(Modifier.width(130.dp), contentAlignment = Alignment.Center) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        BulkApplyAllCheckbox(
                            checked = applyItemCodeToAll,
                            onCheckedChange = { applyItemCodeToAll = it }
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            localizedContext.getString(R.string.itemcode),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontFamily = poppins
                        )
                    }
                }

                Box(Modifier.width(130.dp), contentAlignment = Alignment.Center) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        BulkApplyAllCheckbox(
                            checked = applyRfidToAll,
                            onCheckedChange = { applyRfidToAll = it }
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            localizedContext.getString(R.string.rfid_code),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontFamily = poppins
                        )
                    }
                }
            }

            // ----------------------
            // DATA ROWS
            // ----------------------
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(4.dp)
            ) {

                itemsIndexed(
                    items = tags,
                    key = { index, tag -> "${index}_${tag.epc.orEmpty()}" }
                ) { index, tag ->

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // --- Sr No ---
                        Box(
                            modifier = Modifier
                                .width(50.dp)
                                .height(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${index + 1}",
                                fontSize = 11.sp,
                                color = Color.DarkGray,
                                fontFamily = poppins,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // --- Item Code ---
                        Box(
                            modifier = Modifier
                                .width(150.dp)
                                .height(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicTextField(
                                value = itemCodeList[index] ?: "",
                                onValueChange = { newValue ->
                                    applyItemCodeToRows(
                                        index = index,
                                        value = newValue,
                                        applyToAll = applyItemCodeToAllState,
                                        itemCodeList = itemCodeList,
                                        rowCount = tags.size
                                    )
                                },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = Color.DarkGray),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            viewModel.setLastClickedIndex(index)
                                        }
                                    }
                                    .padding(horizontal = 6.dp),
                                decorationBox = { innerTextField ->
                                    Box(
                                        Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) { innerTextField() }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // --- RFID Code ---
                        Box(
                            modifier = Modifier
                                .width(150.dp)
                                .height(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicTextField(
                                value = rfidCodeList[index] ?: "",
                                onValueChange = { newRfid ->
                                    applyRfidToRows(
                                        index = index,
                                        value = newRfid,
                                        applyToAll = applyRfidToAllState,
                                        rfidCodeList = rfidCodeList,
                                        rowCount = tags.size
                                    )
                                },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = Color.DarkGray),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusable(true)
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            viewModel.setLastClickedIndex(index)
                                        }
                                    }
                                    .padding(horizontal = 6.dp),
                                decorationBox = { innerTextField ->
                                    Box(
                                        Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) { innerTextField() }
                                }
                            )
                        }
                    }


                    Divider(color = Color.LightGray, thickness = 0.6.dp)
                }
            }
        }
    }
}

@Composable
private fun BulkApplyAllCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.size(22.dp),
        colors = CheckboxDefaults.colors(
            checkedColor = Color.White,
            uncheckedColor = Color.White.copy(alpha = 0.7f),
            checkmarkColor = Color.DarkGray
        )
    )
}

private fun applyItemCodeToRows(
    index: Int,
    value: String,
    applyToAll: Boolean,
    itemCodeList: MutableMap<Int, String>,
    rowCount: Int
) {
    val normalized = value.uppercase()
    if (applyToAll) {
        for (i in 0 until rowCount) {
            itemCodeList[i] = normalized
        }
    } else {
        itemCodeList[index] = normalized
    }
}

private fun applyRfidToRows(
    index: Int,
    value: String,
    applyToAll: Boolean,
    rfidCodeList: MutableMap<Int, String>,
    rowCount: Int
) {
    val normalized = value.uppercase()
    if (applyToAll) {
        for (i in 0 until rowCount) {
            rfidCodeList[i] = normalized
        }
    } else {
        rfidCodeList[index] = normalized
    }
}

private fun ensureBarcodeTargetRow(
    viewModel: BulkViewModel,
    rfidCodeList: Map<Int, String>
) {
    if (viewModel.lastClickedIndex != null) return
    val tagCount = viewModel.scannedTags.value.size
    val fallbackIndex = (0 until tagCount).firstOrNull { rfidCodeList[it].isNullOrBlank() } ?: 0
    viewModel.setLastClickedIndex(fallbackIndex)
}

private fun registerBulkBarcodeHandler(
    viewModel: BulkViewModel,
    rfidCodeList: MutableMap<Int, String>,
    applyRfidToAll: () -> Boolean,
    rowCount: () -> Int
) {
    viewModel.barcodeReader.setOnBarcodeScanned { scanned ->
        if (scanned.isBlank()) {
            Log.e("SCAN", "Empty barcode — retrying")
            viewModel.retryBarcodeScan()
            return@setOnBarcodeScanned
        }

        var targetIndex = viewModel.lastClickedIndex
        if (targetIndex == null) {
            ensureBarcodeTargetRow(viewModel, rfidCodeList)
            targetIndex = viewModel.lastClickedIndex
        }

        applyRfidToRows(
            index = targetIndex ?: 0,
            value = scanned,
            applyToAll = applyRfidToAll(),
            rfidCodeList = rfidCodeList,
            rowCount = rowCount()
        )
    }
}

fun BulkViewModel.retryBarcodeScan() {
    viewModelScope.launch {
        delay(300)
        try {
            barcodeReader.close()
            barcodeReader.openIfNeeded()
            barcodeReader.startDecode()
            Log.d("BarcodeReader", "Reader reinitialized and restarted")
        } catch (e: Exception) {
            Log.e("BarcodeReader", "Failed to reinitialize scanner: ${e.message}")
        }
    }
}


// --------------------------------------------
// Dropdown Composable
// --------------------------------------------

@Composable
fun FilterDropdown(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    onAddOption: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clickable { expanded = true },
            border = BorderStroke(1.dp, BackgroundGradient),
            shape = RoundedCornerShape(6.dp),
            color = Color.Transparent
        ) {

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = if (selectedOption.isEmpty()) label else selectedOption,
                    fontSize = 12.sp,
                    fontFamily = poppins,
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = "Dropdown arrow",
                    tint = Color.LightGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontFamily = poppins) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }

            HorizontalDivider()

            DropdownMenuItem(
                text = { Text("➕ Add New", fontFamily = poppins) },
                onClick = {
                    expanded = false
                    onAddOption()
                }
            )
        }
    }
}

