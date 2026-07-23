package com.loyalstring.rfid.ui.screens

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Image
import com.itextpdf.layout.element.LineSeparator
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.loyalstring.rfid.data.model.sampleOut.SampleOutPrintData
import com.loyalstring.rfid.ui.utils.SAMPLE_OUT_ITEM_IMAGES_ENABLED
import com.loyalstring.rfid.ui.utils.loadProductImageBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

suspend fun generateSampleOutPrintPdf(context: Context, data: SampleOutPrintData) =
    withContext(Dispatchers.IO) {

    val file = File(
        context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS),
        "SampleOut_${data.sampleOutNo.ifBlank { "Print" }}.pdf"
    )

    val itemImages = if (SAMPLE_OUT_ITEM_IMAGES_ENABLED) {
        data.items.map { item ->
            loadProductImageBytes(
                context = context,
                imageUrl = item.imageUrl,
                itemCode = item.itemCode,
                designName = item.designName,
            )
        }
    } else {
        emptyList()
    }

    val writer = PdfWriter(file)
    val pdf = PdfDocument(writer)
    val doc = Document(pdf, PageSize.A4)
    doc.setMargins(20f, 20f, 20f, 20f)

    doc.add(
        Paragraph(data.companyName)
            .setTextAlignment(TextAlignment.CENTER)
            .setBold()
            .setFontSize(16f)
    )

    doc.add(LineSeparator(SolidLine(1f)).setMarginTop(8f).setMarginBottom(12f))

    doc.add(
        Paragraph("Sample Out Print")
            .setTextAlignment(TextAlignment.CENTER)
            .setBold()
            .setFontSize(12f)
            .setMarginBottom(14f)
    )

    val infoTable = Table(UnitValue.createPercentArray(floatArrayOf(1f, 1f)))
        .setWidth(UnitValue.createPercentValue(100f))
        .setBorder(Border.NO_BORDER)

    val leftInfo = """
        Customer Name: ${data.customerName}
        Address/City: ${data.addressCity}
        Contact No: ${data.contactNo}
    """.trimIndent()

    val rightInfo = """
        Sample Out No: ${data.sampleOutNo}
        Date: ${data.date}
        ReturnDate: ${data.returnDate}
    """.trimIndent()

    infoTable.addCell(
        Cell().add(Paragraph(leftInfo).setFontSize(10f))
            .setBorder(Border.NO_BORDER)
    )
    infoTable.addCell(
        Cell().add(Paragraph(rightInfo).setFontSize(10f).setTextAlignment(TextAlignment.RIGHT))
            .setBorder(Border.NO_BORDER)
    )

    doc.add(infoTable)
    doc.add(Paragraph("\n"))

    val colWidths = if (SAMPLE_OUT_ITEM_IMAGES_ENABLED) {
        floatArrayOf(
            0.55f,  // Sr.No
            1.8f,   // Item Details
            0.9f,   // Gross
            0.9f,   // Stone
            0.9f,   // Diamond
            0.9f,   // Net
            0.7f,   // Pieces
            0.9f,   // Status
            1.0f    // View / Image
        )
    } else {
        floatArrayOf(
            0.55f,  // Sr.No
            2.0f,   // Item Details
            0.9f,   // Gross
            0.9f,   // Stone
            0.9f,   // Diamond
            0.9f,   // Net
            0.7f,   // Pieces
            0.9f,   // Status
        )
    }

    val table = Table(UnitValue.createPercentArray(colWidths))
        .setWidth(UnitValue.createPercentValue(100f))

    fun headerCell(text: String) =
        Cell()
            .add(Paragraph(text).setBold().setFontSize(9f).setTextAlignment(TextAlignment.CENTER))
            .setBackgroundColor(ColorConstants.LIGHT_GRAY)

    table.addHeaderCell(headerCell("Sr.No"))
    table.addHeaderCell(headerCell("Item Details"))
    table.addHeaderCell(headerCell("Gross Wt"))
    table.addHeaderCell(headerCell("Stone Wt"))
    table.addHeaderCell(headerCell("Diamond\nWt"))
    table.addHeaderCell(headerCell("Net Wt"))
    table.addHeaderCell(headerCell("Pieces"))
    table.addHeaderCell(headerCell("Status"))
    if (SAMPLE_OUT_ITEM_IMAGES_ENABLED) {
        table.addHeaderCell(headerCell("View"))
    }

    fun n(v: String?): Double = v?.toDoubleOrNull() ?: 0.0
    fun fmt(d: Double): String = String.format(Locale.US, "%.3f", d)

    var totalGross = 0.0
    var totalStone = 0.0
    var totalDiamond = 0.0
    var totalNet = 0.0
    var totalPieces = 0.0

    data.items.forEachIndexed { idx, it ->
        val g = n(it.grossWt); totalGross += g
        val s = n(it.stoneWt); totalStone += s
        val d = n(it.diamondWt); totalDiamond += d
        val nw = n(it.netWt); totalNet += nw
        val p = n(it.pieces); totalPieces += p

        table.addCell(Cell().add(Paragraph((idx + 1).toString()).setFontSize(9f)).setTextAlignment(TextAlignment.CENTER))
        table.addCell(Cell().add(Paragraph(it.itemDetails).setFontSize(9f)).setTextAlignment(TextAlignment.LEFT))
        table.addCell(Cell().add(Paragraph(fmt(g)).setFontSize(9f)).setTextAlignment(TextAlignment.CENTER))
        table.addCell(Cell().add(Paragraph(fmt(s)).setFontSize(9f)).setTextAlignment(TextAlignment.CENTER))
        table.addCell(Cell().add(Paragraph(fmt(d)).setFontSize(9f)).setTextAlignment(TextAlignment.CENTER))
        table.addCell(Cell().add(Paragraph(fmt(nw)).setFontSize(9f)).setTextAlignment(TextAlignment.CENTER))
        table.addCell(Cell().add(Paragraph(p.toInt().toString()).setFontSize(9f)).setTextAlignment(TextAlignment.CENTER))
        table.addCell(Cell().add(Paragraph(it.status).setFontSize(9f)).setTextAlignment(TextAlignment.CENTER))

        if (SAMPLE_OUT_ITEM_IMAGES_ENABLED) {
            val viewCell = Cell().setTextAlignment(TextAlignment.CENTER).setPadding(3f)
            val imgBytes = itemImages.getOrNull(idx)
            if (imgBytes != null) {
                viewCell.add(
                    Image(ImageDataFactory.create(imgBytes))
                        .scaleToFit(40f, 40f)
                        .setHorizontalAlignment(HorizontalAlignment.CENTER)
                )
            }
            table.addCell(viewCell)
        }
    }

    table.addCell(Cell().add(Paragraph("Total").setBold().setFontSize(9f)).setTextAlignment(TextAlignment.CENTER))
    table.addCell(Cell().add(Paragraph("").setFontSize(9f)))
    table.addCell(Cell().add(Paragraph(fmt(totalGross)).setBold().setFontSize(9f)).setTextAlignment(TextAlignment.CENTER))
    table.addCell(Cell().add(Paragraph(fmt(totalStone)).setBold().setFontSize(9f)).setTextAlignment(TextAlignment.CENTER))
    table.addCell(Cell().add(Paragraph(fmt(totalDiamond)).setBold().setFontSize(9f)).setTextAlignment(TextAlignment.CENTER))
    table.addCell(Cell().add(Paragraph(fmt(totalNet)).setBold().setFontSize(9f)).setTextAlignment(TextAlignment.CENTER))
    table.addCell(Cell().add(Paragraph(totalPieces.toInt().toString()).setBold().setFontSize(9f)).setTextAlignment(TextAlignment.CENTER))
    table.addCell(Cell().add(Paragraph("").setFontSize(9f)))
    if (SAMPLE_OUT_ITEM_IMAGES_ENABLED) {
        table.addCell(Cell().add(Paragraph("").setFontSize(9f)))
    }

    doc.add(table)
    doc.close()

    withContext(Dispatchers.Main) {
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

        context.startActivity(Intent.createChooser(intent, "Open PDF with..."))
    }
}
