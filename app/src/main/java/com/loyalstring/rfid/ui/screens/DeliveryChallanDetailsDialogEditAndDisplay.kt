package com.loyalstring.rfid.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
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
import com.loyalstring.rfid.data.local.entity.DeliveryChallanItem
import com.loyalstring.rfid.data.local.entity.OrderItem
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.addSingleItem.BranchModel
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.ui.utils.GradientButtonIcon
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.OrderViewModel
import com.loyalstring.rfid.viewmodel.SingleProductViewModel
import com.loyalstring.rfid.viewmodel.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * DeliveryChallanDialogEditAndDisplay
 * A Delivery Challan dialog with the same fields & functionality as the Order details dialog.
 *
 * Usage: call it with the selected OrderItem, branch list and an onSave callback to persist changes.
 */
@Composable
fun DeliveryChallanDialogEditAndDisplay(
    selectedItem: DeliveryChallanItem?,
    branchList: List<BranchModel>,
    salesmanList: UiState<List<EmployeeList>>, // ✅ Added this line
    onDismiss: () -> Unit,
    edit: Int = 0,
    onSave: (DeliveryChallanItem) -> Unit,
    orderViewModel: OrderViewModel = hiltViewModel(),
    singleProductViewModel: SingleProductViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    // Pull employee/client code for network calls
    val employee = UserPreferences.getInstance(context).getEmployee(Employee::class.java)
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

    // --- Form state (same as Order screen) ---
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
    var NetWt by remember { mutableStateOf("") }
    var totalWt by remember { mutableStateOf("") }
    var packingWt by remember { mutableStateOf("") }
    var grossWT by remember { mutableStateOf("") }
    var stoneWt by remember { mutableStateOf("") }
    var dimondWt by remember { mutableStateOf("") }
    var ratePerGRam by remember { mutableStateOf("") }
    var hallMarkAmt by remember { mutableStateOf("") }
    var mrp by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var stoneAmt by remember { mutableStateOf("") }
    var itemAmt by remember { mutableStateOf("") }
    var finePlusWt by remember { mutableStateOf("") }

    // Dropdown expansion states
    var expandedBranch by remember { mutableStateOf(false) }
    var expandedPurity by remember { mutableStateOf(false) }
    var expandedColors by remember { mutableStateOf(false) }
    var expandedScrew by remember { mutableStateOf(false) }
    var expandedPolish by remember { mutableStateOf(false) }

    val colorsList = listOf(
        "Yellow Gold","White Gold","Rose Gold","Green Gold","Black Gold","Blue Gold","Purple Gold"
    )
    val screwList = listOf("Type 1","Type 2","Type 3")
    val polishList = listOf("High Polish","Matte Finish","Satin Finish","Hammered")
    val baseUrl = "https://rrgold.loyalstring.co.in/"

    // Format date helper
    val inputFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    )
    fun formatDateSafe(dateString: String?): String {
        if (dateString.isNullOrBlank() || dateString.equals("null", true)) return ""
        for (format in inputFormats) {
            try {
                val parsed = format.parse(dateString)
                if (parsed != null) {
                    return dateFormatter.format(parsed)
                }
            } catch (_: Exception) { }
        }
        return ""
    }

    // Initialize from selectedItem
    LaunchedEffect(selectedItem) {
        selectedItem?.let { s ->
            branch = s.branchName ?: ""
            productName = s.productName.orEmpty()
            itemCode = s.itemCode.orEmpty()
            totalWt = s.totalWt.orEmpty()
            packingWt = s.packingWt.orEmpty()
            grossWT = s.grWt.orEmpty()
            stoneWt = s.stoneWt.orEmpty()
            dimondWt = s.dimondWt.orEmpty()
            NetWt = s.nWt.orEmpty()
            sku = s.sku.orEmpty()
            purity = s.purity.orEmpty()
            size = s.size.orEmpty()
            length = s.length.orEmpty()
            exhibition = s.exhibition.orEmpty()
            remark = s.remark.orEmpty()
            typeOfColors = s.typeOfColor.orEmpty()
            screwType = s.screwType.orEmpty()
            polishType = s.polishType.orEmpty()
            finePercentage = s.finePer.orEmpty()
            wastage = s.wastage.orEmpty()
            orderDate = formatDateSafe(s.orderDate)
            deliverDate = formatDateSafe(s.deliverDate)
            qty = when {
                s.qty == null -> ""
                s.qty.equals("null", true) -> "1"
                s.qty.isBlank() -> ""
                s.qty == "0" -> "1"
                else -> s.qty
            }
            hallMarkAmt = s.hallmarkAmt.orEmpty()
            mrp = s.mrp.orEmpty()
            ratePerGRam = s.todaysRate.orEmpty()
            stoneAmt = s.stoneAmt.orEmpty()
            finePlusWt = s.finePlusWt.orEmpty()

            // default itemAmt: prefer mrp then itemAmt
            itemAmt = if (!s.mrp.isNullOrEmpty()) s.mrp else s.itemAmt.orEmpty()
            // try numeric formatting safely
            itemAmt = try {
                "%.2f".format(itemAmt.toDouble())
            } catch (_: Exception) { itemAmt }
        }
    }

    // Observe daily rates if available
    val dailyRates by orderViewModel.getAllDailyRate.collectAsState(initial = emptyList())
    LaunchedEffect(purity, dailyRates) {
        if (purity.isNotBlank() && dailyRates.isNotEmpty()) {
            val match = dailyRates.find { it.PurityName.equals(purity, ignoreCase = true) }
            match?.Rate?.let { rate ->
                ratePerGRam = rate
                val net = NetWt.toDoubleOrNull() ?: 0.0
                val totalRate = net * (rate.toDoubleOrNull() ?: 0.0)
                itemAmt = "%.2f".format(totalRate)
            }
        }
    }

    Dialog(onDismissRequest = { onDismiss() }) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // header
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
                            contentDescription = "Delivery Challan Icon",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Delivery Challan Fields",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontFamily = poppins
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    // image row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        AsyncImage(
                            model = baseUrl + (selectedItem?.image ?: ""),
                            contentDescription = "Product image",
                            placeholder = painterResource(R.drawable.add_photo),
                            error = painterResource(R.drawable.add_photo),
                            modifier = Modifier.size(100.dp)
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    // Branch dropdown (convert BranchModel -> String list)
                    val branchNames = branchList.map { it.BranchName ?: "" }
                    DropdownRow(
                        label = "Branch",
                        list = branchNames,
                        selected = branch,
                        expanded = expandedBranch,
                        onSelect = { branch = it },
                        onExpand = { expandedBranch = it }
                    )

                    Spacer(Modifier.height(4.dp))
                    FieldRow("Product Name", productName) { productName = it }
                    Spacer(Modifier.height(4.dp))
                    FieldRow("ItemCode", itemCode) { itemCode = it }
                    Spacer(Modifier.height(4.dp))

                    // Total weight / packing / gross / stone / diamond / net
                    FieldRow("Total Weight", totalWt) { newVal ->
                        totalWt = newVal
                        // recalc gross and net
                        val totalValue = totalWt.toDoubleOrNull() ?: 0.0
                        val pack = packingWt.toDoubleOrNull() ?: 0.0
                        grossWT = String.format("%.3f", totalValue - pack)
                        val stone = stoneWt.toDoubleOrNull() ?: 0.0
                        val diamond = dimondWt.toDoubleOrNull() ?: 0.0
                        val stoneAmtVal = stoneAmt.toDoubleOrNull() ?: 0.0
                        NetWt = String.format("%.3f", totalValue - (stone + diamond + stoneAmtVal))
                        val net = NetWt.toDoubleOrNull() ?: 0.0
                        val rate = ratePerGRam.toDoubleOrNull() ?: 0.0
                        itemAmt = "%.2f".format(net * rate)
                    }

                    Spacer(Modifier.height(4.dp))

                    FieldRow("Packing Wt", packingWt) { newVal ->
                        packingWt = newVal
                        val totalValue = totalWt.toDoubleOrNull() ?: 0.0
                        val pack = packingWt.toDoubleOrNull() ?: 0.0
                        grossWT = String.format("%.3f", totalValue - pack)
                        val stone = stoneWt.toDoubleOrNull() ?: 0.0
                        val diamond = dimondWt.toDoubleOrNull() ?: 0.0
                        NetWt = String.format("%.3f", (totalValue - pack) - (stone + diamond + (stoneAmt.toDoubleOrNull() ?: 0.0)))
                        val net = NetWt.toDoubleOrNull() ?: 0.0
                        val rate = ratePerGRam.toDoubleOrNull() ?: 0.0
                        itemAmt = "%.2f".format(net * rate)
                    }

                    Spacer(Modifier.height(4.dp))
                    FieldRow("Gross Wt", grossWT) { newVal ->
                        grossWT = newVal
                        val g = grossWT.toDoubleOrNull() ?: 0.0
                        val s = stoneWt.toDoubleOrNull() ?: 0.0
                        val d = dimondWt.toDoubleOrNull() ?: 0.0
                        NetWt = "%.3f".format(g - s - d)
                        val net = NetWt.toDoubleOrNull() ?: 0.0
                        val rate = ratePerGRam.toDoubleOrNull() ?: 0.0
                        itemAmt = "%.2f".format(net * rate)
                    }

                    Spacer(Modifier.height(4.dp))
                    FieldRow("Stone Weight", stoneWt) { newVal ->
                        stoneWt = newVal
                        val total = grossWT.toDoubleOrNull() ?: 0.0
                        val stone = stoneWt.toDoubleOrNull() ?: 0.0
                        val diamond = dimondWt.toDoubleOrNull() ?: 0.0
                        NetWt = String.format("%.3f", total - (stone + diamond + (stoneAmt.toDoubleOrNull() ?: 0.0)))
                        val net = NetWt.toDoubleOrNull() ?: 0.0
                        val rate = ratePerGRam.toDoubleOrNull() ?: 0.0
                        itemAmt = "%.2f".format(net * rate)
                    }

                    Spacer(Modifier.height(4.dp))
                    FieldRow("Dimond Weight", dimondWt) { newVal ->
                        dimondWt = newVal
                        val total = grossWT.toDoubleOrNull() ?: 0.0
                        val stone = stoneWt.toDoubleOrNull() ?: 0.0
                        val diamond = dimondWt.toDoubleOrNull() ?: 0.0
                        NetWt = String.format("%.3f", total - (stone + diamond + (stoneAmt.toDoubleOrNull() ?: 0.0)))
                        val net = NetWt.toDoubleOrNull() ?: 0.0
                        val rate = ratePerGRam.toDoubleOrNull() ?: 0.0
                        itemAmt = "%.2f".format(net * rate)
                    }

                    Spacer(Modifier.height(4.dp))

                    // Net Wt display
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                            .padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Net Wt",
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
                            Text(text = if (NetWt.isBlank()) "0.000" else NetWt, fontSize = 13.sp, fontFamily = poppins)
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // Rate / item amount / hallmark / mrp
                    FieldRow("Rate/Gram", ratePerGRam) { newVal ->
                        ratePerGRam = newVal
                        val net = NetWt.toDoubleOrNull() ?: 0.0
                        val rate = ratePerGRam.toDoubleOrNull() ?: 0.0
                        itemAmt = "%.2f".format(net * rate)
                    }

                    Spacer(Modifier.height(4.dp))
                    FieldRow("Hallmark Amt", hallMarkAmt) { newVal ->
                        hallMarkAmt = newVal
                        val newHall = hallMarkAmt.toDoubleOrNull() ?: 0.0
                        val baseAmt = (NetWt.toDoubleOrNull() ?: 0.0) * (ratePerGRam.toDoubleOrNull() ?: 0.0)
                        itemAmt = String.format("%.2f", baseAmt + newHall)
                    }

                    Spacer(Modifier.height(4.dp))
                    FieldRow("Mrp", mrp) { newVal ->
                        mrp = newVal
                        val mrpValue = mrp.toDoubleOrNull()
                        if (mrpValue != null && mrpValue > 0) {
                            itemAmt = String.format("%.2f", mrpValue)
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    FieldRow("Exhibition", exhibition) { exhibition = it }
                    Spacer(Modifier.height(4.dp))
                    FieldRow("Remark", remark) { remark = it }

                    Spacer(Modifier.height(4.dp))

                    // Purity dropdown using simple list from singleProductViewModel
                    val purityList by singleProductViewModel.purityResponse1.collectAsState()
                    val purityNames = purityList.map { it.PurityName ?: "" }
                    DropdownRow(
                        label = "Purity",
                        list = purityNames,
                        selected = purity,
                        expanded = expandedPurity,
                        onSelect = { purity = it },
                        onExpand = { expandedPurity = it }
                    )

                    Spacer(Modifier.height(4.dp))

                    // Size & Length
                    FieldRow("Size", size) { size = it }
                    Spacer(Modifier.height(4.dp))
                    FieldRow("Length", length) { length = it }

                    Spacer(Modifier.height(4.dp))

                    // Colors, Screw, Polish dropdowns
                    DropdownRow("Color", colorsList, typeOfColors, expandedColors, { typeOfColors = it }, { expandedColors = it })
                    Spacer(Modifier.height(4.dp))
                    DropdownRow("Screw Type", screwList, screwType, expandedScrew, { screwType = it }, { expandedScrew = it })
                    Spacer(Modifier.height(4.dp))
                    DropdownRow("Polish Type", polishList, polishType, expandedPolish, { polishType = it }, { expandedPolish = it })

                    Spacer(Modifier.height(4.dp))
                    FieldRow("Fine %", finePercentage) { finePercentage = it }
                    Spacer(Modifier.height(4.dp))
                    FieldRow("Wastage", wastage) { wastage = it }

                    Spacer(Modifier.height(4.dp))

                    // Order Date
                    DateRow("Order Date", orderDate) {
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val selected = Calendar.getInstance().apply { set(y, m, d) }
                                orderDate = dateFormatter.format(selected.time)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }

                    Spacer(Modifier.height(4.dp))

                    // Deliver Date (min is order date if set)
                    DateRow("Deliver Date", deliverDate) {
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val selected = Calendar.getInstance().apply { set(y, m, d) }
                                deliverDate = dateFormatter.format(selected.time)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        ).apply {
                            if (orderDate.isNotEmpty()) {
                                try {
                                    val orderCal = Calendar.getInstance()
                                    orderCal.time = dateFormatter.parse(orderDate)!!
                                    orderCal.set(Calendar.HOUR_OF_DAY, 0); orderCal.set(Calendar.MINUTE, 0)
                                    orderCal.set(Calendar.SECOND, 0); orderCal.set(Calendar.MILLISECOND, 0)
                                    datePicker.minDate = orderCal.timeInMillis
                                } catch (_: Exception) {}
                            }
                        }.show()
                    }

                    Spacer(Modifier.height(4.dp))
                    FieldRow("Quantity", qty) { qty = it }
                }

                // Buttons: Cancel / Save
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    GradientButtonIcon(
                        text = "Cancel",
                        onClick = { onDismiss() },
                        icon = painterResource(id = R.drawable.ic_cancel),
                        iconDescription = "Cancel",
                        modifier = Modifier.weight(1f).height(48.dp).padding(end = 4.dp)
                    )

                    GradientButtonIcon(
                        text = "Save",
                        onClick = {
                            // build updated OrderItem (copy existing and replace fields)
                            selectedItem?.let { s ->
                                val updated = s.copy(
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
                                    sku = sku,
                                    totalWt = totalWt,
                                    packingWt = packingWt,
                                    grWt = grossWT,
                                    stoneWt = stoneWt,
                                    dimondWt = dimondWt,
                                    nWt = NetWt,
                                    todaysRate = ratePerGRam,
                                    hallmarkAmt = hallMarkAmt,
                                    mrp = mrp,
                                    qty = qty,
                                    stoneAmt = stoneAmt,
                                    itemAmt = itemAmt,
                                    finePlusWt = finePlusWt
                                )
                                onSave(updated)
                                onDismiss()
                            } ?: run {
                                // If selectedItem is null, show toast and close
                                Toast.makeText(context, "No item selected", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        },
                        icon = painterResource(id = R.drawable.check_circle),
                        iconDescription = "Save",
                        modifier = Modifier.weight(1f).height(48.dp).padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FieldRow(label: String, value: String, onChange: (String) -> Unit) {
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
                onValueChange = onChange,
                singleLine = true,
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
