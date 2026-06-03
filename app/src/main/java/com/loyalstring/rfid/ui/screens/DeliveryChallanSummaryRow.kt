package com.loyalstring.rfid.ui.screens

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loyalstring.rfid.R
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.worker.LocaleHelper

@SuppressLint("StringFormatInvalid")
@Composable
fun DeliveryChallanSummaryRow(
    gstPercent: Double = 3.0,
    totalAmount: Double,
    onGstCheckedChange: (Boolean) -> Unit = {},
    // ✅ parent ko GST & final total dene ke liye
    onAmountsChange: (gstAmount: Double, finalAmount: Double) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    var isGstChecked by remember { mutableStateOf(true) }

    // ✅ GST amount & final total calculate based on checkbox
    val gstAmount = remember(isGstChecked, totalAmount, gstPercent) {
        if (isGstChecked) totalAmount * gstPercent / 100.0 else 0.0
    }
    val finalAmount = totalAmount + gstAmount

    // ✅ Jab bhi GST ya total change ho, parent ko bata do
    LaunchedEffect(isGstChecked, totalAmount, gstPercent) {
        onAmountsChange(gstAmount, finalAmount)
    }

    val userPreferences = UserPreferences.getInstance(context)
    val savedLang = userPreferences.getAppLanguage().ifBlank { "en" }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = currentLocales[0]?.language ?: savedLang
    val localizedContext = LocaleHelper.applyLocale(context, currentLang)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 🔹 GST Section (Checkbox INSIDE white background)
        Box(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isGstChecked,
                    onCheckedChange = {
                        isGstChecked = it
                        onGstCheckedChange(it)
                    },
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF1565C0),
                        uncheckedColor = Color.Gray
                    ),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))

                // 🔹 GST XX.XX%
                Text(
                    text =localizedContext.getString( R.string.gst_with_percent,
                        gstPercent
                    ),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            }
        }

        // 🔹 Total Amount Label
        Text(
            text =      localizedContext.getString( R.string.total_amount),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black,
            textAlign = TextAlign.Center
        )

        // 🔹 Amount Box (FINAL amount – base + GST if checked)
        Box(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(6.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text =      localizedContext.getString(R.string.currency_amount,
                    finalAmount
                ),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1565C0)
            )
        }
    }
}
