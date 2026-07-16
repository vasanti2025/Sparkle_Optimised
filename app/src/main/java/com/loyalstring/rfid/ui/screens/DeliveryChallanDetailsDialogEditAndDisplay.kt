package com.loyalstring.rfid.ui.screens

import android.util.Log
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
import com.loyalstring.rfid.data.model.deliveryChallan.ChallanDetails
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.ui.utils.GradientButtonIcon
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.ui.utils.ProductImageWithAllFallbacks
import com.loyalstring.rfid.viewmodel.OrderViewModel
import com.loyalstring.rfid.viewmodel.SingleProductViewModel
import com.loyalstring.rfid.viewmodel.UiState
import com.loyalstring.rfid.worker.LocaleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/* ============================
   Helpers (same as first dialog)
   ============================ */
private fun asDouble(v: String?): Double = v?.trim()?.toDoubleOrNull() ?: 0.0
private fun fmt3(v: Double): String = String.format(Locale.getDefault(), "%.3f", v)
private fun fmt2(v: Double): String = String.format(Locale.getDefault(), "%.2f", v)

@Composable
fun DeliveryChallanDialogEditAndDisplay(
    selectedItem: ChallanDetails?,
    branchList: List<BranchModel>,
    salesmanList: UiState<List<EmployeeList>>,
    onDismiss: () -> Unit,
    edit: Int = 0,
    onSave: (ChallanDetails) -> Unit,
    orderViewModel: OrderViewModel = hiltViewModel(),
    singleProductViewModel: SingleProductViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }

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

    var diamondAmt by remember { mutableStateOf("") }
    var branch by remember { mutableStateOf("") }
    var exhibition by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf(0) }

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
    var NetWt by remember { mutableStateOf("") }

    // Pricing
    var ratePerGRam by remember { mutableStateOf("") }
    var hallMarkAmt by remember { mutableStateOf("") }
    var mrp by remember { mutableStateOf("") }
    var itemAmt by remember { mutableStateOf("") }

    // Calculated
    var finePlusWt by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }

    // keep if needed elsewhere
    var stoneAmt by remember { mutableStateOf("") }

    /* ============================
       Calculation (same as first dialog)
       GrossWt = TotalWt - PackingWt   (only if TotalWt exists)
       NetWt   = GrossWt - StoneWt - DiamondWt
       ItemAmt = (NetWt * RatePerGram) + HallMarkAmt  (unless MRP > 0)
       FinePlusWt = NetWt * (Fine% + Wastage%) / 100
       ============================ */
    fun recalcAll() {
        val totalParsed = totalWt.trim().toDoubleOrNull()
        val useTotal = totalParsed != null && totalParsed > 0.0   // ✅ IMPORTANT (0 ko ignore)

        val packing = asDouble(packingWt)
        val grossFromField = asDouble(grossWT)

        val gross = if (useTotal) {
            (totalParsed!! - packing).coerceAtLeast(0.0)
        } else {
            grossFromField.coerceAtLeast(0.0)
        }

        // ✅ Only overwrite grossWT if it is derived from total
        if (useTotal) grossWT = fmt3(gross)

        val stone = asDouble(stoneWt)
        val diamond = asDouble(dimondWt)
        val net = (gross - stone - diamond).coerceAtLeast(0.0)
        NetWt = fmt3(net)

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


    // If user edits GrossWt manually: DON'T format/override grossWT while typing
    fun recalcFromGrossOnly() {
        val gross = asDouble(grossWT).coerceAtLeast(0.0)
        val stone = asDouble(stoneWt)
        val diamond = asDouble(dimondWt)
        val net = (gross - stone - diamond).coerceAtLeast(0.0)
        NetWt = fmt3(net)

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
       Init from selectedItem (FIXED)
       - do NOT assign directly in composition, it breaks editing
       ============================ */
    LaunchedEffect(selectedItem) {
        val s = selectedItem ?: return@LaunchedEffect

        productName = s.ProductName.orEmpty()
        itemCode = s.ItemCode.orEmpty()
        sku = s.SKU.orEmpty()

        totalWt = s.TotalWt.orEmpty()
        packingWt = s.PackingWeight.orEmpty()
        grossWT = s.GrossWt.orEmpty()
        stoneWt = s.TotalStoneWeight.orEmpty()
        dimondWt = s.DiamondWeight.orEmpty()
        NetWt = s.NetWt.orEmpty()

        purity = s.Purity.orEmpty()
        size = s.Size.orEmpty()
        length = "" // no backing field in model

        typeOfColors = s.DiamondColour.orEmpty()
        screwType = ""
        polishType = ""
        finePercentage = s.FinePer.orEmpty()
        wastage = s.fixWastage.orEmpty()

        qty = s.qty.toString()
        hallMarkAmt = s.HallmarkAmount.orEmpty()
        mrp = s.MRP.orEmpty()
        ratePerGRam = s.totayRate.orEmpty()
        stoneAmt = s.StoneAmt
            ?: s.StoneAmount
                    ?: s.TotalStoneAmount
                    ?: "0.00"

        diamondAmt = s.DiamondAmt
            ?: s.DiamondAmt
                    ?: s.TotalDiamondAmount
                    ?: s.DiamondSellAmount
                    ?: s.DiamondPurchaseAmount
                    ?: "0.00"
        categoryId = s.CategoryId ?: 0

        // initial calc
        recalcAll()
    }

    /* ============================
       Daily Rate -> auto set rate by purity
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
   // val purityNames = purityList.map { it.PurityName ?: "" }

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
                        ProductImageWithAllFallbacks(
                            imageUrl = selectedItem?.Image,
                            itemCode = selectedItem?.ItemCode ?: itemCode,
                            designName = selectedItem?.DesignName,
                            baseUrl = baseUrl,
                            contentDescription = localizedContext.getString(R.string.cd_product_image),
                            placeholder = painterResource(R.drawable.add_photo),
                            error = painterResource(R.drawable.add_photo),
                            modifier = Modifier.size(110.dp),
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    FieldRow(localizedContext.getString(R.string.label_product_name), productName,enabled = false ) { productName = it}
                    Spacer(Modifier.height(4.dp))

                    FieldRow(localizedContext.getString(R.string.label_item_code), itemCode ,enabled = false) { itemCode = it}
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

                    // TotalWt -> triggers full recalc (updates gross/net/amt)
                    FieldRow(localizedContext.getString(R.string.label_total_weight), totalWt,enabled = true) { newVal ->
                        totalWt = newVal
                        recalcAll()
                    }

                    Spacer(Modifier.height(4.dp))

                    // PackingWt -> triggers full recalc (updates gross/net/amt)
                    FieldRow(localizedContext.getString(R.string.label_packing_weight), packingWt,enabled = true) { newVal ->
                        packingWt = newVal
                        recalcAll()
                    }

                    Spacer(Modifier.height(4.dp))

                    // GrossWt -> manual edit: do NOT call recalcAll (it formats/overwrites)
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
                    FieldRow(localizedContext.getString(R.string.stone_amount), stoneAmt, enabled = true) { newVal ->
                        stoneAmt = newVal
                        recalcAll()
                    }

                    Spacer(Modifier.height(4.dp))

                    FieldRow(localizedContext.getString(R.string.diamond_amt), diamondAmt, enabled = true) { newVal ->
                        diamondAmt = newVal
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
                                text = if (NetWt.isBlank()) "" else NetWt,
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

                    // Rate -> recalc
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

                    // Hallmark -> recalc
                    FieldRow(localizedContext.getString(R.string.label_hallmark_amount), hallMarkAmt,enabled = true) { newVal ->
                        hallMarkAmt = newVal
                        recalcAll()
                    }

                    Spacer(Modifier.height(4.dp))

                    // MRP -> recalc (MRP overrides itemAmt)
                    FieldRow(localizedContext.getString(R.string.label_mrp), mrp,enabled = true) { newVal ->
                        mrp = newVal
                        recalcAll()
                    }

                    Spacer(Modifier.height(8.dp))

                    // Optional display (same style as your order dialog)
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
                            Text(text = if (finePlusWt.isBlank()) "" else finePlusWt, fontSize = 13.sp, fontFamily = poppins)
                        }
                    }

                    Spacer(Modifier.height(4.dp))

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
                            Text(text = if (itemAmt.isBlank()) "" else itemAmt, fontSize = 13.sp, fontFamily = poppins)
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
                                NetWt = NetWt,

                                totayRate = ratePerGRam,
                                RatePerGram = ratePerGRam,
                                MetalRate = ratePerGRam,

                                HallmarkAmount = hallMarkAmt,
                                MRP = mrp,
                                ItemAmount = itemAmt,
                                TotalItemAmount = itemAmt,
                                Amount = itemAmt,
                                TotalAmount = itemAmt,

                                FineWastageWt = finePlusWt,
                                FinePercentage = finePercentage,
                                fixWastage = wastage,

                                Purity = purity,
                                Size = size,
                                DiamondColour = typeOfColors,
                                Description = remark,
                                Quantity = qty,
                                qty = qty.toIntOrNull() ?: s.qty,
                                StoneAmount = stoneAmt,
                                StoneAmt = stoneAmt,
                                TotalStoneAmount = stoneAmt,

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

/* ============================
   UI Components (unchanged)
   ============================ */

@Composable
fun FieldRow(label: String, value: String, enabled: Boolean,onChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
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
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange ={ if (enabled) onChange(it) },
                singleLine = true,
                enabled = enabled,
                readOnly = !enabled,
                textStyle = TextStyle(fontSize = 13.sp, color = Color.Black),
                decorationBox = { inner ->
                    if (value.isEmpty())
                        Text("Enter $label", fontSize = 13.sp, color = Color.Gray)
                    inner()
                }
            )
        }
    }
}

@Composable
fun DropdownRow(
    label: String,
    list: List<String>,
    selected: String,
    expanded: Boolean,
    onSelect: (String) -> Unit,
    onExpand: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.4f),
            fontSize = 12.sp,
            color = Color.Black,
            fontFamily = poppins
        )

        Box(
            modifier = Modifier
                .weight(0.9f)
                .height(35.dp)
                .clickable { onExpand(true) }
                .background(Color.White, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (selected.isEmpty()) "Select $label" else selected,
                    fontSize = 13.sp,
                    color = if (selected.isEmpty()) Color.Gray else Color.Black,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = "dropdown", tint = Color.Gray)
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpand(false) },
            ) {
                list.forEach {
                    DropdownMenuItem(
                        text = { Text(it, fontSize = 13.sp) },
                        onClick = {
                            onSelect(it)
                            onExpand(false)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DateRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
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
                .clickable { onClick() }
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = if (value.isEmpty()) "Select $label" else value,
                fontSize = 13.sp,
                color = if (value.isEmpty()) Color.Gray else Color.Black
            )
        }
    }
}
