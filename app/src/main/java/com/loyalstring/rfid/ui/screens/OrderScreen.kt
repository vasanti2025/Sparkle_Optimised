package com.loyalstring.rfid.ui.screens


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.RadioButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.sparklepos.models.loginclasses.customerBill.AddEmployeeRequest
import com.example.sparklepos.models.loginclasses.customerBill.EmployeeList
import com.google.gson.Gson
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.AreaBreak
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.AreaBreakType
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.local.entity.OrderItem
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.model.order.CustomOrderItem
import com.loyalstring.rfid.data.model.order.CustomOrderRequest
import com.loyalstring.rfid.data.model.order.CustomOrderResponse
import com.loyalstring.rfid.data.model.order.Customer
import com.loyalstring.rfid.data.model.order.ItemCodeResponse
import com.loyalstring.rfid.data.model.order.Payment
import com.loyalstring.rfid.data.model.order.URDPurchase
import com.loyalstring.rfid.data.remote.data.DailyRateResponse
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.ui.utils.GradientButtonIcon
import com.loyalstring.rfid.ui.utils.NetworkUtils
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.BulkViewModel
import com.loyalstring.rfid.viewmodel.OrderViewModel
import com.loyalstring.rfid.viewmodel.SingleProductViewModel
import com.loyalstring.rfid.viewmodel.UiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.debounce
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.runtime.*

import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach

import kotlinx.coroutines.withContext


