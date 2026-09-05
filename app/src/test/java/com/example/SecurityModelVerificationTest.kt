package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.api.platform.LicenseVerificationResult
import com.example.data.api.platform.UniversalLicensePlatformEngine
import com.example.data.api.security.AppActivationManager
import com.example.data.api.security.SecureIdentityManager
import com.example.data.db.AppDatabase
import com.example.data.model.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SecurityModelVerificationTest {

    private lateinit var context: Context
    private lateinit var activationManager: AppActivationManager
    private lateinit var identityManager: SecureIdentityManager
    private lateinit var platformEngine: UniversalLicensePlatformEngine

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        activationManager = AppActivationManager.getInstance(context)
        identityManager = SecureIdentityManager.getInstance(context)
        platformEngine = UniversalLicensePlatformEngine.getInstance(context)
    }

    /**
     * TEST 1: Fresh Installation Activation Lock
     * A brand new installation MUST NOT be activated by default.
     * Expected: STATUS_FIRST_INSTALL_NOT_ACTIVATED, isActivated == false
     */
    @Test
    fun testFreshInstallationActivationLock() {
        // Clear all activation preferences to simulate fresh installation
        activationManager.resetActivation()

        val currentStatus = activationManager.getActivationStatus()
        assertEquals("Fresh installation must have STATUS_FIRST_INSTALL_NOT_ACTIVATED",
            AppActivationManager.STATUS_FIRST_INSTALL_NOT_ACTIVATED, currentStatus)

        val isActivated = activationManager.isActivated()
        assertFalse("Fresh installation must NOT be activated by default", isActivated)
    }

    /**
     * TEST 2: Valid Activation
     * Upon providing a valid cryptographic activation code bound to the terminal,
     * status transitions to ACTIVATED.
     */
    @Test
    fun testValidActivation() {
        activationManager.resetActivation()
        val installationId = identityManager.getInstallationId()
        val validCode = AppActivationManager.generatePlanActivationCode(installationId, 180)

        runBlocking {
            var activationSuccess = false
            var finalStatus = ""
            activationManager.activateWithCode(validCode) { status, _, success ->
                activationSuccess = success
                finalStatus = status
            }

            assertTrue("Valid cryptographic activation code must succeed", activationSuccess)
            assertEquals("Status must become ACTIVATED", AppActivationManager.STATUS_ACTIVATED, finalStatus)
            assertTrue("App must now be activated", activationManager.isActivated())
            assertEquals("Plan must reflect 6 Months Professional", "6 Months Professional", activationManager.getPlanName())
        }
    }

    /**
     * TEST 3: Offline Existing License
     * Once activated, offline operation must keep the existing license intact.
     * Network unavailability must NOT deactivate the application.
     */
    @Test
    fun testOfflineExistingLicense() {
        val installationId = identityManager.getInstallationId()
        val validCode = AppActivationManager.generateActivationCode(installationId)

        runBlocking {
            // Step A: Activate the device
            activationManager.activateWithCode(validCode) { _, _, _ -> }
            assertTrue("Device must be active", activationManager.isActivated())

            // Step B: Simulate subsequent offline app usage / checks
            // Offline querying should directly return cached active license
            val status = activationManager.getActivationStatus()
            assertEquals("Existing license remains ACTIVATED offline", AppActivationManager.STATUS_ACTIVATED, status)
            assertTrue("App remains fully activated offline", activationManager.isActivated())
        }
    }

    /**
     * TEST 4: Device Binding
     * An activation code generated for device A must FAIL on device B.
     */
    @Test
    fun testDeviceBinding() {
        val legitimateDevice = "INST-TERMINAL-ALPHA-001"
        val foreignDevice = "INST-TERMINAL-BETA-002"

        val legitimateCode = AppActivationManager.generateActivationCode(legitimateDevice)

        // Verifying code for legitimate device passes
        assertTrue("Code valid on legitimate device",
            AppActivationManager.verifyActivationCodeLocally(legitimateDevice, legitimateCode))

        // Verifying code on foreign device fails
        assertFalse("Code bound to device ALPHA must fail on device BETA",
            AppActivationManager.verifyActivationCodeLocally(foreignDevice, legitimateCode))

        // Engine verification also returns installation mismatch
        val platformResult = platformEngine.verifyLicenseCode(
            currentAppId = UniversalLicensePlatformEngine.APP_SENTRY_STORE_POS,
            currentInstallationId = foreignDevice,
            licenseCode = legitimateCode
        )
        assertTrue("Platform engine must reject mismatched installation",
            platformResult !is LicenseVerificationResult.Valid)
    }

    /**
     * TEST 5: Device Transfer
     * When a device transfer occurs:
     * - Old terminal is marked TRANSFERRED and its license is invalidated.
     * - New terminal receives a unique cryptographically bound license.
     */
    @Test
    fun testDeviceTransfer() {
        val oldTerminalId = "INST-OLD-PHONE-TRANSFER-TEST"
        val newTerminalId = "INST-NEW-TABLET-TRANSFER-TEST"

        val originalLicense = platformEngine.generateLicense(
            appId = UniversalLicensePlatformEngine.APP_SENTRY_STORE_POS,
            customerId = "CUST-1001",
            installationId = oldTerminalId,
            durationDays = 90,
            planNameOverride = "3 Months Retail Pro"
        )

        // Transfer license from old device to new device
        val transferredLicense = platformEngine.transferDevice(
            licenseId = originalLicense.licenseId,
            newInstallationId = newTerminalId,
            performedBy = "Master Developer"
        )

        assertNotNull("Transferred license must be generated", transferredLicense)

        // Old terminal license must be invalidated
        val oldTerminalResult = platformEngine.verifyLicenseCode(
            UniversalLicensePlatformEngine.APP_SENTRY_STORE_POS,
            oldTerminalId,
            originalLicense.licenseCode
        )
        assertTrue("Old device must no longer verify after transfer",
            oldTerminalResult !is LicenseVerificationResult.Valid)

        // New terminal license must be valid
        val newTerminalResult = platformEngine.verifyLicenseCode(
            UniversalLicensePlatformEngine.APP_SENTRY_STORE_POS,
            newTerminalId,
            transferredLicense!!.licenseCode
        )
        assertTrue("New device must successfully verify with newly provisioned license",
            newTerminalResult is LicenseVerificationResult.Valid)
    }

    /**
     * TEST 6: No Hardcoded Bypass
     * Verifies that common backdoors or bypass codes (e.g. 1234, 0000, 9999, phone numbers) are rejected.
     */
    @Test
    fun testNoHardcodedBypass() {
        val disallowedBypassCodes = listOf(
            "1234",
            "0000",
            "9999",
            "03080018035",
            "ADMIN",
            "SUPERADMIN",
            "BYPASS",
            "ACTV-BYPASS",
            "UNLOCK",
            "",
            "   "
        )

        runBlocking {
            activationManager.resetActivation()

            for (code in disallowedBypassCodes) {
                var wasSuccess = false
                activationManager.activateWithCode(code) { _, _, isSuccess ->
                    wasSuccess = isSuccess
                }
                assertFalse("Code '$code' must NOT be accepted as a bypass", wasSuccess)
                assertFalse("App must remain unactivated after attempting bypass code '$code'",
                    activationManager.isActivated())
            }
        }
    }

    /**
     * TEST 7: Database Safety
     * Ensures activation state resets and license state checks do not damage the Room database.
     */
    @Test
    fun testDatabaseSafety() {
        val db = AppDatabase.getDatabase(context)
        assertNotNull("Room database must initialize cleanly", db)

        // Activation reset must not throw or alter database integrity
        activationManager.resetActivation()
        assertFalse(activationManager.isActivated())

        // Database remains accessible
        runBlocking {
            val products = db.productDao().getAllProducts()
            assertNotNull(products)
        }
    }
}
