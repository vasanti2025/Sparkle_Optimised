package com.loyalstring.rfid.data.model.setting

import com.google.gson.annotations.SerializedName
import com.loyalstring.rfid.data.remote.data.DailyRateResponse

data class UpdateDailyRatesReq(
    @SerializedName("CategoryId") val categoryId: Int,
    @SerializedName("CategoryName") val categoryName: String,
    @SerializedName("ClientCode") val clientCode: String,
    @SerializedName("EmployeeCode") val employeeCode: String,
    @SerializedName("FinePercentage") val finePercentage: String,
    @SerializedName("PurityId") val purityId: Int,
    @SerializedName("PurityName") val purityName: String,
    @SerializedName("Rate") val rate: String
) {
    constructor(src: DailyRateResponse) : this(
        categoryId     = src.CategoryId ?: 0,
        categoryName   = src.CategoryName.orEmpty(),
        clientCode     = src.ClientCode.orEmpty(),
        employeeCode   = src.EmployeeCode.orEmpty(),
        finePercentage = src.FinePercentage.orEmpty(),
        purityId       = src.PurityId ?: 0,
        purityName     = src.PurityName.orEmpty(),
        rate           = src.Rate.orEmpty()
    )
}


