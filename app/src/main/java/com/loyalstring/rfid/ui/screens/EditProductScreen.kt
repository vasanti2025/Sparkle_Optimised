package com.loyalstring.rfid.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.remote.data.EditDataRequest
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.navigation.Screens
import com.loyalstring.rfid.ui.utils.GradientButton
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.EditProductViewModel
import com.loyalstring.rfid.viewmodel.SingleProductViewModel
import com.loyalstring.rfid.viewmodel.UploadState
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun EditProductScreen(
    onBack: () -> Unit,
    navController: NavHostController,

    item: BulkItem
) {
    val context = LocalContext.current
    val viewModel: EditProductViewModel = hiltViewModel()
    val singleProductViewModel: SingleProductViewModel=hiltViewModel()
    val cacheDir = context.cacheDir
    val employee = UserPreferences.getInstance(context).getEmployee(Employee::class.java)

    var shouldNavigateBack by remember { mutableStateOf(false) }
    var showChooser by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val uploadState by viewModel.uploadState
    val errorMessage by viewModel.errorMessage
    val daoState = remember { mutableStateOf(item.imageUrl) } // initial from Room
    var localPath by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(shouldNavigateBack) {
        if (shouldNavigateBack) {
            kotlinx.coroutines.delay(50)
            onBack()
        }
    }

    LaunchedEffect(viewModel.uploadState.value) {
        if (viewModel.uploadState.value == UploadState.Success) {
            daoState.value = localPath  // now Room is updated too
        }
    }

    LaunchedEffect(uploadState) {
        when (uploadState) {
            UploadState.Uploading -> {
                snackbarHostState.showSnackbar("Uploading image...")
            }

            UploadState.Success -> {
                snackbarHostState.showSnackbar("✅ Image uploaded successfully!")
                navController.navigate(Screens.ProductListScreen.route) {
                    popUpTo(Screens.ProductListScreen.route) { inclusive = true }
                }
            }

            UploadState.Failed -> {
                snackbarHostState.showSnackbar("❌ Upload failed.")
            }

            UploadState.Error -> {
                snackbarHostState.showSnackbar("❌ Error: ${errorMessage ?: "Unknown error"}")
            }

            else -> {
            }
        }
    }


    var compressedImagePath by remember { mutableStateOf<String?>(null) }
    val cameraFile = remember {
        File(
            context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "${item.itemCode}.jpg"
        )
    }

    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                compressAndSetImage(
                    cameraFile.toUri(), context, cacheDir, item.itemCode ?: "image"
                ) { file ->
                    compressedImagePath = file.absolutePath
                    localPath = file.absolutePath
                }
            }
        }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                cameraFile
            )
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                compressAndSetImage(it, context, cacheDir, item.itemCode ?: "image") { file ->
                    compressedImagePath = file.absolutePath
                    localPath = file.absolutePath
                }
            }
        }
    var productName by remember { mutableStateOf(item.productName.orEmpty()) }
    var categoryId by remember { mutableStateOf(item.categoryId) }
    var itemCode by remember { mutableStateOf(item.itemCode.orEmpty()) }
    var rfid by remember { mutableStateOf(item.rfid.orEmpty()) }
    var gwt by remember { mutableStateOf(item.grossWeight.orEmpty()) }
    var swt by remember { mutableStateOf(item.stoneWeight.orEmpty()) }
    var dwt by remember { mutableStateOf(item.diamondWeight.orEmpty()) }
    var nwt by remember { mutableStateOf(item.netWeight.orEmpty()) }
    var category by remember { mutableStateOf(item.category.orEmpty()) }
    var design by remember { mutableStateOf(item.design.orEmpty()) }
    var purity by remember { mutableStateOf(item.purity.orEmpty()) }
    var makingGram by remember { mutableStateOf(item.makingPerGram.orEmpty()) }
    var makingPer by remember { mutableStateOf(item.makingPercent.orEmpty()) }
    var fixedmaking by remember { mutableStateOf(item.fixMaking.orEmpty()) }
    var fixedWastage by remember { mutableStateOf(item.fixWastage.orEmpty()) }
    var stoneAmt by remember { mutableStateOf(item.stoneAmount.orEmpty()) }
    var diamondAmount by remember { mutableStateOf(item.diamondAmount.orEmpty()) }
    var sku by remember { mutableStateOf(item.sku.orEmpty()) }
    var epc by remember { mutableStateOf(item.epc.orEmpty()) }
    var vendor by remember { mutableStateOf(item.vendor.orEmpty()) }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = "Edit Product",
                navigationIcon = {
                    IconButton(onClick = { shouldNavigateBack = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        bottomBar = {
            val scope = rememberCoroutineScope()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                GradientButton(text = "Cancel", onClick = { navController.popBackStack() })
                Spacer(modifier = Modifier.width(12.dp))
                GradientButton(text = "OK", onClick = {
                    // ✅ Upload only if image is selected
                    localPath?.let {
                        val file = File(it)
                        if (file.exists()) {
                            viewModel.uploadImage(
                                clientCode = employee?.clientCode ?: "",
                                itemCode = item.itemCode ?: "",
                                imageFile = file
                            )
                        }
                    }

                    val request = EditDataRequest(
                        Id = item.id ?: 0,
                        ProductTitle = productName,
                        ClipWeight = "",
                        ClipQuantity = "",
                        ItemCode = itemCode,
                        HSNCode = "",
                        Description = "",
                        ProductCode = item.productCode,
                        MetalName = "",
                        CategoryId = categoryId,
                        ProductId = item.productId,
                        DesignId = item.designId,
                        PurityId = 0,
                        Colour = "",
                        Size = "",
                        WeightCategory = null,
                        GrossWt = gwt,
                        NetWt = nwt,
                        CollectionName = "",
                        OccassionName = "",
                        Gender = "",
                        MakingFixedAmt = fixedmaking,
                        MakingPerGram = makingGram,
                        MakingFixedWastage = fixedWastage,
                        MakingPercentage = makingPer,
                        TotalStoneWeight = item.totalStoneWt.toString(),
                        TotalStoneAmount = item.stoneAmount,
                        TotalStonePieces = "",
                        TotalDiamondWeight = item.diamondWeight,
                        TotalDiamondPieces = "",
                        TotalDiamondAmount = item.diamondAmount,
                        Featured = "",
                        Pieces = "",
                        HallmarkAmount = "",
                        HUIDCode = "",
                        MRP = item.mrp.toString(),
                        VendorId = 0,
                        FirmName = "",
                        BoxId = 0,
                        TIDNumber = item.tid,
                        RFIDCode = rfid,
                        FinePercent = "",
                        WastagePercent = "",
                        Images = if (localPath != null) localPath else item.imageUrl,
                        BlackBeads = "",
                        Height = "",
                        Width = "",
                        OrderedItemId = "",
                        OrderNo = "",
                        UrdNo = "",
                        UrdId = null,
                        CuttingGrossWt = "",
                        CuttingNetWt = "",
                        MetalRate = "",
                        LotNumber = "",
                        DeptId = 0,
                        PurchaseCost = "",
                        Margin = "",
                        BranchName = "",
                        BranchType = "",
                        BoxName = "",
                        EstimatedDays = "",
                        OfferPrice = "",
                        Rating = "",
                        Ranking = "",
                        CompanyId = 0,
                        BranchId = item.branchId,
                        EmployeeId = employee?.employeeId,
                        Status = "Active",
                        ClientCode = employee?.clientCode,
                        UpdatedFrom = null,
                        count = 0,
                        SalesmanId = null,
                        TotalCount = 0,
                        MetalId = 0,
                        WarehouseId = 0,
                        CreatedOn = "2025-08-21",
                        LastUpdated = "2025-08-21",
                        TaxId = 0,
                        TaxPercentage = "",
                        OtherWeight = dwt,
                        PouchWeight = "",
                        CategoryName = category,
                        PurityName = purity,
                        TodaysRate = "",
                        ProductName = item.productName,
                        DesignName = design,
                        DiamondSize = "",
                        DiamondWeight = "",
                        DiamondPurchaseRate = "",
                        DiamondSellRate = "",
                        DiamondClarity = "",
                        DiamondColour = "",
                        DiamondShape = "",
                        DiamondCut = "",
                        DiamondSettingType = "",
                        DiamondCertificate = "",
                        DiamondPieces = "",
                        DiamondPurchaseAmount = "",
                        DiamondSellAmount = "",
                        DiamondDescription = "",
                        TagWeight = "",
                        FindingWeight = "",
                        LanyardWeight = "",
                        PacketId = 0,
                        PacketName = "",
                        CollectionId = 0,
                        CollectionNameSKU = sku,
                        PackingWeight = 0.0,
                        TotalWeight = item.totalGwt,
                        StoneColour = "",
                        StoneShape = "",
                        StoneSize = "",
                        StoneRatePerPiece = "",
                        StoneWeightType = "",
                        StoneCertificate = "",
                        StoneSettingType = "",
                        StoneCategory = "",
                        DiamondCategory = "",
                        FromDate = "2025-08-21",
                        ToDate = "2025-08-21",
                        DiamondSleveName = "",
                        DiamondSizeName = "",
                        DiamondRate = "",
                        DiamondAmount = "",
                        DiamondBoxName = "",
                        DiamondPacketName = "",
                        HexCode = "",
                        DiamondDeduct = "",
                        SoldDate = "2025-08-21",
                        OldItemCode = null,
                        Stones = emptyList(),
                        Diamonds = emptyList(),
                        InvoiceDetails = emptyList(),
                        Counter = "",
                        Branch = null,
                        StonePieces = "",
                        Quantity = 1,
                        StoneWeight = swt,
                        epc = epc
                    )

                    val requestList = listOf(request)

                    scope.launch {
                        val ok = singleProductViewModel.updateLabelledStock(requestList)
                        if (ok) {
                            // show success snackbar
                            snackbarHostState.showSnackbar("✅ Stock updated successfully!")
                            navController.popBackStack()
                        }
                    }
                }
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) } // 👈 attach host

    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Build your displayImageSource:
            val baseUrl = "https://rrgold.loyalstring.co.in/"

// Safely resolve display image source
            val displayImageSource: Any? = when {
                !localPath.isNullOrBlank() -> {
                    val file = File(localPath!!)
                    if (file.exists()) file else null
                }

                !daoState.value.isNullOrBlank() -> {
                    val stored =
                        daoState.value!!.trim().trimEnd(',') // remove any trailing commas/spaces
                    if (stored.startsWith("/")) {
                        val file = File(stored)
                        if (file.exists()) file else null
                    } else {
                        stored.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .lastOrNull()
                            ?.let { "$baseUrl$it" }
                    }
                }

                else -> null
            }


// Debug logging
            if (displayImageSource == null) {
                android.util.Log.w(
                    "EditProductScreen",
                    "No image to display. localPath=$localPath, daoState=${daoState.value}"
                )
            }

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
                    .clickable { showChooser = true },
                contentAlignment = Alignment.Center
            ) {
                if (displayImageSource != null) {
                    Image(
                        painter = rememberAsyncImagePainter(displayImageSource),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Pick Image",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }



            Spacer(modifier = Modifier.height(16.dp))
            InputField("Product Name", productName, { productName = it }, true, isNumber = false)
            InputField("Item Code", itemCode, { itemCode = it }, true, false)
            InputField("RFID", rfid, { rfid = it }, false, false)

            InputField("G.Wt", gwt, {
                gwt = it
                nwt = calculateNetWeight(gwt, swt, dwt)
            }, false, true)
            InputField("S.Wt", swt, {
                swt = it
                nwt = calculateNetWeight(gwt, swt, dwt)
            }, false, true)
            InputField("D.Wt", dwt, {
                dwt = it
                nwt = calculateNetWeight(gwt, swt, dwt)
            }, false, true)
            InputField("N.Wt", nwt, { nwt = it }, true, true)
            InputField("Category", category, { category = it }, true, false)
            InputField("Design", design, { design = it }, true, false)
            InputField("Purity", purity, { purity = it }, true, false)
            InputField("Making/Gram", makingGram, { makingGram = it }, false, true)
            InputField("Making %", makingPer, { makingPer = it }, false, true)
            InputField("Fixed Making", fixedmaking, { fixedmaking = it }, false, true)
            InputField("Fixed Wastage", fixedWastage, { fixedWastage = it }, false, true)
            InputField("Stone Amt", stoneAmt, { stoneAmt = it }, false, true)
            InputField("Diamond Amt", diamondAmount, { diamondAmount = it }, false, true)
            InputField("SKU", sku, { sku = it }, true, false)
            InputField("EPC", epc, { epc = it }, false, false)
            InputField("Vendor", vendor, { vendor = it }, true, false)
        }

        if (showChooser) {
            AlertDialog(
                onDismissRequest = { showChooser = false },
                title = { Text("Select Image From", fontFamily = poppins) },
                confirmButton = {
                    TextButton(onClick = {
                        showChooser = false
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            cameraFile
                        )
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }) {
                        Text("Camera", fontFamily = poppins)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showChooser = false
                        galleryLauncher.launch("image/*")
                    }) {
                        Text("Gallery", fontFamily = poppins)
                    }
                }
            )
        }


    }
}

