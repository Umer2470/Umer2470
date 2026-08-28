package com.example.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.data.entity.StoreSettings
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    enum class ReceiptFormat {
        A4,
        THERMAL_80MM
    }

    /**
     * Convenience function to format and generate printable PDF
     */
    fun generatePrintablePdfInvoice(
        context: Context,
        sale: Sale,
        items: List<SaleItem>,
        settings: StoreSettings,
        format: ReceiptFormat = ReceiptFormat.A4
    ): File? {
        val printableInvoice = InvoiceFormattingService.formatSaleTransaction(sale, items, settings)
        return generateInvoicePdf(context, printableInvoice, format)
    }

    /**
     * Generates a PDF file based on the requested format (A4 Tax Invoice or 80mm Thermal Receipt).
     */
    fun generateInvoicePdf(
        context: Context,
        invoice: PrintableInvoice,
        format: ReceiptFormat = ReceiptFormat.A4
    ): File? {
        return when (format) {
            ReceiptFormat.A4 -> generateA4TaxInvoicePdf(context, invoice)
            ReceiptFormat.THERMAL_80MM -> generateThermalReceiptPdf(context, invoice)
        }
    }

    /**
     * Standard A4 Tax Invoice (595 x 842 points @ 72 DPI)
     */
    private fun generateA4TaxInvoicePdf(
        context: Context,
        invoice: PrintableInvoice
    ): File? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val basePaint = Paint().apply {
            color = Color.rgb(30, 41, 59) // Slate 800
            textSize = 10f
            isAntiAlias = true
            isSubpixelText = true
            isFilterBitmap = true
        }

        val titlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42) // Navy 900
            textSize = 18f
            isFakeBoldText = true
            isAntiAlias = true
            isSubpixelText = true
        }

        val headerPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10.5f
            isFakeBoldText = true
            isAntiAlias = true
            isSubpixelText = true
        }

        val whiteBoldPaint = Paint().apply {
            color = Color.WHITE
            textSize = 10f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val mutedPaint = Paint().apply {
            color = Color.rgb(71, 85, 105) // Slate 600
            textSize = 9f
            isAntiAlias = true
            isSubpixelText = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(203, 213, 225) // Slate 300
            strokeWidth = 0.8f
            isAntiAlias = true
        }

        val bgCardPaint = Paint().apply {
            color = Color.rgb(248, 250, 252) // Slate 50
            style = Paint.Style.FILL
        }

        val headerBarPaint = Paint().apply {
            color = Color.rgb(15, 23, 42) // Navy 900
            style = Paint.Style.FILL
        }

        val goldPaint = Paint().apply {
            color = Color.rgb(217, 119, 6) // Gold 600
            textSize = 11f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val greenPaint = Paint().apply {
            color = Color.rgb(5, 150, 105) // Emerald 600
            textSize = 10.5f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val redPaint = Paint().apply {
            color = Color.rgb(225, 29, 72) // Rose 600
            textSize = 10.5f
            isFakeBoldText = true
            isAntiAlias = true
        }

        var y = 42f

        // 1. Top Store Header with Custom/Default Logo
        try {
            val logoBitmap = BrandingImageHelper.getLogoBitmap(context, invoice.header.logoUri)
            val logoRect = RectF(40f, 32f, 82f, 74f)
            canvas.drawBitmap(logoBitmap, null, logoRect, basePaint)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val textStartX = 92f
        canvas.drawText(invoice.header.storeName, textStartX, y, titlePaint)
        y += 15f
        if (invoice.header.tagline.isNotBlank()) {
            canvas.drawText(invoice.header.tagline, textStartX, y, mutedPaint)
            y += 12f
        }
        if (invoice.header.phone.isNotBlank()) {
            canvas.drawText("Phone / WhatsApp: ${invoice.header.phone}", textStartX, y, mutedPaint)
            y += 12f
        }
        if (invoice.header.address.isNotBlank()) {
            canvas.drawText("Address: ${invoice.header.address}", textStartX, y, mutedPaint)
            y += 12f
        }

        // TAX INVOICE Badge (Top Right)
        val badgeRect = RectF(410f, 32f, 555f, 75f)
        canvas.drawRoundRect(badgeRect, 6f, 6f, headerBarPaint)
        canvas.drawText("TAX INVOICE", 435f, 52f, goldPaint)
        canvas.drawText(invoice.meta.invoiceNumber, 435f, 67f, whiteBoldPaint)

        y = maxOf(y + 8f, 92f)
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 18f

        // 2. Invoice Meta & Customer Section (Dual Column Card)
        val metaCardRect = RectF(40f, y - 6f, 555f, y + 54f)
        canvas.drawRoundRect(metaCardRect, 6f, 6f, bgCardPaint)

        // Customer Column
        canvas.drawText("BILL TO / CUSTOMER:", 50f, y + 10f, headerPaint)
        canvas.drawText(invoice.customer.name, 50f, y + 24f, headerPaint)
        if (invoice.customer.phone.isNotBlank()) {
            canvas.drawText("Phone: ${invoice.customer.phone}", 50f, y + 38f, mutedPaint)
        }

        // Meta Column
        canvas.drawText("Invoice #: ${invoice.meta.invoiceNumber}", 350f, y + 10f, basePaint)
        canvas.drawText("Date: ${invoice.meta.saleDate.ifBlank { invoice.meta.formattedDate }}  |  Time: ${invoice.meta.saleTime}", 350f, y + 24f, basePaint)
        canvas.drawText("Payment: ${invoice.meta.paymentType}  |  Cashier: ${invoice.meta.cashierName}", 350f, y + 38f, basePaint)

        y += 72f

        // 3. Table Header Bar
        val tableHeaderRect = RectF(40f, y - 10f, 555f, y + 12f)
        canvas.drawRoundRect(tableHeaderRect, 4f, 4f, headerBarPaint)
        canvas.drawText("#", 50f, y + 5f, whiteBoldPaint)
        canvas.drawText("Item Description", 80f, y + 5f, whiteBoldPaint)
        canvas.drawText("Qty", 310f, y + 5f, whiteBoldPaint)
        canvas.drawText("Unit Rate", 380f, y + 5f, whiteBoldPaint)
        canvas.drawText("Total (${invoice.totals.currencySymbol})", 470f, y + 5f, whiteBoldPaint)
        y += 24f

        // 4. Table Items
        val rowPaint = Paint().apply {
            color = Color.rgb(241, 245, 249) // Slate 100
            style = Paint.Style.FILL
        }

        for ((index, item) in invoice.items.withIndex()) {
            if (index % 2 == 1) {
                val rowRect = RectF(40f, y - 9f, 555f, y + 11f)
                canvas.drawRect(rowRect, rowPaint)
            }
            canvas.drawText("${index + 1}", 50f, y + 4f, mutedPaint)
            canvas.drawText(item.productName.take(38), 80f, y + 4f, basePaint)
            canvas.drawText("%.1f %s".format(item.quantity, item.unit), 310f, y + 4f, basePaint)
            canvas.drawText("%.2f".format(item.unitPrice), 380f, y + 4f, basePaint)
            canvas.drawText("%.2f".format(item.totalPrice), 470f, y + 4f, headerPaint)
            y += 18f
        }

        y += 6f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 18f

        // 5. Totals & Financial Breakdown
        val totalsX = 350f
        canvas.drawText("Subtotal:", totalsX, y, basePaint)
        canvas.drawText("${invoice.totals.currencySymbol} %.2f".format(invoice.totals.subtotal), 470f, y, basePaint)
        y += 16f

        if (invoice.totals.discount > 0) {
            canvas.drawText("Discount:", totalsX, y, basePaint)
            canvas.drawText("-${invoice.totals.currencySymbol} %.2f".format(invoice.totals.discount), 470f, y, redPaint)
            y += 16f
        }
        if (invoice.totals.taxAmount > 0) {
            canvas.drawText("Tax / GST:", totalsX, y, basePaint)
            canvas.drawText("+${invoice.totals.currencySymbol} %.2f".format(invoice.totals.taxAmount), 470f, y, basePaint)
            y += 16f
        }

        canvas.drawLine(totalsX, y - 4f, 555f, y - 4f, linePaint)

        // Net Grand Total Block
        val totalBox = RectF(totalsX - 6f, y - 1f, 555f, y + 23f)
        canvas.drawRoundRect(totalBox, 4f, 4f, headerBarPaint)
        canvas.drawText("GRAND TOTAL:", totalsX + 4f, y + 15f, whiteBoldPaint)
        canvas.drawText("${invoice.totals.currencySymbol} %.2f".format(invoice.totals.netAmount), 460f, y + 15f, goldPaint)
        y += 36f

        canvas.drawText("Paid Amount:", totalsX, y, basePaint)
        canvas.drawText("${invoice.totals.currencySymbol} %.2f".format(invoice.totals.paidAmount), 470f, y, greenPaint)
        y += 16f

        if (invoice.totals.dueAmount > 0) {
            canvas.drawText("Balance Due:", totalsX, y, headerPaint)
            canvas.drawText("${invoice.totals.currencySymbol} %.2f".format(invoice.totals.dueAmount), 470f, y, redPaint)
            y += 16f
        }

        // 6. Draw QR Code & Verification Block on Left
        val qrBitmap = QrCodeRenderer.generateQrBitmap(invoice.qrPayload, 256, 256)
        if (qrBitmap != null) {
            val qrDest = RectF(40f, y - 80f, 130f, y + 10f)
            canvas.drawBitmap(qrBitmap, null, qrDest, basePaint)
            canvas.drawText("Scan to verify invoice", 40f, y + 22f, mutedPaint)
        }

        y = maxOf(y + 40f, 730f)
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 16f

        // 7. Footer Terms & Signature
        canvas.drawText("Terms & Conditions:", 40f, y, headerPaint)
        canvas.drawText("____________________________", 370f, y + 16f, linePaint)
        canvas.drawText("Authorized Signature / Stamp", 370f, y + 30f, mutedPaint)
        y += 14f

        if (invoice.footerText.isNotBlank()) {
            canvas.drawText(invoice.footerText, 40f, y, mutedPaint)
            y += 12f
        }
        canvas.drawText("Thank you for choosing CH UMER Sanitary & Hardware Store. Official System Record.", 40f, y, mutedPaint)

        document.finishPage(page)

        return savePdfDocument(context, document, "Invoice_${sanitizeFilename(invoice.meta.invoiceNumber)}")
    }

    /**
     * Compact 80mm Thermal Receipt PDF (226 x ~600 points)
     */
    private fun generateThermalReceiptPdf(
        context: Context,
        invoice: PrintableInvoice
    ): File? {
        val width = 226 // ~80mm width in PDF points (72 DPI)
        // Calculate dynamic height based on item count
        val calculatedHeight = 420 + (invoice.items.size * 18)
        val height = maxOf(500, calculatedHeight)

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val boldCenter = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val textCenter = Paint().apply {
            color = Color.rgb(40, 40, 40)
            textSize = 8.5f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val textLeft = Paint().apply {
            color = Color.BLACK
            textSize = 8.5f
            isAntiAlias = true
        }

        val textRight = Paint().apply {
            color = Color.BLACK
            textSize = 8.5f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        val boldRight = Paint().apply {
            color = Color.BLACK
            textSize = 9.5f
            isFakeBoldText = true
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        val boldLeft = Paint().apply {
            color = Color.BLACK
            textSize = 9f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.DKGRAY
            strokeWidth = 0.6f
        }

        val centerX = width / 2f
        val margin = 10f
        val rightMargin = width - 10f
        var y = 16f

        // Draw Store Logo if available
        try {
            val logoBitmap = BrandingImageHelper.getLogoBitmap(context, invoice.header.logoUri)
            val logoSize = 34f
            val logoRect = RectF(centerX - (logoSize / 2), y, centerX + (logoSize / 2), y + logoSize)
            canvas.drawBitmap(logoBitmap, null, logoRect, textLeft)
            y += logoSize + 8f
        } catch (e: Exception) {
            y = 25f
        }

        // Store Header
        canvas.drawText(invoice.header.storeName, centerX, y, boldCenter)
        y += 14f
        if (invoice.header.tagline.isNotBlank()) {
            canvas.drawText(invoice.header.tagline, centerX, y, textCenter)
            y += 11f
        }
        if (invoice.header.phone.isNotBlank()) {
            canvas.drawText("Tel: ${invoice.header.phone}", centerX, y, textCenter)
            y += 11f
        }
        if (invoice.header.address.isNotBlank()) {
            canvas.drawText(invoice.header.address, centerX, y, textCenter)
            y += 11f
        }

        y += 4f
        canvas.drawLine(margin, y, rightMargin, y, linePaint)
        y += 14f

        // Details
        canvas.drawText("Inv: ${invoice.meta.invoiceNumber}", margin, y, textLeft)
        canvas.drawText("Date: ${invoice.meta.saleDate.ifBlank { invoice.meta.formattedDate }}", rightMargin, y, textRight)
        y += 12f
        canvas.drawText("Cust: ${invoice.customer.name}", margin, y, textLeft)
        if (invoice.meta.saleTime.isNotBlank()) {
            canvas.drawText("Time: ${invoice.meta.saleTime}", rightMargin, y, textRight)
            y += 12f
        }
        canvas.drawText("Cashier: ${invoice.meta.cashierName}", margin, y, textLeft)
        canvas.drawText("Pay: ${invoice.meta.paymentType}", rightMargin, y, textRight)
        y += 14f

        canvas.drawLine(margin, y, rightMargin, y, linePaint)
        y += 12f

        // Items Header
        canvas.drawText("Item", margin, y, boldLeft)
        canvas.drawText("Qty", 120f, y, boldLeft)
        canvas.drawText("Rate", 160f, y, boldLeft)
        canvas.drawText("Total", rightMargin, y, boldRight)
        y += 6f
        canvas.drawLine(margin, y, rightMargin, y, linePaint)
        y += 12f

        // Items
        for (item in invoice.items) {
            canvas.drawText(item.productName.take(16), margin, y, textLeft)
            canvas.drawText("%.1f".format(item.quantity), 120f, y, textLeft)
            canvas.drawText("%.0f".format(item.unitPrice), 160f, y, textLeft)
            canvas.drawText("%.2f".format(item.totalPrice), rightMargin, y, textRight)
            y += 14f
        }

        y += 4f
        canvas.drawLine(margin, y, rightMargin, y, linePaint)
        y += 14f

        // Totals
        canvas.drawText("Subtotal:", 120f, y, textLeft)
        canvas.drawText("${invoice.totals.currencySymbol} %.2f".format(invoice.totals.subtotal), rightMargin, y, textRight)
        y += 12f

        if (invoice.totals.discount > 0) {
            canvas.drawText("Discount:", 120f, y, textLeft)
            canvas.drawText("-${invoice.totals.currencySymbol} %.2f".format(invoice.totals.discount), rightMargin, y, textRight)
            y += 12f
        }
        if (invoice.totals.taxAmount > 0) {
            canvas.drawText("Tax:", 120f, y, textLeft)
            canvas.drawText("+${invoice.totals.currencySymbol} %.2f".format(invoice.totals.taxAmount), rightMargin, y, textRight)
            y += 12f
        }

        canvas.drawText("TOTAL:", 120f, y, boldLeft)
        canvas.drawText("${invoice.totals.currencySymbol} %.2f".format(invoice.totals.netAmount), rightMargin, y, boldRight)
        y += 14f

        canvas.drawText("Paid:", 120f, y, textLeft)
        canvas.drawText("${invoice.totals.currencySymbol} %.2f".format(invoice.totals.paidAmount), rightMargin, y, textRight)
        y += 12f

        if (invoice.totals.dueAmount > 0) {
            canvas.drawText("Due:", 120f, y, boldLeft)
            canvas.drawText("${invoice.totals.currencySymbol} %.2f".format(invoice.totals.dueAmount), rightMargin, y, boldRight)
            y += 14f
        }

        // QR Code
        val qrBitmap = QrCodeRenderer.generateQrBitmap(invoice.qrPayload, 180, 180)
        if (qrBitmap != null) {
            y += 8f
            val qrDest = RectF(centerX - 40f, y, centerX + 40f, y + 80f)
            canvas.drawBitmap(qrBitmap, null, qrDest, textLeft)
            y += 92f
            canvas.drawText("Scan to verify receipt", centerX, y, textCenter)
            y += 12f
        }

        if (invoice.footerText.isNotBlank()) {
            canvas.drawText(invoice.footerText, centerX, y, textCenter)
            y += 12f
        }
        canvas.drawText("Thank You! Visit Again", centerX, y, boldCenter)

        document.finishPage(page)

        return savePdfDocument(context, document, "Receipt_${sanitizeFilename(invoice.meta.invoiceNumber)}")
    }

    /**
     * Batch export multiple sales transactions to a single combined PDF audit summary for digital record-keeping.
     */
    fun generateBatchInvoicesSummaryPdf(
        context: Context,
        sales: List<Sale>,
        settings: StoreSettings
    ): File? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 16f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 9.5f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.rgb(51, 65, 85)
            textSize = 9f
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 0.8f
        }

        val headerBarPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            style = Paint.Style.FILL
        }

        val whiteBold = Paint().apply {
            color = Color.WHITE
            textSize = 9.5f
            isFakeBoldText = true
            isAntiAlias = true
        }

        var y = 45f
        canvas.drawText("${settings.storeName} - Invoices & Sales Audit Record", 40f, y, titlePaint)
        y += 16f
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        canvas.drawText("Generated on ${sdf.format(Date())} • Total Records: ${sales.size}", 40f, y, textPaint)
        y += 20f

        // Table Header
        val barRect = RectF(40f, y - 8f, 555f, y + 12f)
        canvas.drawRoundRect(barRect, 4f, 4f, headerBarPaint)
        canvas.drawText("Invoice #", 48f, y + 5f, whiteBold)
        canvas.drawText("Date", 130f, y + 5f, whiteBold)
        canvas.drawText("Customer", 225f, y + 5f, whiteBold)
        canvas.drawText("Mode", 355f, y + 5f, whiteBold)
        canvas.drawText("Total (${settings.currencySymbol})", 415f, y + 5f, whiteBold)
        canvas.drawText("Status", 500f, y + 5f, whiteBold)
        y += 22f

        val itemDateFormat = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault())
        var totalAmount = 0.0
        var totalCollected = 0.0
        var totalDue = 0.0

        for (sale in sales.take(35)) {
            totalAmount += sale.netAmount
            totalCollected += sale.paidAmount
            totalDue += sale.dueAmount

            canvas.drawText(sale.invoiceNumber, 48f, y, headerPaint)
            canvas.drawText(itemDateFormat.format(Date(sale.createdAt)), 130f, y, textPaint)
            canvas.drawText(sale.customerName.take(18), 225f, y, textPaint)
            canvas.drawText(sale.paymentType, 355f, y, textPaint)
            canvas.drawText("%.2f".format(sale.netAmount), 415f, y, headerPaint)
            canvas.drawText(if (sale.dueAmount <= 0) "PAID" else "DUE: %.0f".format(sale.dueAmount), 500f, y, textPaint)
            y += 16f
        }

        y += 8f
        canvas.drawLine(40f, y, 555f, y, linePaint)
        y += 18f

        // Audit Totals Summary
        canvas.drawText("SUMMARY TOTALS:", 48f, y, headerPaint)
        canvas.drawText("Total Invoiced: ${settings.currencySymbol} %.2f".format(totalAmount), 160f, y, headerPaint)
        canvas.drawText("Collected: ${settings.currencySymbol} %.2f".format(totalCollected), 320f, y, textPaint)
        canvas.drawText("Pending Due: ${settings.currencySymbol} %.2f".format(totalDue), 450f, y, headerPaint)

        document.finishPage(page)
        return savePdfDocument(context, document, "Invoices_Audit_Report_${System.currentTimeMillis()}")
    }

    private fun savePdfDocument(context: Context, document: PdfDocument, baseFileName: String): File? {
        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        val file = File(outputDir, "${baseFileName}.pdf")

        return try {
            val fos = FileOutputStream(file)
            document.writeTo(fos)
            fos.close()
            document.close()
            file
        } catch (e: Exception) {
            document.close()
            null
        }
    }

    private fun sanitizeFilename(input: String): String {
        return input.replace("/", "_").replace("\\", "_").replace(":", "_").replace(" ", "_")
    }

    /**
     * Open PDF using standard Android Viewer Intent
     */
    fun openPdfFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Open Invoice PDF"))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No PDF viewer app found on device.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error opening PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Share PDF file via Android Chooser
     */
    fun sharePdfFile(context: Context, file: File, title: String = "Share Invoice PDF") {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.nameWithoutExtension)
                putExtra(Intent.EXTRA_TEXT, "Here is the invoice PDF document: ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, title))
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Direct print PDF via Android PrintManager
     */
    fun printPdfFile(context: Context, file: File, jobName: String = "Invoice_Print") {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager != null) {
                val printAdapter = PdfDocumentAdapter(file)
                val attributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(PrintAttributes.Resolution("res1", "default", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
                printManager.print(jobName, printAdapter, attributes)
            } else {
                Toast.makeText(context, "Print service is not available on this device", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Print error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
