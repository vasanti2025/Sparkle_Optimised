package com.loyalstring.rfid.ui.screens
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.model.order.ItemCodeResponse
import com.loyalstring.rfid.data.model.sampleOut.SampleOutListResponse
import com.loyalstring.rfid.worker.LocaleHelper

/*@Composable
fun SampleOutInputRowData(
    itemCode: TextFieldValue,
    onItemCodeChange: (TextFieldValue) -> Unit,
    showDropdown: Boolean,
    setShowDropdown: (Boolean) -> Unit,
    context: Context,
    onClearClicked: () -> Unit,
    filteredList: List<SampleOutListResponse>,
    isLoading: Boolean,
    onItemSelected: (SampleOutListResponse) -> Unit // (kept for compatibility; optional to use)
) {
    val gradient = Brush.horizontalGradient(
        listOf(Color(0xFF5231A7), Color(0xFFD32940))
    )
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val query = itemCode.text.trim()

    /*
      ✅ ASSUMPTION:
      SampleOutListResponse has fields like:
        - itemCode: String?
        - rfid: String?
      If your model uses different names (e.g. rfidNumber / itemcode / RFIDNumber),
      just replace `it.rfid` and `it.itemCode` below.
    */
    val filteredResults = remember(query, filteredList) {
        if (query.isEmpty()) emptyList()
        else {
            when {
                query.any { it.isDigit() } -> {
                    filteredList.filter { it.IssueItems.get(0).SampleOutNo?.contains(query, ignoreCase = true) == true }
                }
                query.length >= 2 -> {
                    filteredList.filter {
                      //  it.rfid?.contains(query, ignoreCase = true) == true ||
                                it.SampleOutNo?.contains(query, ignoreCase = true) == true
                    }
                }
                else -> {
                    filteredList.filter { it.SampleOutNo?.contains(query, ignoreCase = true) == true }
                }
            }
        }
    }

    val ctx: Context = LocalContext.current
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = if (currentLocales.isEmpty) "en" else currentLocales[0]?.language
    val localizedContext = LocaleHelper.applyLocale(ctx, currentLang ?: "en")

    Column(modifier = Modifier.fillMaxWidth()) {

        // 🔹 Input Row (NO SCAN ICON)
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
                                text = localizedContext.getString(R.string.enter_sample_out),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        innerTextField()
                    }
                }
            )

            // ✅ Only Clear button when text is present (no scanning)
            if (itemCode.text.isNotEmpty()) {
                IconButton(
                    onClick = {
                        onClearClicked()
                        setShowDropdown(false)
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = localizedContext.getString(R.string.clear),
                        modifier = Modifier.size(18.dp),
                        tint = Color.Gray
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(26.dp)) // keeps layout aligned
            }
        }

        // 🔽 Dropdown (compact width) - shows Loading / No Results / Results
        DropdownMenu(
            expanded = showDropdown && query.isNotEmpty(),
            onDismissRequest = { setShowDropdown(false) },
            modifier = Modifier
                .widthIn(min = 220.dp, max = 280.dp)
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
                                Text(
                                    localizedContext.getString(R.string.searching),
                                    fontSize = 12.sp
                                )
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
                        val displayText = when {
                           // item.rfid?.contains(query, ignoreCase = true) == true -> item.rfid.orEmpty()
                            item.SampleOutNo?.contains(query, ignoreCase = true) == true -> item.SampleOutNo.orEmpty()
                            else -> item.SampleOutNo.orEmpty()
                        }

                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = displayText,
                                        fontSize = 13.sp,
                                        color = Color.Black
                                    )
                                }
                            },
                            onClick = {
                                onItemCodeChange(TextFieldValue(displayText))
                                setShowDropdown(false)
                                focusManager.clearFocus()
                                keyboardController?.hide()

                                // Optional: call if you want to trigger selection event
                                // onItemSelected(ItemCodeResponse(/* map fields here if needed */))
                            }
                        )
                    }
                }
            }
        }
    }
}*/

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleOutInputRowData(
    itemCode: TextFieldValue,
    onItemCodeChange: (TextFieldValue) -> Unit,
    showDropdown: Boolean,
    setShowDropdown: (Boolean) -> Unit,
    context: Context,
    onClearClicked: () -> Unit,
    filteredList: List<SampleOutListResponse>,
    isLoading: Boolean,
    onItemSelected: (SampleOutListResponse) -> Unit
) {
    val gradient = Brush.horizontalGradient(
        listOf(Color(0xFF5231A7), Color(0xFFD32940))
    )

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    val query = itemCode.text.trim()
    val debouncedQuery = query

    val filteredResults = remember(debouncedQuery, filteredList) {
        val q = debouncedQuery.trim().lowercase()

        if (q.isEmpty()) emptyList()
        else {
            filteredList
                .asSequence()
                .filter { item ->
                    val sampleOutNo = item.SampleOutNo?.trim()?.lowercase().orEmpty()

                    sampleOutNo.startsWith(q) || sampleOutNo.contains(q)
                }
                .sortedBy { item ->
                    val sampleOutNo = item.SampleOutNo?.trim().orEmpty()
                    when {
                        sampleOutNo.equals(debouncedQuery, true) -> 0
                        sampleOutNo.startsWith(debouncedQuery, true) -> 1
                        sampleOutNo.contains(debouncedQuery, true) -> 2
                        else -> 3
                    }
                }
                .take(100)
                .toList()
        }
    }

    val ctx = LocalContext.current
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = if (currentLocales.isEmpty) "en" else currentLocales[0]?.language
    val localizedContext = LocaleHelper.applyLocale(ctx, currentLang ?: "en")

    Column(modifier = Modifier.fillMaxWidth()) {

        val shouldExpand =
            showDropdown &&
                    debouncedQuery.isNotEmpty() &&
                    (isLoading || filteredResults.isNotEmpty())

        ExposedDropdownMenuBox(
            expanded = shouldExpand,
            onExpandedChange = { expanded ->
                if (debouncedQuery.isNotEmpty()) {
                    setShowDropdown(expanded)
                } else {
                    setShowDropdown(false)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .menuAnchor()
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
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (itemCode.text.isEmpty()) {
                                Text(
                                    text = localizedContext.getString(R.string.enter_sample_out),
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                if (itemCode.text.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onClearClicked()
                            setShowDropdown(false)

                            scope.launch {
                                delay(10)
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            }
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = localizedContext.getString(R.string.clear),
                            modifier = Modifier.size(18.dp),
                            tint = Color.Gray
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(26.dp))
                }
            }

            ExposedDropdownMenu(
                expanded = shouldExpand,
                onDismissRequest = { setShowDropdown(false) },
                modifier = Modifier
                    .widthIn(min = 220.dp, max = 280.dp)
                    .heightIn(max = 260.dp)
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
                                    Text(
                                        localizedContext.getString(R.string.searching),
                                        fontSize = 12.sp
                                    )
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
                            val displayText = item.SampleOutNo.orEmpty()

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = displayText,
                                        fontSize = 13.sp,
                                        color = Color.Black
                                    )
                                },
                                onClick = {
                                    onItemCodeChange(TextFieldValue(displayText))
                                    onItemSelected(item)
                                    setShowDropdown(false)

                                    scope.launch {
                                        focusManager.clearFocus(force = true)
                                        delay(50)
                                        keyboardController?.hide()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
