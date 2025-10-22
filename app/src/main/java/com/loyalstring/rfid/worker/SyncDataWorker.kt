package com.loyalstring.rfid.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.loyalstring.rfid.data.local.db.AppDatabase
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.model.setting.LocationGetRequest
import com.loyalstring.rfid.data.model.setting.LocationItem
import com.loyalstring.rfid.data.model.setting.LocationSyncRequest
import com.loyalstring.rfid.repository.BulkRepository
import com.loyalstring.rfid.repository.SettingRepository
import com.loyalstring.rfid.ui.utils.UserPreferences
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background worker that runs automatically at the selected Auto Sync interval.
 * Injected with Hilt so we can use repository, database, or API calls safely.
 */
@HiltWorker
class SyncDataWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted private val params: WorkerParameters,
    private val repo: BulkRepository,

    private val settingRepository: SettingRepository,
    private val userPreferences: UserPreferences
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val taskType = inputData.getString("task_type") ?: return Result.failure()

        return try {
            when (taskType) {
                LOCATION_SYNC_DATA_WORKER -> {
                    try {
                        // 1️⃣ Add location sync call
                        val addLocationResponse = settingRepository.addLocation(
                            LocationSyncRequest(
                                userPreferences.getEmployee(Employee::class.java)?.clientCode.toString(),
                                userPreferences.getEmployee(Employee::class.java)?.id!!,
                                userPreferences.getEmployee(Employee::class.java)?.defaultBranchId!!,
                                inputData.getString("latitude").toString(),
                                inputData.getString("longitude").toString(),
                                inputData.getString("address").toString()
                            )
                        )

                        if (addLocationResponse.isSuccessful) {
                            // 2️⃣ Call getLocation API after success
                            val clientCode = userPreferences.getEmployee(Employee::class.java)?.clientCode ?: return Result.failure()
                            val getLocationResponse = settingRepository.getLocation(LocationGetRequest(
                                ClientCode =  userPreferences.getEmployee(Employee::class.java)?.clientCode.toString(),
                                UserId =   userPreferences.getEmployee(Employee::class.java)?.id!!,
                                BranchId =  userPreferences.getEmployee(Employee::class.java)?.defaultBranchId!!
                            ))

                            if (getLocationResponse.isSuccessful) {
                                // 3️⃣ Flatten and filter non-null items
                                val allLocations: List<LocationItem> = getLocationResponse.body() // List<LocationItem>?

                                    ?.filterNotNull() // remove nulls
                                    ?: emptyList() // fallback to empty list if null


                                // 4️⃣ Save to Room database
                                val db = AppDatabase.getDatabase(applicationContext)
                                db.locationDao().clearAll() // optional: clear old data
                                db.locationDao().insertAll(allLocations)

                                Log.d("LocationWorker", "Saved ${allLocations?.size} locations to database.")
                                return Result.success()
                            } else {
                                Log.e("LocationWorker", "getLocation failed: ${getLocationResponse.errorBody()?.string()}")
                                return Result.retry()
                            }
                        } else {
                            Log.e("LocationWorker", "addLocation failed: ${addLocationResponse.errorBody()?.string()}")
                            return Result.retry()
                        }
                    } catch (e: Exception) {
                        Log.e("LocationWorker", "Error syncing location data: ${e.message}", e)
                        return Result.retry()
                    }
                }

                SYNC_DATA_WORKER -> {
                    // ✅ Perform your repo sync call
                    repo.syncBulkItemsFromServer(
                        ClientCodeRequest(
                            userPreferences.getEmployee(Employee::class.java)?.clientCode
                        )
                    )
                    Result.success()
                }

                else -> {
                    // Unknown task
                    Result.failure()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }


    companion object {
        const val SYNC_DATA_WORKER = "sync_data_worker"
        const val LOCATION_SYNC_DATA_WORKER = "loaction_sync_data_worker"
    }
}

