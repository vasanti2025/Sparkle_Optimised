package com.loyalstring.rfid.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

fun schedulePeriodicSync(
    context: Context,
    taskType: String,
    intervalMinutes: Long,
    inputData: Data? = null
) {
    val workData = when (taskType) {
        SyncDataWorker.LOCATION_SYNC_DATA_WORKER -> {
            val latitude = inputData?.getString("latitude") ?: ""
            val longitude = inputData?.getString("longitude") ?: ""
            val address = inputData?.getString("address") ?: ""

            workDataOf(
                "task_type" to taskType,
                "latitude" to latitude,
                "longitude" to longitude,
                "address" to address
            )
        }
        SyncDataWorker.SYNC_DATA_WORKER -> workDataOf(
            "task_type" to taskType,

        )
        else -> workDataOf("task_type" to taskType) // fallback
    }

    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    val request = PeriodicWorkRequestBuilder<SyncDataWorker>(
        intervalMinutes.coerceAtLeast(15),
        TimeUnit.MINUTES
    )
        .setConstraints(constraints)
        .setInputData(workData)
        .build()

    WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
        taskType, // unique per type
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )

    Log.d("WORKER_SCHEDULER", "✅ Scheduled $taskType every $intervalMinutes minutes")
}

/**
 * Cancel a specific periodic task
 */
fun cancelPeriodicSync(context: Context, taskType: String) {
    WorkManager.getInstance(context.applicationContext)
        .cancelUniqueWork(taskType)
    Log.d("WORKER_SCHEDULER", "🛑 Canceled worker: $taskType")
}

