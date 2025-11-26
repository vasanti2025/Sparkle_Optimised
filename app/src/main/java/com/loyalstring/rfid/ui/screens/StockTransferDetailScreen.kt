package com.loyalstring.rfid.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

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
import com.loyalstring.rfid.viewmodel.StockTransferViewModel
import com.loyalstring.rfid.worker.LocaleHelper


@Composable
fun StockTransferDetailScreen(
    onBack: () -> Unit,
    labelItems: List<LabelledStockItems>,
    requestType: String,
    selectedTransferType: String,
    id: Any,
) {
    val context = LocalContext.current
    val viewModel: StockTransferViewModel = hiltViewModel()
    val employee = remember { UserPreferences.getInstance(context).getEmployee(Employee::class.java) }
    val items by viewModel.labelledStockItems.observeAsState(initial = labelItems)

    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedStatus by remember { mutableStateOf("Pending") }
    val selectedIds = remember { mutableStateListOf<Int>() }
    var selectAll by remember { mutableStateOf(false) }
    val horizontalScrollState = rememberScrollState()

    // ---- Approve / Reject / Lost ----
    var showSuccessDialog by remember { mutableStateOf(false) }
    var apiMessage by remember { mutableStateOf("") }
    var currentActionType by remember { mutableStateOf(0) }

    var expanded by remember { mutableStateOf(false) }
    var selectedTransferType by remember { mutableStateOf(selectedTransferType) }
    val transferTypes by viewModel.transferTypes.collectAsState(initial = emptyList())
    var approvedCount by remember { mutableStateOf(0) }

    // ✅ Refresh API Call
    fun refreshItems() {
        val clientCode = employee?.clientCode ?: return
        viewModel.getLabelledStockByTransferId(
            clientCode = clientCode,
            mainObjectId = id as Int,
            requestType = requestType,
            userId = employee.id,
            branchId = 0
        )
    }

    val approveRejectResponse by viewModel.stApproveRejectResponse.observeAsState()
    var isRefreshing by remember { mutableStateOf(false) }
    LaunchedEffect(approveRejectResponse) {
        approveRejectResponse?.onSuccess {
            isRefreshing = true
            refreshItems()
            showSuccessDialog = true
            apiMessage = "Items processed successfully!"
            selectedIds.clear()
            selectAll = false
            viewModel.clearApproveResult()
            isRefreshing = false
        }
    }

    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = if (currentLocales.isEmpty) "en" else currentLocales[0]?.language
    val localizedContext = LocaleHelper.applyLocale(context, currentLang ?: "en")
    Scaffold(
        topBar = {
            GradientTopBar(
                title = localizedContext.getString(R.string.transfer_details_title),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                showCounter = false
            )
        },
        bottomBar = {
            if (requestType != "Out Request") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GradientButtonIcon(
                        text = localizedContext.getString(R.string.approve_button),
                        onClick = {
                            currentActionType = 1
                            handleDetailAction(
                                requestType,
                                1,
                                context,
                                selectedIds,
                                employee,
                                viewModel
                            ) {
                                approvedCount = selectedIds.size
                                refreshItems()
                                showSuccessDialog = true
                                apiMessage = localizedContext.getString(R.string.items_approved_success)
                                selectedIds.clear()
                                selectAll = false
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .padding(horizontal = 4.dp),
                        icon = painterResource(id = R.drawable.check_circle),
                        iconDescription = localizedContext.getString(R.string.approve_button),
                        fontSize = 12
                    )

                    GradientButtonIcon(
                        text = localizedContext.getString(R.string.reject_button),
                        onClick = {
                            currentActionType = 2
                            handleDetailAction(
                                requestType,
                                2,
                                context,
                                selectedIds,
                                employee,
                                viewModel
                            ) {
                                approvedCount = selectedIds.size
                                refreshItems()
                                showSuccessDialog = true
                                apiMessage = localizedContext.getString(R.string.items_rejected_success)
                                selectedIds.clear()
                                selectAll = false
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .padding(horizontal = 4.dp),
                        icon = painterResource(id = R.drawable.ic_cancel),
                        iconDescription = localizedContext.getString(R.string.reject_button),
                        fontSize = 12
                    )

                    GradientButtonIcon(
                        text = localizedContext.getString(R.string.lost_button),
                        onClick = {
                            currentActionType = 3
                            handleDetailAction(
                                requestType,
                                3,
                                context,
                                selectedIds,
                                employee,
                                viewModel
                            ) {
                                approvedCount = selectedIds.size
                                refreshItems()
                                showSuccessDialog = true
                                apiMessage = localizedContext.getString(R.string.items_lost_success)
                                selectedIds.clear()
                                selectAll = false
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .padding(horizontal = 4.dp),
                        icon = painterResource(id = R.drawable.ic_lost),
                        iconDescription = localizedContext.getString(R.string.lost_button),
                        fontSize = 12
                    )
                }
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
                            .width(220.dp)
                    ) {
                        Text(selectedTransferType)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        if (transferTypes.isEmpty()) {
                            DropdownMenuItem(onClick = { expanded = false }) {
                                Text(localizedContext.getString(R.string.no_transfer_type_found), color = Color.Gray)
                            }
                        } else {
                            transferTypes.forEach { typeItem ->
                                DropdownMenuItem(onClick = {
                                    selectedTransferType = typeItem.TransferType
                                    expanded = false
                                }) {
                                    Text(
                                        text = typeItem.TransferType,
                                        color = if (selectedTransferType == typeItem.TransferType)
                                            Color(0xFF5231A7) else Color.Black,
                                        fontWeight = if (selectedTransferType == typeItem.TransferType)
                                            FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
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
                    localizedContext.getString(R.string.sr_header),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(50.dp)
                )
                Row(
                    modifier = Modifier
                        .horizontalScroll(horizontalScrollState)
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        localizedContext.getString(R.string.category_header),
                        localizedContext.getString(R.string.item_code_header),
                        localizedContext.getString(R.string.branch_header),
                        localizedContext.getString(R.string.gross_wt_header),
                        localizedContext.getString(R.string.net_wt_header)
                    ).forEach { header ->
                        Text(
                            header,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(90.dp)
                        )
                    }
                }
                if (requestType != "Out Request") {
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
                        modifier = Modifier
                            .width(60.dp)
                            .scale(0.9f)
                    )
                }
            }

            // --- Filtered List ---
            val filtered = remember(selectedStatus, items) {
                filteredItems(items, selectedStatus)
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
                            if (!selectedIds.contains(item.TransferItemId ?: 0))
                                selectedIds.add(item.TransferItemId ?: 0)
                        } else selectedIds.remove(item.TransferItemId ?: 0)
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
                        if (requestType != "Out Request") {
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
                            Text(
                                localizedContext.getString(R.string.status_filter_title),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val statusIcons = mapOf(
                            localizedContext.getString(R.string.pending_status) to R.drawable.schedule,
                            localizedContext.getString(R.string.approved_status) to R.drawable.check_circle_gray,
                            localizedContext.getString(R.string.rejected_status) to R.drawable.cancel_gray,
                            localizedContext.getString(R.string.lost_status) to R.drawable.ic_lost
                        )

                        listOf(
                            localizedContext.getString(R.string.pending_status),
                            localizedContext.getString(R.string.approved_status),
                            localizedContext.getString(R.string.rejected_status),
                            localizedContext.getString(R.string.lost_status)
                        ).forEach { status ->
                            Divider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)
                            FilterRow(
                                statusText = status,
                                iconRes = statusIcons[status] ?: R.drawable.schedule,
                                selectedStatus = selectedStatus
                            ) {
                                selectedStatus = status
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
                localizedContext= localizedContext,
                visible = true,
                approvedCount = approvedCount,
                transferType = "",
                onDismiss = { showSuccessDialog = false },
                onContinue = { showSuccessDialog = false },
                apiMessage = apiMessage,
                actionType = currentActionType
            )
        }
    }
}

// unchanged FilterRow, ApproveSuccessDialog, GradientAnimatedCheckmark, filteredItems, handleDetailAction


@Composable
fun FilterRow(
    statusText: String,
    iconRes: Int,
    selectedStatus: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selectedStatus == statusText) Color(0xFFF3E9FF)
                else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = statusText,
            tint = when (statusText) {
                "Pending" -> Color(0xFF9E9E9E)    // gray
                "Approved" -> Color(0xFF9E9E9E)   // green
                "Rejected" -> Color(0xFF9E9E9E)   // red
                "Lost" -> Color(0xFF9E9E9E)       // orange
                else -> Color.Black
            },
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = statusText,
            color = Color.Black,
            fontWeight = if (selectedStatus == statusText) FontWeight.Bold else FontWeight.Normal,
            fontSize = 15.sp
        )
    }
}

@Composable
fun ApproveSuccessDialog(
    localizedContext: Context,
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
        1 -> localizedContext.getString(R.string.approve_action)
        2 -> localizedContext.getString(R.string.reject_action)
        3 -> localizedContext.getString(R.string.lost_action)
        else -> localizedContext.getString(R.string.processed_action)
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
                    text = apiMessage.ifBlank { localizedContext.getString(R.string.api_default_message) },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = localizedContext.getString(R.string.approve_count_text, approvedCount, actionText),
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = localizedContext.getString(R.string.approve_transfer_type_label, transferType),
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
                        text = localizedContext.getString(R.string.continue_text),
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

fun handleDetailAction(
    requestType: String,
    statusType: Int,
    context: Context,
    selectedIds: SnapshotStateList<Int>,
    employee: Employee?,
    viewModel: StockTransferViewModel,
    onSuccess: @Composable () -> Unit
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

    // ✅ Just trigger the ViewModel call — don’t observe here
    viewModel.stApproveReject(request)
}

