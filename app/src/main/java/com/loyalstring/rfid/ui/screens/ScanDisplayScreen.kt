// ScanDisplayScreen.kt
package com.loyalstring.rfid.ui.screens
import kotlinx.coroutines.yield
import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentEnforcement
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.loyalstring.rfid.MainActivity
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.model.stockVerification.Item
import com.loyalstring.rfid.data.reader.ScanKeyListener
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.ui.utils.GradientButton
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.BulkViewModel
import com.loyalstring.rfid.viewmodel.ProductListViewModel
import com.loyalstring.rfid.viewmodel.ScanDisplayViewModel
import com.loyalstring.rfid.viewmodel.SingleProductViewModel
import com.loyalstring.rfid.worker.LocaleHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigDecimal
import java.math.RoundingMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import kotlin.text.clear


// column widths
val colCategoryWidth = 80.dp
val colQtyWidth = 44.dp
val colWeightWidth = 62.dp
val colMatchedQtyWidth = 50.dp
val colMatchedWtWidth = 62.dp
val colStatusWidth = 50.dp
val colDesignNameWidth = 90.dp   // was 100 → FIX wrap issue
val colRfidWidth = 80.dp          // slightly increased
val colItemCodeWidth = 80.dp     // better spacing
val colGWtWidth = 60.dp           // was 50 → avoid tight text
val colStatusIconWidth = 55.dp    // better icon spacing

private const val MENU_ALL = "ALL"
private const val MENU_MATCHED = "MATCHED"
private const val MENU_UNMATCHED = "UNMATCHED"
private const val MENU_SEARCH = "SEARCH"
private const val MENU_UNLABELLED = "UNLABELLED"
private const val MENU_RESUME = "RESUME"

// Pre-compiled regex to avoid creating new Regex objects on each call (ANR fix)
private val WEIGHT_CLEAN_REGEX = Regex("[^0-9.]")

@SuppressLint("UnrememberedMutableState")
@Composable
fun ScanDisplayScreen(onBack: () -> Unit, navController: NavHostController) {
    val scope = rememberCoroutineScope()
    val singleProductViewModel: SingleProductViewModel = hiltViewModel()
    val productListViewModel: ProductListViewModel = hiltViewModel()
    val scanDisplayViewModel: ScanDisplayViewModel = hiltViewModel()
    val bulkViewModel: BulkViewModel = hiltViewModel()
    val context: Context = LocalContext.current
    var shouldNavigateBack by remember { mutableStateOf(false) }
    val emailStatus by scanDisplayViewModel.emailStatus.collectAsState()

    val loading by scanDisplayViewModel.loading.collectAsState()
    val error by scanDisplayViewModel.error.collectAsState()
    val successMsg by scanDisplayViewModel.successMsg.collectAsState()
    var isSending by remember { mutableStateOf(false) }



    // Handle back navigation with delay to allow ripple animation to complete
  /*  LaunchedEffect(shouldNavigateBack) {
        if (shouldNavigateBack) {
            kotlinx.coroutines.delay(50) // Small delay for ripple animation
            onBack()
        }
    }*/

    var showRfidDialog by remember { mutableStateOf(false) }
    if (showRfidDialog) {
        AlertDialog(
            onDismissRequest = {
                showRfidDialog = false
                navController.popBackStack()
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    GradientButton(
                        text = "OK",
                        onClick = {
                            showRfidDialog = false
                            navController.popBackStack()
                        }
                    )
                }
            },
            title = { Text("Missing Data", fontFamily = poppins, fontSize = 18.sp) },
            text = {
                Text(
                    "RFID sheet not uploaded. Please contact administrator.",
                    fontFamily = poppins,
                    fontSize = 14.sp
                )
            }
        )
    }
    val _isLoadingCommon = MutableStateFlow(false)
    val isLoadingCommon by _isLoadingCommon.collectAsState()
    val _isLoadingUI = MutableStateFlow(false)
    val isLoadingUI by _isLoadingUI.collectAsState()
    val isLoading by productListViewModel.isLoading.collectAsState()
    val isLoadingAllItems by productListViewModel.isLoadingAllItems.collectAsState()
    val allItems by productListViewModel.productList.collectAsState(initial = emptyList())

    val userPreferences = UserPreferences.getInstance(context)
    val savedLang = userPreferences.getAppLanguage().ifBlank { "en" }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = currentLocales[0]?.language ?: savedLang
    val localizedContext = LocaleHelper.applyLocale(context, currentLang)

    // ✅ Performance logging for initial load
    LaunchedEffect(allItems.size) {
        if (allItems.isNotEmpty()) {
            Log.d("ScanDisplayScreen", "🚀 [PERF] allItems loaded: ${allItems.size} items")
        }
    }

    LaunchedEffect(allItems) {
        if (allItems.isNotEmpty()) {
            bulkViewModel.preloadFilters(allItems)
        }
    }

    // incoming filter from previous screen (branch/box/counter/exhibition)
    val filterTypeName = navController.previousBackStackEntry
        ?.savedStateHandle
        ?.get<String>("filterType")
    val filterValue = navController.previousBackStackEntry
        ?.savedStateHandle
        ?.get<String>("filterValue")

    LaunchedEffect(filterTypeName, filterValue) {
        if (!filterTypeName.isNullOrEmpty() && !filterValue.isNullOrEmpty()) {
            bulkViewModel.setFilteredItemsByType(filterTypeName, filterValue)
        }
    }




    // ✅ Optimized: Use derivedStateOf and sequence for lazy evaluation, returning a Sequence
    val navFilteredItems by remember(allItems, filterTypeName, filterValue) {
        derivedStateOf {
            val filterStartTime = System.currentTimeMillis()
            val sequence = if (filterTypeName.isNullOrEmpty() || filterValue.isNullOrEmpty()) {
                allItems.asSequence()
            } else {
                when (filterTypeName.lowercase()) {
                    "box" -> allItems.asSequence().filter { it.boxName == filterValue }
                    "counter" -> allItems.asSequence().filter { it.counterName == filterValue }
                    "branch" -> allItems.asSequence().filter { it.branchName == filterValue }
                    "exhibition" -> allItems.asSequence()
                        .filter { it.branchType == filterTypeName && it.branchName == filterValue }
                    else -> allItems.asSequence()
                }
            }
            val filterTime = System.currentTimeMillis() - filterStartTime
            if (filterTime > 10) { // Only log if filtering takes significant time
                Log.d("ScanDisplayScreen", "🚀 [PERF] navFilteredItems (sequence) computed in: ${filterTime}ms")
            }
            sequence // Return a Sequence directly
        }
    }
    /*  remember(allItems, filterTypeName, filterValue) {
          if (filterTypeName.isNullOrEmpty() || filterValue.isNullOrEmpty()) {
              allItems
          } else {
              when (filterTypeName.lowercase()) {
                  "box" -> allItems.filter { it.boxName == filterValue }
                  "counter" -> allItems.filter { it.counterName == filterValue }
                  "branch" -> allItems.filter { it.branchName == filterValue }
                  "branch" -> allItems.filter { it.branchName == filterValue }
                  "exhibition" -> allItems.filter { it.branchType == filterTypeName && it.branchName == filterValue }
                  else -> allItems
              }
          }
      }*/

    LaunchedEffect(emailStatus) {
        emailStatus?.let { status ->

            if (status == "success") {
                Toast.makeText(context,
                    localizedContext.getString(R.string.email_sent_successfully), Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, status, Toast.LENGTH_LONG).show()
            }

            // ✅ VERY IMPORTANT
            scanDisplayViewModel.clearEmailStatus()
        }
    }


    // Multi-select filters
    val selectedCategories = remember { mutableStateListOf<String>() }
    val selectedProducts = remember { mutableStateListOf<String>() }
    val selectedDesigns = remember { mutableStateListOf<String>() }


