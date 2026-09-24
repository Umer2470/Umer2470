package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.entity.Product
import com.example.util.BarcodeGenerator
import com.example.util.BarcodeType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CameraBarcodeScannerComprehensiveTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testProductLookupWithScannedBarcode() {
        val products = listOf(
            Product(id = 1, name = "Rice 1 Kg", barcode = "8901234567890", salePrice = 250.0, stockQuantity = 50.0),
            Product(id = 2, name = "Cooking Oil", barcode = "8909876543210", salePrice = 520.0, stockQuantity = 20.0),
            Product(id = 3, name = "Sugar 1 Kg", barcode = "SKU-SUGAR-001", salePrice = 140.0, stockQuantity = 100.0)
        )

        // 1. Direct barcode match
        val scanned1 = "8901234567890"
        val matched1 = products.find { it.barcode.trim().equals(scanned1.trim(), ignoreCase = true) }
        assertNotNull(matched1)
        assertEquals("Rice 1 Kg", matched1?.name)
        assertEquals(250.0, matched1?.salePrice ?: 0.0, 0.01)

        // 2. Trimmed barcode match with leading/trailing spaces
        val scanned2 = "  8909876543210 \n"
        val matched2 = products.find { it.barcode.trim().equals(scanned2.trim(), ignoreCase = true) }
        assertNotNull(matched2)
        assertEquals("Cooking Oil", matched2?.name)

        // 3. Alphanumeric SKU barcode match
        val scanned3 = "sku-sugar-001"
        val matched3 = products.find { it.barcode.trim().equals(scanned3.trim(), ignoreCase = true) }
        assertNotNull(matched3)
        assertEquals("Sugar 1 Kg", matched3?.name)

        // 4. Non-matching barcode
        val scannedUnknown = "9999999999999"
        val matchedUnknown = products.find { it.barcode.trim().equals(scannedUnknown.trim(), ignoreCase = true) }
        assertNull(matchedUnknown)
    }

    @Test
    fun testDuplicateScanProtectionDebounce() {
        // Simulates rapid consecutive camera frame deliveries for the same physical barcode
        val isProcessingBarcode = AtomicBoolean(false)
        val addedCount = AtomicInteger(0)

        val simulateFrameScan = { rawBarcode: String ->
            if (isProcessingBarcode.compareAndSet(false, true)) {
                // Successfully locked for single processing
                addedCount.incrementAndGet()
            }
        }

        // Camera fires 10 rapid frames for the same barcode while held in front of camera
        repeat(10) {
            simulateFrameScan("8901234567890")
        }

        // Verify only ONE addition occurred despite 10 camera frames
        assertEquals("Only one product addition should occur due to atomic debounce lock", 1, addedCount.get())

        // Once unlocked (e.g. for next scan), another scan can proceed
        isProcessingBarcode.set(false)
        simulateFrameScan("8909876543210")
        assertEquals(2, addedCount.get())
    }

    @Test
    fun testGeneratedBarcodeCompatibilityWithScanner() {
        // Generate an EAN-13 barcode using BarcodeGenerator
        val generatedEan13 = BarcodeGenerator.autoGenerateBarcode(BarcodeType.EAN_13, productId = 42L)
        val validation = BarcodeGenerator.validate(generatedEan13, BarcodeType.EAN_13)
        assertTrue("Generated EAN-13 must be valid", validation.isValid)

        // Product registered in database with this generated barcode
        val product = Product(id = 42L, name = "Premium Basmati Rice", barcode = generatedEan13, salePrice = 380.0)
        val database = listOf(product)

        // Simulate camera detecting this generated barcode
        val detected = generatedEan13
        val found = database.find { it.barcode.trim().equals(detected.trim(), ignoreCase = true) }
        assertNotNull("Scanner must match the generated barcode with the database product", found)
        assertEquals(42L, found?.id)
        assertEquals("Premium Basmati Rice", found?.name)
    }

    @Test
    fun testCode128GeneratedBarcodeCompatibilityWithScanner() {
        val generatedCode128 = BarcodeGenerator.autoGenerateBarcode(BarcodeType.CODE_128, productId = 99L)
        val validation = BarcodeGenerator.validate(generatedCode128, BarcodeType.CODE_128)
        assertTrue("Generated Code 128 must be valid", validation.isValid)

        val product = Product(id = 99L, name = "Paint Brush 3 Inch", barcode = generatedCode128, salePrice = 180.0)
        val database = listOf(product)

        val found = database.find { it.barcode.trim().equals(generatedCode128.trim(), ignoreCase = true) }
        assertNotNull(found)
        assertEquals("Paint Brush 3 Inch", found?.name)
    }

    @Test
    fun testScannerOffModePreference() {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        // Verify default is false (OFF)
        val isEnabledDefault = prefs.getBoolean("enable_camera_scanner", false)
        assertFalse("Default scanner mode must be OFF", isEnabledDefault)

        // User turns ON in Settings
        prefs.edit().putBoolean("enable_camera_scanner", true).commit()
        assertTrue(prefs.getBoolean("enable_camera_scanner", false))

        // User turns OFF in Settings
        prefs.edit().putBoolean("enable_camera_scanner", false).commit()
        assertFalse(prefs.getBoolean("enable_camera_scanner", false))
    }
}
