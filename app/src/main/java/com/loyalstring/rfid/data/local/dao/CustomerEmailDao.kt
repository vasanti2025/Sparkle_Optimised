package com.loyalstring.rfid.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.loyalstring.rfid.data.local.entity.CustomerEmailEntity

@Dao
interface CustomerEmailDao {
    @Query("SELECT * FROM customer_email ORDER BY id DESC")
    suspend fun getAllEmails(): List<CustomerEmailEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEmail(email: CustomerEmailEntity)

    @Delete
    suspend fun deleteEmail(email: CustomerEmailEntity)
}
