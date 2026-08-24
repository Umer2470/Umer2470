package com.example.data.api.config

import android.content.Context

object ApiConfig {
    const val DEFAULT_DEVELOPER_SERVER_URL = "https://pos-api.sentrystore.pk/api/v1/"
    const val CONNECT_TIMEOUT_SECONDS = 15L
    const val READ_TIMEOUT_SECONDS = 15L
    const val WRITE_TIMEOUT_SECONDS = 15L
    const val HEARTBEAT_INTERVAL_MINUTES = 60L
    const val MAX_RETRY_ATTEMPTS = 3

    private var customBaseUrl: String = DEFAULT_DEVELOPER_SERVER_URL

    fun init(context: Context) {
        // Initialized context configuration
    }

    fun getBaseUrl(): String {
        return customBaseUrl
    }

    fun setBaseUrl(url: String) {
        customBaseUrl = if (url.endsWith("/")) url else "$url/"
    }
}
