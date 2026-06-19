package com.loyalstring.rfid.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.core.content.FileProvider
import com.loyalstring.rfid.data.model.quotation.QuotationPrintData
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

private enum class QuotationCellAlign { LEFT, CENTER, RIGHT }

fun GenerateQuotationPdf(context: Context, data: QuotationPrintData) {
    val pdfDocument = PdfDocument()

    // A4 size for PdfDocument (approx points)
    val pageWidth = 595
    val pageHeight = 842
    val margin = 30

    // ---------- Paints ----------
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 22f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
    }

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.BLACK
    }

    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        color = Color.BLACK
    }

    // Outer borders (table)
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    // Light grid paint for summary box
    val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#DADADA")
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.BLACK
    }

    val tableCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        color = Color.BLACK
    }

    // Totals row style
    val totalRowFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F3F3F3")
        style = Paint.Style.FILL
    }

    val totalTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.BLACK
    }

    // ---------- Table columns ----------
    // [Item Code | RFID | Gr Wt | Nt Wt | Pcs | St Wt | St Amt | Amount]
    val tableX = margin
    val tableW = pageWidth - (margin * 2)

    val columnRatios = floatArrayOf(
        0.14f, // Item Code
        0.10f, // RFID
        0.10f, // Gr Wt
        0.10f, // Nt Wt
        0.08f, // Pcs
        0.11f, // St Wt
        0.13f, // St Amt
        0.14f  // Amount
    )

    val colWidths = IntArray(columnRatios.size).also { widths ->
        var used = 0
        for (i in columnRatios.indices) {
            widths[i] = if (i == columnRatios.lastIndex) tableW - used
            else (tableW * columnRatios[i]).toInt().also { used += it }
        }
    }

    val headers = listOf(
        "Item Code",
        "RFID",
        "Gr Wt",
        "Nt Wt",
        "Pcs",
        "St Wt",
        "St Amt",
        "Amount"
    )

    // Text columns left-aligned; numeric columns right-aligned
    val columnAlignments = arrayOf(
        QuotationCellAlign.LEFT,   // Item Code
        QuotationCellAlign.LEFT,   // RFID
        QuotationCellAlign.RIGHT,  // Gr Wt
        QuotationCellAlign.RIGHT,  // Nt Wt
        QuotationCellAlign.RIGHT,  // Pcs
        QuotationCellAlign.RIGHT,  // St Wt
        QuotationCellAlign.RIGHT,  // St Amt
        QuotationCellAlign.RIGHT   // Amount
    )

    val headerAlignments = arrayOf(
        QuotationCellAlign.LEFT,
        QuotationCellAlign.LEFT,
        QuotationCellAlign.CENTER,
        QuotationCellAlign.CENTER,
        QuotationCellAlign.CENTER,
        QuotationCellAlign.CENTER,
        QuotationCellAlign.CENTER,
        QuotationCellAlign.RIGHT
    )

    fun clipTextToWidth(text: String, paint: Paint, maxWidth: Float): String {
        if (text.isEmpty() || paint.measureText(text) <= maxWidth) return text
        val ellipsis = "…"
        val ellipsisW = paint.measureText(ellipsis)
        var end = text.length
        while (end > 0 && paint.measureText(text, 0, end) + ellipsisW > maxWidth) end--
        return if (end <= 0) ellipsis else text.take(end).trimEnd() + ellipsis
    }

    fun drawTextInCell(
        canvas: Canvas,
        text: String,
        cellX: Int,
        cellYTop: Int,
        cellW: Int,
        cellH: Int,
        basePaint: Paint,
        align: QuotationCellAlign
    ) {
        val paint = Paint(basePaint)
        val padH = 5
        val maxTextW = (cellW - padH * 2).coerceAtLeast(8).toFloat()
        val display = clipTextToWidth(text, paint, maxTextW)

        val fm = paint.fontMetrics
        val baselineY = cellYTop + (cellH - (fm.descent - fm.ascent)) / 2f - fm.ascent

        paint.textAlign = when (align) {
            QuotationCellAlign.LEFT -> Paint.Align.LEFT
            QuotationCellAlign.CENTER -> Paint.Align.CENTER
            QuotationCellAlign.RIGHT -> Paint.Align.RIGHT
        }

        val textX = when (align) {
            QuotationCellAlign.LEFT -> cellX + padH.toFloat()
            QuotationCellAlign.CENTER -> cellX + cellW / 2f
            QuotationCellAlign.RIGHT -> cellX + cellW - padH.toFloat()
        }

        canvas.drawText(display, textX, baselineY, paint)
    }

    fun drawTableCells(
        canvas: Canvas,
        rowX: Int,
        rowYTop: Int,
        rowHeight: Int,
        values: List<String>,
        paint: Paint,
        alignments: Array<QuotationCellAlign> = columnAlignments
    ) {
        var cx = rowX
        values.forEachIndexed { index, value ->
            drawTextInCell(
                canvas = canvas,
                text = value,
                cellX = cx,
                cellYTop = rowYTop,
                cellW = colWidths[index],
                cellH = rowHeight,
                basePaint = paint,
                align = alignments[index]
            )
            cx += colWidths[index]
        }
    }

    fun safeStr(s: String?): String = s?.trim().orEmpty()

    fun formatWeight(value: String?): String {
        val v = value?.replace("gm", "", ignoreCase = true)
            ?.replace("g", "", ignoreCase = true)
            ?.trim()
            ?.toDoubleOrNull()
        return if (v == null) "-" else String.format("%.3f", v)
    }

    fun formatAmount(value: String?): String {
        val v = value?.replace(",", "")?.trim()?.toDoubleOrNull()
        return if (v == null) "-" else String.format("%.2f", v)
    }

    fun formatInt(value: String?): String {
        val v = value?.trim()?.toDoubleOrNull()
        return if (v == null) "-" else String.format("%.0f", v)
    }

    fun drawRowLines(canvas: Canvas, yTop: Int, rowHeight: Int) {
        var x = tableX
        canvas.drawLine(tableX.toFloat(), yTop.toFloat(), (tableX + tableW).toFloat(), yTop.toFloat(), linePaint)
        canvas.drawLine(tableX.toFloat(), (yTop + rowHeight).toFloat(), (tableX + tableW).toFloat(), (yTop + rowHeight).toFloat(), linePaint)
        canvas.drawLine(tableX.toFloat(), yTop.toFloat(), tableX.toFloat(), (yTop + rowHeight).toFloat(), linePaint)

        for (w in colWidths) {
            x += w
            canvas.drawLine(x.toFloat(), yTop.toFloat(), x.toFloat(), (yTop + rowHeight).toFloat(), linePaint)
        }
    }

    // ---------- Pagination ----------
    val itemsPerPage = 12
    val pages = if (data.items.isEmpty()) 1 else ((data.items.size + itemsPerPage - 1) / itemsPerPage)

    var itemIndex = 0

    // Precompute totals for totals row
    val totalGross = data.items.sumOf { it.grossWt?.toDoubleOrNull() ?: 0.0 }
    val totalNet = data.items.sumOf { it.netWt?.toDoubleOrNull() ?: 0.0 }
    val totalPcs = data.items.sumOf { it.pcs?.toDoubleOrNull() ?: 0.0 }
    val totalStoneWt = data.items.sumOf { it.stoneWt?.toDoubleOrNull() ?: 0.0 }
    val totalStoneAmt = data.items.sumOf { it.stoneAmt?.toDoubleOrNull() ?: 0.0 }
    val totalAmt = data.items.sumOf { it.amount?.toDoubleOrNull() ?: 0.0 }

    for (pageNo in 0 until pages) {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNo + 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // ✅ Avoid any viewer “black” artifacts: fill white background
        canvas.drawColor(Color.WHITE)

        var y = margin + 10

        // -------- Title --------
        canvas.drawText("QUOTATION", (pageWidth / 2).toFloat(), y.toFloat(), titlePaint)
        y += 25

        // -------- Top fields --------
        val leftX = margin
        val rightX = pageWidth / 2 + 10

        fun drawPair(x: Int, label: String, value: String, yPos: Int) {
            canvas.drawText(label, x.toFloat(), yPos.toFloat(), labelPaint)
            canvas.drawText(value, (x + 120).toFloat(), yPos.toFloat(), valuePaint)
        }

        val y4 = y + 18
        drawPair(leftX, "Customer Name:", safeStr(data.customerName), y4)
        drawPair(rightX, "Remark:", safeStr(data.remark), y4)

        val y5 = y4 + 18
        drawPair(leftX, "Mobile:", safeStr(data.customerMobile), y5)

        val y6 = y5 + 18
        drawPair(leftX, "Customer Address:", safeStr(data.customerAddress), y6)

        y = y6 + 20

        // separator
        canvas.drawLine(margin.toFloat(), y.toFloat(), (pageWidth - margin).toFloat(), y.toFloat(), linePaint)
        y += 14

        // -------- Table header --------
        val headerHeight = 28
        drawRowLines(canvas, y, headerHeight)
        drawTableCells(canvas, tableX, y, headerHeight, headers, tableHeaderPaint, headerAlignments)
        y += headerHeight

        // -------- Table rows --------
        val rowHeight = 32
        val endIndex = min(itemIndex + itemsPerPage, data.items.size)

        if (data.items.isEmpty()) {
            drawRowLines(canvas, y, rowHeight)
            drawTableCells(
                canvas, tableX, y, rowHeight,
                listOf("-", "-", "-", "-", "-", "-", "-", "-"),
                tableCellPaint
            )
            y += rowHeight
        } else {
            for (i in itemIndex until endIndex) {
                val it = data.items[i]
                drawRowLines(canvas, y, rowHeight)

                drawTableCells(
                    canvas = canvas,
                    rowX = tableX,
                    rowYTop = y,
                    rowHeight = rowHeight,
                    values = listOf(
                        safeStr(it.itemCode).ifBlank { "-" },
                        safeStr(it.rfidNo).ifBlank { "-" },
                        formatWeight(it.grossWt),
                        formatWeight(it.netWt),
                        formatInt(it.pcs),
                        formatWeight(it.stoneWt),
                        formatAmount(it.stoneAmt),
                        formatAmount(it.amount)
                    ),
                    paint = tableCellPaint
                )

                y += rowHeight
            }
        }

        itemIndex = endIndex

        // ✅ Totals row inside table like screenshot (only on last page after last item)
        if (pageNo == pages - 1 && itemIndex == data.items.size && data.items.isNotEmpty()) {
            // background
            canvas.drawRect(
                tableX.toFloat(),
                y.toFloat(),
                (tableX + tableW).toFloat(),
                (y + rowHeight).toFloat(),
                totalRowFillPaint
            )

            drawRowLines(canvas, y, rowHeight)

            val totalAlignments = arrayOf(
                QuotationCellAlign.LEFT,
                QuotationCellAlign.LEFT,
                QuotationCellAlign.RIGHT,
                QuotationCellAlign.RIGHT,
                QuotationCellAlign.RIGHT,
                QuotationCellAlign.RIGHT,
                QuotationCellAlign.RIGHT,
                QuotationCellAlign.RIGHT
            )

            drawTableCells(
                canvas = canvas,
                rowX = tableX,
                rowYTop = y,
                rowHeight = rowHeight,
                values = listOf(
                    "Total",
                    "",
                    String.format("%.3f", totalGross),
                    String.format("%.3f", totalNet),
                    String.format("%.0f", totalPcs),
                    String.format("%.3f", totalStoneWt),
                    String.format("%.2f", totalStoneAmt),
                    String.format("%.2f", totalAmt)
                ),
                paint = totalTextPaint,
                alignments = totalAlignments
            )

            y += rowHeight
        }

        // -------- Bottom-right total (no GST breakdown) --------
        if (pageNo == pages - 1) {
            val finalTotal = data.totalAmount.toDoubleOrNull() ?: totalAmt

            val boxW = 260
            val boxH = 44
            val boxX = pageWidth - margin - boxW
            val boxY = pageHeight - margin - boxH - 40

            canvas.drawRect(
                boxX.toFloat(), boxY.toFloat(),
                (boxX + boxW).toFloat(), (boxY + boxH).toFloat(),
                gridPaint
            )

            val valueRight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = Color.BLACK
                textAlign = Paint.Align.RIGHT
            }

            canvas.drawText(
                "Total Amount:",
                (boxX + 12).toFloat(),
                (boxY + 28).toFloat(),
                labelPaint
            )
            canvas.drawText(
                String.format("%.2f", finalTotal),
                (boxX + boxW - 12).toFloat(),
                (boxY + 28).toFloat(),
                valueRight
            )

            canvas.drawText("Customer Sign:", margin.toFloat(), (pageHeight - margin).toFloat(), labelPaint)
            canvas.drawText("For VT", (pageWidth - margin - 60).toFloat(), (pageHeight - margin).toFloat(), labelPaint)
        }

        pdfDocument.finishPage(page)
    }

    // ---------- Save ----------
    val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "quotations")
    if (!dir.exists()) dir.mkdirs()

    val safeNo = data.quotationNo.ifBlank { "NA" }
    val file = File(dir, "Quotation_$safeNo.pdf")

    FileOutputStream(file).use { out ->
        pdfDocument.writeTo(out)
    }
    pdfDocument.close()

    // ---------- Open ----------
    openPdfFile(context, file)
}

