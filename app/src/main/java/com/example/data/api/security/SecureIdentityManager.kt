package com.example.data.api.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.example.util.SecurityUtils
import java.util.UUID

class SecureIdentityManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        ensureIdentityInitialized()
    }

    private fun ensureIdentityInitialized() {
        if (!prefs.contains(KEY_INSTALLATION_ID)) {
            val randomPart = UUID.randomUUID().toString().replace("-", "").take(12).uppercase()
            val generatedId = "APP-$randomPart"
            prefs.edit().putString(KEY_INSTALLATION_ID, generatedId).apply()
        }
        if (!prefs.contains(KEY_DEVICE_FINGERPRINT)) {
            val raw = "${Build.BRAND}-${Build.MODEL}-${Build.MANUFACTURER}-${Build.BOARD}"
            val hash = SecurityUtils.sha256(raw).take(16)
            prefs.edit().putString(KEY_DEVICE_FINGERPRINT, "FP-$hash").apply()
        }
    }

    fun getInstallationId(): String {
        return prefs.getString(KEY_INSTALLATION_ID, null) ?: run {
            val randomPart = UUID.randomUUID().toString().replace("-", "").take(12).uppercase()
            val generatedId = "APP-$randomPart"
            prefs.edit().putString(KEY_INSTALLATION_ID, generatedId).apply()
            generatedId
        }
    }

    fun getDeviceFingerprint(): String {
        return prefs.getString(KEY_DEVICE_FINGERPRINT, "FP-UNKNOWN") ?: "FP-UNKNOWN"
    }

    fun getCustomerId(): String? = prefs.getString(KEY_CUSTOMER_ID, null)

    fun setCustomerId(id: String?) {
        prefs.edit().putString(KEY_CUSTOMER_ID, id).apply()
    }

    fun getStoreId(): String? = prefs.getString(KEY_STORE_ID, null)

    fun setStoreId(id: String?) {
        prefs.edit().putString(KEY_STORE_ID, id).apply()
    }

    fun getAppVersion(): String = "1.0"

    companion object {
        private const val PREFS_NAME = "ch_umer_secure_identity"
        private const val KEY_INSTALLATION_ID = "key_installation_id"
        private const val KEY_DEVICE_FINGERPRINT = "key_device_fingerprint"
        private const val KEY_CUSTOMER_ID = "key_customer_id"
        private const val KEY_STORE_ID = "key_store_id"

        @Volatile
        private var instance: SecureIdentityManager? = null

        fun getInstance(context: Context): SecureIdentityManager {
            return instance ?: synchronized(this) {
                instance ?: SecureIdentityManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
