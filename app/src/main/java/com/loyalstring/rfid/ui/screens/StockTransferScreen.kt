package com.loyalstring.rfid.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.navigation.Screens
import com.loyalstring.rfid.ui.utils.BackgroundGradient
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.SingleProductViewModel
import com.loyalstring.rfid.viewmodel.StockTransferViewModel
import com.loyalstring.rfid.viewmodel.UserPermissionViewModel
import kotlinx.coroutines.launch

@SuppressLint("UnrememberedMutableState")
@Composable
fun StockTransferScreen(
    onBack: () -> Unit,
    navController: NavHostController
) {
    val context = LocalContext.current
    val viewModel: StockTransferViewModel = hiltViewModel()
    val singleProductViewModel: SingleProductViewModel = hiltViewModel()
    val userPermissionViewModel: UserPermissionViewModel = hiltViewModel()

    val transferTypes by viewModel.transferTypes.collectAsState()
    val filteredItems by viewModel.filteredBulkItems.collectAsState()
    var showBottomBar by remember { mutableStateOf(false) }
    var shouldNavigateBack by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }


    LaunchedEffect(shouldNavigateBack) {
        if (shouldNavigateBack) {
            kotlinx.coroutines.delay(50)
            onBack()
        }
    }

    val selectedItems = remember { mutableStateListOf<Int>() }

    val selectAllTransferChecked by derivedStateOf {
        selectedItems.size == filteredItems.size && filteredItems.isNotEmpty()
    }

    var selectedPower by remember {
        mutableIntStateOf(UserPreferences.getInstance(context).getInt(
            UserPreferences.KEY_STOCK_TRANSFER_COUNT))
    }
    remember { mutableIntStateOf(10) }

    var selectedTransferType by remember { mutableStateOf("Transfer Type") }
    var selectedFrom by remember { mutableStateOf("From") }
    var selectedTo by remember { mutableStateOf("To") }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showFromDialog by remember { mutableStateOf(false) }
    var showToDialog by remember { mutableStateOf(false) }


    val counters by remember { derivedStateOf { singleProductViewModel.counters } }
    val branches by remember { derivedStateOf { singleProductViewModel.branches } }
    val boxes by remember { derivedStateOf { singleProductViewModel.boxes } }
    val packets by remember { derivedStateOf { singleProductViewModel.packets } }

    val categoryFilters by viewModel.categoryFilters.collectAsState()
    val productFilters by viewModel.productFilters.collectAsState()
    val designFilters by viewModel.designFilters.collectAsState()

    var selectedTab by remember { mutableStateOf<String?>(null) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var filterType by remember { mutableStateOf("") }

    val transferredItems = remember { mutableStateListOf<BulkItem>() }
    val accessibleBranches = remember { mutableStateListOf<String>() }



    val filterTabs = listOf("Category", "Product", "Design")

    val categories by viewModel.distinctCategories.collectAsState()
    val products by viewModel.distinctProducts.collectAsState()
    val designs by viewModel.distinctDesigns.collectAsState()

    LaunchedEffect(selectedFrom) {
        viewModel.extractCategoryProductDesignFromFiltered()
    }

    LaunchedEffect(selectedTransferType) {
        if (viewModel.getTransferTypeId(selectedTransferType) == 15) {
            val branches = userPermissionViewModel.getAccessibleBranches()
            accessibleBranches.clear()
            accessibleBranches.addAll(branches)
        }
    }

    val (fromType, toType) = remember(selectedTransferType) {
        selectedTransferType.split(" to ", ignoreCase = true).map { it.trim().lowercase() }.let {
            it.getOrNull(0) to it.getOrNull(1)
        }
    }

    40.dp
    100.dp
    80.dp
    80.dp
    96.dp


    val transferTypeId = viewModel.getTransferTypeId(selectedTransferType)

    val fromOptions = when {
        // 🔥 Branch to Branch special case
        transferTypeId == 15 && fromType == "branch" -> accessibleBranches.map { it }

        fromType == "counter" -> counters.map { it.CounterName }
        fromType == "box" -> boxes.map { it.BoxName }
        //fromType == "branch" -> branches.map { it.BranchName }
        fromType == "packet" -> packets.map { it.PacketName }
        else -> emptyList()
    }

    val toOptions = when {
        // 🔥 Branch to Branch special case
        transferTypeId == 15 && toType == "branch" -> accessibleBranches.map { it }

        toType == "counter" -> counters.map { it.CounterName }
        toType == "box" -> boxes.map { it.BoxName }
       // toType == "branch" -> branches.map { it.BranchName }
        toType == "packet" -> packets.map { it.PacketName }
        else -> emptyList()
    }.filter { it != selectedFrom || fromType != toType }


    val employee =
        remember { UserPreferences.getInstance(context).getEmployee(Employee::class.java) }

    LaunchedEffect(Unit) {
        employee?.clientCode?.let {
            viewModel.loadTransferTypes(ClientCodeRequest(it))
            viewModel.fetchCounterNames()
            viewModel.fetchBoxNames()
            viewModel.fetchBranchNames()

        }
    }
    LaunchedEffect(Unit) {
        employee?.clientCode?.let {
            singleProductViewModel.fetchAllStockTransferData(ClientCodeRequest(it))
        }
    }

    rememberCoroutineScope()

    Scaffold(
        topBar = {
            GradientTopBar(
                title = "Stock Transfer",
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
                selectedCount = selectedPower,
                onCountSelected = {
                    selectedPower = it

                }
            )
        },
        bottomBar = {
            if (showBottomBar) {
                BottomActionBar(navController)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(4.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                GradientDropdownButton(
                    label = "Transfer Type",
                    selectedOption = selectedTransferType,
                    onClick = { showTransferDialog = true } // 🔥 open dialog
                )

                GradientDropdownButton(
                    label = "From",
                    selectedOption = selectedFrom,
                    onClick = { showFromDialog = true } // 🔥 open dialog
                )

                GradientDropdownButton(
                    label = "To",
                    selectedOption = selectedTo,
                    onClick = { showToDialog = true } // 🔥 open dialog
                )

            }

            Spacer(modifier = Modifier.height(8.dp))

            HorizontalCategoryScroll(
                items = listOf("Category", "Product", "Design"),
                selectedItem = selectedTab,
                onItemClick = { selected ->
                    selectedTab = selected
                    filterType = selected
                    showFilterDialog = true
                }
            )




            Spacer(modifier = Modifier.height(8.dp))


            // ---------------- Transfer SECTION header (filteredItems) ----------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.DarkGray)
                    .height(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Sr",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontFamily = poppins,
                    modifier = Modifier.width(40.dp),
                    textAlign = TextAlign.Center
                )

                Text(
                    "Product Name",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontFamily = poppins,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                Text(
                    "Label",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontFamily = poppins,
                    modifier = Modifier.width(80.dp),
                    textAlign = TextAlign.Center
                )

                Text(
                    "Gross WT",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontFamily = poppins,
                    modifier = Modifier.width(80.dp),
                    textAlign = TextAlign.Center
                )

                Text(
                    "Net WT",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontFamily = poppins,
                    modifier = Modifier.width(80.dp),
                    textAlign = TextAlign.Center
                )

                Row(
                    modifier = Modifier.width(90.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Checkbox(
                        checked = selectAllTransferChecked,
                        onCheckedChange = { checked ->
                            selectedItems.clear()
                            if (checked) selectedItems.addAll(filteredItems.indices)
                        }
                    )
                    Text("Transfer", color = Color.White, fontSize = 10.sp, fontFamily = poppins)
                }
            }

            //---------------- Transfer SECTION rows (filteredItems) ----------------//
            LazyColumn(modifier = Modifier.weight(1f, false)) {
                itemsIndexed(filteredItems) { index, item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${index + 1}",
                            Modifier.width(40.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 8.sp,
                            fontFamily = poppins
                        )

                        Text(
                            item.productName.orEmpty(),
                            Modifier.weight(1f),
                            textAlign = TextAlign.Start,
                            fontSize = 11.sp,
                            fontFamily = poppins,
                            maxLines = 2
                        )

                        Text(
                            item.rfid.orEmpty(),
                            Modifier.width(80.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 8.sp,
                            fontFamily = poppins
                        )

                        Text(
                            item.grossWeight.orEmpty(),
                            Modifier.width(80.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 8.sp,
                            fontFamily = poppins
                        )

                        Text(
                            item.netWeight.orEmpty(),
                            Modifier.width(80.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 8.sp,
                            fontFamily = poppins
                        )

                        Box(modifier = Modifier.width(90.dp), contentAlignment = Alignment.Center) {
                            Checkbox(
                                checked = false,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        val itemsToReturn = transferredItems.toList()

                                        // ✅ Add them back to first list
                                        viewModel.addBackToFiltered(itemsToReturn)

                                        // ✅ Clear from transferred list
                                        transferredItems.clear()
                                    }
                                }
                            )

                        }
                    }
                }
            }


            Spacer(modifier = Modifier.height(12.dp))

            val totalGrossWeight = selectedItems.sumOf {
                filteredItems.getOrNull(it)?.grossWeight?.toDoubleOrNull() ?: 0.0
            }
            val totalNetWeight = selectedItems.sumOf {
                filteredItems.getOrNull(it)?.netWeight?.toDoubleOrNull() ?: 0.0
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Qty : ${selectedItems.size}",
                    fontSize = 12.sp,
                    fontFamily = poppins,
                    color = Color(0xFF3C3C3C)
                )

                Text(
                    text = "T G.wt : %.3f".format(totalGrossWeight),
                    fontSize = 12.sp,
                    fontFamily = poppins,
                    color = Color(0xFF3C3C3C)
                )

                Text(
                    text = "T N.wt : %.3f".format(totalNetWeight),
                    fontSize = 12.sp,
                    fontFamily = poppins,
                    color = Color(0xFF3C3C3C)
                )

                GradientButton(
                    onClick = {
                        /*  scope.launch {
                              val stockIds =
                                  selectedItems.mapNotNull { filteredItems.getOrNull(it)?.id }
                              val fromId =
                                  fromType?.let { viewModel.getEntityIdByName(it, selectedFrom) }
                              val toId = toType?.let { viewModel.getEntityIdByName(it, selectedTo) }
                              val transferTypeId =
                                  viewModel.transferTypes.value.find { it.TransferType == selectedTransferType }?.Id
                                      ?: 0

                              employee?.let {
                                  it.clientCode?.let { it1 ->
                                      fromId?.let { it2 ->
                                          toId?.let { it3 ->
                                              viewModel.submitStockTransfer(
                                                  it1,
                                                  stockIds,
                                                  transferTypeId,
                                                  it.employeeId.toString(),
                                                  it2,
                                                  it3
                                              ) { success ->
                                                  if (success) {
                                                      // ✅ Add stock IDs to the transferredItems list
                                                      transferredItems.addAll(selectedItems.mapNotNull {
                                                          filteredItems.getOrNull(
                                                              it
                                                          )
                                                      })
                                                      Toast.makeText(
                                                          context,
                                                          "Transfer Successful",
                                                          Toast.LENGTH_SHORT
                                                      ).show()
                                                  } else {
                                                      Toast.makeText(
                                                          context,
                                                          "Transfer Failed",
                                                          Toast.LENGTH_SHORT
                                                      ).show()
                                                  }
                                              }

                                          }
                                      }
                                  }
                              }
                          }*/
                        showBottomBar = true

                        // ✅ Collect the selected items
                        val itemsToTransfer = selectedItems.mapNotNull { filteredItems.getOrNull(it) }

                        // ✅ Add them to transferred list
                        transferredItems.addAll(itemsToTransfer)

                        // ✅ Remove them from first list (filteredItems)
                        viewModel.removeTransferredItems(itemsToTransfer)

                        // ✅ Clear selected indexes
                        selectedItems.clear()
                    },
                    icon = painterResource(id = R.drawable.stock_transfer_svg),
                    text = "",
                    modifier = Modifier.size(48.dp)
                )
            }



            Spacer(modifier = Modifier.height(4.dp))

// Header
            // ---------------- Remove Header ----------------
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Color.DarkGray)
                    .height(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Sr", "Product Name", "Label", "Gross WT", "Net WT").forEach {
                    Text(
                        it,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = poppins,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        lineHeight = 12.sp
                    )
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = false, // don’t track state, just act as trigger
                            onCheckedChange = { checked ->
                                if (checked) {
                                    // 🔥 Move ALL back to transfer list
                                    transferredItems.toList()
                                    transferredItems.clear()
                                    // Put them back in upper list (filteredItems or via ViewModel)
                                    // Example:
                                    // filteredItems.addAll(removedAll)
                                }
                            }
                        )
                        Text(
                            "Remove",
                            color = Color.White,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }

// ---------------- Remove Rows ----------------
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 250.dp)
            ) {
                itemsIndexed(transferredItems) { index, item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${index + 1}",
                            Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 8.sp,
                            fontFamily = poppins
                        )
                        Text(
                            item.productName.orEmpty(),
                            Modifier.weight(1f),
                            textAlign = TextAlign.Start,
                            fontSize = 11.sp,
                            fontFamily = poppins,
                            maxLines = 2
                        )
                        Text(
                            item.rfid.orEmpty(),
                            Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            fontFamily = poppins
                        )
                        Text(
                            item.grossWeight.orEmpty(),
                            Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            fontFamily = poppins
                        )
                        Text(
                            item.netWeight.orEmpty(),
                            Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            fontFamily = poppins
                        )

                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Checkbox(
                                checked = false,
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        val removedItem = transferredItems.removeAt(index)
                                        viewModel.addBackToFiltered(listOf(removedItem))
                                    }
                                },
                                modifier = Modifier.size(22.dp)
                            )

                        }
                    }
                }
            }

