package com.loyalstring.rfid.data.reader

/** Handheld RFID gun (polling) vs external UART RFID tray (callback inventory). */
enum class ScanInputMode {
    RFID_GUN,
    TRAY
}
