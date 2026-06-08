package com.loyalstring.rfid.ui.utils

import android.content.Context
import android.util.Log
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanItemPrint
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanPrintData
import kotlinx.coroutines.launch
import net.posprinter.IConnectListener
import net.posprinter.IDeviceConnection
import net.posprinter.POSConnect
import net.posprinter.POSConst
import net.posprinter.POSPrinter
import java.text.SimpleDateFormat
import java.util.Locale

class PrinterManager(private val context: Context) {

    private var deviceConnection: IDeviceConnection? = null
    private var posPrinter: POSPrinter? = null
    private var companyName: String = ""

/*    fun connectBluetooth(macAddress: String, onResult: (Boolean, String) -> Unit) {
        deviceConnection = POSConnect.createDevice(POSConnect.DEVICE_TYPE_BLUETOOTH)

        deviceConnection?.connect(macAddress, object : IConnectListener {
            override fun onStatus(code: Int, connectInfo: String?, message: String?) {
                Log.d("PRINTER", "BT status=$code info=$connectInfo msg=$message")

                if (code == POSConnect.CONNECT_SUCCESS) {
                    posPrinter = POSPrinter(deviceConnection)
                    onResult(true, message ?: "Bluetooth printer connected")
                } else {
                    onResult(false, message ?: "Bluetooth printer connection failed")
                }
            }
        })
    }*/

    fun connectBluetooth(macAddress: String, onResult: (Boolean, String) -> Unit) {
        try {
            disconnect()
            Thread.sleep(300)
        } catch (_: Exception) { }

        deviceConnection = POSConnect.createDevice(POSConnect.DEVICE_TYPE_BLUETOOTH)

        deviceConnection?.connect(macAddress, object : IConnectListener {
            override fun onStatus(code: Int, connectInfo: String?, message: String?) {
                Log.d("PRINTER", "BT status=$code info=$connectInfo msg=$message")

                if (code == POSConnect.CONNECT_SUCCESS) {
                    posPrinter = POSPrinter(deviceConnection)
                    onResult(true, message ?: "Bluetooth printer connected")
                } else {
                    onResult(false, message ?: "Bluetooth printer connection failed")
                }
            }
        })
    }

    fun disconnect() {
        try {
            deviceConnection?.close()
        } catch (e: Exception) {
            Log.e("PRINTER", "disconnect error", e)
        }
        deviceConnection = null
        posPrinter = null
    }

    fun isConnected(): Boolean {
        return deviceConnection?.isConnect() == true
    }

    private fun safe(value: String?, fallback: String = ""): String {
        return value?.trim().orEmpty().ifEmpty { fallback }
    }

    private fun cleanWeight(value: String?): String {
        val raw = value
            ?.replace("gm", "", ignoreCase = true)
            ?.replace("g", "", ignoreCase = true)
            ?.trim()
            .orEmpty()

        val number = raw.toDoubleOrNull() ?: 0.0
        return String.format(Locale.US, "%.3f", number)
    }