// stable snapshot keys
    val selectedCategoriesKey = selectedCategories.toList()
    val selectedProductsKey = selectedProducts.toList()
    val selectedDesignsKey = selectedDesigns.toList()


    // ✅ Optimized: Use derivedStateOf and cache results
    val allCategories by remember(navFilteredItems) {
        derivedStateOf {
            navFilteredItems
                .mapNotNull { it.category }
                .distinct()
                .sorted()
                .toList()
        }
    }

    val allProducts by remember(navFilteredItems, selectedCategoriesKey) {
        derivedStateOf {
            if (selectedCategoriesKey.isEmpty()) {
                navFilteredItems
                    .mapNotNull { it.productName }
                    .distinct()
                    .sorted()
                    .toList()
            } else {
                val categorySet = selectedCategoriesKey.toSet()
                navFilteredItems
                    .filter { it.category in categorySet }
                    .mapNotNull { it.productName }
                    .distinct()
                    .sorted()
                    .toList()
            }
        }
    }

    val allDesigns by remember(navFilteredItems, selectedCategoriesKey, selectedProductsKey) {
        derivedStateOf {
            val categorySet = if (selectedCategoriesKey.isEmpty()) null else selectedCategoriesKey.toSet()
            val productSet = if (selectedProductsKey.isEmpty()) null else selectedProductsKey.toSet()

            navFilteredItems
                .filter { categorySet == null || it.category in categorySet }
                .filter { productSet == null || it.productName in productSet }
                .mapNotNull { it.design }
                .distinct()
                .sorted()
                .toList()
        }
    }


    var currentLevel by rememberSaveable { mutableStateOf("Category") }
    var currentCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var currentProduct by rememberSaveable { mutableStateOf<String?>(null) }
    var currentDesign by rememberSaveable { mutableStateOf<String?>(null) }

    val scannedFiltered by bulkViewModel.scannedFilteredItems

    //val matchedEpcs by bulkViewModel.matchedEpcSet.collectAsState(initial = emptySet())
    val matchedEpcs by bulkViewModel.matchedEpcSet.collectAsState(initial = emptySet())

    // ✅ Optimized: Use sequence for lazy evaluation // Commented
    /*val summaryItems by remember(navFilteredItems, matchedEpcs) {
        derivedStateOf {
            navFilteredItems.asSequence()
                .map { original ->
                    val keyEpc = original.epc?.trim()?.uppercase()
                    val status =
                        if (keyEpc != null && matchedEpcs.contains(keyEpc)) "Matched" else "Unmatched"
                    original.copy(scannedStatus = status)
                }
                .toList()
        }
    }*/

    var showMenu by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var filterType by remember { mutableStateOf("Category") }

    var selectedMenu by rememberSaveable { mutableStateOf(MENU_ALL) }


    var selectedItem by remember { mutableStateOf<BulkItem?>(null) }
    var showItemDialog by remember { mutableStateOf(false) }

    var selectedPower by remember { mutableIntStateOf(10) }

    LaunchedEffect(Unit) {
        selectedPower = UserPreferences.getInstance(context).getInt(
            UserPreferences.KEY_INVENTORY_COUNT,
            10
        )
    }
    var _isResetting by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(false) }
    var showEmailDialog by remember { mutableStateOf(false) }
    var savedEmails by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedEmail by remember { mutableStateOf<String?>(null) }
    var newEmail by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }
    val effectiveMatchedSet by remember(isScanning, scannedFiltered, matchedEpcs) {
        derivedStateOf {
            if (isScanning) {
                scannedFiltered
                    .mapNotNull { it.tagKey() }   // EPC/RFID normalize
                    .toSet()
            } else {
                matchedEpcs
            }
        }
    }


    LaunchedEffect(Unit) {
        savedEmails = scanDisplayViewModel.getAllEmails()
    }

    LaunchedEffect(key1 = _isResetting) {
        if (_isResetting) {
            delay(1000L) // Add your desired delay here
            _isResetting = false
        }
    }

    //  val scannedFiltered by bulkViewModel.scannedFilteredItems
    //val matchedEpcs by bulkViewModel.matchedEpcSet.collectAsState(initial = emptySet())
    val currentPage by bulkViewModel.currentPage.collectAsState()
    val pageSize by bulkViewModel.pageSize.collectAsState()
    val totalItems by bulkViewModel.totalItems.collectAsState()
    val isLoadingPage by bulkViewModel.isLoadingPage.collectAsState()

    // ✅ show toast ONLY once
    LaunchedEffect(successMsg) {
        successMsg?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            scanDisplayViewModel.clearMessages()
        }
    }

    LaunchedEffect(error) {
        error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            // scanDisplayViewModel.clearError()
        }
    }

    // scopeItems overlay scanned status on filtered base set
    /*   val scopeItems by remember(
           navFilteredItems,
           selectedCategoriesKey,
           selectedProductsKey,
           selectedDesignsKey,
           scannedFiltered
       ) {
           derivedStateOf {
               if (navFilteredItems.isEmpty()) {
                   emptyList()
               } else {
                   navFilteredItems.mapNotNull { original ->
                       val keyEpc = original.epc?.trim()?.uppercase()
                       val status = if (keyEpc != null && matchedEpcs.contains(keyEpc)) "Matched" else "Unmatched"
                       val withScan = original.copy(scannedStatus = status)
                       if ((selectedCategoriesKey.isEmpty() || withScan.category in selectedCategoriesKey) &&
                           (selectedProductsKey.isEmpty() || withScan.productName in selectedProductsKey) &&
                           (selectedDesignsKey.isEmpty() || withScan.design in selectedDesignsKey)
                       ) withScan else null
                   }
               }
           }
       }*/
    // New: scannedItemsSequence is a lazy sequence of ScannedBulkItem
    val scannedItemsSequence by remember(
        navFilteredItems, // Now a Sequence<BulkItem>
        selectedCategoriesKey,
        selectedProductsKey,
        selectedDesignsKey,
        matchedEpcs
    ) {
        derivedStateOf {
            val scopeStartTime = System.currentTimeMillis()
            val categorySet = if (selectedCategoriesKey.isEmpty()) null else selectedCategoriesKey.toSet()
            val productSet = if (selectedProductsKey.isEmpty()) null else selectedProductsKey.toSet()
            val designSet = if (selectedDesignsKey.isEmpty()) null else selectedDesignsKey.toSet()

            val sequence = navFilteredItems.asSequence() // Use the sequence from navFilteredItems
                .filter { categorySet == null || it.category in categorySet }
                .filter { productSet == null || it.productName in productSet }
                .filter { designSet == null || it.design in designSet }
                .map { original ->
                    val keyEpc = original.epc?.trim()?.uppercase()
                    val status = if (keyEpc != null && matchedEpcs.contains(keyEpc)) "Matched" else "Unmatched"
                    ScannedBulkItem(original, status) // Create ScannedBulkItem with derived status
                }

            val scopeTime = System.currentTimeMillis() - scopeStartTime
            if (scopeTime > 100) {
                Log.d("ScanDisplayScreen", "🚀 [PERF] scannedItemsSequence computed in: ${scopeTime}ms")
            }
            sequence // Return a Sequence directly
        }
    }
    /*on back*/
    BackHandler {

        // 🔹 CASE 1: If user is in Matched / Unmatched / Resume mode
        if (selectedMenu != MENU_ALL) {

            selectedMenu = MENU_ALL
            bulkViewModel.clearStickyUnmatched()

            // Reset drill-down completely
            currentLevel = "Category"
            currentCategory = null
            currentProduct = null
            currentDesign = null

            selectedCategories.clear()
            selectedProducts.clear()
            selectedDesigns.clear()

            return@BackHandler
        }

        // 🔹 CASE 2: Drill-down back navigation
        when (currentLevel) {

            "DesignItems" -> {
                currentLevel = "Design"
                selectedDesigns.clear()
                return@BackHandler
            }

            "Design" -> {
                currentLevel = "Product"
                selectedProducts.clear()
                return@BackHandler
            }

            "Product" -> {
                currentLevel = "Category"
                selectedCategories.clear()
                return@BackHandler
            }
        }

        // 🔹 CASE 3: Exit screen
        // stop scanning — called directly (not in a coroutine) so it completes before onBack()
        // fires. stopScanningAndCompute() has no suspend points so this is safe on the main thread.
        bulkViewModel.stopScanningAndCompute()

        // cancel upload
        scanDisplayViewModel.clearMessages()

        // reset email state
        scanDisplayViewModel.clearEmailStatus()

        // reset UI states
        isSending = false
        showEmailDialog = false
        onBack()
    }

    fun getToolbarTitle(): String {
        return when (selectedMenu) {
            MENU_MATCHED ->localizedContext.getString(R.string.matched_items)
            MENU_UNMATCHED -> localizedContext.getString(R.string.unmatched_items)
            MENU_UNLABELLED -> localizedContext.getString(R.string.unlabelled_items)
            MENU_RESUME -> localizedContext.getString(R.string.resume_scan)
            else -> filterValue ?: localizedContext.getString(R.string.inventory)
        }
    }
    fun handleBackPress() {
        // 🔹 CASE 1: Matched / Unmatched / Unlabelled — return to main list immediately
        // (must match BackHandler order; toolbar was slow because drill-down ran first)
        if (selectedMenu != MENU_ALL) {
            selectedMenu = MENU_ALL
            bulkViewModel.clearStickyUnmatched()

            currentLevel = "Category"
            currentCategory = null
            currentProduct = null
            currentDesign = null

            selectedCategories.clear()
            selectedProducts.clear()
            selectedDesigns.clear()
            return
        }

        // 🔹 CASE 2: Drill-down back (main inventory only)
        when (currentLevel) {
            "DesignItems" -> {
                currentLevel = "Design"
                selectedDesigns.clear()
                return
            }
            "Design" -> {
                currentLevel = "Product"
                selectedProducts.clear()
                return
            }
            "Product" -> {
                currentLevel = "Category"
                selectedCategories.clear()
                return
            }
        }

        // 🔹 CASE 3: Exit screen
        bulkViewModel.stopScanningAndCompute()
        onBack()
    }


    // displayItems respects selectedMenu and sticky unmatched ids (existing logic preserved)
    /*  val displayItems = remember(scopeItems, selectedMenu, bulkViewModel.filteredUnmatchedIds.collectAsState().value) {
          if (scopeItems.isEmpty()) {
              emptyList()
          } else {
              when (selectedMenu) {
                  MENU_MATCHED -> scopeItems.filter { it.scannedStatus == "Matched" }
                  MENU_UNMATCHED -> {
                      val unmatchedNow = scopeItems.filter { it.scannedStatus == "Unmatched" }
                      val sticky = scopeItems.filter {
                          val id = it.epc?.trim()?.uppercase()
                          id != null && bulkViewModel.filteredUnmatchedIds.value.contains(id)
                      }
                      (unmatchedNow + sticky).distinctBy { it.epc }
                  }
                  else -> scopeItems
              }
          }
      }
  */

    val stickyUnmatchedIds by bulkViewModel.filteredUnmatchedIds.collectAsState()

    val _asyncDisplayItems = remember { MutableStateFlow<List<ScannedBulkItem>>(emptyList()) }
    val displayItems by _asyncDisplayItems.collectAsState()
    var isComputingDisplay by remember { mutableStateOf(false) }
    var previousSelectedMenu by remember { mutableStateOf(selectedMenu) }

    LaunchedEffect(scannedItemsSequence, selectedMenu, stickyUnmatchedIds) {
        val menuChanged = previousSelectedMenu != selectedMenu
        previousSelectedMenu = selectedMenu

        // Debounce only while scanning on main list — not when switching matched/unmatched/back
        if (isScanning && selectedMenu == MENU_ALL && !menuChanged) {
            delay(700)
        }

        isComputingDisplay = true
        withContext(Dispatchers.Default) {
            try {
                val currentScannedSequence = scannedItemsSequence

                val computedItems = if (!currentScannedSequence.iterator().hasNext()) {
                    emptyList()
                } else {
                    when (selectedMenu) {
                        MENU_MATCHED ->
                            currentScannedSequence
                                .filter { it.currentScannedStatus == "Matched" }
                                .toList()
                        MENU_UNMATCHED -> {
                            val stickySet = stickyUnmatchedIds.toHashSet()
                            val unmatchedNow = currentScannedSequence
                                .filter { it.currentScannedStatus == "Unmatched" }
                                .toList()
                            val stickyExtra = currentScannedSequence
                                .filter { it.epc?.trim()?.uppercase()?.let { e -> stickySet.contains(e) } == true }
                                .toList()
                            // Use LinkedHashSet for O(1) dedup while preserving order
                            val seen = LinkedHashSet<String?>(unmatchedNow.size + stickyExtra.size)
                            (unmatchedNow + stickyExtra).filter { seen.add(it.epc?.trim()?.uppercase()) }
                        }
                        else -> currentScannedSequence.toList()
                    }
                }
                _asyncDisplayItems.value = computedItems
            } catch (e: Exception) {
                Log.e("ScanDisplayScreen", "Error computing display items: ${e.message}")
            }
        }
        isComputingDisplay = false
    }


    // Optimized: Use displayItems which is already materialized instead of calling toList() twice
    val allMatched by remember(displayItems) {
        derivedStateOf { displayItems.isNotEmpty() && displayItems.all { it.currentScannedStatus == "Matched" } }
    }

    val activity = LocalContext.current as? MainActivity

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, activity) {
        val listener = object : ScanKeyListener {
            override fun onBarcodeKeyPressed() {
                bulkViewModel.startBarcodeScanning(context)
            }

            override fun onRfidKeyPressed() {
                if (!isScanning) {
                    if (allMatched) {
                        Toast.makeText(
                            context,
                            localizedContext.getString(R.string.all_items_matched_scan_stopped),
                            Toast.LENGTH_SHORT
                        ).show()
                        return
                    }
                    isScanning = true
                    scope.launch(Dispatchers.Default) {
                      // vasanti  bulkViewModel.setFilteredItems(scannedItemsSequence.map { it.originalBulkItem }.toList())
                        bulkViewModel.setFilteredItems(
                            displayItems.map { it.originalBulkItem }
                        )
                    }
                    bulkViewModel.startScanningInventory(selectedPower)
                } else {
                    isScanning = false
                    bulkViewModel.stopScanningAndCompute()
                }
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> activity?.registerScanKeyListener(listener)
                Lifecycle.Event.ON_PAUSE -> activity?.unregisterScanKeyListener()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        activity?.registerScanKeyListener(listener)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            activity?.unregisterScanKeyListener()
        }
    }

    /*LaunchedEffect(isScanning, allMatched) {
        if (isScanning && allMatched) {
            bulkViewModel.stopScanningAndCompute()
            isScanning = false
            Toast.makeText(context, "All items matched. Scan stopped.", Toast.LENGTH_SHORT).show()
        }
    }*/

    LaunchedEffect(Unit) {
        snapshotFlow { isScanning to allMatched }
            .collect { (isScanningValue, allMatchedValue) ->

                if (isScanningValue && allMatchedValue) {

                   /* vasanti
                   currentCategory = null
                    currentProduct = null
                    currentDesign = null
                    selectedCategories.clear()
                    selectedProducts.clear()
                    selectedDesigns.clear()*/

                    bulkViewModel.stopScanningAndCompute()
                    isScanning = false

                    Toast.makeText(context,
                        localizedContext.getString(R.string.all_items_matched_scan_stopped), Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Optimized: Use displayItems instead of calling toList() on sequence
    LaunchedEffect(isScanning, displayItems.size) {
        if (isScanning && displayItems.isEmpty()) {
            bulkViewModel.stopScanningAndCompute()
            isScanning = false
        }
    }

    DisposableEffect(Unit) { onDispose { bulkViewModel.stopScanningAndCompute()

    } }

    // Optimized: Use allMatched derived state instead of calling toList() twice
    // This LaunchedEffect is now redundant since we have snapshotFlow above
    // Commenting out to avoid duplicate logic
    /*LaunchedEffect(scannedItemsSequence) {
        if (isScanning && scannedItemsSequence.toList().isNotEmpty() && scannedItemsSequence.toList().all { it.currentScannedStatus == "Matched" }) {

            currentCategory = null
            currentProduct = null
            currentDesign = null
            selectedCategories.clear()
            selectedProducts.clear()
            selectedDesigns.clear()


            bulkViewModel.stopScanningAndCompute()
            Toast.makeText(context, "All items matched. Scan stopped.", Toast.LENGTH_SHORT).show()

            isScanning = false
        }
    }*/

    val employee = UserPreferences.getInstance(context).getEmployee(Employee::class.java)
    LaunchedEffect(Unit) {
        val initStartTime = System.currentTimeMillis()
        Log.d("ScanDisplayScreen", "🚀 [PERF] Starting screen initialization...")

        employee?.clientCode?.let {
            val dropdownStartTime = System.currentTimeMillis()
            singleProductViewModel.fetchAllDropdownData(ClientCodeRequest(it))
            Log.d("ScanDisplayScreen", "🚀 [PERF] Dropdown data fetch initiated in: ${System.currentTimeMillis() - dropdownStartTime}ms")
        }

        // ✅ Initialize with pagination - load first page only
        scope.launch(Dispatchers.IO) {
            val paginationStartTime = System.currentTimeMillis()
            bulkViewModel.loadTotalCount()
            val countTime = System.currentTimeMillis() - paginationStartTime
            Log.d("ScanDisplayScreen", "🚀 [PERF] Total count loaded in: ${countTime}ms")

            bulkViewModel.loadPage(0) // Load first page
            val pageLoadTime = System.currentTimeMillis() - paginationStartTime - countTime
            Log.d("ScanDisplayScreen", "🚀 [PERF] First page loaded in: ${pageLoadTime}ms")

            val totalInitTime = System.currentTimeMillis() - initStartTime
            Log.d("ScanDisplayScreen", "🚀 [PERF] Screen initialization completed in: ${totalInitTime}ms")
        }
    }

    fun writeToFile(context: Context, json: String): File {
        val file = File(context.getExternalFilesDir(null), "stock_items.json")
        file.writeText(json)
        return file
    }

    // Avoid feeding UI projection back into VM during scanning to reduce churn

    Scaffold(
        topBar = {
            filterValue?.let {
                GradientTopBar(
                    title =getToolbarTitle(),
                    navigationIcon = {
                        IconButton(onClick = { /*shouldNavigateBack = true*/
                            handleBackPress()}) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    showCounter = true,
                    selectedCount = selectedPower,
                    onCountSelected = { selectedPower = it },
                    titleTextSize = 20.sp
                )
            }
        },
        bottomBar = {
            Column {

                //  SummaryRow(currentLevel, displayItems, selectedMenu)
                SummaryRow(currentLevel, displayItems, selectedMenu,localizedContext)
                ScanBottomBarInventory(
                    onSave = { /* save */
                        _isLoadingCommon.value = true
                        scope.launch(Dispatchers.IO) {
                            val clientCode = employee?.clientCode       // 🔁 replace

                            val finalItems: List<BulkItem> =
                                scannedItemsSequence
                                    .map { scanned ->

                                        val key = scanned.originalBulkItem.tagKey()
                                        val isMatched =
                                            key != null && effectiveMatchedSet.contains(key)

                                        scanned.originalBulkItem.copy(
                                            isScanned = isMatched,
                                            scannedStatus = if (isMatched) "Matched" else "Unmatched"
                                        )
                                    }
                                    .toList()


                            bulkViewModel.saveScanResultLocally(finalItems)
                            val scannedItems = withContext(Dispatchers.Default) { buildItemsForUpload(scannedItemsSequence.toList()) } // Pass the materialized list here

                            Log.d("SAVE_DEBUG", "scopeItems=${scannedItemsSequence.count()} " +
                                    "matched=${scannedItemsSequence.count { it.currentScannedStatus == "Matched" }} " +
                                    "unmatched=${scannedItemsSequence.count { it.currentScannedStatus == "Unmatched" }}")

                            Log.d("SAVE_DEBUG", "payload=${scannedItems.size} " +
                                    "payloadMatched=${scannedItems.count { it.status == "Matched" }} " +
                                    "payloadUnmatched=${scannedItems.count { it.status == "Unmatched" }}")
                            //val scannedItems = buildItemsForUpload(scopeItems, matchedEpcs)
                            //val scannedItems: List<Item> = buildItemsForUpload(scopeItems)// 🔁 replace with your actual list
                            //val json = Gson().toJson(scannedItems)
                            val root = JsonObject()

                            root.addProperty("ClientCode", clientCode)
                            root.add("Items", Gson().toJsonTree(scannedItems))
                            val json = root.toString()

                            //Log.d("JSON_BODY", json)
                            val file = writeToFile(context, json)
                            Log.d("DEBUG_FILE", "Saved at: ${file.absolutePath}")
                            //scanDisplayViewModel.uploadStockVerification(clientCode.toString(), scannedItems, batchSize = 2000)
                            scanDisplayViewModel.uploadStockVerificationFile(clientCode.toString(),file)
                            withContext(Dispatchers.Main) {
                                _isLoadingCommon.value = false
                            }
                        }
                    },
                    onList = { showMenu = true },
                    onScan = {
                        if (!isScanning) {
                            if (allMatched) {
                                Toast.makeText(
                                    context,
                                    localizedContext.getString(R.string.all_items_matched_scan_stopped),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                isScanning = true
                                // bulkViewModel.resetScanResults()
                               //vasanti bulkViewModel.setFilteredItems(scannedItemsSequence.map { it.originalBulkItem }.toList())   // ✅ only current scope
                                bulkViewModel.setFilteredItems(
                                    displayItems.map { it.originalBulkItem }
                                )
                                bulkViewModel.startScanningInventory(selectedPower)
                            }
                        } else {
                            isScanning = false
                            bulkViewModel.stopScanningAndCompute()
                        }
                    },
                    onEmail = {
                        showEmailDialog = true

                    },
                    onReset = {
                        Log.d("ScanDisplayScreen", "Calling")
                        if (!_isResetting) {
                            Log.d("ScanDisplayScreen", "Called")
                            // Instant UI feedback — update state on main thread immediately
                            _isResetting = true
                            isScanning = false
                            selectedCategories.clear()
                            selectedProducts.clear()
                            selectedDesigns.clear()
                            selectedMenu = MENU_ALL
                            currentLevel = "Category"
                            currentCategory = null
                            currentProduct = null
                            currentDesign = null
                            // resetForDisplay() clears all data synchronously then fires
                            // hardware stop in its own coroutine — safe to call on main thread
                            bulkViewModel.resetForDisplay()
                            Log.d("ScanDisplayScreen", "Completed")
                        }
                    },
                    isScanning = isScanning

                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(Modifier.padding(innerPadding)) {
                FilterRow(
                    selectedCategories,
                    selectedProducts,
                    selectedDesigns,
                    onCategoryClick = {
                        filterType = "Category"
                        // entering filter dialog resets drill-down navigation
                        currentLevel = "Category"
                        currentCategory = null
                        currentProduct = null
                        currentDesign = null
                        showDialog = true
                    },
                    onProductClick = {
                        if (selectedCategories.isNotEmpty() || allCategories.isNotEmpty()) {
                            filterType = "Product"
                            // entering filter dialog resets drill-down navigation
                            currentLevel = "Category"
                            currentCategory = null
                            currentProduct = null
                            currentDesign = null
                            showDialog = true
                        } else Toast.makeText(context, "Select category first", Toast.LENGTH_SHORT)
                            .show()
                    },
                    onDesignClick = {
                        if (selectedProducts.isNotEmpty() || allProducts.isNotEmpty()) {
                            filterType = "Design"
                            currentLevel = "Category"
                            currentCategory = null
                            currentProduct = null
                            currentDesign = null
                            showDialog = true
                        } else Toast.makeText(context, "Select product first", Toast.LENGTH_SHORT)
                            .show()
                    }
                )

                TableHeader(currentLevel,localizedContext)

                // ✅ Optimized: Pre-compute all grouped data and filter sets outside LazyColumn
                val categorySetForGrouping = remember(selectedCategories) {
                    if (selectedCategories.isEmpty()) null else selectedCategories.toSet()
                }
                val productSetForGrouping = remember(selectedProducts) {
                    if (selectedProducts.isEmpty()) null else selectedProducts.toSet()
                }
                val designSetForGrouping = remember(selectedDesigns) {
                    if (selectedDesigns.isEmpty()) null else selectedDesigns.toSet()
                }

                // Pre-compute grouped data for each level
                val categoryGrouped = remember(displayItems) {
                    displayItems.groupBy { it.category ?: "Unknown" }
                }
                val productGrouped = remember(displayItems, categorySetForGrouping) {
                    displayItems.asSequence()
                        .filter { categorySetForGrouping == null || it.category in categorySetForGrouping }
                        .groupBy { it.productName ?: "Unknown" }
                }
                val designGrouped = remember(displayItems, categorySetForGrouping, productSetForGrouping) {
                    displayItems.asSequence()
                        .filter { categorySetForGrouping == null || it.category in categorySetForGrouping }
                        .filter { productSetForGrouping == null || it.productName in productSetForGrouping }
                        .groupBy { it.design ?: "Unknown" }
                }
                val designItemsList = remember(displayItems, categorySetForGrouping, productSetForGrouping, designSetForGrouping) {
                    displayItems.asSequence()
                        .filter { categorySetForGrouping == null || it.category in categorySetForGrouping }
                        .filter { productSetForGrouping == null || it.productName in productSetForGrouping }
                        .filter { designSetForGrouping == null || it.design in designSetForGrouping }
                        .toList()
                }

                // Progress bar — visible while display items are being computed on Dispatchers.Default
                if (isComputingDisplay) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF6200EE),
                        trackColor = Color(0xFFE0E0E0)
                    )
                }

                // ---------- L A Z Y   C O L U M N   (drill-down + multi-select friendly) ----------
                LazyColumn(modifier = Modifier.weight(1f)) {
                    when (currentLevel) {
                        "Category" -> {
                            if (displayItems.isNotEmpty()) {
                                categoryGrouped.forEach { (label, items) ->
                                    item(key = "category_$label") {
                                        TableDataRow(TableRow(label, items), currentLevel) {
                                            // drill down to product for the chosen category (single-select drill)
                                            currentCategory = label
                                            selectedCategories.clear()
                                            selectedCategories.add(label)
                                            selectedProducts.clear()
                                            selectedDesigns.clear()
                                            currentLevel = "Product"
                                        }
                                    }
                                }
                            } else if (isLoadingAllItems || allItems.isNotEmpty()) {
                                // Show loading indicator while data is being processed
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }

                        "Product" -> {
                            if (displayItems.isNotEmpty()) {
                                productGrouped.forEach { (label, items) ->
                                    item(key = "product_$label") {
                                        TableDataRow(TableRow(label, items), currentLevel) {
                                            // when clicking product row, drill to design level
                                            currentProduct = label
                                            if (!selectedProducts.contains(label)) selectedProducts.add(
                                                label
                                            )
                                            selectedDesigns.clear()
                                            currentLevel = "Design"
                                        }
                                    }
                                }
                            } else if (isLoadingAllItems || allItems.isNotEmpty()) {
                                // Show loading indicator while data is being processed
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }

                        "Design" -> {
                            if (displayItems.isNotEmpty()) {
                                designGrouped.forEach { (label, items) ->
                                    item(key = "design_$label") {
                                        TableDataRow(TableRow(label, items), currentLevel) {
                                            currentDesign = label
                                            if (!selectedDesigns.contains(label)) selectedDesigns.add(
                                                label
                                            )
                                            currentLevel = "DesignItems"
                                        }
                                    }
                                }
                            } else if (isLoadingAllItems || allItems.isNotEmpty()) {
                                // Show loading indicator while data is being processed
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }

                        "DesignItems" -> {
                            // Use pagination for large lists
                            if (displayItems.isNotEmpty()) {
                                items(
                                    designItemsList,
                                    key = {
                                        it.epc ?: it.itemCode ?: "${it.design}_${it.originalBulkItem.bulkItemId}"
                                    }) { item ->
                                    DesignItemRow(item) { clickedItem ->
                                        selectedItem = clickedItem.originalBulkItem
                                        showItemDialog = true
                                    }
                                }
                            } else if (isLoadingAllItems || allItems.isNotEmpty()) {
                                // Show loading indicator while data is being processed
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }

                            // Load more items when reaching the end (for pagination)
                            if (designItemsList.size >= pageSize && !isLoadingPage) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                scope.launch {
                                                    bulkViewModel.loadNextPage()
                                                }
                                            }
                                        ) {
                                            Text(localizedContext.getString(R.string.load_more_items))
                                        }
                                    }
                                }
                            }

                            // Show loading indicator
                            if (isLoadingPage) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }
                    }
                }
                // -----------------------------------------------------------------------------------
            }
        }
        // 🔹 Loader overlay (TOP LEVEL) - reuse existing shared loader (productListViewModel / bulkViewModel)

        val bulkIsLoading by bulkViewModel.isLoading.collectAsState()
        val showLoader = isLoading || bulkIsLoading || isLoadingAllItems || isLoadingUI || isLoadingCommon

        if (showLoader) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        when {
                            isLoadingAllItems -> "Loading all items... (${allItems.size} loaded)"
                            bulkIsLoading ->localizedContext.getString(R.string.please_wait)
                            isLoadingCommon ->localizedContext.getString(R.string.please_wait)
                            else -> localizedContext.getString(R.string.loading_products)
                        },
                        color = Color.White,
                        fontFamily = poppins
                    )
                }
            }
        }
    }
    /*if (showEmailDialog) {
            AlertDialog(
                onDismissRequest = { showEmailDialog = false },
                title = { Text("Send Report", fontFamily = poppins) },
                text = {
                    Column {
                        if (savedEmails.isNotEmpty()) {
                            Text("Saved Emails:", fontFamily = poppins)
                            savedEmails.forEach { email ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedEmail = email }
                                        .background(
                                            if (selectedEmail == email) Color.LightGray else Color.Transparent
                                        )
                                        .padding(8.dp)
                                ) {
                                    Text(email, fontFamily = poppins)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                        OutlinedTextField(
                            value = newEmail,
                            onValueChange = { newEmail = it },
                            label = { Text("Add New Email", fontFamily = poppins) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val finalEmail = when {
                            newEmail.isNotBlank() -> newEmail
                            !selectedEmail.isNullOrBlank() -> selectedEmail
                            else -> null
                        }
                        if (finalEmail.isNullOrBlank()) {
                            Toast.makeText(
                                context,
                                "Please enter or select an email",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }
                        _isLoadingCommon.value = true
                        scope.launch(Dispatchers.IO) {
                            // save new email

                            if (newEmail.isNotBlank()) {
                                scanDisplayViewModel.saveEmail(newEmail)
                                savedEmails = scanDisplayViewModel.getAllEmails()
                                selectedEmail = newEmail
                                newEmail = ""
                            }
                            try {
                                *//*val summaryList = scanDisplayViewModel.buildSummary(displayItems)
                            val (matched, unmatched) = scanDisplayViewModel.buildDetailedLists(
                                displayItems
                            )

                            val pdfFile = scanDisplayViewModel.generateScanReportPdf(
                                context,
                                summaryList,
                                matched,
                                unmatched
                            )*//*

                            try {
                                val summary = scanDisplayViewModel.buildSummary(displayItems)
                                val (matched, unmatched) = scanDisplayViewModel.buildDetailedLists(
                                    displayItems
                                )

                                *//*val pdfFile = scanDisplayViewModel.generateScanReportPdf(
                                    context = context,
                                    allItemsSummary = summary,
                                    matched
                                )*//*

                                val pdfFile = scanDisplayViewModel.generateScanReportExcel(
                                    context = context,
                                    allItemsSummary = summary,
                                    matched,
                                    unmatched
                                )

                                Toast.makeText(context, "PDF Created: ${pdfFile.name}", Toast.LENGTH_LONG).show()


                                println("=== Trying to send email to $finalEmail ===")

                                scanDisplayViewModel.sendEmailHostinger(
                                    sendEmail = "android@loyalstring.com",
                                    sendPass = "Loyal@123",
                                    recipients = listOf(finalEmail),
                                    subject = "Inventory Scan Report",
                                    body = "<h2>Here is your scan report</h2><p>Details attached.</p>",
                                    type = "text/html",
                                    attachments = mapOf(pdfFile.name to pdfFile.absolutePath)
                                )

                                withContext(Dispatchers.Main) {
                                    _isLoadingCommon.value = false
                                }

                            } catch (e: Exception) {
                                Toast.makeText(context, "PDF Error: ${e.message}", Toast.LENGTH_LONG).show()
                            }



                            println("=== Email sent successfully ===")

                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "Report sent to $finalEmail",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                        } catch (e: Exception) {
                            println("=== Email send failed: ${e.message} ===")
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG)
                                    .show()
                            }
                        }
                    }

                    // build lists

                    // showSuccessDialog = true


                    showEmailDialog = false
                }) {
                    Text("Send", fontFamily = poppins)
                }
            },
            dismissButton = {
                Button(onClick = { showEmailDialog = false }) {
                    Text("Cancel", fontFamily = poppins)
                }
            }
        )
    }*/



    if (showEmailDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSending) showEmailDialog = false },

            title = { Text("Send Report", fontFamily = poppins) },

            text = {
                Column {
                    if (savedEmails.isNotEmpty()) {
                        Text("Saved Emails:", fontFamily = poppins)
                        savedEmails.forEach { email ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !isSending) {
                                        selectedEmail = email
                                        newEmail = email // optional: auto fill
                                    }
                                    .background(
                                        if (selectedEmail == email) Color.LightGray else Color.Transparent
                                    )
                                    .padding(8.dp)
                            ) {
                                Text(email, fontFamily = poppins)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        enabled = !isSending,
                        label = { Text("Add New Email", fontFamily = poppins) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },

            confirmButton = {
                GradientButtonNew(
                    text = "Send",
                    enabled = true,
                    isLoading = isSending,
                    onClick = {
                        val finalEmail = when {
                            newEmail.isNotBlank() -> newEmail.trim()
                            !selectedEmail.isNullOrBlank() -> selectedEmail!!.trim()
                            else -> null
                        }

                        if (finalEmail.isNullOrBlank()) {
                            Toast.makeText(context, "Please enter or select an email", Toast.LENGTH_SHORT).show()
                            return@GradientButtonNew
                        }

                        // ✅ keep dialog open + show loader
                        isSending = true

                        // ✅ copy state now (important)
                        val itemsForReport = displayItems.toList()
                        val emailToSend = finalEmail
                        val newEmailToSave = newEmail.trim()

                        scope.launch {
                            try {
                                // ✅ allow UI to draw loader first
                                yield()

                                // save new email (IO)
                                if (newEmailToSave.isNotBlank() && !savedEmails.contains(newEmailToSave)) {
                                    withContext(Dispatchers.IO) { scanDisplayViewModel.saveEmail(newEmailToSave) }
                                    savedEmails = withContext(Dispatchers.IO) { scanDisplayViewModel.getAllEmails() }
                                    selectedEmail = newEmailToSave
                                    newEmail = ""
                                }

                                // build + generate file (IO)
                                val reportFile = withContext(Dispatchers.IO) {
                                    // Copy BulkItem with correct scannedStatus from ScannedBulkItem
                                    val bulkItemsWithStatus = itemsForReport.map {
                                        it.originalBulkItem.copy(scannedStatus = it.currentScannedStatus)
                                    }
                                    val summaryList = scanDisplayViewModel.buildSummary(bulkItemsWithStatus)
                                    val (matched, unmatched) = scanDisplayViewModel.buildDetailedLists(bulkItemsWithStatus)
                                    /*Log.d("matched_length", ""+summaryList.size)
                                    Log.d("matched_length", ""+matched.size)
                                    Log.d("matched_length", ""+unmatched.size)*/
                                    /*scanDisplayViewModel.generateScanReportExcel(
                                        context, summaryList, matched, unmatched
                                    )*/
                                    scanDisplayViewModel.generateScanReportCsv(
                                        context, summaryList, matched, unmatched
                                    )

                                }

                                // send email (IO)
                                withContext(Dispatchers.IO) {
                                    scanDisplayViewModel.sendEmailHostinger(
                                        sendEmail = "android@loyalstring.com",
                                        sendPass = "Loyal@123",
                                        recipients = listOf(emailToSend),
                                        subject = "Inventory Scan Report",
                                        body = "<h2>Here is your scan report</h2><p>Details attached.</p>",
                                        type = "text/html",
                                        attachments = mapOf(reportFile.name to reportFile.absolutePath)
                                    )
                                }

                                // Toast.makeText(context, "Report sent to $emailToSend", Toast.LENGTH_LONG).show()
                                showEmailDialog = false

                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isSending = false
                            }
                        }
                    }
                )
            },

            dismissButton = {
                GradientButtonNew(
                    text = "Cancel",
                    enabled = !isSending,
                    isLoading = false,
                    onClick = { showEmailDialog = false }
                )
            }
        )
    }



    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Success", fontFamily = poppins) },
            text = { Text("Email sent successfully!", fontFamily = poppins) },
            confirmButton = {
                GradientButton(
                    text = "OK",
                    onClick = { showSuccessDialog = false }
                )
            }
        )
    }

    // ---------- Filter Dialog ----------
    if (showDialog) {
        val items = when (filterType) {
            "Category" -> allCategories
            "Product" -> allProducts
            "Design" -> allDesigns
            else -> emptyList()
        }
        val selected = when (filterType) {
            "Category" -> selectedCategories
            "Product" -> selectedProducts
            "Design" -> selectedDesigns
            else -> mutableStateListOf()
        }

        FilterSelectionDialog(
            title = filterType,
            items = items,
            selectedItems = selected,
            onDismiss = { showDialog = false },
            onConfirm = {
                when (filterType) {
                    "Category" -> {
                        currentLevel = "Product"
                        currentCategory = null
                        selectedProducts.clear()
                        selectedDesigns.clear()
                    }
                    "Product" -> {
                        currentLevel = "Design"
                        currentProduct = null
                        selectedDesigns.clear()
                    }
                    "Design" -> {
                        currentLevel = "DesignItems"
                        currentDesign = null
                    }
                }

                // ❌ Don’t reset to allItems, just close dialog
                showDialog = false
            }

        )
    }

    // Menu (Matched/Unmatched/etc.)
    /*
        val summaryItems by remember(navFilteredItems, matchedEpcs) {
            derivedStateOf {
                navFilteredItems.map { original ->
                    val keyEpc = original.epc?.trim()?.uppercase()
                    val status =
                        if (keyEpc != null && matchedEpcs.contains(keyEpc)) "Matched" else "Unmatched"
                    original.copy(scannedStatus = status)
                }
            }
        }*/
    // Optimized: Use displayItems which is already materialized
    val matchedCount = remember(displayItems) { displayItems.count { it.currentScannedStatus == "Matched" } }
    val unmatchedCount = remember(displayItems) { displayItems.count { it.currentScannedStatus == "Unmatched" } }
    val totalCount = remember(displayItems) { displayItems.size }
    /*val matchedCount = remember(summaryItems) { summaryItems.count { it.scannedStatus == "Matched" } }
    val unmatchedCount = remember(summaryItems) { summaryItems.count { it.scannedStatus == "Unmatched" } }
    val totalCount = remember(summaryItems) { summaryItems.size }*/   // ✅ THIS LINE FIXED

    if (showMenu) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x80000000))
                .clickable { showMenu = false }
        ) {
            Surface(
                modifier = Modifier
                    .padding(top = 60.dp, bottom = 70.dp)
                    .width(180.dp)
                    .fillMaxHeight()
                    .align(Alignment.TopStart),
                shape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp),
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                VerticalMenu(
                    localizedContext = localizedContext,
                    matchedCount = matchedCount,
                    unmatchedCount = unmatchedCount,
                    totalCount = totalCount
                ) { menuItem ->
                    when (menuItem.title) {
                        localizedContext.getString(R.string.unmatched_items) -> {
                            scope.launch {
                                /* bulkViewModel.setLoading(true)
                                 delay(1000)
                                 bulkViewModel.loadUnmatchedFast(scopeItems)
                                 selectedMenu = MENU_UNMATCHED
                                 currentLevel = "DesignItems"*/
                                selectedMenu = MENU_UNMATCHED
                                currentLevel = "DesignItems"
                                /* currentLevel = "DesignItems"

                                 currentCategory = null
                                 currentProduct = null
                                 currentDesign = null
                                 selectedCategories.clear()
                                 selectedProducts.clear()
                                 selectedDesigns.clear()*/

                                // optional: sticky ids set (agar tumhe sticky behaviour chahiye)
                                val ids = scannedItemsSequence.asSequence()
                                    .filter { it.currentScannedStatus.equals("Unmatched", true) }
                                    .mapNotNull { it.epc?.trim()?.uppercase() }
                                    .distinct()
                                    .toList()
                                bulkViewModel.rememberUnmatchedIds(ids)
                            }

                            // Compute unmatched ids off the main thread to avoid freezing UI on large lists
                            /*bulkViewModel.setLoading(true)
                        scope.launch(Dispatchers.IO) {
                            val ids = withContext(Dispatchers.Default) {
                                scopeItems.asSequence()
                                    .filter { it.scannedStatus.equals("Unmatched", true) }
                                    .mapNotNull { it.epc?.trim()?.uppercase() }
                                    .distinct()
                                    .toList()
                            }
                            // Update ViewModel and UI on the main thread to avoid snapshot mutations from background
                            //bulkViewModel.rememberUnmatchedIds(ids) // Now updates _filteredUnmatchedIds directly on a background thread
                            withContext(Dispatchers.Main) { // Move UI updates back to Main thread
                                selectedMenu = MENU_UNMATCHED
                                // ✅ Always show items when unmatched mode
                                currentLevel = "DesignItems"
                                currentCategory = null
                                currentProduct = null
                                currentDesign = null

                                selectedCategories.clear()
                                selectedProducts.clear()
                                selectedDesigns.clear()
                                bulkViewModel.setLoading(false)
                            }
                        }*/

                        }

                        localizedContext.getString(R.string.matched_items) -> {
                            /* selectedMenu = MENU_MATCHED
                             bulkViewModel.clearStickyUnmatched()

                             // ✅ Always show items when matched mode
                             currentLevel = "DesignItems"
                             currentCategory = null
                             currentProduct = null
                             currentDesign = null

                             selectedCategories.clear()
                             selectedProducts.clear()
                             selectedDesigns.clear()*/
                            selectedMenu = MENU_MATCHED
                            currentLevel = "DesignItems"
                            bulkViewModel.clearStickyUnmatched()

                            /* currentLevel = "DesignItems"
                             currentCategory = null
                             currentProduct = null
                             currentDesign = null
                             selectedCategories.clear()
                             selectedProducts.clear()
                             selectedDesigns.clear()*/
                        }

                        localizedContext.getString(R.string.unlabelled_items) -> {
                            selectedMenu = MENU_UNLABELLED
                            currentLevel = "DesignItems"
                            bulkViewModel.clearStickyUnmatched()
                        }
                        // In ScanDisplayScreen
                        localizedContext.getString(R.string.search) -> {
                            scope.launch {
                                bulkViewModel.setLoading(true)
                                // Remove artificial delay
                                // delay(1000)

                                val unmatchedBulkItems = withContext(Dispatchers.Default) {
                                    // toBulkItem() mapping stays on Default — keeps main thread free
                                    scannedItemsSequence
                                        .filter { it.currentScannedStatus.equals("Unmatched", true) }
                                        .distinctBy { it.epc?.trim()?.uppercase() }
                                        .map { it.toBulkItem() }
                                        .toList()
                                }

                                if (unmatchedBulkItems.isNotEmpty()) {
                                    navController.currentBackStackEntry?.savedStateHandle?.set(
                                        "unmatchedItems",
                                        ArrayList(unmatchedBulkItems)
                                    )
                                    navController.navigate("search_screen/unmatched") {
                                        // This is the callback from SearchScreen
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                } else {
                                    navController.navigate("search_screen/normal") {
                                        launchSingleTop = true
                                        restoreState = true
                                    }


                                }
                                //delay(1000)
                                bulkViewModel.setLoading(false)
                                //showToast(context, "End")
                            }

                        }

                       /* "Resume Scan"->{
                            bulkViewModel.restoreScanFromSavedBulkItems(allItems)
                            selectedMenu = MENU_ALL
                            currentLevel = "DesignItems"

                            Toast.makeText(
                                context,
                                "Previous scan restored",
                                Toast.LENGTH_SHORT
                            ).show()
                        }*/
                        localizedContext.getString(R.string.resume_scan) -> {
                            scope.launch {

                                bulkViewModel. restoreScanFromDatabase()
                              //  bulkViewModel.restoreScanFromSavedBulkItems(allItems)

                                selectedMenu = MENU_RESUME
                                currentLevel = "DesignItems"

                                Toast.makeText(
                                    context,
                                    "Previous scan restored",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                    }

                    showMenu = false
                }
            }
        }
    }

    if (showItemDialog && selectedItem != null) {
        ItemDetailsDialog(item = selectedItem!!, onDismiss = { showItemDialog = false })
    }
    /*if (showEmailDialog) {
        AlertDialog(
            onDismissRequest = { showEmailDialog = false },
            title = { Text("Select or Add Email") },
            text = {
                Column {
                    if (savedEmails.isNotEmpty()) {
                        Text("Saved Emails:")
                        savedEmails.forEach { emailEntity ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(4.dp)
                                    .background(
                                        if (selectedEmail == emailEntity) Color.LightGray else Color.Transparent
                                    )
                                    .clickable {
                                        selectedEmail = emailEntity
                                        newEmail = emailEntity // ✅ populate text field
                                    }
                            ) {
                                Text(emailEntity, modifier = Modifier.padding(8.dp))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = newEmail,
                        onValueChange = { newEmail = it },
                        label = { Text("Add New Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                GradientButton(
                    text = "Send",
                    onClick = {
                        val finalEmail = when {
                            newEmail.isNotBlank() -> newEmail
                            !selectedEmail.isNullOrBlank() -> selectedEmail
                            else -> null
                        }
                        if (finalEmail.isNullOrBlank()) {
                            Toast.makeText(
                                context,
                                "Please enter or select an email",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@GradientButton
                        }

                        scope.launch {
                            // save email if new
                            if (newEmail.isNotBlank() && !savedEmails.contains(newEmail)) {
                                scanDisplayViewModel.saveEmail(newEmail)
                                savedEmails = scanDisplayViewModel.getAllEmails()
                                selectedEmail = newEmail
                            }

                            // generate + send
                            val summaryList = scanDisplayViewModel.buildSummary(displayItems)
                            val (matched, unmatched) = scanDisplayViewModel.buildDetailedLists(
                                displayItems
                            )

                           *//* val pdfFile = scanDisplayViewModel.generateScanReportPdf(
                                context,
                                summaryList,
                                matched,
                                unmatched
                            )*//*

                            val pdfFile = scanDisplayViewModel.generateScanReportExcel(
                                context,
                                summaryList,
                                matched,
                                unmatched
                            )


                            scanDisplayViewModel.sendEmailHostinger(
                                sendEmail = "android@loyalstring.com",
                                sendPass = "Loyal@123",
                                recipients = listOf(finalEmail),
                                subject = "Inventory Scan Report",
                                body = "<h2>Here is your inventory scan report</h2><p>Details attached.</p>",
                                type = "text/html",
                                attachments = mapOf(pdfFile.name to pdfFile.absolutePath)
                            )
                        }

                        showEmailDialog = false
                    }
                )
            },
            dismissButton = {
                GradientButton(
                    text = "Cancel",
                    onClick = { showEmailDialog = false }
                )
            }
        )
    }*/

}




@Composable
fun GradientButtonNew(
    text: String,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFD32940), Color(0xFF5231A7))
    )

    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .background(gradient, RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Sending...", color = Color.White, fontFamily = poppins)
                } else {
                    Text(text, color = Color.White, fontFamily = poppins)
                }
            }
        }
    }
}


