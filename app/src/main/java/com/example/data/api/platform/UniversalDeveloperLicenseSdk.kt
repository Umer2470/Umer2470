package com.example.data.api.platform

import android.content.Context
import android.content.SharedPreferences
import com.example.util.SecurityUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Universal Developer License SDK
 * 
 * Reusable client SDK designed to be integrated into any client Android application.
 * Provides:
 * - Application ID Isolation
 * - Installation ID Generation & Device Binding
 * - Local Cryptographic Signature Validation
 * - Offline Cache Safety with Grace Period
 * - Zero Database Destruction Guarantee on Network/License Lapses
 * - Expiry Warning State Assessment
 */
class UniversalDeveloperLicenseSdk private constructor(
    private val context: Context,
    val applicationId: String = DEFAULT_APP_ID
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("${PREFS_PREFIX}_${applicationId.lowercase().replace("-", "_")}", Context.MODE_PRIVATE)

    private val platformEngine = UniversalLicensePlatformEngine.getInstance(context)

    private val _licenseStateFlow = MutableStateFlow(getCurrentState())
    val licenseStateFlow: StateFlow<SdkLicenseState> = _licenseStateFlow.asStateFlow()

    data class SdkLicenseState(
        val isActivated: Boolean,
        val status: String,
        val planName: String,
        val licenseKey: String,
        val expiryTimestamp: Long,
        val daysRemaining: Long,
        val warningTier: ExpiryWarningTier?,
        val message: String
    )

    fun getInstallationId(): String {
        return prefs.getString(KEY_INSTALLATION_ID, null) ?: run {
            val generated = "APP-" + java.util.UUID.randomUUID().toString().replace("-", "").take(12).uppercase()
            prefs.edit().putString(KEY_INSTALLATION_ID, generated).apply()
            generated
        }
    }

    fun isActivated(): Boolean {
        val state = getCurrentState()
        return state.isActivated
    }

    fun getCurrentState(): SdkLicenseState {
        val status = prefs.getString(KEY_STATUS, STATUS_UNACTIVATED) ?: STATUS_UNACTIVATED
        val expiry = prefs.getLong(KEY_EXPIRY, 0L)
        val planName = prefs.getString(KEY_PLAN_NAME, "Standard Commercial Edition") ?: "Standard Commercial Edition"
        val key = prefs.getString(KEY_ACTIVE_KEY, "") ?: ""
        val message = prefs.getString(KEY_MESSAGE, "") ?: ""

        val isExpired = expiry > 0L && System.currentTimeMillis() > expiry
        val effectiveStatus = if (isExpired && (status == STATUS_ACTIVATED || status == STATUS_OFFLINE_ACTIVATED)) {
            STATUS_EXPIRED
        } else {
            status
        }

        val isActivated = (effectiveStatus == STATUS_ACTIVATED || effectiveStatus == STATUS_OFFLINE_ACTIVATED) && !isExpired
        val daysLeft = if (expiry <= 0L) -1L else {
            val diff = expiry - System.currentTimeMillis()
            if (diff > 0) diff / (1000L * 60 * 60 * 24) else 0L
        }
        val warning = platformEngine.checkExpiryWarning(expiry)

        return SdkLicenseState(
            isActivated = isActivated,
            status = effectiveStatus,
            planName = planName,
            licenseKey = key,
            expiryTimestamp = expiry,
            daysRemaining = daysLeft,
            warningTier = warning,
            message = message
        )
    }

    /**
     * Activates the application with a license code.
     * Enforces Application ID isolation, cryptographic signature, and installation matching.
     */
    fun activateWithCode(
        activationCode: String,
        onResult: (isSuccess: Boolean, status: String, message: String) -> Unit
    ) {
        val cleanCode = activationCode.trim().uppercase()
        val installationId = getInstallationId()

        val verification = platformEngine.verifyLicenseCode(
            currentAppId = applicationId,
            currentInstallationId = installationId,
            licenseCode = cleanCode
        )

        when (verification) {
            is LicenseVerificationResult.Valid -> {
                val lic = verification.license
                val token = SecurityUtils.generateDeterministicToken("$applicationId:$installationId")
                prefs.edit()
                    .putString(KEY_STATUS, STATUS_ACTIVATED)
                    .putString(KEY_ACTIVE_KEY, cleanCode)
                    .putString(KEY_PLAN_NAME, lic.planName)
                    .putLong(KEY_EXPIRY, lic.expiryDate)
                    .putString(KEY_TOKEN, token)
                    .putString(KEY_MESSAGE, verification.message)
                    .putLong(KEY_ACTIVATED_AT, System.currentTimeMillis())
                    .apply()

                val newState = getCurrentState()
                _licenseStateFlow.value = newState
                onResult(true, STATUS_ACTIVATED, verification.message)
            }
            is LicenseVerificationResult.AppMismatch -> {
                val errorMsg = "Application Mismatch: Key generated for '${verification.actualApp}' cannot be used on '$applicationId'."
                onResult(false, STATUS_APP_MISMATCH, errorMsg)
            }
            is LicenseVerificationResult.InstallationMismatch -> {
                val errorMsg = "Installation Mismatch: Key bound to device '${verification.actualInstallation}', not '$installationId'."
                onResult(false, STATUS_INSTALLATION_MISMATCH, errorMsg)
            }
            is LicenseVerificationResult.Expired -> {
                onResult(false, STATUS_EXPIRED, verification.message)
            }
            is LicenseVerificationResult.Suspended -> {
                onResult(false, STATUS_SUSPENDED, verification.message)
            }
            is LicenseVerificationResult.Invalid -> {
                onResult(false, STATUS_INVALID, verification.reason)
            }
        }
    }

    fun extendLicense(days: Int) {
        if (days <= 0) return
        val currentExpiry = prefs.getLong(KEY_EXPIRY, 0L)
        val base = if (currentExpiry > System.currentTimeMillis()) currentExpiry else System.currentTimeMillis()
        val newExpiry = base + (days.toLong() * 24 * 60 * 60 * 1000L)
        prefs.edit()
            .putLong(KEY_EXPIRY, newExpiry)
            .putString(KEY_STATUS, STATUS_ACTIVATED)
            .apply()
        _licenseStateFlow.value = getCurrentState()
    }

    fun renewLicense(days: Int, planName: String) {
        val newExpiry = if (days > 0) System.currentTimeMillis() + (days.toLong() * 24 * 60 * 60 * 1000L) else 0L
        prefs.edit()
            .putLong(KEY_EXPIRY, newExpiry)
            .putString(KEY_PLAN_NAME, planName)
            .putString(KEY_STATUS, STATUS_ACTIVATED)
            .apply()
        _licenseStateFlow.value = getCurrentState()
    }

    fun resetActivation() {
        prefs.edit()
            .putString(KEY_STATUS, STATUS_UNACTIVATED)
            .remove(KEY_ACTIVE_KEY)
            .remove(KEY_PLAN_NAME)
            .remove(KEY_EXPIRY)
            .remove(KEY_TOKEN)
            .remove(KEY_MESSAGE)
            .apply()
        _licenseStateFlow.value = getCurrentState()
    }

    companion object {
        const val DEFAULT_APP_ID = "SENTRY-STORE-POS"

        const val STATUS_UNACTIVATED = "FIRST_INSTALL_NOT_ACTIVATED"
        const val STATUS_ACTIVATED = "ACTIVATED"
        const val STATUS_OFFLINE_ACTIVATED = "OFFLINE_ACTIVATED"
        const val STATUS_EXPIRED = "EXPIRED"
        const val STATUS_SUSPENDED = "SUSPENDED"
        const val STATUS_INVALID = "INVALID_CODE"
        const val STATUS_APP_MISMATCH = "APPLICATION_MISMATCH"
        const val STATUS_INSTALLATION_MISMATCH = "INSTALLATION_MISMATCH"

        private const val PREFS_PREFIX = "universal_sdk_license"
        private const val KEY_INSTALLATION_ID = "key_sdk_installation_id"
        private const val KEY_STATUS = "key_sdk_status"
        private const val KEY_ACTIVE_KEY = "key_sdk_active_key"
        private const val KEY_PLAN_NAME = "key_sdk_plan_name"
        private const val KEY_EXPIRY = "key_sdk_expiry"
        private const val KEY_TOKEN = "key_sdk_token"
        private const val KEY_MESSAGE = "key_sdk_message"
        private const val KEY_ACTIVATED_AT = "key_sdk_activated_at"

        @Volatile
        private var instanceMap = mutableMapOf<String, UniversalDeveloperLicenseSdk>()

        fun getInstance(context: Context, applicationId: String = DEFAULT_APP_ID): UniversalDeveloperLicenseSdk {
            val key = applicationId.trim().uppercase()
            return instanceMap[key] ?: synchronized(this) {
                instanceMap[key] ?: UniversalDeveloperLicenseSdk(context.applicationContext, key).also {
                    instanceMap[key] = it
                }
            }
        }
    }
}
