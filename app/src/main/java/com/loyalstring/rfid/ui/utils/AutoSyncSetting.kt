package com.loyalstring.rfid.ui.utils

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.loyalstring.rfid.worker.cancelPeriodicSync
import com.loyalstring.rfid.worker.schedulePeriodicSync


@Composable
fun AutoSyncSetting(userPref: UserPreferences) {
    val context = LocalContext.current
    val SYNC_DATA_WORKER = "sync_data_worker"

    var enabled by remember {
        mutableStateOf(
            userPref.getBoolean(
                UserPreferences.KEY_AUTOSYNC_ENABLED,
                false
            )
        )
    }
    var interval by remember {
        mutableStateOf(
            userPref.getInt(
                UserPreferences.KEY_AUTOSYNC_INTERVAL_MIN,
                15
            )
        )
    }
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {

        // Row: Title + Toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Auto Sync",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.weight(1f),
                fontFamily = poppins
            )

            Switch(
                checked = enabled,
                onCheckedChange = { newValue ->
                    enabled = newValue
                    userPref.saveBoolean(UserPreferences.KEY_AUTOSYNC_ENABLED, newValue)

                    if (enabled) {
                        schedulePeriodicSync(context,SYNC_DATA_WORKER, interval.toLong())
                    } else {
                        cancelPeriodicSync(context,SYNC_DATA_WORKER)
                        //cancelAlarmSync(context)
                    }

                }
            )
        }

        // Only show interval selection if Auto Sync is ON
        if (enabled) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Sync Interval:",
                    fontFamily = poppins,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )

                Box {
                    OutlinedButton(onClick = { expanded = true }) {
                        Text("$interval min", fontFamily = poppins)
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        val intervals = listOf(15, 30, 60, 24 * 60) // 24hr = 1440 min
                        intervals.forEach { value ->
                            val label = when (value) {
                                15 -> "15 min"
                                30 -> "30 min"
                                60 -> "1 hour"
                                24 * 60 -> "24 hours"
                                else -> "$value min"
                            }

                            DropdownMenuItem(
                                text = { Text(label, fontFamily = poppins) },
                                onClick = {
                                    interval = value
                                    userPref.saveInt(
                                        UserPreferences.KEY_AUTOSYNC_INTERVAL_MIN,
                                        value
                                    )
                                    expanded = false
                                    // Re-schedule immediately with new interval
                                    if (enabled) {
                                        schedulePeriodicSync(context, SYNC_DATA_WORKER,interval.toLong())
                                    } else {
                                        cancelPeriodicSync(context,SYNC_DATA_WORKER)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
