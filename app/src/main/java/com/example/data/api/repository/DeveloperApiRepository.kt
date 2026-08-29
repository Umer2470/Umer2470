package com.example.data.api.repository

import android.content.Context
import com.example.data.api.client.DeveloperApiClient
import com.example.data.api.model.*
import com.example.data.api.security.LicenseStateCache
import com.example.data.api.security.SecureIdentityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Response
import java.io.EOFException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

class DeveloperApiRepository(private val context: Context) {

    private val apiClient = DeveloperApiClient.getInstance(context)
    private val identityManager = SecureIdentityManager.getInstance(context)
    private val licenseStateCache = LicenseStateCache.getInstance(context)

    /**
     * Activates the app installation with an activation code (POST /installation/activate).
     */
    suspend fun activateInstallation(activationCode: String): ApiResult<InstallationActivateResponse> {
        return withContext(Dispatchers.IO) {
            executeSafeApiCall(InstallationActivateResponse::class.java) {
                val req = InstallationActivateRequest(
                    installationId = identityManager.getInstallationId(),
                    activationCode = activationCode.trim(),
                    customerId = identityManager.getCustomerId(),
                    storeId = identityManager.getStoreId(),
                    appVersion = identityManager.getAppVersion(),
                    deviceFingerprint = identityManager.getDeviceFingerprint()
                )
                apiClient.apiService.activateInstallation(req)
            }.also { result ->
                if (result is ApiResult.Success) {
                    val data = result.data
                    if (data.isActivationSuccessful()) {
                        licenseStateCache.updateCachedLicenseState(
                            status = data.getEffectiveStatus(),
                            message = data.getEffectiveMessage(),
                            planType = data.planType ?: data.plan ?: "COMMERCIAL",
                            maxShops = data.maxShops ?: 5,
                            maxUsers = data.maxUsers ?: 10
                        )
                    }
                }
            }
        }
    }

    /**
     * Registers a new device installation on the Developer Server (POST /installation/register).
     */
    suspend fun registerInstallation(): ApiResult<RegisterInstallationResponse> {
        return withContext(Dispatchers.IO) {
            executeSafeApiCall(RegisterInstallationResponse::class.java) {
                val req = RegisterInstallationRequest(
                    installationId = identityManager.getInstallationId(),
                    appVersion = identityManager.getAppVersion(),
                    deviceFingerprint = identityManager.getDeviceFingerprint()
                )
                apiClient.apiService.registerInstallation(req)
            }
        }
    }

    /**
     * Validates a license key with the Developer Server (POST /license/validate).
     */
    suspend fun validateLicense(licenseKey: String): ApiResult<LicenseValidateResponse> {
        return withContext(Dispatchers.IO) {
            executeSafeApiCall(LicenseValidateResponse::class.java) {
                val req = LicenseValidateRequest(
                    installationId = identityManager.getInstallationId(),
                    licenseKey = licenseKey.trim(),
                    appVersion = identityManager.getAppVersion()
                )
                apiClient.apiService.validateLicense(req)
            }.also { result ->
                if (result is ApiResult.Success) {
                    val data = result.data
                    licenseStateCache.updateCachedLicenseState(
                        status = data.status ?: "active",
                        message = data.message ?: "License validated successfully",
                        planType = data.planType ?: data.plan ?: "COMMERCIAL",
                        maxShops = data.maxShops ?: 5,
                        maxUsers = data.maxUsers ?: 10
                    )
                }
            }
        }
    }

    /**
     * Sends periodic license heartbeat to the Developer Server (POST /license/heartbeat).
     */
    suspend fun sendHeartbeat(): ApiResult<LicenseHeartbeatResponse> {
        return withContext(Dispatchers.IO) {
            executeSafeApiCall(LicenseHeartbeatResponse::class.java) {
                val req = LicenseHeartbeatRequest(
                    installationId = identityManager.getInstallationId(),
                    customerId = identityManager.getCustomerId(),
                    storeId = identityManager.getStoreId()
                )
                apiClient.apiService.sendHeartbeat(req)
            }.also { result ->
                if (result is ApiResult.Success) {
                    val data = result.data
                    licenseStateCache.updateCachedLicenseState(
                        status = data.status ?: "active",
                        message = data.message ?: "Heartbeat acknowledged"
                    )
                }
            }
        }
    }

