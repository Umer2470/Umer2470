package com.example.data.api.security

import android.content.Context
import android.content.SharedPreferences
import com.example.data.api.client.DeveloperApiClient
import com.example.data.api.model.ApiResult
import com.example.data.api.model.InstallationActivateRequest
import com.example.util.SecurityUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppActivationManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val identityManager = SecureIdentityManager.getInstance(context)
    private val licenseCache = LicenseStateCache.getInstance(context)

    private val _activationStateFlow = MutableStateFlow(getActivationStatus())
    val activationStateFlow: StateFlow<String> = _activationStateFlow.asStateFlow()

    fun isActivated(): Boolean {
        val status = getActivationStatus()
        if (status != STATUS_ACTIVATED && status != STATUS_OFFLINE_ACTIVATED) {
            return false
        }
        val expiry = getExpiryTimestamp()
        if (expiry > 0L && System.currentTimeMillis() > expiry) {
            _activationStateFlow.value = STATUS_EXPIRED
            return false
        }
        return true
    }

    fun getActivationStatus(): String {
        val status = prefs.getString(KEY_ACTIVATION_STATUS, STATUS_FIRST_INSTALL_NOT_ACTIVATED)
            ?: STATUS_FIRST_INSTALL_NOT_ACTIVATED
        if (status == STATUS_ACTIVATED || status == STATUS_OFFLINE_ACTIVATED) {
            val expiry = getExpiryTimestamp()
            if (expiry > 0L && System.currentTimeMillis() > expiry) {
                return STATUS_EXPIRED
            }
        }
        return status
    }

    fun getActivationMessage(): String {
        return prefs.getString(KEY_ACTIVATION_MESSAGE, "") ?: ""
    }

    fun getActiveLicenseKey(): String {
        return prefs.getString(KEY_ACTIVE_LICENSE_KEY, "") ?: ""
    }

    fun getExpiryTimestamp(): Long {
        return prefs.getLong(KEY_EXPIRY_TIMESTAMP, 0L)
    }

    fun getPlanName(): String {
        return prefs.getString(KEY_PLAN_NAME, "Commercial Edition") ?: "Commercial Edition"
    }

    fun getDaysRemaining(): Long {
        val expiry = getExpiryTimestamp()
        if (expiry <= 0L) return -1L // Lifetime
        val diff = expiry - System.currentTimeMillis()
        return if (diff > 0) (diff / (1000L * 60 * 60 * 24)).coerceAtLeast(0L) else 0L
    }

    suspend fun activateWithCode(
        activationCode: String,
        onResult: (status: String, message: String, isSuccess: Boolean) -> Unit
    ) {
        val trimmedCode = activationCode.trim().uppercase()
        val installationId = identityManager.getInstallationId()

        // 1. Check Universal Developer License Platform Engine First
        val platformEngine = com.example.data.api.platform.UniversalLicensePlatformEngine.getInstance(context)
        val platformResult = platformEngine.verifyLicenseCode(
            currentAppId = com.example.data.api.platform.UniversalLicensePlatformEngine.APP_SENTRY_STORE_POS,
            currentInstallationId = installationId,
            licenseCode = trimmedCode
        )

        when (platformResult) {
            is com.example.data.api.platform.LicenseVerificationResult.Valid -> {
                val lic = platformResult.license
                val token = SecurityUtils.generateDeterministicToken(installationId)
                saveActivationSuccess(
                    status = STATUS_ACTIVATED,
                    message = platformResult.message,
                    token = token,
                    code = trimmedCode,
                    expiryMs = lic.expiryDate,
                    planName = lic.planName
                )
                onResult(STATUS_ACTIVATED, platformResult.message, true)
                return
            }
            is com.example.data.api.platform.LicenseVerificationResult.AppMismatch -> {
                val msg = "Application Mismatch: Key generated for '${platformResult.actualApp}' cannot be used on SENTRY STORE POS."
                onResult(STATUS_INSTALLATION_MISMATCH, msg, false)
                return
            }
            is com.example.data.api.platform.LicenseVerificationResult.InstallationMismatch -> {
                val msg = "Installation Mismatch: Key bound to device '${platformResult.actualInstallation}', not '$installationId'."
                onResult(STATUS_INSTALLATION_MISMATCH, msg, false)
                return
            }
            is com.example.data.api.platform.LicenseVerificationResult.Expired -> {
                onResult(STATUS_EXPIRED, platformResult.message, false)
                return
            }
            is com.example.data.api.platform.LicenseVerificationResult.Suspended -> {
                onResult(STATUS_REVOKED, platformResult.message, false)
                return
            }
            is com.example.data.api.platform.LicenseVerificationResult.Invalid -> {
                // Continue to try legacy verification and online API
            }
        }

        // 2. Check Legacy Offline Cryptographic Verification
        val planInfo = parseAndVerifyCode(installationId, trimmedCode)
        if (planInfo != null) {
            val token = SecurityUtils.generateDeterministicToken(installationId)
            val expiryMs = if (planInfo.days > 0) {
                System.currentTimeMillis() + (planInfo.days.toLong() * 24 * 60 * 60 * 1000L)
            } else {
                0L // Lifetime
            }
            saveActivationSuccess(
                status = STATUS_ACTIVATED,
                message = "Application activated (${planInfo.planName}).",
                token = token,
                code = trimmedCode,
                expiryMs = expiryMs,
                planName = planInfo.planName
            )
            onResult(STATUS_ACTIVATED, "Application activated successfully under ${planInfo.planName}.", true)
            return
        }

        // 3. Try Online Activation via Developer Server
        val repo = com.example.data.api.repository.DeveloperApiRepository(context)
        when (val result = repo.activateInstallation(trimmedCode)) {
            is ApiResult.Success -> {
                val token = SecurityUtils.generateDeterministicToken(installationId)
                saveActivationSuccess(
                    status = STATUS_ACTIVATED,
                    message = result.data.getEffectiveMessage(),
                    token = token,
                    code = trimmedCode,
                    expiryMs = 0L,
                    planName = "Commercial Server License"
                )
                onResult(STATUS_ACTIVATED, result.data.getEffectiveMessage(), true)
            }
            is ApiResult.Error -> {
                val errorMsg = result.message
                val status = if (result.isNetworkError) STATUS_NETWORK_ERROR else STATUS_INVALID_CODE
                onResult(status, errorMsg, false)
            }
            is ApiResult.Offline -> {
                onResult(STATUS_NETWORK_ERROR, "No internet connection. Please verify internet or use valid offline activation code.", false)
            }
        }
    }

    fun extendLicense(days: Int) {
        if (days <= 0) return
        val currentExpiry = getExpiryTimestamp()
        val baseTime = if (currentExpiry > System.currentTimeMillis()) currentExpiry else System.currentTimeMillis()
        val newExpiry = baseTime + (days.toLong() * 24 * 60 * 60 * 1000L)
        prefs.edit()
            .putLong(KEY_EXPIRY_TIMESTAMP, newExpiry)
            .putString(KEY_ACTIVATION_STATUS, STATUS_ACTIVATED)
            .apply()
        _activationStateFlow.value = STATUS_ACTIVATED
    }

    fun renewLicense(days: Int, planName: String = "Renewed License") {
        val newExpiry = if (days > 0) System.currentTimeMillis() + (days.toLong() * 24 * 60 * 60 * 1000L) else 0L
        prefs.edit()
            .putLong(KEY_EXPIRY_TIMESTAMP, newExpiry)
            .putString(KEY_PLAN_NAME, planName)
            .putString(KEY_ACTIVATION_STATUS, STATUS_ACTIVATED)
            .apply()
        _activationStateFlow.value = STATUS_ACTIVATED
    }

    private fun mapServerError(code: Int, body: String): String {
        return when (code) {
            400 -> "Invalid activation code format (HTTP 400)."
            401 -> "Unauthorized: Activation key rejected."
            403 -> "Forbidden: License is expired or revoked."
            404 -> "Activation server endpoint not found."
            409 -> "Conflict: Code already activated on another device."
            else -> "Activation failed with server status HTTP $code."
        }
    }

    private fun saveActivationSuccess(
        status: String,
        message: String,
        token: String,
        code: String,
        expiryMs: Long = 0L,
        planName: String = "Commercial Edition"
    ) {
        prefs.edit()
            .putString(KEY_ACTIVATION_STATUS, status)
            .putString(KEY_ACTIVATION_MESSAGE, message)
            .putString(KEY_ACTIVATION_TOKEN, token)
            .putString(KEY_ACTIVE_LICENSE_KEY, code)
            .putLong(KEY_ACTIVATED_TIMESTAMP, System.currentTimeMillis())
            .putLong(KEY_EXPIRY_TIMESTAMP, expiryMs)
            .putString(KEY_PLAN_NAME, planName)
            .apply()
        licenseCache.updateCachedLicenseState(status = "active", message = message)
        _activationStateFlow.value = status
    }

    fun resetActivation() {
        prefs.edit()
            .putString(KEY_ACTIVATION_STATUS, STATUS_FIRST_INSTALL_NOT_ACTIVATED)
            .remove(KEY_ACTIVATION_MESSAGE)
            .remove(KEY_ACTIVATION_TOKEN)
            .remove(KEY_ACTIVE_LICENSE_KEY)
            .remove(KEY_ACTIVATED_TIMESTAMP)
            .remove(KEY_EXPIRY_TIMESTAMP)
            .remove(KEY_PLAN_NAME)
            .apply()
        licenseCache.clear()
        _activationStateFlow.value = STATUS_FIRST_INSTALL_NOT_ACTIVATED
    }

    data class VerifiedPlan(
        val days: Int,
        val planName: String
    )

    companion object {
        const val STATUS_FIRST_INSTALL_NOT_ACTIVATED = "FIRST_INSTALL_NOT_ACTIVATED"
        const val STATUS_ACTIVATED = "ACTIVATED"
        const val STATUS_OFFLINE_ACTIVATED = "OFFLINE_ACTIVATED"
        const val STATUS_INVALID_CODE = "INVALID_CODE"
        const val STATUS_EXPIRED = "EXPIRED"
        const val STATUS_REVOKED = "REVOKED"
        const val STATUS_INSTALLATION_MISMATCH = "INSTALLATION_MISMATCH"
        const val STATUS_ALREADY_USED = "ALREADY_USED"
        const val STATUS_NETWORK_ERROR = "NETWORK_ERROR"

        private const val PREFS_NAME = "sentry_store_app_activation"
        private const val KEY_ACTIVATION_STATUS = "key_activation_status"
        private const val KEY_ACTIVATION_MESSAGE = "key_activation_message"
        private const val KEY_ACTIVATION_TOKEN = "key_activation_token"
        private const val KEY_ACTIVE_LICENSE_KEY = "key_active_license_key"
        private const val KEY_ACTIVATED_TIMESTAMP = "key_activated_timestamp"
        private const val KEY_EXPIRY_TIMESTAMP = "key_expiry_timestamp"
        private const val KEY_PLAN_NAME = "key_plan_name"

        @Volatile
        private var instance: AppActivationManager? = null

        fun getInstance(context: Context): AppActivationManager {
            return instance ?: synchronized(this) {
                instance ?: AppActivationManager(context.applicationContext).also { instance = it }
            }
        }

        fun generateActivationCode(installationId: String): String {
            return generatePlanActivationCode(installationId, 0)
        }

        fun generatePlanActivationCode(installationId: String, planDays: Int): String {
            val prefix = when (planDays) {
                30 -> "ACTV-M1"
                90 -> "ACTV-M3"
                180 -> "ACTV-M6"
                365 -> "ACTV-Y1"
                in 1..36499 -> "ACTV-D$planDays"
                else -> "ACTV-LIFE"
            }
            val salt = if (planDays > 0) "CH_UMER_PLAN_${planDays}_2026" else "CH_UMER_SECRET_KEY_2026"
            val hash = SecurityUtils.sha256("$installationId:$salt")
            val p1 = hash.substring(0, 4)
            val p2 = hash.substring(4, 8)
            val p3 = hash.substring(8, 12)
            return "$prefix-$p1-$p2-$p3"
        }

        fun parseAndVerifyCode(installationId: String, code: String): VerifiedPlan? {
            val upper = code.trim().uppercase()
            // Check legacy lifetime ACTV-XXXX-XXXX-XXXX-XXXX
            val legacyLifetime = run {
                val hash = SecurityUtils.sha256("$installationId:CH_UMER_SECRET_KEY_2026")
                val p1 = hash.substring(0, 4)
                val p2 = hash.substring(4, 8)
                val p3 = hash.substring(8, 12)
                val p4 = hash.substring(12, 16)
                "ACTV-$p1-$p2-$p3-$p4"
            }
            if (upper.equals(legacyLifetime, ignoreCase = true)) {
                return VerifiedPlan(days = 0, planName = "Lifetime Commercial License")
            }

            // Check standard plan codes
            val plans = listOf(
                30 to "Demo 1 Month",
                90 to "3 Months Standard",
                180 to "6 Months Professional",
                365 to "1 Year Enterprise",
                0 to "Lifetime Commercial"
            )
            for ((days, name) in plans) {
                val expected = generatePlanActivationCode(installationId, days)
                if (upper.equals(expected, ignoreCase = true)) {
                    return VerifiedPlan(days = days, planName = name)
                }
            }

            // Check custom days pattern ACTV-D{days}-...
            if (upper.startsWith("ACTV-D")) {
                val parts = upper.split("-")
                if (parts.size >= 4) {
                    val daysStr = parts[1].removePrefix("D")
                    val days = daysStr.toIntOrNull()
                    if (days != null && days > 0) {
                        val expected = generatePlanActivationCode(installationId, days)
                        if (upper.equals(expected, ignoreCase = true)) {
                            return VerifiedPlan(days = days, planName = "$days Days Custom Plan")
                        }
                    }
                }
            }
            return null
        }

        fun verifyOfflineCryptographicCode(installationId: String, code: String): Boolean {
            return parseAndVerifyCode(installationId, code) != null
        }

        fun verifyActivationCodeLocally(installationId: String, code: String): Boolean {
            return verifyOfflineCryptographicCode(installationId, code)
        }
    }
}
