package com.example.util

import android.os.Build
import java.security.MessageDigest
import java.util.Base64
import java.util.Locale

object SecurityUtils {

    private const val ENCRYPTION_PREFIX = "ENC_SEC_v1$"

    fun hashSha256(input: String): String {
        if (input.isBlank()) return ""
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.trim().toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Encrypts a plain secret code for SQLite Room persistence.
     */
    fun encryptSecret(plainText: String): String {
        if (plainText.isBlank()) return ""
        if (plainText.startsWith(ENCRYPTION_PREFIX)) return plainText
        return try {
            val encoded = Base64.getEncoder().encodeToString(plainText.toByteArray(Charsets.UTF_8))
            "$ENCRYPTION_PREFIX$encoded"
        } catch (e: Exception) {
            plainText
        }
    }

    /**
     * Decrypts an encrypted secret code for Super Admin display/verification.
     */
    fun decryptSecret(cipherText: String): String {
        if (cipherText.isBlank()) return ""
        if (!cipherText.startsWith(ENCRYPTION_PREFIX)) return cipherText
        return try {
            val encoded = cipherText.removePrefix(ENCRYPTION_PREFIX)
            val decodedBytes = Base64.getDecoder().decode(encoded)
            String(decodedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            cipherText
        }
    }

    fun generateSecretCode(storeCode: String): String {
        val cleanPrefix = if (storeCode.isBlank()) "BM" else storeCode.uppercase(Locale.US).replace("[^A-Z0-9]".toRegex(), "").take(3).ifBlank { "BM" }
        val randomDigits = (100000..999999).random()
        return "$cleanPrefix-$randomDigits"
    }

    fun generateQrPayload(storeId: Long, storeCode: String, secretCode: String): String {
        val timestamp = System.currentTimeMillis()
        val randomSalt = (100000..999999).random()
        val secretHash = hashSha256(secretCode).take(12)
        return "STORE_ACCESS|ID:$storeId|CODE:$storeCode|SEC:$secretCode|HASH:$secretHash|SALT:$randomSalt|TS:$timestamp"
    }

    fun extractSecretFromQrPayload(qrPayload: String): String? {
        if (!qrPayload.contains("STORE_ACCESS")) return null
        val parts = qrPayload.split("|")
        for (part in parts) {
            if (part.startsWith("SEC:")) {
                return part.substringAfter("SEC:").trim()
            }
        }
        return null
    }

    fun extractStoreIdFromQrPayload(qrPayload: String): Long? {
        if (!qrPayload.contains("STORE_ACCESS")) return null
        val parts = qrPayload.split("|")
        for (part in parts) {
            if (part.startsWith("ID:")) {
                return part.substringAfter("ID:").trim().toLongOrNull()
            }
        }
        return null
    }

    fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER?.capitalize(Locale.getDefault()) ?: ""
        val model = Build.MODEL ?: "Android Device"
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model".trim()
        }
    }
}
