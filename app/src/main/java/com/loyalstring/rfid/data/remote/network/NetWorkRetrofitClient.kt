package com.loyalstring.rfid.data.remote.network
/*
import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import com.loyalstring.rfid.di.NormalRetrofit
import com.loyalstring.rfid.di.SyncRetrofit
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import okio.BufferedSink
import okio.GzipSink
import okio.buffer

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object NetWorkRetrofitClient {

    @Provides
    fun provideBaseUrl() = "https://rrgold.loyalstring.co.in/"

    */
/* ---------------- NORMAL OKHTTP ---------------- *//*


    @Provides
    @Singleton
    @NormalRetrofit
    fun provideNormalOkHttp(): OkHttpClient =
        OkHttpClient.Builder()
            //.addInterceptor(GzipRequestInterceptor())
            .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .writeTimeout(5, TimeUnit.MINUTES)
            .retryOnConnectionFailure(true)
            .build()

    */
/* ---------------- SYNC OKHTTP (STREAMING) ---------------- *//*


    @Provides
    @Singleton
    @SyncRetrofit
    fun provideSyncOkHttp(): OkHttpClient =
        OkHttpClient.Builder()
            .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.MINUTES)
            .writeTimeout(10, TimeUnit.MINUTES)
            .retryOnConnectionFailure(true)
            .build()

    */
/* ---------------- NORMAL RETROFIT ---------------- *//*


    @Provides
    @Singleton
    @NormalRetrofit
    fun provideNormalRetrofit(
        @NormalRetrofit okHttpClient: OkHttpClient,
        baseUrl: String
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()

    */
/* ---------------- SYNC RETROFIT (NO CONVERTERS) ---------------- *//*


    @Provides
    @Singleton
    @SyncRetrofit
    fun provideSyncRetrofit(
        @SyncRetrofit okHttpClient: OkHttpClient,
        baseUrl: String
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .build()

    */
/* ---------------- API SERVICES ---------------- *//*


    @Provides
    @Singleton
    @NormalRetrofit
    fun provideNormalApi(
        @NormalRetrofit retrofit: Retrofit
    ): RetrofitInterface =
        retrofit.create(RetrofitInterface::class.java)

    @Provides
    @Singleton
    @SyncRetrofit
    fun provideSyncApi(
        @SyncRetrofit retrofit: Retrofit
    ): RetrofitInterface =
        retrofit.create(RetrofitInterface::class.java)

    */
/* ---------------- DEFAULT API (CRITICAL FIX) ---------------- *//*


    @Provides
    @Singleton
    fun provideDefaultApi(
        @NormalRetrofit api: RetrofitInterface
    ): RetrofitInterface = api
}

class GzipRequestInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        if (original.body == null || original.header("Content-Encoding") != null) {
            return chain.proceed(original)
        }

        val compressedRequest = original.newBuilder()
            .header("Content-Encoding", "gzip")
            .method(original.method, gzip(original.body!!))
            .build()

        return chain.proceed(compressedRequest)
    }

    private fun gzip(body: RequestBody): RequestBody =
        object : RequestBody() {
            override fun contentType() = body.contentType()
            override fun writeTo(sink: BufferedSink) {
                val gzipSink = GzipSink(sink).buffer()
                body.writeTo(gzipSink)
                gzipSink.close()
            }
        }
}
*/


