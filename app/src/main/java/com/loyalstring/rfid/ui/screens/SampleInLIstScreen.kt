package com.loyalstring.rfid.ui.screens
import android.content.Intent

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.model.sampleIn.SampleInResponse
import com.loyalstring.rfid.data.model.sampleOut.SampleOutListResponse
import com.loyalstring.rfid.data.model.sampleOut.SampleOutPrintData
import com.loyalstring.rfid.data.model.sampleOut.SampleOutPrintItem
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.SampleInViewModel
import com.loyalstring.rfid.worker.LocaleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SampleInListScreen(
    onBack: () -> Unit,
    navController: NavHostController,
) {

    val viewModel: SampleInViewModel = hiltViewModel()
    val context = LocalContext.current
    val employee =
        remember { UserPreferences.getInstance(context).getEmployee(Employee::class.java) }
    val userPreferences = UserPreferences.getInstance(context)
    val savedLang = userPreferences.getAppLanguage().ifBlank { "en" }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = currentLocales[0]?.language ?: savedLang
    val localizedContext = LocaleHelper.applyLocale(context, currentLang)
    val challanList by viewModel.sampleInList.collectAsState()
    val isLoading by viewModel.loading.collectAsState()
    val error by viewModel.error.collectAsState()

    var visibleItems by remember { mutableStateOf(10) }
    var searchQuery by remember { mutableStateOf("") }

    // Fetch once
    LaunchedEffect(Unit) {
        employee?.let {
            viewModel.loadSampleIn(it.clientCode ?: "", "SampleIn")
        }
    }

    val filteredData = if (searchQuery.isNotEmpty()) {
        challanList.filter {
            it.sampleOutNo.orEmpty().contains(searchQuery, true) ||
                    it.customer?.FirstName.orEmpty().contains(searchQuery, true)
        }
    } else challanList

    val visibleData = filteredData
        .sortedByDescending { it.sampleOutNo }
        .take(visibleItems)

    // ✅ Localized column headers
    val headerTitles = listOf(
        localizedContext.getString(R.string.header_s_no),
        localizedContext.getString(R.string.header_so_no),
        localizedContext.getString(R.string.header_customer_name),
        localizedContext.getString(R.string.header_date),
        localizedContext.getString(R.string.header_return_date),
        localizedContext.getString(R.string.header_description),
        localizedContext.getString(R.string.header_product_name),
        localizedContext.getString(R.string.header_total_weight),
        localizedContext.getString(R.string.header_gross_weight),
        localizedContext.getString(R.string.header_stone_weight),
        localizedContext.getString(R.string.header_diamond_weight),
        localizedContext.getString(R.string.header_quantity),
        localizedContext.getString(R.string.header_action)
    )

    val columnWidths = listOf(
        45.dp, 60.dp, 100.dp, 80.dp, 90.dp, 90.dp, 120.dp,
        70.dp, 70.dp, 70.dp, 70.dp, 50.dp, 90.dp
    )

    Column(modifier = Modifier.fillMaxSize()) {
        GradientTopBar(
            title = localizedContext.getString(R.string.sample_in_list_title),
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = localizedContext.getString(R.string.cd_back),
                        tint = Color.White
                    )
                }
            },
            titleTextSize = 20.sp
        )

        SampleInSearchBar(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                visibleItems = 10
            },
            localizedContext=localizedContext
        )

        SampleInTable(
            navController = navController,
            headerTitles = headerTitles,
            columnWidths = columnWidths,
            data = visibleData,
            onLoadMore = {
                if (visibleItems < filteredData.size) visibleItems += 10
            },
            isLoading = isLoading,
            context = context,
            localizedContext =localizedContext
        )

        if (error != null) {
            Text(
                text = error ?: localizedContext.getString(R.string.error_loading_list),
                color = Color.Red,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun SampleInTable(
    navController: NavHostController,
    headerTitles: List<String>,
    columnWidths: List<Dp>,
    data: List<SampleInResponse>,
    onLoadMore: () -> Unit,
    isLoading: Boolean,
    context: Context,
    localizedContext: Context
) {
    val sharedScrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize()) {

        // Header Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.DarkGray)
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(sharedScrollState)
                    .weight(1f)
            ) {
                headerTitles.dropLast(1).forEachIndexed { index, title ->
                    Text(
                        text = title,
                        modifier = Modifier
                            .width(columnWidths[index])
                            .padding(6.dp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = poppins,
                        fontSize = 12.sp
                    )
                }
            }

            // Fixed Action Header
            Box(
                modifier = Modifier
                    .width(columnWidths.last())
                    .height(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = localizedContext.getString(R.string.header_actions),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = poppins
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(data) { index, challan ->

                    // 🔹 Trigger auto load more when reaching last item
                    if (index == data.lastIndex) {
                        onLoadMore()
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Scrollable content row
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(sharedScrollState)
                        ) {
                           /* val designNames = challan.IssueItems.joinToString(", ") {
                                it.DesignName ?: ""
                            }*/





                            val values = listOf(
                                (index + 1).toString(),
                                challan.sampleOutNo ?: "",
                                challan.customer?.FirstName ?: "",
                                formatCreatedOn(challan.createdOn),
                                challan.sampleInDate ?: "",
                                challan.description ?: "",
                                challan.productName,

                                challan.totalWt ?: "0.000",
                                challan.grossWt ?: "0.000",
                                challan.stoneWeight ?: "0.000",
                                challan.diamondWeight ?: "0.000",
                                challan.quantity ?: "0"
                            )

                            values.forEachIndexed { i, rawValue ->
                                val textValue = rawValue?.toString().orEmpty()

                                val isMultiLine =
                                    headerTitles.getOrNull(i) == localizedContext.getString(R.string.header_product_name) ||
                                            headerTitles.getOrNull(i) == localizedContext.getString(R.string.header_customer_name)

                                Text(
                                    text = textValue,
                                    modifier = Modifier
                                        .width(columnWidths[i])
                                        .padding(6.dp),
                                    maxLines = if (isMultiLine) 5 else 1,
                                    style = LocalTextStyle.current.copy(
                                        color = Color.Black,
                                        fontSize = 11.sp,
                                        fontFamily = poppins,
                                        lineHeight = 14.sp
                                    )
                                )
                            }
                        }

                        // Fixed Actions
                        Row(
                            modifier = Modifier
                                .width(columnWidths.last())
                                .height(40.dp),
                            horizontalArrangement = Arrangement.spacedBy(
                                6.dp,
                                Alignment.CenterHorizontally
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Edit Button
                            IconButton(onClick = {
                                CoroutineScope(Dispatchers.Main).launch {
                                    val sampleOutNoSafe = challan.sampleOutNo ?: ""
                                    /*Log.d(
                                        "Edit",
                                        "EDIT Screen $sampleOutNoSafe challan.Id ${challan.Id}"
                                    )*/
                                  //  navController.navigate("updateSampleOutScreen/${challan.Id}/$sampleOutNoSafe")
                                }
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_edit_svg),
                                    contentDescription = localizedContext.getString(R.string.cd_edit),
                                    tint = Color(0xFF37474F),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Print Button
                            IconButton(onClick = {
                                CoroutineScope(Dispatchers.Main).launch {
                                    val sampleOutNoSafe = challan.sampleOutNo ?: ""
                                   // val data = challan.toSampleOutPrintData1(context)
                                    generateSampleInPrintPdf1(context, challan)
                                  //  Log.d("Print", "PRINT Screen $sampleOutNoSafe challan.Id ${challan.Id}")
                                }
                            }) {
                                Icon(
                                    painter = painterResource(id = R.drawable.print_svg),
                                    contentDescription = localizedContext.getString(R.string.cd_print),
                                    tint = Color(0xFF37474F),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Divider(color = Color(0xFFE0E0E0))
                }
            }
        }
    }
}

fun generateSampleInPrintPdf1(context: Context, challan: SampleInResponse) {
    try {
        val pdfDocument = android.graphics.pdf.PdfDocument()

        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
            style = android.graphics.Paint.Style.FILL
            isAntiAlias = true
        }

        val titlePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 18f
            isFakeBoldText = true
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        val linePaint = android.graphics.Paint().apply {
            color = android.graphics.Color.BLACK
            strokeWidth = 1f
            style = android.graphics.Paint.Style.STROKE
            isAntiAlias = true
        }

        val pageWidth = 595
        val pageHeight = 842

        val pageInfo = android.graphics.pdf.PdfDocument.PageInfo
            .Builder(pageWidth, pageHeight, 1)
            .create()

        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        var y = 40f

        canvas.drawText("SAMPLE IN", pageWidth / 2f, y, titlePaint)
        y += 28f

        paint.textSize = 10f
        paint.isFakeBoldText = true

        canvas.drawText("Sample In No: ${challan.sampleOutNo.orEmpty()}", 30f, y, paint)
        canvas.drawText("Date: ${formatCreatedOn(challan.createdOn)}", 400f, y, paint)
        y += 18f

        paint.isFakeBoldText = false
        canvas.drawText(
            "Customer Name: ${challan.customer?.FirstName.orEmpty()}",
            30f,
            y,
            paint
        )
        y += 18f

        canvas.drawText(
            "Return Date: ${formatCreatedOn(challan.sampleInDate)}",
            30f,
            y,
            paint
        )
        y += 18f

        canvas.drawText(
            "Description: ${challan.description.orEmpty()}",
            30f,
            y,
            paint
        )
        y += 24f

        val startX = 30f
        val rowHeight = 26f

        val colWidths = listOf(
            35f,   // Sr
            155f,  // Product
            70f,   // Gross Wt
            70f,   // Stone Wt
            80f,   // Diamond Wt
            70f,   // Net Wt
            45f    // Qty
        )

        val headers = listOf(
            "Sr",
            "Product",
            "Gross Wt",
            "Stone Wt",
            "Diamond Wt",
            "Net Wt",
            "Qty"
        )

        fun drawTextInsideCell(
            text: String,
            x: Float,
            topY: Float,
            width: Float,
            bold: Boolean = false
        ) {
            paint.color = android.graphics.Color.BLACK
            paint.style = android.graphics.Paint.Style.FILL
            paint.textSize = 8.5f
            paint.isFakeBoldText = bold

            val safeText = text.ifBlank { "-" }
            val maxChars = when {
                width >= 150f -> 26
                width >= 80f -> 12
                width >= 70f -> 10
                else -> 6
            }

            canvas.drawText(
                safeText.take(maxChars),
                x + 4f,
                topY + 17f,
                paint
            )
        }

        fun drawTableRow(values: List<String>, topY: Float, bold: Boolean = false) {
            var x = startX

            values.forEachIndexed { index, value ->
                val width = colWidths[index]

                canvas.drawRect(
                    x,
                    topY,
                    x + width,
                    topY + rowHeight,
                    linePaint
                )

                drawTextInsideCell(
                    text = value,
                    x = x,
                    topY = topY,
                    width = width,
                    bold = bold
                )

                x += width
            }
        }

        drawTableRow(headers, y, true)
        y += rowHeight

        val rowValues: List<String> = listOf(
            "1",
            challan.productName.orEmpty(),
            challan.grossWt.orEmpty().ifBlank { "0.000" },
            challan.stoneWeight.orEmpty().ifBlank { "0.000" },
            challan.diamondWeight.orEmpty().ifBlank { "0.000" },
            challan.totalWt.orEmpty().ifBlank { "0.000" },
            challan.quantity.toString().ifBlank { "0" }
        )

        drawTableRow(rowValues, y)
        y += rowHeight + 24f

        paint.textSize = 10f
        paint.isFakeBoldText = true
        paint.style = android.graphics.Paint.Style.FILL
        paint.color = android.graphics.Color.BLACK

      /*  canvas.drawText("Total Weight: ${challan.totalWt.orEmpty().ifBlank { "0.000" }}", 30f, y, paint)
        y += 18f
        canvas.drawText("Gross Weight: ${challan.grossWt.orEmpty().ifBlank { "0.000" }}", 30f, y, paint)
        y += 18f
        canvas.drawText("Stone Weight: ${challan.stoneWeight.orEmpty().ifBlank { "0.000" }}", 30f, y, paint)
        y += 18f
        canvas.drawText("Diamond Weight: ${challan.diamondWeight.orEmpty().ifBlank { "0.000" }}", 30f, y, paint)
        y += 18f
        canvas.drawText("Quantity: ${challan.quantity.toString().ifBlank { "0" }}", 30f, y, paint)*/

        paint.isFakeBoldText = false
        paint.textSize = 8f
        canvas.drawText(
            "Generated by LoyalString RFID App",
            30f,
            pageHeight - 35f,
            paint
        )

        pdfDocument.finishPage(page)

        val fileName = "SampleIn_${challan.sampleOutNo ?: System.currentTimeMillis()}.pdf"
        val file = java.io.File(context.cacheDir, fileName)

        pdfDocument.writeTo(java.io.FileOutputStream(file))
        pdfDocument.close()

        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)

    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(
            context,
            "PDF generate error: ${e.message}",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }
}

fun SampleOutListResponse.toSampleOutPrintData1(context: Context): SampleOutPrintData {
    val org = UserPreferences.getInstance(context).getOrganization()
    val companyName = org?.toString().orEmpty() // agar model me Name field hai to use karo

    val items = (this.IssueItems ?: emptyList()).map { it ->
        SampleOutPrintItem(
            itemDetails = listOfNotNull(it.CategoryName, it.ProductName, it.DesignName, it.PurityName)
                .filter { s -> s.isNotBlank() }
                .joinToString(" - "),
            grossWt = it.GrossWt ?: "0.000",
            stoneWt = it.StoneWeight ?: "0.000",
            diamondWt = it.DiamondWeight ?: "0.000",
            netWt = it.NetWt ?: "0.000",
            pieces = it.Pieces ?: "1",
            status = "Sample Out",
            //imageUrl = it.Image // agar backend me image aa raha hai
        )
    }

    return SampleOutPrintData(
        companyName = companyName,
        customerName = listOfNotNull(this.Customer?.FirstName, this.Customer?.LastName).joinToString(" ").trim(),
        addressCity = this.Customer?.CurrAddTown.orEmpty(),
        contactNo = this.Customer?.Mobile.orEmpty(),
        sampleOutNo = this.SampleOutNo.orEmpty(),
        date = formatCreatedOn(this.CreatedOn), // tumhara existing fn
        returnDate = this.ReturnDate.orEmpty(),
        items = items
    )
}

/*fun SampleOutListResponse.toSampleOutPrintData(context: Context): SampleOutPrintData {
    val org = UserPreferences.getInstance(context).getOrganization()
    val companyName = org?.toString().orEmpty() // agar model me Name field hai to use karo

    val items = (this.IssueItems ?: emptyList()).map { it ->
        SampleOutPrintItem(
            itemDetails = listOfNotNull(it.CategoryName, it.ProductName, it.DesignName, it.PurityName)
                .filter { s -> s.isNotBlank() }
                .joinToString(" - "),
            grossWt = it.GrossWt ?: "0.000",
            stoneWt = it.StoneWeight ?: "0.000",
            diamondWt = it.DiamondWeight ?: "0.000",
            netWt = it.NetWt ?: "0.000",
            pieces = it.Pieces ?: "1",
            status = "Sample Out",
            //imageUrl = it.Image // agar backend me image aa raha hai
        )
    }

    return SampleOutPrintData(
        companyName = companyName,
        customerName = listOfNotNull(this.Customer?.FirstName, this.Customer?.LastName).joinToString(" ").trim(),
        addressCity = this.Customer?.CurrAddTown.orEmpty(),
        contactNo = this.Customer?.Mobile.orEmpty(),
        sampleOutNo = this.SampleOutNo.orEmpty(),
        date = formatCreatedOn(this.CreatedOn), // tumhara existing fn
        returnDate = this.ReturnDate.orEmpty(),
        items = items
    )
}*/

@Composable
fun SampleInSearchBar(value: String, onValueChange: (String) -> Unit, localizedContext: Context) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .height(45.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF2F2F2))
            .border(1.dp, Color.Gray, RoundedCornerShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = localizedContext.getString(R.string.cd_search),
            modifier = Modifier.padding(start = 12.dp),
            tint = Color.Gray
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(color = Color.Black, fontSize = 16.sp),
                cursorBrush = SolidColor(Color.Gray),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = if (value.isNotEmpty()) 36.dp else 12.dp),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Search)
            )
            if (value.isEmpty()) {
                Text(
                    text = localizedContext.getString(R.string.search_hint_sample_out),
                    color = Color.Gray,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 4.dp)
                )
            }
            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                        .size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = localizedContext.getString(R.string.cd_clear),
                        tint = Color.Gray
                    )
                }
            }
        }
    }
}