typealias StockItem = com.loyalstring.rfid.data.model.stockVerification.Item

private fun String.normalizeTagKey(): String? =
    this.trim().uppercase().takeIf { it.isNotBlank() }

private fun BulkItem.tagKey(): String? =
    (this.epc ?: this.rfid)?.normalizeTagKey()

private fun buildItemsForUpload(all: List<ScannedBulkItem>): List<Item> { // Accepts List<ScannedBulkItem>
    return all.asSequence()  // Start with a sequence
        .distinctBy { it.epc?.trim()?.uppercase() ?: it.itemCode ?: it.hashCode().toString() }
        .map { b ->
            val status = when (b.currentScannedStatus.trim().lowercase()) { // Use currentScannedStatus
                "matched" -> "match"
                "unmatched" -> "unmatch"
                "match" -> "match"
                "unmatch" -> "unmatch"
                else -> "unmatch"
            }

           // Log.d("@@","status"+status)
            b.originalBulkItem.toItem(status) // Pass originalBulkItem to toItem
        }
        .filter { !it.itemCode.isNullOrBlank() }
        .toList() // Materialize to a List at the very end
}


/*
private fun buildItemsForUpload(all: List<BulkItem>): List<com.loyalstring.rfid.data.model.stockVerification.Item> {
    return all
        .distinctBy { it.epc?.trim()?.uppercase() ?: it.itemCode ?: it.hashCode().toString() }
        .map { it.toItem() }
        .filter { !it.itemCode.isNullOrBlank() }
}
*/


