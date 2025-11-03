package com.loyalstring.rfid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loyalstring.rfid.data.local.entity.DeliveryChallanItem
import com.loyalstring.rfid.data.local.entity.OrderItem
import com.loyalstring.rfid.data.model.deliveryChallan.ChallanDetails

@Composable
fun DeliveryChallanItemListTable(
    productList: List<ChallanDetails>
) {
    // ✅ One shared scroll state for everything
    val sharedScrollState = rememberScrollState()

    // ✅ Wrap everything inside one horizontal scroll container
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(sharedScrollState)
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 🔹 Header Row
            Row(
                modifier = Modifier
                    .background(Color(0xFF2E2E2E))
                    .padding(vertical = 6.dp)
            ) {
                listOf(
                    "P Name", "Itemcode", "G.Wt", "N.Wt", "F+W Wt", "S.Amt", "D Amt","Item Amt","RFID Code"
                ).forEach { title ->
                    Text(
                        text = title,
                        modifier = Modifier
                            .width(70.dp)
                            .padding(horizontal = 4.dp),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 🔹 Data Rows
            productList.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (index % 2 == 0) Color(0xFFF7F7F7) else Color.White)
                        .padding(vertical = 6.dp),
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
                                .padding(horizontal = 4.dp),
                            fontSize = 12.sp,
                            color = Color.DarkGray,
                            maxLines = 1
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(225.dp))

            // 🔹 Divider
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFBDBDBD))
            )

            // 🔹 Total Row
            val totalQty = productList.size
            val totalGross = productList.sumOf { it.GrossWt?.toDoubleOrNull() ?: 0.0 }
            val totalNet = productList.sumOf { it.NetWt?.toDoubleOrNull() ?: 0.0 }
            val totalFine = productList.sumOf { it.FineWastageWt?.toDoubleOrNull() ?: 0.0 }
            val totalAmt = productList.sumOf { it.ItemAmount?.toDoubleOrNull() ?: 0.0 }

            Row(
                modifier = Modifier
                    .background(Color(0xFF2E2E2E))
                    .padding(vertical = 8.dp)
            ) {
                listOf(
                    "Total", totalQty.toString(),
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
                            .padding(horizontal = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
