package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {

    fun generateInvoicePdf(
        context: Context,
        invoice: PrintableInvoice
    ): File? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42) // Navy 900
            textSize = 20f
            isFakeBoldText = true
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = Color.rgb(51, 65, 85)
            textSize = 14f
            isFakeBoldText = true
            isAntiAlias = true
        }

        var y = 50f

        // Title & Store Info
        canvas.drawText(invoice.header.storeName, 40f, y, titlePaint)
        y += 20f
        if (invoice.header.phone.isNotBlank()) {
            canvas.drawText("Phone: ${invoice.header.phone}", 40f, y, paint)
            y += 16f
        }
        if (invoice.header.address.isNotBlank()) {
            canvas.drawText("Address: ${invoice.header.address}", 40f, y, paint)
            y += 16f
        }

        y += 10f
        canvas.drawLine(40f, y, 555f, y, paint)
        y += 20f

        // Invoice Meta
        canvas.drawText("Invoice #: ${invoice.meta.invoiceNumber}", 40f, y, headerPaint)
        canvas.drawText("Date: ${invoice.meta.formattedDate}", 350f, y, paint)
        y += 18f
        canvas.drawText("Customer: ${invoice.customer.name}", 40f, y, paint)
        canvas.drawText("Cashier: ${invoice.meta.cashierName}", 350f, y, paint)
        y += 18f
        canvas.drawText("Payment Mode: ${invoice.meta.paymentType}", 40f, y, paint)
        y += 25f

        // Items Table Header
        canvas.drawLine(40f, y, 555f, y, paint)
        y += 15f
        canvas.drawText("Item Description", 40f, y, headerPaint)
        canvas.drawText("Qty", 280f, y, headerPaint)
        canvas.drawText("Rate", 360f, y, headerPaint)
        canvas.drawText("Total", 480f, y, headerPaint)
        y += 8f
        canvas.drawLine(40f, y, 555f, y, paint)
        y += 18f

        // Items
        for (item in invoice.items) {
            canvas.drawText(item.productName.take(30), 40f, y, paint)
            canvas.drawText("%.1f %s".format(item.quantity, item.unit), 280f, y, paint)
            canvas.drawText("%.2f".format(item.unitPrice), 360f, y, paint)
            canvas.drawText("%.2f".format(item.totalPrice), 480f, y, paint)
            y += 18f
        }

        y += 10f
        canvas.drawLine(40f, y, 555f, y, paint)
        y += 20f

        // Totals
        canvas.drawText("Subtotal:", 360f, y, paint)
        canvas.drawText("${invoice.totals.currencySymbol} %.2f".format(invoice.totals.subtotal), 480f, y, paint)
        y += 18f
        if (invoice.totals.discount > 0) {
            canvas.drawText("Discount:", 360f, y, paint)
            canvas.drawText("-${invoice.totals.currencySymbol} %.2f".format(invoice.totals.discount), 480f, y, paint)
            y += 18f
        }
        if (invoice.totals.taxAmount > 0) {
            canvas.drawText("Tax:", 360f, y, paint)
            canvas.drawText("+${invoice.totals.currencySymbol} %.2f".format(invoice.totals.taxAmount), 480f, y, paint)
            y += 18f
        }
        canvas.drawText("NET TOTAL:", 360f, y, headerPaint)
        canvas.drawText("${invoice.totals.currencySymbol} %.2f".format(invoice.totals.netAmount), 480f, y, headerPaint)
        y += 20f
        canvas.drawText("Paid Amount:", 360f, y, paint)
        canvas.drawText("${invoice.totals.currencySymbol} %.2f".format(invoice.totals.paidAmount), 480f, y, paint)
        y += 18f
        if (invoice.totals.dueAmount > 0) {
            canvas.drawText("Due Amount:", 360f, y, headerPaint)
            canvas.drawText("${invoice.totals.currencySymbol} %.2f".format(invoice.totals.dueAmount), 480f, y, headerPaint)
            y += 18f
        }

        // Draw QR Code
        val qrBitmap = QrCodeRenderer.generateQrBitmap(invoice.qrPayload, 120, 120)
        if (qrBitmap != null) {
            canvas.drawBitmap(qrBitmap, 40f, y - 40f, paint)
        }

        y += 90f
        if (invoice.footerText.isNotBlank()) {
            canvas.drawText(invoice.footerText, 40f, y, paint)
            y += 16f
        }
        canvas.drawText("Thank you for your business! Powered by CH UMER POS.", 40f, y, paint)

        document.finishPage(page)

        val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
        val file = File(outputDir, "Invoice_${invoice.meta.invoiceNumber.replace("/", "_")}.pdf")

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
}
