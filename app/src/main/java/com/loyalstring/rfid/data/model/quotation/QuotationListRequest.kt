package com.loyalstring.rfid.data.model.quotation

import com.google.gson.annotations.SerializedName

data class QuotationListRequest(
    @SerializedName("ClientCode")
    val ClientCode: String,
    @SerializedName("BranchId")
    val BranchId: Int
)
