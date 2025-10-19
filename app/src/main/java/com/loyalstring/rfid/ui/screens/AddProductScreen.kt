package com.loyalstring.rfid.ui.screens

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.loyalstring.rfid.MainActivity
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.addSingleItem.InsertProductRequest
import com.loyalstring.rfid.data.model.addSingleItem.SKUModel
import com.loyalstring.rfid.data.model.addSingleItem.VendorModel
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.reader.ScanKeyListener
import com.loyalstring.rfid.data.remote.resource.Resource
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.navigation.Screens
import com.loyalstring.rfid.ui.utils.GradientButton
import com.loyalstring.rfid.ui.utils.ToastUtils
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.BulkViewModel
import com.loyalstring.rfid.viewmodel.SingleProductViewModel
import kotlinx.coroutines.launch
import java.io.File


// Imports skipped for brevity — keep your existing ones

// Your data model
data class FormField(
    val label: String,
    val isDropdown: Boolean,
    val options: List<String> = emptyList(),
    val value: String = ""
)

private val sampleFields = listOf(
    FormField("EPC", false),
    FormField("Vendor", true),
    FormField("SKU", true),
    //FormField("Item Code", false),
    FormField("RFID Code", false),
    FormField("Category", true),
    FormField("Product", true),
    FormField("Design", true),
    FormField("Purity", true),
    FormField("Gross Weight", false),
    FormField("Stone Weight", false),
    FormField("Diamond Weight", false),
    FormField("Net Weight", false),
    FormField("Making/Gram", false),
    FormField("Making %", false),
    FormField("Fix Making", false),
    FormField("Fix Wastage", false),
    FormField("Stone Amount", false),
    FormField("Diamond Amount", false),
    // FormField("Image Upload", false)
)
@Composable
fun AddProductScreen(
    onBack: () -> Unit,
    navController: NavHostController,
) {

    val bulkViewModel: BulkViewModel = hiltViewModel()
    val viewModel:SingleProductViewModel = hiltViewModel()
    val context = LocalContext.current
    val employee = UserPreferences.getInstance(context).getEmployee(Employee::class.java)

    val categoryList =
        (viewModel.categoryResponse.observeAsState().value as? Resource.Success)?.data
    val productList = (viewModel.productResponse.observeAsState().value as? Resource.Success)?.data
    val designList = (viewModel.designResponse.observeAsState().value as? Resource.Success)?.data
    val purityList = (viewModel.purityResponse.observeAsState().value as? Resource.Success)?.data
    val vendorList = (viewModel.vendorResponse.observeAsState().value as? Resource.Success)?.data
    val skuList = (viewModel.skuResponse.observeAsState().value as? Resource.Success)?.data

    val vendorNames = vendorList?.mapNotNull { it.VendorName } ?: emptyList()
    val categoryNames = categoryList?.mapNotNull { it.CategoryName } ?: emptyList()

    val scanTrigger by bulkViewModel.scanTrigger.collectAsState()
    val items by bulkViewModel.scannedItems.collectAsState()

    val showDialog = remember { mutableStateOf(false) }
    val imageUrl = remember { mutableStateOf("") }
    val imageUri = rememberSaveable { mutableStateOf<String?>(null) }
    val photoUri = remember { mutableStateOf<Uri?>(null) }
    val shouldLaunchCamera = remember { mutableStateOf(false) }
    var shouldNavigateBack by remember { mutableStateOf(false) }

    LaunchedEffect(shouldNavigateBack) {
        if (shouldNavigateBack) {
            kotlinx.coroutines.delay(50)
            onBack()
        }
    }

    val fieldValues = remember { mutableStateMapOf<String, String>() }
    var isEditMode by remember { mutableStateOf(false) }

    val categoryName = fieldValues["Category"].orEmpty()
    val productName = fieldValues["Product"].orEmpty()
    val designName = fieldValues["Design"].orEmpty()
    val purityName = fieldValues["Purity"].orEmpty()
    val vendorName = fieldValues["Vendor"].orEmpty()
    fieldValues["SKU"].orEmpty()
    rememberCoroutineScope()

    var isScanning by remember { mutableStateOf(false) }
    //var showSuccessDialog by remember { mutableStateOf(false) }

    val activity = LocalContext.current as MainActivity





    val formFields = remember(
        vendorNames, skuList, categoryNames, categoryName, productName, designName, vendorName
    ) {
        sampleFields.map { field ->
            val options = when (field.label) {
                "Vendor" -> vendorNames
                "SKU" -> {
                    skuList?.filter { sku ->
                        sku.SKUVendor.any { it.VendorName == vendorName }
                    }?.mapNotNull { it.StockKeepingUnit } ?: emptyList()
                }
                "Category" -> categoryNames
                "Product" -> {
                    val categoryId = categoryList?.find { it.CategoryName == categoryName }?.Id
                    productList?.filter { it.CategoryId == categoryId }
                        ?.mapNotNull { it.ProductName } ?: emptyList()
                }
                "Design" -> {
                    val productId = productList?.find { it.ProductName == productName }?.Id
                    designList?.filter { it.ProductId == productId }?.mapNotNull { it.DesignName }
                        ?: emptyList()
                }
                "Purity" -> {
                    val categoryId = categoryList?.find { it.CategoryName == categoryName }?.Id
                    purityList?.filter { it.CategoryId == categoryId }?.mapNotNull { it.PurityName }
                        ?: emptyList()
                }
                else -> emptyList()
            }
            field.copy(options = options, value = fieldValues[field.label] ?: "")
        }.toMutableStateList()
    }

    @SuppressLint("DefaultLocale")
    fun updateField(label: String, value: String) {
        fieldValues[label] = value
        if (label == "Gross Weight" || label == "Stone Weight" || label == "Diamond Weight") {
            val gross = fieldValues["" +
                    "Gross Weight"]?.toDoubleOrNull() ?: 0.0
            val stone = fieldValues["Stone Weight"]?.toDoubleOrNull() ?: 0.0
            val diamond = fieldValues["Diamond Weight"]?.toDoubleOrNull() ?: 0.0
            val net = gross - stone - diamond
            fieldValues["Net Weight"] = if (net > 0) String.format("%.2f", net) else ""
        }
    }

    val isCategoryDisabled = fieldValues["SKU"].isNullOrEmpty().not()

    DisposableEffect(Unit) {
        val listener = object : ScanKeyListener {
            override fun onBarcodeKeyPressed() {


                bulkViewModel.startBarcodeScanning(context)
            }

            override fun onRfidKeyPressed() {
                if (isScanning) {
                    bulkViewModel.stopScanning()
                    isScanning = false
                } else {
                    bulkViewModel.startSingleScan(20)
                    isScanning = true
                }
            }
        }
        activity.registerScanKeyListener(listener)

        onDispose {
            activity.unregisterScanKeyListener()
        }
    }



    LaunchedEffect(Unit) {
        employee?.clientCode?.let {
            viewModel.fetchAllDropdownData(ClientCodeRequest(it))
        }
    }

    LaunchedEffect(scanTrigger) {
        scanTrigger?.let { type ->
            when (type) {
                "scan" -> if (items.size != 1) bulkViewModel.startScanning(20)
                "barcode" -> bulkViewModel.startBarcodeScanning(context)
            }
            bulkViewModel.clearScanTrigger()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.barcodeReader.openIfNeeded()
        viewModel.barcodeReader.setOnBarcodeScanned { scanned ->
            bulkViewModel.onBarcodeScanned(scanned)
            bulkViewModel.setRfidForAllTags(scanned)
            updateField("RFID Code", scanned)
        }
    }



    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) imageUri.value = photoUri.value?.toString()
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted && shouldLaunchCamera.value) {
                val uri = File(
                    context.cacheDir,
                    "${System.currentTimeMillis()}.jpg"
                ).apply { createNewFile() }.let {
                    FileProvider.getUriForFile(context, "${context.packageName}.provider", it)
                }
                photoUri.value = uri
                cameraLauncher.launch(uri)
            } else {
                Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
            shouldLaunchCamera.value = false
        }
    )
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> imageUri.value = uri?.toString() }
    if (showDialog.value) {
        ImageUploadDialog(
            showDialog = showDialog.value,
            onDismiss = { showDialog.value = false },
            onConfirm = {
                showDialog.value = false
            },
            onTakePhoto = {
                shouldLaunchCamera.value = true
                if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                } else {
                    val uri = File(
                        context.cacheDir,
                        "${System.currentTimeMillis()}.jpg"
                    ).apply { createNewFile() }.let {
                        FileProvider.getUriForFile(context, "${context.packageName}.provider", it)
                    }
                    photoUri.value = uri
                    cameraLauncher.launch(uri)
                }
            },
            onAttachFile = {
                galleryLauncher.launch("image/*")
            },
            imageUrl = imageUrl.value,
            onImageUrlChange = { imageUrl.value = it },
            imageUri = imageUri.value,
            onImageUriChange = { imageUri.value = it }
        )
    }


    CameraImagePicker(
        imageUri = imageUri,
        onImageSelected = { uri ->
            bulkViewModel
            Log.d("ImageSelected", "URI: $uri")
        }
    )

    fun onSkuSelected(sku: SKUModel) {

        if (categoryList.isNullOrEmpty() ||
            productList.isNullOrEmpty() ||
            designList.isNullOrEmpty() ||
            purityList.isNullOrEmpty()
        ) return
        updateField(
            "Category",
            categoryList.find { it.Id == sku.CategoryId }?.CategoryName.orEmpty()
        )
        updateField("Product", productList.find { it.Id == sku.ProductId }?.ProductName.orEmpty())
        updateField("Design", designList.find { it.Id == sku.DesignId }?.DesignName.orEmpty())
        updateField("Purity", purityList.find { it.Id == sku.PurityId }?.PurityName.orEmpty())
    }

    fun onVendorSelected(vendor: VendorModel) {
        updateField("Vendor", vendor.VendorName ?: "")
        updateField("SKU", "")
    }

    Scaffold(
        modifier = Modifier
            .focusable(true)
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown) {
                    when (event.key.nativeKeyCode) {
                        293, 280, 139 -> {
                            val keyType = if (event.key.nativeKeyCode == 139) "barcode" else "scan"
                            bulkViewModel.onScanKeyPressed(keyType)
                            true
                        }

                        else -> false
                    }
                } else false
            },
        topBar = {
            GradientTopBar(
                title = "Add Single Product",

                navigationIcon = {
                    IconButton(onClick = { shouldNavigateBack = true }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        bottomBar = {
            var isSaving by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()
            ScanBottomBar(
                onSave = {
                    if (isSaving) return@ScanBottomBar
                    isSaving = true
                    try {
                        viewModel.barcodeReader.close()

                        /* fun get(label: String) =
                             formFields.firstOrNull { it.label == label }?.value.orEmpty()*/
                        fun get(label: String) = fieldValues[label].orEmpty()

                        get("Item Code")
                        val rfidCode = get("RFID Code")
                        val epc = get("EPC")
                        val gWt = get("Gross Weight")
                        val ntWt = get("Net Weight")
                        val sWt = get("Stone Weight")
                        val dWt = get("Diamond Weight")
                        val making_gm = get("Making/Gram")
                        val making_perc = get("Making %")
                        val fMaking = get("Fix Making")
                        val fWastage = get("Fix Wastage")
                        val stAmt = get("Stone Amount")
                        val dAmt = get("Diamond Amount")

                        val categoryId =
                            categoryList?.find { it.CategoryName == categoryName }?.Id ?: 0
                        val productId = productList?.find { it.ProductName == productName }?.Id ?: 0
                        val designId = designList?.find { it.DesignName == designName }?.Id ?: 0
                        val vendorId = vendorList?.find { it.VendorName == vendorName }?.Id ?: 0
                        val purityId = purityList?.find { it.PurityName == purityName }?.Id ?: 0

                        val sku = skuList?.firstOrNull()
                        val savedClientCode = employee?.clientCode.orEmpty()
                        val savedEmployeeId = employee?.employeeId ?: 0
                        val savedBranchId = employee?.defaultBranchId ?: 0

                        val request = InsertProductRequest(
                            CategoryId = categoryId,
                            ProductId = productId,
                            DesignId = designId,
                            VendorId = vendorId,
                            PurityId = purityId,
                            RFIDCode = rfidCode,
                            HUIDCode = "",
                            HSNCode = "",
                            Quantity = "1",
                            TotalWeight = 0.0,
                            PackingWeight = 0.0,
                            GrossWt = gWt,
                            TotalStoneWeight = sWt,
                            NetWt = ntWt,
                            Pieces = "",
                            MakingPercentage = making_perc,
                            MakingPerGram = making_gm,
                            MakingFixedAmt = fMaking,
                            MakingFixedWastage = fWastage,
                            MRP = "",
                            ClipWeight = "",
                            ClipQuantity = "",
                            ProductCode = "",
                            Featured = "",
                            ProductTitle = "",
                            Description = "",
                            Gender = "",
                            DiamondId = "",
                            DiamondName = "",
                            DiamondShape = "",
                            DiamondShapeName = "",
                            DiamondClarity = "",
                            DiamondClarityName = "",
                            DiamondColour = "",
                            DiamondColourName = "",
                            DiamondSleve = "",
                            DiamondSize = "",
                            DiamondSellRate = "",
                            DiamondWeight = dWt,
                            DiamondCut = "",
                            DiamondCutName = "",
                            DiamondSettingType = "",
                            DiamondSettingTypeName = "",
                            DiamondCertificate = "",
                            DiamondDescription = "",
                            DiamondPacket = "",
                            DiamondBox = "",
                            DiamondPieces = "",
                            Stones = emptyList(),
                            DButton = "",
                            StoneName = "",
                            StoneShape = "",
                            StoneSize = "",
                            StoneWeight = sWt,
                            StonePieces = "",
                            StoneRatePiece = "",
                            StoneRateKarate = "",
                            StoneAmount = stAmt,
                            StoneDescription = "",
                            StoneCertificate = "",
                            StoneSettingType = "",
                            BranchName = "",
                            BranchId = sku?.BranchId?.takeIf { it != 0 } ?: savedBranchId,
                            PurityName = "",
                            TotalStoneAmount = stAmt,
                            TotalStonePieces = "",
                            ClientCode = (sku?.ClientCode?.takeIf { !it.isNullOrBlank() }
                                ?: savedClientCode),
                            EmployeeCode = sku?.EmployeeId?.takeIf { it != 0 } ?: savedEmployeeId,
                            StoneColour = "",
                            CompanyId = 0,
                            MetalId = 0,
                            WarehouseId = 0,
                            TIDNumber = epc,
                            TotalDiamondWeight = dWt,
                            TotalDiamondAmount = dAmt,
                            Status = "Active"
                        )
                        scope.launch {
                            val isStockAdded =
                                viewModel.insertLabelledStock(request)



                            Log.d("AddProductScreen", "isStockAdded" + isStockAdded)
                            if (isStockAdded) {
                                ToastUtils.showToast(context, "Stock Added Successfully!")
                                bulkViewModel.syncItems()

                            }else
                            {
                                ToastUtils.showToast(context, "Failed to Add Stock")
                            }
                            updateField("Vendor", "")
                            updateField("Product", "")
                            updateField("Category", "")
                            updateField("Design", "")
                            updateField("Purity", "")
                            updateField("SKU", "")
                            updateField("Gross Weight", "")
                            updateField("RFID Code", "")
                            updateField("EPC", "")
                            updateField("Net Weight", "")
                            updateField("Diamond Weight", "")
                            updateField("Making/Gram", "")
                            updateField("Making %", "")
                            updateField("Fix Making", "")
                            updateField("Fix Wastage", "")
                            updateField("Stone Amount", "")
                            updateField("Diamond Amount", "")
                            updateField("Stone Weight", "")

                        }
                    } finally {
                        isSaving = false
                    }
                },

                onList = { navController.navigate(Screens.ProductListScreen.route) },
                onScan = {
                    bulkViewModel.startSingleScan(20)
                },
                onGscan = {},
                onReset = {
                    updateField("Vendor", "")
                    updateField("Product", "")
                    updateField("Category", "")
                    updateField("Design", "")
                    updateField("Purity", "")
                    updateField("SKU", "")
                    updateField("Gross Weight", "")
                    updateField("RFID Code", "")
                    updateField("EPC", "")
                    updateField("Net Weight", "")
                    updateField("Diamond Weight", "")
                    updateField("Making/Gram", "")
                    updateField("Making %", "")
                    updateField("Fix Making", "")
                    updateField("Fix Wastage", "")
                    updateField("Stone Amount", "")
                    updateField("Diamond Amount", "")
                    updateField("Stone Weight", "")
                },
                isScanning = isScanning,
                isEditMode=isEditMode
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(formFields) { field ->
                val isDisabled = when (field.label) {
                    "Category", "Product", "Design" -> isCategoryDisabled
                    else -> false
                }

                FormRow(
                    field = field,
                    value = fieldValues[field.label] ?: "",
                    showDialog = showDialog.value,
                    onShowDialogChange = { showDialog.value = it },
                    imageUrl = imageUrl.value,
                    onImageUrlChange = { imageUrl.value = it },
                    onValueChange = { value ->
                        if (!isDisabled) {
                            updateField(field.label, value)
                            when (field.label) {
                                "Vendor" -> updateField("SKU", "")
                                "Category" -> {
                                    updateField("Product", "")
                                    updateField("Design", "")
                                    updateField("Purity", "")
                                }

                                "Product" -> {
                                    updateField("Design", "")
                                    updateField("Purity", "")
                                }

                                "Design" -> updateField("Purity", "")
                            }
                        }
                    },
                    skuList = skuList,
                    onSkuSelected = { onSkuSelected(it) },
                    selectedCategory = categoryName,
                    selectedProduct = productName,
                    selectedDesign = designName,
                    selectedVendor = vendorName
                )
            }
        }

//        if (showDialog.value) {
//            ImageUploadDialog(
//                showDialog = showDialog.value,
//                onDismiss = { showDialog.value = false },
//                onConfirm = {
//                    updateField("Image Upload", imageUri.value.orEmpty())
//                    showDialog.value = false
//                },
//                onTakePhoto = {
//                    val uri = File(
//                        context.cacheDir,
//                        "${System.currentTimeMillis()}.jpg"
//                    ).apply { createNewFile() }.let {
//                        FileProvider.getUriForFile(context, "${context.packageName}.provider", it)
//                    }
//                    photoUri.value = uri
//                    cameraLauncher.launch(uri)
//                },
//                onAttachFile = {
//                    galleryLauncher.launch("image/*")
//                },
//                imageUrl = imageUrl.value,
//                onImageUrlChange = { imageUrl.value = it },
//                imageUri = imageUri.value,
//                onImageUriChange = { imageUri.value = it }
//            )
//        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanBottomBar(
    onSave: () -> Unit,
    onList: () -> Unit,
    onScan: () -> Unit,
    onGscan: () -> Unit,
    onReset: () -> Unit,
    isScanning: Boolean,
    isEditMode: Boolean
) {

    // We use a Box to allow the center button to overlap/elevate
    Box {
        // 1) The background row of four icons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .background(Color.White), // light gray background
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onSave) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(R.drawable.ic_save),
                        tint = Color.DarkGray,
                        contentDescription = "Save",
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isEditMode) "Update" else "Save",
                        color = Color.DarkGray,
                        fontSize = 12.sp,
                        fontFamily = poppins
                    )
                }
            }
            TextButton(onClick = onList) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(R.drawable.ic_list),
                        tint = Color.DarkGray,
                        contentDescription = "List"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("List", color = Color.DarkGray, fontSize = 12.sp, fontFamily = poppins)
                }
            }
            Spacer(modifier = Modifier.width(64.dp)) // space for center button
       /*     TextButton(onClick = onGscan) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(R.drawable.ic_gscan),
                        tint = Color.DarkGray,
                        contentDescription = "Gscan"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gscan", color = Color.DarkGray, fontSize = 12.sp, fontFamily = poppins)
                }
            }*/
            TextButton(onClick = onGscan) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isScanning) {
                        // Use vector icon when scanning
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Stop Scan",
                            tint = Color.DarkGray
                        )
                    } else {
                        // Use painterResource when not scanning
                        Icon(
                            painter = painterResource(R.drawable.ic_gscan),
                            contentDescription = "Start Scan",
                            tint = Color.DarkGray
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = if (isScanning) "Stop" else "Gscan",
                        color = if (isScanning) Color.DarkGray else Color.DarkGray,
                        fontSize = 12.sp,
                        fontFamily = poppins
                    )
                }
            }


            TextButton(onClick = onReset) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(R.drawable.ic_reset),
                        tint = Color.DarkGray,
                        contentDescription = "Reset"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset", color = Color.DarkGray, fontSize = 12.sp, fontFamily = poppins)
                }
            }
        }


        // 2) The elevated circular Scan button, centered
        Box(
            modifier = Modifier
                .size(65.dp)
                .align(Alignment.TopCenter)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF5231A7), Color(0xFFD32940))
                    )
                )
                .clickable(onClick = onScan),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    painter = painterResource(R.drawable.ic_scan),
                    contentDescription = "Scan",
                    tint = Color.White,
                    modifier = Modifier.size(25.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Scan",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = poppins
                )
            }
        }


    }


}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanBottomBarInventory(
    onSave: () -> Unit,
    onList: () -> Unit,
    onScan: () -> Unit,
    onEmail: () -> Unit,
    onReset: () -> Unit,
    isScanning: Boolean
) {

    // We use a Box to allow the center button to overlap/elevate
    Box {
        // 1) The background row of four icons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .background(Color.White), // light gray background
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onSave) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(R.drawable.ic_save),
                        tint = Color.DarkGray,
                        contentDescription = "Save",
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save", color = Color.DarkGray, fontSize = 12.sp, fontFamily = poppins)
                }
            }
            TextButton(onClick = onList) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(R.drawable.ic_list),
                        tint = Color.DarkGray,
                        contentDescription = "List"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("List", color = Color.DarkGray, fontSize = 12.sp, fontFamily = poppins)
                }
            }
            Spacer(modifier = Modifier.width(64.dp)) // space for center button
            /*     TextButton(onClick = onGscan) {
                     Column(horizontalAlignment = Alignment.CenterHorizontally) {
                         Icon(
                             painter = painterResource(R.drawable.ic_gscan),
                             tint = Color.DarkGray,
                             contentDescription = "Gscan"
                         )
                         Spacer(modifier = Modifier.width(4.dp))
                         Text("Gscan", color = Color.DarkGray, fontSize = 12.sp, fontFamily = poppins)
                     }
                 }*/
            TextButton(onClick = onEmail) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    // Use vector icon when scanning
                    Icon(
                        painter = painterResource(R.drawable.ic_email),
                        contentDescription = "Email",
                        tint = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "Email",
                        color = Color.DarkGray,
                        fontSize = 12.sp,
                        fontFamily = poppins
                    )
                }
            }


            TextButton(onClick = onReset) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(R.drawable.ic_reset),
                        tint = Color.DarkGray,
                        contentDescription = "Reset"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset", color = Color.DarkGray, fontSize = 12.sp, fontFamily = poppins)
                }
            }
        }


        // 2) The elevated circular Scan button, centered
        Box(
            modifier = Modifier
                .size(65.dp)
                .align(Alignment.TopCenter)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF5231A7), Color(0xFFD32940))
                    )
                )
                .clickable(onClick = onScan),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isScanning) {
                    // Use vector icon when scanning
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Stop Scan",
                        tint = Color.White
                    )
                } else {
                    // Use painterResource when not scanning
                    Icon(
                        painter = painterResource(R.drawable.ic_scan),
                        contentDescription = "Start Scan",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (isScanning) "Stop" else "Scan",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = poppins
                )
            }
        }


    }


}


