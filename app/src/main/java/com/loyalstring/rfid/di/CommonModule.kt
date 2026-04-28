package com.loyalstring.rfid.di

import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import com.loyalstring.rfid.repository.CommonRepoImple
import com.loyalstring.rfid.repository.CommonRepository
import com.loyalstring.rfid.repository.RfidScanDesktopRepository
import com.loyalstring.rfid.repository.RfidScanDesktopRepositoryImpl
import com.loyalstring.rfid.repository.SampleInRepository
import com.loyalstring.rfid.repository.SampleInRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class CommonModule {
    @Provides
    @Singleton
    fun provideCommonRepository(
        apiService: RetrofitInterface
    ): CommonRepository {
        return CommonRepoImple(apiService)
    }



}