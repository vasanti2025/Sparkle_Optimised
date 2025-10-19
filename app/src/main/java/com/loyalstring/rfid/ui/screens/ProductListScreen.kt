package com.loyalstring.rfid.ui.screens


import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.remote.data.ProductDeleteModelReq
import com.loyalstring.rfid.data.remote.resource.Resource
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.navigation.Screens
import com.loyalstring.rfid.ui.utils.GradientButton
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.BulkViewModel
import com.loyalstring.rfid.viewmodel.ProductListViewModel
import com.loyalstring.rfid.viewmodel.SingleProductViewModel
import java.io.File


@Composable
fun ProductListScreen(
    onBack: () -> Unit,
    navController: NavHostController
) {
    var isScanning by remember { mutableStateOf(false) }
    val viewModel: ProductListViewModel = hiltViewModel()
    val bulkViewModel: BulkViewModel = hiltViewModel()
    val singleproductViewModel: SingleProductViewModel = hiltViewModel()
    val searchQuery = remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    var selectedCount by remember { mutableStateOf(1) }
    var isGridView by remember { mutableStateOf(false) }

    var showDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<BulkItem?>(null) }
    val context = LocalContext.current
    val employee = UserPreferences.getInstance(context).getEmployee(Employee::class.java)
    var showConfirmDelete by remember { mutableStateOf(false) }
    val baseUrl = "https://rrgold.loyalstring.co.in/"
    //var deletingItemId by remember { mutableStateOf<Int?>(null) }
    var isEditMode by remember { mutableStateOf(false) }
    val deleteResponse by singleproductViewModel.productDeleetResponse.observeAsState()
    var shouldNavigateBack by remember { mutableStateOf(false) }

    LaunchedEffect(shouldNavigateBack) {
        if (shouldNavigateBack) {
            kotlinx.coroutines.delay(50)
            onBack()
        }
    }

    LaunchedEffect(deleteResponse) {
        when (deleteResponse) {
            is Resource.Success -> {

                   // singleproductViewModel.insertLabelledStock(request)
                   // singleproductViewModel.deleteItem(id) // ✅ local delete with cached id
                    Toast.makeText(context, "Item deleted successfully", Toast.LENGTH_SHORT).show()


            }
            is Resource.Error -> {
                Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show()
            }
            else -> Unit
        }
    }
