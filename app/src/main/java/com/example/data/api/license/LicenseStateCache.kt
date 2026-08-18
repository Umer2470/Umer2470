package com.example.data.api.license

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages secure local caching of the Developer API license state.
 * Strictly adheres to the Offline-First rule:
 * Local POS, inventory, sales, customer, and accounting data are NEVER deleted or corrupted
 * even if a license state transitions or the server is unreachable.
 */
class LicenseStateCache private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "app_license_state_cache_prefs"
        private const val KEY_LICENSE_STATUS = "key_cached_license_status"
        private const val KEY_LAST_VALIDATED_TIME = "key_last_validated_timestamp"
        private const val KEY_LICENSE_MESSAGE = "key_cached_license_message"
        private const val KEY_PLAN_TYPE = "key_cached_plan_type"
        private const val KEY_MAX_SHOPS = "key_cached_max_shops"
        private const val KEY_MAX_USERS = "key_cached_max_users"

        const val STATUS_ACTIVE = "active"
        const val STATUS_SUSPENDED = "suspended"
        const val STATUS_EXPIRED = "expired"
        const val STATUS_UPDATE_REQUIRED = "update_required"

        @Volatile
        private var INSTANCE: LicenseStateCache? = null

        fun getInstance(context: Context): LicenseStateCache {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LicenseStateCache(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Updates and caches the license status received from the Developer Server.
     */
    fun updateCachedLicenseState(
        status: String,
        message: String = "",
        planType: String = "COMMERCIAL",
        maxShops: Int = 5,
        maxUsers: Int = 10
    ) {
        prefs.edit()
            .putString(KEY_LICENSE_STATUS, status.lowercase())
            .putLong(KEY_LAST_VALIDATED_TIME, System.currentTimeMillis())
            .putString(KEY_LICENSE_MESSAGE, message)
            .putString(KEY_PLAN_TYPE, planType)
            .putInt(KEY_MAX_SHOPS, maxShops)
            .putInt(KEY_MAX_USERS, maxUsers)
            .apply()
    }

    /**
     * Gets the last known cached license status.
     * Default fallback is STATUS_ACTIVE to guarantee offline continuity.
     */
    fun getCachedLicenseStatus(): String {
        return prefs.getString(KEY_LICENSE_STATUS, STATUS_ACTIVE) ?: STATUS_ACTIVE
    }

    /**
     * Returns true if the cached status allows core local operations.
     * Local POS and business operations remain available offline.
     */
    fun isLocalBusinessOperationAllowed(): Boolean {
        // Local operations and data remain safe and functional
        return true
    }

    /**
     * Gets timestamp of last server validation check.
     */
    fun getLastValidatedTimestamp(): Long {
        return prefs.getLong(KEY_LAST_VALIDATED_TIME, 0L)
    }

    /**
     * Gets cached license message.
     */
    fun getCachedMessage(): String {
        return prefs.getString(KEY_LICENSE_MESSAGE, "System operational in local offline mode.") ?: ""
    }

    /**
     * Gets cached plan details.
     */
    fun getCachedPlanType(): String = prefs.getString(KEY_PLAN_TYPE, "COMMERCIAL") ?: "COMMERCIAL"
    fun getCachedMaxShops(): Int = prefs.getInt(KEY_MAX_SHOPS, 5)
    fun getCachedMaxUsers(): Int = prefs.getInt(KEY_MAX_USERS, 10)
}
