package com.example.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.entity.StoreSettings
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object BarcodeLabelPdfGenerator {

    private const val MM_TO_POINTS = 72f / 25.4f

    /**
     * Generates a printable PDF file with the selected products, copy counts, and layout options.
     */
    fun generateLabelsPdf(
        context: Context,
        labelItems: List<ProductLabelItem>,
        settings: StoreSettings,
        options: LabelPrintOptions
    ): File? {
        // Expand product items based on copies
        val expandedList = mutableListOf<ProductLabelItem>()
        for (item in labelItems) {
            val copies = max(1, item.copies)
            repeat(copies) {
                expandedList.add(item)
            }
        }

        if (expandedList.isEmpty()) {
            return null
        }

        val pdfDocument = PdfDocument()

        return try {
            val storeLogoBitmap = if (options.showStoreLogo) {
                BrandingImageHelper.getLogoBitmap(context, settings.logoUri)
            } else null

            val storeName = if (options.customStoreName.isNotBlank()) {
                options.customStoreName.trim()
            } else {
                settings.storeName.ifBlank { "SENTRY STORE" }
            }

            val currencySymbol = if (options.customCurrencySymbol.isNotBlank()) {
                options.customCurrencySymbol.trim()
            } else {
                settings.currencySymbol.ifBlank { "Rs." }
            }

            val paperType = options.paperType

            if (!paperType.isSheet) {
                // Roll Thermal Label Mode: 1 label per page
                val pageWidth = (paperType.widthMm * MM_TO_POINTS).toInt()
                val pageHeight = (paperType.heightMm * MM_TO_POINTS).toInt()

                var pageNumber = 1
                for (item in expandedList) {
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas

                    val labelRect = RectF(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat())
                    drawSingleLabel(
                        canvas = canvas,
                        bounds = labelRect,
                        item = item,
                        storeName = storeName,
                        currencySymbol = currencySymbol,
                        logoBitmap = storeLogoBitmap,
                        options = options,
                        isRoll = true
                    )

                    pdfDocument.finishPage(page)
                    pageNumber++
                }
            } else {
                // A4 Sheet Mode: Multi-label grid
                val pageWidth = (paperType.widthMm * MM_TO_POINTS).toInt() // 595 pt for A4
                val pageHeight = (paperType.heightMm * MM_TO_POINTS).toInt() // 842 pt for A4
                val labelsPerPage = paperType.labelsPerPage

                val marginH = paperType.marginHorizontalMm * MM_TO_POINTS
                val marginV = paperType.marginVerticalMm * MM_TO_POINTS
                val spacingH = paperType.spacingHorizontalMm * MM_TO_POINTS
                val spacingV = paperType.spacingVerticalMm * MM_TO_POINTS

                val cellWidth = paperType.singleLabelWidthMm * MM_TO_POINTS
                val cellHeight = paperType.singleLabelHeightMm * MM_TO_POINTS

                val totalLabels = expandedList.size
                var labelIndex = 0
                var pageNumber = 1

                while (labelIndex < totalLabels) {
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas

                    // Draw labels on current sheet page
                    for (row in 0 until paperType.rows) {
                        for (col in 0 until paperType.columns) {
                            if (labelIndex >= totalLabels) break

                            val item = expandedList[labelIndex]
                            val left = marginH + col * (cellWidth + spacingH)
                            val top = marginV + row * (cellHeight + spacingV)
                            val right = left + cellWidth
                            val bottom = top + cellHeight

                            val labelRect = RectF(left, top, right, bottom)
                            drawSingleLabel(
                                canvas = canvas,
                                bounds = labelRect,
                                item = item,
                                storeName = storeName,
                                currencySymbol = currencySymbol,
                                logoBitmap = storeLogoBitmap,
                                options = options,
                                isRoll = false
                            )

                            labelIndex++
                        }
                        if (labelIndex >= totalLabels) break
                    }

                    pdfDocument.finishPage(page)
                    pageNumber++
                }
            }

            // Save PDF file
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val filename = "BarcodeLabels_${paperType.id}_$timeStamp.pdf"
            val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.cacheDir
            if (!outputDir.exists()) outputDir.mkdirs()

            val pdfFile = File(outputDir, filename)
            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            pdfDocument.close()
        }
    }

    /**
     * Renders a single professional barcode label within the specified bounding rectangle.
     * NON-NEGOTIABLE: NEVER DRAWS PRODUCT IMAGES.
     */
    fun drawSingleLabel(
        canvas: Canvas,
        bounds: RectF,
        item: ProductLabelItem,
        storeName: String,
        currencySymbol: String,
        logoBitmap: Bitmap?,
        options: LabelPrintOptions,
        isRoll: Boolean
    ) {
        val width = bounds.width()
        val height = bounds.height()

        // 1. Cut border (for A4 sheets or visual guide)
        if (options.showCutBorder) {
            val borderPaint = Paint().apply {
                color = if (isRoll) Color.LTGRAY else Color.argb(80, 180, 180, 180)
                style = Paint.Style.STROKE
                strokeWidth = 0.5f
                isAntiAlias = true
            }
            canvas.drawRect(bounds, borderPaint)
        }

        // Label interior padding (2mm in points)
        val padX = min(width * 0.04f, 6f)
        val padY = min(height * 0.04f, 5f)
        val contentLeft = bounds.left + padX
        val contentRight = bounds.right - padX
        val contentTop = bounds.top + padY
        val contentBottom = bounds.bottom - padY
        val availableH = contentBottom - contentTop
        val contentWidth = contentRight - contentLeft
        val centerX = bounds.centerX()

        var currentY = contentTop

        val fontScale = options.fontSize.scale
        val baseTextSize = min(height * 0.12f, 10f) * fontScale

        val textPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        // 2. STORE NAME / STORE LOGO (Optional Logo, NEVER product image)
        if (options.showStoreName || (options.showStoreLogo && logoBitmap != null)) {
            val headerMaxH = min(availableH * 0.18f, 16f)

            if (options.showStoreLogo && logoBitmap != null) {
                val logoMaxDim = headerMaxH
                val logoRatio = logoBitmap.width.toFloat() / logoBitmap.height.toFloat()
                val logoW = min(logoMaxDim * logoRatio, contentWidth * 0.35f)
                val logoH = logoW / logoRatio
                val logoLeft = centerX - (logoW / 2f)
                val logoDst = RectF(logoLeft, currentY, logoLeft + logoW, currentY + logoH)

                val bmpPaint = Paint().apply { isFilterBitmap = true }
                canvas.drawBitmap(logoBitmap, null, logoDst, bmpPaint)
                currentY += logoH + 2f
            }

            if (options.showStoreName) {
                textPaint.apply {
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textSize = max(baseTextSize * 0.95f, 6.5f)
                }
                val storeNameText = ellipsize(storeName.uppercase(Locale.US), textPaint, contentWidth)
                currentY += textPaint.textSize
                canvas.drawText(storeNameText, centerX, currentY, textPaint)
                currentY += 2f
            }
        }

        // 3. PRODUCT NAME
        if (options.showProductName) {
            textPaint.apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = max(baseTextSize * 1.05f, 7.5f)
            }
            val productName = ellipsize(item.product.name.trim(), textPaint, contentWidth)
            currentY += textPaint.textSize + 1f
            canvas.drawText(productName, centerX, currentY, textPaint)
            currentY += 2f
        }

        // 4. PRICE
        if (options.showPrice) {
            textPaint.apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textSize = max(baseTextSize * 1.15f, 8.5f)
            }
            val formattedPrice = formatPrice(currencySymbol, item.product.salePrice)
            currentY += textPaint.textSize + 1f
            canvas.drawText(formattedPrice, centerX, currentY, textPaint)
            currentY += 2f
        }

        // Calculate bottom element requirements (Barcode text & SKU/Unit)
        var bottomRequiredH = 0f
        val barcodeTextSize = max(baseTextSize * 0.85f, 6f)
        if (options.showBarcodeText) {
            bottomRequiredH += barcodeTextSize + 2f
        }
        val subTextSize = max(baseTextSize * 0.75f, 5.5f)
        if (options.showSku || options.showUnit) {
            bottomRequiredH += subTextSize + 2f
        }

        val barcodeMaxBottom = contentBottom - bottomRequiredH
        val barcodeAvailableH = max(barcodeMaxBottom - currentY - 2f, 15f)

        // Height based on user option
        val requestedBarcodeH = availableH * options.barcodeHeight.heightPercent
        val actualBarcodeH = min(requestedBarcodeH, barcodeAvailableH)

        // 5. BARCODE BITMAP
        val rawBarcode = item.customBarcode.ifBlank { item.product.barcode }
        val effectiveBarcode = rawBarcode.ifBlank {
            BarcodeGenerator.autoGenerateBarcode(item.barcodeType, item.product.id)
        }

        val barcodeWidthPx = 400
        val barcodeHeightPx = (barcodeWidthPx * (actualBarcodeH / contentWidth).coerceIn(0.2f, 0.6f)).toInt()
        val barcodeBmp = BarcodeGenerator.generateBarcodeBitmap(
            content = effectiveBarcode,
            type = item.barcodeType,
            width = barcodeWidthPx,
            height = max(barcodeHeightPx, 80)
        )

        val barcodeTop = currentY + 1f
        val barcodeBottom = barcodeTop + actualBarcodeH

        if (barcodeBmp != null) {
            val barcodeRenderW = if (item.barcodeType == BarcodeType.QR_CODE) {
                // QR is square
                min(actualBarcodeH, contentWidth * 0.7f)
            } else {
                contentWidth * 0.94f
            }
            val barcodeLeft = centerX - (barcodeRenderW / 2f)
            val barcodeDst = RectF(barcodeLeft, barcodeTop, barcodeLeft + barcodeRenderW, barcodeBottom)

            val bmpPaint = Paint().apply { isFilterBitmap = false }
            canvas.drawBitmap(barcodeBmp, null, barcodeDst, bmpPaint)
        } else {
            // Fallback outline if barcode rendering failed
            val fallbackPaint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawRect(centerX - contentWidth * 0.4f, barcodeTop, centerX + contentWidth * 0.4f, barcodeBottom, fallbackPaint)
        }

        var postBarcodeY = barcodeBottom + 2f

        // 6. READABLE BARCODE NUMBER
        if (options.showBarcodeText) {
            textPaint.apply {
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
                textSize = barcodeTextSize
            }
            postBarcodeY += textPaint.textSize
            canvas.drawText(effectiveBarcode, centerX, postBarcodeY, textPaint)
            postBarcodeY += 1.5f
        }

        // 7. OPTIONAL SKU / UNIT
        if (options.showSku || options.showUnit) {
            val detailsList = mutableListOf<String>()
            if (options.showSku) {
                val skuText = item.product.description.ifBlank {
                    item.product.batchNumber.ifBlank { effectiveBarcode }
                }
                detailsList.add("SKU: $skuText")
            }
            if (options.showUnit && item.product.unit.isNotBlank()) {
                detailsList.add("Unit: ${item.product.unit}")
            }

            if (detailsList.isNotEmpty()) {
                textPaint.apply {
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    textSize = subTextSize
                }
                val detailLine = ellipsize(detailsList.joinToString(" • "), textPaint, contentWidth)
                postBarcodeY += textPaint.textSize
                canvas.drawText(detailLine, centerX, postBarcodeY, textPaint)
            }
        }
    }

    private fun formatPrice(currencySymbol: String, price: Double): String {
        return if (price % 1.0 == 0.0) {
            "$currencySymbol ${price.toLong()}"
        } else {
            "$currencySymbol ${String.format(Locale.US, "%.2f", price)}"
        }
    }

    private fun ellipsize(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var truncated = text
        while (truncated.isNotEmpty() && paint.measureText("$truncated…") > maxWidth) {
            truncated = truncated.dropLast(1)
        }
        return if (truncated.isEmpty()) text else "$truncated…"
    }

    /**
     * Sends the generated PDF to Android Print Framework for direct printing on thermal or laser/inkjet printers
     */
    fun printPdf(context: Context, pdfFile: File, jobName: String = "SentryStore_BarcodeLabels") {
        try {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager == null) {
                Toast.makeText(context, "Print service is unavailable on this device", Toast.LENGTH_SHORT).show()
                return
            }

            val printAdapter = PdfDocumentAdapter(pdfFile)
            val printAttributes = PrintAttributes.Builder()
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()

            printManager.print(jobName, printAdapter, printAttributes)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Printing error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Shares the generated PDF file via standard Android share sheet
     */
    fun sharePdf(context: Context, pdfFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Barcode Labels - ${pdfFile.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Barcode Labels PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "No app available to share PDF files", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Sharing failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Generates a printable PDF for Promotional Material & Shelf Talkers featuring the Master Barcode
     */
    fun generatePromoMaterialPdf(
        context: Context,
        items: List<ProductLabelItem>,
        settings: StoreSettings,
        options: PromoPrintOptions
    ): File? {
        val expandedList = mutableListOf<ProductLabelItem>()
        for (item in items) {
            val copies = max(1, item.copies)
            repeat(copies) { expandedList.add(item) }
        }
        if (expandedList.isEmpty()) return null

        val pdfDocument = PdfDocument()

        return try {
            val storeLogoBitmap = if (options.showStoreLogo) {
                BrandingImageHelper.getLogoBitmap(context, settings.logoUri)
            } else null

            val storeName = if (options.customStoreName.isNotBlank()) {
                options.customStoreName.trim()
            } else {
                settings.storeName.ifBlank { "SENTRY STORE" }
            }

            val currencySymbol = if (options.customCurrencySymbol.isNotBlank()) {
                options.customCurrencySymbol.trim()
            } else {
                settings.currencySymbol.ifBlank { "Rs." }
            }

            val materialType = options.materialType

            if (!materialType.isSheet) {
                val pageWidth = (materialType.widthMm * MM_TO_POINTS).toInt()
                val pageHeight = (materialType.heightMm * MM_TO_POINTS).toInt()

                var pageNumber = 1
                for (item in expandedList) {
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas

                    val rect = RectF(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat())
                    drawSinglePromoMaterial(
                        canvas = canvas,
                        bounds = rect,
                        item = item,
                        storeName = storeName,
                        currencySymbol = currencySymbol,
                        logoBitmap = storeLogoBitmap,
                        options = options
                    )

                    pdfDocument.finishPage(page)
                    pageNumber++
                }
            } else {
                // A4 4-Up Promo Sheet
                val pageWidth = (materialType.widthMm * MM_TO_POINTS).toInt() // 595 pt
                val pageHeight = (materialType.heightMm * MM_TO_POINTS).toInt() // 842 pt
                val margin = 8f * MM_TO_POINTS
                val cellW = (pageWidth - margin * 2) / 2f
                val cellH = (pageHeight - margin * 2) / 2f

                var itemIdx = 0
                var pageNumber = 1
                while (itemIdx < expandedList.size) {
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas

                    for (row in 0 until 2) {
                        for (col in 0 until 2) {
                            if (itemIdx >= expandedList.size) break
                            val left = margin + col * cellW
                            val top = margin + row * cellH
                            val rect = RectF(left, top, left + cellW, top + cellH)
                            drawSinglePromoMaterial(
                                canvas = canvas,
                                bounds = rect,
                                item = expandedList[itemIdx],
                                storeName = storeName,
                                currencySymbol = currencySymbol,
                                logoBitmap = storeLogoBitmap,
                                options = options
                            )
                            itemIdx++
                        }
                    }
                    pdfDocument.finishPage(page)
                    pageNumber++
                }
            }

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val filename = "PromoMaterial_${materialType.id}_$timeStamp.pdf"
            val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.cacheDir
            if (!outputDir.exists()) outputDir.mkdirs()

            val pdfFile = File(outputDir, filename)
            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            pdfDocument.close()
        }
    }

    /**
     * Renders a high-impact promotional sign or shelf-talker with Master Barcode
     */
    fun drawSinglePromoMaterial(
        canvas: Canvas,
        bounds: RectF,
        item: ProductLabelItem,
        storeName: String,
        currencySymbol: String,
        logoBitmap: Bitmap?,
        options: PromoPrintOptions
    ) {
        val width = bounds.width()
        val height = bounds.height()
        val centerX = bounds.centerX()

        val themeColor = try {
            Color.parseColor(options.theme.primaryColorHex)
        } catch (_: Exception) {
            Color.RED
        }

        // Draw Outer Border / Background
        if (options.showCutBorder) {
            val borderPaint = Paint().apply {
                color = Color.argb(120, 200, 200, 200)
                style = Paint.Style.STROKE
                strokeWidth = 1f
                isAntiAlias = true
            }
            canvas.drawRect(bounds, borderPaint)
        }

        // Top Banner for Promotional Badge
        val bannerHeight = min(height * 0.16f, 32f)
        val bannerPaint = Paint().apply {
            color = themeColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRect(bounds.left, bounds.top, bounds.right, bounds.top + bannerHeight, bannerPaint)

        // Banner Text (e.g. "SPECIAL OFFER", "HOT DEAL 🔥")
        val badgeText = options.customBadgeText.ifBlank { options.theme.defaultBadge }
        val badgePaint = Paint().apply {
            color = Color.WHITE
            textSize = max(bannerHeight * 0.52f, 8f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val badgeY = bounds.top + (bannerHeight / 2f) - ((badgePaint.descent() + badgePaint.ascent()) / 2f)
        canvas.drawText(badgeText, centerX, badgeY, badgePaint)

        var currentY = bounds.top + bannerHeight + (height * 0.03f)

        // Store Name
        if (options.showStoreName) {
            val storePaint = Paint().apply {
                color = Color.DKGRAY
                textSize = max(min(height * 0.05f, 11f), 7f)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val storeText = ellipsize(storeName.uppercase(Locale.US), storePaint, width * 0.9f)
            currentY += storePaint.textSize
            canvas.drawText(storeText, centerX, currentY, storePaint)
            currentY += 3f
        }

        // Product Name (Bold & Large)
        val namePaint = Paint().apply {
            color = Color.BLACK
            textSize = max(min(height * 0.09f, 18f), 9.5f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val prodName = ellipsize(item.product.name.trim(), namePaint, width * 0.92f)
        currentY += namePaint.textSize + 2f
        canvas.drawText(prodName, centerX, currentY, namePaint)
        currentY += 3f

        // Category / Unit subtitle
        if (options.showCategory || options.showUnit) {
            val subPaint = Paint().apply {
                color = Color.GRAY
                textSize = max(min(height * 0.045f, 9.5f), 6.5f)
                typeface = Typeface.DEFAULT
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val categoryStr = if (options.showCategory) item.product.category else ""
            val unitStr = if (options.showUnit) "Unit: ${item.product.unit}" else ""
            val sub = listOf(categoryStr, unitStr).filter { it.isNotBlank() }.joinToString(" • ")
            if (sub.isNotBlank()) {
                currentY += subPaint.textSize
                canvas.drawText(sub, centerX, currentY, subPaint)
                currentY += 4f
            }
        }

        // PRICE SECTION: Sale Price + Optional Regular Strikethrough
        val salePrice = item.product.salePrice
        val formattedSalePrice = formatPrice(currencySymbol, salePrice)

        if (options.showRegularPrice) {
            val discountRate = max(options.discountPercent, 5)
            val originalPrice = salePrice * (1.0 + discountRate / 100.0)
            val formattedOriginal = formatPrice(currencySymbol, originalPrice)

            val strikePaint = Paint().apply {
                color = Color.RED
                textSize = max(min(height * 0.055f, 11f), 7.5f)
                typeface = Typeface.DEFAULT
                isAntiAlias = true
            }
            val originalText = "Was $formattedOriginal"
            val origWidth = strikePaint.measureText(originalText)
            val origX = centerX - (origWidth / 2f)

            currentY += strikePaint.textSize + 2f
            canvas.drawText(originalText, origX, currentY, strikePaint)
            // Strike-through line
            val lineY = currentY - (strikePaint.textSize * 0.35f)
            canvas.drawLine(origX, lineY, origX + origWidth, lineY, strikePaint)
            currentY += 3f
        }

        // Prominent Sale Price
        val pricePaint = Paint().apply {
            color = themeColor
            textSize = max(min(height * 0.13f, 26f), 12f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        currentY += pricePaint.textSize + 2f
        canvas.drawText(formattedSalePrice, centerX, currentY, pricePaint)
        currentY += 5f

        // MASTER BARCODE SECTION (Guaranteed permanent barcode, rendered crisp)
        val masterBarcode = item.customBarcode.ifBlank { item.product.barcode }.trim()
        if (options.showMasterBarcode && masterBarcode.isNotBlank()) {
            val remainingH = bounds.bottom - currentY - (if (options.showBarcodeNumber) 16f else 6f) - 6f
            val barcodeH = max(min(remainingH, 36f), 16f)
            val barcodeW = min(width * 0.78f, 220f)

            val bmp = BarcodeGenerator.generateBarcodeBitmap(
                content = masterBarcode,
                type = BarcodeGenerator.detectBarcodeType(masterBarcode),
                width = 400,
                height = 100
            )

            if (bmp != null) {
                val bLeft = centerX - (barcodeW / 2f)
                val bDst = RectF(bLeft, currentY, bLeft + barcodeW, currentY + barcodeH)
                val bPaint = Paint().apply { isFilterBitmap = false }
                canvas.drawBitmap(bmp, null, bDst, bPaint)
                currentY += barcodeH + 2f
            }

            // Human Readable Master Barcode Number
            if (options.showBarcodeNumber) {
                val codePaint = Paint().apply {
                    color = Color.BLACK
                    textSize = max(min(height * 0.045f, 9f), 6.5f)
                    typeface = Typeface.MONOSPACE
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                currentY += codePaint.textSize + 1f
                canvas.drawText(masterBarcode, centerX, currentY, codePaint)
            }
        }

        // Footer Text
        if (options.footerText.isNotBlank()) {
            val footerPaint = Paint().apply {
                color = Color.GRAY
                textSize = max(min(height * 0.035f, 7.5f), 5.5f)
                typeface = Typeface.DEFAULT
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val footerY = bounds.bottom - 4f
            canvas.drawText(options.footerText, centerX, footerY, footerPaint)
        }
    }
}
