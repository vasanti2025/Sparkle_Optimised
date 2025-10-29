package com.loyalstring.rfid.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    labelItems: List<LabelledStockItems>,
    requestType: String
) {
    val context = LocalContext.current
    val viewModel: StockTransferViewModel = hiltViewModel()
    val employee = remember { UserPreferences.getInstance(context).getEmployee(Employee::class.java) }

    // ✅ Local mutable list for reactive refresh
    val items = remember { mutableStateListOf<LabelledStockItems>() }
    LaunchedEffect(labelItems) {
        items.clear()
        items.addAll(labelItems)
    }

    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedStatus by remember { mutableStateOf("Pending") }
    val selectedIds = remember { mutableStateListOf<Int>() }
    var selectAll by remember { mutableStateOf(false) }
    val horizontalScrollState = rememberScrollState()

    // ---- Approve / Reject / Lost ----
    var showSuccessDialog by remember { mutableStateOf(false) }
    var apiMessage by remember { mutableStateOf("") }
    var currentActionType by remember { mutableStateOf(0) }


    // ✅ Refresh API Call
    fun refreshItems() {
        val clientCode = employee?.clientCode ?: return
        val firstItem = labelItems.firstOrNull() ?: return
        val transferId = firstItem.Id ?: return

        viewModel.getLabelledStockByTransferId(clientCode, transferId,requestType,employee.id,0) { result ->
            result.onSuccess { updatedList ->
                items.clear()
                items.addAll(updatedList)
            }.onFailure {
                Toast.makeText(context, "Failed to refresh list: ${it.message}", Toast.LENGTH_SHORT).show()
            }
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
                        handleDetailAction(requestType,1, context, selectedIds, employee, viewModel) {
                            refreshItems()
                            showSuccessDialog = true
                            apiMessage = "Items approved successfully!"
                            selectedIds.clear()
                            selectAll = false
                        }
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
                        handleDetailAction(requestType,2, context, selectedIds, employee, viewModel) {
                            refreshItems()
                            showSuccessDialog = true
                            apiMessage = "Items rejected successfully!"
                            selectedIds.clear()
                            selectAll = false
                        }
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
                        handleDetailAction(requestType,3, context, selectedIds, employee, viewModel) {
                            refreshItems()
                            showSuccessDialog = true
                            apiMessage = "Items marked as Lost!"
                            selectedIds.clear()
                            selectAll = false
                        }
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
                    fontWeight = FontWeight.Bold
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
                    "Sr", color = Color.White, fontWeight = FontWeight.Bold,
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
                            header, color = Color.White, fontWeight = FontWeight.Bold,
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
                            filteredItems(items, selectedStatus).mapNotNull { it.TransferItemId }
                        )
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color.White,
                        uncheckedColor = Color.White,
                        checkmarkColor = Color(0xFF3C3C3C)
                    ),
                    modifier = Modifier.width(60.dp).scale(0.9f)
                )
            }

            // --- Filtered Data ---
            val filtered = remember(selectedStatus, items) {
                filteredItems(items, selectedStatus)
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                itemsIndexed(filtered) { index, item ->
                    var checked by remember { mutableStateOf(selectAll) }

                    LaunchedEffect(selectAll) {
                        checked = selectAll
                        if (selectAll) {
                            if (!selectedIds.contains(item.TransferItemId ?: 0)) selectedIds.add(item.TransferItemId ?: 0)
                        } else {
                            selectedIds.remove(item.TransferItemId?: 0)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (index % 2 == 0) Color.White else Color(0xFFF7F7F7))
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${index + 1}",
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
                                    text, color = Color.Black,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(90.dp)
                                )
                            }
                        }
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { isChecked ->
                                checked = isChecked
                                val id = item.TransferItemId ?: 0
                                if (isChecked) {
                                    if (!selectedIds.contains(id)) selectedIds.add(id)
                                } else selectedIds.remove(id)
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
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .padding(bottom = 16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Color(0xFF5231A7), Color(0xFFD32940))
                                    ),
                                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                                )
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Status Filter", color = Color.White, fontWeight = FontWeight.Bold)
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

@Composable
fun ApproveSuccessDialog(
    visible: Boolean,
    approvedCount: Int,
    transferType: String,
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
    apiMessage: String,
    actionType: Int
) {
    if (!visible) return
    val actionText = when (actionType) {
        1 -> stringResource(R.string.approve_action)
        2 -> stringResource(R.string.reject_action)
        3 -> stringResource(R.string.lost_action)
        else -> stringResource(R.string.processed_action)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .width(320.dp) // matches ~447×346 look
            .background(Color.Transparent),
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            // 🔹 Custom header with close button on top right
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, end = 4.dp)
            ) {
                // Close icon
                IconButton(
                    onClick = {
                        onDismiss()
                        onContinue() // ✅ refresh list
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = "Close",
                        tint = Color.Gray
                    )
                }

                // Gradient checkmark centered
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(90.dp)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(Color(0xFF5231A7), Color(0xFFEE316B))
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    GradientAnimatedCheckmark()
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(Modifier.height(16.dp))

                Text(
                    text = apiMessage.ifBlank { stringResource(R.string.api_default_message) },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.approve_count_text, approvedCount, actionText),
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.approve_transfer_type_label, transferType),
                    color = Color(0xFF5231A7),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onContinue,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF3C3C3C),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(42.dp)
                ) {
                    Text(
                        text = stringResource(R.string.continue_text),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    )
}



@Composable
fun GradientAnimatedCheckmark() {
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1000)
    )

    val gradientBrush = Brush.linearGradient(
        colors = listOf(Color(0xFF5231A7), Color(0xFFEE316B))
    )

    Canvas(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
    ) {
        // Background gradient circle
        drawCircle(
            brush = gradientBrush,
            radius = size.minDimension / 2
        )

        // Animated checkmark
        val pathProgress = animationProgress * 1.0f
        val pathLength = 40.dp.toPx()

        val startX = size.width * 0.30f
        val startY = size.height * 0.55f
        val midX = size.width * 0.45f
        val midY = size.height * 0.70f
        val endX = size.width * 0.70f
        val endY = size.height * 0.35f

        drawLine(
            color = Color.White,
            start = Offset(startX, startY),
            end = androidx.compose.ui.geometry.Offset(
                startX + (midX - startX) * pathProgress,
                startY + (midY - startY) * pathProgress
            ),
            strokeWidth = 5f,
            cap = StrokeCap.Round
        )

        if (animationProgress > 0.5f) {
            val remainingProgress = (animationProgress - 0.5f) * 2
            drawLine(
                color = Color.White,
                start = androidx.compose.ui.geometry.Offset(midX, midY),
                end = androidx.compose.ui.geometry.Offset(
                    midX + (endX - midX) * remainingProgress,
                    midY + (endY - midY) * remainingProgress
                ),
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )
        }
    }
}


// ✅ Filters based on RequestStatus (Int code)
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

// ✅ API Call + Success Callback
fun handleDetailAction(
    requestType:String,
    statusType: Int,
    context: Context,
    selectedIds: SnapshotStateList<Int>,
    employee: Employee?,
    viewModel: StockTransferViewModel,
    onSuccess: () -> Unit
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
        ClientCode = employee?.clientCode.orEmpty(),
        UserID = employee?.id.toString(),
        RequestTyp = requestType
    )

    viewModel.stApproveReject(request)
    viewModel.stApproveRejectResponse.observeForever { result ->
        result?.onSuccess {
            onSuccess()
            viewModel.clearApproveResult()
        }
    }
}
