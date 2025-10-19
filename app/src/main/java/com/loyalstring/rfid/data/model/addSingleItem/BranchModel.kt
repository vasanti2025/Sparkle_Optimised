package com.loyalstring.rfid.data.model.addSingleItem

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "branch")
data class BranchModel(
    @PrimaryKey
    val Id: Int = 0,
    val BranchCode: String = "",
    val ClientCode: String = "",
    val CompanyId: Int = 0,
    val BranchName: String = "",
    val BranchType: String = "",
    val BranchHead: String = "",
    val BranchAddress: String = "",
    val PhoneNumber: String = "",
    val MobileNumber: String = "",
    val FaxNumber: String = "",
    val Country: String = "",
    val Town: String = "",
    val State: String = "",
    val City: String = "",
    val Street: String? = null,
    val Area: String? = null,
    val PostalCode: String = "",
    val GSTIN: String = "",
    val BranchEmailId: String = "",
    val BranchStatus: String = "",
    val FinancialYear: String = "",
    val BranchLoginStatus: String? = null,
    val CreatedOn: String = "",
    val LastUpdated: String = "",
    val StatusType: Boolean = true
) : Serializable
