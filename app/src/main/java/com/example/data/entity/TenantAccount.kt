package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tenant_accounts",
    indices = [
        Index("tenantCode", unique = true),
        Index("licenseKey", unique = true)
    ]
)
data class TenantAccount(
    @PrimaryKey val id: String, // e.g. "TENANT_DEFAULT", "TENANT_1001", "TENANT_1002"
    val tenantCode: String, // e.g. "AL-KHAIR-POS", "BAHRIA-BLDG", "LAHORI-STORE"
    val businessName: String, // Customer Business / Shop Title
    val ownerName: String,
    val email: String = "",
    val phone: String = "",
    val address: String = "",
    val licenseKey: String, // Unique commercial key e.g. "SAAS-ALKH-2026-X9A2-7B4K"
    val planType: String = "ANNUAL", // "TRIAL", "MONTHLY", "ANNUAL", "ENTERPRISE", "LIFETIME"
    val status: String = "ACTIVE", // "ACTIVE", "DEACTIVATED", "SUSPENDED", "EXPIRED", "BLOCKED"
    val paymentStatus: String = "PAID", // "PAID", "PENDING", "OVERDUE", "FREE_TRIAL"
    val boundInstallationId: String = "", // Unique hardware/device binding ID
    val isBlocked: Boolean = false, // True if stolen or illegal copy blocked
    val forceLogoutTimestamp: Long = 0L, // Force logout timestamp trigger
    val maxShops: Int = 10,
    val maxUsers: Int = 25,
    val licenseExpiryDate: Long = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000), // 1 year default
    val masterSecretCode: String = "AK-8888",
    val systemNotice: String = "",
    val isMasterOwnerAccount: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastSyncAt: Long = System.currentTimeMillis()
) {
    fun isLicenseActive(): Boolean {
        if (isMasterOwnerAccount) return true
        if (isBlocked || status == "BLOCKED" || status == "SUSPENDED" || status == "DEACTIVATED") return false
        return System.currentTimeMillis() <= licenseExpiryDate
    }

    fun getDaysRemaining(): Long {
        if (isMasterOwnerAccount) return 9999L
        val diff = licenseExpiryDate - System.currentTimeMillis()
        return if (diff > 0) diff / (24 * 60 * 60 * 1000) else 0L
    }
}

@Entity(tableName = "saas_subscription_plans")
data class SubscriptionPlan(
    @PrimaryKey val id: String, // "TRIAL", "MONTHLY", "ANNUAL", "ENTERPRISE", "LIFETIME"
    val name: String,
    val priceRs: Double,
    val durationDays: Int,
    val maxShops: Int,
    val maxUsers: Int,
    val description: String
)
