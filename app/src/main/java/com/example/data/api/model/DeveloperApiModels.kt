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
    @Json(name = "installation_id") val installationId: String,
    @Json(name = "activation_code") val activationCode: String,
    @Json(name = "customer_id") val customerId: String? = null,
    @Json(name = "store_id") val storeId: String? = null,
    @Json(name = "app_version") val appVersion: String? = "1.0",
    @Json(name = "device_fingerprint") val deviceFingerprint: String? = null
)

@JsonClass(generateAdapter = true)
data class InstallationActivateResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "error_message") val errorMessage: String? = null,
    @Json(name = "activation_token") val activationToken: String? = null,
    @Json(name = "token") val token: String? = null,
    @Json(name = "plan_type") val planType: String? = null,
    @Json(name = "plan") val plan: String? = null,
    @Json(name = "customer_id") val customerId: String? = null,
    @Json(name = "store_id") val storeId: String? = null,
    @Json(name = "max_shops") val maxShops: Int? = null,
    @Json(name = "max_users") val maxUsers: Int? = null,
    @Json(name = "expires_at") val expiresAt: Long? = null
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
    @Json(name = "installation_id") val installationId: String,
    @Json(name = "app_version") val appVersion: String? = "1.0",
    @Json(name = "device_fingerprint") val deviceFingerprint: String? = null
)

@JsonClass(generateAdapter = true)
data class RegisterInstallationResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class LicenseValidateRequest(
    @Json(name = "installation_id") val installationId: String,
    @Json(name = "license_key") val licenseKey: String,
    @Json(name = "app_version") val appVersion: String? = "1.0"
)

@JsonClass(generateAdapter = true)
data class LicenseValidateResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "plan_type") val planType: String? = null,
    @Json(name = "plan") val plan: String? = null,
    @Json(name = "max_shops") val maxShops: Int? = null,
    @Json(name = "max_users") val maxUsers: Int? = null,
    @Json(name = "expires_at") val expiresAt: Long? = null
)

@JsonClass(generateAdapter = true)
data class LicenseHeartbeatRequest(
    @Json(name = "installation_id") val installationId: String,
    @Json(name = "customer_id") val customerId: String? = null,
    @Json(name = "store_id") val storeId: String? = null
)

@JsonClass(generateAdapter = true)
data class LicenseHeartbeatResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class HealthCheckResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "server_time") val serverTime: Long? = null
)

@JsonClass(generateAdapter = true)
data class ServerConfigResponse(
    @Json(name = "status") val status: String? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "error") val error: String? = null,
    @Json(name = "supported_versions") val supportedVersions: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class AppVersionCheckRequest(
    @Json(name = "app_version") val appVersion: String,
    @Json(name = "installation_id") val installationId: String
)

@JsonClass(generateAdapter = true)
data class AppVersionCheckResponse(
    @Json(name = "latest_version") val latestVersion: String? = null,
    @Json(name = "update_required") val updateRequired: Boolean? = false,
    @Json(name = "message") val message: String? = null
)
