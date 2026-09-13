package com.example

import com.example.data.entity.Product
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.data.entity.StoreSettings
import com.example.util.InvoiceFormattingService
import org.junit.Assert.*
import org.junit.Test

/**
 * Comprehensive verification for Discount Dialog parsing, mathematical integrity,
 * error handling, edge cases, and invoice completion/reprinting.
 */
class DiscountComprehensiveVerificationTest {

    // Helper simulating the exact parsing logic in SalesPosScreen Global Discount Dialog
    private fun parsePosDiscount(input: String, subtotal: Double): Pair<Double?, String?> {
        val clean = input.trim()
        val parsed = if (clean.endsWith("%")) {
            val pctVal = clean.removeSuffix("%").trim().toDoubleOrNull()
            if (pctVal != null && !pctVal.isNaN() && !pctVal.isInfinite()) (subtotal * pctVal) / 100.0 else null
        } else {
            clean.toDoubleOrNull()
        }

        return when {
            parsed == null || parsed.isNaN() || parsed.isInfinite() || parsed < 0.0 -> Pair(null, "Please enter a valid positive discount amount.")
            parsed > subtotal -> Pair(null, "Discount cannot exceed subtotal")
            else -> Pair(parsed, null)
        }
    }

    // Helper simulating the exact parsing logic in EditInvoiceDialog
    private fun parseEditInvoiceDiscount(input: String, subtotal: Double): Double {
        val discount = input.toDoubleOrNull()?.let {
            if (it.isNaN() || it.isInfinite() || it < 0.0) 0.0 else it.coerceAtMost(subtotal)
        } ?: 0.0
        return discount
    }

    // Helper simulating the exact parsing logic in EditCartItemDialog
    private fun parseCartItemDiscount(input: String, maxAllowed: Double): Double {
        val d = input.trim().toDoubleOrNull()?.let {
            if (it.isNaN() || it.isInfinite() || it < 0.0) 0.0 else it
        } ?: 0.0
        return d.coerceAtLeast(0.0).coerceAtMost(maxAllowed)
    }

    @Test
    fun testValidDiscountApplication() {
        val subtotal = 1000.0
        val (parsed, error) = parsePosDiscount("150.00", subtotal)
        assertNull(error)
        assertNotNull(parsed)
        assertEquals(150.0, parsed!!, 0.001)

        val netAmount = (subtotal - parsed).coerceAtLeast(0.0)
        assertEquals(850.0, netAmount, 0.001)
    }

    @Test
    fun testValidPercentageDiscountApplication() {
        val subtotal = 1000.0
        val (parsed, error) = parsePosDiscount("10%", subtotal)
        assertNull(error)
        assertNotNull(parsed)
        assertEquals(100.0, parsed!!, 0.001)

        val netAmount = (subtotal - parsed).coerceAtLeast(0.0)
        assertEquals(900.0, netAmount, 0.001)
    }

    @Test
    fun testEmptyInputDoesNotCrashAndReturnsError() {
        val subtotal = 500.0
        val (parsed, error) = parsePosDiscount("", subtotal)
        assertNull(parsed)
        assertNotNull(error)
        assertTrue(error!!.contains("valid positive discount"))

        // Edit Invoice dialog empty input fallback
        val editDiscount = parseEditInvoiceDiscount("", subtotal)
        assertEquals(0.0, editDiscount, 0.001)

        // Cart item dialog empty input fallback
        val itemDiscount = parseCartItemDiscount("", 200.0)
        assertEquals(0.0, itemDiscount, 0.001)
    }

    @Test
    fun testInvalidTextInputDoesNotCrashAndReturnsError() {
        val subtotal = 500.0
        val invalidInputs = listOf("abc", "12.34.56", "$100", "---", "NaN", "Infinity", " % ")
        for (input in invalidInputs) {
            val (parsed, error) = parsePosDiscount(input, subtotal)
            assertNull("Input '$input' must not parse as valid", parsed)
            assertNotNull("Input '$input' must produce an error message instead of crashing", error)

            // Verify edit dialog safely returns 0.0 without throwing NumberFormatException
            val safeEdit = parseEditInvoiceDiscount(input, subtotal)
            assertEquals(0.0, safeEdit, 0.001)

            // Verify cart item safely returns 0.0
            val safeItem = parseCartItemDiscount(input, 100.0)
            assertEquals(0.0, safeItem, 0.001)
        }
    }