private fun BulkItem.toItem(status1: String): Item {
    return Item(
        itemCode = this.itemCode ?: "",
        status = status1,

        grossWeight = this.grossWeight?.toDoubleOrNull()?.takeIf { it.isFinite() } ?: 0.0,
        netWeight = this.netWeight?.toDoubleOrNull()?.takeIf { it.isFinite() } ?: 0.0,
        quantity = 1, // agar BulkItem me qty hai to yaha use kar dena

        counterName = this.counterName,
        categoryName = this.category,          // ✅ aapke BulkItem me category field hai (use ho rahi)
        productName = this.productName,
        designName = this.design,
        purityName = this.purity,          // agar purityName nahi hai to this.purity use karo
        companyName = "",
        branchName = this.branchName,

        counterId = this.counterId,
        categoryId = this.categoryId,
        productId = this.productId,
        designId = this.designId,
        purityId = 0,
        companyId =0,
        branchId = this.branchId
    )
}


@Composable
fun FilterRow(
    selectedCategories: List<String>,
    selectedProducts: List<String>,
    selectedDesigns: List<String>,
    onCategoryClick: () -> Unit,
    onProductClick: () -> Unit,
    onDesignClick: () -> Unit
) {
    val catLabel = selectedCategories.joinToString(", ").ifBlank { "Category" }
    val prodLabel = selectedProducts.joinToString(", ").ifBlank { "Product" }
    val designLabel = selectedDesigns.joinToString(", ").ifBlank { "Design" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        OutlinedButton(
            onClick = onCategoryClick,
            modifier = Modifier
                .width(100.dp)
                .height(40.dp),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(
                1.dp,
                Brush.horizontalGradient(listOf(Color(0xFF3053F0), Color(0xFFE82E5A)))
            )
        ) {
            Text(
                catLabel,
                color = Color.DarkGray,
                fontFamily = poppins,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        OutlinedButton(
            onClick = onProductClick,
            modifier = Modifier
                .width(100.dp)
                .height(40.dp),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(
                1.dp,
                Brush.horizontalGradient(listOf(Color(0xFF3053F0), Color(0xFFE82E5A)))
            )
        ) {
            Text(
                prodLabel,
                color = Color.DarkGray,
                fontFamily = poppins,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        OutlinedButton(
            onClick = onDesignClick,
            modifier = Modifier
                .width(100.dp)
                .height(40.dp),
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(
                1.dp,
                Brush.horizontalGradient(listOf(Color(0xFF3053F0), Color(0xFFE82E5A)))
            )
        ) {
            Text(
                designLabel,
                color = Color.DarkGray,
                fontFamily = poppins,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DesignItemRow(item: ScannedBulkItem, onClick: (ScannedBulkItem) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(4.dp))
            .padding(vertical = 4.dp)
            .clickable { onClick(item) },
        horizontalArrangement = Arrangement.Start
    ) {
        TableCell(item.design ?: "-", colDesignNameWidth)
        TableCell(item.rfid ?: "-", colRfidWidth)
        TableCell(item.itemCode ?: "-", colItemCodeWidth)

        // ✅ format weight properly
        val formattedGwt = formatTo3Decimals(parseWeightToBigDecimal(item.grossWeight))
        TableCell(formattedGwt, colGWtWidth)

        StatusIconCell(item.currentScannedStatus, colStatusIconWidth)
    }
}



/* -----------------------
   Filter selection dialog
   ----------------------- */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSelectionDialog(
    title: String,
    items: List<String>,
    selectedItems: SnapshotStateList<String>,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select $title",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = poppins
            )
        },
        text = {
            Column {
                LazyColumn(
                    modifier = Modifier
                        .heightIn(max = 200.dp)
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                ) {
                    items(items) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (selectedItems.contains(item)) selectedItems.remove(item)
                                    else selectedItems.add(item)
                                }
                                .padding(vertical = 6.dp, horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CompositionLocalProvider(LocalMinimumInteractiveComponentEnforcement provides false) {
                                Checkbox(
                                    checked = selectedItems.contains(item),
                                    onCheckedChange = { checked ->
                                        if (checked) selectedItems.add(item) else selectedItems.remove(
                                            item
                                        )
                                    },
                                    modifier = Modifier.size(20.dp),
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF3053F0),
                                        uncheckedColor = Color.Gray
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = item,
                                fontSize = 12.sp,
                                color = Color.DarkGray,
                                fontFamily = poppins,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    GradientButton(text = "CANCEL", onClick = onDismiss)
                    Spacer(modifier = Modifier.width(12.dp))
                    GradientButton(text = "OK", onClick = onConfirm)
                }
            }
        },
        confirmButton = {},
        dismissButton = {}
    )
}

/* -----------------------
   Summary row (simplified)
   ----------------------- */
/*@Composable
fun SummaryRow(currentLevel: String, items: List<BulkItem>, selectedMenu: String) {

    val totalQty = items.size
    val totalGwtBD =
        items.fold(BigDecimal.ZERO) { acc, it -> acc + parseWeightToBigDecimal(it.grossWeight) }

    val matchedItems = items.filter { it.scannedStatus == "Matched" }
    val totalMatchedQty = matchedItems.size
    val totalMatchedWtBD =
        matchedItems.fold(BigDecimal.ZERO) { acc, it -> acc + parseWeightToBigDecimal(it.grossWeight) }

    val unmatchedItems = items.filter { it.scannedStatus == "Unmatched" }
    unmatchedItems.size
    val unmatchedWtBD =
        unmatchedItems.fold(BigDecimal.ZERO) { acc, it -> acc + parseWeightToBigDecimal(it.grossWeight) }
    Log.d("SummryRow","SummryRow")
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF3B363E))
            .padding(vertical = 4.dp)
    ) {
        when {
            selectedMenu == MENU_UNMATCHED -> {
                TableHeaderCell("Total", colDesignNameWidth)
                TableHeaderCell("$totalQty", colRfidWidth)
                TableHeaderCell("$totalMatchedQty", colItemCodeWidth)
                TableHeaderCell(formatTo3Decimals(unmatchedWtBD), colGWtWidth)
                TableHeaderCell("", colStatusIconWidth)
            }

            currentLevel == "DesignItems" -> {
                TableHeaderCell("Total", colDesignNameWidth)
                TableHeaderCell("$totalQty", colRfidWidth)
                TableHeaderCell("$totalMatchedQty", colItemCodeWidth)
                TableHeaderCell(formatTo3Decimals(totalMatchedWtBD), colGWtWidth)
                TableHeaderCell("", colStatusIconWidth)
            }

            else -> {
                TableHeaderCell("Total", colCategoryWidth)
                TableHeaderCell("$totalQty", colQtyWidth)
                TableHeaderCell(formatTo3Decimals(totalGwtBD), colWeightWidth)
                TableHeaderCell("$totalMatchedQty", colMatchedQtyWidth)
                TableHeaderCell(formatTo3Decimals(totalMatchedWtBD), colMatchedWtWidth)
                TableHeaderCell("", colStatusWidth)
            }
        }
    }
}*/

@Composable
fun SummaryRow(currentLevel: String, items: List<ScannedBulkItem>, selectedMenu: String, localizedContext:Context) {
    // ✅ Optimized: Use derivedStateOf and sequence for better performance
    val totals by remember(items) {
        derivedStateOf {
            // Single pass — avoids building intermediate lists and BigDecimal per item
            var totalWt = 0.0
            var matchedQty = 0
            var matchedWt = 0.0
            for (it in items) {
                val w = it.grossWeight?.toDoubleOrNull() ?: 0.0
                totalWt += w
                if (it.currentScannedStatus == "Matched") {
                    matchedQty++
                    matchedWt += w
                }
            }
            val totalQty = items.size
            val unmatchedQty = totalQty - matchedQty
            val unmatchedWt = totalWt - matchedWt
            Triple(
                Triple(totalQty, totalWt.toBigDecimal(), matchedQty),
                Triple(matchedWt.toBigDecimal(), unmatchedQty, unmatchedWt.toBigDecimal()),
                Unit
            )
        }
    }

    val totalQty = totals.first.first
    val totalWtBD = totals.first.second
    val rawMatchedQty = totals.first.third
    val matchedWtBD = totals.second.first
    val rawUnmatchedQty = totals.second.second
    val unmatchedWtBD = totals.second.third

    val matchedQty by animateIntAsState(
        targetValue = rawMatchedQty,
        animationSpec = tween(durationMillis = 1000),
        label = "summaryMatchedQty"
    )
    val unmatchedQty by animateIntAsState(
        targetValue = rawUnmatchedQty,
        animationSpec = tween(durationMillis = 1000),
        label = "summaryUnmatchedQty"
    )

    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF3B363E))
            .padding(vertical = 4.dp)
    ) {

        // ✅ DesignItems view: show Total + Matched + Unmatched (filterwise)
        if (currentLevel == "DesignItems" || selectedMenu == MENU_MATCHED || selectedMenu == MENU_UNMATCHED) {
            TableHeaderCell(localizedContext.getString(R.string.total), colDesignNameWidth)
            TableHeaderCell("$totalQty", colRfidWidth)

            // 2-line info in same cell (M + U)
            Box(
                modifier = Modifier
                    .width(colItemCodeWidth)
                    .padding(horizontal = 2.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "M:$matchedQty\nU:$unmatchedQty",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                    maxLines = 2
                )
            }

            TableHeaderCell(formatTo3Decimals(totalWtBD), colGWtWidth)
            TableHeaderCell("", colStatusIconWidth)
        } else {
            // ✅ Category/Product/Design summary (same as before but now always filterwise)
            TableHeaderCell(localizedContext.getString(R.string.total), colCategoryWidth)
            TableHeaderCell("$totalQty", colQtyWidth)
            TableHeaderCell(formatTo3Decimals(totalWtBD), colWeightWidth)
            TableHeaderCell("$matchedQty", colMatchedQtyWidth)
            TableHeaderCell(formatTo3Decimals(matchedWtBD), colMatchedWtWidth)
            TableHeaderCell("", colStatusWidth)
        }
    }
}



@Composable
fun TableHeaderCell(text: String, width: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            // .padding(horizontal = 2.dp, vertical = 3.dp),
            .padding(start = 10.dp, top = 3.dp, bottom = 3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

/* Reused simple TableHeader */
@Composable
fun TableHeader(currentLevel: String,localizedContext:Context) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF3B363E))
            .padding(vertical = 2.dp)) {
        if (currentLevel == "DesignItems") {
            TableHeaderCell(localizedContext.getString(R.string.design), colDesignNameWidth)
            TableHeaderCell(localizedContext.getString(R.string.rfid_no), colRfidWidth)
            TableHeaderCell(localizedContext.getString(R.string.itemcode), colItemCodeWidth)
            TableHeaderCell(localizedContext.getString(R.string.gross_wt_header), colGWtWidth)
            TableHeaderCell(localizedContext.getString(R.string.status), colStatusIconWidth)
        } else {
            if(currentLevel == "Design")
            {
                TableHeaderCell(localizedContext.getString(R.string.design), colCategoryWidth)
            }else if(currentLevel == "Product"){
                TableHeaderCell(localizedContext.getString(R.string.product), colCategoryWidth)
            }else if(currentLevel == "Category")
            {
                TableHeaderCell(localizedContext.getString(R.string.category), colCategoryWidth)
            }

           // TableHeaderCell(currentLevel, colCategoryWidth)
            TableHeaderCell(localizedContext.getString(R.string.qty), colQtyWidth)
            TableHeaderCell(localizedContext.getString(R.string.gross_wt_header), colWeightWidth)
            TableHeaderCell(localizedContext.getString(R.string.m_qty), colMatchedQtyWidth)
            TableHeaderCell(localizedContext.getString(R.string.m_wt), colMatchedWtWidth)
            TableHeaderCell(localizedContext.getString(R.string.status), colStatusWidth)
        }
    }
}

