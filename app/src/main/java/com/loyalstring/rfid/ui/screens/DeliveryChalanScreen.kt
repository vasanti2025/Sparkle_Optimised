package com.loyalstring.rfid.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
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
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.local.entity.DeliveryChallanItem
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.deliveryChallan.AddDeliveryChallanRequest
import com.loyalstring.rfid.data.model.deliveryChallan.ChallanDetails
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.model.order.ItemCodeResponse
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

@SuppressLint("UnrememberedMutableState")
@Composable
fun DeliveryChalanScreen(
    onBack: () -> Unit,
    navController: NavHostController
) {

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
    // Sample branch/salesman lists (can come from API)
    //val branchList = listOf("Main Branch", "Sub Branch", "Online Branch")
    //val salesmanList = listOf("Rohit", "Priya", "Vikas")

    val branchList = singleProductViewModel.branches
    val salesmanList by orderViewModel.empListFlow.collectAsState()

    LaunchedEffect(employee?.clientCode) {
        val code = employee?.clientCode ?: return@LaunchedEffect
        // No need for withContext here; VM already uses IO
        singleProductViewModel.getAllBranches(ClientCodeRequest(code))
        orderViewModel.getAllEmpList(ClientCodeRequest(code).toString())
    }


    val tags by viewModel.scannedTags.collectAsState()
    val scanTrigger by viewModel.scanTrigger.collectAsState()

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



    LaunchedEffect(shouldNavigateBack) {
        if (shouldNavigateBack) {
            kotlinx.coroutines.delay(50)
            onBack()
        }
    }

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
        when (addCustomerState) {
            is Resource.Success -> {
                Toast.makeText(context, "✅ Customer added successfully!", Toast.LENGTH_SHORT).show()
            }
            is Resource.Error -> {
                Toast.makeText(context, "❌ Failed to add customer!", Toast.LENGTH_SHORT).show()
            }
            else -> { /* Loading or Idle */ }
        }
    }

    LaunchedEffect(tags) {
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
                        MakingFixedAmt = matchedItem.fixMaking ?: "0.0",
                        MakingFixedWastage = matchedItem.fixWastage ?: "0.0",
                        MakingPerGram = matchedItem.makingPerGram ?: "0.0",
                        MakingPercentage = matchedItem.makingPercent ?: "0.0",
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
                        makingPercent = matchedItem.makingPercent ?: "0.0",
                        fixMaking = matchedItem.fixMaking ?: "0.0",
                        fixWastage = matchedItem.fixWastage ?: "0.0"
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
    }

    // 🔹 When last challan number updates → Add the challan
    val lastChallanNo by deliveryChallanViewModel.lastChallanNo.collectAsState()

    LaunchedEffect(lastChallanNo) {
        // Only run when a new value is emitted
        val lastNo = lastChallanNo ?: return@LaunchedEffect
        val newChallanNo = lastNo + 1

        val clientCode = employee?.clientCode ?: return@LaunchedEffect
        val branchId = employee.branchNo ?: 1

        Log.d("DeliveryChallan", "➡️ Adding challan with No: $newChallanNo")

        val request = AddDeliveryChallanRequest(
            BranchId = branchId,
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
            InvoiceStatus = "Pending",
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
            Qty = productList.sumOf { it.qty?.toDouble() ?: 0.0 }.toString(),
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
            TotalGSTAmount = "0.0"
        )

        deliveryChallanViewModel.addDeliveryChallan(request)
    }


// 🔹 Show success message when challan added
    val addChallanResponse by deliveryChallanViewModel.addChallanResponse.collectAsState()

    LaunchedEffect(addChallanResponse) {
        addChallanResponse?.let { response ->
            Toast.makeText(
                context,
               "✅ Challan saved successfully",
                Toast.LENGTH_SHORT
            ).show()

            viewModel.resetProductScanResults()

            // Optional: clear after short delay so toast doesn’t miss it
            kotlinx.coroutines.delay(500)
            //deliveryChallanViewModel.clearAddChallanResponse()
        }
    }

// 🔹 Handle error messages
    LaunchedEffect(deliveryChallanViewModel.error) {
        val errMsg = deliveryChallanViewModel.error.value
        if (!errMsg.isNullOrEmpty()) {
            Toast.makeText(context, "❌ $errMsg", Toast.LENGTH_SHORT).show()
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

    // ✅ This is your barcode scanner logic
    LaunchedEffect(Unit) {

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
                        val rate = 100/*dailyRates.find { it.PurityName.equals(selectedItem?.PurityName, ignoreCase = true) }?.Rate?.toDoubleOrNull() ?: 0.0*/

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
                            MakingFixedAmt = selectedItem?.MakingFixedAmt?.toString() ?: "0.0",
                            MakingFixedWastage = selectedItem?.MakingFixedWastage?.toString() ?: "0.0",
                            MakingPerGram = selectedItem?.MakingPerGram?.toString() ?: "0.0",
                            MakingPercentage = selectedItem?.MakingPercentage?.toString() ?: "0.0",
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

    }
    Scaffold(
        topBar = {
            GradientTopBar(
                title = "Delivery Chalan",
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
                }
            )
        },
        bottomBar = {


            ScanBottomBar(
                onSave = {

                    val clientCode = employee?.clientCode ?: return@ScanBottomBar
                    val branchId = employee.branchNo ?: 1

                    // 🔹 Step 1: Fetch last challan no
                    deliveryChallanViewModel.fetchLastChallanNo(clientCode, branchId)
                },
                onList = { navController.navigate(Screens.DeliveryChallanListScreen.route) },
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
                isEditMode = isEditMode

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
                employeeId = employee?.employeeId?.toString()
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
                    ItemCodeInputRowData(
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
                        onItemSelected = { selectedItem = it }
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
                            text = "Invoice Fields",
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
            DeliveryChallanItemListTable(productList = productList)
            Spacer(modifier = Modifier.height(6.dp))

            DeliveryChallanSummaryRow(
                gstPercent = 3.0,
                totalAmount = 50000.0,
                        onGstCheckedChange = { isChecked ->
                    println("GST Checkbox changed: $isChecked")
                }
            )

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
    if (showInvoiceDialog) {
        /*InvoiceFieldsDialog(
            onDismiss = { showInvoiceDialog = false },
            onConfirm = {
                // ✅ Handle confirm logic here (save or apply data)
                showInvoiceDialog = false
            },
            branchList = branchList,
            salesmanList = salesmanList
        )
*/
        DeliveryChallanDialogEditAndDisplay(
            selectedItem = selectedOrderItem,
            branchList = branchList,
            salesmanList = salesmanList,
            onDismiss = { showInvoiceDialog = false },
            onSave = { updatedItem ->
                println("✅ Saved Invoice: ${updatedItem.branchName}")
                showInvoiceDialog = false
            }
        )
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
        ChallanStatus = "Pending",
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
        Pieces = this.qty ?: "1",
        StoneLessPercent = "0.0",
        DesignId = this.designid,
        PacketId = 0,
        RFIDCode = this.rfidCode?:""
    )
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

