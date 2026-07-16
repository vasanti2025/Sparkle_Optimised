package com.loyalstring.rfid.ui.utils

import com.loyalstring.rfid.viewmodel.BulkViewModel

/** Toggle RFID scan using ViewModel state as source of truth (avoids stale UI flags). */
fun toggleBulkScan(
    viewModel: BulkViewModel,
    selectedPower: Int,
    onScanningChanged: (Boolean) -> Unit = {}
) {
    if (viewModel.isScanning.value) {
        viewModel.stopScanning()
    } else {
        viewModel.startScanning(selectedPower)
    }
    onScanningChanged(viewModel.isScanning.value)
}

fun stopBulkScan(
    viewModel: BulkViewModel,
    onScanningChanged: (Boolean) -> Unit = {}
) {
    viewModel.stopScanning()
    onScanningChanged(false)
}
