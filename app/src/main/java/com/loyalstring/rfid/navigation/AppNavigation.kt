package com.loyalstring.rfid.navigation

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navigation
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.model.order.CustomOrderResponse
import com.loyalstring.rfid.ui.screens.*
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.viewmodel.OrderViewModel
import com.loyalstring.rfid.viewmodel.SingleProductViewModel
import kotlinx.coroutines.CoroutineScope

/**
 * App Navigation Host – separates Auth (Login/Splash) and Main (Home etc.) flows
 * so that Drawer is disabled on Login/Splash.
 */
@RequiresApi(Build.VERSION_CODES.R)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun AppNavigation(
    navController: NavHostController,
    drawerState: DrawerState,
    scope: CoroutineScope,
    userPreferences: UserPreferences,
    startDestination: String
) {
    val currentRoute by navController.currentBackStackEntryAsState()

    // 🔹 Decide whether drawer should be visible or not
   /* val showDrawer = when (currentRoute?.destination?.route) {
        Screens.LoginScreen.route,
        Screens.SplashScreen.route -> false
        else -> true
    }*/

    // 🔹 Create separate graphs for Auth and Main routes
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // ---------------- AUTH GRAPH (no drawer) ----------------
     /*   composable(Screens.SplashScreen.route) {
            SplashScreen { nextRoute ->
                navController.popBackStack()
                navController.navigate(nextRoute)
            }
        }*/

        composable(Screens.LoginScreen.route) {
            LoginScreen(navController)
        }

        // ---------------- MAIN GRAPH (drawer enabled) ----------------
        navigation(
            startDestination = Screens.HomeScreen.route,
            route = "main_graph"
        ) {
            composable(Screens.HomeScreen.route) {
                HomeScreen(
                    onBack = { navController.popBackStack() },
                    navController,
                    drawerState,
                    scope
                )
            }

            composable(Screens.ProductManagementScreen.route) {
                ProductManagementScreen(
                    onBack = { navController.popBackStack() },
                    navController,
                    userPreferences
                )
            }

            composable(Screens.AddProductScreen.route) {
                AddProductScreen(onBack = { navController.popBackStack() }, navController)
            }

            composable(Screens.BulkProductScreen.route) {
                BulkProductScreen(onBack = { navController.popBackStack() }, navController)
            }

            composable(Screens.ImportExcelScreen.route) {
                ImportExcelScreen(onBack = { navController.popBackStack() }, navController)
            }

            composable(Screens.ProductListScreen.route) {
                ProductListScreen(onBack = { navController.popBackStack() }, navController)
            }

            composable(Screens.ScanToDesktopScreen.route) {
                ScanToDesktopScreen(onBack = { navController.popBackStack() }, navController)
            }

            composable(Screens.InventoryMenuScreen.route) {
                InventoryMenuScreen(onBack = { navController.popBackStack() }, navController)
            }

            composable(Screens.ScanDisplayScreen.route) {
                ScanDisplayScreen(onBack = { navController.popBackStack() }, navController)
            }

            composable(Screens.SearchScreen.route) {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    navController = navController
                )
            }

            composable(Screens.StockTransferScreen.route) {
                StockTransferScreen(onBack = { navController.popBackStack() }, navController)
            }

            composable(Screens.EditProductScreen.route) {
                val item = navController.previousBackStackEntry?.savedStateHandle?.get<BulkItem>("item")
                item?.let {
                    EditProductScreen(
                        onBack = { navController.popBackStack() },
                        navController = navController,
                        item = it
                    )
                }
            }

            composable(Screens.SettingsScreen.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    navController,
                    userPreferences
                )
            }

            composable(Screens.OrderScreen.route) {
                val orderViewModel1: OrderViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                val singleProductViewModel: SingleProductViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                OrderScreen(
                    onBack = { navController.popBackStack() },
                    navController,
                    userPreferences,
                    orderViewModel1,
                    singleProductViewModel
                )
            }

            composable(Screens.InvoiceScreen.route) {
                val item =
                    navController.previousBackStackEntry?.savedStateHandle?.get<CustomOrderResponse>("customerOrderResponse")
                item?.let {
                    InvoiceScreen(
                        navController = navController,
                        onBack = { navController.popBackStack() },
                        item = it
                    )
                }
            }

            composable(Screens.OrderListScreen.route) {
                OrderLisrScreen(
                    onBack = { navController.popBackStack() },
                    navController,
                    userPreferences
                )
            }

            composable(Screens.DailyRatesEditorScreen.route) {
                DailyRatesEditorScreen(navController = navController)
            }

            composable(Screens.LocationListScreen.route) {
                LocationListScreen(
                    onBack = { navController.popBackStack() },
                    navController = navController
                )
            }

            composable(Screens.StockInScreen.route) {
                StockInScreen(
                    onBack = { navController.popBackStack() },
                    navController = navController
                )
            }
        }
    }
}
