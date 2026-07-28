package com.loyalstring.rfid.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.local.entity.UserPermissionEntity
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.model.stockTransfer.BranchSelection
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.navigation.Screens
import com.loyalstring.rfid.ui.utils.parseTransferEndpointTypes
import kotlinx.coroutines.launch
import com.loyalstring.rfid.viewmodel.StockTransferViewModel

import com.loyalstring.rfid.data.model.stockTransfer.StockTransferItem
import com.loyalstring.rfid.data.model.stockVerification.AccessibleCompany
import com.loyalstring.rfid.data.remote.data.StockTransferItemData
import com.loyalstring.rfid.data.remote.data.StockTransferRequest
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.viewmodel.UserPermissionViewModel
import com.loyalstring.rfid.worker.LocaleHelper


private const val PRE_COL_SR = 1f
private const val PRE_COL_PNAME = 2.4f
private const val PRE_COL_LABEL = 1.5f
private const val PRE_COL_GWT = 1.2f
private const val PRE_COL_NWT = 1.2f
private const val PRE_COL_ACTION = 1.2f

@SuppressLint("RememberReturnType")
@Composable
fun StockTransferPreviewScreen(
    onBack: () -> Unit,
    navController: NavHostController
) {
    val parentEntry = remember(navController) {
        navController.getBackStackEntry("main_graph")
    }
    val viewModel: StockTransferViewModel = hiltViewModel(parentEntry)
    val previewItems by viewModel.transferPreviewItems.collectAsState()

    val selectedTransferType by viewModel.selectedTransferType.collectAsState()
    val userPermissionViewModel: UserPermissionViewModel = hiltViewModel()
    val isBranchToBranch =
        viewModel.getTransferTypeId(selectedTransferType ?: "") == 15
    val accessibleCompanyNames by viewModel.accessibleCompany.collectAsState()
    val removeCheckedKeys = remember { mutableStateListOf<String>() }

    var showTransferPopup by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val userPreferences = UserPreferences.getInstance(context)
    val savedLang = userPreferences.getAppLanguage().ifBlank { "en" }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = currentLocales[0]?.language ?: savedLang
    val localizedContext = LocaleHelper.applyLocale(context, currentLang)

    val employee = remember {
        UserPreferences.getInstance(context).getEmployee(Employee::class.java)
    }

    val sourceBranchId by viewModel.transferSourceBranchId.collectAsState()
    val destinationBranchId by viewModel.transferDestinationBranchId.collectAsState()
    val allEmployees by userPermissionViewModel.allEmployees.observeAsState(emptyList())
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        employee?.let {
            userPermissionViewModel.loadPermissionsAll(
                it.clientCode.toString()
            )

            Log.d(
                "PermissionLoad",
                "Loading permissions for ClientCode=${it.clientCode} UserId=${it.id}"
            )
        }
    }



    var transferredBy by remember {
        mutableStateOf(
            employee?.userName
                ?: employee?.firstName
                ?: employee?.lastName
                ?: "Admin"
        )
    }
    val stApproveRejectResponse by viewModel.transferStatus.collectAsState()

    var transferredTo by remember { mutableStateOf("Select Emp") }
    var remark by remember { mutableStateOf("") }

    LaunchedEffect(previewItems) {
        Log.d("StockTransferPreview", "previewItems size = ${previewItems.size}")
        previewItems.forEachIndexed { index, item ->
            Log.d(
                "StockTransferPreview",
                "Item $index -> productName=${item.productName}, rfid=${item.rfid}, itemCode=${item.itemCode}, grossWt=${item.grossWeight}, netWt=${item.netWeight}"
            )
        }
    }


