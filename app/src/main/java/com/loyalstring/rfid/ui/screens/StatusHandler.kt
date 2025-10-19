package com.loyalstring.rfid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.Text
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.StateFlow

/**
 * Shows an optional inline banner with progress + status text when [isWorking] is true,
 * and automatically shows a Snackbar on completion (when status contains [successKeyword] or [failureKeyword]).
 */
@Composable
fun StatusHandler(
    scaffoldState: ScaffoldState,
    isWorking: StateFlow<Boolean>,
    statusText: StateFlow<String>,
    successKeyword: String = "success",
    failureKeyword: String = "failed"
) {
    val working by isWorking.collectAsState()
    val status by statusText.collectAsState()

    // Inline banner
    if (working) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE0E0E0))
                .padding(8.dp)
        ) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(text = status, modifier = Modifier.padding(top = 4.dp))
        }
    }

    // Snackbar on completion/error
    LaunchedEffect(status) {
        when {
            status.contains(successKeyword, ignoreCase = true) ||
                    status.contains(failureKeyword, ignoreCase = true) -> {
                scaffoldState.snackbarHostState.showSnackbar(
                    message = status,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }
}
