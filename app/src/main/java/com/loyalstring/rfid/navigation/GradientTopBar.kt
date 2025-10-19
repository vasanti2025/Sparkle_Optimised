package com.loyalstring.rfid.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loyalstring.rfid.ui.utils.poppins

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradientTopBar(
    title: String,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    showCounter: Boolean = false,
    selectedCount: Int = 1,
    onCountSelected: (Int) -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }

    navigationIcon?.let {
        TopAppBar(
            title = { Text(title, color = Color.White, fontFamily = poppins) },
            navigationIcon = it,
            actions = {
                actions()

                if (showCounter) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .padding(4.dp)
                            .background(
                                Color.White,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable { expanded = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = selectedCount.toString(),
                            color = Color.Red,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = poppins
                        )
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .height(500.dp)
                            .width(50.dp)
                    ) {
                        (1..30).forEach { count ->
                            DropdownMenuItem(
                                text = { Text(count.toString(), fontFamily = poppins) },
                                onClick = {
                                    onCountSelected(count)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF5231A7), Color(0xFFD32940))
                    )
                )
        )
    }
}

