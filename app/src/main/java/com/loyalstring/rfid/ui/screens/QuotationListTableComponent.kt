// File: SampleOutListTableComponent.kt
package com.loyalstring.rfid.ui.screens

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.res.painterResource

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.model.quotation.QuotationItem
import com.loyalstring.rfid.data.model.sampleOut.SampleOutDetails
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.viewmodel.OrderViewModel
import com.loyalstring.rfid.viewmodel.SingleProductViewModel
import com.loyalstring.rfid.worker.LocaleHelper

@Composable
fun QuotationListTableComponent(
    productList: SnapshotStateList<QuotationItem>,
    onTotalsChange: (Double, Double, Double) -> Unit = { _, _, _ -> },
    onItemUpdated: (Int, QuotationItem) -> Unit = { _, _ -> },
    onDeleteItem: (Int) -> Unit = {}      // ✅ delete callback
) {
    // 🔹 Shared scroll just for middle columns
    val horizontalScroll = rememberScrollState()

    var selectedItem by remember { mutableStateOf<QuotationItem?>(null) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val orderViewModel: OrderViewModel = hiltViewModel()
    val singleProductViewModel: SingleProductViewModel = hiltViewModel()

    val branchList = singleProductViewModel.branches
    val salesmanList by orderViewModel.empListFlow.collectAsState()
    val userPreferences = UserPreferences.getInstance(context)
    val savedLang = userPreferences.getAppLanguage().ifBlank { "en" }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = currentLocales[0]?.language ?: savedLang
    val localizedContext = LocaleHelper.applyLocale(context, currentLang)

    // 🔹 Header titles & dynamic widths (MUST be same size)
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

    fun columnAlignment(index: Int): TextAlign = when (index) {
        0 -> TextAlign.Start
        else -> TextAlign.End
    }

    @Composable
    fun TableCellText(
        text: String,
        columnIndex: Int,
        color: Color,
        fontWeight: FontWeight = FontWeight.Normal
    ) {
        Text(
            text = text,
            modifier = Modifier
                .width(columnWidths[columnIndex])
                .padding(horizontal = 4.dp),
            fontSize = 11.sp,
            fontWeight = fontWeight,
            color = color,
            maxLines = 1,
            textAlign = columnAlignment(columnIndex)
        )
    }
    // ⚠️ headerTitles.size == columnWidths.size == 9

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(bottom = 5.dp)
    ) {
        // 🔹 HEADER (P Name fixed | middle scroll | Action fixed)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2E2E2E))
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fixed first column: Product Name
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

            // Scrollable middle columns
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontalScroll)
            ) {
                headerTitles.forEachIndexed { index, title ->
                    TableCellText(
                        text = title,
                        columnIndex = index,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Fixed last column: Action
            Text(
                text = localizedContext.getString(R.string.action),
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
            items(productList.size) { index ->
                val item = productList[index]

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (index % 2 == 0) Color(0xFFF4F4F4) else Color.White
                        )

                        .padding(vertical = 3.dp)
                        .clickable {
                            selectedItem = item
                            selectedIndex = index
                            showDialog = true
                            Log.d("SampleOut", "Row clicked index=$index item=${item.ItemCode}")
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Fixed Product Name
                    Text(
                        text = item.ProductName ?: "",
                        modifier = Modifier
                            .width(110.dp)
                            .padding(horizontal = 2.dp),
                        fontSize = 11.sp,
                        color = Color.DarkGray,
                        maxLines = 1
                    )

                    // Scrollable middle cells
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(horizontalScroll)
                    ) {
                        val values = listOf(
                            item.ItemCode ?: "",
                            item.TotalWt ?: "",
                            item.GrossWt ?: "",
                            item.TotalStoneWeight ?: "",
                            item.DiamondWeight ?: "",
                            item.NetWt ?: "",
                            item.FineWastageWt ?: "",
                            item.Quantity.toString(),
                            item.Pieces ?: ""
                        )
                        // ⚠️ values.size MUST be 9

                        values.forEachIndexed { i, value ->
                            TableCellText(
                                text = value,
                                columnIndex = i,
                                color = Color.DarkGray
                            )
                        }
                    }

                    // Fixed Action column (Delete – Edit agar chahiye to yahan add karo)
                    Row(
                        modifier = Modifier
                            .width(40.dp)
                            .padding(horizontal = 4.dp),

                        verticalAlignment = Alignment.CenterVertically

                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_delete), // 🔹 tumhara delete icon
                            contentDescription = localizedContext.getString(R.string.delete),
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    onDeleteItem(index)   // 🔥 parent ko bolo "is index wala item hatao"
                                }
                        )
                    }
                }
            }
        }

        // 🔹 Totals footer (aligned to same columns)
        val totalQty = productList.size
        val totalWt = productList.sumOf { it.TotalWt?.toDoubleOrNull() ?: 0.0 }
        val totalGross = productList.sumOf { it.GrossWt?.toDoubleOrNull() ?: 0.0 }
        val totalStone = productList.sumOf { it.TotalStoneWeight?.toDoubleOrNull() ?: 0.0 }
        val totalDai = productList.sumOf { it.DiamondWeight?.toDoubleOrNull() ?: 0.0 }
        val totalNet = productList.sumOf { it.NetWt?.toDoubleOrNull() ?: 0.0 }
        val totalFinePlusWastage = productList.sumOf { it.FineWastageWt?.toDoubleOrNull() ?: 0.0 }
        val totalPcs = productList.sumOf { it.Pieces?.toDoubleOrNull() ?: 0.0 }
        val totalAmt = productList.sumOf { it.itemAmt?.toDoubleOrNull() ?: 0.0 }

        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2E2E2E))
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Fixed label
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

                // Scrollable totals – EXACTLY 9 values
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(horizontalScroll)
                ) {
                    val totals = listOf(
                        totalQty.toString(),                    // Itemcode col – count
                        "%.3f".format(totalWt),                // T Wt
                        "%.3f".format(totalGross),             // G.Wt
                        "%.3f".format(totalStone),             // S.Wt
                        "%.2f".format(totalDai),               // D Wt
                        "%.2f".format(totalNet),               // N.Wt
                        "%.2f".format(totalFinePlusWastage),   // F+W Wt
                        totalQty.toString(),                   // Qty
                        "%.0f".format(totalPcs)                // Pcs
                    )
                    // ⚠️ totals.size == columnWidths.size == 9

                    totals.forEachIndexed { i, total ->
                        TableCellText(
                            text = total,
                            columnIndex = i,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Fixed last col for footer (blank under "Action")
                Spacer(
                    modifier = Modifier
                        .width(40.dp)
                )
            }
        }

        // 🔹 Edit dialog (agar use karna hai to yahan rakho)
        if (showDialog && selectedItem != null && selectedIndex != null) {
            QuotationDialogEditAndDisplay(
                selectedItem = selectedItem,
                branchList = branchList,
                salesmanList = salesmanList,
                onDismiss = { showDialog = false },
                onSave = { updatedChallan ->
                    showDialog = false
                    onItemUpdated(selectedIndex!!, updatedChallan)
                }
            )
        }
        Spacer(modifier = Modifier.height(5.dp))

        DeliveryChallanSummaryRow(
            totalAmount = totalAmt,
            onAmountsChange = { gst, final ->
                onTotalsChange(totalAmt, gst, final)
            }
        )
    }
}

/*
 Add the following strings to res/values/strings.xml

 <resources>
     <string name="product_name">Product Name</string>
     <string name="itemcode">Itemcode</string>
     <string name="t_wt">T Wt</string>
     <string name="g_wt">G.Wt</string>
     <string name="s_wt">S.Wt</string>
     <string name="d_wt">D Wt</string>
     <string name="n_wt">N.Wt</string>
     <string name="fw_wt">F+W Wt</string>
     <string name="qty">Qty</string>
     <string name="pcs">Pcs</string>
     <string name="action">Action</string>
     <string name="total">Total</string>
     <string name="delete">Delete</string>
 </resources>
*/
