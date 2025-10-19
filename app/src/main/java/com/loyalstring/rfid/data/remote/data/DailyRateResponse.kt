package com.loyalstring.rfid.data.remote.data

data class DailyRateResponse(val EmployeeCode: String = "",
                             val ClientCode: String = "",
                             val PurityName: String = "",
                             val CategoryName: String = "",
                             val FinePercentage: String = "",
                             val PurityId: Int = 0,
                             val CategoryId: Int = 0,
                             val Rate: String = "0.00",
                             val UpdatedDate: String? = null,
                             val Id: Int = 0,
                             val CreatedOn: String = "",
                             val LastUpdated: String = "",
                             val StatusType: Boolean = true)
