package com.loyalstring.rfid.ui.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Mediation
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.model.order.CustomOrderResponse
import com.loyalstring.rfid.data.model.order.ItemCodeResponse
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.OrderViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
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

    val visibleData = filteredData.sortedByDescending { it.CustomOrderId }

    val headerTitles = listOf(
        "O.No",
        "Name",
        "Contact",
        "Product",
        "Branch",
        "Qty",
        "Tot Wt",
        "G wt",
        "N.Wt",
        "Fine Metal",
        "Taxable Amt",
        "Total Amt",
        "Order Date",
        "Status"

    )

    val columnWidths = listOf(
        45.dp,   // Order No
        60.dp,  // Customer Name
        70.dp,  // Contact
        110.dp,  // Product
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
            title = "OrderList",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            actions = {},
            showCounter = false,
            selectedCount = 0,
            onCountSelected = {}
        )

        SearchBar(searchQuery) {
            searchQuery = it
            visibleItems = 10
        }

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
            itemCodeList = itemCodeList
        )
    }
}

@Composable
fun SearchBar(value: String, onValueChange: (String) -> Unit) {
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
            contentDescription = "Search",
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
                    text = "Search by order no. and name",
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
                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
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
    itemCodeList: List<ItemCodeResponse>

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
                    text = "Actions",
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
                                row.CustomOrderItem.joinToString(", ") { it.ProductName ?: "" },
                                row.CustomOrderItem.joinToString(", ") { it.BranchName ?: "" },
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
                                    generateTablePdfWithImages(context, row)
                                }
                            },
                                modifier = Modifier.size(28.dp) ) {
                                Icon(Icons.Default.Print, contentDescription = "Print", tint = Color.DarkGray,
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
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.DarkGray,
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
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.DarkGray,
                                    modifier = Modifier.size(18.dp))
                            }
                        }

                        if (orderToDelete != null) {
                            AlertDialog(
                                onDismissRequest = { orderToDelete = null },
                                title = { Text("Confirm Delete") },
                                text = { Text("Are you sure you want to delete this order?") },
                                confirmButton = {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        GradientButton(
                                            text = "Cancel",
                                            onClick = { orderToDelete = null },
                                            modifier = Modifier.weight(1f)
                                        )
                                        GradientButton(
                                            text = "Yes",
                                            onClick = {
                                                orderToDelete?.let { order ->
                                                    Log.d("customer order id","onclick ok " + order.CustomOrderId)
                                                    employee?.clientCode?.let {
                                                        orderViewModel.deleteOrders(
                                                            ClientCodeRequest(it),
                                                            order.CustomOrderId
                                                        ) { isSuccess ->
                                                            Toast.makeText(
                                                                context,
                                                                if (isSuccess) "Order Deleted Successfully" else "Failed to delete",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                            if (isSuccess) {
                                                                orderViewModel.removeOrderById(order.CustomOrderId)
                                                            }
                                                        }
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