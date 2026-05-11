package com.loyalstring.rfid.ui.screens
import java.text.SimpleDateFormat
import java.util.Date

import java.util.TimeZone
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
import com.loyalstring.rfid.data.model.sampleOut.IssueItemDto
import com.loyalstring.rfid.data.model.sampleOut.SampleOutAddRequest
import com.loyalstring.rfid.data.model.sampleOut.SampleOutDetails
import com.loyalstring.rfid.data.model.sampleOut.SampleOutFields
import com.loyalstring.rfid.data.model.sampleOut.SampleOutIssueItem
import com.loyalstring.rfid.data.model.sampleOut.SampleOutPrintData
import com.loyalstring.rfid.data.model.sampleOut.SampleOutPrintItem
import com.loyalstring.rfid.data.model.sampleOut.SampleOutUpdateRequest
import com.loyalstring.rfid.data.reader.ScanKeyListener
import com.loyalstring.rfid.data.remote.resource.Resource
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.navigation.Screens
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.text.orEmpty

@SuppressLint("UnrememberedMutableState")
@Composable
fun SampleOutScreen(
    onBack: () -> Unit,
    navController: NavHostController,
    Id: Int? = null,
    SampleOutNo: String? =null
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
    val sampleOutViewModel: SampleOutViewModel = hiltViewModel()
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
    val productList = remember { mutableStateListOf<SampleOutDetails>() }
    val deliveryChallanViewModel: DeliveryChallanViewModel = hiltViewModel()
    val tags by viewModel.scannedTags.collectAsState()
    val scanTrigger by viewModel.scanTrigger.collectAsState()
    var baseTotal by remember { mutableStateOf(0.0) }
    var gstAmount by remember { mutableStateOf(0.0) }
    var totalWithGst by remember { mutableStateOf(0.0) }
    var pendingMatchedItem by remember { mutableStateOf<BulkItem?>(null) }

    val errorMsg by sampleOutViewModel.error.collectAsState()
    val loading by sampleOutViewModel.loading.collectAsState()
    // ✅ Success toast – sirf jab addResult non-null ho
    val userPreferences = UserPreferences.getInstance(context)
    val savedLang = userPreferences.getAppLanguage().ifBlank { "en" }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = currentLocales[0]?.language ?: savedLang
    val localizedContext = LocaleHelper.applyLocale(context, currentLang)

// ❌ Error toast – sirf jab errorMsg set ho
    LaunchedEffect(errorMsg) {
        errorMsg?.let { msg ->
            android.widget.Toast
                .makeText(
                    context,
                    "Error: $msg",
                    android.widget.Toast.LENGTH_SHORT
                )
                .show()

            sampleOutViewModel.clearError()
        }
    }




    LaunchedEffect(shouldNavigateBack) {
        if (shouldNavigateBack) {
            kotlinx.coroutines.delay(50)
            onBack()
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
            Log.d("ManualEntry", "Found: ${matchedItem.itemCode}")

            // Prevent duplicates by RFID
            if (productList.any { it.RFIDCode.equals(matchedItem.rfid, ignoreCase = true) }) {
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
            val diamondAmt = safeDouble(matchedItem.diamondAmount)

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
*/

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
            if (productList.any { it.ItemCode == matchedItem.itemCode }) {
                Log.d("RFIDScan", "⚠️ Duplicate RFID skipped: ${matchedItem.itemCode}")
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

            if (productList.none { it.ItemCode == productDetail.ItemCode }) {
                productList.add(productDetail)
                Log.d("RFIDScan", "✅ Added ${productDetail.ItemCode} (${productDetail.RFIDCode})")
            } else {
                Log.d("RFIDScan", "⚠️ Duplicate tag skipped: ${productDetail.RFIDCode}")
            }
        }
    }

    // 🔹 When last challan number updates → Add the challan
    val lastSampleOutNo by sampleOutViewModel.lastSampleOutNo.collectAsState()

    LaunchedEffect(lastSampleOutNo) {

        // Only run when a new value is emitted
        val lastNo = lastSampleOutNo ?: return@LaunchedEffect
        Log.e("SampleOut", "lastNo"+lastNo)

        val clientCode = employee?.clientCode.orEmpty()
        val branchId = employee?.defaultBranchId ?: 1   // ya jahan se bhi branchId le raha hai
        val custId = customerId ?: 0

        // ❌ 1) Client code missing → add API mat call karo
        if (clientCode.isBlank()) {
            Log.e("SampleOut", "ClientCode missing")
            Toast.makeText(
                context,
                localizedContext.getString(R.string.msg_client_code_missing),
                Toast.LENGTH_SHORT
            ).show()
            sampleOutViewModel.clearLastSampleOutNo()
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
            sampleOutViewModel.clearLastSampleOutNo()
            return@LaunchedEffect
        }

        // ❌ 3) Koi items hi nahi → add API mat call karo
        if (productList.isEmpty()) {
            Log.e("SampleOut", "No items in productList")
            Toast.makeText(
                context,
                localizedContext.getString(R.string.add_at_least_one_item),
                Toast.LENGTH_SHORT
            ).show()
            sampleOutViewModel.clearLastSampleOutNo()
            return@LaunchedEffect
        }

        // ✅ Sab validation pass → abhi hi number generate karo + API call
        val newLastSampleOutNO = sampleOutViewModel.getNextSampleOutNo(lastNo)
        Log.d("@@","newLastSampleOutNO"+newLastSampleOutNO   )

        val request = SampleOutAddRequest(
            ClientCode = clientCode,
            BranchId = branchId,
            CustomerId = custId,
            SampleOutNo = newLastSampleOutNO,
            ReturnDate = productList.get(0).ReturnDate,
            Description =  productList.get(0).Description,
            Date = productList.get(0).Date,
            SampleStatus = "SampleOut",
            Quantity = productList.size,
            TotalDiamondWeight = productList.sumOf { it.DiamondWt.toDoubleOrNull() ?: 0.0 }.toString(),
            TotalGrossWt = productList.sumOf { it.GrossWt.toDoubleOrNull() ?: 0.0 }.toString(),
            TotalNetWt = productList.sumOf { it.NetWt.toDoubleOrNull() ?: 0.0 }.toString(),
            TotalStoneWeight = productList.sumOf { it.StoneAmt.toDoubleOrNull() ?: 0.0 }.toString(),
            TotalWt = productList.sumOf { it.TotalWt.toDoubleOrNull() ?: 0.0 }.toString(),
            IssueItems = productList.map { challan ->
                SampleOutIssueItem(
                    ItemCode = challan.ItemCode,
                    SKU = challan.SKU,
                    SKUId = challan.SKUId ?: 0,
                    CategoryId = challan.CategoryId ?: 0,
                    ProductId = challan.ProductId ?: 0,
                    DesignId = challan.DesignId ?: 0,
                    PurityId = challan.PurityId ?: 0,
                    Quantity = challan.qty ?: 1,
                    GrossWt = challan.GrossWt,
                    NetWt = challan.NetWt,
                    TotalWt = challan.TotalWt ?: challan.NetWt,
                    FinePercentage = challan.FinePer,
                    WastegePercentage = challan.StoneLessPercent,
                    StoneWeight = challan.TotalStoneWeight ?: "0.000",
                    DiamondWeight = challan.TotalDiamondWeight ?: "0.000",
                    FineWastageWt = challan.FineWastageWt ?: "0.000",
                    RatePerGram = challan.MetalRate,
                    MetalAmount = challan.MetalAmount,
                    Description = challan.Description ?: "",
                    SampleStatus = "SampleOut",
                    ClientCode = clientCode,
                    StoneAmount = challan.StoneAmt ?: "0.00",
                    SampleOutNo = newLastSampleOutNO,
                    DiamondAmount = challan.DiamondAmt ?: "",
                    Pieces = challan.Pieces ?: "0",
                    CategoryName = challan.CategoryName ?: "",
                    ProductName = challan.ProductName ?: "",
                    PurityName = challan.Purity ?: "",
                    DesignName = challan.DesignName ?: "",
                    Id = challan.LabelledStockId ?: 0,
                    CustomerId = custId,
                    VendorId = 0,
                    BranchId = branchId,
                    LabelledStockId = challan.LabelledStockId ?: 0,
                    CustomerName = customerName,
                    SampleInDate = "2025-12-06",
                    CreatedOn = "2025-12-06",
                    Customer = null
                )
            }
        )

        // ✅ Ab sirf valid state me hi API call hoga
        sampleOutViewModel.addSampleOut(request)
    }


    LaunchedEffect(Unit) {
        sampleOutViewModel.clearAddResult()
        sampleOutViewModel.clearLastSampleOutNo()
    }

    // 🔹 Show success message when challan added
    val addSampleOut by sampleOutViewModel.addResult.collectAsState()
    val updateSampleOut by sampleOutViewModel.updateResult.collectAsState()

    var printData by remember { mutableStateOf<SampleOutPrintData?>(null) }
    var openPdfTrigger by remember { mutableStateOf(false) }

    LaunchedEffect(addSampleOut) {
        // 👉 initial emptyList / clear ke baad emptyList ko ignore karo
        val result = addSampleOut ?: return@LaunchedEffect
        Toast.makeText(
            context,
            localizedContext.getString(R.string.sampleout_saved),
            Toast.LENGTH_SHORT
        ).show()


        // ✅ 1) Build data for PDF (use your current UI values / productList)
        val sampleNo = result.SampleOutNo ?: SampleOutNo ?: ""   // ✅ apne response field ke hisab se
        val date = productList.firstOrNull()?.Date ?: ""
        val returnDate = productList.firstOrNull()?.ReturnDate ?: ""

        val items = productList.map { it ->
            SampleOutPrintItem(
                itemDetails = "${it.CategoryName} - ${it.ProductName} - ${it.DesignName} - ${it.Purity}",
                grossWt = it.GrossWt,
                stoneWt = it.StoneAmt,
                diamondWt = it.DiamondWt,
                netWt = it.NetWt,
                pieces = it.Pieces,
                status = "Sample Out",
               // imageUrl = it.Image // optional
            )
        }

        printData = SampleOutPrintData(
            companyName =  UserPreferences.getInstance(context).getOrganization().toString(), // or from branch/company api
            customerName = customerName,
            addressCity = result.Customer.CurrAddTown.toString(), // map from customer if you have
            contactNo =  result.Customer.Mobile,    // map from customer
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

        // generate + open (main thread ok, but heavy work ho to IO me split kar sakte)
        generateSampleOutPrintPdf(context, data)

        openPdfTrigger = false
    }

    LaunchedEffect(updateSampleOut) {
        val result = updateSampleOut ?: return@LaunchedEffect

        Toast.makeText(context, localizedContext.getString(R.string.sampleout_updated), Toast.LENGTH_SHORT).show()
        sampleOutViewModel.clearUpdateResult()
    }
    fun IssueItemDto.toSampleOutDetails(): SampleOutDetails {
        return SampleOutDetails(
            Id = 0,
            MRP = "0.0", // agar API nahi bhej rahi to default
            CategoryName = this.CategoryName ?: "",
            ChallanStatus = this.SampleStatus ?: "SampleOut",
            ProductName = this.ProductName ?: "",
            Quantity = this.Quantity.toString(),
            HSNCode = "",
            ItemCode = this.ItemCode ?: "",
            GrossWt = this.GrossWt ?: "0.0",
            NetWt = this.NetWt ?: "0.0",
            ProductId = this.ProductId ?: 0,
            CustomerId = this.CustomerId ?: 0,

            MetalRate = this.RatePerGram ?: "0.0",
            MakingCharg = this.MetalAmount ?: "0.0",   // ya alag field ho to change kar
            MetalAmount = this.MetalAmount ?: "0.0",
            ItemAmount = this.MetalAmount ?: "0.0",
            TotalItemAmount = this.MetalAmount ?: "0.0",
            TotalAmount = this.MetalAmount ?: "0.0",
            Price = this.MetalAmount ?: "0.0",

            HUIDCode = "",
            ProductCode = "",
            ProductNo = "",
            Size = "1",
            StoneAmount = this.StoneAmount ?: "0.0",
            TotalWt = this.TotalWt ?: this.NetWt ?: "0.0",
            PackingWeight = "0.0",
            OldGoldPurchase = false,
            RatePerGram = this.RatePerGram ?: "0.0",
            Amount = this.MetalAmount ?: "0.0",
            ChallanType = "Delivery",
            FinePercentage = this.FinePercentage ?: "0.0",
            PurchaseInvoiceNo = "",
            HallmarkAmount = "0.0",
            HallmarkNo = "",
            MakingFixedAmt = "0.0",
            MakingFixedWastage = "0.0",
            MakingPerGram = "0.0",
            MakingPercentage = "0.0",
            Description = this.Description ?: "",

            CuttingGrossWt = this.GrossWt ?: "0.0",
            CuttingNetWt = this.NetWt ?: "0.0",
            BaseCurrency = "INR",
            CategoryId = this.CategoryId ?: 0,
            PurityId = this.PurityId ?: 0,
            TotalStoneWeight = this.StoneWeight ?: "0.0",
            TotalStoneAmount = this.StoneAmount ?: "0.0",
            TotalStonePieces = "0",
            TotalDiamondWeight = this.DiamondWeight ?: "0.0",
            TotalDiamondPieces = "0",
            TotalDiamondAmount = this.DiamondAmount ?: "0.0",

            SKUId = this.SKUId ?: 0,
            SKU = this.SKU ?: "",
            FineWastageWt = this.FineWastageWt ?: "0.0",
            ItemGSTAmount = "0.0",
            ClientCode = this.ClientCode ?: "",

            DiamondSize = "",
            DiamondWeight = this.DiamondWeight ?: "0.0",
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
            DiamondSellAmount = this.DiamondAmount ?: "0.0",
            DiamondDescription = "",

            MetalName = "",
            NetAmount = this.MetalAmount ?: "0.0",
            GSTAmount = "0.0",

            Purity = this.PurityName ?: "",
            DesignName = this.DesignName ?: "",
            CompanyId = 0,
            BranchId = this.BranchId ?: 0,
            CounterId = 0,
            EmployeeId = 0,
            LabelledStockId = this.LabelledStockId ?: 0,
            FineSilver = "0.0",
            FineGold = "0.0",
            DebitSilver = "0.0",
            DebitGold = "0.0",
            BalanceSilver = "0.0",
            BalanceGold = "0.0",
            ConvertAmt = "0.0",
            Pieces = this.Pieces ?: "1",
            StoneLessPercent = this.WastegePercentage ?: "0.0",
            DesignId = this.DesignId ?: 0,
            PacketId = 0,
            RFIDCode = "",       // agar backend bhejta ho to add kar
            Image = "",
            DiamondWt = this.DiamondWeight ?: "0.0",
            StoneAmt = this.StoneAmount ?: "0.0",
            DiamondAmt = this.DiamondAmount ?: "0.0",
            FinePer = this.FinePercentage ?: "0.0",
            FineWt = this.FineWastageWt ?: "0.0",
            qty = this.Quantity,
            tid = "",
            totayRate = this.RatePerGram ?: "0.0",
            makingPercent = "0.0",
            fixMaking = "0.0",
            fixWastage = "0.0",
            TIDNumber = "",
            CustomerName = "", Date = "",
            ReturnDate = ""


        )
    }


    LaunchedEffect(Id) {
        if (Id != null && Id != 0) {
            isEditMode = true

            // ✅ Step 1: Load challan list if not already loaded
            employee?.let {
                sampleOutViewModel.loadSampleOut(it.clientCode ?: "", "SampleOut")
            }

            // ✅ Step 2: Observe challan list and find the matching one
            sampleOutViewModel.sampleOutList.collect { challans ->
                val selected = challans.firstOrNull { it.Id == Id }
                if (selected != null) {
                    sampleOutViewModel.setSelectedSampleOut(selected)

                    // ✅ Step 3: Prefill UI fields
                    customerName = selected.Customer?.FirstName.toString()
                    customerId = selected.CustomerId
                    productList.clear()
                    selected.IssueItems
                        ?.filterNotNull()
                        ?.map { it.toSampleOutDetails() }   // ⬅️ yaha mapper use karo
                        ?.let { mappedList ->
                            productList.clear()
                            productList.addAll(mappedList)
                        }
                }
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
    LaunchedEffect(allItems, dailyRates) {
        viewModel.barcodeReader.openIfNeeded()

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
                val codeRfid     = normalize(item.rfid)
                val codeItemCode = normalize(item.itemCode)
                val codeProduct  = normalize(item.productCode)
                val codeTid      = normalize(item.tid)

                val candidates = listOf(codeRfid, codeItemCode, codeProduct, codeTid)

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
            if (productList.any { it.RFIDCode.equals(matchedItem.rfid, ignoreCase = true) }) {
                Log.d("RFID Scan", "⚠️ Already exists: ${matchedItem.itemCode}")
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

            val newProduct = SampleOutDetails(
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
                FineWastageWt = matchedItem.fixWastage ?: "0.0",
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
                BranchId = matchedItem.branchId ?: 0,
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
                qty = matchedItem.pcs ?: 1,
                tid = matchedItem.tid ?: "",
                totayRate = rate.toString(),
                makingPercent = makingPercentFinal.toString(),
                fixMaking = fixMakingFinal.toString(),
                fixWastage = fixWastageFinal.toString()
            )

            productList.add(newProduct)
            Log.d("RFID Scan", "✅ Added from barcode: ${newProduct.ItemCode} (${newProduct.RFIDCode})")
        }
    }
    val activity = LocalContext.current as? MainActivity
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
        activity?.registerScanKeyListener(listener)

        onDispose {
            activity?.unregisterScanKeyListener()
        }
    }



    Scaffold(
        topBar = {
            GradientTopBar(
                title = localizedContext.getString(R.string.sampleout_details),
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
                        val request = SampleOutUpdateRequest(
                            Id=Id,
                            ClientCode = employee?.clientCode.toString(),
                            BranchId = employee?.branchNo?.toInt(),
                            CustomerId = customerId!!.toInt(),
                            SampleOutNo = SampleOutNo.toString(),
                            ReturnDate = productList.get(0).ReturnDate,
                            Description =  productList.get(0).Description,
                            Date = productList.get(0).Date,
                            SampleStatus = "SampleOut",
                            Quantity = productList.size,
                            TotalDiamondWeight = productList.sumOf { it.DiamondWt.toDoubleOrNull() ?: 0.0 }.toString(),
                            TotalGrossWt = productList.sumOf { it.GrossWt.toDoubleOrNull() ?: 0.0 }.toString(),
                            TotalNetWt = productList.sumOf { it.NetWt.toDoubleOrNull() ?: 0.0 }.toString(),
                            TotalStoneWeight = productList.sumOf { it.StoneAmt.toDoubleOrNull() ?: 0.0 }.toString(),
                            TotalWt = productList.sumOf { it.TotalWt.toDoubleOrNull() ?: 0.0 }.toString(),
                            StatusType = true,
                            SampleInDate = getCurrentUtcDateTime(),
                            IssueItems = productList.map { challan ->
                                SampleOutIssueItem(
                                    ItemCode = challan.ItemCode,
                                    SKU = challan.SKU,
                                    SKUId = challan.SKUId ?: 0,
                                    CategoryId = challan.CategoryId ?: 0,
                                    ProductId = challan.ProductId ?: 0,
                                    DesignId = challan.DesignId ?: 0,
                                    PurityId = challan.PurityId ?: 0,
                                    Quantity = challan.qty ?: 1,
                                    GrossWt = challan.GrossWt,
                                    NetWt = challan.NetWt,
                                    TotalWt = challan.TotalWt ?: challan.NetWt,
                                    FinePercentage = challan.FinePer,
                                    WastegePercentage = challan.StoneLessPercent,
                                    StoneWeight = challan.TotalStoneWeight ?: "0.000",
                                    DiamondWeight = challan.TotalDiamondWeight ?: "0.000",
                                    FineWastageWt = challan.FineWastageWt ?: "0.000",
                                    RatePerGram = challan.MetalRate,
                                    MetalAmount = challan.MetalAmount,
                                    Description = challan.Description ?: "",
                                    SampleStatus = "SampleOut",
                                    ClientCode = challan.ClientCode,
                                    StoneAmount = challan.StoneAmt ?: "0.00",
                                    SampleOutNo = challan.Id.toString(),
                                    DiamondAmount = challan.DiamondAmt ?: "",
                                    Pieces = challan.Pieces ?: "0",
                                    CategoryName = challan.CategoryName ?: "",
                                    ProductName = challan.ProductName ?: "",
                                    PurityName = challan.Purity ?: "",
                                    DesignName = challan.DesignName ?: "",
                                    Id = Id ?: 0,
                                    CustomerId = customerId!!.toInt(),
                                    VendorId = 0,
                                    BranchId = employee?.branchNo?.toInt(),
                                    LabelledStockId = challan.LabelledStockId ?: 0,
                                    CustomerName = customerName,
                                    SampleInDate = getCurrentUtcDateTime(),
                                    CreatedOn = getCurrentUtcDateTime(),
                                    Customer = null
                                )
                            }
                        )

                        // ✅ Ab sirf valid state me hi API call hoga
                        sampleOutViewModel.updateSampleOut(request)
              
                } else {

                    val clientCode = employee?.clientCode ?: return@ScanBottomBar
                    val branchId = employee.branchNo ?: 1

                    // 🔹 Step 1: Fetch last challan no
                    sampleOutViewModel.fetchLastSampleOutNo(clientCode, branchId)

                }},
                onList = { navController.navigate(Screens.SampleOutListScreen.route) },
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
                            text = "Sample Out",
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
            SampleOutListTableComponent(
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



    if (showInvoiceDialog) {
        SampleOutFieldsDialog (
            onDismiss = { showInvoiceDialog = false },
            branchList = branchList,
            salesmanList = customerSuggestions, // ya jo bhi tu use kar raha hai
            onConfirm = { fields ->
                showInvoiceDialog = false
                sampleOutFields = fields

                // 🔥 Yaha sab items pe same values set kar:
                for (i in productList.indices) {
                    val old = productList[i]
                    productList[i] = old.copy(

                        Date = fields.date,
                        Description = fields.description,
                        ReturnDate = fields.returnDate

                        // example mapping:
                       /* BranchId = branchList
                            .firstOrNull { it.BranchName == fields.branchName }
                            ?.Id ?: old.BranchId,
                        FinePer = fields.fine,
                        fixWastage = fields.wastage,*/
                        // agar EmployeeId chahiye:
                        // EmployeeId = ...


                    )
                }
            }
        )
    }


}

fun getCurrentUtcDateTime(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    return sdf.format(Date())
}

fun addItemToList(
    code: String,
    allItems: List<BulkItem>,
    productList: SnapshotStateList<SampleOutDetails>,
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

    if (productList.any { it.tid == matchedItem.tid }) {
        Log.d("DropdownSelect", "⚠️ Duplicate item skipped ${matchedItem.itemCode}")
        return
    }

    Log.d("DropdownSelect", "✅ Matched item ${matchedItem.itemCode}")

    onMatched(matchedItem)
}