@RequiresApi(Build.VERSION_CODES.R)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun OrderScreen(
    onBack: () -> Unit,
    navController: NavHostController,
    userPreferences: UserPreferences,
    orderViewModel: OrderViewModel,
    singleProductViewModel: SingleProductViewModel
) {



    val context = LocalContext.current
    val employee =
        remember { UserPreferences.getInstance(context).getEmployee(Employee::class.java) }

    var shouldNavigateBack by remember { mutableStateOf(false) }

    LaunchedEffect(shouldNavigateBack) {
        if (shouldNavigateBack) {
            kotlinx.coroutines.delay(50)
            onBack()
        }
    }

    val bulkViewModel: BulkViewModel = hiltViewModel()
    var selectedPower by remember { mutableStateOf(30) }
    var selectedCustomer by remember { mutableStateOf<EmployeeList?>(null) }
    //val itemCodeList by orderViewModel.itemCodeResponse.collectAsState()
    val customerSuggestions by orderViewModel.empListFlow.collectAsState(UiState.Loading)

    LaunchedEffect(customerSuggestions) {
        if (customerSuggestions is UiState.Success) {

            val data = (customerSuggestions as UiState.Success<List<EmployeeList>>).data
            Log.d("CustomerList", Gson().toJson(data))

        }
    }

    val editOrder = navController
        .currentBackStackEntry
        ?.savedStateHandle
        ?.get<CustomOrderResponse>("editOrder")

    LaunchedEffect(editOrder) {
        if (editOrder != null) {

            selectedCustomer = editOrder.Customer?.toEmployeeList()


            // ✅ remove after consuming so it won’t run again

        }
    }

    LaunchedEffect(employee?.clientCode) {
        employee?.clientCode?.let { clientCode ->
            withContext(Dispatchers.IO) {
                orderViewModel.getAllEmpList(clientCode)
                orderViewModel.getAllItemCodeList(ClientCodeRequest(clientCode))
                singleProductViewModel.getAllBranches(ClientCodeRequest(clientCode))
                singleProductViewModel.getAllPurity(ClientCodeRequest(clientCode))
                singleProductViewModel.getAllSKU(ClientCodeRequest(clientCode))
            }
        }
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = "Customer Order",
                navigationIcon = {
                    IconButton(onClick = { shouldNavigateBack = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {},
                showCounter = true,
                selectedCount = selectedPower,
                onCountSelected = {
                    selectedPower = it

                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            OrderScreenContent(
                navController = navController,
              //  itemCodeList = itemCodeList,
                userPreferences = userPreferences,
                bulkViewModel = bulkViewModel,
                selectedCustomer = selectedCustomer,
                onCustomerSelected = { selectedCustomer = it },
                selectedPower,
                orderViewModel=orderViewModel,
                singleProductViewModel=singleProductViewModel,
                editOrder=editOrder,

                )
        }
    }
}

// Mapper Extension Function
private fun Customer?.toEmployeeList(): EmployeeList? {
    if (this == null) return null

    return EmployeeList(
        Id = this.Id,
        FirstName = this.FirstName,
        LastName = this.LastName,
        PerAddStreet = this.PerAddStreet,
        CurrAddStreet = this.CurrAddStreet,
        Mobile = this.Mobile,
        Email = this.Email,
        Password = this.Password,
        CustomerLoginId = this.CustomerLoginId,
        DateOfBirth = this.DateOfBirth,
        MiddleName = this.MiddleName,
        PerAddPincode = this.PerAddPincode,
        Gender = this.Gender,
        OnlineStatus = this.OnlineStatus,
        CurrAddTown = this.CurrAddTown,
        CurrAddPincode = this.CurrAddPincode,
        CurrAddState = this.CurrAddState,
        PerAddTown = this.PerAddTown,
        PerAddState = this.PerAddState,
        GstNo = this.GstNo,
        PanNo = this.PanNo,
        AadharNo = this.AadharNo,
        BalanceAmount = this.BalanceAmount,
        AdvanceAmount = this.AdvanceAmount,
        Discount = this.Discount,
        CreditPeriod = this.CreditPeriod,
        FineGold = this.FineGold,
        FineSilver = this.FineSilver,
        ClientCode = this.ClientCode,
        VendorId = this.VendorId,
        AddToVendor = this.AddToVendor,
        CustomerSlabId = this.CustomerSlabId,
        CreditPeriodId = this.CreditPeriodId,
        RateOfInterestId = this.RateOfInterestId,
        CustomerSlab = null,      // not present in Customer
        RateOfInterest = null,    // not present in Customer
        CreatedOn = this.CreatedOn,
        LastUpdated = this.LastUpdated,
        StatusType = this.StatusType,
        Remark = this.Remark,
        Area = this.Area,
        City = this.City,
        Country = this.Country
    )
}


@SuppressLint("UnrememberedMutableState")
@Composable
fun OrderScreenContent(
    navController: NavHostController,
   // itemCodeList: List<ItemCodeResponse>,
    userPreferences: UserPreferences,
    bulkViewModel: BulkViewModel,
    selectedCustomer: EmployeeList?,
    onCustomerSelected: (EmployeeList) -> Unit,
    selectedPower: Int,
    singleProductViewModel: SingleProductViewModel,
    orderViewModel: OrderViewModel,
    editOrder: CustomOrderResponse?
) {
    // PAN state
    var panError by remember { mutableStateOf(false) }
    var gstError by remember { mutableStateOf(false) }
    var itemCodeList by remember { mutableStateOf<List<ItemCodeResponse>>(emptyList()) }
    LaunchedEffect(Unit) {
        orderViewModel.itemCodeResponse.collect { items ->
            itemCodeList = items   // assign collected items into your mutable state
        }
    }
   //var itemCodeList by orderViewModel.itemCodeResponse.collectAsState()
    val context = LocalContext.current
    val isOnline = remember {
        NetworkUtils.isNetworkAvailable(context)
    }
    //val localItemList by bulkViewModel.scannedFilteredItems
    // collect as state directly from BulkViewModel
    LaunchedEffect(Unit) {
        bulkViewModel.getAllItems(context)   // triggers DB fetch
    }

    val localItemList = bulkViewModel.scannedFilteredItems.value

    if (localItemList.isNotEmpty()) {
        Log.d("@@", "Got ${localItemList.size} items from DB")

    }

    Log.d("localItemList","localItemList"+localItemList)





    val df = DecimalFormat("#.00")
// Retrieve logged-in employee from preferences
    val employee = UserPreferences.getInstance(context).getEmployee(Employee::class.java)
    var isEditMode by remember { mutableStateOf(false) }

    orderViewModel.getDailyRate(ClientCodeRequest(employee?.clientCode))

    // Collect the latest rates
    val dailyRates by orderViewModel.getAllDailyRate.collectAsState()


// ViewModels

    var isScanning by remember { mutableStateOf(false) }

// Basic state fields for totals, calculations, item selections
    var selectedItem by remember { mutableStateOf<ItemCodeResponse?>(null) }

    var orderSelectedItem by remember { mutableStateOf<OrderItem?>(null) }
    var firstPress by remember { mutableStateOf(false) }
    var isGstChecked by remember { mutableStateOf(false) }
    var totalAmount by remember { mutableStateOf("0.000") }
    var totalAMt by remember { mutableStateOf(0.0) }
    var quantity by remember { mutableStateOf("") }
    var gst by remember { mutableStateOf("") }
    var gstApplied by remember { mutableStateOf("") }
    var totalNetAmt by remember { mutableStateOf("") }
    var totalGstAmt by remember { mutableStateOf("") }
    var totalPupaseAmt by remember { mutableStateOf("") }
    var totalStoneAmt by remember { mutableStateOf("") }
    var totalStoneWt by remember { mutableStateOf("") }
    var totalDiamondAMt by remember { mutableStateOf("") }
    var totalDiamondWt by remember { mutableStateOf("") }
    var totalNetWt by remember { mutableStateOf("") }
    var totalGrWt by remember { mutableStateOf("") }

    var totalFinemetal by remember { mutableStateOf("") }

// Customer input fields
    var customerName by remember { mutableStateOf("") }
    var customerId by remember { mutableStateOf<Int?>(null) }
    var itemCode by remember { mutableStateOf(TextFieldValue("")) }

// Collecting states from ViewModel
    val isLoading by orderViewModel.isItemCodeLoading.collectAsState()
    val lastOrder by orderViewModel.lastOrderNoresponse.collectAsState()
    val orderSuccess by orderViewModel.orderResponse.collectAsState()
    val items by bulkViewModel.scannedItems.collectAsState()
    val tags by bulkViewModel.scannedTags.collectAsState()
    val scanTrigger by bulkViewModel.scanTrigger.collectAsState()


    val orderRequest by orderViewModel.insertOrderOffline.collectAsState()

    val productList = remember { mutableStateListOf<OrderItem>() }

    LaunchedEffect(Unit) {

        snapshotFlow { productList.toList() } // observe contents
            .collect { list ->
                totalAMt = list.sumOf {orderItem ->
                    val amt = orderItem.itemAmt?.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0) {
                        amt
                    } else {
                        // 🔹 Calculate dynamically if no valid itemAmt
                        val netWt = orderItem.nWt?.toDoubleOrNull() ?: 0.0
                        val rate = dailyRates.find { it.PurityName.equals(orderItem.purity, ignoreCase = true) }?.Rate?.toDoubleOrNull() ?: 0.0
                        //val rate = orderItem.todaysRate?.toDoubleOrNull() ?: 0.0
                        val stoneAmt = orderItem.stoneAmt?.toDoubleOrNull() ?: 0.0
                        val diamondAmt = orderItem.diamondAmt?.toDoubleOrNull() ?: 0.0

                        val makingPercent = orderItem.makingPercentage?.toDoubleOrNull() ?: 0.0
                        val fixMaking = orderItem.makingFixedAmt?.toDoubleOrNull() ?: 0.0
                        val fixWastage = orderItem.makingFixedWastage?.toDoubleOrNull() ?: 0.0

                        val makingAmt = (makingPercent / 100.0) * netWt + fixMaking + fixWastage

                        (netWt * rate) + stoneAmt + diamondAmt + makingAmt

                    }


                }
                Log.d("@@","@@ totalAMt"+totalAMt)
                totalGrWt = list.sumOf { it.grWt?.toDoubleOrNull() ?: 0.0 }.toString()
                totalFinemetal = list.sumOf {
                    val finePer = it.finePer.toDoubleOrNull() ?: 0.0
                    val netWt = it.nWt?.toDoubleOrNull() ?: 0.0
                    (finePer / 100.0) * netWt
                }.toString()
            }

    }

    LaunchedEffect(editOrder) {


        if (editOrder != null) {
            isEditMode = true
            Log.d("EditOrder", "Got order for edit: ${editOrder.Customer.FirstName}")
            // ✅ Prefill customer data
            customerName =
                "${editOrder.Customer.FirstName.orEmpty()} ${editOrder.Customer.LastName.orEmpty()}".trim()
            customerId = editOrder.Customer.Id

            // ✅ Prefill amounts
            totalAmount = editOrder.TotalAmount ?: "0.0"
            gst = editOrder.GST ?: "false"
            gstApplied = editOrder.GSTApplied ?: "false"
            totalNetAmt = editOrder.TotalNetAmount ?: "0"
            totalGstAmt = editOrder.TotalGSTAmount ?: "0"
            totalPupaseAmt = editOrder.TotalPurchaseAmount ?: "0"
            totalStoneAmt = editOrder.TotalStoneAmount ?: "0"
            totalStoneWt = editOrder.TotalStoneWeight ?: "0"
            totalDiamondAMt = editOrder.TotalDiamondAmount ?: "0"
            totalDiamondWt = editOrder.TotalDiamondWeight ?: "0"
            totalNetWt =
                editOrder.CustomOrderItem.sumOf { it.NetWt?.toDoubleOrNull() ?: 0.0 }.toString()
            totalGrWt = editOrder.CustomOrderItem.sumOf { it.GrossWt?.toDoubleOrNull() ?: 0.0 }
                .toString()
            totalFinemetal = editOrder.TotalFineMetal ?: "0"

            // ✅ Prefill product list in Room/State
            orderViewModel.clearOrderItems()
            gst = editOrder.GST ?: "false"
            gstApplied = editOrder.GSTApplied ?: "false"

            // ✅ Sync checkbox with saved GST
            isGstChecked = gstApplied.equals("true", ignoreCase = true)

            // ✅ Recalculate total amount if GST was applied
            val gstPercent = 3.0
            val taxableAmt = totalAMt
            if (isGstChecked) {
                val gstAmt = taxableAmt * gstPercent / 100
                totalAMt = taxableAmt + gstAmt
                totalGstAmt = gstAmt.toString()
            }
            editOrder.CustomOrderItem.forEach { coItem ->
                val orderItem = OrderItem(
                    branchId = coItem.BranchId.toString(),
                    branchName = coItem.BranchName.orEmpty(),
                    exhibition = coItem.Exhibition.orEmpty(),
                    remark = coItem.Remark.orEmpty(),
                    purity = coItem.PurityName.orEmpty(),
                    size = coItem.Size.orEmpty(),
                    length = coItem.Length.orEmpty(),
                    typeOfColor = coItem.TypesOdColors.orEmpty(),
                    screwType = coItem.ScrewType.orEmpty(),
                    polishType = coItem.Polish.orEmpty(),
                    finePer = coItem.FinePercentage.orEmpty(),
                    wastage = coItem.MakingFixedWastage,
                    orderDate = coItem.OrderDate,
                    deliverDate = coItem.DeliverDate,
                    productName = coItem.ProductName.orEmpty(),
                    itemCode = coItem.ItemCode.orEmpty(),
                    rfidCode = coItem.RFIDCode.orEmpty(),
                    itemAmt = coItem.Amount,
                    grWt = coItem.GrossWt,
                    nWt = coItem.NetWt,
                    stoneAmt = coItem.StoneAmount,
                    finePlusWt = coItem.FinePercentage,
                    packingWt = coItem.PackingWeight,
                    totalWt = coItem.TotalWt,
                    stoneWt = coItem.StoneWt,
                    dimondWt = coItem.DiamondWt,
                    sku = coItem.SKU.orEmpty(),
                    qty = coItem.Quantity,
                    hallmarkAmt = "",
                    mrp = coItem.MRP ?: "",
                    image = coItem.Image.orEmpty(),
                    netAmt = "",
                    diamondAmt = coItem.DiamondAmount,
                    categoryId = coItem.CategoryId,
                    categoryName = coItem.CategoryName.orEmpty(),
                    productId = coItem.ProductId,
                    productCode = coItem.ProductCode.orEmpty(),
                    skuId = coItem.SKUId,
                    designid = coItem.DesignId,
                    designName = coItem.DesignName.orEmpty(),
                    purityid = coItem.PurityId,
                    counterId = 0,
                    counterName = "",
                    companyId = 0,
                    epc = "",
                    tid = "",
                    todaysRate = coItem.RatePerGram.orEmpty(),
                    makingPercentage = coItem.MakingPercentage.orEmpty(),
                    makingFixedAmt = coItem.MakingFixed,
                    makingFixedWastage = coItem.MakingFixedWastage,
                    makingPerGram = coItem.MakingPerGram.orEmpty()
                )

                if (!orderItem.itemCode.isNullOrBlank() && orderItem.itemCode != "null") {
                    val alreadyExists = productList.any { it.itemCode == orderItem.itemCode }
                    if (!alreadyExists) {
                        orderViewModel.insertOrderItemToRoom(orderItem)
                        productList.add(orderItem)
                    }
                }
            }
        }else
        {
            customerName = ""
            itemCode = TextFieldValue("")
            orderViewModel.clearOrderItems()
            isEditMode = false
            productList.clear()

        }
    }

    DisposableEffect(Unit) {
        onDispose {
            customerName = ""
            itemCode = TextFieldValue("")
            productList.clear()
            isEditMode = false
            //orderViewModel.clearOrderItems()
        }
    }

// Trigger for refreshing components like dropdowns
    var refreshKey by remember { mutableStateOf(0) }

// Scroll and coroutine scope
    rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

// Handle Order Confirmation
    val onConfirmOrderDetails: (String) -> Unit = { orderDetails ->
        Log.d("OrderDetails", "Order Details Confirmed: $orderDetails")
    }
    val onConfirmOrderDetailsData: (String) -> Unit = { orderDetails ->
        Log.d("OrderDetails", "Order Details Confirmed: $orderDetails")
    }

// --------------------------
// Customer Suggestions Logic
// --------------------------

    val customerSuggestions by orderViewModel.empListFlow.collectAsState(UiState.Loading)


    val filteredCustomers by derivedStateOf {
        when (customerSuggestions) {
            is UiState.Success<*> -> {
                val items = (customerSuggestions as UiState.Success<Any?>).data as List<EmployeeList>
                if (customerName.isBlank()) {
                    items.take(20) // show first 20 when no input
                } else {
                    items.filter {
                        val fullName = "${it.FirstName} ${it.LastName}".trim().lowercase()
                        fullName.contains(customerName.trim().lowercase())
                    }.take(20)
                }
            }
            else -> emptyList()
        }
    }

    LaunchedEffect(customerSuggestions) {
        if (customerSuggestions is UiState.Success) {

            val data = (customerSuggestions as UiState.Success<List<EmployeeList>>).data
            Log.d("CustomerList", Gson().toJson(data))


        }
    }

// ---------------------------
// Customer Add Dialog Control
// ---------------------------
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var customerNameadd by remember { mutableStateOf("") }
    var mobileNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var panNumber by remember { mutableStateOf("") }
    var gstNumber by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }

    val addEmpResponse by orderViewModel.addEmpReposnes.observeAsState()

// Dropdown lists for address
    listOf(
        "Ahmedabad", "Bengaluru", "Chandigarh", "Chennai", "Delhi", "Hyderabad",
        "Jaipur", "Kolkata", "Lucknow", "Mumbai", "Nagpur", "Pune", "Surat", "Vadodara",
        "Bhopal", "Indore", "Coimbatore", "Patna", "Kochi", "Vijayawada", "Agra", "Faridabad",
        "Ghaziabad", "Visakhapatnam", "Rajkot", "Kanpur", "Noida", "Madurai", "Nashik",
        "Ludhiana", "Jodhpur", "Gurugram", "Mysuru", "Bhubaneswar", "Dhanbad",
        "Tiruchirappalli", "Solapur", "Jammu", "Srinagar", "Ranchi", "Aurangabad",
        "Gwalior", "Puducherry", "Mangalore", "Shillong", "Panaji", "Imphal",
        "Agartala", "Dehradun", "Kota", "Udaipur", "Navi Mumbai"
    )

    val stateOptions = listOf(
        "Andhra Pradesh", "Arunachal Pradesh", "Assam", "Bihar", "Chhattisgarh",
        "Goa", "Gujarat", "Haryana", "Himachal Pradesh", "Jharkhand", "Karnataka",
        "Kerala", "Madhya Pradesh", "Maharashtra", "Manipur", "Meghalaya", "Mizoram",
        "Nagaland", "Odisha", "Punjab", "Rajasthan", "Sikkim", "Tamil Nadu", "Telangana",
        "Tripura", "Uttar Pradesh", "Uttarakhand", "West Bengal"
    )

    val countryOptions = listOf("USA", "Canada", "Mexico", "UK", "India")
    var expandedCustomer by remember { mutableStateOf(false) }
    var showDropdownItemcode by remember { mutableStateOf(false) }
    var showEditOrderDialog by remember { mutableStateOf(false) }


// -------------------------
// Invoice Generation Trigger
// -------------------------

    var showInvoice by remember { mutableStateOf(false) }

    val updateResponse by orderViewModel.orderUpdateResponse.collectAsState()
    LaunchedEffect(editOrder) {
        if (editOrder == null) {
            customerName = ""
            itemCode = TextFieldValue("")
            productList.clear()
            isEditMode = false
            orderViewModel.clearOrderItems()
        }
    }
    LaunchedEffect(updateResponse) {
        updateResponse?.let {
            // 🔄 Reset UI state
            customerName = ""
            itemCode = TextFieldValue("")
            productList.clear()
            isEditMode = false

            // 🔄 Reset ViewModel state
            orderViewModel.clearUpdateResponse()
            /*  orderViewModel.clearOrderItems()
              orderViewModel.clearOrderResponse()
              orderViewModel.clearOrderRequest()*/

            // 🔄 Remove editOrder so it won’t prefill next time
            navController.previousBackStackEntry
                ?.savedStateHandle
                ?.remove<CustomOrderResponse>("editOrder")

            // 🔄 Show toast
            Toast.makeText(
                context,
                "Order updated successfully!",
                Toast.LENGTH_SHORT
            ).show()
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.remove<CustomOrderResponse>("editOrder")
            navController.navigate("order_list")

            withContext(Dispatchers.IO) {
                orderViewModel.getAllEmpList(employee?.clientCode.toString())
                orderViewModel.getAllItemCodeList(ClientCodeRequest(employee?.clientCode.toString()))
                singleProductViewModel.getAllBranches(ClientCodeRequest(employee?.clientCode.toString()))
                singleProductViewModel.getAllPurity(ClientCodeRequest(employee?.clientCode.toString()))
                singleProductViewModel.getAllSKU(ClientCodeRequest(employee?.clientCode.toString()))
                singleProductViewModel.getAllBranches(ClientCodeRequest(employee?.clientCode.toString()))
            }

        }
    }


    val onSaveEditedItem: (OrderItem) -> Unit = { updatedItem ->
        val index = productList.indexOfFirst { it.itemCode == updatedItem.itemCode }
        if (index != -1) {
            productList[index] = updatedItem
        }
        showEditOrderDialog = false
    }



    LaunchedEffect(orderRequest) {
        orderRequest?.let {
            if (!isEditMode) {

                val orderResponse = it.toCustomOrderResponse()
                orderViewModel.setOrderResponse(orderResponse)

                orderViewModel.setOrderResponse(orderResponse)
                Toast.makeText(context, "Order Placed Successfully!", Toast.LENGTH_SHORT).show()
                generateTablePdfWithImages(context, orderResponse)
                showInvoice = true
                orderViewModel.clearOrderItems()
                customerName = ""
                itemCode = TextFieldValue("")
                productList.clear()
            }
            orderViewModel.clearOrderRequest()
            orderViewModel.clearOrderResponse()
        }



    }

    LaunchedEffect(orderSuccess) {
        orderSuccess?.let {
            if (!isEditMode) {
                orderViewModel.setOrderResponse(it)
                Toast.makeText(context, "Order Placed Successfully!", Toast.LENGTH_SHORT).show()
                generateTablePdfWithImages(context, it)
                showInvoice = true
                orderViewModel.clearOrderItems()
                customerName = ""
                itemCode = TextFieldValue("")  // reset text field
                productList.clear()
            }
            orderViewModel.clearOrderRequest()
            orderViewModel.clearOrderResponse()
        }
    }

    LaunchedEffect(addEmpResponse) {
        if (addEmpResponse != null) {

            Toast.makeText(context, "Customer added successfully", Toast.LENGTH_SHORT).show()
            orderViewModel.getAllEmpList(employee?.clientCode ?: "")
            // ✅ Clear search so new customer shows in list
            customerName = ""
            productList.clear()
            orderViewModel.clearAddEmpResponse()
        }
    }


    val filteredApiList = remember(itemCode.text, itemCodeList, isLoading) {
        derivedStateOf {
            val query = itemCode.text.trim()
            if (query.isEmpty() || itemCodeList.isEmpty() || isLoading) {
                emptyList()
            } else {
                val firstChar = query.first().toString()
                itemCodeList.filter {
                    it.ItemCode?.contains(firstChar, ignoreCase = true) == true ||
                            it.RFIDCode?.contains(firstChar, ignoreCase = true) == true
                }
            }
        }
    }

    var searchQuery by remember { mutableStateOf("") }

   // var searchQuery by remember { mutableStateOf("") }
    var filteredBulkList by remember { mutableStateOf<List<BulkItem>>(emptyList()) }
    var isFiltering by remember { mutableStateOf(false) }

    LaunchedEffect(itemCode.text, localItemList, isLoading) {
        snapshotFlow { itemCode.text.trim() }
            .onEach {
                // 🚀 Show loading immediately as soon as text changes
                isFiltering = true
            }
            .debounce(300)
            .distinctUntilChanged()
            .collectLatest { query ->
                if (query.isBlank() || localItemList.isNullOrEmpty() || isLoading) {
                    filteredBulkList = emptyList()
                    isFiltering = false
                } else {
                    withContext(Dispatchers.Default) {
                        val results = localItemList.filter {
                            val code = it.itemCode.orEmpty()
                            val rfid = it.rfid.orEmpty()
                            code.contains(query, ignoreCase = true) ||
                                    rfid.contains(query, ignoreCase = true)
                        }.take(50)
                        withContext(Dispatchers.Main) {
                            filteredBulkList = results
                            isFiltering = false
                        }
                    }
                }
            }
    }



    fun BulkItem.toItemCodeResponse(): ItemCodeResponse {
        return ItemCodeResponse(
            Id = this.id ?: 0,
            ProductTitle = this.productName.orEmpty(),
            ItemCode = this.itemCode.orEmpty(),
            RFIDCode = this.rfid.orEmpty(),
            GrossWt = this.grossWeight.orEmpty(),
            NetWt = this.netWeight.orEmpty(),
            TotalStoneWeight = this.stoneWeight.orEmpty(),
            TotalDiamondWeight = this.diamondWeight.orEmpty(),
            CategoryName = this.category.orEmpty(),
            DesignName = this.design.orEmpty(),
            PurityName = this.purity.orEmpty(),
            MakingPerGram = this.makingPerGram.orEmpty(),
            MakingPercentage = this.makingPercent.orEmpty(),
            MakingFixedAmt = this.fixMaking.orEmpty(),
            MakingFixedWastage = this.fixWastage.orEmpty(),
            TotalStoneAmount = this.stoneAmount.orEmpty(),
            TotalDiamondAmount = this.diamondAmount.orEmpty(),
            SKU = this.sku.orEmpty(),
            TIDNumber = this.tid.orEmpty(),
            BoxId = this.boxId ?: 0,
            BoxName = this.boxName.orEmpty(),
            BranchId = this.branchId ?: 0,
            BranchName = this.branchName.orEmpty(),
            PacketId = this.packetId ?: 0,
            PacketName = this.packetName.orEmpty(),
            VendorName = this.vendor.orEmpty(),
            Images = this.imageUrl.orEmpty(),
            Pieces = this.pcs?.toString().orEmpty(),
            TotalWeight = this.totalGwt ?: 0.0,
            MRP = this.mrp?.toString().orEmpty(),
            CounterId = this.counterId ?: 0,
            Stones = emptyList(),
            Diamonds = emptyList(),
            ProductName = this.productName.orEmpty()
        )
    }
    if (localItemList.isNotEmpty()) {

        itemCodeList = remember(localItemList) {
            localItemList.map { it.toItemCodeResponse() }

        }

    }


    val filteredList: List<ItemCodeResponse> =
        filteredApiList.value +
                filteredBulkList.map { it.toItemCodeResponse() }

    Log.d("itemcode list","size"+filteredList.size)




    LaunchedEffect(tags) {

        if (tags.isNotEmpty()) {
            Log.d("RFID", "Tags list: ${tags.map { it.epc }}")
            // Iterate through all scanned EPCs in the `tags` list
            tags.forEachIndexed { index, tag ->
                Log.d("Order Screen", "Scanning tag ${index + 1}: ${tag.epc}")

                // Check if EPC exists before processing
                tag.epc?.let { scannedEpc ->
                    Log.d("Scanned EPC", "Processing EPC: $scannedEpc" +itemCodeList.get(1).TIDNumber)

                    // Find the matched item based on TID from itemCodeList
                    val matchedItem = itemCodeList.find { item ->
                        item.TIDNumber.equals(
                            scannedEpc,
                            ignoreCase = true
                        ) // Match based on TID
                    }

                    if (matchedItem != null) {
                        Log.d("Match Found", "Item: ${matchedItem.ProductName}")

                        // Check if the product already exists in the productList based on TID
                        val existingProduct = productList.find { product ->
                            product.tid == matchedItem.TIDNumber // Match based on TID
                        }

                        if (existingProduct == null) {
                            Log.d("existingProduct", "Item: ${matchedItem.ItemCode}")
                            // If the product doesn't exist, create a new product
                            selectedItem = matchedItem
                            val baseUrl =
                                "https://rrgold.loyalstring.co.in/" // Base URL for images
                            val imageString = selectedItem?.Images.toString()
                            val lastImagePath =
                                imageString.split(",").lastOrNull()?.trim()
                            "$baseUrl$lastImagePath"

                            val netWt: Double = (selectedItem?.GrossWt?.toDoubleOrNull()
                                ?: 0.0) - (selectedItem?.TotalStoneWeight?.toDoubleOrNull()
                                ?: 0.0)

                            val finePercent = selectedItem?.FinePercent?.toDoubleOrNull() ?: 0.0
                            val wastagePercent =
                                selectedItem?.WastagePercent?.toDoubleOrNull() ?: 0.0


                            ((finePercent / 100.0) * netWt) + ((wastagePercent / 100.0) * netWt)
                            val metalAmt: Double = (selectedItem?.NetWt?.toDoubleOrNull()
                                ?: 0.0) * (selectedItem?.TodaysRate?.toDoubleOrNull() ?: 0.0)

                            val makingPercentage =
                                selectedItem?.MakingPercentage?.toDoubleOrNull() ?: 0.0
                            val fixMaking =
                                selectedItem?.MakingFixedAmt?.toDoubleOrNull() ?: 0.0
                            val extraMakingPercent =
                                selectedItem?.MakingPercentage?.toDoubleOrNull() ?: 0.0
                            val fixWastage =
                                selectedItem?.MakingFixedWastage?.toDoubleOrNull() ?: 0.0

                            val makingAmt: Double =
                                ((makingPercentage / 100.0) * netWt) +
                                        fixMaking +
                                        ((extraMakingPercent / 100.0) * netWt) +
                                        fixWastage

                            val totalStoneAmount =
                                selectedItem?.TotalStoneAmount?.toDoubleOrNull() ?: 0.0
                            val diamondAmount =
                                selectedItem?.DiamondPurchaseAmount?.toDoubleOrNull() ?: 0.0
                            val safeMetalAmt = metalAmt
                            val safeMakingAmt = makingAmt
                            val rate = dailyRates.find { it.PurityName.equals(selectedItem?.PurityName, ignoreCase = true) }?.Rate?.toDoubleOrNull() ?: 0.0

                            val itemAmt: Double = (selectedItem?.NetWt?.toDoubleOrNull() ?: 0.0) * rate
                                //totalStoneAmount + diamondAmount + safeMetalAmt + safeMakingAmt

                            // Create new OrderItem with necessary details
                            val newProduct = OrderItem(
                                branchId = selectedItem?.BranchId.toString(),
                                branchName = selectedItem?.BranchName.toString(),
                                exhibition = "",
                                remark = "",
                                purity = selectedItem?.PurityName.toString(),
                                size = selectedItem?.Size.toString(),
                                length = "",
                                typeOfColor = selectedItem?.Colour.toString(),
                                screwType = "",
                                polishType = "",
                                finePer = selectedItem?.FinePercent.toString(),
                                wastage = selectedItem?.WastagePercent.toString(),
                                orderDate = "",
                                deliverDate = "",
                                productName = selectedItem?.ProductName?:"",
                                itemCode = selectedItem?.ItemCode ?: "",
                                rfidCode = selectedItem?.RFIDCode.toString(),
                                itemAmt = itemAmt.toString(),
                                grWt = selectedItem?.GrossWt,
                                nWt = selectedItem?.NetWt,
                                stoneAmt = selectedItem?.TotalStoneAmount,
                                finePlusWt = "",
                                packingWt = selectedItem?.PackingWeight.toString(),
                                totalWt = selectedItem?.TotalWeight.toString(),
                                stoneWt = selectedItem?.TotalStoneWeight.toString(),
                                dimondWt = selectedItem?.DiamondWeight.toString(),
                                sku = selectedItem?.SKU.toString(),
                                qty = selectedItem?.ClipQuantity.toString(),
                                hallmarkAmt = selectedItem?.HallmarkAmount.toString(),
                                mrp = selectedItem?.MRP.toString(),
                                image = lastImagePath.toString(),
                                netAmt = "",
                                diamondAmt = selectedItem?.TotalDiamondAmount.toString(),
                                categoryId = selectedItem?.CategoryId,
                                categoryName = selectedItem?.CategoryName!!,
                                productId = selectedItem?.ProductId ?: 0,
                                productCode = selectedItem?.ProductCode ?: "",
                                skuId            = selectedItem?.SKUId ?: 0,
                                designid         = selectedItem?.DesignId ?: 0,
                                designName       = selectedItem?.DesignName ?: "",
                                purityid         = selectedItem?.PurityId ?: 0,
                                counterId        = selectedItem?.CounterId ?: 0,
                                counterName      = "",
                                companyId        = selectedItem?.CompanyId ?: 0,
                                epc              = selectedItem?.TIDNumber ?: "",
                                tid              = selectedItem?.TIDNumber ?: "",
                                todaysRate       = selectedItem?.TodaysRate?.toString() ?: "0",
                                makingPercentage = selectedItem?.MakingPercentage?.toString() ?: "0",
                                makingFixedAmt   = selectedItem?.MakingFixedAmt?.toString() ?: "0",
                                makingFixedWastage = selectedItem?.MakingFixedWastage?.toString() ?: "0",
                                makingPerGram    = selectedItem?.MakingPerGram?.toString() ?: "0"
                            )
                            Log.d(
                                "Added to Product List",
                                "Product added: ${newProduct.productName}"
                            )

                            // Insert the new product into the database
                            if ((!newProduct.itemCode.isNullOrBlank() && newProduct.itemCode != "null") ||
                                (!newProduct.rfidCode.isNullOrBlank() && newProduct.rfidCode != "null")) {

                                val alreadyExists = productList.any {
                                    it.itemCode == newProduct.itemCode ||
                                            it.rfidCode == newProduct.rfidCode ||
                                            it.tid == newProduct.tid
                                }

                                if (!alreadyExists) {
                                    orderViewModel.insertOrderItemToRoom(newProduct)
                                    productList.add(newProduct)
                                    Log.d("Added", "New product added: ${newProduct.itemCode} / ${newProduct.rfidCode}")
                                } else {
                                    Log.d("Duplicate", "Skipped duplicate: ${newProduct.itemCode} / ${newProduct.rfidCode}")
                                }
                            }
                        } else {
                            Log.d(
                                "Already Exists",
                                "Product already exists in the list: ${existingProduct.productName}"
                            )
                        }

                    } else {
                        Log.d("No Match", "No item matched with scanned TID")
                    }
                }
            }
        }

    }


    LaunchedEffect(scanTrigger) {
        scanTrigger?.let { type ->
            when (type) {
                "scan" -> if (items.size != 1) bulkViewModel.startScanning(30)
                "barcode" -> bulkViewModel.startBarcodeScanning(context)
            }
            bulkViewModel.clearScanTrigger()
        }
    }


    // ✅ Set barcode scan callback ONCE
    // ✅ This is where you reactively compute matchedItem
    val matchedItem by remember(itemCode, itemCodeList) {
        derivedStateOf {

            itemCodeList.find { it.RFIDCode == itemCode.text }
        }
    }

// ✅ Automatically update selectedItem whenever matchedItem changes
    LaunchedEffect(matchedItem) {

        selectedItem = matchedItem
        if (itemCode.text.isNotEmpty()) {

            val baseUrl =
                "https://rrgold.loyalstring.co.in/" // Base URL for images
            val imageString = selectedItem?.Images.toString()
            val lastImagePath =
                imageString.split(",").lastOrNull()?.trim()
            "$baseUrl$lastImagePath"
            val newProduct = OrderItem(
                branchId = selectedItem?.BranchId.toString(),
                branchName = selectedItem?.BranchName.toString(),
                exhibition = "",
                remark = "",
                purity = selectedItem?.PurityName.toString(),
                size = selectedItem?.Size.toString(),
                length = "",
                typeOfColor = selectedItem?.Colour.toString(),
                screwType = "",
                polishType = "",
                finePer = selectedItem?.FinePercent.toString(),
                wastage = selectedItem?.WastagePercent.toString(),
                orderDate = "",
                deliverDate = "",
                productName = selectedItem?.ProductName.toString(),
                itemCode = selectedItem?.ItemCode ?: "",
                rfidCode = selectedItem?.RFIDCode.toString(),
                itemAmt = "",
                grWt = selectedItem?.GrossWt,
                nWt = selectedItem?.NetWt,
                stoneAmt = selectedItem?.TotalStoneAmount,
                finePlusWt = "",
                packingWt = selectedItem?.PackingWeight.toString(),
                totalWt = selectedItem?.TotalWeight.toString(),
                stoneWt = selectedItem?.TotalStoneWeight.toString(),
                dimondWt = selectedItem?.DiamondWeight.toString(),
                sku = selectedItem?.SKU.toString(),
                qty = selectedItem?.ClipQuantity.toString(),
                hallmarkAmt = selectedItem?.HallmarkAmount.toString(),
                mrp = selectedItem?.MRP.toString(),
                image = lastImagePath.toString(),
                netAmt = "",
                diamondAmt = selectedItem?.TotalDiamondAmount.toString(),
                categoryId = selectedItem?.CategoryId,

                categoryName = selectedItem?.CategoryName ?: "",
                productId = selectedItem?.ProductId ?: 0,
                productCode = selectedItem?.ProductCode ?: "",
                skuId = selectedItem?.SKUId ?: 0,
                designid = selectedItem?.DesignId ?: 0,
                designName = selectedItem?.DesignName ?: "",
                purityid = selectedItem?.PurityId ?: 0,
                counterId = selectedItem?.CounterId ?: 0,
                counterName = "",
                companyId = 0,
                epc = selectedItem?.TIDNumber ?: "",
                tid = selectedItem?.TIDNumber ?: "",
                todaysRate = selectedItem?.TodaysRate?.toString() ?: "0",
                makingPercentage = selectedItem?.MakingPercentage?.toString() ?: "0",
                makingFixedAmt = selectedItem?.MakingFixedAmt?.toString() ?: "0",
                makingFixedWastage = selectedItem?.MakingFixedWastage?.toString() ?: "0",
                makingPerGram = selectedItem?.MakingPerGram?.toString() ?: "0"


            )
            //   productList.add(newProduct) // Add to productList if it doesn't already exist
            Log.d(
                "Added to Product List",
                "Product added: ${newProduct.productName}"
            )

            // Insert the new product into the database
            if ((!newProduct.itemCode.isNullOrBlank() && newProduct.itemCode != "null") ||
                (!newProduct.rfidCode.isNullOrBlank() && newProduct.rfidCode != "null")) {

                val alreadyExists = productList.any {
                    it.itemCode == newProduct.itemCode ||
                            it.rfidCode == newProduct.rfidCode ||
                            it.tid == newProduct.tid
                }

                if (!alreadyExists) {
                    orderViewModel.insertOrderItemToRoom(newProduct)
                    productList.add(newProduct)
                    Log.d("Added", "New product added: ${newProduct.itemCode} / ${newProduct.rfidCode}")
                } else {
                    Log.d("Duplicate", "Skipped duplicate: ${newProduct.itemCode} / ${newProduct.rfidCode}")
                }
            }
        }


    }

// ✅ This is your barcode scanner logic
    LaunchedEffect(Unit) {

        bulkViewModel.barcodeReader.openIfNeeded()
        bulkViewModel.barcodeReader.setOnBarcodeScanned { scanned ->
            bulkViewModel.onBarcodeScanned(scanned)
            bulkViewModel.setRfidForAllTags(scanned)
            Log.d("RFID Code", scanned)
            itemCode = TextFieldValue(scanned) // triggers recomposition

            val matchedItem = itemCodeList.find { item ->
                item.RFIDCode.equals(
                    scanned,
                    ignoreCase = true
                ) // Match based on TID
            }

            if (matchedItem != null) {
                if (productList.none { it.itemCode == matchedItem?.ItemCode && it.tid == matchedItem?.TIDNumber }) {

                    Log.d("Match Found", "Item: ${matchedItem.ItemCode}")

                    // Check if the product already exists in the database based on TID (or SKU)
                    val existingProduct = productList.find { product ->
                        product.itemCode == matchedItem.ItemCode // Match based on TID
                    }

                    if (existingProduct == null) {
                        selectedItem = matchedItem
                        val netWt: Double = (selectedItem?.GrossWt?.toDoubleOrNull()
                            ?: 0.0) - (selectedItem?.TotalStoneWeight?.toDoubleOrNull()
                            ?: 0.0)

                        val finePercent =
                            selectedItem?.FinePercent?.toDoubleOrNull() ?: 0.0
                        val wastagePercent =
                            selectedItem?.WastagePercent?.toDoubleOrNull() ?: 0.0


                        ((finePercent / 100.0) * netWt) + ((wastagePercent / 100.0) * netWt)
                        val metalAmt: Double =
                            (selectedItem?.NetWt?.toDoubleOrNull()
                                ?: 0.0) * (selectedItem?.TodaysRate?.toDoubleOrNull()
                                ?: 0.0)

                        val makingPercentage =
                            selectedItem?.MakingPercentage?.toDoubleOrNull() ?: 0.0
                        val fixMaking =
                            selectedItem?.MakingFixedAmt?.toDoubleOrNull() ?: 0.0
                        val extraMakingPercent =
                            selectedItem?.MakingPercentage?.toDoubleOrNull() ?: 0.0
                        val fixWastage =
                            selectedItem?.MakingFixedWastage?.toDoubleOrNull()
                                ?: 0.0

                        val makingAmt: Double =
                            ((makingPercentage / 100.0) * netWt) +
                                    fixMaking +
                                    ((extraMakingPercent / 100.0) * netWt) +
                                    fixWastage

                        val totalStoneAmount =
                            selectedItem?.TotalStoneAmount?.toDoubleOrNull() ?: 0.0
                        val diamondAmount =
                            selectedItem?.DiamondPurchaseAmount?.toDoubleOrNull()
                                ?: 0.0
                        val safeMetalAmt = metalAmt
                        val safeMakingAmt = makingAmt
                        val rate = dailyRates.find { it.PurityName.equals(selectedItem?.PurityName, ignoreCase = true) }?.Rate?.toDoubleOrNull() ?: 0.0

                        val itemAmt: Double = (selectedItem?.NetWt?.toDoubleOrNull() ?: 0.0) * rate
                        val baseUrl =
                            "https://rrgold.loyalstring.co.in/" // Replace with actual base URL
                        val imageString = selectedItem?.Images.toString()
                        val lastImagePath =
                            imageString.split(",").lastOrNull()?.trim()
                        "$baseUrl$lastImagePath"
                        // If the product doesn't exist in productList, add it and insert into database
                        val newProduct = OrderItem(
                            branchId = selectedItem?.BranchId.toString(),
                            branchName = selectedItem?.BranchName.toString(),
                            exhibition = "",
                            remark = "",
                            purity = selectedItem?.PurityName.toString(),
                            size = selectedItem?.Size.toString(),
                            length = "",
                            typeOfColor = selectedItem?.Colour.toString(),
                            screwType = "",
                            polishType = "",
                            finePer = selectedItem?.FinePercent.toString(),
                            wastage = selectedItem?.WastagePercent.toString(),
                            orderDate = "",
                            deliverDate = "",
                            productName = selectedItem?.ProductName.toString(),
                            itemCode = selectedItem?.ItemCode ?: "",
                            rfidCode = selectedItem?.RFIDCode.toString(),
                            itemAmt = itemAmt.toString(),
                            grWt = selectedItem?.GrossWt,
                            nWt = selectedItem?.NetWt,
                            stoneAmt = selectedItem?.TotalStoneAmount,
                            finePlusWt = "",
                            packingWt = selectedItem?.PackingWeight.toString(),
                            totalWt = selectedItem?.TotalWeight.toString(),
                            stoneWt = selectedItem?.TotalStoneWeight.toString(),
                            dimondWt = selectedItem?.DiamondWeight.toString(),
                            sku = selectedItem?.SKU.toString(),
                            qty = selectedItem?.ClipQuantity.toString(),
                            hallmarkAmt = selectedItem?.HallmarkAmount.toString(),
                            mrp = selectedItem?.MRP.toString(),
                            image = lastImagePath.toString(),
                            netAmt = "",
                            diamondAmt = selectedItem?.TotalDiamondAmount.toString(),
                            categoryId = selectedItem?.CategoryId,

                            categoryName = selectedItem?.CategoryName ?: "",
                            productId = selectedItem?.ProductId ?: 0,
                            productCode = selectedItem?.ProductCode ?: "",
                            skuId = selectedItem?.SKUId ?: 0,
                            designid = selectedItem?.DesignId ?: 0,
                            designName = selectedItem?.DesignName ?: "",
                            purityid = selectedItem?.PurityId ?: 0,
                            counterId = selectedItem?.CounterId ?: 0,
                            counterName = "",
                            companyId = 0,
                            epc = selectedItem?.TIDNumber ?: "",
                            tid = selectedItem?.TIDNumber ?: "",
                            todaysRate = selectedItem?.TodaysRate?.toString() ?: "0",
                            makingPercentage = selectedItem?.MakingPercentage?.toString() ?: "0",
                            makingFixedAmt = selectedItem?.MakingFixedAmt?.toString() ?: "0",
                            makingFixedWastage = selectedItem?.MakingFixedWastage?.toString()
                                ?: "0",
                            makingPerGram = selectedItem?.MakingPerGram?.toString() ?: "0"


                        )

                        Log.d(
                            "Added to Product List",
                            "Product added: ${newProduct.productName}"
                        )

                        // Insert the new product into the database
                        if ((!newProduct.itemCode.isNullOrBlank() && newProduct.itemCode != "null") ||
                            (!newProduct.rfidCode.isNullOrBlank() && newProduct.rfidCode != "null")) {

                            val alreadyExists = productList.any {
                                it.itemCode == newProduct.itemCode ||
                                        it.rfidCode == newProduct.rfidCode ||
                                        it.tid == newProduct.tid
                            }

                            if (!alreadyExists) {
                                orderViewModel.insertOrderItemToRoom(newProduct)
                                productList.add(newProduct)
                                Log.d("Added", "New product added: ${newProduct.itemCode} / ${newProduct.rfidCode}")
                            } else {
                                Log.d("Duplicate", "Skipped duplicate: ${newProduct.itemCode} / ${newProduct.rfidCode}")
                            }
                        }
                    } else {
                        Log.d(
                            "Already Exists",
                            "Product already exists in the list: ${existingProduct.productName}"
                        )
                    }

                }
            }else {
                Log.d("No Match", "No item matched with scanned TID")
            }

        }

    }
    var nextOrderNo = remember { mutableStateOf(0) }
    LaunchedEffect(lastOrder) {
        if (!isEditMode) {
            lastOrder.LastOrderNo.toIntOrNull()?.let { last ->
                nextOrderNo.value = last + 1
                Log.d("Order", "Last order number: $last")
                Log.d("Order", "Next order number: ${nextOrderNo.value}")
            }
        }
    }
    LaunchedEffect(productList) {

        totalStoneAmt = productList.sumOf { it.stoneAmt?.toDoubleOrNull() ?: 0.0 }.toString()
        totalNetAmt = productList.sumOf { it.netAmt.toDoubleOrNull() ?: 0.0 }.toString()
        // totalGstAmt= productList.sumOf { it.to?.toDoubleOrNull() ?: 0.0 }.toString()
        totalPupaseAmt = productList.sumOf { it.itemAmt?.toDoubleOrNull() ?: 0.0 }.toString()
        // totalStoneAmt = productList.sumOf { it.stoneAmt?.toDoubleOrNull() ?: 0.0 }.toString()
        totalStoneWt = productList.sumOf { it.stoneWt.toDoubleOrNull() ?: 0.0 }.toString()
        totalDiamondAMt = productList.sumOf { it.diamondAmt.toDoubleOrNull() ?: 0.0 }.toString()
        totalDiamondWt = productList.sumOf { it.dimondWt.toDoubleOrNull() ?: 0.0 }.toString()
        totalDiamondWt = productList.sumOf { it.dimondWt.toDoubleOrNull() ?: 0.0 }.toString()
        // totalAMt = productList.sumOf { it.itemAmt?.toDoubleOrNull() ?: 0.0 }.toString()

        /*  totalAMt = productList.sumOf {
        val netWt = it.nWt?.toDoubleOrNull() ?: 0.0
        val rate = it.todaysRate.toDoubleOrNull() ?: 0.0
        netWt * rate
    }.toDouble()*/
        totalAMt = productList.sumOf { it.itemAmt?.toDoubleOrNull() ?: 0.0 }
        totalGrWt = productList.sumOf { it.grWt?.toDoubleOrNull() ?: 0.0 }.toString()
        totalFinemetal = productList.sumOf {
            val finePer = it.finePer.toDoubleOrNull() ?: 0.0
            val netWt = it.nWt?.toDoubleOrNull() ?: 0.0
            (finePer / 100.0) * netWt
        }.toString()

        // quantity=productList.sumOf { it.qty?.toDoubleOrNull() ?: 0.0 }.toString()

    }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val allItems by orderViewModel.allOrderItems.collectAsState()
    if(allItems!=null)
    {

    }
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    LaunchedEffect(Unit) {
        orderViewModel.getAllOrderItemsFromRoom() // ✅ triggers fetching/updating
    }

    Scaffold(
        bottomBar = {
            Column {
                Spacer(modifier = Modifier.height(2.dp))
                GstRowView(
                    gstPercent = 3.0, // optional because of default value
                    totalAmount = totalAMt, // required
                    onTotalAmountChange = { totalAMt = it }, // required
                    isGstChecked = isGstChecked, // optional but you're overriding it
                    onGstCheckedChange = { isGstChecked = it
                        gstApplied = isGstChecked.toString()
                    } // optional but you're overriding it
                )
                Spacer(modifier = Modifier.height(5.dp))
                ScanBottomBar(
                    onSave = run@{
                        bulkViewModel.barcodeReader.close()


                        if (selectedCustomer == null) {
                            Toast.makeText(context, "Please select a customer.", Toast.LENGTH_SHORT)
                                .show()
                            return@run
                        }

                        if (productList.isEmpty()) {
                            Toast.makeText(
                                context,
                                "Please add at least one product.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@run
                        }


                        //val nextOrderNo = lastOrder.LastOrderNo.toIntOrNull()?.plus(1) ?: 1
                        coroutineScope.launch {
                            val clientCode = employee?.clientCode.orEmpty()
                            if (!isEditMode) {

                                // Fetch last order number from API
                                orderViewModel.fetchLastOrderNo(ClientCodeRequest(clientCode))

                                // Parse response safely
                                var attempts = 0
                                var lastOrderNo: Int? = null
                                while (attempts < 10 && lastOrderNo == null) {
                                    delay(300)
                                    lastOrderNo =
                                        orderViewModel.lastOrderNoresponse.value.LastOrderNo.toIntOrNull()
                                    attempts++
                                }

                                val nextOrderNo = (lastOrderNo ?: 0) + 1

                                Log.d("Order", "Fetched Last Order: $lastOrderNo")
                                Log.d("Order", "Next Order Number: $nextOrderNo")
                                Log.d("Order", "totalStoneAmt: $totalStoneAmt")

                                if (nextOrderNo == 0) {
                                    Toast.makeText(
                                        context,
                                        "Failed to generate order number.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@launch
                                }

                                if (nextOrderNo != 0) {


                                    val gstPercent = 3.0
                                    //val gstApplied = "true"
                                    var taxableAmt = totalAMt ?: 0.0
                                    val isGstApplied: Boolean


                                    val gstAmt: Double
                                    val calculatedTotalAmount: Double
                                    var GST = false
                                    var AdditionTaxApplied = false

                                    if (gstApplied == "true") {
                                        gstAmt = taxableAmt * gstPercent / 100
                                        taxableAmt = totalAMt ?: 0.0
                                        calculatedTotalAmount = taxableAmt + gstAmt
                                        isGstApplied = true
                                        GST = true
                                        AdditionTaxApplied = true
                                    } else {
                                        gstAmt = 0.0
                                        calculatedTotalAmount = taxableAmt
                                        taxableAmt = calculatedTotalAmount
                                        isGstApplied = false
                                        GST = false
                                        AdditionTaxApplied = false

                                    }
                                    Log.d("@@ TOTAL AMOUNT", "TAX" + calculatedTotalAmount+" TAX is "+gstAmt+"   gstApplied"+gstApplied)
                                    val orderDate: String = productList.firstOrNull()?.orderDate
                                        ?.takeIf { !it.isNullOrBlank() }  // only use if not null/empty
                                        ?: LocalDate.now().format(formatter)
                                    val request = CustomOrderRequest(
                                        CustomOrderId = 0,
                                        CustomerId = selectedCustomer.Id.toString(),
                                        ClientCode = employee?.clientCode.orEmpty(),
                                        OrderId = 14,
                                        TotalAmount = calculatedTotalAmount.toString(),
                                        PaymentMode = "",
                                        Offer = null,
                                        Qty = quantity,
                                        GST = GST.toString(),
                                        OrderStatus = "Order Received",
                                        MRP = "",
                                        VendorId = 12,
                                        TDS = null,
                                        PurchaseStatus = null,
                                        GSTApplied = isGstApplied.toString(),
                                        Discount = "",
                                        TotalNetAmount = taxableAmt.toString(),
                                        TotalGSTAmount =  String.format("%.2f", gstAmt),
                                        TotalPurchaseAmount = calculatedTotalAmount.toString(),
                                        ReceivedAmount = "",
                                        TotalBalanceMetal = "",
                                        BalanceAmount = "",
                                        TotalFineMetal = totalFinemetal,
                                        CourierCharge = null,
                                        SaleType = null,
                                        OrderDate =  orderDate,
                                        OrderCount = "1",
                                        AdditionTaxApplied = AdditionTaxApplied.toString(),
                                        CategoryId = 2,
                                        OrderNo = nextOrderNo.toString(),
                                        DeliveryAddress = "123 Street, Mumbai",
                                        BillType = "Retail",
                                        UrdPurchaseAmt = null,
                                        BilledBy = "Employee1",
                                        SoldBy = "Employee1",
                                        CreditSilver = null,
                                        CreditGold = null,
                                        CreditAmount = null,
                                        BalanceAmt = "25000",
                                        BalanceSilver = null,
                                        BalanceGold = null,
                                        TotalSaleGold = null,
                                        TotalSaleSilver = null,
                                        TotalSaleUrdGold = null,
                                        TotalSaleUrdSilver = null,
                                        FinancialYear = "2024-25",
                                        BaseCurrency = "INR",
                                        TotalStoneWeight = totalStoneWt,
                                        TotalStoneAmount = totalStoneAmt,
                                        TotalStonePieces = "3",
                                        TotalDiamondWeight = totalDiamondWt,
                                        TotalDiamondPieces = "2",
                                        TotalDiamondAmount = totalDiamondAMt,
                                        FineSilver = "0",
                                        FineGold = "5.0",
                                        DebitSilver = null,
                                        DebitGold = null,
                                        PaidMetal = "0.0",
                                        PaidAmount = "",
                                        TotalAdvanceAmt = null,
                                        TaxableAmount = calculatedTotalAmount.toString(),
                                        TDSAmount = null,
                                        CreatedOn = "2025-07-08",
                                        //   LastUpdated = "2025-07-08",
                                        StatusType = true,
                                        FineMetal = totalFinemetal,
                                        BalanceMetal = "0.0",
                                        AdvanceAmt = "0",
                                        PaidAmt = "25000",
                                        TaxableAmt = taxableAmt.toString(),
                                        GstAmount =  String.format("%.2f", gstAmt),
                                        GstCheck = isGstChecked.toString(),
                                        Category = "Ring",
                                        TDSCheck = "false",
                                        Remark = "Urgent order",
                                        OrderItemId = null,
                                        StoneStatus = null,
                                        DiamondStatus = null,
                                        BulkOrderId = null,

                                        CustomOrderItem = productList.map { product ->

                                            CustomOrderItem(
                                                CustomOrderId = 0,
                                                RFIDCode =selectedItem?.RFIDCode.toString(),
                                                OrderDate = product.orderDate,
                                                 DeliverDate = product.deliverDate,
                                                SKUId = 0,
                                                SKU = product.sku,
                                                CategoryId = product.categoryId,
                                                VendorId = 0,
                                                CategoryName = product.categoryName,
                                                CustomerName = selectedCustomer.FirstName,
                                                VendorName = "",
                                                ProductId = product.productId,
                                                ProductName = product.productName,
                                                DesignId = product.designid,
                                                DesignName = product.designName,
                                                PurityId = product.purityid,
                                                PurityName = product.purity,
                                                GrossWt = product.grWt.toString(),
                                                StoneWt = product.stoneWt,
                                                DiamondWt = product.dimondWt,
                                                NetWt = product.nWt.toString(),
                                                Size = product.size,
                                                Length = product.length,
                                                TypesOdColors = product.typeOfColor,
                                                Quantity = product.qty,
                                                RatePerGram = "",
                                                MakingPerGram = "",
                                                MakingFixed = "",
                                                FixedWt = "",
                                                MakingPercentage = "",
                                                DiamondPieces = "",
                                                DiamondRate = "",
                                                DiamondAmount = product.diamondAmt,
                                                StoneAmount = product.stoneAmt.toString(),
                                                ScrewType = product.screwType,
                                                Polish = product.polishType,
                                                Rhodium = "",
                                                SampleWt = "",
                                                Image = product.image.split(",").lastOrNull()
                                                    ?.trim()
                                                    .toString(),
                                                ItemCode = product.itemCode,
                                                CustomerId = selectedCustomer.Id ?: 0,
                                                MRP = product.mrp,
                                                HSNCode = "",
                                                UnlProductId = 0,
                                                OrderBy = "",
                                                StoneLessPercent = "",
                                                ProductCode = product.productCode,
                                                TotalWt = product.totalWt,
                                                BillType = "",
                                                FinePercentage = product.finePer,
                                                ClientCode = employee?.clientCode,
                                                OrderId = "",
                                                // CreatedOn = "",
                                                // LastUpdated = "",
                                                StatusType = true,
                                                PackingWeight = product.packingWt,
                                                MetalAmount = "",
                                                OldGoldPurchase = true,
                                                Amount = product.itemAmt.toString(),
                                                totalGstAmount = "",
                                                finalPrice = product.itemAmt.toString(),
                                                MakingFixedWastage = "",
                                                Description = product.remark,
                                                CompanyId = 0,
                                                LabelledStockId = 0,
                                                TotalStoneWeight = product.stoneWt,
                                                BranchId = 0,
                                                BranchName = product.branchName,
                                                Exhibition = product.exhibition,
                                                CounterId = product.counterId.toString(),
                                                EmployeeId = 0,
                                                OrderNo = nextOrderNo.toString(),
                                                OrderStatus = "",
                                                DueDate = "",
                                                Remark = product.remark,
                                                Id = product.id,
                                                PurchaseInvoiceNo = "",
                                                Purity = product.purity,
                                                Status = "",
                                                URDNo = "",
                                                HallmarkAmt=product.hallmarkAmt,
                                                Stones = emptyList(),
                                                Diamond = emptyList()
                                            )
                                        },

                                        Payments = listOf(Payment("")),
                                        uRDPurchases = listOf(URDPurchase("")),
                                        Customer = Customer(
                                            FirstName = selectedCustomer.FirstName.orEmpty(),
                                            LastName = selectedCustomer.LastName.orEmpty(),
                                            PerAddStreet = "",
                                            CurrAddStreet = "",
                                            Mobile = selectedCustomer.Mobile.orEmpty(),
                                            Email = selectedCustomer.Email.orEmpty(),
                                            Password = "",
                                            CustomerLoginId = selectedCustomer.Email.orEmpty(),
                                            DateOfBirth = "",
                                            MiddleName = "",
                                            PerAddPincode = "",
                                            Gender = "",
                                            OnlineStatus = "",
                                            CurrAddTown = selectedCustomer.CurrAddTown.orEmpty(),
                                            CurrAddPincode = "",
                                            CurrAddState = selectedCustomer.CurrAddState.orEmpty(),
                                            PerAddTown = "",
                                            PerAddState = "",
                                            GstNo = selectedCustomer.GstNo.orEmpty(),
                                            PanNo = selectedCustomer.PanNo.orEmpty(),
                                            AadharNo = "",
                                            BalanceAmount = "0",
                                            AdvanceAmount = "0",
                                            Discount = "0",
                                            CreditPeriod = "",
                                            FineGold = "0",
                                            FineSilver = "0",
                                            ClientCode = selectedCustomer.ClientCode.orEmpty(),
                                            VendorId = 0,
                                            AddToVendor = false,
                                            CustomerSlabId = 0,
                                            CreditPeriodId = 0,
                                            RateOfInterestId = 0,
                                            Remark = "",
                                            Area = "",
                                            City = selectedCustomer.City.orEmpty(),
                                            Country = selectedCustomer.Country.orEmpty(),
                                            Id = selectedCustomer.Id ?: 0,
                                            CreatedOn = "2025-07-08",
                                            LastUpdated = "2025-07-08",
                                            StatusType = true
                                        ),
                                    )
                                    if (isOnline) {
                                        orderViewModel.addOrderCustomer(request)
                                    } else {
                                        orderViewModel.saveOrder(request)
                                    }
                                }
                            } else {

                                val gstPercent = 3.0
                                //val gstApplied = "true"
                                var taxableAmt = totalAMt ?: 0.0
                                val isGstApplied: Boolean


                                val gstAmt: Double
                                val calculatedTotalAmount: Double
                                var GST = false
                                var AdditionTaxApplied = false

                                if (editOrder?.GSTApplied == "true") {
                                    gstAmt = taxableAmt * gstPercent / 100
                                    taxableAmt = totalAMt ?: 0.0
                                    calculatedTotalAmount = taxableAmt + gstAmt
                                    isGstApplied = true
                                    GST = true
                                    AdditionTaxApplied = true
                                } else {
                                    gstAmt = 0.0
                                    calculatedTotalAmount = taxableAmt
                                    taxableAmt = calculatedTotalAmount
                                    isGstApplied = false
                                    GST = false
                                    AdditionTaxApplied = false

                                }
                                Log.d("@@", "" + calculatedTotalAmount +"editOrder?.GSTApplied"+editOrder?.GSTApplied +" gstAmt"+gstAmt)

                                val request = CustomOrderRequest(
                                    CustomOrderId = editOrder?.CustomOrderId?.toInt() ?: 0,
                                    CustomerId = editOrder?.Customer?.Id.toString(),
                                    ClientCode = employee?.clientCode.orEmpty(),
                                    OrderId = 14,
                                    TotalAmount = calculatedTotalAmount.toString(),
                                    PaymentMode = "",
                                    Offer = null,
                                    Qty = quantity,
                                    GST = GST.toString(),
                                    OrderStatus = "Order Received",
                                    MRP = editOrder?.MRP,
                                    VendorId = 12,
                                    TDS = null,
                                    PurchaseStatus = null,
                                    GSTApplied = isGstApplied.toString(),
                                    Discount = "",
                                    TotalNetAmount = taxableAmt.toString(),
                                    TotalGSTAmount = String.format("%.2f", gstAmt),
                                    TotalPurchaseAmount = calculatedTotalAmount.toString(),
                                    ReceivedAmount = "",
                                    TotalBalanceMetal = "",
                                    BalanceAmount = "",
                                    TotalFineMetal = totalFinemetal,
                                    CourierCharge = null,
                                    SaleType = null,
                                    OrderDate = "2025-07-08",
                                    OrderCount = "1",
                                    AdditionTaxApplied = AdditionTaxApplied.toString(),
                                    CategoryId = 2,
                                    OrderNo = editOrder?.OrderNo.toString(),
                                    DeliveryAddress = "123 Street, Mumbai",
                                    BillType = "Retail",
                                    UrdPurchaseAmt = null,
                                    BilledBy = "Employee1",
                                    SoldBy = "Employee1",
                                    CreditSilver = null,
                                    CreditGold = null,
                                    CreditAmount = null,
                                    BalanceAmt = "25000",
                                    BalanceSilver = null,
                                    BalanceGold = null,
                                    TotalSaleGold = null,
                                    TotalSaleSilver = null,
                                    TotalSaleUrdGold = null,
                                    TotalSaleUrdSilver = null,
                                    FinancialYear = "2024-25",
                                    BaseCurrency = "INR",
                                    TotalStoneWeight = totalStoneWt,
                                    TotalStoneAmount = totalStoneAmt,
                                    TotalStonePieces = "3",
                                    TotalDiamondWeight = totalDiamondWt,
                                    TotalDiamondPieces = "2",
                                    TotalDiamondAmount = totalDiamondAMt,
                                    FineSilver = "0",
                                    FineGold = "5.0",
                                    DebitSilver = null,
                                    DebitGold = null,
                                    PaidMetal = "0.0",
                                    PaidAmount = "",
                                    TotalAdvanceAmt = null,
                                    TaxableAmount = calculatedTotalAmount.toString(),
                                    TDSAmount = null,
                                    CreatedOn = "2025-07-08",
                                    //   LastUpdated = "2025-07-08",
                                    StatusType = true,
                                    FineMetal = totalFinemetal,
                                    BalanceMetal = "0.0",
                                    AdvanceAmt = "0",
                                    PaidAmt = "25000",
                                    TaxableAmt = taxableAmt.toString(),
                                    GstAmount =  String.format("%.2f", gstAmt),
                                    GstCheck = isGstChecked.toString(),
                                    Category = "Ring",
                                    TDSCheck = "false",
                                    Remark = "Urgent order",
                                    OrderItemId = null,
                                    StoneStatus = null,
                                    DiamondStatus = null,
                                    BulkOrderId = null,

                                    CustomOrderItem = productList.map { product ->

                                        CustomOrderItem(
                                            CustomOrderId = 0,
                                            RFIDCode =product?.rfidCode.toString(),
                                             OrderDate = product.orderDate,
                                             DeliverDate = product.deliverDate,
                                            SKUId = 0,
                                            SKU = product.sku,
                                            CategoryId = selectedItem?.CategoryId,
                                            VendorId = 0,
                                            CategoryName = product.categoryName,
                                            CustomerName = selectedCustomer.FirstName,
                                            VendorName = "",
                                            ProductId = product.productId,
                                            ProductName = product.productName,
                                            DesignId = product.designid,
                                            DesignName = product.designName,
                                            PurityId = product.purityid,
                                            PurityName = product.purity,
                                            GrossWt = product.grWt.toString(),
                                            StoneWt = product.stoneWt,
                                            DiamondWt = product.dimondWt,
                                            NetWt = product.nWt.toString(),
                                            Size = product.size,
                                            Length = product.length,
                                            TypesOdColors = product.typeOfColor,
                                            Quantity = product.qty,
                                            RatePerGram =product.todaysRate,
                                            MakingPerGram = product.makingPerGram,
                                            MakingFixed = product.makingFixedAmt,
                                            FixedWt = "",
                                            MakingPercentage = product.makingPercentage,
                                            DiamondPieces = "",
                                            DiamondRate = "",
                                            DiamondAmount = product.diamondAmt,
                                            StoneAmount = product.stoneAmt.toString(),
                                            ScrewType = product.screwType,
                                            Polish = product.polishType,
                                            Rhodium = "",
                                            SampleWt = "",
                                            Image = product.image.split(",").lastOrNull()?.trim()
                                                .toString(),
                                            ItemCode = product.itemCode,
                                            CustomerId = selectedCustomer.Id ?: 0,
                                            MRP = product.mrp,
                                            HSNCode = "",
                                            UnlProductId = 0,
                                            OrderBy = "",
                                            StoneLessPercent = "",
                                            ProductCode = product.productCode,
                                            TotalWt = product.totalWt,
                                            BillType = "",
                                            FinePercentage = product.finePer,
                                            ClientCode = employee?.clientCode,
                                            OrderId = "",
                                            // CreatedOn = "",
                                            // LastUpdated = "",
                                            StatusType = true,
                                            PackingWeight = product.packingWt,
                                            MetalAmount = "",
                                            OldGoldPurchase = true,
                                            Amount = product.itemAmt.toString(),
                                            totalGstAmount = "",
                                            finalPrice = product.itemAmt.toString(),
                                            MakingFixedWastage = "",
                                            Description = product.remark,
                                            CompanyId = 0,
                                            LabelledStockId = 0,
                                            TotalStoneWeight = product.stoneWt,
                                            BranchId = 0,
                                            BranchName = product.branchName,
                                            Exhibition = product.exhibition,
                                            CounterId = product.counterId.toString(),
                                            EmployeeId = 0,
                                            OrderNo = editOrder?.OrderNo.toString(),
                                            OrderStatus = "",
                                            DueDate = "",
                                            Remark = product.remark,
                                            Id = product.id,
                                            PurchaseInvoiceNo = "",
                                            Purity = product.purity,
                                            Status = "",
                                            URDNo = "",
                                            HallmarkAmt=product.hallmarkAmt,
                                            Stones = emptyList(),
                                            Diamond = emptyList()
                                        )
                                    },

                                    Payments = listOf(Payment("")),
                                    uRDPurchases = listOf(URDPurchase("")),
                                    Customer = Customer(
                                        FirstName = editOrder?.Customer?.FirstName.orEmpty(),
                                        LastName = editOrder?.Customer?.LastName.orEmpty(),
                                        PerAddStreet = "",
                                        CurrAddStreet = "",
                                        Mobile = editOrder?.Customer?.Mobile.orEmpty(),
                                        Email = editOrder?.Customer?.Email.orEmpty(),
                                        Password = "",
                                        CustomerLoginId = editOrder?.Customer?.Email.orEmpty(),
                                        DateOfBirth = "",
                                        MiddleName = "",
                                        PerAddPincode = "",
                                        Gender = "",
                                        OnlineStatus = "",
                                        CurrAddTown = editOrder?.Customer?.CurrAddTown.orEmpty(),
                                        CurrAddPincode = "",
                                        CurrAddState = editOrder?.Customer?.CurrAddState.orEmpty(),
                                        PerAddTown = "",
                                        PerAddState = "",
                                        GstNo = editOrder?.Customer?.GstNo.orEmpty(),
                                        PanNo = editOrder?.Customer?.PanNo.orEmpty(),
                                        AadharNo = "",
                                        BalanceAmount = "0",
                                        AdvanceAmount = "0",
                                        Discount = "0",
                                        CreditPeriod = "",
                                        FineGold = "0",
                                        FineSilver = "0",
                                        ClientCode = editOrder?.Customer?.ClientCode.orEmpty(),
                                        VendorId = 0,
                                        AddToVendor = false,
                                        CustomerSlabId = 0,
                                        CreditPeriodId = 0,
                                        RateOfInterestId = 0,
                                        Remark = "",
                                        Area = "",
                                        City = editOrder?.Customer?.City.orEmpty(),
                                        Country = editOrder?.Customer?.Country.orEmpty(),
                                        Id = editOrder?.Customer?.Id ?: 0,
                                        CreatedOn = "2025-07-08",
                                        LastUpdated = "2025-07-08",
                                        StatusType = true
                                    )
                                )
                                if (isOnline) {
                                    orderViewModel.updateOrderCustomer(request)
                                } else {
                                    orderViewModel.saveOrder(request)
                                }

                            }
                        }
                    },
                    onList = {
                        navController.navigate("order_list")
                    },
                    onScan = {
                        bulkViewModel.startSingleScan(20)

                    },
                    onGscan = {
                        //   resetScan(bulkViewModel,firstPress)
                        if (isScanning && firstPress) {
                            firstPress = false
                            bulkViewModel.stopScanning()
                            isScanning = false
                        } else {
                            bulkViewModel.startScanning(selectedPower)
                            isScanning = true
                            firstPress = true
                        }

                    },
                    onReset = {

                        // bulkViewModel.resetData()
                        bulkViewModel.stopBarcodeScanner()
                        CoroutineScope(Dispatchers.Main).launch {
                            orderViewModel.clearOrderItems()
                        }
                        customerName = ""
                        itemCode = TextFieldValue("")
                        isScanning = false
                        productList.clear()


                    },
                    isScanning = isScanning,
                    isEditMode = isEditMode
                )
            }
        }
    ) { paddingValues ->
        Column(

            modifier = Modifier
                .fillMaxSize()
                .zIndex(0f)
                /*.verticalScroll(scrollState)*/
                .padding(bottom = screenHeight * 0.01f)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            val coroutineScope = rememberCoroutineScope()
            CustomerNameInput(
                customerName = customerName,
                onCustomerNameChange = { customerName = it },
                onClear = {
                    customerName = ""
                    expandedCustomer = false
                },
                onAddCustomerClick = {
                    customerNameadd = ""
                    mobileNumber = ""
                    email = ""
                    panNumber = ""
                    gstNumber = ""
                    street = ""
                    city = ""
                    state = ""
                    country = ""
                    showAddCustomerDialog = true
                },
                filteredCustomers = filteredCustomers,
                isLoading = false,
                onCustomerSelected = {
                    customerName = "${it.FirstName.orEmpty()} ${it.LastName.orEmpty()}".trim()
                    customerId = it.Id ?: 0
                    onCustomerSelected(it)
                },
                coroutineScope = coroutineScope, // ✅ Required argument
                fetchSuggestions = {

                },

                expanded = false,

                )



            // Spacer(modifier = Modifier.height(5.dp))

            // 2. RFID / Itemcode Row
            ItemCodeInputRow(
                itemCode = itemCode,
                onItemCodeChange = { itemCode = it },
                showDropdown = showDropdownItemcode,
                setShowDropdown = { showDropdownItemcode = it },
                context = context,
                onScanClicked = { /* scanner logic */ },
                onClearClicked = { itemCode = TextFieldValue("") },
                onAddOrderClicked = {/* showOrderDialog = true*/ },
                validateBeforeShowingDialog = {
                    validateBeforeShowingDialog(selectedCustomer, productList, context)
                },
                filteredList = filteredList,
                isLoading = isLoading,
                onItemSelected = { selectedItem = it },

                saveToDb = {

                    val orderItem = mapItemCodeToOrderItem(it, dailyRates)

                  /*  if (!orderItem.itemCode.isNullOrBlank() && orderItem.itemCode != "null") {
                        Log.d("itemAmt","itemAmt"+orderItem.itemAmt)
                       // orderViewModel.insertOrderItemToRoom(orderItem)
                        productList.add(orderItem)

                    }*/
                    if (!orderItem.itemCode.isNullOrBlank() && orderItem.itemCode != "null") {
                        val alreadyExists = productList.any { it.itemCode == orderItem.itemCode }
                        if (!alreadyExists) {

                            val netWt = orderItem.nWt?.toDoubleOrNull() ?: 0.0
                            val stoneAmt = orderItem.stoneAmt?.toDoubleOrNull() ?: 0.0
                            val diamondAmt = orderItem.diamondAmt?.toDoubleOrNull() ?: 0.0
                            val makingAmt = orderItem.makingPerGram?.toDoubleOrNull() ?: 0.0
                            val rate = dailyRates.find { it.PurityName.equals(orderItem?.purity, ignoreCase = true) }?.Rate?.toDoubleOrNull() ?: 0.0

                            val calculatedAmt = (netWt * rate)
                            Log.d("@@","calculatedAmt"+calculatedAmt)
                            Log.d("@@","calculatedAmt"+calculatedAmt +",  netWt"+netWt+",  rate"+rate)

                            // ✅ Copy orderItem with new itemAmt
                            val orderItemNew = orderItem.copy(
                                itemAmt  = calculatedAmt.toString()

                            )
                            orderViewModel.insertOrderItemToRoom(orderItemNew)
                            productList.add(orderItemNew)
                        }
                    }

                },
                selectedCustomer = selectedCustomer,

                productList = productList,
                customerId = customerId,
                selectedItem = selectedItem,
                bulkViewModel = bulkViewModel,
                isFiltering=isFiltering
            )
            Spacer(modifier = Modifier.height(4.dp))
            //table row
            OrderItemTableScreen(
                productList = productList,
                selectedItem = selectedItem,
                onItemSelected = { selectedItem = it },
                showEditOrderDialog = showEditOrderDialog,
                onEditOrderClicked = { item ->
                    // Handle edit logic here
                    showEditOrderDialog = true
                    // e.g., selectedOrderItem.value = item
                },

                employee = employee,
                orderViewModel = orderViewModel,

                refreshKey = refreshKey,
                orderSelectedItem = orderSelectedItem,
                onOrderSelectedItemChange = { orderSelectedItem = it },
                onSaveEditedItem = onSaveEditedItem,
                screenHeight=screenHeight
            )

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
    if (showAddCustomerDialog) {
        Popup(
            alignment = Alignment.Center,
            properties = PopupProperties(focusable = true)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .fillMaxWidth(0.95f)
                        .heightIn(min = 300.dp, max = 600.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.DarkGray)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Customer Profile",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val scrollState = rememberScrollState()

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(scrollState)
                                .padding(16.dp)
                        ) {
                            // Dropdown control states
                            var expandedCountry by remember { mutableStateOf(false) }
                            var expandedState by remember { mutableStateOf(false) }

                            @Composable
                            fun textInput(
                                value: String,
                                onChange: (String) -> Unit,
                                label: String,
                                keyboardType: KeyboardType = KeyboardType.Text,
                                maxLength: Int = Int.MAX_VALUE
                            ) {
                                BasicTextField(
                                    value = value,
                                    onValueChange = {
                                        if (it.length <= maxLength) onChange(it)
                                    },
                                    textStyle = TextStyle(fontSize = 16.sp),
                                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Color.Gray.copy(alpha = 0.1f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(12.dp),
                                    decorationBox = { innerTextField ->
                                        Box(Modifier.fillMaxWidth()) {
                                            if (value.isEmpty()) {
                                                Text(label, color = Color.Gray, fontSize = 14.sp)
                                            }
                                            innerTextField()
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            @Composable
                            fun dropdownInput(
                                value: String,
                                onValueChange: (String) -> Unit,
                                label: String,
                                options: List<String>,
                                expanded: Boolean,
                                onExpandedChange: (Boolean) -> Unit,
                                modifier: Modifier = Modifier
                            ) {
                                Column (modifier = modifier) {
                                    BasicTextField(
                                        value = value,
                                        onValueChange = {
                                            onValueChange(it)
                                            onExpandedChange(true)
                                        },
                                        textStyle = TextStyle(fontSize = 16.sp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                Color.Gray.copy(alpha = 0.1f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(12.dp),
                                        decorationBox = { innerTextField ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Box(modifier = Modifier.weight(1f)) {
                                                    if (value.isEmpty()) {
                                                        Text(
                                                            label,
                                                            color = Color.Gray,
                                                            fontSize = 14.sp
                                                        )
                                                    }
                                                    innerTextField()
                                                }
                                                IconButton(
                                                    onClick = {
                                                        if (value.isNotEmpty()) {
                                                            onValueChange("")
                                                            onExpandedChange(false)
                                                        } else {
                                                            onExpandedChange(true)
                                                        }
                                                    },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = if (value.isNotEmpty()) Icons.Default.Close else Icons.Default.ArrowDropDown,
                                                        contentDescription = null,
                                                        tint = Color.Gray
                                                    )
                                                }
                                            }
                                        }
                                    )

                                    if (expanded) {
                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { onExpandedChange(false) }
                                        ) {
                                            options.filter {
                                                it.contains(value, ignoreCase = true)
                                            }.forEach { suggestion ->
                                                DropdownMenuItem(onClick = {
                                                    onValueChange(suggestion)
                                                    onExpandedChange(false)
                                                }) {
                                                    Text(suggestion)
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Text Fields
                            textInput(customerNameadd, { customerNameadd = it }, "Customer Name")
                            textInput(
                                mobileNumber,
                                { if (it.all(Char::isDigit)) mobileNumber = it },
                                "Mobile Number",
                                KeyboardType.Phone,
                                10
                            )
                            textInput(email, { email = it }, "Email")
                            textInput(panNumber, {
                                if (it.length <= 10) {
                                    panNumber = it.uppercase()
                                    panError = it.length != 10 && it.isNotEmpty()
                                }
                            }, "PAN Number")
                            if (panError) {
                                Text(
                                    text = "PAN must be exactly 10 characters",
                                    color = Color.Red,
                                    fontSize = 12.sp
                                )
                            }
                            textInput(gstNumber,  {
                                if (it.length <= 15) {              // ✅ restrict max 15 chars
                                    gstNumber = it.uppercase()      // ✅ always uppercase
                                    gstError = it.length != 15 && it.isNotEmpty() // show error if not exactly 15
                                }
                            }, "GST Number")
                            if (gstError) {
                                Text(
                                    text = "GST must be exactly 15 characters",
                                    color = Color.Red,
                                    fontSize = 12.sp
                                )
                            }
                            textInput(street, { street = it }, "Street")



                            // Dropdowns
                            Row(modifier = Modifier.fillMaxWidth()) {
                                dropdownInput(
                                    value = country,
                                    onValueChange = { country = it },
                                    label = "Country",
                                    options = countryOptions,
                                    expanded = expandedCountry,
                                    onExpandedChange = { expandedCountry = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 4.dp)
                                )
                                dropdownInput(
                                    value = state,
                                    onValueChange = { state = it },
                                    label = "State",
                                    options = stateOptions,
                                    expanded = expandedState,
                                    onExpandedChange = { expandedState = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            textInput(city, { city = it }, "City")
                            /*      dropdownInput(
                                      city,
                                      { city = it },
                                      "City",
                                      cityOptions,
                                      expandedCity,
                                      { expandedCity = it },
                                      modifier = Modifier.fillMaxWidth())*/

                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            GradientButtonIcon(
                                text = "Cancel",
                                onClick = { showAddCustomerDialog = false },
                                icon = painterResource(id = R.drawable.ic_cancel),
                                iconDescription = "Cancel Icon",
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 4.dp)
                            )

                            GradientButtonIcon(
                                text = "OK",
                                onClick = {
                                    fun isValidEmail(email: String) =
                                        email.matches("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$".toRegex())

                                    fun isValidPhone(phone: String) =
                                        phone.matches("^[0-9]{10}$".toRegex())

                                    fun isValidPan(pan: String) =
                                        pan.matches("^[A-Z]{5}[0-9]{4}[A-Z]{1}$".toRegex())

                                    fun isValidGst(gst: String) =
                                        gst.matches("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[A-Z0-9]{1}[A-Z]{1}[0-9]{1}$".toRegex())

                                    when {
                                        customerNameadd.isEmpty() -> Toast.makeText(
                                            context,
                                            "Enter name",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        /*   !isValidPhone(mobileNumber) -> Toast.makeText(
                                               context,
                                               "Invalid phone",
                                               Toast.LENGTH_SHORT
                                           ).show()*/

                                        email.isNotEmpty() && !isValidEmail(email) -> Toast.makeText(
                                            context,
                                            "Invalid email",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        panNumber.isNotEmpty() && !isValidPan(panNumber) -> Toast.makeText(
                                            context,
                                            "Invalid PAN",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        gstNumber.isNotEmpty() && !isValidGst(gstNumber) -> Toast.makeText(
                                            context,
                                            "Invalid GST",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        else -> {
                                            val request = AddEmployeeRequest(
                                                customerNameadd,
                                                "",
                                                "",
                                                email,
                                                "",
                                                "",
                                                "",
                                                0,
                                                0,
                                                0,
                                                mobileNumber,
                                                "Active",
                                                "",
                                                "0",
                                                "0",
                                                street,
                                                "",
                                                "",
                                                city,
                                                state,
                                                "",
                                                "",
                                                "",
                                                "",
                                                country,
                                                "",
                                                "",
                                                "0",
                                                "0",
                                                panNumber,
                                                "0",
                                                "0",
                                                gstNumber,
                                                employee?.clientCode,
                                                0,
                                                "",
                                                false,
                                                employee?.employeeId?.toString()
                                            )
                                            orderViewModel.addEmployee(request)
                                            showAddCustomerDialog = false

                                        }
                                    }
                                },
                                icon = painterResource(id = R.drawable.check_circle),
                                iconDescription = "OK Icon",
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditOrderDialog) {
        val branchList = singleProductViewModel.branches
        Log.d(
            "@@ vasanti",
            "custId" + customerId + " , custdata " + selectedCustomer + "   .selecetditem " + selectedItem + " ,branchlist" + branchList
        )
        Log.d("@@", "@@ vasanti,branchlist" + branchList)
        OrderDetailsDialogEditAndDisplay(

            orderSelectedItem,
            branchList,
            onDismiss = { showEditOrderDialog = false },
            edit = 1,
            onSave = { updatedItem ->
                onSaveEditedItem(updatedItem)   // updates productList in parent
            }

        )
    }

}


fun CustomOrderRequest.toCustomOrderResponse(): CustomOrderResponse {
    return CustomOrderResponse(
        CustomOrderId = this.CustomOrderId,
        CustomerId = this.CustomerId.toIntOrNull() ?: 0,
        ClientCode = this.ClientCode,
        OrderId = this.OrderId,
        TotalAmount = this.TotalAmount,
        PaymentMode = this.PaymentMode,
        Offer = this.Offer,
        Qty = this.Qty,
        GST = this.GST,
        OrderStatus = this.OrderStatus,
        MRP = this.MRP,
        VendorId = this.VendorId,
        TDS = this.TDS,
        PurchaseStatus = this.PurchaseStatus,
        GSTApplied = this.GSTApplied,
        Discount = this.Discount,
        TotalNetAmount = this.TotalNetAmount,
        TotalGSTAmount = this.TotalGSTAmount,
        TotalPurchaseAmount = this.TotalPurchaseAmount,
        ReceivedAmount = this.ReceivedAmount,
        TotalBalanceMetal = this.TotalBalanceMetal,
        BalanceAmount = this.BalanceAmount,
        TotalFineMetal = this.TotalFineMetal,
        CourierCharge = this.CourierCharge,
        SaleType = this.SaleType,
        OrderDate = this.OrderDate,
        OrderCount = this.OrderCount,
        AdditionTaxApplied = this.AdditionTaxApplied,
        CategoryId = this.CategoryId,
        OrderNo = this.OrderNo,
        DeliveryAddress = this.DeliveryAddress,
        BillType = this.BillType,
        UrdPurchaseAmt = this.UrdPurchaseAmt,
        BilledBy = this.BilledBy,
        SoldBy = this.SoldBy,
        CreditSilver = this.CreditSilver,
        CreditGold = this.CreditGold,
        CreditAmount = this.CreditAmount,
        BalanceAmt = this.BalanceAmt,
        BalanceSilver = this.BalanceSilver,
        BalanceGold = this.BalanceGold,
        TotalSaleGold = this.TotalSaleGold,
        TotalSaleSilver = this.TotalSaleSilver,
        TotalSaleUrdGold = this.TotalSaleUrdGold,
        TotalSaleUrdSilver = this.TotalSaleUrdSilver,
        FinancialYear = this.FinancialYear,
        BaseCurrency = this.BaseCurrency,
        TotalStoneWeight = this.TotalStoneWeight,
        TotalStoneAmount = this.TotalStoneAmount,
        TotalStonePieces = this.TotalStonePieces,
        TotalDiamondWeight = this.TotalDiamondWeight,
        TotalDiamondPieces = this.TotalDiamondPieces,
        TotalDiamondAmount = this.TotalDiamondAmount,
        FineSilver = this.FineSilver,
        FineGold = this.FineGold,
        DebitSilver = this.DebitSilver,
        DebitGold = this.DebitGold,
        PaidMetal = this.PaidMetal,
        PaidAmount = this.PaidAmount,
        TotalAdvanceAmt = this.TotalAdvanceAmt,
        TaxableAmount = this.TaxableAmount,
        TDSAmount = this.TDSAmount ?: "",
        CreatedOn = this.CreatedOn ?: "",
        LastUpdated = this.LastUpdated ?: "",
        StatusType = this.StatusType != false,
        FineMetal = this.FineMetal.toString(),
        BalanceMetal = this.BalanceMetal,
        AdvanceAmt = this.AdvanceAmt,
        PaidAmt = this.PaidAmt,
        TaxableAmt = this.TaxableAmt,
        GstAmount = this.GstAmount,
        GstCheck = this.GstCheck,
        Category = this.Category,
        TDSCheck = this.TDSCheck,
        Remark = this.Remark,
        OrderItemId = this.OrderItemId ?: 0,
        StoneStatus = this.StoneStatus,
        DiamondStatus = this.DiamondStatus,
        BulkOrderId = this.BulkOrderId,
        CustomOrderItem = this.CustomOrderItem,
        Payments = this.Payments,
        Customer = this.Customer,
        syncStatus = this.syncStatus,
        ProductName = ""
    )
}




@Composable
fun GstRowView(
    gstPercent: Double = 3.0,
    totalAmount: Double,
    onTotalAmountChange: (Double) -> Unit,
    isGstChecked: Boolean = false,
    onGstCheckedChange: (Boolean) -> Unit = {}
) {
    // Calculate GST-adjusted total
    val baseAmount = totalAmount
    Log.d("GSTROW","totalAmount"+totalAmount)
    val finalAmount = if (isGstChecked) {
        baseAmount + (baseAmount * gstPercent / 100)
    } else {
        baseAmount
    }

    Row(
        modifier = Modifier
            .height(45.dp)
            .background(Color(0xFFF3F2F2))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        //  .heightIn(min = 40.dp), // Ensure enough height for checkbox
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // GST Checkbox Section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Checkbox(
                checked = isGstChecked,
                onCheckedChange = onGstCheckedChange
            )

            Text(
                text = "GST ${gstPercent}%",
                fontSize = 14.sp,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.width(40.dp))

        // Total Amount Section
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total Amount",
                fontSize = 14.sp,
                color = Color.Black,
                modifier = Modifier.padding(end = 8.dp)
            )

            BasicTextField(
                value = if (finalAmount == 0.0) "" else "%.2f".format(finalAmount),
                onValueChange = { newText ->
                    val parsed = newText.toDoubleOrNull()
                    if (parsed != null) {
                        onTotalAmountChange(parsed) // send Double back
                    }
                },
                readOnly = true,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                ),
                decorationBox = { innerTextField ->
                    Box(

                        modifier = Modifier
                            .height(40.dp)
                            .background(Color.White, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (finalAmount == 0.0) {
                            Text("", color = Color.Gray, fontSize = 12.sp)
                        }
                        innerTextField()
                    }
                }
            )

        }

    }

}

fun mapItemCodeToOrderItem(
    item: ItemCodeResponse,
    dailyRates: List<DailyRateResponse> // 👈 pass daily rates here
): OrderItem {
    // 1. Convert NetWt
    val netWt = item.NetWt?.toDoubleOrNull() ?: 0.0

    // 2. Find matching purity rate from API data
    val matchedRate = dailyRates.find {
        it.PurityName.equals(item.PurityName, ignoreCase = true)
    }?.Rate?.toDoubleOrNull() ?: 0.0

    // 3. Calculate item amount
    val itemAmount = netWt * matchedRate

    return OrderItem(
        id = 0,
        branchId = item.BranchId?.toString() ?: "",
        branchName = item.BranchName ?: "",
        exhibition = "",
        remark = "",
        purity = item.PurityName ?: "",
        size = item.Size ?: "",
        length = "",
        typeOfColor = item.Colour ?: "",
        screwType = "",
        polishType = "",
        finePer = item.FinePercent ?: "",
        wastage = item.WastagePercent ?: "",
        orderDate = "",
        deliverDate = "",
        productName = item.ProductName ?: "",
        itemCode = item.ItemCode ?: "",
        rfidCode = item.RFIDCode ?: "",
        grWt = item.GrossWt,
        nWt = item.NetWt,
        stoneAmt = item.TotalStoneAmount,
        finePlusWt = item.FinePercent,
        itemAmt = "%.2f".format(itemAmount), // ✅ calculated
        packingWt = "",
        totalWt = "",
        stoneWt = item.TotalStoneWeight ?: "",
        dimondWt = item.TotalDiamondWeight ?: "",
        sku = item.SKU ?: "",
        qty = "",
        hallmarkAmt = item.HallmarkAmount ?: "",
        mrp = item.MRP ?: "",
        image = item.Images ?: "",
        netAmt = "",
        diamondAmt = item.TotalDiamondAmount ?: "",
        categoryId = item.CategoryId,
        categoryName = item.CategoryName ?: "",
        productId = item.ProductId ?: 0,
        productCode = item.ProductCode ?: "",
        skuId = item.SKUId ?: 0,
        designid = item.DesignId ?: 0,
        designName = item.DesignName ?: "",
        purityid = item.PurityId ?: 0,
        counterId = item.CounterId ?: 0,
        counterName = "",
        companyId = item.CompanyId ?: 0,
        epc = "",
        tid = item.TIDNumber ?: "",
        todaysRate = matchedRate.toString(), // ✅ keep today’s rate
        makingPercentage = item.MakingPercentage ?: "",
        makingFixedAmt = item.MakingFixedAmt ?: "",
        makingFixedWastage = item.MakingFixedWastage ?: "",
        makingPerGram = item.MakingPerGram ?: ""
    )
}

@Composable
fun OrderItemTableScreen(
    productList: List<OrderItem>,
    selectedItem: ItemCodeResponse?,
    onItemSelected: (ItemCodeResponse) -> Unit,
    showEditOrderDialog: Boolean,
    onEditOrderClicked: (OrderItem) -> Unit,
    employee: Employee?,
    orderViewModel: OrderViewModel,
    refreshKey: Int,
    orderSelectedItem: OrderItem?,
    onOrderSelectedItemChange: (OrderItem) -> Unit,
    onSaveEditedItem: (OrderItem) -> Unit,
    screenHeight: Dp
) {
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberLazyListState()
    val selectedIndex = remember { mutableStateOf(-1) }

    // Shared column definitions
    val columnDefinitions = listOf(
        "Product Name" to 110.dp,   // keep product wider
        "Item Code" to 80.dp,
        "Gr. Wt" to 70.dp,
        "N. Wt" to 70.dp,
        "F+W Wt" to 70.dp,
        "S.Amt" to 70.dp,
        "Item.Amt" to 80.dp,
        "RFID Code" to 100.dp
    )

    LaunchedEffect(Unit) {

        orderViewModel.getAllOrderItemsFromRoom()

    }

    val allItems by orderViewModel.allOrderItems.collectAsState()
    if(allItems!=null)
    {

    }

    LaunchedEffect(Unit) {
        orderViewModel.getAllOrderItemsFromRoom() // ✅ triggers fetching/updating
    }


    // Totals
    val totalGrWt = productList.sumOf { it.grWt?.toDoubleOrNull() ?: 0.0 }
    val totalNetWt = productList.sumOf { it.nWt?.toDoubleOrNull() ?: 0.0 }
    val totalStoneAmt = productList.sumOf { it.stoneAmt?.toDoubleOrNull() ?: 0.0 }
    val totalItemAmt = productList.sumOf { it.itemAmt?.toDoubleOrNull() ?: 0.0 }
    val totalQty = productList.size
    val totalFinePlusWt = productList.sumOf { it.finePlusWt?.toDoubleOrNull() ?: 0.0 }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(horizontalScrollState)
                .fillMaxWidth()
        ) {
            Column {
                // ✅ Header Row
                Row(
                    modifier = Modifier
                        .background(Color.DarkGray)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    columnDefinitions.forEach { (label, width) ->
                        Box(
                            modifier = Modifier.width(width),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = poppins
                            )
                        }
                    }
                }

                // ✅ Data Rows
                LazyColumn(
                    state = verticalScrollState,
                    modifier = Modifier.height(screenHeight * 0.48f)
                ) {
                    itemsIndexed(productList) { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(35.dp)
                                .clickable {
                                    onEditOrderClicked(item)
                                    onOrderSelectedItemChange(item)
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            columnDefinitions.forEachIndexed { colIndex, (_, width) ->
                                Box(
                                    modifier = Modifier.width(width),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when (colIndex) {
                                        // Product Name column has radio + text
                                        0 -> Text(
                                            text = item.productName,
                                            fontSize = 13.sp,
                                            color = Color.Black,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 6.dp, end = 4.dp),
                                            textAlign = TextAlign.Center
                                        )

                                        // Other columns map directly
                                        1 -> Text(item.itemCode, fontSize = 13.sp)
                                        2 -> Text(item.grWt ?: "", fontSize = 13.sp)
                                        3 -> Text(item.nWt ?: "", fontSize = 13.sp)
                                        4 -> Text(item.finePlusWt ?: "", fontSize = 13.sp)
                                        5 -> Text(item.stoneAmt ?: "", fontSize = 13.sp)
                                        6 -> Text(item.itemAmt ?: "", fontSize = 13.sp)
                                        7 -> Text(item.rfidCode ?: "", fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // ✅ Total Row
                Row(
                    modifier = Modifier
                        .background(Color.DarkGray)
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        "Total" to columnDefinitions[0].second,
                        "$totalQty" to columnDefinitions[1].second,
                        String.format("%.3f", totalGrWt) to columnDefinitions[2].second,
                        String.format("%.3f", totalNetWt) to columnDefinitions[3].second,
                        "$totalFinePlusWt" to columnDefinitions[4].second,
                        "$totalStoneAmt" to columnDefinitions[5].second,
                        "$totalItemAmt" to columnDefinitions[6].second,
                        "" to columnDefinitions[7].second
                    ).forEach { (text, width) ->
                        Box(
                            modifier = Modifier.width(width),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = text,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = poppins
                            )
                        }
                    }
                }
            }
        }
    }
}


fun validateBeforeShowingDialog(
    selectedCustomer: EmployeeList?,
    productList: List<OrderItem>,
    context: Context
): Boolean {
    return when {
        selectedCustomer == null -> {
            Toast.makeText(context, "Please select a customer.", Toast.LENGTH_SHORT).show()
            false
        }

        productList.isEmpty() -> {
            Toast.makeText(context, "Please add at least one product.", Toast.LENGTH_SHORT).show()
            false
        }

        else -> true
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemCodeInputRow(
    itemCode: TextFieldValue,
    onItemCodeChange: (TextFieldValue) -> Unit,
    showDropdown: Boolean,
    setShowDropdown: (Boolean) -> Unit,
    context: Context,
    onScanClicked: () -> Unit,
    onClearClicked: () -> Unit,
    onAddOrderClicked: () -> Unit,
    validateBeforeShowingDialog: () -> Boolean,
    filteredList: List<ItemCodeResponse>,
    isLoading: Boolean,
    onItemSelected: (ItemCodeResponse) -> Unit,
    modifier: Modifier = Modifier,
    saveToDb: (ItemCodeResponse) -> Unit,
    selectedCustomer: EmployeeList?,
    productList: List<OrderItem>,
    customerId: Int?,
    selectedItem: ItemCodeResponse?,
    bulkViewModel: BulkViewModel,
    isFiltering: Boolean
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var showOrderDialog by remember { mutableStateOf(false) }
    val singleProductViewModel: SingleProductViewModel = hiltViewModel()

    Spacer(modifier = Modifier.height(5.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .padding(horizontal = 10.dp)
    ) {
        // Item Code Input Box with ExposedDropdownMenuBox
        ExposedDropdownMenuBox(
            expanded = showDropdown && filteredList.isNotEmpty(),
            onExpandedChange = { setShowDropdown(it) },
            modifier = Modifier.weight(1.1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .height(35.dp)
                    .gradientBorderBox()
                    .padding(horizontal = 8.dp)
            ) {
                BasicTextField(
                    value = itemCode,
                    onValueChange = {
                        onItemCodeChange(it)
                        setShowDropdown(it.text.isNotEmpty())
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 14.sp, color = Color.Black),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (itemCode.text.isEmpty()) {
                                Text(
                                    "Enter RFID / Itemcode",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                IconButton(
                    onClick = {
                        if (itemCode.text.isNotEmpty()) {
                            onClearClicked()
                            setShowDropdown(false)
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        } else onScanClicked()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    if (itemCode.text.isNotEmpty()) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear",
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.svg_qr),
                            contentDescription = "Scan",
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    bulkViewModel.startBarcodeScanning(context)
                                    setShowDropdown(false)
                                },
                            tint = Color.Unspecified
                        )
                    }
                }
            }

            // Dropdown
            ExposedDropdownMenu(
                expanded = showDropdown && (isLoading || isFiltering || filteredList.isNotEmpty()),
                onDismissRequest = { setShowDropdown(false) },
                modifier = Modifier.fillMaxWidth()
            ) {
                when {
                    // 🔹 Show spinner while API loading OR local search in progress
                    isLoading || isFiltering -> {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Searching...", fontSize = 13.sp)
                                }
                            },
                            onClick = {}
                        )
                    }

                    // 🔹 Show "no results" message
                    filteredList.isEmpty() -> {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "No results found",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            },
                            onClick = {}
                        )
                    }

                    // 🔹 Show actual results
                    else -> {
                        filteredList.forEach { item ->
                            DropdownMenuItem(
                                text = {
                                    val query = itemCode.text.trim()
                                    val match = when {
                                        item.ItemCode?.contains(query, true) == true -> item.ItemCode
                                        item.RFIDCode?.contains(query, true) == true -> item.RFIDCode
                                        else -> ""
                                    }
                                    Text(match.orEmpty(), fontSize = 14.sp)
                                },
                                onClick = {
                                    val query = itemCode.text.trim()
                                    val selectedValue = when {
                                        item.ItemCode?.contains(query, true) == true -> item.ItemCode
                                        item.RFIDCode?.contains(query, true) == true -> item.RFIDCode
                                        else -> ""
                                    }

                                    onItemCodeChange(TextFieldValue(selectedValue.orEmpty()))
                                    onItemSelected(item)

                                    val alreadyExists = productList.any {
                                        it.itemCode == item.ItemCode || it.rfidCode == item.RFIDCode
                                    }
                                    if (!alreadyExists) {
                                        saveToDb(item)
                                    }

                                    setShowDropdown(false)
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                }
                            )
                        }
                    }
                }
            }


        }

        Spacer(modifier = Modifier.width(8.dp))

        // Order Details Button
        Box(
            modifier = Modifier
                .weight(0.8f)
                .height(35.dp)
                .gradientBorderBox()
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    if (validateBeforeShowingDialog()) {
                        showOrderDialog = true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Order Details", fontSize = 13.sp, color = Color.Black)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(id = R.drawable.vector_add),
                    contentDescription = "Add",
                    modifier = Modifier.size(20.dp),
                    tint = Color.Unspecified
                )
            }
        }

        if (showOrderDialog && selectedItem != null) {
            val branchList = singleProductViewModel.branches

            OrderDetailsDialog(
                customerId,
                selectedCustomer,
                selectedItem,
                branchList,
                onDismiss = { showOrderDialog = false },
                onSave = { showOrderDialog = false }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerNameInput(
    customerName: String,
    onCustomerNameChange: (String) -> Unit,
    onClear: () -> Unit,
    onAddCustomerClick: () -> Unit,
    filteredCustomers: List<EmployeeList>,
    isLoading: Boolean,
    onCustomerSelected: (EmployeeList) -> Unit,
    coroutineScope: CoroutineScope,
    fetchSuggestions: () -> Unit,
    expanded: Boolean
) {
    var expandedState by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    ExposedDropdownMenuBox(
        expanded = expandedState && filteredCustomers.isNotEmpty(),
        onExpandedChange = { expandedState = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        // 🔹 Custom BasicTextField styled like before
        Row(
            modifier = Modifier
                .menuAnchor() // anchor for dropdown
                .fillMaxWidth()
                .height(40.dp)
                .gradientBorderBox()
                .padding(1.dp)
                .background(Color.White, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = customerName,
                onValueChange = {
                    onCustomerNameChange(it)
                    expandedState = it.isNotEmpty()
                },
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, color = Color.Black),
                decorationBox = { innerTextField ->
                    if (customerName.isEmpty()) {
                        Text("Enter customer name", color = Color.Gray, fontSize = 13.sp)
                    }
                    innerTextField()
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 6.dp)
            )

            // Right-side icon: + when empty, × when text entered
            if (customerName.isEmpty()) {
                IconButton(
                    onClick = {
                        onAddCustomerClick()
                        expandedState = false
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.vector_add),
                        contentDescription = "Add",
                        modifier = Modifier.size(20.dp),
                        tint = Color.Unspecified
                    )
                }
            } else {
                IconButton(
                    onClick = {
                        onClear()
                        expandedState = false
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // 🔹 Dropdown (Material3)
        ExposedDropdownMenu(
            expanded = expandedState && filteredCustomers.isNotEmpty(),
            onDismissRequest = { expandedState = false }
        ) {
            if (isLoading) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Loading...", fontSize = 13.sp)
                        }
                    },
                    onClick = {}
                )
            } else {
                filteredCustomers.forEach { customer ->
                    DropdownMenuItem(
                        text = { Text("${customer.FirstName} ${customer.LastName}") },
                        onClick = {
                            onCustomerSelected(customer)
                            expandedState = false
                            focusManager.clearFocus()
                            keyboardController?.hide() // hide only after selection
                        }
                    )
                }
            }
        }
    }
}




fun Modifier.gradientBorderBox(
    borderRadius: Dp = 8.dp,
    borderWidth: Dp = 1.dp,
    gradientColors: List<Color> = listOf(Color(0xFF3053F0), Color(0xFFE82E5A))
): Modifier {
    return this
        .border(
            width = borderWidth,
            brush = Brush.horizontalGradient(gradientColors),
            shape = RoundedCornerShape(borderRadius)
        )
        .clip(RoundedCornerShape(borderRadius))
}

/*fun generateInvoicePdfAndOpen(context: Context, order: CustomOrderResponse, employee: Employee?) {
    val document = PdfDocument()
    val paint = Paint()

    // Page size: A4 (595x842 pixels)
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = document.startPage(pageInfo)
    val canvas = page.canvas

    var y = 40

    // ---------- Header ----------
    paint.textSize = 14f
    paint.isFakeBoldText = true
    canvas.drawText("Order Receipt", 220f, y.toFloat(), paint)

    y += 50
    paint.textSize = 10f
    paint.isFakeBoldText = false
    canvas.drawText("Date: ${order.OrderDate}", 20f, y.toFloat(), paint)
    canvas.drawText("KT: 18KT", 450f, y.toFloat(), paint)

    y += 20
    canvas.drawText(
        "Client Name: ${order.Customer?.FirstName.orEmpty()} ${order.Customer?.LastName.orEmpty()}",
        20f,
        y.toFloat(),
        paint
    )
    canvas.drawText("Screw: 88NS", 450f, y.toFloat(), paint)

    y += 20
    canvas.drawText("Separate Tags: YES", 20f, y.toFloat(), paint)
    canvas.drawText("Wastage: 0.0", 450f, y.toFloat(), paint)

    y += 30

    // ---------- Table Header ----------
    val headers =
        listOf("SNO", "TAG", "ITEM", "DESIGN", "STAMP", "GWT", "SWT", "NWT", "FINE", "STN VAL")
    val colX = listOf(10, 50, 100, 150, 245, 295, 345, 395, 445, 500)
    val colWidth = listOf(40, 50, 50, 95, 50, 50, 50, 50, 55, 85)
    val rowHeight = 22

    paint.textSize = 9f
    paint.isFakeBoldText = true

    for (i in headers.indices) {
        val left = colX[i].toFloat()
        val right = (colX[i] + colWidth[i]).toFloat()
        val bottom = (y + rowHeight).toFloat()

        // Draw header cell border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRect(left, y.toFloat(), right, bottom, paint)

        // Draw header text
        paint.style = Paint.Style.FILL
        canvas.drawText(headers[i], left + 2f, bottom - 6f, paint)
    }

    y += rowHeight
    paint.isFakeBoldText = false

    // ---------- Table Rows ----------
    for ((index, item) in order.CustomOrderItem.withIndex()) {
        if (y > 750) break // Prevent overflow

        val netWeight =
            (item.GrossWt?.toDoubleOrNull() ?: 0.0) - (item.StoneWt?.toDoubleOrNull() ?: 0.0)
        val row = listOf(
            "${index + 1}",
            item.ItemCode.orEmpty(),
            item.SKU.orEmpty(),
            item.DesignName.orEmpty(),
            item.Purity.orEmpty(),
            item.GrossWt ?: "0.000",
            item.StoneWt ?: "0.000",
            "%.3f".format(netWeight),
            item.FinePercentage ?: "0.000",
            item.StoneAmount ?: "0.000"
        )

        for (i in row.indices) {
            val left = colX[i].toFloat()
            val right = (colX[i] + colWidth[i]).toFloat()
            val bottom = (y + rowHeight).toFloat()

            // Cell border
            paint.style = Paint.Style.STROKE
            canvas.drawRect(left, y.toFloat(), right, bottom, paint)

            // Cell text
            paint.style = Paint.Style.FILL
            canvas.drawText(row[i], left + 2f, bottom - 6f, paint)
        }

        y += rowHeight
    }

    // ---------- Total Row ----------
    val totalGross = order.CustomOrderItem.sumOf { it.GrossWt?.toDoubleOrNull() ?: 0.0 }
    val totalStone = order.CustomOrderItem.sumOf { it.StoneWt?.toDoubleOrNull() ?: 0.0 }
    val totalNet = totalGross - totalStone
    val totalFine = order.CustomOrderItem.sumOf { it.FinePercentage?.toDoubleOrNull() ?: 0.0 }
    val totalStnValue = order.CustomOrderItem.sumOf { it.StoneAmount?.toDoubleOrNull() ?: 0.0 }

    val totalRow = listOf(
        "TOTAL", "", "", "", "",
        "%.3f".format(totalGross),
        "%.3f".format(totalStone),
        "%.3f".format(totalNet),
        "%.3f".format(totalFine),
        "%.3f".format(totalStnValue)
    )

    for (i in totalRow.indices) {
        val left = colX[i].toFloat()
        val right = (colX[i] + colWidth[i]).toFloat()
        val bottom = (y + rowHeight).toFloat()

        paint.style = Paint.Style.STROKE
        canvas.drawRect(left, y.toFloat(), right, bottom, paint)

        paint.style = Paint.Style.FILL
        canvas.drawText(totalRow[i], left + 2f, bottom - 6f, paint)
    }

    y += rowHeight + 50
    paint.isFakeBoldText = true

    // ---------- Footer (Client Details) ----------
    canvas.drawText(employee?.clients?.organisationName.orEmpty(), 20f, y.toFloat(), paint)
    y += 15
    paint.isFakeBoldText = false

    canvas.drawText(
        "ADDRESS - ${employee?.clients?.streetAddress.orEmpty()} , ${employee?.clients?.city.orEmpty()} - ${employee?.clients?.postalCode.orEmpty()}",
        20f,
        y.toFloat(),
        paint
    )
    y += 15
    canvas.drawText("GST - ${employee?.clients?.gstNo.orEmpty()}", 20f, y.toFloat(), paint)

    y += 15
    canvas.drawText("Note - This is not a Tax Invoice", 20f, y.toFloat(), paint)

    document.finishPage(page)

    // ---------- Save PDF File and Launch Viewer ----------
    try {
        val file = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "Invoice_${System.currentTimeMillis()}.pdf"
        )
        document.writeTo(FileOutputStream(file))
        document.close()

        val uri = FileProvider.getUriForFile(
            context,
            context.packageName + ".provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Open PDF with..."))

    } catch (e: IOException) {
        e.printStackTrace()
        Toast.makeText(context, "Error saving PDF: ${e.message}", Toast.LENGTH_LONG).show()
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No PDF viewer found", Toast.LENGTH_SHORT).show()
    }
}*/


/*fun generateInvoicePdfAndOpen(
    context: Context,
    order: CustomOrderResponse,
    employee: Employee?
) {
    CoroutineScope(Dispatchers.Main).launch {
        // Preload images first
        val imageBitmaps = mutableListOf<Bitmap?>()
        for (item in order.CustomOrderItem) {
            Log.d("@@","image@@"+item.Image)
            val bitmap = loadBitmapFromUrl("https://rrgold.loyalstring.co.in/"+item.Image ?: "")
            imageBitmaps.add(bitmap)
        }

        // Now draw PDF with loaded bitmaps
        val document = PdfDocument()
        val paint = Paint()

        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val boldTextSize = 12f
        val regularTextSize = 11f
        var y = 30

        paint.textSize = boldTextSize
        paint.isFakeBoldText = true
        canvas.drawText("Bill Report", 20f, y.toFloat(), paint)
        y += 20

        val leftX = 25f
        val rightX = 320f

        paint.textSize = regularTextSize
        paint.isFakeBoldText = false

        for ((index, item) in order.CustomOrderItem.withIndex()) {
            // Box
            val boxLeft = 20f
            val boxTop = y.toFloat()
            val boxRight = 575f
            val boxHeight = 80f
            val boxBottom = boxTop + boxHeight
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRect(boxLeft, boxTop, boxRight, boxBottom, paint)

            // Left Column
            paint.style = Paint.Style.FILL
            var leftTextY = y + 15f
            canvas.drawText(
                "Customer Name : ${order.Customer?.FirstName.orEmpty()} ${order.Customer?.LastName.orEmpty()}",
                leftX,
                leftTextY,
                paint
            )
            leftTextY += 18
            canvas.drawText("Order No       : ${item.OrderNo}", leftX, leftTextY, paint)
            leftTextY += 18
            canvas.drawText("Itemcode       : ${item.ItemCode}", leftX, leftTextY, paint)
            leftTextY += 18
            canvas.drawText("Notes          : ${"" ?: "null"}", leftX, leftTextY, paint)

            // Right Column
            var rightTextY = y + 15f
            canvas.drawText("G wt  : ${item.GrossWt}", rightX, rightTextY, paint)
            rightTextY += 18
            canvas.drawText("S wt  : ${item.StoneWt}", rightX, rightTextY, paint)
            rightTextY += 18
            canvas.drawText("N Wt  : ${item.NetWt}", rightX, rightTextY, paint)

            y += boxHeight.toInt() + 10

            // Draw Image from preloaded list
            imageBitmaps.getOrNull(index)?.let { bitmap ->
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 400, 600, true)
                val imageX = (595 - scaledBitmap.width) / 2f
                canvas.drawBitmap(scaledBitmap, imageX, y.toFloat(), null)
                y += scaledBitmap.height + 10
            }
        }

        document.finishPage(page)

        try {
            val file = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                "Bill_Report_${System.currentTimeMillis()}.pdf"
            )
            document.writeTo(FileOutputStream(file))
            document.close()

            val uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".provider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Open PDF with..."))

        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Error saving PDF: ${e.message}", Toast.LENGTH_LONG).show()
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No PDF viewer found", Toast.LENGTH_SHORT).show()
        }
    }
}*/


/*
fun editOrder(
    navController: NavHostController,
    order: CustomOrderResponse
) {
    // Pass the whole order object back to the previous screen
    navController.previousBackStackEntry
        ?.savedStateHandle
        ?.set("editOrder", order)

    // Navigate back
    navController.popBackStack()
}
*/

fun upscaleBitmap(original: Bitmap, scaleFactor: Float = 2f): Bitmap {
    val width = (original.width * scaleFactor).toInt()
    val height = (original.height * scaleFactor).toInt()
    return Bitmap.createScaledBitmap(original, width, height, true)
}

/*fun sharpenBitmap(src: Bitmap): Bitmap {
    val width = src.width
    val height = src.height
    val result = src.config?.let { Bitmap.createBitmap(width, height, it) }
    val kernel = arrayOf(
        floatArrayOf(0f, -1f, 0f),
        floatArrayOf(-1f, 5f, -1f),
        floatArrayOf(0f, -1f, 0f)
    )
    val kernelSize = 3
    val edge = kernelSize / 2
    for (y in edge until height - edge) {
        for (x in edge until width - edge) {
            var r = 0f
            var g = 0f
            var b = 0f
            for (ky in 0 until kernelSize) {
                for (kx in 0 until kernelSize) {
                    val pixel = src.getPixel(x + kx - edge, y + ky - edge)
                    val factor = kernel[ky][kx]
                 *//*   r += Color.red(pixel) * factor
                    g += Color.green(pixel) * factor
                    b += Color.blue(pixel) * factor*//*
                }
            }
            // Clamp values to 0–255
            val newR = r.coerceIn(0f, 255f).toInt()
            val newG = g.coerceIn(0f, 255f).toInt()
            val newB = b.coerceIn(0f, 255f).toInt()
            result.setPixel(x, y, Color.rgb(newR, newG, newB))
        }
    }
    return result
}*/


suspend fun loadBitmapFromUrl(urlString: String): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val url = URL(urlString)
        val inputStream = url.openStream()
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

suspend fun loadImageBytesFromUrl(urlString: String): ByteArray? = withContext(Dispatchers.IO) {
    try {
        val url = URL(urlString)
        url.openStream().use { it.readBytes() }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

suspend fun generateTablePdfWithImages(context: Context, order: CustomOrderResponse) {
    val file = File(
        context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS),
        "Order_${order.Customer.FirstName}.pdf"
    )
    val writer = PdfWriter(file)
    val pdf = PdfDocument(writer)
    val doc = Document(pdf, PageSize.A4)
    doc.setMargins(20f, 20f, 20f, 20f)
    val header = Paragraph("Customer Order")
        .setTextAlignment(TextAlignment.CENTER)
        .setBold()
        .setFontSize(18f)
    doc.add(header)
    doc.add(Paragraph("\n"))
    for ((index, item) in order.CustomOrderItem.withIndex()) {
        // Two column layout (no borders)
        val infoTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f)))
        infoTable.setWidth(UnitValue.createPercentValue(100f))
        infoTable.setBorder(null)
        val leftText = """
            Name     : ${order.Customer.FirstName} ${order.Customer.LastName}
            Order No : ${item.OrderNo ?: "-"}
            Design   : ${item.DesignName ?: "-"}
            RFID No  : ${item.RFIDCode ?: "-"}
        """.trimIndent()

        // Right column text
        val rightText = """
            Gross Wt : ${item.GrossWt ?: "-"}
            Stone Wt : ${item.StoneWt ?: "-"}
            Net Wt   : ${item.NetWt ?: "-"}
            Remark   : ${item.Remark ?: "-"}
        """.trimIndent()
        infoTable.addCell(Paragraph(leftText).setBorder(null))
        infoTable.addCell(Paragraph(rightText).setBorder(null))
        doc.add(infoTable)
        doc.add(Paragraph("\n"))
        // Big Image Below
        val imgBytes = loadImageBytesFromUrl("https://rrgold.loyalstring.co.in/" + item.Image)
        if (imgBytes != null) {
            val imgData = ImageDataFactory.create(imgBytes)
            val img = Image(imgData)
                .setAutoScale(true)
                .setWidth(UnitValue.createPercentValue(100f))
                .setHorizontalAlignment(HorizontalAlignment.CENTER)
            doc.add(img)
        } else {
            doc.add(Paragraph("Image not available").setItalic())
        }
        // Add page break after each item except the last one
        if (index != order.CustomOrderItem.lastIndex) {
            doc.add(AreaBreak(AreaBreakType.NEXT_PAGE))
        }
    }
    doc.close()
    // Open PDF
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Open PDF with..."))
}











