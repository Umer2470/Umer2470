package com.example.data.api.security

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import java.util.UUID

/**
 * Manages unique App Installation ID and tenant/store identity metadata.
 * Uses cryptographically secure random UUID generation on first launch.
 * Never uses sensitive device identifiers like IMEI or phone number.
 */
class SecureIdentityManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "app_secure_identity_prefs"
        private const val KEY_INSTALLATION_ID = "key_installation_id"
        private const val KEY_CUSTOMER_ID = "key_customer_id"
        private const val KEY_STORE_ID = "key_store_id"

        @Volatile
        private var INSTANCE: SecureIdentityManager? = null

        fun getInstance(context: Context): SecureIdentityManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecureIdentityManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        ensureInstallationIdGenerated()
    }

    /**
     * Guarantees a unique cryptographically secure random UUID Installation ID exists.
     * Format: APP-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
     */
    private fun ensureInstallationIdGenerated() {
        if (!prefs.contains(KEY_INSTALLATION_ID) || prefs.getString(KEY_INSTALLATION_ID, null).isNull_or_empty()) {
            val secureUuid = "APP-${UUID.randomUUID()}"
            prefs.edit().putString(KEY_INSTALLATION_ID, secureUuid).apply()
        }
    }

    private fun String?.isNull_or_empty(): Boolean = this == null || this.isEmpty()

    /**
     * Gets the unique App Installation ID.
     */
    fun getInstallationId(): String {
        ensureInstallationIdGenerated()
        return prefs.getString(KEY_INSTALLATION_ID, "") ?: "APP-${UUID.randomUUID()}"
    }

    /**
     * Gets or sets the associated Customer ID.
     */
    fun getCustomerId(): String {
        return prefs.getString(KEY_CUSTOMER_ID, "CUST-DEFAULT") ?: "CUST-DEFAULT"
    }

    fun setCustomerId(customerId: String) {
        prefs.edit().putString(KEY_CUSTOMER_ID, customerId).apply()
    }

    /**
     * Gets or sets the associated Store ID.
     */
    fun getStoreId(): Long {
        return prefs.getLong(KEY_STORE_ID, 1L)
    }

    fun setStoreId(storeId: Long) {
        prefs.edit().putLong(KEY_STORE_ID, storeId).apply()
    }

    /**
     * Returns current App Version name from BuildConfig.
     */
    fun getAppVersion(): String {
        return try {
            BuildConfig.VERSION_NAME
        } catch (e: Exception) {
            "1.0"
        }
    }

    /**
     * Returns full identity packet metadata for API calls.
     */
    fun getIdentityPacket(): Map<String, String> {
        return mapOf(
            "installation_id" to getInstallationId(),
            "customer_id" to getCustomerId(),
            "store_id" to getStoreId().toString(),
            "app_version" to getAppVersion(),
            "platform" to "Android"
        )
    }
}
