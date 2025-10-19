package com.loyalstring.rfid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.loyalstring.rfid.data.model.setting.LocationGetRequest
import com.loyalstring.rfid.data.model.setting.LocationGetResponse
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/*@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationListScreen(
    onBack: () -> Unit,
    navController: NavHostController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val locationResponse by viewModel.locationResponse.collectAsState()
    val locationError by viewModel.locationError.collectAsState()

    var isLoading by remember { mutableStateOf(true) }

    // Fetch locations once when screen opens
    LaunchedEffect(Unit) {
        isLoading = true
        viewModel.fetchLocation(
            locationRequest = LocationGetRequest(
                ClientCode = "LS000093",
                UserId = 1,
                BranchId = 2
            )
        )
        scope.launch {
            kotlinx.coroutines.delay(1000)
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = "Location List",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                showCounter = false,
                selectedCount = 0,
                onCountSelected = {}
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }

                locationError != null -> {
                    Text(
                        text = locationError ?: "Unknown error",
                        color = Color.Red,
                        fontSize = 16.sp
                    )
                }

                locationResponse.isEmpty() -> {
                    Text(text = "No locations found", fontSize = 16.sp)
                }

                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {

                        // Header Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE0E0E0))
                                .padding(vertical = 8.dp, horizontal = 12.dp)
                        ) {
                            Text("S.no", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Date", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("UserId", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Address", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Divider(color = Color.Gray)

                        // Data Rows
                        LazyColumn {
                            itemsIndexed(locationResponse) { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${index + 1}", modifier = Modifier.weight(0.3f), fontSize = 12.sp)
                                    val date = try {
                                        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS", Locale.getDefault())
                                        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                        formatter.format(parser.parse(item.CreatedOn) ?: Date())
                                    } catch (e: Exception) { item.CreatedOn }
                                    Text(date.toString(), modifier = Modifier.weight(1f), fontSize = 12.sp)
                                    Text(item.UserId.toString(), modifier = Modifier.weight(1f), fontSize = 12.sp,  textAlign = TextAlign.Center)
                                    Text(item.Address ?: "-", modifier = Modifier.weight(1.5f), fontSize = 12.sp)
                                }
                                Divider()
                            }
                        }



                    }
                }
            }
        }
    }
}*/

@Composable
fun LocationListScreen(
    onBack: () -> Unit,
    navController: NavHostController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val locations by viewModel.localLocations.collectAsState()
    var isLoading by remember { mutableStateOf(true) }

    // Fetch locations from DB when screen opens
    LaunchedEffect(Unit) {
        isLoading = true
        viewModel.fetchLocationsFromDb()
        scope.launch {
            kotlinx.coroutines.delay(500)
            isLoading = false
        }
    }

    Scaffold(
        topBar = {  GradientTopBar(
            title = "Location List",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            showCounter = false,
            selectedCount = 0,
            onCountSelected = {}
        ) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                locations.isEmpty() -> Text("No locations found", fontSize = 16.sp)
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black)
                                .padding(vertical = 8.dp, horizontal = 12.dp)
                        ) {
                            Text("S.no", modifier = Modifier.weight(0.5f), fontWeight = FontWeight.Bold, fontSize = 12.sp,color = Color.White)
                            Text("Date", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp,color = Color.White)
                            Text("UserId", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 12.sp,color = Color.White)
                            Text("Address", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 12.sp,color = Color.White)
                        }

                        Divider(color = Color.Gray)

                        // Data Rows
                        LazyColumn {
                            itemsIndexed(locations) { index, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${index + 1}", modifier = Modifier.weight(0.3f), fontSize = 12.sp)
                                    val date = try {
                                        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS", Locale.getDefault())
                                        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                        formatter.format(parser.parse(item.CreatedOn) ?: Date())
                                    } catch (e: Exception) { item.CreatedOn }
                                    Text(date.toString(), modifier = Modifier.weight(1f), fontSize = 12.sp)
                                    Text(item.UserId.toString(), modifier = Modifier.weight(1f), fontSize = 12.sp, textAlign = TextAlign.Center)
                                    Text(item.Address ?: "-", modifier = Modifier.weight(1.5f), fontSize = 12.sp)
                                }
                                Divider()
                            }
                        }
                    }
                }
            }
        }
    }
}

