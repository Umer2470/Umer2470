package com.example.data.api.client

import android.content.Context
import com.example.data.api.config.ApiConfig
import com.example.data.api.service.DeveloperApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class DeveloperApiClient private constructor(context: Context) {

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(ApiConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(ApiConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(ApiConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .retryOnConnectionFailure(true)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(ApiConfig.DEFAULT_DEVELOPER_SERVER_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val apiService: DeveloperApiService = retrofit.create(DeveloperApiService::class.java)

    companion object {
        @Volatile
        private var instance: DeveloperApiClient? = null

        fun getInstance(context: Context): DeveloperApiClient {
            return instance ?: synchronized(this) {
                instance ?: DeveloperApiClient(context.applicationContext).also { instance = it }
            }
        }
    }
}
