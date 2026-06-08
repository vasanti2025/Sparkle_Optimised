package com.loyalstring.rfid.ui.screens

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem

import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.remote.data.ProductDeleteModelReq
import com.loyalstring.rfid.data.remote.resource.Resource
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.navigation.Screens
import com.loyalstring.rfid.ui.utils.GradientButton
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.BulkViewModel
import com.loyalstring.rfid.viewmodel.ProductListViewModel
import com.loyalstring.rfid.viewmodel.SingleProductViewModel
import com.loyalstring.rfid.worker.LocaleHelper
import kotlinx.coroutines.withContext
import java.io.File


@SuppressLint("StringFormatInvalid")
@Composable
fun ProductListScreen(
    onBack: () -> Unit,
    navController: NavHostController
) {

    val scope = rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(false) }
    val viewModel: ProductListViewModel = hiltViewModel()
    val bulkViewModel: BulkViewModel = hiltViewModel()
    val singleproductViewModel: SingleProductViewModel = hiltViewModel()
    val searchQuery = remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    var selectedCount by remember { mutableStateOf(1) }
    var isGridView by remember { mutableStateOf(false) }

    var showDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<BulkItem?>(null) }
    val context = LocalContext.current
    val employee = UserPreferences.getInstance(context).getEmployee(Employee::class.java)
    var showConfirmDelete by remember { mutableStateOf(false) }
    val baseUrl = "https://rrgold.loyalstring.co.in/"
    //var deletingItemId by remember { mutableStateOf<Int?>(null) }
    var isEditMode by remember { mutableStateOf(false) }
    val deleteResponse by singleproductViewModel.productDeleetResponse.observeAsState()
    var shouldNavigateBack by remember { mutableStateOf(false) }

    val userPreferences = UserPreferences.getInstance(context)
    val savedLang = userPreferences.getAppLanguage().ifBlank { "en" }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = currentLocales[0]?.language ?: savedLang
    val localizedContext = LocaleHelper.applyLocale(context, currentLang)
    var isPdfExporting by remember { mutableStateOf(false) }
    var deletingItemId by remember { mutableStateOf<Int?>(null) }

    var showFilterDialog by remember { mutableStateOf(false) }

    var selectedSku by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf("") }
    var selectedDesign by remember { mutableStateOf("") }
    var selectedPurity by remember { mutableStateOf("") }

    val isLoading by viewModel.isLoading.collectAsState()
    LaunchedEffect(Unit) {
        ensureProductImagesFolder(context)
    }
    LaunchedEffect(shouldNavigateBack) {
        if (shouldNavigateBack) {
            kotlinx.coroutines.delay(50)
            onBack()
        }
    }

    //   val allItems by viewModel.productList.collectAsState(initial = emptyList())
    val allItems by viewModel.productList.collectAsStateWithLifecycle()
    /*val filteredItems = remember(searchQuery.value, allItems) {
        allItems.filter { item ->
            val query = searchQuery.value.trim().lowercase()

            item.itemCode?.lowercase()?.contains(query) == true ||
                    item.productName?.lowercase()?.contains(query) == true ||
                    item.rfid?.lowercase()?.contains(query) == true
        }
    }*/

    val filteredItems = remember(
        searchQuery.value,
        allItems,
        selectedSku,
        selectedCategory,
        selectedProduct,
        selectedDesign,
        selectedPurity
    ) {
        allItems.filter { item ->
            val query = searchQuery.value.trim().lowercase()

            val searchMatch =
                query.isBlank() ||
                        item.itemCode?.lowercase()?.contains(query) == true ||
                        item.productName?.lowercase()?.contains(query) == true ||
                        item.rfid?.lowercase()?.contains(query) == true

            val skuMatch = selectedSku.isBlank() || item.sku == selectedSku
            val categoryMatch = selectedCategory.isBlank() || item.category == selectedCategory
            val productMatch = selectedProduct.isBlank() || item.productName == selectedProduct
            val designMatch = selectedDesign.isBlank() || item.design == selectedDesign
            val purityMatch = selectedPurity.isBlank() || item.purity == selectedPurity

            searchMatch &&
                    skuMatch &&
                    categoryMatch &&
                    productMatch &&
                    designMatch &&
                    purityMatch
        }
    }



    LaunchedEffect(deleteResponse) {
        when (deleteResponse) {
            is Resource.Success -> {

                // singleproductViewModel.insertLabelledStock(request)
                // singleproductViewModel.deleteItem(id) // ✅ local delete with cached id
                Toast.makeText(context,
                    localizedContext.getString(R.string.item_deleted_successfully), Toast.LENGTH_SHORT).show()


                deletingItemId?.let { id ->
                    viewModel.removeItemLocally(id)
                }
               // not necessary lo call this one alrteday deleted from the locally
               // viewModel.refrshProductList()


            }
            is Resource.Error -> {
                Toast.makeText(context,   localizedContext.getString(R.string.delete_failed), Toast.LENGTH_SHORT).show()
            }
            else -> Unit
        }
    }
    /*    val deleteResult by singleproductViewModel.deleteResult.collectAsState()
        LaunchedEffect(deleteResult) {
            when {
                deleteResult == null -> Unit
                deleteResult ?: 0 > 0 -> {
                    Toast.makeText(context, "Item deleted successfully", Toast.LENGTH_SHORT).show()
                    showConfirmDelete = false
                    selectedItem = null
                }
                else -> {
                    Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show()
                }
            }
        }*/





    Scaffold(
        topBar = {
            GradientTopBar(
                title =   localizedContext.getString(R.string.product_list),
                navigationIcon = {
                    IconButton(onClick = { shouldNavigateBack = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription =   localizedContext.getString(R.string.back),
                            tint = Color.White
                        )
                    }
                },
                selectedCount = selectedCount,
                onCountSelected = { selectedCount = it },
                titleTextSize = 20.sp
            )
        },
        bottomBar = {
            ScanBottomBar(
                onSave = { /* Save logic */ },
                onList = { navController.navigate(Screens.ProductListScreen.route) },
                onScan = { /* Scan logic */ },
                onGscan = { /* Gscan logic */ },
                onReset = {   searchQuery.value = ""

                    selectedSku = ""
                    selectedCategory = ""
                    selectedProduct = ""
                    selectedDesign = ""
                    selectedPurity = ""

                    showFilterDialog = false },
                isScanning = isScanning,
                isEditMode=isEditMode,
                isScreen=false,
                isBulkScanning = false
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()

        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)

                    .background(Color.White)
            ) {
                Spacer(Modifier.height(12.dp))
                /*  OutlinedTextField(
                value = searchQuery.value,
                onValueChange = { searchQuery.value = it },
                placeholder = { Text("Enter RFID / Item code / Product", fontFamily = poppins) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )*/
                TextField(
                    value = searchQuery.value,
                    onValueChange = { searchQuery.value = it },
                    placeholder = {
                        Text(
                            localizedContext.getString(R.string.enter_rfid_item_code_product),
                            fontFamily = poppins
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(5.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF0F0F0),
                        unfocusedContainerColor = Color(0xFFF0F0F0),
                        disabledContainerColor = Color(0xFFF0F0F0),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp) // adds spacing between buttons
                ) {
                    ActionButton(
                        text = if (isGridView)   localizedContext.getString(R.string.list_view) else   localizedContext.getString(R.string.grid_view
                        ),
                        onClick = { isGridView = !isGridView },
                        gradient = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFD32940),
                                Color(0xFF5231A7)
                            ) // blue to cyan gradient
                        ),
                        backgroundColor = Color.Transparent,
                        icon = if (isGridView) {
                            painterResource(id = R.drawable.list_svg)   // 👈 your drawable
                        } else {
                            painterResource(id = R.drawable.grid_svg)
                        }
                    )
                    ActionButton(
                        text = localizedContext.getString(R.string.filter),
                        onClick = {
                            showFilterDialog = true
                        },
                        gradient = Brush.horizontalGradient(
                            colors = listOf(Color(0xFFD32940), Color(0xFF5231A7))
                        ),
                        icon = painterResource(id = R.drawable.filter_svg)
                    )
                    ActionButton(
                        text = localizedContext.getString(R.string.export_pdf),
                        onClick = {
                            isPdfExporting = true

                            scope.launch(Dispatchers.IO) {
                                try {
                                    val pdfFile = exportProductsToPdf(
                                        context = context.applicationContext,
                                        products = allItems
                                    )

                                    withContext(Dispatchers.Main) {
                                        isPdfExporting = false
                                        Toast.makeText(
                                            context,
                                            "PDF Saved: ${pdfFile.absolutePath}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    Log.e("PDF_EXPORT", "PDF export failed", e)

                                    withContext(Dispatchers.Main) {
                                        isPdfExporting = false
                                        Toast.makeText(
                                            context,
                                            "PDF export failed: ${e.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.width(135.dp),
                        gradient = Brush.horizontalGradient(
                            colors = listOf(Color(0xFFD32940), Color(0xFF5231A7))
                        ),
                        icon = painterResource(id = R.drawable.pdf)
                    )

                }

                if (showFilterDialog) {
                    ProductFilterDialog(
                        allItems = allItems,
                        selectedSku = selectedSku,
                        selectedCategory = selectedCategory,
                        selectedProduct = selectedProduct,
                        selectedDesign = selectedDesign,
                        selectedPurity = selectedPurity,
                        onSkuChange = { selectedSku = it },
                        onCategoryChange = { selectedCategory = it },
                        onProductChange = { selectedProduct = it },
                        onDesignChange = { selectedDesign = it },
                        onPurityChange = { selectedPurity = it },
                        onDismiss = { showFilterDialog = false },
                        onClear = {
                            selectedSku = ""
                            selectedCategory = ""
                            selectedProduct = ""
                            selectedDesign = ""
                            selectedPurity = ""
                            showFilterDialog = false
                        },
                        onApply = {
                            showFilterDialog = false
                        }
                    )
                }
                if (isPdfExporting) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x88000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(48.dp)
                            )

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = "Creating PDF...",
                                color = Color.White,
                                fontFamily = poppins,
                                fontSize = 14.sp
                            )
                        }
                    }
                }


                Spacer(Modifier.height(12.dp))

                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredItems) { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedItem = item
                                        showDialog = true
                                    }
                                    .height(IntrinsicSize.Min),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.LightGray),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp) // Less vertical spacing
                                ) {

                                    ProductImageWithFallback(
                                        item = item,
                                        baseUrl = baseUrl,
                                        modifier = Modifier
                                            .size(72.dp)
                                            .align(Alignment.CenterHorizontally)
                                    )
                                    /*if (!item.imageUrl.isNullOrEmpty()) {
                                        val stored = item.imageUrl.trim()
                                            .trimEnd(',') // remove any trailing commas/spaces
                                        if (stored.startsWith("/")) {
                                            val file = File(stored)
                                            if (file.exists()) file
                                            else null
                                        } else {
                                            stored.split(",")
                                                .map { it.trim() }
                                                .filter { it.isNotEmpty() }
                                                .lastOrNull()
                                                ?.let {

                                                    AsyncImage(
                                                        model = baseUrl + it,
                                                        contentDescription = item.itemCode,
                                                        modifier = Modifier
                                                            .size(72.dp)
                                                            .align(Alignment.CenterHorizontally)
                                                    )
                                                }
                                        }

                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Photo,
                                            contentDescription = item.itemCode,
                                            tint = Color.Gray,
                                            modifier = Modifier
                                                .size(72.dp)
                                                .align(Alignment.CenterHorizontally)
                                        )
                                    }*/

                                    // Row: RFID & Item Code
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp) // Better spacing between the two
                                    ) {
                                        Text(
                                            text = localizedContext.getString(R.string.rfid_text, item.rfid?.takeIf { it.isNotBlank() } ?: "-"),
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = localizedContext.getString(R.string.itemcode, item.itemCode?.takeIf { it.isNotBlank() } ?: "-"),
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }


                                    // Row: Gross Wt & Net Wt
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = localizedContext.getString(R.string.gross_wt, item.grossWeight?.takeIf { it.isNotBlank() } ?: "-"),
                                            fontFamily = poppins,
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = localizedContext.getString(R.string.net_weight, item.netWeight?.takeIf { it.isNotBlank() } ?: "-"),
                                            fontFamily = poppins,
                                            fontSize = 9.sp,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }


                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()

                            .background(Color(0xFF2E2E2E)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            localizedContext.getString(R.string.sr_header),
                            Modifier.width(40.dp),
                            color = Color.White,
                            textAlign = TextAlign.Start,
                            fontFamily = poppins,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(scrollState),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                localizedContext.getString(R.string.product_name) to 120.dp,
                                localizedContext.getString(R.string.itemcode) to 70.dp,
                                localizedContext.getString(R.string.rfid)to 60.dp,
                                localizedContext.getString(R.string.gross_wt_header) to 60.dp,
                                localizedContext.getString(R.string.stone_weight) to 60.dp,
                                localizedContext.getString(R.string.diamond_weight) to 60.dp,
                                localizedContext.getString(R.string.net_weight)to 60.dp,
                                localizedContext.getString(R.string.category_header) to 70.dp,
                                localizedContext.getString(R.string.design) to 60.dp,
                                localizedContext.getString(R.string.purity) to 60.dp,
                                localizedContext.getString(R.string.lbl_making_per_gram) to 80.dp,
                                localizedContext.getString(R.string.lbl_making_percent) to 80.dp,
                                localizedContext.getString(R.string.lbl_fix_making) to 80.dp,
                                localizedContext.getString(R.string.lbl_fix_wastage) to 80.dp,
                                localizedContext.getString(R.string.s_amt) to 60.dp,
                                localizedContext.getString(R.string.d_amt) to 60.dp,
                                localizedContext.getString(R.string.sku) to 70.dp,
                                localizedContext.getString(R.string.lbl_epc) to 160.dp,
                                localizedContext.getString(R.string.lbl_vendor) to 80.dp
                                // "TID" to 90.dp
                            ).forEach { (label, width) ->
                                Text(
                                    label,
                                    Modifier.width(width),
                                    color = Color.White,
                                    textAlign = TextAlign.Start,
                                    fontFamily = poppins,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                        Text(
                            localizedContext.getString(R.string.edit),
                            Modifier.width(35.dp),
                            color = Color.White,
                            textAlign = TextAlign.Start,
                            fontFamily = poppins,
                            fontSize = 12.sp
                        )
                        Text(
                            localizedContext.getString(R.string.delete),
                            Modifier.width(55.dp),
                            color = Color.White,
                            textAlign = TextAlign.Start,
                            fontFamily = poppins,
                            fontSize = 12.sp
                        )
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(filteredItems) { index, item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${index + 1}",
                                    Modifier
                                        .width(40.dp)
                                        .padding(5.dp),
                                    textAlign = TextAlign.Start,
                                    fontFamily = poppins,
                                    fontSize = 12.sp
                                )

                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedItem = item
                                            showDialog = true
                                        }
                                        .horizontalScroll(scrollState),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf(
                                        item.productName to 120.dp,
                                        item.itemCode to 70.dp,
                                        item.rfid to 60.dp,
                                        item.grossWeight to 60.dp,
                                        item.stoneWeight to 60.dp,
                                        item.diamondWeight to 60.dp,
                                        item.netWeight to 60.dp,
                                        item.category to 70.dp,
                                        item.design to 60.dp,
                                        item.purity to 60.dp,
                                        item.makingPerGram to 80.dp,
                                        item.makingPercent to 80.dp,
                                        item.fixMaking to 80.dp,
                                        item.fixWastage to 80.dp,
                                        item.stoneAmount to 60.dp,
                                        item.diamondAmount to 60.dp,
                                        item.sku to 70.dp,
                                        (
                                                (item.uhfTagInfo?.epc ?: item.epc)?.takeIf {
                                                    !it.contains(
                                                        "temp",
                                                        ignoreCase = true
                                                    )
                                                } ?: ""
                                                ) to 160.dp,
                                        item.vendor to 80.dp
                                        //  (item.uhfTagInfo?.epc ?: item.epc) to 90.dp
                                    ).forEach { (value, width) ->
                                        Text(
                                            value?.ifBlank { "-" } ?: "-",
                                            Modifier.width(width),
                                            fontSize = 10.sp,
                                            textAlign = TextAlign.Start,
                                            fontFamily = poppins,
                                            maxLines = 1
                                        )
                                    }
                                }

                                val isApiActiveItem = item.Status.equals("ApiActive", ignoreCase = true)
                                IconButton(
                                    onClick = {
                                        if (isApiActiveItem) {
                                            Toast.makeText(
                                                context,
                                                localizedContext.getString(R.string.api_active_cannot_edit),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            return@IconButton
                                        }
                                        Log.d("EDIT_ITEM", "itemCode=${item.itemCode}, purity=${item.purity}, purityId=${item.purityId}")
                                        try {
                                            val currentEntry = navController.currentBackStackEntry
                                            currentEntry?.savedStateHandle?.set("item", item)
                                            navController.navigate(Screens.EditProductScreen.route)
                                        } catch (e: Exception) {
                                            Log.e("NAVIGATION", "BackStackEntry error: ${e.message}")
                                        }
                                    },
                                    modifier = Modifier.width(30.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = com.loyalstring.rfid.R.drawable.ic_edit_svg),
                                        contentDescription = "Edit",
                                        tint = if (isApiActiveItem) Color.LightGray else Color.DarkGray
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        if (isApiActiveItem) {
                                            Toast.makeText(
                                                context,
                                                localizedContext.getString(R.string.api_active_cannot_delete),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            return@IconButton
                                        }
                                        selectedItem = item
                                        showConfirmDelete = true
                                    },
                                    modifier = Modifier.width(50.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(id = com.loyalstring.rfid.R.drawable.ic_delete_svg),
                                        contentDescription = "Delete",
                                        tint = if (isApiActiveItem) Color.LightGray else Color.DarkGray
                                    )
                                }
                            }
                        }
                    }
                }
                if (showDialog && selectedItem != null) {
                    ItemDetailsDialog(item = selectedItem!!, onDismiss = { showDialog = false })
                }
                // existing product details popup
                if (showDialog && selectedItem != null) {
                    ItemDetailsDialog(item = selectedItem!!, onDismiss = { showDialog = false })
                }