// demo list, later API se replace kar dena
    val employeeList = listOf("Admin", "Shubham", "Kajal", "Manager")

    val selectedRemoveItems = previewItems.filter { item ->
        val key = item.itemCode ?: item.rfid ?: ""
        removeCheckedKeys.contains(key)
    }

    val totalQty = previewItems.size
    val selectedQty = selectedRemoveItems.size
    val selectedGrossWeight = selectedRemoveItems.sumOf { it.grossWeight?.toDoubleOrNull() ?: 0.0 }
    val selectedNetWeight = selectedRemoveItems.sumOf { it.netWeight?.toDoubleOrNull() ?: 0.0 }

    val selectAllRemoveChecked = previewItems.isNotEmpty() &&
            previewItems.all { item ->
                val key = item.itemCode ?: item.rfid ?: ""
                removeCheckedKeys.contains(key)
            }
    LaunchedEffect(stApproveRejectResponse) {

        val result = stApproveRejectResponse ?: return@LaunchedEffect

        if (result.isSuccess) {

            Toast.makeText(context,localizedContext.getString(R.string.transfer_success), Toast.LENGTH_SHORT).show()

            showTransferPopup = false
            removeCheckedKeys.clear()
            viewModel.setTransferPreviewItems(emptyList())
            viewModel.clearTransferStatus()
            viewModel.clearApproveResult()

        } else {

            Toast.makeText(
                context,
                result.exceptionOrNull()?.message ?: localizedContext.getString(R.string.transfer_failed),
                Toast.LENGTH_SHORT
            ).show()

            viewModel.clearTransferStatus()
            viewModel.clearApproveResult()
        }
    }

    LaunchedEffect(selectedTransferType) {
        if (viewModel.getTransferTypeId(selectedTransferType ?: "") == 15) {
            val companyNames = userPermissionViewModel.getAccessibleCompany()
            viewModel.loadAccessibleCompany(companyNames)
        }
    }
    Scaffold(
        topBar = {
            GradientTopBar(
                title =localizedContext.getString(R.string.transfer_preview),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                selectedCount = 0,
                titleTextSize = 20.sp
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(vertical = 8.dp)
        ) {
            StockTransferPreviewHeader(
                localizedContext=localizedContext,
                actionTitle = "",
                selectAllChecked = selectAllRemoveChecked,
                onSelectAllChange = { checked ->
                    removeCheckedKeys.clear()
                    if (checked) {
                        previewItems.forEach { item ->
                            val key = item.itemCode ?: item.rfid ?: ""
                            removeCheckedKeys.add(key)
                        }
                    }
                }
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                itemsIndexed(
                    items = previewItems,
                    key = { index, item -> item.itemCode ?: item.rfid ?: "preview_$index" }
                ) { index, item ->

                    val rowKey = item.itemCode ?: item.rfid ?: "preview_$index"

                    StockTransferPreviewRow(
                        sr = index + 1,
                        productName = item.productName ?: "",
                        label = item.rfid ?: item.itemCode ?: "",
                        grossWt = item.grossWeight ?: "0",
                        netWt = item.netWeight ?: "0",
                        checked = removeCheckedKeys.contains(rowKey),
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (!removeCheckedKeys.contains(rowKey)) {
                                    removeCheckedKeys.add(rowKey)
                                }
                            } else {
                                removeCheckedKeys.remove(rowKey)
                            }
                        }
                    )
                }
            }

            StockTransferPreviewBottomBar(
                totalQty = totalQty,
                selectedQty = selectedQty,
                totalGrossWeight = selectedGrossWeight,
                totalNetWeight = selectedNetWeight,
                localizedContext=localizedContext
            )

            StockTransferPreviewActionBar(
                onTransferClick = {
                    Log.d("StockTransferPreview", "Transfer clicked")
                    showTransferPopup = true
                    // TODO: add final transfer API / navigation here
                },
                onInRequestClick = {
                    Log.d("StockTransferPreview", "In Request clicked")
                    navController.navigate(Screens.StockInScreen.route)
                    // TODO: add in-request API here
                },
                onOutRequestClick = {
                    Log.d("StockTransferPreview", "Out Request clicked")
                    navController.navigate(Screens.StockOutScreen.route)
                    // TODO: add out-request API here
                },
                onRemoveClick = {
                    if (removeCheckedKeys.isNotEmpty()) {
                        val updatedList = previewItems.filter { item ->
                            val key = item.itemCode ?: item.rfid ?: ""
                            !removeCheckedKeys.contains(key)
                        }

                        Log.d("StockTransferPreview", "Removing ${removeCheckedKeys.size} items")
                        Log.d("StockTransferPreview", "Updated list size = ${updatedList.size}")

                        viewModel.setTransferPreviewItems(updatedList)
                        removeCheckedKeys.clear()
                    }
                }
            )
        }
        val filteredEmployees = remember(destinationBranchId, allEmployees) {

            if (allEmployees.isNullOrEmpty()) {
                Log.d("EMP_DEBUG", "Employees EMPTY")
                return@remember emptyList()
            }

            val result = allEmployees.filter { emp ->

                val branches = parseBranches(emp.branchSelectionJson)

                val matched = branches.any { it.Id == destinationBranchId }

                matched
            }

            Log.d("EMP_DEBUG", "Filtered Employees = ${result.size}")

            result
        }

        if (showTransferPopup) {
            TransferDetailsDialogNew(
                localizedContext=localizedContext,
                isBranchToBranch= isBranchToBranch,
                transferredBy = transferredBy,
                transferredTo = transferredTo,
                remark = remark,
                employeeList =filteredEmployees,
                onTransferredByChange = { transferredBy = it },
                onTransferredToChange = { transferredTo = it },
                onRemarkChange = { remark = it },
                onDismiss = { showTransferPopup = false },
                onOk = {
                    if (isBranchToBranch && (transferredTo == "Select Emp" || transferredTo.isBlank())) {
                        Toast.makeText(context,localizedContext.getString(R.string.select_employee_error), Toast.LENGTH_SHORT).show()
                        return@TransferDetailsDialogNew
                    }

                    coroutineScope.launch {
                        val clientCode = employee?.clientCode.orEmpty()
                        val transferByEmployee = employee?.employeeId?.toString().orEmpty()
                        val transferTypeId = viewModel.getTransferTypeId(selectedTransferType ?: "")
                        val transferTypeName = selectedTransferType ?: ""
                        val (fromType, toType) = parseTransferEndpointTypes(transferTypeName)

                        val sourceBranch = UserPreferences.getInstance(context)
                            .getBranchID()?.toInt() ?: 0

                        var transferToEmployee = transferByEmployee
                        var destinationBranch = sourceBranch
                        var transferToBranch = sourceBranch.toString()

                        if (isBranchToBranch) {
                            transferToEmployee = transferredTo
                            destinationBranch = destinationBranchId ?: sourceBranch
                            transferToBranch = destinationBranch.toString()
                        }

                        val fromName = viewModel.getPendingFromLocationName()
                        val toName = viewModel.getPendingToLocationName()

                        val sourceId = when {
                            isBranchToBranch -> sourceBranch
                            fromName.isNotBlank() && !fromType.isNullOrBlank() ->
                                viewModel.getEntityIdByName(fromType, fromName)
                            else -> sourceBranch
                        }

                        val destinationId = when {
                            isBranchToBranch -> destinationBranch
                            toName.isNotBlank() && !toType.isNullOrBlank() ->
                                viewModel.getEntityIdByName(toType, toName)
                            else -> sourceBranch
                        }

                        val today = java.text.SimpleDateFormat(
                            "dd-MM-yyyy",
                            java.util.Locale.getDefault()
                        ).format(java.util.Date())

                        val items = previewItems.mapNotNull { item ->
                            val stockId = item.bulkItemId ?: item.itemCode?.toIntOrNull()
                            stockId?.let { StockTransferItemData(it) }
                        }

                        val request = StockTransferRequest(
                            ClientCode = clientCode,
                            StockTransferItems = items,
                            StockType = "labelled",
                            StockTransferTypeName = transferTypeName,
                            TransferTypeId = transferTypeId,
                            TransferByEmployee = transferByEmployee,
                            TransferedToBranch = transferToBranch,
                            TransferToEmployee = transferToEmployee,
                            TransferedBranch = sourceBranch.toString(),
                            Source = sourceId,
                            Destination = destinationId,
                            Remarks = remark,
                            StockTransferDate = today,
                            ReceivedByEmployee = ""
                        )

                        Log.d("StockTransferRequest", request.toString())
                        viewModel.submitStockTransfer(request)
                    }
                }
            )
        }


    }
}
fun parseBranches(json: String?): List<BranchSelection> {
    if (json.isNullOrEmpty()) return emptyList()

    return try {
        val type = object : com.google.gson.reflect.TypeToken<List<BranchSelection>>() {}.type
        com.google.gson.Gson().fromJson(json, type)
    } catch (e: Exception) {
        emptyList()
    }
}

