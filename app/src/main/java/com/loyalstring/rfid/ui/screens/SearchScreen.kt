package com.loyalstring.rfid.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.loyalstring.rfid.MainActivity
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.local.entity.SearchItem
import com.loyalstring.rfid.data.reader.ScanKeyListener
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.SearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    navController: NavHostController,
    listKey: String? = "unmatchedItems"
) {
    val searchViewModel: SearchViewModel = hiltViewModel()
    val context = LocalContext.current
    val activity = context.findActivity() as? MainActivity
    val lifecycleOwner = LocalLifecycleOwner.current

    var isScanning by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var allDbItems by remember { mutableStateOf<List<BulkItem>>(emptyList()) }
    var filteredDbItems by remember { mutableStateOf<List<BulkItem>>(emptyList()) }

    val selectedPower by remember {
        mutableStateOf(
            UserPreferences.getInstance(context).getInt(UserPreferences.KEY_SEARCH_COUNT)
        )
    }

    // ✅ Explicit unmatched flag
    val isUnmatchedList = listKey == "unmatchedItems"

    val inputItems = remember(isUnmatchedList) {
        if (isUnmatchedList) {
            navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<ArrayList<BulkItem>>("unmatchedItems")
                ?: arrayListOf()
        } else {
            arrayListOf()
        }
    }

    Log.d("SEARCH_SCREEN", "Mode: ${if (isUnmatchedList) "UNMATCHED" else "NORMAL"} | Items: ${inputItems.size}")

    // ✅ Only read unmatched items if listKey == "unmatchedItems"



    BackHandler { onBack() }

    // ✅ Load DB items once
    LaunchedEffect(Unit) {
        allDbItems = withContext(Dispatchers.IO) {
            searchViewModel.getAllBulkItemsFromDb()
        }
    }

    // ✅ For unmatched — start search immediately
    LaunchedEffect(isUnmatchedList, inputItems) {
        if (isUnmatchedList && inputItems.isNotEmpty()) {
            searchViewModel.startSearch(inputItems, selectedPower)
        } else {
            searchViewModel.clearSearchItems()
        }
    }

    val searchItems = searchViewModel.searchItems

    // ✅ Update filtered list when query changes (normal mode only)
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotBlank() && !isUnmatchedList) {
            filteredDbItems = allDbItems.filter {
                it.rfid?.contains(searchQuery, true) == true ||
                        it.itemCode?.contains(searchQuery, true) == true
            }
        } else {
            filteredDbItems = emptyList()
        }
    }

    // ✅ Which list to display
    val filteredItems by remember(searchItems, filteredDbItems, isScanning, isUnmatchedList, searchQuery) {
        derivedStateOf {
            when {
                isUnmatchedList -> searchItems
                isScanning -> searchItems
                searchQuery.isNotBlank() -> filteredDbItems.map { it.toSearchItem() }
                else -> emptyList()
            }
        }
    }

    // ✅ RFID key listener
    DisposableEffect(lifecycleOwner, activity) {
        val listener = object : ScanKeyListener {
            override fun onBarcodeKeyPressed() {}
            override fun onRfidKeyPressed() {
                if (isScanning) {
                    searchViewModel.stopSearch()
                    isScanning = false
                    Log.d("SEARCH", "RFID STOPPED")
                } else {
                    val itemsToSearch = when {
                        isUnmatchedList && inputItems.isNotEmpty() -> inputItems
                        !isUnmatchedList && filteredDbItems.isNotEmpty() -> filteredDbItems
                        else -> emptyList()
                    }

                    if (itemsToSearch.isNotEmpty()) {
                        searchViewModel.startSearch(itemsToSearch, selectedPower)
                        isScanning = true
                        Log.d("SEARCH", "RFID STARTED scanning ${itemsToSearch.size} items")
                    } else {
                        Log.d("SEARCH", "⚠️ No items to scan (maybe type a query?)")
                    }
                }
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> activity?.registerScanKeyListener(listener)
                Lifecycle.Event.ON_PAUSE -> {
                    activity?.unregisterScanKeyListener()
                    if (isScanning) {
                        searchViewModel.stopSearch()
                        isScanning = false
                    }
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            activity?.unregisterScanKeyListener()
            if (isScanning) {
                searchViewModel.stopSearch()
                isScanning = false
            }
        }
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = if (isUnmatchedList) "Search (Unmatched)" else "Search (All Items)",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                showCounter = false
            )
        },
        bottomBar = {
            ScanBottomBar(
                onSave = {},
                onList = {},
                onScan = {},
                onGscan = {
                    if (!isScanning) {
                        val itemsToSearch = when {
                            isUnmatchedList && inputItems.isNotEmpty() -> inputItems
                            !isUnmatchedList && filteredDbItems.isNotEmpty() -> filteredDbItems
                            else -> emptyList()
                        }

                        if (itemsToSearch.isNotEmpty()) {
                            searchViewModel.startSearch(itemsToSearch, selectedPower)
                            isScanning = true
                            Log.d("SEARCH", "Manual SCAN started (${itemsToSearch.size}) items")
                        } else {
                            Log.d("SEARCH", "⚠️ No items to scan")
                        }
                    } else {
                        searchViewModel.stopSearch()
                        isScanning = false
                        Log.d("SEARCH", "Manual SCAN stopped")
                    }
                },
                onReset = {
                    searchQuery = ""
                    filteredDbItems = emptyList()
                    searchViewModel.stopSearch()
                    isScanning = false
                },
                isScanning = isScanning,
                isEditMode = false
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    if (isScanning) {
                        searchViewModel.stopSearch()
                        isScanning = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                label = { Text("Enter RFID / Itemcode", fontFamily = poppins) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search)
            )

            if (filteredItems.isEmpty() && !isScanning) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isUnmatchedList)
                            "Scanning unmatched items..."
                        else
                            "Type RFID / Itemcode to search specific items",
                        color = Color.Gray,
                        fontFamily = poppins
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item { HeaderRow() }
                    itemsIndexed(filteredItems, key = { index, item -> "${item.epc}-$index" }) { index, item ->
                        SearchItemRow(index, item)
                    }
                }
            }
        }
    }
}



