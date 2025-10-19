package com.loyalstring.rfid.data.model

data class SummaryItem(
    val counterName: String?,
    val category: String?,
    val product: String?,
    val totalQty: Int,
    val matchQty: Int,
    val unmatchQty: Int,
    val totalGrossWt: String?,
    val matchGrossWt: String?,
    val unmatchGrossWt: String?
)

data class DetailedItem(
    val counterName: String?,
    val category: String?,
    val product: String?,
    val purity: String?,
    val barcodeNumber: String?,
    val itemCode: String?,
    val pieces: Int,
    val grossWeight: String?,
    val stoneWeight: String?,
    val netWeight: String?,
    val mrp: String?
)
