package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.api.platform.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class UniversalLicensePlatformTest {

    private lateinit var context: Context
    private lateinit var engine: UniversalLicensePlatformEngine
    private lateinit var sdk: UniversalDeveloperLicenseSdk

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        engine = UniversalLicensePlatformEngine.getInstance(context)
        sdk = UniversalDeveloperLicenseSdk.getInstance(context, "SENTRY-STORE-POS")
    }

    @Test
    fun testAppRegistryInitialization() {
        val apps = engine.applicationsFlow.value
        assertTrue("Registered apps should not be empty", apps.isNotEmpty())
        val sentryApp = apps.find { it.appId == "SENTRY-STORE-POS" }
        assertNotNull("Sentry Store POS app must be registered", sentryApp)
        assertEquals("SENTRY STORE POS", sentryApp?.appName)
    }

    @Test
    fun testLicenseGenerationAndVerification() {
        val testTerminalId = "INST-ABC12345-TEST"
        val license = engine.generateLicense(
            appId = "SENTRY-STORE-POS",
            customerId = "CUST-1001",
            installationId = testTerminalId,
            durationDays = 90,
            planNameOverride = "3 Months Business Plan"
        )

        val code = license.licenseCode
        assertTrue("Generated code must not be blank", code.isNotBlank())
        assertTrue("Generated code must start with ACTV-", code.startsWith("ACTV-"))

        // Verify with engine
        val result = engine.verifyLicenseCode("SENTRY-STORE-POS", testTerminalId, code)
        assertTrue("Verification should be valid", result is LicenseVerificationResult.Valid)
        val validResult = result as LicenseVerificationResult.Valid
        assertEquals("SENTRY-STORE-POS", validResult.license.appId)
        assertEquals(testTerminalId, validResult.license.installationId)
        assertEquals("3 Months Business Plan", validResult.license.planName)
    }

    @Test
    fun testCrossAppIsolation() {
        val testTerminalId = "INST-CROSS-APP-TEST"
        // Generate license for SENTRY-STORE-POS
        val license = engine.generateLicense(
            appId = "SENTRY-STORE-POS",
            customerId = "CUST-1001",
            installationId = testTerminalId,
            durationDays = 30
        )

        // Attempt to verify with a different appId (e.g. WORKOUT-APP)
        val crossResult = engine.verifyLicenseCode("WORKOUT-APP", testTerminalId, license.licenseCode)
        assertTrue("Cross-app license verification must return AppMismatch or Invalid", crossResult is LicenseVerificationResult.AppMismatch || crossResult is LicenseVerificationResult.Invalid)
    }

    @Test
    fun testInstallationIdBinding() {
        val originalTerminalId = "INST-DEVICE-ALPHA"
        val wrongTerminalId = "INST-DEVICE-BETA"

        val license = engine.generateLicense(
            appId = "SENTRY-STORE-POS",
            customerId = "CUST-1001",
            installationId = originalTerminalId,
            durationDays = 30
        )

        val wrongDeviceResult = engine.verifyLicenseCode("SENTRY-STORE-POS", wrongTerminalId, license.licenseCode)
        assertTrue("Verification on wrong device ID must fail", wrongDeviceResult is LicenseVerificationResult.InstallationMismatch || wrongDeviceResult is LicenseVerificationResult.Invalid)
    }

    @Test
    fun testDeviceTransfer() {
        val oldTerminalId = "INST-OLD-PHONE-01"
        val newTerminalId = "INST-NEW-TABLET-02"

        val originalLicense = engine.generateLicense(
            appId = "SENTRY-STORE-POS",
            customerId = "CUST-1001",
            installationId = oldTerminalId,
            durationDays = 180,
            planNameOverride = "6 Months Pro Plan"
        )

        // Execute device transfer
        val transferredLicense = engine.transferDevice(
            licenseId = originalLicense.licenseId,
            newInstallationId = newTerminalId
        )

        assertNotNull("Transferred license should be generated", transferredLicense)

        // Old device should now fail
        val oldResult = engine.verifyLicenseCode("SENTRY-STORE-POS", oldTerminalId, originalLicense.licenseCode)
        assertTrue("Old device must no longer verify after transfer", oldResult !is LicenseVerificationResult.Valid)

        // New device should succeed
        val newResult = engine.verifyLicenseCode("SENTRY-STORE-POS", newTerminalId, transferredLicense!!.licenseCode)
        assertTrue("New device must verify with transferred license", newResult is LicenseVerificationResult.Valid)
    }

    @Test
    fun testLicenseDeactivation() {
        val terminalId = "INST-REVOKE-TEST"
        val license = engine.generateLicense(
            appId = "SENTRY-STORE-POS",
            customerId = "CUST-1001",
            installationId = terminalId,
            durationDays = 365,
            planNameOverride = "1 Year Enterprise"
        )

        engine.deactivateLicense(license.licenseId)

        val result = engine.verifyLicenseCode("SENTRY-STORE-POS", terminalId, license.licenseCode)
        assertTrue("Deactivated license must fail verification", result !is LicenseVerificationResult.Valid)
    }

    @Test
    fun testLicenseExtension() {
        val terminalId = "INST-EXTEND-TEST"
        val license = engine.generateLicense(
            appId = "SENTRY-STORE-POS",
            customerId = "CUST-1001",
            installationId = terminalId,
            durationDays = 30
        )

        val initialExpiry = license.expiryDate
        val updated = engine.extendLicense(license.licenseId, 30)
        assertNotNull("Updated license must exist", updated)
        assertTrue("Expiry timestamp should increase", updated!!.expiryDate > initialExpiry)
    }

    @Test
    fun testSdkActivation() {
        val myTerminalId = sdk.getInstallationId()
        val license = engine.generateLicense(
            appId = "SENTRY-STORE-POS",
            customerId = "CUST-1001",
            installationId = myTerminalId,
            durationDays = 30,
            planNameOverride = "1 Month Commercial"
        )

        var isSuccessResult = false
        var messageResult = ""
        sdk.activateWithCode(license.licenseCode) { success, _, msg ->
            isSuccessResult = success
            messageResult = msg
        }

        assertTrue("SDK activation should return success: $messageResult", isSuccessResult)
        assertTrue("SDK should now report activated", sdk.isActivated())
        assertEquals("1 Month Commercial", sdk.getCurrentState().planName)
    }
}
