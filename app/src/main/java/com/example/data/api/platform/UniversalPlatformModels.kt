package com.example.data.api.platform

import com.squareup.moshi.JsonClass

/**
 * Universal Developer Platform Models for Multi-Application & Multi-Tenant License Management.
 */

@JsonClass(generateAdapter = true)
data class RegisteredApplication(
    val appId: String, // e.g. "SENTRY-STORE-POS", "WORKOUT-APP", "BUSINESS-APP"
    val appName: String, // e.g. "Sentry Store POS", "FitPulse Pro", "OmniBiz Suite"
    val packageIdentifier: String, // e.g. "com.aistudio.sentrystore.pos", "com.fitpulse.app"
    val version: String = "1.0.0",
    val developerId: String = "DEV-CH-UMER",
    val status: String = "ACTIVE", // "ACTIVE", "MAINTENANCE", "DEPRECATED"
    val secretSalt: String = "SECRET_SALT_2026",
    val defaultGracePeriodDays: Int = 14,
    val iconEmoji: String = "🏪",
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class CustomerAccount(
    val customerId: String, // e.g. "CUST-1001", "CUST-1002"
    val businessName: String,
    val contactPerson: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val status: String = "ACTIVE", // "ACTIVE", "SUSPENDED", "INACTIVE"
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class DeviceInstallation(
    val installationId: String, // e.g. "APP-A1B2C3D4E5F6"
    val appId: String,
    val customerId: String,
    val deviceModel: String = "Android Device",
    val deviceFingerprint: String = "",
    val status: String = "BOUND", // "BOUND", "ACTIVE", "RELEASED", "SUSPENDED"
    val firstSeenAt: Long = System.currentTimeMillis(),
    val lastVerifiedAt: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class UniversalLicense(
    val licenseId: String, // e.g. "LIC-SENTRY-1001-2026-X8Y9"
    val appId: String, // Application ID (Isolation guarantee)
    val customerId: String,
    val installationId: String, // Bound Installation ID
    val licenseCode: String, // Formatted activation key e.g. "ACTV-SENTRY-STORE-POS-M1-XXXX-XXXX-XXXX"
    val planType: String, // "DEMO_1M", "STD_3M", "PRO_6M", "ENT_1Y", "CUSTOM", "LIFETIME"
    val planName: String,
    val durationDays: Int, // 0 = Lifetime
    val startDate: Long = System.currentTimeMillis(),
    val expiryDate: Long = 0L, // 0 = Lifetime
    val status: String = "ACTIVE", // "ACTIVE", "EXPIRED", "SUSPENDED", "REVOKED", "TRANSFERRED"
    val signatureToken: String = "",
    val createdBy: String = "Master Developer",
    val createdAt: Long = System.currentTimeMillis(),
    val lastModifiedAt: Long = System.currentTimeMillis()
) {
    fun isCurrentlyValid(): Boolean {
        if (status != "ACTIVE") return false
        if (expiryDate <= 0L) return true // Lifetime
        return System.currentTimeMillis() <= expiryDate
    }

    fun getDaysRemaining(): Long {
        if (expiryDate <= 0L) return -1L
        val diff = expiryDate - System.currentTimeMillis()
        return if (diff > 0) diff / (1000L * 60 * 60 * 24) else 0L
    }
}

@JsonClass(generateAdapter = true)
data class LicenseAuditLog(
    val id: String,
    val action: String, // e.g. "LICENSE_GENERATED", "LICENSE_ACTIVATED", "LICENSE_RENEWED", "LICENSE_EXTENDED", "LICENSE_SUSPENDED", "DEVICE_TRANSFERRED", "INSTALLATION_RESET"
    val appId: String,
    val customerId: String,
    val installationId: String,
    val licenseId: String,
    val performedBy: String,
    val timestamp: Long = System.currentTimeMillis(),
    val details: String
)

sealed class LicenseVerificationResult {
    data class Valid(
        val license: UniversalLicense,
        val message: String
    ) : LicenseVerificationResult()

    data class Invalid(
        val reason: String,
        val errorCode: String
    ) : LicenseVerificationResult()

    data class Expired(
        val expiryTimestamp: Long,
        val message: String
    ) : LicenseVerificationResult()

    data class Suspended(
        val message: String
    ) : LicenseVerificationResult()

    data class AppMismatch(
        val expectedApp: String,
        val actualApp: String
    ) : LicenseVerificationResult()

    data class InstallationMismatch(
        val expectedInstallation: String,
        val actualInstallation: String
    ) : LicenseVerificationResult()
}

enum class ExpiryWarningTier(val daysThreshold: Int, val warningTitle: String, val level: String) {
    TIER_30_DAYS(30, "License Renewal Notice (30 Days)", "INFO"),
    TIER_15_DAYS(15, "License Renewal Notice (15 Days)", "WARNING"),
    TIER_7_DAYS(7, "License Renewal Notice (7 Days)", "URGENT"),
    TIER_3_DAYS(3, "Critical: License Expires in 3 Days", "CRITICAL"),
    TIER_1_DAY(1, "Final Warning: License Expires Tomorrow", "EMERGENCY"),
    EXPIRED(0, "License Expired", "LOCKED")
}