@OptIn(ExperimentalMaterial3Api::class)
// ✅ Updated FormRow with proper value binding and no local text state

@Composable
fun FormRow(
    field: FormField,
    value: String,
    showDialog: Boolean,
    onShowDialogChange: (Boolean) -> Unit,
    imageUrl: String,
    onImageUrlChange: (String) -> Unit,
    onValueChange: (String) -> Unit,
    skuList: List<SKUModel>? = null,
    onSkuSelected: ((SKUModel) -> Unit)? = null,
    selectedCategory: String? = null,
    selectedProduct: String? = null,
    selectedDesign: String? = null,
    selectedVendor: String? = null

) {
    var expanded by remember { mutableStateOf(false) }


    val filteredOptions = when (field.label) {
        "SKU" -> if (!selectedVendor.isNullOrBlank()) field.options else emptyList()
        "Product" -> if (!selectedCategory.isNullOrBlank()) field.options else emptyList()
        "Design" -> if (!selectedProduct.isNullOrBlank()) field.options else emptyList()
        "Purity" -> if (!selectedDesign.isNullOrBlank()) field.options else emptyList()
        else -> field.options
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color(0xFFE0E0E0), shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = field.label,
            fontSize = 14.sp,
            color = Color.DarkGray,
            modifier = Modifier.weight(0.8f)
        )

        Box(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (field.isDropdown) {
                if (filteredOptions.isEmpty()) {
                    Text(
                        text = "Please select a ${
                            when (field.label) {
                                "Product" -> "Category first"
                                "Design" -> "Product first"
                                "Purity" -> "Design first"
                                else -> "option"
                            }
                        }",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                } else {
                    Column {
                        BasicTextField(
                            value = value,
                            onValueChange = {newValue -> onValueChange(newValue)},
                            readOnly = true,
                            singleLine = true,
                            keyboardOptions = if (
                                field.label.contains("Weight", ignoreCase = true) ||
                                field.label.contains("Amount", ignoreCase = true) ||
                                field.label.contains("Wastage", ignoreCase = true) ||
                                field.label.contains("Making", ignoreCase = true)
                            ) {
                                KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            } else {
                                KeyboardOptions.Default
                            },
                            textStyle = TextStyle(fontSize = 14.sp, color = Color.Black),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = true },
                            decorationBox = { innerTextField ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        if (value.isEmpty()) {
                                            Text(
                                                "select",
                                                color = Color.LightGray,
                                                fontSize = 14.sp,
                                                fontFamily = poppins,
                                                maxLines = 2,
                                                lineHeight = 16.sp, // adds line spacing
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 2.dp)
                                            )
                                        }
                                        innerTextField()
                                    }

                                    Row {
                                        if (value.isNotEmpty()) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Clear",
                                                tint = Color.Gray,
                                                modifier = Modifier
                                                    .padding(end = 8.dp)
                                                    .size(20.dp)
                                                    .clickable { onValueChange("") }
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Filled.ArrowDropDown,
                                            contentDescription = "Dropdown",
                                            modifier = Modifier.clickable { expanded = true }
                                        )
                                    }
                                }
                            }
                        )

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            filteredOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, fontFamily = poppins) },
                                    onClick = {
                                        onValueChange(option)
                                        expanded = false

                                        if (field.label == "SKU") {
                                            skuList?.find { it.StockKeepingUnit == option }?.let {
                                                onSkuSelected?.invoke(it)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            } else if (field.label == "Image Upload") {
                Icon(
                    imageVector = Icons.Default.AddAPhoto,
                    contentDescription = "Upload",
                    modifier = Modifier.clickable { onShowDialogChange(true) }
                )
            } else {
                val isReadOnly = field.label == "Net Weight" // ⬅️ Make Net Weight read-only

                BasicTextField(
                    value = value,
                    onValueChange = { if (!isReadOnly) onValueChange(it) },
                    singleLine = true,
                    readOnly = isReadOnly,
                    keyboardOptions = if (
                        field.label.contains("Weight", ignoreCase = true) ||
                        field.label.contains("Amount", ignoreCase = true) ||
                        field.label.contains("Wastage", ignoreCase = true) ||
                        field.label.contains("Making", ignoreCase = true)
                    ) {
                        KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    } else {
                        KeyboardOptions.Default
                    },
                    textStyle = TextStyle(fontSize = 14.sp, color = Color.Black),
                    decorationBox = { inner ->
                        if (value.isEmpty()) {
                            Text(
                                "Tap to enter…",
                                color = Color.LightGray,
                                fontSize = 14.sp,
                                fontFamily = poppins
                            )
                        }
                        inner()
                    }
                )
            }

        }
    }
}

