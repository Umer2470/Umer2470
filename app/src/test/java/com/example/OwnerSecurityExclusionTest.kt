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
    fun testDedicatedOwnerPinAccepted() {
        // Initial PIN is verified via salted hash in SharedPreferences (not hardcoded)
        assertTrue(
            "Initial dedicated Owner PIN (9999) must be accepted",
            ownerSecurityManager.verifyCredential("9999")
        )
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
        // Update password
        val updateSuccess = ownerSecurityManager.setPassword("7890")
        assertTrue("Setting new password should succeed", updateSuccess)

        // New password should be accepted
        assertTrue("New password 7890 should be accepted", ownerSecurityManager.verifyCredential("7890"))

        // Old password should be rejected
        assertFalse("Old password 9999 should now be rejected", ownerSecurityManager.verifyCredential("9999"))

        // Restore to 9999 for test consistency
        ownerSecurityManager.setPassword("9999")
        assertTrue(ownerSecurityManager.verifyCredential("9999"))
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

        // Verify Owner PIN through ViewModel
        assertTrue("ViewModel verifyOwnerSecurityCode accepts PIN", vm.verifyOwnerSecurityCode("9999"))
        assertTrue("ViewModel verifyOwnerSecurityCredential accepts PIN", vm.verifyOwnerSecurityCredential("9999"))

        // Verify Owner Security Key through ViewModel
        val key = vm.getOwnerSecurityKey()
        assertTrue("ViewModel verifyOwnerSecurityCode accepts Security Key", vm.verifyOwnerSecurityCode(key))
        assertTrue("ViewModel verifyDeveloperAuth accepts Security Key", vm.verifyDeveloperAuth(key))

        // Strict rejection of other credentials
        assertFalse("ViewModel rejects Cashier PIN", vm.verifyOwnerSecurityCode("1234"))
        assertFalse("ViewModel rejects Admin username", vm.verifyOwnerSecurityCode("admin"))
        assertFalse("ViewModel rejects Activation Code", vm.verifyOwnerSecurityCode("ACTV-M3-TEST"))
    }
}
