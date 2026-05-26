package com.loyalstring.rfid.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
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
import com.loyalstring.rfid.MainActivity
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.local.entity.OrderItem
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.addSingleItem.BranchModel
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.model.order.CustomOrderItem
import com.loyalstring.rfid.data.model.order.CustomOrderRequest
import com.loyalstring.rfid.data.model.order.CustomOrderResponse
import com.loyalstring.rfid.data.model.order.Customer
import com.loyalstring.rfid.data.model.order.ItemCodeResponse
import com.loyalstring.rfid.data.reader.ScanKeyListener
import com.loyalstring.rfid.data.remote.data.DailyRateResponse
import com.loyalstring.rfid.data.remote.resource.Resource
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.ui.utils.NetworkUtils
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.viewmodel.BulkViewModel
import com.loyalstring.rfid.viewmodel.DeliveryChallanViewModel
import com.loyalstring.rfid.viewmodel.OrderViewModel
import com.loyalstring.rfid.viewmodel.ProductListViewModel
import com.loyalstring.rfid.viewmodel.SingleProductViewModel
import com.loyalstring.rfid.viewmodel.UiState
import com.loyalstring.rfid.worker.LocaleHelper
import com.rscja.deviceapi.entity.UHFTAGInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.text.orEmpty


@SuppressLint("UnrememberedMutableState")
@Composable
fun OrderScreen(
    onBack: () -> Unit,
    navController: NavHostController,
    userPreferences: UserPreferences,
    orderViewModel: OrderViewModel,
    singleProductViewModel: SingleProductViewModel
)
/*fun OrderNewScreen(
    onBack: () -> Unit,
    navController: NavHostController,
    itemId: Int? = null
)*/ {
    val itemId: Int? = null
    val viewModel: BulkViewModel = hiltViewModel()
    val orderViewModel: OrderViewModel = hiltViewModel()
    val singleProductViewModel: SingleProductViewModel = hiltViewModel()
    val deliveryChallanViewModel: DeliveryChallanViewModel = hiltViewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
  //  var selectedPower by remember { mutableStateOf(10) }

    var selectedPower by remember { mutableIntStateOf(10) }

    val userPreferences = UserPreferences.getInstance(context)
    val savedLang = userPreferences.getAppLanguage().ifBlank { "en" }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = currentLocales[0]?.language ?: savedLang
    val localizedContext = LocaleHelper.applyLocale(context, currentLang)

    LaunchedEffect(Unit) {
        selectedPower = userPreferences.getInt(
            UserPreferences.KEY_ORDER_COUNT,
            10
        )
    }

    var isScanning by remember { mutableStateOf(false) }
//var showSuccessDialog by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    var firstPress by remember { mutableStateOf(false) }
    var shouldNavigateBack by remember { mutableStateOf(false) }
    val employee = UserPreferences.getInstance(context).getEmployee(Employee::class.java)

// Customer input fields
    var customerName by remember { mutableStateOf("") }
    var customerId by remember { mutableStateOf<Int?>(null) }

    Log.d("@@customerId","customerId"+customerId)
    var expandedCustomer by remember { mutableStateOf(false) }

    var itemCode by remember { mutableStateOf(TextFieldValue("")) }
    val isLoading by orderViewModel.isItemCodeLoading.collectAsState()
    var showDropdownItemcode by remember { mutableStateOf(false) }
    var selectedCustomer by remember { mutableStateOf<EmployeeList?>(null) }
    val productList = remember { mutableStateListOf<OrderItem>() }
    var selectedItem by remember { mutableStateOf<ItemCodeResponse?>(null) }
    val productListViewModel: ProductListViewModel = hiltViewModel()
    var createRequested by remember { mutableStateOf(false) }

    var showOrderDetailsDialog by remember { mutableStateOf(false) }
    var lastOrderDetails by remember { mutableStateOf<OrderDetails?>(null) } // ✅ optional default for scans
   // var showInvoiceDialog by remember { mutableStateOf(false) }
  //  var invoiceFields by remember { mutableStateOf<InvoiceFields?>(null) }
// Sample branch/salesman lists (can come from API)
//val branchList = listOf("Main Branch", "Sub Branch", "Online Branch")
//val salesmanList = listOf("Rohit", "Priya", "Vikas")

    val branchList = singleProductViewModel.branches
    val dailyRates by orderViewModel.getAllDailyRate.collectAsState()
    val salesmanList by orderViewModel.empListFlow.collectAsState()
    val touchList by deliveryChallanViewModel.customerTunchList.collectAsState()
    var pendingItem by remember { mutableStateOf<BulkItem?>(null) }
    var pendingBarcodeItem by remember { mutableStateOf<BulkItem?>(null) }
    var baseTotal by remember { mutableStateOf(0.0) }
    var gstAmount by remember { mutableStateOf(0.0) }
    var totalWithGst by remember { mutableStateOf(0.0) }

    var pendingMatchedItem by remember { mutableStateOf<BulkItem?>(null) }


    val activity = LocalContext.current as MainActivity
    DisposableEffect(Unit) {
        val listener = object : ScanKeyListener {
            override fun onBarcodeKeyPressed() {


                viewModel.startBarcodeScanning(context)
            }

            override fun onRfidKeyPressed() {
                if (isScanning) {
                    viewModel.stopScanning()
                    isScanning = false
                } else {
                    viewModel.startScanning(selectedPower)
                    isScanning = true
                }
            }
        }
        activity.registerScanKeyListener(listener)

        onDispose {
            activity.unregisterScanKeyListener()
        }
    }

     val IST: ZoneId = ZoneId.of("Asia/Kolkata")

     fun nowIsoDateTime(): String =
        OffsetDateTime.now(IST).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) // 2025-12-22T12:34:56+05:30

     fun nowDateOnly(): String =
        LocalDate.now(IST).format(DateTimeFormatter.ISO_LOCAL_DATE) // 2025-12-22

     fun pickOrderDate(details: OrderDetails?): String {
        val d = details?.orderDate?.trim()
        return if (d.isNullOrEmpty() || d.equals("null", true)) nowIsoDateTime() else d
    }

     fun pickDeliverDate(details: OrderDetails?): String {
        val d = details?.deliverDate?.trim()
        return if (d.isNullOrEmpty() || d.equals("null", true)) nowDateOnly() else d
    }



    val editOrder = navController
        .currentBackStackEntry
        ?.savedStateHandle
        ?.get<CustomOrderResponse>("editOrder")

    LaunchedEffect(editOrder) {
        if (editOrder != null) {
            val firstCoItem = editOrder?.CustomOrderItem?.firstOrNull()
            selectedItem = firstCoItem?.toItemCodeResponse()
            selectedCustomer = editOrder.Customer.toEmployeeList()


            // ✅ remove after consuming so it won’t run again

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

           /* // ✅ Prefill amounts
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
            }*/
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
                    wastage = coItem.MakingPercentage,
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
                    qty = qtyOrOne(coItem.Quantity),
                    hallmarkAmt = coItem.HallmarkAmount.toString(),
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
                    makingPerGram = coItem.MakingPerGram.orEmpty(),
                    CategoryWt = coItem.WeightCategories.toString(),


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


    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                orderViewModel.getAllEmpList(employee?.clientCode.toString())
                orderViewModel.getAllItemCodeList(ClientCodeRequest(employee?.clientCode.toString()))
                singleProductViewModel.getAllBranches(ClientCodeRequest(employee?.clientCode.toString()))
                singleProductViewModel.getAllPurity(ClientCodeRequest(employee?.clientCode.toString()))
                singleProductViewModel.getAllSKU(ClientCodeRequest(employee?.clientCode.toString()))
                orderViewModel.getDailyRate(ClientCodeRequest(employee?.clientCode))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    val allItems by productListViewModel.productList.collectAsState(initial = emptyList())

    val filteredApiList = remember(itemCode.text, allItems, isLoading) {
        derivedStateOf {
            val query = itemCode.text.trim()
            if (query.isEmpty() || allItems.isEmpty() || isLoading) {
                emptyList()
            } else {
                allItems.filter {
                    it.itemCode?.contains(query, ignoreCase = true) == true ||
                            it.rfid?.contains(query, ignoreCase = true) == true
                }
            }
        }
    }

    val addCustomerState by orderViewModel.addEmpReposnes.observeAsState()
    LaunchedEffect(addCustomerState) {
        when (val state = addCustomerState) {
            is Resource.Success -> {
                Toast.makeText(
                    context,
                    state.message ?: "Customer added successfully",
                    Toast.LENGTH_SHORT
                ).show()
            }
            is Resource.Error -> {
                Toast.makeText(context, state.message ?: "Error", Toast.LENGTH_SHORT).show()
            }
            is Resource.Loading -> {}
            null -> {}
        }
    }


// Collect the latest rates
  //  val dailyRates by orderViewModel.getAllDailyRate.collectAsState()

    LaunchedEffect(employee?.clientCode) {
        val code = employee?.clientCode ?: return@LaunchedEffect
        // No need for withContext here; VM already uses IO
        singleProductViewModel.getAllBranches(ClientCodeRequest(code))
        orderViewModel.getAllEmpList(ClientCodeRequest(code).toString())
    }


    val tags by viewModel.scannedTags.collectAsState()
    val scanTrigger by viewModel.scanTrigger.collectAsState()
    val isOnline = NetworkUtils.isNetworkAvailable(context)

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

    LaunchedEffect(scanTrigger) {
        scanTrigger?.let { type ->
            when (type) {
                "scan" -> if (productList.size != 1) viewModel.startScanning(30)
                "barcode" -> viewModel.startBarcodeScanning(context)
            }
            viewModel.clearScanTrigger()
        }
    }

    /*itemcode*/
  /*  LaunchedEffect(itemCode.text) {
        val query = itemCode.text.trim()
        if (query.isEmpty()) return@LaunchedEffect

        val matchedItem = allItems.firstOrNull {
            it.itemCode.equals(query, ignoreCase = true) ||
                    it.rfid.equals(query, ignoreCase = true)
        }

        if (matchedItem != null) {
            selectedItem = matchedItem.toItemCodeResponse()
            Log.d("ManualEntry", "Found: ${matchedItem.itemCode}")

            // Prevent duplicates by RFID
            if (productList.any { it.itemCode.equals(matchedItem.itemCode, ignoreCase = true) }) {
                Log.d("ManualEntry", "⚠️ Already exists: ${matchedItem.itemCode}")
                return@LaunchedEffect
            }

            // --------- NO CUSTOMER TOUCH / NO TOUCH API HERE ---------
            // Use only matchedItem values (or defaults)

            var makingPercent = matchedItem.makingPercent ?: "0.0"
            var wastagePercent = matchedItem.fixWastage ?: "0.0"
            var makingFixedWastage = matchedItem.fixWastage ?: "0.0"
            var makingFixedAmt = matchedItem.fixMaking ?: "0.0"
            var makingPerGram = matchedItem.makingPerGram ?: "0.0"

            fun safeDouble(value: String?) = value?.toDoubleOrNull() ?: 0.0

            val netWt = safeDouble(matchedItem.netWeight)

            // Find rate from dailyRates if available
            val rate = if (!dailyRates.isNullOrEmpty()) {


                val matchedPurity = matchedItem.purity?.trim().orEmpty()

                val computedRate  = dailyRates
                    .firstOrNull { r ->
                        val ratePurity = r.PurityName?.trim().orEmpty()

                        Log.d("DAILY_RATE_MATCH", "ratePurity='$ratePurity'  matchedPurity='$matchedPurity'")

                        ratePurity.equals(matchedPurity, ignoreCase = true)
                    }
                    ?.Rate
                    ?.toString()
                    ?.toDoubleOrNull()
                    ?: 0.0

                Log.d("DAILY_RATE_MATCH", "FINAL matchedPurity='$matchedPurity'  rate=$computedRate")
                computedRate
            } else {
                0.0
            }

            Log.d("@@","@@rate"+rate);

            val makingPerGramFinal = safeDouble(makingPerGram)
            val fixMaking = safeDouble(makingFixedAmt)
            val makingPercentFinal = safeDouble(makingPercent)
            val fixWastage = safeDouble(makingFixedWastage)
            val stoneAmt = safeDouble(matchedItem.stoneAmount)
            val diamondAmt = safeDouble(matchedItem.diamondAmount)
            fun asDouble(v: Any?): Double = when (v) {
                is Number -> v.toDouble()
                is String -> v.trim().toDoubleOrNull() ?: 0.0
                else -> 0.0
            }

            // Metal Amount = NetWt * Rate
            val metalAmt = netWt * asDouble(rate)

            // Making Amount = (MakingPerGram + FixMaking) + (Making% * NetWt / 100) + FixWastage
            val makingAmt =
                (makingPerGramFinal + fixMaking) + ((makingPercentFinal / 100) * netWt) + fixWastage

            // Item Amount = Stone + Diamond + Metal + Making
            val itemAmt = stoneAmt + diamondAmt + metalAmt + makingAmt

            val productDetail = OrderItem(


                branchId = (matchedItem.branchId ?: 0).toString(),
                branchName = "",

                exhibition = "",
                remark = "",

                purity = matchedItem.purity.orEmpty(),
                size = "1",
                length = "",
                typeOfColor = "",
                screwType = "",
                polishType = "",

                finePer = "0.0",
                wastage = matchedItem.makingPercent ?: "0.0",

                orderDate = pickOrderDate(lastOrderDetails),
                deliverDate = pickDeliverDate(lastOrderDetails),

                productName = matchedItem.productName.orEmpty(),
                itemCode = matchedItem.itemCode.orEmpty(),

                rfidCode = matchedItem.rfid.orEmpty(),

                grWt = matchedItem.grossWeight ?: "0.0",
                nWt = matchedItem.netWeight ?: "0.0",

                stoneAmt = matchedItem.stoneAmount ?: "0.0",
                finePlusWt = "0.0",

                itemAmt = itemAmt.toString(),   // ✅ tumhare calc se
                packingWt = "0.0",

                totalWt = matchedItem.totalWt?.toString() ?: "0.0",
                stoneWt = matchedItem.totalStoneWt?.toString() ?: "0.0",
                dimondWt = matchedItem.diamondWeight ?: "0.0",

                sku = matchedItem.sku.orEmpty(),
                qty = qtyOrOne("0"),

                hallmarkAmt = "" ?: "0.0",
                mrp = matchedItem.mrp?.toString() ?: "0.0",

                image = matchedItem.imageUrl.orEmpty(),
                netAmt = "0.0",

                diamondAmt = matchedItem.diamondAmount ?: "0.0",

                categoryId = matchedItem.categoryId,
                categoryName = matchedItem.category.orEmpty(),

                productId = matchedItem.productId ?: 0,
                productCode = matchedItem.productCode.orEmpty(),

                skuId = matchedItem.SKUId?:0,

                designid = matchedItem.designId ?: 0,
                designName = matchedItem.design.orEmpty(),

                purityid = 0,

                counterId = matchedItem.counterId ?: 0,
                counterName = "",

                companyId = 0,

                epc = matchedItem.epc.orEmpty(),     // agar epc field hai matchedItem me
                tid = matchedItem.tid.orEmpty(),

                todaysRate = rate.toString(),
                makingPercentage = makingPercent,
                makingFixedAmt = makingFixedAmt,
                makingFixedWastage = makingFixedWastage,
                makingPerGram = makingPerGram,
                CategoryWt = matchedItem.CategoryWt?.toString()?.takeIf { it != "null" } ?: "",



            )

            productList.add(productDetail)
            Log.d("ManualEntry", "✅ Added ${matchedItem.itemCode}")

            // clear input
            itemCode = TextFieldValue("")
        }
    }*/
    LaunchedEffect(pendingMatchedItem) {

        val matchedItem = pendingMatchedItem ?: return@LaunchedEffect

       /* val touchMatch = touchList.firstOrNull {
            it.CustomerId == customerId &&
                    it.StockKeepingUnit.equals(matchedItem.sku, ignoreCase = true)
        }*/

    /*    val challanItem = buildChallanDetailsdata(
            matchedItem = item,
            touchMatch = touchMatch,
            dailyRates = dailyRates,
            employee = employee,
            context = context
        )
*/
    fun safeDouble(v: String?) = v?.toDoubleOrNull() ?: 0.0
        fun fmt3(v: Double): String = String.format(Locale.getDefault(), "%.3f", v)
        // 🔹 Touch overrides (default from item)
        var makingPercent = matchedItem.makingPercent ?: "0.0"
        var makingFixedWastage = matchedItem.fixWastage ?: "0.0"
        var makingFixedAmt = matchedItem.fixMaking ?: "0.0"
        var makingPerGram = matchedItem.makingPerGram ?: "0.0"



      /*  if (touchMatch != null) {
            makingPercent = touchMatch.MakingPercentage ?: makingPercent
            makingFixedWastage = touchMatch.MakingFixedWastage ?: makingFixedWastage
            makingFixedAmt = touchMatch.MakingFixedAmt ?: makingFixedAmt
            makingPerGram = touchMatch.MakingPerGram ?: makingPerGram

        }*/

        // 🔹 Rate by purity
        val rate = dailyRates
            ?.firstOrNull { it.PurityName.equals(matchedItem.purity, ignoreCase = true) }
            ?.Rate?.toDoubleOrNull() ?: 0.0

        val netWt = safeDouble(matchedItem.netWeight)
        val stoneAmt = safeDouble(matchedItem.stoneAmount)
        val diamondAmt = safeDouble(matchedItem.diamondAmount)

        val makingAmt =
            safeDouble(makingPerGram) +
                    safeDouble(makingFixedAmt) +
                    (safeDouble(makingPercent) / 100.0 * netWt) +
                    safeDouble(makingFixedWastage)

        val metalAmt = netWt * rate
        val itemAmt = stoneAmt + diamondAmt + metalAmt + makingAmt

        val percent = (makingPercent?.toDoubleOrNull() ?: 0.0)
        val fixedWastage = (makingFixedWastage?.toDoubleOrNull() ?: 0.0)
        val net = netWt?.toDouble() ?: 0.0

        val finePlusWt = fmt3(
            (net * ((0 + fixedWastage) / 100.0)).coerceAtLeast(0.0)
        )

        Log.d("@@"," matchedItem.purity.orEmpty()"+ matchedItem.purity.orEmpty())

        val challanItem= OrderItem(
            branchId = (matchedItem.branchId ?: 0).toString(),
            branchName = "",

            exhibition = "",
            remark = "",

            purity = matchedItem.purity.orEmpty(),
            size = "1",
            length = "",
            typeOfColor = "",
            screwType = "",
            polishType = "",

            finePer = "0.0",
            wastage = matchedItem.makingPercent ?: "0.0",

            orderDate = pickOrderDate(lastOrderDetails),
            deliverDate = pickDeliverDate(lastOrderDetails),

            productName = matchedItem.productName.orEmpty(),
            itemCode = matchedItem.itemCode.orEmpty(),

            rfidCode = matchedItem.rfid.orEmpty(),

            grWt = matchedItem.grossWeight ?: "0.0",
            nWt = matchedItem.netWeight ?: "0.0",

            stoneAmt = matchedItem.stoneAmount ?: "0.0",
            finePlusWt = "0.0",

            itemAmt = itemAmt.toString(),   // ✅ tumhare calc se
            packingWt = "0.0",

            totalWt = matchedItem.totalWt?.toString() ?: "0.0",
            stoneWt = matchedItem.totalStoneWt?.toString() ?: "0.0",
            dimondWt = matchedItem.diamondWeight ?: "0.0",

            sku = matchedItem.sku.orEmpty(),
            qty = qtyOrOne("0"),

            hallmarkAmt = "" ?: "0.0",
            mrp = matchedItem.mrp?.toString() ?: "0.0",

            image = matchedItem.imageUrl.orEmpty(),
            netAmt = "0.0",

            diamondAmt = matchedItem.diamondAmount ?: "0.0",

            categoryId = matchedItem.categoryId,
            categoryName = matchedItem.category.orEmpty(),

            productId = matchedItem.productId ?: 0,
            productCode = matchedItem.productCode.orEmpty(),

            skuId = matchedItem.SKUId?:0,

            designid = matchedItem.designId ?: 0,
            designName = matchedItem.design.orEmpty(),

            purityid = 0,

            counterId = matchedItem.counterId ?: 0,
            counterName = "",

            companyId = 0,

            epc = matchedItem.epc.orEmpty(),     // agar epc field hai matchedItem me
            tid = matchedItem.tid.orEmpty(),

            todaysRate = rate.toString(),
            makingPercentage = makingPercent,
            makingFixedAmt = makingFixedAmt,
            makingFixedWastage = makingFixedWastage,
            makingPerGram = makingPerGram,
            CategoryWt = matchedItem.CategoryWt?.toString()?.takeIf { it != "null" } ?: "",

            )

        val alreadyExists = productList.any {
            matchedItem.itemCode.equals(challanItem.itemCode, ignoreCase = true) ||
                    matchedItem.rfid.equals(challanItem.rfidCode, ignoreCase = true) ||
                    matchedItem.tid.equals(challanItem.tid, ignoreCase = true)
        }

        if (!alreadyExists) {
            productList.add(challanItem)
        } else {
            Toast.makeText(context, "Item already added", Toast.LENGTH_SHORT).show()
        }

        pendingMatchedItem = null
    }



    /*scan the rfid*/
    LaunchedEffect(tags, allItems, dailyRates) {

        if (tags.isEmpty()) return@LaunchedEffect
        if (allItems.isEmpty()) {
            Log.e("RFIDScan", "❌ allItems EMPTY when tags = $tags")
            return@LaunchedEffect
        }

        Log.d("RFIDScan", "📦 ${tags.size} tags received")
        Log.d(
            "RFIDScan",
            "📚 allItems (${allItems.size}): " +
                    allItems.joinToString(" | ") {
                        "epc='${it.epc}', rfid='${it.rfid}', code='${it.itemCode}'"
                    }
        )

        fun safeDouble(v: String?) = v?.toDoubleOrNull() ?: 0.0

        tags.forEach { tagInfo: UHFTAGInfo ->

            // 1️⃣ EPC normalize
            val scannedEpc = tagInfo.getEPC()
                ?.trim()
                ?.uppercase()
                ?.replace(" ", "")
                ?: ""

            Log.d("EPC_SCAN", "Scanned EPC = '$scannedEpc'")

            // 2️⃣ allItems me match
            val matchedItem = allItems.firstOrNull { item ->
                val itemEpc = item.epc
                    ?.trim()
                    ?.uppercase()
                    ?.replace(" ", "")
                    ?: ""

                Log.d("EPC_CHECK", "itemEpc='$itemEpc' vs scanned='$scannedEpc'")
                itemEpc == scannedEpc
            }

            if (matchedItem == null) {
                Log.w("RFIDScan", "❌ No item found for EPC: $scannedEpc")
                return@forEach
            }

            // 3️⃣ Duplicate skip
            if (productList.any { it.tid == matchedItem.tid }) {
                Log.d("RFIDScan", "⚠️ Duplicate RFID skipped: ${matchedItem.rfid}")
                return@forEach
            }

            // 🔹 NO TOUCH / TUNCH LOGIC NOW
            // --- Only use values coming from matchedItem itself ---
            var makingPercent = matchedItem.makingPercent ?: "0.0"
            var wastage = matchedItem.makingPercent ?: "0.0"          // (not used in calc, but kept)
            var makingFixedWastage = matchedItem.fixWastage ?: "0.0"
            var makingFixedAmt = matchedItem.fixMaking ?: "0.0"
            var makingPerGram = matchedItem.makingPerGram ?: "0.0"

            // --- Calculation block (same as before) ---

            val netWt = safeDouble(matchedItem.netWeight)
            val rate = if (!dailyRates.isNullOrEmpty()) {
                dailyRates.firstOrNull {
                    it.PurityName.equals(matchedItem.purity, ignoreCase = true)
                }?.Rate?.toDoubleOrNull() ?: 0.0
            } else 0.0

            val makingPerGramFinal = safeDouble(makingPerGram)
            val fixMaking = safeDouble(makingFixedAmt)
            val makingPercentFinal = safeDouble(makingPercent)
            val fixWastage = safeDouble(makingFixedWastage)
            val stoneAmt = safeDouble(matchedItem.stoneAmount)
            val diamondAmt = safeDouble(matchedItem.diamondAmount)

            // 1. Metal Amt = NetWt * Rate
            val metalAmt = netWt * rate

            // 2. MakingAmt = makingPerGram + fixMaking + (making% * NetWt / 100) + fixWastage
            val makingAmt =
                makingPerGramFinal +
                        fixMaking +
                        ((makingPercentFinal / 100.0) * netWt) +
                        fixWastage

            // 3. ItemAmt = Stone + Diamond + Metal + Making
            val itemAmt = stoneAmt + diamondAmt + metalAmt + makingAmt

            // 4. FineWt = NetWt * Fine% (yahi field use kar raha hun)
            val finePercent = safeDouble(matchedItem.makingPercent)
            val fineWt = netWt * finePercent / 100.0
            selectedItem = matchedItem.toItemCodeResponse()
            // --- Build SampleOutDetails ---
            val productDetail = OrderItem(

                branchId = (matchedItem.branchId ?: 0).toString(),
                branchName = "",
                exhibition = "",
                remark = "",
                purity = matchedItem.purity.orEmpty(),
                size = "",
                length = "",
                typeOfColor = "",
                screwType = "",
                polishType = "",
                finePer = finePercent.toString(),
                wastage = matchedItem.makingPercent ?: "0.0",
                orderDate = pickOrderDate(lastOrderDetails),
                deliverDate = pickDeliverDate(lastOrderDetails),
                productName = matchedItem.productName.orEmpty(),
                itemCode = matchedItem.itemCode.orEmpty(),
                rfidCode = matchedItem.rfid.toString(),
                grWt = matchedItem.grossWeight ?: "0.0",
                nWt = matchedItem.netWeight ?: "0.0",
                stoneAmt = matchedItem.stoneAmount ?: "0.0",
                finePlusWt = fineWt.toString(),
                itemAmt = itemAmt.toString(),
                packingWt = matchedItem.netWeight ?: "0.0",
                totalWt = matchedItem.totalGwt?.toString() ?: "0.0",
                stoneWt = matchedItem.totalStoneWt?.toString() ?: "0.0",
                dimondWt = matchedItem.diamondWeight ?: "0.0",
                sku = matchedItem.sku.orEmpty(),
                qty = qtyOrOne("0"),
                hallmarkAmt = ""?: "0.0",
                mrp = matchedItem.mrp?.toString() ?: "0.0",
                image = matchedItem.imageUrl.orEmpty(),
                netAmt = itemAmt.toString(),
                diamondAmt = matchedItem.diamondAmount ?: "0.0",

                categoryId = matchedItem.categoryId,
                categoryName = matchedItem.category.orEmpty(),
                productId = matchedItem.productId ?: 0,
                productCode = matchedItem.productCode.orEmpty(),
                skuId = matchedItem.SKUId?:0,
                designid = matchedItem.designId ?: 0,
                designName = matchedItem.design.orEmpty(),
                purityid = 0,
                counterId = matchedItem.counterId ?: 0,
                counterName = "",
                companyId = 0,
                epc = matchedItem.tid.orEmpty(),
                tid = matchedItem.tid.orEmpty(),

                todaysRate = rate.toString(),
                makingPercentage = makingPercent,
                makingFixedAmt = makingFixedAmt,
                makingFixedWastage = makingFixedWastage,
                makingPerGram = makingPerGram,
                CategoryWt = matchedItem.CategoryWt?.toString()?.takeIf { it != "null" } ?: "",


            )

            if (productList.none { it.itemCode == productDetail.itemCode }) {
                productList.add(productDetail)
                Log.d("RFIDScan", "✅ Added ${productDetail.itemCode} (${productDetail.rfidCode})")
            } else {
                Log.d("RFIDScan", "⚠️ Duplicate tag skipped: ${productDetail.rfidCode}")
            }
        }
    }


    /*scan bar code */
    LaunchedEffect(allItems, dailyRates) {
        viewModel.barcodeReader.openIfNeeded()
        fun normalize(value: String?): String =
            value
                ?.trim()
                ?.uppercase()
                ?.replace(" ", "")
                ?.replace("\n", "")
                ?.replace("\r", "")
                ?: ""
        viewModel.barcodeReader.setOnBarcodeScanned { scannedRaw ->
            val scanned = normalize(scannedRaw)
            val currentItems = allItems
            itemCode = TextFieldValue(scanned)

            Log.d("RFID Scan", "Scanned raw=[$scannedRaw], normalized=[$scanned], items=${currentItems.size}")

            // 🧪 Debug: ek baar dekh le values kis field me aa rahe
            Log.d(
                "RFID Scan",
                "Candidates: " + currentItems.joinToString(" | ") { item ->
                    "itemCode='${normalize(item.itemCode)}', " +
                            "rfid='${normalize(item.rfid)}', " +
                            "productCode='${normalize(item.productCode)}', " +
                            "tid='${normalize(item.tid)}'"
                }
            )

            // ✅ Match multiple fields: RFID + ItemCode + ProductCode + TID
            val matchedItem = currentItems.firstOrNull { item ->
                // val codeRfid     = normalize(item.rfid)
                val codeItemCode = normalize(item.itemCode)
                val codeProduct  = normalize(item.productCode)
                val codeTid      = normalize(item.tid)

                val candidates = listOf( codeItemCode, codeProduct, codeTid)

                candidates.any { code ->
                    code == scanned ||           // exact match
                            code.contains(scanned) ||    // SJ4281 inside SJ4281-1
                            scanned.contains(code)       // barcode = 000SJ4281, db = SJ4281
                }
            }

            if (matchedItem == null) {
                Log.d("RFID Scan", "❌ No match found for [$scannedRaw] (normalized=[$scanned])")
                return@setOnBarcodeScanned
            }

            // 2️⃣ Duplicate skip
            if (productList.any { it.tid.equals(matchedItem.tid, ignoreCase = true) }) {
                Log.d("RFID Scan", "⚠️ Already exists: ${matchedItem.tid}")
                return@setOnBarcodeScanned
            }

            // --- Calculation helper ---
            fun safeDouble(v: String?) = v?.toDoubleOrNull() ?: 0.0

            val makingPercent = matchedItem.makingPercent ?: "0.0"
            val makingFixedWastage = matchedItem.fixWastage ?: "0.0"
            val makingFixedAmt = matchedItem.fixMaking ?: "0.0"
            val makingPerGram = matchedItem.makingPerGram ?: "0.0"

            val netWt = safeDouble(matchedItem.netWeight)

            val rate = if (!dailyRates.isNullOrEmpty()) {
                dailyRates
                    .firstOrNull { it.PurityName.equals(matchedItem.purity, ignoreCase = true) }
                    ?.Rate?.toDoubleOrNull()
                    ?: 0.0
            } else 0.0

            val makingPerGramFinal = safeDouble(makingPerGram)
            val fixMakingFinal = safeDouble(makingFixedAmt)
            val makingPercentFinal = safeDouble(makingPercent)
            val fixWastageFinal = safeDouble(makingFixedWastage)
            val stoneAmt = safeDouble(matchedItem.stoneAmount)
            val diamondAmt = safeDouble(matchedItem.diamondAmount)

            val metalAmt = netWt * rate
            val makingAmt =
                (makingPerGramFinal + fixMakingFinal) +
                        ((makingPercentFinal / 100.0) * netWt) +
                        fixWastageFinal

            val itemAmt = stoneAmt + diamondAmt + metalAmt + makingAmt

            val baseUrl = "https://rrgold.loyalstring.co.in/"
            val imageString = matchedItem.imageUrl.orEmpty()
            val lastImagePath = imageString.split(",").lastOrNull()?.trim()
            val finalImageUrl = if (!lastImagePath.isNullOrBlank()) "$baseUrl$lastImagePath" else ""

            selectedItem = matchedItem.toItemCodeResponse()
            val newProduct = OrderItem(


                branchId = (matchedItem.branchId ?: 0).toString(),
                branchName = "",

                exhibition = "",
                remark = "",

                purity = matchedItem.purity.orEmpty(),
                size = "1",
                length = "",
                typeOfColor = "",
                screwType = "",
                polishType = "",

                finePer = matchedItem.makingPerGram.toString(),                // example: "91.6"
                wastage = matchedItem.makingPercent?.toString() ?: "0.0",

                orderDate = pickOrderDate(lastOrderDetails),
                deliverDate = pickDeliverDate(lastOrderDetails),

                productName = matchedItem.productName.orEmpty(),
                itemCode = matchedItem.itemCode.orEmpty(),
                rfidCode = matchedItem.rfid.toString(),                               // ✅ unique key

                grWt = matchedItem.grossWeight ?: "0.0",
                nWt = matchedItem.netWeight ?: "0.0",
                stoneAmt = matchedItem.stoneAmount ?: "0.0",
                finePlusWt = "",                  // if you have fineWt, else "0.0"
                itemAmt = itemAmt.toString(),                    // your calculated amount

                packingWt = "0.0",
                totalWt = matchedItem.totalGwt?.toString() ?: (matchedItem.grossWeight ?: "0.0"),
                stoneWt = matchedItem.totalStoneWt?.toString() ?: "0.0",
                dimondWt = matchedItem.diamondWeight ?: "0.0",

                sku = matchedItem.sku.orEmpty(),
                qty = qtyOrOne("0"),

                hallmarkAmt ="0.0"?.toString() ?: "0.0",
                mrp = matchedItem.mrp?.toString() ?: "0.0",
                image = finalImageUrl,                           // your final image url
                netAmt = itemAmt.toString(),
                diamondAmt = matchedItem.diamondAmount ?: "0.0",

                categoryId = matchedItem.categoryId,
                categoryName = matchedItem.category.orEmpty(),
                productId = matchedItem.productId ?: 0,
                productCode = matchedItem.productCode.orEmpty(),
                skuId = matchedItem.SKUId?:0,                                       // if you don’t have SKUId yet
                designid = matchedItem.designId ?: 0,
                designName = matchedItem.design.orEmpty(),
                purityid = 0,                                    // if you don’t have purityId
                counterId = matchedItem.counterId ?: 0,
                counterName = "",
                companyId = 0,

                epc = matchedItem.tid.orEmpty(),                  // EPC usually TID in your case
                tid = matchedItem.tid.orEmpty(),

                todaysRate = rate.toString(),
                makingPercentage = makingPercentFinal.toString(),
                makingFixedAmt = fixMakingFinal.toString(),
                makingFixedWastage = fixWastageFinal.toString(),
                makingPerGram = makingPerGramFinal.toString(),
                CategoryWt = matchedItem.CategoryWt?.toString()?.takeIf { it != "null" } ?: "",


            )

            productList.add(newProduct)
            Log.d("RFID Scan", "✅ Added from barcode: ${newProduct.itemCode} (${newProduct.rfidCode})")
        }
    }

    // 🔹 When last item number updates → Add the item
    val lastOredrNo by orderViewModel.lastOrderNoresponse.collectAsState()
    //val nextNo by orderViewModel.nextOrderNo.collectAsState()

    LaunchedEffect(lastOredrNo,createRequested) {

        // Only run when a new value is emitted
        if (!createRequested) return@LaunchedEffect

        // ✅ 2) LastOrderNo must be non-empty
        val lastNoStr = lastOredrNo.LastOrderNo?.trim()
        if (lastNoStr.isNullOrBlank()) return@LaunchedEffect

        val clientCode = employee?.clientCode.orEmpty()
        val branchId = employee?.defaultBranchId ?: 1   // ya jahan se bhi branchId le raha hai
        val custId = customerId ?: 0
        createRequested=false

        // ❌ 1) Client code missing → add API mat call karo
        if (clientCode.isBlank()) {
            Log.e("SampleOut", "ClientCode missing")
            Toast.makeText(
                context,
                "Client code missing",
                Toast.LENGTH_SHORT
            ).show()
            // sampleOutViewModel.clearLastSampleOutNo()
            return@LaunchedEffect
        }

        // ❌ 2) Customer select nahi hua → add API mat call karo
        if (custId == 0) {
            Log.e("SampleOut", "Customer not selected")
            Toast.makeText(
                context,
                localizedContext.getString(R.string.please_select_customer),
                Toast.LENGTH_SHORT
            ).show()
            //sampleOutViewModel.clearLastSampleOutNo()
            return@LaunchedEffect
        }

        // ❌ 3) Koi items hi nahi → add API mat call karo
        if (productList.isEmpty()) {
            Log.e("SampleOut", "No items in productList")
            Toast.makeText(
                context,
                localizedContext.getString(R.string.please_add_item),
                Toast.LENGTH_SHORT
            ).show()
            //sampleOutViewModel.clearLastSampleOutNo()
            return@LaunchedEffect
        }

        // ✅ Sab validation pass → abhi hi number generate karo + API call
        val lastNoInt = lastOredrNo.LastOrderNo.toString().toIntOrNull() ?: 0
        val newLastOrderNo = lastNoInt+ 1
        Log.d("@@", "newOrderNo" + newLastOrderNo)
        val totalDiamondWeight = productList.sumOf { it.dimondWt.toDoubleOrNull() ?: 0.0 }.toString()
        val totalStoneWeight   = productList.sumOf { it.stoneAmt?.toDoubleOrNull() ?: 0.0 }.toString()
        val totalDiamondAmount = productList.sumOf { it.diamondAmt.toDoubleOrNull() ?: 0.0 }.toString()
        val totalStoneAmount   = productList.sumOf { it.stoneAmt?.toDoubleOrNull() ?: 0.0 }.toString()

        val totalAmount = productList.sumOf { it.itemAmt?.toDoubleOrNull() ?: 0.0 }.toString()
        val orderDate   = productList.firstOrNull()?.orderDate ?: ""   // aapke item me Date string hai

        val categoryId = productList.firstOrNull()?.categoryId ?: 0
        val categoryName = productList.firstOrNull()?.categoryName ?: ""

        val customerObj= CustomOrderRequest(
            CustomOrderId = 0,
            CustomerId = customerId.toString(),
            ClientCode = clientCode,
            OrderId = 0,

            TotalAmount = totalAmount,
            PaymentMode = "",            // sample out me blank rakh do
            Offer = null,
            Qty = productList.size.toString(),
            GST = "0",
            OrderStatus = "Order Received",
            MRP = productList.get(0).mrp,
            VendorId = null,
            TDS = null,
            PurchaseStatus = null,
            GSTApplied = "false",
            Discount = "0",
            TotalNetAmount = totalAmount,
            TotalGSTAmount = "0",
            TotalPurchaseAmount = totalAmount,
            ReceivedAmount = "0",
            TotalBalanceMetal = "0",
            BalanceAmount = totalAmount,
            TotalFineMetal = "0",
            CourierCharge = null,
            SaleType = null,
            OrderDate   = toIsoLocalDate(productList.get(0).orderDate) ?: nowDateOnly(),
           // DeliverDate = toIsoLocalDate(productList.get(0).deliverDate) ?: nowDateOnly(),
            OrderCount = productList.size.toString(),
            AdditionTaxApplied = "false",
            CategoryId = categoryId,
            OrderNo = newLastOrderNo.toString(),
            DeliveryAddress = null,
            BillType = "SampleOut",
            UrdPurchaseAmt = null,
            BilledBy = "",
            SoldBy = "",
            CreditSilver = null,
            CreditGold = null,
            CreditAmount = null,
            BalanceAmt = totalAmount,
            BalanceSilver = null,
            BalanceGold = null,
            TotalSaleGold = null,
            TotalSaleSilver = null,
            TotalSaleUrdGold = null,
            TotalSaleUrdSilver = null,
            FinancialYear = "2025-26",
            BaseCurrency = "INR",

            TotalStoneWeight = totalStoneWeight,
            TotalStoneAmount = totalStoneAmount,
            TotalStonePieces = "0",
            TotalDiamondWeight = totalDiamondWeight,
            TotalDiamondPieces = "0",
            TotalDiamondAmount = totalDiamondAmount,

            FineSilver = "0",
            FineGold = "0",
            DebitSilver = null,
            DebitGold = null,
            PaidMetal = "0",
            PaidAmount = "0",
            TotalAdvanceAmt = null,
            TaxableAmount = totalAmount,
            TDSAmount = null,
            CreatedOn = null,
            StatusType = null,

            FineMetal = "0",
            BalanceMetal = "0",
            AdvanceAmt = "0",
            PaidAmt = "0",
            TaxableAmt = totalAmount,
            GstAmount = "0",
            GstCheck = "false",
            Category = categoryName,
            TDSCheck = "false",
            Remark = "",
            OrderItemId = null,
            StoneStatus = null,
            DiamondStatus = null,
            BulkOrderId = null,

            CustomOrderItem = productList.map { item ->
                // ✅ yaha aapka CustomOrderItem mapping aayega
                CustomOrderItem(


                    CustomOrderId = 0,

                    RFIDCode = item.rfidCode ?: "",

                    OrderDate   = toIsoLocalDate(item.orderDate) ?: nowDateOnly(),
                    DeliverDate = toIsoLocalDate(item.deliverDate) ?: nowDateOnly()    ,    // your field

                    SKUId = item.skuId ?: 0,
                    SKU = item.sku ?: "",

                    CategoryId = item.categoryId ?: 0,
                    VendorId = 0,

                    CategoryName = item.categoryName ?: "",
                    CustomerName = customerName,                    // you already have
                    VendorName = null,

                    ProductId = item.productId ?: 0,
                    ProductName = item.productName ?: "",

                    DesignId = item.designid ?: 0,
                    DesignName = item.designName ?: "",

                    PurityId = item.purityid ?: 0,
                    PurityName = item.purity ?: "",

                    GrossWt = item.grWt ?: "0.0",
                    StoneWt = item.stoneWt ?: "0.0",
                    DiamondWt = item.dimondWt ?: "0.0",
                    NetWt = item.nWt ?: "0.0",

                    Size = item.size ?: "",
                    Length = item.length,

                    TypesOdColors = item.typeOfColor,

                    Quantity = qtyOrOne(item.qty),

                    RatePerGram = item.todaysRate ?: "0.0",
                    MakingPerGram = item.makingPerGram ?: "0.0",
                    MakingFixed = item.makingFixedAmt ?: "0.0",
                    FixedWt = item.makingFixedWastage ?: "0.0",
                    MakingPercentage = item.makingPercentage ?: "0.0",

                    DiamondPieces = "0",
                    DiamondRate = "0.0",
                    DiamondAmount = item.diamondAmt ?: "0.0",

                    StoneAmount = item.stoneAmt?: "0.0",


                    ScrewType = item.screwType,
                    Polish = item.polishType,
                    Rhodium = "",

                    SampleWt = "",
                    Image = item.image ?: "",

                    ItemCode = item.itemCode ?: "",
                    CustomerId = custId,                             // your customer id int
                    MRP = item.mrp ?: "0.0",
                    HSNCode = "" ?: "",

                    UnlProductId = 0,
                    OrderBy = "",

                    StoneLessPercent = "" ?: "0.0",
                    ProductCode = item.productCode ?: "",

                    TotalWt = item.totalWt ?: (item.netAmt ?: "0.0"),

                    BillType = "SampleOut",
                    FinePercentage = item.finePer ?: "0.0",

                    ClientCode = clientCode,
                    OrderId = null,

                    StatusType = true,                               // or false as per your logic

                    PackingWeight = item.packingWt ?: "0.0",
                    MetalAmount = item.makingFixedAmt ?: "0.0",

                    OldGoldPurchase = false,

                    Amount = item.itemAmt ?: item.itemAmt ?: "0.0",

                    totalGstAmount = gstAmount.toString()?: "0.0",
                    finalPrice = "" ?: item.itemAmt ?: "0.0",

                    MakingFixedWastage = item.makingFixedWastage ?: "0.0",
                    Description = ""?: "",

                    CompanyId = item.companyId ?: 0,


                    LabelledStockId =  0,

                    TotalStoneWeight = item.stoneWt ?: "0.0",

                    BranchId = item.branchId.toInt() ?: branchId,          // fallback
                    BranchName = item.branchName ?: "",

                    Exhibition = item.exhibition,
                    CounterId = (item.counterId ?: 0).toString(),  // CustomOrderItem expects String
                    EmployeeId = employee?.id ?: 0,

                    OrderNo = newLastOrderNo.toString(),
                    OrderStatus = "Order Received",
                    DueDate = null,
                    Remark = item.remark ?: null,



                    PurchaseInvoiceNo ="" ?: null,

                    Purity = item.purity ?: "",

                    Status = "" ?: null,
                    URDNo = null,
                    HallmarkAmount = item.hallmarkAmt ?: null,

                    Stones = emptyList(),
                    Diamond = emptyList(),
                    WeightCategories = item.CategoryWt?.takeIf { it.isNotBlank() && it != "null" }
                )
            },

            Payments = emptyList(),
            uRDPurchases = emptyList(),
            Customer = Customer(
                FirstName = selectedCustomer?.FirstName.orEmpty(),
                LastName = selectedCustomer?.LastName.orEmpty(),
                PerAddStreet = "",
                CurrAddStreet = "",
                Mobile = selectedCustomer?.Mobile.orEmpty(),
                Email = selectedCustomer?.Email.orEmpty(),
                Password = "",
                CustomerLoginId = selectedCustomer?.Email.orEmpty(),
                DateOfBirth = "",
                MiddleName = "",
                PerAddPincode = "",
                Gender = "",
                OnlineStatus = "",
                CurrAddTown = selectedCustomer?.CurrAddTown.orEmpty(),
                CurrAddPincode = "",
                CurrAddState = selectedCustomer?.CurrAddState.orEmpty(),
                PerAddTown = "",
                PerAddState = "",
                GstNo = selectedCustomer?.GstNo.orEmpty(),
                PanNo = selectedCustomer?.PanNo.orEmpty(),
                AadharNo = "",
                BalanceAmount = "0",
                AdvanceAmount = "0",
                Discount = "0",
                CreditPeriod = "",
                FineGold = "0",
                FineSilver = "0",
                ClientCode = selectedCustomer?.ClientCode.orEmpty(),
                VendorId = 0,
                AddToVendor = false,
                CustomerSlabId = 0,
                CreditPeriodId = 0,
                RateOfInterestId = 0,
                Remark = "",
                Area = "",
                City = selectedCustomer?.City.orEmpty(),
                Country = selectedCustomer?.Country.orEmpty(),
                Id = selectedCustomer?.Id ?: 0,
                CreatedOn = "2025-07-08",
                LastUpdated = "2025-07-08",
                StatusType = true
            ),

            syncStatus = false,
            LastUpdated = null
        )
        if (isOnline) {
            orderViewModel.addOrderCustomer(customerObj)
        } else {
            Log.d( "@@"," selectedCustomer?.Id.toString()"+selectedCustomer?.Id)
           // orderViewModel.saveOrder(customerObj)
          //  orderViewModel.saveOrderOffline(customerObj,context)
            val localOrderNo = "OFF-${System.currentTimeMillis()}"


            val customerList: List<EmployeeList> =
                (customerSuggestions as? UiState.Success<List<EmployeeList>>)?.data.orEmpty()

            val selectedCustomerObj: EmployeeList? =
                customerList?.firstOrNull { (it.Id ?: 0) == (customerId ?: 0) }

            Log.d( "@@##"," selectedCustomer?.Id.toString()"+selectedCustomerObj?.Id)
            orderViewModel.saveOrderOffline(  buildOrderRequest(
                orderNo = localOrderNo,
                employee!!,productList, selectedCustomerObj,gstAmount,lastOrderDetails), context)
        }

    }

    val orderSuccess by orderViewModel.orderResponse.collectAsState()

    LaunchedEffect(orderSuccess) {
        orderSuccess?.let {
            if (!isEditMode) {
                orderViewModel.setOrderResponse(it)
                Toast.makeText(context,localizedContext.getString(R.string.order_success), Toast.LENGTH_SHORT).show()
                generateTablePdfWithImages(context, it,localizedContext)
                //showInvoice = true
                orderViewModel.clearOrderItems()
                customerName = customerName
                itemCode = TextFieldValue("")  // reset text field
                productList.clear()
            }
            orderViewModel.clearOrderRequest()
            orderViewModel.clearOrderResponse()
        }
    }



    LaunchedEffect(shouldNavigateBack) {
        if (shouldNavigateBack) {
            isEditMode = false
            productList.clear()
            deliveryChallanViewModel.setSelectedChallan(null)
            onBack()
        }
    }


    Scaffold(
        topBar = {
            GradientTopBar(
                title = localizedContext.getString(R.string.order_screen),
                navigationIcon = {
                    IconButton(
                        onClick = { shouldNavigateBack = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {

                },
                showCounter = true,
                selectedCount = selectedPower,
                onCountSelected = {
                    selectedPower = it
                },
                titleTextSize = 20.sp
            )
        },
        bottomBar = {


            ScanBottomBar(
                onSave = {
                   if (isEditMode) {

                       val totalDiamondWeight = productList.sumOf { it.dimondWt.toDoubleOrNull() ?: 0.0 }
                       val totalStoneWeight   = productList.sumOf { it.stoneAmt?.toDoubleOrNull() ?: 0.0 }
                       val totalDiamondAmount = productList.sumOf { it.diamondAmt.toDoubleOrNull() ?: 0.0 }
                       val totalStoneAmount   = productList.sumOf { it.stoneAmt?.toDoubleOrNull() ?: 0.0 }

                       val totalAmount = productList.sumOf { it.itemAmt?.toDoubleOrNull() ?: 0.0 }   // ✅ Double
                       val gstPercent = 3.0

                       val isGstApplied = editOrder?.GSTApplied == "true"

                       val taxableAmt: Double
                       val gstAmt: Double
                       val calculatedTotalAmount: Double
                       val GST: Boolean
                       val AdditionTaxApplied: Boolean

                       if (isGstApplied) {
                           taxableAmt = totalAmount
                           gstAmt = taxableAmt * gstPercent / 100.0
                           calculatedTotalAmount = taxableAmt + gstAmt
                           GST = true
                           AdditionTaxApplied = true
                       } else {
                           taxableAmt = totalAmount
                           gstAmt = 0.0
                           calculatedTotalAmount = taxableAmt
                           GST = false
                           AdditionTaxApplied = false
                       }

// ✅ If you need String for request:
                       val totalAmountStr = calculatedTotalAmount.toString()
                       val taxableAmtStr = taxableAmt.toString()
                       val gstAmtStr = gstAmt.toString()

                       val totalDiamondWeightStr = totalDiamondWeight.toString()
                       val totalStoneWeightStr = totalStoneWeight.toString()
                       val totalDiamondAmountStr = totalDiamondAmount.toString()
                       val totalStoneAmountStr = totalStoneAmount.toString()

                       Log.d("@@", "" + calculatedTotalAmount +"editOrder?.GSTApplied"+editOrder?.GSTApplied +" gstAmt"+gstAmt)

                       val request = CustomOrderRequest(
                           CustomOrderId = editOrder?.CustomOrderId?.toInt() ?: 0,
                           CustomerId = editOrder?.Customer?.Id.toString(),
                           ClientCode = employee?.clientCode.orEmpty(),
                           OrderId = 14,
                           TotalAmount = calculatedTotalAmount.toString(),
                           PaymentMode = "",
                           Offer = null,
                           Qty = "",
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
                           TotalFineMetal = "",
                           CourierCharge = null,
                           SaleType = null,
                           OrderDate = pickOrderDate(lastOrderDetails),
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
                           TotalStoneWeight = totalStoneWeightStr,
                           TotalStoneAmount = totalStoneAmountStr,
                           TotalStonePieces = "3",
                           TotalDiamondWeight = totalDiamondWeightStr,
                           TotalDiamondPieces = "2",
                           TotalDiamondAmount = totalDiamondAmountStr,
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
                           FineMetal = "",
                           BalanceMetal = "0.0",
                           AdvanceAmt = "0",
                           PaidAmt = "25000",
                           TaxableAmt = taxableAmt.toString(),
                           GstAmount =  String.format("%.2f", gstAmt),
                           GstCheck = isGstApplied.toString(),
                           Category = "Ring",
                           TDSCheck = "false",
                           Remark = "Urgent order",
                           OrderItemId = null,
                           StoneStatus = null,
                           DiamondStatus = null,
                           BulkOrderId = null,

                           CustomOrderItem = productList.map { product ->

                               CustomOrderItem(
                                   CustomOrderId = editOrder?.CustomOrderId?.toInt() ?: 0,
                                   RFIDCode =product?.rfidCode.toString(),
                                   OrderDate = isoDateTimeOrNull(product.orderDate) ?: nowIsoDateTime(),
                                   DeliverDate = isoDateTimeOrNull(product.deliverDate) ?: nowIsoDateTime(),
                                   SKUId = editOrder?.SKUId ?: 0,
                                   SKU = product.sku,
                                   CategoryId = selectedItem?.CategoryId,
                                   VendorId = 0,
                                   CategoryName = product.categoryName,
                                   CustomerName = selectedCustomer?.FirstName,
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
                                   Quantity = qtyOrOne(product.qty),
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
                                   CustomerId = selectedCustomer?.Id ?: 0,
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
                                   OrderStatus = "Order Received",
                                   DueDate = "",
                                   Remark = product.remark,

                                   PurchaseInvoiceNo = "",
                                   Purity = product.purity,
                                   Status = "",
                                   URDNo = "",
                                   HallmarkAmount =product.hallmarkAmt,
                                   WeightCategories = product.CategoryWt,
                                   Stones = emptyList(),
                                   Diamond = emptyList()
                               )
                           },

                           Payments = emptyList(),
                           uRDPurchases = emptyList(),
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
                           scope.launch {
                              // if (editOrder != null) {
                                   // this is server order editing offline
                                   orderViewModel.updateSyncedOrderOffline(
                                       serverOrderId = editOrder?.CustomOrderId!!,
                                       clientCode = employee?.clientCode.toString(),
                                       updatedReq = request
                                   )
                                   Toast.makeText(context,localizedContext.getString(R.string.order_update_success), Toast.LENGTH_SHORT).show()
                              /* } else {
                                   // offline-created order editing
                                   orderViewModel.updateOfflineCreatedOrder(
                                       localId = editOrder?.CustomOrderId!!.toString(),
                                       updatedReq = request
                                   )
                                   Toast.makeText(context, "Order Updated Successfully!", Toast.LENGTH_SHORT).show()
                               }*/
                           }
                       }
                    } else {
                       createRequested = true
                        val clientCode = employee?.clientCode ?: return@ScanBottomBar
                        val branchId = employee.branchNo ?: 1
                       val online = NetworkUtils.isNetworkAvailable(context)

                        // 🔹 Step 1: Fetch last item no
                       if (!online) {

                           // ✅ OFFLINE: local order no
                           val localOrderNo = "OFF-${System.currentTimeMillis()}"

                           val customerList: List<EmployeeList> =
                               (customerSuggestions as? UiState.Success<List<EmployeeList>>)?.data.orEmpty()

                           if ((customerId ?: 0) == 0) {
                               Toast.makeText(context, localizedContext.getString(R.string.please_select_customer), Toast.LENGTH_SHORT).show()
                               return@ScanBottomBar
                           }

                           val selectedCustomerObj: EmployeeList? =
                               customerList.firstOrNull { (it.Id ?: 0) == (customerId ?: 0) }

                           if (selectedCustomerObj == null) {
                               Toast.makeText(context, localizedContext.getString(R.string.customer_not_found), Toast.LENGTH_SHORT).show()
                               return@ScanBottomBar
                           }

                           if (productList.isEmpty()) {
                               Toast.makeText(context,localizedContext.getString(R.string.please_add_item), Toast.LENGTH_SHORT).show()
                               return@ScanBottomBar
                           }

// ✅ 1) Build request ONCE
                           val request = buildOrderRequest(
                               orderNo = localOrderNo,
                               employee = employee,
                               productList = productList,
                               selectedCustomer = selectedCustomerObj,
                               gstAmount = gstAmount,
                               lastOrderDetails = lastOrderDetails
                           )

// ✅ 2) Save offline
                           orderViewModel.saveOrderOffline(request, context)

// ✅ 3) Show toast
                           Toast.makeText(context,localizedContext.getString(R.string.order_success), Toast.LENGTH_SHORT).show()

// ✅ 4) Generate + open PDF (offline request-based)

                           scope.launch {
                               generateTablePdfWithImages1(context, request,localizedContext)
                           }

// ✅ 5) Reset UI (IMPORTANT: return should be at end)
                           customerName = ""
                           itemCode = TextFieldValue("")
                           productList.clear()
                         //  orderViewModel.clearOrderItems() // optional, if you want room clear also

                           return@ScanBottomBar


                       }else
                       {
                        orderViewModel.fetchLastOrderNo(ClientCodeRequest(clientCode))
                           }
                    }
                },
                onList = {  navController.navigate("order_list") },
                onScan = {
                    viewModel.startSingleScan(20)
                },
                onGscan = {
                    if (isScanning) {
                        viewModel.stopScanning()
                        isScanning = false
                    } else {
                        viewModel.startScanning(selectedPower)
                        isScanning = true
                    }

                    // viewModel.toggleScanning(selectedPower)

                },
                onReset = {
                    firstPress = false

                    resetAllFields(
                        onResetCustomerName = { customerName = it },
                        onResetCustomerId = { customerId = it },
                        onResetSelectedCustomer = { selectedCustomer = it },
                        onResetExpandedCustomer = { expandedCustomer = it },
                        onResetItemCode = { itemCode = it },
                        onResetSelectedItem = { selectedItem = it },
                        onResetDropdownItemcode = { showDropdownItemcode = it },
                        onResetProductList = { productList.clear() },
                        onResetScanning = { isScanning = it },
                        viewModel = viewModel,
                        deliveryChallanViewModel = deliveryChallanViewModel

                    ) // 🧹 Clear everything in one call
                    viewModel.resetProductScanResults()
                    viewModel.stopBarcodeScanner()
                },
                isScanning = isScanning,
                isEditMode = isEditMode,
                isScreen=false,
                isBulkScanning = false

            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            val coroutineScope = rememberCoroutineScope()

            CustomerNameInputData(
                customerName = customerName,
                onCustomerNameChange = { customerName = it },
                onClear = { customerName = "" },
                onAddCustomerClick = { /* open popup handled internally */ },
                filteredCustomers = filteredCustomers,
                isLoading = false,
                onCustomerSelected = {
                    customerName = "${it.FirstName.orEmpty()} ${it.LastName.orEmpty()}".trim()
                    customerId = it.Id ?: 0
                    selectedCustomer=it
                },
                coroutineScope = coroutineScope,
                fetchSuggestions = { orderViewModel.getAllEmpList(clientCode = employee?.clientCode.toString()) },
                expanded = false,
                onSaveCustomer = { request -> orderViewModel.addEmployee(request) },
                employeeClientCode = employee?.clientCode,
                employeeId = employee?.employeeId?.toString(),
                isEditMode = isEditMode,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 🔹 Left side → Enter RFID / Itemcode box
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .height(35.dp) // ✅ Adjusted height to align with button
                ) {
                    DeliverychallanItemCode(
                        itemCode = itemCode,
                        onItemCodeChange = { itemCode = it },
                        showDropdown = showDropdownItemcode,
                        setShowDropdown = { showDropdownItemcode = it },
                        context = context,
                        onScanClicked = {   // Start RFID scan when QR icon clicked
                            viewModel.startBarcodeScanning(context)
                        },
                        onClearClicked = { itemCode = TextFieldValue("") },
                        filteredList = allItems,
                        isLoading = isLoading,
                        // onItemSelected = { selectedItem = it }
                        onItemSelected = { item ->

                            val code = item.itemCode ?: item.rfid ?: ""

                            itemCode = TextFieldValue(code)

                            addItemToList(
                                code,
                                allItems,
                                productList
                            ) { matched ->
                                pendingMatchedItem = matched
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 🔹 Right side → Invoice Fields button
                Box(
                    modifier = Modifier
                        .weight(0.8f)
                        .height(35.dp) // ✅ same height as RFID box
                        .gradientBorderBox()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                           /* if (productList.size>0) {
                                Toast.makeText(
                                    context,
                                    "Please select Item first",
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {*/
                                showOrderDetailsDialog = true
                          //  }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = localizedContext.getString(R.string.order_details),
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.filter_gary),
                            contentDescription = "Add",
                            modifier = Modifier.size(20.dp),
                            tint = Color.Gray,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
           OrderListTable(

                onTotalsChange = { base, gst, final ->
                    baseTotal = base
                    gstAmount = gst
                    totalWithGst = final
                },
                onItemUpdated = { index, updated ->
                    // ✅ sirf ek item update
                    productList[index] = updated
                },
               productList = productList
            )

            /*      DeliveryChallanItemListTable(
                      productList = productList,
                      onTotalsChange = { base, gst, final ->
                          baseTotal = base      // rows ka sum (without GST)
                          gstAmount = gst       // 3% GST
                          totalWithGst = final  // base + GST
                      }
                  )*/
            //Spacer(modifier = Modifier.height(6.dp))
            /*
                        DeliveryChallanSummaryRow(
                            gstPercent = 3.0,
                            totalAmount = 50000.0,
                                    onGstCheckedChange = { isChecked ->
                                println("GST Checkbox changed: $isChecked")
                            }
                        )*/

        }
    }

    if (showOrderDetailsDialog && productList.size!=0) {
        OrderDetailsDialog(
            selectedCustomerId = customerId,
            selectedCustomer = selectedCustomer,
            selectedItem = productList.get(0),
            branchList = branchList,
            onDismiss = { showOrderDetailsDialog = false },
            onSave = { details ->

                /*    lastOrderDetails = details // ✅ future scans/manual default

                    val updated = buildOrderItemFromSelectedItem(
                        selectedItem = selectedItem!!,
                        details = details,
                        branchList = branchList,
                        dailyRates=dailyRates

                    )

                    // ✅ Upsert in productList (same RFID/itemCode match)
                    val idx = productList.indexOfFirst {
                        (!it.rfidCode.isNullOrBlank() && it.rfidCode.equals(updated.rfidCode, true)) ||
                                (!it.itemCode.isNullOrBlank() && it.itemCode.equals(updated.itemCode, true))
                    }
                    if (idx >= 0) productList[idx] = updated else productList.add(updated)

                    // ✅ Room/VM update so API payload also gets updated values later
                   // orderViewModel.insertOrderItemToRoomORUpdate(updated)*/




                    // 1) Save as defaults for future scans
                    lastOrderDetails = details

                    // 2) Normalize dates (VERY IMPORTANT for .NET DateTime)
                    val normalizedOrderDate = toIsoOffsetDateTime(details.orderDate) ?: nowIsoDateTime()
                    val normalizedDeliverDate = toIsoLocalDate(details.deliverDate) ?: nowDateOnly()

                    // 3) Resolve branch id from branch name
                    val branchObj = branchList.firstOrNull { it.BranchName == details.branch }
                    val branchIdStr = (branchObj?.Id ?: 0).toString()

                    fun safeDouble(v: String?) = v?.trim()?.toDoubleOrNull() ?: 0.0

                    // 4) Apply to ALL existing items (and recalc itemAmt if purity/rate changed)
                    for (i in productList.indices) {
                        val old = productList[i]

                        // rate based on details.purity (fallback to old.todaysRate)
                        val rate = dailyRates.firstOrNull {
                            it.PurityName.equals(details.purity, ignoreCase = true)
                        }?.Rate?.toString()?.toDoubleOrNull()
                            ?: safeDouble(old.todaysRate)

                        val netWt = safeDouble(old.nWt)
                        val stoneAmt = safeDouble(old.stoneAmt)
                        val diamondAmt = safeDouble(old.diamondAmt)

                        val makingPerGram = safeDouble(old.makingPerGram)
                        val makingFixed = safeDouble(old.makingFixedAmt)
                        val makingPercent = safeDouble(old.makingPercentage)
                        val fixWastage = safeDouble(old.makingFixedWastage)

                        val metalAmt = netWt * rate
                        val makingAmt = makingPerGram + makingFixed + ((makingPercent / 100.0) * netWt) + fixWastage
                        val newItemAmt = stoneAmt + diamondAmt + metalAmt + makingAmt
                        Log.d("",""+details.deliverDate+" "+details.orderDate+""+details.polishType)
                        val inputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                        val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

                        val order: Date? = outputFormat.parse(details.orderDate)
                        val delivery: Date? = inputFormat.parse(details.deliverDate)
                        Log.d("@@","data"+order+" "+delivery+""+details.polishType)


                        productList[i] = old.copy(

                                    // details apply
                            branchId = if (branchIdStr != "0") branchIdStr else old.branchId,
                            branchName = details.branch,
                            exhibition = details.exhibition,
                            remark = details.remark,

                            purity = details.purity,
                            size = details.size,
                            length = details.length,
                            typeOfColor = details.typeOfColors,
                            screwType = details.screwType,
                            polishType = details.polishType,
                            finePer = details.finePercentage,
                            wastage = details.wastage,
                            makingPercentage = details.wastage,

                            // normalized dates apply to all
                            orderDate = (order.toString()),
                            deliverDate =(delivery.toString()),

                            // update rate + amounts
                            todaysRate = rate.toString(),
                            itemAmt = String.format(Locale.US, "%.2f", newItemAmt),
                            netAmt = String.format(Locale.US, "%.2f", newItemAmt)
                        )
                    }

                    // 5) Optional: if you store list in Room, upsert all updated items too
                    // productList.forEach { orderViewModel.insertOrderItemToRoomORUpdate(it) }

                    showOrderDetailsDialog = false
                }






        )
    }


}

fun addItemToList(
    code: String,
    allItems: List<BulkItem>,
    productList: SnapshotStateList<OrderItem>,
    onMatched: (BulkItem) -> Unit
) {

    val query = code.trim()

    val matchedItem = allItems.firstOrNull { item ->
        val itemCode = item.itemCode?.trim()
        val rfid = item.rfid?.trim()
        Log.d("@@", "❌ purity${item.purity}")
        itemCode.equals(query, ignoreCase = true) ||
                rfid.equals(query, ignoreCase = true)
    }

    if (matchedItem == null) {
        Log.d("DropdownSelect", "❌ No match in allItems for $query")
        return
    }

    if (productList.any { it.tid == matchedItem.tid }) {
        Log.d("DropdownSelect", "⚠️ Duplicate item skipped ${matchedItem.itemCode}")
        return
    }

    Log.d("DropdownSelect", "✅ Matched item ${matchedItem.itemCode}")

    onMatched(matchedItem)
}

fun CustomOrderItem.toItemCodeResponse(): ItemCodeResponse {
    return ItemCodeResponse(
        Id = this.CustomOrderId,                      // agar hai
        SKUId = this.SKUId,                // agar hai

        ItemCode = this.ItemCode,
        RFIDCode = this.RFIDCode,

        ProductName = this.ProductName,
        ProductCode = this.ProductCode,

        CategoryId = this.CategoryId,
        CategoryName = this.CategoryName,

        ProductId = this.ProductId,
        DesignId = this.DesignId,
        DesignName = this.DesignName,

        PurityId = this.PurityId,
        PurityName = this.PurityName,

        GrossWt = this.GrossWt,
        NetWt = this.NetWt,

        TotalStoneWeight = this.StoneWt,
        TotalStoneAmount = this.StoneAmount,

        DiamondWeight = this.DiamondWt,
        TotalDiamondAmount = this.DiamondAmount,

        MakingPerGram = this.MakingPerGram,
        MakingFixedAmt = this.MakingFixed,
        MakingPercentage = this.MakingPercentage,
        MakingFixedWastage = this.FixedWt,

        BranchId = this.BranchId,
        SKU = this.SKU,
        Images = this.Image,
        MRP = this.MRP,

        // ✅ MUST PASS THESE TWO (otherwise error)
        Stones = this.Stones ?: emptyList(),        // if CustomOrderItem has Stones
        Diamonds =  emptyList()     // if CustomOrderItem has Diamonds
    )
}



suspend fun generateTablePdfWithImages1(context: Context, order: CustomOrderRequest,localizedContext:Context) {
    val file = File(
        context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS),
        "Order_${order.Customer.FirstName}.pdf"
    )
    val writer = PdfWriter(file)
    val pdf = PdfDocument(writer)
    val doc = Document(pdf, PageSize.A4)
    doc.setMargins(20f, 20f, 20f, 20f)
    val header = Paragraph(localizedContext.getString(R.string.customer_order))
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

        val leftText: String
        val rightText: String


        if (order.ClientCode.equals("LS000026")) {


            // 🔹 Special client format
            leftText = """
        Name     : ${order.Customer.FirstName} ${order.Customer.LastName}
        Order No : ${item.OrderNo ?: "-"}
        Design   : ${item.DesignName ?: "-"}
        RFID No  : ${item.RFIDCode ?: "-"}
    """.trimIndent()


            rightText = """
        Quantity : ${item.Quantity ?: "-"}
        Remark  : ${item.Remark ?: "-"}
        Category Wt : ${item.WeightCategories?.takeIf { it.isNotBlank() && it != "null" } ?: "-"}
    """.trimIndent()

        } else
        {
            leftText = """
            Name     : ${order.Customer.FirstName} ${order.Customer.LastName}
            Order No : ${item.OrderNo ?: "-"}
            Design   : ${item.DesignName ?: "-"}
            RFID No  : ${item.RFIDCode ?: "-"}
            Quantity  : ${item.Quantity ?: "-"}
        """.trimIndent()

            // Right column text
            rightText = """
            Gross Wt : ${item.GrossWt ?: "-"}
            Stone Wt : ${item.StoneWt ?: "-"}
            Net Wt   : ${item.NetWt ?: "-"}
            Remark   : ${item.Remark ?: "-"}
        """.trimIndent()
        }
        infoTable.addCell(Paragraph(leftText).setBorder(null))
        infoTable.addCell(Paragraph(rightText).setBorder(null))
        doc.add(infoTable)
        doc.add(Paragraph("\n"))
        // Big Image Below
        val last = item.Image
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.lastOrNull()

        val imgUrl = last?.let {
            if (it.startsWith("http", true)) it
            else "https://rrgold.loyalstring.co.in/$it"
        }



        val imgBytes = loadImageBytesFromUrl(imgUrl.toString())
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

fun buildOrderRequest(
    orderNo: String,
    employee: Employee,
    productList: SnapshotStateList<OrderItem>,
    selectedCustomer: EmployeeList?,
    gstAmount: Double,
    lastOrderDetails: OrderDetails?
): CustomOrderRequest {

    val IST: ZoneId = ZoneId.of("Asia/Kolkata")

    fun nowIsoDateTime(): String =
        OffsetDateTime.now(IST).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) // 2025-12-22T12:34:56+05:30

    fun nowDateOnly(): String =
        LocalDate.now(IST).format(DateTimeFormatter.ISO_LOCAL_DATE) // 2025-12-22

    fun pickOrderDate(details: OrderDetails?): String {
        val d = details?.orderDate?.trim()
        return if (d.isNullOrEmpty() || d.equals("null", true)) nowIsoDateTime() else d
    }

    fun pickDeliverDate(details: OrderDetails?): String {
        val d = details?.deliverDate?.trim()
        return if (d.isNullOrEmpty() || d.equals("null", true)) nowDateOnly() else d
    }


    fun toIsoLocalDate(input: String?): String? {
        val s = input?.trim().orEmpty()
        if (s.isEmpty() || s.equals("null", true)) return null

        // already ISO
        if (Regex("""\d{4}-\d{2}-\d{2}""").matches(s)) return s

        // dd/MM/yyyy -> yyyy-MM-dd
        return try {
            val inFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { isLenient = false }
            val d = inFmt.parse(s) ?: return null
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d)
        } catch (e: Exception) { null }
    }

    fun toIsoOffsetDateTime(input: String?): String? {
        val s = input?.trim().orEmpty()
        if (s.isEmpty() || s.equals("null", true)) return null

        // already ISO offset
        if (s.contains("T") && (s.endsWith("Z") || s.contains("+") || s.contains("-"))) return s

        // if date-only ISO
        if (Regex("""\d{4}-\d{2}-\d{2}""").matches(s)) {
            val IST = ZoneId.of("Asia/Kolkata")
            return LocalDate.parse(s).atStartOfDay(IST).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        }

        // dd/MM/yyyy -> ISO offset datetime
        return try {
            val inFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { isLenient = false }
            val d = inFmt.parse(s) ?: return null
            val IST = ZoneId.of("Asia/Kolkata")
            d.toInstant().atZone(IST).toLocalDate().atStartOfDay(IST).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        } catch (e: Exception) { null }
    }
    val clientCode = employee?.clientCode.orEmpty()
    val fallbackBranchId = employee?.defaultBranchId ?: 1
    val custId = employee?.id ?: 0

    val totalDiamondWeight = productList.sumOf { it.dimondWt?.toDoubleOrNull() ?: 0.0 }.toString()
    val totalStoneWeight   = productList.sumOf { it.stoneWt?.toDoubleOrNull() ?: 0.0 }.toString()   // ✅ stoneWt
    val totalDiamondAmount = productList.sumOf { it.diamondAmt?.toDoubleOrNull() ?: 0.0 }.toString()
    val totalStoneAmount   = productList.sumOf { it.stoneAmt?.toDoubleOrNull() ?: 0.0 }.toString()
    val totalAmount        = productList.sumOf { it.itemAmt?.toDoubleOrNull() ?: 0.0 }.toString()

    val categoryId   = productList.firstOrNull()?.categoryId ?: 0
    val categoryName = productList.firstOrNull()?.categoryName.orEmpty()
    Log.d( "@@"," selectedCustomer?.Id.toString()"+selectedCustomer?.Id.toString())

    return CustomOrderRequest(
        CustomOrderId = 0,
        CustomerId = selectedCustomer?.Id.toString(),
        ClientCode = clientCode,
        OrderId = 0,

        TotalAmount = totalAmount,
        PaymentMode = "",
        Offer = null,
        Qty = productList.size.toString(),
        GST = "0",
        OrderStatus = "Order Received",
        MRP = null,
        VendorId = null,
        TDS = null,
        PurchaseStatus = null,
        GSTApplied = "false",
        Discount = "0",
        TotalNetAmount = totalAmount,
        TotalGSTAmount = "0",
        TotalPurchaseAmount = totalAmount,
        ReceivedAmount = "0",

        // ✅ MISSING REQUIRED
        TotalBalanceMetal = "0",
        BalanceAmount = totalAmount,
        TotalFineMetal = "0",

        CourierCharge = null,
        SaleType = null,
        OrderDate = pickOrderDate(lastOrderDetails),
        OrderCount = productList.size.toString(),

        // ✅ MISSING REQUIRED
        AdditionTaxApplied = "false",

        CategoryId = categoryId,
        OrderNo = orderNo,
        DeliveryAddress = null,
        BillType = "SampleOut",
        UrdPurchaseAmt = null,

        // ✅ MISSING REQUIRED
        BilledBy = employee?.firstName.orEmpty(),
        SoldBy = employee?.firstName.orEmpty(),

        CreditSilver = null,
        CreditGold = null,
        CreditAmount = null,
        BalanceAmt = totalAmount,
        BalanceSilver = null,
        BalanceGold = null,
        TotalSaleGold = null,
        TotalSaleSilver = null,
        TotalSaleUrdGold = null,
        TotalSaleUrdSilver = null,
        FinancialYear = "2025-26",
        BaseCurrency = "INR",

        TotalStoneWeight = totalStoneWeight,
        TotalStoneAmount = totalStoneAmount,
        TotalStonePieces = "0",
        TotalDiamondWeight = totalDiamondWeight,
        TotalDiamondPieces = "0",
        TotalDiamondAmount = totalDiamondAmount,

        // ✅ MISSING REQUIRED
        FineSilver = "0",
        FineGold = "0",

        DebitSilver = null,
        DebitGold = null,

        // ✅ MISSING REQUIRED
        PaidMetal = "0",
        PaidAmount = "0",

        TotalAdvanceAmt = null,
        TaxableAmount = totalAmount,
        TDSAmount = null,
        CreatedOn = null,
        StatusType = true,

        // ✅ MISSING REQUIRED
        FineMetal = "0",
        BalanceMetal = "0",
        AdvanceAmt = "0",
        PaidAmt = "0",

        TaxableAmt = totalAmount,
        GstAmount = "0",
        GstCheck = "false",
        Category = categoryName,

        // ✅ MISSING REQUIRED
        TDSCheck = "false",

        Remark = null,
        OrderItemId = null,
        StoneStatus = null,
        DiamondStatus = null,
        BulkOrderId = null,

        CustomOrderItem = productList.map { item ->
            CustomOrderItem(
                CustomOrderId = 0,
                RFIDCode = item.rfidCode.orEmpty(),
                OrderDate   = toIsoOffsetDateTime(item.orderDate) ?: nowIsoDateTime(),
                DeliverDate = toIsoLocalDate(item.deliverDate) ?: nowDateOnly(),

                SKUId = item.skuId ?: 0,
                SKU = item.sku.orEmpty(),

                CategoryId = item.categoryId ?: 0,
                VendorId = 0,

                CategoryName = item.categoryName.orEmpty(),
                CustomerName = employee?.firstName.orEmpty(),
                VendorName = "",

                ProductId = item.productId ?: 0,
                ProductName = item.productName.orEmpty(),

                DesignId = item.designid ?: 0,
                DesignName = item.designName.orEmpty(),

                PurityId = item.purityid ?: 0,
                PurityName = item.purity.orEmpty(),

                GrossWt = item.grWt ?: "0.0",
                StoneWt = item.stoneWt ?: "0.0",
                DiamondWt = item.dimondWt ?: "0.0",
                NetWt = item.nWt ?: "0.0",

                // ✅ REQUIRED
                Size = item.size?.toString().orEmpty(),
                Length = item.length?.toString().orEmpty(),
                TypesOdColors = item.typeOfColor?.toString().orEmpty(),

                Quantity =qtyOrOne(item.qty),

                RatePerGram = item.todaysRate ?: "0.0",
                MakingPerGram = item.makingPerGram ?: "0.0",
                MakingFixed = item.makingFixedAmt ?: "0.0",
                FixedWt = item.makingFixedWastage ?: "0.0",
                MakingPercentage = item.makingPercentage ?: "0.0",

                DiamondPieces = "0",
                DiamondRate = "0",
                DiamondAmount = item.diamondAmt ?: "0.0",
                StoneAmount = item.stoneAmt ?: "0.0",

                // ✅ REQUIRED
                ScrewType = item.screwType?.toString().orEmpty(),
                Polish = item.polishType?.toString().orEmpty(),
                Rhodium = "",
                SampleWt = "",

                Image = item.image.orEmpty(),
                ItemCode = item.itemCode.orEmpty(),
                CustomerId = selectedCustomer!!.Id!!.toInt(),

                // ✅ REQUIRED
                MRP = item.mrp ?: "0.0",
                HSNCode = "",
                UnlProductId = 0,
                OrderBy = "",
                StoneLessPercent = "0",

                ProductCode = item.productCode.orEmpty(),
                TotalWt = item.totalWt ?: (item.netAmt ?: "0.0"),
                BillType = "SampleOut",
                FinePercentage = item.finePer ?: "0.0",
                ClientCode = clientCode,

                // ✅ REQUIRED
                OrderId = null,

                StatusType = true,
                PackingWeight = item.packingWt ?: "0.0",

                // ✅ REQUIRED
                MetalAmount = "0.0",
                OldGoldPurchase = false,

                Amount = item.itemAmt ?: "0.0",
                totalGstAmount = gstAmount.toString(),
                finalPrice = item.itemAmt ?: "0.0",

                // ✅ REQUIRED
                MakingFixedWastage = item.makingFixedWastage ?: "0.0",
                Description = item.remark.orEmpty(),

                // ✅ REQUIRED
                CompanyId = item.companyId ?: 0,
                LabelledStockId = 0,
                TotalStoneWeight = item.stoneWt ?: "0.0",

                // ✅ Branch safe (string/int dono case me handle karna ho to yaha simple rakho)
                BranchId = item.branchId?.toIntOrNull() ?: fallbackBranchId,
                BranchName = item.branchName.orEmpty(),

                // ✅ REQUIRED
                Exhibition = item.exhibition.orEmpty(),

                CounterId = (item.counterId ?: 0).toString(),
                EmployeeId = employee?.id ?: 0,

                OrderNo = orderNo,
                OrderStatus = "Order Received",

                // ✅ REQUIRED
                DueDate = null,

                Remark = item.remark,

                // ✅ REQUIRED
                PurchaseInvoiceNo = null,
                Purity = item.purity.orEmpty(),
                Status = null,
                URDNo = null,
                HallmarkAmount = item.hallmarkAmt,
                WeightCategories = item.CategoryWt?.takeIf { it.isNotBlank() && it != "null" },

                Stones = emptyList(),
                Diamond = emptyList()
            )
        },

        Payments = emptyList(),
        uRDPurchases = emptyList(),
        Customer = Customer(
            FirstName = selectedCustomer?.FirstName.orEmpty(),
            LastName = selectedCustomer?.LastName.orEmpty(),
            PerAddStreet = "",
            CurrAddStreet = "",
            Mobile = selectedCustomer?.Mobile.orEmpty(),
            Email = selectedCustomer?.Email.orEmpty(),
            Password = "",
            CustomerLoginId = selectedCustomer?.Email.orEmpty(),
            DateOfBirth = "",
            MiddleName = "",
            PerAddPincode = "",
            Gender = "",
            OnlineStatus = "",
            CurrAddTown = selectedCustomer?.CurrAddTown.orEmpty(),
            CurrAddPincode = "",
            CurrAddState = selectedCustomer?.CurrAddState.orEmpty(),
            PerAddTown = "",
            PerAddState = "",
            GstNo = selectedCustomer?.GstNo.orEmpty(),
            PanNo = selectedCustomer?.PanNo.orEmpty(),
            AadharNo = "",
            BalanceAmount = "0",
            AdvanceAmount = "0",
            Discount = "0",
            CreditPeriod = "",
            FineGold = "0",
            FineSilver = "0",
            ClientCode = selectedCustomer?.ClientCode.orEmpty(),
            VendorId = 0,
            AddToVendor = false,
            CustomerSlabId = 0,
            CreditPeriodId = 0,
            RateOfInterestId = 0,
            Remark = "",
            Area = "",
            City = selectedCustomer?.City.orEmpty(),
            Country = selectedCustomer?.Country.orEmpty(),
            Id = selectedCustomer?.Id ?: 0,
            CreatedOn = "2025-07-08",
            LastUpdated = "2025-07-08",
            StatusType = true
        ),

        syncStatus = false,
        LastUpdated = null
    )

}



fun toIsoLocalDate(input: String?): String? {
    val s = input?.trim().orEmpty()
    if (s.isEmpty() || s.equals("null", true)) return null

    // already ISO
    if (Regex("""\d{4}-\d{2}-\d{2}""").matches(s)) return s

    // dd/MM/yyyy -> yyyy-MM-dd
    return try {
        val inFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { isLenient = false }
        val d = inFmt.parse(s) ?: return null
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d)
    } catch (e: Exception) { null }
}

fun toIsoOffsetDateTime(input: String?): String? {
    val s = input?.trim().orEmpty()
    if (s.isEmpty() || s.equals("null", true)) return null

    // already ISO offset
    if (s.contains("T") && (s.endsWith("Z") || s.contains("+") || s.contains("-"))) return s

    // if date-only ISO
    if (Regex("""\d{4}-\d{2}-\d{2}""").matches(s)) {
        val IST = ZoneId.of("Asia/Kolkata")
        return LocalDate.parse(s).atStartOfDay(IST).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    // dd/MM/yyyy -> ISO offset datetime
    return try {
        val inFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { isLenient = false }
        val d = inFmt.parse(s) ?: return null
        val IST = ZoneId.of("Asia/Kolkata")
        d.toInstant().atZone(IST).toLocalDate().atStartOfDay(IST).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    } catch (e: Exception) { null }
}


private fun buildOrderItemFromSelectedItem(
    selectedItem: ItemCodeResponse,
    details: OrderDetails,
    branchList: List<BranchModel>,
    dailyRates: List<DailyRateResponse>
): OrderItem {

    val branchObj = branchList.firstOrNull { it.BranchName == details.branch }
    val branchIdStr = (branchObj?.Id ?: selectedItem.BranchId ?: 0).toString()
    val IST: ZoneId = ZoneId.of("Asia/Kolkata")



    fun nowIsoDateTime(): String =
        OffsetDateTime.now(IST).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) // 2025-12-22T12:34:56+05:30

    fun nowDateOnly(): String =
        LocalDate.now(IST).format(DateTimeFormatter.ISO_LOCAL_DATE) // 2025-12-22
    fun pickOrderDate(details: OrderDetails?): String {
        val d = details?.orderDate?.trim()
        return if (d.isNullOrEmpty() || d.equals("null", true)) nowIsoDateTime() else d
    }

    fun pickDeliverDate(details: OrderDetails?): String {
        val d = details?.deliverDate?.trim()
        return if (d.isNullOrEmpty() || d.equals("null", true)) nowDateOnly() else d
    }
    fun safeDouble(v: String?): Double = v?.toDoubleOrNull() ?: 0.0
    // ✅ NetWt
    val netWt = safeDouble(selectedItem.NetWt?.toString())

    // ✅ Stone/Diamond amount
    val stoneAmt = safeDouble(selectedItem.TotalStoneAmount?.toString())
    val diamondAmt = safeDouble(selectedItem.TotalDiamondAmount?.toString())

    // ✅ Find daily rate by purity (details.purity preferred, else selectedItem PurityName)
    val purityKey = details.purity.takeIf { it.isNotBlank() } ?: (selectedItem.PurityName ?: "")
    val rate = dailyRates
        .firstOrNull { it.PurityName.equals(purityKey, ignoreCase = true) }
        ?.Rate
        ?.toString()
        ?.toDoubleOrNull() ?: 0.0

    // ✅ Making fields
    val makingPerGram = safeDouble(selectedItem.MakingPerGram?.toString())
    val makingFixedAmt = safeDouble(selectedItem.MakingFixedAmt?.toString())
    val makingPercent = safeDouble(selectedItem.MakingPercentage?.toString())
    val makingFixedWastage = safeDouble(selectedItem.MakingFixedWastage?.toString())

    // ✅ Metal + Making
    val metalAmt = netWt * rate
    val makingAmt =
        makingPerGram +
                makingFixedAmt +
                ((makingPercent / 100.0) * netWt) +
                makingFixedWastage
    // ✅ Final Item Amount
    val itemAmt = stoneAmt + diamondAmt + metalAmt + makingAmt
    return OrderItem(


        branchId = branchIdStr,
        branchName = details.branch,

        exhibition = details.exhibition,
        remark = details.remark,

        purity = details.purity,
        size = details.size,
        length = details.length,
        typeOfColor = details.typeOfColors,
        screwType = details.screwType,
        polishType = details.polishType,
        finePer = details.finePercentage,
        wastage = details.wastage,

        orderDate = details.orderDate,
        deliverDate = details.deliverDate,

        productName = selectedItem.ProductName.orEmpty(),
        itemCode = selectedItem.ItemCode.orEmpty(),
        rfidCode = selectedItem.RFIDCode.orEmpty(),

        stoneAmt = stoneAmt.toString(),
        diamondAmt = diamondAmt.toString(),

        // ✅ daily-rate based amount
        itemAmt = String.format(java.util.Locale.US, "%.2f", itemAmt),
        netAmt  = String.format(java.util.Locale.US, "%.2f", itemAmt),

        grWt = selectedItem.GrossWt?.toString() ?: "0.0",
        nWt  = selectedItem.NetWt?.toString() ?: "0.0",

        packingWt = selectedItem.PackingWeight?.toString() ?: "0.0",
        totalWt   = selectedItem.TotalWeight?.toString() ?: "0.0",
        stoneWt   = selectedItem.TotalStoneWeight?.toString() ?: "0.0",
        dimondWt  = selectedItem.DiamondWeight?.toString() ?: "0.0",

        sku = selectedItem.SKU.orEmpty(),
        qty = qtyOrOne("0"),

        hallmarkAmt = selectedItem.HallmarkAmount?.toString() ?: "0.0",
        mrp = selectedItem.MRP?.toString() ?: "0.0",
        image = selectedItem.Images.orEmpty(),

        categoryId = selectedItem.CategoryId,
        categoryName = selectedItem.CategoryName.orEmpty(),
        productId = selectedItem.ProductId ?: 0,
        productCode = selectedItem.ProductCode.orEmpty(),
        skuId = selectedItem.SKUId ?: 0,
        designid = selectedItem.DesignId ?: 0,
        designName = selectedItem.DesignName.orEmpty(),
        purityid = selectedItem.PurityId ?: 0,

        counterId = selectedItem.CounterId ?: 0,
        counterName = "",
        companyId = 0,

        epc = selectedItem.TIDNumber.orEmpty(),
        tid = selectedItem.TIDNumber.orEmpty(),

        // ✅ store rate
        todaysRate = rate.toString(),

        makingPercentage = selectedItem.MakingPercentage?.toString() ?: "0",
        makingFixedAmt = selectedItem.MakingFixedAmt?.toString() ?: "0",
        makingFixedWastage = selectedItem.MakingFixedWastage?.toString() ?: "0",
        makingPerGram = selectedItem.MakingPerGram?.toString() ?: "0",
        finePlusWt = "",
        CategoryWt = selectedItem.weightCategory.toString(),



    )
}


fun isoDateTimeOrNull(date: String?): String? {
    val d = date?.trim()
    return if (d.isNullOrEmpty() || d.equals("null", true)) null else d
}

private fun qtyOrOne(raw: Any?): String {
    return when (raw) {
        null -> "1"

        is Number -> {
            val q = raw.toInt()
            if (q <= 0) "1" else q.toString()
        }

        is String -> {
            val s = raw.trim()
            if (s.isEmpty() || s.equals("null", true)) return "1"

            // handle "0", "0.0", "00"
            val d = s.toDoubleOrNull() ?: return "1"
            val q = d.toInt()
            if (q <= 0) "1" else q.toString()
        }

        else -> "1"
    }
}


suspend fun generateTablePdfWithImages(context: Context, order: CustomOrderResponse,localizedContext:Context) {
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
        val leftText: String
        val rightText: String
      //  Log.d("","order.WeightCategories"+order.WeightCategories)


        if (order.ClientCode.equals("LS000026")) {


            // 🔹 Special client format
            leftText = """
        Name     : ${order.Customer.FirstName} ${order.Customer.LastName}
        Order No : ${item.OrderNo ?: "-"}
        Design   : ${item.DesignName ?: "-"}
        RFID No  : ${item.RFIDCode ?: "-"}
    """.trimIndent()

            rightText = """
        Quantity : ${item.Quantity ?: "-"}
        Category Wt : ${item.WeightCategories?.takeIf { it.isNotBlank() && it != "null" } ?: "-"}
        Purity : ${item.PurityName ?: "-"}
        Remark  : ${item.Remark ?: "-"}
    """.trimIndent()

        } else
        {
            leftText = """
            Name     : ${order.Customer.FirstName} ${order.Customer.LastName}
            Order No : ${item.OrderNo ?: "-"}
            Design   : ${item.DesignName ?: "-"}
            RFID No  : ${item.RFIDCode ?: "-"}
            Quantity  : ${item.Quantity ?: "-"}
        """.trimIndent()

            // Right column text
            rightText = """
            Gross Wt : ${item.GrossWt ?: "-"}
            Stone Wt : ${item.StoneWt ?: "-"}
            Net Wt   : ${item.NetWt ?: "-"}
            Remark   : ${item.Remark ?: "-"}
        """.trimIndent()
        }
        infoTable.addCell(Paragraph(leftText).setBorder(null))
        infoTable.addCell(Paragraph(rightText).setBorder(null))
        doc.add(infoTable)
        doc.add(Paragraph("\n"))
        // Big Image Below
        val last = item.Image
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.lastOrNull()

        val imgUrl = last?.let {
            if (it.startsWith("http", true)) it
            else "https://rrgold.loyalstring.co.in/$it"
        }
        Log.d("@@","item.Image"+imgUrl)
        val imgBytes = loadImageBytesFromUrl(imgUrl.toString())
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
