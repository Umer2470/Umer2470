package com.example.data.backup

import com.example.util.SecurityUtils
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCryptoEngine {

    private val MAGIC_HEADER = "SENTRY_BACKUP_V1".toByteArray(StandardCharsets.UTF_8)
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    private const val ITERATION_COUNT = 10000
    private const val KEY_LENGTH = 256

    // Primary internal salt for backup encryption
    private val MASTER_SALT = "SENTRY_STORE_POS_ENCRYPTED_BACKUP_SALT_2026".toByteArray(StandardCharsets.UTF_8)

    private fun deriveKey(seed: String): SecretKeySpec {
        val spec = PBEKeySpec(seed.toCharArray(), MASTER_SALT, ITERATION_COUNT, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Encrypts plaintext JSON payload into an authenticated AES-256-GCM package.
     */
    fun encryptPayload(plainJson: String, keySeed: String): ByteArray {
        val plainBytes = plainJson.toByteArray(StandardCharsets.UTF_8)
        val secretKey = deriveKey(keySeed)

        val iv = ByteArray(GCM_IV_LENGTH)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec)

        val cipherBytes = cipher.doFinal(plainBytes)

        val output = ByteArrayOutputStream()
        output.write(MAGIC_HEADER)
        output.write(iv)
        output.write(cipherBytes)

        return output.toByteArray()
    }

    /**
     * Decrypts an authenticated AES-256-GCM package back to plaintext JSON.
     * Throws an exception if header magic, IV, or authenticated tag is tampered with.
     */
    fun decryptPayload(encryptedBytes: ByteArray, keySeed: String): String {
        if (encryptedBytes.size < MAGIC_HEADER.size + GCM_IV_LENGTH + 16) {
            throw IllegalArgumentException("Invalid backup package: insufficient file size or corrupted header.")
        }

        // Verify Magic Header
        for (i in MAGIC_HEADER.indices) {
            if (encryptedBytes[i] != MAGIC_HEADER[i]) {
                throw IllegalArgumentException("Unrecognized backup format or corrupted magic header.")
            }
        }

        var offset = MAGIC_HEADER.size
        val iv = encryptedBytes.copyOfRange(offset, offset + GCM_IV_LENGTH)
        offset += GCM_IV_LENGTH

        val cipherBytes = encryptedBytes.copyOfRange(offset, encryptedBytes.size)

        val secretKey = deriveKey(keySeed)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

        val plainBytes = cipher.doFinal(cipherBytes)
        return String(plainBytes, StandardCharsets.UTF_8)
    }

    /**
     * Calculates SHA-256 checksum for data integrity verification.
     */
    fun calculateChecksum(data: String): String {
        return SecurityUtils.sha256(data)
    }
}
