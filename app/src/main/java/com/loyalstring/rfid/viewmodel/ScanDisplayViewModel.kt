package com.loyalstring.rfid.viewmodel

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.loyalstring.rfid.data.local.dao.CustomerEmailDao
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.local.entity.CustomerEmailEntity
import com.loyalstring.rfid.data.model.DetailedItem
import com.loyalstring.rfid.data.model.SummaryItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

import java.util.Properties
import javax.activation.DataHandler
import javax.activation.DataSource
import javax.activation.FileDataSource
import javax.inject.Inject
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import com.itextpdf.layout.Document as PdfLayoutDocument


@HiltViewModel
class ScanDisplayViewModel @Inject constructor(
    private val customerEmailDao: CustomerEmailDao
) : ViewModel() {

    private val _emailStatus = MutableStateFlow<String?>(null)
    val emailStatus: StateFlow<String?> = _emailStatus

    fun saveEmail(email: String) {
        viewModelScope.launch(Dispatchers.IO) {

            customerEmailDao.insertEmail(CustomerEmailEntity(email = email))
        }
    }

    suspend fun getAllEmails(): List<String> {
        return customerEmailDao.getAllEmails().map { it.email }
    }

    suspend fun generateScanReportPdf(
        context: Context,
        allItemsSummary: List<SummaryItem>,
        matchedItems: List<DetailedItem>,
        unmatchedItems: List<DetailedItem>
    ): File {
        val file = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
            "report.pdf"
        )

        val writer = PdfWriter(file)
        val pdf = PdfDocument(writer)
        val doc = PdfLayoutDocument(pdf, PageSize.A4)

        // Header helper
        fun addHeader(title: String) {
            doc.add(Paragraph(title).setBold().setFontSize(12f))
        }

        // ----------------------------
        // 1) All Items Summary
        // ----------------------------
        addHeader("All Items Summary")
        val summaryTable = Table(
            floatArrayOf(60f, 60f, 60f, 50f, 55f, 65f, 60f, 60f, 60f)
        ).useAllAvailableWidth()

        listOf(
            "Counter Name", "Category", "Product",
            "Total Qty", "Match Qty", "Unmatch Qty",
            "Total G.Wt", "Match G.Wt", "Unmatch G.Wt"
        ).forEach { col ->
            summaryTable.addHeaderCell(
                Cell().add(
                    Paragraph(col).setBold().setFontSize(8f).setTextAlignment(TextAlignment.CENTER)
                )
            )
        }

        var totalQty = 0
        var totalMatch = 0
        var totalUnmatch = 0
        var totalGwt = 0.0
        var totalMatchGwt = 0.0
        var totalUnmatchGwt = 0.0

        allItemsSummary.forEach {
            summaryTable.addCell(
                Cell().add(
                    Paragraph(it.counterName ?: "-").setFontSize(8f)
                        .setTextAlignment(TextAlignment.LEFT)
                )
            )
            summaryTable.addCell(
                Cell().add(
                    Paragraph(it.category ?: "-").setFontSize(8f)
                        .setTextAlignment(TextAlignment.LEFT)
                )
            )
            summaryTable.addCell(
                Cell().add(
                    Paragraph(it.product ?: "-").setFontSize(8f)
                        .setTextAlignment(TextAlignment.LEFT)
                )
            )
            summaryTable.addCell(
                Cell().add(
                    Paragraph(it.totalQty.toString()).setFontSize(8f)
                        .setTextAlignment(TextAlignment.CENTER)
                )
            )
            summaryTable.addCell(
                Cell().add(
                    Paragraph(it.matchQty.toString()).setFontSize(8f)
                        .setTextAlignment(TextAlignment.CENTER)
                )
            )
            summaryTable.addCell(
                Cell().add(
                    Paragraph(it.unmatchQty.toString()).setFontSize(8f)
                        .setTextAlignment(TextAlignment.CENTER)
                )
            )
            summaryTable.addCell(
                Cell().add(
                    Paragraph(it.totalGrossWt ?: "-").setFontSize(8f)
                        .setTextAlignment(TextAlignment.CENTER)
                )
            )
            summaryTable.addCell(
                Cell().add(
                    Paragraph(it.matchGrossWt ?: "-").setFontSize(8f)
                        .setTextAlignment(TextAlignment.CENTER)
                )
            )
            summaryTable.addCell(
                Cell().add(
                    Paragraph(it.unmatchGrossWt ?: "-").setFontSize(8f)
                        .setTextAlignment(TextAlignment.CENTER)
                )
            )

            totalQty += it.totalQty
            totalMatch += it.matchQty
            totalUnmatch += it.unmatchQty
            totalGwt += it.totalGrossWt?.toDoubleOrNull() ?: 0.0
            totalMatchGwt += it.matchGrossWt?.toDoubleOrNull() ?: 0.0
            totalUnmatchGwt += it.unmatchGrossWt?.toDoubleOrNull() ?: 0.0
        }

        // Totals row
        summaryTable.addCell(Cell(1, 3).add(Paragraph("TOTAL").setBold().setFontSize(8f)))
        summaryTable.addCell(
            Paragraph(totalQty.toString()).setFontSize(8f).setTextAlignment(TextAlignment.CENTER)
        )
        summaryTable.addCell(
            Paragraph(totalMatch.toString()).setFontSize(8f).setTextAlignment(TextAlignment.CENTER)
        )
        summaryTable.addCell(
            Paragraph(totalUnmatch.toString()).setFontSize(8f)
                .setTextAlignment(TextAlignment.CENTER)
        )
        summaryTable.addCell(
            Paragraph("%.3f".format(totalGwt)).setFontSize(8f)
                .setTextAlignment(TextAlignment.CENTER)
        )
        summaryTable.addCell(
            Paragraph("%.3f".format(totalMatchGwt)).setFontSize(8f)
                .setTextAlignment(TextAlignment.CENTER)
        )
        summaryTable.addCell(
            Paragraph("%.3f".format(totalUnmatchGwt)).setFontSize(8f)
                .setTextAlignment(TextAlignment.CENTER)
        )

        doc.add(summaryTable)
        doc.add(Paragraph("\n"))

        // ----------------------------
        // 2) Unmatched Items
        // ----------------------------
        addHeader("Unmatched Items")
        val unmatchTable = Table(
            floatArrayOf(55f, 55f, 60f, 50f, 70f, 60f, 40f, 55f, 55f, 55f, 55f, 70f)
        ).useAllAvailableWidth()

        listOf(
            "Counter Name", "Category", "Product", "Purity", "Barcode No", "Item Code",
            "Pieces", "Gross Wt", "Stone Wt", "Net Wt", "MRP", "Status"
        ).forEach { col ->
            unmatchTable.addHeaderCell(
                Cell().add(
                    Paragraph(col).setBold().setFontSize(8f).setTextAlignment(TextAlignment.CENTER)
                )
            )
        }

        var unmatchedTotalPieces = 0
        var unmatchedTotalGross = 0.0
        var unmatchedTotalStone = 0.0
        var unmatchedTotalNet = 0.0

        unmatchedItems.forEach { d ->
            unmatchTable.addCell(
                Paragraph(d.counterName ?: "-").setFontSize(8f).setTextAlignment(TextAlignment.LEFT)
            )
            unmatchTable.addCell(
                Paragraph(d.category ?: "-").setFontSize(8f).setTextAlignment(TextAlignment.LEFT)
            )
            unmatchTable.addCell(
                Paragraph(d.product ?: "-").setFontSize(8f).setTextAlignment(TextAlignment.LEFT)
            )
            unmatchTable.addCell(
                Paragraph(d.purity ?: "-").setFontSize(8f).setTextAlignment(TextAlignment.CENTER)
            )
            unmatchTable.addCell(
                Paragraph(d.barcodeNumber ?: "-").setFontSize(8f)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            unmatchTable.addCell(
                Paragraph(d.itemCode ?: "-").setFontSize(8f).setTextAlignment(TextAlignment.CENTER)
            )
            unmatchTable.addCell(
                Paragraph(d.pieces.toString()).setFontSize(8f)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            unmatchTable.addCell(
                Paragraph(d.grossWeight ?: "-").setFontSize(8f)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            unmatchTable.addCell(
                Paragraph(d.stoneWeight ?: "-").setFontSize(8f)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            unmatchTable.addCell(
                Paragraph(d.netWeight ?: "-").setFontSize(8f).setTextAlignment(TextAlignment.CENTER)
            )
            unmatchTable.addCell(
                Paragraph(d.mrp ?: "-").setFontSize(8f).setTextAlignment(TextAlignment.CENTER)
            )
            unmatchTable.addCell(
                Paragraph("Not Found").setFontSize(8f).setTextAlignment(TextAlignment.CENTER)
            )

            unmatchedTotalPieces += d.pieces
            unmatchedTotalGross += d.grossWeight?.toDoubleOrNull() ?: 0.0
            unmatchedTotalStone += d.stoneWeight?.toDoubleOrNull() ?: 0.0
            unmatchedTotalNet += d.netWeight?.toDoubleOrNull() ?: 0.0
        }

        unmatchTable.addCell(Cell(1, 6).add(Paragraph("TOTAL").setBold().setFontSize(8f)))
        unmatchTable.addCell(
            Paragraph(unmatchedTotalPieces.toString()).setFontSize(8f)
                .setTextAlignment(TextAlignment.CENTER)
        )
        unmatchTable.addCell(
            Paragraph("%.3f".format(unmatchedTotalGross)).setFontSize(8f)
                .setTextAlignment(TextAlignment.CENTER)
        )
        unmatchTable.addCell(
            Paragraph("%.3f".format(unmatchedTotalStone)).setFontSize(8f)
                .setTextAlignment(TextAlignment.CENTER)
        )
        unmatchTable.addCell(
            Paragraph("%.3f".format(unmatchedTotalNet)).setFontSize(8f)
                .setTextAlignment(TextAlignment.CENTER)
        )
        unmatchTable.addCell(Paragraph("-").setFontSize(8f).setTextAlignment(TextAlignment.CENTER))
        unmatchTable.addCell(Paragraph("-").setFontSize(8f).setTextAlignment(TextAlignment.CENTER))

        doc.add(unmatchTable)
        doc.add(Paragraph("\n"))

        // ----------------------------
        // 3) Matched Items
        // ----------------------------
        addHeader("Matched Items (Found)")
        val matchTable = Table(
            floatArrayOf(55f, 55f, 60f, 50f, 70f, 60f, 40f, 55f, 55f, 55f, 55f, 70f)
        ).useAllAvailableWidth()


        listOf(
            "Counter Name", "Category", "Product", "Purity", "Barcode No", "Item Code",
            "Pieces", "Gross Wt", "Stone Wt", "Net Wt", "MRP", "Status"
        ).forEach { col ->
            matchTable.addHeaderCell(
                Cell().add(
                    Paragraph(col).setBold().setFontSize(8f).setTextAlignment(TextAlignment.CENTER)
                )
            )
        }

        var matchedTotalPieces = 0
        var matchedTotalGross = 0.0
        var matchedTotalStone = 0.0
        var matchedTotalNet = 0.0

        matchedItems.forEach { d ->
            matchTable.addCell(
                Paragraph(d.counterName ?: "-").setFontSize(8f).setTextAlignment(TextAlignment.LEFT)
            )
            matchTable.addCell(
                Paragraph(d.category ?: "-").setFontSize(8f).setTextAlignment(TextAlignment.LEFT)
            )
            matchTable.addCell(
                Paragraph(d.product ?: "-").setFontSize(8f).setTextAlignment(TextAlignment.LEFT)
            )
            matchTable.addCell(
                Paragraph(d.purity ?: "-").setFontSize(8f).setTextAlignment(TextAlignment.CENTER)
            )
            matchTable.addCell(
                Paragraph(d.barcodeNumber ?: "-").setFontSize(8f)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            matchTable.addCell(
                Paragraph(d.itemCode ?: "-").setFontSize(8f).setTextAlignment(TextAlignment.CENTER)
            )
            matchTable.addCell(
                Paragraph(d.pieces.toString()).setFontSize(8f)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            matchTable.addCell(
                Paragraph(d.grossWeight ?: "-").setFontSize(8f)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            matchTable.addCell(
                Paragraph(d.stoneWeight ?: "-").setFontSize(8f)
                    .setTextAlignment(TextAlignment.CENTER)
            )
            matchTable.addCell(
                Paragraph(d.netWeight ?: "-").setFontSize(8f).setTextAlignment(TextAlignment.CENTER)
            )
            matchTable.addCell(
                Paragraph(d.mrp ?: "-").setFontSize(8f).setTextAlignment(TextAlignment.CENTER)
            )
            matchTable.addCell(
                Paragraph("Found").setFontSize(8f).setTextAlignment(TextAlignment.CENTER)
            )

            matchedTotalPieces += d.pieces
            matchedTotalGross += d.grossWeight?.toDoubleOrNull() ?: 0.0
            matchedTotalStone += d.stoneWeight?.toDoubleOrNull() ?: 0.0
            matchedTotalNet += d.netWeight?.toDoubleOrNull() ?: 0.0
        }

        matchTable.addCell(Cell(1, 6).add(Paragraph("TOTAL").setBold().setFontSize(8f)))
        matchTable.addCell(
            Paragraph(matchedTotalPieces.toString()).setFontSize(8f)
                .setTextAlignment(TextAlignment.CENTER)
        )
        matchTable.addCell(
            Paragraph("%.3f".format(matchedTotalGross)).setFontSize(8f)
                .setTextAlignment(TextAlignment.CENTER)
        )
        matchTable.addCell(
            Paragraph("%.3f".format(matchedTotalStone)).setFontSize(8f)
                .setTextAlignment(TextAlignment.CENTER)
        )
        matchTable.addCell(
            Paragraph("%.3f".format(matchedTotalNet)).setFontSize(8f)
                .setTextAlignment(TextAlignment.CENTER)
        )
        matchTable.addCell(Paragraph("-").setFontSize(8f).setTextAlignment(TextAlignment.CENTER))
        matchTable.addCell(Paragraph("-").setFontSize(8f).setTextAlignment(TextAlignment.CENTER))

        doc.add(matchTable)

        doc.close()
        return file
    }


    // Helper for matched/unmatched
    private fun buildDetailedTable(items: List<DetailedItem>, status: String): Table {
        val headers = listOf(
            "Counter Name", "Category", "Product", "Purity", "Barcode No", "Item Code",
            "Pieces", "Gross Wt", "Stone Wt", "Net Wt", "MRP", "Status"
        )
        val colWidths = floatArrayOf(70f, 70f, 70f, 50f, 80f, 70f, 40f, 60f, 60f, 60f, 60f, 50f)
        val table = Table(colWidths)
        headers.forEach { table.addHeaderCell(it) }

        var totalPieces = 0
        var totalGross = 0.0
        var totalStone = 0.0
        var totalNet = 0.0
        var totalMrp = 0.0

        items.forEach { d ->
            table.addCell(d.counterName ?: "-")
            table.addCell(d.category ?: "-")
            table.addCell(d.product ?: "-")
            table.addCell(d.purity ?: "-")
            table.addCell(d.barcodeNumber ?: "-")
            table.addCell(d.itemCode ?: "-")
            table.addCell(d.pieces.toString())
            table.addCell(d.grossWeight ?: "-")
            table.addCell(d.stoneWeight ?: "-")
            table.addCell(d.netWeight ?: "-")
            table.addCell(d.mrp ?: "-")
            table.addCell(status)

            totalPieces += d.pieces
            totalGross += d.grossWeight?.toDoubleOrNull() ?: 0.0
            totalStone += d.stoneWeight?.toDoubleOrNull() ?: 0.0
            totalNet += d.netWeight?.toDoubleOrNull() ?: 0.0
            totalMrp += d.mrp?.toDoubleOrNull() ?: 0.0
        }

        // totals row
        table.addCell(Cell(1, 6).add(Paragraph("TOTAL").setBold()))
        table.addCell(totalPieces.toString())
        table.addCell(String.format("%.3f", totalGross))
        table.addCell(String.format("%.3f", totalStone))
        table.addCell(String.format("%.3f", totalNet))
        table.addCell(String.format("%.2f", totalMrp))
        table.addCell("")

        return table
    }


    fun sendEmailWithReport(context: Context, file: File, email: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, "Scan Report")
            putExtra(Intent.EXTRA_TEXT, "Please find attached the scan report.")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Send email..."))
    }

    fun sendEmailDirectly(context: Context, file: File, toEmail: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val props = Properties().apply {
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.port", "587")
                }

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication("android@loyalstring.com", "Loyal@123")
                    }
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress("android@loyalstring.com"))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                    subject = "Inventory Scan Report"
                    setText("Please find attached the inventory report.")

                    val multipart = MimeMultipart()
                    val attachmentPart = MimeBodyPart()
                    attachmentPart.attachFile(file)
                    multipart.addBodyPart(attachmentPart)
                    setContent(multipart)
                }

                Transport.send(message) // ✅ runs in background thread


            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendEmailHostinger(
        sendEmail: String,
        sendPass: String,
        recipients: List<String>,
        subject: String,
        body: String,
        type: String = "text/html", // default HTML
        attachments: Map<String, String> = emptyMap() // filename -> filepath
    ) {
        Thread {
            try {
                val props = Properties().apply {
                    put("mail.smtp.host", "smtp.hostinger.com")
                    put("mail.smtp.port", "465")
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.ssl.enable", "true")
                }

                val session = Session.getInstance(props, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(
                            "android@loyalstring.com",  // full email
                            "Android@456#"       // exact Hostinger mailbox password
                        )
                    }
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(sendEmail))
                    setRecipients(
                        Message.RecipientType.TO,
                        recipients.joinToString(",") { it }
                    )
                    setSubject(subject)

                    val multipart = MimeMultipart()

                    // Email body
                    val textPart = MimeBodyPart().apply {
                        if (type.equals("text/html", ignoreCase = true)) {
                            setContent(body, "text/html; charset=utf-8")
                        } else {
                            setText(body)
                        }
                    }
                    multipart.addBodyPart(textPart)

                    // Attachments
                    attachments.forEach { (filename, filepath) ->
                        val attachmentPart = MimeBodyPart()
                        val source: DataSource = FileDataSource(filepath)
                        attachmentPart.dataHandler = DataHandler(source)
                        attachmentPart.fileName = filename
                        multipart.addBodyPart(attachmentPart)
                    }

                    setContent(multipart)
                }

                Transport.send(message)
                _emailStatus.value = "success"
                Log.d("Email", "✅ Email sent successfully to ${recipients.joinToString()}")
            } catch (e: Exception) {
                Log.d("Email", "❌ Failed: ${e.message}", e)
                _emailStatus.value = "error: ${e.message}"

            }
        }.start()
    }


    fun buildSummary(displayItems: List<BulkItem>): List<SummaryItem> {
        return displayItems.groupBy { Triple(it.counterName, it.category, it.productName) }
            .map { (key, items) ->
                val totalQty = items.size
                val matchQty = items.count {
                    it.scannedStatus.equals(
                        "Matched",
                        ignoreCase = true
                    ) || it.scannedStatus.equals("Found", ignoreCase = true)
                }
                val unmatchQty = totalQty - matchQty
                val totalGross = items.sumOf { it.grossWeight?.toDoubleOrNull() ?: 0.0 }
                val matchGross = items.filter {
                    it.scannedStatus.equals(
                        "Matched",
                        true
                    ) || it.scannedStatus.equals("Found", true)
                }
                    .sumOf { it.grossWeight?.toDoubleOrNull() ?: 0.0 }
                val unmatchGross = totalGross - matchGross

                SummaryItem(
                    counterName = key.first,
                    category = key.second,
                    product = key.third,
                    totalQty = totalQty,
                    matchQty = matchQty,
                    unmatchQty = unmatchQty,
                    totalGrossWt = "%.3f".format(totalGross),
                    matchGrossWt = "%.3f".format(matchGross),
                    unmatchGrossWt = "%.3f".format(unmatchGross)
                )
            }
    }

    fun buildDetailedLists(displayItems: List<BulkItem>): Pair<List<DetailedItem>, List<DetailedItem>> {
        val matched = displayItems.filter {
            it.scannedStatus.equals(
                "Matched",
                true
            ) || it.scannedStatus.equals("Found", true)
        }
            .map { toDetailedItem(it) }

        val unmatched = displayItems.filter {
            it.scannedStatus.equals(
                "Unmatched",
                true
            ) || it.scannedStatus.equals("Not Found", true)
        }
            .map { toDetailedItem(it) }

        return Pair(matched, unmatched)
    }

    fun toDetailedItem(item: BulkItem): DetailedItem {
        return DetailedItem(
            counterName = item.counterName,
            category = item.category,
            product = item.productName ?: item.productName,
            purity = item.purity,
            barcodeNumber = item.rfid ?: item.rfid, // adapt if different
            itemCode = item.itemCode,
            pieces = item.pcs ?: 1,
            grossWeight = item.grossWeight,
            stoneWeight = item.stoneWeight,
            netWeight = item.netWeight,
            mrp = item.mrp.toString()
        )
    }


}