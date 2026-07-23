package com.loyalstring.rfid.ui.screens

import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.model.sampleOut.SampleOutListResponse
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.ui.utils.LIST_PAGE_SIZE
import com.loyalstring.rfid.ui.utils.LazyListLoadMoreEffect
import com.loyalstring.rfid.ui.utils.DEFAULT_PRODUCT_IMAGE_BASE_URL
import com.loyalstring.rfid.ui.utils.ProductImageWithAllFallbacks
import com.loyalstring.rfid.ui.utils.SAMPLE_OUT_ITEM_IMAGES_ENABLED
import com.loyalstring.rfid.ui.utils.SampleOutItemImageUi
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.ProductListViewModel
import com.loyalstring.rfid.viewmodel.SampleOutViewModel
import com.loyalstring.rfid.worker.LocaleHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleOutListScreen(
    onBack: () -> Unit,
    navController: NavHostController,
) {

    val viewModel: SampleOutViewModel = hiltViewModel()
    val productListViewModel: ProductListViewModel = hiltViewModel()
    val context = LocalContext.current
    val employee =
        remember { UserPreferences.getInstance(context).getEmployee(Employee::class.java) }
    val userPreferences = UserPreferences.getInstance(context)
    val savedLang = userPreferences.getAppLanguage().ifBlank { "en" }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = currentLocales[0]?.language ?: savedLang
    val localizedContext = LocaleHelper.applyLocale(context, currentLang)
    val challanList by viewModel.sampleOutList.collectAsState()
    val isLoading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    val bulkItems by productListViewModel.productList.collectAsState()

    var visibleItems by remember { mutableStateOf(LIST_PAGE_SIZE) }
    var searchQuery by remember { mutableStateOf("") }

    // Fetch once
    LaunchedEffect(Unit) {
        employee?.let {
            viewModel.loadSampleOut(it.clientCode ?: "", "SampleOut")
        }
    }

    val filteredData = if (searchQuery.isNotEmpty()) {
        challanList.filter {
            it.SampleOutNo.orEmpty().contains(searchQuery, true) ||
                    it.Customer?.FirstName.orEmpty().contains(searchQuery, true)
        }
    } else challanList

    val sortedData = remember(filteredData) {
        filteredData.sortedByDescending { it.Id }
    }
    val visibleData = remember(sortedData, visibleItems) {
        sortedData.take(visibleItems)
    }

    // ✅ Localized column headers
    val headerTitles = listOf(
        localizedContext.getString(R.string.header_s_no),
        localizedContext.getString(R.string.header_so_no),
        localizedContext.getString(R.string.header_customer_name),
        localizedContext.getString(R.string.header_date),
        localizedContext.getString(R.string.header_return_date),
        localizedContext.getString(R.string.header_description),
        localizedContext.getString(R.string.header_product_name),
        localizedContext.getString(R.string.header_total_weight),
        localizedContext.getString(R.string.header_gross_weight),
        localizedContext.getString(R.string.header_stone_weight),
        localizedContext.getString(R.string.header_diamond_weight),
        localizedContext.getString(R.string.header_quantity),
        localizedContext.getString(R.string.header_action)
    )

    val columnWidths = listOf(
        45.dp, 70.dp, 100.dp, 80.dp, 90.dp, 90.dp, 120.dp,
        70.dp, 70.dp, 70.dp, 70.dp, 50.dp,
        if (SAMPLE_OUT_ITEM_IMAGES_ENABLED) 120.dp else 90.dp
    )

    Column(modifier = Modifier.fillMaxSize()) {
        GradientTopBar(
            title = localizedContext.getString(R.string.sample_out_list_title),
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = localizedContext.getString(R.string.cd_back),
                        tint = Color.White
                    )
                }
            },
            titleTextSize = 20.sp
        )

        SampleOutSearchBar(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                visibleItems = LIST_PAGE_SIZE
            },
            localizedContext=localizedContext
        )

        SampleOutTable(
            navController = navController,
            headerTitles = headerTitles,
            columnWidths = columnWidths,
            data = visibleData,
            totalCount = sortedData.size,
            onLoadMore = {
                if (visibleItems < sortedData.size) visibleItems += LIST_PAGE_SIZE
            },
            isLoading = isLoading,
            context = context,
            localizedContext = localizedContext,
            viewModel = viewModel,
            bulkItems = bulkItems,
        )

        if (error != null) {
            Text(
                text = error ?: localizedContext.getString(R.string.error_loading_list),
                color = Color.Red,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun SampleOutTable(
    navController: NavHostController,
    headerTitles: List<String>,
    columnWidths: List<Dp>,
    data: List<SampleOutListResponse>,
    totalCount: Int,
    onLoadMore: () -> Unit,
    isLoading: Boolean,
    context: Context,
    localizedContext: Context,
    viewModel: SampleOutViewModel,
    bulkItems: List<com.loyalstring.rfid.data.local.entity.BulkItem>,
) {
    val sharedScrollState = rememberScrollState()
    val listState = rememberLazyListState()
    var imageDialogItems by remember { mutableStateOf<List<SampleOutItemImageUi>?>(null) }
    var imageDialogTitle by remember { mutableStateOf("") }
    var isLoadingImages by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    if (SAMPLE_OUT_ITEM_IMAGES_ENABLED) {
        imageDialogItems?.let { items ->
            SampleOutItemsImageDialog(
                title = imageDialogTitle,
                items = items,
                onDismiss = { imageDialogItems = null },
            )
        }
    }

    LazyListLoadMoreEffect(
        listState = listState,
        loadedCount = data.size,
        totalCount = totalCount,
        onLoadMore = onLoadMore
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.DarkGray)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(sharedScrollState)
            ) {
                headerTitles.dropLast(1).forEachIndexed { index, title ->
                    Box(
                        modifier = Modifier
                            .width(columnWidths[index])
                            .height(36.dp)
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = poppins,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .width(columnWidths.last())
                    .height(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = headerTitles.last(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = poppins,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(data) { index, challan ->

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(sharedScrollState),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val productNames = challan.IssueItems
                                .mapNotNull { item ->
                                    item.ProductName
                                        ?.takeIf { it.isNotBlank() }
                                        ?: item.DesignName?.takeIf { it.isNotBlank() }
                                        ?: item.ItemCode?.takeIf { it.isNotBlank() }
                                }
                                .joinToString(", ")

                            val values = listOf(
                                (index + 1).toString(),
                                challan.SampleOutNo ?: "",
                                challan.Customer?.FirstName ?: "",
                                formatCreatedOn(challan.CreatedOn),
                                challan.ReturnDate ?: "",
                                challan.Description ?: "",
                                productNames,
                                challan.TotalWt ?: "0.000",
                                challan.TotalGrossWt ?: "0.000",
                                challan.TotalStoneWeight ?: "0.000",
                                challan.TotalDiamondWeight ?: "0.000",
                                challan.Quantity ?: "0"
                            )

                            values.forEachIndexed { i, rawValue ->
                                val textValue = rawValue.toString()
                                val isMultiLine =
                                    headerTitles.getOrNull(i) == localizedContext.getString(R.string.header_product_name) ||
                                            headerTitles.getOrNull(i) == localizedContext.getString(R.string.header_customer_name)

                                Box(
                                    modifier = Modifier
                                        .width(columnWidths[i])
                                        .heightIn(min = 52.dp)
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = textValue,
                                        maxLines = if (isMultiLine) 4 else 2,
                                        overflow = TextOverflow.Ellipsis,
                                        style = LocalTextStyle.current.copy(
                                            color = Color.Black,
                                            fontSize = 11.sp,
                                            fontFamily = poppins,
                                            lineHeight = 14.sp
                                        )
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .width(columnWidths.last())
                                .height(52.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (SAMPLE_OUT_ITEM_IMAGES_ENABLED) {
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            isLoadingImages = true
                                            val items = viewModel.buildSampleOutItemImages(
                                                context = context,
                                                challan = challan,
                                                cachedBulkItems = bulkItems,
                                            )
                                            imageDialogTitle = challan.SampleOutNo.orEmpty()
                                            imageDialogItems = items
                                            isLoadingImages = false
                                        }
                                    },
                                    modifier = Modifier.size(28.dp),
                                    enabled = !isLoadingImages
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_action_eye),
                                        contentDescription = "View",
                                        tint = Color(0xFF37474F),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    scope.launch {
                                        val sampleOutNoSafe = challan.SampleOutNo ?: ""
                                        navController.navigate("updateSampleOutScreen/${challan.Id}/$sampleOutNoSafe")
                                        Log.d("Edit", "EDIT Screen $sampleOutNoSafe challan.Id ${challan.Id}")
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_edit_svg),
                                    contentDescription = localizedContext.getString(R.string.cd_edit),
                                    tint = Color(0xFF37474F),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    scope.launch {
                                        val sampleOutNoSafe = challan.SampleOutNo ?: ""
                                        val printData = viewModel.buildSampleOutPrintData(
                                            context = context,
                                            challan = challan,
                                            cachedBulkItems = bulkItems,
                                        )
                                        generateSampleOutPrintPdf(context, printData)
                                        Log.d("Print", "PRINT Screen $sampleOutNoSafe challan.Id ${challan.Id}")
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.print_svg),
                                    contentDescription = localizedContext.getString(R.string.cd_print),
                                    tint = Color(0xFF37474F),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE0E0E0))
                }
            }
        }
    }
}

@Composable
private fun SampleOutItemsImageDialog(
    title: String,
    items: List<SampleOutItemImageUi>,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (title.isBlank()) "Sample Out Items" else "Sample Out: $title",
                        fontFamily = poppins,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onDismiss) {
                        Text("Close", fontFamily = poppins)
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (items.isEmpty()) {
                        Text("No items found.", fontFamily = poppins, fontSize = 13.sp)
                    } else {
                        items.forEachIndexed { index, item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF7F7F7))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = "${index + 1}. ${item.title}",
                                    fontFamily = poppins,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                SampleOutItemImageContent(item = item)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SampleOutItemImageContent(item: SampleOutItemImageUi) {
    ProductImageWithAllFallbacks(
        imageUrl = item.imageUrl,
        itemCode = item.itemCode,
        designName = item.designName,
        baseUrl = DEFAULT_PRODUCT_IMAGE_BASE_URL,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White),
        contentDescription = item.title,
        placeholder = painterResource(R.drawable.add_photo),
        error = painterResource(R.drawable.add_photo),
        cacheRemoteToLocal = true,
    )
}

@Composable
fun SampleOutSearchBar(value: String, onValueChange: (String) -> Unit, localizedContext: Context) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .height(45.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF2F2F2))
            .border(1.dp, Color.Gray, RoundedCornerShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = localizedContext.getString(R.string.cd_search),
            modifier = Modifier.padding(start = 12.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = Color.Black, fontSize = 15.sp),
                cursorBrush = SolidColor(Color.Gray),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = if (value.isNotEmpty()) 36.dp else 12.dp),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search)
            )
            if (value.isEmpty()) {
                Text(
                    text = localizedContext.getString(R.string.search_hint_sample_out),
                    color = Color.Gray,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 8.dp)
                )
            }
            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                        .size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = localizedContext.getString(R.string.cd_clear),
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}
