package com.example.util

import com.example.data.dao.StoreDao
import com.example.data.entity.TenantAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

enum class AppFeature(val displayName: String, val minimumPlan: String) {
    BASIC_POS("POS Sales & Billing", "TRIAL"),
    INVENTORY_MANAGEMENT("Inventory & Stock Tracking", "TRIAL"),
    LEDGERS("Customer & Supplier Ledgers", "TRIAL"),
    SECURITY_CODES("Store Access & Security Codes", "TRIAL"),
    MULTI_STORE("Multi-Store Branch Management", "MONTHLY"),
    BATCH_IMPORT("Batch Product & Price Data Import", "MONTHLY"),
    BACKUP_RESTORE("Database Backup & Sync", "MONTHLY"),
    ADVANCED_REPORTS("Advanced Reports & Analytics", "MONTHLY"),
    CUSTOM_LOGOS("Custom Store Logo & Branding", "ANNUAL"),
    MASTER_SAAS_CONTROL("Master Owner SaaS Control Panel", "LIFETIME")
}

sealed class LicenseValidationResult {
    data class Valid(
        val tenant: TenantAccount,
        val daysRemaining: Long,
        val planType: String,
        val maxShops: Int,
        val maxUsers: Int,
        val allowedFeatures: Set<AppFeature>,
        val message: String = "Subscription is active and valid."
    ) : LicenseValidationResult()

    data class Expired(
        val tenant: TenantAccount?,
        val expiryDate: Long,
        val message: String = "Subscription has expired. Please renew your commercial license key."
    ) : LicenseValidationResult()

    data class Suspended(
        val tenant: TenantAccount?,
        val reason: String = "Account suspended by SaaS Administrator",
        val message: String = "Tenant account is suspended. Contact support to restore access."
    ) : LicenseValidationResult()

    data class Deactivated(
        val tenant: TenantAccount?,
        val message: String = "Tenant account is deactivated."
    ) : LicenseValidationResult()

    data class NotFound(
        val key: String,
        val message: String = "License Key not found in local database."
    ) : LicenseValidationResult()

    data class InvalidFormat(
        val key: String,
        val message: String = "Invalid License Key format or checksum failed."
    ) : LicenseValidationResult()
}

data class FeatureLimitResult(
    val isAllowed: Boolean,
    val currentCount: Int,
    val maxAllowed: Int,
    val message: String
)

object LicenseValidator {

    private val PLAN_RANK = mapOf(
        "TRIAL" to 1,
        "MONTHLY" to 2,
        "ANNUAL" to 3,
        "ENTERPRISE" to 4,
        "LIFETIME" to 5
    )

    /**
     * Validates a raw license key string against local Room Database records.
     */
    suspend fun validateLicenseKey(dao: StoreDao, key: String): LicenseValidationResult {
        val cleanKey = key.trim().uppercase(Locale.ROOT)
        
        if (!LicenseManager.isValidLicenseFormat(cleanKey)) {
            return LicenseValidationResult.InvalidFormat(
                key = cleanKey,
                message = "License key '$cleanKey' has invalid checksum or format."
            )
        }

        val tenant = dao.getTenantByLicenseKey(cleanKey)
            ?: return LicenseValidationResult.NotFound(
                key = cleanKey,
                message = "No registered tenant account found for key '$cleanKey'."
            )

        return validateTenantSync(tenant)
    }

    /**
     * Validates a specific tenant account stored in local Room Database by tenantId.
     */
    suspend fun validateTenant(dao: StoreDao, tenantId: String): LicenseValidationResult {
        val tenant = dao.getTenantById(tenantId)
        return validateTenantSync(tenant)
    }

    /**
     * Observes real-time license validation status from Room Database for a given tenant.
     */
    fun observeLicenseValidation(dao: StoreDao, tenantId: String): Flow<LicenseValidationResult> {
        return dao.getAllTenants().map { tenants ->
            val tenant = tenants.find { it.id == tenantId } ?: tenants.find { it.id == "TENANT_DEFAULT" }
            validateTenantSync(tenant)
        }
    }

