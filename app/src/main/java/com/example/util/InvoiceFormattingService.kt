package com.example.util

import com.example.data.entity.PaymentQrConfig
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.data.entity.StoreSettings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ScanToPayInfo(
    val isEnabled: Boolean = false,
    val headerLabel: String = "SCAN TO PAY",
    val paymentType: String = "Easypaisa",
    val accountTitle: String = "",
    val accountNumber: String = "",
    val instructions: String = "",
    val imagePath: String? = null
)

data class PrintableInvoice(
    val header: InvoiceHeader,
    val meta: InvoiceMeta,
    val customer: InvoiceCustomer,
    val items: List<InvoiceItem>,
    val totals: InvoiceTotals,
    val footerText: String,
    val qrPayload: String,
    val scanToPay: ScanToPayInfo? = null
)

data class InvoiceHeader(
    val storeName: String,
    val posBrandName: String = "SENTRY STORE POS",
    val tagline: String = "Professional Retail & Business Management",
    val logoUri: String? = null,
    val ownerName: String,
    val phone: String,
    val email: String,
    val address: String
)

data class InvoiceMeta(
    val invoiceNumber: String,
    val formattedDate: String,
    val saleDate: String = "",
    val saleTime: String = "",
    val timestamp: Long = 0L,
    val cashierName: String,
    val paymentType: String
)

data class InvoiceCustomer(
    val name: String,
    val phone: String
)

data class InvoiceItem(
    val serialNumber: Int = 1,
    val productName: String,
    val quantity: Double,
    val unit: String,
    val unitPrice: Double,
    val discount: Double = 0.0,
    val totalPrice: Double
)

data class InvoiceTotals(
    val subtotal: Double,
    val discount: Double,
    val taxAmount: Double,
    val netAmount: Double,
    val paidAmount: Double,
    val dueAmount: Double,
    val currencySymbol: String
)

object InvoiceFormattingService {

    fun isCashPayment(paymentType: String?): Boolean {
        if (paymentType.isNullOrBlank()) return true
        val normalized = paymentType.trim().lowercase()
        return normalized == "cash" ||
                normalized.startsWith("cash ") ||
                normalized == "cash on delivery" ||
                normalized == "cod"
    }

    fun isBankOrDigitalPayment(paymentType: String?): Boolean {
        if (paymentType.isNullOrBlank()) return false
        if (isCashPayment(paymentType)) return false
        val normalized = paymentType.trim().lowercase()
        return normalized.contains("bank") ||
                normalized.contains("transfer") ||
                normalized.contains("card") ||
                normalized.contains("online") ||
                normalized.contains("digital") ||
                normalized.contains("easypaisa") ||
                normalized.contains("jazzcash") ||
                normalized.contains("raast") ||
                normalized.contains("upaisa") ||
                normalized.contains("nayapay") ||
                normalized.contains("sadapay") ||
                normalized.contains("wallet") ||
                normalized.contains("qr") ||
                normalized.contains("pos")
    }

