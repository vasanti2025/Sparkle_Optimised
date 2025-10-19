package com.loyalstring.rfid.di


import com.loyalstring.rfid.data.local.dao.TransferTypeDao
import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import com.loyalstring.rfid.repository.TransferRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TransferModule {

    @Provides
    @Singleton
    fun provideTransferRepository(
        retrofitInterface: RetrofitInterface,
        dao: TransferTypeDao
    ): TransferRepository {
        return TransferRepository(retrofitInterface, dao)
    }
}
