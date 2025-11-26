package com.loyalstring.rfid.ui.screens

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import com.loyalstring.rfid.worker.LocaleHelper
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.model.order.ItemCodeResponse

@Composable
fun ItemCodeInputRowData(
    itemCode: TextFieldValue,
    onItemCodeChange: (TextFieldValue) -> Unit,
    showDropdown: Boolean,
    setShowDropdown: (Boolean) -> Unit,
    context: Context,
    onScanClicked: () -> Unit,
    onClearClicked: () -> Unit,
    filteredList: List<BulkItem>,
    isLoading: Boolean,
    onItemSelected: (ItemCodeResponse) -> Unit
) {
    val gradient = Brush.horizontalGradient(
        listOf(Color(0xFF5231A7), Color(0xFFD32940))
    )
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val query = itemCode.text.trim()

    // ✅ Improved filtering logic
    val filteredResults = remember(query, filteredList) {
        if (query.isEmpty()) emptyList()
        else {
            when {
                // Case 1️⃣: Has digits → definitely RFID
                query.any { it.isDigit() } -> {
                    filteredList.filter { it.rfid?.contains(query, ignoreCase = true) == true }
                }

                // Case 2️⃣: Letters only but matches RFID prefix (e.g., "SJ", "PJ")
                query.length >= 2 -> {
                    filteredList.filter {
                        it.rfid?.contains(query, ignoreCase = true) == true ||
                                it.itemCode?.contains(query, ignoreCase = true) == true
                    }
                }

                // Case 3️⃣: Very short (1 letter) → fallback to ItemCode
                else -> {
                    filteredList.filter {
                        it.itemCode?.contains(query, ignoreCase = true) == true
                    }
                }
            }
        }
    }

    val context: Context = LocalContext.current
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = if (currentLocales.isEmpty) "en" else currentLocales[0]?.language
    val localizedContext = LocaleHelper.applyLocale(context, currentLang ?: "en")


    Column(modifier = Modifier.fillMaxWidth()) {

        // 🔹 Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .border(1.dp, gradient, RoundedCornerShape(10.dp))
                .background(Color.White, RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = itemCode,
                onValueChange = {
                    onItemCodeChange(it)
                    setShowDropdown(it.text.isNotEmpty())
                },
                singleLine = true,
                textStyle = TextStyle(fontSize = 13.sp, color = Color.Black),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (itemCode.text.isEmpty()) {
                            Text(
                                text = localizedContext.getString(R.string.enter_rfid_itemcode),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        innerTextField()
                    }
                }
            )

            // 🔹 QR / Clear Button
            IconButton(
                onClick = {
                    if (itemCode.text.isNotEmpty()) {
                        onClearClicked()
                        setShowDropdown(false)
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    } else {
                        onScanClicked()
                    }
                },
                modifier = Modifier.size(26.dp)
            ) {
                if (itemCode.text.isNotEmpty()) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = localizedContext.getString(R.string.clear),
                        modifier = Modifier.size(18.dp),
                        tint = Color.Gray
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.svg_qr),
                        contentDescription = localizedContext.getString( R.string.scan),
                        modifier = Modifier.size(18.dp),
                        tint = Color.Gray
                    )
                }
            }
        }

        // 🔽 Dropdown (compact width)
        DropdownMenu(
            expanded = showDropdown && (isLoading || filteredResults.isNotEmpty()),
            onDismissRequest = { setShowDropdown(false) },
            modifier = Modifier
                .widthIn(min = 220.dp, max = 280.dp) // ✅ Compact width
                .background(Color.White)
        ) {
            when {
                isLoading -> {
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF5231A7)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(localizedContext.getString( R.string.searching), fontSize = 12.sp)
                            }
                        },
                        onClick = {}
                    )
                }

                filteredResults.isEmpty() -> {
                    DropdownMenuItem(
                        text = {
                            Text(
                                localizedContext.getString(R.string.no_results_found),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        },
                        onClick = {}
                    )
                }

                else -> {
                    filteredResults.forEach { item ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    // 🔹 Show whichever matches the query (RFID or ItemCode)
                                    Text(
                                        text = when {
                                            item.rfid?.contains(query, ignoreCase = true) == true -> item.rfid ?: ""
                                            item.itemCode?.contains(query, ignoreCase = true) == true -> item.itemCode ?: ""
                                            else -> item.itemCode ?: ""
                                        },
                                        fontSize = 13.sp,
                                        color = Color.Black
                                    )
                                }
                            },
                            onClick = {
                                onItemCodeChange(
                                    TextFieldValue(
                                        when {
                                            item.rfid?.contains(query, ignoreCase = true) == true -> item.rfid ?: ""
                                            item.itemCode?.contains(query, ignoreCase = true) == true -> item.itemCode ?: ""
                                            else -> ""
                                        }
                                    )
                                )
                                setShowDropdown(false)
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                        )
                    }
                }
            }
        }
    }
}
