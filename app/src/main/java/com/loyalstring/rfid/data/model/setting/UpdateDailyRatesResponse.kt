package com.loyalstring.rfid.data.model.setting

import com.google.gson.annotations.SerializedName

data class UpdateDailyRatesResponse(
    @SerializedName("EmployeeCode")  val employeeCode: String,
    @SerializedName("ClientCode")    val clientCode: String,
    @SerializedName("PurityName")    val purityName: String,
    @SerializedName("CategoryName")  val categoryName: String,
    @SerializedName("FinePercentage")val finePercentage: String,
    @SerializedName("PurityId")      val purityId: Int,
    @SerializedName("CategoryId")    val categoryId: Int,
    @SerializedName("Rate")          val rate: String,
    @SerializedName("UpdatedDate")   val updatedDate: String?,
    @SerializedName("Id")            val id: Int,
    @SerializedName("CreatedOn")     val createdOn: String,
    @SerializedName("LastUpdated")   val lastUpdated: String,
    @SerializedName("StatusType")    val statusType: Boolean
)
