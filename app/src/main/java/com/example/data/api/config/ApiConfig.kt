package com.example.data.api.config

import android.content.Context
import android.content.SharedPreferences

/**
 * Centralized API configuration for the Developer API Connection Layer.
 * Allows easy modification of base URL, versioning, and connection timeouts.
 * Persists custom base URL across application restarts.
 */
object ApiConfig {

    /**
     * Default base URL for CH UMER DEVELOPER APP Developer Control Center.
     * Guaranteed HTTPS and ends with a trailing slash.
     */
    const val DEFAULT_BASE_URL: String = "https://ais-dev-kjblwnm3esfd3tybhleckt-454250663559.asia-east1.run.app/api/v1/"

    /**
     * Current API Version tag
     */
    const val API_VERSION: String = "v1"

    /**
     * Network Timeouts in Seconds
     */
    const val CONNECT_TIMEOUT_SECONDS: Long = 15L
    const val READ_TIMEOUT_SECONDS: Long = 15L
    const val WRITE_TIMEOUT_SECONDS: Long = 15L

    /**
     * Maximum retry attempts for transient network failures
     */
    const val MAX_RETRY_ATTEMPTS: Int = 2

    private const val PREFS_NAME = "dev_api_config_prefs"
    private const val KEY_CUSTOM_BASE_URL = "key_custom_base_url"

    @Volatile
    private var customBaseUrl: String? = null

    /**
     * Initializes persisted API configuration on app startup.
     */
    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_CUSTOM_BASE_URL, null)
        if (!saved.isNullOrBlank()) {
            customBaseUrl = formatUrl(saved)
        }
    }

    /**
     * Returns the active Base URL, formatted with trailing slash.
     */
    fun getBaseUrl(): String {
        val url = customBaseUrl ?: DEFAULT_BASE_URL
        return formatUrl(url)
    }

    /**
     * Updates and persists custom Base URL.
     */
    fun setBaseUrl(context: Context, newUrl: String) {
        val formatted = formatUrl(newUrl)
        customBaseUrl = formatted
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CUSTOM_BASE_URL, formatted).apply()
    }

    /**
     * Resets Base URL to the production default.
     */
    fun resetToDefault(context: Context) {
        customBaseUrl = null
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_CUSTOM_BASE_URL).apply()
    }

    /**
     * Ensures URL is valid and ends with trailing slash.
     */
    fun formatUrl(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }
}
