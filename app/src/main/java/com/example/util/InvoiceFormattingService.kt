package com.example.util

import com.example.data.entity.Customer
import com.example.data.entity.Purchase
import com.example.data.entity.PurchaseItem
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.data.entity.StoreProfile
import com.example.data.entity.StoreSettings
import com.example.data.entity.Supplier
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Service that formats transaction data into a printable invoice structure,
 * including store header information, itemized lists, totals breakdown,
 * and generated unique invoice IDs.
 *
 * Supports formatting into:
 * 1. [PrintableInvoiceStructure] object (Structured data model)
 * 2. Monospaced Thermal Receipt Text (58mm / 80mm roll POS printers)
 * 3. Printable CSS-Styled HTML
 * 4. ESC/POS Byte Array Commands for Bluetooth/USB thermal printers
 */
object InvoiceFormattingService {

    // --- DATA MODELS FOR PRINTABLE INVOICE ---

    data class StoreHeaderInfo(
        val storeId: Long = 1L,
        val storeName: String,
        val ownerName: String = "",
        val phone: String = "",
        val address: String = "",
        val logoUri: String = "",
        val currencySymbol: String = "Rs.",
        val storeCode: String = "MAIN"
    )

    data class InvoiceCustomerInfo(
        val customerId: Long? = null,
        val name: String = "Cash Customer",
        val phone: String = "",
        val address: String = "",
        val previousBalance: Double = 0.0,
        val currentBalance: Double = 0.0
    )

    data class InvoiceItem(
        val srNo: Int,
        val productId: Long = 0L,
        val productName: String,
        val barcode: String = "",
        val quantity: Double,
        val unit: String = "Pcs",
        val unitPrice: Double,
        val totalPrice: Double,
        val discount: Double = 0.0
    )

    data class InvoiceTotals(
        val subtotal: Double,
        val discount: Double = 0.0,
        val taxPercentage: Double = 0.0,
        val taxAmount: Double = 0.0,
        val netAmount: Double,
        val paidAmount: Double,
        val dueAmount: Double,
        val totalItemCount: Int,
        val totalQuantity: Double
    )

    data class InvoiceMeta(
        val invoiceId: String,
        val transactionType: String = "SALE", // "SALE", "PURCHASE", "RETURN"
        val timestamp: Long,
        val formattedDate: String,
        val cashierName: String = "Counter Staff",
        val paymentType: String = "Cash", // Cash, Udhaar / Credit, Card
        val notes: String = ""
    )

    data class PrintableInvoiceStructure(
        val header: StoreHeaderInfo,
        val customer: InvoiceCustomerInfo,
        val items: List<InvoiceItem>,
        val totals: InvoiceTotals,
        val meta: InvoiceMeta,
        val termsAndConditions: String = "Goods once sold can be exchanged within 7 days with valid original bill. Mixed paints and custom cut materials are non-returnable."
    )

    // --- QR VERIFICATION HELPERS ---

    fun getInvoiceVerificationId(invoiceId: String, timestamp: Long): String {
        val cleanInv = invoiceId.replace("[^A-Za-z0-9]".toRegex(), "").uppercase(Locale.US)
        val hashStr = String.format(Locale.US, "%05d", kotlin.math.abs(timestamp % 100000))
        return "VER-$cleanInv-$hashStr"
    }

    fun getPaymentStatusString(paidAmount: Double, dueAmount: Double, paymentType: String, currencySymbol: String): String {
        return when {
            dueAmount <= 0 -> "Paid ($paymentType)"
            paidAmount <= 0 -> "Unpaid / Udhaar ($currencySymbol ${dueAmount.toInt()})"
            else -> "Partial ($currencySymbol ${paidAmount.toInt()}) / Due: $currencySymbol ${dueAmount.toInt()}"
        }
    }