@Composable
fun TableDataRow(row: TableRow, currentLevel: String, onRowClick: () -> Unit) {
    // ✅ Optimized: Use sequence and memoize computations
    // Single pass: qty, weights, matched count, and row status all at once
    val computedValues = remember(row.items) {
        var totalWt = 0.0
        var matchedQty = 0
        var matchedWt = 0.0
        var unmatchedCount = 0
        for (it in row.items) {
            val w = it.grossWeight?.toDoubleOrNull() ?: 0.0
            totalWt += w
            if (it.currentScannedStatus == "Matched") {
                matchedQty++
                matchedWt += w
            } else {
                unmatchedCount++
            }
        }
        val qty = row.items.size
        val rowStatus = when {
            unmatchedCount == 0 && qty > 0 -> "Matched"
            else -> "Unmatched"
        }
        object {
            val qty = qty
            val matchedQty = matchedQty
            val grossWeight = totalWt.toBigDecimal()
            val matchedWeight = matchedWt.toBigDecimal()
            val status = rowStatus
        }
    }

    val animatedMatchedQty by animateIntAsState(
        targetValue = computedValues.matchedQty,
        animationSpec = tween(durationMillis = 1000),
        label = "matchedQty"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 4.dp)
            .clickable { onRowClick() }
    ) {
        TableCell(row.label, colCategoryWidth)
        TableCell("${computedValues.qty}", colQtyWidth)
        TableCell(formatTo3Decimals(computedValues.grossWeight), colWeightWidth)
        TableCell("$animatedMatchedQty", colMatchedQtyWidth)
        TableCell(formatTo3Decimals(computedValues.matchedWeight), colMatchedWtWidth)
        StatusIconCell(computedValues.status, colStatusWidth)
    }

}

