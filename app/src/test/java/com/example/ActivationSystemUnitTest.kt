package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.api.model.InstallationActivateResponse
import com.example.data.api.security.AppActivationManager
import com.example.data.api.security.SecureIdentityManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ActivationSystemUnitTest {

    private lateinit var context: Context
    private lateinit var activationManager: AppActivationManager
    private lateinit var identityManager: SecureIdentityManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        activationManager = AppActivationManager.getInstance(context)
        identityManager = SecureIdentityManager.getInstance(context)
    }

    @Test
    fun testInstallationIdGenerated() {
        val installationId = identityManager.getInstallationId()
        assertNotNull(installationId)
        assertTrue(installationId.startsWith("APP-"))
    }

    @Test
    fun testOfflineCryptographicActivationCode() {
        val installationId = identityManager.getInstallationId()
        val generatedCode = AppActivationManager.generateActivationCode(installationId)
        assertNotNull(generatedCode)
        assertTrue(generatedCode.startsWith("ACTV-"))

        // Test activation with generated cryptographic code
        runBlocking {
            var activationResult = false
            var resultStatus = ""
            var resultMessage = ""

            activationManager.activateWithCode(generatedCode) { status, msg, isSuccess ->
                activationResult = isSuccess
                resultStatus = status
                resultMessage = msg
            }

            assertTrue("Cryptographic code activation should succeed", activationResult)
            assertEquals(AppActivationManager.STATUS_ACTIVATED, resultStatus)
            assertTrue("App must report isActivated = true", activationManager.isActivated())
        }
    }

    @Test
    fun testInstallationActivateResponseValidation() {
        // Test 1: Successful response
        val successResp = InstallationActivateResponse(
            status = "ACTIVE",
            success = true,
            message = "Activation successful",
            activationToken = "TOK-12345"
        )
        assertTrue(successResp.isActivationSuccessful())
        assertEquals("ACTIVE", successResp.getEffectiveStatus())
        assertEquals("Activation successful", successResp.getEffectiveMessage())

        // Test 2: Invalid Code
        val invalidResp = InstallationActivateResponse(
            status = "INVALID",
            success = false,
            error = "Invalid activation code"
        )
        assertFalse(invalidResp.isActivationSuccessful())
        assertEquals("INVALID", invalidResp.getEffectiveStatus())
        assertEquals("Invalid activation code", invalidResp.getEffectiveMessage())

        // Test 3: Expired License
        val expiredResp = InstallationActivateResponse(
            status = "EXPIRED",
            success = false,
            message = "License has expired"
        )
        assertFalse(expiredResp.isActivationSuccessful())
        assertEquals("EXPIRED", expiredResp.getEffectiveStatus())

        // Test 4: Revoked License
        val revokedResp = InstallationActivateResponse(
            status = "REVOKED",
            success = false,
            message = "License was revoked"
        )
        assertFalse(revokedResp.isActivationSuccessful())
        assertEquals("REVOKED", revokedResp.getEffectiveStatus())

        // Test 5: Installation ID Mismatch
        val mismatchResp = InstallationActivateResponse(
            status = "INSTALLATION_MISMATCH",
            success = false,
            message = "Installation ID mismatch"
        )
        assertFalse(mismatchResp.isActivationSuccessful())
        assertEquals("INSTALLATION_MISMATCH", mismatchResp.getEffectiveStatus())

        // Test 6: Code Already Used
        val usedResp = InstallationActivateResponse(
            status = "ALREADY_USED",
            success = false,
            message = "Activation code already used"
        )
        assertFalse(usedResp.isActivationSuccessful())
        assertEquals("ALREADY_USED", usedResp.getEffectiveStatus())
    }

    @Test
    fun testInvalidCodeRejection() {
        runBlocking {
            activationManager.resetActivation()
            assertFalse(activationManager.isActivated())

            var activationResult = false
            activationManager.activateWithCode("ACTV-INVALID-CODE-0000-0000") { _, _, isSuccess ->
                activationResult = isSuccess
            }

            assertFalse("Invalid code should be rejected", activationResult)
            assertFalse("App must remain unactivated", activationManager.isActivated())
        }
    }

    @Test
    fun testResponseFieldFallbacks() {
        // Fallback for token, plan, error_message
        val fallbackResp = InstallationActivateResponse(
            status = "active",
            token = "ALT-TOKEN-999",
            plan = "ENTERPRISE",
            errorMessage = "No issues",
            maxShops = 15,
            maxUsers = 50
        )
        assertTrue(fallbackResp.isActivationSuccessful())
        assertEquals("active", fallbackResp.getEffectiveStatus())
        assertEquals("No issues", fallbackResp.getEffectiveMessage())
        assertEquals("ALT-TOKEN-999", fallbackResp.token)
        assertEquals("ENTERPRISE", fallbackResp.plan)
    }

    @Test
    fun testResetActivationPreservesIdentity() {
        val originalId = identityManager.getInstallationId()
        activationManager.resetActivation()
        assertEquals(AppActivationManager.STATUS_FIRST_INSTALL_NOT_ACTIVATED, activationManager.getActivationStatus())
        assertFalse(activationManager.isActivated())
        // Installation ID must stay stable across resets
        assertEquals(originalId, identityManager.getInstallationId())
    }
}