    /**
     * Checks developer server health (GET /health).
     */
    suspend fun checkHealth(): ApiResult<HealthCheckResponse> {
        return withContext(Dispatchers.IO) {
            executeSafeApiCall(HealthCheckResponse::class.java) {
                apiClient.apiService.checkHealth()
            }
        }
    }

    /**
     * Gets server configuration (GET /server/config).
     */
    suspend fun getServerConfig(): ApiResult<ServerConfigResponse> {
        return withContext(Dispatchers.IO) {
            executeSafeApiCall(ServerConfigResponse::class.java) {
                apiClient.apiService.getServerConfig()
            }
        }
    }

    /**
     * Checks for app version updates from Developer Server (POST /app/version).
     */
    suspend fun checkAppVersion(): ApiResult<AppVersionCheckResponse> {
        return withContext(Dispatchers.IO) {
            executeSafeApiCall(AppVersionCheckResponse::class.java) {
                val req = AppVersionCheckRequest(
                    appVersion = identityManager.getAppVersion(),
                    installationId = identityManager.getInstallationId()
                )
                apiClient.apiService.checkAppVersion(req)
            }
        }
    }

    /**
     * Safely executes an API call catching malformed JSON, HTML pages, network timeouts,
     * DNS errors, and HTTP error codes without crashing the application.
     */
    private suspend fun <T : Any> executeSafeApiCall(
        clazz: Class<T>,
        call: suspend () -> Response<ResponseBody>
    ): ApiResult<T> {
        return try {
            val response = call()
            val statusCode = response.code()
            val rawBody = try {
                if (response.isSuccessful) {
                    response.body()?.string() ?: ""
                } else {
                    response.errorBody()?.string() ?: ""
                }
            } catch (e: Exception) {
                ""
            }

            if (rawBody.isBlank()) {
                return if (response.isSuccessful) {
                    ApiResult.Error(code = statusCode, message = "Server returned an empty response.")
                } else {
                    val fallbackMsg = mapHttpCodeToFriendlyMessage(statusCode)
                    ApiResult.Error(code = statusCode, message = fallbackMsg)
                }
            }

            val trimmed = rawBody.trim()

            // Detect HTML responses (Cloud Run fallback, Nginx error pages, Web UI index.html)
            if (trimmed.startsWith("<") || trimmed.contains("<!DOCTYPE html", ignoreCase = true) || trimmed.contains("<html", ignoreCase = true)) {
                val htmlError = when (statusCode) {
                    404 -> "Activation service is currently unavailable (HTTP 404)."
                    in 500..599 -> "Licensing server is undergoing maintenance (HTTP $statusCode). Please try again later."
                    else -> "Unable to reach licensing service (HTTP $statusCode). Please check your internet connection."
                }
                return ApiResult.Error(code = statusCode, message = htmlError)
            }

            // Verify if payload looks like JSON
            if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
                return ApiResult.Error(code = statusCode, message = "Unexpected response from licensing service. Please try again.")
            }

            // Attempt Moshi parsing safely
            val parsedDto: T? = try {
                val adapter = apiClient.moshi.adapter(clazz)
                adapter.fromJson(trimmed)
            } catch (e: Exception) {
                null
            }

            if (response.isSuccessful && parsedDto != null) {
                ApiResult.Success(parsedDto)
            } else if (parsedDto != null) {
                val extractedMessage = extractErrorMessageFromDto(parsedDto, trimmed) ?: mapHttpCodeToFriendlyMessage(statusCode)
                ApiResult.Error(code = statusCode, message = extractedMessage)
            } else {
                // Moshi parsing failed; attempt JSONObject extraction for error details
                val extractedMessage = extractErrorMessageFromJsonString(trimmed)
                if (response.isSuccessful) {
                    ApiResult.Error(code = statusCode, message = extractedMessage ?: "Unable to parse server response into expected model.")
                } else {
                    ApiResult.Error(code = statusCode, message = extractedMessage ?: mapHttpCodeToFriendlyMessage(statusCode))
                }
            }
        } catch (e: OfflineNetworkException) {
            ApiResult.Offline
        } catch (e: SocketTimeoutException) {
            ApiResult.Error(
                code = -1,
                message = "Connection timed out while contacting activation server. Please check your internet connection.",
                isNetworkError = true
            )
        } catch (e: UnknownHostException) {
            ApiResult.Error(
                code = -1,
                message = "Unable to resolve server address. Please check your internet connection.",
                isNetworkError = true
            )
        } catch (e: ConnectException) {
            ApiResult.Error(
                code = -1,
                message = "Failed to connect to license service. Server may be temporarily unreachable.",
                isNetworkError = true
            )
        } catch (e: SSLException) {
            ApiResult.Error(
                code = -1,
                message = "Secure SSL handshake with licensing server failed.",
                isNetworkError = true
            )
        } catch (e: EOFException) {
            ApiResult.Error(
                code = -1,
                message = "Server closed connection unexpectedly (EOF). Please try again.",
                isNetworkError = true
            )
        } catch (e: IOException) {
            ApiResult.Error(
                code = -1,
                message = e.localizedMessage ?: "Network I/O error occurred while contacting server.",
                isNetworkError = true
            )
        } catch (e: Exception) {
            ApiResult.Error(
                code = -1,
                message = e.localizedMessage ?: "Unexpected error occurred during server communication.",
                isNetworkError = true
            )
        }
    }

    private fun mapHttpCodeToFriendlyMessage(code: Int): String {
        return when (code) {
            400 -> "Invalid request. Please verify the activation code format."
            401 -> "Invalid activation credentials. Please check your activation key."
            403 -> "Access restricted by licensing server."
            404 -> "Licensing endpoint not found (HTTP 404)."
            408 -> "Request timeout (HTTP 408). Server took too long to respond."
            409 -> "Activation code is already registered to another terminal."
            422 -> "Invalid activation code payload."
            429 -> "Too many requests. Please wait a moment and try again."
            500 -> "Internal server error. Please try again later."
            502 -> "Bad gateway. Licensing service is temporarily unavailable."
            503 -> "Licensing service is temporarily undergoing maintenance."
            504 -> "Gateway timeout. Server took too long to respond."
            else -> "Server returned HTTP $code"
        }
    }

    private fun extractErrorMessageFromJsonString(jsonString: String): String? {
        return try {
            val obj = JSONObject(jsonString)
            val msg = obj.optString("message").ifBlank { null }
                ?: obj.optString("error").ifBlank { null }
                ?: obj.optString("error_message").ifBlank { null }
                ?: obj.optString("details").ifBlank { null }
                ?: obj.optString("status").ifBlank { null }
            msg
        } catch (e: Exception) {
            null
        }
    }

    private fun extractErrorMessageFromDto(dto: Any, rawJson: String): String? {
        return when (dto) {
            is InstallationActivateResponse -> dto.getEffectiveMessage()
            is RegisterInstallationResponse -> dto.message ?: dto.error
            is LicenseValidateResponse -> dto.message ?: dto.error
            is LicenseHeartbeatResponse -> dto.message ?: dto.error
            is HealthCheckResponse -> dto.message ?: dto.error
            is ServerConfigResponse -> dto.message ?: dto.error
            is AppVersionCheckResponse -> dto.message
            else -> extractErrorMessageFromJsonString(rawJson)
        }
    }

    fun getInstallationId(): String = identityManager.getInstallationId()

    fun getCachedLicenseStatus(): String = licenseStateCache.getCachedLicenseStatus()
}
