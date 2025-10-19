package com.loyalstring.rfid.ui.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun MappingDialogWrapper(
    excelColumns: List<String>,
    bulkItemFields: List<String>,
    onDismiss: () -> Unit,
    fileSelected: Boolean,
    onImport: (Map<String, String>) -> Unit,
    isFromSheet: Boolean
) {

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(5.dp),
            modifier = Modifier
                .padding(2.dp)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            TableMappingScreen(
                excelColumns = excelColumns,
                bulkItemFields = bulkItemFields,
                onDismiss = onDismiss,
                fileselected = fileSelected,
                onImport = onImport,
                isFromSheet = isFromSheet
            )
        }
    }
}
