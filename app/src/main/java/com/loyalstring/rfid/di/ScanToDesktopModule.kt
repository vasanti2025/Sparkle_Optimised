package com.loyalstring.rfid.di

import com.loyalstring.rfid.repository.RfidScanDesktopRepository
import com.loyalstring.rfid.repository.RfidScanDesktopRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ScanToDesktopModule {

    @Binds
    @Singleton
    abstract fun bindRfidScanDesktopRepository(
        impl: RfidScanDesktopRepositoryImpl
    ): RfidScanDesktopRepository
}