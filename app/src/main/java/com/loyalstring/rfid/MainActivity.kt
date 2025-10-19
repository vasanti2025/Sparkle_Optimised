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
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.nestedscroll.NestedScrollSource.Companion.SideEffect
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
import com.loyalstring.rfid.ui.utils.NetworkMonitor
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
    //private val viewModel: BulkViewModel by viewModels()
    //lateinit var networkMonitor: NetworkMonitor

    // Removed: defer ViewModel creation to destination screens to speed up first frame
    private var scanKeyListener: ScanKeyListener? = null


    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val t0 = System.nanoTime()
        Log.d("StartupTrace", "MainActivity.onCreate start")
//        Log.d("@@","Start")
//        networkMonitor = NetworkMonitor(this) {
//            orderViewModel.syncDataWhenOnline()
//        }
//        networkMonitor.startMonitoring()

        // Decide start destination before Compose to avoid disk I/O during composition
        val startDestination = if (userPreferences.isLoggedIn()) Screens.HomeScreen.route else Screens.LoginScreen.route

        setContent {

            SparkleRFIDTheme {
                SetupNavigation(baseContext, userPreferences, startDestination)
            }
        }



//        val nfcAdapter = NfcAdapter.getDefaultAdapter(this)
//        nfcAdapter?.disableReaderMode(this)
        val t1 = System.nanoTime()
        Log.d("StartupTrace", "MainActivity.onCreate end ${(t1 - t0)/1_000_000} ms")
    }


    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            when (event.keyCode) {
                139 -> { // Barcode trigger
                    scanKeyListener?.onBarcodeKeyPressed()
                    return true
                }

                280, 293 -> { // RFID trigger
                    scanKeyListener?.onRfidKeyPressed()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == 293 || keyCode == 280 || keyCode == 139) {
            // Toast.makeText(getApplicationContext(), "KEYCODE: "+keyCode, Toast.LENGTH_SHORT).show();
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
    val setupT0 = System.nanoTime()
    android.util.Log.d("StartupTrace", "SetupNavigation start")
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var selectedItemIndex by rememberSaveable {
        mutableIntStateOf(-1)
    }
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Avoid extra singleton lookups; load employee on background thread
    val employeeState = produceState<Employee?>(initialValue = null) {
        value = withContext(Dispatchers.IO) {
            userPreferences.getEmployee(Employee::class.java)
        }
    }

    var drawerReady by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withFrameNanos { _ -> }
        drawerReady = true
    }

    val navigationBody: @Composable () -> Unit = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            AppNavigation(navController, drawerState, scope, userPreferences, startDestination)
        }
    }

    if (!drawerReady) {
        Box(modifier = Modifier.fillMaxSize()) {
            navigationBody()
        }
    } else {
        ModalNavigationDrawer(
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.background(Color.White),
                    drawerContainerColor = Color.White,
                    drawerShape = RectangleShape
                ) {
                    Column {
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
                                    contentDescription = "Default User",
                                    modifier = Modifier
                                        .size(50.dp)
                                        .padding(8.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = employeeState.value?.username.orEmpty(),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Normal,
                                        color = Color.White
                                    ),
                                    fontFamily = poppins
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxHeight()
                        ) {
                            itemsIndexed(
                                items = listOfNavItems,
                                key = { index, item -> item.route + index }
                            ) { index, navigationItem ->
                                NavigationDrawerItem(
                                    modifier = Modifier
                                        .fillMaxWidth(0.7f),
                                    label = {
                                        Text(
                                            text = navigationItem.title,
                                            fontSize = 16.sp,
                                            fontFamily = poppins,
                                            color = Color.DarkGray,
                                        )
                                    },
                                    selected = index == selectedItemIndex,
                                    onClick = {
                                        selectedItemIndex = index
                                        if (selectedItemIndex >= 4) {
                                            when (navigationItem.route) {
                                                "login" -> {
                                                    userPreferences.logout()
                                                    navController.navigate("login") {
                                                        popUpTo(0) { inclusive = true }
                                                        launchSingleTop = true
                                                    }
                                                    scope.launch { drawerState.close() }
                                                    navController.navigate("login") {
                                                        popUpTo(navController.graph.startDestinationId) {
                                                            inclusive = true
                                                        }
                                                        launchSingleTop = true
                                                    }
                                                }

                                                Screens.SettingsScreen.route,
                                                Screens.OrderScreen.route -> {
                                                    navController.navigate(navigationItem.route)
                                                }

                                                else -> {
                                                    Toast.makeText(
                                                        context,
                                                        "Coming soon..",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        } else {
                                            scope.launch {
                                                drawerState.close()
                                                navController.navigate(navigationItem.route)
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
                                        unselectedContainerColor = Color.Transparent
                                    ),
                                )
                            }
                        }
                    }
                }
            },
            drawerState = drawerState
        ) {
            Scaffold(
                modifier = Modifier
                    .focusable(true)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            when (event.key.nativeKeyCode) {
                                293, 280, 139 -> {
                                    true
                                }

                                else -> false
                            }
                        } else false
                    },
                topBar = {
                    when (currentRoute) {
                        Screens.HomeScreen.route -> HomeTopBar {
                            scope.launch {
                                drawerState.open()
                            }
                        }

                        Screens.ProductManagementScreen.route -> ProductTopBar(navController)
                        else -> {}
                    }
                },
                content = { navigationBody() }
            )
        }
    }

    SideEffect {
        val setupT1 = System.nanoTime()
        android.util.Log.d("StartupTrace", "SetupNavigation composed ${(setupT1 - setupT0)/1_000_000} ms")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductTopBar(navController: NavHostController) {
    TopAppBar(
        title = { Text("Product", color = Color.White, fontFamily = poppins) },
        navigationIcon = {
            IconButton(onClick = {

                navController.navigate(Screens.HomeScreen.route) { // Navigate to HomeScreen
                    popUpTo(navController.graph.startDestinationId) // Clear back stack up to start destination
                    launchSingleTop = true // Avoid duplicate instances
                }

            }) {
                Icon(Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        ),
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
            IconButton(onClick = {
                onNavigationClick()
            }) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF5231A7), Color(0xFFD32940))
                )
            )
    )
}