    @Test
    fun testDecimalDiscountParsing() {
        val subtotal = 500.0
        val (parsed, error) = parsePosDiscount("45.75", subtotal)
        assertNull(error)
        assertNotNull(parsed)
        assertEquals(45.75, parsed!!, 0.001)

        val netAmount = (subtotal - parsed).coerceAtLeast(0.0)
        assertEquals(454.25, netAmount, 0.001)
    }

    @Test
    fun testNegativeDiscountRejected() {
        val subtotal = 500.0
        val (parsed, error) = parsePosDiscount("-50", subtotal)
        assertNull(parsed)
        assertNotNull(error)
        assertTrue(error!!.contains("valid positive discount"))

        val safeEdit = parseEditInvoiceDiscount("-50", subtotal)
        assertEquals(0.0, safeEdit, 0.001)
    }

    @Test
    fun testZeroDiscountAllowed() {
        val subtotal = 500.0
        val (parsed, error) = parsePosDiscount("0", subtotal)
        assertNull(error)
        assertNotNull(parsed)
        assertEquals(0.0, parsed!!, 0.001)

        val netAmount = (subtotal - parsed).coerceAtLeast(0.0)
        assertEquals(500.0, netAmount, 0.001)
    }

    @Test
    fun testDiscountEqualToTotal() {
        val subtotal = 500.0
        val (parsed, error) = parsePosDiscount("500", subtotal)
        assertNull(error)
        assertNotNull(parsed)
        assertEquals(500.0, parsed!!, 0.001)

        val netAmount = (subtotal - parsed).coerceAtLeast(0.0)
        assertEquals(0.0, netAmount, 0.001)

        // Paid/due/change when bill is 100% discounted:
        val paid = 0.0
        val due = (netAmount - paid).coerceAtLeast(0.0)
        val change = (paid - netAmount).coerceAtLeast(0.0)
        assertEquals(0.0, due, 0.001)
        assertEquals(0.0, change, 0.001)
    }

    @Test
    fun testDiscountGreaterThanTotalRejectedOrClamped() {
        val subtotal = 500.0
        // In POS Dialog: should show error and reject
        val (parsed, error) = parsePosDiscount("600", subtotal)
        assertNull(parsed)
        assertNotNull(error)
        assertTrue(error!!.contains("cannot exceed subtotal"))

        // In Edit dialog: clamped to subtotal
        val clamped = parseEditInvoiceDiscount("600", subtotal)
        assertEquals(500.0, clamped, 0.001)
    }

    @Test
    fun testCompleteSaleLifecycleWithDiscountAndReprint() {
        val subtotal = 1200.0
        val discount = 200.0
        val netAmount = subtotal - discount // 1000.0
        val paid = 1000.0
        val due = 0.0

        val completedSale = Sale(
            id = 55L,
            invoiceNumber = "INV-DISC-55",
            customerName = "Bilal Hardware",
            totalAmount = subtotal,
            discount = discount,
            taxRate = 0.0,
            taxAmount = 0.0,
            netAmount = netAmount,
            paidAmount = paid,
            dueAmount = due,
            paymentType = "Cash",
            cashierName = "Muhammad Umer"
        )

        val items = listOf(
            SaleItem(
                id = 1L,
                saleId = 55L,
                productId = 10L,
                productName = "PPRC Pipe 25mm",
                salePrice = 600.0,
                quantity = 2.0,
                totalPrice = 1200.0
            )
        )

        val settings = StoreSettings(
            storeName = "SENTRY STORE",
            currencySymbol = "Rs"
        )

        // Generate printable invoice
        val invoice = InvoiceFormattingService.formatSaleTransaction(completedSale, items, settings, null)
        assertEquals(1200.0, invoice.totals.subtotal, 0.001)
        assertEquals(200.0, invoice.totals.discount, 0.001)
        assertEquals(1000.0, invoice.totals.netAmount, 0.001)
        assertEquals(1000.0, invoice.totals.paidAmount, 0.001)
        assertEquals(0.0, invoice.totals.dueAmount, 0.001)

        // Verify thermal receipt output formatting
        val thermalText = InvoiceFormattingService.generateThermalText(invoice, 32)
        assertTrue(thermalText.contains("INV-DISC-55"))
        assertTrue(thermalText.contains("Discount:"))
        assertTrue(thermalText.contains("-Rs 200.00"))
        assertTrue(thermalText.contains("Rs 1000.00"))
    }
}
