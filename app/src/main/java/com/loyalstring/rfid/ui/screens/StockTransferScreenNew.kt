package com.loyalstring.rfid.ui.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import com.loyalstring.rfid.MainActivity
import com.loyalstring.rfid.data.reader.ScanKeyListener
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.remote.resource.Resource
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.navigation.Screens
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.viewmodel.BulkViewModel
import com.loyalstring.rfid.viewmodel.SingleProductViewModel
import com.loyalstring.rfid.viewmodel.StockTransferViewModel
import com.loyalstring.rfid.viewmodel.UserPermissionViewModel
import com.loyalstring.rfid.worker.LocaleHelper
import kotlin.text.isNotBlank

private const val COL_SR = 1f
private const val COL_PNAME = 2.4f
private const val COL_LABEL = 1.5f
private const val COL_GWT = 1.2f
private const val COL_NWT = 1.2f
private const val COL_ACTION = 1.2f

private fun stockTransferItemKey(item: BulkItem): String =
    item.itemCode?.takeIf { it.isNotBlank() }
        ?: item.rfid?.takeIf { it.isNotBlank() }
        ?: item.bulkItemId.takeIf { it > 0 }?.toString()
        ?: ""

@Composable
fun StockTransferScreenNew(
    onBack: () -> Unit,
    navController: NavHostController
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val bulkViewModel: BulkViewModel = hiltViewModel()
    var isScanning by remember { mutableStateOf(false) }
    var isBulkScanning by remember { mutableStateOf(false) }


    var selectedPower by remember { mutableIntStateOf(10) }
    LaunchedEffect(Unit) {
        selectedPower = UserPreferences.getInstance(context).getInt(
            UserPreferences.KEY_STOCK_TRANSFER_COUNT,
            10
        )
    }

    val activity = context as? MainActivity
    DisposableEffect(Unit) {
        val listener = object : ScanKeyListener {
            override fun onBarcodeKeyPressed() {
                bulkViewModel.startBarcodeScanning(context)
            }

            override fun onRfidKeyPressed() {
                if (isBulkScanning) {
                    bulkViewModel.stopScanning()
                    isBulkScanning = false
                } else {
                    bulkViewModel.stopScanning()
                    bulkViewModel.startScanning(selectedPower)
                    isBulkScanning = true
                    isScanning = false
                }
            }
        }
        activity?.registerScanKeyListener(listener)

        onDispose {
            activity?.unregisterScanKeyListener()
            bulkViewModel.stopScanning()
            isBulkScanning = false
        }
    }


    val userPreferences = UserPreferences.getInstance(context)
    val savedLang = userPreferences.getAppLanguage().ifBlank { "en" }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = currentLocales[0]?.language ?: savedLang
    val localizedContext = LocaleHelper.applyLocale(context, currentLang)

    val parentEntry = remember(navController) {
        navController.getBackStackEntry("main_graph")
    }
    val scannedTags by bulkViewModel.scannedTags.collectAsState()

    var itemCode by remember { mutableStateOf(TextFieldValue("")) }
    var showDropdownItemcode by remember { mutableStateOf(false) }
    val viewModel: StockTransferViewModel = hiltViewModel(parentEntry)
    val singleProductViewModel: SingleProductViewModel = hiltViewModel()
    val userPermissionViewModel: UserPermissionViewModel = hiltViewModel()

    val transferTypes by viewModel.transferTypes.collectAsState()
    val filteredStockItems by viewModel.filteredBulkItems.collectAsState()
    val counterNames by viewModel.counterNames.collectAsState()

    val counters by remember { derivedStateOf { singleProductViewModel.counters } }
    val boxes by remember { derivedStateOf { singleProductViewModel.boxes } }
    val packets by remember { derivedStateOf { singleProductViewModel.packets } }
    val branches by remember { derivedStateOf { singleProductViewModel.branches } }

    var selectedTransferType by remember { mutableStateOf("Transfer Type") }
    var selectedFrom by remember { mutableStateOf("From") }
    var selectedTo by remember { mutableStateOf("To") }

    var showTransferDialog by remember { mutableStateOf(false) }
    var showFromDialog by remember { mutableStateOf(false) }
    var showToDialog by remember { mutableStateOf(false) }

    val checkedKeys = remember { mutableStateListOf<String>() }
    val accessibleBranches = remember { mutableStateListOf<String>() }

    val employee = remember {
        UserPreferences.getInstance(context).getEmployee(Employee::class.java)
    }

    var showRequestPopup by remember { mutableStateOf(false) }

    var showFilterDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showProductDialog by remember { mutableStateOf(false) }
    var showDesignDialog by remember { mutableStateOf(false) }

// draft values shown inside popup
    var draftCategory by remember { mutableStateOf("Category") }
    var draftProduct by remember { mutableStateOf("Product") }
    var draftDesign by remember { mutableStateOf("Design") }

// applied values used for list filtering
    var appliedCategory by remember { mutableStateOf<String?>(null) }
    var appliedProduct by remember { mutableStateOf<String?>(null) }
    var appliedDesign by remember { mutableStateOf<String?>(null) }

    val categoryResponse by singleProductViewModel.categoryResponse.observeAsState()
    val productResponse by singleProductViewModel.productResponse.observeAsState()
    val designResponse by singleProductViewModel.designResponse.observeAsState()

    val categoryList = when (val result = categoryResponse) {
        is Resource.Success -> result.data ?: emptyList()
        is Resource.Loading -> emptyList()
        is Resource.Error -> emptyList()
        null -> emptyList()
    }

    val productList = when (val result = productResponse) {
        is Resource.Success -> result.data ?: emptyList()
        is Resource.Loading -> emptyList()
        is Resource.Error -> emptyList()
        null -> emptyList()
    }

    val designList = when (val result = designResponse) {
        is Resource.Success -> result.data ?: emptyList()
        is Resource.Loading -> emptyList()
        is Resource.Error -> emptyList()
        null -> emptyList()
    }



    LaunchedEffect(Unit) {
        viewModel.clearTransferStatus()
        viewModel.clearApproveResult()
        employee?.clientCode?.let { clientCode ->
            viewModel.loadTransferTypes(ClientCodeRequest(clientCode))
            viewModel.fetchCounterNames()
            viewModel.fetchBoxNames()
            viewModel.fetchBranchNames()

            singleProductViewModel.fetchAllStockTransferData(ClientCodeRequest(clientCode))
            viewModel.loadAllLabelledStock()

            singleProductViewModel.getAllCategory(ClientCodeRequest(clientCode))
            singleProductViewModel.getAllProduct(ClientCodeRequest(clientCode))
            singleProductViewModel.getAllDesign(ClientCodeRequest(clientCode))
        }
    }

    LaunchedEffect(branches, selectedTransferType) {
        if (viewModel.getTransferTypeId(selectedTransferType) != 15) return@LaunchedEffect

        val branchId = UserPreferences.getInstance(context)
            .getBranchID()
            ?.toInt()

        if (branchId != null && branches.isNotEmpty()) {
            val matchedBranch = branches.firstOrNull { it.Id == branchId }
            matchedBranch?.let {
                selectedFrom = it.BranchName
            }
        }
    }

    LaunchedEffect(selectedTransferType) {
        if (viewModel.getTransferTypeId(selectedTransferType) == 15) {
            val branchNames = userPermissionViewModel.getAccessibleBranches()
            accessibleBranches.clear()
            accessibleBranches.addAll(branchNames)
            Log.d("BRANCH_DEBUG", "accessibleBranches = $accessibleBranches")
        }
    }

    LaunchedEffect(scannedTags) {
        scannedTags.forEach { scannedTid ->
            val scannedEpc = scannedTid.getEPC()
                ?.trim()
                ?.uppercase()
                ?.replace(" ", "")
                ?: ""

            if (scannedEpc.isNotEmpty()) {
                val matchedItem = filteredStockItems.firstOrNull { item ->
                    val itemEpc = item.epc
                        ?.trim()
                        ?.uppercase()
                        ?.replace(" ", "")
                        ?: ""
                    val itemRfid = item.rfid
                        ?.trim()
                        ?.uppercase()
                        ?.replace(" ", "")
                        ?: ""
                    itemEpc == scannedEpc || itemRfid == scannedEpc
                }

                matchedItem?.let { item ->
                    val key = stockTransferItemKey(item)
                    if (key.isNotEmpty() && !checkedKeys.contains(key)) {
                        checkedKeys.add(key)
                    }
                }
            }
        }
    }

    LaunchedEffect(bulkViewModel.rfidInput.value) {
        val code = bulkViewModel.rfidInput.value.trim()
        if (code.isNotBlank()) {
            itemCode = androidx.compose.ui.text.input.TextFieldValue(code)
            bulkViewModel.rfidInput.value = ""
        }
    }

    val (fromType, toType) = remember(selectedTransferType) {
        selectedTransferType
            .split(" to ", ignoreCase = true)
            .map { it.trim().lowercase() }
            .let { it.getOrNull(0) to it.getOrNull(1) }
    }

    val transferTypeId = viewModel.getTransferTypeId(selectedTransferType)

    val counterOptions = remember(counters, counterNames) {
        val apiCounterNames = counters.mapNotNull { it.CounterName.takeIf { name -> name.isNotBlank() } }
        if (apiCounterNames.isNotEmpty()) {
            apiCounterNames.distinct()
        } else {
            counterNames.filter { it.isNotBlank() }.distinct()
        }
    }

    val fromOptions = remember(
        transferTypeId,
        fromType,
        accessibleBranches.toList(),
        counterOptions,
        boxes,
        packets,
        branches
    ) {
        when {
            transferTypeId == 15 && fromType == "branch" -> accessibleBranches.toList()
            fromType == "counter" -> counterOptions
            fromType == "box" -> boxes.map { it.BoxName }
            fromType == "packet" -> packets.map { it.PacketName }
            fromType == "branch" -> branches.map { it.BranchName }
            else -> emptyList()
        }.filter { !it.isNullOrBlank() }
    }

    val toOptions = remember(
        transferTypeId,
        toType,
        selectedFrom,
        accessibleBranches.toList(),
        counterOptions,
        boxes,
        packets,
        branches
    ) {
        when {
            transferTypeId == 15 && toType == "branch" -> accessibleBranches.toList()
            toType == "counter" -> counterOptions
            toType == "box" -> boxes.map { it.BoxName }
            toType == "packet" -> packets.map { it.PacketName }
            toType == "branch" -> branches.map { it.BranchName }
            else -> emptyList()
        }
            .filter { !it.isNullOrBlank() }
            .filter { it != selectedFrom || fromType != toType }
    }

    LaunchedEffect(selectedFrom, selectedTransferType) {
        if (selectedFrom != "From" && !fromType.isNullOrBlank()) {
            viewModel.filterBulkItemsByFrom(fromType, selectedFrom)
        } else {
            viewModel.loadAllLabelledStock()
        }
        checkedKeys.clear()
    }

    val availableItems = remember(filteredStockItems) { filteredStockItems }

    val searchQuery = itemCode.text.trim()

    val displayItems = remember(
        availableItems,
        appliedCategory,
        appliedProduct,
        appliedDesign,
        searchQuery
    ) {
        availableItems.filter { item ->
            val categoryMatch =
                appliedCategory == null ||
                        item.category.equals(appliedCategory, ignoreCase = true)

            val productMatch =
                appliedProduct == null ||
                        item.productName.equals(appliedProduct, ignoreCase = true)

            val designMatch =
                appliedDesign == null ||
                        item.design.equals(appliedDesign, ignoreCase = true)

            val searchMatch = searchQuery.isEmpty() ||
                    item.itemCode.orEmpty().contains(searchQuery, ignoreCase = true) ||
                    item.rfid.orEmpty().contains(searchQuery, ignoreCase = true) ||
                    item.epc.orEmpty().contains(searchQuery, ignoreCase = true)

            categoryMatch && productMatch && designMatch && searchMatch
        }
    }

    LaunchedEffect(itemCode.text) {
        val query = itemCode.text.trim().uppercase().replace(" ", "")
        if (query.isNotBlank()) {
            val matchedItem = filteredStockItems.firstOrNull { item ->
                val itemCodeClean = item.itemCode.orEmpty().trim().uppercase().replace(" ", "")
                val itemEpcClean = item.epc.orEmpty().trim().uppercase().replace(" ", "")
                val itemRfidClean = item.rfid.orEmpty().trim().uppercase().replace(" ", "")

                itemCodeClean == query || itemEpcClean == query || itemRfidClean == query
            }

            matchedItem?.let { item ->
                val key = stockTransferItemKey(item)
                if (key.isNotEmpty() && !checkedKeys.contains(key)) {
                    checkedKeys.add(key)
                }
            }
        }
    }

    val selectedItems = remember(displayItems, checkedKeys.toList()) {
        displayItems.filter { item ->
            checkedKeys.contains(stockTransferItemKey(item))
        }
    }

    val totalQty = displayItems.size
    val selectedQty = selectedItems.size
    val selectedGrossWeight = selectedItems.sumOf { it.grossWeight?.toDoubleOrNull() ?: 0.0 }
    val selectedNetWeight = selectedItems.sumOf { it.netWeight?.toDoubleOrNull() ?: 0.0 }
    val selectAllChecked = displayItems.isNotEmpty() &&
            displayItems.all { item ->
                checkedKeys.contains(stockTransferItemKey(item))
            }

  /*  Scaffold(
        topBar = {
            GradientTopBar(
                title = "Stock Transfer",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                selectedCount = 0,
                titleTextSize = 20.sp
            )
        },
        bottomBar = {
            ScanBottomBar(
                onSave = {
                    bulkViewModel.barcodeReader.close()
                    if (selectedItems.isNotEmpty()) {
                        viewModel.setTransferPreviewItems(selectedItems)
                        viewModel.setPendingLocationNames(
                            from = if (selectedFrom == "From") "" else selectedFrom,
                            to = if (selectedTo == "To") "" else selectedTo
                        )
                        Log.d("@@", "selectedItems = $selectedItems")
                        navController.navigate(Screens.StockTransferPreviewScreen.route)
                    }
                },
                onList = { navController.navigate(Screens.ProductListScreen.route) },
                onScan = {

                        bulkViewModel.startSingleScan(20)



                },
                onGscan = {
                    if (isBulkScanning) {
                        bulkViewModel.stopScanning()
                        isBulkScanning = false
                    } else {
                        bulkViewModel.stopScanning()
                        bulkViewModel.startScanning(selectedPower)
                        isBulkScanning = true
                        isScanning = false
                    }
                },
                onReset = {
                    try {
                        bulkViewModel.stopBarcodeScanner()
                        bulkViewModel.resetProductScanResults()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                isScanning = isScanning,
                isEditMode = false,
                isScreen = true,
                isBulkScanning= isBulkScanning
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(vertical = 8.dp)
        ) {

            // Same horizontal alignment for filter + itemcode row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                TransferFilter(
                    selectedTransferType = selectedTransferType,
                    selectedFrom = selectedFrom,
                    selectedTo = selectedTo,
                    onTransferClick = { showTransferDialog = true },
                    onFromClick = { showFromDialog = true },
                    onToClick = { showToDialog = true }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        StockTransferItemCode(
                            itemCode = itemCode,
                            onItemCodeChange = { itemCode = it },
                            showDropdown = showDropdownItemcode,
                            setShowDropdown = { showDropdownItemcode = it },
                            context = context,
                            onScanClicked = {
                                bulkViewModel.startBarcodeScanning(context)
                            },
                            onClearClicked = { itemCode = TextFieldValue("") },
                            filteredList = filteredStockItems,
                            isLoading = false,
                            onItemSelected = { item ->
                                val code = item.itemCode.orEmpty()
                                itemCode = TextFieldValue(code)

                                val key = stockTransferItemKey(item)
                                if (!checkedKeys.contains(key)) {
                                    checkedKeys.add(key)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Icon(
                        painter = painterResource(id = R.drawable.filter_gary),
                        contentDescription = "Filter",
                        modifier = Modifier
                            .size(24.dp)
                            .clickable {
                                showFilterDialog = true
                            },
                        tint = Color.Gray
                    )
                }
            }

            StockTransferHeader(
                actionTitle = "",
                selectAllChecked = selectAllChecked,
                onSelectAllChange = { checked ->
                    checkedKeys.clear()
                    if (checked) {
                        displayItems.forEach { item ->
                            val key = stockTransferItemKey(item)
                            checkedKeys.add(key)
                        }
                    }
                }
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 120.dp),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                itemsIndexed(
                    items = displayItems,
                    key = { index, item -> stockTransferItemKey(item).ifBlank { index.toString() } }
                ) { index, item ->

                    val rowKey = stockTransferItemKey(item).ifBlank { index.toString() }

                    StockTransferRow(
                        sr = index + 1,
                        productName = item.productName ?: "",
                        label = item.itemCode.orEmpty(),
                        grossWt = item.grossWeight ?: "0",
                        netWt = item.netWeight ?: "0",
                        checked = checkedKeys.contains(rowKey),
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (!checkedKeys.contains(rowKey)) {
                                    checkedKeys.add(rowKey)
                                }
                            } else {
                                checkedKeys.remove(rowKey)
                            }
                        }
                    )
                }
            }

            StockTransferBottomBar(
                totalQty = totalQty,
                selectedQty = selectedQty,
                totalGrossWeight = selectedGrossWeight,
                totalNetWeight = selectedNetWeight
            )
        }
    }*/

    Scaffold(
        topBar = {
            GradientTopBar(
                title = localizedContext.getString(R.string.stock_transfer_title),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                showCounter = true,
                selectedCount = selectedPower,
                onCountSelected = {
                    selectedPower = it
                    UserPreferences.getInstance(context).saveInt(
                        UserPreferences.KEY_STOCK_TRANSFER_COUNT,
                        it
                    )
                },
                titleTextSize = 20.sp
            )
        },
        bottomBar = {
            ScanBottomBar(
                onSave = {
                    bulkViewModel.barcodeReader.close()
                    if (selectedFrom == "From" || selectedFrom.isBlank()) {
                        Toast.makeText(context, localizedContext.getString(R.string.select_from_error), Toast.LENGTH_SHORT).show()
                        return@ScanBottomBar
                    }
                    if (selectedTo == "To" || selectedTo.isBlank()) {
                        Toast.makeText(context, localizedContext.getString(R.string.select_to_error), Toast.LENGTH_SHORT).show()
                        return@ScanBottomBar
                    }
                    if (selectedItems.isNotEmpty()) {
                        viewModel.setTransferPreviewItems(selectedItems)
                        viewModel.setPendingLocationNames(
                            from = selectedFrom,
                            to = selectedTo
                        )
                        Log.d("@@", "selectedItems = $selectedItems")
                        navController.navigate(Screens.StockTransferPreviewScreen.route)
                    }
                },
                onList = { showRequestPopup = true },
                onScan = {
                    bulkViewModel.startSingleScan(20)
                },
                onGscan = {
                    if (isBulkScanning) {
                        bulkViewModel.stopScanning()
                        isBulkScanning = false
                    } else {
                        bulkViewModel.stopScanning()
                        bulkViewModel.startScanning(selectedPower)
                        isBulkScanning = true
                        isScanning = false
                    }
                },
                onReset = {
                    try {
                        bulkViewModel.stopBarcodeScanner()
                        bulkViewModel.resetProductScanResults()
                        checkedKeys.clear()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                isScanning = isScanning,
                isEditMode = false,
                isScreen = true,
                isBulkScanning = isBulkScanning
            )
        }
    ) { padding ->

        // ✅ GET LOADER STATE
        val isScreenLoading by viewModel.isLoading.collectAsState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // ✅ MAIN UI (UNCHANGED)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp)
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {

                    TransferFilter(
                        selectedTransferType = selectedTransferType,
                        selectedFrom = selectedFrom,
                        selectedTo = selectedTo,
                        onTransferClick = { showTransferDialog = true },
                        onFromClick = { showFromDialog = true },
                        onToClick = { showToDialog = true }
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Box(modifier = Modifier.weight(1f)) {

                            StockTransferItemCode(
                                itemCode = itemCode,
                                onItemCodeChange = { itemCode = it },
                                showDropdown = showDropdownItemcode,
                                setShowDropdown = { showDropdownItemcode = it },
                                context = context,
                                onScanClicked = {
                                    bulkViewModel.startBarcodeScanning(context)
                                },
                                onClearClicked = { itemCode = TextFieldValue("") },
                                filteredList = filteredStockItems,
                                isLoading = false,
                                onItemSelected = { item ->
                                    val code = item.itemCode.orEmpty()
                                    itemCode = TextFieldValue(code)

                                    val key = stockTransferItemKey(item)
                                    if (!checkedKeys.contains(key)) {
                                        checkedKeys.add(key)
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Icon(
                            painter = painterResource(id = R.drawable.filter_gary),
                            contentDescription = "Filter",
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { showFilterDialog = true },
                            tint = Color.Gray
                        )
                    }
                }

                StockTransferHeader(
                    localizedContext=localizedContext,
                    actionTitle = "",
                    selectAllChecked = selectAllChecked,
                    onSelectAllChange = { checked ->
                        checkedKeys.clear()
                        if (checked) {
                            displayItems.forEach { item ->
                                val key = stockTransferItemKey(item)
                                checkedKeys.add(key)
                            }
                        }
                    }
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 120.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    itemsIndexed(
                        items = displayItems,
                        key = { index, item -> stockTransferItemKey(item).ifBlank { index.toString() } }
                    ) { index, item ->

                        val rowKey = stockTransferItemKey(item).ifBlank { index.toString() }

                        StockTransferRow(
                            sr = index + 1,
                            productName = item.productName ?: "",
                            label = item.itemCode.orEmpty(),
                            grossWt = item.grossWeight ?: "0",
                            netWt = item.netWeight ?: "0",
                            checked = checkedKeys.contains(rowKey),
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (!checkedKeys.contains(rowKey)) {
                                        checkedKeys.add(rowKey)
                                    }
                                } else {
                                    checkedKeys.remove(rowKey)
                                }
                            }
                        )
                    }
                }

                StockTransferBottomBar(
                    totalQty = totalQty,
                    selectedQty = selectedQty,
                    totalGrossWeight = selectedGrossWeight,
                    totalNetWeight = selectedNetWeight,
                    localizedContext= localizedContext

                )
            }

            // ✅ LOADER OVERLAY
            if (isScreenLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = localizedContext.getString(R.string.loading_stock),
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }


    if (showTransferDialog) {
        Dialog(onDismissRequest = { showTransferDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp) // fixed height better here
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    DialogHeader(localizedContext.getString(R.string.choose_transfer_type))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = true)
                            .padding(12.dp)
                    ) {
                        items(transferTypes) { type ->
                            DialogItem(type.TransferType ?: "") {
                                selectedTransferType = type.TransferType ?: ""
                                viewModel.onTransferTypeSelected(type.TransferType ?: "")
                                selectedFrom = "From"
                                selectedTo = "To"

                                if (viewModel.getTransferTypeId(selectedTransferType) == 15) {
                                    selectedFrom = employee?.branchName ?: "From"
                                }

                                showTransferDialog = false
                            }
                        }
                    }


                }
            }
        }
    }

    if (showFromDialog) {
        Dialog(onDismissRequest = { showFromDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    DialogHeader(localizedContext.getString(R.string.select_from))

                    LazyColumn {
                        items(fromOptions) { option ->
                            DialogItem(option) {
                                selectedFrom = option
                                showFromDialog = false
                                if (selectedTo == option) selectedTo = "To"
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilterDialog) {
        Dialog(onDismissRequest = { showFilterDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = localizedContext.getString(R.string.filter),
                        fontSize = 18.sp,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FilterField(
                        title = localizedContext.getString(R.string.category),
                        value = draftCategory,
                        onClick = { showCategoryDialog = true }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    FilterField(
                        title = localizedContext.getString(R.string.product),
                        value = draftProduct,
                        onClick = { showProductDialog = true }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    FilterField(
                        title = localizedContext.getString(R.string.lbl_design),
                        value = draftDesign,
                        onClick = { showDesignDialog = true }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                draftCategory = "Category"
                                draftProduct = "Product"
                                draftDesign = "Design"

                                appliedCategory = null
                                appliedProduct = null
                                appliedDesign = null

                                checkedKeys.clear()
                                showFilterDialog = false
                            }
                        ) {
                            Text(localizedContext.getString(R.string.clear))
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                appliedCategory =
                                    if (draftCategory == "Category") null else draftCategory
                                appliedProduct =
                                    if (draftProduct == "Product") null else draftProduct
                                appliedDesign =
                                    if (draftDesign == "Design") null else draftDesign

                                checkedKeys.clear()
                                showFilterDialog = false
                            }
                        ) {
                            Text(localizedContext.getString(R.string.apply))
                        }
                    }
                }
            }
        }
    }

    if (showCategoryDialog) {
        Dialog(onDismissRequest = { showCategoryDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    DialogHeader(localizedContext.getString(R.string.select_category))

                    LazyColumn(modifier = Modifier.padding(12.dp)) {
                        items(categoryList) { item ->
                            DialogItem(item.CategoryName ?: "") {
                                draftCategory = item.CategoryName ?: "Category"
                                draftProduct = "Product"
                                draftDesign = "Design"
                                showCategoryDialog = false
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProductDialog) {
        Dialog(onDismissRequest = { showProductDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    DialogHeader(localizedContext.getString(R.string.select_product))

                    LazyColumn(modifier = Modifier.padding(12.dp)) {
                        items(productList) { item ->
                            DialogItem(item.ProductName ?: "") {
                                draftProduct = item.ProductName ?: "Product"
                                draftDesign = "Design"
                                showProductDialog = false
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRequestPopup) {
        Dialog(onDismissRequest = { showRequestPopup = false }) {

            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {

                    DialogHeader(localizedContext.getString(R.string.stock_requests))

                    Spacer(modifier = Modifier.height(8.dp))

                    // ✅ IN REQUEST
                    DialogItem("In Request") {

                        Log.d("StockTransferPreview", "In Request clicked")

                        showRequestPopup = false

                        navController.navigate(Screens.StockInScreen.route)
                    }

                    // ✅ OUT REQUEST
                    DialogItem("Out Request") {

                        Log.d("StockTransferPreview", "Out Request clicked")

                        showRequestPopup = false

                        navController.navigate(Screens.StockOutScreen.route)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    if (showDesignDialog) {
        Dialog(onDismissRequest = { showDesignDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    DialogHeader(localizedContext.getString(R.string.select_design))

                    LazyColumn(modifier = Modifier.padding(12.dp)) {
                        items(designList) { item ->
                            DialogItem(item.DesignName ?: "") {
                                draftDesign = item.DesignName ?: "Design"
                                showDesignDialog = false
                            }
                        }
                    }
                }
            }
        }
    }


    if (showToDialog) {
        Dialog(onDismissRequest = { showToDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    DialogHeader(localizedContext.getString(R.string.select_to))

                    LazyColumn {
                     /*   items(toOptions) { option ->
                            DialogItem(option) {
                                selectedTo = option
                                showToDialog = false
                            }
                        }*/

                        items(toOptions) { option ->
                            DialogItem(option) {

                                selectedTo = option
                                showToDialog = false

                                val sourceBranchId = branches.firstOrNull {
                                    it.BranchName == selectedFrom
                                }?.Id ?: 0

                                val destinationBranchId = branches.firstOrNull {
                                    it.BranchName == option
                                }?.Id ?: 0

                                viewModel.setTransferBranches(
                                    source = sourceBranchId,
                                    destination = destinationBranchId
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterField(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Column {
        Text(
            text = title,
            fontSize = 13.sp,
            color = Color.DarkGray
        )

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF3F3F3), RoundedCornerShape(10.dp))
                .clickable { onClick() }
                .padding(horizontal = 12.dp, vertical = 14.dp)
        ) {
            Text(
                text = value,
                color = if (value == title) Color.Gray else Color.Black,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun StockTransferHeader(
    localizedContext:Context,
    actionTitle: String,
    selectAllChecked: Boolean,
    onSelectAllChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(35.dp)
            .background(Color(0xFF4A4A4A))
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            localizedContext.getString(R.string.sr),
            modifier = Modifier.weight(COL_SR),
            color = Color.White,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
        Text(
            localizedContext.getString(R.string.product_name),
            modifier = Modifier.weight(COL_PNAME),
            color = Color.White,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
        Text(
            localizedContext.getString(R.string.itemcode),
            modifier = Modifier.weight(COL_LABEL),
            color = Color.White,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
        Text(
            localizedContext.getString(R.string.gross_wt_header),
            modifier = Modifier.weight(COL_GWT),
            color = Color.White,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
        Text(
            localizedContext.getString(R.string.net_wt_header),
            modifier = Modifier.weight(COL_NWT),
            color = Color.White,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )

        Row(
            modifier = Modifier.weight(COL_ACTION),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = actionTitle,
                color = Color.White,
                fontSize = 10.sp
            )
            Checkbox(
                checked = selectAllChecked,
                onCheckedChange = onSelectAllChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = Color.White,
                    uncheckedColor = Color.White,
                    checkmarkColor = Color.Black
                )
            )
        }
    }
}

@Composable
fun StockTransferRow(
    sr: Int,
    productName: String,
    label: String,
    grossWt: String,
    netWt: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp), // fixed row height for proper alignment
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = sr.toString(),
            modifier = Modifier.weight(COL_SR),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        Text(
            text = productName,
            modifier = Modifier
                .weight(COL_PNAME)
                .padding(horizontal = 2.dp),
            fontSize = 10.sp,
            textAlign = TextAlign.Start,
            maxLines = 1, // important
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = label,
            modifier = Modifier
                .weight(COL_LABEL)
                .padding(horizontal = 2.dp),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 1, // important
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = grossWt,
            modifier = Modifier.weight(COL_GWT),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        Text(
            text = netWt,
            modifier = Modifier.weight(COL_NWT),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        Box(
            modifier = Modifier.weight(COL_ACTION),
            contentAlignment = Alignment.Center
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF6750A4),
                    uncheckedColor = Color.Gray,
                    checkmarkColor = Color.White
                )
            )
        }
    }
}

@Composable
fun StockTransferBottomBar(
    totalQty: Int,
    selectedQty: Int,
    totalGrossWeight: Double,
    totalNetWeight: Double,
    localizedContext: Context
  /*  buttonText: String,
    onTransferClick: () -> Unit*/
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFD9D9D9))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text =localizedContext.getString(R.string.t_qty, totalQty),
            fontSize = 11.sp,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = localizedContext.getString(R.string.selected_qty,selectedQty),
            fontSize = 11.sp,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = localizedContext.getString(R.string.t_gross_weight, totalGrossWeight),
            fontSize = 11.sp,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = localizedContext.getString(R.string.t_net_weight, totalNetWeight),
            fontSize = 11.sp,
            modifier = Modifier.weight(1f)
        )

      /*  Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd
        ) {
            Button(
                onClick = onTransferClick,
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(buttonText, fontSize = 11.sp)
            }
        }*/
    }
}
@Composable
fun DialogHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFFB71C1C), Color(0xFF3F51B5))
                )
            )
            .padding(16.dp)
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp
        )
    }
}

@Composable
fun DialogItem(text: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEDEDED))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(12.dp),
            fontSize = 14.sp
        )
    }
}