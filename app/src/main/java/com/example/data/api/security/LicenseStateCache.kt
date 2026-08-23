package com.example.data.api.security

import android.content.Context
import android.content.SharedPreferences

class LicenseStateCache private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getCachedLicenseStatus(): String {
        return prefs.getString(KEY_LICENSE_STATUS, "active") ?: "active"
    }

    fun getCachedPlanType(): String {
        return prefs.getString(KEY_PLAN_TYPE, "COMMERCIAL") ?: "COMMERCIAL"
    }

    fun getMaxShops(): Int {
        return prefs.getInt(KEY_MAX_SHOPS, 5)
    }

    fun getMaxUsers(): Int {
        return prefs.getInt(KEY_MAX_USERS, 10)
    }

    fun updateCachedLicenseState(
        status: String,
        message: String = "",
        planType: String = "COMMERCIAL",
        maxShops: Int = 5,
        maxUsers: Int = 10
    ) {
        prefs.edit()
            .putString(KEY_LICENSE_STATUS, status)
            .putString(KEY_STATUS_MESSAGE, message)
            .putString(KEY_PLAN_TYPE, planType)
            .putInt(KEY_MAX_SHOPS, maxShops)
            .putInt(KEY_MAX_USERS, maxUsers)
            .putLong(KEY_LAST_CHECKED, System.currentTimeMillis())
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "ch_umer_license_cache"
        private const val KEY_LICENSE_STATUS = "key_license_status"
        private const val KEY_STATUS_MESSAGE = "key_status_message"
        private const val KEY_PLAN_TYPE = "key_plan_type"
        private const val KEY_MAX_SHOPS = "key_max_shops"
        private const val KEY_MAX_USERS = "key_max_users"
        private const val KEY_LAST_CHECKED = "key_last_checked"

        @Volatile
        private var instance: LicenseStateCache? = null

        fun getInstance(context: Context): LicenseStateCache {
            return instance ?: synchronized(this) {
                instance ?: LicenseStateCache(context.applicationContext).also { instance = it }
            }
        }
    }
}
