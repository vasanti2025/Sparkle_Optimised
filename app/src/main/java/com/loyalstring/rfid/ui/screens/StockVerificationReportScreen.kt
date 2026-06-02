package com.loyalstring.rfid.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.loyalstring.rfid.data.model.addSingleItem.BranchModel
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.model.report.BatchDetailsResponse
import com.loyalstring.rfid.data.model.report.BatchItem
import com.loyalstring.rfid.data.model.report.Branch
import com.loyalstring.rfid.data.model.report.Category
import com.loyalstring.rfid.data.model.report.Design
import com.loyalstring.rfid.data.model.report.Product
import com.loyalstring.rfid.data.model.report.SessionItem
import com.loyalstring.rfid.data.model.report.StockVerificationReqReport
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.viewmodel.SingleProductViewModel
import com.loyalstring.rfid.viewmodel.StockVerificationViewModel
import com.loyalstring.rfid.viewmodel.UiState
import com.loyalstring.rfid.worker.LocaleHelper

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun StockVerificationReportScreen(
    onBack: () -> Unit,
    navController: NavHostController
) {

    val singleProductViewModel: SingleProductViewModel = hiltViewModel()
    var showDatePicker by remember { mutableStateOf(false) }
    val viewModel: StockVerificationViewModel = hiltViewModel()
    var selectedDate by remember { mutableStateOf(getTodayDate()) }
    var selectedReportType by rememberSaveable { mutableStateOf("INVENTORY") }

    val branchList = singleProductViewModel.branches

    var showBatchFilter by remember { mutableStateOf(false) }

    var fromDate by remember { mutableStateOf(getTodayDate()) }
    var toDate by remember { mutableStateOf(getTodayDate()) }

    var selectedBranchId by remember { mutableStateOf<Int?>(null) }
    val state by viewModel.reportState.collectAsState()
    val context = LocalContext.current
    val userPreferences = UserPreferences.getInstance(context)
    val savedLang = userPreferences.getAppLanguage().ifBlank { "en" }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = currentLocales[0]?.language ?: savedLang
    val localizedContext = LocaleHelper.applyLocale(context, currentLang)
    val employee =
        remember { UserPreferences.getInstance(context).getEmployee(Employee::class.java) }

    val state0 by viewModel.sessionState.collectAsState()
    LaunchedEffect(employee?.clientCode) {
        singleProductViewModel.getAllBranches(
            ClientCodeRequest(employee?.clientCode.toString())
        )
    }

    LaunchedEffect(selectedReportType, selectedDate) {
        /*  viewModel.fetchStockVerificationReport(
              StockVerificationReqReport(
                  ClientCode = employee?.clientCode.toString(),
                  ReportDate = selectedDate
              )
          )*/
        if (selectedReportType == "SCAN") {

            viewModel.fetchStockVerificationReport(
                StockVerificationReqReport(
                    ClientCode = employee?.clientCode.toString(),
                    ReportDate = selectedDate
                )
            )

        } else {

            viewModel.fetchSessions(

                employee?.clientCode.toString(

                )
            )

        }
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = localizedContext.getString(R.string.stock_verification_report),
                navigationIcon = {
                    IconButton(onClick = { onBack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                showCounter = false,
                selectedCount = 0,
                onCountSelected = {},
                titleTextSize = 20.sp
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            Column {


              /*  ReportRadioButtons(
                    selectedReportType = selectedReportType,
                    onSelectionChange = { selectedReportType = it },
                    localizedContext = localizedContext,
                    showFilter = selectedReportType == "INVENTORY",
                    onFilterClick = { showBatchFilter = true }
                )*/
            /*    ReportRadioButtons(
                    selectedReportType = selectedReportType,
                    onSelectionChange = { selectedReportType = it },
                    localizedContext = localizedContext,
                    showFilter = selectedReportType == "INVENTORY",
                    onFilterClick = { showBatchFilter = true },
                    showDateIcon = selectedReportType == "SCAN",
                    onDateClick = { showDatePicker = true }
                )*/

                val reportScrollState = rememberScrollState()
                val batchScrollState = rememberScrollState()
                ReportRadioButtons(
                    selectedReportType = selectedReportType,
                    selectedDate = selectedDate,
                    onSelectionChange = { selectedReportType = it },
                    localizedContext = localizedContext,
                    showFilter = selectedReportType == "INVENTORY",
                    onFilterClick = { showBatchFilter = true },
                    showDateIcon = selectedReportType == "SCAN",
                    onDateClick = { showDatePicker = true }
                )

              /*  if (selectedReportType == "SCAN") {

                    DateSelector(
                        selectedDate = selectedDate,
                        onClick = { showDatePicker = true },
                        localizedContext = localizedContext
                    )

                }*/

                if (selectedReportType == "SCAN") {

                    //ReportHeaderRow(localizedContext)
                   /* Box(modifier = Modifier.horizontalScroll(reportScrollState)) {
                        ReportHeaderRow(localizedContext)
                    }

                    when (state) {

                        is UiState.Loading -> {
                            Box(
                                Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) { Text(localizedContext.getString(R.string.loading))}
                        }

                        is UiState.Success -> {

                            val data = (state as UiState.Success).data

                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp)
                            ) {

                                items(
                                    items = data.Branches ?: emptyList(),
                                    key = { it.BranchId ?: it.hashCode() }
                                ) { branch ->
                                  //  BranchItem(branch, navController, selectedDate)
                                    Box(modifier = Modifier.horizontalScroll(reportScrollState)) {
                                        BranchItem(branch, navController, selectedDate)
                                    }
                                }
                            }
                        }

                        is UiState.Error -> {
                            Text((state as UiState.Error).message)
                        }

                        else -> {}
                    }*/
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .horizontalScroll(reportScrollState)
                    ) {
                        Column(
                            modifier = Modifier.width(360.dp)
                        ) {
                            ReportHeaderRow(localizedContext)

                            when (state) {
                                is UiState.Loading -> {
                                    Box(
                                        Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(localizedContext.getString(R.string.loading))
                                    }
                                }

                                is UiState.Success -> {
                                    val data = (state as UiState.Success).data

                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(
                                            items = data.Branches ?: emptyList(),
                                            key = { it.BranchId ?: it.hashCode() }
                                        ) { branch ->
                                            BranchItem(branch, navController, selectedDate)
                                        }
                                    }
                                }

                                is UiState.Error -> {
                                    Text((state as UiState.Error).message)
                                }

                                else -> {}
                            }
                        }
                    }

                } else {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .horizontalScroll(batchScrollState)
                    ) {
                        Column(
                            modifier = Modifier.width(620.dp)
                        ) {
                            BatchHeaderRow(localizedContext = localizedContext)

                            when (state0) {
                                is UiState.Loading -> {
                                    Text("Loading...", modifier = Modifier.padding(16.dp))
                                }

                                is UiState.Success -> {
                                    val sessions =
                                        (state0 as UiState.Success).data.Sessions ?: emptyList()

                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(
                                            items = sessions,
                                            key = { it.ScanBatchId ?: it.hashCode() }
                                        ) { session ->
                                            SessionItem(session) {
                                                navController.navigate(
                                                    "batch_details_screen/${session.ScanBatchId}"
                                                )
                                            }
                                        }
                                    }
                                }

                                is UiState.Error -> {
                                    Text((state0 as UiState.Error).message)
                                }

                                else -> {}
                            }
                        }
                    }
                }
            }

        }
    }
    if (showDatePicker) {

        val todayMillis = System.currentTimeMillis()

        val datePickerState = rememberDatePickerState(
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= todayMillis
                }
            }
        )

      //  val datePickerState = rememberDatePickerState()

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = formatDate(millis)
                            selectedDate = date

                            viewModel.fetchStockVerificationReport(
                                StockVerificationReqReport(
                                    ClientCode = employee?.clientCode.toString(),
                                    ReportDate = date
                                )
                            )
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(localizedContext.getString(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(localizedContext.getString(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showBatchFilter) {

        BatchFilterDialog(
            fromDate = fromDate,
            toDate = toDate,
            selectedBranchId = selectedBranchId,
            branchList = branchList,
            onDismiss = { showBatchFilter = false },
            localizedContext=localizedContext,
            onApply = { branchId, from, to ->

                selectedBranchId = branchId
                fromDate = from
                toDate = to

                showBatchFilter = false

                viewModel.filterSessions(
                    //   clientCode = employee?.clientCode.toString(),
                    branchId = branchId,
                    fromDate = from,
                    toDate = to
                )
            }
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchFilterDialog(
    fromDate: String,
    toDate: String,
    selectedBranchId: Int?,
    branchList: List<BranchModel>,
    onDismiss: () -> Unit,
    localizedContext: Context,
    onApply: (Int?, String, String) -> Unit
) {

    var from by remember { mutableStateOf(fromDate) }
    var to by remember { mutableStateOf(toDate) }
    var branchId by remember { mutableStateOf(selectedBranchId) }

    var showFromDatePicker by remember { mutableStateOf(false) }
    var showToDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(localizedContext.getString(R.string.filter))},

        text = {

            Column {

                Text(localizedContext.getString(R.string.branch))

                Spacer(Modifier.height(6.dp))

                var expanded by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxWidth()) {

                    OutlinedTextField(
                        value = branchList.find { it.Id == branchId }?.BranchName ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label =  { Text(localizedContext.getString(R.string.branch)) },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { expanded = true }
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {

                        branchList.forEach { branch ->

                            DropdownMenuItem(
                                text = { Text(branch.BranchName ?: "") },
                                onClick = {
                                    branchId = branch.Id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

              /*  val fromInteraction = remember { MutableInteractionSource() }

                OutlinedTextField(
                    value = from,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("From Date") },
                    trailingIcon = {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                    },
                    interactionSource = fromInteraction
                )

                LaunchedEffect(fromInteraction) {
                    fromInteraction.interactions.collect {
                        showFromDatePicker = true
                    }
                }*/
                Box {

                    OutlinedTextField(
                        value = from,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(localizedContext.getString(R.string.from_date)) },
                        trailingIcon = {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                        }
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable {
                                showFromDatePicker = true
                            }
                    )
                }
                Spacer(Modifier.height(12.dp))

                Box {

                    OutlinedTextField(
                        value = to,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(localizedContext.getString(R.string.to_date)) },
                        trailingIcon = {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                        }
                    )

                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable {
                                showToDatePicker = true
                            }
                    )
                }

            /*    val toInteraction = remember { MutableInteractionSource() }

                OutlinedTextField(
                    value = to,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("To Date") },
                    trailingIcon = {
                        Icon(Icons.Default.DateRange, contentDescription = null)
                    },
                    interactionSource = toInteraction
                )

                LaunchedEffect(toInteraction) {
                    toInteraction.interactions.collect {
                        showToDatePicker = true
                    }
                }*/
            }
        },

        confirmButton = {
            TextButton(
                onClick = {
                    onApply(branchId, from, to)
                }
            ) {
                Text(localizedContext.getString(R.string.apply))
            }
        },

        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(localizedContext.getString(R.string.cancel))
            }
        }
    )

    // FROM DATE PICKER
    if (showFromDatePicker) {

        val datePickerState = rememberDatePickerState()

        DatePickerDialog(
            onDismissRequest = { showFromDatePicker = false },
            confirmButton = {

                TextButton(onClick = {

                    datePickerState.selectedDateMillis?.let {

                        from = formatDate(it)

                    }

                    showFromDatePicker = false

                }) {
                    Text(localizedContext.getString(R.string.ok))
                }

            },
            dismissButton = {
                TextButton(onClick = { showFromDatePicker = false }) {
                    Text(localizedContext.getString(R.string.cancel))
                }
            }

        ) {
            DatePicker(state = datePickerState)
        }
    }

    // TO DATE PICKER
// TO DATE PICKER
    if (showToDatePicker) {

        val fromMillis = parseDateToMillis(from)
        val todayMillis = System.currentTimeMillis()

        val datePickerState = rememberDatePickerState(
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis >= fromMillis && utcTimeMillis <= todayMillis
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showToDatePicker = false },
            confirmButton = {

                TextButton(onClick = {

                    datePickerState.selectedDateMillis?.let {

                        to = formatDate(it)

                    }

                    showToDatePicker = false

                }) {
                    Text("OK")
                }

            },
            dismissButton = {
                TextButton(onClick = { showToDatePicker = false }) {
                    Text("Cancel")
                }
            }

        ) {
            DatePicker(state = datePickerState)
        }
    }
}

fun parseDateToMillis(date: String): Long {
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        sdf.parse(date)?.time ?: 0L
    } catch (e: Exception) {
        0L
    }
}

@Composable
fun BatchDetailsScreen(
    scanBatchId: String,
    navController: NavHostController,
    viewModel: StockVerificationViewModel = hiltViewModel(),
    ) {

    val context = LocalContext.current
    val employee =
        UserPreferences.getInstance(context).getEmployee(Employee::class.java)

    val state by viewModel.batchDetailsState.collectAsState()

    LaunchedEffect(scanBatchId) {
        viewModel.fetchBatchDetails(
            clientCode = employee?.clientCode.toString(),
            scanBatchId = scanBatchId
        )
    }


    val userPreferences = UserPreferences.getInstance(context)
    val savedLang = userPreferences.getAppLanguage().ifBlank { "en" }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = currentLocales[0]?.language ?: savedLang
    val localizedContext = LocaleHelper.applyLocale(context, currentLang)

    Scaffold(

        topBar = {
            GradientTopBar(
                title = localizedContext.getString(R.string.batch_details),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                showCounter = false,
                selectedCount = 0,
                onCountSelected = {},
                titleTextSize = 20.sp
            )
        }

    ) { padding ->

        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
        ) {

            when (state) {

                is UiState.Loading -> {
                    Text("Loading...", modifier = Modifier.align(Alignment.Center))
                }

                is UiState.Success -> {

                    val data = (state as UiState.Success<BatchDetailsResponse>).data

                    LazyColumn {

                        item {
                            BatchTableSection(
                                title = localizedContext.getString(R.string.matched_items),
                                count = data.MatchedList?.size ?: 0,
                                color = Color(0xFF2E7D32),
                                items = data.MatchedList ?: emptyList(),
                                localizedContext=localizedContext
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        item {
                            BatchTableSection(
                                title = localizedContext.getString(R.string.unmatched_items),
                                count = data.UnmatchedList?.size ?: 0,
                                color = Color(0xFFC62828),
                                items = data.UnmatchedList ?: emptyList(),
                                localizedContext=localizedContext
                            )
                        }

                    }
                }

                is UiState.Error -> {
                    Text((state as UiState.Error).message)
                }

                else -> {
                    Text(localizedContext.getString(R.string.no_data))
                }
            }
        }
    }
}

@Composable
fun BatchTableSection(
    title: String,
    count: Int,
    color: Color,
    items: List<BatchItem>,
    localizedContext: Context// your model
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {

        Column {

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color.copy(alpha = 0.1f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    localizedContext.getString(R.string.items_count, count),
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }

            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE0E0E0))
                    .padding(vertical = 8.dp)
            ) {

                TableHeader(localizedContext.getString(R.string.item_code), 1.2f)
                TableHeader(localizedContext.getString(R.string.product), 1.3f)
                TableHeader(localizedContext.getString(R.string.branch), 1f)
                TableHeader(localizedContext.getString(R.string.category), 1.1f)
                TableHeader(localizedContext.getString(R.string.rfid), 1f)
               /* TableHeader("Gross", 0.8f)
                TableHeader("Net", 0.8f)*/
            }

            if (items.isEmpty()) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(localizedContext.getString(R.string.no_matched_items))
                }

            } else {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                ) {
                    items(
                        items = items,
                        key = { it.ItemCode ?: it.RFIDCode ?: it.hashCode() }
                    ) { item ->

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            TableCell(item.ItemCode ?: "", 1.2f)
                            TableCell(item.ProductName ?: "N/A", 1.3f)
                            TableCell(item.BranchName ?: "N/A", 1f)
                            TableCell(item.CategoryName ?: "N/A", 1.1f)
                            TableCell(item.RFIDCode ?: "-", 1f)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.TableHeader(text: String, weight: Float) {
    Text(
        text = text,
        modifier = Modifier
            .weight(weight)
            .padding(horizontal = 6.dp),
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp
    )
}

@Composable
fun RowScope.TableCell(text: String, weight: Float) {
    Text(
        text = text,
        modifier = Modifier
            .weight(weight)
            .padding(horizontal = 6.dp),
        fontSize = 12.sp
    )
}

@Composable
fun SessionItem(
    session: SessionItem,
    onClick: (SessionItem) -> Unit
) {

    Row(
        modifier = Modifier
            //.width(620.dp)
           .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(Color(0xFFF5F5F5))
            .clickable { onClick(session) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        TableText(session.BranchName ?: "_", Modifier.weight(1.2f))

        TableText(
            formatDateTime(session.StartedOn),
            Modifier.weight(1f)
        )

        TableText(
            formatDateTime(session.EndedOn),
            Modifier.weight(1f)
        )

        QtyBadge(session.TotalQty, Color(0xFFBBDEFB), Modifier.weight(1f))

        QtyBadge(session.MatchQty, Color(0xFFC8E6C9), Modifier.weight(1f))

        QtyBadge(session.UnmatchQty, Color(0xFFFFCDD2), Modifier.weight(1f))
    }
}

@Composable
fun BatchHeaderRow(localizedContext: Context) {

    Row(
        modifier = Modifier
           // .width(620.dp)
            .fillMaxWidth()
            .background(Color.Black)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        HeaderCell(localizedContext.getString(R.string.branch), Modifier.weight(1.4f))
        HeaderCell(localizedContext.getString(R.string.start), Modifier.weight(1.4f))
        HeaderCell(localizedContext.getString(R.string.end), Modifier.weight(1.4f))
        HeaderCell(localizedContext.getString(R.string.total_qty), Modifier.weight(1f))
        HeaderCell(localizedContext.getString(R.string.match), Modifier.weight(1f))
        HeaderCell(localizedContext.getString(R.string.unmatch), Modifier.weight(1.2f))
    }
}

@Composable
fun HeaderCell(text: String, modifier: Modifier) {
    Text(
        text = text,
        modifier = modifier,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = Color.White
    )
}

@Composable
fun TableText(text: String, modifier: Modifier) {

    Text(
        text = text,
        modifier = modifier.padding(horizontal = 4.dp),
        fontSize = 12.sp,
        textAlign = TextAlign.Center
    )
}

@Composable
fun QtyBadge(value: Int, color: Color, modifier: Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value.toString(),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = Color.Black
        )
    }
}


/*@Composable
fun ReportRadioButtons(
    selectedReportType: String,
    onSelectionChange: (String) -> Unit,
    localizedContext: Context
) {
    val selectedBrush = androidx.compose.ui.graphics.Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF5231A7),
            Color(0xFFD32940)
        )
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .background(
                    brush = if (selectedReportType == "INVENTORY") selectedBrush
                    else androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(Color(0xFFF1F1F1), Color(0xFFF1F1F1))
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { onSelectionChange("INVENTORY") }
                .padding(horizontal = 10.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = localizedContext.getString(R.string.batchwise),
                color = if (selectedReportType == "INVENTORY") Color.White else Color.Black
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .background(
                    brush = if (selectedReportType == "SCAN") selectedBrush
                    else androidx.compose.ui.graphics.Brush.horizontalGradient(
                        colors = listOf(Color(0xFFF1F1F1), Color(0xFFF1F1F1))
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { onSelectionChange("SCAN") }
                .padding(horizontal = 10.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = localizedContext.getString(R.string.consolidated),
                color = if (selectedReportType == "SCAN") Color.White else Color.Black
            )
        }
    }
}*/

/*@Composable
fun ReportRadioButtons(
    selectedReportType: String,
    onSelectionChange: (String) -> Unit,
    localizedContext: Context,
    showFilter: Boolean = false,
    onFilterClick: () -> Unit = {}
) {
    val selectedBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF5231A7),
            Color(0xFFD32940)
        )
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .background(
                    brush = if (selectedReportType == "INVENTORY") selectedBrush
                    else Brush.horizontalGradient(
                        colors = listOf(Color(0xFFF1F1F1), Color(0xFFF1F1F1))
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { onSelectionChange("INVENTORY") }
                .padding(horizontal = 10.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = localizedContext.getString(R.string.batchwise),
                color = if (selectedReportType == "INVENTORY") Color.White else Color.Black
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .background(
                    brush = if (selectedReportType == "SCAN") selectedBrush
                    else Brush.horizontalGradient(
                        colors = listOf(Color(0xFFF1F1F1), Color(0xFFF1F1F1))
                    ),
                    shape = RoundedCornerShape(10.dp)
                )
                .clickable { onSelectionChange("SCAN") }
                .padding(horizontal = 10.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = localizedContext.getString(R.string.consolidated),
                color = if (selectedReportType == "SCAN") Color.White else Color.Black
            )
        }

        if (showFilter) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        brush = selectedBrush,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onFilterClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.filter_gary),
                    contentDescription = localizedContext.getString(R.string.filter),
                    tint = Color.White
                )
            }
        }
    }
}*/

@Composable
fun ReportRadioButtons(
    selectedReportType: String,
    selectedDate: String,
    onSelectionChange: (String) -> Unit,
    localizedContext: Context,
    showFilter: Boolean = false,
    onFilterClick: () -> Unit = {},
    showDateIcon: Boolean = false,
    onDateClick: () -> Unit = {}
) {
    val selectedBrush = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFF5231A7),
            Color(0xFFD32940)
        )
    )

    val unselectedBg = Color(0xFFF4F4F4)
    val borderColor = Color(0xFFD9D9D9)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SelectableTopButton(
                text = localizedContext.getString(R.string.batchwise),
                selected = selectedReportType == "INVENTORY",
                selectedBrush = selectedBrush,
                unselectedColor = unselectedBg,
                borderColor = borderColor,
                onClick = { onSelectionChange("INVENTORY") },
                modifier = Modifier.weight(1f, fill = true)
            )

            SelectableTopButton(
                text = localizedContext.getString(R.string.consolidated),
                selected = selectedReportType == "SCAN",
                selectedBrush = selectedBrush,
                unselectedColor = unselectedBg,
                borderColor = borderColor,
                onClick = { onSelectionChange("SCAN") },
                modifier = Modifier.weight(1f, fill = true)
            )
        }

        if (showFilter) {
            SmallActionButton(
                selectedBrush = selectedBrush,
                onClick = onFilterClick
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = localizedContext.getString(R.string.filter),
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (showDateIcon) {
            DateActionButton(
                dateText = formatDisplayDate(selectedDate),
                borderColor = borderColor,
                onClick = onDateClick
            )
        }
    }
}
@Composable
fun DateActionButton(
    dateText: String,
    borderColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .height(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF4F4F4), RoundedCornerShape(10.dp))
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(start = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = dateText,
            color = Color(0xFF222222),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )

        Icon(
            imageVector = Icons.Default.DateRange,
            contentDescription = null,
            tint = Color(0xFF666666),
            modifier = Modifier.size(16.dp)
        )
    }
}

