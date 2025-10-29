package com.loyalstring.rfid.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.model.stockTransfer.LabelledStockItems
import com.loyalstring.rfid.data.model.stockTransfer.STApproveRejectRequest
import com.loyalstring.rfid.data.model.stockTransfer.StockTransferItem
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.ui.utils.GradientButtonIcon
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.StockTransferViewModel

@Composable
fun StockTransferDetailScreen(
    onBack: () -> Unit,
    labelItems: List<LabelledStockItems>
) {
    val context = LocalContext.current
    val viewModel: StockTransferViewModel = hiltViewModel()
    val employee = remember { UserPreferences.getInstance(context).getEmployee(Employee::class.java) }

    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedStatus by remember { mutableStateOf("Pending") }

    val selectedIds = remember { mutableStateListOf<Int>() }
    var selectAll by remember { mutableStateOf(false) }

    val horizontalScrollState = rememberScrollState()

    // ---- Approve / Reject / Lost Result ----
    val approveResult by viewModel.stApproveRejectResponse.observeAsState()
    var showSuccessDialog by remember { mutableStateOf(false) }
    var apiMessage by remember { mutableStateOf("") }
    var currentActionType by remember { mutableStateOf(0) }

    // Listen for approve/reject response
    LaunchedEffect(approveResult) {
        approveResult?.onSuccess {
            apiMessage = it.Message ?: "Action completed successfully!"
            showSuccessDialog = true
            viewModel.clearApproveResult()
        }?.onFailure {
            Toast.makeText(context, it.localizedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = "Transfer Details",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
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
                    onClick = {
                        currentActionType = 1
                        handleDetailAction(1, context, selectedIds, employee, viewModel)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .padding(horizontal = 4.dp),
                    icon = painterResource(id = R.drawable.check_circle),
                    iconDescription = "Approve",
                    fontSize = 12
                )

                GradientButtonIcon(
                    text = "Reject",
                    onClick = {
                        currentActionType = 2
                        handleDetailAction(2, context, selectedIds, employee, viewModel)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .padding(horizontal = 4.dp),
                    icon = painterResource(id = R.drawable.ic_cancel),
                    iconDescription = "Reject",
                    fontSize = 12
                )

                GradientButtonIcon(
                    text = "Lost",
                    onClick = {
                        currentActionType = 3
                        handleDetailAction(3, context, selectedIds, employee, viewModel)
                    },
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

            // --- Filter Row ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Status: $selectedStatus",
                    color = Color.Black,
                    fontFamily = poppins,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )

                IconButton(onClick = { showFilterDialog = true }) {
                    Icon(Icons.Default.Tune, contentDescription = "Filter", tint = Color(0xFF3C3C3C))
                }
            }

            // --- Table Header ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF3C3C3C))
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Sr", color = Color.White, fontFamily = poppins, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, modifier = Modifier.width(50.dp)
                )

                Row(
                    modifier = Modifier
                        .horizontalScroll(horizontalScrollState)
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Category", "Item Code", "Branch", "G Wt", "N Wt").forEach { header ->
                        Text(
                            header, color = Color.White, fontFamily = poppins, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center, modifier = Modifier.width(90.dp)
                        )
                    }
                }

                Checkbox(
                    checked = selectAll,
                    onCheckedChange = { checked ->
                        selectAll = checked
                        selectedIds.clear()
                        if (checked) selectedIds.addAll(
                            filteredItems(labelItems, selectedStatus).mapNotNull { it.Id }
                        )
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color.White,
                        uncheckedColor = Color.White,
                        checkmarkColor = Color(0xFF3C3C3C)
                    ),
                    modifier = Modifier
                        .width(60.dp)
                        .scale(0.9f)
                )
            }

            // --- Filtered Data ---
            val filtered = remember(selectedStatus, labelItems) {
                filteredItems(labelItems, selectedStatus)
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                itemsIndexed(filtered) { index, item ->
                    var checked by remember { mutableStateOf(selectAll) }

                    LaunchedEffect(selectAll) {
                        checked = selectAll
                        if (selectAll) {
                            if (!selectedIds.contains(item.Id ?: 0)) selectedIds.add(item.Id ?: 0)
                        } else {
                            selectedIds.remove(item.Id ?: 0)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (index % 2 == 0) Color.White else Color(0xFFF7F7F7))
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${index + 1}",
                            modifier = Modifier.width(50.dp),
                            textAlign = TextAlign.Center,
                            color = Color.Black
                        )

                        Row(
                            modifier = Modifier
                                .horizontalScroll(horizontalScrollState)
                                .weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                item.CategoryName ?: "-",
                                item.ItemCode ?: "-",
                                item.BranchName ?: "-",
                                item.GrossWt ?: "-",
                                item.NetWt ?: "-"
                            ).forEach { text ->
                                Text(
                                    text,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(90.dp)
                                )
                            }
                        }

                        Checkbox(
                            checked = checked,
                            onCheckedChange = { isChecked ->
                                checked = isChecked
                                val id = item.Id ?: 0
                                if (isChecked) {
                                    if (!selectedIds.contains(id)) selectedIds.add(id)
                                } else {
                                    selectedIds.remove(id)
                                }
                                selectAll = selectedIds.size == filtered.size
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF5231A7),
                                uncheckedColor = Color.Gray,
                                checkmarkColor = Color.White
                            ),
                            modifier = Modifier.width(60.dp)
                        )
                    }
                    Divider(color = Color(0xFFE0E0E0))
                }
            }
        }

        // --- Filter Dialog ---
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
                                text = "Status Filter",
                                color = Color.White,
                                fontFamily = poppins,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        listOf("Pending", "Approved", "Rejected", "Lost").forEach {
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

        // --- Success Dialog ---
        if (showSuccessDialog) {
            ApproveSuccessDialog(
                visible = true,
                approvedCount = selectedIds.size,
                transferType = "",
                onDismiss = { showSuccessDialog = false },
                onContinue = { showSuccessDialog = false },
                apiMessage = apiMessage,
                actionType = currentActionType
            )
        }
    }
}

// ✅ Fixed Filter Logic — now filters based on `LabelledStockItems.RequestStatus`
fun filteredItems(list: List<LabelledStockItems>, selectedStatus: String): List<LabelledStockItems> {
    return list.filter { item ->
        val status = item.RequestStatus ?: -1
        when (selectedStatus.lowercase()) {
            "pending" -> status == 0
            "approved" -> status == 1
            "rejected" -> status == 2
            "lost" -> status == 3
            else -> true
        }
    }
}

// ---- Action Handler ----
fun handleDetailAction(
    statusType: Int,
    context: Context,
    selectedIds: SnapshotStateList<Int>,
    employee: Employee?,
    viewModel: StockTransferViewModel
) {
    if (selectedIds.isEmpty()) {
        Toast.makeText(context, "Please select at least one item", Toast.LENGTH_SHORT).show()
        return
    }

    val items = selectedIds.map { id ->
        StockTransferItem(Id = id, Approved = true, Status = statusType)
    }

    val request = STApproveRejectRequest(
        StockTransferItems = items,
        ClientCode = employee?.clientCode.orEmpty()
    )

    viewModel.stApproveReject(request)
}
