package com.loyalstring.rfid.data.model.sampleOut

class SampleOutUpdateRequest(
    val ClientCode: String,
    val CustomerId: Int,
    val SampleOutNo: String,
    val ReturnDate: String,
    val Description: String,
    val SampleStatus: String,
    val Quantity: Int,
    val TotalDiamondWeight: String,
    val TotalGrossWt: String,
    val TotalNetWt: String,
    val TotalStoneWeight: String,
    val TotalWt: String,
    val BranchId: Int?,
    val Id: Int? = null, // 🔹 update ke time aayega, add ke time null reh sakta
    val IssueItems: List<SampleOutIssueItem>,
    val Date: String,
    val StatusType: Boolean=true,
    val  SampleInDate : String="2026-05-11T09:47:26.893Z"

    )