package com.loyalstring.rfid.ui.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.loyalstring.rfid.data.model.addSingleItem.SKUModel

@Composable
fun CustomDropdownMenu(
    fieldLabel: String,
    options: List<String>,
    selectedOption: String,
    onValueChange: (String) -> Unit,
    skuList: List<SKUModel>?, // Your SKU list model
    onSkuSelected: ((SKUModel) -> Unit)? // Optional callback
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .background(Color.LightGray)
            .padding(12.dp)
    ) {
        Text(text = selectedOption.ifBlank { "Select Option" })

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false

                        if (fieldLabel == "SKU") {
                            val selectedSku = skuList?.find { it.StockKeepingUnit == option }
                            selectedSku?.let {
                                onSkuSelected?.invoke(it)
                            }
                        }
                    }
                )
            }
        }
    }
}
