package com.loyalstring.rfid.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.loyalstring.rfid.MainActivity
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.reader.ScanKeyListener
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.SearchViewModel

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    navController: NavHostController,
) {
    val searchViewModel: SearchViewModel = hiltViewModel()
    var isScanning by remember { mutableStateOf(false) }
    var firstPress by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isEditMode by remember { mutableStateOf(false) }
    var shouldNavigateBack by remember { mutableStateOf(false) }

    val unmatchedItems = navController
        .previousBackStackEntry
        ?.savedStateHandle
        ?.get<ArrayList<BulkItem>>("unmatchedItems")
        ?: arrayListOf()

    Log.d("SEARCH_SCREEN", "Got ${unmatchedItems.size} unmatched items")

    val context = LocalContext.current
    val activity = context.findActivity() as? MainActivity
    val lifecycleOwner = LocalLifecycleOwner.current

    // Handle back navigation with delay to allow ripple animation to complete
    LaunchedEffect(shouldNavigateBack) {
        if (shouldNavigateBack) {
            kotlinx.coroutines.delay(50) // Small delay for ripple animation
            onBack()
        }
    }

    // ✅ Filter ViewModel items (not local allItems)
    val filteredItems by remember(searchQuery, searchViewModel.searchItems) {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                searchViewModel.searchItems
            } else {
                searchViewModel.searchItems.filter {
                    it.rfid.contains(searchQuery, ignoreCase = true) ||
                            it.itemCode.contains(searchQuery, ignoreCase = true)
                }
            }
        }
    }
    LaunchedEffect(unmatchedItems) {
        if (unmatchedItems.isNotEmpty()) {
            searchViewModel.startSearch(unmatchedItems)
        }
    }

    // ✅ Register scan key listener
    DisposableEffect(lifecycleOwner, activity) {
        val listener = object : ScanKeyListener {
            override fun onBarcodeKeyPressed() {}
            override fun onRfidKeyPressed() {
                if (isScanning) {
                    searchViewModel.stopSearch()
                    isScanning = false
                    Log.d("@@", "RFID STOPPED from key")
                } else {
                    searchViewModel.startSearch(unmatchedItems)
                    isScanning = true
                    Log.d("@@", "RFID STARTED from key")
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
                title = "Search",
                navigationIcon = {
                    IconButton(onClick = { shouldNavigateBack = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                showCounter = true,
                selectedCount = filteredItems.size,
                onCountSelected = {}
            )
        },
        bottomBar = {
            ScanBottomBar(
                onSave = { },
                onList = { },
                onScan = { },
                onGscan = {
                    if (!isScanning) {
                        firstPress = true
                        isScanning = true
                        searchViewModel.startSearch(unmatchedItems)
                    } else {
                        searchViewModel.stopSearch()
                        firstPress = false
                        isScanning = false
                    }
                },
                onReset = {
                    searchQuery = ""
                    searchViewModel.stopSearch()
                    firstPress = false
                    isScanning = false
                },
                isScanning = isScanning,
                isEditMode=isEditMode

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
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                label = { Text("Enter RFID / Itemcode", fontFamily = poppins) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                singleLine = true
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                // ✅ Header row
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF3B363E))
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("Sr No", "RFIDcode", "Itemcode", "Progress", "Percentage")
                            .forEach {
                                Text(
                                    text = it,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f),
                                    fontFamily = poppins,
                                    fontSize = 12.sp
                                )
                            }
                    }
                }

                // ✅ Show filtered ViewModel items only
                itemsIndexed(
                    filteredItems,
                    key = { index, item -> "${item.epc}-$index" }
                ) { index, item ->
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
                                trackColor = Color.LightGray,
                            )
                        }

                        Text(
                            "${percent.toInt()}%",
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 4.dp),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}


fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun getColorByPercentage(percent: Int): Color {
    return when {
        percent <= 25 -> Color.Red
        percent <= 50 -> Color.Yellow
        percent <= 75 -> Color(0xFF2196F3)
        else -> Color(0xFF4CAF50)
    }
}
