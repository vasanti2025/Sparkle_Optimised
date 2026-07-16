package com.loyalstring.rfid.ui.screens

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.sparklepos.models.loginclasses.customerBill.EmployeeList
import com.google.gson.Gson
import com.loyalstring.rfid.MainActivity
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.model.order.ItemCodeResponse
import com.loyalstring.rfid.data.model.quotation.AddQuotationRequest
import com.loyalstring.rfid.data.model.quotation.QuotationItem
import com.loyalstring.rfid.data.model.quotation.QuotationPrintData
import com.loyalstring.rfid.data.model.quotation.QuotationPrintItem
import com.loyalstring.rfid.data.model.quotation.UpdateQuotationRequest
import com.loyalstring.rfid.data.model.sampleOut.SampleOutFields
import com.loyalstring.rfid.data.reader.ScanKeyListener
import com.loyalstring.rfid.data.remote.resource.Resource
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.navigation.Screens
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.calcQuotationFromItemFields
import com.loyalstring.rfid.ui.utils.fineWastageWtFromPercent
import com.loyalstring.rfid.ui.utils.isBulkItemAlreadyInQuotationList
import com.loyalstring.rfid.ui.utils.quotationPrintWastagePercent
import com.loyalstring.rfid.ui.utils.resolveWastagePercentForCalc
import com.loyalstring.rfid.ui.utils.toAddQuotationApiItem
import com.loyalstring.rfid.ui.utils.stopBulkScan
import com.loyalstring.rfid.ui.utils.toggleBulkScan
import com.loyalstring.rfid.ui.utils.resolveProductImageUrl
import com.loyalstring.rfid.viewmodel.BulkViewModel
import com.loyalstring.rfid.viewmodel.DeliveryChallanViewModel
import com.loyalstring.rfid.viewmodel.OrderViewModel
import com.loyalstring.rfid.viewmodel.ProductListViewModel
import com.loyalstring.rfid.viewmodel.QuotationViewModel
import com.loyalstring.rfid.viewmodel.SingleProductViewModel
import com.loyalstring.rfid.viewmodel.UiState
import com.loyalstring.rfid.worker.LocaleHelper
import androidx.compose.runtime.rememberUpdatedState
import com.rscja.deviceapi.entity.UHFTAGInfo
import java.util.Locale

private fun QuotationItem.toUiQuotationItem(): QuotationItem {
    val safeQty = qty ?: (Quantity?.toIntOrNull() ?: Pieces?.toIntOrNull() ?: 1)
    return copy(
        qty = safeQty,
        MetalRate = MetalRate ?: RatePerGram ?: totayRate ?: "0.0",
        totayRate = totayRate ?: MetalRate ?: RatePerGram ?: "0.0",
        makingPercent = makingPercent ?: MakingPercentage ?: "0.0",
        fixMaking = fixMaking ?: MakingFixedAmt ?: "0.0",
        fixWastage = fixWastage ?: MakingFixedWastage ?: "0.0",
        StoneAmt = StoneAmt ?: StoneAmount ?: TotalStoneAmount ?: "0.0",
        DiamondWt = DiamondWt ?: TotalDiamondWeight ?: DiamondWeight ?: "0.0",
        DiamondAmt = DiamondAmt ?: TotalDiamondAmount ?: DiamondSellAmount ?: "0.0",
        FinePer = FinePer ?: FinePercentage ?: "0.0"
    )
}


