package com.loyalstring.rfid.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.sparklepos.models.loginclasses.customerBill.AddEmployeeRequest
import com.example.sparklepos.models.loginclasses.customerBill.EmployeeList
import com.loyalstring.rfid.R
import com.loyalstring.rfid.ui.utils.GradientButtonIcon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/*----------------------------------------------------------
   CUSTOMER NAME INPUT (with search dropdown)
-----------------------------------------------------------*/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerNameInputData(
    customerName: String,
    onCustomerNameChange: (String) -> Unit,
    onClear: () -> Unit,
    onAddCustomerClick: () -> Unit,
    filteredCustomers: List<EmployeeList>,
    isLoading: Boolean,
    onCustomerSelected: (EmployeeList) -> Unit,
    coroutineScope: CoroutineScope,
    fetchSuggestions: suspend () -> Unit,
    expanded: Boolean,
    onSaveCustomer: (AddEmployeeRequest) -> Unit,
    employeeClientCode: String? = null,
    employeeId: String? = null
) {
    var expandedState by remember { mutableStateOf(false) }
    var showAddCustomerDialog by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val gradientBrush = Brush.horizontalGradient(
        listOf(Color(0xFF5231A7), Color(0xFFD32940))
    )

    ExposedDropdownMenuBox(
        expanded = expandedState && filteredCustomers.isNotEmpty(),
        onExpandedChange = { expandedState = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .height(40.dp)
                .border(1.5.dp, gradientBrush, RoundedCornerShape(10.dp))
                .background(Color.White, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = customerName,
                    onValueChange = {
                        onCustomerNameChange(it)
                        expandedState = it.isNotEmpty()
                        coroutineScope.launch { fetchSuggestions() }
                    },
                    textStyle = TextStyle(fontSize = 15.sp, color = Color.Black),
                    singleLine = true,
                    decorationBox = { inner ->
                        if (customerName.isEmpty()) {
                            Text("Enter customer name", color = Color.Gray, fontSize = 12.sp)
                        }
                        inner()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 6.dp)
                )

                if (customerName.isEmpty()) {
                    IconButton(
                        onClick = {
                            onAddCustomerClick()
                            expandedState = false
                            showAddCustomerDialog = true
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.vector_add),
                            contentDescription = "Add",
                            tint = Color.Unspecified
                        )
                    }
                } else {
                    IconButton(
                        onClick = {
                            onClear()
                            expandedState = false
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = Color(0xFF3C3C3C)
                        )
                    }
                }
            }
        }

        // Dropdown list for existing customers
        ExposedDropdownMenu(
            expanded = expandedState && filteredCustomers.isNotEmpty(),
            onDismissRequest = { expandedState = false },
            modifier = Modifier.background(Color.White)
        ) {
            if (isLoading) {
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF5231A7)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Loading...", fontSize = 12.sp)
                        }
                    },
                    onClick = {}
                )
            } else {
                filteredCustomers.forEach { customer ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                "${customer.FirstName.orEmpty()} ${customer.LastName.orEmpty()}",
                                fontSize = 14.sp
                            )
                        },
                        onClick = {
                            onCustomerSelected(customer)
                            expandedState = false
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    )
                }
            }
        }
    }

    if (showAddCustomerDialog) {
        AddCustomerDialog(
            onDismiss = { showAddCustomerDialog = false },
            onSaveCustomer = {
                onSaveCustomer(it)
                showAddCustomerDialog = false
            },
            employeeClientCode = employeeClientCode,
            employeeId = employeeId
        )
    }
}