    fun formatSaleTransaction(
        sale: Sale,
        items: List<SaleItem>,
        settings: StoreSettings,
        activePaymentQr: PaymentQrConfig? = null
    ): PrintableInvoice {
        val dateFormat = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(sale.createdAt))
        val dateOnlyFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())
        val timeOnlyFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        val saleDate = dateOnlyFormat.format(Date(sale.createdAt))
        val saleTime = timeOnlyFormat.format(Date(sale.createdAt))

        val resolvedCashier = when {
            sale.cashierName.isNotBlank() -> sale.cashierName
            settings.defaultCashierName.isNotBlank() -> settings.defaultCashierName
            else -> "Not Assigned"
        }

        val invoiceItems = items.mapIndexed { index, item ->
            val calculatedLineDisc = (item.quantity * item.salePrice - item.totalPrice).coerceAtLeast(0.0)
            InvoiceItem(
                serialNumber = index + 1,
                productName = item.productName,
                quantity = item.quantity,
                unit = item.unit.ifBlank { "Unit" },
                unitPrice = item.salePrice,
                discount = calculatedLineDisc,
                totalPrice = item.totalPrice
            )
        }

        val subtotal = items.sumOf { it.totalPrice }
        val net = if (sale.netAmount > 0) sale.netAmount else (subtotal - sale.discount + sale.taxAmount)

        val qrPayload = buildString {
            appendLine("Store: ${settings.storeName}")
            appendLine("Invoice #: ${sale.invoiceNumber}")
            appendLine("Date: $saleDate")
            appendLine("Time: $saleTime")
            appendLine("Customer: ${sale.customerName}")
            appendLine("Total Amount: ${settings.currencySymbol} %.2f".format(net))
            appendLine("Payment Status: ${if (sale.dueAmount <= 0) "Paid" else "Due: " + settings.currencySymbol + " %.2f".format(sale.dueAmount)}")
            appendLine("Verification ID: VER-${sale.invoiceNumber}")
        }

        val isDigitalOrBank = !isCashPayment(sale.paymentType) && isBankOrDigitalPayment(sale.paymentType)

        val scanToPay = if (
            isDigitalOrBank &&
            settings.isScanToPayEnabled &&
            activePaymentQr != null &&
            activePaymentQr.isEnabled &&
            activePaymentQr.imagePath.isNotBlank()
        ) {
            ScanToPayInfo(
                isEnabled = true,
                headerLabel = settings.scanToPayLabel.ifBlank { "SCAN TO PAY" },
                paymentType = activePaymentQr.paymentType.ifBlank { activePaymentQr.name },
                accountTitle = activePaymentQr.accountTitle,
                accountNumber = activePaymentQr.accountNumber,
                instructions = activePaymentQr.instructions,
                imagePath = activePaymentQr.imagePath
            )
        } else null

        return PrintableInvoice(
            header = InvoiceHeader(
                storeName = settings.storeName.ifBlank { "SENTRY STORE" },
                posBrandName = settings.posBrandName.ifBlank { "SENTRY STORE POS" },
                tagline = settings.tagline.ifBlank { "Professional Retail & Business Management" },
                logoUri = settings.logoUri,
                ownerName = settings.ownerName,
                phone = settings.phone,
                email = settings.email,
                address = settings.address
            ),
            meta = InvoiceMeta(
                invoiceNumber = sale.invoiceNumber,
                formattedDate = formattedDate,
                saleDate = saleDate,
                saleTime = saleTime,
                timestamp = sale.createdAt,
                cashierName = resolvedCashier,
                paymentType = sale.paymentType
            ),
            customer = InvoiceCustomer(
                name = sale.customerName.ifBlank { "Walk-in Customer" },
                phone = ""
            ),
            items = invoiceItems,
            totals = InvoiceTotals(
                subtotal = subtotal,
                discount = sale.discount,
                taxAmount = sale.taxAmount,
                netAmount = net,
                paidAmount = sale.paidAmount,
                dueAmount = sale.dueAmount,
                currencySymbol = settings.currencySymbol
            ),
            footerText = settings.invoiceFooterText,
            qrPayload = qrPayload,
            scanToPay = scanToPay
        )
    }

    fun generateThermalText(invoice: PrintableInvoice, paperWidthColumns: Int = 32): String {
        val width = if (paperWidthColumns in listOf(32, 48)) paperWidthColumns else 32
        val divider = "-".repeat(width)
        val doubleDivider = "=".repeat(width)

        fun center(text: String): String {
            if (text.length <= width) {
                val padding = (width - text.length) / 2
                return " ".repeat(padding) + text
            }
            return text.chunked(width).joinToString("\n") { chunk ->
                val padding = (width - chunk.length).coerceAtLeast(0) / 2
                " ".repeat(padding) + chunk
            }
        }

        fun row(left: String, right: String): String {
            val space = width - left.length - right.length
            return if (space > 0) left + " ".repeat(space) + right else "$left $right".take(width)
        }

        return buildString {
            appendLine(center(invoice.header.storeName))
            if (invoice.header.tagline.isNotBlank()) appendLine(center(invoice.header.tagline))
            if (invoice.header.phone.isNotBlank()) appendLine(center("Tel: ${invoice.header.phone}"))
            if (invoice.header.address.isNotBlank()) appendLine(center(invoice.header.address))
            appendLine(doubleDivider)
            appendLine(row("Invoice #:", invoice.meta.invoiceNumber))
            appendLine(row("Date:", invoice.meta.saleDate.ifBlank { invoice.meta.formattedDate }))
            appendLine(row("Time:", invoice.meta.saleTime))
            appendLine(row("Cashier:", invoice.meta.cashierName))
            appendLine(row("Customer:", invoice.customer.name))
            appendLine(row("Pay Mode:", invoice.meta.paymentType))
            appendLine(divider)
            appendLine(row("S.No Item (Qty x Rate)", "Total"))
            appendLine(divider)

            for (item in invoice.items) {
                val line1 = "${item.serialNumber}. ${item.productName}"
                val line2Left = "   %.1f %s x %.2f".format(item.quantity, item.unit, item.unitPrice)
                val line2Right = "%.2f".format(item.totalPrice)
                appendLine(line1)
                appendLine(row(line2Left, line2Right))
            }

            appendLine(divider)
            appendLine(row("Subtotal:", "${invoice.totals.currencySymbol} %.2f".format(invoice.totals.subtotal)))
            if (invoice.totals.discount > 0) {
                appendLine(row("Discount:", "-${invoice.totals.currencySymbol} %.2f".format(invoice.totals.discount)))
            }
            if (invoice.totals.taxAmount > 0) {
                appendLine(row("Tax:", "+${invoice.totals.currencySymbol} %.2f".format(invoice.totals.taxAmount)))
            }
            appendLine(row("NET TOTAL:", "${invoice.totals.currencySymbol} %.2f".format(invoice.totals.netAmount)))
            appendLine(row("Paid Amount:", "${invoice.totals.currencySymbol} %.2f".format(invoice.totals.paidAmount)))
            if (invoice.totals.dueAmount > 0) {
                appendLine(row("Due Amount:", "${invoice.totals.currencySymbol} %.2f".format(invoice.totals.dueAmount)))
            }
            appendLine(doubleDivider)
            val scanToPay = invoice.scanToPay
            val isEligible = !isCashPayment(invoice.meta.paymentType) && isBankOrDigitalPayment(invoice.meta.paymentType)
            if (scanToPay != null && scanToPay.isEnabled && isEligible) {
                appendLine(center(scanToPay.headerLabel.ifBlank { "SCAN TO PAY" }))
                appendLine(center("[ PAYMENT QR CODE ]"))
                appendLine(center(scanToPay.paymentType))
                if (scanToPay.accountNumber.isNotBlank()) {
                    appendLine(center("A/C: ${scanToPay.accountNumber}"))
                }
                if (scanToPay.accountTitle.isNotBlank()) {
                    appendLine(center("Title: ${scanToPay.accountTitle}"))
                }
                if (scanToPay.instructions.isNotBlank()) {
                    appendLine(center(scanToPay.instructions))
                }
                appendLine(divider)
            }
            if (invoice.footerText.isNotBlank()) {
                appendLine(center(invoice.footerText))
            }
            appendLine(center("*** Powered by ${invoice.header.posBrandName} ***"))
        }
    }
}
