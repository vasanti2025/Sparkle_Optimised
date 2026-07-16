package com.loyalstring.rfid.ui.utils

import com.loyalstring.rfid.data.model.quotation.QuotationItem
import java.util.Locale

data class QuotationItemAmounts(
    val metalAmt: Double,
    val makingAmt: Double,
    val itemAmt: Double
)

/**
 * Quotation item calculation:
 * - metalAmt (rate) = netWt * ratePerGram
 * - makingAmt = metalAmt * (wastagePercent / 100)
 * - itemAmt = metalAmt + makingAmt + stoneAmt + diamondAmt + hallmarkAmt
 */
fun calcQuotationItemAmounts(
    netWt: Double,
    ratePerGram: Double,
    wastagePercent: Double,
    stoneAmt: Double = 0.0,
    diamondAmt: Double = 0.0,
    hallmarkAmt: Double = 0.0
): QuotationItemAmounts {
    val metalAmt = netWt * ratePerGram
    val makingAmt = metalAmt * (wastagePercent / 100.0)
    val itemAmt = metalAmt + makingAmt + stoneAmt + diamondAmt + hallmarkAmt
    return QuotationItemAmounts(metalAmt, makingAmt, itemAmt)
}

/**
 * Convert stored wastage value to percent for calculation.
 * - 0.200 (FineWastageWt style) -> 2%
 * - 2 or 2.0 (already percent) -> 2%
 */
fun resolveWastagePercentForCalc(raw: String?): Double {
    val v = raw?.trim()?.replace(",", "")?.toDoubleOrNull() ?: return 0.0
    return if (v > 0.0 && v < 1.0) v * 10.0 else v
}

/** API/storage format: FineWastageWt = wastage% / 10 */
fun fineWastageWtFromPercent(percent: Double): String =
    String.format(Locale.getDefault(), "%.3f", percent / 10.0)

fun calcQuotationFromItemFields(
    netWt: Double,
    ratePerGram: Double,
    wastageRaw: String?,
    stoneAmt: Double = 0.0,
    diamondAmt: Double = 0.0,
    hallmarkAmt: Double = 0.0
): QuotationItemAmounts {
    val wastagePercent = resolveWastagePercentForCalc(wastageRaw)
    return calcQuotationItemAmounts(
        netWt = netWt,
        ratePerGram = ratePerGram,
        wastagePercent = wastagePercent,
        stoneAmt = stoneAmt,
        diamondAmt = diamondAmt,
        hallmarkAmt = hallmarkAmt
    )
}

/**
 * Wastage% from API response: FineWastageWt * 10
 * e.g. FineWastageWt = 0.200 -> wastage% = 2
 */
fun wastagePercentFromFineWastageWt(fineWastageWt: String?): String? {
    val raw = fineWastageWt?.trim().orEmpty()
    if (raw.isBlank() || raw.equals("null", true)) return null
    val wt = raw.replace(",", "").toDoubleOrNull() ?: return null
    return String.format(Locale.getDefault(), "%.2f", wt * 10.0)
}

fun resolveQuotationPrintWastagePercent(
    fineWastageWt: String?,
    fixWastage: String? = null,
    makingFixedWastage: String? = null
): String {
    wastagePercentFromFineWastageWt(fineWastageWt)?.let { return it }

    val fallback = fixWastage?.trim()?.takeIf { it.isNotBlank() && !it.equals("null", true) }
        ?: makingFixedWastage?.trim()?.takeIf { it.isNotBlank() && !it.equals("null", true) }
        ?: return "0.00"

    val fallbackVal = fallback.replace(",", "").toDoubleOrNull() ?: return fallback

    // Values like 0.200 are weight-style and need * 10 to become percent.
    return if (fallbackVal > 0.0 && fallbackVal < 1.0) {
        String.format(Locale.getDefault(), "%.2f", fallbackVal * 10.0)
    } else {
        String.format(Locale.getDefault(), "%.2f", fallbackVal)
    }
}

/** Quotation list PDF: wastage% = FineWastageWt * 10 */
fun QuotationItem.quotationPrintWastagePercent(): String {
    val fineWt = FineWastageWt
        ?.trim()
        ?.takeIf { it.isNotBlank() && !it.equals("null", true) }
        ?.replace(",", "")
        ?.toDoubleOrNull()

    if (fineWt != null) {
        return String.format(Locale.getDefault(), "%.2f", fineWt * 10.0)
    }

    return resolveQuotationPrintWastagePercent(
        fineWastageWt = null,
        fixWastage = fixWastage,
        makingFixedWastage = MakingFixedWastage
    )
}

/** Clean item payload for AddQuotation API – avoids invalid FineWastageWt objects. */
fun QuotationItem.toAddQuotationApiItem(
    clientCode: String,
    customerId: Int,
    branchId: Int
): QuotationItem {
    val wastagePercent = resolveWastagePercentForCalc(
        fixWastage ?: MakingFixedWastage
    )
    val fineWastageWtValue = fineWastageWtFromPercent(wastagePercent)
    val wastageStr = String.format(Locale.getDefault(), "%.2f", wastagePercent)

    return copy(
        ClientCode = clientCode,
        CustomerId = customerId,
        BranchId = branchId,
        FineWastageWt = fineWastageWtValue,
        MakingFixedWastage = wastageStr,
        fixWastage = wastageStr,
        Quantity = (qty ?: Quantity?.toIntOrNull() ?: 1).toString(),
        Pieces = Pieces?.takeIf { it.isNotBlank() && it != "0" } ?: (qty ?: 1).toString(),
        DiamondAmt = DiamondAmt?.takeIf { it.isNotBlank() } ?: "",
        DiamondWeight = DiamondWeight?.takeIf { it.isNotBlank() } ?: DiamondWt ?: "",
        StoneAmount = StoneAmt ?: StoneAmount ?: TotalStoneAmount ?: "0",
        RatePerGram = RatePerGram ?: MetalRate ?: totayRate ?: "0",
        MetalAmount = MetalAmount ?: "0.00",
        TotalItemAmount = TotalItemAmount ?: itemAmt ?: Amount ?: TotalAmount,
        itemAmt = itemAmt ?: TotalItemAmount ?: Amount ?: TotalAmount
    )
}