fun calculateNetWeight(gross: String, stone: String, diamond: String): String {
    val g = gross.toDoubleOrNull() ?: 0.0
    val s = stone.toDoubleOrNull() ?: 0.0
    val d = diamond.toDoubleOrNull() ?: 0.0
    return (g - (s + d)).coerceAtLeast(0.0).toString()
}

private fun toIntOrZero(v: Any?): Int = when (v) {
    is Int -> v
    is Number -> v.toInt()
    is String -> v.toIntOrNull() ?: 0
    else -> 0
}

private fun toDoubleOrNullSafe(v: Any?): Double? = when (v) {
    is Double -> v
    is Number -> v.toDouble()
    is String -> v.toDoubleOrNull()
    else -> null
}

private fun toDoubleOrZero(v: Any?): Double = toDoubleOrNullSafe(v) ?: 0.0
private fun str(v: Any?): String = v?.toString().orEmpty()

fun showImageChooser(
    context: Context,
    galleryLauncher: ManagedActivityResultLauncher<String, Uri?>,
    cameraLauncher: ManagedActivityResultLauncher<Uri, Boolean>,
    cameraFile: File
) {
    AlertDialog.Builder(context)
        .setTitle("Select Image Source")
        .setItems(arrayOf("Camera", "Gallery")) { _, which ->
            when (which) {
                0 -> {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        cameraFile
                    )
                    cameraLauncher.launch(uri)
                }

                1 -> galleryLauncher.launch("image/*")
            }
        }
        .show()
}

fun compressAndSetImage(
    uri: Uri,
    context: Context,
    cacheDir: File,
    fileName: String,
    onCompressed: (File) -> Unit
) {
    try {
        val originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }

        val file = File(cacheDir, "$fileName.jpg")
        var quality = 90

        while (true) {
            FileOutputStream(file).use { out ->
                originalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            if (file.length() < 200 * 1024 || quality <= 10) break
            quality -= 10
        }

        onCompressed(file)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@Composable
fun InputField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    readOnly: Boolean,
    isNumber: Boolean
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontFamily = poppins) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        readOnly = readOnly,
        keyboardOptions = if (isNumber) {
            KeyboardOptions(keyboardType = KeyboardType.Number)
        } else {
            KeyboardOptions.Default
        }
    )
}