/*    val deleteResult by singleproductViewModel.deleteResult.collectAsState()
    LaunchedEffect(deleteResult) {
        when {
            deleteResult == null -> Unit
            deleteResult ?: 0 > 0 -> {
                Toast.makeText(context, "Item deleted successfully", Toast.LENGTH_SHORT).show()
                showConfirmDelete = false
                selectedItem = null
            }
            else -> {
                Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show()
            }
        }
    }*/



    val allItems by viewModel.productList.collectAsState(initial = emptyList())
    val filteredItems = remember(searchQuery.value, allItems) {
        allItems.filter { item ->
            val query = searchQuery.value.trim().lowercase()
            item.itemCode!!.lowercase().contains(query) ||
                    item.productName!!.lowercase().contains(query) ||
                    item.rfid!!.lowercase().contains(query)
        }
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = "Product List",
                navigationIcon = {
                    IconButton(onClick = { shouldNavigateBack = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                showCounter = true,
                selectedCount = selectedCount,
                onCountSelected = { selectedCount = it }
            )
        },
        bottomBar = {
            ScanBottomBar(
                onSave = { /* Save logic */ },
                onList = { navController.navigate(Screens.ProductListScreen.route) },
                onScan = { /* Scan logic */ },
                onGscan = { /* Gscan logic */ },
                onReset = { /* Reset logic */ },
                isScanning = isScanning,
                isEditMode=isEditMode
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)

                .background(Color.White)
        ) {
            Spacer(Modifier.height(12.dp))

          /*  OutlinedTextField(
                value = searchQuery.value,
                onValueChange = { searchQuery.value = it },
                placeholder = { Text("Enter RFID / Item code / Product", fontFamily = poppins) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )*/
            TextField(
                value = searchQuery.value,
                onValueChange = { searchQuery.value = it },
                placeholder = { Text("Enter RFID / Item code / Product", fontFamily = poppins) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(5.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF0F0F0),
                    unfocusedContainerColor = Color(0xFFF0F0F0),
                    disabledContainerColor = Color(0xFFF0F0F0),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )


            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp) // adds spacing between buttons
            ) {
                ActionButton(
                    text = if (isGridView) "List View" else "Grid View",
                    onClick = { isGridView = !isGridView },
                    gradient = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFD32940), Color(0xFF5231A7)) // blue to cyan gradient
                    ),
                    backgroundColor = Color.Transparent,
                    icon = if (isGridView) {
                        painterResource(id = R.drawable.list_svg)   // 👈 your drawable
                    } else {
                        painterResource(id = R.drawable.grid_svg)
                    }
                )
                ActionButton(
                    text = "Filter",
                    onClick = { },
                    gradient = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFD32940), Color(0xFF5231A7)) // red to purple
                    ),
                    icon =  painterResource(id = R.drawable.filter_svg)
                )
                ActionButton(
                    text = "Export Pdf",
                    onClick = { },
                    modifier = Modifier.defaultMinSize(minWidth = 120.dp),
                    gradient = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFD32940), Color(0xFF5231A7)) // red to purple
                    ),
                    icon = painterResource(id = R.drawable.pdf)
                )

            }


            Spacer(Modifier.height(12.dp))

            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredItems) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedItem = item
                                    showDialog = true
                                }
                                .height(IntrinsicSize.Min),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.LightGray),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(4.dp) // Less vertical spacing
                            ) {
                                if (!item.imageUrl.isNullOrEmpty()) {
                                    val stored = item.imageUrl.trim()
                                        .trimEnd(',') // remove any trailing commas/spaces
                                    if (stored.startsWith("/")) {
                                        val file = File(stored)
                                        if (file.exists()) file
                                        else null
                                    } else {
                                        stored.split(",")
                                            .map { it.trim() }
                                            .filter { it.isNotEmpty() }
                                            .lastOrNull()
                                            ?.let {

                                                AsyncImage(
                                                    model = baseUrl + it,
                                                    contentDescription = item.itemCode,
                                                    modifier = Modifier
                                                        .size(72.dp)
                                                        .align(Alignment.CenterHorizontally)
                                                )
                                            }
                                    }

                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Photo,
                                        contentDescription = item.itemCode,
                                        tint = Color.Gray,
                                        modifier = Modifier
                                            .size(72.dp)
                                            .align(Alignment.CenterHorizontally)
                                    )
                                }

                                // Row: RFID & Item Code
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp) // Better spacing between the two
                                ) {
                                    Text(
                                        text = "RFID: ${item.rfid}",
                                        fontFamily = poppins,
                                        fontSize = 9.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "Item: ${item.itemCode}",
                                        fontFamily = poppins,
                                        fontSize = 9.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }


                                // Row: Gross Wt & Net Wt
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "G.Wt: ${item.grossWeight}",
                                        fontFamily = poppins,
                                        fontSize = 9.sp,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "N.Wt: ${item.netWeight}",
                                        fontFamily = poppins,
                                        fontSize = 9.sp,
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }


            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()

                        .background(Color(0xFF2E2E2E)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "S.No",
                        Modifier.width(40.dp),
                        color = Color.White,
                        textAlign = TextAlign.Start,
                        fontFamily = poppins,
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(scrollState),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(
                            "Product Name" to 120.dp,
                            "Item code" to 70.dp,
                            "RFID" to 60.dp,
                            "G.wt" to 60.dp,
                            "S.wt" to 60.dp,
                            "D.wt" to 60.dp,
                            "N.wt" to 60.dp,
                            "Category" to 70.dp,
                            "Design" to 60.dp,
                            "Purity" to 60.dp,
                            "Making/g" to 80.dp,
                            "Making%" to 80.dp,
                            "Fix Making" to 80.dp,
                            "Fix Wastage" to 80.dp,
                            "S Amt" to 60.dp,
                            "D Amt" to 60.dp,
                            "SKU" to 70.dp,
                            "EPC" to 160.dp,
                            "Vendor" to 80.dp
                           // "TID" to 90.dp
                        ).forEach { (label, width) ->
                            Text(
                                label,
                                Modifier.width(width),
                                color = Color.White,
                                textAlign = TextAlign.Start,
                                fontFamily = poppins,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                    Text(
                        "Edit",
                        Modifier.width(35.dp),
                        color = Color.White,
                        textAlign = TextAlign.Start,
                        fontFamily = poppins,
                        fontSize = 12.sp
                    )
                    Text(
                        "Delete",
                        Modifier.width(55.dp),
                        color = Color.White,
                        textAlign = TextAlign.Start,
                        fontFamily = poppins,
                        fontSize = 12.sp
                    )
                }

                LazyColumn(
                    contentPadding = PaddingValues(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(filteredItems) { index, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${index + 1}",
                                Modifier
                                    .width(40.dp)
                                    .padding(5.dp),
                                textAlign = TextAlign.Start,
                                fontFamily = poppins,
                                fontSize = 12.sp
                            )

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedItem = item
                                        showDialog = true
                                    }
                                    .horizontalScroll(scrollState),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf(
                                    item.productName to 120.dp,
                                    item.itemCode to 70.dp,
                                    item.rfid to 60.dp,
                                    item.grossWeight to 60.dp,
                                    item.stoneWeight to 60.dp,
                                    item.diamondWeight to 60.dp,
                                    item.netWeight to 60.dp,
                                    item.category to 70.dp,
                                    item.design to 60.dp,
                                    item.purity to 60.dp,
                                    item.makingPerGram to 80.dp,
                                    item.makingPercent to 80.dp,
                                    item.fixMaking to 80.dp,
                                    item.fixWastage to 80.dp,
                                    item.stoneAmount to 60.dp,
                                    item.diamondAmount to 60.dp,
                                    item.sku to 70.dp,
                                    (
                                            (item.uhfTagInfo?.epc ?: item.epc)?.takeIf {
                                                !it.contains(
                                                    "temp",
                                                    ignoreCase = true
                                                )
                                            } ?: ""
                                            ) to 160.dp,
                                    item.vendor to 80.dp
                                  //  (item.uhfTagInfo?.epc ?: item.epc) to 90.dp
                                ).forEach { (value, width) ->
                                    Text(
                                        value?.ifBlank { "-" } ?: "-",
                                        Modifier.width(width),
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Start,
                                        fontFamily = poppins,
                                        maxLines = 1
                                    )
                                }
                            }

                            IconButton(onClick = {
                                try {
                                    val currentEntry = navController.currentBackStackEntry
                                    currentEntry?.savedStateHandle?.set("item", item)
                                    navController.navigate(Screens.EditProductScreen.route)
                                } catch (e: Exception) {
                                    Log.e("NAVIGATION", "BackStackEntry error: ${e.message}")
                                }
                            }, modifier = Modifier.width(30.dp)) {
                                Icon(
                                    painter = painterResource(id = com.loyalstring.rfid.R.drawable.ic_edit_svg),
                                    contentDescription = "Edit",
                                    tint = Color.DarkGray
                                )
                            }
                            IconButton(
                                onClick = {
                                    selectedItem = item
                                    showConfirmDelete = true
                                },


                               // onClick = { /* Delete */ },
                                modifier = Modifier.width(50.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = com.loyalstring.rfid.R.drawable.ic_delete_svg),
                                    contentDescription = "Delete",
                                    tint = Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }
            if (showDialog && selectedItem != null) {
                ItemDetailsDialog(item = selectedItem!!, onDismiss = { showDialog = false })
            }
            // existing product details popup
            if (showDialog && selectedItem != null) {
                ItemDetailsDialog(item = selectedItem!!, onDismiss = { showDialog = false })
            }

// ✅ add confirmation popup here
            ConfirmDeleteDialog(
                visible = showConfirmDelete,
                productName = selectedItem?.productName,
                onConfirm = {
                    val id = selectedItem?.id ?: 0
                    val clientCode = employee?.clientCode
                    if (id > 0) {
                        //deletingItemId = id // ✅ keep id safe
                        singleproductViewModel.deleetProduct(
                            listOf(
                                ProductDeleteModelReq(
                                    Id = id,
                                    ClientCode = clientCode.toString()
                                )
                            )
                        )
                    }
                    showConfirmDelete = false
                    selectedItem = null
                },
                onDismiss = {
                    showConfirmDelete = false
                }
            )

        }
    }
}

