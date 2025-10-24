package com.loyalstring.rfid.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.model.stockTransfer.StockInOutRequest
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.ui.utils.GradientButtonIcon
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.StockTransferViewModel

@Composable
fun StockInScreen(
    onBack: () -> Unit,
    navController: NavHostController
) {
    var shouldNavigateBack by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var selectedTransferType by remember { mutableStateOf("Transfer Type") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedStatus by remember { mutableStateOf("All") }
    var selectionMode by remember { mutableStateOf(false) } // ✅ toggles between status and checkbox

    val context = LocalContext.current
    val viewModel: StockTransferViewModel = hiltViewModel()
    val employee = remember { UserPreferences.getInstance(context).getEmployee(Employee::class.java) }
    val horizontalScrollState = rememberScrollState()

    // 🔹 Fetch transfer types
    val transferTypes by viewModel.transferTypes.collectAsState(initial = emptyList())

    // 🔹 API state
    val stockTransfers = remember { mutableStateListOf<StockTransfer>() }
    val isLoading = remember { mutableStateOf(false) }
    val errorMessage = remember { mutableStateOf<String?>(null) }

    // 🔹 Fetch Transfer Types
    LaunchedEffect(Unit) {
        employee?.clientCode?.let {
            viewModel.loadTransferTypes(ClientCodeRequest(it))
        }
    }

    // 🔹 Fetch Data
    fun fetchStockTransfers() {
        employee?.clientCode?.let { clientCode ->
            isLoading.value = true
            val request = StockInOutRequest(
                ClientCode = clientCode,
                StockType = "labelled",
                TransferType = 0,
                BranchId = employee?.branchNo ?: 0,
                UserID = employee?.id ?: 0,
                RequestType = "In Request"
            )
            viewModel.getAllStockTransfers(request) { result ->
                isLoading.value = false
                result.onSuccess { responseList ->
                    stockTransfers.clear()
                    stockTransfers.addAll(
                        responseList.map {
                            StockTransfer(
                                type = it.StockTransferTypeName ?: "N/A",
                                from = it.SourceName ?: "-",
                                to = it.DestinationName ?: "-",
                                gWt = it.LabelledStockItems.firstOrNull()?.GrossWt ?: "0.00",
                                nWt = it.LabelledStockItems.firstOrNull()?.NetWt ?: "0.00",
                                pending = it.Pending.toString(),
                                status = when {
                                    it.Approved > 0 -> "Approved"
                                    it.Rejected > 0 -> "Rejected"
                                    it.Lost > 0 -> "Lost"
                                    else -> "Pending"
                                },
                                transferBy = it.TransferByEmployee ?: "-",
                                transferTo = it.TransferedToBranch ?: "-",
                                transferType = it.StockTransferTypeName ?: "-"
                            )
                        }
                    )
                }.onFailure { e ->
                    Log.e("StockTransferVM", "Error: ${e.message}")
                    errorMessage.value = e.message ?: "Something went wrong."
                }
            }
        }
    }

    // 🔹 Load Data Initially
    LaunchedEffect(Unit) {
        fetchStockTransfers()
    }

    // 🔹 Filter Data
    val filteredTransfers = remember(selectedTransferType, selectedStatus, stockTransfers) {
        stockTransfers.filter {
            (selectedTransferType == "Transfer Type" || it.type.equals(selectedTransferType, true)) &&
                    (selectedStatus == "All" || it.status.equals(selectedStatus, true))
        }
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = "Stock Transfer",
                navigationIcon = {
                    IconButton(onClick = { shouldNavigateBack = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                showCounter = false
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GradientButtonIcon(
                    text = "Approve",
                    onClick = { /* TODO */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .padding(horizontal = 4.dp),
                    icon = painterResource(id = R.drawable.check_circle),
                    iconDescription = "Approve",
                    fontSize = 12
                )

                Spacer(Modifier.width(8.dp))

                GradientButtonIcon(
                    text = "Reject",
                    onClick = { /* TODO */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .padding(horizontal = 4.dp),
                    icon = painterResource(id = R.drawable.ic_cancel),
                    iconDescription = "Reject",
                    fontSize = 12
                )

                Spacer(Modifier.width(8.dp))

                GradientButtonIcon(
                    text = "Lost",
                    onClick = { /* TODO */ },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .padding(horizontal = 4.dp),
                    icon = painterResource(id = R.drawable.ic_lost),
                    iconDescription = "Lost",
                    fontSize = 12
                )
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.White)
        ) {
            // 🔹 Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Button(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFF8F8F8),
                            contentColor = Color.Black
                        ),
                        elevation = null,
                        modifier = Modifier
                            .height(40.dp)
                            .width(200.dp)
                    ) {
                        Text(selectedTransferType)
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        if (transferTypes.isNotEmpty()) {
                            transferTypes.forEach { typeItem ->
                                DropdownMenuItem(onClick = {
                                    selectedTransferType = typeItem.TransferType
                                    expanded = false
                                }) {
                                    Text(typeItem.TransferType)
                                }
                            }
                        } else {
                            listOf("Internal", "External", "Vendor").forEach { type ->
                                DropdownMenuItem(onClick = {
                                    selectedTransferType = type
                                    expanded = false
                                }) {
                                    Text(type)
                                }
                            }
                        }
                    }
                }

                IconButton(onClick = { showFilterDialog = true }) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "Filter",
                        tint = Color(0xFF3C3C3C)
                    )
                }
            }

            // 🔹 Loading / Error / Table
            when {
                isLoading.value -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF5231A7))
                    }
                }

                errorMessage.value != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = errorMessage.value ?: "Error loading data.",
                            color = Color.Red,
                            fontFamily = poppins,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                else -> {
                    // 🔹 Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF3C3C3C))
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "Sr",
                            color = Color.White,
                            fontFamily = poppins,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(50.dp).padding(horizontal = 4.dp)
                        )

                        Row(
                            modifier = Modifier
                                .horizontalScroll(horizontalScrollState)
                                .weight(1f)
                        ) {
                            listOf("From", "To", "G Wt", "N Wt", "Transfer By", "Transfer To", "Transfer Type").forEach { header ->
                                Text(
                                    text = header,
                                    color = Color.White,
                                    fontFamily = poppins,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(80.dp).padding(horizontal = 4.dp)
                                )
                            }
                        }

                        // ✅ Click to toggle selection mode
                        Text(
                            text = if (selectionMode) "Select" else "Status",
                            color = Color.White,
                            fontFamily = poppins,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .width(80.dp)
                                .padding(horizontal = 4.dp)
                                .clickable { selectionMode = !selectionMode }
                        )
                    }

                    // 🔹 Data Rows
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        itemsIndexed(filteredTransfers) { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (index % 2 == 0) Color.White else Color(0xFFF7F7F7))
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = Color.Black,
                                    fontFamily = poppins,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(50.dp).padding(horizontal = 4.dp)
                                )

                                Row(
                                    modifier = Modifier
                                        .horizontalScroll(horizontalScrollState)
                                        .weight(1f)
                                ) {
                                    listOf(item.from, item.to, item.gWt, item.nWt, item.transferBy, item.transferTo, item.transferType).forEach { text ->
                                        Text(
                                            text = text,
                                            color = Color.Black,
                                            fontFamily = poppins,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.width(80.dp).padding(horizontal = 4.dp)
                                        )
                                    }
                                }

                                if (selectionMode) {
                                    var checked by remember { mutableStateOf(false) }
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { checked = it },
                                        modifier = Modifier
                                            .width(80.dp)
                                            .padding(horizontal = 4.dp)
                                    )
                                } else {
                                    Text(
                                        text = item.pending,
                                        color = Color(0xFF1976D2),
                                        fontFamily = poppins,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .width(80.dp)
                                            .padding(horizontal = 4.dp)
                                            .clickable { selectionMode = true }
                                    )
                                }
                            }
                            Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
                        }
                    }
                }
            }
        }

        if (shouldNavigateBack) onBack()
    }
}

// 🔹 Filter Row Composable
@Composable
fun FilterRow(
    text: String,
    iconRes: Int,
    selectedStatus: String,
    onClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F8F8))
            .clickable { onClick(text) }
            .padding(start = 50.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = text,
            tint = Color(0xFF6B6B6B),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = Color(0xFF3C3C3C),
            fontFamily = poppins,
            fontWeight = if (selectedStatus == text) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Start
        )
    }
}

// 🔹 Data Model
data class StockTransfer(
    val type: String,
    val from: String,
    val to: String,
    val gWt: String,
    val nWt: String,
    val pending: String,
    val status: String,
    val transferBy: String,
    val transferTo: String,
    val transferType: String
)
