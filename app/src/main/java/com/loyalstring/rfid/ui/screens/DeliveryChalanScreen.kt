package com.loyalstring.rfid.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.sparklepos.models.loginclasses.customerBill.EmployeeList
import com.google.gson.Gson
import com.loyalstring.rfid.MainActivity
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.local.entity.DeliveryChallanItem
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.deliveryChallan.AddDeliveryChallanRequest
import com.loyalstring.rfid.data.model.deliveryChallan.ChallanDetails
import com.loyalstring.rfid.data.model.deliveryChallan.CustomerTunchResponse
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanItemPrint
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanPrintData
import com.loyalstring.rfid.data.model.deliveryChallan.InvoiceFields
import com.loyalstring.rfid.data.model.deliveryChallan.UpdateDeliveryChallanRequest
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.model.order.ItemCodeResponse
import com.loyalstring.rfid.data.reader.ScanKeyListener
import com.loyalstring.rfid.data.remote.data.DailyRateResponse
import com.loyalstring.rfid.data.remote.resource.Resource
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.navigation.Screens
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
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@SuppressLint("UnrememberedMutableState")
@Composable
fun DeliveryChalanScreen(
    onBack: () -> Unit,
    navController: NavHostController,
    challanId: Int? = null
) {
    var isSaving by remember { mutableStateOf(false) }
    val viewModel: BulkViewModel = hiltViewModel()
    val orderViewModel: OrderViewModel = hiltViewModel()
    val singleProductViewModel: SingleProductViewModel = hiltViewModel()
    val deliveryChallanViewModel: DeliveryChallanViewModel = hiltViewModel()
    val context = LocalContext.current
    var selectedPower by remember { mutableStateOf(10) }
    var isScanning by remember { mutableStateOf(false) }
    //var showSuccessDialog by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    var firstPress by remember { mutableStateOf(false) }
    var shouldNavigateBack by remember { mutableStateOf(false) }
    val employee = UserPreferences.getInstance(context).getEmployee(Employee::class.java)

    val userPreferences = UserPreferences.getInstance(context)
    val savedLang = userPreferences.getAppLanguage().ifBlank { "en" }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = currentLocales[0]?.language ?: savedLang
    val localizedContext = LocaleHelper.applyLocale(context, currentLang)

    // Customer input fields
    var customerName by remember { mutableStateOf("") }
    var customerId by remember { mutableStateOf<Int?>(null) }
    var expandedCustomer by remember { mutableStateOf(false) }

    var itemCode by remember { mutableStateOf(TextFieldValue("")) }
    val isLoading by orderViewModel.isItemCodeLoading.collectAsState()
    var showDropdownItemcode by remember { mutableStateOf(false) }
    var selectedCustomer by remember { mutableStateOf<EmployeeList?>(null) }
    val productList = remember { mutableStateListOf<ChallanDetails>() }
    var selectedItem by remember { mutableStateOf<ItemCodeResponse?>(null) }
    val productListViewModel: ProductListViewModel = hiltViewModel()
    var showInvoiceDialog by remember { mutableStateOf(false) }
    var invoiceFields by remember { mutableStateOf<InvoiceFields?>(null) }

    // Sample branch/salesman lists (can come from API)
    //val branchList = listOf("Main Branch", "Sub Branch", "Online Branch")
    //val salesmanList = listOf("Rohit", "Priya", "Vikas")

    val branchList = singleProductViewModel.branches
    val salesmanList by orderViewModel.empListFlow.collectAsState()
    val touchList by deliveryChallanViewModel.customerTunchList.collectAsState()
    //var pendingItem by remember { mutableStateOf<BulkItem?>(null) }
    //  val pendingItem = remember { mutableStateListOf<BulkItem>() }
    val pendingItem = mutableListOf<BulkItem>()
    var pendingBarcodeItem by remember { mutableStateOf<BulkItem?>(null) }
    var baseTotal by remember { mutableStateOf(0.0) }
    var gstAmount by remember { mutableStateOf(0.0) }
    var totalWithGst by remember { mutableStateOf(0.0) }

    LaunchedEffect(employee?.clientCode) {
        val code = employee?.clientCode ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                orderViewModel.getAllEmpList(code)
                orderViewModel.getAllItemCodeList(ClientCodeRequest(code))
                singleProductViewModel.getAllBranches(ClientCodeRequest(code))
                singleProductViewModel.getAllPurity(ClientCodeRequest(code))
                singleProductViewModel.getAllSKU(ClientCodeRequest(code))
                orderViewModel.getDailyRate(ClientCodeRequest(code))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Collect the latest rates
    val dailyRates by orderViewModel.getAllDailyRate.collectAsState()


    val tags by viewModel.scannedTags.collectAsState()
    val scanTrigger by viewModel.scanTrigger.collectAsState()

    LaunchedEffect(challanId) {
        if (challanId != null && challanId != 0) {
            isEditMode = true

            // ✅ Step 1: Load challan list if not already loaded
            employee?.let {
                deliveryChallanViewModel.fetchAllChallans(it.clientCode ?: "", it.branchNo ?: 0)
            }

            // ✅ Step 2: Observe challan list and find the matching one
            deliveryChallanViewModel.challanList.collect { challans ->
                val selected = challans.firstOrNull { it.Id == challanId }
                if (selected != null) {
                    deliveryChallanViewModel.setSelectedChallan(selected)

                    // ✅ Step 3: Prefill UI fields
                    customerName = selected.CustomerName.toString()
                    customerId = selected.CustomerId
                    Log.d("@@","customerName"+customerName+" "+customerId)
                    productList.clear()
                   // selected.ChallanDetails?.let { productList.addAll(it) }
                    selected.ChallanDetails?.forEach { item ->
                        val newItem = item   // same object reference

                        newItem.CustomerName = customerName
                        newItem.CustomerId = customerId!!
                        // copy other common fields if needed
                        // newItem.BranchId = selected.BranchId

                        val alreadyExists = productList.any {
                            (!newItem.ItemCode.isNullOrBlank() &&
                                    it.ItemCode.equals(newItem.ItemCode, ignoreCase = true)) ||
                                    (!newItem.RFIDCode.isNullOrBlank() &&
                                            it.RFIDCode.equals(newItem.RFIDCode, ignoreCase = true)) ||
                                    (!newItem.tid.isNullOrBlank() &&
                                            it.tid.equals(newItem.tid, ignoreCase = true))
                        }
                        if (!alreadyExists) {
                            productList.add(newItem)
                        } else {
                            Toast.makeText(context, "Item already added", Toast.LENGTH_SHORT).show()
                        }

                    }

                }
            }
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


    val selectedOrderItem = DeliveryChallanItem(
        id = 0,
        branchId = "",
        branchName = "Main Branch",
        exhibition = "",
        remark = "",
        purity = "22K",
        size = "",
        length = "",
        typeOfColor = "",
        screwType = "",
        polishType = "",
        finePer = "",
        wastage = "",
        orderDate = "",
        deliverDate = "",
        productName = "",
        itemCode = "",
        rfidCode = "",
        grWt = "",
        nWt = "",
        stoneAmt = "",
        finePlusWt = "",
        itemAmt = "",
        packingWt = "",
        totalWt = "",
        stoneWt = "",
        dimondWt = "",
        sku = "",
        qty = "",
        hallmarkAmt = "",
        mrp = "",
        image = "",
        netAmt = "",
        diamondAmt = "",
        categoryId = 0,
        categoryName = "",
        productId = 0,
        productCode = "",
        skuId = 0,
        designid = 0,
        designName = "",
        purityid = 0,
        counterId = 0,
        counterName = "",
        companyId = 0,
        epc = "",
        tid = "",
        todaysRate = "",
        makingPercentage = "",
        makingFixedAmt = "",
        makingFixedWastage = "",
        makingPerGram = ""
    )



    var itemCodeList by remember { mutableStateOf<List<ItemCodeResponse>>(emptyList()) }
    LaunchedEffect(Unit) {
        orderViewModel.itemCodeResponse.collect { items ->
            itemCodeList = items   // assign collected items into your mutable state
        }
    }
   /* val filteredApiList = remember(itemCode.text, itemCodeList, isLoading) {
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
    }*/
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



  /*  LaunchedEffect(shouldNavigateBack) {
        if (shouldNavigateBack) {
            kotlinx.coroutines.delay(50)
            onBack()
        }
    }*/

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



    val addCustomerState by orderViewModel.addEmpReposnes.observeAsState()
    LaunchedEffect(addCustomerState) {
        when (val state = addCustomerState) {
            is Resource.Success -> {
                Toast.makeText(
                    context,
                    state.message ?: localizedContext.getString(R.string.msg_customer_added),
                    Toast.LENGTH_SHORT
                ).show()
            }
            is Resource.Error -> {
                Toast.makeText(context, state.message ?: localizedContext.getString(R.string.error), Toast.LENGTH_SHORT).show()
            }
            is Resource.Loading -> {}
            null -> {}
        }
    }
    /*LaunchedEffect(tags) {
        if (tags.isNotEmpty()) {
            Log.d("RFIDScan", "📦 Received ${tags.size} scanned tags: $tags")

            tags.forEach { epc ->

                // 🟣 Find matching item by RFID in your local product list or API
                val matchedItem = allItems.firstOrNull { item ->
                    val match = item.epc.equals("E2801191A50400703908F0FB", ignoreCase = true)
                    Log.d("matchedItem", "🔍 Checking ${item.epc} — match = $match")
                    match
                }

                Log.d(
                    "matchedItem",
                    "📦 Received ${tags.size} scanned matchedItem tags: $matchedItem"
                )
                // inside your loop where you have `matchedItem` (ItemCodeResponse?)
                if (matchedItem != null) {
                    // safe numeric helpers
                    fun String?.asDouble(default: Double = 0.0): Double = this?.toDoubleOrNull() ?: default
                    fun Int?.asDouble(default: Double = 0.0): Double = this?.toDouble() ?: default
                    fun Int?.asInt(default: Int = 0): Int = this ?: default

                    val qtyValue: Double = matchedItem.totalQty?.toDouble()
                        ?: matchedItem.pcs?.toDouble() ?: 1.0


                    val selectedSku = matchedItem.sku ?: ""


                    // 1) Trigger ViewModel API load (NO CALLBACK)
                    if (touchList.isEmpty()) {
                        deliveryChallanViewModel.fetchCustomerTunch(
                            employee?.clientCode.toString(),
                            employee?.id?.toInt()
                        )
                        return@LaunchedEffect    // wait for next recomposition
                    }
                    // 2) Read touch list from StateFlow
                    val touchMatch = touchList.firstOrNull {
                        it.CustomerId == customerId &&
                        it.StockKeepingUnit.equals(selectedSku, ignoreCase = true)
                    }

                    var makingPercent = matchedItem.makingPercent ?: "0.0"
                    var wastagePercent = matchedItem.fixWastage ?: "0.0"
                    var makingFixedWastage = matchedItem.fixWastage ?: "0.0"
                    var makingFixedAmt = matchedItem.fixMaking ?: "0.0"
                    var makingPerGram = matchedItem.makingPerGram ?: "0.0"


                    if (touchMatch != null) {
                        Log.d("TouchMatch", "✔ SKU matched in Touch API")

                        makingPercent = touchMatch.MakingPercentage ?: makingPercent
                        wastagePercent = touchMatch.WastageWt ?: wastagePercent
                        makingFixedWastage = touchMatch.MakingFixedWastage ?: makingFixedWastage
                        makingFixedAmt = touchMatch.MakingFixedAmt ?: makingFixedAmt
                        makingPerGram = touchMatch.MakingPerGram ?: makingPerGram
                    } else {
                        Log.d("TouchMatch", "❌ No touch settings found for SKU")
                    }



                    val productDetail = ChallanDetails(
                        ChallanId = 0,
                        MRP = matchedItem.mrp?.toString() ?: "0.0",
                        CategoryName = matchedItem.category.orEmpty(),
                        ChallanStatus = "Pending",
                        ProductName = matchedItem.productName.orEmpty(),
                        Quantity = (matchedItem.totalQty ?: matchedItem.pcs ?: 1).toString(),
                        HSNCode = "",
                        ItemCode = matchedItem.itemCode.orEmpty(),
                        GrossWt = matchedItem.grossWeight ?: "0.0",
                        NetWt = matchedItem.netWeight ?: "0.0",
                        ProductId = matchedItem.productId ?: 0,
                        CustomerId = 0,
                        MetalRate = ""?.toString() ?: "0.0",
                        MakingCharg = matchedItem.makingPerGram ?: "0.0",
                        Price = matchedItem.mrp?.toString() ?: "0.0",
                        HUIDCode = "",
                        ProductCode = matchedItem.productCode.orEmpty(),
                        ProductNo = "",
                        Size = "1" ?: "",
                        StoneAmount = matchedItem.stoneAmount ?: "0.0",
                        TotalWt = matchedItem.totalGwt?.toString() ?: "0.0",
                        PackingWeight = "" ?: "0.0",
                        MetalAmount =""?.toString() ?: "0.0",
                        OldGoldPurchase = false,
                        RatePerGram ="" ?: "0.0",
                        Amount =""?.toString() ?: "0.0",
                        ChallanType = "Delivery",
                        FinePercentage = ""?: "0.0",
                        PurchaseInvoiceNo = "",
                        HallmarkAmount = "" ?: "0.0",
                        HallmarkNo =""?: "",
                        MakingFixedAmt = makingFixedAmt,
                        MakingFixedWastage = makingFixedWastage,
                        MakingPerGram = makingPerGram,
                        MakingPercentage = makingPercent,
                        Description = ""?: "",
                        CuttingGrossWt = matchedItem.grossWeight ?: "0.0",
                        CuttingNetWt = matchedItem.netWeight ?: "0.0",
                        BaseCurrency = "INR",
                        CategoryId = matchedItem.categoryId ?: 0,
                        PurityId = 0?: 0,
                        TotalStoneWeight = matchedItem.totalStoneWt?.toString() ?: "0.0",
                        TotalStoneAmount = matchedItem.stoneAmount ?: "0.0",
                        TotalStonePieces = ""?.toString() ?: "0",
                        TotalDiamondWeight = matchedItem.diamondWeight ?: "0.0",
                        TotalDiamondPieces ="".toString() ?: "0",
                        TotalDiamondAmount = matchedItem.diamondAmount ?: "0.0",
                        SKUId =0 ?: 0,
                        SKU = matchedItem.sku.orEmpty(),
                        FineWastageWt = matchedItem.fixWastage ?: "0.0",
                        TotalItemAmount ="".toString() ?: "0.0",
                        ItemAmount = "".toString() ?: "0.0",
                        ItemGSTAmount = "0.0",
                        ClientCode = "",
                        DiamondSize = "",
                        DiamondWeight = "0.0",
                        DiamondPurchaseRate = "0.0",
                        DiamondSellRate = "0.0",
                        DiamondClarity = "",
                        DiamondColour = "",
                        DiamondShape = "",
                        DiamondCut = "",
                        DiamondName = "",
                        DiamondSettingType = "",
                        DiamondCertificate = "",
                        DiamondPieces = "0",
                        DiamondPurchaseAmount = "0.0",
                        DiamondSellAmount = "0.0",
                        DiamondDescription = "",
                        MetalName = "",
                        NetAmount = "0.0",
                        GSTAmount = "0.0",
                        TotalAmount = "0.0",

                        Purity = matchedItem.purity ?: "",
                        DesignName = matchedItem.design ?: "",
                        CompanyId = 0?: 0,
                        BranchId = matchedItem.branchId ?: 0,
                        CounterId = matchedItem.counterId ?: 0,
                        EmployeeId = 0,
                        LabelledStockId = 0 ?: 0,
                        FineSilver = "0.0",
                        FineGold = "0.0",
                        DebitSilver = "0.0",
                        DebitGold = "0.0",
                        BalanceSilver = "0.0",
                        BalanceGold = "0.0",
                        ConvertAmt = "0.0",
                        Pieces = matchedItem.pcs?.toString() ?: "1",
                        StoneLessPercent = "0.0",
                        DesignId = matchedItem.designId ?: 0,
                        PacketId = matchedItem.packetId ?: 0,
                        RFIDCode = matchedItem.rfid.orEmpty(),
                        Image = matchedItem.imageUrl.orEmpty(),
                        DiamondWt = matchedItem.diamondWeight ?: "0.0",
                        StoneAmt = matchedItem.stoneAmount ?: "0.0",
                        DiamondAmt = matchedItem.diamondAmount ?: "0.0",
                        FinePer ="" ?: "0.0",
                        FineWt = "" ?: "0.0",
                        qty = (matchedItem.pcs ?: 1),
                        tid = matchedItem.tid ?: "",
                        totayRate = ""?.toString() ?: "0.0",
                        makingPercent = makingPercent,
                        fixMaking = makingFixedAmt,
                        fixWastage = makingFixedWastage

                    )


                    // Prevent duplicates by RFIDCode (or ItemCode if you prefer)
                    if (productList.none { it.RFIDCode == productDetail.RFIDCode }) {
                        productList.add(productDetail)
                        Log.d("RFIDScan", "✅ Added ${productDetail.ItemCode} (${productDetail.RFIDCode})")
                    } else {
                        Log.d("RFIDScan", "⚠️ Duplicate tag skipped: ${productDetail.RFIDCode}")
                    }
                } else {
                    Log.w("RFIDScan", "❌ No match found for RFID: $epc")
                }

            }
        }
    }*/

    var pendingRFIDItem by remember { mutableStateOf<BulkItem?>(null) }

  /*  LaunchedEffect(tags,allItems) {
        if (tags.isEmpty()) return@LaunchedEffect

        Log.d("RFIDScan", "📦 ${tags.size} tags received → $tags")

        Log.d(
            "RFIDScan",
            "📚 allItems (${allItems.size}): " +
                    allItems.joinToString(separator = " | ") { item ->
                        "epc=${item.epc}, rfid=${item.rfid}, code=${item.itemCode}"
                    }
        )

        tags.forEach { epc ->

            // find item matching RFID
          *//*  val matchedItem = allItems.firstOrNull { item ->
                val itemEpc = item.epc?.trim()
                val scannedEpc = epc.toString().trim()

                // 👇 yaha dono print / log karo
                Log.d("EPC_CHECK", "Item EPC: $itemEpc | Scanned EPC: $scannedEpc")

                itemEpc.equals(scannedEpc, ignoreCase = true)
            }*//*

            val matchedItem = allItems.firstOrNull { item ->
                val itemEpc = item.epc?.trim()?.uppercase()?.replace(" ", "") ?: ""
                val scannedEpc = epc.toString().trim().uppercase().replace(" ", "")
                Log.w("matchedItem itemEpc", "❌ No item found for EPC: $itemEpc")
                Log.w("matchedItem scannedEpc", "❌ No item found for EPC: $scannedEpc")
                itemEpc == scannedEpc
            }

            if (matchedItem == null) {
                Log.w("RFIDScan", "❌ No item found for EPC: $epc")
                return@forEach
            }

            // prevent duplicates
            if (productList.any { it.RFIDCode == matchedItem.rfid }) {
                Log.d("RFIDScan", "⚠️ Duplicate RFID skipped: ${matchedItem.rfid}")
                return@forEach
            }

            Log.d("RFIDScan", "✔ RFID matched item: ${matchedItem.itemCode}")

            // store temporarily
            pendingRFIDItem = matchedItem

            // call API only if touchList empty
            if (touchList.isEmpty()) {
                deliveryChallanViewModel.fetchCustomerTunch(
                    employee?.clientCode.orEmpty(),
                    employee?.id?.toInt() ?: 0
                )
            }
        }
    }*/
    LaunchedEffect(allItems, dailyRates, touchList) {
        viewModel.barcodeReader.openIfNeeded()

        fun normalize(value: String?): String {
            return value
                ?.trim()
                ?.uppercase()
                ?.replace(" ", "")
                ?.replace("\n", "")
                ?.replace("\r", "")
                ?: ""
        }

        fun sameCode(a: String?, b: String?): Boolean {
            val x = normalize(a)
            val y = normalize(b)
            return x.isNotBlank() && y.isNotBlank() && x == y
        }

        fun showToast(msg: String) {
            (context as? Activity)?.runOnUiThread {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.barcodeReader.setOnBarcodeScanned { scannedRaw ->
            val scanned = normalize(scannedRaw)

            itemCode = TextFieldValue(scanned)

            val matchedItem = allItems.firstOrNull { item ->
                val candidates = listOf(
                    normalize(item.itemCode),
                    normalize(item.rfid),
                    normalize(item.productCode),
                    normalize(item.tid),
                    normalize(item.epc)
                )

                candidates.any { code ->
                    code.isNotBlank() &&
                            (code == scanned || code.contains(scanned) || scanned.contains(code))
                }
            }

            if (matchedItem == null) {
                showToast("Item not found")
                Log.d("BarcodeScan", "❌ No item found for $scannedRaw")
                return@setOnBarcodeScanned
            }



            val alreadyExists = productList.any {
                sameCode(it.ItemCode, matchedItem.itemCode) ||
                        sameCode(it.RFIDCode, matchedItem.rfid) ||
                        sameCode(it.tid, matchedItem.tid)
            }

            if (alreadyExists) {
                showToast("Item already added: ${matchedItem.itemCode}")
                itemCode = TextFieldValue("")
                return@setOnBarcodeScanned
            }

            val touchMatch = touchList.firstOrNull {
                it.CustomerId == customerId &&
                        it.StockKeepingUnit.equals(matchedItem.sku, ignoreCase = true)
            }

            val challanItem = buildChallanDetails(
                matchedItem = matchedItem,
                touchMatch = touchMatch,
                dailyRates = dailyRates,
                employee = employee,
                context = context
            )

            productList.add(challanItem)
            showToast("Item added: ${challanItem.ItemCode}")

            itemCode = TextFieldValue("")
        }
    }


    LaunchedEffect(tags, allItems, touchList, dailyRates) {

        if (tags.isEmpty()) return@LaunchedEffect
        if (allItems.isEmpty()) {
            Log.e("RFIDScan", "❌ allItems EMPTY when tags = $tags")
            return@LaunchedEffect
        }

        // 🔹 Pehle tunch load kara lo
        if (touchList.isEmpty()) {
            Log.d("RFIDScan", "⏳ Touch list empty, calling API then wait…")
            deliveryChallanViewModel.fetchCustomerTunch(
                employee?.clientCode.orEmpty(),
                employee?.id?.toInt() ?: 0
            )
           // return@LaunchedEffect   // touchList aane ke baad ye effect dubara chalega
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

            // 4️⃣ Touch match nikaalo
            val selectedSku = matchedItem.sku ?: ""
            val touchMatch = touchList.firstOrNull {
                it.CustomerId == customerId &&
                        it.StockKeepingUnit.equals(selectedSku, ignoreCase = true)
            }

            // --- Defaults from item ---
            var makingPercent = matchedItem.makingPercent ?: "0.0"
            var wastagePercent = matchedItem.fixWastage ?: "0.0"
            var makingFixedWastage = matchedItem.fixWastage ?: "0.0"
            var makingFixedAmt = matchedItem.fixMaking ?: "0.0"
            var makingPerGram = matchedItem.makingPerGram ?: "0.0"

            // --- Override from Touch (agar mila to) ---
            if (touchMatch != null) {
                Log.d("TouchMatch", "✔ Touch match for SKU=${matchedItem.sku}")
                makingPercent = touchMatch.MakingPercentage ?: makingPercent
                wastagePercent = touchMatch.WastageWt ?: wastagePercent
                makingFixedWastage = touchMatch.MakingFixedWastage ?: makingFixedWastage
                makingFixedAmt = touchMatch.MakingFixedAmt ?: makingFixedAmt
                makingPerGram = touchMatch.MakingPerGram ?: makingPerGram
            } else {
                Log.d("TouchMatch", "❌ No touch data for SKU=${matchedItem.sku}")
            }

            // --- Calculation block (tumhare formula ke hisaab se) ---

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

            // 2. MakingAmt (full):
            //    Making Amt = makingPerGram + fixMaking + (making% * NetWt / 100) + fixWastage
            val makingAmt =
                makingPerGramFinal +
                        fixMaking +
                        ((makingPercentFinal / 100.0) * netWt) +
                        fixWastage

            // 3. ItemAmt = Stone + Diamond + Metal + Making
            val itemAmt = stoneAmt + diamondAmt + metalAmt + makingAmt

            // 4. FineWt = NetWt * Fine%  (agar chahiye)
            val finePercent = safeDouble(matchedItem.makingPercent) // ya alag field
            val fineWt = netWt * finePercent / 100.0
            val fixedWastage = (makingFixedWastage?.toDoubleOrNull() ?: 0.0)
            val net = netWt?.toDouble() ?: 0.0
            fun fmt3(v: Double): String = String.format(Locale.getDefault(), "%.3f", v)

            val finePlusWt = fmt3(
                (net * ((0 + fixedWastage) / 100.0)).coerceAtLeast(0.0)
            )

            // --- Build ChallanDetails ---
            val productDetail = ChallanDetails(
                ChallanId = 0,
                MRP = matchedItem.mrp?.toString() ?: "0.0",
                CategoryName = matchedItem.category.orEmpty(),
                ChallanStatus = "Sold",
                ProductName = matchedItem.productName.orEmpty(),
                Quantity = (matchedItem.totalQty ?: matchedItem.pcs ?: 1).toString(),
                HSNCode = "",
                ItemCode = matchedItem.itemCode.orEmpty(),
                GrossWt = matchedItem.grossWeight ?: "0.0",
                NetWt = matchedItem.netWeight ?: "0.0",
                ProductId = matchedItem.productId ?: 0,
                CustomerId = 0,

                MetalRate = rate.toString(),
                MakingCharg = makingAmt.toString(),
                Price = matchedItem.mrp?.toString() ?: "0.0",
                HUIDCode = "",
                ProductCode = matchedItem.productCode.orEmpty(),
                ProductNo = "",
                Size = "" ?: "",
                StoneAmount = matchedItem.stoneAmount ?: "0.0",
                TotalWt = matchedItem.totalGwt?.toString() ?: "0.0",
                PackingWeight = matchedItem.netWeight ?: "0.0",
                MetalAmount = metalAmt.toString(),
                OldGoldPurchase = false,
                RatePerGram = rate.toString(),
                Amount = itemAmt.toString(),
                ChallanType = "Delivery",
                FinePercentage = finePercent.toString(),
                PurchaseInvoiceNo = "",
                HallmarkAmount = "0.0",
                HallmarkNo = "",
                MakingFixedAmt = makingFixedAmt,
                MakingFixedWastage = makingFixedWastage,
                MakingPerGram = makingPerGram,
                MakingPercentage = makingPercent,
                Description = "",

                CuttingGrossWt = matchedItem.grossWeight ?: "0.0",
                CuttingNetWt = matchedItem.netWeight ?: "0.0",
                BaseCurrency = "INR",
                CategoryId = matchedItem.categoryId ?: 0,
                PurityId = 0,
                TotalStoneWeight = matchedItem.totalStoneWt?.toString() ?: "0.0",
                TotalStoneAmount = matchedItem.stoneAmount ?: "0.0",
                TotalStonePieces = "0",
                TotalDiamondWeight = matchedItem.diamondWeight ?: "0.0",
                TotalDiamondPieces = "0",
                TotalDiamondAmount = matchedItem.diamondAmount ?: "0.0",

                SKUId = 0,
                SKU = matchedItem.sku.orEmpty(),
                FineWastageWt = finePlusWt ?: "0.0",
                TotalItemAmount = itemAmt.toString(),
                ItemAmount = itemAmt.toString(),
                ItemGSTAmount = "0.0",
                ClientCode = employee?.clientCode.orEmpty(),

                DiamondSize = "",
                DiamondWeight = matchedItem.diamondWeight ?: "0.0",
                DiamondPurchaseRate = "0.0",
                DiamondSellRate = "0.0",
                DiamondClarity = "",
                DiamondColour = "",
                DiamondShape = "",
                DiamondCut = "",
                DiamondName = "",
                DiamondSettingType = "",
                DiamondCertificate = "",
                DiamondPieces = "0",
                DiamondPurchaseAmount = "0.0",
                DiamondSellAmount = diamondAmt.toString(),
                DiamondDescription = "",

                MetalName = "",
                NetAmount = itemAmt.toString(),
                GSTAmount = "0.0",
                TotalAmount = itemAmt.toString(),

                Purity = matchedItem.purity ?: "",
                DesignName = matchedItem.design ?: "",
                CompanyId = 0,
                BranchId = matchedItem.branchId ?:  UserPreferences.getInstance(context).getBranchID()!!.toInt(),
                CounterId = matchedItem.counterId ?: 0,
                EmployeeId = 0,
                LabelledStockId = matchedItem.bulkItemId ?: 0,
                FineSilver = "0.0",
                FineGold = fineWt.toString(),
                DebitSilver = "0.0",
                DebitGold = "0.0",
                BalanceSilver = "0.0",
                BalanceGold = "0.0",
                ConvertAmt = "0.0",
                Pieces = (matchedItem.pcs ?: 1).toString(),
                StoneLessPercent = "0.0",
                DesignId = matchedItem.designId ?: 0,
                PacketId = matchedItem.packetId ?: 0,
                RFIDCode = matchedItem.rfid.orEmpty(),
                Image = matchedItem.imageUrl.orEmpty(),
                DiamondWt = matchedItem.diamondWeight ?: "0.0",
                StoneAmt = matchedItem.stoneAmount ?: "0.0",
                DiamondAmt = matchedItem.diamondAmount ?: "0.0",
                FinePer = finePercent.toString(),
                FineWt = fineWt.toString(),
                qty = matchedItem.pcs ?: 1,
                tid = matchedItem.tid ?: "",
                totayRate = rate.toString(),
                makingPercent = makingPercent,
                fixMaking = makingFixedAmt,
                fixWastage = makingFixedWastage,
                TIDNumber = matchedItem.tid ?: "",
                CustomerName = "",
                Pcs = matchedItem.pcs?:0
            )

            if (productList.none { it.ItemCode == productDetail.ItemCode }) {
                productList.add(productDetail)
                Log.d("RFIDScan", "✅ Added ${productDetail.ItemCode} (${productDetail.RFIDCode})")
            } else {
                Log.d("RFIDScan", "⚠️ Duplicate tag skipped: ${productDetail.RFIDCode}")
            }
        }
    }



    // 🔹 When last challan number updates → Add the challan
    val lastChallanNo by deliveryChallanViewModel.lastChallanNo.collectAsState()

    LaunchedEffect(lastChallanNo) {

        // Only run when a new value is emitted
        if (!isSaving) return@LaunchedEffect

        val lastNo = lastChallanNo ?: return@LaunchedEffect
        val newChallanNo = lastNo + 1

        val clientCode = employee?.clientCode ?: return@LaunchedEffect
        val branchId = UserPreferences.getInstance(context).getBranchID()!!.toInt()


        Log.d("DeliveryChallan", "➡️ Adding challan with No: $newChallanNo")
        if (customerName.isNullOrBlank()) {
            Toast
                .makeText(context,localizedContext.getString(R.string.please_select_customer), Toast.LENGTH_SHORT)
                .show()
            isSaving = false
            return@LaunchedEffect
        }

        Log.d("@@","branchId"+branchId)
        val request = AddDeliveryChallanRequest(
            BranchId = branchId?.toInt() ?: userPreferences.getBranchID()!!.toInt(),
            TransactionAmtType = "Cash",
            TransactionMetalType = "Gold",
            MetalType = "Gold",
            TransactionDetails = "Delivery Challan Created",
            UrdWt = "0.0",
            UrdAmt = "0.0",
            UrdQuantity = "0",
            UrdGrossWt = "0.0",
            UrdNetWt = "0.0",
            UrdStoneWt = "0.0",
            URDNo = "",
            ClientCode = clientCode,
            CustomerId = customerId?.toString() ?: "0",
            Billedby = employee?.firstName ?: "",
            SaleType = "Challan",
            Soldby = employee?.firstName ?: "",
            PaymentMode = "Cash",
            UrdPurchaseAmt = "0.0",
            GST = "3.0",
            gstDiscout = "0.0",
            TDS = "0.0",
            ReceivedAmount = "0.0",
            ChallanStatus = "Sold",
            Visibility = "true",
            Offer = "0.0",
            CourierCharge = "0.0",
            TotalAmount = productList.sumOf { it.TotalWt.toDoubleOrNull() ?: 0.0 }.toString(),
            BillType = "DeliveryChallan",
            InvoiceDate = java.time.LocalDate.now().toString(),
            InvoiceNo = "",
            BalanceAmt = "0.0",
            CreditAmount = "0.0",
            CreditGold = "0.0",
            CreditSilver = "0.0",
            GrossWt = productList.sumOf { it.GrossWt?.toDoubleOrNull() ?: 0.0 }.toString(),
            NetWt = productList.sumOf { it.NetWt?.toDoubleOrNull() ?: 0.0 }.toString(),
            StoneWt = productList.sumOf { it.NetWt.toDoubleOrNull() ?: 0.0 }.toString(),
            StonePieces = "0",
            Qty = productList.map { it.qty }
                .let { qtyList ->
                    val nonNullSum = qtyList.filterNotNull().sumOf { it }
                    if (nonNullSum == 0 && qtyList.all { it == null }) {
                        productList.size
                    } else {
                        productList.size
                    }
                }.toString(),
            TotalDiamondAmount = "0.0",
            TotalDiamondPieces = "0",
            DiamondPieces = "0",
            TotalDiamondWeight = "0.0",
            DiamondWt = "0.0",
            TotalSaleGold = "0.0",
            TotalSaleSilver = "0.0",
            TotalSaleUrdGold = "0.0",
            TotalSaleUrdSilver = "0.0",
            TotalStoneAmount = "0.0",
            TotalStonePieces = "0",
            TotalStoneWeight = "0.0",
            BalanceGold = "0.0",
            BalanceSilver = "0.0",
            OrderType = "Delivery",
            ChallanDetails = productList,
            Payments = emptyList(),
            TotalPaidMetal = "0.0",
            TotalPaidAmount = "0.0",
            TotalAdvanceAmount = "0.0",
            TotalAdvancePaid = "0.0",
            TotalNetAmount = productList.sumOf { it.TotalWt.toDoubleOrNull() ?: 0.0 }.toString(),
            TotalFineMetal = "0.0",
            TotalBalanceMetal = "0.0",
            GSTApplied = "true",
            gstCheckboxConfirm = "true",
            AdditionTaxApplied = "false",
            TotalGSTAmount = "0.0",
            CustomerName = customerName
        )

        deliveryChallanViewModel.addDeliveryChallan(request)

    }



    /*LaunchedEffect(itemCode.text) {
        val query = itemCode.text.trim()
        if (query.isNotEmpty()) {
            // Find the matching item from your list (ItemCode or RFID)
            val matchedItem = allItems.firstOrNull {
                it.itemCode.equals(query, ignoreCase = true) ||
                        it.rfid.equals(query, ignoreCase = true)
            }

            if (matchedItem != null) {
                // Prevent duplicates
                if (productList.none { it.RFIDCode == matchedItem.rfid }) {


                    //val selectedSku = matchedItem.sku ?: ""
                    val selectedSku= "NECKLACETEMPLE"
                    Log.d(
                        "selectedSku", ""+selectedSku);

                    // 1) Trigger ViewModel API load (NO CALLBACK)
                    if (touchList.isEmpty()) {
                        deliveryChallanViewModel.fetchCustomerTunch(
                            employee?.clientCode.toString(),
                            employee?.id?.toInt()
                        )
                        // 🔥 Don't return — item should still be added
                    }
                    // 2) Read touch list from StateFlow

                    touchList.forEachIndexed { index, t ->
                        Log.d("TouchList", "[$index] SKU = ${t.StockKeepingUnit}, CustomerId = ${t.CustomerId}")
                    }
                    val touchMatch = touchList.firstOrNull {
                      *//*  it.CustomerId == customerId &&*//*
                                it.StockKeepingUnit.equals(selectedSku, ignoreCase = true)
                    }



                    var makingPercent = matchedItem.makingPercent ?: "0.0"
                    var wastagePercent = matchedItem.fixWastage ?: "0.0"
                    var makingFixedWastage = matchedItem.fixWastage ?: "0.0"
                    var makingFixedAmt = matchedItem.fixMaking ?: "0.0"
                    var makingPerGram = matchedItem.makingPerGram ?: "0.0"


                    if (touchMatch != null) {
                        Log.d("TouchMatch", "✔ SKU matched in Touch API")
                        // print all fields
                        Log.d(
                            "TouchMatch", """
        ✔ Touch API Data:
        SKU = ${touchMatch.StockKeepingUnit}
        MakingPercentage = ${touchMatch.MakingPercentage}
        WastageWt = ${touchMatch.WastageWt}
        MakingFixedWastage = ${touchMatch.MakingFixedWastage}
        MakingFixedAmt = ${touchMatch.MakingFixedAmt}
        MakingPerGram = ${touchMatch.MakingPerGram}
    """.trimIndent()
                        )

                        makingPercent = touchMatch.MakingPercentage ?: makingPercent
                        wastagePercent = touchMatch.WastageWt ?: wastagePercent
                        makingFixedWastage = touchMatch.MakingFixedWastage ?: makingFixedWastage
                        makingFixedAmt = touchMatch.MakingFixedAmt ?: makingFixedAmt
                        makingPerGram = touchMatch.MakingPerGram ?: makingPerGram
                    } else {
                        Log.d("TouchMatch", "❌ No touch settings found for SKU")
                    }

                    val productDetail = ChallanDetails(
                        ChallanId = 0,
                        MRP = matchedItem.mrp?.toString() ?: "0.0",
                        CategoryName = matchedItem.category.orEmpty(),
                        ChallanStatus = "Pending",
                        ProductName = matchedItem.productName.orEmpty(),
                        Quantity = (matchedItem.totalQty ?: matchedItem.pcs ?: 1).toString(),
                        HSNCode = "",
                        ItemCode = matchedItem.itemCode.orEmpty(),
                        GrossWt = matchedItem.grossWeight ?: "0.0",
                        NetWt = matchedItem.netWeight ?: "0.0",
                        ProductId = matchedItem.productId ?: 0,
                        CustomerId = 0,
                        MetalRate = ""?.toString() ?: "0.0",
                        MakingCharg = matchedItem.makingPerGram ?: "0.0",
                        Price = matchedItem.mrp?.toString() ?: "0.0",
                        HUIDCode = "",
                        ProductCode = matchedItem.productCode.orEmpty(),
                        ProductNo = "",
                        Size = "1" ?: "",
                        StoneAmount = matchedItem.stoneAmount ?: "0.0",
                        TotalWt = matchedItem.totalGwt?.toString() ?: "0.0",
                        PackingWeight = "" ?: "0.0",
                        MetalAmount =""?.toString() ?: "0.0",
                        OldGoldPurchase = false,
                        RatePerGram ="" ?: "0.0",
                        Amount =""?.toString() ?: "0.0",
                        ChallanType = "Delivery",
                        FinePercentage = ""?: "0.0",
                        PurchaseInvoiceNo = "",
                        HallmarkAmount = "" ?: "0.0",
                        HallmarkNo =""?: "",
                        MakingFixedAmt = makingFixedAmt,
                        MakingFixedWastage = makingFixedWastage,
                        MakingPerGram = makingPerGram,
                        MakingPercentage = makingPercent,
                        Description = ""?: "",
                        CuttingGrossWt = matchedItem.grossWeight ?: "0.0",
                        CuttingNetWt = matchedItem.netWeight ?: "0.0",
                        BaseCurrency = "INR",
                        CategoryId = matchedItem.categoryId ?: 0,
                        PurityId = 0?: 0,
                        TotalStoneWeight = matchedItem.totalStoneWt?.toString() ?: "0.0",
                        TotalStoneAmount = matchedItem.stoneAmount ?: "0.0",
                        TotalStonePieces = ""?.toString() ?: "0",
                        TotalDiamondWeight = matchedItem.diamondWeight ?: "0.0",
                        TotalDiamondPieces ="".toString() ?: "0",
                        TotalDiamondAmount = matchedItem.diamondAmount ?: "0.0",
                        SKUId =0 ?: 0,
                        SKU = matchedItem.sku.orEmpty(),
                        FineWastageWt = matchedItem.fixWastage ?: "0.0",
                        TotalItemAmount ="".toString() ?: "0.0",
                        ItemAmount = "".toString() ?: "0.0",
                        ItemGSTAmount = "0.0",
                        ClientCode = "",
                        DiamondSize = "",
                        DiamondWeight = "0.0",
                        DiamondPurchaseRate = "0.0",
                        DiamondSellRate = "0.0",
                        DiamondClarity = "",
                        DiamondColour = "",
                        DiamondShape = "",
                        DiamondCut = "",
                        DiamondName = "",
                        DiamondSettingType = "",
                        DiamondCertificate = "",
                        DiamondPieces = "0",
                        DiamondPurchaseAmount = "0.0",
                        DiamondSellAmount = "0.0",
                        DiamondDescription = "",
                        MetalName = "",
                        NetAmount = "0.0",
                        GSTAmount = "0.0",
                        TotalAmount = "0.0",

                        Purity = matchedItem.purity ?: "",
                        DesignName = matchedItem.design ?: "",
                        CompanyId = 0?: 0,
                        BranchId = matchedItem.branchId ?: 0,
                        CounterId = matchedItem.counterId ?: 0,
                        EmployeeId = 0,
                        LabelledStockId = 0 ?: 0,
                        FineSilver = "0.0",
                        FineGold = "0.0",
                        DebitSilver = "0.0",
                        DebitGold = "0.0",
                        BalanceSilver = "0.0",
                        BalanceGold = "0.0",
                        ConvertAmt = "0.0",
                        Pieces = matchedItem.pcs?.toString() ?: "1",
                        StoneLessPercent = "0.0",
                        DesignId = matchedItem.designId ?: 0,
                        PacketId = matchedItem.packetId ?: 0,
                        RFIDCode = matchedItem.rfid.orEmpty(),
                        Image = matchedItem.imageUrl.orEmpty(),
                        DiamondWt = matchedItem.diamondWeight ?: "0.0",
                        StoneAmt = matchedItem.stoneAmount ?: "0.0",
                        DiamondAmt = matchedItem.diamondAmount ?: "0.0",
                        FinePer ="" ?: "0.0",
                        FineWt = "" ?: "0.0",
                        qty = (matchedItem.pcs ?: 1),
                        tid = matchedItem.tid ?: "",
                        totayRate = ""?.toString() ?: "0.0",
                        makingPercent = makingPercent,
                        fixMaking = makingFixedAmt,
                        fixWastage = makingFixedWastage

                    )


                    productList.add(productDetail)
                    Log.d("ManualEntry", "✅ Added ${matchedItem.itemCode}")
                    itemCode = TextFieldValue("") // clear input
                } else {
                    Log.d("ManualEntry", "⚠️ Already exists: ${matchedItem.itemCode}")
                }
            }
        }
    }
*/
    var pendingMatchedItem by remember { mutableStateOf<BulkItem?>(null) }
  /*  LaunchedEffect(itemCode.text) {
        val query = itemCode.text.trim()
        if (query.isEmpty()) return@LaunchedEffect

        val matchedItem = allItems.firstOrNull { item ->
            val hasRfid = !item.rfid.isNullOrBlank()
            if (hasRfid) item.rfid.equals(query, true)
            else item.itemCode.equals(query, true)
        } ?: return@LaunchedEffect

        val isDuplicate = productList.any { product ->
            if (!matchedItem.rfid.isNullOrBlank())
                product.RFIDCode.equals(matchedItem.rfid, true)
            else
                product.ItemCode.equals(matchedItem.itemCode, true)
        }

        if (isDuplicate) {
            itemCode = TextFieldValue("")
            return@LaunchedEffect
        }

        pendingMatchedItem = matchedItem
        itemCode = TextFieldValue("")   // ✅ safe now

        if (touchList.isEmpty()) {
            deliveryChallanViewModel.fetchCustomerTunch(
                employee?.clientCode.orEmpty(),
                employee?.id?.toInt() ?: 0
            )
        }
    }*/

  /*  LaunchedEffect(pendingMatchedItem, touchList) {
        val matchedItem = pendingMatchedItem ?: return@LaunchedEffect

        // 🔹 Try to find touch match (may be null)
        val touchMatch = touchList.firstOrNull { touch ->
            touch.CustomerId == customerId &&
                    touch.StockKeepingUnit
                        ?.trim()
                        ?.equals(matchedItem.sku, true) == true
        }

        // 🔹 Build even if touchMatch == null
        val challanItem = buildChallanDetails(
            matchedItem = matchedItem,
            touchMatch = touchMatch,   // ✅ null-safe
            dailyRates = dailyRates,
            employee = employee,
            context=context
        )

        productList.add(challanItem)
        pendingMatchedItem = null
    }*/



    /* LaunchedEffect(itemCode.text) {
         val query = itemCode.text.trim()
         if (query.isEmpty()) return@LaunchedEffect

         val matchedItem = allItems.firstOrNull { item ->
             val hasRfid = !item.rfid.isNullOrBlank()

             if (hasRfid) {
                 item.rfid.equals(query, ignoreCase = true)
             } else {
                 item.itemCode.equals(query, ignoreCase = true)
             }
         } ?: return@LaunchedEffect

         Log.d("@@","matchedItem"+matchedItem)

         // ✅ Prevent duplicate by TID (BEST)
         val isDuplicate = productList.any { product ->
             if (!matchedItem.rfid.isNullOrBlank()) {
                 product.RFIDCode.equals(matchedItem.rfid, ignoreCase = true)
             } else {
                 product.ItemCode.equals(matchedItem.itemCode, ignoreCase = true)
             }
         }

         if (isDuplicate) {
             Log.d("ManualEntry", "⚠️ Duplicate skipped: ${matchedItem.itemCode}")
             itemCode = TextFieldValue("")
             return@LaunchedEffect
         }
         Log.d("@@","matchedItem after last"+matchedItem)

         // ✅ Add to queue (NOT overwrite)
         pendingItem.add(matchedItem)

         Log.d("@@","matchedItem after last"+pendingItem.toString())


         // Fetch touch once
         // Fetch touch once
         if (touchList.isEmpty()) {
             deliveryChallanViewModel.fetchCustomerTunch(
                 employee?.clientCode.orEmpty(),
                 employee?.id?.toInt() ?: 0
             )

             Log.d("TOUCH_MATCH_DEBUG", "Touch list empty → fetching, wait...")
             return@LaunchedEffect   // 🔥 THIS WAS MISSING
         }


         *//*val touchMatch = touchList.firstOrNull {
            it.CustomerId == customerId &&
                    it.StockKeepingUnit.equals(matchedItem.sku, true)
        }*//*

      //  if(!touchList.isEmpty()) {
            val touchMatch = touchList.firstOrNull { touch ->
                val customerOk = touch.CustomerId == customerId
                val skuOk = touch.StockKeepingUnit
                    ?.trim()
                    ?.equals(matchedItem.sku, ignoreCase = true) == true

                Log.d(
                    "TOUCH_MATCH_DEBUG",
                    "checking → CustomerId=${touch.CustomerId}, SKU=${touch.StockKeepingUnit}, " +
                            "customerOk=$customerOk, skuOk=$skuOk"
                )

                customerOk && skuOk
            }


            val challanItem = buildChallanDetails(
                matchedItem = matchedItem,
                touchMatch = touchMatch,
                dailyRates = dailyRates,
                employee = employee
            )

            productList.add(challanItem)
            if (!matchedItem.toString().isNullOrBlank()) {
                itemCode = TextFieldValue("")
            }
       *//* }
        else
        {
            Log.d(
                "TOUCH_MATCH_DEBUG",
                "checking → tounchlist=${touchList.size}")
        }*//*

       // itemCode = TextFieldValue("") // clear input safely
    }*/

   /* LaunchedEffect(touchList, pendingItem) {

        if (touchList.isEmpty()) return@LaunchedEffect
        if (pendingItem.isEmpty()) return@LaunchedEffect
        Log.d("@@", "pendingItem" + pendingItem)
        val itemsToProcess = pendingItem.toList() // 🔑 SAFE COPY

        itemsToProcess.forEach { item ->

            // 🔒 Duplicate check
            if (productList.any { it.ItemCode == item.itemCode }) {
                pendingItem.remove(item)
                return@forEach
            }

            val touchMatch = touchList.firstOrNull {
                it.CustomerId == customerId &&
                        it.StockKeepingUnit.equals(item.sku, ignoreCase = true)
            }

            val challanItem = buildChallanDetails(
                matchedItem = item,
                touchMatch = touchMatch,
                dailyRates = dailyRates,
                employee = employee
            )

            productList.add(challanItem)
            pendingItem.remove(item)

            Log.d("ManualEntry", "✅ Added ${item.itemCode}")
        }
    }*/

  /*  LaunchedEffect(touchList, pendingItem) {

       *//* if (touchList.isEmpty()) return@LaunchedEffect
        if (pendingItem.isEmpty()) return@LaunchedEffect
*//*

        if (pendingItem.isNotEmpty()) {
            itemCode = TextFieldValue("")
        }

        Log.d("@@", "touch=${touchList.size}, pending=${pendingItem.size}")
        val itemsToProcess = pendingItem.toList()

        itemsToProcess.forEach { item ->

            val isDuplicate = productList.any { product ->
                if (!item.rfid.isNullOrBlank()) {
                    product.RFIDCode.equals(item.rfid, true)
                } else {
                    product.ItemCode.equals(item.itemCode, true)
                }
            }

            if (isDuplicate) {
                pendingItem.remove(item)
                return@forEach
            }

            val touchMatch = touchList.firstOrNull {
                it.CustomerId == customerId &&
                        it.StockKeepingUnit.equals(item.sku, true)
            }

            val challanItem = buildChallanDetails(
                matchedItem = item,
                touchMatch = touchMatch,
                dailyRates = dailyRates,
                employee = employee
            )

            productList.add(challanItem)
            pendingItem.remove(item)

            Log.d("ManualEntry", "✅ Added ${item.itemCode}")
        }
    }*/



// 🔹 Show success message when challan addedfval pendingItem =
    val addChallanResponse by deliveryChallanViewModel.addChallanResponse.collectAsState()

    LaunchedEffect(addChallanResponse) {
        addChallanResponse?.let { response ->
            Toast.makeText(
                context,
               localizedContext.getString(R.string.msg_challan_saved),
                Toast.LENGTH_SHORT
            ).show()

            viewModel.resetProductScanResults()


            // Optional: clear after short delay so toast doesn’t miss it
            kotlinx.coroutines.delay(500)
            //deliveryChallanViewModel.clearAddChallanResponse()

            // Prepare print data from your screen state
            val itemsForPrint = productList.map { detail ->
                DeliveryChallanItemPrint(
                    itemName = detail.DesignName ?: "",
                    purity = detail.Purity ?: "",
                    pcs = detail.Pieces?.toIntOrNull() ?: detail.qty ?: 0,
                    grossWt = detail.GrossWt ?: "0.000 gm",
                    stoneWt = detail.TotalStoneWeight ?: "0 gm",
                    netWt = detail.NetWt ?: "0.000 gm",
                    ratePerGram = detail.MetalRate ?: "0",
                    wastage = detail.FineWastageWt ?: "0%",
                    itemAmount = detail.ItemAmount ?: "0.00"
                )
            }

            fun formatIsoToNormal(input: String?): String {
                if (input.isNullOrBlank()) return ""

                return try {
                    val odt = OffsetDateTime.parse(input) // parses +05:30 also
                    val outFmt =
                        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm", Locale.getDefault())
                    odt.format(outFmt)
                } catch (e: Exception) {
                    ""
                }
            }

            val printData = DeliveryChallanPrintData(
                branchName = "QA",
                city = response?.customer?.City ?: "",
                createdDateTime = formatIsoToNormal(response.CreatedOn),
                customerName = customerName,
                quotationNo = (response.InvoiceNo ?: "0").toString(),
                phone = response?.customer?.Mobile ?: "",
                items = itemsForPrint,
                taxableAmount = String.format("%.2f", baseTotal),
                cgstPercent = 1.5,
                cgstAmount = String.format("%.2f", baseTotal * 0.015),
                sgstPercent = 1.5,
                sgstAmount = String.format("%.2f", baseTotal * 0.015),
                totalNetAmount = String.format("%.2f", totalWithGst)
            )

            val uri = generateDeliveryChallanPdf(context, printData)

            if (uri != null) {
                openPdfPreview(context, uri)
                // or:
                // sharePdfOnWhatsApp(context, uri)
            }

            //val activity = LocalContext.current.findActivity()

          /*  BT printer call
           ensureBluetoothPermissions(context) {
                CoroutineScope(Dispatchers.IO).launch {

                    BluetoothThermalPrinterHelper.printDeliveryChallan(
                        context = context,
                        data = printData,
                        printerName = "4B-2043PB-B799"
                    )
                }
            }*/
            //Toast.makeText(context, "printer class", Toast.LENGTH_SHORT).show()
            deliveryChallanViewModel.clearAddChallanResponse()
        }


        resetAllFields(   onResetCustomerName = { customerName = it },
            onResetCustomerId = { customerId = it },
            onResetSelectedCustomer = { selectedCustomer = it },
            onResetExpandedCustomer = { expandedCustomer = it },
            onResetItemCode = { itemCode = it },
            onResetSelectedItem = { selectedItem = it },
            onResetDropdownItemcode = { showDropdownItemcode = it },
            onResetProductList = { productList.clear() },
            onResetScanning = { isScanning = it },
            viewModel = viewModel,
            deliveryChallanViewModel = deliveryChallanViewModel)
    }



// 🔹 Handle error messages
    LaunchedEffect(deliveryChallanViewModel.error) {
        val errMsg = deliveryChallanViewModel.error.value
        if (!errMsg.isNullOrEmpty()) {
            Toast.makeText(context, "❌ $errMsg", Toast.LENGTH_SHORT).show()
        }
    }

    val activity = LocalContext.current as? MainActivity
    DisposableEffect(Unit) {
        val listener = object : ScanKeyListener {
            override fun onBarcodeKeyPressed() {
                viewModel.startBarcodeScanning(context)
            }

            override fun onRfidKeyPressed() {
                if (!isScanning) {
                    isScanning = true
                   /* scope.launch(Dispatchers.Default) {
                        viewModel.setFilteredItems(scannedItemsSequence.map { it.originalBulkItem }.toList())
                    }*/
                    viewModel.startScanningInventory(selectedPower)
                } else {
                    isScanning = false
                    viewModel.stopScanningAndCompute()
                }
            }
        }
        activity?.registerScanKeyListener(listener)
        onDispose { activity?.unregisterScanKeyListener() }
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

    LaunchedEffect(deliveryChallanViewModel.updateChallanResponse.collectAsState().value) {
        deliveryChallanViewModel.updateChallanResponse.collect { response ->
            if (response != null) {
                Toast.makeText(context, localizedContext.getString(R.string.msg_challan_updated), Toast.LENGTH_SHORT).show()
               // deliveryChallanViewModel.fetchAllChallans(clientCode, branchId)
                onBack()
            }
        }
    }
    fun addItemToList(code: String) {

        val query = code.trim()

        val matchedItem = allItems.firstOrNull { item ->
            val itemCode = item.itemCode?.trim()
            val rfid = item.rfid?.trim()

            itemCode.equals(query, ignoreCase = true) ||
                    rfid.equals(query, ignoreCase = true)
        }

        if (matchedItem == null) {
            Log.d("DropdownSelect", "❌ No match in allItems for $query")
            return
        }

        // duplicate check
        if (productList.any { it.tid == matchedItem.tid }) {
            Log.d("DropdownSelect", "⚠️ Duplicate item skipped ${matchedItem.itemCode}")
            return
        }

        Log.d("DropdownSelect", "✅ Matched item ${matchedItem.itemCode}")

        pendingMatchedItem = matchedItem
    }
    LaunchedEffect(pendingMatchedItem) {

        val item = pendingMatchedItem ?: return@LaunchedEffect

        // clear first, so touchList/recomposition se dobara add na ho
        pendingMatchedItem = null

        val alreadyExists = productList.any {
            (!item.itemCode.isNullOrBlank() &&
                    it.ItemCode.equals(item.itemCode, ignoreCase = true)) ||
                    (!item.rfid.isNullOrBlank() &&
                            it.RFIDCode.equals(item.rfid, ignoreCase = true)) ||
                    (!item.tid.isNullOrBlank() &&
                            it.tid.equals(item.tid, ignoreCase = true))
        }

        if (alreadyExists) {
            Toast.makeText(context, "Item already added", Toast.LENGTH_SHORT).show()
            itemCode = TextFieldValue("")
            return@LaunchedEffect
        }

        val touchMatch = touchList.firstOrNull {
            it.CustomerId == customerId &&
                    it.StockKeepingUnit.equals(item.sku, ignoreCase = true)
        }

        val challanItem = buildChallanDetails(
            matchedItem = item,
            touchMatch = touchMatch,
            dailyRates = dailyRates,
            employee = employee,
            context = context
        )

        productList.add(challanItem)
        itemCode = TextFieldValue("")
    }

    // ✅ This is your barcode scanner logic
  /*  LaunchedEffect(Unit) {

        viewModel.barcodeReader.openIfNeeded()
        viewModel.barcodeReader.setOnBarcodeScanned { scanned ->
            viewModel.onBarcodeScanned(scanned)
            viewModel.setRfidForAllTags(scanned)
            Log.d("RFID Code", scanned)
            itemCode = TextFieldValue(scanned) // triggers recomposition

            val matchedItem = itemCodeList.find { item ->
                item.RFIDCode.equals(
                    scanned,
                    ignoreCase = true
                ) // Match based on TID
            }

            if (matchedItem != null) {
                if (productList.none { it.ItemCode == matchedItem?.ItemCode && it.tid == matchedItem?.TIDNumber }) {

                    Log.d("Match Found", "Item: ${matchedItem.ItemCode}")

                    // Check if the product already exists in the database based on TID (or SKU)
                    val existingProduct = productList.find { product ->
                        product.ItemCode == matchedItem.ItemCode // Match based on TID
                    }

                    if (existingProduct == null) {
                        selectedItem = matchedItem
                        val netWt: Double = (selectedItem?.GrossWt?.toDoubleOrNull()
                            ?: 0.0) - (selectedItem?.TotalStoneWeight?.toDoubleOrNull()
                            ?: 0.0)


                        val metalAmt: Double =
                            (selectedItem?.NetWt?.toDoubleOrNull()
                                ?: 0.0) * (selectedItem?.TodaysRate?.toDoubleOrNull()
                                ?: 0.0)

                        val selectedSku = selectedItem?.SKU ?: ""


                        // 1) Trigger ViewModel API load (NO CALLBACK)
                        if (touchList.isEmpty()) {
                            deliveryChallanViewModel.fetchCustomerTunch(
                                employee?.clientCode.toString(),
                                employee?.id?.toInt()
                            )
                            return@setOnBarcodeScanned    // wait for next recomposition
                        }
                        // 2) Read touch list from StateFlow
                        val touchMatch = touchList.firstOrNull {
                            it.CustomerId == customerId &&
                            it.StockKeepingUnit.equals(selectedSku, ignoreCase = true)
                        }

                        var makingPercent = selectedItem?.MakingPercentage ?: "0.0"
                        var wastagePercent = selectedItem?.WastagePercent ?: "0.0"
                        var makingFixedWastage = selectedItem?.MakingFixedWastage ?: "0.0"
                        var makingFixedAmt = selectedItem?.MakingFixedAmt ?: "0.0"
                        var makingPerGram = selectedItem?.MakingPerGram ?: "0.0"


                        if (touchMatch != null) {
                            Log.d("TouchMatch", "✔ SKU matched in Touch API")

                            makingPercent = touchMatch.MakingPercentage ?: makingPercent
                            wastagePercent = touchMatch.WastageWt ?: wastagePercent
                            makingFixedWastage = touchMatch.MakingFixedWastage ?: makingFixedWastage
                            makingFixedAmt = touchMatch.MakingFixedAmt ?: makingFixedAmt
                            makingPerGram = touchMatch.MakingPerGram ?: makingPerGram
                        } else {
                            Log.d("TouchMatch", "❌ No touch settings found for SKU")
                        }
                        val mp = makingPercent.toDoubleOrNull() ?: 0.0
                        val wp = wastagePercent.toDoubleOrNull() ?: 0.0
                        val mfa = makingFixedAmt.toDoubleOrNull() ?: 0.0
                        val mfw = makingFixedWastage.toDoubleOrNull() ?: 0.0
                        val nw = netWt.toDouble() ?: 0.0

// Making Amount
                        val makingAmt =
                            ((mp / 100.0) * nw) +
                                    mfa +
                                    ((mp / 100.0) * nw) +
                                    mfw

// Fine & Wastage
                        val finePercent = selectedItem?.FinePercent?.toDoubleOrNull() ?: 0.0
                        val fineWt = (finePercent / 100.0) * nw
                        val wastageWt = (wp / 100.0) * nw
                        val totalStoneAmount =
                            selectedItem?.TotalStoneAmount?.toDoubleOrNull() ?: 0.0
                        val diamondAmount =
                            selectedItem?.DiamondPurchaseAmount?.toDoubleOrNull()
                                ?: 0.0
                        val safeMetalAmt = metalAmt
                        val safeMakingAmt = makingAmt
                        val rate = 100*//*dailyRates.find { it.PurityName.equals(selectedItem?.PurityName, ignoreCase = true) }?.Rate?.toDoubleOrNull() ?: 0.0*//*

                        val itemAmt: Double = (selectedItem?.NetWt?.toDoubleOrNull() ?: 0.0) * rate
                        val baseUrl =
                            "https://rrgold.loyalstring.co.in/" // Replace with actual base URL
                        val imageString = selectedItem?.Images.toString()
                        val lastImagePath =
                            imageString.split(",").lastOrNull()?.trim()
                        "$baseUrl$lastImagePath"
                        // If the product doesn't exist in productList, add it and insert into database
                        val newProduct = ChallanDetails(
                            ChallanId = 0,
                            MRP = selectedItem?.MRP ?: "0.0",
                            CategoryName = selectedItem?.CategoryName ?: "",
                            ChallanStatus = "Pending",
                            ProductName = selectedItem?.ProductName ?: "",
                            Quantity = selectedItem?.ClipQuantity ?: "1",
                            HSNCode = "",
                            ItemCode = selectedItem?.ItemCode ?: "",
                            GrossWt = selectedItem?.GrossWt ?: "0.0",
                            NetWt = selectedItem?.NetWt ?: "0.0",
                            ProductId = selectedItem?.ProductId ?: 0,
                            CustomerId = 0,
                            MetalRate = selectedItem?.TodaysRate?.toString() ?: "0.0",
                            MakingCharg = selectedItem?.MakingFixedAmt?.toString() ?: "0.0",
                            Price = itemAmt.toString(),
                            HUIDCode = "",
                            ProductCode = selectedItem?.ProductCode ?: "",
                            ProductNo = "",
                            Size = selectedItem?.Size ?: "",
                            StoneAmount = selectedItem?.TotalStoneAmount ?: "0.0",
                            TotalWt = selectedItem?.TotalWeight?.toString() ?: "0.0",
                            PackingWeight = selectedItem?.PackingWeight?.toString() ?: "0.0",
                            MetalAmount = itemAmt.toString(),
                            OldGoldPurchase = false,
                            RatePerGram = selectedItem?.MakingPerGram?.toString() ?: "0.0",
                            Amount = itemAmt.toString(),
                            ChallanType = "Delivery",
                            FinePercentage = selectedItem?.FinePercent?.toString() ?: "0.0",
                            PurchaseInvoiceNo = "",
                            HallmarkAmount = selectedItem?.HallmarkAmount?.toString() ?: "0.0",
                            HallmarkNo = "",
                            MakingFixedAmt = makingFixedAmt,
                            MakingFixedWastage = makingFixedWastage,
                            MakingPerGram = makingPerGram,
                            MakingPercentage = makingPercent,
                            Description = "",
                            CuttingGrossWt = selectedItem?.GrossWt ?: "0.0",
                            CuttingNetWt = selectedItem?.NetWt ?: "0.0",
                            BaseCurrency = "INR",
                            CategoryId = selectedItem?.CategoryId ?: 0,
                            PurityId = selectedItem?.PurityId ?: 0,
                            TotalStoneWeight = selectedItem?.TotalStoneWeight ?: "0.0",
                            TotalStoneAmount = selectedItem?.TotalStoneAmount ?: "0.0",
                            TotalStonePieces = "0",
                            TotalDiamondWeight = selectedItem?.DiamondWeight ?: "0.0",
                            TotalDiamondPieces = "0",
                            TotalDiamondAmount = selectedItem?.TotalDiamondAmount ?: "0.0",
                            SKUId = selectedItem?.SKUId ?: 0,
                            SKU = selectedItem?.SKU ?: "",
                            FineWastageWt = selectedItem?.WastagePercent?.toString() ?: "0.0",
                            TotalItemAmount = itemAmt.toString(),
                            ItemAmount = itemAmt.toString(),
                            ItemGSTAmount = "0.0",
                            ClientCode = employee?.clientCode ?: "",
                            DiamondSize = "",
                            DiamondWeight = selectedItem?.DiamondWeight ?: "0.0",
                            DiamondPurchaseRate = "0.0",
                            DiamondSellRate = "0.0",
                            DiamondClarity = "",
                            DiamondColour = "",
                            DiamondShape = "",
                            DiamondCut = "",
                            DiamondName = "",
                            DiamondSettingType = "",
                            DiamondCertificate = "",
                            DiamondPieces = "0",
                            DiamondPurchaseAmount = "0.0",
                            DiamondSellAmount = "0.0",
                            DiamondDescription = "",
                            MetalName = selectedItem?.MetalName ?: "Gold",
                            NetAmount = itemAmt.toString(),
                            GSTAmount = "0.0",
                            TotalAmount = itemAmt.toString(),
                            Purity = selectedItem?.PurityName ?: "",
                            DesignName = selectedItem?.DesignName ?: "",
                            CompanyId = 0,
                            BranchId = selectedItem?.BranchId ?: 0,
                            CounterId = selectedItem?.CounterId ?: 0,
                            EmployeeId = employee?.employeeId ?: 0,
                            LabelledStockId = 0,
                            FineSilver = "0.0",
                            FineGold = "0.0",
                            DebitSilver = "0.0",
                            DebitGold = "0.0",
                            BalanceSilver = "0.0",
                            BalanceGold = "0.0",
                            ConvertAmt = "0.0",
                            Pieces = selectedItem?.ClipQuantity ?: "1",
                            StoneLessPercent = "0.0",
                            DesignId = selectedItem?.DesignId ?: 0,
                            PacketId = selectedItem?.PacketId ?: 0,
                            RFIDCode =selectedItem?.RFIDCode ?: "",
                        )

                        productList.add(newProduct)


                    } else {
                        Log.d(
                            "Already Exists",
                            "Product already exists in the list: ${existingProduct.ProductName}"
                        )
                    }

                }
            }else {
                Log.d("No Match", "No item matched with scanned TID")
            }

        }

    }*/
    LaunchedEffect(Unit) {
        viewModel.barcodeReader.openIfNeeded()

        viewModel.barcodeReader.setOnBarcodeScanned { scanned ->

            Log.d("RFID Scan", scanned)
            itemCode = TextFieldValue(scanned)

            val matchedItem = allItems.firstOrNull {
                it.itemCode.equals(scanned, ignoreCase = true)
            }

            if (matchedItem == null) {
                Log.d("RFID Scan", "❌ No match found for $scanned")
                return@setOnBarcodeScanned
            }

            // Prevent duplicates
            if (productList.any { it.tid == matchedItem.tid }) {
                Log.d("RFID Scan", "⚠️ Already exists: ${matchedItem.itemCode}")
                return@setOnBarcodeScanned
            }

            // Save temporarily
            pendingBarcodeItem = matchedItem

            // Fetch Touch API only if empty
            if (touchList.isEmpty()) {
                deliveryChallanViewModel.fetchCustomerTunch(
                    employee?.clientCode.orEmpty(),
                    employee?.id?.toInt() ?: 0
                )
            }
        }
    }

    // Yeh helper ek baar upar composable me rakh sakta hai
    fun safeDouble(value: String?) = value?.toDoubleOrNull() ?: 0.0

    LaunchedEffect(touchList) {

        // 🔹 Barcode scan ke baad set hua item
        val matchedItem = pendingBarcodeItem ?: return@LaunchedEffect

        // print list
        Log.d("TouchList", "size=${touchList.size}")
        touchList.forEachIndexed { index, t ->
            Log.d("TouchList", "[$index] SKU=${t.StockKeepingUnit}, Cust=${t.CustomerId}")
        }

        val selectedSku = matchedItem.sku.orEmpty()

        // 🔹 Touch API se customer + SKU wise match
        val touchMatch = touchList.firstOrNull {
            it.CustomerId == customerId &&
                    it.StockKeepingUnit.equals(selectedSku, ignoreCase = true)
        }

        // defaults from item
        var makingPercent = matchedItem.makingPercent ?: "0.0"
        var makingFixedWastage = matchedItem.fixWastage ?: "0.0"
        var makingFixedAmt = matchedItem.fixMaking ?: "0.0"
        var makingPerGram = matchedItem.makingPerGram ?: "0.0"

        // 🔹 Touch data override
        if (touchMatch != null) {
            Log.d(
                "TouchMatch2", """
✔ Touch API Match:
SKU=${touchMatch.StockKeepingUnit}
MakingPercentage=${touchMatch.MakingPercentage}
WastageWt=${touchMatch.WastageWt}
MakingFixedWastage=${touchMatch.MakingFixedWastage}
MakingFixedAmt=${touchMatch.MakingFixedAmt}
MakingPerGram=${touchMatch.MakingPerGram}
""".trimIndent()
            )

            makingPercent = touchMatch.MakingPercentage ?: makingPercent
            makingFixedWastage = touchMatch.MakingFixedWastage ?: makingFixedWastage
            makingFixedAmt = touchMatch.MakingFixedAmt ?: makingFixedAmt
            makingPerGram = touchMatch.MakingPerGram ?: makingPerGram
        } else {
            Log.d("TouchMatch2", "❌ No touch for this SKU")
        }

        // --- Compute Metal, Making, and Item Amounts ---
        val netWt = safeDouble(matchedItem.netWeight)

        // 🔹 Purity-wise daily rate
        val rate = if (!dailyRates.isNullOrEmpty()) {
            dailyRates
                .firstOrNull { it.PurityName.equals(matchedItem.purity, ignoreCase = true) }
                ?.Rate?.toDoubleOrNull()
                ?: 0.0
        } else {
            0.0
        }

        val makingPerGramFinal = safeDouble(makingPerGram)
        val fixMakingFinal = safeDouble(makingFixedAmt)
        val makingPercentFinal = safeDouble(makingPercent)
        val fixWastageFinal = safeDouble(makingFixedWastage)
        val stoneAmt = safeDouble(matchedItem.stoneAmount)
        val diamondAmt = safeDouble(matchedItem.diamondAmount)

        // Metal Amount = NetWt * Rate
        val metalAmt = netWt * rate

        // Making Amount = (MakingPerGram + FixMaking) + (Making% * NetWt / 100) + FixWastage
        val makingAmt =
            (makingPerGramFinal + fixMakingFinal) +
                    ((makingPercentFinal / 100.0) * netWt) +
                    fixWastageFinal

        // Item Amount = Stone + Diamond + Metal + Making
        val itemAmt = stoneAmt + diamondAmt + metalAmt + makingAmt

        val baseUrl = "https://rrgold.loyalstring.co.in/"
        val imageString = matchedItem.imageUrl.orEmpty()
        val lastImagePath = imageString.split(",").lastOrNull()?.trim()
        val finalImageUrl = if (!lastImagePath.isNullOrBlank()) "$baseUrl$lastImagePath" else ""
        val fixedWastage = (makingFixedWastage?.toDoubleOrNull() ?: 0.0)
        val net = netWt?.toDouble() ?: 0.0
        fun fmt3(v: Double): String = String.format(Locale.getDefault(), "%.3f", v)
        val finePlusWt = fmt3(
            (net * ((0 + fixedWastage) / 100.0)).coerceAtLeast(0.0)
        )
        // 🔹 NOW add product
        val newProduct = ChallanDetails(
            ChallanId = 0,
            MRP = matchedItem.mrp?.toString() ?: "0.0",
            CategoryName = matchedItem.category.orEmpty(),
            ChallanStatus = "Sold",
            ProductName = matchedItem.productName.orEmpty(),
            Quantity = (matchedItem.totalQty ?: matchedItem.pcs ?: 1).toString(),
            HSNCode = "",
            ItemCode = matchedItem.itemCode.orEmpty(),
            GrossWt = matchedItem.grossWeight ?: "0.0",
            NetWt = matchedItem.netWeight ?: "0.0",
            ProductId = matchedItem.productId ?: 0,
            CustomerId = 0,

            // ✅ Rate / Metal / Making / Item
            MetalRate = rate.toString(),
            MakingCharg = makingAmt.toString(),
            MetalAmount = metalAmt.toString(),
            ItemAmount = itemAmt.toString(),
            TotalItemAmount = itemAmt.toString(),
            TotalAmount = itemAmt.toString(),
            Price = itemAmt.toString(),

            HUIDCode = "",
            ProductCode = matchedItem.productCode.orEmpty(),
            ProductNo = "",
            Size = "1",
            StoneAmount = matchedItem.stoneAmount ?: "0.0",
            TotalWt = matchedItem.totalGwt?.toString() ?: "0.0",
            PackingWeight = "0.0",
            OldGoldPurchase = false,
            RatePerGram = makingPerGramFinal.toString(),
            Amount = itemAmt.toString(),
            ChallanType = "Delivery",
            FinePercentage = "0.0",
            PurchaseInvoiceNo = "",
            HallmarkAmount = "0.0",
            HallmarkNo = "",
            MakingFixedAmt = fixMakingFinal.toString(),
            MakingFixedWastage = fixWastageFinal.toString(),
            MakingPerGram = makingPerGramFinal.toString(),
            MakingPercentage = makingPercentFinal.toString(),
            Description = "",
            CuttingGrossWt = matchedItem.grossWeight ?: "0.0",
            CuttingNetWt = matchedItem.netWeight ?: "0.0",
            BaseCurrency = "INR",
            CategoryId = matchedItem.categoryId ?: 0,
            PurityId = 0,
            TotalStoneWeight = matchedItem.totalStoneWt?.toString() ?: "0.0",
            TotalStoneAmount = matchedItem.stoneAmount ?: "0.0",
            TotalStonePieces = "0",
            TotalDiamondWeight = matchedItem.diamondWeight ?: "0.0",
            TotalDiamondPieces = "0",
            TotalDiamondAmount = matchedItem.diamondAmount ?: "0.0",
            SKUId = 0,
            SKU = matchedItem.sku.orEmpty(),
            FineWastageWt = finePlusWt?: "0.0",

            ItemGSTAmount = "0.0",
            ClientCode = employee?.clientCode ?: "",
            DiamondSize = "",
            DiamondWeight = matchedItem.diamondWeight ?: "0.0",
            DiamondPurchaseRate = "0.0",
            DiamondSellRate = "0.0",
            DiamondClarity = "",
            DiamondColour = "",
            DiamondShape = "",
            DiamondCut = "",
            DiamondName = "",
            DiamondSettingType = "",
            DiamondCertificate = "",
            DiamondPieces = "0",
            DiamondPurchaseAmount = "0.0",
            DiamondSellAmount = "0.0",
            DiamondDescription = "",
            MetalName = "",
            NetAmount = itemAmt.toString(),
            GSTAmount = "0.0",

            Purity = matchedItem.purity ?: "",
            DesignName = matchedItem.design ?: "",
            CompanyId = 0,
            BranchId = matchedItem.branchId ?: UserPreferences.getInstance(context).getBranchID()!!.toInt(),
            CounterId = matchedItem.counterId ?: 0,
            EmployeeId = employee?.employeeId ?: 0,
            LabelledStockId = matchedItem.bulkItemId,
            FineSilver = "0.0",
            FineGold = "0.0",
            DebitSilver = "0.0",
            DebitGold = "0.0",
            BalanceSilver = "0.0",
            BalanceGold = "0.0",
            ConvertAmt = "0.0",
            Pieces = matchedItem.pcs?.toString() ?: "1",
            StoneLessPercent = "0.0",
            DesignId = matchedItem.designId ?: 0,
            PacketId = matchedItem.packetId ?: 0,
            RFIDCode = matchedItem.rfid.orEmpty(),
            Image = finalImageUrl,
            DiamondWt = matchedItem.diamondWeight ?: "0.0",
            StoneAmt = matchedItem.stoneAmount ?: "0.0",
            DiamondAmt = matchedItem.diamondAmount ?: "0.0",
            FinePer = "0.0",
            FineWt = "0.0",
            qty = (matchedItem.pcs ?: 1),
            tid = matchedItem.tid ?: "",
            totayRate = rate.toString(),
            makingPercent = makingPercentFinal.toString(),
            fixMaking = fixMakingFinal.toString(),
            fixWastage = fixWastageFinal.toString(),
            Pcs = matchedItem.pcs?:0
        )

        productList.add(newProduct)
        pendingBarcodeItem = null
    }




    Scaffold(
        topBar = {
            GradientTopBar(
                title = localizedContext.getString(R.string.delivery_challan),
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
                        // ✅ 1️⃣ Create the update request object
                       // Log.d("@@","@@"barnchId+"      "+deliveryChallanViewModel.selectedChallan.value!!.CustomerId)

                        Log.d("@@","@@"+deliveryChallanViewModel.selectedChallan.value+"      "+deliveryChallanViewModel.selectedChallan.value!!.CustomerId)
                        val updateRequest = UpdateDeliveryChallanRequest(
                            Id = deliveryChallanViewModel.selectedChallan.value?.Id ?: 0,
                            StatusType = true,
                            CustomerId = deliveryChallanViewModel.selectedChallan.value!!.CustomerId,
                            CustomerName = deliveryChallanViewModel.selectedChallan.value!!.CustomerName.toString(),
                            VendorId = 0,
                            BranchId = productList.get(0).BranchId,
                            TotalAmount = productList.sumOf { it.ItemAmount?.toDoubleOrNull() ?: 0.0 }.toString(),
                            PaymentMode = "Cash",
                            Offer = "0.0",
                            Qty = productList.size.toString(),
                            GST = "3.0",
                            ReceivedAmount = "0.0",
                            ChallanStatus = "Sold",
                            Visibility = "1",
                            MRP = productList.sumOf { it.MRP?.toDoubleOrNull() ?: 0.0 }.toString(),
                            GrossWt = productList.sumOf { it.GrossWt?.toDoubleOrNull() ?: 0.0 }.toString(),
                            NetWt = productList.sumOf { it.NetWt?.toDoubleOrNull() ?: 0.0 }.toString(),
                            StoneWt = productList.sumOf { it.TotalStoneWeight?.toDoubleOrNull() ?: 0.0 }.toString(),
                            TotalNetAmount = productList.sumOf { it.ItemAmount?.toDoubleOrNull() ?: 0.0 }.toString(),
                            TotalGSTAmount = "0.0",
                            TotalPurchaseAmount = "0.0",
                            PurchaseStatus = "N/A",
                            GSTApplied = "true",
                            Discount = "0.0",
                            TotalBalanceMetal = "0.0",
                            BalanceAmount = "0.0",
                            TotalFineMetal = productList.sumOf { it.FineWastageWt?.toDoubleOrNull() ?: 0.0 }.toString(),
                            CourierCharge = "0.0",
                            TDS = "0.0",
                            URDNo = "",
                            gstCheckboxConfirm = "false",
                            AdditionTaxApplied = "false",
                            CategoryId = 0,
                            InvoiceNo = "",
                            DeliveryAddress = "",
                            BillType = "Normal",
                            UrdPurchaseAmt = "0.0",
                            BilledBy = ""?: "System",
                            TotalAdvanceAmount = "0.0",
                            TotalAdvancePaid = "0.0",
                            CreditSilver = "0.0",
                            CreditGold = "0.0",
                            CreditAmount = "0.0",
                            BalanceAmt = "0.0",
                            BalanceSilver = "0.0",
                            BalanceGold = "0.0",
                            TotalSaleGold = "0.0",
                            TotalSaleSilver = "0.0",
                            TotalSaleUrdGold = "0.0",
                            TotalSaleUrdSilver = "0.0",
                            SaleType = "Delivery",
                            FinancialYear = "2025",
                            BaseCurrency = "INR",
                            TotalStoneWeight = productList.sumOf { it.TotalStoneWeight?.toDoubleOrNull() ?: 0.0 }.toString(),
                            TotalStoneAmount = productList.sumOf { it.StoneAmount?.toDoubleOrNull() ?: 0.0 }.toString(),
                            TotalStonePieces ="",
                            TotalDiamondWeight = productList.sumOf { it.DiamondWeight?.toDoubleOrNull() ?: 0.0 }.toString(),
                            TotalDiamondPieces = productList.sumOf { it.DiamondPieces?.toIntOrNull() ?: 0 }.toString(),
                            TotalDiamondAmount = productList.sumOf { it.DiamondSellAmount?.toDoubleOrNull() ?: 0.0 }.toString(),
                            ClientCode = productList.get(0).ClientCode,
                            ChallanNo = challanId.toString(),
                            InvoiceCount = "1",
                            FineSilver = "0.0",
                            FineGold = "0.0",
                            DebitSilver = "0.0",
                            DebitGold = "0.0",
                            TotalPaidMetal = "0.0",
                            TotalPaidAmount = "0.0",
                            UrdWt = "0.0",
                            UrdAmt = "0.0",
                            TransactionAmtType = "CREDIT",
                            TransactionMetalType = "GOLD",
                            Description = "Updated from mobile app",
                            MetalType = "GOLD",
                            ChallanDetails = productList,
                            Payments = emptyList() // You can populate this if you have payment info
                        )

                        // ✅ 2️⃣ Call update API
                        deliveryChallanViewModel.updateDeliveryChallan(updateRequest)
                    } else {

                        val clientCode = employee?.clientCode ?: return@ScanBottomBar
                        val branchId = employee.branchNo ?: UserPreferences.getInstance(context).getBranchID()!!.toInt()

                        // 🔹 Step 1: Fetch last challan no
                        isSaving = true
                        deliveryChallanViewModel.clearlastDeliveryChallanNo()
                        deliveryChallanViewModel.fetchLastChallanNo(clientCode, branchId)
                    }
                },
                onList = { navController.navigate(Screens.DeliveryChallanListScreen.route,)
                  //  navController.navigate(Screens.PrinterScreen.route)
                    },
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
                },
                coroutineScope = coroutineScope,
                fetchSuggestions = { orderViewModel.getAllEmpList(clientCode = employee?.clientCode.toString()) },
                expanded = false,
                onSaveCustomer = { request -> orderViewModel.addEmployee(request) },
                employeeClientCode = employee?.clientCode,
                employeeId = employee?.employeeId?.toString(),
                isEditMode = isEditMode
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
                          //  Bu = item

                            val code = item.itemCode ?: item.rfid ?: ""

                            itemCode = TextFieldValue(code)

                            addItemToList(code)
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
                            showInvoiceDialog = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text =localizedContext.getString(R.string.challan_fields),
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
            DeliveryChallanItemListTable(
                productList = productList,
                onTotalsChange = { base, gst, final ->
                    baseTotal = base
                    gstAmount = gst

                    totalWithGst = final
                },
                onItemUpdated = { index, updated ->
                    // ✅ sirf ek item update
                    productList[index] = updated
                }
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

    // 🧩 Debug log before showing Invoice dialog
    when (salesmanList) {
        is UiState.Success -> {
            val list = (salesmanList as UiState.Success<List<EmployeeList>>).data
            Log.d("SalesmanDebug", "✅ Loaded ${list.size} salesmen:")
            list.take(10).forEachIndexed { index, emp ->
                Log.d(
                    "SalesmanDebug",
                    "[$index] ${emp.FirstName ?: emp.FirstName ?: emp.LastName ?: "Unknown"}"
                )
            }
        }

        is UiState.Loading -> {
            Log.d("SalesmanDebug", "⏳ Salesman list is still loading...")
        }

        is UiState.Error -> {
            Log.e(
                "SalesmanDebug",
                "❌ Failed to load salesmen: ${(salesmanList as UiState.Error).message}"
            )
        }

        else -> {
            Log.d("SalesmanDebug", "ℹ️ Salesman list is in unknown state: $salesmanList")
        }
    }

    // 🔹 Show the dialog when state = true
  /*  if (showInvoiceDialog) {
        InvoiceFieldsDialog(
            onDismiss = { showInvoiceDialog = false },
            onConfirm = {
                // ✅ Handle confirm logic here (save or apply data)
                showInvoiceDialog = false
            },
            branchList = branchList,
            salesmanList = salesmanList
        )*/

    if (showInvoiceDialog && productList.size!=0) {
        InvoiceFieldsDialog(
            selectedItem= productList.get(0),
            onDismiss = { showInvoiceDialog = false },
            branchList = branchList,
            salesmanList = customerSuggestions, // ya jo bhi tu use kar raha hai
            onConfirm = { fields ->
                showInvoiceDialog = false
                invoiceFields = fields

                // 🔥 Yaha sab items pe same values set kar:
                for (i in productList.indices) {

                    val old = productList[i] ?: continue

                    val resolvedBranchId = branchList
                        ?.firstOrNull { it.BranchName == fields?.branchName }
                        ?.Id
                        ?: old.BranchId

                    productList[i] = old.copy(
                        BranchId = resolvedBranchId,
                        FinePer = fields?.fine ?: old.FinePer,
                        fixWastage = fields?.wastage ?: old.fixWastage
                    )
                }

            }
        )
    }


    /*  DeliveryChallanDialogEditAndDisplay(
          selectedItem = selectedOrderItem,
          branchList = branchList,
          salesmanList = salesmanList,
          onDismiss = { showInvoiceDialog = false },
          onSave = { updatedItem ->
              println("✅ Saved Invoice: ${updatedItem.branchName}")
              showInvoiceDialog = false
          }
      )*/

    }

fun buildChallanDetails(
    matchedItem: BulkItem,
    touchMatch: CustomerTunchResponse?,   // use your actual model name
    dailyRates: List<DailyRateResponse>?,
    employee: Employee?,
    context: Context
): ChallanDetails {

    fun safeDouble(v: String?) = v?.toDoubleOrNull() ?: 0.0
     fun fmt3(v: Double): String = String.format(Locale.getDefault(), "%.3f", v)
    // 🔹 Touch overrides (default from item)
    var makingPercent = matchedItem.makingPercent ?: "0.0"
    var makingFixedWastage = matchedItem.fixWastage ?: "0.0"
    var makingFixedAmt = matchedItem.fixMaking ?: "0.0"
    var makingPerGram = matchedItem.makingPerGram ?: "0.0"



    if (touchMatch != null) {
        makingPercent = touchMatch.MakingPercentage ?: makingPercent
        makingFixedWastage = touchMatch.MakingFixedWastage ?: makingFixedWastage
        makingFixedAmt = touchMatch.MakingFixedAmt ?: makingFixedAmt
        makingPerGram = touchMatch.MakingPerGram ?: makingPerGram

    }

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

    return ChallanDetails(
        ChallanId = 0,
        MRP = matchedItem.mrp?.toString() ?: "0.0",
        CategoryName = matchedItem.category.orEmpty(),
        ChallanStatus = "Sold",
        ProductName = matchedItem.productName.orEmpty(),
        Quantity = (matchedItem.totalQty ?: matchedItem.pcs ?: 1).toString(),
        HSNCode = "",
        ItemCode = matchedItem.itemCode.orEmpty(),
        GrossWt = matchedItem.grossWeight ?: "0.0",
        NetWt = matchedItem.netWeight ?: "0.0",
        ProductId = matchedItem.productId ?: 0,
        CustomerId = 0,
        MetalRate = ""?.toString() ?: "0.0",
        MakingCharg = makingAmt.toString() ?: "0.0",
        Price = matchedItem.mrp?.toString() ?: "0.0",
        HUIDCode = "",
        ProductCode = matchedItem.productCode.orEmpty(),
        ProductNo = "",
        Size = "1" ?: "",
        StoneAmount = matchedItem.stoneAmount ?: "0.0",
        TotalWt = matchedItem.totalGwt?.toString() ?: "0.0",
        PackingWeight = "" ?: "0.0",
        MetalAmount =metalAmt?.toString() ?: "0.0",
        OldGoldPurchase = false,
        RatePerGram ="" ?: "0.0",
        Amount =itemAmt?.toString() ?: "0.0",
        ChallanType = "Delivery",
        FinePercentage = ""?: "0.0",
        PurchaseInvoiceNo = "",
        HallmarkAmount = "" ?: "0.0",
        HallmarkNo =""?: "",
        MakingFixedAmt = makingFixedAmt,
        MakingFixedWastage = makingFixedWastage,
        MakingPerGram = makingPerGram,
        MakingPercentage = makingPercent,
        Description = ""?: "",
        CuttingGrossWt = matchedItem.grossWeight ?: "0.0",
        CuttingNetWt = matchedItem.netWeight ?: "0.0",
        BaseCurrency = "INR",
        CategoryId = matchedItem.categoryId ?: 0,
        PurityId = 0?: 0,
        TotalStoneWeight = matchedItem.totalStoneWt?.toString() ?: "0.0",
        TotalStoneAmount = matchedItem.stoneAmount ?: "0.0",
        TotalStonePieces = ""?.toString() ?: "0",
        TotalDiamondWeight = matchedItem.diamondWeight ?: "0.0",
        TotalDiamondPieces ="".toString() ?: "0",
        TotalDiamondAmount = matchedItem.diamondAmount ?: "0.0",
        SKUId =0 ?: 0,
        SKU = matchedItem.sku.orEmpty(),
        FineWastageWt = finePlusWt ?: "0.0",
        TotalItemAmount ="".toString() ?: "0.0",
        ItemAmount = itemAmt.toString() ?: "0.0",
        ItemGSTAmount = "0.0",
        ClientCode = "",
        DiamondSize = "",
        DiamondWeight = "0.0",
        DiamondPurchaseRate = "0.0",
        DiamondSellRate = "0.0",
        DiamondClarity = "",
        DiamondColour = "",
        DiamondShape = "",
        DiamondCut = "",
        DiamondName = "",
        DiamondSettingType = "",
        DiamondCertificate = "",
        DiamondPieces = "0",
        DiamondPurchaseAmount = "0.0",
        DiamondSellAmount = "0.0",
        DiamondDescription = "",
        MetalName = "",
        NetAmount = "0.0",
        GSTAmount = "0.0",
        TotalAmount = itemAmt.toString(),

        Purity = matchedItem.purity ?: "",
        DesignName = matchedItem.design ?: "",
        CompanyId = 0?: 0,
        BranchId = matchedItem.branchId ?:UserPreferences.getInstance(context).getBranchID()!!.toInt(),
        CounterId = matchedItem.counterId ?: 0,
        EmployeeId = 0,
        LabelledStockId = matchedItem.bulkItemId ?: 0,
        FineSilver = "0.0",
        FineGold = "0.0",
        DebitSilver = "0.0",
        DebitGold = "0.0",
        BalanceSilver = "0.0",
        BalanceGold = "0.0",
        ConvertAmt = "0.0",
        Pieces = matchedItem.pcs?.toString() ?: "1",
        StoneLessPercent = "0.0",
        DesignId = matchedItem.designId ?: 0,
        PacketId = matchedItem.packetId ?: 0,
        RFIDCode = matchedItem.rfid.orEmpty(),
        Image = matchedItem.imageUrl.orEmpty(),
        DiamondWt = matchedItem.diamondWeight ?: "0.0",
        StoneAmt = matchedItem.stoneAmount ?: "0.0",
        DiamondAmt = matchedItem.diamondAmount ?: "0.0",
        FinePer ="" ?: "0.0",
        FineWt = "" ?: "0.0",
        qty = (matchedItem.pcs ?: 1),
        tid = matchedItem.tid ?: "",
        totayRate = ""?.toString() ?: "0.0",
        makingPercent = makingPercent,
        fixMaking = makingFixedAmt,
        fixWastage = makingFixedWastage

    )
}

fun ensureBluetoothPermissions(context: Context, onGranted: () -> Unit) {
    val activity = context.findActivity() ?: run {
        Log.e("@BT", "❌ Cannot get Activity from context")
        return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )

        val missing = permissions.any {
            ActivityCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing) {
            ActivityCompat.requestPermissions(activity, permissions, 777)
        } else {
            onGranted()
        }
    } else {
        onGranted()
    }
}



fun resetAllFields(
    onResetCustomerName: (String) -> Unit,
    onResetCustomerId: (Int?) -> Unit,
    onResetSelectedCustomer: (EmployeeList?) -> Unit,
    onResetExpandedCustomer: (Boolean) -> Unit,
    onResetItemCode: (TextFieldValue) -> Unit,
    onResetSelectedItem: (ItemCodeResponse?) -> Unit,
    onResetDropdownItemcode: (Boolean) -> Unit,
    onResetProductList: () -> Unit,
    onResetScanning: (Boolean) -> Unit,
    viewModel: BulkViewModel,
    deliveryChallanViewModel: DeliveryChallanViewModel
) {
    // Clear customer info
    onResetCustomerName("")
    onResetCustomerId(null)
    onResetSelectedCustomer(null)
    onResetExpandedCustomer(false)

    // Clear item entry
    onResetItemCode(TextFieldValue(""))
    onResetSelectedItem(null)
    onResetDropdownItemcode(false)

    // Clear product list
    onResetProductList()

    // Stop scanning and clear scan data
    onResetScanning(false)
    viewModel.resetProductScanResults()
    viewModel.stopBarcodeScanner()

    // Reset challan-related data if needed
   // deliveryChallanViewModel.resetChallanState()

    Log.d("DeliveryChallan", "🧹 All fields reset")
}



fun DeliveryChallanItem.toChallanDetails(): ChallanDetails {
    return ChallanDetails(
        ChallanId = 0,
        MRP = this.mrp ?: "0.0",
        CategoryName = this.categoryName ?: "",
        ChallanStatus = "Sold",
        ProductName = this.productName ?: "",
        Quantity = this.qty ?: "1",
        HSNCode = "",
        ItemCode = this.itemCode ?: "",
        GrossWt = this.grWt ?: "0.0",
        NetWt = this.nWt ?: "0.0",
        ProductId = this.productId,
        CustomerId = 0,
        MetalRate = this.todaysRate ?: "0.0",
        MakingCharg = this.makingFixedAmt ?: "0.0",
        Price = this.itemAmt ?: "0.0",
        HUIDCode = "",
        ProductCode = this.productCode ?: "",
        ProductNo = "",
        Size = this.size ?: "",
        StoneAmount = this.stoneAmt ?: "0.0",
        TotalWt = this.totalWt ?: "0.0",
        PackingWeight = this.packingWt ?: "0.0",
        MetalAmount = this.itemAmt ?: "0.0",
        OldGoldPurchase = false,
        RatePerGram = this.makingPerGram ?: "0.0",
        Amount = this.itemAmt ?: "0.0",
        ChallanType = "Delivery",
        FinePercentage = this.finePer ?: "0.0",
        PurchaseInvoiceNo = "",
        HallmarkAmount = this.hallmarkAmt ?: "0.0",
        HallmarkNo = "",
        MakingFixedAmt = this.makingFixedAmt ?: "0.0",
        MakingFixedWastage = this.makingFixedWastage ?: "0.0",
        MakingPerGram = this.makingPerGram ?: "0.0",
        MakingPercentage = this.makingPercentage ?: "0.0",
        Description = "",
        CuttingGrossWt = this.grWt ?: "0.0",
        CuttingNetWt = this.nWt ?: "0.0",
        BaseCurrency = "INR",
        CategoryId = this.categoryId?:0,
        PurityId = this.purityid,
        TotalStoneWeight = this.stoneWt ?: "0.0",
        TotalStoneAmount = this.stoneAmt ?: "0.0",
        TotalStonePieces = "0",
        TotalDiamondWeight = this.dimondWt ?: "0.0",
        TotalDiamondPieces = "0",
        TotalDiamondAmount = "0.0",
        SKUId = this.skuId,
        SKU = this.sku ?: "",
        FineWastageWt = this.wastage ?: "0.0",
        TotalItemAmount = this.itemAmt ?: "0.0",
        ItemAmount = this.itemAmt ?: "0.0",
        ItemGSTAmount = "0.0",
        ClientCode = "",
        DiamondSize = "",
        DiamondWeight = this.dimondWt ?: "0.0",
        DiamondPurchaseRate = "0.0",
        DiamondSellRate = "0.0",
        DiamondClarity = "",
        DiamondColour = "",
        DiamondShape = "",
        DiamondCut = "",
        DiamondName = "",
        DiamondSettingType = "",
        DiamondCertificate = "",
        DiamondPieces = "0",
        DiamondPurchaseAmount = "0.0",
        DiamondSellAmount = "0.0",
        DiamondDescription = "",
        MetalName = "",
        NetAmount = this.netAmt ?: "0.0",
        GSTAmount = "0.0",
        TotalAmount = this.itemAmt ?: "0.0",
        Purity = this.purity ?: "",
        DesignName = this.designName ?: "",
        CompanyId = this.companyId,
        BranchId = this.branchId.toIntOrNull() ?: 0,
        CounterId = this.counterId,
        EmployeeId = 0,
        LabelledStockId = this.id,
        FineSilver = "0.0",
        FineGold = "0.0",
        DebitSilver = "0.0",
        DebitGold = "0.0",
        BalanceSilver = "0.0",
        BalanceGold = "0.0",
        ConvertAmt = "0.0",
        Pieces = (this.pcs ?: 1).toString(),
        StoneLessPercent = "0.0",
        DesignId = this.designid,
        PacketId = 0,
        RFIDCode = this.rfidCode?:""
    )
}




fun BulkItem.toItemCodeResponse(): ItemCodeResponse {
    return ItemCodeResponse(
        Id = this.bulkItemId ?: 0,
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
        pieces = this.pcs?.toString().orEmpty(),
        TotalWeight = this.totalGwt ?: 0.0,
        MRP = this.mrp?.toString().orEmpty(),
        CounterId = this.counterId ?: 0,
        Stones = emptyList(),
        Diamonds = emptyList(),
        ProductName = this.productName.orEmpty()
    )

}

@Composable
fun getActivity(): Activity? {
    var activity: Activity? = null
    var context = LocalContext.current
    while (context is android.content.ContextWrapper) {
        if (context is Activity) {
            activity = context
            break
        }
        context = context.baseContext
    }
    return activity
}

