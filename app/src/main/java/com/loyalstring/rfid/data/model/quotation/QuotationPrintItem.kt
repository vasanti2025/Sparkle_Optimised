package com.loyalstring.rfid.data.model.quotation

data class QuotationPrintItem(
    val itemCode: String? = null,
    val rfidNo: String? = null,
    val grossWt: String? = null,
    val netWt: String? = null,
    val pcs: String? = null,
    val stoneWt: String? = null,
    val stoneAmt: String? = null,
    val amount: String? = null
)

data class QuotationPrintData(
    val ownerName: String,
    val ownerAddress: String,
    val ownerContact: String,

    val quotationNo: String,
    val date: String,
    val salesMan: String? = "",
    val remark: String? = "",

    val customerName: String,
    val customerMobile: String,
    val customerAddress: String,

    val items: List<QuotationPrintItem>,

    val totalAmount: String,
    val cgst: String = "0.00",
    val sgst: String = "0.00",
    val igst: String = "0.00"
)
