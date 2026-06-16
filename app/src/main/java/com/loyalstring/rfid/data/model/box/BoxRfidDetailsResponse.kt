package com.loyalstring.rfid.data.model.box

import com.google.gson.annotations.SerializedName

data class BoxRfidDetailsResponse(
    @SerializedName("Success") val success: Boolean? = null,
    @SerializedName("Message") val message: String? = null,
    @SerializedName("SearchedRfidCode") val searchedRfidCode: String? = null,
    @SerializedName("RfidBarcode") val rfidBarcode: String? = null,
    @SerializedName("TidValue") val tidValue: String? = null,
    @SerializedName("FoundInRfidTable") val foundInRfidTable: Boolean? = null,
    @SerializedName("MatchType") val matchType: String? = null,
    @SerializedName("Boxes") val boxes: List<BoxRfidBoxEntry>? = null,
    @SerializedName("Products") val products: List<BoxRfidProduct>? = null
)

data class BoxRfidBoxEntry(
    @SerializedName("Box") val box: BoxRfidBoxInfo? = null,
    @SerializedName("Summary") val summary: BoxRfidBoxSummary? = null,
    @SerializedName("Products") val products: List<BoxRfidProduct>? = null
)

data class BoxRfidBoxInfo(
    @SerializedName("BoxId") val boxId: Int? = null,
    @SerializedName("BoxName") val boxName: String? = null,
    @SerializedName("EmptyWeight") val emptyWeight: String? = null,
    @SerializedName("RfidCode") val rfidCode: String? = null,
    @SerializedName("HexCode") val hexCode: String? = null,
    @SerializedName("TidNumber") val tidNumber: String? = null,
    @SerializedName("IsRfidTagged") val isRfidTagged: Boolean? = null,
    @SerializedName("TaggedOn") val taggedOn: String? = null,
    @SerializedName("LastPackedOn") val lastPackedOn: String? = null,
    @SerializedName("LastScannedOn") val lastScannedOn: String? = null
)

data class BoxRfidBoxSummary(
    @SerializedName("TotalProducts") val totalProducts: Int? = null,
    @SerializedName("TotalGrossWt") val totalGrossWt: Double? = null,
    @SerializedName("TotalNetWt") val totalNetWt: Double? = null,
    @SerializedName("TotalPieces") val totalPieces: Int? = null,
    @SerializedName("BoxEmptyWeight") val boxEmptyWeight: Double? = null,
    @SerializedName("GrandTotalWeight") val grandTotalWeight: Double? = null
)

data class BoxRfidProduct(
    @SerializedName("IsInBox") val isInBox: Boolean? = null,
    @SerializedName("BoxId") val boxId: Int? = null,
    @SerializedName("BoxName") val boxName: String? = null,
    @SerializedName("BoxRfidCode") val boxRfidCode: String? = null,
    @SerializedName("BoxHexCode") val boxHexCode: String? = null,
    @SerializedName("BoxTidNumber") val boxTidNumber: String? = null,
    @SerializedName("Box") val box: BoxRfidBoxInfo? = null,
    @SerializedName("LabelledStockId") val labelledStockId: Int? = null,
    @SerializedName("ItemCode") val itemCode: String? = null,
    @SerializedName("ProductTitle") val productTitle: String? = null,
    @SerializedName("HexCode") val hexCode: String? = null,
    @SerializedName("TidNumber") val tidNumber: String? = null,
    @SerializedName("RfidCode") val rfidCode: String? = null,
    @SerializedName("GrossWt") val grossWt: String? = null,
    @SerializedName("NetWt") val netWt: String? = null,
    @SerializedName("Pieces") val pieces: String? = null,
    @SerializedName("Mrp") val mrp: String? = null,
    @SerializedName("CategoryName") val categoryName: String? = null,
    @SerializedName("ProductName") val productName: String? = null,
    @SerializedName("DesignName") val designName: String? = null,
    @SerializedName("PurityName") val purityName: String? = null,
    @SerializedName("VendorName") val vendorName: String? = null,
    @SerializedName("Sku") val sku: String? = null,
    @SerializedName("Status") val status: String? = null,
    @SerializedName("AddedToBoxOn") val addedToBoxOn: String? = null,
    @SerializedName("AddedBy") val addedBy: String? = null,
    @SerializedName("ScanMatched") val scanMatched: Boolean? = null
)
