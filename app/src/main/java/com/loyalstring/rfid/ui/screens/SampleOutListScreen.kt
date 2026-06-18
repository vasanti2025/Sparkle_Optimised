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
import com.loyalstring.rfid.data.model.sampleOut.SampleOutListResponse
import com.loyalstring.rfid.data.model.sampleOut.SampleOutPrintData
import com.loyalstring.rfid.data.model.sampleOut.SampleOutPrintItem
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.SampleOutViewModel
import com.loyalstring.rfid.worker.LocaleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleOutListScreen(
    onBack: () -> Unit,
    navController: NavHostController,
) {

    val viewModel: SampleOutViewModel = hiltViewModel()
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

    var visibleItems by remember { mutableStateOf(10) }
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

    val visibleData = filteredData
        .sortedByDescending { it.Id }
        .take(visibleItems)

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
        45.dp, 60.dp, 100.dp, 80.dp, 90.dp, 90.dp, 120.dp,
        70.dp, 70.dp, 70.dp, 70.dp, 50.dp, 90.dp
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
                visibleItems = 10
            },
            localizedContext=localizedContext
        )

        SampleOutTable(
            navController = navController,
            headerTitles = headerTitles,
            columnWidths = columnWidths,
            data = visibleData,
            onLoadMore = {
                if (visibleItems < filteredData.size) visibleItems += 10
            },
            isLoading = isLoading,
            context = context,
            localizedContext=localizedContext
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
    onLoadMore: () -> Unit,
    isLoading: Boolean,
    context: Context,
    localizedContext: Context
) {
    val sharedScrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {

        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.DarkGray)
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(sharedScrollState)
                    .weight(1f)
            ) {
                headerTitles.dropLast(1).forEachIndexed { index, title ->
                    Text(
                        text = title,
                        modifier = Modifier
                            .width(columnWidths[index])
                            .padding(6.dp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = poppins,
                        fontSize = 12.sp
                    )
                }
            }

            // Fixed Action Header
            Box(
                modifier = Modifier
                    .width(columnWidths.last())
                    .height(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = localizedContext.getString(R.string.header_actions),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = poppins
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(data) { index, challan ->

                    // 🔹 Trigger auto load more when reaching last item
                    if (index == data.lastIndex) {
                        onLoadMore()
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Scrollable content row
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(sharedScrollState)
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
                                val textValue = rawValue?.toString().orEmpty()

                                val isMultiLine =
                                    headerTitles.getOrNull(i) == localizedContext.getString(R.string.header_product_name) ||
                                            headerTitles.getOrNull(i) == localizedContext.getString(R.string.header_customer_name)

                                Text(
                                    text = textValue,
                                    modifier = Modifier
                                        .width(columnWidths[i])
                                        .padding(6.dp),
                                    maxLines = if (isMultiLine) 5 else 1,
                                    style = LocalTextStyle.current.copy(
                                        color = Color.Black,
                                        fontSize = 11.sp,
                                        fontFamily = poppins,
                                        lineHeight = 14.sp
                                    )
                                )
                            }
                        }

                        // Fixed Actions
                        Row(
                            modifier = Modifier
                                .width(columnWidths.last())
                                .height(40.dp),
                            horizontalArrangement = Arrangement.spacedBy(
                                6.dp,
                                Alignment.CenterHorizontally
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Edit Button
                            IconButton(onClick = {
                                CoroutineScope(Dispatchers.Main).launch {
                                    val sampleOutNoSafe = challan.SampleOutNo ?: ""
                                    Log.d(
                                        "Edit",
                                        "EDIT Screen $sampleOutNoSafe challan.Id ${challan.Id}"
                                    )
                                    navController.navigate("updateSampleOutScreen/${challan.Id}/$sampleOutNoSafe")
                                }
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_edit_svg),
                                    contentDescription = localizedContext.getString(R.string.cd_edit),
                                    tint = Color(0xFF37474F),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Print Button
                            IconButton(onClick = {
                                CoroutineScope(Dispatchers.Main).launch {
                                    val sampleOutNoSafe = challan.SampleOutNo ?: ""
                                    val data = challan.toSampleOutPrintData(context)
                                    generateSampleOutPrintPdf(context, data)
                                    Log.d("Print", "PRINT Screen $sampleOutNoSafe challan.Id ${challan.Id}")
                                }
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.print_svg),
                                    contentDescription = localizedContext.getString(R.string.cd_print),
                                    tint = Color(0xFF37474F),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Divider(color = Color(0xFFE0E0E0))
                }
            }
        }
    }
}

fun SampleOutListResponse.toSampleOutPrintData(context: Context): SampleOutPrintData {
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
