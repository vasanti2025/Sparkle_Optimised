package com.loyalstring.rfid.ui.screens

import android.util.Log
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.loyalstring.rfid.R
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.navigation.Screens
import com.loyalstring.rfid.ui.utils.BackgroundGradient
import com.loyalstring.rfid.ui.utils.ToastUtils
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.BulkViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun InventoryMenuScreen(
    onBack: () -> Unit,
    navController: NavHostController,
    //bulkViewModel: BulkViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val composeStart = remember { System.nanoTime() }
    LaunchedEffect(Unit) {
        Log.d("StartupTrace", "InventoryMenuScreen compose start")
        withFrameNanos { frameTime ->
            Log.d(
                "StartupTrace",
                "InventoryMenuScreen compose end ${(frameTime - composeStart) / 1_000_000} ms"
            )
        }
    }

    val bulkViewModel: BulkViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()
    var shouldNavigateBack by remember { mutableStateOf(false) }
    
    // Handle back navigation with delay to allow ripple animation to complete
    LaunchedEffect(shouldNavigateBack) {
        if (shouldNavigateBack) {
            kotlinx.coroutines.delay(50) // Small delay for ripple animation
            onBack()
        }
    }
    
    // Lazy load filters only when needed
    LaunchedEffect(Unit) {
        bulkViewModel.ensureFiltersLoaded()
    }
    
    // Use remember to avoid recreating on recomposition
    val counters by remember { bulkViewModel.counters }.collectAsState()
    val branches by remember { bulkViewModel.branches }.collectAsState()
    val boxes by remember { bulkViewModel.boxes }.collectAsState()
    val exhibitions by remember { bulkViewModel.exhibitions }.collectAsState()

    // Dialog state
    var showDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("") }
    var dialogItems by remember { mutableStateOf(listOf<String>()) }
    var onItemSelected by remember { mutableStateOf<(String) -> Unit>({}) }