fun formatDisplayDate(date: String): String {
    return try {
        val input = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val output = java.text.SimpleDateFormat("dd-MM-yy", java.util.Locale.getDefault())
        output.format(input.parse(date)!!)
    } catch (e: Exception) {
        date
    }
}

@Composable
fun SmallActionButton(
    selectedBrush: Brush,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(selectedBrush)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun DateChip(
    dateText: String,
    borderColor: Color
) {
    Box(
        modifier = Modifier
            .height(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = dateText,
            color = Color(0xFF222222),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SelectableTopButton(
    text: String,
    selected: Boolean,
    selectedBrush: Brush,
    unselectedColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .then(
                if (selected) {
                    Modifier.background(selectedBrush, RoundedCornerShape(10.dp))
                } else {
                    Modifier
                        .background(unselectedColor, RoundedCornerShape(10.dp))
                        .border(
                            width = 1.dp,
                            color = borderColor,
                            shape = RoundedCornerShape(10.dp)
                        )
                }
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else Color(0xFF222222),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
fun formatDateTime(dateTime: String?): String {

    if (dateTime.isNullOrEmpty()) return "-"

    return try {
        val inputFormat =
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())

        val outputFormat =
            java.text.SimpleDateFormat("dd/MM/yyyy hh:mm a", java.util.Locale.getDefault())

        val date = inputFormat.parse(dateTime)

        outputFormat.format(date!!)
    } catch (e: Exception) {
        dateTime
    }
}

@Composable
fun DateSelector(
    selectedDate: String,
    onClick: () -> Unit,
    localizedContext: Context
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
    ) {

        OutlinedTextField(
            value = selectedDate,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            label = { Text(localizedContext.getString(R.string.select_date)) },

            trailingIcon = {
                Icon(Icons.Default.DateRange, contentDescription = null)
            },
            singleLine = true,
            interactionSource = interactionSource
        )

        // 👇 THIS makes whole field clickable
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect {
                onClick()
            }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaterialDatePickerDialog(
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    localizedContext:Context
) {

    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(formatDate(millis))
                    }
                }
            ) { Text(localizedContext.getString(R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

fun getTodayDate(): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    return sdf.format(java.util.Date())
}

fun formatDate(millis: Long): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(millis))
}


@Composable
fun ReportHeaderRow(localizedContext: Context) {

    Row(
        modifier = Modifier

            .fillMaxWidth()
            .background(Color.Black)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        HeaderText(localizedContext.getString(R.string.branch), Modifier.weight(1.3f))
        HeaderText(localizedContext.getString(R.string.total_inv), Modifier.weight(0.9f))
        HeaderText(localizedContext.getString(R.string.matched), Modifier.weight(0.9f))
        HeaderText(localizedContext.getString(R.string.unmatched), Modifier.weight(0.9f))
     //   HeaderText("Match", Modifier.weight(1f))
       // HeaderText("Unm", Modifier.weight(1f))
    }
}

@Composable
fun HeaderText(text: String, modifier: Modifier) {
    Text(
        text = text,
        modifier = modifier,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,   // 🔥 Bold text
        textAlign = TextAlign.Center,
        color = Color.White
    )
}

@Composable
fun BranchItem(branch: Branch, navController: NavHostController, selectedDate: String){

    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier

            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },   // ✅ IMPORTANT
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5F5F5)
            )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = branch.BranchName ?: "",
                    modifier = Modifier
                        .weight(1.3f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                CenterBadge(
                    branch.TotalInventoryItems ?: 0,
                    Color(0xFFBBDEFB),
                    Modifier.weight(1f)
                ) {
                    navController.navigate(
                        "detail_screen/" +
                                "${branch.BranchId}/" +
                                "0/" +
                                "0/" +
                                "0/" +
                                "TOTAL/" +
                                selectedDate
                    )
                }

                CenterBadge(
                    branch.TotalScannedItems ?: 0,
                    Color(0xFFC8E6C9),
                    Modifier.weight(1f)
                ) {
                    navController.navigate(
                        "detail_screen/" +
                                "${branch.BranchId}/" +
                                "0/" +
                                "0/" +
                                "0/" +
                                "MATCHED/" +
                                selectedDate
                    )
                }

                CenterBadge(
                    branch.NotScannedItems ?: 0,
                    Color(0xFFFFCDD2),
                    Modifier.weight(1f)
                ) {
                    navController.navigate(
                        "detail_screen/" +
                                "${branch.BranchId}/" +
                                "0/" +
                                "0/" +
                                "0/" +
                                "UNMATCHED/" +
                                selectedDate
                    )
                }
               /* CenterBadge(branch.TotalInventoryItems ?: 0, Color(0xFFBBDEFB), Modifier.weight(1f))
                CenterBadge(branch.TotalScannedItems ?: 0, Color(0xFFC8E6C9), Modifier.weight(1f))
                CenterBadge(branch.NotScannedItems ?: 0, Color(0xFFFFCDD2), Modifier.weight(1f))*/
               // CenterBadge(0, Color(0xFFB2DFDB), Modifier.weight(1f))
                //CenterBadge(branch.UnmatchQty ?: 0, Color(0xFFFFE0B2), Modifier.weight(1f))
            }
        }

        // ✅ OUTSIDE CARD
        if (expanded) {
            branch.Categories?.forEach { category ->
                CategoryItem(
                    branch.BranchId!!.toInt(),category,navController,selectedDate)
            }
        }
    }
}

