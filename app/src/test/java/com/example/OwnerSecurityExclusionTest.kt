package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.api.security.OwnerSecurityManager
import com.example.ui.viewmodel.StoreViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OwnerSecurityExclusionTest {

    private lateinit var context: Context
    private lateinit var ownerSecurityManager: OwnerSecurityManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        ownerSecurityManager = OwnerSecurityManager.getInstance(context)
    }

    @Test
    fun testHardcoded9999AndPhoneBypassesStrictlyRejected() {
        // Old fixed backdoor 9999 must never unlock Owner / Developer Center
        assertFalse(
            "Hardcoded bypass 9999 must be strictly rejected",
            ownerSecurityManager.verifyCredential("9999")
        )
        // Contact number / phone bypass must be strictly rejected
        assertFalse(
            "Contact number bypass must be strictly rejected",
            ownerSecurityManager.verifyCredential("03080018035")
        )
    }

    @Test
    fun testFirstTimeOwnerSecuritySetupFlow() {
        // Reset state for isolation
        context.getSharedPreferences("sentry_store_owner_security", Context.MODE_PRIVATE)
            .edit().remove("key_owner_pin_hash").putBoolean("key_is_configured", false).apply()

        assertFalse("Owner Security must not be configured initially without setup", ownerSecurityManager.isOwnerSecurityConfigured())

        // Trivial or unauthorized bypass codes must be rejected during setup
        assertFalse("Cannot setup 9999 as owner PIN", ownerSecurityManager.setupOwnerSecurity("9999"))
        assertFalse("Cannot setup 1234 as owner PIN", ownerSecurityManager.setupOwnerSecurity("1234"))
        assertFalse("Cannot setup 0000 as owner PIN", ownerSecurityManager.setupOwnerSecurity("0000"))
        assertFalse("Cannot setup phone number as owner PIN", ownerSecurityManager.setupOwnerSecurity("03080018035"))
        assertFalse("Short PIN must be rejected", ownerSecurityManager.setupOwnerSecurity("12"))

        // Legitimate dedicated PIN setup
        val setupSuccess = ownerSecurityManager.setupOwnerSecurity("8765")
        assertTrue("Setup of valid dedicated PIN must succeed", setupSuccess)
        assertTrue("Owner Security must now be configured", ownerSecurityManager.isOwnerSecurityConfigured())

        // Verification of established PIN
        assertTrue("Configured Owner PIN 8765 must be accepted", ownerSecurityManager.verifyCredential("8765"))
        assertFalse("9999 must still be rejected", ownerSecurityManager.verifyCredential("9999"))
    }

    @Test
    fun testDedicatedOwnerSecurityKeyAccepted() {
        val securityKey = ownerSecurityManager.getSecurityKey()
        assertNotNull("Owner Security Key should not be null", securityKey)
        assertTrue("Owner Security Key should start with OWNER-KEY-", securityKey.startsWith("OWNER-KEY-"))

        // Must verify with the dedicated Security Key
        assertTrue(
            "Dedicated Owner Security Key must unlock Owner access",
            ownerSecurityManager.verifyCredential(securityKey)
        )
    }

    @Test
    fun testCashierAndAdminAndActivationCodesRejected() {
        ownerSecurityManager.setupOwnerSecurity("8765")

        // Cashier PINs
        assertFalse("Cashier PIN 1234 must be strictly rejected", ownerSecurityManager.verifyCredential("1234"))
        assertFalse("Cashier PIN 0000 must be strictly rejected", ownerSecurityManager.verifyCredential("0000"))
        assertFalse("Cashier PIN 1111 must be strictly rejected", ownerSecurityManager.verifyCredential("1111"))

        // Supervisor / Admin passwords
        assertFalse("Admin password must not unlock Owner Center", ownerSecurityManager.verifyCredential("admin123"))
        assertFalse("Supervisor code must not unlock Owner Center", ownerSecurityManager.verifyCredential("supervisor"))

        // Activation / License code
        assertFalse("Activation code must not unlock Owner Center", ownerSecurityManager.verifyCredential("ACTV-M1-XYZ123"))

        // Blank and spaces
        assertFalse("Blank string must be rejected", ownerSecurityManager.verifyCredential(""))
        assertFalse("Whitespace must be rejected", ownerSecurityManager.verifyCredential("   "))
    }

    @Test
    fun testUpdateOwnerSecurityPassword() {
        ownerSecurityManager.setupOwnerSecurity("8765")
        assertTrue("Current password 8765 accepted", ownerSecurityManager.verifyCredential("8765"))

        // Update password
        val updateSuccess = ownerSecurityManager.setPassword("7890")
        assertTrue("Setting new password should succeed", updateSuccess)

        // New password should be accepted
        assertTrue("New password 7890 should be accepted", ownerSecurityManager.verifyCredential("7890"))

        // Old password should be rejected
        assertFalse("Old password 8765 should now be rejected", ownerSecurityManager.verifyCredential("8765"))
        assertFalse("9999 should be rejected", ownerSecurityManager.verifyCredential("9999"))
    }

    @Test
    fun testRegenerateOwnerSecurityKey() {
        val originalKey = ownerSecurityManager.getSecurityKey()
        assertTrue(ownerSecurityManager.verifyCredential(originalKey))

        val newKey = ownerSecurityManager.regenerateSecurityKey()
        assertNotEquals("New security key must differ from original", originalKey, newKey)
        assertTrue("New key must start with OWNER-KEY-", newKey.startsWith("OWNER-KEY-"))

        // New key accepted
        assertTrue("New key must be accepted", ownerSecurityManager.verifyCredential(newKey))

        // Old key rejected
        assertFalse("Old key must be revoked and rejected", ownerSecurityManager.verifyCredential(originalKey))
    }

    @Test
    fun testBiometricsPermanentlyDisallowed() {
        assertFalse(
            "Biometric authentication must be completely disabled for Owner / Developer access",
            ownerSecurityManager.isBiometricAllowed()
        )
    }

    @Test
    fun testStoreViewModelSecurityIntegration() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = StoreViewModel(app)

        vm.setupOwnerSecurity("5678")

        // Verify Owner PIN through ViewModel
        assertTrue("ViewModel verifyOwnerSecurityCode accepts configured PIN", vm.verifyOwnerSecurityCode("5678"))
        assertTrue("ViewModel verifyOwnerSecurityCredential accepts configured PIN", vm.verifyOwnerSecurityCredential("5678"))
        assertTrue("ViewModel verifyDeveloperAuth accepts configured PIN", vm.verifyDeveloperAuth("5678"))

        // Verify Owner Security Key through ViewModel
        val key = vm.getOwnerSecurityKey()
        assertTrue("ViewModel verifyOwnerSecurityCode accepts Security Key", vm.verifyOwnerSecurityCode(key))
        assertTrue("ViewModel verifyDeveloperAuth accepts Security Key", vm.verifyDeveloperAuth(key))

        // Strict rejection of other credentials
        assertFalse("ViewModel rejects 9999 bypass", vm.verifyOwnerSecurityCode("9999"))
        assertFalse("ViewModel rejects phone bypass", vm.verifyOwnerSecurityCode("03080018035"))
        assertFalse("ViewModel rejects Cashier PIN", vm.verifyOwnerSecurityCode("1234"))
        assertFalse("ViewModel rejects Admin username", vm.verifyOwnerSecurityCode("admin"))
        assertFalse("ViewModel rejects Activation Code", vm.verifyOwnerSecurityCode("ACTV-M3-TEST"))
    }
}
