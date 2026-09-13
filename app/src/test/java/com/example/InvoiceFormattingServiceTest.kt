package com.example

import com.example.data.entity.PaymentQrConfig
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
            paidAmount = 100.0,
            paymentType = "Cash"
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
        assertFalse("Cash payments must never show Scan to Pay", text.contains("SCAN TO PAY"))
    }

    @Test
    fun testScanToPayQrHiddenOnCashPayment() {
        val cashSale = Sale(
            id = 3L,
            invoiceNumber = "INV-CASH-1",
            totalAmount = 200.0,
            netAmount = 200.0,
            paidAmount = 200.0,
            paymentType = "Cash"
        )
        val items = listOf(
            SaleItem(id = 1L, saleId = 3L, productId = 1L, productName = "Hardware Item", salePrice = 200.0, quantity = 1.0, totalPrice = 200.0)
        )
        val settings = StoreSettings(
            storeName = "SENTRY STORE",
            isScanToPayEnabled = true
        )
        val qrConfig = PaymentQrConfig(
            id = 1L,
            name = "Easypaisa",
            paymentType = "Easypaisa",
            accountNumber = "03001234567",
            accountTitle = "Store Account",
            imagePath = "/data/user/0/com.example/qr_1.png",
            isEnabled = true
        )

        val invoice = InvoiceFormattingService.formatSaleTransaction(cashSale, items, settings, qrConfig)
        assertNull("Scan-to-Pay QR must be null for cash payments", invoice.scanToPay)

        val thermalText = InvoiceFormattingService.generateThermalText(invoice, 32)
        assertFalse("Thermal text for cash invoice must not contain SCAN TO PAY", thermalText.contains("SCAN TO PAY"))
    }

    @Test
    fun testScanToPayQrVisibleOnBankAndDigitalPayments() {
        val digitalSale = Sale(
            id = 4L,
            invoiceNumber = "INV-DIGITAL-1",
            totalAmount = 500.0,
            netAmount = 500.0,
            paidAmount = 500.0,
            paymentType = "Bank Transfer"
        )
        val items = listOf(
            SaleItem(id = 1L, saleId = 4L, productId = 1L, productName = "Sanitary Pipe", salePrice = 500.0, quantity = 1.0, totalPrice = 500.0)
        )
        val settings = StoreSettings(
            storeName = "SENTRY STORE",
            isScanToPayEnabled = true
        )
        val qrConfig = PaymentQrConfig(
            id = 1L,
            name = "HBL Bank",
            paymentType = "Bank Transfer",
            accountNumber = "PK12HABB0001234567890",
            accountTitle = "Sentry Hardware",
            imagePath = "/data/user/0/com.example/qr_bank.png",
            isEnabled = true
        )

        val invoice = InvoiceFormattingService.formatSaleTransaction(digitalSale, items, settings, qrConfig)
        assertNotNull("Scan-to-Pay QR must be present for Bank Transfer", invoice.scanToPay)
        assertEquals("Bank Transfer", invoice.scanToPay?.paymentType)

        val thermalText = InvoiceFormattingService.generateThermalText(invoice, 32)
        assertTrue("Thermal text for bank transfer must contain SCAN TO PAY", thermalText.contains("SCAN TO PAY"))
        assertTrue("Thermal text must contain account number", thermalText.contains("PK12HABB0001234567890"))
    }
}
