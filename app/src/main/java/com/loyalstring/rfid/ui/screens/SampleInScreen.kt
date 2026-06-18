package com.loyalstring.rfid.ui.screens

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Surface
import androidx.compose.ui.res.painterResource

import com.loyalstring.rfid.ui.utils.poppins
import androidx.compose.material3.Text
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight

import android.annotation.SuppressLint
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.sparklepos.models.loginclasses.customerBill.EmployeeList
import com.google.gson.Gson
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.model.sampleOut.IssueItemDto
import com.loyalstring.rfid.data.model.sampleOut.SampleOutCustomer
import com.loyalstring.rfid.data.model.sampleOut.SampleOutFields
import com.loyalstring.rfid.data.model.sampleOut.SampleOutIssueItem
import com.loyalstring.rfid.data.model.sampleOut.SampleOutListResponse
import com.loyalstring.rfid.data.model.sampleOut.SampleOutPrintData
import com.loyalstring.rfid.data.model.sampleOut.SampleOutPrintItem
import com.loyalstring.rfid.data.model.sampleOut.SampleOutUpdateRequest
import com.loyalstring.rfid.data.remote.resource.Resource
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.navigation.Screens
import com.loyalstring.rfid.ui.utils.GradientButtonIcon
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.viewmodel.BulkViewModel
import com.loyalstring.rfid.viewmodel.DeliveryChallanViewModel
import com.loyalstring.rfid.viewmodel.OrderViewModel
import com.loyalstring.rfid.viewmodel.ProductListViewModel
import com.loyalstring.rfid.viewmodel.SampleOutViewModel
import com.loyalstring.rfid.viewmodel.SingleProductViewModel
import com.loyalstring.rfid.viewmodel.UiState
import com.loyalstring.rfid.worker.LocaleHelper
import com.rscja.deviceapi.entity.UHFTAGInfo