// Total Row
            Spacer(modifier = Modifier.height(8.dp))

            val transferredGross = transferredItems.sumOf {
                it.grossWeight?.toDoubleOrNull() ?: 0.0
            }
            val transferredNet = transferredItems.sumOf {
                it.netWeight?.toDoubleOrNull() ?: 0.0
            }
// 📦 Summary Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Qty : ${selectedItems.size}",
                        fontSize = 14.sp,
                        fontFamily = poppins,
                        color = Color(0xFF3C3C3C)
                    )
                    Text(
                        text = "T G.wt : %.3f".format(totalGrossWeight),
                        fontSize = 14.sp,
                        fontFamily = poppins,
                        color = Color(0xFF3C3C3C)
                    )
                    Text(
                        text = "T N.wt : %.3f".format(totalNetWeight),
                        fontSize = 14.sp,
                        fontFamily = poppins,
                        color = Color(0xFF3C3C3C)
                    )

                    GradientButton(
                            onClick = {
                                showDialog = true
                            },
                            icon = painterResource(id = R.drawable.list_svg),
                            text = "",
                        modifier = Modifier.weight(1f).size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    GradientButton(
                            onClick = {
                                viewModel.filterBulkItemsByFrom(viewModel.currentFrom.value, selectedFrom)
                            },
                            icon = painterResource(id = R.drawable.ic_reset),
                            text = "",
                            modifier = Modifier.weight(1f).size(20.dp)
                     )
                    }


                Spacer(modifier = Modifier.height(12.dp))

                //  Full-width buttons

            }
            if (showDialog) {
                TransferDetailsDialog(
                    transferredBy = employee?.firstName +" "+ employee?.lastName,
                    employees = listOf("John", "Alice", "Mike"),
                    onDismiss = { showDialog = false },
                    onConfirm = { transferredBy, transferredTo, remark ->
                        // handle confirm logic
                    }
                )
            }


            if (showTransferDialog) {
                TransferTypeDialog(
                    transferTypes = transferTypes.map { it.TransferType },
                    onSelect = { selected ->
                        selectedTransferType = selected
                        viewModel.onTransferTypeSelected(selected)
                        viewModel.extractFromAndToOptions(selected)
                        selectedFrom = "From"
                        selectedTo = "To"
                        selectedItems.clear()
                    },
                    onDismiss = { showTransferDialog = false }
                )
            }
            if (showFromDialog) {
                SelectionDialog(
                    title = "Choose From",
                    options = fromOptions,
                    onSelect = {
                        selectedFrom = it
                        viewModel.filterBulkItemsByFrom(viewModel.currentFrom.value, it)
                        viewModel.extractCategoryProductDesignFromFiltered() // 🔥 new line
                        selectedItems.clear()
                    },
                    onDismiss = { showFromDialog = false }
                )
            }

            if (showToDialog) {
                SelectionDialog(
                    title = "Choose To",
                    options = toOptions,
                    onSelect = { selectedTo = it },
                    onDismiss = { showToDialog = false }
                )
            }

            if (showFilterDialog) {
                val options = when (filterType) {
                    "Category" -> categoryFilters
                    "Product" -> productFilters
                    "Design" -> designFilters
                    else -> emptyList()
                }

                FilterSelectionDialogST(
                    title = "Select $filterType",
                    options = options,
                    onSelect = { selectedValue ->
                        when (filterType) {
                            "Category" -> {
                                viewModel.filterItemsByCategory(selectedValue)
                            }
                            "Product" -> {
                                viewModel.filterItemsByProduct(selectedValue)
                            }
                            "Design" -> {
                                viewModel.filterItemsByDesign(selectedValue)
                            }
                        }
                        selectedTab = filterType
                        selectedItems.clear()
                        showFilterDialog = false
                    },
                    onDismiss = { showFilterDialog = false }
                )

            }





        }
    }
}

