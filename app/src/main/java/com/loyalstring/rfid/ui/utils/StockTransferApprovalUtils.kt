package com.loyalstring.rfid.ui.utils

import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.model.addSingleItem.BoxModel
import com.loyalstring.rfid.data.model.addSingleItem.BranchModel
import com.loyalstring.rfid.data.model.addSingleItem.CounterModel
import com.loyalstring.rfid.data.model.addSingleItem.PacketModel
import com.loyalstring.rfid.data.model.stockTransfer.LabelledStockItems
import com.loyalstring.rfid.data.model.stockTransfer.StockTransferInOutResponse
import com.loyalstring.rfid.data.model.stockTransfer.StockTransferLineItem
import com.loyalstring.rfid.data.remote.data.IdNamePair
import com.loyalstring.rfid.repository.BulkRepositoryImpl

private const val BRANCH_TO_BRANCH_TRANSFER_TYPE_ID = 15

data class TransferLocationLookups(
    val counterPairs: List<IdNamePair> = emptyList(),
    val boxPairs: List<IdNamePair> = emptyList(),
    val branchPairs: List<IdNamePair> = emptyList(),
    val packetPairs: List<IdNamePair> = emptyList(),
)

data class TransferMasterData(
    val counters: List<CounterModel> = emptyList(),
    val branches: List<BranchModel> = emptyList(),
    val boxes: List<BoxModel> = emptyList(),
    val packets: List<PacketModel> = emptyList(),
    val lookups: TransferLocationLookups = TransferLocationLookups(),
)

/**
 * Self-approval applies when the transfer initiator and destination employee are the same.
 * Non branch-to-branch transfers always target the logged-in initiator as destination employee.
 */
fun isSelfApprovalTransfer(transfer: StockTransferInOutResponse): Boolean {
    val transferBy = transfer.TransferByEmployee?.trim().orEmpty()
    val transferTo = transfer.TransferToEmployee?.trim().orEmpty()

    return if (transfer.TransferTypeId == BRANCH_TO_BRANCH_TRANSFER_TYPE_ID) {
        transferBy.isNotBlank() && transferBy == transferTo
    } else {
        true
    }
}

fun parseTransferEndpointTypes(transferTypeName: String?): Pair<String?, String?> {
    val normalized = transferTypeName
        ?.trim()
        ?.replace(Regex("\\s+"), " ")
        ?: return null to null

    val parts = normalized.split(Regex("\\s+to\\s+", RegexOption.IGNORE_CASE))
        .map { it.trim().lowercase() }
    if (parts.size < 2) return null to null
    return parts[0] to parts[1]
}

private fun nameFromPairs(id: Int, pairs: List<IdNamePair>): String? =
    pairs.firstOrNull { it.id == id }?.name?.takeIf { it.isNotBlank() }

private fun resolveNameAcrossSources(
    id: Int,
    endpointType: String?,
    counters: List<CounterModel>,
    branches: List<BranchModel>,
    boxes: List<BoxModel>,
    packets: List<PacketModel>,
    lookups: TransferLocationLookups
): String? {
    endpointType?.lowercase()?.let { type ->
        resolveTypedLocationName(
            endpointType = type,
            id = id,
            counters = counters,
            branches = branches,
            boxes = boxes,
            packets = packets,
            lookups = lookups
        )?.let { return it }
    }

    return resolveTypedLocationName("counter", id, counters, branches, boxes, packets, lookups)
        ?: resolveTypedLocationName("box", id, counters, branches, boxes, packets, lookups)
        ?: resolveTypedLocationName("branch", id, counters, branches, boxes, packets, lookups)
        ?: resolveTypedLocationName("packet", id, counters, branches, boxes, packets, lookups)
}

private fun resolveTypedLocationName(
    endpointType: String,
    id: Int,
    counters: List<CounterModel>,
    branches: List<BranchModel>,
    boxes: List<BoxModel>,
    packets: List<PacketModel>,
    lookups: TransferLocationLookups
): String? {
    return when (endpointType.lowercase()) {
        "counter" -> {
            counters.firstOrNull { it.Id == id }?.CounterName?.takeIf { it.isNotBlank() }
                ?: counters.firstOrNull { it.CounterNumber == id.toString() }?.CounterName?.takeIf { it.isNotBlank() }
                ?: nameFromPairs(id, lookups.counterPairs)
        }
        "branch" -> {
            branches.firstOrNull { it.Id == id }?.BranchName?.takeIf { it.isNotBlank() }
                ?: nameFromPairs(id, lookups.branchPairs)
        }
        "box" -> {
            boxes.firstOrNull { it.Id == id }?.BoxName?.takeIf { it.isNotBlank() }
                ?: nameFromPairs(id, lookups.boxPairs)
        }
        "packet" -> {
            packets.firstOrNull { it.Id == id }?.PacketName?.takeIf { it.isNotBlank() }
                ?: nameFromPairs(id, lookups.packetPairs)
        }
        else -> null
    }
}

