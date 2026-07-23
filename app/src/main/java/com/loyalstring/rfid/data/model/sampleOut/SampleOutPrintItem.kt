package com.loyalstring.rfid.data.model.sampleOut

data class SampleOutPrintItem(
    val itemDetails: String,
    val grossWt: String? = "0",
    val stoneWt: String? = "0",
    val diamondWt: String? = "0",
    val netWt: String? = "0",
    val pieces: String? = "0",
    val status: String = "Sample Out",
    val imageUrl: String? = null,
    val itemCode: String? = null,
    val designName: String? = null,
)

data class SampleOutPrintData(
    val companyName: String,
    val customerName: String,
    val addressCity: String,
    val contactNo: String,
    val sampleOutNo: String,
    val date: String,
    val returnDate: String,
    val items: List<SampleOutPrintItem>
)
