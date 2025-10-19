package com.loyalstring.rfid.di

import android.content.Context
import com.loyalstring.rfid.data.local.dao.BulkItemDao
import com.loyalstring.rfid.data.local.dao.CustomerEmailDao
import com.loyalstring.rfid.data.local.dao.DropdownDao
import com.loyalstring.rfid.data.local.dao.EpcDao
import com.loyalstring.rfid.data.local.dao.OrderItemDao
import com.loyalstring.rfid.data.local.dao.TransferTypeDao
import com.loyalstring.rfid.data.local.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideDropdownDao(db: AppDatabase): DropdownDao {
        return db.dropdownDao()
    }

    @Provides
    fun provideBulkItemDao(db: AppDatabase): BulkItemDao {
        return db.bulkItemDao()
    }

    @Provides
    fun provideOrderItemDao(db: AppDatabase): OrderItemDao {
        return db.orderItemDao()
    }

    @Provides
    fun provideTransferDao(db: AppDatabase): TransferTypeDao {
        return db.stockTransferDao()
    }

    @Provides
    fun provideEpcDao(db: AppDatabase): EpcDao {
        return db.epcDao()
    }

    @Provides
    fun provideCustomerEmailDao(db: AppDatabase): CustomerEmailDao {
        return db.customerEmailDao()
    }
}
