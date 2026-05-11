package com.loyalstring.rfid.data.model.sampleOut

data class IssueItemDto( val ItemCode: String,
                         val SKU: String,
                         val SKUId: Int,
                         val CategoryId: Int,
                         val ProductId: Int,
                         val DesignId: Int,
                         val PurityId: Int,
                         val Quantity: Int,
                         val GrossWt: String,
                         val NetWt: String,
                         val TotalWt: String?,             // null in JSON
                         val FinePercentage: String?,      // null in JSON
                         val WastegePercentage: String?,   // null in JSON
                         val StoneWeight: String,
                         val DiamondWeight: String,
                         val FineWastageWt: String,
                         val RatePerGram: String,
                         val MetalAmount: String,
                         val Description: String?,
                         val SampleStatus: String,
                         val ClientCode: String,
                         val StoneAmount: String,
                         val SampleOutNo: String,
                         val DiamondAmount: String,
                         val Pieces: String,
                         val CategoryName: String,
                         val ProductName: String,
                         val PurityName: String,
                         val DesignName: String,
                         val Id: Int,
                         val CustomerId: Int,
                         val VendorId: Int,
                         val CreatedOn: String,
                         val CustomerName: String?,        // null in JSON
                         val SampleInDate: String?,        // null in JSON
                         val BranchId: Int?,               // null in JSON
                         val Customer: SampleCustomerDto?, // currently null, but structure ready
                         val LabelledStockId: Int,
                         val RFIDCode: String,
                         val TIDNumber: String
)