//    LaunchedEffect(Unit) {
//        showDialog = false
//        dialogTitle = ""
//        dialogItems = emptyList()
//    }



    val menuItems = listOf(
        "Scan Display" to R.drawable.scan_barcode,
        "Scan Counter" to R.drawable.scan_counter,
        "Scan Box" to R.drawable.scan_box,
        "Scan Branch" to R.drawable.scan_branch,
        "Exhibition" to R.drawable.scan_exhibition
    )

    Scaffold(
        topBar = {
            GradientTopBar(
                title = "Inventory",
                navigationIcon = {
                    IconButton(onClick = { shouldNavigateBack = true }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            menuItems.forEach { (title, icon) ->
                MenuButton(title = title, icon = icon) {
                    when (title) {
                        "Scan Display" -> {
                            navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.set("filterType", "Scan Display")
                            navController.currentBackStackEntry
                                ?.savedStateHandle
                                ?.set("filterValue", "Scan Display")
                            navController.navigate(Screens.ScanDisplayScreen.route)
                        }

                        "Scan Counter" -> {
                            // 🔥 Load data in background, then open dialog
                            //CoroutineScope(Dispatchers.IO).launch {
                                scope.launch(Dispatchers.IO) {
                                withContext(Dispatchers.Main) {
                                    if (counters.isEmpty()) {
                                        ToastUtils.showToast(
                                            context,
                                            "No counters available"
                                        )
                                    } else {
                                        dialogTitle = "Counter"
                                        dialogItems = counters
                                        onItemSelected = { selected ->
                                            navController.currentBackStackEntry
                                                ?.savedStateHandle
                                                ?.set("filterType", "Counter")
                                            navController.currentBackStackEntry
                                                ?.savedStateHandle
                                                ?.set("filterValue", selected)
                                            navController.navigate(Screens.ScanDisplayScreen.route)
                                        }
                                        showDialog = true
                                    }
                                }
                            }
                        }

                        "Scan Branch" -> {
                            //CoroutineScope(Dispatchers.IO).launch {
                                scope.launch(Dispatchers.IO) {
                                withContext(Dispatchers.Main) {
                                    if (branches.isEmpty()) {
                                        ToastUtils.showToast(
                                            context,
                                            "No branches available"
                                        )
                                    } else {
                                        dialogTitle = "Branch"
                                        dialogItems = branches
                                        onItemSelected = { selected ->
                                            navController.currentBackStackEntry
                                                ?.savedStateHandle
                                                ?.set("filterType", "Branch")
                                            navController.currentBackStackEntry
                                                ?.savedStateHandle
                                                ?.set("filterValue", selected)
                                            navController.navigate(Screens.ScanDisplayScreen.route)
                                        }
                                        showDialog = true
                                    }
                                }
                            }
                        }

                        "Scan Box" -> {
                            //CoroutineScope(Dispatchers.IO).launch {
                            scope.launch(Dispatchers.IO) {
                                withContext(Dispatchers.Main) {
                                    if (boxes.isEmpty()) {
                                        ToastUtils.showToast(
                                            context,
                                            "No boxes available"
                                        )
                                    } else {
                                        dialogTitle = "Box"
                                        dialogItems = boxes
                                        onItemSelected = { selected ->
                                            navController.currentBackStackEntry
                                                ?.savedStateHandle
                                                ?.set("filterType", "Box")
                                            navController.currentBackStackEntry
                                                ?.savedStateHandle
                                                ?.set("filterValue", selected)
                                            navController.navigate(Screens.ScanDisplayScreen.route)
                                        }
                                        showDialog = true
                                    }
                                }
                            }
                        }

                        "Exhibition" -> {
                            //CoroutineScope(Dispatchers.IO).launch {
                            scope.launch(Dispatchers.IO) {
                                withContext(Dispatchers.Main) {
                                    if (exhibitions.isEmpty()) {
                                        ToastUtils.showToast(
                                            context,
                                            "No exhibitions branch available"
                                        )
                                    } else {
                                        dialogTitle = "Exhibition"
                                        dialogItems = exhibitions
                                        onItemSelected = { selected ->
                                            navController.currentBackStackEntry
                                                ?.savedStateHandle
                                                ?.set("filterType", "Exhibition")
                                            navController.currentBackStackEntry
                                                ?.savedStateHandle
                                                ?.set("filterValue", selected)
                                            navController.navigate(Screens.ScanDisplayScreen.route)
                                        }
                                        showDialog = true
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        SelectionDialog(
            title = dialogTitle,
            items = dialogItems,
            onDismiss = { showDialog = false },
            onSelect = {
                showDialog = false
                onItemSelected(it)
            }
        )
    }
}

@Composable
fun MenuButton(
    title: String,
    icon: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(
                color = Color(0xFF3B363E),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.CenterStart // anchor from start
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 85.dp), // 🔥 adjust this value until it looks visually centered
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = title,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = poppins
            )
        }
    }
}


@Composable
fun SelectionDialog(
    title: String,
    items: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(Color.White, shape = RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            // ✅ Close button at very top-right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = "Close",
                        tint = Color.Gray
                    )
                }
            }

            // Header Row (Select + Plus)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F3F3), RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select $title",
                    fontFamily = poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = Color(0xFF3B363E)
                )

                // + button
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(
                            BackgroundGradient,
                            shape = CircleShape
                        )
                        .clickable { expanded = !expanded },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.vector_add),
                        contentDescription = "Expand",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Expandable list
            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
//                        .padding(top = 8.dp)
                        .background(Color(0xFFF5F3F3), RoundedCornerShape(12.dp))
                ) {
                    items.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(item)
                                    expanded = false
                                }
                                .padding(vertical = 12.dp, horizontal = 16.dp)
                        ) {
                            Text(
                                text = item,
                                fontFamily = poppins,
                                fontSize = 14.sp,
                                color = Color(0xFF3B363E)
                            )
                        }
                    }
                }
            }
        }
    }
}





