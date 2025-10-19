package com.loyalstring.rfid.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.navigation.Screens
import com.loyalstring.rfid.ui.utils.FilePickerDialog
import com.loyalstring.rfid.ui.utils.GradientButton
import com.loyalstring.rfid.ui.utils.MappingDialogWrapper
import com.loyalstring.rfid.ui.utils.ToastUtils
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.BulkViewModel
import com.loyalstring.rfid.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    navController: NavHostController,
    userPreferences: UserPreferences
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    var showSheetInput by remember { mutableStateOf(false) }
    var sheetUrl by remember { mutableStateOf("") }
    val context: Context = LocalContext.current
    var shouldNavigateBack by remember { mutableStateOf(false) }

    LaunchedEffect(shouldNavigateBack) {
        if (shouldNavigateBack) {
            kotlinx.coroutines.delay(50)
            onBack()
        }
    }

    val menuItems = listOf(
        SettingsMenuItem("Sheet URL", Icons.Default.Attachment, 0)
    )


    Scaffold(
        topBar = {
            GradientTopBar(
                title = "Settings",
                navigationIcon = {
                    IconButton(onClick = { shouldNavigateBack = true }) {
                        Icon(
                            Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {

                },
                showCounter = false,
                selectedCount = 0,
                onCountSelected = {
                    0
                }

            )
        },
        /*   bottomBar = {
               ScanBottomBar(
                   onSave = { */
        /* Save logic */
        /* },
                        onList = { navController.navigate(Screens.ProductListScreen.route) },
                        onScan = { */
        /* Scan logic */
        /* },
                        onGscan = { */
        /* Gscan logic */
        /* },
                        onReset = { */
        /* Reset logic */
        /* }
                    )
                }*/
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .padding(vertical = 8.dp)
            ) {
                items(menuItems) { item ->
                    MenuItemRow(item = item) {
                        if (item.title == "Sheet URL") {
                            showSheetInput = true
                        }
                    }
                }
            }
        }

    }

    // ✅ Dialog must be outside LazyColumn
    if (showSheetInput) {
        SheetInputDialog(
            sheetUrl = sheetUrl,
            onValueChange = { sheetUrl = it },
            onDismiss = { showSheetInput = false },
            onSetClick = {
                viewModel.updateSheetUrl(sheetUrl)
                showSheetInput = false
                if (userPreferences.getSheetUrl()?.isNotEmpty() == true) {
                    ToastUtils.showToast(context, "Sheet URL updated successfully")
                }
            }
        )
    }


}


@Composable
fun MenuItemRow(item: SettingsMenuItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        color = Color(0xFFF7F7F7)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = item.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                color = Color.Black
            )

            item.badgeCount?.takeIf { it > 0 }?.let { count ->
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE0E0E0), RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = count.toString(),
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                }
            }
        }
    }
}

@Composable
fun SheetInputDialog(
    sheetUrl: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSetClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = { Text("Set Sheet URL", fontFamily = poppins, fontSize = 14.sp) },
        text = {
            SheetInputSection(
                sheetUrl = sheetUrl,
                onValueChange = onValueChange,
                onSetClick = onSetClick
            )
        },
        dismissButton = {
        }
    )
}


@Composable
fun SheetInputSection(
    sheetUrl: String,
    onValueChange: (String) -> Unit,
    onSetClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        OutlinedTextField(
            value = sheetUrl,
            onValueChange = onValueChange,
            label = { Text("Enter Sheet Url", fontFamily = poppins, fontSize = 12.sp) },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF2F2F2), RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.padding(8.dp))

        GradientButton(
            text = "Set Sheet Id",
            onClick = onSetClick,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}


data class SettingsMenuItem(
    val title: String,
    val icon: ImageVector,
    val badgeCount: Int? = null
)
