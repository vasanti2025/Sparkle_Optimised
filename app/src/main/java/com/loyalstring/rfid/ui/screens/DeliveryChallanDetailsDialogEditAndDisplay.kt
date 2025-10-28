package com.loyalstring.rfid.ui.screens

import android.app.DatePickerDialog
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
import coil.compose.AsyncImage
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.local.entity.OrderItem
import com.loyalstring.rfid.ui.utils.GradientButtonIcon
import com.loyalstring.rfid.ui.utils.poppins
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InvoiceDetailsDialogEditAndDisplay(
    selectedItem: OrderItem,
    branchList: List<String>,
    salesmanList: List<String>,
    onDismiss: () -> Unit,
    onSave: (OrderItem) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // ✅ Form States
    var branch by remember { mutableStateOf(selectedItem.branchName) }
    var exhibition by remember { mutableStateOf(selectedItem.exhibition) }
    var remark by remember { mutableStateOf(selectedItem.remark) }
    var purity by remember { mutableStateOf(selectedItem.purity) }
    var size by remember { mutableStateOf(selectedItem.size) }
    var length by remember { mutableStateOf(selectedItem.length) }
    var color by remember { mutableStateOf(selectedItem.typeOfColor) }
    var screwType by remember { mutableStateOf(selectedItem.screwType) }
    var polishType by remember { mutableStateOf(selectedItem.polishType) }
    var finePer by remember { mutableStateOf(selectedItem.finePer) }
    var wastage by remember { mutableStateOf(selectedItem.wastage) }
    var orderDate by remember { mutableStateOf(selectedItem.orderDate) }
    var deliverDate by remember { mutableStateOf(selectedItem.deliverDate) }

    // Dropdown expansions
    var expandedBranch by remember { mutableStateOf(false) }
    var expandedPurity by remember { mutableStateOf(false) }
    var expandedColor by remember { mutableStateOf(false) }
    var expandedScrew by remember { mutableStateOf(false) }
    var expandedPolish by remember { mutableStateOf(false) }

    val purityList = listOf("22K", "18K", "14K")
    val colorList = listOf("Yellow Gold", "White Gold", "Rose Gold", "Green Gold", "Black Gold")
    val screwList = listOf("Type 1", "Type 2", "Type 3")
    val polishList = listOf("High Polish", "Matte", "Satin", "Hammered")
    val baseUrl =
        "https://rrgold.loyalstring.co.in/"

    Dialog(onDismissRequest = { onDismiss() }) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            tonalElevation = 4.dp
        ) {
            Column {
                // 🔹 Header
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
                            contentDescription = "Invoice Icon",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Invoice Fields",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontFamily = poppins
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))

                // 🔹 Scrollable Form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp) // Set the height you need
                            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center // Center horizontally
                    ) {
                        AsyncImage(

                            model =baseUrl+selectedItem?.image,
                            contentDescription = "Image from URL",
                            placeholder = painterResource(R.drawable.add_photo), // Optional
                            error = painterResource(R.drawable.add_photo),       // Optional
                            modifier = Modifier.size(100.dp)
                        )
                        /* AsyncImage(
                             model = baseUrl + selectedItem?.image,
                             contentDescription = "Image from URL",
                             placeholder = painterResource(R.drawable.add_photo),
                             error = painterResource(R.drawable.add_photo),
                             modifier = Modifier.fillMaxWidth(),
                             contentScale = ContentScale.FillBounds  // ✅ maintains ratio, auto height
                         )*/

                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    DropdownRow("Branch", branchList, branch, expandedBranch,
                        { branch = it }, { expandedBranch = it })
                    FieldRow("Exhibition", exhibition) { exhibition = it }
                    FieldRow("Remark", remark) { remark = it }
                    DropdownRow("Purity", purityList, purity, expandedPurity,
                        { purity = it }, { expandedPurity = it })
                    FieldRow("Size", size) { size = it }
                    FieldRow("Length", length) { length = it }
                    DropdownRow("Color", colorList, color, expandedColor,
                        { color = it }, { expandedColor = it })
                    DropdownRow("Screw Type", screwList, screwType, expandedScrew,
                        { screwType = it }, { expandedScrew = it })
                    DropdownRow("Polish Type", polishList, polishType, expandedPolish,
                        { polishType = it }, { expandedPolish = it })
                    FieldRow("Fine %", finePer) { finePer = it }
                    FieldRow("Wastage", wastage) { wastage = it }

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
                        ).show()
                    }
                }

                Spacer(Modifier.height(4.dp))

                // 🔹 Buttons
                Row(
                    Modifier.fillMaxWidth().padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    GradientButtonIcon(
                        text = "Cancel",
                        onClick = { onDismiss() },
                        icon = painterResource(id = R.drawable.ic_cancel),
                        iconDescription = "Cancel",
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .padding(end = 4.dp)
                    )

                    GradientButtonIcon(
                        text = "OK",
                        onClick = {
                            val updated = selectedItem.copy(
                                branchName = branch,
                                exhibition = exhibition,
                                remark = remark,
                                purity = purity,
                                size = size,
                                length = length,
                                typeOfColor = color,
                                screwType = screwType,
                                polishType = polishType,
                                finePer = finePer,
                                wastage = wastage,
                                orderDate = orderDate,
                                deliverDate = deliverDate
                            )
                            onSave(updated)
                            onDismiss()
                        },
                        icon = painterResource(id = R.drawable.check_circle),
                        iconDescription = "Save",
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

/*-------------------------------------------------
   Helper composables for Left-Label layout
-------------------------------------------------*/

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
                    if (value.isEmpty()) Text("Enter $label", fontSize = 13.sp, color = Color.Gray)
                    inner()
                }
            )
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
