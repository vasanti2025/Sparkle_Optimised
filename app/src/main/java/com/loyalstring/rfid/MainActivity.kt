package com.loyalstring.rfid

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.reader.ScanKeyListener
import com.loyalstring.rfid.navigation.AppNavigation
import com.loyalstring.rfid.navigation.Screens
import com.loyalstring.rfid.navigation.listOfNavItems
import com.loyalstring.rfid.ui.theme.SparkleRFIDTheme
import com.loyalstring.rfid.ui.utils.BackgroundGradient
import com.loyalstring.rfid.ui.utils.ToastUtils
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.BulkViewModel
import com.loyalstring.rfid.viewmodel.OrderViewModel
import com.loyalstring.rfid.viewmodel.SingleProductViewModel
import com.loyalstring.rfid.viewmodel.UserPermissionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferences: UserPreferences
    private var scanKeyListener: ScanKeyListener? = null
    /* override fun attachBaseContext(newBase: Context) {
         val prefs = UserPreferences.getInstance(newBase)
         val langCode = prefs.getAppLanguage().ifBlank { "en" }
         Log.d("@@ langCode","langCode"+langCode)
         val localizedContext = LocaleHelper.applyLocale(newBase, langCode)
         super.attachBaseContext(localizedContext)
     }*/
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = UserPreferences.getInstance(this)
        val savedLang = prefs.getAppLanguage().ifBlank { "en" }

        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(savedLang)
        )

        Log.d(
            "LocaleDebug",
            "AppCompat locales = ${AppCompatDelegate.getApplicationLocales().toLanguageTags()}"
        )

        super.onCreate(savedInstanceState)

        val startDestination = if (userPreferences.isLoggedIn()) {
            "main_graph"
        } else {
            Screens.LoginScreen.route
        }

        setContent {

            val ctx = LocalContext.current

            LaunchedEffect(Unit) {
                val cfg = ctx.resources.configuration
                Log.d(
                    "LocaleDebug",
                    "Compose ctx locale = ${cfg.locales[0].toLanguageTag()}"
                )
                val s = ctx.getString(R.string.menu_rates_title)
                Log.d("LocaleDebug", "menu_rates_title from Compose ctx = $s")
            }
            SparkleRFIDTheme {
                SetupNavigation(this, userPreferences, startDestination)
            }
        }


    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            when (event.keyCode) {
                139 -> {
                    scanKeyListener?.onBarcodeKeyPressed()
                    return true
                }
                280, 293 -> {
                    scanKeyListener?.onRfidKeyPressed()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == 293 || keyCode == 280 || keyCode == 139) {
            if (event.repeatCount == 0) {
                if (KeyEvent.KEYCODE_F9 == event.keyCode) {
                    scanKeyListener?.onBarcodeKeyPressed()
                } else {
                    scanKeyListener?.onRfidKeyPressed()
                }
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    fun registerScanKeyListener(listener: ScanKeyListener) {
        scanKeyListener = listener
    }

    fun unregisterScanKeyListener() {
        scanKeyListener = null
    }
}

@RequiresApi(Build.VERSION_CODES.R)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SetupNavigation(
    context: Context,
    userPreferences: UserPreferences,
    startDestination: String,
) {
    val orderViewModel1: OrderViewModel = hiltViewModel()
    val viewModel: BulkViewModel = hiltViewModel()
    val singleProductViewModel: SingleProductViewModel = hiltViewModel()
    val userPermissionViewModel: UserPermissionViewModel = hiltViewModel()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var selectedItemIndex by rememberSaveable { mutableIntStateOf(-1) }
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var branchPermissionReady by remember { mutableStateOf(false) }
    var permissionApiCalled by remember { mutableStateOf(false) } // Reactive state for employee (same variable name)
    var employee by remember { mutableStateOf<Employee?>(null) }

// Load employee on first composition
    LaunchedEffect(Unit) {
        employee = UserPreferences.getInstance(context).getEmployee(Employee::class.java)
    }

// Refresh when drawer opens (always show latest info)
    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) {
            employee = UserPreferences.getInstance(context).getEmployee(Employee::class.java)
        }
    }

    // Sync Data on Load
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            viewModel.syncRFIDDataIfNeeded(context)
        }
    }

    LaunchedEffect(employee?.clientCode) {
        employee?.clientCode?.let { clientCode ->
            withContext(Dispatchers.IO) {
                //Unnecessary
                //Unnecessary
                /*orderViewModel1.getAllEmpList(clientCode)
                orderViewModel1.getAllItemCodeList(ClientCodeRequest(clientCode))
                singleProductViewModel.getAllBranches(ClientCodeRequest(clientCode))
                singleProductViewModel.getAllPurity(ClientCodeRequest(clientCode))
                singleProductViewModel.getAllSKU(ClientCodeRequest(clientCode))*/
                orderViewModel1.getDailyRate(ClientCodeRequest(employee?.clientCode))
            }
        }
    }

    val navigationBody: @Composable () -> Unit = {
        AppNavigation(navController, drawerState, scope, userPreferences, startDestination)
    }
 //   val allEmployees by userPermissionViewModel.allEmployees.observeAsState(emptyList())
    val allEmployeesState by userPermissionViewModel.allEmployees.observeAsState()
    val allEmployees = allEmployeesState ?: emptyList()

    var prefUserId: Int? by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(employee) {
        val prefs = UserPreferences.getInstance(context)
        prefUserId = prefs.getUserId()

        Log.d("USER_DEBUG", "Updated prefUserId = $prefUserId")
    }

    LaunchedEffect(prefUserId) {
        Log.d("USER_DEBUG", "LaunchedEffect prefUserId = $prefUserId")

        val savedEmployee = UserPreferences.getInstance(context).getEmployee(Employee::class.java)
        Log.d("USER_DEBUG", "savedEmployee from prefs = $savedEmployee")

        employee = savedEmployee

        if (savedEmployee == null) {
            Log.d("USER_DEBUG", "savedEmployee is null")
            return@LaunchedEffect
        }

        if (savedEmployee.clientCode.isNullOrBlank()) {
            Log.d("USER_DEBUG", "clientCode is null or blank")
            return@LaunchedEffect
        }

        Log.d("USER_DEBUG", "employee loaded = $savedEmployee")
        Log.d("USER_DEBUG", "clientCode = '${savedEmployee.clientCode}'")

        userPermissionViewModel.loadPermissionsAll(savedEmployee.clientCode.toString())
    }

    LaunchedEffect(allEmployees, prefUserId) {
        Log.d("USER_DEBUG", "allEmployees size = ${allEmployees.size}")
        Log.d("USER_DEBUG", "PrefUserId = $prefUserId")

        if (allEmployees.isEmpty()) {
            Log.d("USER_DEBUG", "Employee list still empty")
            return@LaunchedEffect
        }

        if (prefUserId == null) {
            Log.d("USER_DEBUG", "prefUserId is null")
            return@LaunchedEffect
        }

        val selectedUser = allEmployees.firstOrNull {
            it.UserId.toString().trim() == prefUserId.toString().trim()
        }

        if (selectedUser == null) {
            Log.d("USER_DEBUG", "No matching user found for prefUserId = $prefUserId")
            return@LaunchedEffect
        }

        Log.d("USER_DEBUG", "SelectedUserId = ${selectedUser.UserId}")
        Log.d("USER_DEBUG", "branchSelectionJson = ${selectedUser.branchSelectionJson}")

        val branchIds = getBranchIdsFromBranchSelectionJson(selectedUser.branchSelectionJson)

        Log.d("USER_DEBUG", "parsed branchIds = $branchIds")

        if (branchIds.isEmpty()) {
            Log.d("USER_DEBUG", "branchIds empty, skipping save to avoid overwriting old value")
            return@LaunchedEffect
        }

        val prefs = UserPreferences.getInstance(context)
        prefs.saveBranchIds(branchIds)

        Log.d("USER_DEBUG", "saved branchIds = ${prefs.getBranchIds()}")
    }
     /*   if (showExpiryPopup) {
            Dialog(onDismissRequest = { }) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    tonalElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth(0.92f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White, RoundedCornerShape(12.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF3A3A3A))
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 16.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.stylus_note),
                                    contentDescription = "Expiry",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = if (isPlanExpired) "Subscription Expired" else "Expiry Warning",
                                    fontSize = 18.sp,
                                    color = Color.White,
                                    fontFamily = poppins
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = expiryPopupMessage,
                                fontSize = 14.sp,
                                color = Color.DarkGray,
                                fontFamily = poppins
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (isPlanExpired) {
                                GradientButtonIcon(
                                    text = "OK",
                                    onClick = {
                                        showExpiryPopup = false
                                        userPreferences.logout()
                                        UserPreferences.getInstance(context).clearAll()
                                        navController.navigate(Screens.LoginScreen.route) {
                                            popUpTo(0) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    },
                                    icon = painterResource(id = R.drawable.check_circle),
                                    iconDescription = "OK",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                )
                            } else {
                                GradientButtonIcon(
                                    text = "Later",
                                    onClick = {
                                        showExpiryPopup = false
                                    },
                                    icon = painterResource(id = R.drawable.ic_cancel),
                                    iconDescription = "Later",
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .padding(end = 6.dp)
                                )

                                GradientButtonIcon(
                                    text = "Continue",
                                    onClick = {
                                        showExpiryPopup = false
                                    },
                                    icon = painterResource(id = R.drawable.check_circle),
                                    iconDescription = "Continue",
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .padding(start = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }*/

    // Drawer visibility logic (hide on Login)
    val disableDrawerRoutes = listOf(Screens.LoginScreen.route)
    val shouldShowDrawer = currentRoute !in disableDrawerRoutes

    if (shouldShowDrawer) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.background(Color.White),
                    drawerContainerColor = Color.White,
                    drawerShape = RectangleShape
                ) {
                    Column {
                        // Header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.7f)
                                .height(64.dp)
                                .background(BackgroundGradient),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.ic_user),
                                    contentDescription = "User Icon",
                                    modifier = Modifier.size(36.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = employee?.username ?: "User",
                                    color = Color.White,
                                    fontFamily = poppins,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        //    Most likely 72.dp will loo
                        // Scrollable Drawer List
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .verticalScroll(scrollState)
                        ) {
                            listOfNavItems.forEachIndexed { index, navigationItem ->
                                NavigationDrawerItem(
                                    modifier = Modifier.fillMaxWidth(0.7f),
                                    label = {
                                        Text(
                                            text = stringResource(navigationItem.titleResId),
                                            fontSize = 16.sp,
                                            fontFamily = poppins,
                                            color = Color.DarkGray
                                        )
                                    },
                                    selected = index == selectedItemIndex,
                                    onClick = {
                                        selectedItemIndex = index
                                        when (navigationItem.route) {
                                            "login" -> {
                                                userPreferences.logout()
                                                UserPreferences.getInstance(context).clearAll()
                                                scope.launch { drawerState.close() }
                                                navController.navigate("login") {
                                                    popUpTo(0) { inclusive = true }
                                                    launchSingleTop = true
                                                }
                                            }

                                            Screens.SettingsScreen.route ->{
                                                scope.launch {
                                                    drawerState.close()
                                                    navController.navigate(navigationItem.route)
                                                }
                                            }
                                            Screens.OrderScreen.route -> {
                                                scope.launch {
                                                    drawerState.close()
                                                    navController.navigate(navigationItem.route)
                                                }

                                            }
                                            Screens.SearchScreen.route -> {
                                                scope.launch {
                                                    drawerState.close()
                                                    /* navController.navigate("${Screens.SearchScreen.route}/normal") {
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }    */
                                                    navController.navigate("search_screen/normal") {
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }

                                            }

                                            else -> {
                                                scope.launch {
                                                    drawerState.close()
                                                    navController.navigate(navigationItem.route)
                                                }
                                            }
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            modifier = Modifier.size(24.dp),
                                            painter = painterResource(navigationItem.selectedIcon),
                                            tint = Color.DarkGray,
                                            contentDescription = context.getString(navigationItem.titleResId)
                                        )
                                    },
                                    colors = NavigationDrawerItemDefaults.colors(
                                        selectedContainerColor = Color.Transparent,
                                        unselectedContainerColor = Color.Transparent,
                                        selectedIconColor = Color.DarkGray,
                                        unselectedIconColor = Color.DarkGray,
                                        selectedTextColor = Color.DarkGray,
                                        unselectedTextColor = Color.DarkGray
                                    )
                                )
                            }
                        }
                    }
                }
            }
        ) {
            Scaffold(
                modifier = Modifier
                    .focusable(true)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            when (event.nativeKeyEvent.keyCode) {
                                293, 280, 139 -> true
                                else -> false
                            }
                        } else false
                    },
                topBar = {
                    when (currentRoute) {
                        Screens.HomeScreen.route -> HomeTopBar {
                            scope.launch { drawerState.open() }
                        }

                        Screens.ProductManagementScreen.route -> ProductTopBar(navController)
                        else -> {}
                    }
                },
                content = { navigationBody() }
            )
        }
    } else {
        Scaffold(content = { navigationBody() })
    }
}