import android.content.Context
import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import com.loyalstring.rfid.di.NormalRetrofit
import com.loyalstring.rfid.di.SyncRetrofit
import com.loyalstring.rfid.ui.utils.UserPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink
import okio.GzipSink
import okio.buffer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetWorkRetrofitClient {

    private const val DEFAULT_BASE_URL = "https://rrgold.loyalstring.co.in/"

    @Provides
    fun provideBaseUrl(): String = DEFAULT_BASE_URL

    /* ---------------- NORMAL OKHTTP ---------------- */

    @Provides
    @Singleton
    @NormalRetrofit
    fun provideNormalOkHttp(
        @ApplicationContext context: Context
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(DynamicBaseUrlInterceptor(context))
            // .addInterceptor(GzipRequestInterceptor())
            .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .writeTimeout(5, TimeUnit.MINUTES)
            .retryOnConnectionFailure(true)
            .build()

    /* ---------------- SYNC OKHTTP ---------------- */

    @Provides
    @Singleton
    @SyncRetrofit
    fun provideSyncOkHttp(
        @ApplicationContext context: Context
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(DynamicBaseUrlInterceptor(context))
            .protocols(listOf(okhttp3.Protocol.HTTP_1_1))
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.MINUTES)
            .writeTimeout(10, TimeUnit.MINUTES)
            .retryOnConnectionFailure(true)
            .build()

    /* ---------------- NORMAL RETROFIT ---------------- */

    @Provides
    @Singleton
    @NormalRetrofit
    fun provideNormalRetrofit(
        @NormalRetrofit okHttpClient: OkHttpClient,
        baseUrl: String
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .client(okHttpClient)
            .build()

    /* ---------------- SYNC RETROFIT ---------------- */

    @Provides
    @Singleton
    @SyncRetrofit
    fun provideSyncRetrofit(
        @SyncRetrofit okHttpClient: OkHttpClient,
        baseUrl: String
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .build()

    /* ---------------- API SERVICES ---------------- */

    @Provides
    @Singleton
    @NormalRetrofit
    fun provideNormalApi(
        @NormalRetrofit retrofit: Retrofit
    ): RetrofitInterface =
        retrofit.create(RetrofitInterface::class.java)

    @Provides
    @Singleton
    @SyncRetrofit
    fun provideSyncApi(
        @SyncRetrofit retrofit: Retrofit
    ): RetrofitInterface =
        retrofit.create(RetrofitInterface::class.java)

    /* ---------------- DEFAULT API ---------------- */

    @Provides
    @Singleton
    fun provideDefaultApi(
        @NormalRetrofit api: RetrofitInterface
    ): RetrofitInterface = api
}

/* ---------------- DYNAMIC BASE URL INTERCEPTOR ---------------- */

class DynamicBaseUrlInterceptor(
    private val context: Context
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val customUrl = UserPreferences.getInstance(context)
            .getCustomApi()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        android.util.Log.d("API_URL_DEBUG", "Saved customUrl = $customUrl")
        android.util.Log.d("API_URL_DEBUG", "Original URL = ${originalRequest.url}")

        if (customUrl.isNullOrBlank()) {
            android.util.Log.d("API_URL_DEBUG", "Custom empty, using default URL")
            return chain.proceed(originalRequest)
        }

        val finalCustomUrl = if (customUrl.endsWith("/")) customUrl else "$customUrl/"
        val customHttpUrl = finalCustomUrl.toHttpUrlOrNull()

        if (customHttpUrl == null) {
            android.util.Log.e("API_URL_DEBUG", "Invalid custom URL = $finalCustomUrl")
            return chain.proceed(originalRequest)
        }

        val newUrl = originalRequest.url.newBuilder()
            .scheme(customHttpUrl.scheme)
            .host(customHttpUrl.host)
            .port(customHttpUrl.port)
            .build()

        android.util.Log.d("API_URL_DEBUG", "Final URL = $newUrl")

        val newRequest = originalRequest.newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(newRequest)
    }
}

/* ---------------- GZIP INTERCEPTOR OPTIONAL ---------------- */

class GzipRequestInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()

        if (original.body == null || original.header("Content-Encoding") != null) {
            return chain.proceed(original)
        }

        val compressedRequest = original.newBuilder()
            .header("Content-Encoding", "gzip")
            .method(original.method, gzip(original.body!!))
            .build()

        return chain.proceed(compressedRequest)
    }

    private fun gzip(body: RequestBody): RequestBody =
        object : RequestBody() {
            override fun contentType() = body.contentType()

            override fun writeTo(sink: BufferedSink) {
                val gzipSink = GzipSink(sink).buffer()
                body.writeTo(gzipSink)
                gzipSink.close()
            }
        }
}
