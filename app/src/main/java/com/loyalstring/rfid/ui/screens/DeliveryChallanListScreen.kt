package com.loyalstring.rfid.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanResponseList
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.DeliveryChallanViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import androidx.compose.ui.res.painterResource
import java.util.*
import com.loyalstring.rfid.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryChallanListScreen(
    onBack: () -> Unit,
    navController: NavHostController
) {
    val viewModel: DeliveryChallanViewModel = hiltViewModel()
    val context = LocalContext.current
    val employee = remember { UserPreferences.getInstance(context).getEmployee(Employee::class.java) }

    val challanList by viewModel.challanList.collectAsState()
    val isLoading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    var visibleItems by remember { mutableStateOf(10) }
    var searchQuery by remember { mutableStateOf("") }

    // Fetch Challans once
    LaunchedEffect(Unit) {
        employee?.let {
            viewModel.fetchAllChallans(it.clientCode ?: "", it.branchNo ?: 0)
        }
    }

    val filteredData = if (searchQuery.isNotEmpty()) {
        challanList.filter {
            it.ChallanNo.orEmpty().contains(searchQuery, true) ||
                    it.ChallanNo.orEmpty().contains(searchQuery, true)
        }
    } else challanList

    val visibleData = filteredData.sortedByDescending { it.Id }

    val headerTitles = listOf(
        "S.No",
        "Challan No",
        "Date",
        "Customer Name",
        "Qty",
        "G.Wt",
        "S.Wt",
        "D.Wt",
        "N.Wt",
        "F+Wt",
        "Tax Amt",
        "Total Amt",
        "Branch",
        "Action"
    )

    val columnWidths = listOf(
        45.dp,  // S.No
        100.dp, // Challan No
        80.dp,  // Date
        140.dp, // Customer Name
        50.dp,  // Qty
        70.dp,  // G.Wt
        70.dp,  // S.Wt
        70.dp,  // D.Wt
        70.dp,  // N.Wt
        70.dp,  // F+Wt
        90.dp,  // Tax Amt
        100.dp, // Total Amt
        100.dp, // Branch
        90.dp   // Actions
    )

    Column(modifier = Modifier.fillMaxSize()) {
        GradientTopBar(
            title = "Delivery Challan List",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            showCounter = false
        )

        // ✅ Use renamed composable to avoid Material3 name conflict
        DeliverySearchBar(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                visibleItems = 10
            }
        )

        DeliveryChallanTable(
            navController = navController,
            headerTitles = headerTitles,
            columnWidths = columnWidths,
            data = visibleData.take(visibleItems),
            onLoadMore = {
                if (visibleItems < filteredData.size) visibleItems += 10
            },
            isLoading = isLoading,
            context = context
        )

        if (error != null) {
            Text(
                text = error ?: "Error loading challans",
                color = Color.Red,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun DeliveryChallanTable(
    navController: NavHostController,
    headerTitles: List<String>,
    columnWidths: List<Dp>,
    data: List<DeliveryChallanResponseList>,
    onLoadMore: () -> Unit,
    isLoading: Boolean,
    context: Context
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
                        fontSize = 12.sp,
                        maxLines = 1
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
                itemsIndexed(data) { index, challan ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Data Columns
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(sharedScrollState)
                        ) {
                            val values = listOf(
                                (index + 1).toString(),
                                challan.ChallanNo ?: "",
                                formatDate(challan.LastUpdated ?: ""),
                                challan.CustomerId ?: "",
                                challan.Qty ?: "0",
                                challan.GrossWt ?: "0.000",
                                challan.StoneWt ?: "0.000",
                                challan.TotalDiamondWeight ?: "0.000",
                                challan.NetWt ?: "0.000",
                                challan.TotalFineMetal ?: "0.000",
                                challan.TotalGSTAmount ?: "0.00",
                                challan.TotalAmount ?: "0.00",
                                challan.BranchId ?: "-"
                            )


                                values.forEachIndexed { index, rawValue ->
                                    val value = rawValue ?: ""           // handle null
                                    Text(
                                        text = value.toString(),          // enforce String type
                                        modifier = Modifier
                                            .width(columnWidths[index])
                                            .padding(6.dp),
                                        fontSize = 11.sp,
                                        fontFamily = poppins,
                                        maxLines = 1
                                    )

                            }
                        }

                        // Fixed Actions
                        // Fixed Actions
                        Row(
                            modifier = Modifier
                                .width(columnWidths.last())
                                .height(40.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 🖨️ Print Button
                            IconButton(onClick = {
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(
                                        context,
                                        "Printing ${challan.ChallanNo ?: "Challan"}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    // TODO: Integrate PDF print logic here
                                }
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_edit_svg),
                                    contentDescription = "Edit",
                                    tint = Color(0xFF37474F),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // ✏️ Edit Button
                            IconButton(onClick = {
                                // ✅ Navigate to your Edit screen, pass challanId or challanNo
                                navController.navigate("editDeliveryChallan/${challan.Id}")
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.print_svg),
                                    contentDescription = "Print",
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

@Composable
fun DeliverySearchBar(value: String, onValueChange: (String) -> Unit) {
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
        Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.padding(start = 12.dp), tint = Color.Gray)
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
                    text = "Search by challan no or customer name",
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

/*fun formatDate(dateString: String): String {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = format.parse(dateString)
        val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        dateString
    }
}*/
