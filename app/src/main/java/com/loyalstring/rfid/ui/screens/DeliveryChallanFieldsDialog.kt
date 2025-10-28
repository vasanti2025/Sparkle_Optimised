package com.loyalstring.rfid.ui.screens



import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.loyalstring.rfid.R
import com.loyalstring.rfid.ui.utils.GradientButtonIcon
import com.loyalstring.rfid.ui.utils.poppins
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InvoiceFieldsDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    branchList: List<String>,
    salesmanList: List<String>
) {
    val gradient = Brush.horizontalGradient(listOf(Color(0xFF5231A7), Color(0xFFD32940)))
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val calendar = Calendar.getInstance()

    var selectedBranch by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var fine by remember { mutableStateOf("") }
    var wastage by remember { mutableStateOf("") }
    var salesman by remember { mutableStateOf("") }

    var expandedBranch by remember { mutableStateOf(false) }
    var expandedSalesman by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = { onDismiss() }) {
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
                // 🔹 Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF3A3A3A))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.stylus_note), // or use any Material icon you prefer
                            contentDescription = "Invoice Fileds",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "Invoice Fileds",
                            fontSize = 18.sp,
                            color = Color.White,
                            fontFamily = poppins
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                // 🔹 Content
                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    // Branch
                    DropdownMenuField(
                        label = "Select Branch",
                        options = branchList,
                        selectedValue = selectedBranch,
                        expanded = expandedBranch,
                        onValueChange = { selectedBranch = it },
                        onExpandedChange = { expandedBranch = it }
                    )

                    Spacer(Modifier.height(8.dp))

                    // Date picker
                    FieldWithLabel(
                        label = "Date",
                        value = date,
                        placeholder = "Select date",
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val selected = Calendar.getInstance().apply {
                                        set(year, month, day)
                                    }
                                    date = dateFormat.format(selected.time)
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                    )

                    Spacer(Modifier.height(8.dp))

                    // Fine %
                    InputField(label = "Fine%", value = fine, onValueChange = { fine = it })

                    Spacer(Modifier.height(8.dp))

                    // Wastage
                    InputField(label = "Wastage", value = wastage, onValueChange = { wastage = it })

                    Spacer(Modifier.height(8.dp))

                    // Salesman
                    DropdownMenuField(
                        label = "Salesman",
                        options = salesmanList,
                        selectedValue = salesman,
                        expanded = expandedSalesman,
                        onValueChange = { salesman = it },
                        onExpandedChange = { expandedSalesman = it }
                    )
                }

                Spacer(Modifier.height(10.dp))

                // 🔹 Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    GradientButtonIcon(
                        text = "Cancel",
                        onClick = onDismiss,
                        icon = painterResource(id = R.drawable.ic_cancel),
                        iconDescription = "Cancel",
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .padding(end = 6.dp)
                    )
                    GradientButtonIcon(
                        text = "Ok",
                        onClick = onConfirm,
                        icon = painterResource(id = R.drawable.check_circle),
                        iconDescription = "OK",
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

/*-----------------------------------------------------------
   Shared UI Components (clean & reusable)
-----------------------------------------------------------*/

// 🔸 Dropdown Field
@Composable
fun <T> DropdownMenuField(
    label: String,
    options: List<T>,
    selectedValue: String,
    expanded: Boolean,
    onValueChange: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    getOptionLabel: (T) -> String = { it.toString() }
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(Color(0xFFF2F2F3), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clickable { onExpandedChange(true) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedValue.isEmpty()) label else selectedValue,
                fontSize = 13.sp,
                color = if (selectedValue.isEmpty()) Color.Gray else Color.Black,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Dropdown",
                tint = Color.Gray
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            offset = DpOffset(0.dp, 4.dp)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(getOptionLabel(option), fontSize = 13.sp) },
                    onClick = {
                        onValueChange(getOptionLabel(option))
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}

// 🔸 Text Input Field
@Composable
fun InputField(label: String, value: String, onValueChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(Color(0xFFF2F2F3), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(fontSize = 13.sp, color = Color.Black),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(label, fontSize = 13.sp, color = Color.Gray)
                }
                inner()
            }
        )
    }
}

// 🔸 Readonly Field with Click Action (for Date)
@Composable
fun FieldWithLabel(label: String, value: String, placeholder: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(Color(0xFFF2F2F3), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = if (value.isEmpty()) placeholder else value,
            fontSize = 13.sp,
            color = if (value.isEmpty()) Color.Gray else Color.Black
        )
    }
}
