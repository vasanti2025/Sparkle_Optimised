package com.loyalstring.rfid.ui.utils

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GradientButtonIcon(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradientColors: List<Color> = listOf(Color(0xFFD32940), Color(0xFF5231A7)),
    icon: Painter? = null,
    iconDescription: String? = null,
    fontSize: Int = 14
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.horizontalGradient(colors = gradientColors))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (icon != null) {
                Image(
                    painter = icon,
                    contentDescription = iconDescription,
                    modifier = Modifier
                        .size(18.dp) // ✅ slightly larger icon for better visibility
                        .padding(end = 4.dp), // tighter spacing with text
                    colorFilter = ColorFilter.tint(Color.White)
                )
            }

            Text(
                text = text,
                color = Color.White,
                fontFamily = poppins,
                fontSize = fontSize.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
