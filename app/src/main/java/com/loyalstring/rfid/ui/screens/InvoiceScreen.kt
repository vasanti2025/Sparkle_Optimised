package com.loyalstring.rfid.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.loyalstring.rfid.data.model.order.CustomOrderResponse
import com.loyalstring.rfid.viewmodel.OrderViewModel

@Composable
fun InvoiceScreen(
    orderViewModel: OrderViewModel = hiltViewModel(),
    navController: NavHostController,
    onBack: () -> Boolean,
    item: CustomOrderResponse
) {
    if (item != null) {
        ProformaInvoiceScreen(order = item)
    } else {
        Text("No order found", modifier = Modifier.fillMaxSize(), textAlign = TextAlign.Center)
    }
}

@Composable
fun TableCell(text: String, modifier: Modifier = Modifier, fontSize: Int = 5) {
    Box(
        modifier = modifier
            .border(1.dp, Color.Black)
            .padding(horizontal = 2.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = fontSize.sp)
    }
}

@Composable
fun ProformaInvoiceScreen(order: CustomOrderResponse) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Text(
            "Proforma Invoice",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("DATE: ${order.OrderDate}", fontSize = 12.sp)
            Text("KT: 18KT", fontSize = 12.sp)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("CLIENT NAME: ${order.Customer?.FirstName + " " + order.Customer?.LastName ?: "N/A"}", fontSize = 12.sp)
            Text("SCREW: 88NS", fontSize = 12.sp)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("SEPARATE TAGS: YES", fontSize = 12.sp)
            Text("WASTAGE: 0.0", fontSize = 12.sp)
        }

        Divider(thickness = 1.dp, color = Color.Black, modifier = Modifier.padding(vertical = 8.dp))

        // Column width config
        val colWidths = listOf(
            30.dp, 70.dp, 80.dp, 70.dp, 50.dp, // SNO, TAG NO, ITEM NAME, DESIGN, STAMP
            50.dp, 50.dp, 50.dp, 50.dp, 60.dp  // GWT, SWT, NWT, FINE, STN VALUE
        )

        val headers = listOf("SNO", "TAG NO", "ITEM NAME", "DESIGN", "STAMP", "G WT", "S WT", "N WT", "FINE", "STN VALUE")

        // Header Row
        Row(Modifier.fillMaxWidth()) {
            headers.forEachIndexed { i, h ->
                TableCell(text = h, modifier = Modifier.width(colWidths[i]))
            }
        }

        // Data Rows
        order.CustomOrderItem.forEachIndexed { index, item ->
            val netWeight = (item.GrossWt?.toDoubleOrNull() ?: 0.0) - (item.StoneWt?.toDoubleOrNull() ?: 0.0)
            val cells = listOf(
                "${index + 1}",
                item.ItemCode ?: "",
                item.SKU ?: "",
                item.DesignName ?: "",
                item.Purity ?: "",
                item.GrossWt ?: "0.000",
                item.StoneWt ?: "0.000",
                "%.3f".format(netWeight),
                item.FinePercentage ?: "0.000",
                item.StoneAmount ?: "0.000"
            )
            Row(Modifier.fillMaxWidth()) {
                cells.forEachIndexed { i, text ->
                    TableCell(text = text, modifier = Modifier.width(colWidths[i]))
                }
            }
        }

        // Totals
        val totalGross = order.CustomOrderItem.sumOf { it.GrossWt?.toDoubleOrNull() ?: 0.0 }
        val totalStone = order.CustomOrderItem.sumOf { it.StoneWt?.toDoubleOrNull() ?: 0.0 }
        val totalNet = totalGross - totalStone
        val totalFine = order.CustomOrderItem.sumOf { it.FinePercentage?.toDoubleOrNull() ?: 0.0 }
        val totalStnValue = order.CustomOrderItem.sumOf { it.StoneAmount?.toDoubleOrNull() ?: 0.0 }

        val totalCells = listOf(
            "TOTAL", "", "", "", "",
            "%.3f".format(totalGross),
            "%.3f".format(totalStone),
            "%.3f".format(totalNet),
            "%.3f".format(totalFine),
            "%.3f".format(totalStnValue)
        )

        Row(Modifier.fillMaxWidth()) {
            totalCells.forEachIndexed { i, text ->
                TableCell(text = text, modifier = Modifier.width(colWidths[i]), fontSize = 5)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Footer
        Text("PUSHPA JEWELLERS LIMITED", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text("ADDRESS - 6th Floor, Flat 4A, 22 East Topsia Road, Kolkata - 700046", fontSize = 12.sp)
        Text("GST - 19AAECP0980J1Z9", fontSize = 12.sp)
        Text("BANK NAME - ICICI BANK LTD", fontSize = 12.sp)
        Text("A/C - 035605005192 | IFSC - ICIC0000650", fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Text("Note - This is not a Tax Invoice", color = Color.Red, fontSize = 8.sp)
    }
}
