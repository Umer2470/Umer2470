package com.example.util

import java.security.MessageDigest
import java.util.Locale

object SecurityUtils {
    fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        val hexString = StringBuilder()
        for (b in hash) {
            val hex = Integer.toHexString(0xff and b.toInt())
            if (hex.length == 1) hexString.append('0')
            hexString.append(hex)
        }
        return hexString.toString().uppercase(Locale.ROOT)
    }

    fun generateDeterministicToken(installationId: String, secretSalt: String = "CH_UMER_SENTRY_STORE_2026"): String {
        val combined = "$installationId:$secretSalt"
        val hash = sha256(combined)
        return "TOK-${hash.take(16)}"
    }
}
