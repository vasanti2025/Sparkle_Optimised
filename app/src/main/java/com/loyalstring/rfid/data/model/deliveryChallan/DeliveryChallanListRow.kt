package com.loyalstring.rfid.data.model.deliveryChallan

/** Lightweight row for list UI — avoids parsing/rendering heavy nested item details. */
data class DeliveryChallanListRow(
    val Id: Int,
    val CreatedOn: String?,
    val ChallanNo: String?,
    val CustomerName: String?,
    val Qty: String?,
    val GrossWt: String?,
    val StoneWt: String?,
    val TotalDiamondWeight: String?,
    val NetWt: String?,
    val TotalFineMetal: String?,
    val TotalGSTAmount: String?,
    val TotalNetAmount: String?,
    val BranchId: Int = 0,
    val ClientCode: String = "",
)

fun DeliveryChallanResponseList.toListRow() = DeliveryChallanListRow(
    Id = Id,
    CreatedOn = CreatedOn,
    ChallanNo = ChallanNo,
    CustomerName = CustomerName,
    Qty = Qty,
    GrossWt = GrossWt,
    StoneWt = StoneWt,
    TotalDiamondWeight = TotalDiamondWeight,
    NetWt = NetWt,
    TotalFineMetal = TotalFineMetal,
    TotalGSTAmount = TotalGSTAmount,
    TotalNetAmount = TotalNetAmount,
    BranchId = BranchId,
    ClientCode = ClientCode,
)