@Composable
fun TransferDetailsDialogNew(
    localizedContext: Context,
    isBranchToBranch: Boolean,
    transferredBy: String,
    transferredTo: String,
    remark: String,
    employeeList: List<UserPermissionEntity>,
    onTransferredByChange: (String) -> Unit,
    onTransferredToChange: (String) -> Unit,
    onRemarkChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onOk: () -> Unit

) {
    var showByDropdown by remember { mutableStateOf(false) }
    var showToDropdown by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFFB71C1C),
                                    Color(0xFF3F51B5)
                                )
                            ),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = localizedContext.getString(R.string.transfer_details),
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                ReadOnlyTransferFieldRow(
                    label = localizedContext.getString(R.string.transferred_by),
                    value = transferredBy
                )

                Spacer(modifier = Modifier.height(10.dp))

               /* TransferFieldRow(
                    label = "Transferred to",
                    value = transferredTo,
                    expanded = showToDropdown,
                    onExpandChange = { showToDropdown = it },
                    options = employeeList,
                    onValueSelected = {
                        onTransferredToChange(it)
                        showToDropdown = false
                    }
                )
*/

                if (isBranchToBranch) {

                    Spacer(modifier = Modifier.height(10.dp))

                    TransferFieldRow(
                        label = localizedContext.getString(R.string.transferred_to),
                        value = transferredTo,
                        expanded = showToDropdown,
                        onExpandChange = { showToDropdown = it },
                        options = employeeList,
                        onValueSelected = {
                            onTransferredToChange(it)
                            showToDropdown = false
                        }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF3F3F3))
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = localizedContext.getString(R.string.remark),
                        color = Color.DarkGray,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                androidx.compose.foundation.text.BasicTextField(
                    value = remark,
                    onValueChange = onRemarkChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color(0xFFF7F7F7), RoundedCornerShape(2.dp))
                        .padding(10.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = Color.Black,
                        fontSize = 14.sp
                    ),
                    decorationBox = { innerTextField ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (remark.isBlank()) {
                                Text(
                                    text = localizedContext.getString(R.string.remark_hint),
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GradientDialogButton(
                        text = localizedContext.getString(R.string.cancel),
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss
                    )

                    GradientDialogButton(
                        text = localizedContext.getString(R.string.ok),
                        modifier = Modifier.weight(1f),
                        onClick = onOk
                    )
                }
            }
        }
    }
}

