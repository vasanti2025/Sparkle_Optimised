package com.loyalstring.rfid.ui.screens



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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.model.order.ItemCodeResponse
import com.loyalstring.rfid.worker.LocaleHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockTransferItemCode(
    itemCode: TextFieldValue,
    onItemCodeChange: (TextFieldValue) -> Unit,
    showDropdown: Boolean,
    setShowDropdown: (Boolean) -> Unit,
    context: Context,
    onScanClicked: () -> Unit,
    onClearClicked: () -> Unit,
    filteredList: List<BulkItem>,
    isLoading: Boolean,
    onItemSelected: (BulkItem) -> Unit
) {
    val gradient = Brush.horizontalGradient(
        listOf(Color(0xFF5231A7), Color(0xFFD32940))
    )
    val MAX_RESULTS = 50
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    val focusRequester = remember { FocusRequester() }

    val query = itemCode.text.trim()

    // ✅ Same filtering logic
    /* val filteredResults = remember(query, filteredList) {
         if (query.isEmpty()) emptyList()
         else {
             when {
                 query.any { it.isDigit() } -> {
                     filteredList.filter { it.rfid?.contains(query, ignoreCase = true) == true }
                 }

                 query.length >= 2 -> {
                     filteredList.filter {
                         it.rfid?.contains(query, ignoreCase = true) == true ||
                                 it.itemCode?.contains(query, ignoreCase = true) == true
                     }
                 }

                 else -> {
                     filteredList.filter { it.itemCode?.contains(query, ignoreCase = true) == true }
                 }
             }
         }
     }*/

    /*    var debouncedQuery by remember { mutableStateOf("") }

        LaunchedEffect(query) {
            delay(300)
            debouncedQuery = query
        }*/
    val debouncedQuery = query
    /*

        val filteredResults = remember(debouncedQuery, filteredList) {
          //  if (debouncedQuery.length < 2) emptyList()
            if (debouncedQuery.isEmpty()) emptyList()
            else {
                filteredList
                    .asSequence()
                  */
    /*  .filter {
                        it.rfid?.contains(debouncedQuery, true) == true ||
                                it.itemCode?.contains(debouncedQuery, true) == true
                    }
                    .take(50)
                    .toList()*//*

                .filter {
                    it.rfid?.contains(debouncedQuery, true) == true ||
                            it.itemCode?.contains(debouncedQuery, true) == true
                }
                .sortedBy {
                    when {
                        it.itemCode?.equals(debouncedQuery, true) == true -> 2   // exact match (lower priority)
                        it.itemCode?.startsWith(debouncedQuery, true) == true -> 0 // best match
                        it.itemCode?.contains(debouncedQuery, true) == true -> 1
                        else -> 3
                    }
                }
                .take(50)
                .toList()
        }
    }
*/

    /* val filteredResults = remember(debouncedQuery, filteredList) {
         if (debouncedQuery.isEmpty()) emptyList()
         else {
             filteredList
                 .asSequence()
                 .filter { item ->
                     val code = item.itemCode?.trim()?.lowercase() ?: ""
                     val rfid = item.rfid?.trim()?.lowercase() ?: ""
                     val query = debouncedQuery.trim().lowercase()

                     code.contains(query) || rfid.contains(query)
                 }
                 .sortedBy {
                     when {
                         it.itemCode?.equals(debouncedQuery, true) == true -> 0
                         it.itemCode?.startsWith(debouncedQuery, true) == true -> 1
                         it.itemCode?.contains(debouncedQuery, true) == true -> 2
                         it.rfid?.contains(debouncedQuery, true) == true -> 3
                         else -> 4
                     }
                 }
                 .take(100)
                 .toList()
         }
     }
 */
    val filteredResults = remember(debouncedQuery, filteredList) {
        val query = debouncedQuery.trim()
        if (query.isEmpty()) {
            emptyList()
        } else {
            filteredList
                .asSequence()
                .filter { item ->
                    item.itemCode.orEmpty().contains(query, ignoreCase = true) ||
                    item.rfid.orEmpty().contains(query, ignoreCase = true) ||
                    item.epc.orEmpty().contains(query, ignoreCase = true)
                }
                .sortedBy { item ->
                    val code = item.itemCode.orEmpty()
                    val rfid = item.rfid.orEmpty()
                    val epc = item.epc.orEmpty()
                    when {
                        code.equals(query, ignoreCase = true) || rfid.equals(query, ignoreCase = true) || epc.equals(query, ignoreCase = true) -> 0
                        code.startsWith(query, ignoreCase = true) || rfid.startsWith(query, ignoreCase = true) || epc.startsWith(query, ignoreCase = true) -> 1
                        code.contains(query, ignoreCase = true) || rfid.contains(query, ignoreCase = true) || epc.contains(query, ignoreCase = true) -> 2
                        else -> 3
                    }
                }
                .take(100)
                .toList()
        }
    }


    val ctx: Context = LocalContext.current
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = if (currentLocales.isEmpty) "en" else currentLocales[0]?.language
    val localizedContext = LocaleHelper.applyLocale(ctx, currentLang ?: "en")

    Column(modifier = Modifier.fillMaxWidth()) {
        //   val shouldExpand = showDropdown && query.isNotEmpty() && (isLoading || filteredResults.isNotEmpty())
        val shouldExpand =
            showDropdown && debouncedQuery.isNotEmpty()
        /* val shouldExpand =
             debouncedQuery.isNotEmpty() &&
                     (isLoading || filteredResults.isNotEmpty())*/
        ExposedDropdownMenuBox(
            expanded = shouldExpand,
            /*  onExpandedChange = { expanded ->
                  if (query.isNotEmpty()) setShowDropdown(expanded) else setShowDropdown(false)
              }*/
            onExpandedChange = { expanded ->
                if (debouncedQuery.isNotEmpty()) {
                    setShowDropdown(expanded)
                } else {
                    setShowDropdown(false)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {

            // 🔹 Input Row (Anchor)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .menuAnchor() // ✅ proper anchor (no overlap)
                    .border(1.dp, gradient, RoundedCornerShape(3.dp))
                    .background(Color.White, RoundedCornerShape(3.dp))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                BasicTextField(
                    value = itemCode,
                    /*  onValueChange = {
                          onItemCodeChange(it)

                          // ✅ IME switching (text<->number) should NOT kill dropdown
                          scope.launch {
                             // delay(60)
                             // setShowDropdown(it.text.isNotEmpty())
                              setShowDropdown(true)
                          }
                      },*/
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
                                    text = localizedContext.getString(R.string.enter_rfid_itemcode),
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                IconButton(
                    onClick = {
                        if (itemCode.text.isNotEmpty()) {
                            onClearClicked()
                            setShowDropdown(false)

                            // ✅ clear → keep keyboard ON for typing next
                            scope.launch {
                                delay(10)
                                focusRequester.requestFocus()
                                keyboardController?.show()
                            }
                        } else {
                            onScanClicked()
                        }
                    },
                    modifier = Modifier.size(26.dp)
                ) {
                    if (itemCode.text.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = localizedContext.getString(R.string.clear),
                            modifier = Modifier.size(18.dp),
                            tint = Color.Gray
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.svg_qr),
                            contentDescription = localizedContext.getString(R.string.scan),
                            modifier = Modifier.size(18.dp),
                            tint = Color.Gray
                        )
                    }
                }
            }

            // 🔽 Dropdown (anchored properly below)
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
                                    text = localizedContext.getString(R.string.no_results_found),
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            },
                            onClick = {}
                        )
                    }

                    else -> {
                        filteredResults.forEach { item ->

                            val displayText = item.itemCode.orEmpty()

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = displayText,
                                        fontSize = 13.sp,
                                        color = Color.Black
                                    )
                                },
                                onClick = {

                                    val selectedText = displayText ?: ""

                                    onItemCodeChange(TextFieldValue(selectedText))

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



/*





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

*/
