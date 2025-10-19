//package com.loyalstring.rfid.ui.screens
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.widget.Toast
//import androidx.compose.foundation.background
//import androidx.compose.foundation.clickable
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.fillMaxHeight
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.automirrored.filled.ArrowBack
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Surface
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.derivedStateOf
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.livedata.observeAsState
//import androidx.compose.runtime.mutableIntStateOf
//import androidx.compose.runtime.mutableStateListOf
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.saveable.rememberSaveable
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.unit.dp
//import androidx.hilt.navigation.compose.hiltViewModel
//import androidx.navigation.NavHostController
//import com.loyalstring.rfid.data.model.ClientCodeRequest
//import com.loyalstring.rfid.data.model.addSingleItem.BranchModel
//import com.loyalstring.rfid.data.model.addSingleItem.CategoryModel
//import com.loyalstring.rfid.data.model.addSingleItem.DesignModel
//import com.loyalstring.rfid.data.model.addSingleItem.ProductModel
//import com.loyalstring.rfid.data.model.login.Employee
//import com.loyalstring.rfid.data.remote.resource.Resource
//import com.loyalstring.rfid.navigation.GradientTopBar
//import com.loyalstring.rfid.navigation.Screens
//import com.loyalstring.rfid.ui.utils.UserPreferences
//import com.loyalstring.rfid.viewmodel.BulkViewModel
//import com.loyalstring.rfid.viewmodel.ProductListViewModel
//import com.loyalstring.rfid.viewmodel.SingleProductViewModel
//
//@SuppressLint("UnrememberedMutableState")
//@Composable
//fun ScanBranchScreen(onBack: () -> Unit, navController: NavHostController) {
//    val singleProductViewModel: SingleProductViewModel = hiltViewModel()
//    val productListViewModel: ProductListViewModel = hiltViewModel()
//    val bulkViewModel: BulkViewModel = hiltViewModel()
//    val context: Context = LocalContext.current
//
//    val categoryResponse = singleProductViewModel.categoryResponse.observeAsState().value
//    val productResponse = singleProductViewModel.productResponse.observeAsState().value
//    val designResponse = singleProductViewModel.designResponse.observeAsState().value
//
//    val allCategories =
//        (categoryResponse as? Resource.Success<List<CategoryModel>>)?.data ?: emptyList()
//    val allProducts =
//        (productResponse as? Resource.Success<List<ProductModel>>)?.data ?: emptyList()
//    val allDesigns = (designResponse as? Resource.Success<List<DesignModel>>)?.data ?: emptyList()
//    val allItems by productListViewModel.productList.collectAsState(initial = emptyList())
//
//    val selectedCategories = remember { mutableStateListOf<String>() }
//    val selectedProducts = remember { mutableStateListOf<String>() }
//    val selectedDesigns = remember { mutableStateListOf<String>() }
//
//    var currentLevel by rememberSaveable { mutableStateOf("Category") }
//    var currentCategory by rememberSaveable { mutableStateOf<String?>(null) }
//    var currentProduct by remember { mutableStateOf<String?>(null) }
//    var showMenu by remember { mutableStateOf(false) }
//    var showDialog by remember { mutableStateOf(false) }
//    var filterType by remember { mutableStateOf("Category") }
//    var selectedMenu by remember { mutableStateOf("All") }
//    var selectedPower by remember { mutableIntStateOf(10) }
//    var firstPress by remember { mutableStateOf(false) }
//
//    val selectedBranches =
//        navController.previousBackStackEntry?.savedStateHandle?.get<List<BranchModel>>("selectedBranches")
//            ?: emptyList()
//
//    val filteredByBranchItems by remember(allItems, selectedBranches) {
//        derivedStateOf {
//            if (selectedBranches.isEmpty()) allItems
//            else allItems.filter { item -> selectedBranches.any { it.BranchName == item.branchName } }
//        }
//    }
//
//
//    val scannedFilteredItems = bulkViewModel.scannedFilteredItems.value
//    val selectedCategoriesState = selectedCategories.toList()
//    val selectedProductsState = selectedProducts.toList()
//    val selectedDesignsState = selectedDesigns.toList()
//
//    val filteredItems = remember(
//        filteredByBranchItems,
//        selectedCategoriesState,
//        selectedProductsState,
//        selectedDesignsState
//    ) {
//        filteredByBranchItems.filter { item ->
//            (selectedCategoriesState.isEmpty() || selectedCategoriesState.contains(item.category.orEmpty())) &&
//                    (selectedProductsState.isEmpty() || selectedProductsState.contains(item.productName.orEmpty())) &&
//                    (selectedDesignsState.isEmpty() || selectedDesignsState.contains(item.design.orEmpty()))
//        }
//    }
//
//    val displayItems = remember(filteredItems, scannedFilteredItems, selectedMenu) {
//        when (selectedMenu) {
//            "Matched" -> scannedFilteredItems.filter { it.scannedStatus == "Matched" }
//            "Unmatched" -> scannedFilteredItems.filter { it.scannedStatus == "Unmatched" }
//            else -> if (scannedFilteredItems.isNotEmpty()) scannedFilteredItems else filteredItems
//        }
//    }
//
//    val tableRows by remember(displayItems, currentLevel, currentCategory, currentProduct) {
//        derivedStateOf {
//            when (currentLevel) {
//                "Category" -> displayItems
//                    .filter {
//                        selectedCategories.isEmpty() || selectedCategories.contains(it.category.orEmpty())
//                    }
//                    .groupBy { it.category ?: "Unknown" }
//                    .map { TableRow(it.key, it.value) }
//
//                "Product" -> displayItems
//                    .filter {
//                        (selectedCategories.isEmpty() || selectedCategories.contains(it.category.orEmpty())) &&
//                                (selectedProducts.isEmpty() || selectedProducts.contains(it.productName.orEmpty()))
//                    }
//                    .groupBy { it.productName ?: "Unknown" }
//                    .map { TableRow(it.key, it.value) }
//
//                "Design" -> displayItems
//                    .filter {
//                        (selectedCategories.isEmpty() || selectedCategories.contains(it.category.orEmpty())) &&
//                                (selectedProducts.isEmpty() || selectedProducts.contains(it.productName.orEmpty())) &&
//                                (selectedDesigns.isEmpty() || selectedDesigns.contains(it.design.orEmpty()))
//                    }
//                    .groupBy { it.design ?: "Unknown" }
//                    .map { TableRow(it.key, it.value) }
//
//                else -> emptyList()
//            }
//        }
//    }
//
//    LaunchedEffect(selectedCategories, selectedProducts, selectedDesigns, currentLevel) {
//        println("▶️ Filters — Category: $selectedCategories, Product: $selectedProducts, Design: $selectedDesigns")
//        println("▶️ Level: $currentLevel | Category: $currentCategory | Product: $currentProduct")
//    }
//
//    val employee = UserPreferences.getInstance(context).getEmployee(Employee::class.java)
//    LaunchedEffect(Unit) {
//        employee?.clientCode?.let {
//            singleProductViewModel.fetchAllDropdownData(ClientCodeRequest(it))
//        }
//    }
//
//    Box {
//        Scaffold(
//            topBar = {
//                GradientTopBar(
//                    title = "Scan Branch",
//                    navigationIcon = {
//                        IconButton(onClick = onBack) {
//                            Icon(
//                                Icons.AutoMirrored.Filled.ArrowBack,
//                                contentDescription = "Back",
//                                tint = Color.White
//                            )
//                        }
//                    },
//                    showCounter = true,
//                    selectedCount = selectedPower,
//                    onCountSelected = { selectedPower = it }
//                )
//            },
//            bottomBar = {
//                Column {
//                    SummaryRow(tableRows)
//                    ScanBottomBar(
//                        onSave = {},
//                        onList = { showMenu = true },
//                        onScan = {},
//                        onGscan = {
//                            if (!firstPress) {
//                                firstPress = true
//                                if (selectedCategories.isNotEmpty()) {
//                                    bulkViewModel.startScanning(selectedPower)
//                                } else {
//                                    Toast.makeText(
//                                        context,
//                                        "Please select a Category first",
//                                        Toast.LENGTH_SHORT
//                                    ).show()
//                                }
//                            } else {
//                                firstPress = false
//                                bulkViewModel.onScanStopped()
//                                bulkViewModel.computeScanResults(filteredItems)
//                            }
//                        },
//                        onReset = {
//                            firstPress = false
//                            selectedCategories.clear()
//                            selectedProducts.clear()
//                            selectedDesigns.clear()
//                        }
//                    )
//                }
//            }
//        ) { innerPadding ->
//            Column(Modifier.padding(innerPadding)) {
//                FilterRow(
//                    selectedCategories = selectedCategories,
//                    selectedProducts = selectedProducts,
//                    selectedDesigns = selectedDesigns,
//                    onCategoryClick = {
//                        filterType = "Category"
//                        showDialog = true
//                    },
//                    onProductClick = {
//                        filterType = "Product"
//                        showDialog = true
//                    },
//                    onDesignClick = {
//                        filterType = "Design"
//                        showDialog = true
//                    }
//                )
//
//                LazyColumn(modifier = Modifier.fillMaxSize()) {
//                    item { TableHeader(currentLevel) }
//                    items(tableRows) { row ->
//                        TableDataRow(row, currentLevel) {
//                            when (currentLevel) {
//                                "Category" -> {
//                                    currentCategory = row.label
//                                    currentLevel = "Product"
//                                    selectedCategories.clear()
//                                    selectedCategories.add(row.label)
//                                }
//
//                                "Product" -> {
//                                    currentProduct = row.label
//                                    currentLevel = "Design"
//                                    selectedProducts.clear()
//                                    selectedProducts.add(row.label)
//                                }
//                            }
//
//                        }
//                    }
//                }
//            }
//        }
//
//        if (showDialog) {
//            val items = when (filterType) {
//                "Category" -> allCategories.map { it.CategoryName }
//                "Product" -> allProducts
//                    .filter { selectedCategories.isEmpty() || selectedCategories.contains(it.CategoryName) }
//                    .map { it.ProductName }
//
//                "Design" -> {
//                    // Get ProductIds from selected Product Names
//                    val selectedProductIds = allProducts
//                        .filter { selectedProducts.contains(it.ProductName) }
//                        .map { it.ProductName }
//
//                    // Now filter designs by selected productIds
//                    allDesigns
//                        .filter { selectedProductIds.isEmpty() || selectedProductIds.contains(it.ProductName) }
//                        .map { it.DesignName }
//                }
//
//                else -> emptyList()
//            }
//
//            val selected = when (filterType) {
//                "Category" -> selectedCategories
//                "Product" -> selectedProducts
//                "Design" -> selectedDesigns
//                else -> mutableStateListOf()
//            }
//
//            FilterSelectionDialog(
//                title = filterType,
//                items = items,
//                selectedItems = selected,
//                onDismiss = { showDialog = false },
//                onConfirm = {
//                    // Clear dependent selections
//                    when (filterType) {
//                        "Category" -> {
//                            selectedProducts.clear()
//                            selectedDesigns.clear()
//                            currentCategory = null
//                            currentProduct = null
//                            currentLevel = "Category"
//                        }
//
//                        "Product" -> {
//                            selectedDesigns.clear()
//                            currentProduct = null
//                            currentLevel = "Product"
//                        }
//
//                        "Design" -> {
//                            currentLevel = "Design"
//                        }
//                    }
//                    showDialog = false
//                }
//            )
//
//        }
//
//        if (showMenu) {
//            Box(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .background(Color(0x80000000))
//                    .clickable { showMenu = false }
//            )
//
//            Surface(
//                modifier = Modifier
//                    .padding(top = 60.dp, bottom = 70.dp)
//                    .width(180.dp)
//                    .fillMaxHeight()
//                    .align(Alignment.TopStart),
//                shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp),
//                shadowElevation = 8.dp,
//                color = Color.White
//            ) {
//                VerticalMenu(
//                    onMenuClick = { menuItem ->
//                        when (menuItem.title) {
//                            "Matched Items" -> selectedMenu = "Matched"
//                            "UnMatched Items" -> selectedMenu = "Unmatched"
//                            "Search" -> {
//                                val unmatched =
//                                    displayItems.filter { it.scannedStatus == "Unmatched" }
//                                try {
//                                    navController.getBackStackEntry(Screens.SearchScreen.route).savedStateHandle["unmatchedItems"] =
//                                        unmatched
//                                } catch (e: Exception) {
//                                    navController.navigate(Screens.SearchScreen.route)
//                                    navController.currentBackStackEntry?.savedStateHandle?.set(
//                                        "unmatchedItems",
//                                        unmatched
//                                    )
//                                    return@VerticalMenu
//                                }
//                                navController.navigate(Screens.SearchScreen.route)
//                            }
//
//                            else -> selectedMenu = "All"
//                        }
//                        showMenu = false
//                    }
//                )
//            }
//        }
//    }
//}
