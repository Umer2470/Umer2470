package com.example.data.api.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.data.api.config.ApiConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

enum class ConnectionState {
    ONLINE_CONNECTED,     // 🟢 Internet ON + Server Reachable
    ONLINE_UNREACHABLE,   // 🟡 Internet ON + Server Unreachable
    OFFLINE               // 🔴 Internet OFF
}

data class DetailedConnectionStatus(
    val state: ConnectionState = ConnectionState.OFFLINE,
    val isInternetAvailable: Boolean = false,
    val isServerReachable: Boolean = false,
    val latencyMs: Long = 0L,
    val httpCode: Int = 0,
    val statusMessage: String = "No Internet Connection",
    val lastCheckedTimestamp: Long = 0L
)

/**
 * Real-time Network Connectivity & Developer Server Reachability Monitor.
 * Automatically checks network state changes and pings the configured Developer API server.
 */
class NetworkConnectivityMonitor private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _connectionStatus = MutableStateFlow(DetailedConnectionStatus())
    val connectionStatus: StateFlow<DetailedConnectionStatus> = _connectionStatus.asStateFlow()

    private val pingClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private var onNetworkRestoredCallback: (() -> Unit)? = null

    init {
        registerNetworkCallback()
        checkConnectionNow()
    }

    companion object {
        @Volatile
        private var INSTANCE: NetworkConnectivityMonitor? = null

        fun getInstance(context: Context): NetworkConnectivityMonitor {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NetworkConnectivityMonitor(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun setOnNetworkRestoredCallback(callback: () -> Unit) {
        this.onNetworkRestoredCallback = callback
    }

    private fun registerNetworkCallback() {
        if (connectivityManager == null) return

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        try {
            connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    scope.launch {
                        pingServerInternal()
                        onNetworkRestoredCallback?.invoke()
                    }
                }

                override fun onLost(network: Network) {
                    scope.launch {
                        val isStillConnected = checkRawInternetAvailable()
                        if (!isStillConnected) {
                            _connectionStatus.value = DetailedConnectionStatus(
                                state = ConnectionState.OFFLINE,
                                isInternetAvailable = false,
                                isServerReachable = false,
                                latencyMs = 0L,
                                httpCode = 0,
                                statusMessage = "No Internet Connection",
                                lastCheckedTimestamp = System.currentTimeMillis()
                            )
                        } else {
                            pingServerInternal()
                        }
                    }
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    if (hasInternet) {
                        scope.launch {
                            pingServerInternal()
                        }
                    }
                }
            })
        } catch (e: Exception) {
            // Fallback for restricted environments
        }
    }

    fun checkRawInternetAvailable(): Boolean {
        return try {
            val cm = connectivityManager ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }

    fun checkConnectionNow() {
        scope.launch {
            pingServerInternal()
        }
    }

    suspend fun pingServer(): DetailedConnectionStatus {
        return pingServerInternal()
    }

    private suspend fun pingServerInternal(): DetailedConnectionStatus {
        val hasInternet = checkRawInternetAvailable()
        val now = System.currentTimeMillis()

        if (!hasInternet) {
            val status = DetailedConnectionStatus(
                state = ConnectionState.OFFLINE,
                isInternetAvailable = false,
                isServerReachable = false,
                latencyMs = 0L,
                httpCode = 0,
                statusMessage = "No Internet Connection",
                lastCheckedTimestamp = now
            )
            _connectionStatus.value = status
            return status
        }

        // Ping developer API health endpoint
        val baseUrl = ApiConfig.getBaseUrl()
        val healthUrl = if (baseUrl.endsWith("/")) "${baseUrl}health" else "$baseUrl/health"

        val startTime = System.currentTimeMillis()
        var latency = 0L
        var httpCode = 0
        var isReachable = false
        var msg = ""

        try {
            val req = Request.Builder()
                .url(healthUrl)
                .get()
                .build()

            val response = pingClient.newCall(req).execute()
            latency = System.currentTimeMillis() - startTime
            httpCode = response.code
            isReachable = response.isSuccessful || httpCode in 200..399
            response.close()

            msg = if (isReachable) {
                "Server Connected (${latency}ms)"
            } else {
                "Server Unreachable (HTTP $httpCode)"
            }
        } catch (e: IOException) {
            latency = System.currentTimeMillis() - startTime
            isReachable = false
            httpCode = 0
            msg = "Server Unreachable (${e.javaClass.simpleName})"
        } catch (e: Exception) {
            latency = System.currentTimeMillis() - startTime
            isReachable = false
            httpCode = 0
            msg = "Connection Error"
        }

        val finalState = if (isReachable) {
            ConnectionState.ONLINE_CONNECTED
        } else {
            ConnectionState.ONLINE_UNREACHABLE
        }

        val resultStatus = DetailedConnectionStatus(
            state = finalState,
            isInternetAvailable = true,
            isServerReachable = isReachable,
            latencyMs = latency,
            httpCode = httpCode,
            statusMessage = msg,
            lastCheckedTimestamp = now
        )

        _connectionStatus.value = resultStatus
        return resultStatus
    }
}