/*----------------------------------------------------------
   ADD CUSTOMER POPUP DIALOG
-----------------------------------------------------------*/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onSaveCustomer: (AddEmployeeRequest) -> Unit,
    employeeClientCode: String? = null,
    employeeId: String? = null
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Form States
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pan by remember { mutableStateOf("") }
    var gst by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }

    // Dropdown flags
    var expandedCountry by remember { mutableStateOf(false) }
    var expandedState by remember { mutableStateOf(false) }

    // Error states
    var panError by remember { mutableStateOf<String?>(null) }
    var gstError by remember { mutableStateOf<String?>(null) }

    val countryOptions = listOf("India", "USA", "UAE", "UK")
    val stateOptions = listOf("Maharashtra", "Gujarat", "Karnataka", "Delhi")

    Popup(alignment = Alignment.Center, properties = PopupProperties(focusable = true)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .fillMaxWidth(0.95f)
                    .heightIn(min = 300.dp, max = 600.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {

                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(
                                Color.DarkGray,
                                shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Customer Profile",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Scrollable Form
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(scrollState)
                            .padding(16.dp)
                    ) {

                        // Common Modifier
                        fun fieldModifier() = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF5F5F5))
                            .padding(horizontal = 10.dp, vertical = 6.dp)

                        // Common Text Style
                        val fieldTextStyle = LocalTextStyle.current.copy(
                            fontSize = 13.sp,
                            color = Color.Black
                        )

                        // ----------- NAME -----------
                        BasicTextField(
                            value = name,
                            onValueChange = { name = it },
                            textStyle = fieldTextStyle,
                            singleLine = true,
                            modifier = fieldModifier(),
                            decorationBox = { innerTextField ->
                                if (name.isEmpty()) {
                                    Text("Customer Name", color = Color.Gray, fontSize = 12.sp)
                                }
                                innerTextField()
                            }
                        )
                        Spacer(Modifier.height(10.dp))

                        // ----------- PHONE -----------
                        BasicTextField(
                            value = phone,
                            onValueChange = {
                                if (it.length <= 10) phone = it.filter(Char::isDigit)
                            },
                            textStyle = fieldTextStyle,
                            singleLine = true,
                            modifier = fieldModifier(),
                            decorationBox = { innerTextField ->
                                if (phone.isEmpty()) {
                                    Text("Mobile Number", color = Color.Gray, fontSize = 12.sp)
                                }
                                innerTextField()
                            }
                        )
                        Spacer(Modifier.height(10.dp))

                        // ----------- EMAIL -----------
                        BasicTextField(
                            value = email,
                            onValueChange = { email = it },
                            textStyle = fieldTextStyle,
                            singleLine = true,
                            modifier = fieldModifier(),
                            decorationBox = { innerTextField ->
                                if (email.isEmpty()) {
                                    Text("Email", color = Color.Gray, fontSize = 12.sp)
                                }
                                innerTextField()
                            }
                        )
                        Spacer(Modifier.height(10.dp))

                        // ----------- PAN -----------
                        BasicTextField(
                            value = pan,
                            onValueChange = {
                                pan = it.uppercase().take(10)
                                panError = when {
                                    pan.isEmpty() -> null
                                    pan.length != 10 -> "PAN must be exactly 10 characters"
                                    !pan.matches("^[A-Z]{5}[0-9]{4}[A-Z]$".toRegex()) -> "Invalid PAN format"
                                    else -> null
                                }
                            },
                            textStyle = fieldTextStyle,
                            singleLine = true,
                            modifier = fieldModifier(),
                            decorationBox = { innerTextField ->
                                if (pan.isEmpty()) {
                                    Text("PAN Number", color = Color.Gray, fontSize = 12.sp)
                                }
                                innerTextField()
                            }
                        )
                        panError?.let { Text(it, color = Color.Red, fontSize = 12.sp) }
                        Spacer(Modifier.height(10.dp))

                        // ----------- GST -----------
                        BasicTextField(
                            value = gst,
                            onValueChange = {
                                gst = it.uppercase().take(15)
                                gstError = when {
                                    gst.isEmpty() -> null
                                    gst.length != 15 -> "GST must be exactly 15 characters"
                                    !gst.matches("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][A-Z0-9][A-Z][0-9]$".toRegex()) ->
                                        "Invalid GST format"
                                    else -> null
                                }
                            },
                            textStyle = fieldTextStyle,
                            singleLine = true,
                            modifier = fieldModifier(),
                            decorationBox = { innerTextField ->
                                if (gst.isEmpty()) {
                                    Text("GST Number", color = Color.Gray, fontSize = 12.sp)
                                }
                                innerTextField()
                            }
                        )
                        gstError?.let { Text(it, color = Color.Red, fontSize = 12.sp) }
                        Spacer(Modifier.height(10.dp))

                        // ----------- ADDRESS -----------
                        BasicTextField(
                            value = street,
                            onValueChange = { street = it },
                            textStyle = fieldTextStyle,
                            singleLine = true,
                            modifier = fieldModifier(),
                            decorationBox = { innerTextField ->
                                if (street.isEmpty()) {
                                    Text("Street / Address", color = Color.Gray, fontSize = 12.sp)
                                }
                                innerTextField()
                            }
                        )
                        Spacer(Modifier.height(10.dp))

                        // ----------- COUNTRY & STATE -----------
                        Row(Modifier.fillMaxWidth()) {
                            // Country
                            Box(
                                modifier = fieldModifier()
                                    .weight(1f)
                                    .clickable { expandedCountry = !expandedCountry }
                            ) {
                                Text(
                                    text = if (country.isEmpty()) "Country" else country,
                                    fontSize = 12.sp,
                                    color = if (country.isEmpty()) Color.Gray else Color.Black,
                                    modifier = Modifier.align(Alignment.CenterStart)
                                )
                                Icon(
                                    imageVector = if (expandedCountry) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                )

                                DropdownMenu(
                                    expanded = expandedCountry,
                                    onDismissRequest = { expandedCountry = false }
                                ) {
                                    countryOptions.forEach {
                                        DropdownMenuItem(
                                            text = { Text(it) },
                                            onClick = {
                                                country = it
                                                expandedCountry = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            // State
                            Box(
                                modifier = fieldModifier()
                                    .weight(1f)
                                    .clickable { expandedState = !expandedState }
                            ) {
                                Text(
                                    text = if (state.isEmpty()) "State" else state,
                                    fontSize = 12.sp,
                                    color = if (state.isEmpty()) Color.Gray else Color.Black,
                                    modifier = Modifier.align(Alignment.CenterStart)
                                )
                                Icon(
                                    imageVector = if (expandedState) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                )

                                DropdownMenu(
                                    expanded = expandedState,
                                    onDismissRequest = { expandedState = false }
                                ) {
                                    stateOptions.forEach {
                                        DropdownMenuItem(
                                            text = { Text(it) },
                                            onClick = {
                                                state = it
                                                expandedState = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        // ----------- CITY -----------
                        BasicTextField(
                            value = city,
                            onValueChange = { city = it },
                            textStyle = fieldTextStyle,
                            singleLine = true,
                            modifier = fieldModifier(),
                            decorationBox = { innerTextField ->
                                if (city.isEmpty()) {
                                    Text("City", color = Color.Gray, fontSize = 12.sp)
                                }
                                innerTextField()
                            }
                        )
                    }

                    // Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        GradientButtonIcon(
                            text = "Cancel",
                            onClick = onDismiss,
                            icon = painterResource(id = R.drawable.ic_cancel),
                            iconDescription = "Cancel Icon",
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 4.dp)
                        )

                        GradientButtonIcon(
                            text = "OK",
                            onClick = {
                                when {
                                    name.isEmpty() -> Toast.makeText(context, "Enter name", Toast.LENGTH_SHORT).show()
                                    email.isNotEmpty() &&
                                            !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()) ->
                                        Toast.makeText(context, "Invalid email", Toast.LENGTH_SHORT).show()
                                    pan.isNotEmpty() &&
                                            !pan.matches("^[A-Z]{5}[0-9]{4}[A-Z]{1}$".toRegex()) ->
                                        Toast.makeText(context, "Invalid PAN", Toast.LENGTH_SHORT).show()
                                    gst.isNotEmpty() &&
                                            !gst.matches("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][A-Z0-9][A-Z][0-9]$".toRegex()) ->
                                        Toast.makeText(context, "Invalid GST", Toast.LENGTH_SHORT).show()
                                    else -> {
                                        val request = AddEmployeeRequest(
                                            name, "", "", email, "", "", "",
                                            0, 0, 0, phone, "Active", "",
                                            "0", "0", street, "", "", city,
                                            state, "", "", "", "", country, "",
                                            "", "0", "0", pan, "0", "0", gst,
                                            employeeClientCode, 0, "", false, employeeId
                                        )
                                        onSaveCustomer(request)
                                        onDismiss()
                                    }
                                }
                            },
                            icon = painterResource(id = R.drawable.check_circle),
                            iconDescription = "OK Icon",
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// Toast Helper
fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

@Composable
fun GradientButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF5231A7), Color(0xFFD32940))
                )
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
