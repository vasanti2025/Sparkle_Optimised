package com.loyalstring.rfid.ui.screens

import android.widget.Toast
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
import com.loyalstring.rfid.ui.utils.NetworkUtils
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.BulkViewModel
import com.loyalstring.rfid.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(navController: NavController, viewModel: LoginViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val bulkviewmodel: BulkViewModel = hiltViewModel()
    val userPrefs = remember { UserPreferences(context) }

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(false) }

    val loginResponse by viewModel.loginResponse.observeAsState()
    val isLoading = loginResponse is Resource.Loading
    val errorMessage = (loginResponse as? Resource.Error)?.message
    val loginSuccess = loginResponse is Resource.Success


    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(loginSuccess) {
        if (loginSuccess) {
            val loginData = (loginResponse as? Resource.Success<LoginResponse>)?.data
            loginData?.let { response ->
                userPrefs.saveToken(response.token.orEmpty())
                userPrefs.saveUserName(response.employee?.username.toString())
                userPrefs.saveEmployee(response.employee)
                userPrefs.setLoggedIn(true)
                userPrefs.saveClient(response.employee?.clients!!)

                launch {
                    bulkviewmodel.syncRFIDDataIfNeeded(context)
                }
                navController.navigate(Screens.HomeScreen.route)
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
            CurvedGradientHeader()

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
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
                    Text("Remember Me", fontSize = 14.sp)
                }

                Text("Forgot Password?", color = Color.Blue, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .defaultMinSize(minHeight = 50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BackgroundGradient)
                    .clickable(enabled = !isLoading) {
                        if (username.isBlank() || password.isBlank()) {
                            Toast.makeText(
                                context,
                                "Please enter username and password",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@clickable
                        }
                        if (!NetworkUtils.isNetworkAvailable(context)) {
                            Toast.makeText(
                                context,
                                "Please Check Your Internet Connection",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@clickable
                        }

                        viewModel.login(LoginRequest(username, password), rememberMe)
                        userPrefs.saveLoginCredentials(username, password, rememberMe)

                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(
                        "Login",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        fontFamily = poppins
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TroubleLoginText {
                Toast.makeText(context, "Contact Us clicked", Toast.LENGTH_SHORT).show()
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun CurvedGradientHeader() {
    val headerHeight = 250.dp

    Box(
        modifier = Modifier
            .width(550.13666.dp)
            .height(300.8201.dp)
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
                "Welcome to",
                color = Color.White,
                fontSize = 35.sp,
                fontFamily = poppins,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Sparkle RFID",
                color = Color.White,
                fontSize = 35.sp,
                fontFamily = poppins,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(40.dp))
            Text(
                "Please log in to continue",
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
    val annotatedText = buildAnnotatedString {
        append("Trouble login? ")
        pushStringAnnotation(tag = "contact", annotation = "contact")
        withStyle(style = SpanStyle(color = Color.Blue, fontWeight = FontWeight.Bold)) {
            append("Contact US")
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
