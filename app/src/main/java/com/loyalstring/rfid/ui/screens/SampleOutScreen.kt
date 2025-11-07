package com.loyalstring.rfid.ui.screens

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.sparklepos.models.loginclasses.customerBill.EmployeeList
import com.google.gson.Gson
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.navigation.Screens
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.viewmodel.BulkViewModel
import com.loyalstring.rfid.viewmodel.OrderViewModel
import com.loyalstring.rfid.viewmodel.UiState

@SuppressLint("UnrememberedMutableState")
@Composable
fun SampleOutScreen(
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

    Scaffold(
        topBar = {
            GradientTopBar(
                title = "Sample Out",
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
                isEditMode = isEditMode,
                isScreen=false

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
                onCustomerSelected = { customerName = "${it.FirstName.orEmpty()} ${it.LastName.orEmpty()}".trim()
                    customerId = it.Id ?: 0},
                coroutineScope = coroutineScope,
                fetchSuggestions = {orderViewModel.getAllEmpList(clientCode = employee?.clientCode.toString()) },
                expanded = false,
                onSaveCustomer = { request -> orderViewModel.addEmployee(request) },
                employeeClientCode = employee?.clientCode,
                employeeId = employee?.employeeId?.toString()
            )

            /*  CustomerNameInputData(
                  customerName = customerName,
                  onCustomerNameChange = { customerName = it },
                  onClear = {
                      customerName = ""
                      expandedCustomer = false
                  },
                  onAddCustomerClick = {
                     *//* customerNameadd = ""
                    mobileNumber = ""
                    email = ""
                    panNumber = ""
                    gstNumber = ""
                    street = ""
                    city = ""
                    state = ""
                    country = ""
                    showAddCustomerDialog = true*//*
                },
                filteredCustomers = filteredCustomers,
                isLoading = false,
                onCustomerSelected = {
                    customerName = "${it.FirstName.orEmpty()} ${it.LastName.orEmpty()}".trim()
                    customerId = it.Id ?: 0
                    //selectedCustomer(it)
                },
                coroutineScope = coroutineScope, // ✅ Required argument
                fetchSuggestions = {
                    orderViewModel.getAllEmpList(clientCode = employee?.clientCode.toString())
                },
                expanded = false,

                )*/


        }
    }

}