package com.example.data.api.network

import android.content.Context
import com.example.data.api.config.ApiConfig
import com.example.data.api.security.SecureIdentityManager
import com.example.data.api.security.SecureTokenManager
import com.example.data.api.service.DeveloperApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Central ApiClient factory configuring Retrofit, OkHttpClient, timeouts,
 * JSON converters, and security interceptors for the Developer API layer.
 */
class ApiClient private constructor(private val context: Context) {

    private val identityManager = SecureIdentityManager.getInstance(context)
    private val tokenManager = SecureTokenManager.getInstance(context)

    init {
        ApiConfig.init(context)
    }

    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(ApiConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(ApiConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(ApiConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(NetworkErrorInterceptor(context))
        .addInterceptor(AuthInterceptor(tokenManager, identityManager))
        .addInterceptor(loggingInterceptor)
        .build()

    @Volatile
    private var currentBaseUrl: String = ApiConfig.getBaseUrl()

    @Volatile
    private var retrofitInstance: Retrofit = buildRetrofit(currentBaseUrl)

    @Volatile
    private var apiServiceInstance: DeveloperApiService = retrofitInstance.create(DeveloperApiService::class.java)

    private fun buildRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val apiService: DeveloperApiService
        get() {
            val activeUrl = ApiConfig.getBaseUrl()
            if (activeUrl != currentBaseUrl) {
                synchronized(this) {
                    if (activeUrl != currentBaseUrl) {
                        currentBaseUrl = activeUrl
                        retrofitInstance = buildRetrofit(activeUrl)
                        apiServiceInstance = retrofitInstance.create(DeveloperApiService::class.java)
                    }
                }
            }
            return apiServiceInstance
        }

    fun notifyBaseUrlChanged() {
        synchronized(this) {
            currentBaseUrl = ApiConfig.getBaseUrl()
            retrofitInstance = buildRetrofit(currentBaseUrl)
            apiServiceInstance = retrofitInstance.create(DeveloperApiService::class.java)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: ApiClient? = null

        fun getInstance(context: Context): ApiClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ApiClient(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