@Composable
fun CameraImagePicker(
    imageUri: MutableState<String?>,
    onImageSelected: (Uri) -> Unit,
) {
    val context = LocalContext.current
    val photoUri = remember { mutableStateOf<Uri?>(null) }
    val shouldLaunchCamera = remember { mutableStateOf(false) }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            shouldLaunchCamera.value = true
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri.value?.let {
                imageUri.value = it.toString()
                onImageSelected(it)
            }
        }
    }

    // Gallery Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri.value = it.toString()
            onImageSelected(it)
        }
    }

    // Launch camera if permission is granted
    LaunchedEffect(shouldLaunchCamera.value) {
        if (shouldLaunchCamera.value) {
            val photoFile = File(
                context.cacheDir,
                "${System.currentTimeMillis()}.jpg"
            ).apply {
                createNewFile()
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                photoFile
            )

            photoUri.value = uri
            shouldLaunchCamera.value = false

            try {
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Camera error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // UI Buttons
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = {
            val permission = android.Manifest.permission.CAMERA
            if (ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                shouldLaunchCamera.value = true
            } else {
                cameraPermissionLauncher.launch(permission)
            }
        }) {
            Text("Camera")
        }

        Button(onClick = {
            galleryLauncher.launch("image/*")
        }) {
            Text("Gallery")
        }
    }

    // Show selected image preview
    imageUri.value?.let { uri ->
        Spacer(modifier = Modifier.height(12.dp))
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(12.dp))
        )
    }
}


@Composable
fun ImageUploadDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onTakePhoto: () -> Unit,
    onAttachFile: () -> Unit,
    imageUrl: String,
    onImageUrlChange: (String) -> Unit,
    imageUri: String?, // <-- add this
    onImageUriChange: (String?) -> Unit // <-- add this
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = null,
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Image placeholder
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color(0xFFF0F0F0), shape = RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUri.isNullOrEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(
                                        Color(0xFFF0F0F0),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddAPhoto,
                                    contentDescription = "Add Photo",
                                    tint = Color.Gray
                                )
                            }
                        } else {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = "Uploaded Image",
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Take Photo Button
                    GradientButton(text = "Take Photo", onClick = onTakePhoto)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Image URL Input
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = onImageUrlChange,
                        placeholder = { Text("Image Url", fontFamily = poppins) },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Attach File",
                                tint = Color(0xFF8B0000),
                                modifier = Modifier.clickable(onClick = onAttachFile)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GradientButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )

                    GradientButton(
                        text = "Ok",
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    )
                }
            },
            shape = RoundedCornerShape(16.dp),
        )

    }


}







