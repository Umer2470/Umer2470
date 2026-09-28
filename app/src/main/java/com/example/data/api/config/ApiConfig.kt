package com.example.data.api.config

import android.content.Context

object ApiConfig {
    const val DEFAULT_DEVELOPER_SERVER_URL = "https://ais-pre-repstbphrkqk34xvfwxoji-454250663559.asia-east1.run.app/api/v1/"
    const val CONNECT_TIMEOUT_SECONDS = 15L
    const val READ_TIMEOUT_SECONDS = 15L
    const val WRITE_TIMEOUT_SECONDS = 15L
    const val HEARTBEAT_INTERVAL_MINUTES = 60L
    const val MAX_RETRY_ATTEMPTS = 3

    private var customBaseUrl: String = DEFAULT_DEVELOPER_SERVER_URL

    fun init(context: Context) {
        try {
            val prefs = context.getSharedPreferences("app_server_prefs", Context.MODE_PRIVATE)
            val saved = prefs.getString("server_base_url", null)
            if (!saved.isNullOrBlank()) {
                setBaseUrl(saved)
            }
        } catch (_: Exception) {}
    }

    fun getBaseUrl(): String {
        return customBaseUrl
    }

    fun setBaseUrl(url: String) {
        customBaseUrl = if (url.endsWith("/")) url else "$url/"
    }
}