fun getBranchIdsFromBranchSelectionJson(branchSelectionJson: String?): List<Int> {
    return try {
        if (branchSelectionJson.isNullOrBlank()) {
            return listOf(1) // ✅ default
        }

        val parsed = com.google.gson.JsonParser.parseString(branchSelectionJson)

        val jsonArray = when {
            parsed.isJsonArray -> parsed.asJsonArray
            parsed.isJsonPrimitive && parsed.asJsonPrimitive.isString -> {
                com.google.gson.JsonParser.parseString(parsed.asString).asJsonArray
            }
            else -> return listOf(1) // ✅ default
        }

        val branchIds = jsonArray.mapNotNull { element ->
            val obj = element.asJsonObject
            if (obj.has("Id") && !obj.get("Id").isJsonNull) obj.get("Id").asInt else null
        }

        if (branchIds.isEmpty()) listOf(1) else branchIds // ✅ fallback

    } catch (e: Exception) {
        Log.e("USER_DEBUG", "Branch parse error: ${e.message}")
        listOf(1) // ✅ fallback
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductTopBar(navController: NavHostController) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.product),
                color = Color.White,
                fontFamily = poppins
            )
        },
        navigationIcon = {
            IconButton(onClick = {
                navController.navigate(Screens.HomeScreen.route) {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF5231A7), Color(0xFFD32940))
                )
            )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(onNavigationClick: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.home),
                color = Color.White,
                fontFamily = poppins
            )
        },
        navigationIcon = {
            IconButton(onClick = { onNavigationClick() }) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF5231A7), Color(0xFFD32940))
                )
            )
    )
}
