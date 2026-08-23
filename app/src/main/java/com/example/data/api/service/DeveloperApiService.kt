package com.example.data.api.service

import com.example.data.api.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface DeveloperApiService {
    @POST("installation/activate")
    suspend fun activateInstallation(
        @Body request: InstallationActivateRequest
    ): Response<ResponseBody>

    @POST("installation/register")
    suspend fun registerInstallation(
        @Body request: RegisterInstallationRequest
    ): Response<ResponseBody>

    @POST("license/validate")
    suspend fun validateLicense(
        @Body request: LicenseValidateRequest
    ): Response<ResponseBody>

    @POST("license/heartbeat")
    suspend fun sendHeartbeat(
        @Body request: LicenseHeartbeatRequest
    ): Response<ResponseBody>

    @GET("health")
    suspend fun checkHealth(): Response<ResponseBody>

    @GET("server/config")
    suspend fun getServerConfig(): Response<ResponseBody>

    @POST("app/version")
    suspend fun checkAppVersion(
        @Body request: AppVersionCheckRequest
    ): Response<ResponseBody>
}
