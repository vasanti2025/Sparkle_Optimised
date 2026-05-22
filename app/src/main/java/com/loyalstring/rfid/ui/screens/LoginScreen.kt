package com.loyalstring.rfid.ui.screens

import androidx.compose.material3.Surface
import androidx.compose.ui.window.Dialog

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.model.login.LoginRequest
import com.loyalstring.rfid.data.model.login.LoginResponse
import com.loyalstring.rfid.data.remote.resource.Resource
import com.loyalstring.rfid.navigation.Screens
import com.loyalstring.rfid.ui.utils.BackGroundLinerGradient
import com.loyalstring.rfid.ui.utils.BackgroundGradient
import com.loyalstring.rfid.ui.utils.GradientButtonIcon
import com.loyalstring.rfid.ui.utils.NetworkUtils
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.BulkViewModel
import com.loyalstring.rfid.viewmodel.LoginViewModel
import com.loyalstring.rfid.viewmodel.ScanDisplayViewModel
import com.loyalstring.rfid.viewmodel.UserPermissionViewModel
import com.loyalstring.rfid.worker.LocaleHelper
import kotlinx.coroutines.launch

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter

@Composable
fun LoginScreen(navController: NavController, viewModel: LoginViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val bulkviewmodel: BulkViewModel = hiltViewModel()
    val scanDisplayViewModel: ScanDisplayViewModel = hiltViewModel()
    val userPermissionViewModel: UserPermissionViewModel = hiltViewModel()
    val userPrefs = remember { UserPreferences(context) }
    var selectedLoginMode by remember { mutableStateOf("password") }
    var shouldNavigateAfterPermission by remember { mutableStateOf(false) }
    var showCustomApiDialog by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }

    var showExpiryPopup by remember { mutableStateOf(false) }
    var expiryPopupMessage by remember { mutableStateOf("") }
    var pendingLoginResponse by remember { mutableStateOf<LoginResponse?>(null) }

    val loginResponse by viewModel.loginResponse.observeAsState()
    val isLoading = loginResponse is Resource.Loading
    val errorMessage = (loginResponse as? Resource.Error)?.message
    val loginSuccess = loginResponse is Resource.Success
    val permissionResponse by userPermissionViewModel.permissionResponse.observeAsState()

    val userPreferences = UserPreferences.getInstance(context)
    val savedLang = userPreferences.getAppLanguage().ifBlank { "en" }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = currentLocales[0]?.language ?: savedLang
    val localizedContext = LocaleHelper.applyLocale(context, currentLang)

    LaunchedEffect(permissionResponse, shouldNavigateAfterPermission) {
        if (!shouldNavigateAfterPermission) return@LaunchedEffect

        when (permissionResponse) {
            is Resource.Success -> {
                shouldNavigateAfterPermission = false
                navController.navigate(Screens.HomeScreen.route) {
                    popUpTo(Screens.LoginScreen.route) { inclusive = true }
                }
            }
            is Resource.Error -> {
                shouldNavigateAfterPermission = false
                Toast.makeText(
                    context,
                    (permissionResponse as Resource.Error).message,
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> {}
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    /* LaunchedEffect(loginSuccess) {
         if (loginSuccess){
             val loginData = (loginResponse as? Resource.Success<LoginResponse>)?.data
             loginData?.let { response ->
                 userPrefs.saveToken(response.token.orEmpty())
                 userPrefs.saveUserName(response.employee?.username.toString())
                 userPrefs.saveEmployee(response.employee)
                 userPrefs.setLoggedIn(true)
                 userPrefs.saveBranchId(response.employee!!.defaultBranchId)
                 userPrefs.saveClient(response.employee?.clients!!)
                 userPrefs.saveOrganization(response.employee?.clients!!.organisationName.toString())

                 response.employee.empEmail?.let { scanDisplayViewModel.saveEmail(it) }

                 userPrefs.saveLoginCredentials(username, password, rememberMe,response.employee.clients.rfidType.toString(),response.employee.id,response.employee.defaultBranchId,response.employee.clients.organisationName.toString())

                 // Sync RFID data in background — does not block navigation
                 launch {
                     bulkviewmodel.syncRFIDDataIfNeeded(context)
                 }

                 // PERF-FIX: Load permissions first; navigation to HomeScreen is handled
                 // ONLY in the permissionResponse LaunchedEffect below (Resource.Success branch).
                 // Previously, navigate(HomeScreen) was called HERE as well, causing the app to
                 // navigate twice: once immediately after login and once after permissions loaded.
                 // This double-navigate pushed HomeScreen twice onto the back stack, causing
                 // back-button freezes and unexpected navigation behavior.
                 response.employee.clientCode?.let {
                     userPermissionViewModel.loadPermissions(it, response.employee.id)
                 } ?: run {
                     // No clientCode — navigate directly since permissions cannot be loaded
                     navController.navigate(Screens.HomeScreen.route) {
                         popUpTo(Screens.LoginScreen.route) { inclusive = true }
                     }
                 }

             }
         }
     }*/

    fun completeLogin(response: LoginResponse) {
        shouldNavigateAfterPermission = true
        userPrefs.saveToken(response.token.orEmpty())
        userPrefs.saveUserName(response.employee?.username.toString())
        userPrefs.saveEmployee(response.employee)
        userPrefs.setUserId(response.employee!!.id)
        userPrefs.setLoggedIn(true)
        userPrefs.saveBranchId(response.employee!!.defaultBranchId)
        userPrefs.saveClient(response.employee?.clients!!)
        userPrefs.saveOrganization(response.employee?.clients!!.organisationName.toString())

        response.employee.empEmail?.let { scanDisplayViewModel.saveEmail(it) }

        userPrefs.saveLoginCredentials(
            username,
            password,
            rememberMe,
            response.employee.clients.rfidType.toString(),
            response.employee.id,
            response.employee.defaultBranchId,
            response.employee.clients.organisationName.toString()
        )

        response.employee.clientCode?.let {
            userPermissionViewModel.loadPermissions(it, response.employee.id)
        }

        navController.navigate(Screens.HomeScreen.route) {
            popUpTo(Screens.LoginScreen.route) { inclusive = true }
        }
    }

    LaunchedEffect(loginSuccess) {
        if (loginSuccess) {
            val loginData = (loginResponse as? Resource.Success<LoginResponse>)?.data
            loginData?.let { response ->

                //  val expiryDateStr = response.employee?.clients?.expiryDate
                val expiryDateStr =response.employee?.clients?.planExpiryDate
                val daysRemaining = getDaysRemaining(expiryDateStr)

                when {
                    daysRemaining == null -> completeLogin(response)

                    daysRemaining < 0 -> {
                        Toast.makeText(
                            context,
                            localizedContext.getString(R.string.your_subscription_has_expired_please_contact_support),
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    daysRemaining in 0..15 -> {
                        pendingLoginResponse = response
                        expiryPopupMessage =
                            localizedContext.getString(R.string.subscription_expiry_warning, daysRemaining)
                        showExpiryPopup = true
                    }

                    else -> completeLogin(response)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CurvedGradientHeader(localizedContext=localizedContext)

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selectedLoginMode == "password") BackgroundGradient
                            else androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(Color(0xFFE0E0E0), Color(0xFFCCCCCC))
                            )
                        )
                        .clickable { selectedLoginMode = "password" }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = localizedContext.getString(R.string.password),
                        color = if (selectedLoginMode == "password") Color.White else Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        fontFamily = poppins
                    )
                }

                /*     Box(
                         modifier = Modifier
                             .weight(1f)
                             .clip(RoundedCornerShape(12.dp))
                             .background(
                                 if (selectedLoginMode == "face") BackgroundGradient
                                 else androidx.compose.ui.graphics.Brush.linearGradient(
                                     listOf(Color(0xFFE0E0E0), Color(0xFFCCCCCC))
                                 )
                             )
                             .clickable { selectedLoginMode = "face" }
                             .padding(vertical = 14.dp),
                         contentAlignment = Alignment.Center
                     ) {
                         Text(
                             text = localizedContext.getString(R.string.face_login),
                             color = if (selectedLoginMode == "face") Color.White else Color.Black,
                             fontWeight = FontWeight.Bold,
                             fontSize = 15.sp,
                             fontFamily = poppins
                         )
                     }*/
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selectedLoginMode == "face") BackgroundGradient
                            else androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(Color(0xFFE0E0E0), Color(0xFFCCCCCC))
                            )
                        )
                        .clickable { selectedLoginMode = "face" },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.face_svg),
                        contentDescription = localizedContext.getString(R.string.face_login),
                        tint = if (selectedLoginMode == "face") Color.White else Color.DarkGray,
                        modifier = Modifier.size(28.dp)
                    )
                }

            }
            if (selectedLoginMode == "password") {

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(localizedContext.getString(R.string.username)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(localizedContext.getString(R.string.password)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                painter = painterResource(id = if (passwordVisible) R.drawable.ic_action_eye else R.drawable.ic_action_eye_off),
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = Color.DarkGray
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it })
                        Text(localizedContext.getString(R.string.remember_me), fontSize = 14.sp)
                    }

                    Text(localizedContext.getString(R.string.forgot_password), color = Color.Blue, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
            if (selectedLoginMode == "face") {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = localizedContext.getString(R.string.use_face_detection_to_continue_login),
                    fontSize = 15.sp,
                    color = Color.Gray,
                    fontFamily = poppins
                )

                Spacer(modifier = Modifier.height(12.dp))

                Icon(
                    painter = painterResource(id = R.drawable.face_lock),
                    contentDescription = "Face Login",
                    tint = Color(0xFFC7C7C9),
                    modifier = Modifier.size(72.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .defaultMinSize(minHeight = 50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BackgroundGradient)
                    .clickable(enabled = !isLoading) {
                        if (!NetworkUtils.isNetworkAvailable(context)) {
                            Toast.makeText(
                                context,
                                localizedContext.getString(R.string.please_check_your_internet_connection),
                                Toast.LENGTH_SHORT
                            ).show()
                            return@clickable
                        }

                        if (selectedLoginMode == "password") {
                            if (username.isBlank() || password.isBlank()) {
                                Toast.makeText(
                                    context,
                                    localizedContext.getString(R.string.please_enter_username_and_password),
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@clickable
                            }

                            viewModel.login(LoginRequest(username, password), rememberMe)
                            userPrefs.saveLoginCredentials(
                                username,
                                password,
                                rememberMe,
                                "",
                                0,
                                0,
                                ""
                            )
                        } else {
                            navController.navigate(Screens.RecogniseFaceLogin.route)
                        }


                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = if (selectedLoginMode == "password") localizedContext.getString(R.string.login) else localizedContext.getString(
                            R.string.login_with_face
                        ),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        fontFamily = poppins
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TroubleLoginText {
                Toast.makeText(context,
                    localizedContext.getString(R.string.contact_us_clicked), Toast.LENGTH_SHORT).show()
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Configure Custom API",
                color = Color.Blue,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable {
                    showCustomApiDialog = true
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showCustomApiDialog) {

        var customApi by remember {
            mutableStateOf(
                userPrefs.getCustomApi() ?: ""
            )
        }

        Dialog(
            onDismissRequest = {
                showCustomApiDialog = false
            }
        ) {

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Configure Custom API",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Ensure the API URL belongs to an authorized RRGOLD server.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = customApi,
                        onValueChange = {
                            customApi = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Enter API URL")
                        },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        GradientButtonIcon(
                            text = "Cancel",
                            onClick = {
                                showCustomApiDialog = false
                            },
                            icon = painterResource(id = R.drawable.ic_cancel),
                            iconDescription = "Cancel",
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .padding(end = 6.dp)
                        )

                        GradientButtonIcon(
                            text = "Save",
                            onClick = {

                                var finalUrl = customApi.trim()

                                if (finalUrl.isNotEmpty()) {

                                    if (!finalUrl.startsWith("http://") &&
                                        !finalUrl.startsWith("https://")
                                    ) {
                                        finalUrl = "http://$finalUrl"
                                    }

                                   /* if (!finalUrl.endsWith("/")) {
                                        finalUrl += "/"
                                    }*/
                                }

                                userPrefs.saveCustomApi(finalUrl)

                                Toast.makeText(
                                    context,
                                    "Custom API Saved",
                                    Toast.LENGTH_SHORT
                                ).show()

                                showCustomApiDialog = false
                            },
                            icon = painterResource(id = R.drawable.check_circle),
                            iconDescription = "Save",
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
    if (showExpiryPopup) {
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
                    // Header same like below popup
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
                                painter = painterResource(id = R.drawable.ic_warning), // apna icon de
                                contentDescription = localizedContext.getString(R.string.expiry_warning),
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = localizedContext.getString(R.string.expiry_warning),
                                fontSize = 18.sp,
                                color = Color.White,
                                fontFamily = poppins
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Content
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

                    // Buttons same size like below popup
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        GradientButtonIcon(
                            text = localizedContext.getString(R.string.cancel),
                            onClick = {
                                showExpiryPopup = false
                                pendingLoginResponse = null
                            },
                            icon = painterResource(id = R.drawable.ic_cancel),
                            iconDescription = localizedContext.getString(R.string.cancel),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .padding(end = 6.dp)
                        )

                        GradientButtonIcon(
                            text = localizedContext.getString(R.string.continue_text),
                            onClick = {
                                showExpiryPopup = false
                                pendingLoginResponse?.let { completeLogin(it) }
                                pendingLoginResponse = null
                            },
                            icon = painterResource(id = R.drawable.check_circle),
                            iconDescription = localizedContext.getString(R.string.continue_text),
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
}


@Composable
fun CurvedGradientHeader(localizedContext: Context) {
    val headerHeight = 250.dp

    Box(
        modifier = Modifier
            .width(550.13666.dp)
            .height(285.8201.dp)
    ) {
        Canvas(
            Modifier
                .width(550.13666.dp)
                .height(300.8201.dp)
        ) {
            val width = size.width
            val height = size.height

            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(0f, height * 0.3f)
                quadraticTo(
                    width * -0.3f, height * 1.3f,
                    width, height * 0.75f
                )
                lineTo(width, 0.56f)
                close()
            }

            drawPath(
                path = path,
                BackGroundLinerGradient
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                localizedContext.getString(R.string.welcome_to),
                color = Color.White,
                fontSize = 35.sp,
                fontFamily = poppins,
                fontWeight = FontWeight.Bold
            )
            Text(
                localizedContext.getString(R.string.sparkle_rfid),
                color = Color.White,
                fontSize = 35.sp,
                fontFamily = poppins,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                localizedContext.getString(R.string.please_log_in_to_continue),
                color = Color.White,
                fontSize = 14.sp,
                fontFamily = poppins,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun TroubleLoginText(onContactClick: () -> Unit) {
    val context = LocalContext.current
    val userPreferences = UserPreferences.getInstance(context)
    val savedLang = userPreferences.getAppLanguage().ifBlank { "en" }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = currentLocales[0]?.language ?: savedLang
    val localizedContext = LocaleHelper.applyLocale(context, currentLang)
    val annotatedText = buildAnnotatedString {
        append(localizedContext.getString(R.string.trouble_login))
        pushStringAnnotation(tag = "contact", annotation = "contact")
        withStyle(style = SpanStyle(color = Color.Blue, fontWeight = FontWeight.Bold)) {
            append(localizedContext.getString(R.string.contact_us))
        }
        pop()
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        ClickableText(
            text = annotatedText,
            onClick = { offset ->
                annotatedText.getStringAnnotations("contact", offset, offset)
                    .firstOrNull()?.let { onContactClick() }
            },
            style = TextStyle(color = Color.Gray, fontSize = 14.sp)
        )
    }
}

fun getDaysRemaining(expiryDateStr: String?): Long? {
    return try {
        if (expiryDateStr.isNullOrBlank()) return null

        val cleanDate = expiryDateStr.trim().take(10)
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val expiryDate = java.time.LocalDate.parse(cleanDate, formatter)
        val today = java.time.LocalDate.now()

        java.time.temporal.ChronoUnit.DAYS.between(today, expiryDate)
    } catch (e: Exception) {
        android.util.Log.e("FACE_LOGIN", "Date parse failed: $expiryDateStr", e)
        null
    }
}


