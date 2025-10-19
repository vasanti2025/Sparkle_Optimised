package com.loyalstring.rfid.data.remote.data

data class StockTransferRequest(
    val ClientCode: String,
    val StockTransferItems: List<StockTransferItem>,
    val StockType: String,
    val TransferTypeId: Int,
    val TransferByEmployee: String,
    val TransferedToBranch: String,
    val Source: Int,
    val Destination: Int,
    val Remarks: String,
    val ReceivedByEmployee: String
)

data class StockTransferItem(
    val stockId: Int
)