private fun openPdfFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
    }
    context.startActivity(intent)
}


/*
package com.loyalstring.rfid.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.core.content.FileProvider
import com.loyalstring.rfid.data.model.quotation.QuotationPrintData
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

fun GenerateQuotationPdf(context: Context, data: QuotationPrintData) {
    val pdfDocument = PdfDocument()

    // A4 approx size in "points-like px" used by PdfDocument
    val pageWidth = 595
    val pageHeight = 842
    val margin = 30

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 22f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
    }

    val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.BLACK
    }

    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        color = Color.BLACK
    }

    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        strokeWidth = 1f
        style = Paint.Style.STROKE   // ✅ important
    }

    val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 11f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        color = Color.BLACK
    }

    val tableCellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        color = Color.BLACK
    }

    // ---------- Table columns (similar to your screenshot) ----------
    // [Image | Particulars | GrossWt | NetWt | Qty | Rate/Gm | Making/Gm | Amount]
    val tableX = margin
    val tableW = pageWidth - (margin * 2)

    val colWidths = intArrayOf(
        (tableW * 0.10).toInt(), // Image
        (tableW * 0.28).toInt(), // Particulars
        (tableW * 0.10).toInt(), // Gross
        (tableW * 0.10).toInt(), // Net
        (tableW * 0.08).toInt(), // Qty
        (tableW * 0.11).toInt(), // Rate/Gm
        (tableW * 0.12).toInt(), // Making/Gm
        (tableW * 0.11).toInt()  // Amount
    )
    val headers = listOf("Image", "Particulars", "Gross Wt", "Net Wt", "Quantity", "Rate/Gm", "Making/Gm", "Amount")

    fun drawRowLines(canvas: Canvas, yTop: Int, rowHeight: Int) {
        var x = tableX
        canvas.drawLine(tableX.toFloat(), yTop.toFloat(), (tableX + tableW).toFloat(), yTop.toFloat(), linePaint)
        canvas.drawLine(tableX.toFloat(), (yTop + rowHeight).toFloat(), (tableX + tableW).toFloat(), (yTop + rowHeight).toFloat(), linePaint)
        canvas.drawLine(tableX.toFloat(), yTop.toFloat(), tableX.toFloat(), (yTop + rowHeight).toFloat(), linePaint)

        for (w in colWidths) {
            x += w
            canvas.drawLine(x.toFloat(), yTop.toFloat(), x.toFloat(), (yTop + rowHeight).toFloat(), linePaint)
        }
    }

    fun drawTextInCell(canvas: Canvas, text: String, x: Int, y: Int, w: Int, paint: Paint) {
        val clipped = text.take(40) // simple clip
        canvas.drawText(clipped, (x + 6).toFloat(), (y).toFloat(), paint)
    }

    // ---------- Pagination ----------
    val itemsPerPage = 14  // adjust if needed
    val pages = if (data.items.isEmpty()) 1 else ((data.items.size + itemsPerPage - 1) / itemsPerPage)

    var itemIndex = 0

    for (pageNo in 0 until pages) {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNo + 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        var y = margin + 10

        // -------- Title --------
        canvas.drawText("QUOTATION", (pageWidth / 2).toFloat(), y.toFloat(), titlePaint)
        y += 25

        // -------- Top section: Left & Right --------
        val leftX = margin
        val rightX = pageWidth / 2 + 10

        fun drawPair(x: Int, label: String, value: String, yPos: Int) {
            canvas.drawText(label, x.toFloat(), yPos.toFloat(), labelPaint)
            canvas.drawText(value, (x + 120).toFloat(), yPos.toFloat(), valuePaint)
        }

       */
