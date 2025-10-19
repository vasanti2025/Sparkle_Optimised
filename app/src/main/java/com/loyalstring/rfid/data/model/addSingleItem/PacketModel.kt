package com.loyalstring.rfid.data.model.addSingleItem

data class PacketModel(
    val Id: Int,
    val SKUId: Int,
    val StockKeepingUnit: String?,
    val CategoryId: Int,
    val CategoryName: String,
    val ProductId: Int,
    val ProductName: String,
    val PacketName: String,
    val BranchId: Int,
    val CompanyId: Int,
    val ClientCode: String,
    val EmployeeId: Int,
    val EmptyWeight: String,
    val Status: String,
    val Description: String,
    val CreatedOn: String,      // You can change to LocalDateTime if needed
    val LastUpdated: String,    // Same here
    val BoxName: String?
)
