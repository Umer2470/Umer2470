package com.example.data.api.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

enum class ConnectionState {
    CONNECTED,
    DISCONNECTED,
    UNKNOWN
}

class NetworkConnectionMonitor(private val context: Context) {

    val connectionFlow: Flow<ConnectionState> = callbackFlow {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

        if (connectivityManager == null) {
            trySend(ConnectionState.UNKNOWN)
            close()
            return@callbackFlow
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(ConnectionState.CONNECTED)
            }

            override fun onLost(network: Network) {
                trySend(ConnectionState.DISCONNECTED)
            }

            override fun onUnavailable() {
                trySend(ConnectionState.DISCONNECTED)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        // Emit initial state
        val isConnected = isNetworkAvailable(connectivityManager)
        trySend(if (isConnected) ConnectionState.CONNECTED else ConnectionState.DISCONNECTED)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()

    private fun isNetworkAvailable(cm: ConnectivityManager): Boolean {
        val activeNetwork = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
