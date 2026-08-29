package com.example.data.api.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(
        val code: Int,
        val message: String,
        val isNetworkError: Boolean = false
    ) : ApiResult<Nothing>()
    object Offline : ApiResult<Nothing>()
}

class OfflineNetworkException(message: String = "Device is currently offline") : java.io.IOException(message)

@JsonClass(generateAdapter = true)
data class InstallationActivateRequest(
    @field:Json(name = "installation_id") val installationId: String,
    @field:Json(name = "activation_code") val activationCode: String,
    @field:Json(name = "customer_id") val customerId: String? = null,
    @field:Json(name = "store_id") val storeId: String? = null,
    @field:Json(name = "app_version") val appVersion: String? = "1.0",
    @field:Json(name = "device_fingerprint") val deviceFingerprint: String? = null
)

@JsonClass(generateAdapter = true)
data class InstallationActivateResponse(
    @field:Json(name = "status") val status: String? = null,
    @field:Json(name = "success") val success: Boolean? = null,
    @field:Json(name = "message") val message: String? = null,
    @field:Json(name = "error") val error: String? = null,
    @field:Json(name = "error_message") val errorMessage: String? = null,
    @field:Json(name = "activation_token") val activationToken: String? = null,
    @field:Json(name = "token") val token: String? = null,
    @field:Json(name = "plan_type") val planType: String? = null,
    @field:Json(name = "plan") val plan: String? = null,
    @field:Json(name = "customer_id") val customerId: String? = null,
    @field:Json(name = "store_id") val storeId: String? = null,
    @field:Json(name = "max_shops") val maxShops: Int? = null,
    @field:Json(name = "max_users") val maxUsers: Int? = null,
    @field:Json(name = "expires_at") val expiresAt: Long? = null
) {
    fun isActivationSuccessful(): Boolean {
        if (success == true) return true
        val effectiveStatus = (status ?: "").trim().lowercase()
        return effectiveStatus in listOf("active", "activated", "success", "ok", "valid")
    }

    fun getEffectiveStatus(): String {
        return status ?: if (success == true) "active" else "failed"
    }

    fun getEffectiveMessage(): String {
        return message ?: errorMessage ?: error ?: if (isActivationSuccessful()) "Activation successful" else "Activation failed"
    }
}

@JsonClass(generateAdapter = true)
data class RegisterInstallationRequest(
    @field:Json(name = "installation_id") val installationId: String,
    @field:Json(name = "app_version") val appVersion: String? = "1.0",
    @field:Json(name = "device_fingerprint") val deviceFingerprint: String? = null
)

@JsonClass(generateAdapter = true)
data class RegisterInstallationResponse(
    @field:Json(name = "status") val status: String? = null,
    @field:Json(name = "message") val message: String? = null,
    @field:Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class LicenseValidateRequest(
    @field:Json(name = "installation_id") val installationId: String,
    @field:Json(name = "license_key") val licenseKey: String,
    @field:Json(name = "app_version") val appVersion: String? = "1.0"
)

@JsonClass(generateAdapter = true)
data class LicenseValidateResponse(
    @field:Json(name = "status") val status: String? = null,
    @field:Json(name = "message") val message: String? = null,
    @field:Json(name = "error") val error: String? = null,
    @field:Json(name = "plan_type") val planType: String? = null,
    @field:Json(name = "plan") val plan: String? = null,
    @field:Json(name = "max_shops") val maxShops: Int? = null,
    @field:Json(name = "max_users") val maxUsers: Int? = null,
    @field:Json(name = "expires_at") val expiresAt: Long? = null
)

@JsonClass(generateAdapter = true)
data class LicenseHeartbeatRequest(
    @field:Json(name = "installation_id") val installationId: String,
    @field:Json(name = "customer_id") val customerId: String? = null,
    @field:Json(name = "store_id") val storeId: String? = null
)

@JsonClass(generateAdapter = true)
data class LicenseHeartbeatResponse(
    @field:Json(name = "status") val status: String? = null,
    @field:Json(name = "message") val message: String? = null,
    @field:Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class HealthCheckResponse(
    @field:Json(name = "status") val status: String? = null,
    @field:Json(name = "message") val message: String? = null,
    @field:Json(name = "error") val error: String? = null,
    @field:Json(name = "server_time") val serverTime: Long? = null
)

@JsonClass(generateAdapter = true)
data class ServerConfigResponse(
    @field:Json(name = "status") val status: String? = null,
    @field:Json(name = "message") val message: String? = null,
    @field:Json(name = "error") val error: String? = null,
    @field:Json(name = "supported_versions") val supportedVersions: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class AppVersionCheckRequest(
    @field:Json(name = "app_version") val appVersion: String,
    @field:Json(name = "installation_id") val installationId: String
)

@JsonClass(generateAdapter = true)
data class AppVersionCheckResponse(
    @field:Json(name = "latest_version") val latestVersion: String? = null,
    @field:Json(name = "update_required") val updateRequired: Boolean? = false,
    @field:Json(name = "message") val message: String? = null
)
