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
        return status == STATUS_ACTIVATED || status == STATUS_OFFLINE_ACTIVATED
    }

    fun getActivationStatus(): String {
        return prefs.getString(KEY_ACTIVATION_STATUS, STATUS_FIRST_INSTALL_NOT_ACTIVATED)
            ?: STATUS_FIRST_INSTALL_NOT_ACTIVATED
    }

    fun getActivationMessage(): String {
        return prefs.getString(KEY_ACTIVATION_MESSAGE, "") ?: ""
    }

    fun getActiveLicenseKey(): String {
        return prefs.getString(KEY_ACTIVE_LICENSE_KEY, "") ?: ""
    }

    suspend fun activateWithCode(
        activationCode: String,
        onResult: (status: String, message: String, isSuccess: Boolean) -> Unit
    ) {
        val trimmedCode = activationCode.trim().uppercase()
        val installationId = identityManager.getInstallationId()

        // 1. Check Offline Cryptographic Verification First
        if (verifyOfflineCryptographicCode(installationId, trimmedCode)) {
            val token = SecurityUtils.generateDeterministicToken(installationId)
            saveActivationSuccess(
                status = STATUS_ACTIVATED,
                message = "Application successfully activated.",
                token = token,
                code = trimmedCode
            )
            onResult(STATUS_ACTIVATED, "Application activated successfully.", true)
            return
        }

        // 2. Try Online Activation via Developer Server
        val repo = com.example.data.api.repository.DeveloperApiRepository(context)
        when (val result = repo.activateInstallation(trimmedCode)) {
            is ApiResult.Success -> {
                val token = SecurityUtils.generateDeterministicToken(installationId)
                saveActivationSuccess(
                    status = STATUS_ACTIVATED,
                    message = result.data.getEffectiveMessage(),
                    token = token,
                    code = trimmedCode
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

    private fun saveActivationSuccess(status: String, message: String, token: String, code: String) {
        prefs.edit()
            .putString(KEY_ACTIVATION_STATUS, status)
            .putString(KEY_ACTIVATION_MESSAGE, message)
            .putString(KEY_ACTIVATION_TOKEN, token)
            .putString(KEY_ACTIVE_LICENSE_KEY, code)
            .putLong(KEY_ACTIVATED_TIMESTAMP, System.currentTimeMillis())
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
            .apply()
        licenseCache.clear()
        _activationStateFlow.value = STATUS_FIRST_INSTALL_NOT_ACTIVATED
    }

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

        private const val PREFS_NAME = "ch_umer_app_activation"
        private const val KEY_ACTIVATION_STATUS = "key_activation_status"
        private const val KEY_ACTIVATION_MESSAGE = "key_activation_message"
        private const val KEY_ACTIVATION_TOKEN = "key_activation_token"
        private const val KEY_ACTIVE_LICENSE_KEY = "key_active_license_key"
        private const val KEY_ACTIVATED_TIMESTAMP = "key_activated_timestamp"

        @Volatile
        private var instance: AppActivationManager? = null

        fun getInstance(context: Context): AppActivationManager {
            return instance ?: synchronized(this) {
                instance ?: AppActivationManager(context.applicationContext).also { instance = it }
            }
        }

        fun generateActivationCode(installationId: String): String {
            val hash = SecurityUtils.sha256("$installationId:CH_UMER_SECRET_KEY_2026")
            val p1 = hash.substring(0, 4)
            val p2 = hash.substring(4, 8)
            val p3 = hash.substring(8, 12)
            val p4 = hash.substring(12, 16)
            return "ACTV-$p1-$p2-$p3-$p4"
        }

        fun verifyOfflineCryptographicCode(installationId: String, code: String): Boolean {
            val expectedCode = generateActivationCode(installationId)
            return code.equals(expectedCode, ignoreCase = true)
        }

        fun verifyActivationCodeLocally(installationId: String, code: String): Boolean {
            return verifyOfflineCryptographicCode(installationId, code)
        }
    }
}
