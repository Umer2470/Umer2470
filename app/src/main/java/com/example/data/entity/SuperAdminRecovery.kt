package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "super_admin_recovery")
data class SuperAdminRecovery(
    @PrimaryKey val id: Int = 1,
    val tenantId: String = "TENANT_DEFAULT",
    val passphraseWordsCsv: String = "",
    val emergencyCode: String = "",
    val recoveryEmail: String = "",
    val recoveryMobile: String = "",
    val isConfigured: Boolean = false,
    val failedAttemptsCount: Int = 0,
    val lockoutEndTimeMs: Long = 0L,
    val lastRecoveryTimestamp: Long = 0L,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun getPassphraseWordsList(): List<String> {
        if (passphraseWordsCsv.isBlank()) return emptyList()
        return passphraseWordsCsv.split(",", " ").map { it.trim().lowercase() }.filter { it.isNotBlank() }
    }

    fun isLockedOut(): Boolean {
        return failedAttemptsCount >= 5 && System.currentTimeMillis() < lockoutEndTimeMs
    }

    fun getRemainingLockoutSeconds(): Long {
        if (!isLockedOut()) return 0L
        val diff = lockoutEndTimeMs - System.currentTimeMillis()
        return if (diff > 0) diff / 1000L else 0L
    }
}
