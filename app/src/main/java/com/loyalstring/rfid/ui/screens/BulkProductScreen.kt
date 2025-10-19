
package com.loyalstring.rfid.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.loyalstring.rfid.MainActivity
import com.loyalstring.rfid.data.reader.ScanKeyListener
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.navigation.Screens
import com.loyalstring.rfid.ui.utils.AddItemDialog
import com.loyalstring.rfid.ui.utils.BackgroundGradient
import com.loyalstring.rfid.ui.utils.ToastUtils
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.BulkViewModel

@SuppressLint("RestrictedApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkProductScreen(
    onBack: () -> Unit, navController: NavHostController// hardware key events
) {

    val viewModel: BulkViewModel = hiltViewModel()
    val context = LocalContext.current
    // Observe barcode and tag data
    val tags by viewModel.scannedTags.collectAsState()
    val rfidMap by viewModel.rfidMap.collectAsState()
    val itemCodes = remember { mutableStateOf("") }
    // Dropdown options
    val categories by viewModel.categories.collectAsState()
    val products by viewModel.products.collectAsState()
    val designs by viewModel.designs.collectAsState()

    var selectedCategory by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf("") }
    var selectedDesign by remember { mutableStateOf("") }

    var showAddDialogFor by remember { mutableStateOf<String?>(null) }
    var firstPress by remember { mutableStateOf(false) }
    var shouldNavigateBack by remember { mutableStateOf(false) }

    LaunchedEffect(shouldNavigateBack) {
        if (shouldNavigateBack) {
            kotlinx.coroutines.delay(50)
            onBack()
        }
    }

    var clickedIndex by remember { mutableStateOf<Int?>(null) }

    var selectedPower by remember { mutableStateOf(10) }
    remember { mutableStateOf("") }



    val allScannedTags by viewModel.allScannedTags
    val existingTags by viewModel.existingItems
    val duplicateTags by viewModel.duplicateItems

    val activity = LocalContext.current as MainActivity
    var isScanning by remember { mutableStateOf(false) }
    //var showSuccessDialog by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }


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


    // ✅ Set barcode scan callback ONCE
    LaunchedEffect(Unit) {
        viewModel.barcodeReader.openIfNeeded()
        viewModel.barcodeReader.setOnBarcodeScanned { scanned ->
            viewModel.onBarcodeScanned(scanned)
            viewModel.setRfidForAllTags(scanned)

        }
    }



    Scaffold(
        topBar = {
            GradientTopBar(
                title = "Add Bulk Products",
                navigationIcon = {
                    IconButton(onClick = { shouldNavigateBack = true }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {

                },
                showCounter = true,
                selectedCount = selectedPower,
                onCountSelected = {
                    selectedPower = it
                }
            )
        },
        bottomBar = {


            ScanBottomBar(
                onSave = {
                    viewModel.barcodeReader.close()

                    if (selectedCategory.isNotBlank() && selectedProduct.isNotBlank() && selectedDesign.isNotBlank()) {
                        tags.forEachIndexed { index, _ ->
                            val itemCode = itemCodes.value
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
                    } else {
                        ToastUtils.showToast(context, "Category/Product/Design cannot be empty")
                    }
                },
                onList = { navController.navigate(Screens.ProductListScreen.route) },
                onScan = {
                    viewModel.startSingleScan(20)
                },
                onGscan = {

                    if (isScanning) {
                        viewModel.stopScanning()
                        isScanning = false
                    } else {
                        viewModel.startScanning(selectedPower)
                        isScanning = true


                    }

                   // viewModel.toggleScanning(selectedPower)

                },
                onReset = {
                    firstPress = false
                    viewModel.resetProductScanResults()
                    viewModel.stopBarcodeScanner()
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
                .background(Color.White)
        ) {
            Row(
                modifier = Modifier
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterDropdown(
                    label = "Category",
                    options = categories.map { it.name },
                    selectedOption = selectedCategory,
                    onOptionSelected = {
                        selectedCategory = it
                    },
                    onAddOption = { showAddDialogFor = "Category" },
                    modifier = Modifier
                        .weight(1f)

                )

                FilterDropdown(
                    label = "Product",
                    options = products.map { it.name },
                    selectedOption = selectedProduct,
                    onOptionSelected = {
                        selectedProduct = it
                    },
                    onAddOption = { showAddDialogFor = "Product" },
                    modifier = Modifier
                        .weight(1f)
                )

                FilterDropdown(
                    label = "Design",
                    options = designs.map { it.name },
                    selectedOption = selectedDesign,
                    onOptionSelected = { selectedDesign = it },
                    onAddOption = { showAddDialogFor = "Design" },
                    modifier = Modifier
                        .weight(1f)

                )
            }
            Spacer(modifier = Modifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(BackgroundGradient))

            if (showAddDialogFor != null) {
                AddItemDialog(
                    title = showAddDialogFor!!,
                    onAdd = { newItem ->
                        when (showAddDialogFor) {
                            "Category" -> {
                                selectedCategory = newItem
                                viewModel.saveDropdownCategory(newItem, "Category")
                            }

                            "Product" -> {
                                selectedProduct = newItem
                                viewModel.saveDropdownProduct(newItem, "Product")
                            }

                            "Design" -> {
                                selectedDesign = newItem
                                viewModel.saveDropdownDesign(newItem, "Design")
                            }
                        }
                        showAddDialogFor = null
                    },
                    onDismiss = { showAddDialogFor = null }
                )
            }

            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .background(Color.DarkGray)
                    .padding(vertical = 4.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.width(50.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Sr No.",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = poppins
                    )
                }
                Box(modifier = Modifier.width(150.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "Item Code",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = poppins
                    )
                }
                Box(modifier = Modifier.width(150.dp), contentAlignment = Alignment.Center) {
                    Text(
                        "RFID Code",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        fontFamily = poppins
                    )
                }
            }

            LazyColumn(
                contentPadding = PaddingValues(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFFF0F0F0))
            ) {
                itemsIndexed(
                    items = tags,
                    key = { index, item -> item.epc ?: index }
                ) { index, item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Sr No
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
                                    fontFamily = poppins
                                )
                            }

// Item Code
                          Box(
                                modifier = Modifier
                                    .width(150.dp)
                                    .height(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                              val rfid = rfidMap[index]
                              rfid != null
                              rfid ?: ""
                                BasicTextField(
                                    value = itemCodes.value,
                                    onValueChange = { itemCodes.value = it },
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(
                                        fontSize = 11.sp,
                                        color = Color.DarkGray,
                                        fontFamily = poppins
                                    ),
                                    decorationBox = { innerTextField ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            innerTextField()
                                        }
                                    }
                                )
                            }

// RFID Text
                         /*   Box(
                                modifier = Modifier
                                    .width(150.dp)
                                    .height(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val rfid = rfidMap[index]
                                val isScanned = rfid != null
                                val displayText = rfid ?: "scan here"
                                val textColor = if (!isScanned) Color.Blue else Color.DarkGray
                                val style =
                                    if (!isScanned) TextDecoration.Underline else TextDecoration.None
                                Text(

                                    text = displayText,
                                    modifier = Modifier.clickable {
                                        clickedIndex = index
                                        viewModel.startBarcodeScanning(context)
                                    },
                                    fontSize = 11.sp,
                                    color = textColor,
                                    textDecoration = style,
                                    fontFamily = poppins
                                )
                            }*/



                            val rowValue = rfidMap[index] ?: ""

                            BasicTextField(
                                value = rowValue,
                                onValueChange = { newValue ->
                                    viewModel.updateRfidForIndex(index, newValue) // ✅ update that row in VM
                                },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 11.sp,
                                    color = Color.DarkGray,
                                    fontFamily = poppins
                                ),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 4.dp)
                                    .clickable {
                                        clickedIndex = index
                                        viewModel.startBarcodeScanning(context) // scanner will call updateRfidForIndex
                                    },
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        innerTextField()
                                    }
                                }
                            )




                        }

                        Spacer(
                            modifier = Modifier
                                .height(0.5.dp)
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
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                println("EXISTING :${existingTags.size}")
                println("ALL ITEMS : ${allScannedTags.size}")
                println("DUPLICATE : ${duplicateTags.size}")

                Text(
                    "Exist Items: ${existingTags.size}", color = Color.White, fontFamily = poppins,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .background(Color.DarkGray)
                        .padding(3.dp)
                )
                Text(
                    "Total Items: ${allScannedTags.size}",
                    color = Color.White,
                    fontFamily = poppins,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .background(Color.DarkGray)
                        .padding(3.dp)

                )
            }
        }
      /*  fun updateRfidAt(index: Int, newValue: String) {
            val currentMap = rfidMap.value.toMutableMap()
            currentMap[index] = newValue
            _rfidMap.value = currentMap
        }*/
    }


}



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
                .height(40.dp) // ensures enough height for text and icon
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
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
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
                text = {
                    Text(
                        "➕ Add New",
                        color = Color.DarkGray,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = poppins
                    )
                },
                onClick = {
                    expanded = false
                    onAddOption()
                }
            )
        }
    }
}



