package com.loyalstring.rfid.di
import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import com.loyalstring.rfid.repository.DeliveryChallanRepository
import com.loyalstring.rfid.repository.DeliveryChallanRepositoryImpl

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
@Module
@InstallIn(SingletonComponent::class)
object DeliveryChallanModule {
    @Provides
    @Singleton
    fun provideDeliveryChallanRepository(
        apiService: RetrofitInterface
    ): DeliveryChallanRepository {
        return DeliveryChallanRepositoryImpl(apiService)
    }
}