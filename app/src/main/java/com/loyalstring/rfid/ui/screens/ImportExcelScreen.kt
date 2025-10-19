package com.loyalstring.rfid.ui.screens

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.loyalstring.rfid.R
import com.loyalstring.rfid.navigation.Screens
import com.loyalstring.rfid.ui.utils.BackgroundGradient
import com.loyalstring.rfid.ui.utils.FilePickerDialog
import com.loyalstring.rfid.ui.utils.MappingDialogWrapper
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.ImportExcelViewModel

@Composable
fun ImportExcelScreen(
    onBack: () -> Unit,
    navController: NavHostController
) {
    val viewModel: ImportExcelViewModel = hiltViewModel()
    val context: Context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    rememberCoroutineScope()

    var excelColumns by remember { mutableStateOf(listOf<String>()) }
    var showMappingDialog by remember { mutableStateOf(false) }
    var showFilePicker by remember { mutableStateOf(true) }
    var fileSelected by remember { mutableStateOf(false) }
    var showProgress by remember { mutableStateOf(false) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var showOverlay by remember { mutableStateOf(false) }

    val isImportDone by viewModel.isImportDone.collectAsState()
    val importProgress by viewModel.importProgress.collectAsState()
    val isDone by viewModel.isImportDone.collectAsState()
    var dialogMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val bulkItemFieldNames = listOf(
        "itemCode",
        "rfid",
        "grossWeight",
        "stoneWeight",
        "diamondWeight",
        "netWeight",
        "counterName",
        "category",
        "productName",
        "branchName",
        "design",
        "purity",
        "makingPerGram",
        "makingPercent",
        "fixMaking",
        "fixWastage",
        "stoneAmount",
        "diamondAmount",
        "sku",
        "vendor",
        "boxName"
    )


    LaunchedEffect(Unit) {
        viewModel.syncRFIDDataIfNeeded(context)
    }

    LaunchedEffect(isImportDone) {
        if (isImportDone) {
            showOverlay = false
            showProgress = false

            if ((importProgress.failedFields.isEmpty()) && (importProgress.importedFields !=0)) {
                dialogMessage = "✅ Import successful: ${importProgress.importedFields} fields"
                isError = false
            } else {
                dialogMessage = "⚠️ Imported with errors: ${importProgress.failedFields.joinToString()}"
                isError = true
            }
          /*  navController.navigate(Screens.ProductManagementScreen.route) {
                popUpTo(Screens.ImportExcelScreen.route) { inclusive = true }
            }*/
        }
    }


    /* LaunchedEffect(isImportDone) {
         if (isImportDone) {
             showOverlay = false
             showProgress = false
             val message = if (importProgress.failedFields.isEmpty()) {
                 //showSuccessDialog = true
                 "✅ Import successful: ${importProgress.importedFields} fields"
             } else {
               //  showSuccessDialog = true
                 "⚠️ Imported with errors: ${importProgress.failedFields.joinToString()}"
             }
             scope.launch {
                 //snackbarHostState.showSnackbar(message)
                 showSuccessDialog = true
             }
             navController.navigate(Screens.ProductManagementScreen.route) {
                 popUpTo(Screens.ImportExcelScreen.route) { inclusive = true }
             }
         }
     }*/
    if (dialogMessage != null) {
        ImportResultDialog(
            message = dialogMessage!!,
            isError = isError,
            navController = navController,
            onDismiss = { dialogMessage = null }
        )
    }

    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                val headers = viewModel.parseExcelHeaders(context, it)
                excelColumns = headers
                showMappingDialog = true
                showProgress = true
                selectedUri = it
                viewModel.setSelectedFile(it)
            }
        }



    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (showFilePicker) {
                FilePickerDialog(
                    onDismiss = {
                        showFilePicker = false
                        navController.navigate(Screens.ProductManagementScreen.route)
                    },
                    onFileSelected = {
                        showFilePicker = false
                        fileSelected = true
                        launcher.launch(
                            arrayOf(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "application/vnd.ms-excel"
                            )
                        )
                    },
                    onConfirm = {}
                )
            }

            if (showMappingDialog) {
                showProgress = false
                MappingDialogWrapper(
                    excelColumns = excelColumns,
                    bulkItemFields = bulkItemFieldNames,
                    onDismiss = {
                        showMappingDialog = false
                        navController.navigate(Screens.ProductManagementScreen.route)
                    },
                    fileSelected = fileSelected,
                    onImport = { mapping ->
                        selectedUri?.let {
                            showOverlay = true
                            viewModel.importMappedData(context, mapping)


                            showMappingDialog = false
                        }
                    },
                    isFromSheet = false
                )
            }

            if (showOverlay) {
                ExcelImportProgressOverlay(importProgress = importProgress)
            }

            // ✅ This block is your actual import progress visualization
            if (!showMappingDialog && !showFilePicker && showProgress) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    if (importProgress.totalFields > 0 && !isDone) {
                        LinearProgressIndicator(
                            progress = { importProgress.importedFields.toFloat() / importProgress.totalFields },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Importing ${importProgress.importedFields} of ${importProgress.totalFields}...",
                            fontFamily = poppins
                        )
                    }

                    if (isDone) {
                        Text(
                            "✅ Imported ${importProgress.importedFields} items",
                            fontFamily = poppins
                        )
                        if (importProgress.failedFields.isNotEmpty()) {
                            Text(
                                "⚠️ Failed fields: ${importProgress.failedFields.joinToString()}",
                                fontFamily = poppins
                            )
                        }
                    }
                }
            }
        }

    }
}

@Composable
fun ImportResultDialog(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit,
    navController: NavHostController
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = {},
        text = {
            Box {
                // Close Button
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.Gray,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(0.dp)
                        .size(20.dp)
                        .clickable {
                            onDismiss()
                            navController.navigate(Screens.ProductManagementScreen.route) {
                                popUpTo(Screens.ImportExcelScreen.route) { inclusive = true }
                            }
                        }
                )

                // Main Content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 32.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Image(
                        painter = painterResource(
                            id = if (isError) R.drawable.sucsess else R.drawable.sucsess
                        ),
                        contentDescription = null,
                        modifier = Modifier.size(120.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = message,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isError) Color.Red else Color.Black
                    )
                    Spacer(modifier = Modifier.height(25.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BackgroundGradient)
                            .clickable {
                                onDismiss()
                                navController.navigate(Screens.ProductManagementScreen.route) {
                                    popUpTo(Screens.ImportExcelScreen.route) { inclusive = true }
                                }

                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Done",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}



@Composable
fun importSuccessDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {}, // We'll handle dismiss with icon + "Done"
        title = {},
        text = {
            Box {
                // Close (X) Button in top-right corner
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.Gray,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(0.dp)
                        .size(20.dp)
                        .clickable { onDismiss() }
                )

                // Main dialog content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(top = 32.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.sucsess),
                        contentDescription = null,
                        modifier = Modifier.size(120.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Data Sync Successfully!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(25.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF3053F0), Color(0xFFE82E5A))
                                )
                            )
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Done",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}
