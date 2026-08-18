package com.example.data.api.service

import com.example.data.api.model.AppVersionCheckRequest
import com.example.data.api.model.AppVersionCheckResponse
import com.example.data.api.model.HealthCheckResponse
import com.example.data.api.model.InstallationActivateRequest
import com.example.data.api.model.InstallationActivateResponse
import com.example.data.api.model.LicenseHeartbeatRequest
import com.example.data.api.model.LicenseHeartbeatResponse
import com.example.data.api.model.LicenseValidateRequest
import com.example.data.api.model.LicenseValidateResponse
import com.example.data.api.model.RegisterInstallationRequest
import com.example.data.api.model.RegisterInstallationResponse
import com.example.data.api.model.ServerConfigResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * Retrofit interface defining Developer Server API endpoints for CH UMER DEVELOPER APP.
 */
interface DeveloperApiService {

    @GET("health")
    suspend fun checkHealth(): Response<HealthCheckResponse>

    @GET("config")
    suspend fun getServerConfig(): Response<ServerConfigResponse>

    @POST("installation/register")
    suspend fun registerInstallation(
        @Body request: RegisterInstallationRequest
    ): Response<RegisterInstallationResponse>

    @POST("installation/activate")
    suspend fun activateInstallation(
        @Body request: InstallationActivateRequest
    ): Response<InstallationActivateResponse>

    @POST("license/validate")
    suspend fun validateLicense(
        @Body request: LicenseValidateRequest
    ): Response<LicenseValidateResponse>

    @POST("license/heartbeat")
    suspend fun sendHeartbeat(
        @Body request: LicenseHeartbeatRequest
    ): Response<LicenseHeartbeatResponse>

    @POST("app/version")
    suspend fun checkAppVersion(
        @Body request: AppVersionCheckRequest
    ): Response<AppVersionCheckResponse>
}