// ✅ add confirmation popup here
                ConfirmDeleteDialog(
                    visible = showConfirmDelete,
                    productName = selectedItem?.productName,
                    onConfirm = {
                        if (selectedItem?.Status.equals("ApiActive", ignoreCase = true)) {
                            Toast.makeText(
                                context,
                                localizedContext.getString(R.string.api_active_cannot_delete),
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            val id = selectedItem?.bulkItemId ?: 0
                            val clientCode = employee?.clientCode
                            if (id > 0) {
                                deletingItemId = id
                                singleproductViewModel.deleetProduct(
                                    listOf(
                                        ProductDeleteModelReq(
                                            Id = id,
                                            ClientCode = clientCode.toString()
                                        )
                                    )
                                )
                            }
                        }
                        showConfirmDelete = false
                        selectedItem = null
                    },
                    onDismiss = {
                        showConfirmDelete = false
                    }
                )

            }
        }
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Loading products...",
                        color = Color.White,
                        fontFamily = poppins,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ProductFilterDialog(
    allItems: List<BulkItem>,
    selectedSku: String,
    selectedCategory: String,
    selectedProduct: String,
    selectedDesign: String,
    selectedPurity: String,
    onSkuChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onProductChange: (String) -> Unit,
    onDesignChange: (String) -> Unit,
    onPurityChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onApply: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterDropDown("SKU", selectedSku, allItems.mapNotNull { it.sku }.distinct(), onSkuChange)
                FilterDropDown("Category", selectedCategory, allItems.mapNotNull { it.category }.distinct(), onCategoryChange)
                FilterDropDown("Product", selectedProduct, allItems.mapNotNull { it.productName }.distinct(), onProductChange)
                FilterDropDown("Design", selectedDesign, allItems.mapNotNull { it.design }.distinct(), onDesignChange)
                FilterDropDown("Purity", selectedPurity, allItems.mapNotNull { it.purity }.distinct(), onPurityChange)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GradientButton(
                        text = "Cancel",
                        onClick = onClear,
                        modifier = Modifier.weight(1f)
                    )

                    GradientButton(
                        text = "Ok",
                        onClick = onApply,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun FilterDropDown(
    label: String,
    selectedValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF1F1F1), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontFamily = poppins,
            fontSize = 14.sp
        )

        Box(
            modifier = Modifier
                .weight(1.6f)
                .background(Color.White, RoundedCornerShape(4.dp))
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = selectedValue.ifBlank { "Tap to enter..." },
                fontSize = 12.sp,
                fontFamily = poppins,
                color = if (selectedValue.isBlank()) Color.Gray else Color.Black
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("All") },
                    onClick = {
                        onValueChange("")
                        expanded = false
                    }
                )

                options.forEach { value ->
                    DropdownMenuItem(
                        text = { Text(value) },
                        onClick = {
                            onValueChange(value)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}


fun ensureProductImagesFolder(context: Context): File? {
    return try {
        val imageDir = File(context.getExternalFilesDir(null), "product_images")

        if (!imageDir.exists()) {
            val created = imageDir.mkdirs()
            Log.d("ProductImageFolder", "Folder created: $created, path=${imageDir.absolutePath}")
        } else {
            Log.d("ProductImageFolder", "Folder already exists: ${imageDir.absolutePath}")
        }

        imageDir
    } catch (e: Exception) {
        Log.e("ProductImageFolder", "Folder create failed: ${e.message}", e)
        null
    }
}

@Composable
fun ConfirmDeleteDialog(
    visible: Boolean,
    productName: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Product Delete",
            fontSize = 18.sp,
            fontFamily = poppins,// 👈 set your desired size
            fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Are you sure you want to delete ${productName ?: "this item"}?",
                fontSize = 16.sp,
                fontFamily = poppins
            )
        },
        confirmButton = {
            // TextButton(onClick = onConfirm) { Text("Yes") }
            GradientButton(text = "Yes", onClick = onConfirm)
        },
        dismissButton = {
            GradientButton(text ="Cancel",onClick = onDismiss)
        }
    )
}



