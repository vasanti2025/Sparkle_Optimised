package com.loyalstring.rfid.data.remote.data

data class DailyRateResponse(val EmployeeCode: String,
                             val ClientCode: String,
                             val PurityName: String,
                             val CategoryName: String,
                             val FinePercentage: String,
                             val PurityId: Int,
                             val CategoryId: Int,
                             val Rate: String,
                             val UpdatedDate: String?,
                             val Id: Int,
                             val CreatedOn: String,
                             val LastUpdated: String,
                             val StatusType: Boolean)
