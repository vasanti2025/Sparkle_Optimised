package com.loyalstring.rfid.data.model.report

data class ScanBatchRequest( val ClientCode: String,
                             val ScanBatchId: String,
                             val ReturnAllData: Boolean
)