@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradient: Brush,
    cornerRadius: Dp = 8.dp,
    backgroundColor: Color = Color.Transparent,
    textColor: Color = Color.Black,
    icon: Painter
) {
    Box(
        modifier = modifier
            .border(
                width = 1.5.dp,
                brush = gradient, // 🔥 gradient stroke
                shape = RoundedCornerShape(cornerRadius)
            )
            .background(
                color = backgroundColor, // inner background (white/transparent)
                shape = RoundedCornerShape(cornerRadius)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}



@Composable
fun ItemDetailsDialog(
    item: BulkItem,
    onDismiss: () -> Unit
) {
    val baseUrl = "https://rrgold.loyalstring.co.in/"
    /* val imageUrl = item.imageUrl?.split(",")
         ?.lastOrNull()
         ?.trim()
         ?.let { "$baseUrl$it" }*/

    val context = LocalContext.current
    val localItemImage = remember(item.itemCode) {
        getLocalImageFileForItem(context, item.itemCode)
    }

    val directLocalPath = remember(item.imageUrl) {
        item.imageUrl
            ?.trim()
            ?.trimEnd(',')
            ?.takeIf { it.startsWith("/") }
            ?.let { File(it) }
            ?.takeIf { it.exists() }
    }

    val remoteUrl = remember(item.imageUrl) {
        item.imageUrl
            ?.trim()
            ?.trimEnd(',')
            ?.takeIf { it.isNotBlank() && !it.startsWith("/") }
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.lastOrNull()
            ?.let { "$baseUrl$it" }
    }

    var loadError by remember(item.itemCode, item.imageUrl) { mutableStateOf(false) }
    val finalImageModel: Any? = when {
        directLocalPath != null -> directLocalPath
        localItemImage != null -> localItemImage
        !loadError && !remoteUrl.isNullOrBlank() -> remoteUrl
        else -> null
    }

    var scale by remember { mutableStateOf(1f) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Item Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 14.sp,
                        fontFamily = poppins
                    )
                    TextButton(onClick = onDismiss) {
                        Text("Close", fontFamily = poppins)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (finalImageModel != null) {
                    Image(
                        painter = rememberAsyncImagePainter(
                            model = finalImageModel,
                            onError = { loadError = true }
                        ),
                        contentDescription = "Zoomable Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale
                            )
                            .pointerInput(Unit) {
                                detectTransformGestures { _, _, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 5f)
                                }
                            }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                InfoRow("Product Name", item.productName)
                InfoRow("Item Code", item.itemCode)
                InfoRow("RFID", item.rfid)
                InfoRow("G.Wt", item.grossWeight)
                InfoRow("S.Wt", item.stoneWeight)
                InfoRow("D.Wt", item.diamondWeight)
                InfoRow("N.Wt", item.netWeight)
                InfoRow("Category", item.category)
                InfoRow("Design", item.design)
                InfoRow("Purity", item.purity)
                InfoRow("Making/Gram", item.makingPerGram)
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String?) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            "$label:",
            modifier = Modifier.weight(1f),
            color = Color.DarkGray,
            fontSize = 12.sp,
            fontFamily = poppins
        )
        Text(value ?: "-", modifier = Modifier.weight(1.5f), fontSize = 12.sp, fontFamily = poppins)
    }
}