@SuppressLint("UnrememberedMutableState")
@Composable
fun QuotationScreen(
    onBack: () -> Unit,
    navController: NavHostController,
    Id: Int? = null,
    QuotationNo: String? =null
) {
    val viewModel: BulkViewModel = hiltViewModel()
    val orderViewModel: OrderViewModel = hiltViewModel()
    val context = LocalContext.current
    var selectedPower by remember { mutableStateOf(10) }
    var isScanning by remember { mutableStateOf(false) }
    //var showSuccessDialog by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    var firstPress by remember { mutableStateOf(false) }
    var shouldNavigateBack by remember { mutableStateOf(false) }
    val productListViewModel: ProductListViewModel = hiltViewModel()
    val quotationViewModel: QuotationViewModel = hiltViewModel()
    val singleProductViewModel: SingleProductViewModel = hiltViewModel()
    val employee = UserPreferences.getInstance(context).getEmployee(Employee::class.java)
    val dailyRates by orderViewModel.getAllDailyRate.collectAsState()
    // Customer input fields
    var customerName by remember { mutableStateOf("") }
    var customerId by remember { mutableStateOf<Int?>(null) }
    var expandedCustomer by remember { mutableStateOf(false) }
    var selectedCustomer by remember { mutableStateOf<EmployeeList?>(null) }
    var showInvoiceDialog by remember { mutableStateOf(false) }

    var itemCode by remember { mutableStateOf(TextFieldValue("")) }
    var showDropdownItemcode by remember { mutableStateOf(false) }
    val allItems by productListViewModel.productList.collectAsState(initial = emptyList())
    var selectedItem by remember { mutableStateOf<ItemCodeResponse?>(null) }
    val isLoading by orderViewModel.isItemCodeLoading.collectAsState()

    val branchList = singleProductViewModel.branches
    var sampleOutFields by remember { mutableStateOf<SampleOutFields?>(null) }
    val productList = remember { mutableStateListOf<QuotationItem>() }
    val deliveryChallanViewModel: DeliveryChallanViewModel = hiltViewModel()
    val tags by viewModel.scannedTags.collectAsState()
    val scanTrigger by viewModel.scanTrigger.collectAsState()
    var baseTotal by remember { mutableStateOf(0.0) }
    var gstAmount by remember { mutableStateOf(0.0) }
    var totalWithGst by remember { mutableStateOf(0.0) }

    var pendingMatchedItem by remember { mutableStateOf<BulkItem?>(null) }
    var isSaveClicked by remember { mutableStateOf(false) }
    var editPrefilled by remember(Id, QuotationNo) { mutableStateOf(false) }

    val quotationError by quotationViewModel.error.collectAsState()
    LaunchedEffect(quotationError) {
        quotationError?.let { msg ->
            Toast.makeText(context, "Error: $msg", Toast.LENGTH_SHORT).show()
            quotationViewModel.clearError()
        }
    }

    LaunchedEffect(employee?.clientCode) {
        val code = employee?.clientCode ?: return@LaunchedEffect
        orderViewModel.getAllEmpList(code)
        orderViewModel.getAllItemCodeList(ClientCodeRequest(code))
        singleProductViewModel.getAllBranches(ClientCodeRequest(code))
        singleProductViewModel.getAllPurity(ClientCodeRequest(code))
        singleProductViewModel.getAllSKU(ClientCodeRequest(code))
        orderViewModel.getDailyRate(ClientCodeRequest(code))
    }

    val quotationList by quotationViewModel.quotationList.collectAsState()

    LaunchedEffect(Id, QuotationNo, quotationList, editPrefilled) {
        val editId = Id ?: 0
        val editNo = QuotationNo?.trim()
        val shouldEdit = (editId != 0) || (!editNo.isNullOrBlank())
        if (!shouldEdit || editPrefilled) return@LaunchedEffect

        val clientCode = employee?.clientCode.orEmpty()
        if (clientCode.isBlank()) return@LaunchedEffect

        val selected = when {
            editId != 0 -> quotationList.firstOrNull { it.id == editId }
            !editNo.isNullOrBlank() -> quotationList.firstOrNull { it.quotationNo == editNo }
            else -> null
        }

        if (selected == null) {
            if (quotationList.isEmpty()) {
                quotationViewModel.loadQuotationList(
                    clientCode = clientCode,
                    branchId = employee?.defaultBranchId
                )
            }
            return@LaunchedEffect
        }

        isEditMode = true
        quotationViewModel.setSelectedQuotation(selected)
        customerName = selected.customer?.FirstName.orEmpty().trim()
        customerId = selected.customerId
        productList.clear()
        productList.addAll(
            selected.quotationItem
                ?.filterNotNull()
                ?.map { it.toUiQuotationItem() }
                ?: emptyList()
        )
        editPrefilled = true
    }

    val userPreferences = UserPreferences.getInstance(context)
    val savedLang = userPreferences.getAppLanguage().ifBlank { "en" }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = currentLocales[0]?.language ?: savedLang
    val localizedContext = LocaleHelper.applyLocale(context, currentLang)


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
                "scan" -> toggleBulkScan(viewModel, selectedPower) { isScanning = it }
                "barcode" -> viewModel.startBarcodeScanning(context)
            }
            viewModel.clearScanTrigger()
        }
    }


    LaunchedEffect(shouldNavigateBack) {
        if (shouldNavigateBack) {
            kotlinx.coroutines.delay(50)
            onBack()
        }
    }
    val activity = LocalContext.current as? MainActivity
    DisposableEffect(Unit) {
        val listener = object : ScanKeyListener {
            override fun onBarcodeKeyPressed() {


                viewModel.startBarcodeScanning(context)
            }

            override fun onRfidKeyPressed() {
                toggleBulkScan(viewModel, selectedPower) { isScanning = it }
            }
        }
        activity?.registerScanKeyListener(listener)

        onDispose {
            activity?.unregisterScanKeyListener()
            stopBulkScan(viewModel) { isScanning = it }
        }
    }


    val addCustomerState by orderViewModel.addEmpReposnes.observeAsState()
    LaunchedEffect(addCustomerState) {
        when (val state = addCustomerState) {
            is Resource.Success -> {
                Toast.makeText(
                    context,
                    state.message ?:localizedContext.getString(R.string.msg_customer_added),
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

    /*itemcode*/
/*    LaunchedEffect(itemCode.text) {
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
            if (productList.any { it.ItemCode.equals(matchedItem.itemCode, ignoreCase = true) }) {
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

            val productDetail = QuotationItem(
              //  id = 0,
                MRP = matchedItem.mrp?.toString() ?: "0.0",
                CategoryName = matchedItem.category.orEmpty(),
                //ChallanStatus = "Pending",
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
             //   ChallanType = "Delivery",
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
                itemAmt = itemAmt.toString(),
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
              //  DiamondName = "",
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
    }*/

    LaunchedEffect(pendingMatchedItem) {

        val matchedItem = pendingMatchedItem ?: return@LaunchedEffect

      /*  val touchMatch = touchList.firstOrNull {
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



       /* if (touchMatch != null) {
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
        val wastagePercent = resolveWastagePercentForCalc(makingFixedWastage)

        val amounts = calcQuotationFromItemFields(
            netWt = netWt,
            ratePerGram = rate,
            wastageRaw = makingFixedWastage,
            stoneAmt = stoneAmt,
            diamondAmt = diamondAmt
        )
        val metalAmt = amounts.metalAmt
        val makingAmt = amounts.makingAmt
        val itemAmt = amounts.itemAmt

        val fixedWastage = wastagePercent
        val net = netWt

        val finePlusWt = fmt3(
            (net * (fixedWastage / 100.0)).coerceAtLeast(0.0)
        )


            val productDetail = QuotationItem(
                //  id = 0,
                MRP = matchedItem.mrp?.toString() ?: "0.0",
        CategoryName = matchedItem.category.orEmpty(),
        //ChallanStatus = "Pending",
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
        //   ChallanType = "Delivery",
        FinePercentage = "0.0",
        PurchaseInvoiceNo = "",
        HallmarkAmount = "0.0",
        HallmarkNo = "",
        MakingFixedAmt = makingFixedAmt,
        MakingFixedWastage = wastagePercent.toString(),
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
        FineWastageWt = fineWastageWtFromPercent(wastagePercent),
        TotalItemAmount = itemAmt.toString(),
        itemAmt = itemAmt.toString(),
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
        //  DiamondName = "",
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
        fixWastage = wastagePercent.toString()
        )


        val alreadyExists = isBulkItemAlreadyInQuotationList(productList, matchedItem)

        if (!alreadyExists) {
            productList.add(productDetail)
        } else {
            Toast.makeText(context, "Item already added", Toast.LENGTH_SHORT).show()
        }


        pendingMatchedItem = null
    }

    val processedTagEpcs = remember { mutableSetOf<String>() }
    val latestAllItems by rememberUpdatedState(allItems)
    val latestDailyRates by rememberUpdatedState(dailyRates)

    /*scan the rfid*/
    LaunchedEffect(tags, allItems, dailyRates) {

        if (tags.isEmpty()) return@LaunchedEffect
        if (allItems.isEmpty()) {
            Log.e("RFIDScan", "❌ allItems EMPTY when tags = $tags")
            return@LaunchedEffect
        }

        Log.d("RFIDScan", "📦 ${tags.size} tags received")

        fun safeDouble(v: String?) = v?.toDoubleOrNull() ?: 0.0

        tags.forEach { tagInfo: UHFTAGInfo ->

            // 1️⃣ EPC normalize
            val scannedEpc = tagInfo.getEPC()
                ?.trim()
                ?.uppercase()
                ?.replace(" ", "")
                ?: ""

            if (scannedEpc.isBlank() || scannedEpc in processedTagEpcs) {
                return@forEach
            }

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
            if (isBulkItemAlreadyInQuotationList(productList, matchedItem)) {
                Log.d("RFIDScan", "⚠️ Duplicate RFID skipped: ${matchedItem.tid}")
                return@forEach
            }

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

            val fixWastage = resolveWastagePercentForCalc(makingFixedWastage)
            val stoneAmt = safeDouble(matchedItem.stoneAmount)
            val diamondAmt = safeDouble(matchedItem.diamondAmount)

            val amounts = calcQuotationFromItemFields(
                netWt = netWt,
                ratePerGram = rate,
                wastageRaw = makingFixedWastage,
                stoneAmt = stoneAmt,
                diamondAmt = diamondAmt
            )
            val metalAmt = amounts.metalAmt
            val makingAmt = amounts.makingAmt
            val itemAmt = amounts.itemAmt

            // 4. FineWt = NetWt * Fine% (yahi field use kar raha hun)
            val finePercent = safeDouble(matchedItem.makingPercent)
            val fineWt = netWt * finePercent / 100.0

            // --- Build SampleOutDetails ---
            val productDetail = QuotationItem(
               // Id = 0,
                MRP = matchedItem.mrp?.toString() ?: "0.0",
                CategoryName = matchedItem.category.orEmpty(),
                //ChallanStatus = "Pending",
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
               // ChallanType = "Delivery",
                FinePercentage = finePercent.toString(),
                PurchaseInvoiceNo = "",
                HallmarkAmount = "0.0",
                HallmarkNo = "",
                MakingFixedAmt = makingFixedAmt,
                MakingFixedWastage = fixWastage.toString(),
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
                FineWastageWt = fineWastageWtFromPercent(fixWastage),
                TotalItemAmount = itemAmt.toString(),
                itemAmt = itemAmt.toString(),
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
                //DiamondName = "",
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
                fixWastage = fixWastage.toString(),
               // TIDNumber = matchedItem.tid ?: "",
                //CustomerName = ""
            )

            if (productList.none { it.ItemCode == productDetail.ItemCode }) {
                productList.add(productDetail)
                processedTagEpcs.add(scannedEpc)
                Log.d("RFIDScan", "✅ Added ${productDetail.ItemCode} (${productDetail.RFIDCode})")
            } else {
                Log.d("RFIDScan", "⚠️ Duplicate tag skipped: ${productDetail.RFIDCode}")
            }
        }
    }
    fun normalize(value: String?): String =
        value
            ?.trim()
            ?.uppercase()
            ?.replace(" ", "")
            ?.replace("\n", "")
            ?.replace("\r", "")
            ?: ""

    /*scan bar code */
    LaunchedEffect(Unit) {
        viewModel.barcodeReader.openIfNeeded()

        fun safeDouble(v: String?) = v?.toDoubleOrNull() ?: 0.0

        fun sameCode(a: String?, b: String?): Boolean {
            val x = normalize(a)
            val y = normalize(b)
            return x.isNotBlank() && y.isNotBlank() && x == y
        }

        viewModel.barcodeReader.setOnBarcodeScanned { scannedRaw ->
            val scanned = normalize(scannedRaw)
            val currentItems = latestAllItems
            val rates = latestDailyRates
            itemCode = TextFieldValue(scanned)

            val matchedItem = currentItems.firstOrNull { item ->
                val candidates = listOf(
                    normalize(item.itemCode),
                    normalize(item.rfid),
                    normalize(item.productCode),
                    normalize(item.tid)
                )

                candidates.any { code ->
                    code.isNotBlank() &&
                            (code == scanned || code.contains(scanned) || scanned.contains(code))
                }
            }

            if (matchedItem == null) {
                Log.d("RFID Scan", "❌ No match found: $scannedRaw")
                Toast.makeText(context, "Item not found", Toast.LENGTH_SHORT).show()
                return@setOnBarcodeScanned
            }

            val alreadyExists = isBulkItemAlreadyInQuotationList(productList, matchedItem)

            if (alreadyExists) {
                Log.d("RFID Scan", "⚠️ Already exists: ${matchedItem.itemCode}")
                Toast.makeText(context, "Item already exists: ${matchedItem.itemCode}", Toast.LENGTH_SHORT).show()
                return@setOnBarcodeScanned
            }

            val makingPercent = matchedItem.makingPercent ?: "0.0"
            val makingFixedWastage = matchedItem.fixWastage ?: "0.0"
            val makingFixedAmt = matchedItem.fixMaking ?: "0.0"
            val makingPerGram = matchedItem.makingPerGram ?: "0.0"

            val netWt = safeDouble(matchedItem.netWeight)

            val rate = if (rates.isNotEmpty()) {
                rates
                    .firstOrNull { it.PurityName.equals(matchedItem.purity, ignoreCase = true) }
                    ?.Rate?.toDoubleOrNull() ?: 0.0
            } else 0.0

            val makingPerGramFinal = safeDouble(makingPerGram)
            val fixMakingFinal = safeDouble(makingFixedAmt)
            val makingPercentFinal = safeDouble(makingPercent)
            val fixWastageFinal = resolveWastagePercentForCalc(makingFixedWastage)
            val stoneAmt = safeDouble(matchedItem.stoneAmount)
            val diamondAmt = safeDouble(matchedItem.diamondAmount)

            val amounts = calcQuotationFromItemFields(
                netWt = netWt,
                ratePerGram = rate,
                wastageRaw = makingFixedWastage,
                stoneAmt = stoneAmt,
                diamondAmt = diamondAmt
            )
            val metalAmt = amounts.metalAmt
            val makingAmt = amounts.makingAmt
            val itemAmt = amounts.itemAmt

            val finalImageUrl = resolveProductImageUrl(matchedItem.imageUrl).orEmpty()

            val newProduct = QuotationItem(
                MRP = matchedItem.mrp?.toString() ?: "0.0",
                CategoryName = matchedItem.category.orEmpty(),
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
                MetalAmount = metalAmt.toString(),
                itemAmt = itemAmt.toString(),
                TotalItemAmount = itemAmt.toString(),
                TotalAmount = itemAmt.toString(),
                Price = itemAmt.toString(),
                ProductCode = matchedItem.productCode.orEmpty(),
                Size = "1",
                StoneAmount = matchedItem.stoneAmount ?: "0.0",
                TotalWt = matchedItem.totalGwt?.toString() ?: "0.0",
                PackingWeight = "0.0",
                OldGoldPurchase = false,
                RatePerGram = rate.toString(),
                Amount = itemAmt.toString(),
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
                FineWastageWt = fineWastageWtFromPercent(fixWastageFinal),
                ItemGSTAmount = "0.0",
                ClientCode = employee?.clientCode ?: "",
                DiamondWeight = matchedItem.diamondWeight ?: "0.0",
                DiamondPieces = "0",
                DiamondPurchaseAmount = "0.0",
                DiamondSellAmount = "0.0",
                NetAmount = itemAmt.toString(),
                GSTAmount = "0.0",
                Purity = matchedItem.purity ?: "",
                DesignName = matchedItem.design ?: "",
                CompanyId = 0,
                BranchId = matchedItem.branchId ?: 0,
                CounterId = matchedItem.counterId ?: 0,
                EmployeeId = employee?.employeeId ?: 0,
                LabelledStockId = 0,
                Pieces = matchedItem.pcs?.toString() ?: "1",
                DesignId = matchedItem.designId ?: 0,
                PacketId = matchedItem.packetId ?: 0,
                RFIDCode = matchedItem.rfid.orEmpty(),
                Image = finalImageUrl,
                DiamondWt = matchedItem.diamondWeight ?: "0.0",
                StoneAmt = matchedItem.stoneAmount ?: "0.0",
                DiamondAmt = matchedItem.diamondAmount ?: "0.0",
                FinePer = "0.0",
                FineWt = "0.0",
                qty = matchedItem.pcs ?: 1,
                tid = matchedItem.tid ?: "",
                totayRate = rate.toString(),
                makingPercent = makingPercentFinal.toString(),
                fixMaking = fixMakingFinal.toString(),
                fixWastage = fixWastageFinal.toString()
            )

            productList.add(newProduct)
           // showToast("Item added: ${newProduct.ItemCode}")
        }
    }

    val lastQuotationNo by quotationViewModel.lastQuotationNo.collectAsState()

    LaunchedEffect(lastQuotationNo) {
        if (!isSaveClicked) return@LaunchedEffect

        val lastNo = lastQuotationNo ?: return@LaunchedEffect
        Log.e("SampleOut", "lastNo"+lastNo)

        val clientCode = employee?.clientCode.orEmpty()
        val branchId = userPreferences.getBranchID()?.takeIf { it > 0 }
            ?: employee?.defaultBranchId?.takeIf { it > 0 }
            ?: 0
        val custId = customerId ?: 0

        // ❌ 1) Client code missing → add API mat call karo
        if (clientCode.isBlank()) {
            Log.e("SampleOut", "ClientCode missing")
            Toast.makeText(
                context,
                localizedContext.getString(R.string.msg_client_code_missing),
                Toast.LENGTH_SHORT
            ).show()
            isSaveClicked = false
            quotationViewModel.clearLastQuotationNo()
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
            isSaveClicked = false
            quotationViewModel.clearLastQuotationNo()
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
            isSaveClicked = false
            quotationViewModel.clearLastQuotationNo()
            return@LaunchedEffect
        }

        // ✅ Sab validation pass → abhi hi number generate karo + API call
        val lastNoStr = lastQuotationNo ?: return@LaunchedEffect


        val lastNoInt = lastNo.lastQuotationNo?.toIntOrNull() ?: 0
        val newLastQuotationNo = lastNoInt + 1

        Log.d("@@", "lastNoInt = $lastNoInt")
        Log.d("@@", "newLastQuotationNo = $newLastQuotationNo")

        val request = AddQuotationRequest(
            ClientCode = clientCode,
            BranchId = branchId,
            CustomerId = custId.toString(),
            QuotationNo=newLastQuotationNo.toString(),
          //  SampleOutNo = newLastSampleOutNO,
            //ReturnDate = productList.get(0).ReturnDate,
          //  Description =  productList.get(0).Description,
           // Date = productList.get(0).Date,
            //SampleStatus = "SampleOut",
           // Quantity = productList.size.toString(),
            TotalDiamondWeight = productList.sumOf { it.DiamondWt?.toDoubleOrNull() ?: 0.0 }.toString(),
            GrossWt = productList.sumOf { it.GrossWt?.toDoubleOrNull() ?: 0.0 }.toString(),
            NetWt = productList.sumOf { it.NetWt?.toDoubleOrNull() ?: 0.0 }.toString(),
            TotalStoneWeight = productList.sumOf { it.StoneAmt?.toDoubleOrNull() ?: 0.0 }.toString(),
           // TotalWt = productList.sumOf { it.TotalWt?.toDoubleOrNull() ?: 0.0 }.toString(),
            QuotationItem = productList.map { challan ->
                challan.toAddQuotationApiItem(
                    clientCode = clientCode,
                    customerId = custId,
                    branchId = branchId
                ).copy(
                    ItemCode = challan.ItemCode,
                    SKU = challan.SKU ?: "",
                    SKUId = challan.SKUId ?: 0,
                    CategoryId = challan.CategoryId ?: 0,
                    ProductId = challan.ProductId ?: 0,
                    DesignId = challan.DesignId ?: 0,
                    PurityId = challan.PurityId ?: 0,
                    GrossWt = challan.GrossWt,
                    NetWt = challan.NetWt,
                    TotalWt = challan.TotalWt ?: challan.NetWt,
                    FinePercentage = challan.FinePer ?: challan.FinePercentage ?: "0.0",
                    Description = challan.Description ?: "",
                    CategoryName = challan.CategoryName ?: "",
                    ProductName = challan.ProductName ?: "",
                    DesignName = challan.DesignName ?: "",
                    LabelledStockId = challan.LabelledStockId ?: 0,
                    CreatedOn = challan.CreatedOn ?: "2025-12-06",
                    RatePerGram = challan.MetalRate ?: challan.RatePerGram ?: "0",
                    MetalAmount = challan.MetalAmount ?: "0.00",
                    MakingCharg = challan.MakingCharg ?: "0.00",
                    TotalItemAmount = challan.TotalItemAmount ?: challan.itemAmt ?: "0.00",
                    itemAmt = challan.itemAmt ?: challan.TotalItemAmount ?: "0.00"
                )
            }
        )

        // ✅ Ab sirf valid state me hi API call hoga
        quotationViewModel.saveQuotation(request)
        isSaveClicked = false
        quotationViewModel.clearLastQuotationNo()
    }

    val addSampleOut by quotationViewModel.addResult.collectAsState()
    val updateSampleOut by quotationViewModel.updateResult.collectAsState()

    var printData by remember { mutableStateOf<QuotationPrintData?>(null) }
    var openPdfTrigger by remember { mutableStateOf(false) }

    LaunchedEffect(addSampleOut) {
        // 👉 initial emptyList / clear ke baad emptyList ko ignore karo
        val result = addSampleOut ?: return@LaunchedEffect
        Toast.makeText(
            context,
            localizedContext.getString(R.string.msg_quotation_saved),
            Toast.LENGTH_SHORT
        ).show()


        // ✅ Build print data
        val items = productList.map { it ->
            QuotationPrintItem(
                itemCode = it.ItemCode.orEmpty().ifBlank { "-" },
                rfidNo = it.RFIDCode.orEmpty().ifBlank { "-" },
                grossWt = it.GrossWt,
                netWt = it.NetWt,
                pcs = it.Pieces ?: "1",
                stoneWt = it.TotalStoneWeight ?: "0.000",
                stoneAmt = it.StoneAmt ?: it.StoneAmount ?: it.TotalStoneAmount ?: "0.00",
                wastagePercent = it.quotationPrintWastagePercent(),
                amount = it.TotalItemAmount ?: it.itemAmt ?: "0.00"
            )
        }

        // owner/customer values aapke actual source se lao (branch/org/customer object)
        val quotationNoStr = result.quotationNo?.toString() ?: ""   // response me jo field ho
        val dateStr = result.quotationItem?.toString()?.take(10) ?: ""

        printData = QuotationPrintData(
            ownerName = "VTjewellers_Rajapur",
            ownerAddress = "VT jewellers Near old MG road",
            ownerContact = "9342232444",

            quotationNo = quotationNoStr,
            date = dateStr,
            salesMan = "",
            remark = "",

            customerName = customerName,
            customerMobile = selectedCustomer?.Mobile ?: "",
            customerAddress = "wakad, pune, Maharashtra, India",

            items = items,
            totalAmount = items.sumOf { it.amount?.toDoubleOrNull() ?: 0.0 }.toString()
        )

        openPdfTrigger = true
        quotationViewModel.clearAddResult()
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

    LaunchedEffect(openPdfTrigger, printData) {
        if (!openPdfTrigger) return@LaunchedEffect
        val data = printData ?: return@LaunchedEffect

        GenerateQuotationPdf(context, data)
        openPdfTrigger = false
    }

    LaunchedEffect(updateSampleOut) {
        val result = updateSampleOut ?: return@LaunchedEffect

        Toast.makeText(context, localizedContext.getString(R.string.msg_quotation_updated), Toast.LENGTH_SHORT).show()
        quotationViewModel.clearUpdateResult()
    }


    Scaffold(
        topBar = {
            GradientTopBar(
                title = localizedContext.getString(R.string.quotations),
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
                        val request = UpdateQuotationRequest(
                            id = Id ?: 0,
                            clientCode = employee?.clientCode.toString(),
                            branchId = employee?.branchNo?.toInt(),
                            customerId = customerId!!.toInt(),

                            date = productList[0].LastUpdated,
                            quotationDate =productList[0].LastUpdated,
                            quotationStatus = "Delivered",

                            totalDiamondWeight = productList.sumOf { it.DiamondWt?.toDoubleOrNull() ?: 0.0 }.toString(),
                            grossWt = productList.sumOf { it.GrossWt?.toDoubleOrNull() ?: 0.0 }.toString(),
                            totalNetAmount = productList.sumOf { it.NetWt?.toDoubleOrNull() ?: 0.0 }.toString(),
                            totalStoneAmount = productList.sumOf { it.StoneAmt?.toDoubleOrNull() ?: 0.0 }.toString(),

                            quotationItem = productList.map { challan ->
                                QuotationItem(
                                    QuotationItemId = challan.QuotationItemId ?: 0,
                                    ItemCode = challan.ItemCode,
                                    SKU = challan.SKU,
                                    SKUId = challan.SKUId ?: 0,
                                    CategoryId = challan.CategoryId ?: 0,
                                    ProductId = challan.ProductId ?: 0,
                                    DesignId = challan.DesignId ?: 0,
                                    PurityId = challan.PurityId ?: 0,

                                    GrossWt = challan.GrossWt,
                                    NetWt = challan.NetWt,
                                    TotalWt = challan.TotalWt ?: challan.NetWt,

                                    // ✅ your extra fields (same names as data class)
                                    DiamondWt = challan.DiamondWt ?: "0.0",
                                    StoneAmt  = challan.StoneAmt ?: "0.0",
                                    DiamondAmt = challan.DiamondAmt ?: "0.0",
                                    FinePer = challan.FinePer ?: "0.0",
                                    FineWt = challan.FineWt ?: "0.0",

                                    qty = challan.qty ?: 1,
                                    tid = challan.tid ?: "",

                                    totayRate = challan.totayRate ?: "0.0",
                                    makingPercent = challan.makingPercent ?: "0.0",
                                    fixMaking = challan.fixMaking ?: "0.0",
                                    fixWastage = challan.fixWastage ?: "0.0"
                                )

                            }

                        )

                        // ✅ Ab sirf valid state me hi API call hoga
                        quotationViewModel.updateQuotation(request)

                    } else {

                        val clientCode = employee?.clientCode ?: return@ScanBottomBar
                        isSaveClicked = true
                        quotationViewModel.clearLastQuotationNo()
                        quotationViewModel.loadLastQuotationNo(clientCode)
                   }},
                onList = { navController.navigate(Screens.QuotationListScreen.route) },
                onScan = {
                    viewModel.startSingleScan(20)
                },
                onGscan = {
                    toggleBulkScan(viewModel, selectedPower) { isScanning = it }
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
                onCustomerSelected = { customerName = "${it.FirstName.orEmpty()} ${it.LastName.orEmpty()}".trim()
                    customerId = it.Id ?: 0},
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
                            showInvoiceDialog = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = localizedContext.getString(R.string.quotation_details),
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
            QuotationListTableComponent(
                productList = productList,
                onTotalsChange = { base, gst, final ->
                    baseTotal = base
                    gstAmount = gst
                    totalWithGst = final
                },
                onItemUpdated = { index, updated ->
                    // ✅ sirf ek item update
                    productList[index] = updated
                },
                onDeleteItem = { index ->
                    if (index in productList.indices) {
                        productList.removeAt(index)   // ✅ yaha se item hatao
                    }
                }
            )




        }
    }






}

fun addItemToList(
    code: String,
    allItems: List<BulkItem>,
    productList: SnapshotStateList<QuotationItem>,
    onMatched: (BulkItem) -> Unit
) {

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

    if (isBulkItemAlreadyInQuotationList(productList, matchedItem)) {
        Log.d("DropdownSelect", "⚠️ Duplicate item skipped ${matchedItem.itemCode}")
        return
    }

    Log.d("DropdownSelect", "✅ Matched item ${matchedItem.itemCode}")

    onMatched(matchedItem)
}