@Composable
fun HeaderRow() {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF3B363E))
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf("Sr No", "RFID", "Itemcode", "Progress", "Percent").forEach {
            Text(it, color = Color.White, modifier = Modifier.weight(1f), fontFamily = poppins, fontSize = 12.sp)
        }
    }
}

@Composable
fun SearchItemRow(index: Int, item: SearchItem) {
    val percent = item.proximityPercent.toFloat()
    val progressColor = getColorByPercentage(percent.toInt())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Text("${index + 1}", modifier = Modifier.weight(1f), fontSize = 12.sp)
        Text(item.rfid, modifier = Modifier.weight(1f), fontSize = 12.sp)
        Text(item.itemCode, modifier = Modifier.weight(1f), fontSize = 12.sp)

        Box(modifier = Modifier.weight(2f)) {
            LinearProgressIndicator(
                progress = { percent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = Color.LightGray
            )
        }

        Text("${percent.toInt()}%", modifier = Modifier.weight(1f), fontSize = 12.sp)
    }
}

fun BulkItem.toSearchItem(): SearchItem = SearchItem(
    epc = this.epc ?: this.rfid ?: "",
    itemCode = this.itemCode ?: "",
    rfid = this.rfid ?: "",
    productName = this.productName ?: "",
    proximityPercent = 0
)

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun getColorByPercentage(percent: Int): Color = when {
    percent <= 25 -> Color.Red
    percent <= 50 -> Color.Yellow
    percent <= 75 -> Color(0xFF2196F3)
    else -> Color(0xFF4CAF50)
}
