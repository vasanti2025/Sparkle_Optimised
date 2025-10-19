package com.loyalstring.rfid.data.model.order

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.loyalstring.rfid.data.local.converters.CustomOrderConverters
import kotlinx.android.parcel.Parcelize

@Entity(tableName = "customerorderequest")
@TypeConverters(CustomOrderConverters::class)
data class CustomOrderRequest(
    @PrimaryKey(autoGenerate = true)
    val CustomOrderId: Int,
    val CustomerId: String,
    val ClientCode: String,
    val OrderId: Int,
    val TotalAmount: String,
    val PaymentMode: String,
    val Offer: String? = null,
    val Qty: String,
    val GST: String,
    val OrderStatus: String,
    val MRP: String? = null,
    val VendorId: Int? = null,
    val TDS: String? = null,
    val PurchaseStatus: String? = null,
    val GSTApplied: String,
    val Discount: String,
    val TotalNetAmount: String,
    val TotalGSTAmount: String,
    val TotalPurchaseAmount: String,
    val ReceivedAmount: String,
    val TotalBalanceMetal: String,
    val BalanceAmount: String,
    val TotalFineMetal: String,
    val CourierCharge: String? = null,
    val SaleType: String? = null,
    val OrderDate: String,
    val OrderCount: String,
    val AdditionTaxApplied: String,
    val CategoryId: Int,
    val OrderNo: String,
    val DeliveryAddress: String? = null,
    val BillType: String,
    val UrdPurchaseAmt: String? = null,
    val BilledBy: String,
    val SoldBy: String,
    val CreditSilver: String? = null,
    val CreditGold: String? = null,
    val CreditAmount: String? = null,
    val BalanceAmt: String,
    val BalanceSilver: String? = null,
    val BalanceGold: String? = null,
    val TotalSaleGold: String? = null,
    val TotalSaleSilver: String? = null,
    val TotalSaleUrdGold: String? = null,
    val TotalSaleUrdSilver: String? = null,
    val FinancialYear: String,
    val BaseCurrency: String,
    val TotalStoneWeight: String,
    val TotalStoneAmount: String,
    val TotalStonePieces: String,
    val TotalDiamondWeight: String,
    val TotalDiamondPieces: String,
    val TotalDiamondAmount: String,
    val FineSilver: String,
    val FineGold: String,
    val DebitSilver: String? = null,
    val DebitGold: String? = null,
    val PaidMetal: String,
    val PaidAmount: String,
    val TotalAdvanceAmt: String? = null,
    val TaxableAmount: String,
    val TDSAmount: String? = null,
    val CreatedOn: String? = null,
    //val LastUpdated: String? = null,
    val StatusType: Boolean? = null,
    val FineMetal: String,
    val BalanceMetal: String,
    val AdvanceAmt: String,
    val PaidAmt: String,
    val TaxableAmt: String,
    val GstAmount: String,
    val GstCheck: String,
    val Category: String,

    val TDSCheck: String,
    val Remark: String? = null,
    val OrderItemId: Int? = null,
    val StoneStatus: String? = null,
    val DiamondStatus: String? = null,
    val BulkOrderId: String? = null,
    val CustomOrderItem: List<CustomOrderItem>,
    val Payments: List<Payment>,
    val uRDPurchases: List<URDPurchase>,
    val Customer: Customer,
    @ColumnInfo(name = "syncStatus")
    val syncStatus: Boolean = false,

    @ColumnInfo(name = "LastUpdated")
    val LastUpdated: String? = null

)
