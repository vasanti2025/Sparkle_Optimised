package com.loyalstring.rfid.ui.screens

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.model.sampleOut.IssueItemDto
import com.loyalstring.rfid.data.model.sampleOut.SampleOutListResponse
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.viewmodel.OrderViewModel
import com.loyalstring.rfid.viewmodel.SingleProductViewModel
import com.loyalstring.rfid.worker.LocaleHelper

private data class RowItem(
    val parentIndex: Int,
    val childIndex: Int,
    val parent: SampleOutListResponse,
    val issue: IssueItemDto
)

@Composable
fun SampleInListTableComponent(
    productList: SnapshotStateList<SampleOutListResponse>,
    scannedItemCodes: Set<String>,

    isReturnMode: Boolean,
    onReturnModeChange: (Boolean) -> Unit,  // ✅ ADD THIS

    selectedReturnItemCodes: Set<String>,
    onSelectedReturnItemCodesChange: (Set<String>) -> Unit,

    onItemUpdated: (Int, SampleOutListResponse) -> Unit = { _, _ -> },
    onDeleteItem: (Int) -> Unit = {}
) {
    val horizontalScroll = rememberScrollState()

    var selectedItem by remember { mutableStateOf<SampleOutListResponse?>(null) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val orderViewModel: OrderViewModel = hiltViewModel()
    val singleProductViewModel: SingleProductViewModel = hiltViewModel()

    // kept (as in your code)
    val branchList = singleProductViewModel.branches
    val salesmanList by orderViewModel.empListFlow.collectAsState()

    val userPreferences = UserPreferences.getInstance(context)
    val savedLang = userPreferences.getAppLanguage().ifBlank { "en" }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = currentLocales[0]?.language ?: savedLang
    val localizedContext = LocaleHelper.applyLocale(context, currentLang)

    fun norm(v: String?) = v?.trim()?.uppercase()?.replace(" ", "") ?: ""
    fun safeD(v: String?): Double = v?.toDoubleOrNull() ?: 0.0
    fun safeI(v: Any?): Int = when (v) {
        is Int -> v
        is String -> v.toIntOrNull() ?: 0
        else -> 0
    }

    // ✅ normalize scanned item codes
    val scannedNormalized = remember(scannedItemCodes) {
        scannedItemCodes.map { norm(it) }.toSet()
    }

    // ✅ normalize selected return item codes
    val selectedReturnNormalized = remember(selectedReturnItemCodes) {
        selectedReturnItemCodes.map { norm(it) }.toSet()
    }

    // ✅ Flatten all IssueItems -> show all rows
    val rows by remember {
        derivedStateOf {
            productList.flatMapIndexed { pIndex, p ->
                p.IssueItems.orEmpty().mapIndexed { cIndex, issue ->
                    RowItem(pIndex, cIndex, p, issue)
                }
            }
        }
    }

    // ✅ Match / Not Match count (based on issue.ItemCode)
    val matchCount = rows.count { norm(it.issue.ItemCode) in scannedNormalized }
    val notMatchCount = rows.size - matchCount

    val headerTitles = listOf(
        localizedContext.getString(R.string.itemcode),
        localizedContext.getString(R.string.t_wt),
        localizedContext.getString(R.string.g_wt),
        localizedContext.getString(R.string.s_wt),
        localizedContext.getString(R.string.d_wt),
        localizedContext.getString(R.string.n_wt),
        localizedContext.getString(R.string.fw_wt),
        localizedContext.getString(R.string.qty),
        localizedContext.getString(R.string.pcs)
    )

    val columnWidths = listOf(
        90.dp, // Itemcode
        60.dp, // T Wt
        60.dp, // G.Wt
        60.dp, // S.Wt
        60.dp, // D Wt
        60.dp, // N.Wt
        70.dp, // F+W Wt
        50.dp, // Qty
        50.dp  // Pcs
    )

    // ✅ Totals (row-wise + parent totals)
    val totalRows = rows.size
    val totalWt = productList.sumOf { safeD(it.TotalWt) }
    val totalStone = productList.sumOf { safeD(it.TotalStoneWeight) }
    val totalFinePlusWastage = productList.sumOf { safeD(it.FineWastageWt) }

    val totalGross = rows.sumOf { safeD(it.issue.GrossWt) }
    val totalNet = rows.sumOf { safeD(it.issue.NetWt) }
    val totalDai = rows.sumOf { safeD(it.issue.DiamondWeight) }
    val totalPcs = rows.sumOf { safeD(it.issue.Pieces) }
    val totalQty = rows.sumOf { safeI(it.issue.Quantity) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(bottom = 5.dp)
    ) {

        // 🔹 TABLE HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2E2E2E))
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = localizedContext.getString(R.string.product_name),
                modifier = Modifier
                    .width(110.dp)
                    .padding(horizontal = 2.dp),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScroll)
            ) {
                headerTitles.forEachIndexed { index, title ->
                    Text(
                        text = title,
                        modifier = Modifier
                            .width(columnWidths[index])
                            .padding(horizontal = 2.dp),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }

            Text(
                text = localizedContext.getString(R.string.status_header),
                modifier = Modifier
                    .width(40.dp)
                    .padding(horizontal = 1.dp),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }

        // 🔹 DATA ROWS
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(
                items = rows,
                key = { "${it.parentIndex}_${it.childIndex}_${it.issue.ItemCode}" }
            ) { row ->
                val codeNorm = norm(row.issue.ItemCode)
                val isMatched = codeNorm.isNotEmpty() && codeNorm in scannedNormalized

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if ((row.parentIndex + row.childIndex) % 2 == 0) Color(0xFFF4F4F4) else Color.White
                        )
                        .padding(vertical = 3.dp)
                        .clickable(enabled = !isReturnMode) {
                            // ✅ keep your edit-click only in normal mode
                            selectedItem = row.parent
                            selectedIndex = row.parentIndex
                            showDialog = true
                            Log.d("SampleIn", "Row clicked code=${row.issue.ProductName}")
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ✅ Product Name (parent) - change this to your real field if needed
                    Text(
                        text = row.issue.ProductName?:"-", // if ProductName exists
                        modifier = Modifier
                            .width(110.dp)
                            .padding(horizontal = 2.dp),
                        fontSize = 11.sp,
                        color = Color.DarkGray,
                        maxLines = 1
                    )

                    // scroll columns
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(horizontalScroll)
                    ) {
                        val values = listOf(
                            row.issue.ItemCode ?: "",
                            row.parent.TotalWt ?: "",
                            row.issue.GrossWt ?: "",
                            row.parent.TotalStoneWeight ?: "",
                            row.issue.DiamondWeight ?: "",
                            row.issue.NetWt ?: "",
                            row.parent.FineWastageWt ?: "",
                            (row.issue.Quantity?.toString() ?: ""),
                            row.issue.Pieces ?: ""
                        )

                        values.forEachIndexed { i, value ->
                            Text(
                                text = value,
                                modifier = Modifier
                                    .width(columnWidths[i])
                                    .padding(horizontal = 2.dp),
                                fontSize = 11.sp,
                                color = Color.DarkGray,
                                maxLines = 1
                            )
                        }
                    }

                    // ✅ Status / Checkbox
                    Row(
                        modifier = Modifier
                            .width(40.dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (isReturnMode) {
                            // ✅ Return mode: show checkbox ONLY for matched items (enabled),
                            // unmatched stays disabled (optional) or you can show ❌ only.
                            if (isMatched) {
                                val checked = codeNorm in selectedReturnNormalized
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { newValue ->
                                        val updated = selectedReturnNormalized.toMutableSet()
                                        if (newValue) updated.add(codeNorm) else updated.remove(codeNorm)
                                        onSelectedReturnItemCodesChange(updated)
                                    }
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Cancel,
                                    contentDescription = "Not Matched",
                                    tint = Color(0xFFD32F2F),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            // ✅ Normal mode: show green/red icon
                            Icon(
                                imageVector = if (isMatched) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                contentDescription = if (isMatched) "Matched" else "Not Matched",
                                tint = if (isMatched) Color(0xFF1B8F3A) else Color(0xFFD32F2F),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // 🔹 TOTAL FOOTER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2E2E2E))
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = localizedContext.getString(R.string.total),
                modifier = Modifier
                    .width(110.dp)
                    .padding(horizontal = 2.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScroll)
            ) {
                val totals = listOf(
                    totalRows.toString(),
                    "%.3f".format(totalWt),
                    "%.3f".format(totalGross),
                    "%.3f".format(totalStone),
                    "%.2f".format(totalDai),
                    "%.2f".format(totalNet),
                    "%.2f".format(totalFinePlusWastage),
                    totalQty.toString(),
                    "%.0f".format(totalPcs)
                )

                totals.forEachIndexed { i, total ->
                    Text(
                        text = total,
                        modifier = Modifier
                            .width(columnWidths[i])
                            .padding(horizontal = 2.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(40.dp))
        }

        // ✅ MATCH / NOT MATCH chips (show only after scan)
        if (scannedItemCodes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CountChip(
                    title = localizedContext.getString(R.string.return_label),
                    count = matchCount,
                    badgeColor = Color(0xFF7B1FA2),
                    modifier = Modifier.weight(1f),
                    enabled = matchCount > 0,
                    onClick = {
                        onReturnModeChange(true) // ✅
                        onSelectedReturnItemCodesChange(emptySet()) // ✅ clear selection
                    }
                )

                CountChip(
                    title = localizedContext.getString(R.string.non_return_label),
                    count = notMatchCount,
                    badgeColor = Color(0xFF7B1FA2),
                    modifier = Modifier.weight(1f),
                    enabled = notMatchCount > 0,
                    onClick = {
                        onReturnModeChange(false) // ✅
                        onSelectedReturnItemCodesChange(emptySet())
                    }
                )
            }

        }

        // 🔹 Your dialog logic (keep if needed)
        // if (showDialog && selectedItem != null && selectedIndex != null) { ... }
    }
}

@Composable
private fun CountChip(
    title: String,
    count: Int,
    badgeColor: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF2F2F2))
            .then(
                if (onClick != null)
                    Modifier.clickable(enabled = enabled) { onClick() }
                else
                    Modifier
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            color = if (enabled) Color.Black else Color.Gray
        )

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(badgeColor)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = count.toString(),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}



/*
package com.loyalstring.rfid.ui.screens

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.model.sampleOut.IssueItemDto
import com.loyalstring.rfid.data.model.sampleOut.SampleOutListResponse
import com.loyalstring.rfid.viewmodel.OrderViewModel
import com.loyalstring.rfid.viewmodel.SingleProductViewModel
import com.loyalstring.rfid.worker.LocaleHelper

private data class RowItem(
    val parentIndex: Int,
    val childIndex: Int,
    val parent: SampleOutListResponse,
    val issue: IssueItemDto
)

@Composable
fun SampleInListTableComponent(
    productList: SnapshotStateList<SampleOutListResponse>,
    scannedItemCodes: Set<String>, // ✅ parent se pass (ItemCode set)
    onItemUpdated: (Int, SampleOutListResponse) -> Unit = { _, _ -> },
    onDeleteItem: (Int) -> Unit = {}
) {
    val horizontalScroll = rememberScrollState()

    var selectedItem by remember { mutableStateOf<SampleOutListResponse?>(null) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val orderViewModel: OrderViewModel = hiltViewModel()
    val singleProductViewModel: SingleProductViewModel = hiltViewModel()

    // not used now but kept (as you had)
    val branchList = singleProductViewModel.branches
    val salesmanList by orderViewModel.empListFlow.collectAsState()

    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = if (currentLocales.isEmpty) "en" else currentLocales[0]?.language
    val localizedContext = LocaleHelper.applyLocale(context, currentLang ?: "en")

    fun norm(v: String?) = v?.trim()?.uppercase()?.replace(" ", "") ?: ""
    fun safeD(v: String?): Double = v?.toDoubleOrNull() ?: 0.0
    fun safeI(v: Any?): Int = when (v) {
        is Int -> v
        is String -> v.toIntOrNull() ?: 0
        else -> 0
    }

    // ✅ normalize scanned item codes
    val scannedNormalized = remember(scannedItemCodes) {
        scannedItemCodes.map { norm(it) }.toSet()
    }

    // ✅ Flatten all IssueItems -> show all rows
    val rows by remember {
        derivedStateOf {
            productList.flatMapIndexed { pIndex, p ->
                p.IssueItems.orEmpty().mapIndexed { cIndex, issue ->
                    RowItem(pIndex, cIndex, p, issue)
                }
            }
        }
    }
    // ✅ Match / Not Match count (based on issue.ItemCode)
    val matchCount = rows.count { norm(it.issue.ItemCode) in scannedNormalized }
    val notMatchCount = rows.size - matchCount

    val headerTitles = listOf(
        localizedContext.getString(R.string.itemcode),
        localizedContext.getString(R.string.t_wt),
        localizedContext.getString(R.string.g_wt),
        localizedContext.getString(R.string.s_wt),
        localizedContext.getString(R.string.d_wt),
        localizedContext.getString(R.string.n_wt),
        localizedContext.getString(R.string.fw_wt),
        localizedContext.getString(R.string.qty),
        localizedContext.getString(R.string.pcs)
    )

    val columnWidths = listOf(
        90.dp, // Itemcode
        60.dp, // T Wt
        60.dp, // G.Wt
        60.dp, // S.Wt
        60.dp, // D Wt
        60.dp, // N.Wt
        70.dp, // F+W Wt
        50.dp, // Qty
        50.dp  // Pcs
    )

    // ✅ Totals (row-wise + parent totals)
    val totalRows = rows.size
    val totalWt = productList.sumOf { safeD(it.TotalWt) }
    val totalStone = productList.sumOf { safeD(it.TotalStoneWeight) }
    val totalFinePlusWastage = productList.sumOf { safeD(it.FineWastageWt) }

    val totalGross = rows.sumOf { safeD(it.issue.GrossWt) }
    val totalNet = rows.sumOf { safeD(it.issue.NetWt) }
    val totalDai = rows.sumOf { safeD(it.issue.DiamondWeight) }
    val totalPcs = rows.sumOf { safeD(it.issue.Pieces) }
    val totalQty = rows.sumOf { safeI(it.issue.Quantity) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(bottom = 5.dp)
    ) {

        // 🔹 TABLE HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2E2E2E))
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = localizedContext.getString(R.string.product_name),
                modifier = Modifier
                    .width(110.dp)
                    .padding(horizontal = 2.dp),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScroll)
            ) {
                headerTitles.forEachIndexed { index, title ->
                    Text(
                        text = title,
                        modifier = Modifier
                            .width(columnWidths[index])
                            .padding(horizontal = 2.dp),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }

            Text(
                text = localizedContext.getString(R.string.status_header),
                modifier = Modifier
                    .width(40.dp)
                    .padding(horizontal = 1.dp),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }

        // 🔹 DATA ROWS
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(
                items = rows,
                key = { "${it.parentIndex}_${it.childIndex}_${it.issue.ItemCode}" }
            ) { row ->
                val code = norm(row.issue.ItemCode)
                val isMatched = code.isNotEmpty() && code in scannedNormalized

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if ((row.parentIndex + row.childIndex) % 2 == 0) Color(0xFFF4F4F4) else Color.White
                        )
                        .padding(vertical = 3.dp)
                        .clickable {
                            selectedItem = row.parent
                            selectedIndex = row.parentIndex
                            showDialog = true
                            Log.d("SampleIn", "Row clicked code=${row.issue.ItemCode}")
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ✅ Product Name (parent)
                    Text(
                        text = "" ?: "", // ✅ yahi show hoga
                        modifier = Modifier
                            .width(110.dp)
                            .padding(horizontal = 2.dp),
                        fontSize = 11.sp,
                        color = Color.DarkGray,
                        maxLines = 1
                    )

                    // scroll columns
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(horizontalScroll)
                    ) {
                        val values = listOf(
                            row.issue.ItemCode ?: "",
                            row.parent.TotalWt ?: "",
                            row.issue.GrossWt ?: "",
                            row.parent.TotalStoneWeight ?: "",
                            row.issue.DiamondWeight ?: "",
                            row.issue.NetWt ?: "",
                            row.parent.FineWastageWt ?: "",
                            (row.issue.Quantity?.toString() ?: ""),
                            row.issue.Pieces ?: ""
                        )

                        values.forEachIndexed { i, value ->
                            Text(
                                text = value,
                                modifier = Modifier
                                    .width(columnWidths[i])
                                    .padding(horizontal = 2.dp),
                                fontSize = 11.sp,
                                color = Color.DarkGray,
                                maxLines = 1
                            )
                        }
                    }

                    // ✅ Status
                    Row(
                        modifier = Modifier
                            .width(40.dp)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isMatched) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                            contentDescription = if (isMatched) "Matched" else "Not Matched",
                            tint = if (isMatched) Color(0xFF1B8F3A) else Color(0xFFD32F2F),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 🔹 TOTAL FOOTER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2E2E2E))
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = localizedContext.getString(R.string.total),
                modifier = Modifier
                    .width(110.dp)
                    .padding(horizontal = 2.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScroll)
            ) {
                val totals = listOf(
                    totalRows.toString(),
                    "%.3f".format(totalWt),
                    "%.3f".format(totalGross),
                    "%.3f".format(totalStone),
                    "%.2f".format(totalDai),
                    "%.2f".format(totalNet),
                    "%.2f".format(totalFinePlusWastage),
                    totalQty.toString(),
                    "%.0f".format(totalPcs)
                )

                totals.forEachIndexed { i, total ->
                    Text(
                        text = total,
                        modifier = Modifier
                            .width(columnWidths[i])
                            .padding(horizontal = 2.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(40.dp))
        }

        // ✅ MATCH / NOT MATCH chips (show only after scan)
        if (scannedItemCodes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CountChip(
                    title = localizedContext.getString(R.string.return_label),
                    count = matchCount,
                    badgeColor = Color(0xFF7B1FA2),
                    modifier = Modifier.weight(1f)
                )
                CountChip(
                    title = localizedContext.getString(R.string.non_return_label),
                    count = notMatchCount,
                    badgeColor = Color(0xFF7B1FA2),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CountChip(
    title: String,
    count: Int,
    badgeColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF2F2F2))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text = title, fontSize = 12.sp, color = Color.Black)

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(badgeColor)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = count.toString(),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

*/
/*
package com.loyalstring.rfid.ui.screens

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.model.sampleOut.SampleOutListResponse
import com.loyalstring.rfid.viewmodel.OrderViewModel
import com.loyalstring.rfid.viewmodel.SingleProductViewModel
import com.loyalstring.rfid.worker.LocaleHelper

@Composable
fun SampleInListTableComponent(
    productList: SnapshotStateList<SampleOutListResponse>,
    scannedItemCodes: Set<String>, // ✅ pass from parent; scan ke baad update hoga
    onItemUpdated: (Int, SampleOutListResponse) -> Unit = { _, _ -> },
    onDeleteItem: (Int) -> Unit = {}
) {
    val horizontalScroll = rememberScrollState()

    var selectedItem by remember { mutableStateOf<SampleOutListResponse?>(null) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val orderViewModel: OrderViewModel = hiltViewModel()
    val singleProductViewModel: SingleProductViewModel = hiltViewModel()

    val branchList = singleProductViewModel.branches
    val salesmanList by orderViewModel.empListFlow.collectAsState()

    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = if (currentLocales.isEmpty) "en" else currentLocales[0]?.language
    val localizedContext = LocaleHelper.applyLocale(context, currentLang ?: "en")

    // ✅ normalize scanned codes
    val scannedNormalized = remember(scannedItemCodes) {
        scannedItemCodes.map { it.trim().uppercase() }.toSet()
    }

    val headerTitles = listOf(
        localizedContext.getString(R.string.itemcode),
        localizedContext.getString(R.string.t_wt),
        localizedContext.getString(R.string.g_wt),
        localizedContext.getString(R.string.s_wt),
        localizedContext.getString(R.string.d_wt),
        localizedContext.getString(R.string.n_wt),
        localizedContext.getString(R.string.fw_wt),
        localizedContext.getString(R.string.qty),
        localizedContext.getString(R.string.pcs)
    )

    val columnWidths = listOf(
        90.dp, // Itemcode
        60.dp, // T Wt
        60.dp, // G.Wt
        60.dp, // S.Wt
        60.dp, // D Wt
        60.dp, // N.Wt
        70.dp, // F+W Wt
        50.dp, // Qty
        50.dp  // Pcs
    )

    // ✅ helper
    fun safeD(v: String?): Double = v?.toDoubleOrNull() ?: 0.0

    // ✅ totals should be based on IssueItems (because we show IssueItems rows)
    val allIssueItems = remember(productList) {
        productList.flatMap { it.IssueItems ?: emptyList() }
    }

    val totalRows = allIssueItems.size
    val totalWt = productList.sumOf { safeD(it.TotalWt) }                 // parent-level total
    val totalStone = productList.sumOf { safeD(it.TotalStoneWeight) }     // parent-level total
    val totalFinePlusWastage = productList.sumOf { safeD(it.FineWastageWt) }

    val totalGross = allIssueItems.sumOf { safeD(it.GrossWt) }
    val totalNet = allIssueItems.sumOf { safeD(it.NetWt) }
    val totalDai = allIssueItems.sumOf { safeD(it.DiamondWeight) }
    val totalPcs = allIssueItems.sumOf { safeD(it.Pieces) }
    val totalQty = allIssueItems.sumOf { (it.Quantity ?: 0) } // if qty is Int? else adjust

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(bottom = 5.dp)
    ) {
        // 🔹 HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2E2E2E))
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = localizedContext.getString(R.string.product_name),
                modifier = Modifier
                    .width(110.dp)
                    .padding(horizontal = 2.dp),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScroll)
            ) {
                headerTitles.forEachIndexed { index, title ->
                    Text(
                        text = title,
                        modifier = Modifier
                            .width(columnWidths[index])
                            .padding(horizontal = 2.dp),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }

            Text(
                text = localizedContext.getString(R.string.status_header),
                modifier = Modifier
                    .width(40.dp)
                    .padding(horizontal = 1.dp),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }

        // 🔹 DATA ROWS (✅ show ALL IssueItems)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            productList.forEachIndexed { parentIndex, parent ->
                val childList = parent.IssueItems ?: emptyList()

                childList.forEachIndexed { childIndex, issueItem ->
                    item(key = "${parentIndex}_${childIndex}_${issueItem.ItemCode}") {
                        val code = (issueItem.ItemCode ?: "").trim().uppercase()
                        val isScanned = code.isNotEmpty() && code in scannedNormalized

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if ((parentIndex + childIndex) % 2 == 0) Color(0xFFF4F4F4) else Color.White
                                )
                                .padding(vertical = 3.dp)
                                .clickable {
                                    selectedItem = parent
                                    selectedIndex = parentIndex
                                    showDialog = true
                                    Log.d(
                                        "SampleIn",
                                        "Row clicked parent=$parentIndex child=$childIndex code=${issueItem.ItemCode}"
                                    )
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // fixed product name (parent)
                            Text(
                                text = "" ?: "",
                                modifier = Modifier
                                    .width(110.dp)
                                    .padding(horizontal = 2.dp),
                                fontSize = 11.sp,
                                color = Color.DarkGray,
                                maxLines = 1
                            )

                            // scrollable columns (mix parent + child)
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(horizontalScroll)
                            ) {
                                val values = listOf(
                                    issueItem.ItemCode ?: "",
                                    parent.TotalWt ?: "",
                                    issueItem.GrossWt ?: "",
                                    parent.TotalStoneWeight ?: "",
                                    issueItem.DiamondWeight ?: "",
                                    issueItem.NetWt ?: "",
                                    parent.FineWastageWt ?: "",
                                    (issueItem.Quantity?.toString() ?: ""),
                                    issueItem.Pieces ?: ""
                                )

                                values.forEachIndexed { i, value ->
                                    Text(
                                        text = value,
                                        modifier = Modifier
                                            .width(columnWidths[i])
                                            .padding(horizontal = 2.dp),
                                        fontSize = 11.sp,
                                        color = Color.DarkGray,
                                        maxLines = 1
                                    )
                                }
                            }

                            // status
                            Row(
                                modifier = Modifier
                                    .width(40.dp)
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isScanned) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                    contentDescription = if (isScanned) "Scanned" else "Not Scanned",
                                    tint = if (isScanned) Color(0xFF1B8F3A) else Color(0xFFD32F2F)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 🔹 TOTAL FOOTER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2E2E2E))
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = localizedContext.getString(R.string.total),
                modifier = Modifier
                    .width(110.dp)
                    .padding(horizontal = 2.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScroll)
            ) {
                val totals = listOf(
                    totalRows.toString(),                 // Itemcode col -> row count
                    "%.3f".format(totalWt),              // T Wt (parent total)
                    "%.3f".format(totalGross),           // G.Wt (child sum)
                    "%.3f".format(totalStone),           // S.Wt (parent total)
                    "%.2f".format(totalDai),             // D Wt (child sum)
                    "%.2f".format(totalNet),             // N.Wt (child sum)
                    "%.2f".format(totalFinePlusWastage), // F+W Wt (parent total)
                    totalQty.toString(),                 // Qty (child sum)
                    "%.0f".format(totalPcs)              // Pcs (child sum)
                )

                totals.forEachIndexed { i, total ->
                    Text(
                        text = total,
                        modifier = Modifier
                            .width(columnWidths[i])
                            .padding(horizontal = 2.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(40.dp))
        }

        // 🔹 Edit dialog (same as before)
      *//*

*/
/*  if (showDialog && selectedItem != null && selectedIndex != null) {
            SampleOutDialogEditAndDisplay(
                selectedItem = selectedItem,
                branchList = branchList,
                salesmanList = salesmanList,
                onDismiss = { showDialog = false },
                onSave = { updatedChallan ->
                    showDialog = false
                    onItemUpdated(selectedIndex!!, updatedChallan)
                }
            )
        }*//*
*/
/*

    }
}
*/

