package com.example.data.fbr

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class FbrApiService {

    companion object {
        private const val TAG = "FbrApiService"
        private const val SANDBOX_URL = "https://esp.fbr.gov.pk:8244/FBR/v1/api/Live/PostData"
        private const val PRODUCTION_URL = "https://esp.fbr.gov.pk:8244/FBR/v1/api/Live/PostData"
        private const val CONNECT_TIMEOUT_MS = 10000
        private const val READ_TIMEOUT_MS = 15000
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val payloadAdapter = moshi.adapter(FbrInvoicePayload::class.java)
    private val responseAdapter = moshi.adapter(FbrInvoiceResponse::class.java)

    /**
     * Submit invoice payload to official FBR server.
     */
    suspend fun submitInvoice(
        payload: FbrInvoicePayload,
        authToken: String,
        isProduction: Boolean
    ): FbrSubmissionResult = withContext(Dispatchers.IO) {
        if (authToken.isBlank()) {
            return@withContext FbrSubmissionResult.Failure(
                errorMessage = "FBR API Auth Bearer Token is missing. Please configure in Settings.",
                responseCode = "AUTH_MISSING",
                retryable = false
            )
        }
        if (payload.posId <= 0) {
            return@withContext FbrSubmissionResult.Failure(
                errorMessage = "FBR POS ID is invalid or not configured.",
                responseCode = "POSID_INVALID",
                retryable = false
            )
        }

        val targetUrl = if (isProduction) PRODUCTION_URL else SANDBOX_URL
        var connection: HttpURLConnection? = null

        try {
            val url = URL(targetUrl)
            connection = (url.openConnection() as HttpsURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                doInput = true
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Authorization", "Bearer ${authToken.trim()}")
            }

            val jsonBody = payloadAdapter.toJson(payload)
            Log.d(TAG, "Submitting to FBR: $jsonBody")

            OutputStreamWriter(connection.outputStream, "UTF-8").use { writer ->
                writer.write(jsonBody)
                writer.flush()
            }

            val responseCode = connection.responseCode
            val responseStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }

            val responseBody = BufferedReader(InputStreamReader(responseStream, "UTF-8")).use { reader ->
                reader.readText()
            }

            Log.d(TAG, "FBR Response ($responseCode): $responseBody")

            if (responseCode == 200 || responseCode == 201) {
                try {
                    val parsed = responseAdapter.fromJson(responseBody)
                    val fbrInv = parsed?.invoiceNumber.orEmpty()
                    val respCode = parsed?.code ?: responseCode.toString()

                    if (fbrInv.isNotBlank() || parsed?.response.equals("OK", ignoreCase = true) || parsed?.status.equals("Success", ignoreCase = true)) {
                        val officialNumber = if (fbrInv.isNotBlank()) fbrInv else "${payload.posId}-${payload.usin}"
                        FbrSubmissionResult.Success(
                            fbrInvoiceNumber = officialNumber,
                            responseCode = respCode,
                            qrCodeData = officialNumber,
                            message = parsed?.response ?: "Acknowledged by FBR"
                        )
                    } else {
                        val err = parsed?.errors?.joinToString(", ")
                            ?: parsed?.response
                            ?: "FBR validation rejected the invoice submission."
                        FbrSubmissionResult.Failure(
                            errorMessage = err,
                            responseCode = respCode,
                            retryable = true
                        )
                    }
                } catch (e: Exception) {
                    FbrSubmissionResult.Failure(
                        errorMessage = "Failed to parse FBR response: ${e.message}",
                        responseCode = responseCode.toString(),
                        retryable = true
                    )
                }
            } else if (responseCode == 401 || responseCode == 403) {
                FbrSubmissionResult.Failure(
                    errorMessage = "FBR Authentication failed (HTTP $responseCode). Verify Bearer Token.",
                    responseCode = responseCode.toString(),
                    retryable = false
                )
            } else {
                val errorMsg = try {
                    val json = JSONObject(responseBody)
                    json.optString("Response", json.optString("message", "FBR Server returned HTTP $responseCode"))
                } catch (_: Exception) {
                    "FBR Server returned HTTP $responseCode: $responseBody"
                }
                FbrSubmissionResult.Failure(
                    errorMessage = errorMsg,
                    responseCode = responseCode.toString(),
                    retryable = true
                )
            }
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "FBR Host unreachable: ${e.message}")
            FbrSubmissionResult.Failure(
                errorMessage = "FBR server unreachable (Offline / DNS check failed).",
                isNetworkError = true,
                retryable = true
            )
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "FBR Timeout: ${e.message}")
            FbrSubmissionResult.Failure(
                errorMessage = "FBR server request timed out.",
                isNetworkError = true,
                retryable = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "FBR Exception: ${e.message}", e)
            FbrSubmissionResult.Failure(
                errorMessage = "Network or SSL connection error: ${e.localizedMessage ?: "Unknown error"}",
                isNetworkError = true,
                retryable = true
            )
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Test connection to FBR server using configured credentials.
     */
    suspend fun testConnection(
        posId: String,
        authToken: String,
        isProduction: Boolean
    ): Pair<FbrConnectionStatus, String> = withContext(Dispatchers.IO) {
        if (authToken.isBlank() || posId.isBlank()) {
            return@withContext Pair(
                FbrConnectionStatus.CONFIGURATION_REQUIRED,
                "POS ID and API Auth Token must be provided."
            )
        }

        val parsedPosId = posId.toIntOrNull()
        if (parsedPosId == null || parsedPosId <= 0) {
            return@withContext Pair(
                FbrConnectionStatus.CONFIGURATION_REQUIRED,
                "POS ID must be a valid positive integer."
            )
        }

        // Ping FBR endpoint with a minimal test payload
        val testPayload = FbrInvoicePayload(
            posId = parsedPosId,
            usin = "TEST-PING-${System.currentTimeMillis()}",
            dateTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
            totalSaleValue = 0.0,
            totalTaxCharged = 0.0,
            totalBillAmount = 0.0,
            invoiceType = 1,
            items = emptyList()
        )

        when (val res = submitInvoice(testPayload, authToken, isProduction)) {
            is FbrSubmissionResult.Success -> {
                Pair(FbrConnectionStatus.CONNECTED, "Connection verified. FBR Server acknowledged endpoint.")
            }
            is FbrSubmissionResult.Failure -> {
                if (res.responseCode == "401" || res.responseCode == "403") {
                    Pair(FbrConnectionStatus.AUTHENTICATION_FAILED, res.errorMessage)
                } else if (res.isNetworkError) {
                    Pair(FbrConnectionStatus.OFFLINE, res.errorMessage)
                } else {
                    // If FBR responded with POS verification message or validation code, the server IS reachable
                    Pair(FbrConnectionStatus.CONNECTED, "Endpoint reached. Status: ${res.errorMessage}")
                }
            }
            is FbrSubmissionResult.NotRequired -> {
                Pair(FbrConnectionStatus.OFFLINE, "FBR Integration is disabled.")
            }
        }
    }
}