fun resolveTransferLocationName(
    endpointType: String?,
    locationId: Int?,
    apiName: String?,
    counters: List<CounterModel>,
    branches: List<BranchModel>,
    boxes: List<BoxModel>,
    packets: List<PacketModel>,
    lookups: TransferLocationLookups = TransferLocationLookups()
): String {
    apiName?.trim()?.takeIf { it.isNotBlank() && it != "-" }?.let { return it }
    val id = locationId ?: 0
    if (id <= 0) return "-"

    return resolveNameAcrossSources(
        id = id,
        endpointType = endpointType,
        counters = counters,
        branches = branches,
        boxes = boxes,
        packets = packets,
        lookups = lookups
    ) ?: id.toString()
}

private fun fallbackNamesFromItems(
    items: List<LabelledStockItems>,
    fromType: String?,
    toType: String?,
    counters: List<CounterModel>,
    branches: List<BranchModel>,
    boxes: List<BoxModel>,
    packets: List<PacketModel>,
    lookups: TransferLocationLookups
): Pair<String?, String?> {
    val item = items.firstOrNull() ?: return null to null

    fun fromItem(type: String?): String? = when (type?.lowercase()) {
        "counter" -> item.CounterId?.takeIf { it > 0 }?.let { counterId ->
            resolveNameAcrossSources(counterId, "counter", counters, branches, boxes, packets, lookups)
        }
        "box" -> item.BoxName?.takeIf { it.isNotBlank() }
            ?: item.BoxId?.takeIf { it > 0 }?.let { boxId ->
                resolveNameAcrossSources(boxId, "box", counters, branches, boxes, packets, lookups)
            }
        "branch" -> item.BranchName?.takeIf { it.isNotBlank() }
            ?: item.BranchId?.takeIf { it > 0 }?.let { branchId ->
                resolveNameAcrossSources(branchId, "branch", counters, branches, boxes, packets, lookups)
            }
        "packet" -> item.PacketName?.takeIf { it.isNotBlank() }
            ?: item.PacketId?.takeIf { it > 0 }?.let { packetId ->
                resolveNameAcrossSources(packetId, "packet", counters, branches, boxes, packets, lookups)
            }
        else -> null
    }

    return fromItem(fromType) to fromItem(toType)
}

private fun cleanName(value: String?): String? =
    value?.trim()?.takeIf { it.isNotBlank() && it != "-" }

private fun directTransferNames(
    transfer: StockTransferInOutResponse,
    fromType: String?,
    toType: String?
): Pair<String?, String?> {
    val from = cleanName(transfer.SourceName)
    val to = cleanName(transfer.DestinationName)
    return from to to
}

private fun lineItemNames(
    transfer: StockTransferInOutResponse,
    fromType: String?,
    toType: String?
): Pair<String?, String?> {
    val line = transfer.StockTransferItems?.firstOrNull()
        ?: return null to null

    val from = cleanName(line.SourceName)
        ?: when (fromType?.lowercase()) {
            "counter" -> cleanName(line.CounterName)
            "box" -> cleanName(line.BoxName)
            "packet" -> cleanName(line.PacketName)
            "branch" -> cleanName(line.BranchName)
            else -> null
        }

    val to = cleanName(line.DestinationName)
        ?: when (toType?.lowercase()) {
            "counter" -> cleanName(line.CounterName)
            "box" -> cleanName(line.BoxName)
            "packet" -> cleanName(line.PacketName)
            "branch" -> cleanName(line.BranchName)
            else -> null
        }

    return from to to
}