@Composable
fun ReadOnlyTransferFieldRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .background(Color(0xFFF3F3F3), RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = label,
                color = Color.DarkGray,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .background(Color(0xFFEAEAEA), RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = value,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable



fun TransferFieldRow(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    options: List<UserPermissionEntity>,
    onValueSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // ✅ LABEL BOX (like Transferred By)
        Box(
            modifier = Modifier
                .weight(1f)
                .background(Color(0xFFF3F3F3), RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp, vertical = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = label,
                color = Color.DarkGray,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // ✅ DROPDOWN FIELD
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { onExpandChange(!expanded) },
            modifier = Modifier.weight(1f)
        ) {

            Row(
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .background(Color(0xFFF7F7F7), RoundedCornerShape(4.dp))
                    .clickable { onExpandChange(true) }
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                val selectedEmployee = options.find { it.EmployeeId == value.toIntOrNull() }

                val selectedName = selectedEmployee?.firstName ?: "Select Emp"

                Text(
                    text = selectedName,
                    color = if (value == "Select Emp") Color.Gray else Color.DarkGray,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    painter = painterResource(id = android.R.drawable.arrow_down_float),
                    contentDescription = "Dropdown",
                    tint = Color.DarkGray
                )
            }

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandChange(false) }
            ) {

                options.forEach { option ->

                    DropdownMenuItem(
                        text = {
                            Text("${option.firstName} (${option.EmployeeId})")
                        },
                        onClick = {
                            onValueSelected(option.EmployeeId.toString())
                            Log.d("TransferEmployee", "Selected Employee: ${option.firstName} ID=${option.EmployeeId}")
                            onExpandChange(false)
                        }
                    )
                }
            }
        }
    }
}
@Composable
fun GradientDialogButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFB71C1C),
                        Color(0xFF3F51B5)
                    )
                )
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 15.sp
        )
    }
}


