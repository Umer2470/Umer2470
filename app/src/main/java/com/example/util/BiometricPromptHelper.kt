package com.example.util

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricPromptHelper {

    fun isBiometricAvailable(context: Context): Pair<Boolean, String> {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Pair(true, "Biometric authentication is ready.")
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> Pair(false, "No biometric hardware on this device.")
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> Pair(false, "Biometric hardware is currently unavailable.")
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Pair(false, "No biometrics/fingerprints enrolled on device.")
            else -> Pair(false, "Biometric authentication unavailable.")
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

    fun authenticateSuperAdmin(
        context: Context,
        title: String = "Super Admin Biometric Security",
        subtitle: String = "Authenticate fingerprint to verify Super Admin identity",
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val activity = findFragmentActivity(context)
        if (activity == null) {
            // Fallback for environment without FragmentActivity attached
            onError("FragmentActivity context unavailable.")
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
            .setNegativeButtonText("Use PIN")
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
                onError("Fingerprint not recognized. Try again.")
            }
        })

        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Failed to launch BiometricPrompt")
        }
    }
}