@Composable
fun TableCell(text: String, width: Dp) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(start = 10.dp, top = 3.dp, bottom = 3.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text, fontSize = 11.sp, color = Color.DarkGray, fontFamily = poppins)
    }
}

@Composable
fun StatusIconCell(status: String?, width: Dp) {
    val iconRes = when (status) {
        "Matched" -> R.drawable.ic_matched
        "Unmatched" -> R.drawable.ic_unmatched
        else -> R.drawable.ic_unmatched
    }
    Box(modifier = Modifier
        .width(width)
        .padding(2.dp), contentAlignment = Alignment.Center) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun VerticalMenu(localizedContext:Context,
                 matchedCount: Int,
                  unmatchedCount: Int,
                  totalCount: Int,
                  onMenuClick: (MenuItem) -> Unit) {
    val menuItems = listOf(
        MenuItem(localizedContext.getString(R.string.matched_items), R.drawable.ic_list_matched, matchedCount),
        MenuItem(localizedContext.getString(R.string.unmatched_items), R.drawable.ic_list_unmatched, unmatchedCount),
        MenuItem(localizedContext.getString(R.string.unlabelled_items), R.drawable.ic_list_unlabelled, totalCount),
        MenuItem(localizedContext.getString(R.string.resume_scan), R.drawable.ic_resume_scan, null),
        MenuItem(localizedContext.getString(R.string.search), R.drawable.search_gr_svg, unmatchedCount)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(30.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        menuItems.forEach { item -> MenuCard(item = item, onClick = { onMenuClick(item) }) }
    }
}

@Composable
fun MenuCard(item: MenuItem, onClick: () -> Unit) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(3.dp),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(listOf(Color(0xFF3053F0), Color(0xFFE82E5A)))
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = item.iconRes),
                contentDescription = item.title,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                fontFamily = poppins
            )
        }
    }
}

