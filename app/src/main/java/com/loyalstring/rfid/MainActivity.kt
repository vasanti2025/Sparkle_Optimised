package com.loyalstring.rfid

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.BulkViewModel
import com.loyalstring.rfid.viewmodel.OrderViewModel
import com.loyalstring.rfid.viewmodel.SingleProductViewModel
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

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val startDestination = if (userPreferences.isLoggedIn()) {
            "main_graph"
        } else {
            Screens.LoginScreen.route
        }

        setContent {
            SparkleRFIDTheme {
                SetupNavigation(baseContext, userPreferences, startDestination)
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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var selectedItemIndex by rememberSaveable { mutableIntStateOf(-1) }
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val employee = remember {
        UserPreferences.getInstance(context).getEmployee(Employee::class.java)
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
                orderViewModel1.getAllEmpList(clientCode)
                orderViewModel1.getAllItemCodeList(ClientCodeRequest(clientCode))
                singleProductViewModel.getAllBranches(ClientCodeRequest(clientCode))
                singleProductViewModel.getAllPurity(ClientCodeRequest(clientCode))
                singleProductViewModel.getAllSKU(ClientCodeRequest(clientCode))
            }
        }
    }

    val navigationBody: @Composable () -> Unit = {
        AppNavigation(navController, drawerState, scope, userPreferences, startDestination)
    }

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
                                .fillMaxWidth(0.8f)
                                .background(BackgroundGradient)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.ic_user),
                                    contentDescription = "User Icon",
                                    modifier = Modifier
                                        .size(50.dp)
                                        .padding(8.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = employee?.username ?: "User",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Normal,
                                        color = Color.White
                                    ),
                                    fontFamily = poppins
                                )
                            }
                        }

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
                                            text = navigationItem.title,
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
                                                    navController.navigate("${Screens.SearchScreen.route}/normal") {
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }                                                }

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
                                            contentDescription = navigationItem.title
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductTopBar(navController: NavHostController) {
    TopAppBar(
        title = { Text("Product", color = Color.White, fontFamily = poppins) },
        navigationIcon = {
            IconButton(onClick = {
                navController.navigate(Screens.HomeScreen.route) {
                    popUpTo(navController.graph.startDestinationId)
                    launchSingleTop = true
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
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
        title = { Text("Home", color = Color.White) },
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
