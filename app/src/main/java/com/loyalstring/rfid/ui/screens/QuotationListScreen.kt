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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.model.quotation.QuotationListResponse
import com.loyalstring.rfid.data.model.quotation.QuotationPrintData
import com.loyalstring.rfid.data.model.quotation.QuotationPrintItem
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.ui.utils.LIST_PAGE_SIZE
import com.loyalstring.rfid.ui.utils.LazyListLoadMoreEffect
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.ui.utils.quotationPrintWastagePercent
import com.loyalstring.rfid.viewmodel.QuotationViewModel
import com.loyalstring.rfid.worker.LocaleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val QuotationActionColumnWidth = 76.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotationListScreen(
    onBack: () -> Unit,
    navController: NavHostController,
) {

    val viewModel: QuotationViewModel = hiltViewModel()
    val context = LocalContext.current
    val employee =
        remember { UserPreferences.getInstance(context).getEmployee(Employee::class.java) }
    val userPreferences = UserPreferences.getInstance(context)
    val savedLang = userPreferences.getAppLanguage().ifBlank { "en" }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = currentLocales[0]?.language ?: savedLang
    val localizedContext = LocaleHelper.applyLocale(context, currentLang)
    val challanList by viewModel.quotationList.collectAsState()
    val isLoading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    var visibleItems by remember { mutableStateOf(LIST_PAGE_SIZE) }
    var searchQuery by remember { mutableStateOf("") }

    // Fetch once
    LaunchedEffect(Unit) {
        employee?.let {
            viewModel.loadQuotationList(
                clientCode = it.clientCode.toString(),
                branchId = it.defaultBranchId
            )
        }
    }

    val filteredData = if (searchQuery.isNotEmpty()) {
        challanList.filter {
            it.quotationNo.orEmpty().contains(searchQuery, true) ||
                    it.customer?.FirstName.orEmpty().contains(searchQuery, true)
        }
    } else challanList

    val sortedData = remember(filteredData) {
        filteredData.sortedByDescending { it.id }
    }
    val visibleData = remember(sortedData, visibleItems) {
        sortedData.take(visibleItems)
    }

    // Scrollable data columns only — Actions stay fixed on the right
    val scrollableHeaderTitles = listOf(
        localizedContext.getString(R.string.header_s_no),
        localizedContext.getString(R.string.Quotation_no),
        localizedContext.getString(R.string.header_customer_name),
        localizedContext.getString(R.string.header_description),
        localizedContext.getString(R.string.header_product_name),
        localizedContext.getString(R.string.header_total_weight),
        localizedContext.getString(R.string.header_gross_weight),
        localizedContext.getString(R.string.header_stone_weight),
        localizedContext.getString(R.string.header_diamond_weight),
        localizedContext.getString(R.string.header_quantity),
        localizedContext.getString(R.string.order_date),
        localizedContext.getString(R.string.delivery_date),
    )

    val scrollableColumnWidths = listOf(
        40.dp, 68.dp, 96.dp, 72.dp, 100.dp, 64.dp,
        64.dp, 56.dp, 56.dp, 56.dp, 44.dp, 72.dp, 72.dp
    )

    Column(modifier = Modifier.fillMaxSize()) {
        GradientTopBar(
            title = localizedContext.getString(R.string.quotation_List),
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

        QuotationSearchBar(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                visibleItems = LIST_PAGE_SIZE
            },
            localizedContext=localizedContext
        )

        QuotationTable(
            navController = navController,
            scrollableHeaderTitles = scrollableHeaderTitles,
            scrollableColumnWidths = scrollableColumnWidths,
            data = visibleData,
            totalCount = sortedData.size,
            onLoadMore = {
                if (visibleItems < sortedData.size) visibleItems += LIST_PAGE_SIZE
            },
            isLoading = isLoading,
            context = context,
            localizedContext =localizedContext
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
fun QuotationTable(
    navController: NavHostController,
    scrollableHeaderTitles: List<String>,
    scrollableColumnWidths: List<Dp>,
    data: List<QuotationListResponse>,
    totalCount: Int,
    onLoadMore: () -> Unit,
    isLoading: Boolean,
    context: Context,
    localizedContext: Context
) {
    val sharedScrollState = rememberScrollState()
    val listState = rememberLazyListState()
    val customerHeader = localizedContext.getString(R.string.header_customer_name)
    val productHeader = localizedContext.getString(R.string.header_product_name)
    val actionHeader = localizedContext.getString(R.string.header_actions)

    LazyListLoadMoreEffect(
        listState = listState,
        loadedCount = data.size,
        totalCount = totalCount,
        onLoadMore = onLoadMore
    )

    Column(modifier = Modifier.fillMaxSize()) {

        // Header: scrollable columns + fixed Actions on the right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.DarkGray)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(sharedScrollState)
            ) {
                scrollableHeaderTitles.forEachIndexed { index, title ->
                    Text(
                        text = title,
                        modifier = Modifier
                            .width(scrollableColumnWidths[index])
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = poppins,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(QuotationActionColumnWidth)
                    .background(Color(0xFF424242))
                    .padding(vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = actionHeader,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = poppins,
                    maxLines = 1
                )
            }
        }

        HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)

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
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(sharedScrollState)
                        ) {
                            val designNames = challan.quotationItem.joinToString(", ") {
                                it.DesignName ?: ""
                            }

                            val values = listOf(
                                (index + 1).toString(),
                                challan.quotationNo ?: "",
                                challan.customer?.FirstName ?: "",
                                "",
                                designNames,
                                "",
                                challan.grossWt ?: "0.000",
                                challan.stoneWt ?: "0.000",
                                challan.totalDiamondWeight ?: "0.000",
                                challan.qty ?: "0",
                                challan.createdOn ?: "",
                                challan.quotationDate ?: ""
                            )

                            values.forEachIndexed { i, rawValue ->
                                val textValue = rawValue.toString()
                                val isMultiLine =
                                    scrollableHeaderTitles.getOrNull(i) == customerHeader ||
                                        scrollableHeaderTitles.getOrNull(i) == productHeader

                                Text(
                                    text = textValue,
                                    modifier = Modifier
                                        .width(scrollableColumnWidths[i])
                                        .padding(horizontal = 4.dp, vertical = 4.dp),
                                    maxLines = if (isMultiLine) 3 else 1,
                                    style = LocalTextStyle.current.copy(
                                        color = Color.Black,
                                        fontSize = 11.sp,
                                        fontFamily = poppins,
                                        lineHeight = 13.sp
                                    )
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .width(QuotationActionColumnWidth)
                                .background(Color(0xFFFAFAFA)),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    CoroutineScope(Dispatchers.Main).launch {
                                        val quotationNo = challan.quotationNo ?: ""
                                        Log.d(
                                            "Edit",
                                            "EDIT Screen $quotationNo challan.Id ${challan.id}"
                                        )
                                        navController.navigate(
                                            "updateQuotationScreen/${challan.id}/$quotationNo"
                                        )
                                    }
                                },
                                modifier = Modifier.size(36.dp)
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
                                    CoroutineScope(Dispatchers.Main).launch {
                                        val sampleOutNoSafe = challan.quotationNo ?: ""
                                        val printData = challan.toQuotationPrintData(context)
                                        GenerateQuotationPdf(context, printData)
                                        Log.d(
                                            "Print",
                                            "PRINT Screen $sampleOutNoSafe challan.Id ${challan.id}"
                                        )
                                    }
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.print_svg),
                                    contentDescription = localizedContext.getString(R.string.cd_print),
                                    tint = Color(0xFF37474F),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE0E0E0), thickness = 1.dp)
                }
            }
        }
    }
}