private suspend fun bulkLocationName(
    bulkRepository: BulkRepositoryImpl,
    stockId: Int?,
    type: String?
): String? {
    val id = stockId?.takeIf { it > 0 } ?: return null
    val bulk = bulkRepository.bulkItemDao.getById(id) ?: return null
    return when (type?.lowercase()) {
        "counter" -> cleanName(bulk.counterName)
        "box" -> cleanName(bulk.boxName)
        "branch" -> cleanName(bulk.branchName)
        "packet" -> cleanName(bulk.packetName)
        else -> cleanName(bulk.counterName)
            ?: cleanName(bulk.boxName)
            ?: cleanName(bulk.branchName)
            ?: cleanName(bulk.packetName)
    }
}

suspend fun resolveTransferFromToNamesAsync(
    transfer: StockTransferInOutResponse,
    masterData: TransferMasterData,
    bulkRepository: BulkRepositoryImpl,
    labelledItems: List<LabelledStockItems> = emptyList()
): Pair<String, String> {
    val (fromType, toType) = parseTransferEndpointTypes(transfer.StockTransferTypeName)
    val (directFrom, directTo) = directTransferNames(transfer, fromType, toType)
    val (lineFrom, lineTo) = lineItemNames(transfer, fromType, toType)

    var from = directFrom ?: lineFrom
    var to = directTo ?: lineTo

    if (from.isNullOrBlank()) {
        from = resolveTransferLocationName(
            endpointType = fromType,
            locationId = transfer.Source,
            apiName = null,
            counters = masterData.counters,
            branches = masterData.branches,
            boxes = masterData.boxes,
            packets = masterData.packets,
            lookups = masterData.lookups
        ).takeUnless { it == "-" || it.isNumericId() }
    }

    if (to.isNullOrBlank()) {
        to = resolveTransferLocationName(
            endpointType = toType,
            locationId = transfer.Destination,
            apiName = null,
            counters = masterData.counters,
            branches = masterData.branches,
            boxes = masterData.boxes,
            packets = masterData.packets,
            lookups = masterData.lookups
        ).takeUnless { it == "-" || it.isNumericId() }
    }

    val items = labelledItems.ifEmpty { resolveLabelledItemsFromTransfer(transfer) }
    if (from.isNullOrBlank() || to.isNullOrBlank()) {
        val stockIds = items.mapNotNull { it.Id?.takeIf { id -> id > 0 } }
        if (from.isNullOrBlank()) {
            from = stockIds.firstOrNull()?.let { bulkLocationName(bulkRepository, it, fromType) }
                ?: fallbackNamesFromItems(
                    items, fromType, toType,
                    masterData.counters, masterData.branches, masterData.boxes, masterData.packets,
                    masterData.lookups
                ).first
        }
        if (to.isNullOrBlank()) {
            to = transfer.StockTransferItems?.firstOrNull()?.let { line ->
                cleanName(line.DestinationName)
                    ?: when (toType?.lowercase()) {
                        "counter" -> cleanName(line.CounterName)
                        "box" -> cleanName(line.BoxName)
                        "packet" -> cleanName(line.PacketName)
                        "branch" -> cleanName(line.BranchName)
                        else -> null
                    }
            } ?: stockIds.lastOrNull()?.let { bulkLocationName(bulkRepository, it, toType) }
        }
    }

    if (to.isNullOrBlank()) {
        to = cleanName(transfer.TransferedToBranch)
    }

    return (from?.takeIf { it.isNotBlank() } ?: "-") to (to?.takeIf { it.isNotBlank() } ?: "-")
}

