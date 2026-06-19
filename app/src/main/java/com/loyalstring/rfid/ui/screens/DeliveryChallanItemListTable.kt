// File: DeliveryChallanItemListTable.kt
package com.loyalstring.rfid.ui.screens
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
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
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.GradientButton
import com.loyalstring.rfid.viewmodel.OrderViewModel
import com.loyalstring.rfid.viewmodel.SingleProductViewModel
import com.loyalstring.rfid.worker.LocaleHelper

@Composable
fun DeliveryChallanItemListTable(
    productList: List<ChallanDetails>,
    onTotalsChange: (baseTotal: Double, gstAmount: Double, finalTotal: Double) -> Unit = { _, _, _ -> },
    onItemUpdated: (index: Int, updated: ChallanDetails) -> Unit = { _, _ -> },
    onDeleteItem: (index: Int) -> Unit = {}
) {
    val horizontalScroll = rememberScrollState()
    var selectedItem by remember { mutableStateOf<ChallanDetails?>(null) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var deleteIndex by remember { mutableStateOf<Int?>(null) }
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
        // Single vertical scroll for header + rows; middle columns scroll horizontally.
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            item {
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
                        modifier = Modifier.width(cellWidth),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

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
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                item.ProductName ?: "",
                                item.ItemCode ?: "",
                                item.GrossWt ?: "",
                                item.NetWt ?: "",
                                item.FineWastageWt ?: "",
                                item.StoneAmount ?: "",
                                item.DiamondSellAmount ?: "",
                                item.ItemAmount ?: "",
                                item.RFIDCode ?: ""
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

                        Box(
                            modifier = Modifier
                                .width(cellWidth)
                                .height(30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            IconButton(onClick = { deleteIndex = index }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_delete),
                                    contentDescription = "Delete",
                                    tint = Color.Red,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                    }
                }
            }
        }

        // 🔹 Fixed Footer Row (Totals)
        val totalQty = productList.size
        val totalGross = productList.sumOf { it.GrossWt?.toDoubleOrNull() ?: 0.0 }
        val totalNet = productList.sumOf { it.NetWt?.toDoubleOrNull() ?: 0.0 }
        val totalFine = productList.sumOf { it.FineWastageWt?.toDoubleOrNull() ?: 0.0 }
        val totalAmt = productList.sumOf { it.ItemAmount?.toDoubleOrNull() ?: 0.0 }
        val totdiamondAmt = productList.sumOf { it.DiamondAmt?.toDoubleOrNull() ?: 0.0 }
        val totstoneAMt = productList.sumOf { it.StoneAmount?.toDoubleOrNull() ?: 0.0 }

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
                        "%.2f".format(totstoneAMt),
                        "%.2f".format(totdiamondAmt),
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

            if (deleteIndex != null) {
                AlertDialog(
                    onDismissRequest = { deleteIndex = null },
                    title = {
                        Text(localizedContext.getString(R.string.confirm_delete))
                    },
                    text = {
                        Text(localizedContext.getString(R.string.delete_confirmation_message))
                    },
                    confirmButton = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GradientButton(
                                text = localizedContext.getString(R.string.cancel),
                                onClick = { deleteIndex = null },
                                modifier = Modifier.weight(1f)
                            )
                            GradientButton(
                                text = localizedContext.getString(R.string.yes),
                                onClick = {
                                    deleteIndex?.let { onDeleteItem(it) }
                                    deleteIndex = null
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                )
            }

            if (showDialog && selectedItem != null && selectedIndex != null) {
                DeliveryChallanDialogEditAndDisplay(
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
}

