package com.example.data.api.security

import android.content.Context
import android.content.SharedPreferences
import com.example.util.SecurityUtils
import java.util.UUID

/**
 * Dedicated Owner Security Credential Management for SENTRY STORE POS.
 *
 * Enforces strictly isolated credentials for the Owner / Developer Control Center:
 * 1. Dedicated Owner Security Password / PIN
 * OR
 * 2. Dedicated Owner Security Key
 *
 * Completely independent from:
 * - Employee / Cashier PIN
 * - Admin password
 * - Supervisor password
 * - POS PIN
 * - License / Activation Code
 *
 * Biometric authentication (Fingerprint, Face Unlock) is completely excluded and removed.
 */
class OwnerSecurityManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        ensureInitialized(context)
    }

    private fun ensureInitialized(context: Context) {
        // 1. Ensure Salt is present
        if (!prefs.contains(KEY_SALT)) {
            val generatedSalt = UUID.randomUUID().toString().replace("-", "")
            prefs.edit().putString(KEY_SALT, generatedSalt).apply()
        }

        val salt = prefs.getString(KEY_SALT, "") ?: ""

        // 2. Ensure Dedicated Owner Security Key is generated
        if (!prefs.contains(KEY_OWNER_SECURITY_KEY)) {
            val randomToken = UUID.randomUUID().toString().replace("-", "").take(16).uppercase()
            val generatedKey = "OWNER-KEY-$randomToken"
            val keyHash = hashWithSalt(generatedKey, salt)
            prefs.edit()
                .putString(KEY_OWNER_SECURITY_KEY, generatedKey)
                .putString(KEY_OWNER_SECURITY_KEY_HASH, keyHash)
                .apply()
        }

        // 3. Ensure Dedicated Owner Security Password/PIN is initialized
        if (!prefs.contains(KEY_OWNER_PIN_HASH)) {
            // Check legacy preference first to preserve any user-configured PIN
            val legacyPrefs = context.getSharedPreferences("sentry_store_pos_preferences", Context.MODE_PRIVATE)
            val legacyPin = legacyPrefs.getString("owner_security_code", null)
                ?: context.getSharedPreferences("pos_app_preferences", Context.MODE_PRIVATE).getString("owner_security_code", null)

            val initialPin = if (!legacyPin.isNullOrBlank()) {
                legacyPin.trim()
            } else {
                // Initialize default 4-digit Owner PIN into SharedPreferences (not hardcoded in verification)
                "9999"
            }
            val pinHash = hashWithSalt(initialPin, salt)
            prefs.edit().putString(KEY_OWNER_PIN_HASH, pinHash).apply()
        }
    }

    private fun hashWithSalt(input: String, salt: String): String {
        return SecurityUtils.sha256(input + "_" + salt)
    }

    /**
     * Verifies whether the provided credential matches:
     * 1. Dedicated Owner Security Password / PIN
     * OR
     * 2. Dedicated Owner Security Key
     *
     * Returns true ONLY for matching dedicated owner credentials.
     * Cashier PINs, Admin passwords, Supervisor PINs, and Activation codes are strictly rejected.
     */
    fun verifyCredential(input: String): Boolean {
        val clean = input.trim()
        if (clean.isBlank()) return false

        val salt = prefs.getString(KEY_SALT, "") ?: ""
        val inputHash = hashWithSalt(clean, salt)

        val storedPinHash = prefs.getString(KEY_OWNER_PIN_HASH, null)
        val storedKeyHash = prefs.getString(KEY_OWNER_SECURITY_KEY_HASH, null)
        val storedRawKey = prefs.getString(KEY_OWNER_SECURITY_KEY, null)

        // 1. Match Dedicated Owner PIN / Password
        if (storedPinHash != null && inputHash.equals(storedPinHash, ignoreCase = true)) {
            return true
        }

        // 2. Match Dedicated Owner Security Key (Hash or exact raw key)
        if (storedKeyHash != null && inputHash.equals(storedKeyHash, ignoreCase = true)) {
            return true
        }
        if (storedRawKey != null && clean.equals(storedRawKey, ignoreCase = true)) {
            return true
        }

        return false
    }

    /**
     * Retrieves the dedicated Owner Security Key for display to the verified proprietor.
     */
    fun getSecurityKey(): String {
        return prefs.getString(KEY_OWNER_SECURITY_KEY, "") ?: ""
    }

    /**
     * Updates the Dedicated Owner Security Password / PIN.
     */
    fun setPassword(newPassword: String): Boolean {
        val clean = newPassword.trim()
        if (clean.length < 4) return false

        val salt = prefs.getString(KEY_SALT, "") ?: ""
        val newHash = hashWithSalt(clean, salt)
        prefs.edit().putString(KEY_OWNER_PIN_HASH, newHash).apply()
        return true
    }

    /**
     * Regenerates a new unique Dedicated Owner Security Key.
     */
    fun regenerateSecurityKey(): String {
        val salt = prefs.getString(KEY_SALT, "") ?: ""
        val randomToken = UUID.randomUUID().toString().replace("-", "").take(16).uppercase()
        val generatedKey = "OWNER-KEY-$randomToken"
        val keyHash = hashWithSalt(generatedKey, salt)
        prefs.edit()
            .putString(KEY_OWNER_SECURITY_KEY, generatedKey)
            .putString(KEY_OWNER_SECURITY_KEY_HASH, keyHash)
            .apply()
        return generatedKey
    }

    /**
     * Biometric authentication is permanently disabled for Owner / Developer access.
     */
    fun isBiometricAllowed(): Boolean = false

    companion object {
        private const val PREFS_NAME = "sentry_store_owner_security"
        private const val KEY_SALT = "key_security_salt"
        private const val KEY_OWNER_PIN_HASH = "key_owner_pin_hash"
        private const val KEY_OWNER_SECURITY_KEY = "key_owner_security_key"
        private const val KEY_OWNER_SECURITY_KEY_HASH = "key_owner_security_key_hash"

        @Volatile
        private var INSTANCE: OwnerSecurityManager? = null

        fun getInstance(context: Context): OwnerSecurityManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OwnerSecurityManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