/*for local image*/
fun getLocalImageFileForItem(context: android.content.Context, itemCode: String?): File? {
    if (itemCode.isNullOrBlank()) return null

    val cleanItemCode = itemCode.trim()

    val possibleDirs = listOf(
        File(context.filesDir, "product_images"),
        File(context.getExternalFilesDir(null), "product_images")
    )

    possibleDirs.forEach { dir ->
        if (dir.exists()) {
            val file = listOf(
                File(dir, "$cleanItemCode.jpg"),
                File(dir, "$cleanItemCode.jpeg"),
                File(dir, "$cleanItemCode.png"),
                File(dir, "$cleanItemCode.webp")
            ).firstOrNull { it.exists() }

            if (file != null) return file
        }
    }

    return null
}

@Composable
fun ProductImageWithFallback(
    item: BulkItem,
    baseUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var loadError by remember(item.itemCode, item.imageUrl) { mutableStateOf(false) }
    var localImageFile by remember(item.itemCode, item.imageUrl) {
        mutableStateOf<File?>(getLocalImageFileForItem(context, item.itemCode))
    }

    val directLocalPath = remember(item.imageUrl) {
        item.imageUrl
            ?.trim()
            ?.trimEnd(',')
            ?.takeIf { it.startsWith("/") }
            ?.let { File(it) }
            ?.takeIf { it.exists() }
    }

    val remoteUrl = remember(item.imageUrl) {
        item.imageUrl
            ?.trim()
            ?.trimEnd(',')
            ?.takeIf { it.isNotBlank() && !it.startsWith("/") }
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.lastOrNull()
            ?.let { baseUrl + it }
    }

    LaunchedEffect(remoteUrl, item.itemCode) {
        if (localImageFile == null && !remoteUrl.isNullOrBlank() && !item.itemCode.isNullOrBlank()) {
            val savedFile = withContext(kotlinx.coroutines.Dispatchers.IO) {
                saveImageFromUrlToLocal(context, remoteUrl, item.itemCode!!)
            }
            if (savedFile != null) {
                localImageFile = savedFile
            }
        }
    }

    val finalModel: Any? = when {
        directLocalPath != null -> directLocalPath
        localImageFile != null -> localImageFile
        !loadError && !remoteUrl.isNullOrBlank() -> remoteUrl
        else -> null
    }

    if (finalModel != null) {
        AsyncImage(
            model = finalModel,
            contentDescription = item.itemCode,
            modifier = modifier,
            onError = {
                loadError = true
            }
        )
    } else {
        Icon(
            imageVector = Icons.Default.Photo,
            contentDescription = item.itemCode,
            tint = Color.Gray,
            modifier = modifier
        )
    }
}

