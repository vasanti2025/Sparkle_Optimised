package com.loyalstring.rfid.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loyalstring.rfid.data.model.deliveryChallan.ChallanDetails
import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.loyalstring.rfid.viewmodel.OrderViewModel
import com.loyalstring.rfid.viewmodel.SingleProductViewModel

@Composable
fun DeliveryChallanItemListTable(
    productList: List<ChallanDetails>
) {
    val horizontalScroll = rememberScrollState()
    var selectedItem by remember { mutableStateOf<ChallanDetails?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    val orderViewModel: OrderViewModel = hiltViewModel()
    val singleProductViewModel: SingleProductViewModel = hiltViewModel()

    val branchList = singleProductViewModel.branches
    val salesmanList by orderViewModel.empListFlow.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(bottom = 5.dp) // ✅ Adds space below entire table (footer included)
    ) {
        // 🔹 Scrollable content (Header + Data)
        Box(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(horizontalScroll)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 🔹 Header Row (compact)
                items(productList.size) { index ->
                    val item = productList[index]
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF2E2E2E))
                            .clickable {
                                selectedItem = item
                                showDialog = true
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        listOf(
                            "P Name", "Itemcode", "G.Wt", "N.Wt", "F+W Wt",
                            "S.Amt", "D Amt", "Item Amt", "RFID Code"
                        ).forEach { title ->
                            Text(
                                text = title,
                                modifier = Modifier
                                    .width(70.dp)
                                    .padding(horizontal = 2.dp),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // 🔹 Data Rows (compact)
                items(productList.size) { index ->
                    val item = productList[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (index % 2 == 0) Color(0xFFF4F4F4) else Color.White)
                            .padding(vertical = 3.dp),
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
                                modifier = Modifier
                                    .width(70.dp)
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

        // 🔹 Fixed Footer Row (Totals)
        val totalQty = productList.size
        val totalGross = productList.sumOf { it.GrossWt?.toDoubleOrNull() ?: 0.0 }
        val totalNet = productList.sumOf { it.NetWt?.toDoubleOrNull() ?: 0.0 }
        val totalFine = productList.sumOf { it.FineWastageWt?.toDoubleOrNull() ?: 0.0 }
        val totalAmt = productList.sumOf { it.ItemAmount?.toDoubleOrNull() ?: 0.0 }

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
                        "Total",
                        totalQty.toString(),
                        "%.3f".format(totalGross),
                        "%.3f".format(totalNet),
                        "%.3f".format(totalFine),
                        "%.2f".format(totalAmt),
                        "%.2f".format(totalAmt),
                        "%.2f".format(totalAmt),
                        "%.2f".format(totalAmt)
                    ).forEach { total ->
                        Text(
                            text = total,
                            modifier = Modifier
                                .width(70.dp)
                                .padding(horizontal = 2.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
            }

            if (showDialog && selectedItem != null) {
                DeliveryChallanDialogEditAndDisplay(
                    selectedItem = selectedItem,
                    branchList = branchList,
                    salesmanList = salesmanList,
                    onDismiss = { showDialog = false },
                    onSave = { updatedItem ->
                        // optional save logic
                        showDialog = false
                    }
                )
            }

            // ✅ Add space below footer
            Spacer(modifier = Modifier.height(5.dp))

            DeliveryChallanSummaryRow(
                gstPercent = 3.0,
                totalAmount = 50000.0,
                onGstCheckedChange = { isChecked ->
                    println("GST Checkbox changed: $isChecked")
                }
            )
        }
    }
}
