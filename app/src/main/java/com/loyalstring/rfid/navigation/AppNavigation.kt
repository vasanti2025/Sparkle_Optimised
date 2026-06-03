package com.loyalstring.rfid.navigation

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.model.stockTransfer.LabelledStockItems
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
                    onBack = {
                      goBack(navController)
                    },
                    navController,
                    drawerState,
                    scope
                )
            }

            composable(Screens.ProductManagementScreen.route) {
                ProductManagementScreen(
                    onBack = {
                      goBack(navController)
                    },
                    navController,
                    userPreferences
                )
            }

            composable(Screens.AddProductScreen.route) {
                AddProductScreen(onBack = {
                  goBack(navController)
                }, navController)
            }

            composable(Screens.BulkProductScreen.route) {
                BulkProductScreen(onBack = {
                  goBack(navController)
                }, navController)
            }

            composable(Screens.ImportExcelScreen.route) {
                ImportExcelScreen(onBack = {
                  goBack(navController)
                }, navController)
            }

            composable(Screens.ProductListScreen.route) {
                ProductListScreen(onBack = {
                  goBack(navController)
                }, navController)
            }

            composable(Screens.ScanToDesktopScreen.route) {
                ScanToDesktopScreen(onBack = {
                  goBack(navController)
                }, navController)
            }

            composable(Screens.InventoryMenuScreen.route) {
                InventoryMenuScreen(onBack = {
                  goBack(navController)
                }, navController)
            }

            composable(Screens.ScanDisplayScreen.route) {
                ScanDisplayScreen(onBack = {
                  goBack(navController)
                }, navController)
            }

            composable(Screens.SearchScreen.route) {
                SearchScreen(
                    onBack = {
                      goBack(navController)
                    },
                    navController = navController,
                    listKey = null
                )
            }

            composable(
                route = "search_screen/{mode}",
                arguments = listOf(
                    navArgument("mode") {
                        type = NavType.StringType
                        defaultValue = "normal"
                    }
                )
            ) { backStackEntry ->
                val mode = backStackEntry.arguments?.getString("mode") ?: "normal"

                SearchScreen(
                    onBack = {
                      goBack(navController)
                    },
                    navController = navController,
                    listKey = if (mode == "unmatched") "unmatchedItems" else null
                )
            }



            composable(Screens.StockTransferScreenNew.route) {
                StockTransferScreenNew(onBack = {
                  goBack(navController)
                }, navController)
            }

            composable(Screens.EditProductScreen.route) {
                val item = navController.previousBackStackEntry?.savedStateHandle?.get<BulkItem>("item")
                item?.let {
                    EditProductScreen(
                        onBack = {
                          goBack(navController)
                        },
                        navController = navController,
                        item = it
                    )
                }
            }

            composable(Screens.SettingsScreen.route) {
                SettingsScreen(
                    onBack = {
                      goBack(navController)
                    },
                    navController,
                    userPreferences
                )
            }

            composable(Screens.OrderScreen.route) {
                val orderViewModel1: OrderViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                val singleProductViewModel: SingleProductViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                OrderScreen(
                    onBack = {
                      goBack(navController)
                    },
                    navController,
                    userPreferences,
                    orderViewModel1,
                    singleProductViewModel
                )
            }

         /*   composable(Screens.InvoiceScreen.route) {
                val item =
                    navController.previousBackStackEntry?.savedStateHandle?.get<CustomOrderResponse>("customerOrderResponse")
                item?.let {
                    InvoiceScreen(
                        navController = navController,
                        onBack = { navController.popBackStack() },
                        item = it
                    )
                }
            }*/

            composable(Screens.OrderListScreen.route) {
                OrderLisrScreen(
                    onBack = {
                      goBack(navController)
                    },
                    navController,
                    userPreferences
                )
            }

            composable(Screens.DailyRatesEditorScreen.route) {
                DailyRatesEditorScreen(navController = navController)
            }


            composable(Screens.LocationListScreen.route) {
                LocationListScreen(
                    onBack = {
                      goBack(navController)
                    },
                    navController = navController
                )
            }

            composable(Screens.StockInScreen.route) {
                StockInScreen(
                    onBack = {
                      goBack(navController)
                    },
                    navController = navController,
                    requestType = "In Request"   // ✅ pass the actual value here
                )
            }

            composable(Screens.StockOutScreen.route) {
                StockInScreen(
                    onBack = {
                      goBack(navController)
                    },
                    navController = navController,
                    requestType = "Out Request"
                )
            }

            composable("stock_transfer_detail") { backStackEntry ->
                val previousEntry = navController.previousBackStackEntry
                val labelItems = previousEntry
                    ?.savedStateHandle
                    ?.get<List<LabelledStockItems>>("labelItems")
                    ?: emptyList()

                val requestType = previousEntry
                    ?.savedStateHandle
                    ?.get<String>("requestType")
                    ?: "in"  // default or fallback
                val selectedTransferType = previousEntry
                    ?.savedStateHandle
                    ?.get<String>("selectedTransferType")
                    ?: "in"  // default or fallback

                val Id = previousEntry
                    ?.savedStateHandle
                    ?.get<Int>("Id")
                    ?: "0"

                StockTransferDetailScreen(
                    onBack = {
                      goBack(navController)
                    },
                    labelItems = labelItems,
                    requestType = requestType,
                    selectedTransferType =selectedTransferType,
                    id =Id
                )
            }
            // PERF-FIX: Removed duplicate "stock_transfer_detail" composable that was registered
            // twice. Duplicate routes in NavHost cause NavigationDuplicateException and undefined
            // back-stack behavior — the second registration silently shadows the first, leading
            // to navigation state corruption and back-button freezes/crashes.
            /*
            composable("stock_transfer_detail") { backStackEntry ->
                val previousEntry = navController.previousBackStackEntry
                val labelItems = previousEntry?.savedStateHandle?.get<List<LabelledStockItems>>("labelItems") ?: emptyList()
                val requestType = previousEntry?.savedStateHandle?.get<String>("requestType") ?: "in"
                val selectedTransferType = previousEntry?.savedStateHandle?.get<String>("selectedTransferType") ?: "in"
                val Id = previousEntry?.savedStateHandle?.get<Int>("Id") ?: "0"
                StockTransferDetailScreen(onBack = { navController.popBackStack() }, labelItems = labelItems, requestType = requestType, selectedTransferType = selectedTransferType, id = Id)
            }

            composable(Screens.DeliveryChalan.route) {
                DeliveryChalanScreen(onBack = { navController.popBackStack() }, navController = navController)
            }
            */

            composable("editDeliveryChallan/{challanId}") { backStackEntry ->
                val challanId = backStackEntry.arguments?.getString("challanId")?.toIntOrNull()
                DeliveryChalanScreen(
                    onBack = {
                      goBack(navController)
                    },
                    navController = navController,
                    challanId = challanId // ✅ pass here
                )
            }

            composable(Screens.DeliveryChallanListScreen.route) {
                DeliveryChallanListScreen(
                    onBack = {
                      goBack(navController)
                    },
                    navController,
                    )
            }

            // PERF-FIX: Kept exactly ONE registration of DeliveryChalan route (the duplicate was removed above).
            composable(Screens.DeliveryChalan.route) {
                DeliveryChalanScreen(
                    onBack = {
                      goBack(navController)
                    },
                    navController = navController
                )
            }

            composable(Screens.SampleOutScreen.route) {
                SampleOutScreen(
                    onBack = {
                      goBack(navController)
                    },
                    navController = navController

                )
            }

            composable(Screens.SampleOutListScreen.route) {
                SampleOutListScreen(
                    onBack = {
                      goBack(navController)
                    },
                    navController,
                )
            }

           /* composable("updateSampleOutScreen/{Id}/{SampleOutNo") { backStackEntry ->
                val Id = backStackEntry.arguments?.getString("Id")?.toIntOrNull()
                val SampleOutNo = backStackEntry.arguments?.getString("SampleOutNo")?.toIntOrNull()
                SampleOutScreen(
                    onBack = { navController.popBackStack() },
                    navController = navController,
                    Id = Id ,
                    SampleOutNo=SampleOutNo// ✅ pass here
                )
            }*/
            composable(
                route = "updateSampleOutScreen/{Id}/{SampleOutNo}",
                arguments = listOf(
                    navArgument("Id") { type = NavType.IntType },
                    navArgument("SampleOutNo") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("Id")
                val sampleOutNo = backStackEntry.arguments?.getString("SampleOutNo")
                Log.d("@@","ID"+id +"   sampleOutNo "+sampleOutNo)

                SampleOutScreen(
                    onBack = {
                      goBack(navController)
                    },
                    navController = navController,
                    Id = id,
                    SampleOutNo = sampleOutNo   // ✅ isko String? hi rakho
                )
            }

            composable(Screens.SampleInScreen.route) {
                SampleInScreen(
                    onBack = {
                      goBack(navController)
                    },
                    navController = navController

                )
            }

            composable(Screens.SampleInListScreen.route) {
                SampleInListScreen(
                    onBack = {
                      goBack(navController)
                    },
                    navController,
                )
            }

            composable(Screens.QuotationScreen.route) {
                QuotationScreen(
                    onBack = {
                      goBack(navController)
                    },
                    navController = navController

                )
            }

            composable(Screens.QuotationListScreen.route) {
                QuotationListScreen(
                    onBack = {
                      goBack(navController)
                    },
                    navController,
                )
            }

            composable(
                route = "updateQuotationScreen/{Id}/{QuotationNo}",
                arguments = listOf(
                    navArgument("Id") { type = NavType.IntType },
                    navArgument("QuotationNo") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getInt("Id")
                val QuotationNo = backStackEntry.arguments?.getString("QuotationNo")
                Log.d("@@","ID"+id +"   QuotationNo "+QuotationNo)

                QuotationScreen(
                    onBack = {
                      goBack(navController)
                    },
                    navController = navController,
                    Id = id,
                    QuotationNo = QuotationNo   // ✅ isko String? hi rakho
                )
            }

            composable(Screens.StockVerificationReport.route) {
                StockVerificationReportScreen(
                    onBack = {
                      goBack(navController)
                    },
                    navController,
                )
            }

            composable(
                "detail_screen/{branchId}/{categoryId}/{productId}/{designId}/{type}/{date}"
            ) { backStackEntry ->

                DetailScreen(
                    branchId = backStackEntry.arguments?.getString("branchId"),
                    categoryId = backStackEntry.arguments?.getString("categoryId"),
                    productId = backStackEntry.arguments?.getString("productId"),
                    designId = backStackEntry.arguments?.getString("designId"),
                    type = backStackEntry.arguments?.getString("type"),
                    date = backStackEntry.arguments?.getString("date"),
                    onBack = {
                      goBack(navController)
                    }
                )
            }

            composable(
                route = "batch_details_screen/{scanBatchId}"
            ) { backStackEntry ->

                val scanBatchId =
                    backStackEntry.arguments?.getString("scanBatchId") ?: ""

                BatchDetailsScreen(
                    scanBatchId = scanBatchId,
                    navController = navController
                )
            }

            composable(Screens.StockTransferPreviewScreen.route) {
                StockTransferPreviewScreen(
                    onBack = {
                      goBack(navController)
                    },
                    navController = navController
                )
            }

            composable(Screens.PrivacyPolicyScreen.route) {
                PrivacyPolicyScreen(
                    onBack = {
                      goBack(navController)
                    }
                )
            }


            composable(Screens.PrinterScreen.route) {
                PrinterScreen(navController = navController)
            }

            composable(Screens.RecogniseFaceLogin.route) {
                RecogniseFaceLogin(navController = navController)
            }

            composable(Screens.FaceManagement.route) {
                FaceManagement(navController = navController)
            }

            composable(Screens.AddFaceScreen.route) {
                AddFaceScreen(navController = navController)
            }



        }
    }
    
    
}

fun goBack(navController: NavHostController) {
    val popped = navController.popBackStack()

    if (!popped) {
        navController.navigate(Screens.HomeScreen.route) {
            launchSingleTop = true
        }
    }
}