// ✅ Replace "QuotationListItem" with your challan model class name
fun QuotationListResponse.toQuotationPrintData(context: Context): QuotationPrintData {

    // ✅ date safe format (if you already have date string, directly use it)
    fun formatDateSafe(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        // if raw = "2025-12-24T10:20:30" -> take 2025-12-24
        return raw.take(10)
    }

    // ✅ Build print items from quotationItem list
    val printItems: List<QuotationPrintItem> =
        (this.quotationItem ?: emptyList()).filterNotNull().map { itItem ->

            QuotationPrintItem(
                itemCode = itItem.ItemCode.orEmpty().ifBlank { "-" },
                rfidNo = itItem.RFIDCode.orEmpty().ifBlank { "-" },
                grossWt = itItem.GrossWt ?: "0.0",
                netWt = itItem.NetWt ?: "0.0",
                pcs = itItem.Pieces ?: "1",
                stoneWt = itItem.TotalStoneWeight ?: "0.000",
                stoneAmt = itItem.StoneAmt ?: itItem.StoneAmount ?: itItem.TotalStoneAmount ?: "0.00",
                wastagePercent = itItem.quotationPrintWastagePercent(),
                amount = itItem.TotalItemAmount ?: itItem.itemAmt ?: itItem.TotalAmount ?: "0.00"
            )
        }

    val total = printItems.sumOf { it.amount?.toDoubleOrNull() ?: 0.0 }.toString()

    return QuotationPrintData(
        ownerName = "VTjewellers_Rajapur",          // ✅ or fetch from prefs/branch/company
        ownerAddress = "VT jewellers Near old MG road",
        ownerContact = "9342232444",

        quotationNo = this.quotationNo?.toString() ?: "",
        date = formatDateSafe(this.date),           // ✅ replace this.date with your field
       // salesMan = this.salesMan ?: "",
        //remark = this.remark ?: "",

        customerName = "${this.customer?.FirstName.orEmpty()} ${this.lastName.orEmpty()}".trim(),
        customerMobile = this.customer?.Mobile ?: "",
        customerAddress = this.customer?.CurrAddTown ?: "",

        items = printItems,
        totalAmount = total,

      /*  cgst = this.cgst ?: "0.00",
        sgst = this.sgst ?: "0.00",
        igst = this.igst ?: "0.00"*/
    )
}

