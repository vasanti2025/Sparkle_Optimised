package com.loyalstring.rfid.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.remote.data.Branch
import com.loyalstring.rfid.data.remote.data.EditDataRequest
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.navigation.Screens
import com.loyalstring.rfid.ui.utils.GradientButton
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.ProductImageWithAllFallbacks
import com.loyalstring.rfid.ui.utils.getLocalProductImageFile
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.BulkViewModel
import com.loyalstring.rfid.viewmodel.EditProductViewModel
import com.loyalstring.rfid.viewmodel.SingleProductViewModel
import com.loyalstring.rfid.viewmodel.UploadState
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EditProductScreen(
    onBack: () -> Unit,
    navController: NavHostController,

    item: BulkItem
) {
    val context = LocalContext.current
    val viewModel: EditProductViewModel = hiltViewModel()
    val bulkViewModel: BulkViewModel = hiltViewModel()
    val singleProductViewModel: SingleProductViewModel=hiltViewModel()
    val cacheDir = context.cacheDir
    val employee = UserPreferences.getInstance(context).getEmployee(Employee::class.java)
    val isApiActiveItem = item.Status.equals("ApiActive", ignoreCase = true)

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

    var branchId by remember { mutableStateOf(item.branchId ?: 0) }
    var branchName by remember { mutableStateOf(item.branchName.orEmpty()) }
    var productId by remember { mutableStateOf(item.productId ?: 0) }
    var designId by remember { mutableStateOf(item.designId ?: 0) }
    var productCode by remember { mutableStateOf(item.productCode.orEmpty()) }
    var productNameState by remember { mutableStateOf(item.productName.orEmpty()) }
    var imageUrlState by remember { mutableStateOf(item.imageUrl.orEmpty()) }
    var purityId by remember { mutableStateOf(item.purityId ?: 0) }
    Log.d("EDIT_ITEM", item.toString())
    Log.d("purityId","purityId"+purityId+" "+makingPer)

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
                },
                titleTextSize = 20.sp
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
                if (!isApiActiveItem) {
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

                   /* val request = EditDataRequest(
                        Id = item.bulkItemId ?: 0,
                        ProductTitle = productName,
                        ClipWeight = "",
                        ClipQuantity = "",
                        ItemCode = itemCode,
                        HSNCode = "",
                        Description = "",
                        ProductCode = productCode,
                        MetalName = "",
                        CategoryId = categoryId,
                        ProductId = productId,
                        DesignId =designId,
                        PurityId = purityId,
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
                        TotalStoneWeight = item.totalStoneWt.toString()?:"0.00",
                        TotalStoneAmount = item.stoneAmount,
                        TotalStonePieces = "",
                        TotalDiamondWeight = item.diamondWeight,
                        TotalDiamondPieces = "",
                        TotalDiamondAmount = item.diamondAmount,
                        Featured = "",
                        Pieces = item.pcs.toString(),
                        HallmarkAmount = "",
                        HUIDCode = "",
                        MRP = item.mrp.toString(),
                        VendorId = 0,
                        FirmName = "",
                        BoxId = item.boxId?:0,
                        TIDNumber = item.epc,
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
                        BranchName =branchName,
                        BranchType = "",
                        BoxName = "",
                        EstimatedDays = "",
                        OfferPrice = "",
                        Rating = "",
                        Ranking = "",
                        CompanyId = 0,
                        BranchId = branchId,
                        EmployeeId = employee?.employeeId,
                        Status = item.Status,
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
                        ProductName = productName,
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
                        OldItemCode = item.itemCode,
                        Stones = emptyList(),
                        Diamonds = emptyList(),
                        InvoiceDetails = emptyList(),
                        Counter = "",
                        Branch = null,
                        StonePieces = "",
                        Quantity = 1,
                        StoneWeight = swt,
                        epc = epc,
                        SKUId= item.SKUId,
                        UserId = employee?.userId
                    )*/
                   fun safeStr(value: Any?, defaultValue: String = ""): String {
                       return value?.toString()
                           ?.trim()
                           ?.takeIf { it.isNotBlank() && !it.equals("null", true) }
                           ?: defaultValue
                   }

                    fun safeDouble(value: Any?, defaultValue: Double = 0.0): Double {
                        return value?.toString()
                            ?.trim()
                            ?.takeIf { it.isNotBlank() && !it.equals("null", true) }
                            ?.toDoubleOrNull()
                            ?: defaultValue
                    }

                    val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                    val request = EditDataRequest(
                        Id = item.bulkItemId ?: 0,

                        ProductTitle = safeStr(productName, safeStr(item.productName)),
                        ClipWeight = "0.000",
                        ClipQuantity = "0",
                        ItemCode = safeStr(itemCode, safeStr(item.itemCode)),
                        HSNCode = "",
                        Description = "",
                        ProductCode = safeStr(productCode),

                        MetalName = "",
                        CategoryId = categoryId ?: 0,
                        ProductId = productId,
                        DesignId = designId,
                        PurityId = purityId,

                        Colour = "",
                        Size = "",
                        WeightCategory = null,

                        GrossWt = safeStr(gwt, safeStr(item.grossWeight, "0.000")),
                        NetWt = safeStr(nwt, safeStr(item.netWeight, "0.000")),

                        CollectionName = "",
                        OccassionName = "",
                        Gender = "",

                        MakingFixedAmt = safeStr(fixedmaking, "0.000"),
                        MakingPerGram = safeStr(makingGram, "0.000"),
                        MakingFixedWastage = safeStr(fixedWastage, "0.000"),
                        MakingPercentage = safeStr(makingPer, "0.000"),

                        TotalStoneWeight = safeStr(swt, safeStr(item.totalStoneWt, "0.000")),
                        TotalStoneAmount = safeStr(stoneAmt, safeStr(item.stoneAmount, "0.00")),
                        TotalStonePieces = "0",

                        TotalDiamondWeight = safeStr(dwt, safeStr(item.diamondWeight, "0.000")),
                        TotalDiamondPieces = "0",
                        TotalDiamondAmount = safeStr(diamondAmount, safeStr(item.diamondAmount, "0.00")),

                        Featured = "",
                        Pieces = safeStr(item.pcs, "0"),
                        HallmarkAmount = "",
                        HUIDCode = "",
                        MRP = safeStr(item.mrp, "0.000"),

                        // IMPORTANT: do not force wrong blank/null values
                        VendorId = 1 ?: 1,
                        FirmName = "",
                        BoxId = item.boxId ?: 0,

                        TIDNumber = safeStr(epc, safeStr(item.epc)),
                        RFIDCode = safeStr(rfid, safeStr(item.rfid)),

                        FinePercent = "",
                        WastagePercent = "",

                        Images = if (!localPath.isNullOrBlank()) localPath else safeStr(item.imageUrl),

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

                        BranchName = safeStr(branchName, safeStr(item.branchName)),
                        BranchType = "",
                        BoxName = "",
                        EstimatedDays = "",
                        OfferPrice = "",
                        Rating = "",
                        Ranking = "",

                        CompanyId = 0,
                        BranchId = branchId,
                        EmployeeId = employee?.employeeId,

                        Status = item.Status,
                        ClientCode = employee?.clientCode,
                        UpdatedFrom = null,
                        count = 0,
                        SalesmanId = null,
                        TotalCount = 0,

                        // IMPORTANT: web request has MetalId = 1, do not send 0
                        MetalId = 1?: 1,
                        WarehouseId = 0,

                        CreatedOn = todayDate,
                        LastUpdated = todayDate,

                        TaxId = 0,
                        TaxPercentage = "",

                        OtherWeight = safeStr(dwt, "0.000"),
                        PouchWeight = "",

                        CategoryName = safeStr(category, safeStr(item.category)),
                        PurityName = safeStr(purity, safeStr(item.purity)),
                        TodaysRate = "",
                        ProductName = safeStr(productName, safeStr(item.productName)),
                        DesignName = safeStr(design, safeStr(item.design)),

                        DiamondSize = "",
                        DiamondWeight = safeStr(dwt, safeStr(item.diamondWeight, "0.000")),
                        DiamondPurchaseRate = "",
                        DiamondSellRate = "",
                        DiamondClarity = "",
                        DiamondColour = "",
                        DiamondShape = "",
                        DiamondCut = "",
                        DiamondSettingType = "",
                        DiamondCertificate = "",
                        DiamondPieces = "0",
                        DiamondPurchaseAmount = safeStr(diamondAmount, safeStr(item.diamondAmount, "0.00")),
                        DiamondSellAmount = safeStr(diamondAmount, safeStr(item.diamondAmount, "0.00")),
                        DiamondDescription = "",

                        TagWeight = "",
                        FindingWeight = "",
                        LanyardWeight = "",

                        PacketId = 0,
                        PacketName = "",
                        CollectionId = 0,
                        CollectionNameSKU = safeStr(sku, safeStr(item.sku)),

                        PackingWeight = 0.0,
                        TotalWeight = safeDouble(item.totalGwt, safeDouble(gwt, 0.0)),

                        StoneColour = "",
                        StoneShape = "",
                        StoneSize = "",
                        StoneRatePerPiece = "",
                        StoneWeightType = "",
                        StoneCertificate = "",
                        StoneSettingType = "",
                        StoneCategory = "",

                        DiamondCategory = "",
                        FromDate = todayDate,
                        ToDate = todayDate,
                        DiamondSleveName = "",
                        DiamondSizeName = "",
                        DiamondRate = "",
                        DiamondAmount = safeStr(diamondAmount, safeStr(item.diamondAmount, "0.00")),
                        DiamondBoxName = "",
                        DiamondPacketName = "",
                        HexCode = "",
                        DiamondDeduct = "",
                        SoldDate = todayDate,

                        OldItemCode =false,

                        Stones = emptyList(),
                        Diamonds = emptyList(),
                        InvoiceDetails = emptyList(),

                        Counter = "",
                        Branch = Branch(
                            label = safeStr(branchName, safeStr(item.branchName)),
                            value = branchId ?: 0
                        ),

                        StonePieces = "0",
                        Quantity = 1,
                        StoneWeight = safeStr(swt, safeStr(item.totalStoneWt, "0.000")),

                        epc = safeStr(epc, safeStr(item.epc)),
                        SKUId = 1,
                        UserId = employee?.id
                    )


                    Log.d("UPDATE_REQ", "purityId=$purityId, makingPer=$makingPer")

                    val requestList = listOf(request)

                    scope.launch {
                        val ok = singleProductViewModel.updateLabelledStock(requestList)
                        if (ok) {
                            // show success snackbar
                            snackbarHostState.showSnackbar("✅ Stock updated successfully!")
                            bulkViewModel.syncItems(context)
                            navController.popBackStack()
                        }
                    }
                }
                )
                }
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

            val baseUrl = "https://rrgold.loyalstring.co.in/"
            val priorityLocalPaths = remember(localPath, daoState.value) {
                buildList {
                    localPath?.trim()?.takeIf { it.isNotBlank() }?.let { add(it) }
                    daoState.value?.trim()?.trimEnd(',')?.takeIf { it.startsWith("/") }?.let { add(it) }
                }
            }

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
                    .clickable(enabled = !isApiActiveItem) { showChooser = true },
                contentAlignment = Alignment.Center
            ) {
                ProductImageWithAllFallbacks(
                    imageUrl = daoState.value ?: item.imageUrl,
                    itemCode = item.itemCode,
                    designName = item.design,
                    baseUrl = baseUrl,
                    priorityLocalPaths = priorityLocalPaths,
                    modifier = Modifier.fillMaxSize(),
                    cacheRemoteToLocal = true,
                    fallbackIcon = Icons.Default.PhotoCamera,
                    fallbackIconTint = Color.White,
                )
            }



            Spacer(modifier = Modifier.height(16.dp))

            if (isApiActiveItem) {
                Text(
                    text = stringResource(R.string.api_active_cannot_edit),
                    color = Color.Red,
                    fontFamily = poppins,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            InputField("Product Name", productName, { productName = it }, true, isNumber = false)
            InputField("Item Code", itemCode, { itemCode = it }, true, false)
            InputField("RFID", rfid, { rfid = it }, isApiActiveItem, false)

            InputField("G.Wt", gwt, {
                gwt = it
                nwt = calculateNetWeight(gwt, swt, dwt)
            }, isApiActiveItem, true)
            InputField("S.Wt", swt, {
                swt = it
                nwt = calculateNetWeight(gwt, swt, dwt)
            }, isApiActiveItem, true)
            InputField("D.Wt", dwt, {
                dwt = it
                nwt = calculateNetWeight(gwt, swt, dwt)
            }, isApiActiveItem, true)
            InputField("N.Wt", nwt, { nwt = it }, true, true)
            InputField("Category", category, { category = it }, true, false)
            InputField("Design", design, { design = it }, true, false)
            InputField("Purity", purity, { purity = it }, true, false)
            InputField("Making/Gram", makingGram, { makingGram = it }, isApiActiveItem, true)
            InputField("Making %", makingPer, { makingPer = it }, isApiActiveItem, true)
            InputField("Fixed Making", fixedmaking, { fixedmaking = it }, isApiActiveItem, true)
            InputField("Fixed Wastage", fixedWastage, { fixedWastage = it }, isApiActiveItem, true)
            InputField("Stone Amt", stoneAmt, { stoneAmt = it }, isApiActiveItem, true)
            InputField("Diamond Amt", diamondAmount, { diamondAmount = it }, isApiActiveItem, true)
            InputField("SKU", sku, { sku = it }, true, false)
            InputField("EPC", epc, { epc = it }, isApiActiveItem, false)
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

/*fun compressAndSetImage(
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
}*/

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

        // temp compressed file for upload
        val cacheFile = File(cacheDir, "$fileName.jpg")
        var quality = 90

        while (true) {
            FileOutputStream(cacheFile).use { out ->
                originalBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            if (cacheFile.length() < 200 * 1024 || quality <= 10) break
            quality -= 10
        }

        // permanent local file for display fallback
        val localFile = saveBitmapToProductImages(context, originalBitmap, fileName)

        onCompressed(localFile ?: cacheFile)
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

fun getProductImageFile(
    context: Context,
    itemCode: String?,
    designName: String? = null,
): File? = getLocalProductImageFile(context, itemCode, designName)

fun saveBitmapToProductImages(
    context: Context,
    bitmap: Bitmap,
    itemCode: String
): File? {


    return try {
        val imageDir = File(context.getExternalFilesDir(null), "product_images")
        if (!imageDir.exists()) {
            imageDir.mkdirs()
        }

        val file = File(imageDir, "${itemCode.trim()}.jpg")

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
        }

        Log.d("EditProductScreen", "Saved local image: ${file.absolutePath}")
        file
    } catch (e: Exception) {
        Log.e("EditProductScreen", "Failed to save local image: ${e.message}", e)
        null
    }
}


