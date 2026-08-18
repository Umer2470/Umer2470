package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.entity.Customer
import com.example.data.entity.CustomerLedger
import com.example.data.entity.Expense
import com.example.data.entity.Product
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.data.entity.StoreSettings
import com.example.data.entity.Supplier
import com.example.data.entity.SupplierLedger
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    enum class ReceiptFormat {
        A4, THERMAL_58MM, THERMAL_80MM
    }

    /**
     * Generates a printable PDF invoice from a sale transaction,
     * including store details, itemized list, subtotal, tax, discount, and total amount,
     * using standard Android PdfDocument library.
     */
    fun generatePrintablePdfInvoice(
        context: Context,
        sale: Sale,
        items: List<SaleItem>,
        settings: StoreSettings,
        format: ReceiptFormat = ReceiptFormat.A4,
        cashierName: String? = null
    ): File? {
        val formattedInvoice = InvoiceFormattingService.formatSaleTransaction(
            sale = sale,
            items = items,
            settings = settings,
            cashierName = cashierName
        )
        return generateFromInvoice(context, formattedInvoice, format)
    }

    fun generateAndGetFile(
        context: Context,
        sale: Sale,
        items: List<SaleItem>,
        settings: StoreSettings,
        format: ReceiptFormat = ReceiptFormat.A4,
        cashierName: String? = null
    ): File? {
        return generatePrintablePdfInvoice(context, sale, items, settings, format, cashierName)
    }

    fun generateFromInvoice(
        context: Context,
        invoice: InvoiceFormattingService.PrintableInvoiceStructure,
        format: ReceiptFormat = ReceiptFormat.A4
    ): File? {
        return when (format) {
            ReceiptFormat.A4 -> generateA4InvoicePdfFromStructure(context, invoice)
            ReceiptFormat.THERMAL_58MM -> generateThermalReceiptPdfFromStructure(context, invoice, widthMm = 58)
            ReceiptFormat.THERMAL_80MM -> generateThermalReceiptPdfFromStructure(context, invoice, widthMm = 80)
        }
    }

    private fun getWrappedLines(text: String, maxWidth: Float, paint: Paint): List<String> {
        if (paint.measureText(text) <= maxWidth) return listOf(text)
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine.append(if (currentLine.isEmpty()) word else " $word")
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                    currentLine = StringBuilder(word)
                } else {
                    var remaining = word
                    while (remaining.isNotEmpty()) {
                        var count = paint.breakText(remaining, true, maxWidth, null)
                        if (count <= 0) count = 1
                        lines.add(remaining.substring(0, count))
                        remaining = remaining.substring(count)
                    }
                }
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines.ifEmpty { listOf(text) }
    }

    private fun drawQrCodeOnCanvas(canvas: Canvas, payload: String, x: Float, y: Float, size: Float) {
        try {
            val pxSize = size.toInt().coerceAtLeast(120)
            val writer = com.google.zxing.qrcode.QRCodeWriter()
            val bitMatrix = writer.encode(payload, com.google.zxing.BarcodeFormat.QR_CODE, pxSize, pxSize)
            val w = bitMatrix.width
            val h = bitMatrix.height
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            for (cx in 0 until w) {
                for (cy in 0 until h) {
                    bitmap.setPixel(cx, cy, if (bitMatrix.get(cx, cy)) Color.BLACK else Color.WHITE)
                }
            }
            canvas.drawBitmap(bitmap, null, RectF(x, y, x + size, y + size), null)
        } catch (e: Exception) {
            e.printStackTrace()
            val matrix = QrCodeRenderer.generateQrMatrix(payload)
            val gridSize = matrix.size
            val cellSize = size / gridSize

            val bgPaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawRect(x, y, x + size, y + size, bgPaint)

            val blackPaint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.FILL
                isAntiAlias = false
            }
            for (r in 0 until gridSize) {
                for (c in 0 until gridSize) {
                    if (matrix[r][c]) {
                        canvas.drawRect(
                            x + c * cellSize,
                            y + r * cellSize,
                            x + (c + 1) * cellSize,
                            y + (r + 1) * cellSize,
                            blackPaint
                        )
                    }
                }
            }
        }
    }

    private fun draw1DBarcodeOnCanvas(canvas: Canvas, code: String, startX: Float, startY: Float, width: Float, height: Float) {
        try {
            val cleanCode = code.uppercase(Locale.US).ifBlank { "INV-STORE-0001" }
            val hashBytes = java.security.MessageDigest.getInstance("SHA-256").digest(cleanCode.toByteArray(Charsets.UTF_8))
            val barCount = 50
            val barWidth = width / barCount

            val bgPaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawRect(startX - 2f, startY - 2f, startX + width + 2f, startY + height + 2f, bgPaint)

            val blackPaint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.FILL
                isAntiAlias = false
            }

            var currentX = startX
            for (i in 0 until barCount) {
                val byteIndex = (i / 8) % hashBytes.size
                val bitIndex = i % 8
                val isGuard = (i < 3 || i >= barCount - 3)
                val isBar = isGuard || (((hashBytes[byteIndex].toInt() shr (7 - bitIndex)) and 1) == 1)
                if (isBar) {
                    canvas.drawRect(currentX, startY, currentX + (barWidth * 0.7f), startY + height, blackPaint)
                }
                currentX += barWidth
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun generateA4InvoicePdfFromStructure(
        context: Context,
        invoice: InvoiceFormattingService.PrintableInvoiceStructure
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }

            val primaryColor = Color.rgb(15, 23, 42)
            val brandAccent = Color.rgb(37, 99, 235)
            val textColorDark = Color.rgb(30, 41, 59)
            val textColorMuted = Color.rgb(100, 116, 139)
            val bgLightGray = Color.rgb(248, 250, 252)
            val borderGray = Color.rgb(226, 232, 240)

            var currentY = 30f

            // Top Decorative Brand Bar
            paint.color = brandAccent
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 10f, paint)

            currentY += 15f

            // Header Brand Card
            val boxLeft = 30f
            val boxRight = pageWidth - 30f

            paint.color = bgLightGray
            canvas.drawRoundRect(boxLeft, currentY, boxRight, currentY + 70f, 10f, 10f, paint)

            var textStart = boxLeft + 15f
            if (invoice.header.logoUri.isNotBlank()) {
                loadAndDrawLogo(canvas, invoice.header.logoUri, boxLeft + 12f, currentY + 10f, 50f, 50f)
                textStart = boxLeft + 70f
            }

            paint.apply {
                color = primaryColor
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText(invoice.header.storeName, textStart, currentY + 28f, paint)

            paint.apply {
                color = textColorMuted
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            canvas.drawText("Proprietor: ${invoice.header.ownerName}  •  Ph: ${invoice.header.phone}", textStart, currentY + 44f, paint)
            canvas.drawText("Address: ${invoice.header.address}", textStart, currentY + 58f, paint)

            paint.apply {
                color = brandAccent
                textSize = 15f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText(invoice.meta.transactionType, boxRight - 15f, currentY + 30f, paint)

            currentY += 80f

            // Invoice & Customer Meta Banner
            paint.color = borderGray
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRoundRect(boxLeft, currentY, boxRight, currentY + 52f, 8f, 8f, paint)
            paint.style = Paint.Style.FILL

            paint.apply {
                color = textColorDark
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("Customer:", boxLeft + 12f, currentY + 20f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(invoice.customer.name, boxLeft + 65f, currentY + 20f, paint)

            canvas.drawText("Cashier: ${invoice.meta.cashierName}", boxLeft + 12f, currentY + 38f, paint)

            paint.textAlign = Paint.Align.RIGHT
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Invoice #: ${invoice.meta.invoiceId}", boxRight - 12f, currentY + 20f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Date: ${invoice.meta.formattedDate}  |  Pay Mode: ${invoice.meta.paymentType}", boxRight - 12f, currentY + 38f, paint)

            currentY += 64f

            // Items Table Header
            paint.color = primaryColor
            canvas.drawRoundRect(boxLeft, currentY, boxRight, currentY + 24f, 6f, 6f, paint)

            paint.apply {
                color = Color.WHITE
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val colSr = boxLeft + 8f
            val colItem = boxLeft + 35f
            val colItemWidth = 230f
            val colQty = boxLeft + 280f
            val colRate = boxLeft + 350f
            val colDisc = boxLeft + 430f
            val colTotal = boxRight - 12f

            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("#", colSr, currentY + 16f, paint)
            canvas.drawText("Product Name", colItem, currentY + 16f, paint)

            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("Qty", colQty, currentY + 16f, paint)

            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Unit Price", colRate, currentY + 16f, paint)
            canvas.drawText("Discount", colDisc, currentY + 16f, paint)
            canvas.drawText("Total (${invoice.header.currencySymbol})", colTotal, currentY + 16f, paint)

            currentY += 30f

            // Items Rows with Automatic Word Wrapping
            paint.apply {
                color = textColorDark
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            for (item in invoice.items) {
                if (currentY > pageHeight - 180f) break

                val wrappedLines = getWrappedLines(item.productName, colItemWidth, paint)
                val lineLeading = 13f
                val rowHeight = maxOf(22f, wrappedLines.size * lineLeading + 8f)

                if (item.srNo % 2 == 0) {
                    val bgPaint = Paint().apply { color = Color.rgb(248, 250, 252) }
                    canvas.drawRect(boxLeft, currentY - 4f, boxRight, currentY + rowHeight - 4f, bgPaint)
                }

                paint.textAlign = Paint.Align.LEFT
                canvas.drawText("${item.srNo}", colSr, currentY + 10f, paint)

                var textY = currentY + 10f
                for (line in wrappedLines) {
                    canvas.drawText(line, colItem, textY, paint)
                    textY += lineLeading
                }

                paint.textAlign = Paint.Align.CENTER
                val qtyStr = if (item.quantity % 1.0 == 0.0) "${item.quantity.toInt()} ${item.unit}" else "${item.quantity} ${item.unit}"
                canvas.drawText(qtyStr, colQty, currentY + 10f, paint)

                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText(String.format(Locale.US, "%.0f", item.unitPrice), colRate, currentY + 10f, paint)
                canvas.drawText(String.format(Locale.US, "%.0f", item.discount), colDisc, currentY + 10f, paint)
                canvas.drawText(String.format(Locale.US, "%.0f", item.totalPrice), colTotal, currentY + 10f, paint)

                currentY += rowHeight
            }

            paint.color = borderGray
            canvas.drawLine(boxLeft, currentY, boxRight, currentY, paint)

            currentY += 15f

            // Bottom Section: Summary Box (Right) + QR & Barcode (Left)
            val summaryWidth = 230f
            val summaryBoxLeft = boxRight - summaryWidth
            val summaryBoxRight = boxRight

            paint.color = bgLightGray
            canvas.drawRoundRect(summaryBoxLeft, currentY, summaryBoxRight, currentY + 110f, 8f, 8f, paint)

            var summaryY = currentY + 18f

            fun drawSummaryRow(label: String, value: String, isBold: Boolean = false, isHighlight: Boolean = false) {
                paint.apply {
                    color = if (isHighlight) brandAccent else textColorDark
                    textSize = if (isBold) 10.5f else 9.5f
                    typeface = Typeface.create(Typeface.DEFAULT, if (isBold) Typeface.BOLD else Typeface.NORMAL)
                    textAlign = Paint.Align.LEFT
                }
                canvas.drawText(label, summaryBoxLeft + 12f, summaryY, paint)

                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText(value, summaryBoxRight - 12f, summaryY, paint)

                summaryY += 18f
            }

            drawSummaryRow("Subtotal:", "${invoice.header.currencySymbol} ${String.format(Locale.US, "%.0f", invoice.totals.subtotal)}")
            if (invoice.totals.discount > 0) {
                drawSummaryRow("Discount:", "- ${invoice.header.currencySymbol} ${String.format(Locale.US, "%.0f", invoice.totals.discount)}")
            }
            drawSummaryRow("Net Payable:", "${invoice.header.currencySymbol} ${String.format(Locale.US, "%.0f", invoice.totals.netAmount)}", isBold = true, isHighlight = true)
            drawSummaryRow("Paid Amount:", "${invoice.header.currencySymbol} ${String.format(Locale.US, "%.0f", invoice.totals.paidAmount)}")
            if (invoice.totals.dueAmount > 0) {
                drawSummaryRow("Remaining Due:", "${invoice.header.currencySymbol} ${String.format(Locale.US, "%.0f", invoice.totals.dueAmount)}", isBold = true)
            }

            // Draw QR Code and Barcode on Left Side
            val qrSize = 70f
            val qrX = boxLeft + 10f
            val qrY = currentY + 5f
            val qrPayload = InvoiceFormattingService.getQrCodePayload(
                storeName = invoice.header.storeName,
                invoiceNumber = invoice.meta.invoiceId,
                formattedDate = invoice.meta.formattedDate,
                customerName = invoice.customer.name,
                totalAmount = invoice.totals.netAmount,
                currencySymbol = invoice.header.currencySymbol,
                paidAmount = invoice.totals.paidAmount,
                dueAmount = invoice.totals.dueAmount,
                paymentType = invoice.meta.paymentType,
                timestamp = invoice.meta.timestamp
            )
            drawQrCodeOnCanvas(canvas, qrPayload, qrX, qrY, qrSize)

            val verId = InvoiceFormattingService.getInvoiceVerificationId(invoice.meta.invoiceId, invoice.meta.timestamp)
            paint.apply {
                color = textColorMuted
                textSize = 7f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("VERIFICATION ID:", qrX, qrY + qrSize + 10f, paint)
            canvas.drawText(verId, qrX, qrY + qrSize + 19f, paint)

            val barcodeX = qrX + qrSize + 20f
            val barcodeY = currentY + 15f
            val barcodeWidth = 140f
            val barcodeHeight = 35f
            draw1DBarcodeOnCanvas(canvas, invoice.meta.invoiceId, barcodeX, barcodeY, barcodeWidth, barcodeHeight)

            paint.apply {
                color = textColorMuted
                textSize = 8.5f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(invoice.meta.invoiceId, barcodeX + (barcodeWidth / 2f), barcodeY + barcodeHeight + 12f, paint)

            currentY += 125f

            // Terms & Footer
            paint.apply {
                color = textColorMuted
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("Terms & Conditions: ${invoice.termsAndConditions}", boxLeft + 5f, currentY, paint)

            currentY = pageHeight - 40f
            paint.color = borderGray
            canvas.drawLine(boxLeft, currentY, boxRight, currentY, paint)

            currentY += 16f
            paint.apply {
                color = primaryColor
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("*** Thank You For Shopping With ${invoice.header.storeName}! ***", pageWidth / 2f, currentY, paint)

            pdfDocument.finishPage(page)

            val invoicesDir = File(context.cacheDir, "invoices").apply { if (!exists()) mkdirs() }
            val cleanInvId = invoice.meta.invoiceId.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val pdfFile = File(invoicesDir, "Invoice_${cleanInvId}_A4.pdf")
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun generateThermalReceiptPdfFromStructure(
        context: Context,
        invoice: InvoiceFormattingService.PrintableInvoiceStructure,
        widthMm: Int = 58
    ): File? {
        return try {
            val pdfDocument = PdfDocument()

            val pageWidth = if (widthMm == 58) 204 else 283
            val margin = 8f
            val rightMargin = pageWidth - 8f
            val maxItemWidth = rightMargin - margin

            val tempPaint = Paint().apply {
                isAntiAlias = true
                textSize = 8.5f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }

            var itemsTotalHeight = 0f
            for (item in invoice.items) {
                val lines = getWrappedLines(item.productName, maxItemWidth, tempPaint)
                itemsTotalHeight += (lines.size * 11f) + 16f
            }

            val estimatedHeight = (360f + itemsTotalHeight).toInt().coerceAtLeast(450)
            val pageHeight = estimatedHeight

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }
            var currentY = 18f

            val center = pageWidth / 2f

            val thermalTypeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            val thermalNormal = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)

            // Header Store Brand
            paint.apply {
                color = Color.BLACK
                textSize = if (widthMm == 58) 12f else 14f
                typeface = thermalTypeface
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(invoice.header.storeName.uppercase(Locale.getDefault()), center, currentY, paint)

            currentY += 13f
            paint.textSize = 8f
            paint.typeface = thermalNormal
            canvas.drawText("Ph: ${invoice.header.phone}", center, currentY, paint)

            currentY += 11f
            val addrLines = getWrappedLines(invoice.header.address, maxItemWidth, paint)
            for (line in addrLines.take(2)) {
                canvas.drawText(line, center, currentY, paint)
                currentY += 11f
            }

            fun drawDashedLine() {
                val linePaint = Paint().apply {
                    color = Color.BLACK
                    strokeWidth = 1f
                    style = Paint.Style.STROKE
                    pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
                }
                canvas.drawLine(margin, currentY, rightMargin, currentY, linePaint)
                currentY += 10f
            }

            drawDashedLine()

            paint.apply {
                textSize = 8f
                typeface = thermalNormal
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("INV: ${invoice.meta.invoiceId}", margin, currentY, paint)

            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("PAY: ${invoice.meta.paymentType}", rightMargin, currentY, paint)

            currentY += 11f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("CUST: ${invoice.customer.name}", margin, currentY, paint)

            currentY += 11f
            canvas.drawText("CASHIER: ${invoice.meta.cashierName}", margin, currentY, paint)

            currentY += 11f
            canvas.drawText("DATE: ${invoice.meta.formattedDate}", margin, currentY, paint)

            currentY += 12f
            drawDashedLine()

            // Itemized list with word wrapping
            for (item in invoice.items) {
                paint.apply {
                    textSize = 8.5f
                    typeface = thermalTypeface
                    textAlign = Paint.Align.LEFT
                }
                val nameLines = getWrappedLines("${item.srNo}. ${item.productName}", maxItemWidth, paint)
                for (line in nameLines) {
                    canvas.drawText(line, margin, currentY, paint)
                    currentY += 11f
                }

                paint.typeface = thermalNormal
                val qtyStr = if (item.quantity % 1.0 == 0.0) "${item.quantity.toInt()} ${item.unit}" else "${item.quantity} ${item.unit}"
                canvas.drawText("  $qtyStr x ${String.format(Locale.US, "%.0f", item.unitPrice)}", margin, currentY, paint)

                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("${invoice.header.currencySymbol} ${String.format(Locale.US, "%.0f", item.totalPrice)}", rightMargin, currentY, paint)

                currentY += 14f
            }

            drawDashedLine()

            // Totals
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("Subtotal:", margin, currentY, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("${invoice.header.currencySymbol} ${String.format(Locale.US, "%.0f", invoice.totals.subtotal)}", rightMargin, currentY, paint)
            currentY += 12f

            if (invoice.totals.discount > 0) {
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText("Discount:", margin, currentY, paint)
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("- ${invoice.header.currencySymbol} ${String.format(Locale.US, "%.0f", invoice.totals.discount)}", rightMargin, currentY, paint)
                currentY += 12f
            }

            paint.apply {
                textSize = 9.5f
                typeface = thermalTypeface
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("NET AMOUNT:", margin, currentY, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("${invoice.header.currencySymbol} ${String.format(Locale.US, "%.0f", invoice.totals.netAmount)}", rightMargin, currentY, paint)

            currentY += 14f
            paint.apply {
                textSize = 8.5f
                typeface = thermalNormal
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("Paid Amount:", margin, currentY, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("${invoice.header.currencySymbol} ${String.format(Locale.US, "%.0f", invoice.totals.paidAmount)}", rightMargin, currentY, paint)

            if (invoice.totals.dueAmount > 0) {
                currentY += 12f
                paint.apply {
                    textSize = 8.5f
                    typeface = thermalTypeface
                    textAlign = Paint.Align.LEFT
                }
                canvas.drawText("Remaining Due:", margin, currentY, paint)
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("${invoice.header.currencySymbol} ${String.format(Locale.US, "%.0f", invoice.totals.dueAmount)}", rightMargin, currentY, paint)
            }

            currentY += 14f
            drawDashedLine()

            // QR Code & Barcode on Receipt
            val thermalQrSize = 60f
            val thermalQrX = center - (thermalQrSize / 2f)
            val thermalQrPayload = InvoiceFormattingService.getQrCodePayload(
                storeName = invoice.header.storeName,
                invoiceNumber = invoice.meta.invoiceId,
                formattedDate = invoice.meta.formattedDate,
                customerName = invoice.customer.name,
                totalAmount = invoice.totals.netAmount,
                currencySymbol = invoice.header.currencySymbol,
                paidAmount = invoice.totals.paidAmount,
                dueAmount = invoice.totals.dueAmount,
                paymentType = invoice.meta.paymentType,
                timestamp = invoice.meta.timestamp
            )
            drawQrCodeOnCanvas(canvas, thermalQrPayload, thermalQrX, currentY, thermalQrSize)
            currentY += thermalQrSize + 6f

            val verId = InvoiceFormattingService.getInvoiceVerificationId(invoice.meta.invoiceId, invoice.meta.timestamp)
            paint.apply {
                textSize = 7.5f
                typeface = thermalTypeface
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("SCAN QR TO VERIFY INVOICE", center, currentY, paint)
            currentY += 10f
            canvas.drawText("VER ID: $verId", center, currentY, paint)
            currentY += 12f

            val thermalBarcodeW = pageWidth - 40f
            val thermalBarcodeH = 25f
            val thermalBarcodeX = 20f
            draw1DBarcodeOnCanvas(canvas, invoice.meta.invoiceId, thermalBarcodeX, currentY, thermalBarcodeW, thermalBarcodeH)
            currentY += thermalBarcodeH + 12f

            paint.apply {
                textSize = 8f
                typeface = thermalTypeface
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("THANK YOU FOR YOUR VISIT!", center, currentY, paint)

            pdfDocument.finishPage(page)

            val invoicesDir = File(context.cacheDir, "invoices").apply { if (!exists()) mkdirs() }
            val cleanInvId = invoice.meta.invoiceId.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val pdfFile = File(invoicesDir, "Receipt_${cleanInvId}_${widthMm}mm.pdf")
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun generateA4InvoicePdf(
        context: Context,
        sale: Sale,
        items: List<SaleItem>,
        settings: StoreSettings
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }

            val primaryColor = Color.rgb(15, 23, 42) // Slate 900
            val brandAccent = Color.rgb(37, 99, 235) // Blue 600
            val textColorDark = Color.rgb(30, 41, 59) // Slate 800
            val textColorMuted = Color.rgb(100, 116, 139) // Slate 500
            val bgLightGray = Color.rgb(248, 250, 252) // Slate 50
            val borderGray = Color.rgb(226, 232, 240) // Slate 200

            var currentY = 35f

            // 1. Top Decorative Brand Bar
            paint.color = brandAccent
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 12f, paint)

            currentY += 20f

            // 2. Header Brand Card
            val boxLeft = 35f
            val boxRight = pageWidth - 35f

            paint.color = bgLightGray
            canvas.drawRoundRect(boxLeft, currentY, boxRight, currentY + 75f, 12f, 12f, paint)

            var textStart = boxLeft + 20f
            if (settings.logoUri.isNotBlank()) {
                loadAndDrawLogo(canvas, settings.logoUri, boxLeft + 15f, currentY + 12f, 50f, 50f)
                textStart = boxLeft + 75f
            }

            paint.apply {
                color = primaryColor
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText(settings.storeName, textStart, currentY + 30f, paint)

            paint.apply {
                color = textColorMuted
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            canvas.drawText("Proprietor: ${settings.ownerName}  •  Ph: ${settings.phone}", textStart, currentY + 48f, paint)
            canvas.drawText("Address: ${settings.address}", textStart, currentY + 62f, paint)

            paint.apply {
                color = brandAccent
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("TAX INVOICE", boxRight - 20f, currentY + 32f, paint)

            currentY += 90f

            // 3. Invoice & Customer Meta Banner
            paint.color = borderGray
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1f
            canvas.drawRoundRect(boxLeft, currentY, boxRight, currentY + 50f, 8f, 8f, paint)
            paint.style = Paint.Style.FILL

            val dateFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val formattedDate = dateFormatter.format(Date(sale.timestamp))

            paint.apply {
                color = textColorDark
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("Bill To:", boxLeft + 15f, currentY + 22f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(sale.customerName, boxLeft + 60f, currentY + 22f, paint)

            canvas.drawText("Payment Mode: ${sale.paymentType}", boxLeft + 15f, currentY + 38f, paint)

            paint.textAlign = Paint.Align.RIGHT
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Invoice #: ${sale.invoiceNumber}", boxRight - 15f, currentY + 22f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Date: $formattedDate", boxRight - 15f, currentY + 38f, paint)

            currentY += 65f

            // 4. Items Table Header
            paint.color = primaryColor
            canvas.drawRoundRect(boxLeft, currentY, boxRight, currentY + 26f, 6f, 6f, paint)

            paint.apply {
                color = Color.WHITE
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val colSr = boxLeft + 10f
            val colItem = boxLeft + 45f
            val colQty = boxLeft + 310f
            val colRate = boxLeft + 400f
            val colTotal = boxRight - 15f

            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("#", colSr, currentY + 17f, paint)
            canvas.drawText("Item Description", colItem, currentY + 17f, paint)

            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("Qty", colQty, currentY + 17f, paint)

            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Rate (${settings.currencySymbol})", colRate, currentY + 17f, paint)
            canvas.drawText("Total (${settings.currencySymbol})", colTotal, currentY + 17f, paint)

            currentY += 34f

            // 5. Items Rows
            paint.apply {
                color = textColorDark
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            var srNo = 1
            for (item in items) {
                if (currentY > pageHeight - 160f) break

                if (srNo % 2 == 0) {
                    val bgPaint = Paint().apply { color = Color.rgb(248, 250, 252) }
                    canvas.drawRect(boxLeft, currentY - 12f, boxRight, currentY + 10f, bgPaint)
                }

                paint.textAlign = Paint.Align.LEFT
                canvas.drawText("$srNo", colSr, currentY, paint)

                var pName = item.productName
                if (pName.length > 38) pName = pName.take(35) + "..."
                canvas.drawText(pName, colItem, currentY, paint)

                paint.textAlign = Paint.Align.CENTER
                val qtyStr = if (item.quantity % 1.0 == 0.0) "${item.quantity.toInt()} ${item.unit}" else "${item.quantity} ${item.unit}"
                canvas.drawText(qtyStr, colQty, currentY, paint)

                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText(String.format(Locale.US, "%.0f", item.salePrice), colRate, currentY, paint)
                canvas.drawText(String.format(Locale.US, "%.0f", item.totalPrice), colTotal, currentY, paint)

                currentY += 22f
                srNo++
            }

            paint.color = borderGray
            canvas.drawLine(boxLeft, currentY, boxRight, currentY, paint)

            currentY += 20f

            // 6. Summary Totals Box
            val summaryBoxLeft = pageWidth - 240f
            val summaryBoxRight = boxRight

            paint.color = bgLightGray
            canvas.drawRoundRect(summaryBoxLeft, currentY, summaryBoxRight, currentY + 115f, 8f, 8f, paint)

            var summaryY = currentY + 22f

            fun drawSummaryRow(label: String, value: String, isBold: Boolean = false, isHighlight: Boolean = false) {
                paint.apply {
                    color = if (isHighlight) brandAccent else textColorDark
                    textSize = if (isBold) 11f else 10f
                    typeface = Typeface.create(Typeface.DEFAULT, if (isBold) Typeface.BOLD else Typeface.NORMAL)
                    textAlign = Paint.Align.LEFT
                }
                canvas.drawText(label, summaryBoxLeft + 15f, summaryY, paint)

                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText(value, summaryBoxRight - 15f, summaryY, paint)

                summaryY += 20f
            }

            drawSummaryRow("Subtotal:", "${settings.currencySymbol} ${String.format(Locale.US, "%.0f", sale.totalAmount)}")
            if (sale.discount > 0) {
                drawSummaryRow("Discount:", "- ${settings.currencySymbol} ${String.format(Locale.US, "%.0f", sale.discount)}")
            }
            drawSummaryRow("Net Payable:", "${settings.currencySymbol} ${String.format(Locale.US, "%.0f", sale.netAmount)}", isBold = true, isHighlight = true)
            drawSummaryRow("Paid Amount:", "${settings.currencySymbol} ${String.format(Locale.US, "%.0f", sale.paidAmount)}")
            if (sale.dueAmount > 0) {
                drawSummaryRow("Remaining Udhaar:", "${settings.currencySymbol} ${String.format(Locale.US, "%.0f", sale.dueAmount)}", isBold = true)
            }

            currentY += 135f

            // 7. Terms & Footer
            paint.apply {
                color = textColorMuted
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("Terms & Conditions: Goods once sold can be exchanged within 7 days with valid original bill.", boxLeft + 5f, currentY, paint)
            currentY += 14f
            canvas.drawText("Custom cut pipes, mixed paints or altered building materials are non-returnable.", boxLeft + 5f, currentY, paint)

            currentY = pageHeight - 45f
            paint.color = borderGray
            canvas.drawLine(boxLeft, currentY, boxRight, currentY, paint)

            currentY += 18f
            paint.apply {
                color = primaryColor
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("*** Thank You For Shopping With ${settings.storeName}! ***", pageWidth / 2f, currentY, paint)

            pdfDocument.finishPage(page)

            val invoicesDir = File(context.cacheDir, "invoices").apply { if (!exists()) mkdirs() }
            val pdfFile = File(invoicesDir, "Invoice_${sale.invoiceNumber}_A4.pdf")
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Thermal Receipt PDF generator (58mm or 80mm roll printer format)
     */
    private fun generateThermalReceiptPdf(
        context: Context,
        sale: Sale,
        items: List<SaleItem>,
        settings: StoreSettings,
        widthMm: Int = 58
    ): File? {
        return try {
            val pdfDocument = PdfDocument()

            // 58mm = ~164 - 204 points width (72 pt / inch -> 2.28 in = 164 pt, using 204 pt for clean 200dpi thermal receipt rendering)
            // 80mm = ~283 points width (3.15 in = 226 - 283 pt)
            val pageWidth = if (widthMm == 58) 204 else 283
            // Dynamic page height based on item count
            val estimatedHeight = 320 + (items.size * 22)
            val pageHeight = estimatedHeight.coerceAtLeast(400)

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }

            var currentY = 20f

            val center = pageWidth / 2f
            val margin = 8f
            val rightMargin = pageWidth - 8f

            // Monospace / Crisp Thermal Font
            val thermalTypeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            val thermalNormal = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)

            // Store Name Header
            paint.apply {
                color = Color.BLACK
                textSize = if (widthMm == 58) 12f else 14f
                typeface = thermalTypeface
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(settings.storeName.uppercase(Locale.getDefault()), center, currentY, paint)

            currentY += 14f
            paint.textSize = 8f
            paint.typeface = thermalNormal
            canvas.drawText("Ph: ${settings.phone}", center, currentY, paint)

            currentY += 12f
            canvas.drawText(settings.address.take(35), center, currentY, paint)

            currentY += 14f

            // Dashed Separator Line
            fun drawDashedLine() {
                val linePaint = Paint().apply {
                    color = Color.BLACK
                    strokeWidth = 1f
                    style = Paint.Style.STROKE
                    pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
                }
                canvas.drawLine(margin, currentY, rightMargin, currentY, linePaint)
                currentY += 12f
            }

            drawDashedLine()

            // Meta Info
            val dateFormatter = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault())
            val formattedDate = dateFormatter.format(Date(sale.timestamp))

            paint.apply {
                textSize = 8f
                typeface = thermalNormal
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("INV: ${sale.invoiceNumber}", margin, currentY, paint)

            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText(sale.paymentType, rightMargin, currentY, paint)

            currentY += 12f
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("CUST: ${sale.customerName.take(22)}", margin, currentY, paint)

            currentY += 12f
            canvas.drawText("DATE: $formattedDate", margin, currentY, paint)

            currentY += 14f
            drawDashedLine()

            // Item Headers
            paint.apply {
                textSize = 8f
                typeface = thermalTypeface
                textAlign = Paint.Align.LEFT
            }

            val colQtyWidth = if (widthMm == 58) 30f else 45f
            val colPriceWidth = if (widthMm == 58) 45f else 60f

            canvas.drawText("ITEM", margin, currentY, paint)
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("QTY", rightMargin - colPriceWidth, currentY, paint)
            canvas.drawText("AMT", rightMargin, currentY, paint)

            currentY += 14f

            // Item Rows
            paint.typeface = thermalNormal
            for (item in items) {
                paint.textAlign = Paint.Align.LEFT
                val pName = if (widthMm == 58 && item.productName.length > 15) item.productName.take(13) + ".." else item.productName.take(22)
                canvas.drawText(pName, margin, currentY, paint)

                paint.textAlign = Paint.Align.RIGHT
                val qtyStr = if (item.quantity % 1.0 == 0.0) "${item.quantity.toInt()}" else "${item.quantity}"
                canvas.drawText(qtyStr, rightMargin - colPriceWidth, currentY, paint)
                canvas.drawText("${item.totalPrice.toInt()}", rightMargin, currentY, paint)

                currentY += 14f
            }

            drawDashedLine()

            // Totals
            fun drawThermalTotalRow(label: String, valStr: String, isBold: Boolean = false) {
                paint.apply {
                    textSize = if (isBold) 9.5f else 8.5f
                    typeface = if (isBold) thermalTypeface else thermalNormal
                    textAlign = Paint.Align.LEFT
                }
                canvas.drawText(label, margin, currentY, paint)

                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText(valStr, rightMargin, currentY, paint)

                currentY += 14f
            }

            drawThermalTotalRow("TOTAL:", "${settings.currencySymbol} ${sale.totalAmount.toInt()}")
            if (sale.discount > 0) {
                drawThermalTotalRow("DISCOUNT:", "-${settings.currencySymbol} ${sale.discount.toInt()}")
            }
            drawThermalTotalRow("NET PAYABLE:", "${settings.currencySymbol} ${sale.netAmount.toInt()}", isBold = true)
            drawThermalTotalRow("PAID:", "${settings.currencySymbol} ${sale.paidAmount.toInt()}")
            if (sale.dueAmount > 0) {
                drawThermalTotalRow("DUE BALANCE:", "${settings.currencySymbol} ${sale.dueAmount.toInt()}", isBold = true)
            }

            drawDashedLine()

            // Footer
            paint.apply {
                textSize = 8f
                typeface = thermalNormal
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("*** THANK YOU ***", center, currentY, paint)
            currentY += 12f
            canvas.drawText("NO REFUND WITHOUT RECEIPT", center, currentY, paint)

            pdfDocument.finishPage(page)

            val invoicesDir = File(context.cacheDir, "invoices").apply { if (!exists()) mkdirs() }
            val pdfFile = File(invoicesDir, "Receipt_${sale.invoiceNumber}_${widthMm}mm.pdf")
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateCustomerLedgerPdf(
        context: Context,
        customer: Customer,
        ledgers: List<CustomerLedger>,
        settings: StoreSettings
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }

            val primaryColor = Color.rgb(15, 23, 42)
            val brandAccent = Color.rgb(37, 99, 235)
            val textColorDark = Color.rgb(30, 41, 59)
            val textColorMuted = Color.rgb(100, 116, 139)
            val bgLightGray = Color.rgb(248, 250, 252)
            val borderGray = Color.rgb(226, 232, 240)
            val creditRed = Color.rgb(225, 29, 72)
            val debitGreen = Color.rgb(16, 185, 129)

            var currentY = 40f

            paint.color = brandAccent
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 12f, paint)

            currentY += 15f
            paint.apply {
                color = primaryColor
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(settings.storeName, pageWidth / 2f, currentY, paint)

            currentY += 16f
            paint.apply {
                color = textColorMuted
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            canvas.drawText("Proprietor: ${settings.ownerName}  |  Phone: ${settings.phone}", pageWidth / 2f, currentY, paint)

            currentY += 14f
            canvas.drawText("Address: ${settings.address}", pageWidth / 2f, currentY, paint)

            currentY += 15f
            paint.color = borderGray
            paint.strokeWidth = 1.5f
            canvas.drawLine(35f, currentY, pageWidth - 35f, currentY, paint)

            currentY += 25f
            val boxLeft = 35f
            val boxRight = pageWidth - 35f

            paint.color = bgLightGray
            canvas.drawRoundRect(boxLeft, currentY - 15f, boxRight, currentY + 30f, 8f, 8f, paint)

            paint.apply {
                color = primaryColor
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("CUSTOMER LEDGER STATEMENT (UDHAAR)", boxLeft + 15f, currentY + 5f, paint)

            paint.apply {
                color = if (customer.balance > 0) creditRed else debitGreen
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("Pending Balance: ${settings.currencySymbol} ${customer.balance.toInt()}", boxRight - 15f, currentY + 5f, paint)

            currentY += 45f

            paint.apply {
                color = textColorDark
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("Customer Name: ${customer.name}", boxLeft, currentY, paint)
            canvas.drawText("Phone: ${customer.phone.ifBlank { "N/A" }}", boxLeft + 220f, currentY, paint)

            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Date: $dateStr", boxRight, currentY, paint)

            currentY += 20f

            paint.color = primaryColor
            canvas.drawRoundRect(boxLeft, currentY, boxRight, currentY + 24f, 4f, 4f, paint)

            paint.apply {
                color = Color.WHITE
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("Date", boxLeft + 10f, currentY + 16f, paint)
            canvas.drawText("Description", boxLeft + 90f, currentY + 16f, paint)
            canvas.drawText("Type / Mode", boxLeft + 280f, currentY + 16f, paint)

            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Amount (${settings.currencySymbol})", boxLeft + 420f, currentY + 16f, paint)
            canvas.drawText("Balance (${settings.currencySymbol})", boxRight - 10f, currentY + 16f, paint)

            currentY += 30f

            val dateFormat = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault())
            paint.apply {
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            for (ledger in ledgers.take(25)) {
                if (currentY > pageHeight - 80f) break

                val isCredit = ledger.type == "CREDIT_SALE" || ledger.type == "DEBIT_ADJUSTMENT"
                paint.color = textColorDark

                paint.textAlign = Paint.Align.LEFT
                canvas.drawText(dateFormat.format(Date(ledger.timestamp)), boxLeft + 10f, currentY, paint)

                val desc = if (ledger.description.length > 28) ledger.description.take(26) + ".." else ledger.description
                canvas.drawText(desc, boxLeft + 90f, currentY, paint)

                val typeTag = if (isCredit) "Udhaar (+)" else "Payment (-)"
                paint.color = if (isCredit) creditRed else debitGreen
                canvas.drawText("$typeTag • ${ledger.paymentMethod}", boxLeft + 280f, currentY, paint)

                paint.textAlign = Paint.Align.RIGHT
                paint.color = if (isCredit) creditRed else debitGreen
                canvas.drawText("${if (isCredit) "+" else "-"} ${ledger.amount.toInt()}", boxLeft + 420f, currentY, paint)

                paint.color = textColorDark
                canvas.drawText("${ledger.balanceAfter.toInt()}", boxRight - 10f, currentY, paint)

                currentY += 18f
                paint.color = borderGray
                paint.strokeWidth = 0.5f
                canvas.drawLine(boxLeft, currentY - 12f, boxRight, currentY - 12f, paint)
            }

            currentY += 20f
            paint.color = borderGray
            paint.strokeWidth = 1f
            canvas.drawLine(boxLeft, currentY, boxRight, currentY, paint)

            currentY += 25f
            paint.apply {
                color = primaryColor
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("*** Statement Generated by ${settings.storeName} ***", pageWidth / 2f, currentY, paint)

            pdfDocument.finishPage(page)

            val dir = File(context.cacheDir, "ledgers").apply { if (!exists()) mkdirs() }
            val pdfFile = File(dir, "Customer_Ledger_${customer.name.replace(" ", "_")}.pdf")
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateSupplierLedgerPdf(
        context: Context,
        supplier: Supplier,
        ledgers: List<SupplierLedger>,
        settings: StoreSettings
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }

            val primaryColor = Color.rgb(15, 23, 42)
            val brandAccent = Color.rgb(37, 99, 235)
            val textColorDark = Color.rgb(30, 41, 59)
            val textColorMuted = Color.rgb(100, 116, 139)
            val bgLightGray = Color.rgb(248, 250, 252)
            val borderGray = Color.rgb(226, 232, 240)
            val creditRed = Color.rgb(225, 29, 72)
            val debitGreen = Color.rgb(16, 185, 129)

            var currentY = 40f

            paint.color = brandAccent
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 12f, paint)

            currentY += 15f
            paint.apply {
                color = primaryColor
                textSize = 22f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(settings.storeName, pageWidth / 2f, currentY, paint)

            currentY += 16f
            paint.apply {
                color = textColorMuted
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            canvas.drawText("Proprietor: ${settings.ownerName}  |  Phone: ${settings.phone}", pageWidth / 2f, currentY, paint)

            currentY += 14f
            canvas.drawText("Address: ${settings.address}", pageWidth / 2f, currentY, paint)

            currentY += 15f
            paint.color = borderGray
            paint.strokeWidth = 1.5f
            canvas.drawLine(35f, currentY, pageWidth - 35f, currentY, paint)

            currentY += 25f
            val boxLeft = 35f
            val boxRight = pageWidth - 35f

            paint.color = bgLightGray
            canvas.drawRoundRect(boxLeft, currentY - 15f, boxRight, currentY + 30f, 8f, 8f, paint)

            paint.apply {
                color = primaryColor
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("SUPPLIER LEDGER STATEMENT", boxLeft + 15f, currentY + 5f, paint)

            paint.apply {
                color = creditRed
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("Store Payable Balance: ${settings.currencySymbol} ${supplier.payableBalance.toInt()}", boxRight - 15f, currentY + 5f, paint)

            currentY += 45f

            paint.apply {
                color = textColorDark
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("Vendor Name: ${supplier.name}", boxLeft, currentY, paint)
            canvas.drawText("Company: ${supplier.company.ifBlank { "N/A" }}", boxLeft + 220f, currentY, paint)

            val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Date: $dateStr", boxRight, currentY, paint)

            currentY += 20f

            paint.color = primaryColor
            canvas.drawRoundRect(boxLeft, currentY, boxRight, currentY + 24f, 4f, 4f, paint)

            paint.apply {
                color = Color.WHITE
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            paint.textAlign = Paint.Align.LEFT
            canvas.drawText("Date", boxLeft + 10f, currentY + 16f, paint)
            canvas.drawText("Description", boxLeft + 90f, currentY + 16f, paint)
            canvas.drawText("Type / Mode", boxLeft + 280f, currentY + 16f, paint)

            paint.textAlign = Paint.Align.RIGHT
            canvas.drawText("Amount (${settings.currencySymbol})", boxLeft + 420f, currentY + 16f, paint)
            canvas.drawText("Balance (${settings.currencySymbol})", boxRight - 10f, currentY + 16f, paint)

            currentY += 30f

            val dateFormat = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault())
            paint.apply {
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            for (ledger in ledgers.take(25)) {
                if (currentY > pageHeight - 80f) break

                val isPurchase = ledger.type == "PURCHASE" || ledger.type == "CREDIT_ADJUSTMENT"
                paint.color = textColorDark

                paint.textAlign = Paint.Align.LEFT
                canvas.drawText(dateFormat.format(Date(ledger.timestamp)), boxLeft + 10f, currentY, paint)

                val desc = if (ledger.description.length > 28) ledger.description.take(26) + ".." else ledger.description
                canvas.drawText(desc, boxLeft + 90f, currentY, paint)

                val typeTag = if (isPurchase) "Purchase (+)" else "Payment (-)"
                paint.color = if (isPurchase) creditRed else debitGreen
                canvas.drawText("$typeTag • ${ledger.paymentMethod}", boxLeft + 280f, currentY, paint)

                paint.textAlign = Paint.Align.RIGHT
                paint.color = if (isPurchase) creditRed else debitGreen
                canvas.drawText("${if (isPurchase) "+" else "-"} ${ledger.amount.toInt()}", boxLeft + 420f, currentY, paint)

                paint.color = textColorDark
                canvas.drawText("${ledger.balanceAfter.toInt()}", boxRight - 10f, currentY, paint)

                currentY += 18f
                paint.color = borderGray
                paint.strokeWidth = 0.5f
                canvas.drawLine(boxLeft, currentY - 12f, boxRight, currentY - 12f, paint)
            }

            currentY += 20f
            paint.color = borderGray
            paint.strokeWidth = 1f
            canvas.drawLine(boxLeft, currentY, boxRight, currentY, paint)

            currentY += 25f
            paint.apply {
                color = primaryColor
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("*** Supplier Statement Generated by ${settings.storeName} ***", pageWidth / 2f, currentY, paint)

            pdfDocument.finishPage(page)

            val dir = File(context.cacheDir, "ledgers").apply { if (!exists()) mkdirs() }
            val pdfFile = File(dir, "Supplier_Ledger_${supplier.name.replace(" ", "_")}.pdf")
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun openOrSharePdf(context: Context, pdfFile: File, action: String = "open") {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, pdfFile)

            if (action == "share") {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Invoice ${pdfFile.nameWithoutExtension}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Invoice PDF"))
            } else {
                val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val chooser = Intent.createChooser(viewIntent, "Open Invoice PDF")
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Saved PDF to ${pdfFile.absolutePath}", Toast.LENGTH_LONG).show()
        }
    }

    fun printPdf(context: Context, pdfFile: File) {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager != null) {
                val printAdapter = PdfDocumentAdapter(pdfFile)
                val printAttributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
                printManager.print("Invoice_${pdfFile.nameWithoutExtension}", printAdapter, printAttributes)
            } else {
                openOrSharePdf(context, pdfFile, "open")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            openOrSharePdf(context, pdfFile, "open")
        }
    }

    /**
     * Directly share PDF attachment and/or text message to WhatsApp or WhatsApp Business
     */
    fun shareToWhatsApp(
        context: Context,
        pdfFile: File? = null,
        messageText: String? = null,
        phoneNumber: String? = null
    ) {
        try {
            val authority = "${context.packageName}.fileprovider"

            // Try standard WhatsApp package first
            val intent = Intent(Intent.ACTION_SEND).apply {
                setPackage("com.whatsapp")
                if (pdfFile != null) {
                    val uri: Uri = FileProvider.getUriForFile(context, authority, pdfFile)
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    type = "text/plain"
                }
                if (!messageText.isNullOrEmpty()) {
                    putExtra(Intent.EXTRA_TEXT, messageText)
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to WhatsApp Business package
            try {
                val authority = "${context.packageName}.fileprovider"
                val waBusinessIntent = Intent(Intent.ACTION_SEND).apply {
                    setPackage("com.whatsapp.w4b")
                    if (pdfFile != null) {
                        val uri: Uri = FileProvider.getUriForFile(context, authority, pdfFile)
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } else {
                        type = "text/plain"
                    }
                    if (!messageText.isNullOrEmpty()) {
                        putExtra(Intent.EXTRA_TEXT, messageText)
                    }
                }
                context.startActivity(waBusinessIntent)
            } catch (e2: Exception) {
                // If neither WhatsApp is directly installed or launch fails, open system share chooser
                if (pdfFile != null) {
                    openOrSharePdf(context, pdfFile, action = "share")
                } else if (!messageText.isNullOrEmpty()) {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, messageText)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share via WhatsApp / App"))
                } else {
                    Toast.makeText(context, "WhatsApp is not installed on this device", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadAndDrawLogo(canvas: Canvas, logoUri: String, x: Float, y: Float, width: Float, height: Float) {
        if (logoUri.isBlank()) return
        try {
            val file = File(logoUri)
            if (file.exists()) {
                val bmp = BitmapFactory.decodeFile(file.absolutePath)
                if (bmp != null) {
                    val scaled = Bitmap.createScaledBitmap(bmp, width.toInt(), height.toInt(), true)
                    canvas.drawBitmap(scaled, x, y, null)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Generates a multi-page Sales & Financial Accounting PDF report and exports it to device storage.
     */
    fun generateSalesReportPdf(
        context: Context,
        sales: List<Sale>,
        saleItems: List<SaleItem>,
        expenses: List<Expense>,
        settings: StoreSettings,
        title: String = "SALES & FINANCIAL ACCOUNTING REPORT"
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val margin = 35f

            val paint = Paint().apply { isAntiAlias = true }

            val primaryColor = Color.rgb(15, 23, 42)
            val brandAccent = Color.rgb(37, 99, 235)
            val textColorDark = Color.rgb(30, 41, 59)
            val textColorMuted = Color.rgb(100, 116, 139)
            val bgLightGray = Color.rgb(248, 250, 252)
            val borderGray = Color.rgb(226, 232, 240)
            val creditRed = Color.rgb(225, 29, 72)
            val debitGreen = Color.rgb(16, 185, 129)

            val dateFormat = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault())
            val dateOnlyFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

            val totalTransactions = sales.size
            val grossRevenue = sales.sumOf { it.totalAmount }
            val totalDiscounts = sales.sumOf { it.discount }
            val netRevenue = sales.sumOf { it.netAmount }
            val totalPaid = sales.sumOf { it.paidAmount }
            val totalDue = sales.sumOf { it.dueAmount }

            val saleIds = sales.map { it.id }.toSet()
            val filteredSaleItems = saleItems.filter { it.saleId in saleIds }
            val totalCogs = filteredSaleItems.sumOf { it.quantity * it.purchasePrice }
            val grossSalesProfit = filteredSaleItems.sumOf { it.totalPrice - (it.quantity * it.purchasePrice) } - totalDiscounts
            val totalExpenses = expenses.sumOf { it.amount }
            val netOperatingProfit = grossSalesProfit - totalExpenses

            val cashTotal = sales.filter { it.paymentType.contains("Cash", ignoreCase = true) }.sumOf { it.netAmount }
            val udhaarTotal = sales.filter { it.paymentType.contains("Udhaar", ignoreCase = true) || it.paymentType.contains("Credit", ignoreCase = true) || it.dueAmount > 0 }.sumOf { it.netAmount }

            var pageNum = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas: Canvas = page.canvas

            var currentY = 40f

            fun drawHeader() {
                paint.color = brandAccent
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 12f, paint)

                currentY = 30f
                paint.apply {
                    color = primaryColor
                    textSize = 20f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(settings.storeName, pageWidth / 2f, currentY, paint)

                currentY += 15f
                paint.apply {
                    color = textColorMuted
                    textSize = 9f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }
                canvas.drawText("Proprietor: ${settings.ownerName}  |  Phone: ${settings.phone}", pageWidth / 2f, currentY, paint)

                currentY += 12f
                canvas.drawText("Address: ${settings.address}", pageWidth / 2f, currentY, paint)

                currentY += 12f
                paint.color = borderGray
                paint.strokeWidth = 1f
                canvas.drawLine(margin, currentY, pageWidth - margin, currentY, paint)

                currentY += 20f
                paint.color = bgLightGray
                canvas.drawRoundRect(margin, currentY - 12f, pageWidth - margin, currentY + 24f, 6f, 6f, paint)

                paint.apply {
                    color = primaryColor
                    textSize = 12f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.LEFT
                }
                canvas.drawText(title, margin + 12f, currentY + 6f, paint)

                paint.apply {
                    color = textColorMuted
                    textSize = 9f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.RIGHT
                }
                canvas.drawText("Generated: ${dateOnlyFormat.format(Date())}", pageWidth - margin - 12f, currentY + 6f, paint)

                currentY += 35f
            }

            drawHeader()

            // Executive Financial KPI Summary Box
            paint.color = bgLightGray
            canvas.drawRoundRect(margin, currentY, pageWidth - margin, currentY + 115f, 8f, 8f, paint)

            paint.apply {
                color = primaryColor
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("EXECUTIVE FINANCIAL SUMMARY", margin + 12f, currentY + 18f, paint)

            paint.apply {
                color = textColorDark
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            // Left Column
            val col1X = margin + 12f
            canvas.drawText("Total Transactions: $totalTransactions Sales", col1X, currentY + 36f, paint)
            canvas.drawText("Gross Sales Value: ${settings.currencySymbol} ${grossRevenue.toInt()}", col1X, currentY + 52f, paint)
            canvas.drawText("Discounts Allowed: - ${settings.currencySymbol} ${totalDiscounts.toInt()}", col1X, currentY + 68f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Net Revenue: ${settings.currencySymbol} ${netRevenue.toInt()}", col1X, currentY + 84f, paint)

            // Middle Column
            val col2X = margin + 200f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Cost of Goods (COGS): ${settings.currencySymbol} ${totalCogs.toInt()}", col2X, currentY + 36f, paint)
            canvas.drawText("Gross Sales Profit: ${settings.currencySymbol} ${grossSalesProfit.toInt()}", col2X, currentY + 52f, paint)
            canvas.drawText("Operating Expenses: - ${settings.currencySymbol} ${totalExpenses.toInt()}", col2X, currentY + 68f, paint)
            paint.apply {
                color = if (netOperatingProfit >= 0) debitGreen else creditRed
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("Net Operating Profit: ${settings.currencySymbol} ${netOperatingProfit.toInt()}", col2X, currentY + 84f, paint)

            // Right Column
            val col3X = margin + 380f
            paint.apply {
                color = textColorDark
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            canvas.drawText("Cash Sales: ${settings.currencySymbol} ${cashTotal.toInt()}", col3X, currentY + 36f, paint)
            canvas.drawText("Udhaar Credit Sales: ${settings.currencySymbol} ${udhaarTotal.toInt()}", col3X, currentY + 52f, paint)
            canvas.drawText("Total Amount Paid: ${settings.currencySymbol} ${totalPaid.toInt()}", col3X, currentY + 68f, paint)
            paint.apply {
                color = creditRed
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("Outstanding Due: ${settings.currencySymbol} ${totalDue.toInt()}", col3X, currentY + 84f, paint)

            currentY += 130f

            fun drawTableHeader() {
                paint.color = primaryColor
                canvas.drawRoundRect(margin, currentY, pageWidth - margin, currentY + 22f, 4f, 4f, paint)

                paint.apply {
                    color = Color.WHITE
                    textSize = 9f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText("Date & Time", margin + 8f, currentY + 15f, paint)
                canvas.drawText("Invoice #", margin + 110f, currentY + 15f, paint)
                canvas.drawText("Customer Name", margin + 180f, currentY + 15f, paint)
                canvas.drawText("Pay Mode", margin + 310f, currentY + 15f, paint)

                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("Net Amt", margin + 410f, currentY + 15f, paint)
                canvas.drawText("Paid", margin + 470f, currentY + 15f, paint)
                canvas.drawText("Due", pageWidth - margin - 8f, currentY + 15f, paint)

                currentY += 26f
            }

            drawTableHeader()

            paint.apply {
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            for (sale in sales) {
                if (currentY > pageHeight - 60f) {
                    paint.apply {
                        color = textColorMuted
                        textSize = 8f
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText("Page $pageNum • Generated by ${settings.storeName}", pageWidth / 2f, pageHeight - 25f, paint)

                    pdfDocument.finishPage(page)

                    pageNum++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas

                    currentY = 40f
                    drawHeader()
                    drawTableHeader()
                    paint.apply {
                        textSize = 8.5f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    }
                }

                paint.color = textColorDark
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText(dateFormat.format(Date(sale.timestamp)), margin + 8f, currentY, paint)
                canvas.drawText(sale.invoiceNumber, margin + 110f, currentY, paint)

                val custName = if (sale.customerName.length > 20) sale.customerName.take(18) + ".." else sale.customerName
                canvas.drawText(custName, margin + 180f, currentY, paint)
                canvas.drawText(sale.paymentType, margin + 310f, currentY, paint)

                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("${settings.currencySymbol}${sale.netAmount.toInt()}", margin + 410f, currentY, paint)
                canvas.drawText("${settings.currencySymbol}${sale.paidAmount.toInt()}", margin + 470f, currentY, paint)

                paint.color = if (sale.dueAmount > 0) creditRed else textColorDark
                canvas.drawText("${settings.currencySymbol}${sale.dueAmount.toInt()}", pageWidth - margin - 8f, currentY, paint)

                currentY += 16f
                paint.color = borderGray
                paint.strokeWidth = 0.5f
                canvas.drawLine(margin, currentY - 10f, pageWidth - margin, currentY - 10f, paint)
            }

            paint.apply {
                color = textColorMuted
                textSize = 8f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Page $pageNum • Confidential External Accounting Document", pageWidth / 2f, pageHeight - 25f, paint)

            pdfDocument.finishPage(page)

            saveAndExportPdfFile(context, pdfDocument, "SalesReport_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Generates a multi-page Inventory & Stock Valuation PDF report and exports it to device storage.
     */
    fun generateInventoryReportPdf(
        context: Context,
        products: List<Product>,
        settings: StoreSettings
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val margin = 35f

            val paint = Paint().apply { isAntiAlias = true }

            val primaryColor = Color.rgb(15, 23, 42)
            val brandAccent = Color.rgb(37, 99, 235)
            val textColorDark = Color.rgb(30, 41, 59)
            val textColorMuted = Color.rgb(100, 116, 139)
            val bgLightGray = Color.rgb(248, 250, 252)
            val borderGray = Color.rgb(226, 232, 240)
            val creditRed = Color.rgb(225, 29, 72)
            val debitGreen = Color.rgb(16, 185, 129)

            val dateOnlyFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

            val totalProducts = products.size
            val totalUnits = products.sumOf { it.stockQuantity }.toInt()
            val totalCostValuation = products.sumOf { it.stockQuantity * it.purchasePrice }
            val totalRetailValuation = products.sumOf { it.stockQuantity * it.salePrice }
            val projectedProfit = totalRetailValuation - totalCostValuation
            val lowStockCount = products.count { it.stockQuantity in 1.0..it.minStockLevel }
            val outOfStockCount = products.count { it.stockQuantity <= 0 }

            var pageNum = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas: Canvas = page.canvas

            var currentY = 40f

            fun drawHeader() {
                paint.color = brandAccent
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 12f, paint)

                currentY = 30f
                paint.apply {
                    color = primaryColor
                    textSize = 20f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(settings.storeName, pageWidth / 2f, currentY, paint)

                currentY += 15f
                paint.apply {
                    color = textColorMuted
                    textSize = 9f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }
                canvas.drawText("Proprietor: ${settings.ownerName}  |  Phone: ${settings.phone}", pageWidth / 2f, currentY, paint)

                currentY += 12f
                canvas.drawText("Address: ${settings.address}", pageWidth / 2f, currentY, paint)

                currentY += 12f
                paint.color = borderGray
                paint.strokeWidth = 1f
                canvas.drawLine(margin, currentY, pageWidth - margin, currentY, paint)

                currentY += 20f
                paint.color = bgLightGray
                canvas.drawRoundRect(margin, currentY - 12f, pageWidth - margin, currentY + 24f, 6f, 6f, paint)

                paint.apply {
                    color = primaryColor
                    textSize = 12f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.LEFT
                }
                canvas.drawText("INVENTORY & STOCK VALUATION REPORT", margin + 12f, currentY + 6f, paint)

                paint.apply {
                    color = textColorMuted
                    textSize = 9f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.RIGHT
                }
                canvas.drawText("As of: ${dateOnlyFormat.format(Date())}", pageWidth - margin - 12f, currentY + 6f, paint)

                currentY += 35f
            }

            drawHeader()

            // Executive Inventory Valuation KPI Box
            paint.color = bgLightGray
            canvas.drawRoundRect(margin, currentY, pageWidth - margin, currentY + 100f, 8f, 8f, paint)

            paint.apply {
                color = primaryColor
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
            }
            canvas.drawText("EXECUTIVE INVENTORY VALUATION SUMMARY", margin + 12f, currentY + 18f, paint)

            paint.apply {
                color = textColorDark
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            val col1X = margin + 12f
            canvas.drawText("Total Catalog Items: $totalProducts Products", col1X, currentY + 36f, paint)
            canvas.drawText("Total Physical Units: $totalUnits Units", col1X, currentY + 54f, paint)
            canvas.drawText("Low Stock Items: $lowStockCount | Out of Stock: $outOfStockCount", col1X, currentY + 72f, paint)

            val col2X = margin + 260f
            canvas.drawText("Total Cost Value (COGS): ${settings.currencySymbol} ${totalCostValuation.toInt()}", col2X, currentY + 36f, paint)
            canvas.drawText("Total Retail Sales Value: ${settings.currencySymbol} ${totalRetailValuation.toInt()}", col2X, currentY + 54f, paint)
            paint.apply {
                color = debitGreen
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("Projected Inventory Margin: +${settings.currencySymbol} ${projectedProfit.toInt()}", col2X, currentY + 72f, paint)

            currentY += 115f

            fun drawTableHeader() {
                paint.color = primaryColor
                canvas.drawRoundRect(margin, currentY, pageWidth - margin, currentY + 22f, 4f, 4f, paint)

                paint.apply {
                    color = Color.WHITE
                    textSize = 9f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText("Product Name / Code", margin + 8f, currentY + 15f, paint)
                canvas.drawText("Category", margin + 190f, currentY + 15f, paint)

                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("Stock Qty", margin + 290f, currentY + 15f, paint)
                canvas.drawText("Cost Price", margin + 370f, currentY + 15f, paint)
                canvas.drawText("Sale Price", margin + 450f, currentY + 15f, paint)
                canvas.drawText("Total Cost Val", pageWidth - margin - 8f, currentY + 15f, paint)

                currentY += 26f
            }

            drawTableHeader()

            paint.apply {
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            for (p in products) {
                if (currentY > pageHeight - 60f) {
                    paint.apply {
                        color = textColorMuted
                        textSize = 8f
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText("Page $pageNum • Generated by ${settings.storeName}", pageWidth / 2f, pageHeight - 25f, paint)

                    pdfDocument.finishPage(page)

                    pageNum++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas

                    currentY = 40f
                    drawHeader()
                    drawTableHeader()
                    paint.apply {
                        textSize = 8.5f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    }
                }

                paint.color = textColorDark
                paint.textAlign = Paint.Align.LEFT
                val pName = if (p.name.length > 25) p.name.take(23) + ".." else p.name
                canvas.drawText(pName, margin + 8f, currentY, paint)

                val catStr = if (p.category.length > 15) p.category.take(13) + ".." else p.category
                canvas.drawText(catStr, margin + 190f, currentY, paint)

                paint.textAlign = Paint.Align.RIGHT
                val qtyColor = when {
                    p.stockQuantity <= 0 -> creditRed
                    p.stockQuantity <= p.minStockLevel -> Color.rgb(217, 119, 6)
                    else -> textColorDark
                }
                paint.color = qtyColor
                canvas.drawText("${p.stockQuantity.toInt()} ${p.unit}", margin + 290f, currentY, paint)

                paint.color = textColorDark
                canvas.drawText("${settings.currencySymbol}${p.purchasePrice.toInt()}", margin + 370f, currentY, paint)
                canvas.drawText("${settings.currencySymbol}${p.salePrice.toInt()}", margin + 450f, currentY, paint)

                val itemTotalCost = p.stockQuantity * p.purchasePrice
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("${settings.currencySymbol}${itemTotalCost.toInt()}", pageWidth - margin - 8f, currentY, paint)
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                currentY += 16f
                paint.color = borderGray
                paint.strokeWidth = 0.5f
                canvas.drawLine(margin, currentY - 10f, pageWidth - margin, currentY - 10f, paint)
            }

            paint.apply {
                color = textColorMuted
                textSize = 8f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Page $pageNum • Official Inventory Valuation Record", pageWidth / 2f, pageHeight - 25f, paint)

            pdfDocument.finishPage(page)

            saveAndExportPdfFile(context, pdfDocument, "InventoryReport_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveAndExportPdfFile(context: Context, pdfDocument: PdfDocument, filename: String): File? {
        return try {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "reports").apply { if (!exists()) mkdirs() }
            val pdfFile = File(dir, filename)
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            try {
                val publicDownloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (publicDownloadsDir != null && (publicDownloadsDir.exists() || publicDownloadsDir.mkdirs())) {
                    val publicFile = File(publicDownloadsDir, filename)
                    pdfFile.copyTo(publicFile, overwrite = true)
                    Toast.makeText(context, "Exported PDF saved to Downloads/$filename", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Exported PDF saved to ${pdfFile.name}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Exported PDF saved to ${pdfFile.name}", Toast.LENGTH_LONG).show()
            }

            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }
}