/*private fun QuotationListResponse.toQuotationPrintData(
    context: Context
) {
}*/

/*fun SampleOutListResponse.toSampleOutPrintData(context: Context): SampleOutPrintData {
    val org = UserPreferences.getInstance(context).getOrganization()
    val companyName = org?.toString().orEmpty() // agar model me Name field hai to use karo

    val items = (this.IssueItems ?: emptyList()).map { it ->
        SampleOutPrintItem(
            itemDetails = listOfNotNull(it.CategoryName, it.ProductName, it.DesignName, it.PurityName)
                .filter { s -> s.isNotBlank() }
                .joinToString(" - "),
            grossWt = it.GrossWt ?: "0.000",
            stoneWt = it.StoneWeight ?: "0.000",
            diamondWt = it.DiamondWeight ?: "0.000",
            netWt = it.NetWt ?: "0.000",
            pieces = it.Pieces ?: "1",
            status = "Sample Out",
            //imageUrl = it.Image // agar backend me image aa raha hai
        )
    }

    return SampleOutPrintData(
        companyName = companyName,
        customerName = listOfNotNull(this.Customer?.FirstName, this.Customer?.LastName).joinToString(" ").trim(),
        addressCity = this.Customer?.CurrAddTown.orEmpty(),
        contactNo = this.Customer?.Mobile.orEmpty(),
        sampleOutNo = this.SampleOutNo.orEmpty(),
        date = formatCreatedOn(this.CreatedOn), // tumhara existing fn
        returnDate = this.ReturnDate.orEmpty(),
        items = items
    )
}*/

@Composable
fun QuotationSearchBar(value: String, onValueChange: (String) -> Unit, localizedContext: Context) {
    val searchTextStyle = LocalTextStyle.current.copy(
        color = Color.Black,
        fontSize = 13.sp,
        fontFamily = poppins,
        lineHeight = 16.sp
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF2F2F2))
            .border(1.dp, Color.Gray, RoundedCornerShape(10.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = localizedContext.getString(R.string.cd_search),
            modifier = Modifier
                .padding(start = 10.dp)
                .size(18.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                maxLines = 1,
                textStyle = searchTextStyle,
                cursorBrush = SolidColor(Color.Gray),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 2.dp,
                        end = if (value.isNotEmpty()) 4.dp else 8.dp
                    ),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search)
            )
            if (value.isEmpty()) {
                Text(
                    text = localizedContext.getString(R.string.search_hint_sample_out),
                    color = Color.Gray,
                    fontSize = 13.sp,
                    fontFamily = poppins,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 2.dp, end = 8.dp)
                )
            }
        }
        if (value.isNotEmpty()) {
            IconButton(
                onClick = { onValueChange("") },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = localizedContext.getString(R.string.cd_clear),
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
