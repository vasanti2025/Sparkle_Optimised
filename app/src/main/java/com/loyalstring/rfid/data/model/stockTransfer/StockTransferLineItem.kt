package com.loyalstring.rfid.data.model.stockTransfer

import com.google.gson.annotations.SerializedName

data class StockTransferLineItem(
    @SerializedName("Id") val Id: Int? = null,
    @SerializedName("TransferItemId") val TransferItemId: Int? = null,
    @SerializedName("LabelledStockId") val LabelledStockId: Int? = null,
    @SerializedName("StockId") val StockId: Int? = null,
    @SerializedName("SourceName") val SourceName: String? = null,
    @SerializedName("DestinationName") val DestinationName: String? = null,
    @SerializedName("CounterName") val CounterName: String? = null,
    @SerializedName("BoxName") val BoxName: String? = null,
    @SerializedName("PacketName") val PacketName: String? = null,
    @SerializedName("CounterId") val CounterId: Int? = null,
    @SerializedName("BoxId") val BoxId: Int? = null,
    @SerializedName("PacketId") val PacketId: Int? = null,
    @SerializedName("RequestStatus") val RequestStatus: Int? = null,
    @SerializedName("Status") val Status: Int? = null,
    @SerializedName("ItemCode") val ItemCode: String? = null,
    @SerializedName("ProductTitle") val ProductTitle: String? = null,
    @SerializedName("CategoryName") val CategoryName: String? = null,
    @SerializedName("BranchName") val BranchName: String? = null,
    @SerializedName("GrossWt") val GrossWt: String? = null,
    @SerializedName("NetWt") val NetWt: String? = null,
)
