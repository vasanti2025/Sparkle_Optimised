package com.loyalstring.rfid.di

import com.loyalstring.rfid.data.local.dao.BulkItemDao
import com.loyalstring.rfid.data.local.dao.EpcDao
import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import com.loyalstring.rfid.repository.BulkRepository
import com.loyalstring.rfid.repository.BulkRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object SyncRepositoryModule {

    @Provides
    fun provideBulkRepository(
        apiService: RetrofitInterface,
        bulkItemDao: BulkItemDao,
        epcDao: EpcDao
    ): BulkRepository {
        return BulkRepositoryImpl(
            apiService = apiService,
            bulkItemDao = bulkItemDao,
            epcDao = epcDao
        )
    }
}