@Composable
fun FilterSelectionDialogST(
    title: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 12.sp, color = Color.Black, fontFamily = poppins)
                IconButton(onClick = onDismiss) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "Close",
                        tint = Color.Gray
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn {
                items(options) { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(option, color = Color.Black, fontSize = 9.sp)
                        Icon(
                            painter = painterResource(R.drawable.ic_add_custom),
                            contentDescription = null,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}


@Composable
fun TransferDetailsDialog(
    transferredBy: String = "",
    employees: List<String> = listOf("Emp1", "Emp2", "Emp3"),
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var selectedTransferredBy by remember { mutableStateOf(transferredBy) }
    var selectedTransferredTo by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            // Gradient header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                       BackgroundGradient,
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Transfer Details",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = poppins
                )
            }

            Spacer(Modifier.height(16.dp))

            // Transferred by
            DropdownField(
                label = "Transferred by",
                value = selectedTransferredBy,
                options = listOf(""),
                onValueChange = { selectedTransferredBy = it }
            )

            Spacer(Modifier.height(12.dp))

            // Transferred to
            DropdownField(
                label = "Transferred to",
                value = if (selectedTransferredTo.isEmpty()) "Select Emp" else selectedTransferredTo,
                options = employees,
                onValueChange = { selectedTransferredTo = it }
            )

            Spacer(Modifier.height(12.dp))

            // Remark
            Text("Remark", fontSize = 14.sp, fontFamily = poppins, color = Color.Black)
            androidx.compose.material3.OutlinedTextField(
                value = remark,
                onValueChange = { remark = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                placeholder = { Text("Remark.....") }
            )

            Spacer(Modifier.height(16.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                GradientButton(
                    onClick = onDismiss,
                    text = "Cancel",
                    icon = painterResource(id = R.drawable.ic_close), // your cancel icon
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                GradientButton(
                    onClick = {
                        onConfirm(selectedTransferredBy, selectedTransferredTo, remark)
                        onDismiss()
                    },
                    text = "Ok",
                    icon = painterResource(id = R.drawable.ic_matched), // your ok icon
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
fun DropdownField(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        Text(label, fontSize = 14.sp, fontFamily = poppins, color = Color.Black)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(12.dp)
        ) {
            Text(value, fontSize = 14.sp, fontFamily = poppins, color = Color.DarkGray)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}


@Composable
fun TransferTypeDialog(
    transferTypes: List<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
        ) {
            // Gradient header with title + close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        BackgroundGradient,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Choose a Transfer Type",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = poppins
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close), // ❌ you can swap to Close icon
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            // List of transfer types
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                items(transferTypes) { type ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                            .clickable {
                                onSelect(type)
                                onDismiss()
                            }
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type,
                            fontSize = 14.sp,
                            fontFamily = poppins,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun SelectionDialog(
    title: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
        ) {
            // Gradient header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        BackgroundGradient,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontFamily = poppins
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close), // replace with Close if imported
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            // Options list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                items(options) { option ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                            .clickable {
                                onSelect(option)
                                onDismiss()
                            }
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option,
                            fontSize = 14.sp,
                            fontFamily = poppins,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun BottomActionBar(navController: NavHostController) {
    var showStockIn by remember { mutableStateOf(false) }
    var showStockOut by remember { mutableStateOf(false) }

    when {
       /* showStockIn -> StockInScreen(  transferTypes = listOf("Internal", "External"),
            selectedTransferType = "Internal",
            onTransferTypeSelected = {},
            stockTransfers = listOf(),
            onApprove = {},
            onReject = {},
            onLost = {},
            onBack = { showStockIn = false }) */// ✅ works fine)
     ///   showStockOut -> StockOutScreen(onBack = { showStockOut = false })
        else -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActionButton(
                    text = "Transfer",
                    icon = painterResource(R.drawable.stock_transfer_svg),
                    onClick = {  }
                )
                ActionButton(
                    text = "InRequest",
                    icon = painterResource(R.drawable.ic_in_request),
                    onClick = { navController.navigate(Screens.StockInScreen.route)} // ✅ open StockInScreen
                )
                ActionButton(
                    text = "OutRequest",
                    icon = painterResource(R.drawable.ic_out_request),
                    onClick = { navController.navigate(Screens.StockOutScreen.route)} // ✅ open StockOutScreen
                )


            }
        }
    }
}


@Composable
fun ActionButton(text: String, icon: Painter,  onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3C3C3C)),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            painter = icon,
            contentDescription = text,
            tint = Color.White,
            modifier = Modifier
                .size(18.dp)
                .padding(end = 6.dp)
        )
        Text(
            text,
            color = Color.White,
            fontSize = 14.sp,
            maxLines = 1
        )
    }
}


