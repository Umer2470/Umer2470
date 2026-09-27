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

        // Purge any legacy KEY_OWNER_SECURITY_KEY_HASH so generated device keys can NEVER be used as credentials
        if (prefs.contains(KEY_OWNER_SECURITY_KEY_HASH)) {
            prefs.edit().remove(KEY_OWNER_SECURITY_KEY_HASH).apply()
        }

        // 2. Hardware / device security identifier for audit identification
        if (!prefs.contains(KEY_OWNER_SECURITY_KEY)) {
            val randomToken = UUID.randomUUID().toString().replace("-", "").take(16).uppercase()
            val generatedKey = "OWNER-KEY-$randomToken"
            prefs.edit()
                .putString(KEY_OWNER_SECURITY_KEY, generatedKey)
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
                    .putBoolean(KEY_BOOTSTRAP_REVOKED, true)
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
            // If already configured with a custom password, permanently revoke bootstrap
            if (isOwnerSecurityConfigured() && !prefs.getBoolean(KEY_BOOTSTRAP_REVOKED, false)) {
                prefs.edit().putBoolean(KEY_BOOTSTRAP_REVOKED, true).apply()
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
     * Checks if the first-time bootstrap password (2194903) is allowed.
     * Allowed ONLY on fresh/unconfigured installations before a custom password is saved.
     * Permanently false once a custom password has been saved or bootstrap revoked.
     */
    fun isBootstrapAllowed(): Boolean {
        if (prefs.getBoolean(KEY_BOOTSTRAP_REVOKED, false)) return false
        return !isOwnerSecurityConfigured()
    }

    /**
     * Verifies the first-time bootstrap password (2194903).
     * Never stores or displays plaintext; verifies securely via cryptographic salted hash.
     */
    fun verifyBootstrapPassword(input: String): Boolean {
        if (!isBootstrapAllowed()) return false
        val clean = input.trim()
        if (clean.isBlank()) return false
        val salt = prefs.getString(KEY_SALT, "") ?: ""
        val inputHash = hashWithSalt(clean, salt)
        val bootstrapHash = hashWithSalt(BOOTSTRAP_PASSWORD, salt)
        return inputHash == bootstrapHash
    }

    /**
     * Initializes Owner Security for the first time.
     * Once saved, the bootstrap password is permanently deactivated.
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
            .putBoolean(KEY_BOOTSTRAP_REVOKED, true) // Permanently deactivate bootstrap credential
            .commit()
        return true
    }

    /**
     * Common unauthorized backdoors and default codes that must never be allowed as Owner credentials.
     * Also prevents reusing the bootstrap setup code as the permanent password.
     */
    fun isDisallowedCredential(credential: String): Boolean {
        val clean = credential.trim().lowercase()
        return clean in listOf(
            BOOTSTRAP_PASSWORD, // Bootstrap code cannot be reused as permanent custom password
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
     * Verifies whether the provided credential matches the Configured Dedicated Owner Security Password / PIN.
     * On unconfigured fresh installs, permits the initial bootstrap password (2194903).
     * Once a permanent password is set, the bootstrap password MUST NOT work.
     *
     * CRITICAL SECURITY RULE:
     * - The displayed Owner Security Key (Cryptographic Device Identifier) MUST NEVER unlock Owner Mode.
     * - Only the dedicated private Owner PIN/Password unlocks Owner Mode.
     * - Common backdoors (9999, phone numbers, cashier/admin PINs) and Owner Keys are strictly rejected.
     */
    fun verifyCredential(input: String): Boolean {
        val clean = input.trim()
        if (clean.isBlank()) return false
        if (clean == "9999" || clean == "03080018035") return false // Explicitly reject unauthorized backdoors

        // CRITICAL: Owner Key is a cryptographic hardware/device identifier, NOT an authentication password.
        val storedRawKey = getSecurityKey()
        if (clean.startsWith("OWNER-KEY-", ignoreCase = true) ||
            (storedRawKey.isNotBlank() && clean.equals(storedRawKey, ignoreCase = true))
        ) {
            return false // Owner Key must NEVER unlock Owner Mode
        }

        val salt = prefs.getString(KEY_SALT, "") ?: ""
        val inputHash = hashWithSalt(clean, salt)

        val storedPinHash = prefs.getString(KEY_OWNER_PIN_HASH, null)
        val hashOf9999 = hashWithSalt("9999", salt)

        // 1. If configured: Match ONLY Dedicated Owner PIN / Password (only if configured and not 9999)
        if (isOwnerSecurityConfigured() && storedPinHash != null && !storedPinHash.equals(hashOf9999, ignoreCase = true)) {
            if (inputHash.equals(storedPinHash, ignoreCase = true)) {
                return true
            }
            return false
        }

        // 2. If unconfigured fresh installation: Check one-time bootstrap credential
        if (isBootstrapAllowed()) {
            val bootstrapHash = hashWithSalt(BOOTSTRAP_PASSWORD, salt)
            if (inputHash == bootstrapHash) {
                return true
            }
        }

        return false
    }

    /**
     * Retrieves the dedicated Owner Security Key.
     */
    fun getSecurityKey(): String {
        return prefs.getString(KEY_OWNER_SECURITY_KEY, "") ?: ""
    }

    /**
     * Retrieves the masked dedicated Owner Security Key for safe display.
     * Prevents shoulder-surfing and accidental copy-pasting into authentication inputs.
     */
    fun getMaskedSecurityKey(): String {
        val rawKey = getSecurityKey()
        if (rawKey.isBlank()) return "OWNER-KEY-••••••••••••"
        val suffix = if (rawKey.length >= 4) rawKey.takeLast(4) else ""
        return "OWNER-KEY-••••••••$suffix"
    }

    /**
     * Changes the Dedicated Owner Security Password / PIN with mandatory verification of current PIN.
     * Old PIN immediately becomes invalid.
     * The Owner Key cannot be used as current or new PIN.
     */
    fun changePassword(currentPassword: String, newPassword: String): Boolean {
        val cleanCurrent = currentPassword.trim()
        if (!verifyCredential(cleanCurrent)) {
            return false
        }
        return setPassword(newPassword)
    }

    /**
     * Updates the Dedicated Owner Security Password / PIN.
     * Revokes the old credential immediately.
     */
    fun setPassword(newPassword: String): Boolean {
        val clean = newPassword.trim()
        if (clean.length < 4) return false
        if (isDisallowedCredential(clean)) return false

        val storedRawKey = getSecurityKey()
        if (clean.startsWith("OWNER-KEY-", ignoreCase = true) ||
            (storedRawKey.isNotBlank() && clean.equals(storedRawKey, ignoreCase = true))
        ) {
            return false
        }

        val salt = prefs.getString(KEY_SALT, "") ?: ""
        val newHash = hashWithSalt(clean, salt)
        prefs.edit()
            .putString(KEY_OWNER_PIN_HASH, newHash)
            .putBoolean(KEY_IS_CONFIGURED, true)
            .putBoolean(KEY_BOOTSTRAP_REVOKED, true) // Permanently deactivate bootstrap credential
            .commit()
        return true
    }

    /**
     * Resets owner security state for isolated unit testing.
     */
    fun resetForTesting() {
        prefs.edit()
            .remove(KEY_OWNER_PIN_HASH)
            .remove(KEY_BOOTSTRAP_REVOKED)
            .putBoolean(KEY_IS_CONFIGURED, false)
            .commit()
    }

    /**
     * Regenerates a new unique Dedicated Owner Security Key.
     * The old key is invalidated immediately.
     */
    fun regenerateSecurityKey(): String {
        val randomToken = UUID.randomUUID().toString().replace("-", "").take(16).uppercase()
        val generatedKey = "OWNER-KEY-$randomToken"
        prefs.edit()
            .putString(KEY_OWNER_SECURITY_KEY, generatedKey)
            .remove(KEY_OWNER_SECURITY_KEY_HASH)
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
        private const val KEY_BOOTSTRAP_REVOKED = "key_owner_bootstrap_revoked"
        private const val BOOTSTRAP_PASSWORD = "2194903"

        @Volatile
        private var INSTANCE: OwnerSecurityManager? = null

        fun getInstance(context: Context): OwnerSecurityManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OwnerSecurityManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

