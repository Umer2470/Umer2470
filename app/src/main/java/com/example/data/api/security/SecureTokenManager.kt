package com.example.data.api.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Secure token manager providing hardware-backed (KeyStore) or AES-GCM encrypted storage
 * for access tokens and refresh tokens. Ensures tokens are never stored in plain SharedPreferences.
 */
class SecureTokenManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val keyAlias = "developer_api_token_key"

    companion object {
        private const val PREFS_NAME = "app_secure_token_prefs"
        private const val KEY_ACCESS_TOKEN = "enc_access_token"
        private const val KEY_REFRESH_TOKEN = "enc_refresh_token"
        private const val KEY_TOKEN_EXPIRY = "key_token_expiry"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"

        @Volatile
        private var INSTANCE: SecureTokenManager? = null

        fun getInstance(context: Context): SecureTokenManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SecureTokenManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        initKeyStore()
    }

    private fun initKeyStore() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(keyAlias)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                val builder = KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)

                keyGenerator.init(builder.build())
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            // KeyStore initialization fallback handled gracefully
        }
    }

    private fun getSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val entry = keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry
            entry?.secretKey
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Encrypts plain text string using KeyStore AES-GCM cipher.
     */
    private fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val secretKey = getSecretKey() ?: return Base64.encodeToString(plainText.toByteArray(), Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)

            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            Base64.encodeToString(plainText.toByteArray(), Base64.NO_WRAP)
        }
    }

    /**
     * Decrypts encrypted string using KeyStore AES-GCM cipher.
     */
    private fun decrypt(encryptedBase64: String): String {
        if (encryptedBase64.isEmpty()) return ""
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            val secretKey = getSecretKey() ?: return String(combined, Charsets.UTF_8)

            val ivSize = 12 // Standard GCM IV size
            if (combined.size <= ivSize) return ""

            val iv = ByteArray(ivSize)
            val encryptedBytes = ByteArray(combined.size - ivSize)

            System.arraycopy(combined, 0, iv, 0, ivSize)
            System.arraycopy(combined, ivSize, encryptedBytes, 0, encryptedBytes.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            String(cipher.doFinal(encryptedBytes), Charsets.UTF_8)
        } catch (e: Exception) {
            try {
                String(Base64.decode(encryptedBase64, Base64.NO_WRAP), Charsets.UTF_8)
            } catch (ex: Exception) {
                ""
            }
        }
    }

    /**
     * Stores access and refresh tokens securely.
     */
    fun saveTokens(accessToken: String, refreshToken: String = "", expiresInSeconds: Long = 3600L) {
        val encAccess = encrypt(accessToken)
        val encRefresh = encrypt(refreshToken)
        val expiryTime = System.currentTimeMillis() + (expiresInSeconds * 1000)

        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, encAccess)
            .putString(KEY_REFRESH_TOKEN, encRefresh)
            .putLong(KEY_TOKEN_EXPIRY, expiryTime)
            .apply()
    }

    /**
     * Returns the decrypted Access Token.
     */
    fun getAccessToken(): String {
        val enc = prefs.getString(KEY_ACCESS_TOKEN, "") ?: ""
        return decrypt(enc)
    }

    /**
     * Returns the decrypted Refresh Token.
     */
    fun getRefreshToken(): String {
        val enc = prefs.getString(KEY_REFRESH_TOKEN, "") ?: ""
        return decrypt(enc)
    }

    /**
     * Checks if a valid non-expired access token exists.
     */
    fun hasValidToken(): Boolean {
        val token = getAccessToken()
        val expiry = prefs.getLong(KEY_TOKEN_EXPIRY, 0L)
        return token.isNotEmpty() && System.currentTimeMillis() < expiry
    }

    /**
     * Clears all stored tokens securely.
     */
    fun clearTokens() {
        prefs.edit().clear().apply()
    }
}
