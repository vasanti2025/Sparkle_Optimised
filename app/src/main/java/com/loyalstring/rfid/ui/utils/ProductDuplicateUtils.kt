package com.loyalstring.rfid.ui.utils

import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.local.entity.OrderItem
import com.loyalstring.rfid.data.model.deliveryChallan.ChallanDetails
import com.loyalstring.rfid.data.model.quotation.QuotationItem
import com.loyalstring.rfid.data.model.sampleOut.SampleOutDetails

fun normalizeProductCode(value: String?): String =
    value
        ?.trim()
        ?.uppercase()
        ?.replace(" ", "")
        ?.replace("\n", "")
        ?.replace("\r", "")
        ?: ""

fun isSameProductCode(a: String?, b: String?): Boolean {
    val left = normalizeProductCode(a)
    val right = normalizeProductCode(b)
    return left.isNotBlank() && right.isNotBlank() && left == right
}

fun isDuplicateProductIdentity(
    existingItemCode: String?,
    existingRfid: String?,
    existingTid: String?,
    existingEpc: String? = null,
    existingProductCode: String? = null,
    newItemCode: String?,
    newRfid: String?,
    newTid: String?,
    newEpc: String? = null,
    newProductCode: String? = null,
): Boolean {
    return isSameProductCode(existingItemCode, newItemCode) ||
            isSameProductCode(existingRfid, newRfid) ||
            isSameProductCode(existingTid, newTid) ||
            isSameProductCode(existingEpc, newEpc) ||
            isSameProductCode(existingProductCode, newProductCode)
}

fun isBulkItemAlreadyInOrderList(
    productList: List<OrderItem>,
    matchedItem: BulkItem,
): Boolean = productList.any { existing ->
    isDuplicateProductIdentity(
        existingItemCode = existing.itemCode,
        existingRfid = existing.rfidCode,
        existingTid = existing.tid,
        existingEpc = existing.epc,
        existingProductCode = existing.productCode,
        newItemCode = matchedItem.itemCode,
        newRfid = matchedItem.rfid,
        newTid = matchedItem.tid,
        newEpc = matchedItem.epc,
        newProductCode = matchedItem.productCode,
    )
}

fun isBulkItemAlreadyInQuotationList(
    productList: List<QuotationItem>,
    matchedItem: BulkItem,
): Boolean = productList.any { existing ->
    isDuplicateProductIdentity(
        existingItemCode = existing.ItemCode,
        existingRfid = existing.RFIDCode,
        existingTid = existing.tid,
        existingProductCode = existing.ProductCode,
        newItemCode = matchedItem.itemCode,
        newRfid = matchedItem.rfid,
        newTid = matchedItem.tid,
        newProductCode = matchedItem.productCode,
    )
}

fun isBulkItemAlreadyInSampleOutList(
    productList: List<SampleOutDetails>,
    matchedItem: BulkItem,
): Boolean = productList.any { existing ->
    isDuplicateProductIdentity(
        existingItemCode = existing.ItemCode,
        existingRfid = existing.RFIDCode,
        existingTid = existing.tid,
        existingProductCode = existing.ProductCode,
        newItemCode = matchedItem.itemCode,
        newRfid = matchedItem.rfid,
        newTid = matchedItem.tid,
        newProductCode = matchedItem.productCode,
    )
}

fun isBulkItemAlreadyInChallanList(
    productList: List<ChallanDetails>,
    matchedItem: BulkItem,
): Boolean = productList.any { existing ->
    isDuplicateProductIdentity(
        existingItemCode = existing.ItemCode,
        existingRfid = existing.RFIDCode,
        existingTid = existing.tid,
        existingProductCode = existing.ProductCode,
        newItemCode = matchedItem.itemCode,
        newRfid = matchedItem.rfid,
        newTid = matchedItem.tid,
        newProductCode = matchedItem.productCode,
    )
}
