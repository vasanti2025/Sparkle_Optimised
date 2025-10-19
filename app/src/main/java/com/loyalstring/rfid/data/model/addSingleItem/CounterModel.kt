package com.loyalstring.rfid.data.model.addSingleItem

import java.io.Serializable

data class CounterModel(
    val ClientCode: String,
    val CompanyId: Int,
    val BranchId: Int,
    val CounterNumber: String,
    val CounterName: String,
    val CounterDescription: String?,
    val CounterStatus: String?,
    val CounterLoginStatus: String?,
    val FinancialYear: String?,
    val Id: Int,
    val CreatedOn: String,
    val LastUpdated: String,
    val StatusType: Boolean
) : Serializable
