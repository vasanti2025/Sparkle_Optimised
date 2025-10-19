package com.loyalstring.rfid.di

import android.content.Context
import com.loyalstring.rfid.data.reader.RFIDReaderManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ReaderModule {

    @Provides
    fun provideReaderManager(@ApplicationContext context: Context): RFIDReaderManager {
        return RFIDReaderManager(context)
    }

}