    fun getQrCodePayload(
        storeName: String,
        invoiceNumber: String,
        formattedDate: String,
        customerName: String,
        totalAmount: Double,
        currencySymbol: String,
        paidAmount: Double,
        dueAmount: Double,
        paymentType: String,
        timestamp: Long
    ): String {
        val verId = getInvoiceVerificationId(invoiceNumber, timestamp)
        val payStatus = getPaymentStatusString(paidAmount, dueAmount, paymentType, currencySymbol)
        val headerTitle = storeName.ifBlank { "OFFICIAL STORE" }.uppercase(Locale.US)
        return """
=== $headerTitle INVOICE VERIFICATION ===
Store Name: $storeName
Invoice Number: $invoiceNumber
Invoice Date & Time: $formattedDate
Customer Name: $customerName
Total Amount: $currencySymbol ${totalAmount.toInt()}
Payment Status: $payStatus
Invoice Verification ID: $verId
Status: VERIFIED AUTHENTIC OFFICIAL INVOICE
""".trimIndent()
    }

    fun PrintableInvoiceStructure.getInvoiceVerificationId(): String {
        return InvoiceFormattingService.getInvoiceVerificationId(meta.invoiceId, meta.timestamp)
    }

    fun PrintableInvoiceStructure.getPaymentStatusString(): String {
        return InvoiceFormattingService.getPaymentStatusString(totals.paidAmount, totals.dueAmount, meta.paymentType, header.currencySymbol)
    }

    fun PrintableInvoiceStructure.getQrCodePayload(): String {
        return InvoiceFormattingService.getQrCodePayload(
            storeName = header.storeName,
            invoiceNumber = meta.invoiceId,
            formattedDate = meta.formattedDate,
            customerName = customer.name,
            totalAmount = totals.netAmount,
            currencySymbol = header.currencySymbol,
            paidAmount = totals.paidAmount,
            dueAmount = totals.dueAmount,
            paymentType = meta.paymentType,
            timestamp = meta.timestamp
        )
    }

    // --- INVOICE ID GENERATOR ---

