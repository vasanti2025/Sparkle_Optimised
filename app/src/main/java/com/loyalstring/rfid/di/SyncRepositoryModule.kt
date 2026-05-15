package com.loyalstring.rfid.di

import android.content.Context
import com.loyalstring.rfid.data.local.dao.BulkItemDao
import com.loyalstring.rfid.data.local.dao.EpcDao
import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import com.loyalstring.rfid.repository.BulkRepository
import com.loyalstring.rfid.repository.BulkRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object SyncRepositoryModule {

    @Provides
    fun provideBulkRepository(
        @SyncRetrofit syncApi: RetrofitInterface,
        @NormalRetrofit normalApi: RetrofitInterface,
        bulkItemDao: BulkItemDao,
        epcDao: EpcDao,
        @ApplicationContext context: Context
    ): BulkRepository {
        return BulkRepositoryImpl(
            syncApi = syncApi,
            apiService = normalApi,
            bulkItemDao = bulkItemDao,
            epcDao = epcDao,
            context = context
        )
    }
}
