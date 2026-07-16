package com.loyalstring.rfid.data.model.quotation

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.loyalstring.rfid.data.model.FlexibleNumericStringTypeAdapter

data class QuotationItem(
    val QuotationItemId: Int? = null,
    val MRP: String? = null,
    val Image: String? = null,
    val StoneLessPercent: String? = null,
    val CategoryName: String? = null,
    val QuotationStatus: String? = null,
    val ProductName: String? = null,
    val Quantity: String? = null,
    val HSNCode: String? = null,
    val ItemCode: String? = null,
    val GrossWt: String? = null,
    val NetWt: String? = null,
    val ProductId: Int? = null,
    val CustomerId: Int? = null,
    val MetalRate: String? = null,
    val MakingCharg: String? = null,
    val Price: String? = null,
    val HUIDCode: String? = null,
    val ProductCode: String? = null,
    val ProductNo: String? = null,
    val Size: String? = null,
    val StoneAmount: String? = null,
    val TotalWt: String? = null,
    val PackingWeight: String? = null,
    val MetalAmount: String? = null,
    val OldGoldPurchase: Boolean? = null,
    val RatePerGram: String? = null,
    val Amount: String? = null,
    val BillType: String? = null,

    val PurProductNetWt: String? = null,
    val PurProductGrossWt: String? = null,
    val PurProductAmt: String? = null,
    val PurProductId: Int? = null,

    val UnlProductNetWt: String? = null,
    val UnlProductGrossWt: String? = null,
    val UnlProductAmt: String? = null,
    val UnlProductId: Int? = null,

    val FinePercentage: String? = null,
    val PurchaseInvoiceNo: String? = null,
    val HallmarkAmount: String? = null,
    val HallmarkNo: String? = null,
    @SerializedName("MakingFixedAmt")
    val MakingFixedAmt: String? = null,
    @JsonAdapter(FlexibleNumericStringTypeAdapter::class)
    @SerializedName("MakingFixedWastage")
    val MakingFixedWastage: String? = null,
    val MakingPerGram: String? = null,
    val MakingPercentage: String? = null,
    val Description: String? = null,
    val CuttingGrossWt: String? = null,
    val CuttingNetWt: String? = null,
    val BaseCurrency: String? = null,
    val CategoryId: Int? = null,
    val PurityId: Int? = null,

    val TotalStoneWeight: String? = null,
    val TotalStoneAmount: String? = null,
    val TotalStonePieces: String? = null,

    val TotalDiamondWeight: String? = null,
    val TotalDiamondPieces: String? = null,
    val TotalDiamondAmount: String? = null,

    val SKUId: Int? = null,
    @SerializedName("SKU")
    val SKU: String? = null,
    @JsonAdapter(FlexibleNumericStringTypeAdapter::class)
    @SerializedName(value = "FineWastageWt", alternate = ["fineWastageWt"])
    val FineWastageWt: String? = null,
    val TotalItemAmount: String? = null,
    val Pieces: String? = null,
    val DesignId: Int? = null,
    val DesignName: String? = null,
    val ClientCode: String? = null,

    val DiamondSize: String? = null,
    val DiamondWeight: String? = null,
    val DiamondPurchaseRate: String? = null,
    val DiamondSellRate: String? = null,
    val DiamondClarity: String? = null,
    val DiamondColour: String? = null,
    val DiamondShape: String? = null,
    val DiamondCut: String? = null,
    val DiamondSettingType: String? = null,
    val DiamondCertificate: String? = null,
    val DiamondPieces: String? = null,
    val DiamondPurchaseAmount: String? = null,
    val DiamondSellAmount: String? = null,
    val DiamondDescription: String? = null,

    val CreatedOn: String? = null,
    val LastUpdated: String? = null,

    val NetAmount: String? = null,
    val GSTAmount: String? = null,
    val TotalAmount: String? = null,

    val Purity: String? = null,
    val CompanyId: Int? = null,
    val BranchId: Int? = null,
    val CounterId: Int? = null,
    val EmployeeId: Int? = null,
    val LabelledStockId: Int? = null,

    val FineSilver: String? = null,
    val FineGold: String? = null,
    val DebitSilver: String? = null,
    val DebitGold: String? = null,
    val BalanceSilver: String? = null,
    val BalanceGold: String? = null,

    val ConvertAmt: String? = null,
    val PacketId: Int? = null,
    val finalPrice: String? = null,
    val totalGstAmount: String? = null,
    val StoneRateKarate: String? = null,
    val Quotationid: Int? = null,
    val MetalName: String? = null,
    val Stones: List<Any>? = null,
    val RFIDCode:String?=null,
    val DiamondWt: String? = null,
    val StoneAmt: String? = null,
    val DiamondAmt: String? = null,
    val FinePer: String? = null,
    val FineWt: String? = null,
    val qty: Int? = null,
    val tid: String? = null,
    val totayRate: String? = null,
    @SerializedName("makingPercent")
    val makingPercent: String? = null,
    @SerializedName("fixMaking")
    val fixMaking: String? = null,
    @JsonAdapter(FlexibleNumericStringTypeAdapter::class)
    @SerializedName("fixWastage")
    val fixWastage: String? = null,
    val itemAmt: String? = null,
    val ItemGSTAmount:String?=null

)
