package com.loyalstring.rfid.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.loyalstring.rfid.ui.utils.PrinterManager
import com.loyalstring.rfid.ui.utils.resolvePrintHeader

import android.Manifest
import android.content.Context

import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanPrintData
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.remote.data.CompanyDetails
import com.loyalstring.rfid.data.remote.resource.Resource
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.viewmodel.LoginViewModel

@Composable
fun PrinterScreen(navController: NavHostController) {
    val context = LocalContext.current
    val activity = context as Activity
    val printerManager = remember { PrinterManager(context) }
    val loginViewModel: LoginViewModel = hiltViewModel()
    val employee = remember {
        UserPreferences.getInstance(context).getEmployee(Employee::class.java)
    }
    val organizationName = remember {
        UserPreferences.getInstance(context).getOrganization()
    }

    var status by remember { mutableStateOf("Not connected") }
    var companyName by remember { mutableStateOf("") }

    val printData = navController.previousBackStackEntry
        ?.savedStateHandle
        ?.get<DeliveryChallanPrintData>("printer_print_data")

    LaunchedEffect(employee?.clientCode) {
        employee?.clientCode?.takeIf { it.isNotBlank() }?.let { code ->
            loginViewModel.getCompanyDetails(ClientCodeRequest(code))
        }
    }

    val companyDetailsState by loginViewModel.companyDetailsResponse.observeAsState()
    LaunchedEffect(companyDetailsState) {
        when (val result = companyDetailsState) {
            is Resource.Success<*> -> {
                @Suppress("UNCHECKED_CAST")
                val list = result.data as? List<CompanyDetails>
                companyName = list?.firstOrNull()?.compName.orEmpty()
            }
            else -> Unit
        }
    }

    val printHeader = resolvePrintHeader(
        clientCode = employee?.clientCode,
        companyName = companyName,
        organizationName = organizationName
    )

    Column(modifier = Modifier.padding(16.dp)) {
        Button(
            onClick = {
                logBondedDevices(context)
                if (hasBluetoothPermissions(activity)) {
                    printerManager.connectBluetooth("60:6E:41:BE:B7:99") { _, msg ->
                        status = msg
                    }
                } else {
                    requestBluetoothPermissions(activity)
                    status = "Please grant Bluetooth permissions"
                }
            }
        ) {
            Text("Connect Bluetooth Printer")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (printData != null) {
                    printerManager.printDeliveryChallanCompact(printData, printHeader) { _, msg ->
                        status = msg
                    }
                } else {
                    status = "No print data found"
                }
            }
        ) {
            Text("Print Challan")
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(text = status)
    }
}

fun logBondedDevices(context: Context) {
    val bluetoothAdapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter() ?: return

    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        val granted = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.BLUETOOTH_CONNECT
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!granted) {
            Log.d("BT_DEBUG", "BLUETOOTH_CONNECT permission not granted")
            return
        }
    }

    bluetoothAdapter.bondedDevices?.forEach { device ->
        Log.d("BT_DEBUG", "name=${device.name}, address=${device.address}")
    }
}

fun hasBluetoothPermissions(activity: Activity): Boolean {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        androidx.core.content.ContextCompat.checkSelfPermission(
            activity,
            android.Manifest.permission.BLUETOOTH_CONNECT
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        androidx.core.content.ContextCompat.checkSelfPermission(
            activity,
            android.Manifest.permission.BLUETOOTH
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
                androidx.core.content.ContextCompat.checkSelfPermission(
                    activity,
                    android.Manifest.permission.BLUETOOTH_ADMIN
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED &&
                androidx.core.content.ContextCompat.checkSelfPermission(
                    activity,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}

fun requestBluetoothPermissions(activity: Activity) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        androidx.core.app.ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_SCAN
            ),
            1001
        )
    } else {
        androidx.core.app.ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                android.Manifest.permission.BLUETOOTH,
                android.Manifest.permission.BLUETOOTH_ADMIN,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ),
            1002
        )
    }
}