@Composable
fun CenterBadge(
    value: Int,
    color: Color,
    modifier: Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .clickable(enabled = onClick != null) {
                onClick?.invoke()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value.toString(),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            color = Color.Black
        )
    }
}
@Composable
fun CategoryItem(
    branchId: Int,
    category: Category,
    navController: NavHostController,
    selectedDate: String
) {

    var expanded by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
    ) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF2F2F2)
            )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // 🔹 Small left indicator
                Box(
                    Modifier
                        .size(6.dp)
                        .background(
                            Color.Gray,
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = category.CategoryName ?: "",
                    modifier = Modifier.weight(1.6f),
                    fontSize = 13.sp,
                    color = Color(0xFF444444),
                    textAlign = TextAlign.Center
                )

                // 🔵 TOTAL
                CenterBadge(
                    category.TotalInventoryItems ?: 0,
                    Color(0xFFBBDEFB),
                    Modifier.weight(1f)
                ) {
                    navController.navigate(
                        "detail_screen/" +
                                "$branchId/" +
                                "${category.CategoryId}/" +
                                "0/" +
                                "0/" +
                                "TOTAL/" +
                                selectedDate
                    )
                }

// 🟢 MATCHED
                CenterBadge(
                    category.TotalScannedItems ?: 0,
                    Color(0xFFC8E6C9),
                    Modifier.weight(1f)
                ) {
                    navController.navigate(
                        "detail_screen/" +
                                "$branchId/" +
                                "${category.CategoryId}/" +
                                "0/" +
                                "0/" +
                                "MATCHED/" +
                                selectedDate
                    )
                }

// 🔴 UNMATCHED
                CenterBadge(
                    category.NotScannedItems ?: 0,
                    Color(0xFFFFCDD2),
                    Modifier.weight(1f)
                ) {
                    navController.navigate(
                        "detail_screen/" +
                                "$branchId/" +
                                "${category.CategoryId}/" +
                                "0/" +
                                "0/" +
                                "UNMATCHED/" +
                                selectedDate
                    )
                }
            }
        }

        if (expanded) {
            category.Products?.forEach { product ->
                ProductItem(
                    branchId = branchId,
                    categoryId = category.CategoryId ?: 0,
                    product = product,
                    navController = navController,
                    selectedDate = selectedDate
                )
            }
        }
    }
}

