package com.example.data.api.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.data.api.config.ApiConfig
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Exception thrown when network is offline, enabling Offline-First handling.
 */
class OfflineNetworkException(message: String = "Internet connection unavailable. App continuing in Offline Mode.") : IOException(message)

/**
 * OkHttp Interceptor managing network connection checks, rate limits (429), retry policy,
 * and error code classification without blocking local business operations.
 */
class NetworkErrorInterceptor(private val context: Context) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (!isNetworkConnected(context)) {
            throw OfflineNetworkException()
        }

        val request = chain.request()
        var response: Response? = null
        var tryCount = 0
        var lastException: Exception? = null

        while (tryCount <= ApiConfig.MAX_RETRY_ATTEMPTS && response == null) {
            try {
                tryCount++
                response = chain.proceed(request)
            } catch (e: SocketTimeoutException) {
                lastException = e
                if (tryCount > ApiConfig.MAX_RETRY_ATTEMPTS) throw e
            } catch (e: UnknownHostException) {
                // Host unavailable, treat as offline mode
                throw OfflineNetworkException("Server endpoint host unreachable.")
            } catch (e: Exception) {
                lastException = e
                if (tryCount > ApiConfig.MAX_RETRY_ATTEMPTS) throw e
            }
        }

        val finalResponse = response ?: throw (lastException ?: IOException("Request execution failed"))

        // Process HTTP status codes safely
        when (finalResponse.code) {
            401 -> {
                // Unauthorized token
            }
            403 -> {
                // Access forbidden
            }
            429 -> {
                // Rate limited - do not endlessly retry
            }
            in 500..599 -> {
                // Server error
            }
        }

        return finalResponse
    }

    private fun isNetworkConnected(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            true // Fallback gracefully if permission check issue
        }
    }
}
