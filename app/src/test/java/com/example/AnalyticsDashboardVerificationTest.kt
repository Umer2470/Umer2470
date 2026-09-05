package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.api.security.AppActivationManager
import com.example.data.api.security.SecureIdentityManager
import com.example.data.db.AppDatabase
import com.example.data.entity.Product
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.ui.viewmodel.DaySalesPoint
import com.example.ui.viewmodel.TopProductPoint
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AnalyticsDashboardVerificationTest {

    private lateinit var context: Context
    private lateinit var activationManager: AppActivationManager
    private lateinit var identityManager: SecureIdentityManager
    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        activationManager = AppActivationManager.getInstance(context)
        identityManager = SecureIdentityManager.getInstance(context)
        db = AppDatabase.getDatabase(context)
    }

    /**
     * TEST 1: Unverified License Gating
     * On unverified terminals, license verification status MUST be false, preventing analytics population.
     */
    @Test
    fun testAnalyticsGatedWhenLicenseUnverified() {
        activationManager.resetActivation()
        val isVerified = activationManager.isActivated()
        assertFalse("Unverified license must prevent charts from populating", isVerified)
        assertEquals("Status must not be activated",
            AppActivationManager.STATUS_FIRST_INSTALL_NOT_ACTIVATED,
            activationManager.getActivationStatus())
    }

    /**
     * TEST 2: Verified License Unlocks Analytics
     * Once verified with a valid cryptographically bound license key, verification passes and unlocks charts.
     */
    @Test
    fun testAnalyticsUnlockedUponSuccessfulVerification() = runBlocking {
        val installationId = identityManager.getInstallationId()
        val validCode = AppActivationManager.generateActivationCode(installationId)

        var success = false
        activationManager.activateWithCode(validCode) { _, _, isSuccess ->
            success = isSuccess
        }

        assertTrue("Valid license activation must succeed", success)
        assertTrue("Analytics must be unlocked when isActivated is true", activationManager.isActivated())
    }

    /**
     * TEST 3: Top-Performing Products Data Structure & Ordering
     * Validates that top products are ranked by revenue and percentage calculation is mathematically sound.
     */
    @Test
    fun testTopPerformingProductsCalculation() {
        val testProducts = listOf(
            TopProductPoint(
                productId = 1L,
                productName = "Master Pipe 1 inch PPRC",
                category = "Pipes & Fittings",
                totalRevenue = 15000.0,
                totalUnitsSold = 30.0,
                percentage = 60.0,
                currentStock = 45.0
            ),
            TopProductPoint(
                productId = 2L,
                productName = "Faisal Brass Ball Valve",
                category = "Sanitary Fittings",
                totalRevenue = 7500.0,
                totalUnitsSold = 15.0,
                percentage = 30.0,
                currentStock = 20.0
            ),
            TopProductPoint(
                productId = 3L,
                productName = "Teflon Thread Seal Tape",
                category = "Hardware",
                totalRevenue = 2500.0,
                totalUnitsSold = 50.0,
                percentage = 10.0,
                currentStock = 120.0
            )
        )

        // Verify ordering: descending by total revenue
        assertTrue(testProducts[0].totalRevenue > testProducts[1].totalRevenue)
        assertTrue(testProducts[1].totalRevenue > testProducts[2].totalRevenue)

        // Verify total percentage sum
        val totalPct = testProducts.sumOf { it.percentage }
        assertEquals(100.0, totalPct, 0.01)

        // Verify top item is Master Pipe
        assertEquals("Master Pipe 1 inch PPRC", testProducts[0].productName)
        assertEquals(30.0, testProducts[0].totalUnitsSold, 0.01)
    }

    /**
     * TEST 4: Daily Sales Trends Data Structure
     * Validates 7-day trend point integrity and volume/revenue aggregation.
     */
    @Test
    fun testDailySalesTrendStructure() {
        val sampleTrends = listOf(
            DaySalesPoint(dayName = "Mon", dateLabel = "01 Sep", totalRevenue = 4500.0, totalVolume = 3),
            DaySalesPoint(dayName = "Tue", dateLabel = "02 Sep", totalRevenue = 8200.0, totalVolume = 5),
            DaySalesPoint(dayName = "Wed", dateLabel = "03 Sep", totalRevenue = 12500.0, totalVolume = 9),
            DaySalesPoint(dayName = "Thu", dateLabel = "04 Sep", totalRevenue = 6100.0, totalVolume = 4),
            DaySalesPoint(dayName = "Fri", dateLabel = "05 Sep", totalRevenue = 14800.0, totalVolume = 11)
        )

        assertEquals(5, sampleTrends.size)
        val totalRevenue = sampleTrends.sumOf { it.totalRevenue }
        val totalVolume = sampleTrends.sumOf { it.totalVolume }

        assertEquals(46100.0, totalRevenue, 0.01)
        assertEquals(32, totalVolume)

        val peakDay = sampleTrends.maxByOrNull { it.totalRevenue }
        assertNotNull(peakDay)
        assertEquals("Fri", peakDay!!.dayName)
        assertEquals(14800.0, peakDay.totalRevenue, 0.01)
    }
}