fun resolveTransferFromToNames(
    transfer: StockTransferInOutResponse,
    counters: List<CounterModel>,
    branches: List<BranchModel>,
    boxes: List<BoxModel>,
    packets: List<PacketModel>,
    lookups: TransferLocationLookups = TransferLocationLookups(),
    labelledItems: List<LabelledStockItems> = emptyList()
): Pair<String, String> {
    val (fromType, toType) = parseTransferEndpointTypes(transfer.StockTransferTypeName)
    val (directFrom, directTo) = directTransferNames(transfer, fromType, toType)
    if (!directFrom.isNullOrBlank() && !directTo.isNullOrBlank()) {
        return directFrom to directTo
    }

    val (lineFrom, lineTo) = lineItemNames(transfer, fromType, toType)

    var from = directFrom ?: lineFrom ?: resolveTransferLocationName(
        endpointType = fromType,
        locationId = transfer.Source,
        apiName = transfer.SourceName,
        counters = counters,
        branches = branches,
        boxes = boxes,
        packets = packets,
        lookups = lookups
    )
    var to = directTo ?: lineTo ?: resolveTransferLocationName(
        endpointType = toType,
        locationId = transfer.Destination,
        apiName = transfer.DestinationName,
        counters = counters,
        branches = branches,
        boxes = boxes,
        packets = packets,
        lookups = lookups
    )

    val items = labelledItems.ifEmpty { resolveLabelledItemsFromTransfer(transfer) }
    if ((from == "-" || from.isNumericId()) || (to == "-" || to.isNumericId())) {
        val (itemFrom, itemTo) = fallbackNamesFromItems(
            items = items,
            fromType = fromType,
            toType = toType,
            counters = counters,
            branches = branches,
            boxes = boxes,
            packets = packets,
            lookups = lookups
        )
        if (from == "-" || from.isNumericId()) {
            itemFrom?.takeIf { it.isNotBlank() }?.let { from = it }
        }
        if (to == "-" || to.isNumericId()) {
            itemTo?.takeIf { it.isNotBlank() }?.let { to = it }
        }
    }

    return from to to
}

private fun String.isNumericId(): Boolean = matches(Regex("^\\d+$"))

fun lineItemToLabelledStock(lineItem: StockTransferLineItem): LabelledStockItems {
    val stockId = lineItem.LabelledStockId?.takeIf { it > 0 }
        ?: lineItem.StockId?.takeIf { it > 0 }
    return LabelledStockItems(
        Id = stockId,
        TransferItemId = lineItem.TransferItemId?.takeIf { it > 0 } ?: lineItem.Id,
        ItemCode = lineItem.ItemCode,
        ProductTitle = lineItem.ProductTitle,
        CategoryName = lineItem.CategoryName,
        BranchName = lineItem.BranchName,
        BoxName = lineItem.BoxName,
        BoxId = lineItem.BoxId,
        CounterId = lineItem.CounterId,
        PacketName = lineItem.PacketName,
        PacketId = lineItem.PacketId,
        GrossWt = lineItem.GrossWt,
        NetWt = lineItem.NetWt,
        RequestStatus = lineItem.RequestStatus ?: lineItem.Status,
        Stones = emptyList(),
        Diamonds = emptyList()
    )
}

fun bulkItemToLabelledStock(
    bulkItem: BulkItem,
    lineItem: StockTransferLineItem? = null
): LabelledStockItems {
    return LabelledStockItems(
        Id = bulkItem.bulkItemId.takeIf { it > 0 },
        TransferItemId = lineItem?.TransferItemId?.takeIf { it > 0 } ?: lineItem?.Id,
        ItemCode = bulkItem.itemCode ?: lineItem?.ItemCode,
        ProductTitle = bulkItem.productName ?: lineItem?.ProductTitle,
        CategoryName = bulkItem.category ?: lineItem?.CategoryName,
        BranchName = bulkItem.branchName ?: lineItem?.BranchName,
        GrossWt = bulkItem.grossWeight ?: lineItem?.GrossWt,
        NetWt = bulkItem.netWeight ?: lineItem?.NetWt,
        RequestStatus = lineItem?.RequestStatus ?: lineItem?.Status,
        Stones = emptyList(),
        Diamonds = emptyList()
    )
}

fun resolveLabelledItemsFromTransfer(
    transfer: StockTransferInOutResponse?
): List<LabelledStockItems> {
    if (transfer == null) return emptyList()

    transfer.LabelledStockItems?.takeIf { it.isNotEmpty() }?.let { return it }

    return transfer.StockTransferItems.orEmpty().map(::lineItemToLabelledStock)
}

fun stockTransferListStatusText(
    pending: Int,
    approved: Int,
    rejected: Int,
    lost: Int,
    selectedStatus: String,
    pendingLabel: String,
    approvedLabel: String,
    rejectedLabel: String,
    lostLabel: String
): String {
    return when (selectedStatus) {
        pendingLabel -> "P: $pending"
        approvedLabel -> "A: $approved"
        rejectedLabel -> "R: $rejected"
        lostLabel -> "L: $lost"
        else -> buildString {
            if (pending > 0) append("P:$pending ")
            if (approved > 0) append("A:$approved ")
            if (rejected > 0) append("R:$rejected ")
            if (lost > 0) append("L:$lost")
        }.trim().ifBlank { "P:0" }
    }
}
