package com.loyalstring.rfid.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.sparklepos.models.loginclasses.customerBill.EmployeeList
import com.google.gson.Gson
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.local.entity.OrderItem
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.model.order.ItemCodeResponse
import com.loyalstring.rfid.data.remote.resource.Resource
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.navigation.Screens
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.viewmodel.BulkViewModel
import com.loyalstring.rfid.viewmodel.OrderViewModel
import com.loyalstring.rfid.viewmodel.ProductListViewModel
import com.loyalstring.rfid.viewmodel.UiState

@SuppressLint("UnrememberedMutableState")
@Composable
fun DeliveryChalanScreen(
    onBack: () -> Unit,
    navController: NavHostController
) {

    val viewModel: BulkViewModel = hiltViewModel()
    val orderViewModel: OrderViewModel = hiltViewModel()
    val context = LocalContext.current
    var selectedPower by remember { mutableStateOf(10) }
    var isScanning by remember { mutableStateOf(false) }
    //var showSuccessDialog by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    var firstPress by remember { mutableStateOf(false) }
    var shouldNavigateBack by remember { mutableStateOf(false) }
    val employee = UserPreferences.getInstance(context).getEmployee(Employee::class.java)

    // Customer input fields
    var customerName by remember { mutableStateOf("") }
    var customerId by remember { mutableStateOf<Int?>(null) }
    var expandedCustomer by remember { mutableStateOf(false) }

    var itemCode by remember { mutableStateOf(TextFieldValue("")) }
    val isLoading by orderViewModel.isItemCodeLoading.collectAsState()
    var showDropdownItemcode by remember { mutableStateOf(false) }
    var selectedCustomer by remember { mutableStateOf<EmployeeList?>(null) }
    val productList = remember { mutableStateListOf<OrderItem>() }
    var selectedItem by remember { mutableStateOf<ItemCodeResponse?>(null) }
    val productListViewModel: ProductListViewModel = hiltViewModel()
    var showInvoiceDialog by remember { mutableStateOf(false) }
    // Sample branch/salesman lists (can come from API)
    val branchList = listOf("Main Branch", "Sub Branch", "Online Branch")
    val salesmanList = listOf("Rohit", "Priya", "Vikas")
    val selectedOrderItem = OrderItem(
        id = 0,
        branchId = "",
        branchName = "Main Branch",
        exhibition = "",
        remark = "",
        purity = "22K",
        size = "",
        length = "",
        typeOfColor = "",
        screwType = "",
        polishType = "",
        finePer = "",
        wastage = "",
        orderDate = "",
        deliverDate = "",
        productName = "",
        itemCode = "",
        rfidCode = "",
        grWt = "",
        nWt = "",
        stoneAmt = "",
        finePlusWt = "",
        itemAmt = "",
        packingWt = "",
        totalWt = "",
        stoneWt = "",
        dimondWt = "",
        sku = "",
        qty = "",
        hallmarkAmt = "",
        mrp = "",
        image = "",
        netAmt = "",
        diamondAmt = "",
        categoryId = 0,
        categoryName = "",
        productId = 0,
        productCode = "",
        skuId = 0,
        designid = 0,
        designName = "",
        purityid = 0,
        counterId = 0,
        counterName = "",
        companyId = 0,
        epc = "",
        tid = "",
        todaysRate = "",
        makingPercentage = "",
        makingFixedAmt = "",
        makingFixedWastage = "",
        makingPerGram = ""
    )



    var itemCodeList by remember { mutableStateOf<List<ItemCodeResponse>>(emptyList()) }
    LaunchedEffect(Unit) {
        orderViewModel.itemCodeResponse.collect { items ->
            itemCodeList = items   // assign collected items into your mutable state
        }
    }
   /* val filteredApiList = remember(itemCode.text, itemCodeList, isLoading) {
        derivedStateOf {
            val query = itemCode.text.trim()
            if (query.isEmpty() || itemCodeList.isEmpty() || isLoading) {
                emptyList()
            } else {
                val firstChar = query.first().toString()
                itemCodeList.filter {
                    it.ItemCode?.contains(firstChar, ignoreCase = true) == true ||
                            it.RFIDCode?.contains(firstChar, ignoreCase = true) == true
                }
            }
        }
    }*/
    val allItems by productListViewModel.productList.collectAsState(initial = emptyList())

    val filteredApiList = remember(itemCode.text, allItems, isLoading) {
        derivedStateOf {
            val query = itemCode.text.trim()
            if (query.isEmpty() || allItems.isEmpty() || isLoading) {
                emptyList()
            } else {
                allItems.filter {
                    it.itemCode?.contains(query, ignoreCase = true) == true ||
                            it.rfid?.contains(query, ignoreCase = true) == true
                }
            }
        }
    }
    var filteredBulkList by remember { mutableStateOf<List<BulkItem>>(emptyList()) }
    var isFiltering by remember { mutableStateOf(false) }


  /*  val filteredList: List<BulkItem> =
        filteredApiList.value +
                filteredBulkList.map { it.toItemCodeResponse() }

    Log.d("itemcode list", "size" + filteredList.size)*/


    LaunchedEffect(shouldNavigateBack) {
        if (shouldNavigateBack) {
            kotlinx.coroutines.delay(50)
            onBack()
        }
    }

    val customerSuggestions by orderViewModel.empListFlow.collectAsState(UiState.Loading)


    val filteredCustomers by derivedStateOf {
        when (customerSuggestions) {
            is UiState.Success<*> -> {
                val items = (customerSuggestions as UiState.Success<Any?>).data as List<EmployeeList>
                if (customerName.isBlank()) {
                    items.take(20) // show first 20 when no input
                } else {
                    items.filter {
                        val fullName = "${it.FirstName} ${it.LastName}".trim().lowercase()
                        fullName.contains(customerName.trim().lowercase())
                    }.take(20)
                }
            }
            else -> emptyList()
        }
    }

    LaunchedEffect(customerSuggestions) {
        if (customerSuggestions is UiState.Success) {

            val data = (customerSuggestions as UiState.Success<List<EmployeeList>>).data
            Log.d("CustomerList", Gson().toJson(data))


        }
    }



    val addCustomerState by orderViewModel.addEmpReposnes.observeAsState()

    LaunchedEffect(addCustomerState) {
        when (addCustomerState) {
            is Resource.Success -> {
                Toast.makeText(context, "✅ Customer added successfully!", Toast.LENGTH_SHORT).show()
            }
            is Resource.Error -> {
                Toast.makeText(context, "❌ Failed to add customer!", Toast.LENGTH_SHORT).show()
            }
            else -> { /* Loading or Idle */ }
        }
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = "Delivery Chalan",
                navigationIcon = {
                    IconButton(
                        onClick = { shouldNavigateBack = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {

                },
                showCounter = true,
                selectedCount = selectedPower,
                onCountSelected = {
                    selectedPower = it
                }
            )
        },
        bottomBar = {


            ScanBottomBar(
                onSave = {

                },
                onList = { navController.navigate(Screens.ProductListScreen.route) },
                onScan = {
                    viewModel.startSingleScan(20)
                },
                onGscan = {
                    if (isScanning) {
                        viewModel.stopScanning()
                        isScanning = false
                    } else {
                        viewModel.startScanning(selectedPower)
                        isScanning = true
                    }

                    // viewModel.toggleScanning(selectedPower)

                },
                onReset = {
                    firstPress = false
                    viewModel.resetProductScanResults()
                    viewModel.stopBarcodeScanner()
                },
                isScanning = isScanning,
                isEditMode = isEditMode

            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            val coroutineScope = rememberCoroutineScope()

            CustomerNameInputData(
                customerName = customerName,
                onCustomerNameChange = { customerName = it },
                onClear = { customerName = "" },
                onAddCustomerClick = { /* open popup handled internally */ },
                filteredCustomers = filteredCustomers,
                isLoading = false,
                onCustomerSelected = {
                    customerName = "${it.FirstName.orEmpty()} ${it.LastName.orEmpty()}".trim()
                    customerId = it.Id ?: 0
                },
                coroutineScope = coroutineScope,
                fetchSuggestions = { orderViewModel.getAllEmpList(clientCode = employee?.clientCode.toString()) },
                expanded = false,
                onSaveCustomer = { request -> orderViewModel.addEmployee(request) },
                employeeClientCode = employee?.clientCode,
                employeeId = employee?.employeeId?.toString()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 🔹 Left side → Enter RFID / Itemcode box
                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .height(35.dp) // ✅ Adjusted height to align with button
                ) {
                    ItemCodeInputRowData(
                        itemCode = itemCode,
                        onItemCodeChange = { itemCode = it },
                        showDropdown = showDropdownItemcode,
                        setShowDropdown = { showDropdownItemcode = it },
                        context = context,
                        onScanClicked = { /* scanner logic */ },
                        onClearClicked = { itemCode = TextFieldValue("") },
                        filteredList = allItems,
                        isLoading = isLoading,
                        onItemSelected = { selectedItem = it }
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 🔹 Right side → Invoice Fields button
                Box(
                    modifier = Modifier
                        .weight(0.8f)
                        .height(35.dp) // ✅ same height as RFID box
                        .gradientBorderBox()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            showInvoiceDialog = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Invoice Fields",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.filter_gary),
                            contentDescription = "Add",
                            modifier = Modifier.size(20.dp),
                            tint = Color.Gray,
                        )
                    }
                }
            }

        }
        }
    // 🔹 Show the dialog when state = true
    if (showInvoiceDialog) {
     /*   InvoiceFieldsDialog(
            onDismiss = { showInvoiceDialog = false },
            onConfirm = {
                // ✅ Handle confirm logic here (save or apply data)
                showInvoiceDialog = false
            },
            branchList = branchList,
            salesmanList = salesmanList
        )*/

        InvoiceDetailsDialogEditAndDisplay(
            selectedItem = selectedOrderItem,
            branchList = branchList,
            salesmanList = salesmanList,
            onDismiss = { showInvoiceDialog = false },
            onSave = { updatedItem ->
                println("✅ Saved Invoice: ${updatedItem.branchName}")
                showInvoiceDialog = false
            }
        )
    }
    }


fun BulkItem.toItemCodeResponse(): ItemCodeResponse {
    return ItemCodeResponse(
        Id = this.id ?: 0,
        ProductTitle = this.productName.orEmpty(),
        ItemCode = this.itemCode.orEmpty(),
        RFIDCode = this.rfid.orEmpty(),
        GrossWt = this.grossWeight.orEmpty(),
        NetWt = this.netWeight.orEmpty(),
        TotalStoneWeight = this.stoneWeight.orEmpty(),
        TotalDiamondWeight = this.diamondWeight.orEmpty(),
        CategoryName = this.category.orEmpty(),
        DesignName = this.design.orEmpty(),
        PurityName = this.purity.orEmpty(),
        MakingPerGram = this.makingPerGram.orEmpty(),
        MakingPercentage = this.makingPercent.orEmpty(),
        MakingFixedAmt = this.fixMaking.orEmpty(),
        MakingFixedWastage = this.fixWastage.orEmpty(),
        TotalStoneAmount = this.stoneAmount.orEmpty(),
        TotalDiamondAmount = this.diamondAmount.orEmpty(),
        SKU = this.sku.orEmpty(),
        TIDNumber = this.tid.orEmpty(),
        BoxId = this.boxId ?: 0,
        BoxName = this.boxName.orEmpty(),
        BranchId = this.branchId ?: 0,
        BranchName = this.branchName.orEmpty(),
        PacketId = this.packetId ?: 0,
        PacketName = this.packetName.orEmpty(),
        VendorName = this.vendor.orEmpty(),
        Images = this.imageUrl.orEmpty(),
        Pieces = this.pcs?.toString().orEmpty(),
        TotalWeight = this.totalGwt ?: 0.0,
        MRP = this.mrp?.toString().orEmpty(),
        CounterId = this.counterId ?: 0,
        Stones = emptyList(),
        Diamonds = emptyList(),
        ProductName = this.productName.orEmpty()
    )

}

