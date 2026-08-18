package com.example.data.api.security

import android.content.Context
import android.content.SharedPreferences
import com.example.data.api.model.ApiResult
import com.example.data.api.repository.DeveloperApiRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages mandatory First-Time Installation Activation, Hardware/Installation ID binding,
 * and Authoritative Developer Server verification.
 *
 * Activation Lifecycle States:
 * - FIRST_INSTALL_NOT_ACTIVATED: Fresh installation, POS locked until activation.
 * - ACTIVATION_PENDING: Activation code submitted, waiting for verification.
 * - ACTIVATED: Authorized and permanently active according to license policy.
 * - SUSPENDED: Temporarily suspended by developer.
 * - REVOKED: Permanently revoked by developer.
 */
class AppActivationManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val identityManager = SecureIdentityManager.getInstance(context)
    private val tokenManager = SecureTokenManager.getInstance(context)
    private val devRepository = DeveloperApiRepository(context)

    private val _activationStateFlow = MutableStateFlow(getActivationStatus())
    val activationStateFlow: StateFlow<String> = _activationStateFlow.asStateFlow()

    companion object {
        private const val PREFS_NAME = "ch_umer_secure_activation_prefs"
        private const val KEY_ACTIVATION_STATUS = "key_secure_activation_status"
        private const val KEY_ACTIVATED_AT = "key_activated_at_timestamp"
        private const val KEY_ACTIVATED_BY = "key_activated_by"
        private const val KEY_BOUND_INSTALLATION_ID = "key_bound_installation_id"
        private const val KEY_ACTIVATION_CODE_HASH = "key_activation_code_hash"
        private const val KEY_LICENSE_ID = "key_activated_license_id"
        private const val KEY_ACTIVATION_SIGNATURE = "key_activation_signature"

        const val STATUS_FIRST_INSTALL_NOT_ACTIVATED = "FIRST_INSTALL_NOT_ACTIVATED"
        const val STATUS_ACTIVATION_PENDING = "ACTIVATION_PENDING"
        const val STATUS_ACTIVATED = "ACTIVATED"
        const val STATUS_SUSPENDED = "SUSPENDED"
        const val STATUS_REVOKED = "REVOKED"

        @Volatile
        private var INSTANCE: AppActivationManager? = null

        fun getInstance(context: Context): AppActivationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppActivationManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        /**
         * Computes SHA-256 hash.
         */
        fun hashSha256(input: String): String {
            return try {
                val md = MessageDigest.getInstance("SHA-256")
                val bytes = md.digest(input.toByteArray(Charsets.UTF_8))
                bytes.joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                input.hashCode().toString()
            }
        }

        /**
         * Generates a cryptographically bound Developer Activation Code for a given installation ID.
         * Format: ACTV-XXXX-XXXX-XXXX-XXXX
         */
        fun generateActivationCode(
            installationId: String,
            customerId: String = "CUST-DEFAULT",
            planType: String = "COMMERCIAL",
            salt: String = "CH_UMER_DEV_2026"
        ): String {
            val raw = "${installationId.trim().uppercase()}|$customerId|$planType|$salt"
            val hash = hashSha256(raw).uppercase()
            val chunk1 = hash.substring(0, 4)
            val chunk2 = hash.substring(4, 8)
            val chunk3 = hash.substring(8, 12)
            val chunk4 = hash.substring(12, 16)
            return "ACTV-$chunk1-$chunk2-$chunk3-$chunk4"
        }
    }

    /**
     * Checks whether the current installation is fully activated and bound.
     */
    fun isActivated(): Boolean {
        val status = getActivationStatus()
        if (status != STATUS_ACTIVATED) return false

        // Verify that the bound installation ID matches current device installation ID
        val boundId = prefs.getString(KEY_BOUND_INSTALLATION_ID, "")
        val currentId = identityManager.getInstallationId()
        if (boundId.isNullOrBlank() || boundId != currentId) {
            return false
        }

        // Verify activation signature integrity
        val savedHash = prefs.getString(KEY_ACTIVATION_CODE_HASH, "")
        val expectedSig = hashSha256("$boundId|$status|$savedHash|CH_UMER_STORE_POS")
        val savedSig = prefs.getString(KEY_ACTIVATION_SIGNATURE, "")

        return savedSig == expectedSig
    }

    /**
     * Gets current activation status string.
     */
    fun getActivationStatus(): String {
        return prefs.getString(KEY_ACTIVATION_STATUS, STATUS_FIRST_INSTALL_NOT_ACTIVATED) ?: STATUS_FIRST_INSTALL_NOT_ACTIVATED
    }

    /**
     * Gets timestamp of activation.
     */
    fun getActivatedAt(): Long {
        return prefs.getLong(KEY_ACTIVATED_AT, 0L)
    }

    /**
     * Gets formatted activation date string.
     */
    fun getActivatedAtFormatted(): String {
        val ts = getActivatedAt()
        if (ts <= 0L) return "Not Activated"
        return SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(ts))
    }

    /**
     * Gets the party or mechanism that authorized the activation.
     */
    fun getActivatedBy(): String {
        return prefs.getString(KEY_ACTIVATED_BY, "Developer Authorization") ?: "Developer Authorization"
    }

    /**
     * Gets the bound Installation ID.
     */
    fun getInstallationId(): String {
        return identityManager.getInstallationId()
    }

    /**
     * Verifies and activates application using developer/server provided Activation Code.
     * Server is the primary authority. Cryptographic offline verification is supported as fallback
     * for codes legitimately generated by Developer Control Center.
     */
    suspend fun activateWithCode(
        activationCode: String,
        customerId: String = identityManager.getCustomerId(),
        onResult: (status: String, message: String, isSuccess: Boolean) -> Unit
    ) {
        val cleanCode = activationCode.trim().uppercase()
        if (cleanCode.isBlank()) {
            onResult(STATUS_FIRST_INSTALL_NOT_ACTIVATED, "Activation code cannot be empty.", false)
            return
        }

        val currentInstallationId = identityManager.getInstallationId()
        val currentAppVersion = identityManager.getAppVersion()

        withContext(Dispatchers.IO) {
            // Step 1: Query Developer API Server
            val apiResult = devRepository.activateInstallation(cleanCode)

            when (apiResult) {
                is ApiResult.Success -> {
                    val resp = apiResult.data
                    val serverStatus = resp.status.uppercase()
                    if (serverStatus == "ACTIVE" || serverStatus == "ACTIVATED") {
                        persistActivationSuccess(
                            installationId = currentInstallationId,
                            activationCode = cleanCode,
                            activatedBy = "Developer Server (Online Authoritative)",
                            licenseId = resp.licenseId ?: "LIC-${System.currentTimeMillis()}"
                        )
                        withContext(Dispatchers.Main) {
                            _activationStateFlow.value = STATUS_ACTIVATED
                            onResult(STATUS_ACTIVATED, resp.message.ifBlank { "Activation Successful! Application is authorized." }, true)
                        }
                    } else {
                        val message = when (serverStatus) {
                            "ALREADY_USED" -> "Code Already Used: This activation code has already been claimed."
                            "INSTALLATION_MISMATCH" -> "Installation Not Authorized: Code is bound to a different installation ID."
                            "EXPIRED" -> "Expired: This activation code has expired."
                            "REVOKED" -> "Revoked: This activation code was revoked by the developer."
                            else -> "Invalid Activation Code: Verification failed on developer server."
                        }
                        withContext(Dispatchers.Main) {
                            onResult(serverStatus, message, false)
                        }
                    }
                }
                is ApiResult.Offline, is ApiResult.Error -> {
                    // Fallback to Developer Cryptographic Binding Verification if server is unreachable
                    val expectedDevCode = generateActivationCode(currentInstallationId, customerId)
                    val expectedUniversalCode = generateActivationCode(currentInstallationId, "CUST-DEFAULT")

                    if (cleanCode == expectedDevCode || cleanCode == expectedUniversalCode || verifyCryptographicDeveloperKey(cleanCode, currentInstallationId)) {
                        persistActivationSuccess(
                            installationId = currentInstallationId,
                            activationCode = cleanCode,
                            activatedBy = "Developer Cryptographic Key (Offline Authorized)",
                            licenseId = "LIC-OFFLINE-${currentInstallationId.takeLast(6)}"
                        )
                        withContext(Dispatchers.Main) {
                            _activationStateFlow.value = STATUS_ACTIVATED
                            onResult(STATUS_ACTIVATED, "Activation Successful! Application authorized.", true)
                        }
                    } else {
                        val errorDetail = if (apiResult is ApiResult.Error) "Server error: ${apiResult.message}" else "Server Unavailable. Invalid activation code."
                        withContext(Dispatchers.Main) {
                            onResult("INVALID", "Invalid Activation Code: $errorDetail", false)
                        }
                    }
                }
            }
        }
    }

    /**
     * Verifies if a code matches developer crypto signature for this installation.
     */
    private fun verifyCryptographicDeveloperKey(code: String, installationId: String): Boolean {
        if (!code.startsWith("ACTV-")) return false
        val clean = code.removePrefix("ACTV-").replace("-", "").uppercase()
        if (clean.length < 12) return false

        // Check if hash of installation + salt produces this signature chunk
        val testHash = hashSha256("${installationId.trim().uppercase()}|CH_UMER_DEV_2026").uppercase()
        return clean.startsWith(testHash.substring(0, 8))
    }

    /**
     * Persists authorized activation state securely.
     */
    private fun persistActivationSuccess(
        installationId: String,
        activationCode: String,
        activatedBy: String,
        licenseId: String
    ) {
        val codeHash = hashSha256(activationCode)
        val now = System.currentTimeMillis()
        val signature = hashSha256("$installationId|$STATUS_ACTIVATED|$codeHash|CH_UMER_STORE_POS")

        prefs.edit()
            .putString(KEY_ACTIVATION_STATUS, STATUS_ACTIVATED)
            .putLong(KEY_ACTIVATED_AT, now)
            .putString(KEY_ACTIVATED_BY, activatedBy)
            .putString(KEY_BOUND_INSTALLATION_ID, installationId)
            .putString(KEY_ACTIVATION_CODE_HASH, codeHash)
            .putString(KEY_LICENSE_ID, licenseId)
            .putString(KEY_ACTIVATION_SIGNATURE, signature)
            .apply()

        // Also store token in KeyStore Token Manager
        tokenManager.saveTokens(
            accessToken = "TOKEN-${installationId.takeLast(8)}-$now",
            refreshToken = "",
            expiresInSeconds = 315360000L
        )
    }

    /**
     * Suspends installation (Developer Control action).
     */
    fun suspendInstallation(reason: String = "Suspended by Developer") {
        prefs.edit()
            .putString(KEY_ACTIVATION_STATUS, STATUS_SUSPENDED)
            .apply()
        _activationStateFlow.value = STATUS_SUSPENDED
    }

    /**
     * Revokes installation (Developer Control action).
     */
    fun revokeInstallation(reason: String = "Revoked by Developer") {
        prefs.edit()
            .putString(KEY_ACTIVATION_STATUS, STATUS_REVOKED)
            .apply()
        _activationStateFlow.value = STATUS_REVOKED
    }

    /**
     * Resets activation state back to unactivated (Requires Super Admin authorization).
     */
    fun resetActivation() {
        prefs.edit()
            .putString(KEY_ACTIVATION_STATUS, STATUS_FIRST_INSTALL_NOT_ACTIVATED)
            .remove(KEY_ACTIVATED_AT)
            .remove(KEY_ACTIVATED_BY)
            .remove(KEY_ACTIVATION_CODE_HASH)
            .remove(KEY_ACTIVATION_SIGNATURE)
            .apply()
        _activationStateFlow.value = STATUS_FIRST_INSTALL_NOT_ACTIVATED
    }
}
