package com.loyalstring.rfid.ui.screens

import android.annotation.SuppressLint
import android.util.Log
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color

import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope

import androidx.navigation.NavHostController
import com.loyalstring.rfid.MainActivity

import com.loyalstring.rfid.data.reader.ScanKeyListener
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.navigation.Screens
import com.loyalstring.rfid.ui.utils.*
import com.loyalstring.rfid.viewmodel.BulkViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@SuppressLint("RestrictedApi", "UnrememberedMutableState")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkProductScreen(
    onBack: () -> Unit,
    navController: NavHostController
) {

    val viewModel: BulkViewModel = hiltViewModel()
    val context = LocalContext.current

    // Observers
    val tags by viewModel.scannedTags.collectAsState()
    val rfidMap by viewModel.rfidMap.collectAsState()


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

    var selectedPower by remember {
        mutableStateOf(
            UserPreferences.getInstance(context).getInt(UserPreferences.KEY_PRODUCT_COUNT)
        )
    }

    // For each row, maintain its own itemCode
    val itemCodeList = remember { mutableStateMapOf<Int, String>() }

    var isScanning by remember { mutableStateOf(false) }
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
        }
    }

   /* LaunchedEffect(Unit) {
        viewModel.barcodeReader.setOnBarcodeScanned { scanned ->
            val bulk = viewModel.isBulkMode.value
            if (bulk) {
                viewModel.setRfidForAllTags(scanned)
            } else {
                val targetIndex = viewModel.lastClickedIndex
                Log.d("SCAN", "Scanned = $scanned | targetIndex = $targetIndex")
                if (targetIndex != null) {
                    viewModel.updateRfidForIndex(targetIndex, scanned)
                    //viewModel.setLastClickedIndex(null)
                } else {
                    Log.e("SCAN", "❌ No active field when scanned = $scanned")
                }
            }
        }
    }*/

    LaunchedEffect(Unit) {
        viewModel.barcodeReader.setOnBarcodeScanned { scanned ->
            if (scanned.isNullOrBlank()) {
                // ❌ Scan failed or returned empty
                Log.e("SCAN", "❌ Empty or failed scan, retrying...")
                viewModel.retryBarcodeScan()
                return@setOnBarcodeScanned
            }

            val bulk = viewModel.isBulkMode.value
            if (bulk) {
                viewModel.setRfidForAllTags(scanned)
            } else {
                val targetIndex = viewModel.lastClickedIndex
                Log.d("SCAN", "Scanned = $scanned | targetIndex = $targetIndex")
                if (targetIndex != null) {
                    viewModel.updateRfidForIndex(targetIndex, scanned)
                } else {
                    Log.e("SCAN", "❌ No active field when scanned = $scanned — reinitializing reader")
                    viewModel.retryBarcodeScan()
                }
            }
        }
    }


    Scaffold(
        topBar = {
            GradientTopBar(
                title = "Add Bulk Products",
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
                actions = {},
                showCounter = true,
                selectedCount = selectedPower,
                onCountSelected = { selectedPower = it }
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

                    tags.forEachIndexed { index, _ ->
                        val itemCode = itemCodeList[index] ?: ""
                        if (itemCode.isNotBlank()) {

                            viewModel.saveBulkItems(
                                selectedCategory,
                                itemCode,
                                selectedProduct,
                                selectedDesign,
                                tags,
                                index
                            )
                        }
                    }

                    ToastUtils.showToast(context, "Items saved successfully")
                    viewModel.resetScanResults()
                    navController.navigate(Screens.ProductManagementScreen.route)
                },

                onList = { navController.navigate(Screens.ProductListScreen.route) },

                onScan = {
                    viewModel.setBulkMode(false)
                    val activeIndex = viewModel.lastClickedIndex
                    Log.d("UI", "Scan button pressed → lastClickedIndex = $activeIndex")
                    viewModel.startSingleScan(22)
                },

                onGscan = {
                    viewModel.setBulkMode(true)
                    if (isScanning) {
                        viewModel.stopScanning()
                        isScanning = false
                    } else {
                        viewModel.startScanning(selectedPower)
                        isScanning = true
                    }
                },

                onReset = {
                    viewModel.resetProductScanResults()
                    viewModel.stopBarcodeScanner()
                    itemCodeList.clear()
                },

                isScanning = isScanning,
                isEditMode = false,
                isScreen = false
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

                Box(Modifier.width(50.dp), contentAlignment = Alignment.Center) {
                    Text("Sr No.", color = Color.White, fontSize = 13.sp, fontFamily = poppins)
                }

                Box(Modifier.width(150.dp), contentAlignment = Alignment.Center) {
                    Text("Item Code", color = Color.White, fontSize = 13.sp, fontFamily = poppins)
                }

                Box(Modifier.width(150.dp), contentAlignment = Alignment.Center) {
                    Text("RFID Code", color = Color.White, fontSize = 13.sp, fontFamily = poppins)
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
                    key = { index, item -> index to (rfidMap[index] ?: "") }   // 🔥 FIXED — forces correct recomposition
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
                                    itemCodeList[index] = newValue
                                    if (viewModel.isBulkMode.value) {
                                        tags.forEachIndexed { i, _ -> itemCodeList[i] = newValue }
                                    }
                                },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = Color.DarkGray),
                                modifier = Modifier
                                    .fillMaxWidth()
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
                        val focusRequester = remember { FocusRequester() }

                        Box(
                            modifier = Modifier
                                .width(150.dp)
                                .height(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicTextField(
                                value = rfidMap[index] ?: "",
                                onValueChange = { newRFID -> viewModel.updateRfidForIndex(index, newRFID) },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, color = Color.DarkGray),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                                    .focusable(true)
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            Log.d("UI", "Focused index = $index ✅")
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

private fun BulkViewModel.retryBarcodeScan() {

        viewModelScope.launch {
            delay(300) // short pause before re-initialization
            try {
                barcodeReader.close()        // ensure clean state
                barcodeReader.openIfNeeded() // reopen the device
                barcodeReader.startDecode()  // start a fresh scan
                Log.d("BarcodeReader", "🔁 Reader reinitialized and restarted")
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