@Composable
fun ProductItem(
    product: Product,
    branchId: Int,
    categoryId: Int?,
    navController: NavHostController,
    selectedDate: String
) {

    var expanded by remember { mutableStateOf(false) }

    Column {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFAFAFA))
                .clickable { expanded = !expanded }
                .padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ◦ product dot
            Box(
                Modifier
                    .size(5.dp)
                    .background(Color.Gray, shape = androidx.compose.foundation.shape.CircleShape)
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = product.ProductName ?: "",
                modifier = Modifier.weight(1.6f),
                fontSize = 13.sp,
                color = Color(0xFF555555),
                textAlign = TextAlign.Center
            )
            CenterBadge(
                product.TotalInventoryItems ?: 0,
                Color(0xFFBBDEFB),
                Modifier.weight(1f)
            ) {
                navController.navigate(
                    "detail_screen/" +
                            "$branchId/" +
                            "$categoryId/" +
                            "${product.ProductId}/" +
                            "0/" +
                            "TOTAL/" +
                            selectedDate
                )
            }
            CenterBadge(
                product.TotalScannedItems ?: 0,
                Color(0xFFC8E6C9),
                Modifier.weight(1f)
            ) {
                navController.navigate(
                    "detail_screen/" +
                            "$branchId/" +
                            "$categoryId/" +
                            "${product.ProductId}/" +
                            "0/" +
                            "MATCHED/" +
                            selectedDate
                )
            }
            CenterBadge(
                product.NotScannedItems ?: 0,
                Color(0xFFFFCDD2),
                Modifier.weight(1f)
            ) {
                navController.navigate(
                    "detail_screen/" +
                            "$branchId/" +
                            "$categoryId/" +
                            "${product.ProductId}/" +
                            "0/" +
                            "UNMATCHED/" +
                            selectedDate
                )
            }
        }

        if (expanded) {
           product.Designs?.forEach { design ->
                DesignItem( branchId = branchId,
                    categoryId = categoryId?: 0,
                    productId = product.ProductId,
                    design=design,
                    navController = navController,
                    selectedDate = selectedDate)
            }
        }
    }
}

