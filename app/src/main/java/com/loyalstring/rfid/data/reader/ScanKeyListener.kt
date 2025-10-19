package com.loyalstring.rfid.data.reader

interface ScanKeyListener {
    fun onBarcodeKeyPressed()
    fun onRfidKeyPressed()
}