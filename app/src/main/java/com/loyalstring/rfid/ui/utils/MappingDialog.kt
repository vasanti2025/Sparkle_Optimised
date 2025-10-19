package com.loyalstring.rfid.ui.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@Composable
fun MappingDialog(
    excelColumns: List<String>,
    onDismiss: () -> Unit,
    onImport: (Map<String, String>) -> Unit
) {
    val mappings = remember { mutableStateMapOf<String, String>() }

    val bulkItemFieldNames = listOf(
        "itemCode",
        "rfid",
        "grossWeight",
        "stoneWeight",
        "diamondWeight",
        "netWeight",
        "category",
        "design",
        "purity",
        "makingPerGram",
        "makingPercent",
        "fixMaking",
        "fixWastage",
        "stoneAmount",
        "diamondAmount",
        "sku",
        "epc",
        "vendor",
        "tid",
        "uhftagInfo"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Map Excel Columns", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Match Excel headers to BulkItem fields")
            }
        },
        text = {
            Column {
                excelColumns.forEach { column ->
                    var expanded by remember { mutableStateOf(false) }
                    var selected by remember { mutableStateOf("") }

                    Text(text = column, fontWeight = FontWeight.Bold)
                    Box {
                        OutlinedTextField(
                            value = selected,
                            onValueChange = {},
                            label = { Text("Map to") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = true },
                            readOnly = true
                        )
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            bulkItemFieldNames.forEach { field ->
                                DropdownMenuItem(
                                    text = { Text(field) },
                                    onClick = {
                                        selected = field
                                        mappings[column] = field
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = { onImport(mappings) }) {
                Text("Import")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("Cancel")
            }
        }
    )
}

