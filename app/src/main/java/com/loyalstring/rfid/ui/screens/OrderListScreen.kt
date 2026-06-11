package com.loyalstring.rfid.ui.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.model.order.CustomOrderResponse
import com.loyalstring.rfid.data.model.order.ItemCodeResponse
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.ui.utils.NetworkUtils
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.OrderViewModel
import com.loyalstring.rfid.worker.LocaleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderLisrScreen(
    onBack: () -> Unit,
    navController: NavHostController,
    userPreferences: UserPreferences
) {
    val orderViewModel: OrderViewModel = hiltViewModel()
    val context = LocalContext.current
    val employee =
        remember { UserPreferences.getInstance(context).getEmployee(Employee::class.java) }

    val allItems by orderViewModel.getAllOrderList.collectAsState()
    val isLoading by orderViewModel.isLoading.collectAsState(false)
    var visibleItems by remember { mutableStateOf(7000) }
    var searchQuery by remember { mutableStateOf("") }

    val userPreferences = UserPreferences.getInstance(context)
    val savedLang = userPreferences.getAppLanguage().ifBlank { "en" }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = currentLocales[0]?.language ?: savedLang
    val localizedContext = LocaleHelper.applyLocale(context, currentLang)

    LaunchedEffect(Unit) {
        employee?.clientCode?.let {
            orderViewModel.fetchAllOrderListFromApi(ClientCodeRequest(it))
        }
    }

    LaunchedEffect(Unit) {
        employee?.clientCode?.let { clientCode ->

            orderViewModel.getAllItemCodeList(ClientCodeRequest(clientCode))

        }
    }
    val itemCodeList by orderViewModel.itemCodeResponse.collectAsState()

    val filteredData = if (searchQuery.isNotEmpty()) {
        allItems.filter {
            it.OrderNo.orEmpty().contains(searchQuery, true) ||
                    it.ProductName.orEmpty().contains(searchQuery, true) ||
                    it.Customer?.FirstName.orEmpty().contains(searchQuery, true)
        }
    } else allItems

  //  val visibleData = filteredData.sortedByDescending { it.CustomOrderId }

    val visibleData = filteredData.sortedWith(
        compareByDescending<CustomOrderResponse> { it.CustomOrderId == 0 } // local first (true > false)
            .thenByDescending { it.CreatedOn } // then latest first
    )

    val headerTitles = listOf(
        localizedContext.getString(R.string.order_no) ,
        localizedContext.getString(R.string.customer_name),
        localizedContext.getString(R.string.contact),
        localizedContext.getString(R.string.product),
        localizedContext.getString(R.string.rfid_code),
        localizedContext.getString(R.string.tid_number),
        localizedContext.getString(R.string.branch),
        localizedContext.getString(R.string.qty),
        localizedContext.getString(R.string.header_total_weight),
        localizedContext.getString(R.string.header_gross_weight),
        localizedContext.getString(R.string.header_net_weight),
        localizedContext.getString(R.string.fine_metal),
        localizedContext.getString(R.string.taxable_amount),
        localizedContext.getString(R.string.total_amount),
        localizedContext.getString(R.string.order_date),
        localizedContext.getString(R.string.status)

    )

    val columnWidths = listOf(
        45.dp,   // Order No
        60.dp,  // Customer Name
        70.dp,  // Contact
        110.dp,  // Product
        80.dp,  // RFID Code
        70.dp,  // TID Number
        70.dp,  // Branch
        40.dp,   // Qty
        60.dp, // tot wt
        60.dp,   // G.Wt
        60.dp,   // N.Wt,
        70.dp,   //finemetal
        100.dp,  // Taxable Amt
        80.dp,  // Total Amt
        80.dp,  // Order Date
        105.dp   // Status
    )


    Column(modifier = Modifier.fillMaxSize()) {
        GradientTopBar(
            title =  localizedContext.getString(R.string.order_list),
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription =  localizedContext.getString(R.string.back),
                        tint = Color.White
                    )
                }
            },
            selectedCount = 0,
            titleTextSize = 20.sp
        )

        SearchBar(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                visibleItems = 10
            },
            localizedContext = localizedContext
        )

        OrderTableWithPagination(
            navController = navController,
            headerTitles = headerTitles,
            columnWidths=columnWidths,
            data = visibleData,
            onLoadMore = {
                if (visibleItems < filteredData.size) {
                    visibleItems += 10
                }
            },
            isLoading = isLoading,
            context = context,
            employee = employee,
            itemCodeList = itemCodeList,
            localizedContext= localizedContext
        )
    }
}

