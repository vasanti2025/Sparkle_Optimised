package com.loyalstring.rfid.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.loyalstring.rfid.data.model.setting.LocationItem

@Dao
interface LocationDao {
    @Query("SELECT * FROM location_table ORDER BY CreatedOn DESC")
    suspend fun getAll(): List<LocationItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(locations: List<LocationItem>)

    @Query("DELETE FROM location_table")
    suspend fun clearAll()
}