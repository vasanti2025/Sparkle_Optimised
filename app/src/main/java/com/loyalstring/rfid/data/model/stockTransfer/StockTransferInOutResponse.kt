package com.loyalstring.rfid.data.model.stockTransfer

import com.loyalstring.rfid.data.model.order.ItemCodeResponse

data class StockTransferInOutResponse(val Id: Int,
                                      val TransferTypeId: Int,
                                      val StockType: String,
                                      val Source: Int,
                                      val Destination: Int,
                                      val SourceName: String,
                                      val DestinationName: String,
                                      val TransferByEmployee: String,
                                      val TransferedToBranch: String,
                                      val ReceivedByEmployee: String,
                                      val Remarks: String,
                                      val ClientCode: String,
                                      val StockTransferTypeName: String,
                                      val Pending: Int,
                                      val Approved: Int,
                                      val Rejected: Int,
                                      val Lost: Int,
                                      val Direction: Int,
                                      val StockTransferItems: Any?, // can define later if needed
                                      val LabelledStockItems: List<LabelledStockItems>? = null,
                                      val UnlabelledStockItems: Any?, // can define later if needed
                                      val RequestType: String)
