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
 * No hardcoded backdoors or default bypass codes (such as 9999 or phone numbers) exist.
 */
class OwnerSecurityManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        ensureInitialized(context)
    }

    private fun ensureInitialized(context: Context) {
        // 1. Ensure cryptographic salt exists
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

        // 3. Purge unauthorized 9999 bypass if present in stored hash
        val hashOf9999 = hashWithSalt("9999", salt)
        val currentPinHash = prefs.getString(KEY_OWNER_PIN_HASH, null)
        if (currentPinHash != null && currentPinHash.equals(hashOf9999, ignoreCase = true)) {
            prefs.edit()
                .remove(KEY_OWNER_PIN_HASH)
                .putBoolean(KEY_IS_CONFIGURED, false)
                .apply()
        }

        // 4. Migrate any custom legacy PIN if set by the proprietor (excluding unauthorized backdoors)
        if (!prefs.contains(KEY_OWNER_PIN_HASH)) {
            val legacyPrefs = context.getSharedPreferences("sentry_store_pos_preferences", Context.MODE_PRIVATE)
            val legacyPin = legacyPrefs.getString("owner_security_code", null)
                ?: context.getSharedPreferences("pos_app_preferences", Context.MODE_PRIVATE).getString("owner_security_code", null)

            val cleanLegacy = legacyPin?.trim()
            if (!cleanLegacy.isNullOrBlank() && cleanLegacy.length >= 4 && !isDisallowedCredential(cleanLegacy)) {
                val pinHash = hashWithSalt(cleanLegacy, salt)
                prefs.edit()
                    .putString(KEY_OWNER_PIN_HASH, pinHash)
                    .putBoolean(KEY_IS_CONFIGURED, true)
                    .apply()
            } else {
                // DO NOT seed a hidden backdoor or default PIN.
                // Mark unconfigured so the proprietor goes through first-time setup flow.
                prefs.edit().putBoolean(KEY_IS_CONFIGURED, false).apply()
            }
        } else {
            if (!prefs.contains(KEY_IS_CONFIGURED)) {
                prefs.edit().putBoolean(KEY_IS_CONFIGURED, true).apply()
            }
        }
    }

    private fun hashWithSalt(input: String, salt: String): String {
        return SecurityUtils.sha256(input + "_" + salt)
    }

    /**
     * Checks if the Owner has completed the initial security setup.
     */
    fun isOwnerSecurityConfigured(): Boolean {
        val isConfigured = prefs.getBoolean(KEY_IS_CONFIGURED, false)
        val storedHash = prefs.getString(KEY_OWNER_PIN_HASH, null)
        val salt = prefs.getString(KEY_SALT, "") ?: ""
        val hashOf9999 = hashWithSalt("9999", salt)
        return isConfigured && !storedHash.isNullOrBlank() && !storedHash.equals(hashOf9999, ignoreCase = true)
    }

    /**
     * Initializes Owner Security for the first time.
     */
    fun setupOwnerSecurity(newPin: String): Boolean {
        val clean = newPin.trim()
        if (clean.length < 4) return false
        if (isDisallowedCredential(clean)) return false

        val salt = prefs.getString(KEY_SALT, "") ?: ""
        val newHash = hashWithSalt(clean, salt)
        prefs.edit()
            .putString(KEY_OWNER_PIN_HASH, newHash)
            .putBoolean(KEY_IS_CONFIGURED, true)
            .apply()
        return true
    }

    /**
     * Common unauthorized backdoors and default codes that must never be allowed as Owner credentials.
     */
    fun isDisallowedCredential(credential: String): Boolean {
        val clean = credential.trim().lowercase()
        return clean in listOf(
            "9999",
            "1234",
            "0000",
            "1111",
            "2026",
            "8888",
            "5555",
            "03080018035",
            "admin",
            "superadmin",
            "supervisor",
            "cashier",
            "password"
        )
    }

    /**
     * Verifies whether the provided credential matches:
     * 1. Configured Dedicated Owner Security Password / PIN
     * OR
     * 2. Dedicated Owner Security Key
     *
     * Returns true ONLY for verified dedicated owner credentials.
     * Common backdoors (9999, phone numbers, cashier/admin PINs) are strictly rejected.
     */
    fun verifyCredential(input: String): Boolean {
        val clean = input.trim()
        if (clean.isBlank()) return false
        if (clean == "9999" || clean == "03080018035") return false // Explicitly reject unauthorized backdoors

        val salt = prefs.getString(KEY_SALT, "") ?: ""
        val inputHash = hashWithSalt(clean, salt)

        val storedPinHash = prefs.getString(KEY_OWNER_PIN_HASH, null)
        val storedKeyHash = prefs.getString(KEY_OWNER_SECURITY_KEY_HASH, null)
        val storedRawKey = prefs.getString(KEY_OWNER_SECURITY_KEY, null)
        val hashOf9999 = hashWithSalt("9999", salt)

        // 1. Match Dedicated Owner PIN / Password (only if configured and not 9999)
        if (isOwnerSecurityConfigured() && storedPinHash != null && !storedPinHash.equals(hashOf9999, ignoreCase = true)) {
            if (inputHash.equals(storedPinHash, ignoreCase = true)) {
                return true
            }
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
     * Revokes the old credential immediately.
     */
    fun setPassword(newPassword: String): Boolean {
        val clean = newPassword.trim()
        if (clean.length < 4) return false
        if (isDisallowedCredential(clean)) return false

        val salt = prefs.getString(KEY_SALT, "") ?: ""
        val newHash = hashWithSalt(clean, salt)
        prefs.edit()
            .putString(KEY_OWNER_PIN_HASH, newHash)
            .putBoolean(KEY_IS_CONFIGURED, true)
            .apply()
        return true
    }

    /**
     * Regenerates a new unique Dedicated Owner Security Key.
     * The old key is invalidated immediately.
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
        private const val KEY_IS_CONFIGURED = "key_is_configured"

        @Volatile
        private var INSTANCE: OwnerSecurityManager? = null

        fun getInstance(context: Context): OwnerSecurityManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OwnerSecurityManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