/* val y1 = y + 10
        drawPair(leftX, "Owner Name:", data.ownerName, y1)
        drawPair(rightX, "Quotation No:", data.quotationNo, y1)

        val y2 = y1 + 18
        drawPair(leftX, "Owner Address:", data.ownerAddress, y2)
        drawPair(rightX, "Date:", data.date, y2)

        val y3 = y2 + 18
       drawPair(leftX, "Contact:", data.customerMobile, y3)
        //drawPair(rightX, "Sales Men:", data.salesMen, y3)*//*


        val y4 = y + 18
        drawPair(leftX, "Customer Name:", data.customerName, y4)
        drawPair(rightX, "Remark:", data.remark.toString(), y4)

        val y5 = y4 + 18
        drawPair(leftX, "Mobile:", data.customerMobile, y5)

        val y6 = y5 + 18
        drawPair(leftX, "Customer Address:", data.customerAddress, y6)

        y = y6 + 20

        // separator
        canvas.drawLine(margin.toFloat(), y.toFloat(), (pageWidth - margin).toFloat(), y.toFloat(), linePaint)
        y += 14

        // -------- Table header --------
        val headerHeight = 24
        drawRowLines(canvas, y, headerHeight)
        var x = tableX
        for (i in headers.indices) {
            drawTextInCell(canvas, headers[i], x, y + 16, colWidths[i], tableHeaderPaint)
            x += colWidths[i]
        }
        y += headerHeight

        // -------- Table rows --------
        val rowHeight = 36
        val endIndex = min(itemIndex + itemsPerPage, data.items.size)

        if (data.items.isEmpty()) {
            // Empty state row
            drawRowLines(canvas, y, rowHeight)
            x = tableX
            drawTextInCell(canvas, "-", x, y + 20, colWidths[0], tableCellPaint)
            x += colWidths[0]
            drawTextInCell(canvas, "-", x, y + 20, colWidths[1], tableCellPaint)
            y += rowHeight
        } else {
            for (i in itemIndex until endIndex) {
                val it = data.items[i]
                drawRowLines(canvas, y, rowHeight)

                var cx = tableX

              */
