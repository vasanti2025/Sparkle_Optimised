package com.loyalstring.rfid.ui.screens

import android.util.Log
import android.widget.Toast
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.model.stockTransfer.StockInOutRequest
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.StockTransferViewModel
import java.io.Serializable

@Composable
fun StockInScreen(
    onBack: () -> Unit,
    navController: NavHostController,
    requestType: String
) {
    val context = LocalContext.current
    val viewModel: StockTransferViewModel = hiltViewModel()
    val employee =
        remember { UserPreferences.getInstance(context).getEmployee(Employee::class.java) }

    var shouldNavigateBack by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var selectedTransferType by rememberSaveable { mutableStateOf("Transfer Type") }
    var showFilterDialog by rememberSaveable { mutableStateOf(false) }
    var selectedStatus by rememberSaveable { mutableStateOf("All") }

    val horizontalScrollState = rememberScrollState()
    val transferTypes by viewModel.transferTypes.collectAsState(initial = emptyList())
    val stockTransfers = remember { mutableStateListOf<StockTransfer>() }

    val isLoading = remember { mutableStateOf(false) }
    val errorMessage = remember { mutableStateOf<String?>(null) }

    // 🔹 Fetch transfer types once
    LaunchedEffect(Unit) {
        employee?.clientCode?.let {
            // 🟢 Load transfer types first
            viewModel.loadTransferTypes(ClientCodeRequest(it))
        }

        // 🟢 Then fetch transfers
      //  fetchStockTransfers()
    }

   /* // 🔹 API Fetch Function
    fun fetchStockTransfers() {

        val selectedTypeId = transferTypes.firstOrNull {
            it.TransferType.equals(selectedTransferType, ignoreCase = true)
        }?.Id ?: 0
        employee?.clientCode?.let { clientCode ->
            isLoading.value = true
            val request = StockInOutRequest(
                ClientCode = clientCode,
                StockType = "labelled",
                TransferType = selectedTypeId,
                BranchId = employee?.branchNo ?: 0,
                UserID = employee?.id ?: 0,
                RequestType = requestType
            )

            viewModel.getAllStockTransfers(request) { result ->
                isLoading.value = false
                result.onSuccess { responseList ->
                    stockTransfers.clear()
                    stockTransfers.addAll(
                        responseList.map {
                            StockTransfer(
                                id = it.Id ?: 0,
                                type = it.StockTransferTypeName ?: "Branch To Branch",
                                from = it.SourceName ?: "-",
                                to = it.DestinationName ?: "-",
                                gWt = safeNumber(it.LabelledStockItems?.firstOrNull()?.GrossWt),
                                nWt = safeNumber(it.LabelledStockItems?.firstOrNull()?.NetWt),
                                pending = it.Pending ?: 0,
                                approved = it.Approved ?: 0,
                                rejected = it.Rejected ?: 0,
                                lost = it.Lost ?: 0,
                                transferBy = it.TransferByEmployee ?: "-",
                                transferTo = it.TransferedToBranch ?: "-",
                                transferType = it.StockTransferTypeName ?: "-",
                                fulldata = it.LabelledStockItems ?: "-"
                            )
                        }
                    )
                }.onFailure { e ->
                    errorMessage.value = e.message ?: "Something went wrong."
                }
            }
        }
    }*/

    fun fetchStockTransfers() {
        // 🟢 Only send TransferType if user selected something real
        val selectedTypeId = transferTypes.firstOrNull {
            it.TransferType.equals(selectedTransferType, ignoreCase = true)
        }?.Id

        val transferTypeValue =
            if (selectedTransferType == "Transfer Type" || transferTypes.isEmpty()) null
            else selectedTypeId

        employee?.clientCode?.let { clientCode ->
            isLoading.value = true
            val request = StockInOutRequest(
                ClientCode = clientCode,
                StockType = "labelled",
                TransferType = transferTypeValue, // ✅ null means all data
                BranchId = employee?.branchNo ?: 0,
                UserID = employee?.id ?: 0,
                RequestType = requestType
            )

            viewModel.getAllStockTransfers(request) { result ->
                isLoading.value = false
                result.onSuccess { responseList ->
                    stockTransfers.clear()
                    stockTransfers.addAll(
                        responseList.map {
                            StockTransfer(
                                id = it.Id ?: 0,
                                type = it.StockTransferTypeName ?: "Branch To Branch",
                                from = it.SourceName ?: "-",
                                to = it.DestinationName ?: "-",
                                gWt = safeNumber(it.LabelledStockItems?.firstOrNull()?.GrossWt),
                                nWt = safeNumber(it.LabelledStockItems?.firstOrNull()?.NetWt),
                                pending = it.Pending ?: 0,
                                approved = it.Approved ?: 0,
                                rejected = it.Rejected ?: 0,
                                lost = it.Lost ?: 0,
                                transferBy = it.TransferByEmployee ?: "-",
                                transferTo = it.TransferedToBranch ?: "-",
                                transferType = it.StockTransferTypeName ?: "-",
                                fulldata = it.LabelledStockItems ?: "-"
                            )
                        }
                    )
                }.onFailure { e ->
                    errorMessage.value = e.message ?: "Something went wrong."
                }
            }
        }
    }


    // 🔹 Initial API Call
    LaunchedEffect(Unit) {
        fetchStockTransfers()
    }

    // ✅ 🔁 Re-fetch when coming back from detail screen
    val navBackStackEntry = remember(navController) { navController.currentBackStackEntry }
   /* DisposableEffect(navBackStackEntry) {
        val lifecycle = navBackStackEntry?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // 🔁 Re-fetch list on return
                fetchStockTransfers()
            }
        }
        lifecycle?.addObserver(observer)
        onDispose {
            lifecycle?.removeObserver(observer)
        }
    }
*/
  /*  LaunchedEffect(selectedTransferType) {
        if (selectedTransferType != "Transfer Type" && transferTypes.isNotEmpty()) {
            fetchStockTransfers()
        }
    }*/
    //val navBackStackEntry = remember(navController) { navController.currentBackStackEntry }

    DisposableEffect(navBackStackEntry) {
        val lifecycle = navBackStackEntry?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Log.d("StockInScreen", "Returned from detail — refreshing list")
                selectedStatus = "All"  // optional reset
                selectedTransferType = "Transfer Type" // optional reset
                fetchStockTransfers()
            }
        }
        lifecycle?.addObserver(observer)
        onDispose {
            lifecycle?.removeObserver(observer)
        }
    }
    // 🔹 Apply Filters
    val filteredTransfers = remember(selectedTransferType, selectedStatus, stockTransfers) {
        stockTransfers.filter {
            val matchesType =
                selectedTransferType == "Transfer Type" || it.type.equals(selectedTransferType, true)
            val matchesStatus = when (selectedStatus) {
                "Pending" -> it.pending > 0
                "Approved" -> it.approved > 0
                "Rejected" -> it.rejected > 0
                "Lost" -> it.lost > 0
                else -> true
            }
            matchesType && matchesStatus
        }
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = "Stock Transfers",
                navigationIcon = {
                    IconButton(onClick = { shouldNavigateBack = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                showCounter = false
            )
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
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        if (transferTypes.isNotEmpty()) {
                            transferTypes.forEach { typeItem ->
                                DropdownMenuItem(onClick = {
                                    selectedTransferType = typeItem.TransferType
                                    expanded = false
                                    fetchStockTransfers() // 🔁 refresh instantly on type change
                                }) { Text(typeItem.TransferType) }
                            }
                        } else {
                            listOf("Internal", "External", "Vendor").forEach { type ->
                                DropdownMenuItem(onClick = {
                                    selectedTransferType = type
                                    expanded = false
                                    fetchStockTransfers()
                                }) { Text(type) }
                            }
                        }
                    }
                }

                IconButton(onClick = { showFilterDialog = true }) {
                    Icon(Icons.Default.Tune, contentDescription = "Filter", tint = Color(0xFF3C3C3C))
                }
            }

            when {
                isLoading.value -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF5231A7))
                }

                errorMessage.value != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = errorMessage.value ?: "Error loading data.",
                        color = Color.Red,
                        fontFamily = poppins,
                        fontWeight = FontWeight.Medium
                    )
                }

                else -> {
                    // 🔹 Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF3C3C3C))
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sr", color = Color.White, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center, modifier = Modifier.width(40.dp))

                        Row(
                            modifier = Modifier
                                .horizontalScroll(horizontalScrollState)
                                .weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("From", "To", "G Wt", "N Wt", "Transfer By", "Transfer To", "Transfer Type")
                                .forEach { header ->
                                    Text(
                                        header,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.width(90.dp)
                                    )
                                }
                        }

                        Text(
                            "Status",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(100.dp)
                        )
                    }

                    // 🔹 Data Rows
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        val displayList = if (selectedStatus == "All") stockTransfers else filteredTransfers

                        itemsIndexed(displayList) { index, item ->
                        //itemsIndexed(filteredTransfers) { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (index % 2 == 0) Color.White else Color(0xFFF7F7F7))
                                    .padding(vertical = 8.dp)
                                    .clickable {
                                        val transferData = item.fulldata
                                        if (transferData != null) {
                                            navController.currentBackStackEntry
                                                ?.savedStateHandle
                                                ?.apply {
                                                    set("labelItems", transferData)
                                                    set("requestType", requestType)
                                                    set("selectedTransferType", selectedTransferType)// 🔹 Pass current request type here
                                                }
                                            navController.navigate("stock_transfer_detail")
                                        } else {
                                            Toast.makeText(context, "Transfer details not found", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${index + 1}",
                                    color = Color.Black,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(40.dp)
                                )

                                Row(
                                    modifier = Modifier
                                        .horizontalScroll(horizontalScrollState)
                                        .weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf(
                                        item.from, item.to, item.gWt, item.nWt,
                                        item.transferBy, item.transferTo, item.transferType
                                    ).forEach { text ->
                                        Text(
                                            text,
                                            color = Color.Black,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.width(90.dp)
                                        )
                                    }
                                }

                                // ✅ Show only related count per filter
                                val displayText = when (selectedStatus) {
                                    "Pending" -> "P: ${item.pending}"
                                    "Approved" -> "A: ${item.approved}"
                                    "Rejected" -> "R: ${item.rejected}"
                                    "Lost" -> "L: ${item.lost}"
                                    else -> "P:${item.pending}"
                                  //  else -> "P:${item.pending} | A:${item.approved} | R:${item.rejected} | L:${item.lost}"
                                }

                                Text(
                                    text = displayText,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(120.dp)
                                )
                            }
                            Divider(color = Color(0xFFE0E0E0))
                        }
                    }
                }
            }
        }

        // 🔹 Filter Dialog
        if (showFilterDialog) {
            AlertDialog(
                onDismissRequest = { showFilterDialog = false },
                shape = RoundedCornerShape(16.dp),
                backgroundColor = Color.Transparent,
                buttons = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, shape = RoundedCornerShape(16.dp))
                            .padding(bottom = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF5231A7), Color(0xFFD32940))
                                    ),
                                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                )
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Select Status",
                                color = Color.White,
                                fontFamily = poppins,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        listOf("All", "Pending", "Approved", "Rejected", "Lost").forEach {
                            Divider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)
                            FilterRow(it, R.drawable.schedule, selectedStatus) {
                                selectedStatus = it
                                showFilterDialog = false
                            }
                        }
                    }
                }
            )
        }

        if (shouldNavigateBack) onBack()
    }
}

// ✅ Utility Function
fun safeNumber(value: Any?): String {
    return try {
        when (value) {
            null -> "0.000"
            is Number -> String.format("%.3f", value.toDouble())
            else -> {
                val str = value.toString().trim()
                val parsed = str.toDoubleOrNull()
                if (parsed != null && parsed.isFinite()) String.format("%.3f", parsed) else "0.000"
            }
        }
    } catch (e: Exception) {
        Log.e("SafeNumber", "Invalid numeric value: $value (${e.message})")
        "0.000"
    }
}

// 🔹 Filter Row
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
            fontWeight = if (selectedStatus == text) FontWeight.Bold else FontWeight.Normal
        )
    }
}

// 🔹 Data Model
data class StockTransfer(
    val id: Int,
    val type: String,
    val from: String,
    val to: String,
    val gWt: String,
    val nWt: String,
    val pending: Int,
    val approved: Int,
    val rejected: Int,
    val lost: Int,
    val transferBy: String,
    val transferTo: String,
    val transferType: String,
    val fulldata: Any
) : Serializable
