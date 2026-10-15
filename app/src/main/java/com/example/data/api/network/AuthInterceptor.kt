package com.example.data.api.network

import com.example.data.api.security.SecureIdentityManager
import com.example.data.api.security.SecureTokenManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp Interceptor that attaches secure authentication token and installation metadata
 * headers to all outgoing Developer API requests.
 */
class AuthInterceptor(
    private val tokenManager: SecureTokenManager,
    private val identityManager: SecureIdentityManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val builder = originalRequest.newBuilder()

        // Attach Authorization Token if available
        val accessToken = tokenManager.getAccessToken()
        if (accessToken.isNotBlank()) {
            builder.header("Authorization", "Bearer $accessToken")
        }

        // Attach Identity & System metadata headers
        builder.header("X-App-Installation-ID", identityManager.getInstallationId())
        builder.header("X-Customer-ID", identityManager.getCustomerId())
        builder.header("X-Store-ID", identityManager.getStoreId().toString())
        builder.header("X-App-Version", identityManager.getAppVersion())
        builder.header("Accept", "application/json")
        builder.header("Content-Type", "application/json")

        return chain.proceed(builder.build())
    }
}
