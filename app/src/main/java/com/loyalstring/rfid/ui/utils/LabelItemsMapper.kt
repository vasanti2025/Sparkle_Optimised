package com.loyalstring.rfid.ui.utils

import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.remote.response.AlllabelResponse


fun AlllabelResponse.LabelItem.toBulkItem(): BulkItem {
    return BulkItem(
        productName = this.productName,
        itemCode = this.itemCode ?: "",
        rfid = this.rfidCode ?: "",

        grossWeight = this.grossWt ?: "",
        stoneWeight = this.totalStoneWeight ?: "",
        diamondWeight = this.totalDiamondWeight ?:"",
        netWeight = this.netWt ?: "",

        category = this.categoryName ?: "",
        design = this.designName ?: "",
        purity = this.purityName ?: "",

        makingPerGram = this.makingPerGram ?: "",
        makingPercent = this.makingPercentage ?: "",
        fixMaking = this.makingFixedAmt ?: "",
        fixWastage = this.makingFixedWastage ?: "",

        stoneAmount = this.totalStoneAmount ?: "",
        diamondAmount = this.totalDiamondAmount ?:"",

        sku = this.sku ?: "",
        vendor = this.vendorName ?: "",
        tid = this.tidNumber ?: "",
        id = this.id?:0,
        epc = this.tidNumber, // or wherever epc is coming from

        box = "",
        designCode = "",
        productCode = "",
        imageUrl = this.image ?: "",
        totalQty = this.quantity?.toIntOrNull() ?: 0,
        pcs = this.pieces?.toIntOrNull() ?: 0,
        matchedPcs = 0,
        totalGwt = 0.0,
        matchGwt = 0.0,
        totalStoneWt = this.totalStoneWeight?.toDoubleOrNull() ?: 0.0,
        matchStoneWt = 0.0,
        totalNetWt = this.netWt?.toDoubleOrNull() ?: 0.0,
        matchNetWt = 0.0,
        unmatchedQty = 0,
        matchedQty = 0,
        unmatchedGrossWt = 0.0,
        mrp = this.mrp?.toDoubleOrNull() ?: 0.0,
        counterName = this.counterName ?: "",
        counterId = this.counterId?.toInt() ?: 0,
        boxId = this.boxId?.toInt() ?: 0,
        boxName = this.boxName ?: "",
        scannedStatus = "",
        branchId = this.branchId ?: 0,
        branchName = this.branchName ?: "",
        categoryId = this.categoryId ?: 0,
        productId = this.productId ?: 0,
        designId = this.designId ?: 0,
        packetId = this.packetId ?: 0,
        packetName = this.packetName ?: "",
        branchType = this.branchType ?: ""
    ).apply {
        uhfTagInfo = null
    }
}