@Composable
fun DesignItem( branchId: Int,
                categoryId: Int,
                productId: Int?,
                design: Design,
                navController: NavHostController,
                selectedDate: String) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFFFFFFF))
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // ◦◦ smaller design dot
        Box(
            Modifier
                .size(4.dp)
                .background(Color.LightGray, shape = androidx.compose.foundation.shape.CircleShape)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = design.DesignName ?: "",
            modifier = Modifier.weight(1.6f),
            fontSize = 12.sp,
            color = Color(0xFF777777),
            textAlign = TextAlign.Center
        )

        // 🔵 TOTAL
        CenterBadge(
            design.TotalInventoryItems ?: 0,
            Color(0xFFBBDEFB),
            Modifier.weight(1f)
        ) {
            navController.navigate(
                "detail_screen/" +
                        "$branchId/" +
                        "$categoryId/" +
                        "${productId ?: 0}/" +
                        "${design.DesignId}/" +
                        "TOTAL/" +
                        selectedDate
            )
        }

// 🟢 MATCHED
        CenterBadge(
            design.TotalScannedItems ?: 0,
            Color(0xFFC8E6C9),
            Modifier.weight(1f)
        ) {
            navController.navigate(
                "detail_screen/" +
                        "$branchId/" +
                        "$categoryId/" +
                        "${productId ?: 0}/" +
                        "${design.DesignId}/" +
                        "MATCHED/" +
                        selectedDate
            )
        }

// 🔴 UNMATCHED
        CenterBadge(
            design.NotScannedItems ?: 0,
            Color(0xFFFFCDD2),
            Modifier.weight(1f)
        ) {
            navController.navigate(
                "detail_screen/" +
                        "$branchId/" +
                        "$categoryId/" +
                        "${productId ?: 0}/" +
                        "${design.DesignId}/" +
                        "UNMATCHED/" +
                        selectedDate
            )
        }
    }
}

@Composable
fun Badge(value: Int, color: Color, modifier: Modifier = Modifier) {

    Box(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .background(
                color,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(value.toString(), fontSize = 12.sp)
    }
}