@Composable
fun ConfirmDeleteDialog(
    visible: Boolean,
    productName: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Product Delete",
            fontSize = 18.sp,
            fontFamily = poppins,// 👈 set your desired size
            fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Are you sure you want to delete ${productName ?: "this item"}?",
                fontSize = 16.sp,
                fontFamily = poppins
            )
        },
        confirmButton = {
           // TextButton(onClick = onConfirm) { Text("Yes") }
            GradientButton(text = "Yes", onClick = onConfirm)
        },
        dismissButton = {
            GradientButton(text ="Cancel",onClick = onDismiss)
        }
    )
}



@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradient: Brush,
    cornerRadius: Dp = 8.dp,
    backgroundColor: Color = Color.Transparent,
    textColor: Color = Color.Black,
    icon: Painter
) {
    Box(
        modifier = modifier
            .border(
                width = 1.5.dp,
                brush = gradient, // 🔥 gradient stroke
                shape = RoundedCornerShape(cornerRadius)
            )
            .background(
                color = backgroundColor, // inner background (white/transparent)
                shape = RoundedCornerShape(cornerRadius)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}



@Composable
fun ItemDetailsDialog(
    item: BulkItem,
    onDismiss: () -> Unit
) {
    val baseUrl = "https://rrgold.loyalstring.co.in/"
    val imageUrl = item.imageUrl?.split(",")
        ?.lastOrNull()
        ?.trim()
        ?.let { "$baseUrl$it" }

    var scale by remember { mutableStateOf(1f) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Item Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 14.sp,
                        fontFamily = poppins
                    )
                    TextButton(onClick = onDismiss) {
                        Text("Close", fontFamily = poppins)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!imageUrl.isNullOrBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUrl),
                        contentDescription = "Zoomable Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale
                            )
                            .pointerInput(Unit) {
                                detectTransformGestures { _, _, zoom, _ ->
                                    scale = (scale * zoom).coerceIn(1f, 5f)
                                }
                            }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                InfoRow("Product Name", item.productName)
                InfoRow("Item Code", item.itemCode)
                InfoRow("RFID", item.rfid)
                InfoRow("G.Wt", item.grossWeight)
                InfoRow("S.Wt", item.stoneWeight)
                InfoRow("D.Wt", item.diamondWeight)
                InfoRow("N.Wt", item.netWeight)
                InfoRow("Category", item.category)
                InfoRow("Design", item.design)
                InfoRow("Purity", item.purity)
                InfoRow("Making/Gram", item.makingPerGram)
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String?) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            "$label:",
            modifier = Modifier.weight(1f),
            color = Color.DarkGray,
            fontSize = 12.sp,
            fontFamily = poppins
        )
        Text(value ?: "-", modifier = Modifier.weight(1.5f), fontSize = 12.sp, fontFamily = poppins)
    }
}


