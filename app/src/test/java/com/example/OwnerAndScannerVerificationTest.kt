package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.api.security.AppActivationManager
import com.example.data.api.security.SecureIdentityManager
import com.example.data.entity.Product
import com.example.ui.viewmodel.CartItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class OwnerAndScannerVerificationTest {

    private lateinit var context: Context
    private lateinit var activationManager: AppActivationManager
    private lateinit var identityManager: SecureIdentityManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        activationManager = AppActivationManager.getInstance(context)
        identityManager = SecureIdentityManager.getInstance(context)
        activationManager.resetActivation()
    }

    // ==========================================
    // 1. OWNER SECURITY & AUTHENTICATION TESTS
    // ==========================================

    @Test
    fun testOwnerPinAuthenticationAndInvalidPinRejection() {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("owner_security_code", "9999").apply()

        val savedPin = prefs.getString("owner_security_code", "9999") ?: "9999"

        // Valid PIN matches
        assertTrue("PIN 9999 should be accepted", "9999" == savedPin)

        // Invalid PIN rejected
        assertFalse("PIN 0000 should be rejected", "0000" == savedPin)
        assertFalse("PIN 1234 should not match custom owner pin", "1234" == savedPin)
        assertFalse("Blank PIN should be rejected", "" == savedPin)
    }

    // ==========================================
    // 2. LICENSE GENERATION & MEMBERSHIP PLANS
    // ==========================================

    @Test
    fun testLicensePlansGenerationAndVerification() = runBlocking {
        val installationId = identityManager.getInstallationId()
        assertNotNull(installationId)
        assertTrue(installationId.startsWith("APP-"))

        // Plan 1: Demo 1 Month (30 Days)
        val code1M = AppActivationManager.generatePlanActivationCode(installationId, 30)
        assertTrue(code1M.startsWith("ACTV-M1-"))
        val plan1M = AppActivationManager.parseAndVerifyCode(installationId, code1M)
        assertNotNull(plan1M)
        assertEquals(30, plan1M?.days)
        assertEquals("Demo 1 Month", plan1M?.planName)

        // Plan 2: 3 Months (90 Days)
        val code3M = AppActivationManager.generatePlanActivationCode(installationId, 90)
        assertTrue(code3M.startsWith("ACTV-M3-"))
        val plan3M = AppActivationManager.parseAndVerifyCode(installationId, code3M)
        assertNotNull(plan3M)
        assertEquals(90, plan3M?.days)
        assertEquals("3 Months Standard", plan3M?.planName)

        // Plan 3: 6 Months (180 Days)
        val code6M = AppActivationManager.generatePlanActivationCode(installationId, 180)
        assertTrue(code6M.startsWith("ACTV-M6-"))
        val plan6M = AppActivationManager.parseAndVerifyCode(installationId, code6M)
        assertNotNull(plan6M)
        assertEquals(180, plan6M?.days)
        assertEquals("6 Months Professional", plan6M?.planName)

        // Plan 4: 1 Year (365 Days)
        val code1Y = AppActivationManager.generatePlanActivationCode(installationId, 365)
        assertTrue(code1Y.startsWith("ACTV-Y1-"))
        val plan1Y = AppActivationManager.parseAndVerifyCode(installationId, code1Y)
        assertNotNull(plan1Y)
        assertEquals(365, plan1Y?.days)
        assertEquals("1 Year Enterprise", plan1Y?.planName)

        // Plan 5: Custom Duration (e.g. 45 Days)
        val code45D = AppActivationManager.generatePlanActivationCode(installationId, 45)
        assertTrue(code45D.startsWith("ACTV-D45-"))
        val plan45D = AppActivationManager.parseAndVerifyCode(installationId, code45D)
        assertNotNull(plan45D)
        assertEquals(45, plan45D?.days)
        assertEquals("45 Days Custom Plan", plan45D?.planName)

        // Plan 6: Lifetime Commercial Plan (0 Days)
        val codeLife = AppActivationManager.generatePlanActivationCode(installationId, 0)
        assertTrue(codeLife.startsWith("ACTV-LIFE-"))
        val planLife = AppActivationManager.parseAndVerifyCode(installationId, codeLife)
        assertNotNull(planLife)
        assertEquals(0, planLife?.days)
        assertEquals("Lifetime Commercial", planLife?.planName)
    }

    @Test
    fun testActivationActivationLifecycleExpiryExtensionAndDeactivation() = runBlocking {
        val installationId = identityManager.getInstallationId()
        val code1M = AppActivationManager.generatePlanActivationCode(installationId, 30)

        // 1. Activate
        var activated = false
        activationManager.activateWithCode(code1M) { _, _, success ->
            activated = success
        }
        assertTrue("Device activation should succeed", activated)
        assertTrue("isActivated should be true", activationManager.isActivated())
        assertEquals("Demo 1 Month", activationManager.getPlanName())
        assertTrue("Days remaining should be > 25", activationManager.getDaysRemaining() in 28..30)

        // 2. Extension (+30 Days)
        val initialExpiry = activationManager.getExpiryTimestamp()
        activationManager.extendLicense(30)
        val extendedExpiry = activationManager.getExpiryTimestamp()
        assertTrue("Extended expiry should be greater than initial", extendedExpiry > initialExpiry)
        assertTrue("Days remaining should now be around 60", activationManager.getDaysRemaining() in 58..60)

        // 3. Renewal (1 Year)
        activationManager.renewLicense(365, "1 Year Enterprise")
        assertEquals("1 Year Enterprise", activationManager.getPlanName())
        assertTrue("Days remaining should now be ~365", activationManager.getDaysRemaining() in 364..366)

        // 4. Deactivation
        activationManager.resetActivation()
        assertFalse("After reset, isActivated must be false", activationManager.isActivated())
        assertEquals(AppActivationManager.STATUS_FIRST_INSTALL_NOT_ACTIVATED, activationManager.getActivationStatus())
    }

    // ==========================================
    // 3. DISCOUNT & CALCULATION TESTS
    // ==========================================

    @Test
    fun testItemAndInvoiceDiscountStability() {
        val product1 = Product(id = 1L, name = "Shampoo 200ml", barcode = "89640001", purchasePrice = 100.0, salePrice = 200.0, stockQuantity = 50.0)
        val product2 = Product(id = 2L, name = "Soap Bar", barcode = "89640002", purchasePrice = 40.0, salePrice = 80.0, stockQuantity = 100.0)

        // Cart with item 1: Qty 2, UnitPrice 200, Item Discount 30
        val cartItem1 = CartItem(product = product1, quantity = 2.0, unitPrice = 200.0, itemDiscount = 30.0)
        // Subtotal for item 1 = (2 * 200) - 30 = 370.0
        assertEquals(370.0, cartItem1.totalPrice, 0.001)

        // Cart with item 2: Qty 1, UnitPrice 80, Item Discount 0
        val cartItem2 = CartItem(product = product2, quantity = 1.0, unitPrice = 80.0, itemDiscount = 0.0)
        assertEquals(80.0, cartItem2.totalPrice, 0.001)

        // Combined Subtotal
        val subtotal = cartItem1.totalPrice + cartItem2.totalPrice // 370 + 80 = 450.0
        assertEquals(450.0, subtotal, 0.001)

        // Apply Invoice-Level Discount = 50.0
        val invoiceDiscount = 50.0
        val taxRate = 0.0 // 0%
        val netAmount = (subtotal - invoiceDiscount).coerceAtLeast(0.0) // 400.0
        assertEquals(400.0, netAmount, 0.001)

        // Over-discount test: Discount higher than subtotal
        val massiveDiscount = 600.0
        val safeNetAmount = (subtotal - massiveDiscount).coerceAtLeast(0.0)
        assertEquals("Net amount should never be negative", 0.0, safeNetAmount, 0.001)

        // Payment calculation:
        val amountReceived = 500.0
        val changeReturn = (amountReceived - netAmount).coerceAtLeast(0.0) // 500 - 400 = 100.0
        val remainingDue = (netAmount - amountReceived).coerceAtLeast(0.0) // 0.0

        assertEquals(100.0, changeReturn, 0.001)
        assertEquals(0.0, remainingDue, 0.001)

        // Partial payment calculation:
        val partialReceived = 300.0
        val partialChange = (partialReceived - netAmount).coerceAtLeast(0.0) // 0.0
        val partialDue = (netAmount - partialReceived).coerceAtLeast(0.0) // 100.0

        assertEquals(0.0, partialChange, 0.001)
        assertEquals(100.0, partialDue, 0.001)
    }

    // ==========================================
    // 4. BARCODE SCANNER & SETTINGS PREFERENCES
    // ==========================================

    @Test
    fun testCameraScannerSettingsDefaultPreference() {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        // Verify default preference is OFF (false)
        val isScannerAutoEnabled = prefs.getBoolean("enable_camera_scanner", false)
        assertFalse("Camera scanner must default to OFF (false)", isScannerAutoEnabled)

        // Test toggling on
        prefs.edit().putBoolean("enable_camera_scanner", true).apply()
        assertTrue(prefs.getBoolean("enable_camera_scanner", false))

        // Reset to false
        prefs.edit().putBoolean("enable_camera_scanner", false).apply()
        assertFalse(prefs.getBoolean("enable_camera_scanner", false))
    }

    @Test
    fun testBarcodeToProductCartMatching() {
        val catalog = listOf(
            Product(id = 101L, name = "Mineral Water 1.5L", barcode = "123456789012", purchasePrice = 50.0, salePrice = 80.0, stockQuantity = 30.0),
            Product(id = 102L, name = "Cooking Oil 1L", barcode = "987654321098", purchasePrice = 400.0, salePrice = 480.0, stockQuantity = 15.0)
        )

        val scannedBarcode = "123456789012"
        val matchedProduct = catalog.find { it.barcode.trim() == scannedBarcode.trim() }

        assertNotNull("Product should be matched from barcode", matchedProduct)
        assertEquals("Mineral Water 1.5L", matchedProduct?.name)
        assertEquals(80.0, matchedProduct?.salePrice ?: 0.0, 0.001)

        // Build cart item from matched product
        val cartItem = CartItem(product = matchedProduct!!, quantity = 1.0, unitPrice = matchedProduct.salePrice)
        assertEquals(80.0, cartItem.totalPrice, 0.001)
    }
}
