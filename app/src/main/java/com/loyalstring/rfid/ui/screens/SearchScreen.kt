package com.loyalstring.rfid.ui.screens

import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.loyalstring.rfid.data.reader.RFIDReaderManager
import com.loyalstring.rfid.data.reader.ScanKeyListener
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.SearchViewModel
import com.rscja.deviceapi.RFIDWithUHFUART
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    navController: NavHostController,
    listKey: String? = "unmatchedItems"
) {
    val listState = rememberLazyListState()
    var lastSearchItems by remember { mutableStateOf<List<SearchItem>>(emptyList()) }
    var lastSortedSearchItems by remember { mutableStateOf<List<SearchItem>>(emptyList()) }
    val searchViewModel: SearchViewModel = hiltViewModel()
    val context = LocalContext.current
    val activity = context.findActivity() as? MainActivity
    val lifecycleOwner = LocalLifecycleOwner.current
    val readerManager: RFIDReaderManager
    val coroutineScope = rememberCoroutineScope()

    var isScanning by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var allDbItems by remember { mutableStateOf<List<BulkItem>>(emptyList()) }
    var filteredDbItems by remember { mutableStateOf<List<BulkItem>>(emptyList()) }

    var selectedPower by remember {
        mutableIntStateOf(UserPreferences.getInstance(context).getInt(
            UserPreferences.KEY_SEARCH_COUNT))
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

    // rememberUpdatedState captures the LATEST value of each var inside non-composable
    // callbacks (DisposableEffect lambdas) without stale-closure bugs.
    val latestInputItems by rememberUpdatedState(inputItems)
    val latestSelectedPower by rememberUpdatedState(selectedPower)
    val latestIsScanning by rememberUpdatedState(isScanning)

    Log.d("SEARCH_SCREEN", "Mode: ${if (isUnmatchedList) "UNMATCHED" else "NORMAL"} | Items: ${inputItems.size}")

    BackHandler { onBack() }

    // ✅ Load DB items once
    LaunchedEffect(Unit) {
        allDbItems = withContext(Dispatchers.IO) {
            searchViewModel.getAllBulkItemsFromDb()
        }
    }

    // Initial auto-start on first composition (inputItems available immediately from savedStateHandle)
    LaunchedEffect(isUnmatchedList, inputItems) {
        if (isUnmatchedList && inputItems.isNotEmpty()) {
            delay(150) // brief yield to let compose settle before starting IO work
            if (!isScanning) {
                searchViewModel.startSearch(inputItems, selectedPower)
                isScanning = true
                Log.d("AUTO_SCAN", "Initial auto scan started (${inputItems.size} items)")
            }
        } else if (!isUnmatchedList) {
            searchViewModel.clearSearchItems()
            isScanning = false
        }
    }


    val searchItems = searchViewModel.searchItems
    LaunchedEffect(searchItems) {
        if (searchItems.isNotEmpty()) {
            lastSearchItems = searchItems
        }
    }

    // ✅ Update filtered list when query changes (normal mode only)
    /*   LaunchedEffect(searchQuery) {
           if (searchQuery.isNotBlank() && !isUnmatchedList) {
               filteredDbItems = allDbItems.filter {
                   it.rfid?.contains(searchQuery, true) == true ||
                           it.itemCode?.contains(searchQuery, true) == true
               }
           } else {
               filteredDbItems = emptyList()
           }
       }*/


    LaunchedEffect(searchQuery, allDbItems) {

        if (isUnmatchedList) return@LaunchedEffect

        val query = searchQuery.trim()

        if (query.isEmpty()) {
            filteredDbItems = emptyList()
            if (isScanning) {
                searchViewModel.stopSearch()
                isScanning = false
            }
            return@LaunchedEffect
        }

        // Wait until DB loaded
        if (allDbItems.isEmpty()) return@LaunchedEffect

        // PERF-FIX: Added 300ms debounce so that filtering and scan-restart do not trigger
        // on every single keystroke while the user is still typing. Without this, each
        // character typed called searchViewModel.startSearch() → stopInventory() + startInventory()
        // which caused RFID hardware thrashing and visible UI lag on fast typing.
        delay(300)

        val matched = allDbItems.filter {
            it.rfid?.trim()?.equals(query, true) == true ||
                    it.itemCode?.trim()?.equals(query, true) == true
        }

        filteredDbItems = matched

        if (matched.isNotEmpty()) {
            if (!isScanning) {
                searchViewModel.startSearch(matched, selectedPower)
                isScanning = true
                Log.d("AUTO_SCAN", "Auto scan started for matched item")
            }
        } else {
            if (isScanning) {
                searchViewModel.stopSearch()
                isScanning = false
            }
        }
    }


    /*    LaunchedEffect(searchQuery, allDbItems) {

            if (searchQuery.isBlank()) {
                if (isScanning) {
                    searchViewModel.stopSearch()
                    isScanning = false
                }
                filteredDbItems = emptyList()
                return@LaunchedEffect
            }

            if (!isUnmatchedList) {

                filteredDbItems = allDbItems.filter {
                    it.rfid?.contains(searchQuery, true) == true ||
                            it.itemCode?.contains(searchQuery, true) == true
                }

                if (filteredDbItems.isNotEmpty()) {

                    if (!isScanning) {
                        searchViewModel.startSearch(filteredDbItems, selectedPower)
                        isScanning = true
                        Log.d("AUTO_SCAN", "Auto scan started on screen open")
                    }

                } else {
                    if (isScanning) {
                        searchViewModel.stopSearch()
                        isScanning = false
                    }
                }
            }
        }*/



/* vasanti   val filteredItems by remember(
        searchItems, filteredDbItems, isScanning, isUnmatchedList, searchQuery, inputItems
    ) {
        derivedStateOf {
            try {
                when {
                    // --- unmatched mode ---
                    isUnmatchedList -> {
                        when {
                            // 1️⃣ Scanning → show live scanned search results
                            isScanning -> searchItems

                            // 2️⃣ Query typed → filter unmatched items by EPC/ItemCode/RFID
                            searchQuery.isNotBlank() -> {
                                inputItems.filter {
                                    val query = searchQuery.trim()
                                    (it.itemCode?.contains(query, ignoreCase = true) == true) ||
                                            (it.rfid?.contains(query, ignoreCase = true) == true) ||
                                            (it.epc?.contains(query, ignoreCase = true) == true)
                                }.map { it.toSearchItem() }
                            }

                            // 3️⃣ Default → show all unmatched items
                            else -> inputItems.map { it.toSearchItem() }
                        }
                    }

                    // --- normal mode ---
                    isScanning -> searchItems
                    searchQuery.isNotBlank() -> filteredDbItems.map { it.toSearchItem() }
                    else -> emptyList()
                }
            } catch (e: Exception) {
                Log.e("SearchScreen", "❌ Error computing filteredItems", e)
                emptyList()
            }
        }
    }*/
    val filteredItems by remember(
        searchItems,
        filteredDbItems,
        isScanning,
        isUnmatchedList,
        searchQuery,
        inputItems,
        lastSortedSearchItems
    ) {
        derivedStateOf {
            val query = searchQuery.trim()

            val baseList: List<SearchItem> = when {
                isScanning && searchItems.isNotEmpty() -> searchItems
                !isScanning && lastSortedSearchItems.isNotEmpty() -> lastSortedSearchItems

                isUnmatchedList -> inputItems.map { it.toSearchItem() }

                query.isNotBlank() -> filteredDbItems.map { it.toSearchItem() }

                else -> emptyList()
            }

            baseList.toList().sortedWith(
                compareByDescending<SearchItem> {
                    it.proximityPercent
                }.thenByDescending {
                    query.isNotBlank() && (
                            it.itemCode.equals(query, true) ||
                                    it.rfid.equals(query, true) ||
                                    it.epc.equals(query, true)
                            )
                }
            )
        }
    }

    LaunchedEffect(filteredItems.firstOrNull()?.epc, filteredItems.firstOrNull()?.proximityPercent) {
        if (filteredItems.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    LaunchedEffect(isScanning, searchItems) {
        if (isScanning && searchItems.isNotEmpty()) {
            lastSortedSearchItems = searchItems.sortedByDescending { it.proximityPercent }
        }
    }


    // ✅ RFID key listener + lifecycle-driven auto-scan
    DisposableEffect(lifecycleOwner, activity) {
        val listener = object : ScanKeyListener {
            override fun onBarcodeKeyPressed() {}
            override fun onRfidKeyPressed() {
                // latestIsScanning always gives the CURRENT value (rememberUpdatedState, no stale closure)
                if (latestIsScanning) {
                    lastSortedSearchItems = searchItems.sortedByDescending { it.proximityPercent }

                    isScanning = false
                    coroutineScope.launch(Dispatchers.IO) {
                        searchViewModel.stopSearch()
                    }

                    Log.d("SEARCH", "RFID STOPPED")
                } else {
                    val itemsToSearch = when {
                        isUnmatchedList -> {
                            val q = searchQuery.trim()
                            if (q.isNotBlank()) {
                                latestInputItems.filter {
                                    it.itemCode?.contains(q, ignoreCase = true) == true ||
                                    it.rfid?.contains(q, ignoreCase = true) == true ||
                                    it.epc?.contains(q, ignoreCase = true) == true
                                }
                            } else latestInputItems
                        }
                        filteredDbItems.isNotEmpty() -> filteredDbItems
                        else -> emptyList()
                    }
                    if (itemsToSearch.isNotEmpty()) {
                        isScanning = true  // instant UI feedback
                        coroutineScope.launch(Dispatchers.IO) {
                            searchViewModel.startSearch(itemsToSearch, latestSelectedPower)
                        }
                        Log.d("SEARCH", "RFID STARTED scanning ${itemsToSearch.size} items")
                    } else {
                        Log.d("SEARCH", "⚠️ No items to scan")
                    }
                }
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    activity?.registerScanKeyListener(listener)
                    // Auto-restart scan on every RESUME when unmatched items exist and not scanning.
                    // Uses coroutineScope so the 600ms delay doesn't block the observer callback.
                    if (isUnmatchedList && latestInputItems.isNotEmpty() && !latestIsScanning) {
                        coroutineScope.launch {
                            delay(150)
                            if (!latestIsScanning) {
                                isScanning = true
                                launch(Dispatchers.IO) {
                                    searchViewModel.startSearch(latestInputItems, latestSelectedPower)
                                }
                                Log.d("AUTO_SCAN", "Auto scan restarted on RESUME (${latestInputItems.size} items)")
                            }
                        }
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    activity?.unregisterScanKeyListener()
                    if (latestIsScanning) {
                        isScanning = false
                        coroutineScope.launch(Dispatchers.IO) {
                            searchViewModel.stopSearch()
                        }
                    }
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            activity?.unregisterScanKeyListener()
            if (latestIsScanning) {
                coroutineScope.launch(Dispatchers.IO) {
                    searchViewModel.stopSearch()
                }
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
                showCounter = true,
                selectedCount = selectedPower,
                onCountSelected = {
                    selectedPower = it

                } ,
                titleTextSize = 20.sp)
        },
        bottomBar = {
            ScanBottomBar(
                onSave = {},
                onList = {},
                onScan = {
                    if (!isScanning) {
                        val itemsToSearch = when {
                            isUnmatchedList -> {
                                // If user typed a query, scan only the matching subset of
                                // unmatched items. Without this, all unmatched items were
                                // passed to startSearch() regardless of the search filter.
                                val q = searchQuery.trim()
                                if (q.isNotBlank()) {
                                    inputItems.filter {
                                        it.itemCode?.contains(q, ignoreCase = true) == true ||
                                        it.rfid?.contains(q, ignoreCase = true) == true ||
                                        it.epc?.contains(q, ignoreCase = true) == true
                                    }
                                } else inputItems
                            }
                            filteredDbItems.isNotEmpty() -> filteredDbItems
                            else -> emptyList()
                        }

                        if (itemsToSearch.isNotEmpty()) {
                            isScanning = true  // instant UI feedback
                            coroutineScope.launch(Dispatchers.IO) {
                                searchViewModel.startSearch(itemsToSearch, selectedPower)
                            }
                            Log.d("SEARCH", "Manual SCAN started (${itemsToSearch.size}) items")
                        } else {
                            Log.d("SEARCH", "⚠️ No items to scan")
                        }
                    } else {

                            lastSortedSearchItems = searchItems.sortedByDescending { it.proximityPercent }

                            isScanning = false
                            coroutineScope.launch(Dispatchers.IO) {
                                searchViewModel.stopSearch()
                            }

                            Log.d("SEARCH", "Manual SCAN stopped")

                    }
                },
                onGscan = {

                },
                onReset = {
                    searchQuery = ""
                    lastSearchItems = emptyList()
                    lastSortedSearchItems = emptyList()
                    filteredDbItems = emptyList()
                    searchViewModel.stopSearch()
                    isScanning = false
                },
                isScanning = isScanning,
                isEditMode = false,
                isScreen=true,
                isBulkScanning = false
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
                LazyColumn( modifier = Modifier.fillMaxSize(),
                    state = listState) {
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
    val animatedPercent by animateFloatAsState(
        targetValue = percent,
        animationSpec = tween(durationMillis = 600),
        label = "searchProximity"
    )
    val progressColor = getColorByPercentage(animatedPercent.toInt())

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
                progress = { animatedPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = Color.LightGray
            )
        }

        Text("${animatedPercent.toInt()}%", modifier = Modifier.weight(1f), fontSize = 12.sp)
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




/*
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
import com.loyalstring.rfid.data.reader.RFIDReaderManager
import com.loyalstring.rfid.data.reader.ScanKeyListener
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.SearchViewModel
import com.rscja.deviceapi.RFIDWithUHFUART
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    navController: NavHostController,
    listKey: String? = "unmatchedItems"
) {
    var showMenu by remember { mutableStateOf(false) }
    val searchViewModel: SearchViewModel = hiltViewModel()
    val context = LocalContext.current
    val activity = context.findActivity() as? MainActivity
    val lifecycleOwner = LocalLifecycleOwner.current
     val readerManager: RFIDReaderManager

    var isScanning by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var allDbItems by remember { mutableStateOf<List<BulkItem>>(emptyList()) }
    var filteredDbItems by remember { mutableStateOf<List<BulkItem>>(emptyList()) }
    var showLoader by remember { mutableStateOf(true) } // Initialize loader to true

    var selectedPower by remember {
        mutableIntStateOf(UserPreferences.getInstance(context).getInt(
            UserPreferences.KEY_SEARCH_COUNT))
    }

    BackHandler { onBack() }

    // ✅ Load DB items once
    LaunchedEffect(Unit) {
        delay(1000)
        allDbItems = withContext(Dispatchers.IO) {
            searchViewModel.getAllBulkItemsFromDb()
        }
        showLoader = false // Dismiss loader after data is loaded
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
        delay(1000)
        withContext(Dispatchers.IO) {
            if (isUnmatchedList && inputItems.isNotEmpty()) {
                searchViewModel.startSearch(inputItems, selectedPower)
                isScanning = true
            } else {
                searchViewModel.clearSearchItems()
            }
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

    val filteredItems by remember(
        searchItems, filteredDbItems, isScanning, isUnmatchedList, searchQuery, inputItems
    ) {
        derivedStateOf {
            try {
                when {
                    // --- unmatched mode ---
                    isUnmatchedList -> {
                        when {
                            // 1️⃣ Scanning → show live scanned search results
                            isScanning -> searchItems

                            // 2️⃣ Query typed → filter unmatched items by EPC/ItemCode/RFID
                            searchQuery.isNotBlank() -> {
                                inputItems.filter {
                                    val query = searchQuery.trim()
                                    (it.itemCode?.contains(query, ignoreCase = true) == true) ||
                                            (it.rfid?.contains(query, ignoreCase = true) == true) ||
                                            (it.epc?.contains(query, ignoreCase = true) == true)
                                }.map { it.toSearchItem() }
                            }

                            // 3️⃣ Default → show all unmatched items
                            else -> inputItems.map { it.toSearchItem() }
                        }
                    }

                    // --- normal mode ---
                    isScanning -> searchItems
                    searchQuery.isNotBlank() -> filteredDbItems.map { it.toSearchItem() }
                    else -> emptyList()
                }
            } catch (e: Exception) {
                Log.e("SearchScreen", "❌ Error computing filteredItems", e)
                emptyList()
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
                showCounter = true,
                selectedCount = selectedPower,
                onCountSelected = {
                    selectedPower = it

                },
                titleTextSize = 20.sp
            )
        },
        bottomBar = {
            ScanBottomBar(
                onSave = {},
                onList = {showMenu = true },
                onScan = {
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
                onGscan = {

                },
                onReset = {
                    searchQuery = ""
                    filteredDbItems = emptyList()
                    searchViewModel.stopSearch()
                    isScanning = false
                },
                isScanning = isScanning,
                isEditMode = false,
                isScreen=true
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

            if (showLoader) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredItems.isEmpty() && !isScanning) {
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
    val animatedPercent by animateFloatAsState(
        targetValue = percent,
        animationSpec = tween(durationMillis = 600),
        label = "searchProximity"
    )
    val progressColor = getColorByPercentage(animatedPercent.toInt())

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
                progress = { animatedPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = Color.LightGray
            )
        }

        Text("${animatedPercent.toInt()}%", modifier = Modifier.weight(1f), fontSize = 12.sp)
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
*/