@Composable
fun StockTransferPreviewHeader(
    localizedContext: Context,
    actionTitle: String,
    selectAllChecked: Boolean,
    onSelectAllChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF4A4A4A))
            .height(34.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            localizedContext.getString(R.string.sr),
            modifier = Modifier.weight(PRE_COL_SR),
            color = Color.White,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
        Text(
            localizedContext.getString(R.string.product_name),
            modifier = Modifier.weight(PRE_COL_PNAME),
            color = Color.White,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
        Text(
            localizedContext.getString(R.string.itemcode),
            modifier = Modifier.weight(PRE_COL_LABEL),
            color = Color.White,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
        Text(
            localizedContext.getString(R.string.gross_wt_header),
            modifier = Modifier.weight(PRE_COL_GWT),
            color = Color.White,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
        Text(
            localizedContext.getString(R.string.net_wt_header),
            modifier = Modifier.weight(PRE_COL_NWT),
            color = Color.White,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )

        Row(
            modifier = Modifier.weight(PRE_COL_ACTION),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (actionTitle.isNotBlank()) {
                Text(
                    text = actionTitle,
                    color = Color.White,
                    fontSize = 10.sp
                )
            }
            Checkbox(
                checked = selectAllChecked,
                onCheckedChange = onSelectAllChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = Color.White,
                    uncheckedColor = Color.White,
                    checkmarkColor = Color.Black
                )
            )
        }
    }
}

@Composable
fun StockTransferPreviewRow(
    sr: Int,
    productName: String,
    label: String,
    grossWt: String,
    netWt: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = sr.toString(),
            modifier = Modifier.weight(PRE_COL_SR),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        Text(
            text = productName,
            modifier = Modifier
                .weight(PRE_COL_PNAME)
                .padding(horizontal = 2.dp),
            fontSize = 10.sp,
            textAlign = TextAlign.Start,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = label,
            modifier = Modifier
                .weight(PRE_COL_LABEL)
                .padding(horizontal = 2.dp),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = grossWt,
            modifier = Modifier.weight(PRE_COL_GWT),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        Text(
            text = netWt,
            modifier = Modifier.weight(PRE_COL_NWT),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        Box(
            modifier = Modifier.weight(PRE_COL_ACTION),
            contentAlignment = Alignment.Center
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF6750A4),
                    uncheckedColor = Color.Gray,
                    checkmarkColor = Color.White
                )
            )
        }
    }
}

@Composable
fun StockTransferPreviewBottomBar(
    totalQty: Int,
    selectedQty: Int,
    totalGrossWeight: Double,
    totalNetWeight: Double,
    localizedContext: Context
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFD9D9D9))
            .padding(horizontal = 0.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = localizedContext.getString(R.string.t_qty, totalQty),
            fontSize = 10.sp,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )

        Text(
            text =  localizedContext.getString(R.string.selected_qty,selectedQty),
            fontSize = 10.sp,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )

        Text(
            text = localizedContext.getString(R.string.t_gross_weight, totalGrossWeight),
            fontSize = 10.sp,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )

        Text(
            text = localizedContext.getString(R.string.t_net_weight, totalNetWeight),
            fontSize = 10.sp,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StockTransferPreviewActionBar(
    onTransferClick: () -> Unit,
    onInRequestClick: () -> Unit,
    onOutRequestClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GradientPreviewActionButton(
            text = "Transfer",
            icon = painterResource(id = R.drawable.stock_tr_gr_svg),
            onClick = onTransferClick,
            modifier = Modifier.weight(1f)
        )

        GradientPreviewActionButton(
            text = "InRequest",
            icon = painterResource(id = R.drawable.ic_in_request),
            onClick = onInRequestClick,
            modifier = Modifier.weight(1f)
        )

        GradientPreviewActionButton(
            text = "OutRequest",
            icon = painterResource(id = R.drawable.ic_out_request),
            onClick = onOutRequestClick,
            modifier = Modifier.weight(1f)
        )

        GradientPreviewActionButton(
            text = "Remove",
            icon = painterResource(id = R.drawable.ic_delete_svg),
            onClick = onRemoveClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun GradientPreviewActionButton(
    text: String,
    icon: Painter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradientBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFB71C1C),
            Color(0xFF3F51B5)
        )
    )

    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(gradientBrush)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = text,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = text,
                color = Color.White,
                fontSize = 9.sp,
                maxLines = 1
            )
        }
    }


}