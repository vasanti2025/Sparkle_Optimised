package com.loyalstring.rfid.data.model.order
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Customer(
    val FirstName: String,
    val LastName: String,
    val PerAddStreet: String,
    val CurrAddStreet: String,
    val Mobile: String,
    val Email: String,
    val Password: String,
    val CustomerLoginId: String,
    val DateOfBirth: String,
    val MiddleName: String,
    val PerAddPincode: String,
    val Gender: String,
    val OnlineStatus: String,
    val CurrAddTown: String,
    val CurrAddPincode: String,
    val CurrAddState: String,
    val PerAddTown: String,
    val PerAddState: String,
    val GstNo: String,
    val PanNo: String,
    val AadharNo: String,
    val BalanceAmount: String,
    val AdvanceAmount: String,
    val Discount: String,
    val CreditPeriod: String,
    val FineGold: String,
    val FineSilver: String,
    val ClientCode: String,
    val VendorId: Int,
    val AddToVendor: Boolean,
    val CustomerSlabId: Int,
    val CreditPeriodId: Int,
    val RateOfInterestId: Int,
    val Remark: String,
    val Area: String,
    val City: String,
    val Country: String,
    val Id: Int,
    val CreatedOn: String,
    val LastUpdated: String,
    val StatusType: Boolean
) : Parcelable

