package com.example.util

import com.example.data.entity.SubscriptionPlan
import com.example.data.entity.TenantAccount
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

object LicenseManager {

    val DEFAULT_PLANS = listOf(
        SubscriptionPlan("TRIAL_7", "7-Day Trial", 0.0, 7, 2, 5, "7-Day Full Feature Trial Access"),
        SubscriptionPlan("TRIAL_15", "15-Day Trial", 0.0, 15, 2, 5, "15-Day Full Feature Trial Access"),
        SubscriptionPlan("TRIAL_30", "30-Day Trial", 0.0, 30, 2, 5, "30-Day Full Feature Trial Access"),
        SubscriptionPlan("MONTHLY", "Monthly Professional", 5000.0, 30, 5, 10, "Full SaaS Features • 5 Shops • 10 Users • Monthly Billing"),
        SubscriptionPlan("ANNUAL", "Annual Commercial Enterprise", 45000.0, 365, 15, 50, "Full Multi-Store SaaS • 15 Shops • 50 Users • Priority Support"),
        SubscriptionPlan("LIFETIME", "Lifetime Master License", 120000.0, 36500, 50, 200, "Unlimited Access • All Features • Lifetime License")
    )

    fun generateLicenseKey(prefix: String, planType: String = "ANNUAL"): String {
        val cleanPrefix = prefix.uppercase(Locale.ROOT).replace(Regex("[^A-Z0-9]"), "").take(4).ifBlank { "POS" }
        val planCode = when {
            planType.uppercase(Locale.ROOT).startsWith("TRIAL") -> "TR"
            planType.uppercase(Locale.ROOT) == "MONTHLY" -> "MO"
            planType.uppercase(Locale.ROOT) == "LIFETIME" -> "LT"
            else -> "AN"
        }
        val year = "2026"
        val randomHex = UUID.randomUUID().toString().replace("-", "").take(4).uppercase(Locale.ROOT)
        
        val rawToHash = "$cleanPrefix-$planCode-$year-$randomHex-SAAS-SECRET-2026"
        val checksum = SecurityUtils.hashSha256(rawToHash).take(4).uppercase(Locale.ROOT)

        return "SAAS-$cleanPrefix-$planCode-$year-$randomHex-$checksum"
    }

    fun isValidLicenseFormat(key: String): Boolean {
        val parts = key.trim().uppercase(Locale.ROOT).split("-")
        if (parts.size < 6 || parts[0] != "SAAS") return false
        val prefix = parts[1]
        val planCode = parts[2]
        val year = parts[3]
        val randomHex = parts[4]
        val providedChecksum = parts[5]

        val rawToHash = "$prefix-$planCode-$year-$randomHex-SAAS-SECRET-2026"
        val expectedChecksum = SecurityUtils.hashSha256(rawToHash).take(4).uppercase(Locale.ROOT)

        return providedChecksum == expectedChecksum
    }

    fun generateMasterSecretCode(tenantCode: String): String {
        val hash = SecurityUtils.hashSha256("$tenantCode-MASTER-SECRET-KEY-2026")
        val digits = hash.filter { it.isDigit() }.take(4)
        return if (digits.length == 4) "AK-$digits" else "AK-8888"
    }

    fun createDefaultMasterAccount(): TenantAccount {
        return TenantAccount(
            id = "TENANT_MASTER_OWNER",
            tenantCode = "MASTER_OWNER",
            businessName = "Master SaaS Developer Portal",
            ownerName = "Master Owner (Developer)",
            email = "developer@saaspos.com",
            phone = "03000000000",
            licenseKey = "SAAS-MASTER-OWNER-2026-KEYS-9999",
            planType = "LIFETIME",
            status = "ACTIVE",
            paymentStatus = "PAID",
            maxShops = 1000,
            maxUsers = 10000,
            licenseExpiryDate = System.currentTimeMillis() + (36500L * 24 * 60 * 60 * 1000),
            masterSecretCode = "AK-9999",
            isMasterOwnerAccount = true
        )
    }

    fun createFreeTrialTenant(
        trialDays: Int = 15,
        installationId: String = ""
    ): TenantAccount {
        val tenantId = "TENANT_TRIAL_" + UUID.randomUUID().toString().take(6).uppercase(Locale.ROOT)
        val tenantCode = "TR-" + (1000..9999).random()
        val licenseKey = generateLicenseKey(tenantCode, "TRIAL_$trialDays")
        val durationMs = trialDays * 24L * 60 * 60 * 1000

        return TenantAccount(
            id = tenantId,
            tenantCode = tenantCode,
            businessName = "Trial Store Workspace",
            ownerName = "Trial Customer",
            phone = "",
            licenseKey = licenseKey,
            planType = "TRIAL_$trialDays",
            status = "ACTIVE",
            paymentStatus = "FREE_TRIAL",
            boundInstallationId = installationId,
            maxShops = 2,
            maxUsers = 5,
            licenseExpiryDate = System.currentTimeMillis() + durationMs,
            masterSecretCode = "AK-8888",
            isMasterOwnerAccount = false
        )
    }

    fun createInitialCustomerTenant(
        id: String = "TENANT_001",
        tenantCode: String = "COMMERCIAL-POS",
        businessName: String = "Commercial Store",
        ownerName: String = "Store Owner",
        phone: String = "03000000000",
        planType: String = "ANNUAL",
        installationId: String = ""
    ): TenantAccount {
        val plan = DEFAULT_PLANS.find { it.id == planType } ?: DEFAULT_PLANS[4]
        val durationMs = plan.durationDays * 24L * 60 * 60 * 1000
        val licenseKey = generateLicenseKey(tenantCode, planType)

        return TenantAccount(
            id = id,
            tenantCode = tenantCode,
            businessName = businessName,
            ownerName = ownerName,
            phone = phone,
            licenseKey = licenseKey,
            planType = plan.id,
            status = "ACTIVE",
            paymentStatus = if (planType.startsWith("TRIAL")) "FREE_TRIAL" else "PAID",
            boundInstallationId = installationId,
            maxShops = plan.maxShops,
            maxUsers = plan.maxUsers,
            licenseExpiryDate = System.currentTimeMillis() + durationMs,
            masterSecretCode = "AK-8888",
            isMasterOwnerAccount = false
        )
    }
}
