package com.example

import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.data.entity.StoreSettings
import com.example.util.InvoiceFormattingService
import org.junit.Assert.*
import org.junit.Test

class InvoiceFormattingServiceTest {

    @Test
    fun testFormatSaleTransaction() {
        val sale = Sale(
            id = 1L,
            invoiceNumber = "INV-2026-001",
            customerName = "Walk-in Customer",
            cashierName = "Admin",
            totalAmount = 500.0,
            discount = 50.0,
            taxAmount = 0.0,
            netAmount = 450.0,
            paidAmount = 500.0,
            dueAmount = 0.0,
            paymentType = "Cash",
            createdAt = 1774000000000L
        )

        val items = listOf(
            SaleItem(
                id = 1L,
                saleId = 1L,
                productId = 1L,
                productName = "Rice 5kg",
                salePrice = 250.0,
                quantity = 2.0,
                totalPrice = 500.0,
                unit = "Bag"
            )
        )

        val settings = StoreSettings(
            storeName = "CH UMER SENTRY STORE",
            phone = "03080018035",
            address = "Store #1"
        )

        val invoice = InvoiceFormattingService.formatSaleTransaction(sale, items, settings)

        assertEquals("INV-2026-001", invoice.meta.invoiceNumber)
        assertEquals("CH UMER SENTRY STORE", invoice.header.storeName)
        assertEquals(450.0, invoice.totals.netAmount, 0.001)
        assertEquals(1, invoice.items.size)
        assertTrue("QR payload must contain store name", invoice.qrPayload.contains("CH UMER SENTRY STORE"))
        assertTrue("QR payload must contain invoice number", invoice.qrPayload.contains("INV-2026-001"))
    }

    @Test
    fun testGenerateThermalText() {
        val sale = Sale(
            id = 2L,
            invoiceNumber = "INV-100",
            customerName = "Cash Buyer",
            totalAmount = 100.0,
            netAmount = 100.0,
            paidAmount = 100.0
        )
        val items = listOf(
            SaleItem(id = 1L, saleId = 2L, productId = 1L, productName = "Item A", salePrice = 100.0, quantity = 1.0, totalPrice = 100.0)
        )
        val settings = StoreSettings(storeName = "POS STORE", phone = "03080018035")
        val invoice = InvoiceFormattingService.formatSaleTransaction(sale, items, settings)
        val text = InvoiceFormattingService.generateThermalText(invoice, 32)

        assertTrue(text.contains("POS STORE"))
        assertTrue(text.contains("INV-100"))
        assertTrue(text.contains("Item A"))
    }
}
