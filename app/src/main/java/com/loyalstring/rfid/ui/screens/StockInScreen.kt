package com.loyalstring.rfid.ui.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
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
import androidx.compose.material.CircularProgressIndicator
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
import androidx.navigation.NavHostController
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.model.stockTransfer.STApproveRejectRequest
import com.loyalstring.rfid.data.model.stockTransfer.StockInOutRequest
import com.loyalstring.rfid.data.model.stockTransfer.StockTransferItem
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
    var selectionMode by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val viewModel: StockTransferViewModel = hiltViewModel()
    val employee = remember { UserPreferences.getInstance(context).getEmployee(Employee::class.java) }
    val horizontalScrollState = rememberScrollState()

    val transferTypes by viewModel.transferTypes.collectAsState(initial = emptyList())
    val stockTransfers = remember { mutableStateListOf<StockTransfer>() }
    val isLoading = remember { mutableStateOf(false) }
    val errorMessage = remember { mutableStateOf<String?>(null) }

    val approveResult by viewModel.stApproveRejectResponse.observeAsState()
    val errorMessage1 by viewModel.errorMessage.observeAsState()

    val selectedIds = remember { mutableStateListOf<Int>() }
    var selectAll by remember { mutableStateOf(false) }

    var showSuccessDialog by remember { mutableStateOf(false) }
    var approvedCount by remember { mutableStateOf(0) }
    var apiMessage by remember { mutableStateOf("") }
    var currentActionType by remember { mutableStateOf(0) }

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
                                id = it.Id ?: 0,
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

    LaunchedEffect(Unit) {
        fetchStockTransfers()
    }

    LaunchedEffect(approveResult) {
        approveResult?.onSuccess { response ->
            approvedCount = selectedIds.size
            apiMessage = response.Message ?: "Action completed successfully!"
            showSuccessDialog = true

            // ✅ Reset selections for next action
            selectedIds.clear()
            selectAll = false
            selectionMode = false
            viewModel.clearApproveResult()

        }?.onFailure {
            Toast.makeText(context, "Failed: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

   /* LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, "Error: $it", Toast.LENGTH_SHORT).show()
        }
    }
*/
    val filteredTransfers = remember(selectedTransferType, selectedStatus, stockTransfers) {
        stockTransfers.filter {
            (selectedTransferType == "Transfer Type" || it.type.equals(selectedTransferType, true)) &&
                    (selectedStatus == "All" || it.status.equals(selectedStatus, true))
        }
    }

    if (showSuccessDialog) {
        // ✅ Find the transfer type of first selected item
        val selectedTransferType = stockTransfers
            .firstOrNull { selectedIds.contains(it.id) }
            ?.transferType
            ?: "Box"

        ApproveSuccessDialog(
            visible = true,
            approvedCount = approvedCount,
            transferType = selectedTransferType, // ✅ passed here
            onDismiss = { showSuccessDialog = false },
            onContinue = {
                showSuccessDialog = false
                selectedIds.clear()
                selectAll = false
                selectionMode = false
                fetchStockTransfers()
            },
            apiMessage = apiMessage,
            actionType = currentActionType
        )
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
                    onClick = {
                        currentActionType = 1
                        handleStockTransferAction(1, context, selectedIds, employee, viewModel)
                    },
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
                    onClick = {
                        currentActionType = 2
                        handleStockTransferAction(2, context, selectedIds, employee, viewModel)
                    },
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
                    onClick = {
                        currentActionType = 3
                        handleStockTransferAction(3, context, selectedIds, employee, viewModel)
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
            // 🔹 Filter Header Row
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
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.padding(start = 4.dp))
                    }

                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        if (transferTypes.isNotEmpty()) {
                            transferTypes.forEach { typeItem ->
                                DropdownMenuItem(onClick = {
                                    selectedTransferType = typeItem.TransferType
                                    expanded = false
                                }) { Text(typeItem.TransferType) }
                            }
                        } else {
                            listOf("Internal", "External", "Vendor").forEach { type ->
                                DropdownMenuItem(onClick = {
                                    selectedTransferType = type
                                    expanded = false
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
                isLoading.value -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF5231A7))
                }

                errorMessage.value != null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = errorMessage.value ?: "Error loading data.", color = Color.Red, fontFamily = poppins, fontWeight = FontWeight.Medium)
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
                        Text("Sr", color = Color.White, fontFamily = poppins, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center, modifier = Modifier
                                .width(50.dp)
                                .padding(horizontal = 4.dp))

                        Row(modifier = Modifier
                            .horizontalScroll(horizontalScrollState)
                            .weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            listOf("From", "To", "G Wt", "N Wt", "Transfer By", "Transfer To", "Transfer Type").forEach { header ->
                                Text(header, color = Color.White, fontFamily = poppins, fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center, modifier = Modifier
                                        .width(80.dp)
                                        .padding(horizontal = 4.dp))
                            }
                        }

                        Row(
                            modifier = Modifier
                                .width(80.dp)
                                .padding(horizontal = 0.dp)
                                .clickable { selectionMode = !selectionMode },
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (selectionMode) "Select" else "Status",
                                color = Color.White,
                                fontFamily = poppins,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            if (selectionMode) {
                                Checkbox(
                                    checked = selectAll,
                                    onCheckedChange = { isChecked ->
                                        selectAll = isChecked
                                        selectedIds.clear()
                                        if (isChecked) selectedIds.addAll(filteredTransfers.map { it.id })
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color.White,
                                        uncheckedColor = Color.White,
                                        checkmarkColor = Color(0xFF3C3C3C)
                                    ),
                                    modifier = Modifier
                                        .scale(0.8f)
                                        .align(Alignment.CenterVertically)
                                )
                            }
                        }
                    }

                    // 🔹 Table Data Rows
                    LazyColumn(modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)) {
                        itemsIndexed(filteredTransfers) { index, item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (index % 2 == 0) Color.White else Color(
                                            0xFFF7F7F7
                                        )
                                    )
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${index + 1}", color = Color.Black, fontFamily = poppins,
                                    textAlign = TextAlign.Center, modifier = Modifier
                                        .width(50.dp)
                                        .padding(horizontal = 4.dp))

                                Row(modifier = Modifier
                                    .horizontalScroll(horizontalScrollState)
                                    .weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                    listOf(item.from, item.to, item.gWt, item.nWt, item.transferBy, item.transferTo, item.transferType).forEach { text ->
                                        Text(text, color = Color.Black, fontFamily = poppins,
                                            textAlign = TextAlign.Center, modifier = Modifier
                                                .width(80.dp)
                                                .padding(horizontal = 4.dp)
                                                .align(Alignment.CenterVertically))
                                    }
                                }

                                if (selectionMode) {
                                    var checked by remember { mutableStateOf(selectAll) }

                                    LaunchedEffect(selectAll) {
                                        checked = selectAll
                                        if (selectAll) {
                                            if (!selectedIds.contains(item.id)) selectedIds.add(item.id)
                                        } else {
                                            selectedIds.remove(item.id)
                                        }
                                    }

                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { isChecked ->
                                            checked = isChecked
                                            if (isChecked) {
                                                if (!selectedIds.contains(item.id)) selectedIds.add(item.id)
                                            } else {
                                                selectedIds.remove(item.id)
                                            }
                                            selectAll = selectedIds.size == filteredTransfers.size
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Color(0xFF5231A7),
                                            uncheckedColor = Color.Gray,
                                            checkmarkColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .width(80.dp)
                                            .padding(horizontal = 4.dp)
                                            .scale(0.8f)
                                            .align(Alignment.CenterVertically)
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
                                            .align(Alignment.CenterVertically)
                                    )
                                }
                            }
                            Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Transparent)
                            .padding(horizontal = 24.dp)
                    ) {
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
                                    text = "Stock Transfer Status",
                                    color = Color.White,
                                    fontFamily = poppins,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Divider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)
                            FilterRow("Pending", R.drawable.schedule, selectedStatus) {
                                selectedStatus = it; showFilterDialog = false
                            }
                            Divider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)
                            FilterRow("Approved", R.drawable.check_circle_gray, selectedStatus) {
                                selectedStatus = it; showFilterDialog = false
                            }
                            Divider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)
                            FilterRow("Rejected", R.drawable.cancel_gray, selectedStatus) {
                                selectedStatus = it; showFilterDialog = false
                            }
                            Divider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)
                            FilterRow("Lost", R.drawable.ic_lost, selectedStatus) {
                                selectedStatus = it; showFilterDialog = false
                            }
                        }
                    }
                }
            )
        }

        if (shouldNavigateBack) onBack()
    }
}

// 🔹 Common API Call Function
fun handleStockTransferAction(
    statusType: Int,
    context: Context,
    selectedIds: SnapshotStateList<Int>,
    employee: Employee?,
    viewModel: StockTransferViewModel
) {
    if (selectedIds.isEmpty()) {
        Toast.makeText(context, "Please select at least one transfer", Toast.LENGTH_SHORT).show()
        return
    }

    val items = selectedIds.map { id ->
        StockTransferItem(
            Id = id,
            Approved = true,
            Status = statusType
        )
    }

    val request = STApproveRejectRequest(
        StockTransferItems = items,
        ClientCode = employee?.clientCode.orEmpty()
    )

    viewModel.stApproveReject(request)
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
        1 -> "Approved"
        2 -> "Rejected"
        3 -> "Marked as Lost"
        else -> "Processed"
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
                    text = apiMessage,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "$approvedCount Items Request " + actionText,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = transferType,
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
                        text = "Continue",
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
            start = androidx.compose.ui.geometry.Offset(startX, startY),
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



// 🔹 Data Model
data class StockTransfer(
    val id: Int,
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
