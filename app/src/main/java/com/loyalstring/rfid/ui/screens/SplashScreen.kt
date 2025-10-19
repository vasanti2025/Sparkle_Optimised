package com.loyalstring.rfid.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.loyalstring.rfid.R
import com.loyalstring.rfid.navigation.Screens
import com.loyalstring.rfid.viewmodel.LoginViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateNext: (String) -> Unit
) {
    val viewModel: LoginViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        delay(100) // Show splash for 1 second
        val isLoggedIn = viewModel.isUserRemembered()
        onNavigateNext(if (isLoggedIn) Screens.HomeScreen.route else Screens.LoginScreen.route)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F0F0))
    ) {
        // Centered Sparkle logo
        Image(
            painter = painterResource(R.drawable.drawer_icon),
            contentDescription = "Splash Logo",
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .padding(20.dp)
                .align(Alignment.Center)
                .height(300.dp)
        )

        // Bottom-centered Loyal String logo
        Image(
            painter = painterResource(R.drawable.loyal_string_logo1),
            contentDescription = "Loyal String Logo",
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 5.dp)
                .height(20.dp)
        )
    }
}
