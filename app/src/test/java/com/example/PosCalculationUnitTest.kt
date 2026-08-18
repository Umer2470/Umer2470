package com.example

import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.data.entity.StoreSettings
import com.example.util.InvoiceFormattingService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class PosCalculationUnitTest {

    @Test
    fun testCartItemSubtotalCalculation() {
        val unitPrice = 150.0
        val quantity = 3
        val discount = 50.0
        val taxRatePercent = 5.0 // 5%

        val rawTotal = unitPrice * quantity
        val afterDiscount = rawTotal - discount
        val taxAmount = afterDiscount * (taxRatePercent / 100.0)
        val finalTotal = afterDiscount + taxAmount

        assertEquals(450.0, rawTotal, 0.001)
        assertEquals(400.0, afterDiscount, 0.001)
        assertEquals(20.0, taxAmount, 0.001)
        assertEquals(420.0, finalTotal, 0.001)
    }

    @Test
    fun testInventoryStockDeduction() {
        val initialStock = 25
        val soldQuantity = 5
        val remainingStock = initialStock - soldQuantity

        assertEquals(20, remainingStock)
    }

    @Test
    fun testInvoiceFormattingServiceSaleTransaction() {
        val settings = StoreSettings(
            storeName = "Super Store",
            ownerName = "John Doe",
            phone = "1234567890",
            address = "123 Main St",
            currencySymbol = "$"
        )

        val sale = Sale(
            id = 1001L,
            invoiceNumber = "INV-1001",
            customerName = "Jane Smith",
            totalAmount = 200.0,
            discount = 20.0,
            netAmount = 180.0,
            paidAmount = 200.0,
            dueAmount = 0.0,
            paymentType = "Cash"
        )

        val items = listOf(
            SaleItem(
                id = 1L,
                saleId = 1001L,
                productId = 5L,
                productName = "Product A",
                quantity = 2.0,
                unit = "Pcs",
                purchasePrice = 50.0,
                salePrice = 100.0,
                totalPrice = 200.0
            )
        )

        val printableInvoice = InvoiceFormattingService.formatSaleTransaction(
            sale = sale,
            items = items,
            settings = settings
        )

        assertNotNull(printableInvoice)
        assertEquals("Super Store", printableInvoice.header.storeName)
        assertEquals("Jane Smith", printableInvoice.customer.name)
        assertEquals(1, printableInvoice.items.size)
        assertEquals("Product A", printableInvoice.items[0].productName)
        assertEquals(200.0, printableInvoice.totals.subtotal, 0.001)
        assertEquals(20.0, printableInvoice.totals.discount, 0.001)
        assertEquals(180.0, printableInvoice.totals.netAmount, 0.001)
    }

    @Test
    fun testCashierProfileResolutionAndHistoricalInvoiceSafety() {
        val settingsAli = StoreSettings(
            storeName = "Al-Khair Store",
            defaultCashierName = "Ali Raza",
            defaultCashierDesignation = "Senior Cashier"
        )

        // Invoice 1 created under Ali Raza
        val sale1 = Sale(
            id = 1L,
            invoiceNumber = "INV-001",
            customerName = "Customer 1",
            totalAmount = 500.0,
            netAmount = 500.0,
            paidAmount = 500.0,
            dueAmount = 0.0,
            cashierName = "Ali Raza"
        )

        val items = listOf(
            SaleItem(id = 1L, saleId = 1L, productId = 10L, productName = "Item X", quantity = 1.0, unit = "Pcs", purchasePrice = 100.0, salePrice = 500.0, totalPrice = 500.0)
        )

        val inv1 = InvoiceFormattingService.formatSaleTransaction(sale1, items, settingsAli)
        assertEquals("Ali Raza", inv1.meta.cashierName)

        // Cashier is now changed to Muhammad Usman in settings
        val settingsUsman = settingsAli.copy(
            defaultCashierName = "Muhammad Usman",
            defaultCashierDesignation = "Shift Operator"
        )

        // Invoice 2 created under Muhammad Usman
        val sale2 = Sale(
            id = 2L,
            invoiceNumber = "INV-002",
            customerName = "Customer 2",
            totalAmount = 300.0,
            netAmount = 300.0,
            paidAmount = 300.0,
            dueAmount = 0.0,
            cashierName = "Muhammad Usman"
        )

        val inv2 = InvoiceFormattingService.formatSaleTransaction(sale2, items, settingsUsman)
        assertEquals("Muhammad Usman", inv2.meta.cashierName)

        // Historical Invoice 1 MUST continue to show Ali Raza
        val inv1Reopened = InvoiceFormattingService.formatSaleTransaction(sale1, items, settingsUsman)
        assertEquals("Ali Raza", inv1Reopened.meta.cashierName)
    }

    @Test
    fun testUnassignedCashierFallback() {
        val emptySettings = StoreSettings(
            storeName = "Test Mart",
            defaultCashierName = ""
        )

        val saleWithoutCashier = Sale(
            id = 3L,
            invoiceNumber = "INV-003",
            customerName = "Walk-in Customer",
            totalAmount = 100.0,
            netAmount = 100.0,
            paidAmount = 100.0,
            dueAmount = 0.0,
            cashierName = ""
        )

        val items = listOf(
            SaleItem(id = 1L, saleId = 3L, productId = 1L, productName = "Sample", quantity = 1.0, unit = "Pcs", purchasePrice = 50.0, salePrice = 100.0, totalPrice = 100.0)
        )

        val inv = InvoiceFormattingService.formatSaleTransaction(saleWithoutCashier, items, emptySettings)
        assertEquals("Not Assigned", inv.meta.cashierName)

        val thermalText = InvoiceFormattingService.generateThermalText(inv, paperWidthColumns = 32)
        org.junit.Assert.assertTrue(thermalText.contains("Cashier:") && thermalText.contains("Not Assigned"))
    }

    @Test
    fun testThermalReceiptCashierFormatting() {
        val settings = StoreSettings(
            storeName = "Al-Khair POS",
            defaultCashierName = "Ahmed Khan"
        )

        val sale = Sale(
            id = 4L,
            invoiceNumber = "INV-004",
            customerName = "Bilal",
            totalAmount = 250.0,
            netAmount = 250.0,
            paidAmount = 250.0,
            dueAmount = 0.0,
            cashierName = "Ahmed Khan"
        )

        val items = listOf(
            SaleItem(id = 1L, saleId = 4L, productId = 2L, productName = "Biscuits", quantity = 2.0, unit = "Pkt", purchasePrice = 50.0, salePrice = 125.0, totalPrice = 250.0)
        )

        val inv = InvoiceFormattingService.formatSaleTransaction(sale, items, settings)
        val thermal58 = InvoiceFormattingService.generateThermalText(inv, paperWidthColumns = 32)
        val thermal80 = InvoiceFormattingService.generateThermalText(inv, paperWidthColumns = 48)

        org.junit.Assert.assertTrue(thermal58.contains("Ahmed Khan"))
        org.junit.Assert.assertTrue(thermal80.contains("Ahmed Khan"))

        val escPos = com.example.util.EscPosThermalPrinterService.generateEscPosCommandBytes(sale, items, settings)
        val escPosString = String(escPos, Charsets.UTF_8)
        org.junit.Assert.assertTrue(escPosString.contains("Ahmed Khan"))
    }
}

