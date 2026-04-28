package com.loyalstring.rfid.ui.screens

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.livedata.observeAsState
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.viewmodel.UserPermissionViewModel
import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.ConfigurationCompat
import androidx.navigation.NavController
import com.loyalstring.rfid.R
import com.loyalstring.rfid.getBranchIdsFromBranchSelectionJson
import com.loyalstring.rfid.navigation.NavItems
import com.loyalstring.rfid.navigation.listOfNavItems
import com.loyalstring.rfid.ui.utils.ToastUtils
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.worker.LocaleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "UnusedBoxWithConstraintsScope")
@Composable
fun HomeScreen(
    onBack: () -> Unit,
    navController: NavController,
    drawerState: DrawerState,
    scope: CoroutineScope
) {


    val items = remember {
        listOfNavItems.filter {
            it.titleResId != R.string.home && it.titleResId != R.string.logout
        }
    }
    val context: Context = LocalContext.current
    val userPermissionViewModel: UserPermissionViewModel = hiltViewModel()

    val allEmployeesState by userPermissionViewModel.allEmployees.observeAsState()
    val allEmployees = allEmployeesState ?: emptyList()

    // PERF-FIX: Both AppCompatDelegate.getApplicationLocales() AND applyLocale() wrapped
    // inside a single remember{} block so NEITHER runs on recomposition.
    // AppCompatDelegate.getApplicationLocales() crosses into the framework via JNI on
    // every call — even though it's fast individually, doing it on every frame at
    // 60fps (during scroll/animation) adds measurable overhead on Cortex-A55 cores.
    /*
    val userPreferences = UserPreferences.getInstance(context)
    val savedLang = userPreferences.getAppLanguage().ifBlank { "en" }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = currentLocales[0]?.language ?: savedLang
    val localizedContext = LocaleHelper.applyLocale(context, currentLang)
    val delegateLang = currentLocales[0]?.language
    val delegateTags = currentLocales.toLanguageTags()
    val configLang = ConfigurationCompat.getLocales(localizedContext.resources.configuration)[0]?.language
    val configTags = ConfigurationCompat.getLocales(localizedContext.resources.configuration).toLanguageTags()
    Log.d("LANG_DEBUG", "AppCompatDelegate locales = $delegateTags")
    Log.d("LANG_DEBUG", "currentLang = $currentLang")
    Log.d("LANG_DEBUG", "delegateLang = $delegateLang")
    Log.d("LANG_DEBUG", "localizedContext config language = $configLang")
    Log.d("LANG_DEBUG", "localizedContext config tags = $configTags")
    */
    val localizedContext = remember {
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        val savedLang = UserPreferences.getInstance(context).getAppLanguage().ifBlank { "en" }
        val currentLang = currentLocales[0]?.language ?: savedLang
        LocaleHelper.applyLocale(context, currentLang)
    }

    LaunchedEffect(Unit) {
        val prefs = UserPreferences.getInstance(context)
        val employee = prefs.getEmployee(Employee::class.java)

        if (employee == null) {
            Log.d("HOME_BRANCH", "Employee null")
            return@LaunchedEffect
        }

        val clientCode = employee.clientCode

        if (clientCode.isNullOrBlank()) {
            Log.d("HOME_BRANCH", "ClientCode blank")
            return@LaunchedEffect
        }

        Log.d("HOME_BRANCH", "Calling permission API from Home, clientCode=$clientCode")

        userPermissionViewModel.loadPermissionsAll(clientCode)
    }

    LaunchedEffect(allEmployees) {
        val prefs = UserPreferences.getInstance(context)
        val employee = prefs.getEmployee(Employee::class.java)

        if (employee == null) {
            Log.d("HOME_BRANCH", "Employee null while saving branch")
            return@LaunchedEffect
        }

        if (allEmployees.isEmpty()) {
            Log.d("HOME_BRANCH", "Permission employee list empty")
            return@LaunchedEffect
        }

        val prefUserId = prefs.getUserId()
        val employeeTableId = employee.id
        val employeeId = employee.employeeId

        Log.d("HOME_BRANCH", "prefUserId=$prefUserId")
        Log.d("HOME_BRANCH", "employee.id=$employeeTableId")
        Log.d("HOME_BRANCH", "employee.employeeId=$employeeId")
        Log.d("HOME_BRANCH", "allEmployees size=${allEmployees.size}")

        val selectedUser = allEmployees.firstOrNull { user ->
            val apiUserId = user.UserId?.toString()?.trim()

            apiUserId == prefUserId?.toString()?.trim() ||
                    apiUserId == employeeTableId?.toString()?.trim() ||
                    apiUserId == employeeId?.toString()?.trim()
        }

        if (selectedUser == null) {
            Log.d("HOME_BRANCH", "No matching user found")
            return@LaunchedEffect
        }

        Log.d("HOME_BRANCH", "SelectedUserId=${selectedUser.UserId}")
        Log.d("HOME_BRANCH", "branchSelectionJson=${selectedUser.branchSelectionJson}")

        val branchIds = getBranchIdsFromBranchSelectionJson(
            selectedUser.branchSelectionJson
        )

        Log.d("HOME_BRANCH", "parsed branchIds=$branchIds")

        if (branchIds.isEmpty()) {
            Log.d("HOME_BRANCH", "branchIds empty, not saving")
            return@LaunchedEffect
        }

        prefs.saveBranchIds(branchIds)

        Log.d("HOME_BRANCH", "saved branchIds=${prefs.getBranchIds()}")
    }
    val topBarBrush = remember {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFF5231A7), Color(0xFFD32940))
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text( localizedContext.getString(R.string.home), color = Color.White, fontFamily = poppins) },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.background(topBarBrush)
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
        ) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight
            val columns = when {
                screenWidth < 328.dp -> 2
                screenWidth < 400.dp -> 3
                else -> 4
            }

            val spacing = 16.dp
            val totalSpacing = spacing * (columns + 1)
            val cardSize = (screenWidth - totalSpacing) / columns

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                LazyVerticalGrid(
                    modifier = Modifier
                        .weight(1f),
                    columns = GridCells.Fixed(columns),
                    verticalArrangement = Arrangement.spacedBy(spacing),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    contentPadding = PaddingValues(top = 16.dp)
                ) {
                    itemsIndexed(
                        items = items,
                        key = { index, item -> "${item.route}#${index}" }
                    ) { index, item ->
                        val iconResId = item.selectedIcon
                        HomeGridCard(
                            rememberScope = scope,
                            item = item,
                            navController = navController,
                            size = cardSize,
                            context = context,
                            iconResId = iconResId,
                            index = index,
                            localizedContext=localizedContext
                        )
                    }
                }


                /* Text(
                     text = "TEST CRASH",
                     fontSize = 13.sp,
                     textAlign = TextAlign.Center,
                     fontFamily = poppins,
                     maxLines = 2,
                     lineHeight = 16.sp, // adds line spacing
                     modifier = Modifier
                         .fillMaxWidth()
                         .clickable {
                             throw RuntimeException("Test Crash") // Force a crash
                         }
                         .padding(horizontal = 2.dp) // optional to help with narrow cards
                 )*/

                Spacer(modifier = Modifier.height(12.dp))



                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.drawer_icon),
                        contentDescription = "Sparkle RFID Logo",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
    // PERF-FIX: Removed t0/t1 timing log — referenced t0 that no longer exists.
}


@Composable
fun HomeGridCard(
    rememberScope: CoroutineScope,
    item: NavItems,
    navController: NavController,
    size: Dp,
    context: Context,
    iconResId: Int,
    index: Int,
    localizedContext:Context
) {
    Card(
        onClick = {
            rememberScope.launch {
                if (item.route.isNotEmpty()) {
                    navController.navigate(item.route) {
                        launchSingleTop = true
                    }
                } else {
                    ToastUtils.showToast(context,  localizedContext.getString(R.string.coming_soon))
                }
            }
        },
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = stringResource(item.titleResId),
                tint = Color.Unspecified,
                modifier = Modifier.size(size * 0.4f) // Scale icon size with card
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = localizedContext.getString(item.titleResId),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                fontFamily = poppins,
                maxLines = 2,
                lineHeight = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp)
            )
        }
    }
    // PERF-FIX: Removed Log.d() from composable body — it ran on every recomposition of
    // every grid card (n cards × m recompositions), flooding logcat and adding overhead.
}





