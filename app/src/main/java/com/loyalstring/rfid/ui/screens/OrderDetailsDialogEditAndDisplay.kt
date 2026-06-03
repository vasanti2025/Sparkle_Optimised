package com.loyalstring.rfid.ui.screens

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.local.entity.OrderItem
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.addSingleItem.BranchModel
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.ui.utils.GradientButtonIcon
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.OrderViewModel
import com.loyalstring.rfid.viewmodel.SingleProductViewModel
import com.loyalstring.rfid.worker.LocaleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/* ============================
   Helpers
   ============================ */
private fun asDouble(v: String?): Double = v?.trim()?.toDoubleOrNull() ?: 0.0
private fun fmt3(v: Double): String = String.format(Locale.getDefault(), "%.3f", v)
private fun fmt2(v: Double): String = String.format(Locale.getDefault(), "%.2f", v)

@SuppressLint("DefaultLocale")
@Composable
fun OrderDetailsDialogEditAndDisplay(
    selectedItem: OrderItem?,
    branchList: List<BranchModel>,
    onDismiss: () -> Unit,
    onSave: (OrderItem) -> Unit,
) {
    Log.e("TAG", "RFID Code: ${selectedItem?.rfidCode} image url ${selectedItem?.grWt}")

    val orderViewModel: OrderViewModel = hiltViewModel()
    val singleProductViewModel: SingleProductViewModel = hiltViewModel()
    val context = LocalContext.current
    val employee = UserPreferences.getInstance(context).getEmployee(Employee::class.java)

    /* ============================
       Load Master Data (ONCE)
       ============================ */
    LaunchedEffect(employee?.clientCode) {
        withContext(Dispatchers.IO) {
            val cc = employee?.clientCode.toString()
            orderViewModel.getAllEmpList(cc)
            orderViewModel.getAllItemCodeList(ClientCodeRequest(cc))
            singleProductViewModel.getAllBranches(ClientCodeRequest(cc))
            singleProductViewModel.getAllPurity(ClientCodeRequest(cc))
            singleProductViewModel.getAllSKU(ClientCodeRequest(cc))
            orderViewModel.getDailyRate(ClientCodeRequest(cc))
        }
    }

    // If UI passed branchList empty, try fetching again (fallback)
    LaunchedEffect(branchList.size) {
        if (branchList.isEmpty()) {
            withContext(Dispatchers.IO) {
                singleProductViewModel.getAllBranches(
                    ClientCodeRequest(employee?.clientCode.toString())
                )
            }
        }
    }

    /* ============================
       Date helpers
       ============================ */
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val inputFormats = remember {
        listOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        )
    }

    fun formatDateSafe(dateString: String?): String {
        if (dateString.isNullOrBlank() || dateString.equals("null", true)) return ""
        for (format in inputFormats) {
            try {
                val parsed = format.parse(dateString)
                if (parsed != null) return dateFormatter.format(parsed)
            } catch (_: Exception) {
            }
        }
        return ""
    }

    /* ============================
       ✅ Stable init key + stable item snapshot
       ============================ */
    val initKey = remember(selectedItem?.tid, selectedItem?.rfidCode, selectedItem?.itemCode) {
        selectedItem?.tid?.takeIf { it.isNotBlank() }
            ?: selectedItem?.rfidCode?.takeIf { it.isNotBlank() }
            ?: selectedItem?.itemCode?.takeIf { it.isNotBlank() }
            ?: "INIT"
    }
    val stableItem = remember(initKey) { selectedItem } // freeze

    /* ============================
       Form State
       ============================ */
    var selectedBranchId by rememberSaveable(initKey) { mutableStateOf(stableItem?.branchId ?: "") }
    var branch by rememberSaveable(initKey) { mutableStateOf("") }
    var exhibition by rememberSaveable(initKey) { mutableStateOf("") }
    var remark by rememberSaveable(initKey) { mutableStateOf("") }
    var purity by rememberSaveable(initKey) { mutableStateOf("") }
    var size by rememberSaveable(initKey) { mutableStateOf("") }
    var length by rememberSaveable(initKey) { mutableStateOf("") }
    var typeOfColors by rememberSaveable(initKey) { mutableStateOf("") }
    var screwType by rememberSaveable(initKey) { mutableStateOf("") }
    var polishType by rememberSaveable(initKey) { mutableStateOf("") }
    var finePercentage by rememberSaveable(initKey) { mutableStateOf("") }
    var wastage by rememberSaveable(initKey) { mutableStateOf("") }
    var orderDate by rememberSaveable(initKey) { mutableStateOf("") }
    var deliverDate by rememberSaveable(initKey) { mutableStateOf("") }
    var categoryId by remember { mutableStateOf(0) }

    var productName by rememberSaveable(initKey) { mutableStateOf("") }
    var itemCode by rememberSaveable(initKey) { mutableStateOf("") }
    var sku by rememberSaveable(initKey) { mutableStateOf("") }

    // Weights
    var totalWt by rememberSaveable(initKey) { mutableStateOf("") }
    var packingWt by rememberSaveable(initKey) { mutableStateOf("") }
    var grossWT by rememberSaveable(initKey) { mutableStateOf("") }
    var stoneWt by rememberSaveable(initKey) { mutableStateOf("") }
    var dimondWt by rememberSaveable(initKey) { mutableStateOf("") }
    var NetWt by rememberSaveable(initKey) { mutableStateOf("") }

    // Pricing
    var ratePerGRam by rememberSaveable(initKey) { mutableStateOf("") }

    // ✅ Cursor-safe: hallmark & mrp as plain strings (no TextFieldValue, no blur formatting)
    var hallMarkAmt by rememberSaveable(initKey) { mutableStateOf("") }
    var mrp by rememberSaveable(initKey) { mutableStateOf("") }

    var itemAmt by rememberSaveable(initKey) { mutableStateOf("") }

    // Calculated
    var finePlusWt by rememberSaveable(initKey) { mutableStateOf("") }
    var qty by rememberSaveable(initKey) { mutableStateOf("") }

    var stoneAmt by rememberSaveable(initKey) { mutableStateOf("") }
    var diamondAmt by rememberSaveable(initKey) { mutableStateOf("") }
    // Gross focus (auto calc should not override when user typing)
    var grossHasFocus by remember { mutableStateOf(false) }

    val userPreferences = UserPreferences.getInstance(context)
    val savedLang = userPreferences.getAppLanguage().ifBlank { "en" }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = currentLocales[0]?.language ?: savedLang
    val localizedContext = LocaleHelper.applyLocale(context, currentLang)
    /* ============================
       Unified Recalculation
       IMPORTANT:
       - Never format/overwrite hallMarkAmt or mrp inside recalcAll()
       ============================ */
    fun recalcAll() {
        val totalParsed = totalWt.trim().toDoubleOrNull()
        val packing = asDouble(packingWt)
        val grossInput = asDouble(grossWT)

        val autoGross = (totalParsed != null && totalParsed > 0.0 && !grossHasFocus)

        val gross = if (autoGross) {
            (totalParsed - packing).coerceAtLeast(0.0)
        } else {
            grossInput.coerceAtLeast(0.0)
        }

        if (autoGross) {
            val newGrossStr = fmt3(gross)
            if (grossWT != newGrossStr) grossWT = newGrossStr
        }

        val stone = asDouble(stoneWt)
        val diamond = asDouble(dimondWt)
        val net = (gross - stone - diamond).coerceAtLeast(0.0)
        NetWt = fmt3(net)

        val fineP = asDouble(finePercentage)
        val wastP = asDouble(wastage)
        finePlusWt = fmt3((net * ((fineP + wastP) / 100.0)).coerceAtLeast(0.0))

        val rate = asDouble(ratePerGRam)
        val hallmark = asDouble(hallMarkAmt)
      //  val baseAmt = (net * rate) + hallmark
        val stoneAmount = asDouble(stoneAmt)
        val diamondAmount = asDouble(diamondAmt)

        val baseAmt = (net * rate) + hallmark + stoneAmount + diamondAmount

        val mrpVal = asDouble(mrp)
        itemAmt = if (mrpVal > 0) fmt2(mrpVal) else fmt2(baseAmt)
    }

    /* ============================
       Init ONCE from stableItem
       ============================ */
    LaunchedEffect(initKey) {
        if (stableItem == null) return@LaunchedEffect
        categoryId=stableItem.categoryId?:0
        selectedBranchId = stableItem.branchId ?: ""
        branch = stableItem.branchName?.takeIf { !it.equals("null", true) } ?: ""

        productName = stableItem.productName.orEmpty()
        itemCode = stableItem.itemCode.orEmpty()
        sku = stableItem.sku.orEmpty()

        totalWt = stableItem.totalWt.orEmpty()
        packingWt = stableItem.packingWt?.takeIf { !it.equals("null", true) } ?: ""
        grossWT = stableItem.grWt.orEmpty()

        stoneWt = stableItem.stoneWt.orEmpty()
        dimondWt = stableItem.dimondWt?.takeIf { !it.equals("null", true) } ?: ""
        NetWt = stableItem.nWt.orEmpty()

        exhibition = stableItem.exhibition.orEmpty()
        purity = stableItem.purity?.takeIf { !it.equals("null", true) } ?: ""
        size = stableItem.size?.takeIf { !it.equals("null", true) } ?: ""
        length = stableItem.length.orEmpty()
        stoneAmt = stableItem.stoneAmt.orEmpty()
        diamondAmt = stableItem.diamondAmt.orEmpty()
        remark = stableItem.remark.orEmpty()
        typeOfColors = stableItem.typeOfColor?.takeIf { !it.equals("null", true) } ?: "Select color"
        screwType = stableItem.screwType.orEmpty()
        polishType = stableItem.polishType.orEmpty()

        finePercentage = stableItem.finePer?.takeIf { !it.equals("null", true) } ?: ""
        wastage = stableItem.makingPercentage?.takeIf { !it.equals("null", true) } ?: ""

        orderDate = formatDateSafe(stableItem.orderDate)
        deliverDate = formatDateSafe(stableItem.deliverDate)

        // ✅ Simple init (no cursor/selection logic)
        hallMarkAmt = stableItem.hallmarkAmt.orEmpty()
        mrp = stableItem.mrp.orEmpty()

        ratePerGRam = stableItem.todaysRate.orEmpty()

        qty = when {
            stableItem.qty.isNullOrBlank() -> "1"
            stableItem.qty.equals("null", true) -> "1"
            stableItem.qty == "0" -> "1"
            else -> stableItem.qty.orEmpty()
        }

        recalcAll()
    }

    /* ============================
       Default OrderDate = Today
       ============================ */
    val calendar = remember { Calendar.getInstance() }
    LaunchedEffect(orderDate) {
        if (orderDate.isBlank()) {
            orderDate = dateFormatter.format(calendar.time)
        }
    }

    /* ============================
       Daily Rate -> set rate by purity
       ============================ */
    val dailyRates by orderViewModel.getAllDailyRate.collectAsState()
    LaunchedEffect(purity, dailyRates) {
        if (purity.isNotBlank() && dailyRates.isNotEmpty()) {
            val match = dailyRates.find { it.PurityName.equals(purity, ignoreCase = true) }
            val newRate = match?.Rate ?: ""
            if (newRate.isNotBlank()) {
                ratePerGRam = newRate
                recalcAll()
            }
        }
    }

    /* ============================
       Dropdown data
       ============================ */
    val purityList by singleProductViewModel.purityResponse1.collectAsState()
    Log.d("@@ purity","purity"+purity)
    LaunchedEffect(purityList, categoryId) {
        if (purity.isNotBlank()) {

            Log.d("@@ purity", "purity = $purity")
            Log.d("@@ categoryId", "categoryId = $categoryId")

            val match = purityList.firstOrNull {
                it.PurityName.equals(purity, ignoreCase = true) &&
                        (it.CategoryId ?: 0) == categoryId
            }

            if (match != null) {
                purity = match.PurityName ?: ""
            }
        }
    }

    val filteredPurityList = purityList.filter {
        (it.CategoryId ?: 0) == categoryId
    }
    val skuList by singleProductViewModel.skuResponse1.collectAsState()

    val colorsList = listOf(
        "Yellow Gold", "White Gold", "Rose Gold", "Green Gold",
        "Black Gold", "Blue Gold", "Purple Gold"
    )
    val screwList = listOf("Type 1", "Type 2", "Type 3")
    val polishList = listOf("High Polish", "Matte Finish", "Satin Finish", "Hammered")

    val baseUrl = "https://rrgold.loyalstring.co.in/"

    var expandedBranch by remember { mutableStateOf(false) }
    var expandedPurity by remember { mutableStateOf(false) }
    var expandedColors by remember { mutableStateOf(false) }
    var expandedScrew by remember { mutableStateOf(false) }
    var expandedPolish by remember { mutableStateOf(false) }
    var expandedSKU by remember { mutableStateOf(false) }

    // ✅ Recalculate on changes (DO NOT format hallmark/mrp here)
    LaunchedEffect(
        totalWt, packingWt, grossWT,
        stoneWt, dimondWt,
        finePercentage, wastage,
        ratePerGRam,
        hallMarkAmt, mrp, stoneAmt, diamondAmt
    ) {
        recalcAll()
    }

    Dialog(onDismissRequest = { onDismiss() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(8.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                // Title Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.order_edit_icon),
                            contentDescription = "Custom Order Icon",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = localizedContext.getString(R.string.custom_order_fields),
                            fontSize = 18.sp,
                            color = Color.White,
                            fontFamily = poppins
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(8.dp)
                ) {

                    // Image
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        AsyncImage(
                            model = baseUrl + (stableItem?.image ?: ""),
                            contentDescription = "Image from URL",
                            placeholder = painterResource(R.drawable.add_photo),
                            error = painterResource(R.drawable.add_photo),
                            modifier = Modifier.size(100.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Branch Dropdown
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        color = Color(0xFFF2F2F3),
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (branchList.isNotEmpty()) {
                                DropdownMenuFieldDisplay(
                                    label = localizedContext.getString(R.string.branch),
                                    options = branchList,
                                    selectedValue = branch,
                                    expanded = expandedBranch,
                                    onValueChange = { selectedLabel ->
                                        branch = selectedLabel
                                        val b = branchList.firstOrNull { it.BranchName == selectedLabel }
                                        selectedBranchId = (b?.Id ?: selectedBranchId).toString()
                                    },
                                    onExpandedChange = { expandedBranch = it },
                                    getOptionLabel = { it.BranchName },
                                    enabled = true
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    FieldRowReadOnly(label = localizedContext.getString(R.string.product_name), value = productName)
                    Spacer(modifier = Modifier.height(4.dp))

                    FieldRowReadOnly(label = localizedContext.getString(R.string.item_code), value = itemCode)
                    Spacer(modifier = Modifier.height(4.dp))

                    FieldRowInput(
                        label = localizedContext.getString(R.string.label_total_weight),
                        value = totalWt,
                        placeholder =localizedContext.getString(R.string.enter_total_weight),
                        onChange = { totalWt = it }
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    FieldRowInput(
                        label = localizedContext.getString(R.string.packing_wt),
                        value = packingWt,
                        placeholder = localizedContext.getString(R.string.enter_packing_weight),
                        onChange = { packingWt = it }
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Gross Wt (editable) - focus tracking
                    FieldRowInput(
                        label = localizedContext.getString(R.string.gross_wt_header),
                        value = grossWT,
                        placeholder = localizedContext.getString(R.string.enter_gross_weight),
                        onChange = { grossWT = it },
                        onFocusChange = { focused ->
                            if (grossHasFocus && !focused) {
                                grossWT = if (grossWT.isBlank()) "" else fmt3(asDouble(grossWT))
                            }
                            grossHasFocus = focused
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    FieldRowInput(
                        label = localizedContext.getString(R.string.stone_weight),
                        value = stoneWt,
                        placeholder = localizedContext.getString(R.string.enter_stone_weight),
                        onChange = { stoneWt = it }
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    FieldRowInput(
                        label = localizedContext.getString(R.string.diamond_weight),
                        value = dimondWt,
                        placeholder = localizedContext.getString(R.string.enter_diamond_weight),
                        onChange = { dimondWt = it }
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    FieldRowDisplay(label = localizedContext.getString(R.string.net_weight), value = if (NetWt.isBlank()) "0.000" else NetWt)
                    Spacer(modifier = Modifier.height(4.dp))

                    FieldRowInput(
                        label = localizedContext.getString(R.string.rate_per_gram),
                        value = ratePerGRam,
                        placeholder = localizedContext.getString(R.string.enter_rate),
                        onChange = { ratePerGRam = it }
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    FieldRowInput(
                        label = localizedContext.getString(R.string.stone_amt),
                        value = stoneAmt,
                        placeholder =  localizedContext.getString(R.string.enter_stone_amount),
                        onChange = { stoneAmt = it }
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    FieldRowInput(
                        label = localizedContext.getString(R.string.diamond_amt),
                        value = diamondAmt,
                        placeholder = localizedContext.getString(R.string.enter_diamond_amount),
                        onChange = { diamondAmt = it }
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    FieldRowInput(
                        label = localizedContext.getString(R.string.exhibition),
                        value = exhibition,
                        placeholder = localizedContext.getString(R.string.enter_exhibition),
                        onChange = { exhibition = it }
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    FieldRowInput(
                        label = localizedContext.getString(R.string.remark),
                        value = remark,
                        placeholder = localizedContext.getString(R.string.enter_remark),
                        onChange = { remark = it }
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // SKU dropdown (disabled)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        color = Color(0xFFF2F2F3),
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            DropdownMenuFieldDisplay(
                                label = localizedContext.getString(R.string.sku),
                                options = skuList,
                                selectedValue = sku,
                                expanded = expandedSKU,
                                onValueChange = { sku = it },
                                onExpandedChange = { expandedSKU = it },
                                getOptionLabel = { it.PurityName.toString() },
                                enabled = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Purity dropdown
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        color = Color(0xFFF2F2F3),
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            DropdownMenuFieldDisplay(
                                label = localizedContext.getString(R.string.purity),
                                options = filteredPurityList,
                                selectedValue = purity,
                                expanded = expandedPurity,
                                onValueChange = { purity = it },
                                onExpandedChange = { expandedPurity = it },
                                getOptionLabel = { it.PurityName },
                                enabled = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    FieldRowInput(
                        label = localizedContext.getString(R.string.size),
                        value = size,
                        placeholder = localizedContext.getString(R.string.enter_size),
                        onChange = { size = it }
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    FieldRowInput(
                        label = localizedContext.getString(R.string.length),
                        value = length,
                        placeholder =localizedContext.getString(R.string.enter_length),
                        onChange = { length = it }
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Colors
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        color = Color(0xFFF2F2F3),
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            DropdownMenuFieldDisplay(
                                label = localizedContext.getString(R.string.colors),
                                options = colorsList,
                                selectedValue = typeOfColors,
                                expanded = expandedColors,
                                onValueChange = { typeOfColors = it },
                                onExpandedChange = { expandedColors = it },
                                getOptionLabel = { it.toString() },
                                enabled = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Screw Type
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        color = Color(0xFFF2F2F3),
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            DropdownMenuFieldDisplay(
                                label = localizedContext.getString(R.string.screw_type),
                                options = screwList,
                                selectedValue = screwType,
                                expanded = expandedScrew,
                                onValueChange = { screwType = it },
                                onExpandedChange = { expandedScrew = it },
                                getOptionLabel = { it.toString() },
                                enabled = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Polish Type
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        color = Color(0xFFF2F2F3),
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            DropdownMenuFieldDisplay(
                                label = localizedContext.getString(R.string.polish_type),
                                options = polishList,
                                selectedValue = polishType,
                                expanded = expandedPolish,
                                onValueChange = { polishType = it },
                                onExpandedChange = { expandedPolish = it },
                                getOptionLabel = { it.toString() },
                                enabled = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    FieldRowInput(
                        label = localizedContext.getString(R.string.fine_percent),
                        value = finePercentage,
                        placeholder = localizedContext.getString(R.string.enter_fine),
                        onChange = { finePercentage = it }
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    FieldRowInput(
                        label = localizedContext.getString(R.string.wastage),
                        value = wastage,
                        placeholder = localizedContext.getString(R.string.enter_wastage),
                        onChange = { wastage = it }
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    DateRow(
                        label = localizedContext.getString(R.string.order_date),
                        value = orderDate,
                        placeholder = localizedContext.getString(R.string.enter_order_date),
                        onPick = { picked -> orderDate = picked }
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    DateRow(
                        label = deliverDate,
                        value = deliverDate,
                        placeholder = localizedContext.getString(R.string.enter_deliver_date),
                        minDateYmd = orderDate,
                        onPick = { picked -> deliverDate = picked }
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    FieldRowInput(
                        label = localizedContext.getString(R.string.quantity),
                        value = qty,
                        placeholder = localizedContext.getString(R.string.enter_quantity),
                        onChange = { qty = it }
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // ✅ Hallmark (cursor-safe)
                    FieldRowInput(
                        label = localizedContext.getString(R.string.hallmark_amt),
                        value = hallMarkAmt,
                        placeholder =localizedContext.getString(R.string.enter_hallmark),
                        onChange = { hallMarkAmt = it }
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // ✅ MRP (cursor-safe)
                    FieldRowInput(
                        label = localizedContext.getString(R.string.mrp),
                        value = mrp,
                        placeholder =localizedContext.getString(R.string.enter_mrp),
                        onChange = { mrp = it }
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    FieldRowDisplay(
                        label = localizedContext.getString(R.string.header_fine_plus_weight),
                        value = if (finePlusWt.isBlank()) "0.000" else finePlusWt
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    FieldRowDisplay(
                        label = localizedContext.getString(R.string.item_amount),
                        value = if (itemAmt.isBlank()) "0.00" else itemAmt
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    GradientButtonIcon(
                        text = localizedContext.getString(R.string.cancel),
                        onClick = { onDismiss() },
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .padding(start = 8.dp, bottom = 16.dp),
                        icon = painterResource(id = R.drawable.ic_cancel),
                        iconDescription = "Cancel Icon",
                        fontSize = 12
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    GradientButtonIcon(
                        text = localizedContext.getString(R.string.ok),
                        onClick = {
                            if (deliverDate.isBlank()) {
                                Toast.makeText(context, localizedContext.getString(R.string.enter_delivery_date_error), Toast.LENGTH_SHORT).show()
                                return@GradientButtonIcon
                            }

                            // Ensure latest calc before save
                            recalcAll()

                            // ✅ Save-time formatting (safe)
                            val hallmarkToSave =
                                if (hallMarkAmt.isBlank()) "" else fmt2(asDouble(hallMarkAmt))
                            val mrpToSave =
                                if (mrp.isBlank()) "" else fmt2(asDouble(mrp))

                            val orderItem = OrderItem(
                                branchId = selectedBranchId,
                                branchName = branch,
                                exhibition = exhibition,
                                remark = remark,
                                purity = purity,
                                size = size,
                                length = length,
                                typeOfColor = typeOfColors,
                                screwType = screwType,
                                polishType = polishType,
                                finePer = finePercentage,
                                wastage = wastage,
                                orderDate = orderDate,
                                deliverDate = deliverDate,

                                productName = productName,
                                itemCode = itemCode,
                                rfidCode = stableItem?.rfidCode.orEmpty(),

                                totalWt = totalWt,
                                packingWt = packingWt,
                                grWt = grossWT,
                                stoneWt = stoneWt,
                                dimondWt = dimondWt,
                                nWt = NetWt,

                                todaysRate = ratePerGRam,

                                hallmarkAmt = hallmarkToSave,
                                mrp = mrpToSave,
                                itemAmt = itemAmt,

                                finePlusWt = finePlusWt,
                                stoneAmt = stoneAmt,
                                sku = sku,
                                qty = qty,

                                image = stableItem?.image.orEmpty(),
                                netAmt = "",

                                diamondAmt = diamondAmt,

                                categoryId = stableItem?.categoryId ?: 0,
                                categoryName = stableItem?.categoryName.orEmpty(),
                                productId = stableItem?.productId ?: 0,
                                productCode = stableItem?.productName.orEmpty(),
                                skuId = stableItem?.skuId ?: 0,
                                designid = stableItem?.designid ?: 0,
                                designName = stableItem?.designName.orEmpty(),
                                purityid = stableItem?.purityid ?: 0,
                                counterId = stableItem?.counterId ?: 0,
                                counterName = "",

                                companyId = 0,
                                epc = stableItem?.epc ?: "",
                                tid = stableItem?.tid ?: "",

                                makingPercentage = wastage,
                                makingFixedAmt = stableItem?.makingFixedAmt.orEmpty(),
                                makingFixedWastage = stableItem?.makingFixedWastage.orEmpty(),
                                makingPerGram = stableItem?.makingPerGram.orEmpty(),
                                CategoryWt = stableItem?.CategoryWt.toString()
                            )

                            orderViewModel.insertOrderItemToRoomORUpdate(orderItem)
                            onSave(orderItem)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .padding(end = 8.dp, bottom = 16.dp),
                        icon = painterResource(id = R.drawable.check_circle),
                        iconDescription = "Check Icon",
                        fontSize = 12
                    )
                }
            }
        }
    }
}

/* ============================
   Reusable UI Rows
   ============================ */

@Composable
private fun FieldRowReadOnly(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier
                .weight(0.4f)
                .padding(start = 2.dp),
            fontSize = 12.sp,
            color = Color.Black,
            fontFamily = poppins
        )
        Box(
            modifier = Modifier
                .weight(0.9f)
                .padding(start = 6.dp, top = 4.dp, end = 2.dp, bottom = 4.dp)
                .height(32.dp)
                .background(Color.White, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                readOnly = true,
                onValueChange = {},
                singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp, color = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(2.dp)
            )
        }
    }
}

@Composable
private fun FieldRowDisplay(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier
                .weight(0.4f)
                .padding(start = 2.dp),
            fontSize = 12.sp,
            color = Color.Black,
            fontFamily = poppins
        )
        Box(
            modifier = Modifier
                .weight(0.9f)
                .padding(start = 6.dp, top = 4.dp, end = 2.dp, bottom = 4.dp)
                .height(32.dp)
                .background(Color.White, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = value,
                fontSize = 13.sp,
                color = Color.Black,
                modifier = Modifier.padding(start = 4.dp),
                fontFamily = poppins
            )
        }
    }
}

@Composable
private fun FieldRowInput(
    label: String,
    value: String,
    placeholder: String,
    onChange: (String) -> Unit,
    onFocusChange: ((Boolean) -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier
                .weight(0.4f)
                .padding(start = 2.dp),
            fontSize = 12.sp,
            color = Color.Black,
            fontFamily = poppins
        )
        Box(
            modifier = Modifier
                .weight(0.9f)
                .padding(start = 6.dp, top = 4.dp, end = 2.dp, bottom = 4.dp)
                .height(32.dp)
                .background(Color.White, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.CenterStart
        ) {
            // Placeholder
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 4.dp),
                    fontFamily = poppins
                )
            }

            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp, color = Color.Black),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(2.dp)
                    .onFocusChanged { onFocusChange?.invoke(it.isFocused) }
            )
        }
    }
}

@Composable
private fun DateRow(
    label: String,
    value: String,
    placeholder: String,
    minDateYmd: String? = null,
    onPick: (String) -> Unit,
) {
    val context = LocalContext.current
    val cal = remember { Calendar.getInstance() }
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier
                .weight(0.4f)
                .padding(start = 2.dp),
            fontSize = 12.sp,
            color = Color.Black,
            fontFamily = poppins
        )

        Box(
            modifier = Modifier
                .weight(0.9f)
                .padding(start = 6.dp, top = 4.dp, end = 2.dp, bottom = 4.dp)
                .height(32.dp)
                .background(Color.White, RoundedCornerShape(4.dp))
                .clickable {
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val selectedDate =
                                Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                            onPick(dateFormatter.format(selectedDate.time))
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).apply {
                        val minCal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        if (!minDateYmd.isNullOrBlank()) {
                            try {
                                val parsed = dateFormatter.parse(minDateYmd)
                                if (parsed != null) minCal.time = parsed
                            } catch (_: Exception) {
                            }
                        }

                        datePicker.minDate = minCal.timeInMillis
                    }.show()
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (value.isBlank()) placeholder else value,
                    fontSize = 13.sp,
                    color = if (value.isBlank()) Color.Gray else Color.Black,
                    modifier = Modifier.weight(1f),
                    fontFamily = poppins
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_calender),
                    contentDescription = "Calendar",
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/* ============================
   Dropdown Menu Field
   ============================ */
@Composable
fun <T> DropdownMenuFieldDisplay(
    label: String,
    options: List<T>,
    selectedValue: String,
    expanded: Boolean,
    onValueChange: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    labelColor: Color = Color.Black,
    getOptionLabel: (T) -> String,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = label,
            modifier = Modifier.weight(0.4f),
            fontSize = 12.sp,
            color = labelColor,
            fontFamily = poppins
        )

        // Right-side area
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, top = 1.dp, end = 2.dp, bottom = 1.dp)
        ) {

            // Dropdown field (anchor)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(45.dp)
                    .background(Color.White, RoundedCornerShape(4.dp))
                    .clickable(enabled = enabled) { onExpandedChange(true) },
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedValue.isEmpty()) "Select $label" else selectedValue,
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = if (enabled) Color.Black else Color.Gray
                        ),
                        modifier = Modifier.weight(1f),
                        fontFamily = poppins
                    )

                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Dropdown",
                        tint = if (enabled) Color.Black else Color.Gray
                    )
                }
            }

            // ✅ Menu: right aligned + half width (change width as you like)
            DropdownMenu(
                expanded = enabled && expanded,
                onDismissRequest = { onExpandedChange(false) },
                modifier = Modifier
                    .align(Alignment.TopEnd)   // ✅ right side
                    .width(180.dp)             // ✅ NOT full width (set your "half" here)
            ) {
                options.forEach { option ->
                    val optionLabel = getOptionLabel(option)
                    DropdownMenuItem(
                        text = { Text(optionLabel, fontFamily = poppins) },
                        onClick = {
                            onValueChange(optionLabel)
                            onExpandedChange(false)
                        }
                    )
                }
            }
        }
    }
}



/*
package com.loyalstring.rfid.ui.screens

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.local.entity.OrderItem
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.addSingleItem.BranchModel
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.ui.utils.GradientButtonIcon
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.OrderViewModel
import com.loyalstring.rfid.viewmodel.SingleProductViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

*/
/* ============================
   Helpers
   ============================ *//*

private fun asDouble(v: String?): Double = v?.trim()?.toDoubleOrNull() ?: 0.0
private fun fmt3(v: Double): String = String.format(Locale.getDefault(), "%.3f", v)
private fun fmt2(v: Double): String = String.format(Locale.getDefault(), "%.2f", v)

@SuppressLint("DefaultLocale")
@Composable
fun OrderDetailsDialogEditAndDisplay(
    selectedItem: OrderItem?,
    branchList: List<BranchModel>,
    onDismiss: () -> Unit,
    onSave: (OrderItem) -> Unit,
) {
    Log.e("TAG", "RFID Code: ${selectedItem?.rfidCode + " image url " + selectedItem?.grWt.toString()}")

    val orderViewModel: OrderViewModel = hiltViewModel()
    val singleProductViewModel: SingleProductViewModel = hiltViewModel()
    val context = LocalContext.current
    val employee = UserPreferences.getInstance(context).getEmployee(Employee::class.java)

    */
/* ============================
       Load Master Data (ONCE)
       ============================ *//*

    LaunchedEffect(employee?.clientCode) {
        withContext(Dispatchers.IO) {
            val cc = employee?.clientCode.toString()
            orderViewModel.getAllEmpList(cc)
            orderViewModel.getAllItemCodeList(ClientCodeRequest(cc))
            singleProductViewModel.getAllBranches(ClientCodeRequest(cc))
            singleProductViewModel.getAllPurity(ClientCodeRequest(cc))
            singleProductViewModel.getAllSKU(ClientCodeRequest(cc))
            orderViewModel.getDailyRate(ClientCodeRequest(cc))
        }
    }

    // If UI passed branchList empty, try fetching again (fallback)
    LaunchedEffect(branchList.size) {
        if (branchList.isEmpty()) {
            withContext(Dispatchers.IO) {
                singleProductViewModel.getAllBranches(ClientCodeRequest(employee?.clientCode.toString()))
            }
        }
    }

    */
/* ============================
       Date helpers
       ============================ *//*

    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val inputFormats = remember {
        listOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
            SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        )
    }

    fun formatDateSafe(dateString: String?): String {
        if (dateString.isNullOrBlank() || dateString.equals("null", true)) return ""
        for (format in inputFormats) {
            try {
                val parsed = format.parse(dateString)
                if (parsed != null) return dateFormatter.format(parsed)
            } catch (_: Exception) {
            }
        }
        return ""
    }

    */
/* ============================
       Form State
       ============================ *//*

    var selectedBranchId by remember { mutableStateOf(selectedItem?.branchId ?: "") }
    var branch by remember { mutableStateOf("") }
    var exhibition by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }
    var purity by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("") }
    var length by remember { mutableStateOf("") }
    var typeOfColors by remember { mutableStateOf("") }
    var screwType by remember { mutableStateOf("") }
    var polishType by remember { mutableStateOf("") }
    var finePercentage by remember { mutableStateOf("") }
    var wastage by remember { mutableStateOf("") }
    var orderDate by remember { mutableStateOf("") }
    var deliverDate by remember { mutableStateOf("") }

    var productName by remember { mutableStateOf("") }
    var itemCode by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }

    // Weights
    var totalWt by remember { mutableStateOf("") }
    var packingWt by remember { mutableStateOf("") }
    var grossWT by remember { mutableStateOf("") }
    var stoneWt by remember { mutableStateOf("") }
    var dimondWt by remember { mutableStateOf("") }
    var NetWt by remember { mutableStateOf("") }

    // Pricing
    var ratePerGRam by remember { mutableStateOf("") }
    var hallMarkAmt by remember { mutableStateOf("") }
    var mrp by remember { mutableStateOf("") }
    var itemAmt by remember { mutableStateOf("") }

    // Calculated
    var finePlusWt by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }

    // Not used in net-wt formula (amount, not weight) -> keep if needed elsewhere
    var stoneAmt by remember { mutableStateOf("") }

    */
/* ============================
       Unified Recalculation
       GrossWt = TotalWt - PackingWt
       NetWt   = GrossWt - StoneWt - DiamondWt
       ItemAmt = (NetWt * RatePerGram) + HallMarkAmt  (unless MRP > 0)
       FinePlusWt = NetWt * (Fine% + Wastage%) / 100
       ============================ *//*

    fun recalcAll() {
        val totalParsed = totalWt.trim().toDoubleOrNull()
        val packing = asDouble(packingWt)
        val grossFromRow = grossWT.trim().toDoubleOrNull() ?: 0.0

        // ✅ Use total only if it's a valid positive number
        val gross = if (totalParsed != null && totalParsed > 0.0) {
            (totalParsed - packing).coerceAtLeast(0.0)
        } else {
            // fallback to already existing grossWT (selectedItem.grWt)
            grossFromRow.coerceAtLeast(0.0)
        }

        grossWT = fmt3(gross)

        val stone = asDouble(stoneWt)
        val diamond = asDouble(dimondWt)
        val net = (gross - stone - diamond).coerceAtLeast(0.0)
        NetWt = fmt3(net)

        val fineP = asDouble(finePercentage)
        val wastP = asDouble(wastage)
        finePlusWt = fmt3((net * ((fineP + wastP) / 100.0)).coerceAtLeast(0.0))

        val rate = asDouble(ratePerGRam)
        val hallmark = asDouble(hallMarkAmt)
        val baseAmt = (net * rate) + hallmark

        val mrpVal = asDouble(mrp)
        itemAmt = if (mrpVal > 0) fmt2(mrpVal) else fmt2(baseAmt)
    }



    */
/* ============================
       Init from selectedItem
       ============================ *//*

    LaunchedEffect(selectedItem) {
        if (selectedItem == null) return@LaunchedEffect

        selectedBranchId = selectedItem.branchId ?: ""
        branch = selectedItem.branchName?.takeIf { !it.equals("null", true) } ?: ""

        productName = selectedItem.productName.orEmpty()
        itemCode = selectedItem.itemCode.orEmpty()
        sku = selectedItem.sku.orEmpty()

        totalWt = selectedItem.totalWt.orEmpty()
        packingWt = selectedItem.packingWt?.takeIf { !it.equals("null", true) } ?: ""
        grossWT = selectedItem.grWt.orEmpty()

        stoneWt = selectedItem.stoneWt.orEmpty()
        dimondWt = selectedItem.dimondWt?.takeIf { !it.equals("null", true) } ?: ""
        NetWt = selectedItem.nWt.orEmpty()

        exhibition = selectedItem.exhibition.orEmpty()
        purity = selectedItem.purity.orEmpty()
        size = selectedItem.size?.takeIf { !it.equals("null", true) } ?: ""
        length = selectedItem.length.orEmpty()
        stoneAmt = selectedItem.stoneAmt.orEmpty()

        remark = selectedItem.remark.orEmpty()
        typeOfColors = selectedItem.typeOfColor?.takeIf { !it.equals("null", true) } ?: "Select color"
        screwType = selectedItem.screwType.orEmpty()
        polishType = selectedItem.polishType.orEmpty()

        finePercentage = selectedItem.finePer?.takeIf { !it.equals("null", true) } ?: ""
        wastage = selectedItem.makingPercentage?.takeIf { !it.equals("null", true) } ?: ""

        orderDate = formatDateSafe(selectedItem.orderDate)
        deliverDate = formatDateSafe(selectedItem.deliverDate)

        hallMarkAmt = selectedItem.hallmarkAmt.orEmpty()
        mrp = selectedItem.mrp.orEmpty()

        Log.d("","hallMarkAmt"+hallMarkAmt+" "+mrp+" "+wastage)

        // rate: try selected rate; later dailyRate will override when purity matches
        ratePerGRam = selectedItem.todaysRate.orEmpty()

        qty = when {
            selectedItem.qty.isNullOrBlank() -> "1"
            selectedItem.qty.equals("null", true) -> "1"
            selectedItem.qty == "0" -> "1"
            else -> selectedItem.qty.orEmpty()
        }

        recalcAll()
    }

    */
/* ============================
       Default OrderDate = Today (only if empty)
       ============================ *//*

    val calendar = remember { Calendar.getInstance() }
    LaunchedEffect(orderDate) {
        if (orderDate.isBlank()) {
            orderDate = dateFormatter.format(calendar.time)
        }
    }

    */
/* ============================
       Daily Rate -> set rate by purity
       ============================ *//*

    val dailyRates by orderViewModel.getAllDailyRate.collectAsState()
    LaunchedEffect(purity, dailyRates) {
        if (purity.isNotBlank() && dailyRates.isNotEmpty()) {
            val match = dailyRates.find { it.PurityName.equals(purity, ignoreCase = true) }
            val newRate = match?.Rate ?: ""
            if (newRate.isNotBlank()) {
                ratePerGRam = newRate
                recalcAll()
            }
        }
    }

    */
/* ============================
       Dropdown data
       ============================ *//*

    val purityList by singleProductViewModel.purityResponse1.collectAsState()
    val skuList by singleProductViewModel.skuResponse1.collectAsState()

    val colorsList = listOf(
        "Yellow Gold",
        "White Gold",
        "Rose Gold",
        "Green Gold",
        "Black Gold",
        "Blue Gold",
        "Purple Gold"
    )
    val screwList = listOf("Type 1", "Type 2", "Type 3")
    val polishList = listOf("High Polish", "Matte Finish", "Satin Finish", "Hammered")

    val baseUrl = "https://rrgold.loyalstring.co.in/"

    var expandedBranch by remember { mutableStateOf(false) }
    var expandedPurity by remember { mutableStateOf(false) }
    var expandedColors by remember { mutableStateOf(false) }
    var expandedScrew by remember { mutableStateOf(false) }
    var expandedPolish by remember { mutableStateOf(false) }
    var expandedSKU by remember { mutableStateOf(false) }

    LaunchedEffect(
        totalWt, packingWt, grossWT,
        stoneWt, dimondWt,
        finePercentage, wastage,
        ratePerGRam, hallMarkAmt, mrp
    ) {
        recalcAll()
    }

    Dialog(onDismissRequest = { onDismiss() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Title Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.order_edit_icon),
                            contentDescription = "Custom Order Icon",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Custom Order Fields",
                            fontSize = 18.sp,
                            color = Color.White,
                            fontFamily = poppins
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .imePadding()
                        .padding(8.dp)
                )

               */
/* Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                )*//*
 {
                    // Image
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        AsyncImage(
                            model = baseUrl + (selectedItem?.image ?: ""),
                            contentDescription = "Image from URL",
                            placeholder = painterResource(R.drawable.add_photo),
                            error = painterResource(R.drawable.add_photo),
                            modifier = Modifier.size(100.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Branch Dropdown (also updates branchId)
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        color = Color(0xFFF2F2F3),
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (branchList.isNotEmpty()) {
                                DropdownMenuFieldDisplay(
                                    label = "Branch",
                                    options = branchList,
                                    selectedValue = branch,
                                    expanded = expandedBranch,
                                    onValueChange = { selectedLabel ->
                                        branch = selectedLabel
                                        val b = branchList.firstOrNull { it.BranchName == selectedLabel }
                                        selectedBranchId = (b?.Id ?: selectedBranchId).toString()
                                    },
                                    onExpandedChange = { expandedBranch = it },
                                    getOptionLabel = { it.BranchName },
                                    enabled = true
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Product Name (readonly)
                    FieldRowReadOnly(label = "Product Name", value = productName)

                    Spacer(modifier = Modifier.height(4.dp))

                    // Item Code (readonly)
                    FieldRowReadOnly(label = "ItemCode", value = itemCode)

                    Spacer(modifier = Modifier.height(4.dp))

                    // Total Weight (editable) -> affects Gross, Net, Amount
                    FieldRowInput(
                        label = "Total Weight",
                        value = totalWt,
                        placeholder = "Enter total wt",
                        onChange = {
                            totalWt = it
                            //recalcAll()
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Packing Wt (editable) -> affects Gross, Net, Amount
                    FieldRowInput(
                        label = "Packing Wt",
                        value = packingWt,
                        placeholder = "Enter packing wt",
                        onChange = {
                            packingWt = it
                           // recalcAll()
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Gross Wt (editable) -> if user changes gross directly, we recompute net from it.
                    FieldRowInput(
                        label = "Gross Wt",
                        value = grossWT,
                        placeholder = "Enter gross wt",
                        onChange = {
                            grossWT = it
                         //   recalcAll()
                           // if gross manually edited, recompute net using gross (not total/packing)
                            val gross = asDouble(grossWT)
                            val stone = asDouble(stoneWt)
                            val diamond = asDouble(dimondWt)
                            val net = (gross - stone - diamond).coerceAtLeast(0.0)
                            NetWt = fmt3(net)

                            // update amount + fine+wt from net
                            val fineP = asDouble(finePercentage)
                            val wastP = asDouble(wastage)
                            finePlusWt = fmt3(net * ((fineP + wastP) / 100.0))

                            val rate = asDouble(ratePerGRam)
                            val hallmark = asDouble(hallMarkAmt)
                            val baseAmt = (net * rate) + hallmark
                            val mrpVal = asDouble(mrp)
                            itemAmt = if (mrpVal > 0) fmt2(mrpVal) else fmt2(baseAmt)
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Stone Weight
                    FieldRowInput(
                        label = "Stone Weight",
                        value = stoneWt,
                        placeholder = "Enter stone wt",
                        onChange = {
                            stoneWt = it
                          //  recalcAll()
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Diamond Weight
                    FieldRowInput(
                        label = "Dimond Weight",
                        value = dimondWt,
                        placeholder = "Enter dimond wt",
                        onChange = {
                            dimondWt = it
                           // recalcAll()
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Net Wt (readonly)
                    FieldRowDisplay(label = "Net Wt", value = if (NetWt.isBlank()) "0.000" else NetWt)

                    Spacer(modifier = Modifier.height(4.dp))

                    // Rate/Gram
                    FieldRowInput(
                        label = "Rate/Gram",
                        value = ratePerGRam,
                        placeholder = "Enter Rate/gram",
                        onChange = {
                            ratePerGRam = it
                           // recalcAll()
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Exhibition
                    FieldRowInput(
                        label = "Exhibition",
                        value = exhibition,
                        placeholder = "Enter exhibition",
                        onChange = { exhibition = it }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Remark
                    FieldRowInput(
                        label = "Remark",
                        value = remark,
                        placeholder = "Enter remark",
                        onChange = { remark = it }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // SKU dropdown (disabled)
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        color = Color(0xFFF2F2F3),
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            DropdownMenuFieldDisplay(
                                label = "SKU",
                                options = skuList,
                                selectedValue = sku,
                                expanded = expandedSKU,
                                onValueChange = { sku = it },
                                onExpandedChange = { expandedSKU = it },
                                getOptionLabel = { it.PurityName.toString() }, // keep your mapping
                                enabled = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Purity dropdown
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        color = Color(0xFFF2F2F3),
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            DropdownMenuFieldDisplay(
                                label = "Purity",
                                options = purityList,
                                selectedValue = purity,
                                expanded = expandedPurity,
                                onValueChange = {
                                    purity = it
                                    // rate auto updates via LaunchedEffect(purity, dailyRates)
                                },
                                onExpandedChange = { expandedPurity = it },
                                getOptionLabel = { it.PurityName },
                                enabled = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Size
                    FieldRowInput(
                        label = "Size",
                        value = size,
                        placeholder = "Enter size",
                        onChange = { size = it }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Length
                    FieldRowInput(
                        label = "Length",
                        value = length,
                        placeholder = "Enter length",
                        onChange = { length = it }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Colors
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        color = Color(0xFFF2F2F3),
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            DropdownMenuFieldDisplay(
                                label = "Colors",
                                options = colorsList,
                                selectedValue = typeOfColors,
                                expanded = expandedColors,
                                onValueChange = { typeOfColors = it },
                                onExpandedChange = { expandedColors = it },
                                getOptionLabel = { it.toString() },
                                enabled = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Screw Type
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        color = Color(0xFFF2F2F3),
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            DropdownMenuFieldDisplay(
                                label = "Screw Type",
                                options = screwList,
                                selectedValue = screwType,
                                expanded = expandedScrew,
                                onValueChange = { screwType = it },
                                onExpandedChange = { expandedScrew = it },
                                getOptionLabel = { it.toString() },
                                enabled = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Polish Type
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        color = Color(0xFFF2F2F3),
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            DropdownMenuFieldDisplay(
                                label = "Polish Type",
                                options = polishList,
                                selectedValue = polishType,
                                expanded = expandedPolish,
                                onValueChange = { polishType = it },
                                onExpandedChange = { expandedPolish = it },
                                getOptionLabel = { it.toString() },
                                enabled = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Fine %
                    FieldRowInput(
                        label = "Fine %",
                        value = finePercentage,
                        placeholder = "Enter Fine %",
                        onChange = {
                            finePercentage = it
                          //  recalcAll()
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Wastage
                    FieldRowInput(
                        label = "Wastage",
                        value = wastage,
                        placeholder = "Enter wastage",
                        onChange = {
                            wastage = it
                           // recalcAll()
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Order Date (picker)
                    DateRow(
                        label = "Order Date",
                        value = orderDate,
                        placeholder = "Enter Order Date",
                        onPick = { picked -> orderDate = picked }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Deliver Date (picker, min=orderDate)
                    DateRow(
                        label = "Deliver Date",
                        value = deliverDate,
                        placeholder = "Enter Deliver Date",
                        minDateYmd = orderDate,
                        onPick = { picked -> deliverDate = picked }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Quantity
                    FieldRowInput(
                        label = "Quantity",
                        value = qty,
                        placeholder = "Enter quantity",
                        onChange = { qty = it }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Hallmark Amt
                    FieldRowInput(
                        label = "Hallmark Amt",
                        value = hallMarkAmt,
                        placeholder = "Enter hallmark amt",
                        onChange = {
                            hallMarkAmt = it
                           // recalcAll()
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // MRP (if >0 overrides itemAmt)
                    FieldRowInput(
                        label = "Mrp",
                        value = mrp,
                        placeholder = "Enter mrp",
                        onChange = {
                            mrp = it
                          //  recalcAll()
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Optional: Show calculated values (if you want)
                    FieldRowDisplay(label = "Fine+Wastage Wt", value = if (finePlusWt.isBlank()) "0.000" else finePlusWt)
                    Spacer(modifier = Modifier.height(4.dp))
                    FieldRowDisplay(label = "Item Amount", value = if (itemAmt.isBlank()) "0.00" else itemAmt)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    GradientButtonIcon(
                        text = "Cancel",
                        onClick = { onDismiss() },
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .padding(start = 8.dp, bottom = 16.dp),
                        icon = painterResource(id = R.drawable.ic_cancel),
                        iconDescription = "Cancel Icon",
                        fontSize = 12
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    GradientButtonIcon(
                        text = "OK",
                        onClick = {
                            if (deliverDate.isBlank()) {
                                Toast.makeText(context, "Please enter Delivery Date", Toast.LENGTH_SHORT).show()
                                return@GradientButtonIcon
                            }

                            // Ensure latest calc before save
                            recalcAll()

                            val orderItem = OrderItem(
                                branchId = selectedBranchId,
                                branchName = branch,
                                exhibition = exhibition,
                                remark = remark,
                                purity = purity,
                                size = size,
                                length = length,
                                typeOfColor = typeOfColors,
                                screwType = screwType,
                                polishType = polishType,
                                finePer = finePercentage,
                                wastage = wastage,
                                orderDate = orderDate,
                                deliverDate = deliverDate,

                                productName = productName,
                                itemCode = itemCode,
                                rfidCode = selectedItem?.rfidCode.orEmpty(),

                                // saved values
                                totalWt = totalWt,
                                packingWt = packingWt,
                                grWt = grossWT,
                                stoneWt = stoneWt,
                                dimondWt = dimondWt,
                                nWt = NetWt,

                                //todaysRate = ratePerGRam, // (if your entity has it; else remove)
                                todaysRate = ratePerGRam,  // ✅ today's rate = rate/gram (not itemAmt)

                                hallmarkAmt = hallMarkAmt,
                                mrp = mrp,
                                itemAmt = itemAmt,

                                finePlusWt = finePlusWt,
                                stoneAmt = stoneAmt,
                                sku = sku,
                                qty = qty,

                                image = selectedItem?.image.orEmpty(),
                                netAmt = "",

                                diamondAmt = selectedItem?.diamondAmt.orEmpty(),

                                categoryId = selectedItem?.categoryId ?: 0,
                                categoryName = selectedItem?.categoryName.orEmpty(),
                                productId = selectedItem?.productId ?: 0,
                                productCode = selectedItem?.productName.orEmpty(),
                                skuId = selectedItem?.skuId ?: 0,
                                designid = selectedItem?.designid ?: 0,
                                designName = selectedItem?.designName.orEmpty(),
                                purityid = selectedItem?.purityid ?: 0,
                                counterId = selectedItem?.counterId ?: 0,
                                counterName = "",

                                companyId = 0,
                                epc = selectedItem?.epc ?: "",
                                tid = selectedItem?.tid ?: "",

                                makingPercentage = wastage,
                                makingFixedAmt = selectedItem?.makingFixedAmt.orEmpty(),
                                makingFixedWastage = selectedItem?.makingFixedWastage.orEmpty(),
                                makingPerGram = selectedItem?.makingPerGram.orEmpty()
                            )
                            Log.d("","hallMarkAmt"+hallMarkAmt+" "+mrp+" "+wastage)
                            orderViewModel.insertOrderItemToRoomORUpdate(orderItem)
                            onSave(orderItem)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .padding(end = 8.dp, bottom = 16.dp),
                        icon = painterResource(id = R.drawable.check_circle),
                        iconDescription = "Check Icon",
                        fontSize = 12
                    )
                }
            }
        }
    }
}

*/
/* ============================
   Reusable UI Rows
   ============================ *//*


@Composable
private fun FieldRowReadOnly(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.4f).padding(start = 2.dp),
            fontSize = 12.sp,
            color = Color.Black,
            fontFamily = poppins
        )
        Box(
            modifier = Modifier
                .weight(0.9f)
                .padding(start = 6.dp, top = 4.dp, end = 2.dp, bottom = 4.dp)
                .height(32.dp)
                .background(Color.White, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                readOnly = true,
                onValueChange = {},
                singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp, color = Color.Black),
                modifier = Modifier.fillMaxWidth().padding(2.dp)
            )
        }
    }
}

@Composable
private fun FieldRowDisplay(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.4f).padding(start = 2.dp),
            fontSize = 12.sp,
            color = Color.Black,
            fontFamily = poppins
        )
        Box(
            modifier = Modifier
                .weight(0.9f)
                .padding(start = 6.dp, top = 4.dp, end = 2.dp, bottom = 4.dp)
                .height(32.dp)
                .background(Color.White, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = value,
                fontSize = 13.sp,
                color = Color.Black,
                modifier = Modifier.padding(start = 4.dp),
                fontFamily = poppins
            )
        }
    }
}

@Composable
private fun FieldRowInput(
    label: String,
    value: String,
    placeholder: String,
    onChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.4f).padding(start = 2.dp),
            fontSize = 12.sp,
            color = Color.Black,
            fontFamily = poppins
        )
        Box(
            modifier = Modifier
                .weight(0.9f)
                .padding(start = 6.dp, top = 4.dp, end = 2.dp, bottom = 4.dp)
                .height(32.dp)
                .background(Color.White, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 4.dp),
                    fontFamily = poppins
                )
            }

            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp, color = Color.Black),
                modifier = Modifier.fillMaxWidth().padding(2.dp)
            )
        }
    }
}

@Composable
private fun DateRow(
    label: String,
    value: String,
    placeholder: String,
    minDateYmd: String? = null, // if provided => minDate = this date
    onPick: (String) -> Unit,
) {
    val context = LocalContext.current
    val cal = remember { Calendar.getInstance() }
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.4f).padding(start = 2.dp),
            fontSize = 12.sp,
            color = Color.Black,
            fontFamily = poppins
        )

        Box(
            modifier = Modifier
                .weight(0.9f)
                .padding(start = 6.dp, top = 4.dp, end = 2.dp, bottom = 4.dp)
                .height(32.dp)
                .background(Color.White, RoundedCornerShape(4.dp))
                .clickable {
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val selectedDate = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                            onPick(dateFormatter.format(selectedDate.time))
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).apply {
                        // minDate logic
                        val minCal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        if (!minDateYmd.isNullOrBlank()) {
                            try {
                                val parsed = dateFormatter.parse(minDateYmd)
                                if (parsed != null) minCal.time = parsed
                            } catch (_: Exception) {
                            }
                        } else {
                            // default => today
                            // (already set to today 00:00)
                        }

                        datePicker.minDate = minCal.timeInMillis
                    }.show()
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (value.isBlank()) placeholder else value,
                    fontSize = 13.sp,
                    color = if (value.isBlank()) Color.Gray else Color.Black,
                    modifier = Modifier.weight(1f),
                    fontFamily = poppins
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_calender),
                    contentDescription = "Calendar",
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

*/
/* ============================
   Dropdown Menu Field
   - FIXED: clickable only if enabled
   - FIXED: menu only opens if enabled
   ============================ *//*

@Composable
fun <T> DropdownMenuFieldDisplay(
    label: String,
    options: List<T>,
    selectedValue: String,
    expanded: Boolean,
    onValueChange: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    labelColor: Color = Color.Black,
    getOptionLabel: (T) -> String,
    enabled: Boolean
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(0.4f),
                fontSize = 12.sp,
                color = labelColor,
                fontFamily = poppins
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, top = 1.dp, end = 2.dp, bottom = 1.dp)
                    .height(45.dp)
                    .background(Color.White, RoundedCornerShape(4.dp))
                    .clickable(enabled = enabled) { onExpandedChange(true) },
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedValue.isEmpty()) "Select $label" else selectedValue,
                        style = TextStyle(fontSize = 12.sp, color = if (enabled) Color.Black else Color.Gray),
                        modifier = Modifier.weight(1f),
                        fontFamily = poppins
                    )

                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Dropdown",
                        tint = if (enabled) Color.Black else Color.Gray
                    )
                }
            }

            DropdownMenu(
                expanded = enabled && expanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                options.forEach { option ->
                    val optionLabel = getOptionLabel(option)
                    DropdownMenuItem(
                        text = { Text(optionLabel, fontFamily = poppins) },
                        onClick = {
                            onValueChange(optionLabel)
                            onExpandedChange(false)
                        }
                    )
                }
            }
        }
    }
}
*/