@Composable
fun SearchBar(value: String, onValueChange: (String) -> Unit,localizedContext:Context) {
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
            contentDescription =  localizedContext.getString(R.string.search),
            modifier = Modifier.padding(start = 12.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = Color.Black, fontSize = 16.sp),
                cursorBrush = SolidColor(Color.Gray),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = if (value.isNotEmpty()) 36.dp else 12.dp),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search)
            )
            if (value.isEmpty()) {
                Text(
                    text =  localizedContext.getString(R.string.search_placeholder),
                    color = Color.Gray,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 4.dp)
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
                    Icon(Icons.Default.Close, contentDescription =  localizedContext.getString(R.string.clear), tint = Color.Gray)
                }
            }
        }
    }
}
// Replace the entire OrderTableWithPagination method with this updated version
@Composable
fun OrderTableWithPagination(
    navController: NavHostController,
    headerTitles: List<String>,
    columnWidths: List<Dp>,
    data: List<CustomOrderResponse>,
    onLoadMore: () -> Unit,
    isLoading: Boolean,
    context: Context,
    employee: Employee?,
    itemCodeList: List<ItemCodeResponse>,
    localizedContext:Context

) {
    val sharedScrollState = rememberScrollState()
    val orderViewModel: OrderViewModel = hiltViewModel()
    var orderToDelete by remember { mutableStateOf<CustomOrderResponse?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Updated header layout: scrollable data + fixed "Actions"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.DarkGray)
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(sharedScrollState)
            ) {
                headerTitles.forEachIndexed { index, title ->
                    Text(
                        text = title,
                        modifier = Modifier
                            .width(columnWidths[index])
                            .padding(6.dp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = poppins,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }

            Box(
                modifier = Modifier
                    .width(90.dp) // same as actions column width
                    .height(32.dp), // same height as header row
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text =  localizedContext.getString(R.string.actions),
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
                items(data) { row ->

                    // === Calculations ===
                    val totalWt = row.CustomOrderItem.sumOf {
                        val totalwt = it.TotalWt?.toFloatOrNull() ?: 0f
                        val qty = it.Quantity?.toIntOrNull() ?: 0
                        (totalwt * qty).toDouble()
                    }.toString()

                    val totalgrWt = row.CustomOrderItem.sumOf {
                        val totalgrWt = it.GrossWt?.toFloatOrNull() ?: 0f
                        val qty = it.Quantity?.toIntOrNull() ?: 0
                        (totalgrWt * qty).toDouble()
                    }.toString()

                    val totalNetWt = row.CustomOrderItem.sumOf {
                        val netWt = it.NetWt?.toFloatOrNull() ?: 0f
                        val qty = it.Quantity?.toIntOrNull() ?: 0
                        (netWt * qty).toDouble()
                    }.toString()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Scrollable Data Row
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(sharedScrollState)
                        ) {
                            val values = listOf(
                                row.OrderNo ?: "",
                                row.Customer?.FirstName ?: "",
                                row.Customer?.Mobile ?: "",
                                row.CustomOrderItem.joinToString(", ") { it.ProductName.orEmpty() },
                                row.RfidCode ?: "",
                                row.TidNumber ?: "",
                                row.CustomOrderItem.joinToString(", ") { it.BranchName.orEmpty() },
                                row.CustomOrderItem.sumOf { it.Quantity?.toIntOrNull() ?: 0 }.toString(),
                                totalWt,
                                totalgrWt,
                                totalNetWt,
                                row.TotalFineMetal ?: "",
                                row.TotalNetAmount ?: "",
                                row.TotalAmount ?: "",
                                formatDate(row.OrderDate),
                                formatDate(row.OrderStatus)
                            )

                            values.forEachIndexed { index, value ->
                                Text(
                                    text = value,
                                    modifier = Modifier
                                        .width(columnWidths[index])
                                        .padding(6.dp),
                                    fontSize = 10.sp,
                                    fontFamily = poppins,
                                    maxLines = 1
                                )
                            }
                        }

                        // Fixed Actions column (separate)
                        Row(
                            modifier = Modifier
                                .width(90.dp) // 👈 consistent width for Actions
                                .height(40.dp),

                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                CoroutineScope(Dispatchers.Main).launch {
                                    generateTablePdfWithImages(context, row,localizedContext)
                                }
                            },
                                modifier = Modifier.size(28.dp) ) {
                                Icon(Icons.Default.Print, contentDescription =  localizedContext.getString(R.string.print), tint = Color.DarkGray,
                                    modifier = Modifier.size(18.dp))
                            }



                            IconButton(onClick = {
                                /*navController.currentBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("editOrder", row)

                                navController.navigate("order_screen")*/
                               // editOrder(navController, row)
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set("editOrder", row)

                                // Navigate back
                                navController.popBackStack()
                            }, modifier = Modifier.size(28.dp) ) {
                                Icon(Icons.Default.Edit, contentDescription =  localizedContext.getString(R.string.edit), tint = Color.DarkGray,
                                    modifier = Modifier.size(18.dp))
                            }

                            IconButton(onClick = {
                                orderToDelete = row
                                Log.d("deleting item","delete"+row.CustomOrderId)
                                /*employee?.clientCode?.let {
                                    orderViewModel.deleteOrders(
                                        ClientCodeRequest(it),
                                        row.CustomOrderId
                                    ) { isSuccess ->
                                        Toast.makeText(
                                            context,
                                            if (isSuccess) "Order Deleted Successfully" else "Failed to delete",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        if (isSuccess) {
                                            orderViewModel.removeOrderById(row.CustomOrderId)
                                        }
                                    }
                                }*/
                            }, modifier = Modifier.size(28.dp) ) {
                                Icon(Icons.Default.Delete, contentDescription =  localizedContext.getString(R.string.delete), tint = Color.DarkGray,
                                    modifier = Modifier.size(18.dp))
                            }
                        }

                        if (orderToDelete != null) {
                            AlertDialog(
                                onDismissRequest = { orderToDelete = null },
                                title = { Text( localizedContext.getString(R.string.confirm_delete)) },
                                text = { Text( localizedContext.getString(R.string.delete_confirmation_message)) },
                                confirmButton = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        GradientButton(
                                            text =  localizedContext.getString(R.string.cancel),
                                            onClick = { orderToDelete = null },
                                            modifier = Modifier.weight(1f)
                                        )
                                        GradientButton(
                                            text = localizedContext.getString(R.string.yes),
                                            onClick = {
                                                orderToDelete?.let { order ->
                                                    Log.d("customer order id","onclick ok " + order.CustomOrderId)
                                                    val isOnline = NetworkUtils.isNetworkAvailable(context)
                                                    if(isOnline) {
                                                        employee?.clientCode?.let {
                                                            orderViewModel.deleteOrders(
                                                                ClientCodeRequest(it),
                                                                order.CustomOrderId
                                                            ) { isSuccess ->
                                                                Toast.makeText(
                                                                    context,
                                                                    if (isSuccess)  localizedContext.getString(R.string.order_deleted_success) else  localizedContext.getString(R.string.delete_failed_message),
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                                if (isSuccess) {
                                                                    orderViewModel.removeOrderById(
                                                                        order.CustomOrderId
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }else
                                                    {

                                                        Log.d("orderListscreen","Order.orderNo"+order.OrderNo.toString())
                                                       orderViewModel.deleteOrderOffline(order)
                                                        orderViewModel.enqueuePendingSync(context)
                                                    }
                                                }
                                                orderToDelete = null
                                            },
                                            modifier = Modifier.weight(1f)
                                        )


                                    }
                                },
                                dismissButton = {} // 👈 leave empty since both buttons are inside confirmButton
                            )
                        }

                    }
                    }
                }
            }

        }
    }






fun formatDate(dateString: String): String {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = format.parse(dateString)
        val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        dateString
    }
}