    /**
     * Generates a unique, standard formatted invoice ID.
     * Example: INV-MAIN-20260804-0042
     */
    fun generateInvoiceId(
        storeCode: String = "MAIN",
        timestamp: Long = System.currentTimeMillis(),
        sequenceNumber: Long = (1..9999).random().toLong()
    ): String {
        val dateCode = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date(timestamp))
        val seqFormatted = String.format(Locale.US, "%04d", sequenceNumber % 10000)
        val cleanCode = storeCode.uppercase(Locale.US).replace("[^A-Z0-9]".toRegex(), "").take(6).ifBlank { "STORE" }
        return "INV-$cleanCode-$dateCode-$seqFormatted"
    }

    // --- TRANSACTION FORMATTERS ---

    /**
     * Formats a Sale transaction into a complete PrintableInvoiceStructure
     */
    fun formatSaleTransaction(
        sale: Sale,
        items: List<SaleItem>,
        settings: StoreSettings,
        storeProfile: StoreProfile? = null,
        customer: Customer? = null,
        cashierName: String? = null
    ): PrintableInvoiceStructure {
        val invoiceId = if (sale.invoiceNumber.isNotBlank()) {
            sale.invoiceNumber
        } else {
            generateInvoiceId(
                storeCode = storeProfile?.code ?: "MAIN",
                timestamp = sale.timestamp,
                sequenceNumber = sale.id
            )
        }

        val formattedDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(sale.timestamp))

        val resolvedCashier = when {
            !cashierName.isNullOrBlank() -> cashierName
            sale.cashierName.isNotBlank() -> sale.cashierName
            settings.defaultCashierName.isNotBlank() -> settings.defaultCashierName
            else -> "Not Assigned"
        }

        val header = StoreHeaderInfo(
            storeId = storeProfile?.id ?: settings.activeStoreId,
            storeName = storeProfile?.storeName ?: settings.storeName,
            ownerName = storeProfile?.ownerName ?: settings.ownerName,
            phone = storeProfile?.phone ?: settings.phone,
            address = storeProfile?.address ?: settings.address,
            logoUri = storeProfile?.logoUri ?: settings.logoUri,
            currencySymbol = settings.currencySymbol,
            storeCode = storeProfile?.code ?: "MAIN"
        )

        val customerInfo = InvoiceCustomerInfo(
            customerId = sale.customerId ?: customer?.id,
            name = customer?.name ?: sale.customerName,
            phone = customer?.phone ?: "",
            address = customer?.address ?: "",
            currentBalance = customer?.balance ?: 0.0
        )

        val invoiceItems = items.mapIndexed { index, item ->
            InvoiceItem(
                srNo = index + 1,
                productId = item.productId,
                productName = item.productName,
                quantity = item.quantity,
                unit = item.unit,
                unitPrice = item.salePrice,
                totalPrice = item.totalPrice
            )
        }

        val totalQty = items.sumOf { it.quantity }
        val taxAmt = if (settings.taxPercentage > 0) (sale.netAmount * (settings.taxPercentage / 100.0)) else 0.0

        val totals = InvoiceTotals(
            subtotal = sale.totalAmount,
            discount = sale.discount,
            taxPercentage = settings.taxPercentage,
            taxAmount = taxAmt,
            netAmount = sale.netAmount + taxAmt,
            paidAmount = sale.paidAmount,
            dueAmount = sale.dueAmount,
            totalItemCount = items.size,
            totalQuantity = totalQty
        )

        val meta = InvoiceMeta(
            invoiceId = invoiceId,
            transactionType = "SALE INVOICE",
            timestamp = sale.timestamp,
            formattedDate = formattedDate,
            cashierName = resolvedCashier,
            paymentType = sale.paymentType
        )

        return PrintableInvoiceStructure(
            header = header,
            customer = customerInfo,
            items = invoiceItems,
            totals = totals,
            meta = meta
        )
    }

    /**
     * Formats a Purchase transaction into a complete PrintableInvoiceStructure
     */
    fun formatPurchaseTransaction(
        purchase: Purchase,
        items: List<PurchaseItem>,
        settings: StoreSettings,
        storeProfile: StoreProfile? = null,
        supplier: Supplier? = null,
        userRole: String = "ADMIN"
    ): PrintableInvoiceStructure {
        val invoiceId = purchase.purchaseNumber.ifBlank {
            generateInvoiceId(
                storeCode = storeProfile?.code ?: "PUR",
                timestamp = purchase.timestamp,
                sequenceNumber = purchase.id
            )
        }

        val formattedDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(purchase.timestamp))

        val header = StoreHeaderInfo(
            storeId = storeProfile?.id ?: settings.activeStoreId,
            storeName = storeProfile?.storeName ?: settings.storeName,
            ownerName = storeProfile?.ownerName ?: settings.ownerName,
            phone = storeProfile?.phone ?: settings.phone,
            address = storeProfile?.address ?: settings.address,
            logoUri = storeProfile?.logoUri ?: settings.logoUri,
            currencySymbol = settings.currencySymbol,
            storeCode = storeProfile?.code ?: "MAIN"
        )

        val supplierCustomerInfo = InvoiceCustomerInfo(
            customerId = supplier?.id ?: purchase.supplierId,
            name = supplier?.name ?: purchase.supplierName,
            phone = supplier?.phone ?: "",
            address = supplier?.address ?: "",
            currentBalance = supplier?.payableBalance ?: 0.0
        )

        val invoiceItems = items.mapIndexed { index, item ->
            InvoiceItem(
                srNo = index + 1,
                productId = item.productId,
                productName = item.productName,
                quantity = item.quantity,
                unit = "Pcs",
                unitPrice = item.costPrice,
                totalPrice = item.totalPrice
            )
        }

        val totalQty = items.sumOf { it.quantity }

        val totals = InvoiceTotals(
            subtotal = purchase.totalAmount,
            discount = 0.0,
            taxPercentage = 0.0,
            taxAmount = 0.0,
            netAmount = purchase.totalAmount,
            paidAmount = purchase.paidAmount,
            dueAmount = purchase.dueAmount,
            totalItemCount = items.size,
            totalQuantity = totalQty
        )

        val meta = InvoiceMeta(
            invoiceId = invoiceId,
            transactionType = "PURCHASE VOUCHER",
            timestamp = purchase.timestamp,
            formattedDate = formattedDate,
            cashierName = userRole,
            paymentType = if (purchase.dueAmount > 0) "Supplier Credit" else "Cash/Bank"
        )

        return PrintableInvoiceStructure(
            header = header,
            customer = supplierCustomerInfo,
            items = invoiceItems,
            totals = totals,
            meta = meta
        )
    }

    // --- TEXT & THERMAL RECEIPT GENERATOR ---

    /**
     * Formats the invoice into monospaced text for 58mm (32 chars) or 80mm (42 chars) roll printers.
     */
    fun generateThermalText(invoice: PrintableInvoiceStructure, paperWidthColumns: Int = 32): String {
        val width = if (paperWidthColumns <= 35) 32 else 42
        val sb = StringBuilder()
        val lineDivider = "-".repeat(width)
        val doubleDivider = "=".repeat(width)

        fun center(text: String): String {
            if (text.length >= width) return text.take(width)
            val padding = (width - text.length) / 2
            return " ".repeat(padding) + text
        }

        fun row(left: String, right: String): String {
            val maxLeft = width - right.length - 1
            val cleanLeft = if (left.length > maxLeft) left.take(maxLeft - 1) + "." else left
            val spaces = width - cleanLeft.length - right.length
            return cleanLeft + " ".repeat(spaces.coerceAtLeast(1)) + right
        }

        val cur = invoice.header.currencySymbol

        // Header
        sb.append(doubleDivider).append("\n")
        sb.append(center(invoice.header.storeName)).append("\n")
        if (invoice.header.ownerName.isNotBlank()) {
            sb.append(center("Prop: ${invoice.header.ownerName}")).append("\n")
        }
        if (invoice.header.phone.isNotBlank()) {
            sb.append(center("Ph: ${invoice.header.phone}")).append("\n")
        }
        if (invoice.header.address.isNotBlank()) {
            sb.append(center(invoice.header.address)).append("\n")
        }
        sb.append(doubleDivider).append("\n")

        // Meta
        sb.append(center("*** ${invoice.meta.transactionType} ***")).append("\n")
        sb.append(row("Invoice #:", invoice.meta.invoiceId)).append("\n")
        sb.append(row("Date:", invoice.meta.formattedDate)).append("\n")
        sb.append(row("Customer:", invoice.customer.name)).append("\n")
        if (invoice.customer.phone.isNotBlank()) {
            sb.append(row("Phone:", invoice.customer.phone)).append("\n")
        }
        sb.append(row("Payment:", invoice.meta.paymentType)).append("\n")
        sb.append(row("Cashier:", invoice.meta.cashierName)).append("\n")
        sb.append(lineDivider).append("\n")

        // Items Header
        if (width == 32) {
            sb.append("Item              Qty   Amount").append("\n")
        } else {
            sb.append("Item Description        Qty    Rate   Total").append("\n")
        }
        sb.append(lineDivider).append("\n")

        // Items List
        for (item in invoice.items) {
            val qtyStr = if (item.quantity % 1.0 == 0.0) "${item.quantity.toInt()} ${item.unit}" else "${item.quantity} ${item.unit}"
            val priceStr = String.format(Locale.US, "%.0f", item.totalPrice)

            if (width == 32) {
                sb.append(item.productName.take(32)).append("\n")
                sb.append(row("  $qtyStr x ${String.format(Locale.US, "%.0f", item.unitPrice)}", "$cur $priceStr")).append("\n")
            } else {
                val pName = item.productName.take(22).padEnd(23)
                val qStr = qtyStr.padEnd(7)
                val rStr = String.format(Locale.US, "%.0f", item.unitPrice).padEnd(7)
                sb.append("$pName $qStr $rStr $priceStr\n")
            }
        }

        sb.append(lineDivider).append("\n")

        // Totals Summary
        sb.append(row("Total Items / Qty:", "${invoice.totals.totalItemCount} / ${if (invoice.totals.totalQuantity % 1.0 == 0.0) invoice.totals.totalQuantity.toInt() else invoice.totals.totalQuantity}")).append("\n")
        sb.append(row("Subtotal:", "$cur ${String.format(Locale.US, "%.0f", invoice.totals.subtotal)}")).append("\n")

        if (invoice.totals.discount > 0) {
            sb.append(row("Discount:", "- $cur ${String.format(Locale.US, "%.0f", invoice.totals.discount)}")).append("\n")
        }

        if (invoice.totals.taxAmount > 0) {
            sb.append(row("Tax (${invoice.totals.taxPercentage}%):", "$cur ${String.format(Locale.US, "%.0f", invoice.totals.taxAmount)}")).append("\n")
        }

        sb.append(doubleDivider).append("\n")
        sb.append(row("NET PAYABLE:", "$cur ${String.format(Locale.US, "%.0f", invoice.totals.netAmount)}")).append("\n")
        sb.append(row("PAID AMOUNT:", "$cur ${String.format(Locale.US, "%.0f", invoice.totals.paidAmount)}")).append("\n")

        if (invoice.totals.dueAmount > 0) {
            sb.append(row("REMAINING DUE:", "$cur ${String.format(Locale.US, "%.0f", invoice.totals.dueAmount)}")).append("\n")
        }
        sb.append(doubleDivider).append("\n")

        // Footer
        sb.append(center("Thank You For Shopping!")).append("\n")
        sb.append(center(invoice.header.storeName)).append("\n")
        sb.append("\n\n")

        return sb.toString()
    }

    // --- HTML GENERATOR FOR WEB & PRINTING ---

    /**
     * Formats the invoice into a printable CSS-styled HTML document.
     */
    fun generatePrintableHtml(invoice: PrintableInvoiceStructure): String {
        val cur = invoice.header.currencySymbol
        val itemsHtml = StringBuilder()

        for (item in invoice.items) {
            val qtyStr = if (item.quantity % 1.0 == 0.0) "${item.quantity.toInt()} ${item.unit}" else "${item.quantity} ${item.unit}"
            itemsHtml.append("""
                <tr>
                    <td style="padding: 8px; border-bottom: 1px solid #e2e8f0;">${item.srNo}</td>
                    <td style="padding: 8px; border-bottom: 1px solid #e2e8f0; font-weight: 500;">${item.productName}</td>
                    <td style="padding: 8px; border-bottom: 1px solid #e2e8f0; text-align: center;">$qtyStr</td>
                    <td style="padding: 8px; border-bottom: 1px solid #e2e8f0; text-align: right;">$cur ${String.format(Locale.US, "%.0f", item.unitPrice)}</td>
                    <td style="padding: 8px; border-bottom: 1px solid #e2e8f0; text-align: right; font-weight: bold;">$cur ${String.format(Locale.US, "%.0f", item.totalPrice)}</td>
                </tr>
            """.trimIndent())
        }

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Invoice #${invoice.meta.invoiceId}</title>
                <style>
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; color: #1e293b; margin: 0; padding: 20px; background: #fff; }
                    .invoice-card { max-width: 800px; margin: 0 auto; border: 1px solid #cbd5e1; border-radius: 8px; padding: 24px; }
                    .header-bar { background: #0f172a; color: white; padding: 16px; border-radius: 6px; display: flex; justify-space-between; align-items: center; }
                    .store-title { font-size: 20px; font-weight: bold; margin: 0; }
                    .store-sub { font-size: 12px; color: #94a3b8; margin-top: 4px; }
                    .meta-grid { display: flex; justify-content: space-between; margin: 20px 0; padding: 12px; background: #f8fafc; border-radius: 6px; }
                    .table { width: 100%; border-collapse: collapse; margin-top: 16px; }
                    .th { background: #1e293b; color: white; padding: 10px; text-align: left; font-size: 13px; }
                    .summary-box { width: 280px; margin-left: auto; margin-top: 20px; background: #f8fafc; padding: 16px; border-radius: 6px; border: 1px solid #e2e8f0; }
                    .summary-row { display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 14px; }
                    .summary-bold { font-weight: bold; font-size: 16px; color: #2563eb; }
                    .footer { text-align: center; margin-top: 30px; font-size: 12px; color: #64748b; border-top: 1px solid #e2e8f0; padding-top: 12px; }
                </style>
            </head>
            <body>
                <div class="invoice-card">
                    <div class="header-bar">
                        <div>
                            <div class="store-title">${invoice.header.storeName}</div>
                            <div class="store-sub">Prop: ${invoice.header.ownerName} | Ph: ${invoice.header.phone}</div>
                            <div class="store-sub">${invoice.header.address}</div>
                        </div>
                        <div style="text-align: right;">
                            <h2 style="margin:0; color: #60a5fa;">${invoice.meta.transactionType}</h2>
                            <div style="font-size: 12px;">#${invoice.meta.invoiceId}</div>
                        </div>
                    </div>

                    <div class="meta-grid">
                        <div>
                            <strong>Bill To:</strong> ${invoice.customer.name}<br>
                            <span style="font-size: 12px; color: #64748b;">Phone: ${invoice.customer.phone.ifBlank { "N/A" }}</span><br>
                            <span style="font-size: 12px; color: #334155;"><strong>Cashier:</strong> ${invoice.meta.cashierName}</span>
                        </div>
                        <div style="text-align: right;">
                            <strong>Date:</strong> ${invoice.meta.formattedDate}<br>
                            <strong>Payment Mode:</strong> ${invoice.meta.paymentType}
                        </div>
                    </div>

                    <table class="table">
                        <thead>
                            <tr>
                                <th class="th">#</th>
                                <th class="th">Item Description</th>
                                <th class="th" style="text-align: center;">Qty</th>
                                <th class="th" style="text-align: right;">Rate</th>
                                <th class="th" style="text-align: right;">Total</th>
                            </tr>
                        </thead>
                        <tbody>
                            $itemsHtml
                        </tbody>
                    </table>

                    <div class="summary-box">
                        <div class="summary-row"><span>Subtotal:</span> <span>$cur ${String.format(Locale.US, "%.0f", invoice.totals.subtotal)}</span></div>
                        ${if (invoice.totals.discount > 0) "<div class=\"summary-row\"><span>Discount:</span> <span>- $cur ${String.format(Locale.US, "%.0f", invoice.totals.discount)}</span></div>" else ""}
                        <div class="summary-row summary-bold"><span>Net Amount:</span> <span>$cur ${String.format(Locale.US, "%.0f", invoice.totals.netAmount)}</span></div>
                        <div class="summary-row"><span>Paid Amount:</span> <span>$cur ${String.format(Locale.US, "%.0f", invoice.totals.paidAmount)}</span></div>
                        ${if (invoice.totals.dueAmount > 0) "<div class=\"summary-row\" style=\"color: #dc2626; font-weight: bold;\"><span>Due Balance:</span> <span>$cur ${String.format(Locale.US, "%.0f", invoice.totals.dueAmount)}</span></div>" else ""}
                    </div>

                    <div class="footer">
                        <p>${invoice.termsAndConditions}</p>
                        <strong>*** Thank You For Shopping With ${invoice.header.storeName}! ***</strong>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    // --- ESC/POS COMMAND GENERATOR ---

    /**
     * Generates ESC/POS command byte array for hardware thermal printer integration.
     */
    fun generatePrintableEscPosBytes(invoice: PrintableInvoiceStructure): ByteArray {
        val stream = ByteArrayOutputStream()

        // ESC @ Initialize Printer
        stream.write(byteArrayOf(0x1B, 0x40))

        // ESC a 1 Center align
        stream.write(byteArrayOf(0x1B, 0x61, 0x01))

        // GS ! 0x11 Double height + width for Header
        stream.write(byteArrayOf(0x1D, 0x21, 0x11))
        stream.write("${invoice.header.storeName}\n".toByteArray(Charsets.UTF_8))

        // Reset text size
        stream.write(byteArrayOf(0x1D, 0x21, 0x00))
        stream.write("Ph: ${invoice.header.phone}\n".toByteArray(Charsets.UTF_8))
        stream.write("${invoice.header.address}\n".toByteArray(Charsets.UTF_8))
        stream.write("================================\n".toByteArray(Charsets.UTF_8))

        // Left align
        stream.write(byteArrayOf(0x1B, 0x61, 0x00))
        stream.write("Inv #: ${invoice.meta.invoiceId}\n".toByteArray(Charsets.UTF_8))
        stream.write("Date:  ${invoice.meta.formattedDate}\n".toByteArray(Charsets.UTF_8))
        stream.write("Cust:  ${invoice.customer.name}\n".toByteArray(Charsets.UTF_8))
        stream.write("--------------------------------\n".toByteArray(Charsets.UTF_8))

        val thermalText = generateThermalText(invoice, paperWidthColumns = 32)
        stream.write(thermalText.toByteArray(Charsets.UTF_8))

        // Feed & Cut Paper (GS V 66 0)
        stream.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00))

        return stream.toByteArray()
    }
}