fun exportProductsToPdf(
    context: Context,
    products: List<BulkItem>
): File {

    val file = File(
        context.getExternalFilesDir(null),
        "LabelledStock_${System.currentTimeMillis()}.pdf"
    )

    val writer = com.itextpdf.kernel.pdf.PdfWriter(file)
    val pdf = com.itextpdf.kernel.pdf.PdfDocument(writer)
    pdf.defaultPageSize = com.itextpdf.kernel.geom.PageSize.A4.rotate()

    val document = com.itextpdf.layout.Document(pdf)
    document.setMargins(20f, 15f, 20f, 15f)

    document.add(
        com.itextpdf.layout.element.Paragraph("Labelled Stock Report")
            .setBold()
            .setFontSize(16f)
            .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
    )

    document.add(
        com.itextpdf.layout.element.Paragraph("Total Items: ${products.size}")
            .setFontSize(10f)
            .setMarginBottom(10f)
    )

    fun headerCell(text: String): com.itextpdf.layout.element.Cell {
        return com.itextpdf.layout.element.Cell()
            .add(com.itextpdf.layout.element.Paragraph(text).setFontSize(8f).setBold())
            .setBackgroundColor(com.itextpdf.kernel.colors.ColorConstants.BLACK)
            .setFontColor(com.itextpdf.kernel.colors.ColorConstants.WHITE)
            .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
            .setPadding(4f)
    }

    fun bodyCell(text: String?): com.itextpdf.layout.element.Cell {
        return com.itextpdf.layout.element.Cell()
            .add(
                com.itextpdf.layout.element.Paragraph(
                    text?.takeIf { it.isNotBlank() } ?: "-"
                ).setFontSize(7f)
            )
            .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
            .setPadding(3f)
    }

    val headers = listOf(
        "Sr", "Product", "Item Code", "RFID", "Gross Wt", "Stone Wt",
        "Diamond Wt", "Net Wt", "Category", "Design", "Purity", "SKU", "EPC", "Vendor"
    )

    val chunkSize = 50
    val chunks = products.chunked(chunkSize)

    chunks.forEachIndexed { pageIndex, chunk ->

        val table = com.itextpdf.layout.element.Table(
            floatArrayOf(
                35f, 90f, 70f, 70f, 55f, 55f, 55f, 55f,
                70f, 70f, 55f, 65f, 120f, 80f
            )
        ).useAllAvailableWidth()

        headers.forEach { table.addHeaderCell(headerCell(it)) }

        chunk.forEachIndexed { index, item ->
            val srNo = pageIndex * chunkSize + index + 1

            table.addCell(bodyCell(srNo.toString()))
            table.addCell(bodyCell(item.productName))
            table.addCell(bodyCell(item.itemCode))
            table.addCell(bodyCell(item.rfid))
            table.addCell(bodyCell(item.grossWeight))
            table.addCell(bodyCell(item.stoneWeight))
            table.addCell(bodyCell(item.diamondWeight))
            table.addCell(bodyCell(item.netWeight))
            table.addCell(bodyCell(item.category))
            table.addCell(bodyCell(item.design))
            table.addCell(bodyCell(item.purity))
            table.addCell(bodyCell(item.sku))
            table.addCell(
                bodyCell(
                    (item.uhfTagInfo?.epc ?: item.epc)?.takeIf {
                        !it.contains("temp", ignoreCase = true)
                    }
                )
            )
            table.addCell(bodyCell(item.vendor))
        }

        document.add(table)

        if (pageIndex != chunks.lastIndex) {
            document.add(
                com.itextpdf.layout.element.AreaBreak(
                    com.itextpdf.layout.properties.AreaBreakType.NEXT_PAGE
                )
            )
        }
    }

    document.close()
    return file
}