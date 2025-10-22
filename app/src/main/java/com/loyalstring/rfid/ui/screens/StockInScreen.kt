package com.loyalstring.rfid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.loyalstring.rfid.R
import com.loyalstring.rfid.navigation.GradientTopBar

@Composable
fun StockInScreen(
    onBack: () -> Unit,
    navController: NavHostController
) {
    var shouldNavigateBack by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var selectedTransferType by remember { mutableStateOf("Transfer Type") }

    // Shared horizontal scroll state for header + rows
    val horizontalScrollState = rememberScrollState()

    Scaffold(
        topBar = {
            GradientTopBar(
                title = "Stock Transfer",
                navigationIcon = {
                    IconButton(onClick = { shouldNavigateBack = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                showCounter = false
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionButton(
                    text = "Approve",
                    icon = painterResource(R.drawable.check_circle),
                    onClick = { /* TODO: Approve logic */ }
                )
                ActionButton(
                    text = "Reject",
                    icon = painterResource(R.drawable.ic_cancel),
                    onClick = { /* TODO: Reject logic */ }
                )
                ActionButton(
                    text = "Lost",
                    icon = painterResource(R.drawable.ic_lost),
                    onClick = { /* TODO: Lost logic */ }
                )
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.White)
        ) {
            // 🔹 Dropdown + Filter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    Button(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFF8F8F8),
                            contentColor = Color.Black
                        ),
                        elevation = null,
                        modifier = Modifier
                            .height(40.dp)
                            .width(200.dp)
                    ) {
                        Text(selectedTransferType)
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("Internal", "External", "Vendor").forEach { type ->
                            DropdownMenuItem(onClick = {
                                selectedTransferType = type
                                expanded = false
                            }) {
                                Text(type)
                            }
                        }
                    }
                }

                IconButton(onClick = { /* TODO: Filter logic */ }) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "Filter",
                        tint = Color(0xFF3C3C3C)
                    )
                }
            }

            // 🔹 Table Header (fixed Sr + Pending, scrolls middle)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF3C3C3C))
                    .padding(vertical = 8.dp)
            ) {
                // Fixed Sr column
                Text(
                    text = "Sr",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .width(50.dp)
                        .padding(horizontal = 4.dp)
                )

                // Shared scrollable middle section
                Row(
                    modifier = Modifier
                        .horizontalScroll(horizontalScrollState)
                        .weight(1f)
                ) {
                    listOf("From", "To", "G Wt", "N Wt").forEach { header ->
                        Text(
                            text = header,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .width(80.dp)
                                .padding(horizontal = 4.dp)
                        )
                    }
                }

                // Fixed Pending column
                Text(
                    text = "Pending",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .width(80.dp)
                        .padding(horizontal = 4.dp)
                )
            }

            // 🔹 Scrollable Data Rows (header scrolls together)
            val dummyList = listOf(
                listOf("Display", "Box1", "Box1", "Box1", "2"),
                listOf("Packet 1", "Box2", "Box2", "Box2", "1"),
                listOf("Counter", "Box3", "Box3", "Box3", "3"),
                listOf("Locker", "Box4", "Box4", "Box4", "0")
            )

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(dummyList) { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (index % 2 == 0) Color.White else Color(0xFFF7F7F7))
                            .padding(vertical = 6.dp)
                    ) {
                        // Fixed Sr column
                        Text(
                            text = "${index + 1}",
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .width(50.dp)
                                .padding(horizontal = 4.dp)
                        )

                        // Shared scrollable middle columns
                        Row(
                            modifier = Modifier
                                .horizontalScroll(horizontalScrollState)
                                .weight(1f)
                        ) {
                            item.subList(0, item.size - 1).forEach { text ->
                                Text(
                                    text = text,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .width(80.dp)
                                        .padding(horizontal = 4.dp)
                                )
                            }
                        }

                        // Fixed Pending column
                        Text(
                            text = item.last(),
                            color = Color(0xFF1976D2),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .width(80.dp)
                                .padding(horizontal = 4.dp)
                        )
                    }

                    Divider(color = Color(0xFFE0E0E0), thickness = 1.dp)
                }
            }
        }

        if (shouldNavigateBack) onBack()
    }
}
