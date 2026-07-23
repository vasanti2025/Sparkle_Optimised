package com.loyalstring.rfid.ui.screens
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.lazy.items

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanItemPrint
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanListRow
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanPrintData
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanResponseList
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.remote.data.CompanyDetails
import com.loyalstring.rfid.data.remote.resource.Resource
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.navigation.Screens
import com.loyalstring.rfid.ui.utils.DeliveryChallanListCache
import com.loyalstring.rfid.ui.utils.LIST_PAGE_SIZE
import com.loyalstring.rfid.ui.utils.LazyListLoadMoreEffect
import com.loyalstring.rfid.ui.utils.PrinterManager
import com.loyalstring.rfid.ui.utils.resolvePrintHeader
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.DeliveryChallanViewModel
import com.loyalstring.rfid.viewmodel.LoginViewModel
import com.loyalstring.rfid.worker.LocaleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryChallanListScreen(
    onBack: () -> Unit,
    navController: NavHostController,
) {
    val viewModel: DeliveryChallanViewModel = hiltViewModel()
    val loginViewModel: LoginViewModel = hiltViewModel()
    val context = LocalContext.current
    val employee = remember {
        UserPreferences.getInstance(context).getEmployee(Employee::class.java)
    }



    val userPreferences = UserPreferences.getInstance(context)
    val savedLang = userPreferences.getAppLanguage().ifBlank { "en" }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = currentLocales[0]?.language ?: savedLang
    val localizedContext = LocaleHelper.applyLocale(context, currentLang)

    val challanList by viewModel.challanList.collectAsState()
    val isLoading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()
    var companyName by rememberSaveable { mutableStateOf("") }

    var visibleItems by remember { mutableStateOf(LIST_PAGE_SIZE) }
    var searchQuery by remember { mutableStateOf("") }
    var showPrintDialog by remember { mutableStateOf(false) }
    var selectedPrintData by remember { mutableStateOf<DeliveryChallanPrintData?>(null) }

    val bootstrapRows = remember(employee?.clientCode, employee?.branchNo) {
        employee?.let { emp ->
            DeliveryChallanListCache.readMemoryRows(
                clientCode = emp.clientCode.orEmpty(),
                branchId = emp.branchNo ?: 0,
            )
        }.orEmpty()
    }
    val listForUi = if (challanList.isNotEmpty()) challanList else bootstrapRows

    LaunchedEffect(Unit) {
        employee?.let { emp ->
            val clientCode = emp.clientCode ?: return@let
            val branchId = emp.branchNo ?: 0
            if (bootstrapRows.isNotEmpty()) {
                viewModel.seedRowsIfEmpty(bootstrapRows)
            }
            viewModel.fetchAllChallans(clientCode, branchId)
        }
    }

    val filteredData by remember {
        derivedStateOf {
            if (searchQuery.isNotEmpty()) {
                listForUi.filter {
                    it.ChallanNo.orEmpty().contains(searchQuery, true) ||
                            it.CustomerName.orEmpty().contains(searchQuery, true)
                }
            } else {
                listForUi
            }
        }
    }

    val visibleData = remember(filteredData, visibleItems) {
        filteredData.take(visibleItems)
    }

    // 🔹 Localized table headers
    val headerTitles = listOf(
        localizedContext.getString(R.string.header_s_no),
        localizedContext.getString(R.string.header_challan_no),
        localizedContext.getString(R.string.header_date),
        localizedContext.getString(R.string.header_customer_name),
        localizedContext.getString(R.string.header_qty),
        localizedContext.getString(R.string.header_gross_weight),
        localizedContext.getString(R.string.header_stone_weight),
        localizedContext.getString(R.string.header_diamond_weight),
        localizedContext.getString(R.string.header_net_weight),
        localizedContext.getString(R.string.header_fine_plus_weight),
        localizedContext.getString(R.string.header_tax_amount),
        localizedContext.getString(R.string.header_total_amount),
        localizedContext.getString(R.string.header_branch),
        localizedContext.getString(R.string.header_actions)
    )

    val columnWidths = listOf(
        45.dp,  // S.No
        45.dp,  // Challan No
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
        110.dp   // Actions
    )
   // val companyDetailsState by loginViewModel.companyDetailsResponse.observeAsState()

    LaunchedEffect(employee?.clientCode) {
        employee?.clientCode?.takeIf { it.isNotBlank() }?.let { code ->
            loginViewModel.getCompanyDetails(ClientCodeRequest(code))
        }
    }

    val companyDetailsState: Resource<List<CompanyDetails>>? by
    loginViewModel.companyDetailsResponse.observeAsState()

    LaunchedEffect(companyDetailsState) {
        when (val result = companyDetailsState) {
            is Resource.Success<*> -> {
                companyName = (result.data as? List<CompanyDetails>)?.firstOrNull()?.compName.orEmpty()
            }
            is Resource.Error<*> -> companyName = ""
            else -> Unit
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        GradientTopBar(
            title = localizedContext.getString(R.string.title_delivery_challan_list),
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = localizedContext.getString(R.string.cd_back),
                        tint = Color.White
                    )
                }
            },
            titleTextSize = 20.sp
        )

        DeliverySearchBar(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                visibleItems = LIST_PAGE_SIZE
            },
            localizedContext=localizedContext

        )

        DeliveryChallanTable(
            companyName= companyName,
            navController = navController,
            headerTitles = headerTitles,
            columnWidths = columnWidths,
            data = visibleData,
            totalCount = filteredData.size,
            onLoadMore = {
                if (visibleItems < filteredData.size) visibleItems += LIST_PAGE_SIZE
            },
            isLoading = isLoading && listForUi.isEmpty(),
            context = context,
            localizedContext=localizedContext,
            selectedPrintData = selectedPrintData,
            onSelectedPrintDataChange = { selectedPrintData = it },
            showPrintDialog = showPrintDialog,
            onShowPrintDialogChange = { showPrintDialog = it },

        )

        if (error != null) {
            Text(
                text = error ?: localizedContext.getString(R.string.error_loading_challans),
                color = Color.Red,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
@SuppressLint("MissingPermission")
@Composable
fun DeliveryChallanTable(
    companyName: String,
    navController: NavHostController,
    headerTitles: List<String>,
    columnWidths: List<Dp>,
    data: List<DeliveryChallanListRow>,
    totalCount: Int,
    onLoadMore: () -> Unit,
    isLoading: Boolean,
    context: Context,
    localizedContext: Context,
    selectedPrintData: DeliveryChallanPrintData?,
    onSelectedPrintDataChange: (DeliveryChallanPrintData?) -> Unit,
    showPrintDialog: Boolean,
    onShowPrintDialogChange: (Boolean) -> Unit,

) {
    val sharedScrollState = rememberScrollState()
    val listState = rememberLazyListState()
    val viewModel: DeliveryChallanViewModel = hiltViewModel()
    val employee = remember {
        UserPreferences.getInstance(context).getEmployee(Employee::class.java)
    }
    val organizationName = remember {
        UserPreferences.getInstance(context).getOrganization()
    }
    val printHeader = resolvePrintHeader(
        clientCode = employee?.clientCode,
        companyName = companyName,
        organizationName = organizationName
    )

    var isPrinterConnected by remember { mutableStateOf(false) }
    val activity = context as? Activity
    val printerManager = remember { PrinterManager(context) }
    var showBluetoothControls by remember { mutableStateOf(false) }
    var bluetoothStatus by remember { mutableStateOf("Not connected") }

    var bondedDevices by remember { mutableStateOf<List<android.bluetooth.BluetoothDevice>>(emptyList()) }
    var showDeviceList by remember { mutableStateOf(false) }
    var challanToDelete by remember { mutableStateOf<DeliveryChallanListRow?>(null) }

    LazyListLoadMoreEffect(
        listState = listState,
        loadedCount = data.size,
        totalCount = totalCount,
        onLoadMore = onLoadMore
    )

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
                    text = headerTitles.last(),
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
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    items = data,
                    key = { _, challan -> challan.Id }
                ) { index, challan ->

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
                                formatCreatedOn(challan.CreatedOn),
                                challan.CustomerName ?: "",
                                challan.Qty ?: "0",
                                challan.GrossWt ?: "0.000",
                                challan.StoneWt ?: "0.000",
                                challan.TotalDiamondWeight ?: "0.000",
                                challan.NetWt ?: "0.000",
                                challan.TotalFineMetal ?: "0.000",
                                challan.TotalGSTAmount ?: "0.00",
                                challan.TotalNetAmount ?: "0.00",
                                "_"
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
                        Row(
                            modifier = Modifier
                                .width(columnWidths.last())
                                .height(40.dp),
                            horizontalArrangement = Arrangement.spacedBy(
                                6.dp,
                                Alignment.CenterHorizontally
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 🖨️ Edit Button (left icon)
                            IconButton(onClick = {
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(
                                        context,
                                        context.getString(
                                            R.string.toast_editing_challan,
                                            challan.ChallanNo ?: ""
                                        ),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    navController.navigate("editDeliveryChallan/${challan.Id}")
                                }
                            }, modifier = Modifier.size(26.dp)) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_edit_svg),
                                    contentDescription = localizedContext.getString(R.string.cd_edit_challan),
                                    tint = Color(0xFF37474F),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // ✏️ Print Button (middle icon)

                            IconButton(onClick = {
                                val fullChallan = viewModel.getFullChallan(challan.Id)
                                if (fullChallan != null) {
                                    onSelectedPrintDataChange(fullChallan.toDeliveryChallanPrintData(context))
                                    onShowPrintDialogChange(true)
                                } else {
                                    Toast.makeText(
                                        context,
                                        localizedContext.getString(R.string.error_loading_challans),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }, modifier = Modifier.size(26.dp)) {
                                Icon(
                                    painter = painterResource(id = R.drawable.print_svg),
                                    contentDescription = localizedContext.getString(R.string.cd_print_challan),
                                    tint = Color(0xFF37474F),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // 🗑️ Delete Button (right icon)
                            IconButton(onClick = {
                                challanToDelete = challan
                            }, modifier = Modifier.size(26.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = localizedContext.getString(R.string.delete),
                                    tint = Color(0xFFD32940),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            if (showPrintDialog && selectedPrintData != null) {
                                AlertDialog(
                                    onDismissRequest = {
                                        onShowPrintDialogChange(false)
                                        onSelectedPrintDataChange(null)
                                        showBluetoothControls = false
                                        bluetoothStatus = "Not connected"
                                    },
                                    title = {
                                        Text( localizedContext.getString(R.string.print_options), fontFamily = poppins)
                                    },
                                    text = {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            GradientDialogButtonnew(
                                                text =  localizedContext.getString(R.string.view_pdf),
                                                onClick = {
                                                    val uri = generateDeliveryChallanPdf(context, selectedPrintData!!)
                                                    if (uri != null) {
                                                        openPdfPreview(context, uri)
                                                    } else {
                                                        Toast.makeText(context,  localizedContext.getString(R.string.pdf_not_generated), Toast.LENGTH_SHORT).show()
                                                    }
                                                    onShowPrintDialogChange(false)
                                                }
                                            )

                                            GradientDialogButtonnew(
                                                text =  localizedContext.getString(R.string.bluetooth_printer),
                                                onClick = {
                                                    showBluetoothControls = true
                                                }
                                            )
                                            if (showBluetoothControls) {
                                                Spacer(modifier = Modifier.height(8.dp))

                                                GradientDialogButtonnew(
                                                    text = localizedContext.getString(R.string.connect_bluetooth_printer),
                                                    onClick = {
                                                        if (activity == null) {
                                                            bluetoothStatus = "Activity not found"
                                                            return@GradientDialogButtonnew
                                                        }

                                                        if (hasBluetoothPermissions(activity)) {
                                                            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

                                                            if (bluetoothAdapter == null) {
                                                                bluetoothStatus = "Bluetooth not supported"
                                                                return@GradientDialogButtonnew
                                                            }

                                                            if (!bluetoothAdapter.isEnabled) {
                                                                bluetoothStatus = "Please turn on Bluetooth"
                                                                return@GradientDialogButtonnew
                                                            }

                                                            bondedDevices = bluetoothAdapter.bondedDevices.toList()
                                                            showDeviceList = true

                                                            if (bondedDevices.isEmpty()) {
                                                                bluetoothStatus = "No paired Bluetooth devices found"
                                                            } else {
                                                                bluetoothStatus = "Select printer device"
                                                            }

                                                        } else {
                                                            requestBluetoothPermissions(activity)
                                                            bluetoothStatus = "Please grant Bluetooth permissions"
                                                        }
                                                    }
                                                )

                                                // ✅ ADD DEVICE LIST HERE
                                                if (showDeviceList) {
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .heightIn(max = 220.dp),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = Color(0xFFF5F5F5)
                                                        )
                                                    ) {
                                                        LazyColumn(
                                                            modifier = Modifier.fillMaxWidth()
                                                        ) {
                                                            items(bondedDevices) { device ->
                                                                Text(
                                                                    text = "${device.name ?: "Unknown"}\n${device.address}",
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .clickable {
                                                                            bluetoothStatus = "Connecting to ${device.name ?: device.address}..."
                                                                            isPrinterConnected = false
                                                                            showDeviceList = false

                                                                            printerManager.disconnect()

                                                                            Handler(Looper.getMainLooper()).postDelayed({
                                                                                printerManager.connectBluetooth(device.address) { success, msg ->
                                                                                    bluetoothStatus = msg
                                                                                    isPrinterConnected = success
                                                                                }
                                                                            }, 500)
                                                                        }
                                                                        .padding(10.dp),
                                                                    fontFamily = poppins,
                                                                    fontSize = 13.sp
                                                                )

                                                                Divider()
                                                            }
                                                        }
                                                    }
                                                }

                                                GradientDialogButtonnew(
                                                    text = localizedContext.getString(R.string.print_challan),
                                                    onClick = {
                                                        if (selectedPrintData != null) {
                                                            printerManager.printDeliveryChallanCompact(
                                                                selectedPrintData,
                                                                printHeader,
                                                                clientCode = employee?.clientCode
                                                            ) { _, msg ->
                                                                bluetoothStatus = msg
                                                            }
                                                        } else {
                                                            bluetoothStatus = localizedContext.getString(R.string.no_print_data_found)
                                                        }
                                                    },
                                                    enabled = isPrinterConnected
                                                )

                                                Text(
                                                    text = bluetoothStatus,
                                                    fontFamily = poppins
                                                )
                                            }
                                        }
                                    },
                                    confirmButton = {},
                                    dismissButton = {
                                        TextButton(
                                            onClick = {
                                                onShowPrintDialogChange(false)
                                                onSelectedPrintDataChange(null)
                                                showBluetoothControls = false
                                                bluetoothStatus =  localizedContext.getString(R.string.not_connected)
                                            }
                                        ) {
                                            Text( localizedContext.getString(R.string.cancel))
                                        }
                                    }
                                )
                            }

                            if (challanToDelete != null) {
                                AlertDialog(
                                    onDismissRequest = { challanToDelete = null },
                                    title = {
                                        Text(
                                            text = localizedContext.getString(R.string.delete_confirmation_title),
                                            fontFamily = poppins
                                        )
                                    },
                                    text = {
                                        Text(
                                            text = localizedContext.getString(R.string.delete_confirmation_message),
                                            fontFamily = poppins
                                        )
                                    },
                                    confirmButton = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            GradientDialogButtonnew(
                                                text = localizedContext.getString(R.string.cancel_button),
                                                onClick = { challanToDelete = null },
                                                modifier = Modifier.weight(1f)
                                            )
                                            GradientDialogButtonnew(
                                                text = localizedContext.getString(R.string.delete_button),
                                                onClick = {
                                                    challanToDelete?.let { challan ->
                                                        employee?.clientCode?.let { clientCode ->
                                                            viewModel.deleteDeliveryChallan(
                                                                clientCode = clientCode,
                                                                id = challan.Id ?: 0
                                                            ) { isSuccess ->
                                                                Toast.makeText(
                                                                    context,
                                                                    if (isSuccess) localizedContext.getString(R.string.delete_success_message)
                                                                    else localizedContext.getString(R.string.delete_failed_message),
                                                                    Toast.LENGTH_SHORT
                                                                ).show()
                                                            }
                                                        }
                                                    }
                                                    challanToDelete = null
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    },
                                    dismissButton = {}
                                )
                            }
                         /*   IconButton(onClick = {
                                val printData: DeliveryChallanPrintData = challan.toDeliveryChallanPrintData(context)

                                val uri = generateDeliveryChallanPdf(context, printData)
                                if (uri != null) {
                                    openPdfPreview(context, uri)
                                }
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.print_svg),
                                    contentDescription = localizedContext.getString(R.string.cd_print_challan),
                                    tint = Color(0xFF37474F),
                                    modifier = Modifier.size(18.dp)
                                )
                            }*/
                        }
                    }

                    Divider(color = Color(0xFFE0E0E0))
                }
            }
        }
    }
}


private fun DeliveryChallanResponseList.toDeliveryChallanPrintData(
    context: Context
): DeliveryChallanPrintData {

    // ✅ Safe numeric conversions
    fun str(v: Any?): String = v?.toString()?.trim().orEmpty()
    fun d(v: Any?, def: Double = 0.0): Double = str(v).toDoubleOrNull() ?: def

    val taxable =0.0

    // If your list row has CGST/SGST percent, use it; else default 1.5
    val cgstPercent = d("", 1.5)
    val sgstPercent = d("", 1.5)

    // If amounts are present, use them; else compute
    val cgstAmount = ""?.takeIf { it.isNotBlank() }
        ?: String.format("%.2f", taxable * (cgstPercent / 100))

    val sgstAmount = ""?.takeIf { it.isNotBlank() }
        ?: String.format("%.2f", taxable * (sgstPercent / 100))


 val    itemsForPrint =  this.ChallanDetails?.map { detail ->
        DeliveryChallanItemPrint(
            itemName = detail.DesignName ?: "",
            purity = detail.Purity ?: "",
            pcs = detail.Pieces?.toIntOrNull() ?: detail.qty ?: 0,
            grossWt = detail.GrossWt ?: "0.000 gm",
            stoneWt = detail.TotalStoneWeight ?: "0 gm",
            netWt = detail.NetWt ?: "0.000 gm",
            stoneAmt = detail.StoneAmount?.takeIf { it.isNotBlank() }
                ?: detail.TotalStoneAmount?.takeIf { it.isNotBlank() }
                ?: "0.00",
            ratePerGram = detail.MetalRate ?: "0",
            wastage = detail.FineWastageWt ?: "0%",
            itemAmount = detail.ItemAmount ?: "0.00"
        )
    }
    return DeliveryChallanPrintData(
        branchName = "" ?: "QA",
        city = this.customer?.City?:"",
        createdDateTime = this.CreatedOn ?: "",
        customerName = this.CustomerName ?: "",
        quotationNo = this.ChallanNo ?: (this.Id?.toString() ?: ""),
        phone =this.customer?.Mobile?: "",

        // ✅ List screen normally doesn't have items -> keep empty

        items = itemsForPrint!!,

        taxableAmount = "0.0",

        cgstPercent = cgstPercent,
        cgstAmount = cgstAmount,
        sgstPercent = sgstPercent,
        sgstAmount = sgstAmount,

        totalNetAmount = this.TotalNetAmount ?: "0.00"
    )
}


fun formatCreatedOn(createdOn: String?): String {
    if (createdOn.isNullOrBlank()) return ""
    return try {
        val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val output = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val date = input.parse(createdOn)
        if (date != null) output.format(date) else createdOn
    } catch (e: Exception) {
        e.printStackTrace()
        createdOn
    }
}

@Composable
fun GradientDialogButtonnew(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = if (enabled) {
                        listOf(
                            Color(0xFFD32940),
                            Color(0xFF5231A7)
                        )
                    } else {
                        listOf(
                            Color(0xFFBDBDBD),
                            Color(0xFF9E9E9E)
                        )
                    }
                )
            )
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontFamily = poppins,
            fontSize = 14.sp,
            maxLines = 1
        )
    }
}

@Composable
fun DeliverySearchBar(value: String, onValueChange: (String) -> Unit, localizedContext: Context) {
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
            contentDescription = localizedContext.getString(R.string.cd_search),
            modifier = Modifier.padding(start = 12.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = Color.Black, fontSize = 15.sp),
                cursorBrush = SolidColor(Color.Gray),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = if (value.isNotEmpty()) 36.dp else 12.dp),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search)
            )
            if (value.isEmpty()) {
                Text(
                    text = localizedContext.getString(R.string.hint_search_challan_or_customer),
                    color = Color.Gray,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth()
                        .padding(start = 4.dp, end = 8.dp)
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
                    Icon(
                        Icons.Default.Close,
                        contentDescription = localizedContext.getString(R.string.cd_clear),
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}
