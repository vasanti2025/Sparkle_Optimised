package com.example.sparklepos.models.loginclasses.customerBill

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customer")
data class EmployeeList(
    @PrimaryKey(autoGenerate = true) val custId: Int = 0,

    val Id: Int? = null,
    val FirstName: String? = null,
    val LastName: String? = null,
    val PerAddStreet: String? = null,
    val CurrAddStreet: String? = null,
    val Mobile: String? = null,
    val Email: String? = null,
    val Password: String? = null,
    val CustomerLoginId: String? = null,
    val DateOfBirth: String? = null,
    val MiddleName: String? = null,
    val PerAddPincode: String? = null,
    val Gender: String? = null,
    val OnlineStatus: String? = null,
    val CurrAddTown: String? = null,
    val CurrAddPincode: String? = null,
    val CurrAddState: String? = null,
    val PerAddTown: String? = null,
    val PerAddState: String? = null,
    val GstNo: String? = null,
    val PanNo: String? = null,
    val AadharNo: String? = null,
    val BalanceAmount: String? = null,
    val AdvanceAmount: String? = null,
    val Discount: String? = null,
    val CreditPeriod: String? = null,
    val FineGold: String? = null,
    val FineSilver: String? = null,
    val ClientCode: String? = null,
    val VendorId: Int? = null,
    val AddToVendor: Boolean? = null,
    val CustomerSlabId: Int? = null,
    val CreditPeriodId: Int? = null,
    val RateOfInterestId: Int? = null,
    val CustomerSlab: String? = null,
    val RateOfInterest: String? = null,
    val CreatedOn: String? = null,
    val LastUpdated: String? = null,
    val StatusType: Boolean? = null,
    val Remark: String? = null,
    val Area: String? = null,
    val City: String? = null,
    val Country: String? = null
)
