package com.loyalstring.rfid.ui.screens

import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.sparklepos.models.loginclasses.customerBill.EmployeeList
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.addSingleItem.BranchModel
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.model.quotation.QuotationItem
import com.loyalstring.rfid.ui.utils.GradientButtonIcon
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.ui.utils.resolveProductImageUrl
import com.loyalstring.rfid.viewmodel.OrderViewModel
import com.loyalstring.rfid.viewmodel.SingleProductViewModel
import com.loyalstring.rfid.viewmodel.UiState
import com.loyalstring.rfid.worker.LocaleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/* ============================
   Helpers (same as Delivery Challan dialog)
   ============================ */
private fun asDouble(v: String?): Double = v?.trim()?.toDoubleOrNull() ?: 0.0
private fun fmt3(v: Double): String = String.format(Locale.getDefault(), "%.3f", v)
private fun fmt2(v: Double): String = String.format(Locale.getDefault(), "%.2f", v)
private fun cleanStr(v: String?): String =
    v?.takeIf { it.isNotBlank() && !it.equals("null", true) } ?: ""

@Composable
fun QuotationDialogEditAndDisplay(
    selectedItem: QuotationItem?,
    branchList: List<BranchModel>,
    salesmanList: UiState<List<EmployeeList>>,
    onDismiss: () -> Unit,
    edit: Int = 0,
    onSave: (QuotationItem) -> Unit,
    orderViewModel: OrderViewModel = hiltViewModel(),
    singleProductViewModel: SingleProductViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val userPreferences = UserPreferences.getInstance(context)
    val savedLang = userPreferences.getAppLanguage().ifBlank { "en" }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = currentLocales[0]?.language ?: savedLang
    val localizedContext = LocaleHelper.applyLocale(context, currentLang)

    val employee = UserPreferences.getInstance(context).getEmployee(Employee::class.java)

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                val cc = employee?.clientCode.toString()
                orderViewModel.getAllEmpList(cc)
                orderViewModel.getAllItemCodeList(ClientCodeRequest(cc))
                singleProductViewModel.getAllBranches(ClientCodeRequest(cc))
                singleProductViewModel.getAllPurity(ClientCodeRequest(cc))
                singleProductViewModel.getAllSKU(ClientCodeRequest(cc))
                orderViewModel.getDailyRate(ClientCodeRequest(cc))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /* ============================
       State
       ============================ */
    var stoneAmt by remember { mutableStateOf("") }
    var diamondAmt by remember { mutableStateOf("") }
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

    var productName by remember { mutableStateOf("") }
    var itemCode by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }

    // Weights
    var totalWt by remember { mutableStateOf("") }
    var packingWt by remember { mutableStateOf("") }
    var grossWT by remember { mutableStateOf("") }
    var stoneWt by remember { mutableStateOf("") }
    var dimondWt by remember { mutableStateOf("") }
    var netWt by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf(0) }

    // Pricing
    var ratePerGRam by remember { mutableStateOf("") }
    var hallMarkAmt by remember { mutableStateOf("") }
    var mrp by remember { mutableStateOf("") }
    var itemAmt by remember { mutableStateOf("") }

    // Calculated
    var finePlusWt by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }



    /* ============================
       Calculation (SAME as Delivery Challan dialog)
       GrossWt = TotalWt - PackingWt   (only if TotalWt > 0)
       NetWt   = GrossWt - StoneWt - DiamondWt
       ItemAmt = (NetWt * RatePerGram) + HallMarkAmt  (unless MRP > 0)
       FinePlusWt = NetWt * (Fine% + Wastage%) / 100
       ============================ */
    fun recalcAll() {
        val totalParsed = totalWt.trim().toDoubleOrNull()
        val useTotal = totalParsed != null && totalParsed > 0.0 // IMPORTANT: 0 ko ignore

        val packing = asDouble(packingWt)
        val grossFromField = asDouble(grossWT)

        val gross = if (useTotal) {
            (totalParsed!! - packing).coerceAtLeast(0.0)
        } else {
            grossFromField.coerceAtLeast(0.0)
        }

        // Only overwrite grossWT when derived from TotalWt
        if (useTotal) grossWT = fmt3(gross)

        val stone = asDouble(stoneWt)
        val diamond = asDouble(dimondWt)
        val net = (gross - stone - diamond).coerceAtLeast(0.0)
        netWt = fmt3(net)

        val fineP = asDouble(finePercentage)
        val wastP = asDouble(wastage)
        finePlusWt = fmt3((net * ((fineP + wastP) / 100.0)).coerceAtLeast(0.0))

        val rate = asDouble(ratePerGRam)
        val hallmark = asDouble(hallMarkAmt)
        val stoneAmount = asDouble(stoneAmt)
        val diamondAmount = asDouble(diamondAmt)

        val baseAmt = (net * rate) + hallmark + stoneAmount + diamondAmount

        val mrpVal = asDouble(mrp)
        itemAmt = if (mrpVal > 0) fmt2(mrpVal) else fmt2(baseAmt)
    }

    fun recalcFromGrossOnly() {
        val gross = asDouble(grossWT).coerceAtLeast(0.0)
        val stone = asDouble(stoneWt)
        val diamond = asDouble(dimondWt)
        val net = (gross - stone - diamond).coerceAtLeast(0.0)
        netWt = fmt3(net)

        val fineP = asDouble(finePercentage)
        val wastP = asDouble(wastage)
        finePlusWt = fmt3((net * ((fineP + wastP) / 100.0)).coerceAtLeast(0.0))

        val rate = asDouble(ratePerGRam)
        val hallmark = asDouble(hallMarkAmt)
        val stoneAmount = asDouble(stoneAmt)
        val diamondAmount = asDouble(diamondAmt)

        val baseAmt = (net * rate) + hallmark + stoneAmount + diamondAmount

        val mrpVal = asDouble(mrp)
        itemAmt = if (mrpVal > 0) fmt2(mrpVal) else fmt2(baseAmt)
    }

    /* ============================
       Init from selectedItem (IMPORTANT: LaunchedEffect only)
       ============================ */
    LaunchedEffect(selectedItem) {
        val s = selectedItem ?: return@LaunchedEffect


        productName = cleanStr(s.ProductName)
        itemCode = cleanStr(s.ItemCode)
        sku = cleanStr(s.SKU)

        totalWt = cleanStr(s.TotalWt)
        packingWt = cleanStr(s.PackingWeight)
        grossWT = cleanStr(s.GrossWt)
        stoneWt = cleanStr(s.TotalStoneWeight)
        dimondWt = cleanStr(s.DiamondWeight)
        netWt = cleanStr(s.NetWt)
        categoryId= s.CategoryId?: 0
        purity = cleanStr(s.Purity)
        size = cleanStr(s.Size)
        length = ""

        typeOfColors = cleanStr(s.DiamondColour)
        screwType = ""
        polishType = ""

        // Quotation fields mapping (as per your model usage)
        finePercentage = cleanStr(s.FinePercentage)
        wastage = cleanStr(s.MakingFixedWastage)

        qty = s.Quantity?.toString() ?: ""
        hallMarkAmt = cleanStr(s.HallmarkAmount)
        mrp = cleanStr(s.MRP)

        // rate will come from DailyRate (purity-based), but keep existing if any
        ratePerGRam = cleanStr(s.RatePerGram).ifBlank { cleanStr(s.MetalRate) }

        stoneAmt = cleanStr(
            s.StoneAmt
                ?: s.StoneAmount
                ?: s.TotalStoneAmount
                ?: "0.00"
        )

        diamondAmt = cleanStr(
            s.DiamondAmt
                ?: s.TotalDiamondAmount
                ?: s.DiamondSellAmount
                ?: s.DiamondPurchaseAmount
                ?: "0.00"
        )

        // initial calc
        recalcAll()
    }

    /* ============================
       Daily Rate -> auto set rate by purity (same behavior)
       ============================ */
    val dailyRates by orderViewModel.getAllDailyRate.collectAsState(initial = emptyList())
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

    // purity list from vm
    val purityList by singleProductViewModel.purityResponse1.collectAsState()
    val purityNames = purityList
        .filter { (it.CategoryId ?: 0) == categoryId }
        .mapNotNull { it.PurityName }
        .distinct()

    // dropdown states
    var expandedPurity by remember { mutableStateOf(false) }
    var expandedColors by remember { mutableStateOf(false) }
    var expandedScrew by remember { mutableStateOf(false) }
    var expandedPolish by remember { mutableStateOf(false) }

    val colorsList = listOf(
        "Yellow Gold", "White Gold", "Rose Gold", "Green Gold",
        "Black Gold", "Blue Gold", "Purple Gold"
    )
    val screwList = listOf("Type 1", "Type 2", "Type 3")
    val polishList = listOf("High Polish", "Matte Finish", "Satin Finish", "Hammered")
    val baseUrl = "https://rrgold.loyalstring.co.in/"

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

                // ---------- HEADER ----------
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Color.DarkGray),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.order_edit_icon),
                            contentDescription = localizedContext.getString(R.string.cd_custom_order_icon),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = localizedContext.getString(R.string.title_custom_order_fields),
                            color = Color.White,
                            fontSize = 18.sp,
                            fontFamily = poppins
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // ---------- BODY ----------
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        AsyncImage(
                            model = resolveProductImageUrl(selectedItem?.Image, baseUrl) ?: "",
                            contentDescription = localizedContext.getString(R.string.cd_product_image),
                            placeholder = painterResource(R.drawable.add_photo),
                            error = painterResource(R.drawable.add_photo),
                            modifier = Modifier.size(110.dp)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    FieldRow(localizedContext.getString(R.string.label_product_name), productName,enabled = false) { productName = it }
                    Spacer(Modifier.height(4.dp))

                    FieldRow(localizedContext.getString(R.string.label_item_code), itemCode,enabled = false) { itemCode = it }
                    Spacer(Modifier.height(4.dp))

                    FieldRow(localizedContext.getString(R.string.label_sku), sku,enabled = false) { sku = it }
                    Spacer(Modifier.height(4.dp))

                    DropdownRow(
                        label = localizedContext.getString(R.string.label_purity),
                        list = purityNames,
                        selected = purity,
                        expanded = expandedPurity,
                        onSelect = { purity = it },
                        onExpand = { expandedPurity = it }
                    )

                    Spacer(Modifier.height(6.dp))

                    FieldRow(localizedContext.getString(R.string.label_total_weight), totalWt,enabled = true) { newVal ->
                        totalWt = newVal
                        recalcAll()
                    }
                    Spacer(Modifier.height(4.dp))

                    FieldRow(localizedContext.getString(R.string.label_packing_weight), packingWt,enabled = true) { newVal ->
                        packingWt = newVal
                        recalcAll()
                    }
                    Spacer(Modifier.height(4.dp))

                    // GrossWt manual edit
                    FieldRow(localizedContext.getString(R.string.label_gross_weight), grossWT,enabled = true) { newVal ->
                        grossWT = newVal
                        recalcFromGrossOnly()
                    }
                    Spacer(Modifier.height(4.dp))

                    FieldRow(localizedContext.getString(R.string.label_stone_weight), stoneWt,enabled = true) { newVal ->
                        stoneWt = newVal
                        recalcAll()
                    }
                    Spacer(Modifier.height(4.dp))

                    FieldRow(localizedContext.getString(R.string.label_diamond_weight), dimondWt,enabled = true) { newVal ->
                        dimondWt = newVal
                        recalcAll()
                    }
                    Spacer(Modifier.height(4.dp))

                    // NetWt display
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = localizedContext.getString(R.string.label_net_weight),
                            modifier = Modifier.weight(0.4f),
                            fontSize = 12.sp,
                            color = Color.Black,
                            fontFamily = poppins
                        )
                        Box(
                            modifier = Modifier
                                .weight(0.9f)
                                .height(35.dp)
                                .background(Color.White, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = if (netWt.isBlank()) "" else netWt,
                                fontSize = 13.sp,
                                fontFamily = poppins
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    FieldRow(localizedContext.getString(R.string.label_size), size,enabled = true) { size = it }
                    Spacer(Modifier.height(4.dp))

                    FieldRow(localizedContext.getString(R.string.label_length), length,enabled = true) { length = it }
                    Spacer(Modifier.height(4.dp))

                    DropdownRow(
                        label = localizedContext.getString(R.string.label_type_color),
                        list = colorsList,
                        selected = typeOfColors,
                        expanded = expandedColors,
                        onSelect = { typeOfColors = it },
                        onExpand = { expandedColors = it }
                    )
                    Spacer(Modifier.height(4.dp))

                    DropdownRow(
                        label = localizedContext.getString(R.string.label_screw_type),
                        list = screwList,
                        selected = screwType,
                        expanded = expandedScrew,
                        onSelect = { screwType = it },
                        onExpand = { expandedScrew = it }
                    )
                    Spacer(Modifier.height(4.dp))

                    DropdownRow(
                        label = localizedContext.getString(R.string.label_polish_type),
                        list = polishList,
                        selected = polishType,
                        expanded = expandedPolish,
                        onSelect = { polishType = it },
                        onExpand = { expandedPolish = it }
                    )
                    Spacer(Modifier.height(4.dp))

                    FieldRow(localizedContext.getString(R.string.label_rate_per_gram), ratePerGRam,enabled = true) { newVal ->
                        ratePerGRam = newVal
                        recalcAll()
                    }
                    Spacer(Modifier.height(4.dp))

                    FieldRow(localizedContext.getString(R.string.label_fine_percent), finePercentage,enabled = true) { newVal ->
                        finePercentage = newVal
                        recalcAll()
                    }
                    Spacer(Modifier.height(4.dp))

                    FieldRow(localizedContext.getString(R.string.label_wastage_percent), wastage,enabled = true) { newVal ->
                        wastage = newVal
                        recalcAll()
                    }
                    Spacer(Modifier.height(4.dp))

                    FieldRow(localizedContext.getString(R.string.label_quantity), qty,enabled = true) { qty = it }
                    Spacer(Modifier.height(4.dp))

                    FieldRow(localizedContext.getString(R.string.stone_amt), stoneAmt, enabled = true) { newVal ->
                        stoneAmt = newVal
                        recalcAll()
                    }
                    Spacer(Modifier.height(4.dp))

                    FieldRow(localizedContext.getString(R.string.diamond_amt), diamondAmt, enabled = true) { newVal ->
                        diamondAmt = newVal
                        recalcAll()
                    }
                    Spacer(Modifier.height(4.dp))

                    FieldRow(localizedContext.getString(R.string.label_hallmark_amount), hallMarkAmt,enabled = true) { newVal ->
                        hallMarkAmt = newVal
                        recalcAll()
                    }
                    Spacer(Modifier.height(4.dp))

                    FieldRow(localizedContext.getString(R.string.label_mrp), mrp,enabled = true) { newVal ->
                        mrp = newVal
                        recalcAll()
                    }

                    Spacer(Modifier.height(8.dp))

                    // Fine+Wastage Wt
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = localizedContext.getString(R.string.fine_wastage_wt),
                            modifier = Modifier.weight(0.4f),
                            fontSize = 12.sp,
                            color = Color.Black,
                            fontFamily = poppins
                        )
                        Box(
                            modifier = Modifier
                                .weight(0.9f)
                                .height(35.dp)
                                .background(Color.White, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = if (finePlusWt.isBlank()) "" else finePlusWt,
                                fontSize = 13.sp,
                                fontFamily = poppins
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Item Amount
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = localizedContext.getString(R.string.item_amount),
                            modifier = Modifier.weight(0.4f),
                            fontSize = 12.sp,
                            color = Color.Black,
                            fontFamily = poppins
                        )
                        Box(
                            modifier = Modifier
                                .weight(0.9f)
                                .height(35.dp)
                                .background(Color.White, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = if (itemAmt.isBlank()) "" else itemAmt,
                                fontSize = 13.sp,
                                fontFamily = poppins
                            )
                        }
                    }
                }

                // ---------- BUTTONS ----------
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    GradientButtonIcon(
                        text = localizedContext.getString(R.string.btn_cancel),
                        onClick = { onDismiss() },
                        icon = painterResource(id = R.drawable.ic_cancel),
                        iconDescription = "Cancel",
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .padding(end = 4.dp)
                    )

                    GradientButtonIcon(
                        text = localizedContext.getString(R.string.btn_save),
                        onClick = {
                            val s = selectedItem
                            if (s == null) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.msg_no_item_selected),
                                    Toast.LENGTH_SHORT
                                ).show()
                                onDismiss()
                                return@GradientButtonIcon
                            }

                            // final calculate before save
                            recalcAll()

                            val updated = s.copy(
                                TotalWt = totalWt,
                                PackingWeight = packingWt,
                                GrossWt = grossWT,
                                TotalStoneWeight = stoneWt,
                                DiamondWeight = dimondWt,
                                NetWt = netWt,

                                RatePerGram = ratePerGRam,
                                MetalRate = ratePerGRam,

                                HallmarkAmount = hallMarkAmt,
                                MRP = mrp,

                                // item amount targets (keep all as you used in challan dialog)
                                TotalItemAmount = itemAmt,
                                Amount = itemAmt,
                                TotalAmount = itemAmt,
                                itemAmt = itemAmt,

                                // fine+wastage
                                FineWastageWt = finePlusWt,
                                FinePercentage = finePercentage,
                                MakingFixedWastage = wastage,

                                Purity = purity,
                                Size = size,
                                DiamondColour = typeOfColors,
                                Description = remark,
                                Quantity = qty,
                                StoneAmount = stoneAmt,
                                TotalStoneAmount = stoneAmt,
                                StoneAmt = stoneAmt,

                                DiamondAmt = diamondAmt,
                                TotalDiamondAmount = diamondAmt,
                                DiamondSellAmount = diamondAmt,
                                DiamondPurchaseAmount = diamondAmt,
                            )

                            onSave(updated)
                            onDismiss()
                        },
                        icon = painterResource(id = R.drawable.check_circle),
                        iconDescription = "Save",
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

/* ============================================================
   NOTE:
   Reuse SAME FieldRow / DropdownRow composables you already have
   (no change needed).
   ============================================================ */
