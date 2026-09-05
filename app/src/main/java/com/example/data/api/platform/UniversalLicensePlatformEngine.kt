package com.example.data.api.platform

import android.content.Context
import android.content.SharedPreferences
import com.example.util.SecurityUtils
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * Universal Developer License Platform Engine
 * 
 * Provides centralized Developer/Owner control architecture for managing:
 * - Multiple Applications (Application Registry with Isolation)
 * - Multiple Customers / Accounts
 * - Multiple Installations / Device Bindings
 * - Application-Specific Cryptographic License Generation & Validation
 * - Full Lifecycle (Activate, Renew, Extend, Suspend, Reactivate, Deactivate, Device Transfer)
 * - Expiry Management & Warnings (30d, 15d, 7d, 3d, 1d)
 * - Immutable Audit Logging
 */
class UniversalLicensePlatformEngine private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // Application Registry State
    private val _applicationsFlow = MutableStateFlow<List<RegisteredApplication>>(emptyList())
    val applicationsFlow: StateFlow<List<RegisteredApplication>> = _applicationsFlow.asStateFlow()

    // Customer Accounts State
    private val _customersFlow = MutableStateFlow<List<CustomerAccount>>(emptyList())
    val customersFlow: StateFlow<List<CustomerAccount>> = _customersFlow.asStateFlow()

    // Installations State
    private val _installationsFlow = MutableStateFlow<List<DeviceInstallation>>(emptyList())
    val installationsFlow: StateFlow<List<DeviceInstallation>> = _installationsFlow.asStateFlow()

    // Licenses State
    private val _licensesFlow = MutableStateFlow<List<UniversalLicense>>(emptyList())
    val licensesFlow: StateFlow<List<UniversalLicense>> = _licensesFlow.asStateFlow()

    // Audit Logs State
    private val _auditLogsFlow = MutableStateFlow<List<LicenseAuditLog>>(emptyList())
    val auditLogsFlow: StateFlow<List<LicenseAuditLog>> = _auditLogsFlow.asStateFlow()

    init {
        loadOrInitializeRegistry()
    }

    private fun loadOrInitializeRegistry() {
        // 1. Applications
        val appsJson = prefs.getString(KEY_APPLICATIONS, null)
        val loadedApps: List<RegisteredApplication> = if (appsJson != null) {
            try {
                val type = Types.newParameterizedType(List::class.java, RegisteredApplication::class.java)
                moshi.adapter<List<RegisteredApplication>>(type).fromJson(appsJson) ?: defaultApplications()
            } catch (e: Exception) {
                defaultApplications()
            }
        } else {
            defaultApplications()
        }
        _applicationsFlow.value = loadedApps
        saveApplications(loadedApps)

        // 2. Customers
        val customersJson = prefs.getString(KEY_CUSTOMERS, null)
        val loadedCustomers: List<CustomerAccount> = if (customersJson != null) {
            try {
                val type = Types.newParameterizedType(List::class.java, CustomerAccount::class.java)
                moshi.adapter<List<CustomerAccount>>(type).fromJson(customersJson) ?: defaultCustomers()
            } catch (e: Exception) {
                defaultCustomers()
            }
        } else {
            defaultCustomers()
        }
        _customersFlow.value = loadedCustomers
        saveCustomers(loadedCustomers)

        // 3. Installations
        val installationsJson = prefs.getString(KEY_INSTALLATIONS, null)
        val loadedInstallations: List<DeviceInstallation> = if (installationsJson != null) {
            try {
                val type = Types.newParameterizedType(List::class.java, DeviceInstallation::class.java)
                moshi.adapter<List<DeviceInstallation>>(type).fromJson(installationsJson) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        _installationsFlow.value = loadedInstallations

        // 4. Licenses
        val licensesJson = prefs.getString(KEY_LICENSES, null)
        val loadedLicenses: List<UniversalLicense> = if (licensesJson != null) {
            try {
                val type = Types.newParameterizedType(List::class.java, UniversalLicense::class.java)
                moshi.adapter<List<UniversalLicense>>(type).fromJson(licensesJson) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
        _licensesFlow.value = loadedLicenses

        // 5. Audit Logs
        val logsJson = prefs.getString(KEY_AUDIT_LOGS, null)
        val loadedLogs: List<LicenseAuditLog> = if (logsJson != null) {
            try {
                val type = Types.newParameterizedType(List::class.java, LicenseAuditLog::class.java)
                moshi.adapter<List<LicenseAuditLog>>(type).fromJson(logsJson) ?: defaultInitialLogs()
            } catch (e: Exception) {
                defaultInitialLogs()
            }
        } else {
            defaultInitialLogs()
        }
        _auditLogsFlow.value = loadedLogs
    }

    private fun defaultApplications(): List<RegisteredApplication> = listOf(
        RegisteredApplication(
            appId = APP_SENTRY_STORE_POS,
            appName = "SENTRY STORE POS",
            packageIdentifier = "com.aistudio.sentrystore.pos",
            version = "2.4.0",
            developerId = "DEV-CH-UMER",
            status = "ACTIVE",
            secretSalt = "SENTRY_POS_MASTER_SALT_2026",
            iconEmoji = "🏪"
        ),
        RegisteredApplication(
            appId = "WORKOUT-APP",
            appName = "FitPulse Pro Gym & Workout",
            packageIdentifier = "com.aistudio.fitpulse.app",
            version = "1.0.0",
            developerId = "DEV-CH-UMER",
            status = "ACTIVE",
            secretSalt = "FITPULSE_GYM_SALT_2026",
            iconEmoji = "🏋️"
        ),
        RegisteredApplication(
            appId = "BUSINESS-APP",
            appName = "OmniBiz ERP Suite",
            packageIdentifier = "com.aistudio.omnibiz.erp",
            version = "1.5.0",
            developerId = "DEV-CH-UMER",
            status = "ACTIVE",
            secretSalt = "OMNIBIZ_ERP_SALT_2026",
            iconEmoji = "💼"
        ),
        RegisteredApplication(
            appId = "INVENTORY-APP",
            appName = "Apex Inventory & Warehouse",
            packageIdentifier = "com.aistudio.apexinv.app",
            version = "1.2.0",
            developerId = "DEV-CH-UMER",
            status = "ACTIVE",
            secretSalt = "APEX_INV_SALT_2026",
            iconEmoji = "📦"
        ),
        RegisteredApplication(
            appId = "ATTENDANCE-APP",
            appName = "TimeTrack Biometric Staff",
            packageIdentifier = "com.aistudio.timetrack.app",
            version = "1.1.0",
            developerId = "DEV-CH-UMER",
            status = "ACTIVE",
            secretSalt = "TIMETRACK_STAFF_SALT_2026",
            iconEmoji = "⏱️"
        )
    )

    private fun defaultCustomers(): List<CustomerAccount> = listOf(
        CustomerAccount(
            customerId = "CUST-1001",
            businessName = "Al-Khair Cash & Carry",
            contactPerson = "Muhammad Umer",
            phone = "03080018035",
            email = "support@sentrystore.com",
            address = "Main Market, Commercial Center",
            status = "ACTIVE",
            notes = "Primary VIP Retail Enterprise Account"
        ),
        CustomerAccount(
            customerId = "CUST-1002",
            businessName = "Bahria Heights Mart",
            contactPerson = "Tariq Mahmood",
            phone = "03001234567",
            email = "bahria@mart.pk",
            address = "Sector C, Bahria Town",
            status = "ACTIVE",
            notes = "Multi-branch store setup"
        ),
        CustomerAccount(
            customerId = "CUST-1003",
            businessName = "Lahori Sweets & Bakers",
            contactPerson = "Haji Aslam",
            phone = "03217654321",
            email = "aslam@lahoribakers.com",
            address = "Mall Road, Lahore",
            status = "ACTIVE"
        )
    )

    private fun defaultInitialLogs(): List<LicenseAuditLog> = listOf(
        LicenseAuditLog(
            id = "LOG-INIT-01",
            action = "PLATFORM_INITIALIZED",
            appId = APP_SENTRY_STORE_POS,
            customerId = "CUST-1001",
            installationId = "SYSTEM",
            licenseId = "SYS-PLATFORM",
            performedBy = "Master Developer",
            details = "Universal Developer License Platform initialized with active registry."
        )
    )

    // ==========================================
    // APPLICATION REGISTRY OPERATIONS
    // ==========================================

    fun registerApplication(
        appId: String,
        appName: String,
        packageIdentifier: String,
        version: String = "1.0.0",
        developerId: String = "DEV-CH-UMER",
        iconEmoji: String = "📱"
    ): RegisteredApplication {
        val cleanAppId = appId.trim().uppercase().replace(" ", "-")
        val newApp = RegisteredApplication(
            appId = cleanAppId,
            appName = appName.trim(),
            packageIdentifier = packageIdentifier.trim(),
            version = version.trim(),
            developerId = developerId.trim(),
            status = "ACTIVE",
            secretSalt = "${cleanAppId}_SALT_${System.currentTimeMillis()}",
            iconEmoji = iconEmoji
        )
        val updated = _applicationsFlow.value.filter { it.appId != cleanAppId } + newApp
        _applicationsFlow.value = updated
        saveApplications(updated)
        logAction(
            action = "APPLICATION_REGISTERED",
            appId = cleanAppId,
            customerId = "ALL",
            installationId = "N/A",
            licenseId = "N/A",
            performedBy = "Master Developer",
            details = "Registered new application: ${appName.trim()} ($cleanAppId)"
        )
        return newApp
    }

    fun getApplication(appId: String): RegisteredApplication? {
        return _applicationsFlow.value.firstOrNull { it.appId.equals(appId.trim(), ignoreCase = true) }
    }

    fun updateApplicationStatus(appId: String, status: String) {
        val updated = _applicationsFlow.value.map {
            if (it.appId.equals(appId, ignoreCase = true)) it.copy(status = status) else it
        }
        _applicationsFlow.value = updated
        saveApplications(updated)
    }

    // ==========================================
    // CUSTOMER MANAGEMENT
    // ==========================================

    fun createCustomer(
        businessName: String,
        contactPerson: String,
        phone: String = "",
        email: String = "",
        address: String = "",
        notes: String = ""
    ): CustomerAccount {
        val nextId = "CUST-${1000 + _customersFlow.value.size + 1}"
        val newCust = CustomerAccount(
            customerId = nextId,
            businessName = businessName.trim(),
            contactPerson = contactPerson.trim(),
            phone = phone.trim(),
            email = email.trim(),
            address = address.trim(),
            status = "ACTIVE",
            notes = notes.trim()
        )
        val updated = _customersFlow.value + newCust
        _customersFlow.value = updated
        saveCustomers(updated)
        logAction(
            action = "CUSTOMER_CREATED",
            appId = "ALL",
            customerId = nextId,
            installationId = "N/A",
            licenseId = "N/A",
            performedBy = "Developer Control",
            details = "Created Customer Account: $businessName ($nextId)"
        )
        return newCust
    }

    fun updateCustomer(customer: CustomerAccount) {
        val updated = _customersFlow.value.map {
            if (it.customerId == customer.customerId) customer else it
        }
        _customersFlow.value = updated
        saveCustomers(updated)
    }

    // ==========================================
    // INSTALLATION & DEVICE BINDING
    // ==========================================

    fun registerOrGetInstallation(
        installationId: String,
        appId: String,
        customerId: String = "CUST-1001",
        deviceModel: String = "Android Terminal",
        deviceFingerprint: String = ""
    ): DeviceInstallation {
        val cleanInstId = installationId.trim()
        val existing = _installationsFlow.value.firstOrNull {
            it.installationId.equals(cleanInstId, ignoreCase = true) && it.appId.equals(appId, ignoreCase = true)
        }
        if (existing != null) {
            val updated = existing.copy(lastVerifiedAt = System.currentTimeMillis())
            val list = _installationsFlow.value.map { if (it.installationId == cleanInstId && it.appId == appId) updated else it }
            _installationsFlow.value = list
            saveInstallations(list)
            return updated
        }
        val newInst = DeviceInstallation(
            installationId = cleanInstId,
            appId = appId.trim().uppercase(),
            customerId = customerId,
            deviceModel = deviceModel,
            deviceFingerprint = deviceFingerprint,
            status = "BOUND"
        )
        val updatedList = _installationsFlow.value + newInst
        _installationsFlow.value = updatedList
        saveInstallations(updatedList)
        logAction(
            action = "INSTALLATION_BOUND",
            appId = appId,
            customerId = customerId,
            installationId = cleanInstId,
            licenseId = "N/A",
            performedBy = "Installation System",
            details = "Device bound: $cleanInstId for App $appId"
        )
        return newInst
    }

    // ==========================================
    // APPLICATION-SPECIFIC LICENSE GENERATION
    // ==========================================

    /**
     * Generates an application-isolated cryptographically-signed license.
     * 
     * Key Isolation Property:
     * A license generated for `APP-001` (SENTRY-STORE-POS) will NOT validate on `APP-002` (WORKOUT-APP),
     * because the Application ID is directly baked into the cryptographic salt and payload signature.
     */
    fun generateLicense(
        appId: String,
        customerId: String,
        installationId: String,
        durationDays: Int, // 30, 90, 180, 365, 0 (Lifetime), or custom
        planNameOverride: String? = null,
        createdBy: String = "Master Developer"
    ): UniversalLicense {
        val cleanAppId = appId.trim().uppercase()
        val cleanInstId = installationId.trim()
        val app = getApplication(cleanAppId) ?: registerApplication(cleanAppId, cleanAppId, "com.app.$cleanAppId")

        val (planType, defaultPlanName) = when (durationDays) {
            30 -> "DEMO_1M" to "Demo 1 Month Plan"
            90 -> "STD_3M" to "3 Months Standard Plan"
            180 -> "PRO_6M" to "6 Months Professional Plan"
            365 -> "ENT_1Y" to "1 Year Enterprise Plan"
            0 -> "LIFETIME" to "Lifetime Commercial License"
            else -> "CUSTOM" to "$durationDays Days Custom Plan"
        }
        val effectivePlanName = planNameOverride ?: defaultPlanName
        val now = System.currentTimeMillis()
        val expiryDate = if (durationDays > 0) now + (durationDays.toLong() * 24 * 60 * 60 * 1000L) else 0L

        // Generate Application-Isolated Cryptographic License Code
        val licenseCode = generateApplicationCryptographicCode(cleanAppId, cleanInstId, durationDays, app.secretSalt)
        val token = SecurityUtils.generateDeterministicToken(
            "$cleanAppId:$cleanInstId",
            secretSalt = app.secretSalt
        )

        val licenseId = "LIC-${cleanAppId.take(6)}-${1000 + _licensesFlow.value.size + 1}"
        val universalLicense = UniversalLicense(
            licenseId = licenseId,
            appId = cleanAppId,
            customerId = customerId,
            installationId = cleanInstId,
            licenseCode = licenseCode,
            planType = planType,
            planName = effectivePlanName,
            durationDays = durationDays,
            startDate = now,
            expiryDate = expiryDate,
            status = "ACTIVE",
            signatureToken = token,
            createdBy = createdBy,
            createdAt = now,
            lastModifiedAt = now
        )

        val updatedLicenses = _licensesFlow.value.filter {
            !(it.appId.equals(cleanAppId, ignoreCase = true) && it.installationId.equals(cleanInstId, ignoreCase = true) && it.status == "ACTIVE")
        } + universalLicense

        _licensesFlow.value = updatedLicenses
        saveLicenses(updatedLicenses)

        // Ensure installation is registered
        registerOrGetInstallation(cleanInstId, cleanAppId, customerId)

        logAction(
            action = "LICENSE_GENERATED",
            appId = cleanAppId,
            customerId = customerId,
            installationId = cleanInstId,
            licenseId = licenseId,
            performedBy = createdBy,
            details = "Generated $effectivePlanName for $cleanAppId (Code: $licenseCode)"
        )

        return universalLicense
    }

    // ==========================================
    // CRYPTOGRAPHIC VERIFICATION & ISOLATION
    // ==========================================

    /**
     * Verifies license code with strict Application ID & Installation ID isolation.
     */
    fun verifyLicenseCode(
        currentAppId: String,
        currentInstallationId: String,
        licenseCode: String
    ): LicenseVerificationResult {
        val cleanAppId = currentAppId.trim().uppercase()
        val cleanInstId = currentInstallationId.trim()
        val upperCode = licenseCode.trim().uppercase()

        if (upperCode.isBlank()) {
            return LicenseVerificationResult.Invalid("Activation code cannot be empty.", "EMPTY_CODE")
        }

        val app = getApplication(cleanAppId)

        // 1. Check Universal Engine Active Licenses Store first
        val storedLicense = _licensesFlow.value.firstOrNull {
            it.licenseCode.equals(upperCode, ignoreCase = true)
        }

        if (storedLicense != null) {
            // Strict Application Isolation check
            if (!storedLicense.appId.equals(cleanAppId, ignoreCase = true)) {
                return LicenseVerificationResult.AppMismatch(
                    expectedApp = cleanAppId,
                    actualApp = storedLicense.appId
                )
            }

            // Strict Installation Isolation check
            if (!storedLicense.installationId.equals(cleanInstId, ignoreCase = true)) {
                return LicenseVerificationResult.InstallationMismatch(
                    expectedInstallation = cleanInstId,
                    actualInstallation = storedLicense.installationId
                )
            }

            // Status check
            if (storedLicense.status == "SUSPENDED") {
                return LicenseVerificationResult.Suspended("License has been suspended by Developer / Owner.")
            }
            if (storedLicense.status == "REVOKED" || storedLicense.status == "TRANSFERRED") {
                return LicenseVerificationResult.Invalid("License is no longer valid (${storedLicense.status}).", "INACTIVE_STATUS")
            }

            // Expiry check
            if (storedLicense.expiryDate > 0L && System.currentTimeMillis() > storedLicense.expiryDate) {
                return LicenseVerificationResult.Expired(storedLicense.expiryDate, "License expired on ${formatDate(storedLicense.expiryDate)}.")
            }

            return LicenseVerificationResult.Valid(storedLicense, "License verified for ${storedLicense.planName}.")
        }

        // 2. Check if it is an explicit App-Prefixed Universal Code: ACTV-{APP_ID}-{PLAN}-{P1}-{P2}-{P3}
        if (upperCode.startsWith("ACTV-") && upperCode.contains("-")) {
            val parts = upperCode.split("-")
            // Format: ACTV-APPID-... or ACTV-M1-... (Legacy Sentry Store POS)
            if (parts.size >= 4) {
                // If code contains explicit application ID tag
                val potentialAppId = parts.getOrNull(1) ?: ""
                val matchesApp = cleanAppId.equals(potentialAppId, ignoreCase = true) ||
                        cleanAppId.contains(potentialAppId, ignoreCase = true) ||
                        potentialAppId.contains(cleanAppId, ignoreCase = true) ||
                        (cleanAppId.startsWith("SENTRY") && (potentialAppId == "POS" || potentialAppId == "SENTRY"))
                if (!matchesApp && (potentialAppId.startsWith("APP") || potentialAppId.contains("POS") || potentialAppId.contains("WORKOUT") || potentialAppId.contains("BUSINESS"))) {
                    return LicenseVerificationResult.AppMismatch(
                        expectedApp = cleanAppId,
                        actualApp = potentialAppId
                    )
                }
            }
        }

        // 3. Fallback: Offline Cryptographic Mathematical Algorithm Verification
        val salt = app?.secretSalt ?: "CH_UMER_SECRET_KEY_2026"
        val verifiedPlan = verifyOfflineMathematicalCode(cleanAppId, cleanInstId, upperCode, salt)
        if (verifiedPlan != null) {
            val now = System.currentTimeMillis()
            val expiry = if (verifiedPlan.days > 0) now + (verifiedPlan.days.toLong() * 24 * 60 * 60 * 1000L) else 0L
            val synthesizedLicense = UniversalLicense(
                licenseId = "LIC-OFFLINE-${cleanAppId.take(4)}-${System.currentTimeMillis() % 10000}",
                appId = cleanAppId,
                customerId = "CUST-1001",
                installationId = cleanInstId,
                licenseCode = upperCode,
                planType = if (verifiedPlan.days == 0) "LIFETIME" else "CUSTOM_${verifiedPlan.days}D",
                planName = verifiedPlan.planName,
                durationDays = verifiedPlan.days,
                startDate = now,
                expiryDate = expiry,
                status = "ACTIVE",
                signatureToken = SecurityUtils.generateDeterministicToken("$cleanAppId:$cleanInstId", salt),
                createdBy = "Offline Crypto Verifier"
            )
            return LicenseVerificationResult.Valid(synthesizedLicense, "Cryptographic license verified (${verifiedPlan.planName}).")
        }

        return LicenseVerificationResult.Invalid("Invalid activation key or installation mismatch.", "VERIFICATION_FAILED")
    }

    private data class VerifiedMathPlan(val days: Int, val planName: String)

    private fun verifyOfflineMathematicalCode(
        appId: String,
        installationId: String,
        code: String,
        salt: String
    ): VerifiedMathPlan? {
        val upper = code.trim().uppercase()

        // 1. Universal App-Scoped Pattern: ACTV-{APP}-{PLAN}-{HASH1}-{HASH2}-{HASH3}
        val planConfigs = listOf(
            30 to "Demo 1 Month",
            90 to "3 Months Standard",
            180 to "6 Months Professional",
            365 to "1 Year Enterprise",
            0 to "Lifetime Commercial"
        )

        for ((days, name) in planConfigs) {
            val expectedUniversal = generateApplicationCryptographicCode(appId, installationId, days, salt)
            if (upper.equals(expectedUniversal, ignoreCase = true)) {
                return VerifiedMathPlan(days, name)
            }
        }

        // 2. Legacy Sentry Store POS Codes (ACTV-M1-..., ACTV-M3-..., ACTV-LIFE-..., ACTV-XXXX-XXXX-XXXX-XXXX)
        if (appId.equals(APP_SENTRY_STORE_POS, ignoreCase = true) || appId.equals("APP-001", ignoreCase = true)) {
            // Check legacy plan format
            for ((days, name) in planConfigs) {
                val expectedLegacy = com.example.data.api.security.AppActivationManager.generatePlanActivationCode(installationId, days)
                if (upper.equals(expectedLegacy, ignoreCase = true)) {
                    return VerifiedMathPlan(days, name)
                }
            }

            // Check legacy 16-char lifetime format
            val legacyLifetime = run {
                val hash = SecurityUtils.sha256("$installationId:CH_UMER_SECRET_KEY_2026")
                val p1 = hash.substring(0, 4)
                val p2 = hash.substring(4, 8)
                val p3 = hash.substring(8, 12)
                val p4 = hash.substring(12, 16)
                "ACTV-$p1-$p2-$p3-$p4"
            }
            if (upper.equals(legacyLifetime, ignoreCase = true)) {
                return VerifiedMathPlan(0, "Lifetime Commercial License")
            }

            // Check custom days ACTV-D{days}-...
            if (upper.startsWith("ACTV-D")) {
                val parts = upper.split("-")
                if (parts.size >= 4) {
                    val daysStr = parts[1].removePrefix("D")
                    val days = daysStr.toIntOrNull()
                    if (days != null && days > 0) {
                        val expected = com.example.data.api.security.AppActivationManager.generatePlanActivationCode(installationId, days)
                        if (upper.equals(expected, ignoreCase = true)) {
                            return VerifiedMathPlan(days, "$days Days Custom Plan")
                        }
                    }
                }
            }
        }

        return null
    }

    private fun generateApplicationCryptographicCode(
        appId: String,
        installationId: String,
        planDays: Int,
        salt: String
    ): String {
        val planTag = when (planDays) {
            30 -> "M1"
            90 -> "M3"
            180 -> "M6"
            365 -> "Y1"
            0 -> "LIFE"
            else -> "D$planDays"
        }
        val appTag = appId.replace("SENTRY-STORE-", "").take(6)
        val payload = "$appId:$installationId:$planDays:$salt"
        val hash = SecurityUtils.sha256(payload)
        val p1 = hash.substring(0, 4)
        val p2 = hash.substring(4, 8)
        val p3 = hash.substring(8, 12)
        return "ACTV-$appTag-$planTag-$p1-$p2-$p3"
    }

    // ==========================================
    // LICENSE LIFECYCLE MANAGEMENT
    // ==========================================

    fun extendLicense(licenseId: String, days: Int, performedBy: String = "Developer / Owner"): UniversalLicense? {
        if (days <= 0) return null
        val target = _licensesFlow.value.firstOrNull { it.licenseId == licenseId } ?: return null
        val base = if (target.expiryDate > System.currentTimeMillis()) target.expiryDate else System.currentTimeMillis()
        val newExpiry = base + (days.toLong() * 24 * 60 * 60 * 1000L)
        val updated = target.copy(
            expiryDate = newExpiry,
            status = "ACTIVE",
            lastModifiedAt = System.currentTimeMillis()
        )
        val list = _licensesFlow.value.map { if (it.licenseId == licenseId) updated else it }
        _licensesFlow.value = list
        saveLicenses(list)

        logAction(
            action = "LICENSE_EXTENDED",
            appId = target.appId,
            customerId = target.customerId,
            installationId = target.installationId,
            licenseId = licenseId,
            performedBy = performedBy,
            details = "Extended by +$days days. New Expiry: ${formatDate(newExpiry)}"
        )
        return updated
    }

    fun renewLicense(
        licenseId: String,
        newPlanDays: Int,
        newPlanName: String,
        performedBy: String = "Developer / Owner"
    ): UniversalLicense? {
        val target = _licensesFlow.value.firstOrNull { it.licenseId == licenseId } ?: return null
        val newExpiry = if (newPlanDays > 0) System.currentTimeMillis() + (newPlanDays.toLong() * 24 * 60 * 60 * 1000L) else 0L
        val updated = target.copy(
            durationDays = newPlanDays,
            planName = newPlanName,
            expiryDate = newExpiry,
            status = "ACTIVE",
            lastModifiedAt = System.currentTimeMillis()
        )
        val list = _licensesFlow.value.map { if (it.licenseId == licenseId) updated else it }
        _licensesFlow.value = list
        saveLicenses(list)

        logAction(
            action = "LICENSE_RENEWED",
            appId = target.appId,
            customerId = target.customerId,
            installationId = target.installationId,
            licenseId = licenseId,
            performedBy = performedBy,
            details = "Renewed for $newPlanName ($newPlanDays days)"
        )
        return updated
    }

    fun suspendLicense(licenseId: String, reason: String, performedBy: String = "Developer / Owner"): UniversalLicense? {
        val target = _licensesFlow.value.firstOrNull { it.licenseId == licenseId } ?: return null
        val updated = target.copy(status = "SUSPENDED", lastModifiedAt = System.currentTimeMillis())
        val list = _licensesFlow.value.map { if (it.licenseId == licenseId) updated else it }
        _licensesFlow.value = list
        saveLicenses(list)

        logAction(
            action = "LICENSE_SUSPENDED",
            appId = target.appId,
            customerId = target.customerId,
            installationId = target.installationId,
            licenseId = licenseId,
            performedBy = performedBy,
            details = "Suspended. Reason: $reason"
        )
        return updated
    }

    fun reactivateLicense(licenseId: String, performedBy: String = "Developer / Owner"): UniversalLicense? {
        val target = _licensesFlow.value.firstOrNull { it.licenseId == licenseId } ?: return null
        val updated = target.copy(status = "ACTIVE", lastModifiedAt = System.currentTimeMillis())
        val list = _licensesFlow.value.map { if (it.licenseId == licenseId) updated else it }
        _licensesFlow.value = list
        saveLicenses(list)

        logAction(
            action = "LICENSE_REACTIVATED",
            appId = target.appId,
            customerId = target.customerId,
            installationId = target.installationId,
            licenseId = licenseId,
            performedBy = performedBy,
            details = "Reactivated to ACTIVE status"
        )
        return updated
    }

    fun deactivateLicense(licenseId: String, performedBy: String = "Developer / Owner"): UniversalLicense? {
        val target = _licensesFlow.value.firstOrNull { it.licenseId == licenseId } ?: return null
        val updated = target.copy(status = "REVOKED", lastModifiedAt = System.currentTimeMillis())
        val list = _licensesFlow.value.map { if (it.licenseId == licenseId) updated else it }
        _licensesFlow.value = list
        saveLicenses(list)

        logAction(
            action = "LICENSE_DEACTIVATED",
            appId = target.appId,
            customerId = target.customerId,
            installationId = target.installationId,
            licenseId = licenseId,
            performedBy = performedBy,
            details = "License permanently revoked/deactivated"
        )
        return updated
    }

    // ==========================================
    // DEVICE TRANSFER WORKFLOW
    // ==========================================

    /**
     * Transfers an active license from an Old Device/Installation to a New Device/Installation.
     */
    fun transferDevice(
        licenseId: String,
        newInstallationId: String,
        performedBy: String = "Developer / Owner"
    ): UniversalLicense? {
        val target = _licensesFlow.value.firstOrNull { it.licenseId == licenseId } ?: return null
        val oldInstallation = target.installationId
        val cleanNewInst = newInstallationId.trim()

        // 1. Release old installation binding
        val updatedInstallations = _installationsFlow.value.map {
            if (it.installationId == oldInstallation && it.appId == target.appId) {
                it.copy(status = "RELEASED")
            } else it
        }
        _installationsFlow.value = updatedInstallations
        saveInstallations(updatedInstallations)

        // 2. Generate new cryptographic license for new device
        val app = getApplication(target.appId)
        val remainingDays = target.getDaysRemaining()
        val durationForNew = if (target.expiryDate <= 0L) 0 else remainingDays.toInt().coerceAtLeast(1)
        val newCode = generateApplicationCryptographicCode(target.appId, cleanNewInst, durationForNew, app?.secretSalt ?: "SALT")
        val newToken = SecurityUtils.generateDeterministicToken("${target.appId}:$cleanNewInst", app?.secretSalt ?: "SALT")

        val oldTransferredLicense = target.copy(
            status = "TRANSFERRED",
            lastModifiedAt = System.currentTimeMillis()
        )

        val newLicense = UniversalLicense(
            licenseId = "LIC-TRF-${target.appId.take(4)}-${System.currentTimeMillis() % 100000}",
            appId = target.appId,
            customerId = target.customerId,
            installationId = cleanNewInst,
            licenseCode = newCode,
            planType = target.planType,
            planName = target.planName,
            durationDays = durationForNew,
            startDate = System.currentTimeMillis(),
            expiryDate = target.expiryDate,
            signatureToken = newToken,
            status = "ACTIVE",
            createdBy = performedBy,
            createdAt = System.currentTimeMillis(),
            lastModifiedAt = System.currentTimeMillis()
        )

        val list = _licensesFlow.value.map { if (it.licenseId == licenseId) oldTransferredLicense else it } + newLicense
        _licensesFlow.value = list
        saveLicenses(list)

        // 3. Register new device installation
        registerOrGetInstallation(cleanNewInst, target.appId, target.customerId)

        logAction(
            action = "DEVICE_TRANSFERRED",
            appId = target.appId,
            customerId = target.customerId,
            installationId = cleanNewInst,
            licenseId = newLicense.licenseId,
            performedBy = performedBy,
            details = "Transferred from Old Terminal $oldInstallation to New Terminal $cleanNewInst. New Code: $newCode"
        )

        return newLicense
    }

    // ==========================================
    // EXPIRY WARNINGS
    // ==========================================

    fun checkExpiryWarning(expiryTimestamp: Long): ExpiryWarningTier? {
        if (expiryTimestamp <= 0L) return null // Lifetime
        val now = System.currentTimeMillis()
        val diffMs = expiryTimestamp - now
        if (diffMs <= 0) return ExpiryWarningTier.EXPIRED

        val daysLeft = diffMs / (1000L * 60 * 60 * 24)
        return when {
            daysLeft <= 1 -> ExpiryWarningTier.TIER_1_DAY
            daysLeft <= 3 -> ExpiryWarningTier.TIER_3_DAYS
            daysLeft <= 7 -> ExpiryWarningTier.TIER_7_DAYS
            daysLeft <= 15 -> ExpiryWarningTier.TIER_15_DAYS
            daysLeft <= 30 -> ExpiryWarningTier.TIER_30_DAYS
            else -> null
        }
    }

    // ==========================================
    // AUDIT LOGGING & PERSISTENCE
    // ==========================================

    private fun logAction(
        action: String,
        appId: String,
        customerId: String,
        installationId: String,
        licenseId: String,
        performedBy: String,
        details: String
    ) {
        val entry = LicenseAuditLog(
            id = "LOG-${System.currentTimeMillis() % 100000}-${UUID.randomUUID().toString().take(4).uppercase()}",
            action = action,
            appId = appId,
            customerId = customerId,
            installationId = installationId,
            licenseId = licenseId,
            performedBy = performedBy,
            timestamp = System.currentTimeMillis(),
            details = details
        )
        val updated = listOf(entry) + _auditLogsFlow.value.take(200)
        _auditLogsFlow.value = updated
        try {
            val type = Types.newParameterizedType(List::class.java, LicenseAuditLog::class.java)
            val json = moshi.adapter<List<LicenseAuditLog>>(type).toJson(updated)
            prefs.edit().putString(KEY_AUDIT_LOGS, json).apply()
        } catch (e: Exception) {
            // Ignore logging serialization failure
        }
    }

    private fun saveApplications(list: List<RegisteredApplication>) {
        try {
            val type = Types.newParameterizedType(List::class.java, RegisteredApplication::class.java)
            prefs.edit().putString(KEY_APPLICATIONS, moshi.adapter<List<RegisteredApplication>>(type).toJson(list)).apply()
        } catch (e: Exception) {}
    }

    private fun saveCustomers(list: List<CustomerAccount>) {
        try {
            val type = Types.newParameterizedType(List::class.java, CustomerAccount::class.java)
            prefs.edit().putString(KEY_CUSTOMERS, moshi.adapter<List<CustomerAccount>>(type).toJson(list)).apply()
        } catch (e: Exception) {}
    }

    private fun saveInstallations(list: List<DeviceInstallation>) {
        try {
            val type = Types.newParameterizedType(List::class.java, DeviceInstallation::class.java)
            prefs.edit().putString(KEY_INSTALLATIONS, moshi.adapter<List<DeviceInstallation>>(type).toJson(list)).apply()
        } catch (e: Exception) {}
    }

    private fun saveLicenses(list: List<UniversalLicense>) {
        try {
            val type = Types.newParameterizedType(List::class.java, UniversalLicense::class.java)
            prefs.edit().putString(KEY_LICENSES, moshi.adapter<List<UniversalLicense>>(type).toJson(list)).apply()
        } catch (e: Exception) {}
    }

    private fun formatDate(timestamp: Long): String {
        return if (timestamp <= 0L) "Lifetime" else {
            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
        }
    }

    companion object {
        const val APP_SENTRY_STORE_POS = "SENTRY-STORE-POS"

        private const val PREFS_NAME = "universal_developer_license_platform"
        private const val KEY_APPLICATIONS = "key_registered_applications"
        private const val KEY_CUSTOMERS = "key_customer_accounts"
        private const val KEY_INSTALLATIONS = "key_device_installations"
        private const val KEY_LICENSES = "key_universal_licenses"
        private const val KEY_AUDIT_LOGS = "key_license_audit_logs"

        @Volatile
        private var instance: UniversalLicensePlatformEngine? = null

        fun getInstance(context: Context): UniversalLicensePlatformEngine {
            return instance ?: synchronized(this) {
                instance ?: UniversalLicensePlatformEngine(context.applicationContext).also { instance = it }
            }
        }
    }
}