@Composable
fun GradientDropdownButton(
    label: String,
    selectedOption: String,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.padding(horizontal = 4.dp)) {
        Box(
            modifier = Modifier
                .width(100.dp) // 🔥 Fixed width (adjust as needed)
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF2F1EFA), Color(0xFFE5203F))
                    ),
                    shape = RoundedCornerShape(10)
                )
                .clickable { onClick() }
                .padding(horizontal = 4.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center // keep text centered
        ) {
            Text(
                text = selectedOption,
                color = Color.DarkGray,
                fontSize = 10.sp,
                fontFamily = poppins,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis // handle long text
            )
        }
    }
}


@Composable
fun HorizontalCategoryScroll(
    items: List<String>,
    selectedItem: String?,
    onItemClick: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        IconButton(onClick = {
            scope.launch {
                val first = listState.firstVisibleItemIndex
                listState.animateScrollToItem((first - 1).coerceAtLeast(0))
            }
        }) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Scroll Left")
        }

        LazyRow(
            state = listState,
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(items) { item ->
                val isSelected = item == selectedItem
                Button(
                    onClick = { onItemClick(item) },
                    shape = RoundedCornerShape(10),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) Color(0xFF3B363E) else Color(0xFFEAEAEA),
                        contentColor = if (isSelected) Color.White else Color.Black
                    )
                ) {
                    Text(item, fontSize = 12.sp, fontFamily = poppins)
                }
            }
        }

        IconButton(onClick = {
            scope.launch {
                val next = listState.firstVisibleItemIndex + 1
                if (next < items.size) listState.animateScrollToItem(next)
            }
        }) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Scroll Right")
        }
    }
}
@Composable
fun GradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: Painter? = null,
    text: String,
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .background(BackgroundGradient, shape = RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            elevation = null,
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (icon != null) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(30.dp)
                        .padding(end = 12.dp)
                )
            }
            Text(text, color = Color.White, fontSize = 14.sp, fontFamily = poppins)
        }
    }
}