@SuppressLint("UnrememberedMutableState")
@Composable
fun SampleInScreen(
    onBack: () -> Unit,
    navController: NavHostController,
    Id: Int? = null,
    SampleOutNo: String? =null
) {



    val viewModel: BulkViewModel = hiltViewModel()
    val orderViewModel: OrderViewModel = hiltViewModel()
    val context = LocalContext.current
    val userPreferences = UserPreferences.getInstance(context)
    val savedLang = userPreferences.getAppLanguage().ifBlank { "en" }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = currentLocales[0]?.language ?: savedLang
    val localizedContext = LocaleHelper.applyLocale(context, currentLang)

    var selectedPower by remember { mutableStateOf(10) }
    var isScanning by remember { mutableStateOf(false) }
    //var showSuccessDialog by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    var firstPress by remember { mutableStateOf(false) }
    var shouldNavigateBack by remember { mutableStateOf(false) }
    val productListViewModel: ProductListViewModel = hiltViewModel()
    val sampleOutViewModel: SampleOutViewModel = hiltViewModel()
    val singleProductViewModel: SingleProductViewModel = hiltViewModel()
    val employee = UserPreferences.getInstance(context).getEmployee(Employee::class.java)
    val dailyRates by orderViewModel.getAllDailyRate.collectAsState()
    // Customer input fields
    var customerName by remember { mutableStateOf("") }
    var customerId by remember { mutableStateOf<Int?>(null) }
    var expandedCustomer by remember { mutableStateOf(false) }
    var selectedCustomer by remember { mutableStateOf<EmployeeList?>(null) }
    var ShowSampleInDailog by remember { mutableStateOf(false) }

    var itemCode by remember { mutableStateOf(TextFieldValue("")) }
    var showDropdownItemcode by remember { mutableStateOf(false) }
    val allItems by productListViewModel.productList.collectAsState(initial = emptyList())
   // var selectedItem by remember { mutableStateOf<ItemCodeResponse?>(null) }
    var selectedItem by remember { mutableStateOf<SampleOutListResponse?>(null) }
    val isLoading by orderViewModel.isItemCodeLoading.collectAsState()

    val branchList = singleProductViewModel.branches
    var sampleOutFields by remember { mutableStateOf<SampleOutFields?>(null) }
    val productList = remember { mutableStateListOf<SampleOutListResponse>() }
    val deliveryChallanViewModel: DeliveryChallanViewModel = hiltViewModel()
    val tags by viewModel.scannedTags.collectAsState()
    val scanTrigger by viewModel.scanTrigger.collectAsState()
    var baseTotal by remember { mutableStateOf(0.0) }
    var gstAmount by remember { mutableStateOf(0.0) }
    var totalWithGst by remember { mutableStateOf(0.0) }
    var scannedCodes by remember { mutableStateOf(setOf<String>()) }
    var manualMatchItem by remember { mutableStateOf<IssueItemDto?>(null) }
    var manualRemoveItem by remember { mutableStateOf<IssueItemDto?>(null) }

    fun normCode(v: String?) =
        v?.trim()
            ?.uppercase()
            ?.replace(" ", "")
            ?.replace("-", "")
            ?: ""

    var isReturnMode by remember { mutableStateOf(false) }
    var selectedReturnCodes by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(employee?.clientCode) {
        val code = employee?.clientCode ?: return@LaunchedEffect
        orderViewModel.getAllEmpList(code)
        orderViewModel.getAllItemCodeList(ClientCodeRequest(code))
        singleProductViewModel.getAllBranches(ClientCodeRequest(code))
        singleProductViewModel.getAllPurity(ClientCodeRequest(code))
        singleProductViewModel.getAllSKU(ClientCodeRequest(code))
        orderViewModel.getDailyRate(ClientCodeRequest(code))
        sampleOutViewModel.loadSampleOut(code, "SampleOut")
    }


    val errorMsg by sampleOutViewModel.error.collectAsState()
    val loading by sampleOutViewModel.loading.collectAsState()

    LaunchedEffect(errorMsg) {
        errorMsg?.let { msg ->
            Toast.makeText(context, "Error: $msg", Toast.LENGTH_SHORT).show()
            sampleOutViewModel.clearError()
        }
    }

    var printData by remember { mutableStateOf<SampleOutPrintData?>(null) }
    var openPdfTrigger by remember { mutableStateOf(false) }
    val customerSuggestions by orderViewModel.empListFlow.collectAsState(UiState.Loading)

    val challanList by sampleOutViewModel.sampleOutList.collectAsState()

    val customerWiseChallanList by derivedStateOf {
        if (customerId == null || customerId == 0) {
            emptyList()
        } else {
            challanList.filter { challan ->
                challan.CustomerId == customerId ||
                        challan.Customer?.Id == customerId
            }
        }
    }

    val updateSampleOut by sampleOutViewModel.updateResult.collectAsState()

    LaunchedEffect(updateSampleOut) {
        val result = updateSampleOut ?: return@LaunchedEffect

        Toast.makeText(context,localizedContext.getString(R.string.samplein_saved_success), Toast.LENGTH_SHORT).show()
        sampleOutViewModel.clearUpdateResult()


        // ✅ 1) Build data for PDF (use your current UI values / productList)
        val sampleNo = result.SampleOutNo ?: SampleOutNo ?: ""   // ✅ apne response field ke hisab se
        val date = productList.firstOrNull()?.Date ?: ""
        val returnDate = productList.firstOrNull()?.ReturnDate ?: ""

        val items: List<SampleOutPrintItem> = productList
            .flatMap { challan ->
                challan.IssueItems.orEmpty().map { issue ->
                    SampleOutPrintItem(
                        itemDetails = listOf(
                            issue.CategoryName,
                            issue.ProductName,
                            issue.DesignName,
                            issue.PurityName,
                            issue.SKU
                        ).filter { !it.isNullOrBlank() }
                            .joinToString(" - "),

                        grossWt = issue.GrossWt ?: "0.000",
                        stoneWt = issue.StoneWeight ?: "0.000",
                        diamondWt = issue.DiamondWeight ?: "0.000",
                        netWt = issue.NetWt ?: "0.000",
                        pieces = issue.Pieces ?: "0",
                        status = /*issue.SampleStatus ?:*/ "SampleIn"
                    )
                }
            }

        printData = SampleOutPrintData(
            companyName =  UserPreferences.getInstance(context).getOrganization().toString(), // or from branch/company api
            customerName = customerName,
            addressCity = result.Customer?.CurrAddTown?.toString().orEmpty(), // map from customer if you have
            contactNo =  result.Customer?.Mobile?.toString().orEmpty(),    // map from customer
            sampleOutNo = sampleNo,
            date = date,
            returnDate = returnDate,
            items = items
        )

        // ✅ 2) trigger open
        openPdfTrigger = true


        sampleOutViewModel.clearAddResult()   // yaha pe emptyList set karo
        viewModel.resetProductScanResults()
        kotlinx.coroutines.delay(500)

        resetAllFields1(   onResetCustomerName = { customerName = it },
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

    LaunchedEffect(openPdfTrigger, printData) {
        if (!openPdfTrigger) return@LaunchedEffect
        val data = printData ?: return@LaunchedEffect

        // generate + open (main thread ok, but heavy work ho to IO me split kar sakte)
        generateSampleInPrintPdf(context, data)

        openPdfTrigger = false
    }


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

    val addCustomerState by orderViewModel.addEmpReposnes.observeAsState()
    LaunchedEffect(addCustomerState) {
        when (val state = addCustomerState) {
            is Resource.Success -> {
                Toast.makeText(
                    context,
                    state.message ?: localizedContext.getString(R.string.customer_added_success),
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


    LaunchedEffect(shouldNavigateBack) {
        if (shouldNavigateBack) {
            kotlinx.coroutines.delay(50)
            onBack()
        }
    }

  /*  onReturnClick = {
        isReturnMode = true
       selectedReturnCodes = emptySet<String>()
    } */

  /*  LaunchedEffect(itemCode.text) {
        val query = itemCode.text.trim()
        if (query.isEmpty()) return@LaunchedEffect

        val challan = customerWiseChallanList.firstOrNull {
            it.SampleOutNo.equals(query, ignoreCase = true)
        }

        if (challan == null) {
            Log.d("ManualEntry", "No challan found for SampleOutNo: $query")
            return@LaunchedEffect
        }

        if (productList.any { it.SampleOutNo.equals(challan.SampleOutNo, ignoreCase = true) }) {
            Log.d("ManualEntry", "Already exists SampleOutNo: ${challan.SampleOutNo}")
            return@LaunchedEffect
        }

        selectedItem = challan
        productList.add(challan)

        Log.d("ManualEntry", "Added by SampleOutNo: ${challan.SampleOutNo}")
    }*/
    LaunchedEffect(tags, productList.size) {
        if (tags.isEmpty()) return@LaunchedEffect
        if (productList.isEmpty()) return@LaunchedEffect

        fun norm(v: String?) = v
            ?.trim()
            ?.uppercase()
            ?.replace(" ", "")
            ?.replace("-", "")
            ?: ""

        val selectedIssues = productList.flatMap { it.IssueItems.orEmpty() }
        if (selectedIssues.isEmpty()) return@LaunchedEffect

        val updatedScanned = scannedCodes.toMutableSet()

        tags.forEach { tagInfo ->
            val scannedCode = norm(tagInfo.getEPC())
            if (scannedCode.isEmpty()) return@forEach

            val matchedIssue = selectedIssues.firstOrNull { issue ->
                val itemCode = norm(issue.ItemCode)
                val rfidCode = norm(issue.RFIDCode)
                val tidNumber = norm(issue.TIDNumber)
                itemCode == scannedCode || rfidCode == scannedCode || tidNumber == scannedCode
            } ?: return@forEach

            updatedScanned.add(norm(matchedIssue.ItemCode))
            updatedScanned.add(norm(matchedIssue.RFIDCode))
            updatedScanned.add(norm(matchedIssue.TIDNumber))
        }

        scannedCodes = updatedScanned.toSet()
    }
 /*   LaunchedEffect(tags, allItems, productList.size) {
        if (tags.isEmpty()) return@LaunchedEffect
        if (productList.isEmpty()) {
            Log.w("RFIDScan", "Please select SampleOutNo first")
            return@LaunchedEffect
        }
        if (allItems.isEmpty()) {
            Log.e("RFIDScan", "allItems EMPTY")
            return@LaunchedEffect
        }

        fun norm(v: String?) = v?.trim()?.uppercase()?.replace(" ", "") ?: ""

        val selectedItemCodes = productList
            .flatMap { it.IssueItems.orEmpty() }
            .mapNotNull { it.ItemCode }
            .map { norm(it) }
            .toSet()

        val updatedScanned = scannedCodes.toMutableSet()

        tags.forEach { tagInfo ->
            val scannedEpc = norm(tagInfo.getEPC())
            if (scannedEpc.isEmpty()) return@forEach

            val matchedItem = allItems.firstOrNull {
                norm(it.epc) == scannedEpc
            } ?: return@forEach

            val scannedItemCodeNorm = norm(matchedItem.itemCode)

            if (scannedItemCodeNorm in selectedItemCodes) {
                updatedScanned.add(scannedItemCodeNorm)

                Log.d("RFIDScan", "Matched selected item: $scannedItemCodeNorm")
            } else {
                Log.d("RFIDScan", "Ignored outside selected SampleOutNo item: $scannedItemCodeNorm")
            }
        }

        scannedCodes = updatedScanned.toSet()
    }*/

   /* LaunchedEffect(tags, challanList, allItems) {

        if (tags.isEmpty()) return@LaunchedEffect
        if (challanList.isEmpty()) {
            Log.e("RFIDScan", "❌ challanList EMPTY")
            return@LaunchedEffect
        }
        if (allItems.isEmpty()) {
            Log.e("RFIDScan", "❌ allItems EMPTY")
            return@LaunchedEffect
        }

        fun norm(v: String?) = v?.trim()?.uppercase()?.replace(" ", "") ?: ""

        // ✅ maintain scanned status codes (ItemCode)
        val updatedScanned = scannedCodes.toMutableSet()

        tags.forEach { tagInfo: UHFTAGInfo ->

            // 1️⃣ scanned EPC normalize
            val scannedEpc = norm(tagInfo.getEPC())
            if (scannedEpc.isEmpty()) return@forEach

            // 2️⃣ find item in allItems by EPC
            val matchedFromAllItems = allItems.firstOrNull { it ->
                norm(it.epc) == scannedEpc
            }

            if (matchedFromAllItems == null) {
                Log.w("RFIDScan", "❌ No item in allItems for EPC: $scannedEpc")
                return@forEach
            }

            // 3️⃣ get ItemCode from allItems match
            val scannedItemCode = matchedFromAllItems.itemCode?.trim()
            if (scannedItemCode.isNullOrEmpty()) {
                Log.w("RFIDScan", "❌ itemCode empty for EPC: $scannedEpc")
                return@forEach
            }

            val scannedItemCodeNorm = scannedItemCode.uppercase()
            updatedScanned.add(scannedItemCodeNorm) // ✅ status green by itemcode

            // 4️⃣ find challan in challanList whose IssueItems contains this itemCode
            val challan = productList.firstOrNull { ch ->
                ch.IssueItems.orEmpty().any { itItem ->
                    itItem.ItemCode?.trim().equals(scannedItemCode, ignoreCase = true)
                }
            }

            if (challan == null) {
                Log.w("RFIDScan", "Scanned item not in selected SampleOut list: $scannedItemCode")
                return@forEach
            }

            if (challan == null) {
                Log.w("RFIDScan", "❌ No challan found for ItemCode=$scannedItemCode (EPC=$scannedEpc)")
                return@forEach
            }

            // 5️⃣ duplicate check ONLY in productList (by SampleOutNo)
       *//*     if (productList.any { it.SampleOutNo.equals(challan.SampleOutNo, ignoreCase = true) }) {
                Log.d("RFIDScan", "⚠️ Duplicate challan skipped: ${challan.SampleOutNo}")
                return@forEach
            }*//*
            selectedItem = challan
            // ✅ add challan once
            productList.add(challan)
            Log.d("RFIDScan", "✅ Added challan=${challan.SampleOutNo} for ItemCode=$scannedItemCode")
        }

        // ✅ update scanned set once at end
        scannedCodes = updatedScanned.toSet()
    }*/
    val selectedSet = selectedReturnCodes // normalized set

// Filter only selected IssueItems (you can build payload from this)
    val selectedRows = productList.flatMap { parent ->
        parent.IssueItems.orEmpty().filter { issue ->
            val code = issue.ItemCode?.trim()?.uppercase()?.replace(" ", "") ?: ""
            code in selectedSet
        }
    }

// ✅ Now save `selectedRows` only
    Log.d("SAVE", "Selected items count = ${selectedRows.size}")




    /*   *//*itemcode*//*
    LaunchedEffect(itemCode.text) {
        val query = itemCode.text.trim()
        if (query.isEmpty()) return@LaunchedEffect

        val matchedItem1 = challanList.firstOrNull {
            it.SampleOutNo.equals(query, ignoreCase = true)
        }

        if (matchedItem1 != null) {
            Log.d("ManualEntry", "Found: ${matchedItem1.SampleOutNo}")

           // Prevent duplicates by RFID
           *//* if (productList.any { it.sam.equals(matchedItem1.SampleOutNo, ignoreCase = true) }) {
                Log.d("ManualEntry", "⚠️ Already exists: ${matchedItem1.itemCode}")
                return@LaunchedEffect
            }*//*

            val matchedItem = allItems.firstOrNull { it.itemCode !=query }


            if(matchedItem!=null) {

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
                    dailyRates
                        .firstOrNull { it.PurityName.equals(matchedItem.purity, ignoreCase = true) }
                        ?.Rate?.toDoubleOrNull()
                        ?: 0.0
                } else {
                    0.0
                }

                val makingPerGramFinal = safeDouble(makingPerGram)
                val fixMaking = safeDouble(makingFixedAmt)
                val makingPercentFinal = safeDouble(makingPercent)
                val fixWastage = safeDouble(makingFixedWastage)
                val stoneAmt = safeDouble(matchedItem.stoneAmount)
                val diamondAmt = safeDouble(matchedItem?.diamondAmount)

                // Metal Amount = NetWt * Rate
                val metalAmt = netWt * rate

                // Making Amount = (MakingPerGram + FixMaking) + (Making% * NetWt / 100) + FixWastage
                val makingAmt =
                    (makingPerGramFinal + fixMaking) + ((makingPercentFinal / 100) * netWt) + fixWastage

                // Item Amount = Stone + Diamond + Metal + Making
                val itemAmt = stoneAmt + diamondAmt + metalAmt + makingAmt

                val productDetail = SampleOutDetails(
                    Id = 0,
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
                    MetalRate = rate.toString(),
                    MakingCharg = makingAmt.toString(),
                    Price = matchedItem.mrp?.toString() ?: "0.0",
                    HUIDCode = "",
                    ProductCode = matchedItem.productCode.orEmpty(),
                    ProductNo = "",
                    Size = "1",
                    StoneAmount = matchedItem.stoneAmount ?: "0.0",
                    TotalWt = matchedItem.totalGwt?.toString() ?: "0.0",
                    PackingWeight = "0.0",
                    MetalAmount = metalAmt.toString(),
                    OldGoldPurchase = false,
                    RatePerGram = rate.toString(),
                    Amount = itemAmt.toString(),
                    ChallanType = "Delivery",
                    FinePercentage = "0.0",
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
                    FineWastageWt = matchedItem.fixWastage ?: "0.0",
                    TotalItemAmount = itemAmt.toString(),
                    ItemAmount = itemAmt.toString(),
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
                    CompanyId = 0,
                    BranchId = matchedItem.branchId ?: 0,
                    CounterId = matchedItem.counterId ?: 0,
                    EmployeeId = 0,
                    LabelledStockId = 0,
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
                    FinePer = "0.0",
                    FineWt = "0.0",
                    qty = (matchedItem.pcs ?: 1),
                    tid = matchedItem.tid ?: "",
                    totayRate = rate.toString(),
                    makingPercent = makingPercent,
                    fixMaking = makingFixedAmt,
                    fixWastage = makingFixedWastage
                )

                productList.add(productDetail)
                Log.d("ManualEntry", "✅ Added ${matchedItem.itemCode}")

                // clear input
                itemCode = TextFieldValue("")
            }
        }
    }

    *//*scan the rfid*//*
    LaunchedEffect(tags, allItems, dailyRates) {
        val updatedScanned = scannedCodes.toMutableSet()
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
            if (productList.any { it.RFIDCode == matchedItem.rfid }) {
                Log.d("RFIDScan", "⚠️ Duplicate RFID skipped: ${matchedItem.rfid}")
                return@forEach
            }

            val matchedRfid = matchedItem.rfid?.trim()
            if (matchedRfid.isNullOrEmpty()) {
                Log.w("RFIDScan", "❌ Matched item RFID empty for EPC: $scannedEpc")
                return@forEach
            }

            // ✅ Status mark (GREEN) — table will check RFIDCode now
            updatedScanned.add(matchedRfid.uppercase())
            scannedCodes = updatedScanned.toSet()

            // 🔹 NO TOUCH / TUNCH LOGIC NOW
            // --- Only use values coming from matchedItem itself ---
            var makingPercent = matchedItem.makingPercent ?: "0.0"
            var wastagePercent = matchedItem.fixWastage ?: "0.0"          // (not used in calc, but kept)
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
            scannedCodes = scannedCodes + (matchedItem.rfid ?: "")

            // --- Build SampleOutDetails ---
            val productDetail = SampleOutDetails(
                Id = 0,
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

                MetalRate = rate.toString(),
                MakingCharg = makingAmt.toString(),
                Price = matchedItem.mrp?.toString() ?: "0.0",
                HUIDCode = "",
                ProductCode = matchedItem.productCode.orEmpty(),
                ProductNo = "",
                Size = "",
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
                FineWastageWt = matchedItem.fixWastage ?: "0.0",
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
                BranchId = matchedItem.branchId ?: 0,
                CounterId = matchedItem.counterId ?: 0,
                EmployeeId = 0,
                LabelledStockId = matchedItem.id ?: 0,
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
                CustomerName = ""
            )

            if (productList.none { it.RFIDCode == productDetail.RFIDCode }) {
                productList.add(productDetail)
                Log.d("RFIDScan", "✅ Added ${productDetail.ItemCode} (${productDetail.RFIDCode})")
            } else {
                Log.d("RFIDScan", "⚠️ Duplicate tag skipped: ${productDetail.RFIDCode}")
            }
        }
    }*/

  /*  fun onScanned(itemCode: String) {
        scannedCodes = scannedCodes + itemCode
    }*/
    Scaffold(
        topBar = {
            GradientTopBar(
                title = localizedContext.getString(R.string.sample_in),
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

                    val sampleOutNoFinal = itemCode.text.ifBlank {
                        productList.firstOrNull()?.SampleOutNo.orEmpty()
                    }
                    fun norm(v: String?) = v?.trim()?.uppercase()?.replace(" ", "") ?: ""
                    fun safeD(v: String?) = v?.toDoubleOrNull() ?: 0.0
                    fun safeI(v: Any?) = when (v) {
                        is Int -> v
                        is String -> v.toIntOrNull() ?: 0
                        else -> 0
                    }

                    // ✅ parent + issue pairs
                    val allPairs: List<Pair<SampleOutListResponse, IssueItemDto>> =
                        productList.flatMap { parent ->
                            parent.IssueItems.orEmpty().map { issue -> parent to issue }
                        }

                    val selectedNorm = selectedReturnCodes.map { norm(it) }.toSet()

                    // ✅ ONLY selected if ReturnMode, else all
                  /*  val pairsToSend = if (isReturnMode) {
                        allPairs.filter { (_, issue) -> norm(issue.ItemCode) in selectedNorm }
                    } else {
                        allPairs
                    }*/

                    val scannedNorm = scannedCodes.map { norm(it) }.toSet()

                    val pairsToSend = allPairs

                    if (isReturnMode && pairsToSend.isEmpty()) {
                        Log.d("SAVE", "❌ No checkbox selected")
                        return@ScanBottomBar
                    }

                    val issuesToSend = pairsToSend.map { it.second }

                    val totalDiamond = issuesToSend.sumOf { safeD(it.DiamondWeight) }.toString()
                    val totalGross  = issuesToSend.sumOf { safeD(it.GrossWt) }.toString()
                    val totalNet    = issuesToSend.sumOf { safeD(it.NetWt) }.toString()

                    // ⚠️ If your IssueItemDto does NOT have StoneWeight/TotalWt, keep 0 and NetWt fallback
                    val totalStone  = issuesToSend.sumOf { safeD(it.StoneWeight ?: "0") }.toString()
                    val totalWt     = issuesToSend.sumOf { safeD(it.TotalWt ?: it.NetWt) }.toString()

                    val first = productList.firstOrNull()
                    Log.d("@@", "selectedItem?.Id" + selectedItem?.Id)


                    val allItemCodes = allPairs
                        .map { (_, issue) -> norm(issue.ItemCode) }
                        .filter { it.isNotBlank() }
                        .toSet()

                    val allItemsMatched =
                        allItemCodes.isNotEmpty() && allItemCodes.all { it in scannedNorm }

                    val mainStatus = if (allItemsMatched) {
                        "SampleIn"
                    } else {
                        "SampleOut"
                    }

                    val request = SampleOutUpdateRequest(
                        Id = selectedItem?.Id ?: productList.firstOrNull()?.Id ?: 0,
                        ClientCode = employee?.clientCode.orEmpty(),
                        BranchId = (employee?.branchNo as? String)?.toIntOrNull() ?: 1,
                        CustomerId = customerId ?: 0,
                        SampleOutNo = sampleOutNoFinal,

                        ReturnDate = first?.ReturnDate ?: "",
                        Description = first?.Description ?: "",
                        Date = first?.Date ?: "",

                        SampleStatus =mainStatus ,
                        Quantity = issuesToSend.size,

                        TotalDiamondWeight = totalDiamond,
                        TotalGrossWt = totalGross,
                        TotalNetWt = totalNet,
                        TotalStoneWeight = totalStone,
                        TotalWt = totalWt,
                       StatusType=true,
                        SampleInDate = getCurrentUtcDateTime(),


                        IssueItems = pairsToSend.map { (parent, issue) ->

                            val itemStatus = if (norm(issue.ItemCode) in scannedNorm) {
                                "SampleIn"
                            } else {
                                "SampleOut"
                            }
                            SampleOutIssueItem(
                                ItemCode = issue.ItemCode,
                                SKU = issue.SKU,
                                SKUId = issue.SKUId ?: 0,
                                CategoryId = issue.CategoryId ?: 0,
                                ProductId = issue.ProductId ?: 0,
                                DesignId = issue.DesignId ?: 0,
                                PurityId = issue.PurityId ?: 0,

                                Quantity = (safeI(issue.Quantity).takeIf { it > 0 } ?: 1),
                                GrossWt = issue.GrossWt,
                                NetWt = issue.NetWt,
                                TotalWt = issue.TotalWt ?: issue.NetWt,

                                FinePercentage = issue.FinePercentage,
                                WastegePercentage = issue.WastegePercentage,
                                StoneWeight = issue.StoneWeight ?: "0.000",
                                DiamondWeight = issue.DiamondWeight ?: "0.000",

                                FineWastageWt = issue.FineWastageWt ?: "0.000",
                                RatePerGram = issue.RatePerGram,
                                MetalAmount = issue.MetalAmount,

                                Description = issue.Description ?: "",
                                SampleStatus = itemStatus,
                                ClientCode = employee?.clientCode.orEmpty(),

                                StoneAmount = issue.StoneAmount ?: "0.00",
                                DiamondAmount = issue.DiamondAmount ?: "0.00",
                                Pieces = issue.Pieces ?: "0",

                                CategoryName = issue.CategoryName ?: "",
                                ProductName = issue.ProductName ?: "",
                                PurityName = issue.PurityName ?: "",
                                DesignName = issue.DesignName ?: "",

                                Id = issue.Id ?: 0,
                                CustomerId = customerId ?: 0,
                                VendorId = 0,
                                BranchId = (employee?.branchNo as? String)?.toIntOrNull() ?: 1,
                                //  val branchId = employee?.branchNo?.toIntOrNull() ?: 1

                                LabelledStockId = issue.LabelledStockId ?: 0,
                                CustomerName = customerName,

                                // ✅ parent challan no
                                SampleOutNo = parent.SampleOutNo ?: SampleOutNo.orEmpty(),

                                SampleInDate = getCurrentUtcDateTime(),
                                CreatedOn = getCurrentUtcDateTime(),
                                Customer = selectedCustomer as SampleOutCustomer?
                            )
                        }
                    )

                    sampleOutViewModel.updateSampleOut(request)
                }

                ,
                onList = { navController.navigate(Screens.SampleInListScreen.route) },
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

                    resetAllFields1(
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
                onCustomerSelected = { customerName = "${it.FirstName.orEmpty()} ${it.LastName.orEmpty()}".trim()
                    customerId = it.Id ?: 0
                    itemCode = TextFieldValue("")
                    productList.clear()
                    selectedItem = null
                  //  scannedCodes = emptySet()
                   // selectedReturnCodes = emptySet()
                                     },
                coroutineScope = coroutineScope,
                fetchSuggestions = {orderViewModel.getAllEmpList(clientCode = employee?.clientCode.toString()) },
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

                    SampleOutInputRowData(
                       /* itemCode = itemCode,
                        onItemCodeChange = { itemCode = it },
                        showDropdown = showDropdownItemcode,
                        setShowDropdown = { showDropdownItemcode = it },
                        context = context,
                       *//* onScanClicked = {   // Start RFID scan when QR icon clicked
                            viewModel.startBarcodeScanning(context)
                        },*//*
                        onClearClicked = { itemCode = TextFieldValue("") },
                        filteredList = customerWiseChallanList,
                        isLoading = isLoading,
                        onItemSelected = { item ->
                            selectedItem = item
                            Log.d("SelectedSampleOut", "Selected Id = ${item.Id}")
                        }*/
                        itemCode = itemCode,
                        onItemCodeChange = { itemCode = it },
                        showDropdown = showDropdownItemcode,
                        setShowDropdown = { showDropdownItemcode = it },
                        context = context,
                        onClearClicked = { itemCode = TextFieldValue("") },
                        filteredList = customerWiseChallanList,
                        isLoading = isLoading,
                       /* onItemSelected = { item ->
                          //  scannedCodes = emptySet()
                           // selectedReturnCodes = emptySet()
                            selectedItem = item
                            itemCode = TextFieldValue(item.SampleOutNo ?: "")
                            Log.d("SelectedSampleOut", "Selected Id = ${item.Id}")
                        }*/

                        onItemSelected = { item ->
                            selectedItem = item
                            itemCode = TextFieldValue(item.SampleOutNo ?: "")
                            showDropdownItemcode = false

                            val alreadyAdded = productList.any {
                                it.SampleOutNo.equals(item.SampleOutNo, ignoreCase = true)
                            }

                            if (!alreadyAdded) {
                                productList.clear() // agar ek hi SampleOutNo chahiye
                                productList.add(item)
                            }

                            Log.d("SelectedSampleOut", "Selected Id = ${item.Id}")
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
                            ShowSampleInDailog = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Sample In",
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



// ✅ Return button click:

            SampleInListTableComponent(
                productList = productList,
                scannedItemCodes = scannedCodes,
                isReturnMode = isReturnMode,
               // selectedReturnItemCodes = selectedReturnCodes,
                onReturnModeChange = { isReturnMode = it },  // ✅

                selectedReturnItemCodes = selectedReturnCodes,
                onSelectedReturnItemCodesChange = { selectedReturnCodes = it }, // ✅
                        // ✅ new
                        onManualMatchClick = { issue ->
                    manualMatchItem = issue
                },
                onManualRemoveClick = { issue ->
                    manualRemoveItem = issue
                }
            )



        }
    }

    val gradient = Brush.horizontalGradient(
        listOf(
            Color(0xFF5231A7),
            Color(0xFFD32940)
        )
    )

    if (manualMatchItem != null) {
        Dialog(
            onDismissRequest = {
                manualMatchItem = null
            }
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(gradient)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.check_circle),
                                contentDescription = "Confirm Match",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "Confirm Match",
                                fontSize = 18.sp,
                                color = Color.White,
                                fontFamily = poppins
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Are you sure you want to add this item in matched list?",
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        GradientButtonIcon(
                            text = "No",
                            onClick = {
                                manualMatchItem = null
                            },
                            icon = painterResource(id = R.drawable.ic_cancel),
                            iconDescription = "No",
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .padding(end = 6.dp)
                        )

                        GradientButtonIcon(
                            text = "Yes",
                            onClick = {
                                val issue = manualMatchItem

                                val codesToAdd = listOf(
                                    normCode(issue?.ItemCode),
                                    normCode(issue?.RFIDCode),
                                    normCode(issue?.TIDNumber)
                                ).filter { it.isNotBlank() }

                                scannedCodes = scannedCodes + codesToAdd
                                manualMatchItem = null

                                Toast.makeText(
                                    context,
                                    "Item added in matched list",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            icon = painterResource(id = R.drawable.check_circle),
                            iconDescription = "Yes",
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .padding(start = 6.dp)
                        )
                    }
                }
            }
        }
    }

    if (manualRemoveItem != null) {
        Dialog(
            onDismissRequest = {
                manualRemoveItem = null
            }
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                tonalElevation = 4.dp,
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(12.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(gradient)
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_cancel),
                                contentDescription = "Remove Match",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "Remove Match",
                                fontSize = 18.sp,
                                color = Color.White,
                                fontFamily = poppins
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Are you sure you want to remove this item from matched list?",
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        GradientButtonIcon(
                            text = "No",
                            onClick = {
                                manualRemoveItem = null
                            },
                            icon = painterResource(id = R.drawable.ic_cancel),
                            iconDescription = "No",
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .padding(end = 6.dp)
                        )

                        GradientButtonIcon(
                            text = "Yes",
                            onClick = {
                                val issue = manualRemoveItem

                                val codesToRemove = listOf(
                                    normCode(issue?.ItemCode),
                                    normCode(issue?.RFIDCode),
                                    normCode(issue?.TIDNumber)
                                ).filter { it.isNotBlank() }.toSet()

                                scannedCodes = scannedCodes.filterNot {
                                    normCode(it) in codesToRemove
                                }.toSet()

                                manualRemoveItem = null

                                Toast.makeText(
                                    context,
                                    "Item removed from matched list",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            icon = painterResource(id = R.drawable.check_circle),
                            iconDescription = "Yes",
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .padding(start = 6.dp)
                        )
                    }
                }
            }
        }
    }
    if (ShowSampleInDailog) {
        SampleInDetailsDailog (
            onDismiss = { ShowSampleInDailog = false },
            //branchList = branchList,
            //salesmanList = customerSuggestions, // ya jo bhi tu use kar raha hai
            onConfirm = { fields ->
                ShowSampleInDailog = false
                //sampleOutFields = fields

                // 🔥 Yaha sab items pe same values set kar:
                for (i in productList.indices) {
                    val old = productList[i]
                    productList[i] = old.copy(
                        Id = old.Id ?: 0,
                        SampleStatus = old.SampleStatus.orEmpty(),
                        SampleOutNo = old.SampleOutNo.orEmpty(),
                        StatusType = old.StatusType ?: false,
                        CreatedOn = old.CreatedOn.orEmpty(),
                        LastUpdated = old.LastUpdated.orEmpty(),
                        CustomerId = old.CustomerId ?: 0,
                        Quantity = old.Quantity ?: 0,
                        TotalWt = old.TotalWt?.takeIf { it.isNotBlank() } ?: "0",
                        PackingWeight = old.PackingWeight?.takeIf { it.isNotBlank() } ?: "0",
                        Status = old.Status?.takeIf { it.isNotBlank() } ?: "SampleIn",
                        TotalGrossWt = old.TotalGrossWt?.takeIf { it.isNotBlank() } ?: "0",
                        TotalNetWt = old.TotalNetWt?.takeIf { it.isNotBlank() } ?: "0",
                        VendorId = old.VendorId ?: 0,
                        FineWastagePercent = old.FineWastagePercent?.takeIf { it.isNotBlank() } ?: "0",
                        FineWastageWt = old.FineWastageWt?.takeIf { it.isNotBlank() } ?: "0",
                        TotalStoneWeight = old.TotalStoneWeight?.takeIf { it.isNotBlank() } ?: "0",
                        TotalDiamondWeight = old.TotalDiamondWeight?.takeIf { it.isNotBlank() } ?: "0",
                        ReturnDate = fields.returnDate.orEmpty(),
                        Description = fields.description.orEmpty(),
                        ClientCode = old.ClientCode.orEmpty(),
                        SampleInDate = old.SampleInDate.orEmpty(),
                        BranchId = old.BranchId ?: 0,
                        IssueItems = old.IssueItems ?: emptyList(),
                        Customer = old.Customer,
                        Date = fields.date.orEmpty()
                    )
                }
            }
        )
    }


}

fun resetAllFields1(
    onResetCustomerName: (String) -> Unit,
    onResetCustomerId: (Int?) -> Unit,
    onResetSelectedCustomer: (EmployeeList?) -> Unit,
    onResetExpandedCustomer: (Boolean) -> Unit,
    onResetItemCode: (TextFieldValue) -> Unit,
    onResetSelectedItem: (SampleOutListResponse?) -> Unit,
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