/*  // Image cell (optional)
                if (!it.imagePath.isNullOrBlank()) {
                    try {
                        val bmp = BitmapFactory.decodeFile(it.imagePath)
                        if (bmp != null) {
                            val targetW = colWidths[0] - 12
                            val targetH = rowHeight - 12
                            val scaled = Bitmap.createScaledBitmap(bmp, targetW, targetH, true)
                            canvas.drawBitmap(scaled, (cx + 6).toFloat(), (y + 6).toFloat(), null)
                        } else {
                            drawTextInCell(canvas, "-", cx, y + 22, colWidths[0], tableCellPaint)
                        }
                    } catch (_: Exception) {
                        drawTextInCell(canvas, "-", cx, y + 22, colWidths[0], tableCellPaint)
                    }
                } else {
                    drawTextInCell(canvas, "-", cx, y + 22, colWidths[0], tableCellPaint)
                }*//*

                cx += colWidths[0]

                drawTextInCell(canvas, it.particulars, cx, y + 20, colWidths[1], tableCellPaint); cx += colWidths[1]
                drawTextInCell(canvas, it.grossWt!!.ifBlank { "-" }, cx, y + 20, colWidths[2], tableCellPaint); cx += colWidths[2]
                drawTextInCell(canvas, it.netWt!!.ifBlank { "-" }, cx, y + 20, colWidths[3], tableCellPaint); cx += colWidths[3]
              //  drawTextInCell(canvas, it.quantity.ifBlank { "-" }, cx, y + 20, colWidths[4], tableCellPaint); cx += colWidths[4]
                drawTextInCell(canvas, it.ratePerGm!!.ifBlank { "-" }, cx, y + 20, colWidths[5], tableCellPaint); cx += colWidths[5]
                drawTextInCell(canvas, it.makingPerGm!!.ifBlank { "-" }, cx, y + 20, colWidths[6], tableCellPaint); cx += colWidths[6]
                drawTextInCell(canvas, it.amount!!.ifBlank { "-" }, cx, y + 20, colWidths[7], tableCellPaint)

                y += rowHeight
            }
        }

        itemIndex = endIndex

        // -------- Totals area (bottom-right, like screenshot) ----------
        // Only draw on LAST page
        if (pageNo == pages - 1) {
            y += 8

            // totals line
            canvas.drawLine(margin.toFloat(), y.toFloat(), (pageWidth - margin).toFloat(), y.toFloat(), linePaint)
            y += 14

            // Summary row (Gross/Net/Qty/Amount)
            // We’ll print few totals aligned roughly
            val summaryY = y + 10
            canvas.drawText("Totals:", (pageWidth - margin - 250).toFloat(), summaryY.toFloat(), labelPaint)
          //  canvas.drawText("Gross: ${data.totalGrossWt}", (pageWidth - margin - 200).toFloat(), (summaryY + 16).toFloat(), valuePaint)
            //canvas.drawText("Net: ${data.totalNetWt}", (pageWidth - margin - 200).toFloat(), (summaryY + 32).toFloat(), valuePaint)
            //canvas.drawText("Qty: ${data.totalQty}", (pageWidth - margin - 200).toFloat(), (summaryY + 48).toFloat(), valuePaint)
            canvas.drawText("Amount: ${data.totalAmount}", (pageWidth - margin - 200).toFloat(), (summaryY + 64).toFloat(), valuePaint)

            // GST box (very simple)
            val boxW = 240
            val boxH = 70
            val boxX = pageWidth - margin - boxW
            val boxY = pageHeight - margin - boxH

            canvas.drawRect(
                boxX.toFloat(), boxY.toFloat(),
                (boxX + boxW).toFloat(), (boxY + boxH).toFloat(),
                linePaint
            )
            canvas.drawText("CGST:", (boxX + 10).toFloat(), (boxY + 24).toFloat(), labelPaint)
            canvas.drawText(data.cgst, (boxX + boxW - 10).toFloat(), (boxY + 24).toFloat(),
                Paint(valuePaint).apply { textAlign = Paint.Align.RIGHT })

            canvas.drawText("SGST:", (boxX + 10).toFloat(), (boxY + 46).toFloat(), labelPaint)
            canvas.drawText(data.sgst, (boxX + boxW - 10).toFloat(), (boxY + 46).toFloat(),
                Paint(valuePaint).apply { textAlign = Paint.Align.RIGHT })
        }

        pdfDocument.finishPage(page)
    }

    // ---------- Save ----------
    val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "quotations")
    if (!dir.exists()) dir.mkdirs()

    val safeNo = data.quotationNo.ifBlank { "NA" }
    val file = File(dir, "Quotation_$safeNo.pdf")

    FileOutputStream(file).use { out ->
        pdfDocument.writeTo(out)
    }
    pdfDocument.close()

    // ---------- Open ----------
    openPdfFile(context, file)
}

private fun openPdfFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
    }
    context.startActivity(intent)
}
*/
