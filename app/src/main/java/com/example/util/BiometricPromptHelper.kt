package com.example.util

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricPromptHelper {

    enum class BiometricStatus {
        READY,
        NO_HARDWARE,
        HW_UNAVAILABLE,
        NONE_ENROLLED,
        UNAVAILABLE
    }

    fun getBiometricStatus(context: Context): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.READY
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HW_UNAVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NONE_ENROLLED
            else -> BiometricStatus.UNAVAILABLE
        }
    }

    fun isBiometricAvailable(context: Context): Pair<Boolean, String> {
        return when (getBiometricStatus(context)) {
            BiometricStatus.READY -> Pair(true, "Biometric authentication is ready (Fingerprint / Face).")
            BiometricStatus.NO_HARDWARE -> Pair(false, "No biometric sensor hardware found on this device.")
            BiometricStatus.HW_UNAVAILABLE -> Pair(false, "Biometric sensor hardware is currently unavailable.")
            BiometricStatus.NONE_ENROLLED -> Pair(false, "No fingerprints or face unlock registered on this device.")
            BiometricStatus.UNAVAILABLE -> Pair(false, "Biometric authentication is unavailable.")
        }
    }

    fun findFragmentActivity(context: Context): FragmentActivity? {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is FragmentActivity) {
                return currentContext
            }
            currentContext = currentContext.baseContext
        }
        return null
    }

    fun authenticateUser(
        context: Context,
        title: String = "Secure Biometric Unlock",
        subtitle: String = "Verify fingerprint or face to access CH UMER POS",
        negativeButtonText: String = "Use PIN / Password",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val activity = findFragmentActivity(context)
        if (activity == null) {
            onError("Biometric authentication requires an active activity.")
            return
        }

        val (available, message) = isBiometricAvailable(context)
        if (!available) {
            onError(message)
            return
        }

        val executor = ContextCompat.getMainExecutor(context)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription("Touch the fingerprint sensor or look at the screen to verify identity")
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    onError(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onError("Biometric not recognized. Please try again.")
            }
        })

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Failed to launch Biometric prompt")
        }
    }

    fun authenticateSuperAdmin(
        context: Context,
        title: String = "Super Admin Biometric Security",
        subtitle: String = "Authenticate fingerprint to verify Super Admin identity",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        authenticateUser(
            context = context,
            title = title,
            subtitle = subtitle,
            negativeButtonText = "Use PIN",
            onSuccess = onSuccess,
            onError = onError
        )
    }
}
