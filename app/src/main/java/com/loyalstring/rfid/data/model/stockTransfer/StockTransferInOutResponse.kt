package com.loyalstring.rfid.data.model.stockTransfer

import com.google.gson.annotations.SerializedName

data class StockTransferInOutResponse(
    @SerializedName("Id") val Id: Int,
    @SerializedName("TransferTypeId") val TransferTypeId: Int,
    @SerializedName("StockType") val StockType: String,
    @SerializedName("Source") val Source: Int,
    @SerializedName("Destination") val Destination: Int,
    @SerializedName("SourceName") val SourceName: String? = null,
    @SerializedName("DestinationName") val DestinationName: String? = null,
    @SerializedName("TransferByEmployee") val TransferByEmployee: String,
    @SerializedName("TransferToEmployee") val TransferToEmployee: String? = null,
    @SerializedName("TransferedToBranch") val TransferedToBranch: String,
    @SerializedName("ReceivedByEmployee") val ReceivedByEmployee: String,
    @SerializedName("Remarks") val Remarks: String,
    @SerializedName("ClientCode") val ClientCode: String,
    @SerializedName("StockTransferTypeName") val StockTransferTypeName: String,
    @SerializedName("Pending") val Pending: Int,
    @SerializedName("Approved") val Approved: Int,
    @SerializedName("Rejected") val Rejected: Int,
    @SerializedName("Lost") val Lost: Int,
    @SerializedName("Direction") val Direction: Int,
    @SerializedName("StockTransferItems") val StockTransferItems: List<StockTransferLineItem>? = null,
    @SerializedName("LabelledStockItems") val LabelledStockItems: List<LabelledStockItems>? = null,
    @SerializedName("UnlabelledStockItems") val UnlabelledStockItems: Any? = null,
    @SerializedName("RequestType") val RequestType: String
)