fun parseWeightToBigDecimal(weight: String?): BigDecimal {
    if (weight.isNullOrBlank()) return BigDecimal.ZERO
    return try {
        val cleaned = weight.replace(WEIGHT_CLEAN_REGEX, "")
        if (cleaned.isBlank()) BigDecimal.ZERO else BigDecimal(cleaned)
    } catch (e: Exception) {
        BigDecimal.ZERO
    }
}

fun formatTo3Decimals(b: BigDecimal): String {
    return b.setScale(3, RoundingMode.HALF_UP).toPlainString()
}

data class ScannedBulkItem(
    val originalBulkItem: BulkItem,
    val currentScannedStatus: String // "Matched" or "Unmatched"
) {
    // Delegate properties for convenient access, or access originalBulkItem directly
    val category: String? get() = originalBulkItem.category
    val productName: String? get() = originalBulkItem.productName
    val design: String? get() = originalBulkItem.design
    val epc: String? get() = originalBulkItem.epc
    val rfid: String? get() = originalBulkItem.rfid
    val itemCode: String? get() = originalBulkItem.itemCode
    val grossWeight: String? get() = originalBulkItem.grossWeight
    val netWeight: String? get() = originalBulkItem.netWeight
    val boxName: String? get() = originalBulkItem.boxName
    val counterName: String? get() = originalBulkItem.counterName
    val branchName: String? get() = originalBulkItem.branchName
    val branchType: String? get() = originalBulkItem.branchType
    val purity: String? get() = originalBulkItem.purity
    val counterId: Int? get() = originalBulkItem.counterId
    val categoryId: Int? get() = originalBulkItem.categoryId
    val productId: Int? get() = originalBulkItem.productId
    val designId: Int? get() = originalBulkItem.designId
    // Add other properties you use from BulkItem here
}

data class MenuItem(val title: String, val iconRes: Int,val count: Int? = null)
data class TableRow(val label: String, val items: List<ScannedBulkItem>)

fun ScannedBulkItem.toBulkItem(): BulkItem {
    return originalBulkItem.copy(
        isScanned = this.currentScannedStatus == "Matched"
    )
}