    /**
     * Synchronous evaluation function for a TenantAccount entity.
     */
    fun validateTenantSync(tenant: TenantAccount?): LicenseValidationResult {
        if (tenant == null) {
            return LicenseValidationResult.NotFound(
                key = "",
                message = "Tenant profile not initialized in local database."
            )
        }

        if (tenant.isMasterOwnerAccount) {
            return LicenseValidationResult.Valid(
                tenant = tenant,
                daysRemaining = 9999L,
                planType = "LIFETIME",
                maxShops = tenant.maxShops,
                maxUsers = tenant.maxUsers,
                allowedFeatures = AppFeature.values().toSet(),
                message = "Master Owner Account - Unlimited Access"
            )
        }

        if (tenant.isBlocked || tenant.status == "BLOCKED") {
            return LicenseValidationResult.Suspended(
                tenant = tenant,
                reason = "BLOCKED_ILLEGAL_COPY",
                message = "This copy of the app or license key has been BLOCKED by the developer due to unauthorized copy or security policy violation."
            )
        }

        when (tenant.status.uppercase(Locale.ROOT)) {
            "SUSPENDED" -> return LicenseValidationResult.Suspended(
                tenant = tenant,
                message = "Tenant workspace '${tenant.businessName}' is suspended by SaaS Admin."
            )
            "DEACTIVATED" -> return LicenseValidationResult.Deactivated(
                tenant = tenant,
                message = "Tenant workspace '${tenant.businessName}' is currently deactivated."
            )
        }

        if (System.currentTimeMillis() > tenant.licenseExpiryDate) {
            return LicenseValidationResult.Expired(
                tenant = tenant,
                expiryDate = tenant.licenseExpiryDate,
                message = "License key '${tenant.licenseKey}' expired for '${tenant.businessName}'."
            )
        }

        val allowedFeatures = AppFeature.values().filter { feature ->
            isFeatureAllowedForPlan(tenant.planType, feature)
        }.toSet()

        return LicenseValidationResult.Valid(
            tenant = tenant,
            daysRemaining = tenant.getDaysRemaining(),
            planType = tenant.planType,
            maxShops = tenant.maxShops,
            maxUsers = tenant.maxUsers,
            allowedFeatures = allowedFeatures,
            message = "Commercial license active (${tenant.getDaysRemaining()} days remaining)."
        )
    }

    /**
     * Checks if a feature is allowed for a tenant account based on subscription status and plan level.
     */
    fun isFeatureAllowed(tenant: TenantAccount?, feature: AppFeature): Boolean {
        if (tenant == null) return false
        if (tenant.isMasterOwnerAccount) return true
        if (!tenant.isLicenseActive()) return false
        return isFeatureAllowedForPlan(tenant.planType, feature)
    }

    private fun isFeatureAllowedForPlan(planType: String, feature: AppFeature): Boolean {
        val userRank = PLAN_RANK[planType.uppercase(Locale.ROOT)] ?: 1
        val requiredRank = PLAN_RANK[feature.minimumPlan.uppercase(Locale.ROOT)] ?: 1
        return userRank >= requiredRank
    }

    /**
     * Verifies if adding another shop is within the subscription limit.
     */
    fun checkShopLimit(tenant: TenantAccount?, currentShopCount: Int): FeatureLimitResult {
        if (tenant == null) {
            return FeatureLimitResult(false, currentShopCount, 0, "No active tenant account.")
        }
        if (tenant.isMasterOwnerAccount) {
            return FeatureLimitResult(true, currentShopCount, tenant.maxShops, "Master Owner unlimited shops.")
        }
        if (!tenant.isLicenseActive()) {
            return FeatureLimitResult(false, currentShopCount, tenant.maxShops, "License is inactive or expired.")
        }
        val allowed = currentShopCount < tenant.maxShops
        val msg = if (allowed) {
            "Allowed: $currentShopCount / ${tenant.maxShops} shops used."
        } else {
            "Shop limit reached (${tenant.maxShops} max for ${tenant.planType} plan). Upgrade license to add more shops."
        }
        return FeatureLimitResult(allowed, currentShopCount, tenant.maxShops, msg)
    }

    /**
     * Verifies if adding another user is within the subscription limit.
     */
    fun checkUserLimit(tenant: TenantAccount?, currentUserCount: Int): FeatureLimitResult {
        if (tenant == null) {
            return FeatureLimitResult(false, currentUserCount, 0, "No active tenant account.")
        }
        if (tenant.isMasterOwnerAccount) {
            return FeatureLimitResult(true, currentUserCount, tenant.maxUsers, "Master Owner unlimited users.")
        }
        if (!tenant.isLicenseActive()) {
            return FeatureLimitResult(false, currentUserCount, tenant.maxUsers, "License is inactive or expired.")
        }
        val allowed = currentUserCount < tenant.maxUsers
        val msg = if (allowed) {
            "Allowed: $currentUserCount / ${tenant.maxUsers} users used."
        } else {
            "User limit reached (${tenant.maxUsers} max for ${tenant.planType} plan). Upgrade license to add more users."
        }
        return FeatureLimitResult(allowed, currentUserCount, tenant.maxUsers, msg)
    }
}
