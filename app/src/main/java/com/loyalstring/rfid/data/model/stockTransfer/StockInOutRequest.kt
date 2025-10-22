package com.loyalstring.rfid.data.model.stockTransfer

data class StockInOutRequest(val ClientCode: String,
                             val StockType: String,
                             val TransferType: Int,
                             val BranchId: Int,
                             val StartDate: String,
                             val EndDate: String,
                             val UserID: Int,
                             val RequestType: String
)
