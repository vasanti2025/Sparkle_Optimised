package com.loyalstring.rfid.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.loyalstring.rfid.R

enum class Screens(val route: String) {
    //SplashScreen("splash"),
    LoginScreen("login"),
    HomeScreen("home"),
    ProductManagementScreen("products"),
    AddProductScreen("add product"),
    BulkProductScreen("bulk products"),
    ImportExcelScreen("import excel"),
    ExportExcelScreen("export excel"),
    ProductListScreen("product list"),
    ScanToDesktopScreen("scan_web"),
    ScanDisplayScreen("scan_display"),
    SettingsScreen("settings"),
    InventoryMenuScreen("inventory"),
    SearchScreen("search_screen"),
    EditProductScreen("edit_screen"),
    ScanCounterScreen("scan_counter"),
    ScanBranchScreen("scan_branch"),
    ScanExhibitionScreen("scan_exhib"),
    ScanBoxScreen("scan_box"),
    OrderScreen("order"),
    InvoiceScreen("invoiceScreen"),
    StockTransferScreen("stock_transfer"),
    OrderListScreen("order_list")


}
data class NavItems (
    val title:String,
    val unselectedIcon: ImageVector,
    val selectedIcon: Int,
    val route:String
)
val listOfNavItems = listOf<NavItems>(
    NavItems(
        title = "Home",
        unselectedIcon= Icons.Outlined.Home,
        selectedIcon = ( R.drawable.home_svg),
        route = Screens.HomeScreen.route
    ),
    NavItems(
        title = "Product",
        unselectedIcon= Icons.Outlined.MailOutline,
        selectedIcon = ( R.drawable.product_gr_svg),
        route = Screens.ProductManagementScreen.route
    ),
    NavItems(
        title = "Inventory",
        unselectedIcon= Icons.Outlined.FavoriteBorder,
        selectedIcon = ( R.drawable.inventory_gr_svg),
        route = "inventory"
    ),
    NavItems(
        title = "Order",
        unselectedIcon= Icons.Outlined.Settings,
        selectedIcon = ( R.drawable.order_gr_svg),
        route = Screens.OrderScreen.route
    ),
    NavItems(
        title = "Search",
        unselectedIcon= Icons.Outlined.FavoriteBorder,
        selectedIcon =(R.drawable.search_gr_svg),
        route = ""

    ), NavItems(
        title = "Stock\nTransfer",
        unselectedIcon= Icons.Outlined.FavoriteBorder,
        selectedIcon = ( R.drawable.stock_tr_gr_svg),
        route = "stock_transfer"

    ), NavItems(
        title = "Report",
        unselectedIcon= Icons.Outlined.FavoriteBorder,
        selectedIcon = ( R.drawable.report_gr_svg),
        route = ""

    ),NavItems(
        title = "Quotations",
        unselectedIcon= Icons.Outlined.FavoriteBorder,
        selectedIcon = ( R.drawable.quotation_gr_svg),
        route = ""

    ),NavItems(
        title = "Estimate",
        unselectedIcon= Icons.Outlined.FavoriteBorder,
        selectedIcon = ( R.drawable.estimate_gr_svg),
        route = ""

    ),NavItems(
        title = "Invoice",
        unselectedIcon= Icons.Outlined.FavoriteBorder,
        selectedIcon = ( R.drawable.invoice_gr_svg),
        route = ""

    ),NavItems(
        title = "Sample In",
        unselectedIcon= Icons.Outlined.FavoriteBorder,
        selectedIcon =( R.drawable.sample_in_gr_svg),
        route = ""

    ),NavItems(
        title = "Sample Out",
        unselectedIcon= Icons.Outlined.FavoriteBorder,
        selectedIcon = ( R.drawable.sample_out_gr_svg),
        route = ""

    ),NavItems(
        title = "Settings",
        unselectedIcon= Icons.Outlined.FavoriteBorder,
        selectedIcon = ( R.drawable.setting_gr_svg),
        route = "settings"

    ),
    NavItems(
        title = "Logout",
        unselectedIcon = Icons.AutoMirrored.Default.Logout,
        selectedIcon = (R.drawable.logout),
        route = "login"

    ),

)