    private fun formatDate(value: String?): String {
        val raw = safe(value, "-")
        return try {
            val formats = listOf(
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()),
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
                SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            )

            val parsed = formats.firstNotNullOfOrNull { fmt ->
                try { fmt.parse(raw) } catch (_: Exception) { null }
            }

            if (parsed != null) {
                SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(parsed)
            } else raw
        } catch (_: Exception) {
            raw
        }
    }

    private fun padRight(value: String, length: Int): String {
        val v = value.trim()
        return if (v.length >= length) v.take(length) else v + " ".repeat(length - v.length)
    }

    private fun padLeft(value: String, length: Int): String {
        val v = value.trim()
        return if (v.length >= length) v.take(length) else " ".repeat(length - v.length) + v
    }

    private fun fitItemName(value: String, length: Int): String {
        val clean = safe(value, "-")
        return if (clean.length > length) clean.take(length) else clean
    }

    private fun divider(): String {
        return "━".repeat(24)
    }

    /**
     * 58mm printer compact width
     * total = 29 chars
     * SNo(3) + Item(8) + PCS(4) + G.W(7) + N.W(7)
     */
    private fun itemRow(
        sno: String,
        itemName: String,
        pcs: String,
        grossWt: String,
        netWt: String
    ): String {
        return buildString {
            append(padRight(sno, 7))
            append(padRight(fitItemName(itemName, 16), 16))
            append(padLeft(pcs, 4))
            append(padLeft(grossWt, 10))
            append(padLeft(netWt, 10))
        }
    }

    /**
     * Summary row total = 29 chars
     * Item(8) + T.P(5) + T.G.W(8) + T.N.W(8)
     */
    private fun summaryRow(
        sno: String,
        itemName: String,
        totalPcs: String,
        totalGrossWt: String,
        totalNetWt: String
    ): String {
        return buildString {
            append(padRight(sno, 7))
            append(padRight(fitItemName(itemName, 16), 16))
            append(padLeft(totalPcs, 4))
            append(padLeft(totalGrossWt, 10))
            append(padLeft(totalNetWt, 10))
        }
    }

    private data class SummaryData(
        val itemName: String,
        val totalPcs: Int,
        val totalGrossWt: Double,
        val totalNetWt: Double
    )

    private fun buildSummary(items: List<DeliveryChallanItemPrint>): List<SummaryData> {
        return items
            .groupBy { safe(it.itemName, "-") }
            .map { (itemName, groupedItems) ->
                SummaryData(
                    itemName = itemName,
                    totalPcs = groupedItems.sumOf { it.pcs },
                    totalGrossWt = groupedItems.sumOf {
                        cleanWeight(it.grossWt).toDoubleOrNull() ?: 0.0
                    },
                    totalNetWt = groupedItems.sumOf {
                        cleanWeight(it.netWt).toDoubleOrNull() ?: 0.0
                    }
                )
            }
    }

    fun printDeliveryChallanCompact(
        data: DeliveryChallanPrintData,
        companyName: String,
        onResult: ((Boolean, String) -> Unit)? = null
    ) {
        val printer = posPrinter
        if (printer == null) {
            onResult?.invoke(false, "Printer not connected")
            return
        }

        val items = data.items
        if (items.isEmpty()) {
            onResult?.invoke(false, "No items available to print")
            return
        }

        val summaryList = buildSummary(items)
        val dateText = formatDate(data.createdDateTime)
        val phoneText = safe(data.phone, "-")
        val nameText = safe(data.customerName, "-")

        try {
            var chain = printer.initializePrinter()
            val companyText = safe(companyName, "Company")

            chain = chain
                .printText(
                    "$companyText\n",
                    POSConst.ALIGNMENT_CENTER,
                    POSConst.FNT_DEFAULT,
                    POSConst.TXT_2WIDTH or POSConst.TXT_2HEIGHT
                )
                .printText(
                    divider() + "\n",
                    POSConst.ALIGNMENT_LEFT,
                    POSConst.FNT_DEFAULT,
                    POSConst.TXT_1WIDTH or POSConst.TXT_1HEIGHT
                )
                .printText(
                    "Name: $nameText\n",
                    POSConst.ALIGNMENT_LEFT,
                    POSConst.TXT_1WIDTH,
                    POSConst.TXT_1HEIGHT
                )
                .printText(
                    "Phone : $phoneText\n",
                    POSConst.ALIGNMENT_LEFT,
                    POSConst.TXT_1WIDTH,
                    POSConst.TXT_1HEIGHT
                )
                .printText(
                    "Date : $dateText\n",
                    POSConst.ALIGNMENT_RIGHT,
                    POSConst.TXT_1WIDTH,
                    POSConst.TXT_1HEIGHT
                )
                .printText(
                    "Status : Order Summary\n",
                    POSConst.ALIGNMENT_RIGHT,
                    POSConst.TXT_1WIDTH,
                    POSConst.TXT_1HEIGHT
                )
                .printText(
                    divider() + "\n",
                    POSConst.ALIGNMENT_LEFT,
                    POSConst.TXT_1WIDTH,
                    POSConst.TXT_1HEIGHT
                )

            // Main header
            chain = chain
                .printText(
                    itemRow("Sr No", "Item Name", "PCS", "G.W", "N.W") + "\n",
                    POSConst.ALIGNMENT_LEFT,
                    POSConst.TXT_1WIDTH,
                    POSConst.TXT_1HEIGHT
                )
                .printText(
                    divider() + "\n",
                    POSConst.ALIGNMENT_LEFT,
                    POSConst.TXT_1WIDTH,
                    POSConst.TXT_1HEIGHT
                )

            // Main rows
            items.forEachIndexed { index, item ->
                chain = chain.printText(
                    itemRow(
                        sno = (index + 1).toString(),
                        itemName = safe(item.itemName, "-"),
                        pcs = item.pcs.toString(),
                        grossWt = cleanWeight(item.grossWt),
                        netWt = cleanWeight(item.netWt)
                    ) + "\n",
                    POSConst.ALIGNMENT_LEFT,
                    POSConst.TXT_1WIDTH,
                    POSConst.TXT_1HEIGHT
                )
            }

            chain = chain
                .printText(
                    divider() + "\n",
                    POSConst.ALIGNMENT_LEFT,
                    POSConst.TXT_1WIDTH,
                    POSConst.TXT_1HEIGHT
                )
                .feedLine()

            // Summary header
            chain = chain
                .printText(
                    summaryRow("Sr No", "Item Name", "T.P", "T.G.W", "T.N.W")+ "\n",
                    POSConst.ALIGNMENT_LEFT,
                    POSConst.TXT_1WIDTH,
                    POSConst.TXT_1HEIGHT
                )
                .printText(
                    divider() + "\n",
                    POSConst.ALIGNMENT_LEFT,
                    POSConst.TXT_1WIDTH,
                    POSConst.TXT_1HEIGHT
                )

            // Summary rows
            summaryList.forEachIndexed { index, summary ->
                chain = chain.printText(
                    summaryRow(
                        sno = (index + 1).toString(),
                        itemName = summary.itemName,
                        totalPcs = summary.totalPcs.toString(),
                        totalGrossWt = String.format(Locale.US, "%.3f", summary.totalGrossWt),
                        totalNetWt = String.format(Locale.US, "%.3f", summary.totalNetWt)
                    ) + "\n",
                    POSConst.ALIGNMENT_LEFT,
                    POSConst.TXT_1WIDTH,
                    POSConst.TXT_1HEIGHT
                )
            }

            chain = chain
                .printText(
                    divider() + "\n",
                    POSConst.ALIGNMENT_LEFT,
                    POSConst.TXT_1WIDTH,
                    POSConst.TXT_1HEIGHT
                )
                .feedLine(3)

            onResult?.invoke(true, "Printed successfully")
        } catch (e: Exception) {
            Log.e("PRINTER", "printDeliveryChallanCompact error", e)
            onResult?.invoke(false, e.message ?: "Printing failed")
        }
    }
}

/** Print header: LS000053 → Rough Estimation; else company API name or saved organization. */
fun resolvePrintHeader(
    clientCode: String?,
    companyName: String?,
    organizationName: String?
): String {
    if (clientCode.equals("LS000053", ignoreCase = true)) {
        return "Rough Estimation"
    }
    return companyName?.trim()?.takeIf { it.isNotEmpty() }
        ?: organizationName?.trim()?.takeIf { it.isNotEmpty() }
        ?: "Company"
}