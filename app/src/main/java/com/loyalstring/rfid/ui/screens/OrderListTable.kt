// File: DeliveryChallanItemListTable.kt
package com.loyalstring.rfid.ui.screens

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.loyalstring.rfid.R

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loyalstring.rfid.data.model.deliveryChallan.ChallanDetails
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.loyalstring.rfid.data.local.entity.OrderItem
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.viewmodel.OrderViewModel
import com.loyalstring.rfid.viewmodel.SingleProductViewModel
import com.loyalstring.rfid.worker.LocaleHelper

@Composable
fun OrderListTable(
    onTotalsChange: (baseTotal: Double, gstAmount: Double, finalTotal: Double) -> Unit = { _, _, _ -> },
    onItemUpdated: (index: Int, updated: OrderItem) -> Unit = { _, _ -> },
    onDeleteItem: (Int) -> Unit = {},
    productList: List<OrderItem>
) {
    val horizontalScroll = rememberScrollState()
    var selectedItem by remember { mutableStateOf<OrderItem?>(null) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    val orderViewModel: OrderViewModel = hiltViewModel()
    val singleProductViewModel: SingleProductViewModel = hiltViewModel()

    val context = LocalContext.current
    val branchList = singleProductViewModel.branches
    val salesmanList by orderViewModel.empListFlow.collectAsState()
    val userPreferences = UserPreferences.getInstance(context)
    val savedLang = userPreferences.getAppLanguage().ifBlank { "en" }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = currentLocales[0]?.language ?: savedLang
    val localizedContext = LocaleHelper.applyLocale(context, currentLang)
    // header titles come from strings.xml (translated)
    val headerTitles = listOf(
        localizedContext.getString(R.string.product_name_short),
        localizedContext.getString(R.string.itemcode),
        localizedContext.getString(R.string.g_wt),
        localizedContext.getString(R.string.n_wt),
        localizedContext.getString(R.string.fw_wt),
        localizedContext.getString(R.string.s_amt),
        localizedContext.getString(R.string.d_amt),
        localizedContext.getString(R.string.item_amt),
        localizedContext.getString(R.string.rfid_code)
    )

    val cellWidth = 70.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(bottom = 5.dp)
    ) {
        // 🔹 Scrollable content (Header + Data)
    /*    Box(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(horizontalScroll)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header
                item {
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF2E2E2E))
                            .padding(vertical = 4.dp)
                    ) {
                        headerTitles.forEach { title ->
                            Text(
                                text = title,
                                modifier = Modifier
                                    .width(cellWidth)
                                    .padding(horizontal = 2.dp),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }



                // Data rows
                items(productList.size) { index ->
                    val item = productList[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (index % 2 == 0) Color(0xFFF4F4F4) else Color.White)
                            .padding(vertical = 3.dp)
                            .clickable {
                                selectedItem = item
                                selectedIndex = index
                                showDialog = true
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            item.productName ?: "",
                            item.itemCode ?: "",
                            item.grWt ?: "",
                            item.nWt ?: "",
                            item.finePlusWt ?: "",
                            item.stoneAmt ?: "",
                            item.diamondAmt ?: "",
                            (item.itemAmt) ?: "",
                            item.rfidCode ?: ""
                        ).forEach { value ->
                            Text(
                                text = value,
                                modifier = Modifier
                                    .width(cellWidth)
                                    .padding(horizontal = 2.dp),
                                fontSize = 11.sp,
                                color = Color.DarkGray,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
*/
        Column(modifier = Modifier.weight(1f)) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2E2E2E))
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(horizontalScroll)
                ) {
                    headerTitles.forEach { title ->
                        Text(
                            text = title,
                            modifier = Modifier.width(cellWidth).padding(horizontal = 2.dp),
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Text(
                    text = localizedContext.getString(R.string.action),
                    modifier = Modifier.width(45.dp),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(productList.size) { index ->
                    val item = productList[index]

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (index % 2 == 0) Color(0xFFF4F4F4) else Color.White)
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(horizontalScroll)
                                .clickable {
                                    selectedItem = item
                                    selectedIndex = index
                                    showDialog = true
                                }
                        ) {
                            listOf(
                                item.productName ?: "",
                                item.itemCode ?: "",
                                item.grWt ?: "",
                                item.nWt ?: "",
                                item.finePlusWt ?: "",
                                item.stoneAmt ?: "",
                                item.diamondAmt ?: "",
                                item.itemAmt ?: "",
                                item.rfidCode ?: ""
                            ).forEach { value ->
                                Text(
                                    text = value,
                                    modifier = Modifier.width(cellWidth).padding(horizontal = 2.dp),
                                    fontSize = 11.sp,
                                    color = Color.DarkGray,
                                    maxLines = 1
                                )
                            }
                        }

                        Image(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = localizedContext.getString(R.string.delete),
                            modifier = Modifier
                                .width(45.dp)
                                .size(18.dp)
                                .clickable {
                                    onDeleteItem(index)
                                }
                        )
                    }
                }
            }
        }
        // 🔹 Fixed Footer Row (Totals)
        val totalQty = productList.size
        val totalGross = productList.sumOf { it.grWt?.toDoubleOrNull() ?: 0.0 }
        val totalNet = productList.sumOf { it.nWt?.toDoubleOrNull() ?: 0.0 }
        val totalFine = productList.sumOf { it.makingFixedWastage?.toDoubleOrNull() ?: 0.0 }
        val totalAmt = productList.sumOf { it.itemAmt?.toDoubleOrNull() ?: 0.0 }

        val totalDaimondAmt = productList.sumOf { it.diamondAmt?.toDoubleOrNull() ?: 0.0 }
        val totalStoneAmt = productList.sumOf { it.stoneAmt?.toDoubleOrNull() ?: 0.0 }

        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScroll)
                    .background(Color(0xFF2E2E2E))
                    .padding(vertical = 6.dp)
            ) {
                Row {
                    listOf(
                        localizedContext.getString(R.string.total),
                        totalQty.toString(),
                        "%.3f".format(totalGross),
                        "%.3f".format(totalNet),
                        "%.3f".format(totalFine),
                        "%.2f".format(totalStoneAmt),
                        "%.2f".format(totalDaimondAmt),
                        "%.2f".format(totalAmt),
                        "%.2f".format(totalAmt)
                    ).forEach { total ->
                        Text(
                            text = total,
                            modifier = Modifier
                                .width(cellWidth)
                                .padding(horizontal = 2.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
                Spacer(modifier = Modifier.width(45.dp))
            }

            // optional dialog for editing a row
            /*
            if (showDialog && selectedItem != null) {
                DeliveryChallanDialogEditAndDisplay(
                    selectedItem = selectedItem,
                    branchList = branchList,
                    salesmanList = salesmanList,
                    onDismiss = { showDialog = false },
                    onSave = { updatedItem ->
                        showDialog = false
                        selectedIndex?.let { onItemUpdated(it, updatedItem) }
                    }
                )
            }
            */

            if (showDialog && selectedItem != null && selectedIndex != null) {
                OrderDetailsDialogEditAndDisplay(
                    selectedItem = selectedItem,
                    branchList = branchList,
                   // salesmanList = salesmanList,
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
}

