package com.example.util

import com.example.data.entity.Product

/**
 * Supported paper & sticker sheet sizes
 */
enum class LabelPaperType(
    val id: String,
    val displayName: String,
    val isSheet: Boolean,
    val widthMm: Float,
    val heightMm: Float,
    val columns: Int,
    val rows: Int,
    val marginHorizontalMm: Float = 4f,
    val marginVerticalMm: Float = 4f,
    val spacingHorizontalMm: Float = 2f,
    val spacingVerticalMm: Float = 2f
) {
    // Single Roll Label Printers (Direct Thermal / Thermal Transfer)
    THERMAL_50X25(
        id = "THERMAL_50X25",
        displayName = "Thermal Roll 50 × 25 mm (Standard)",
        isSheet = false,
        widthMm = 50f,
        heightMm = 25f,
        columns = 1,
        rows = 1,
        marginHorizontalMm = 1.5f,
        marginVerticalMm = 1.5f,
        spacingHorizontalMm = 0f,
        spacingVerticalMm = 0f
    ),
    THERMAL_50X30(
        id = "THERMAL_50X30",
        displayName = "Thermal Roll 50 × 30 mm",
        isSheet = false,
        widthMm = 50f,
        heightMm = 30f,
        columns = 1,
        rows = 1,
        marginHorizontalMm = 2f,
        marginVerticalMm = 2f,
        spacingHorizontalMm = 0f,
        spacingVerticalMm = 0f
    ),
    THERMAL_40X30(
        id = "THERMAL_40X30",
        displayName = "Thermal Roll 40 × 30 mm",
        isSheet = false,
        widthMm = 40f,
        heightMm = 30f,
        columns = 1,
        rows = 1,
        marginHorizontalMm = 1.5f,
        marginVerticalMm = 1.5f,
        spacingHorizontalMm = 0f,
        spacingVerticalMm = 0f
    ),
    THERMAL_38X25(
        id = "THERMAL_38X25",
        displayName = "Thermal Roll 38 × 25 mm",
        isSheet = false,
        widthMm = 38f,
        heightMm = 25f,
        columns = 1,
        rows = 1,
        marginHorizontalMm = 1.5f,
        marginVerticalMm = 1.5f,
        spacingHorizontalMm = 0f,
        spacingVerticalMm = 0f
    ),
    THERMAL_ROLL_58MM(
        id = "THERMAL_ROLL_58MM",
        displayName = "Thermal Receipt 58 mm (Continuous)",
        isSheet = false,
        widthMm = 58f,
        heightMm = 35f,
        columns = 1,
        rows = 1,
        marginHorizontalMm = 2f,
        marginVerticalMm = 2f,
        spacingHorizontalMm = 0f,
        spacingVerticalMm = 0f
    ),
    THERMAL_ROLL_80MM(
        id = "THERMAL_ROLL_80MM",
        displayName = "Thermal Receipt 80 mm (Continuous)",
        isSheet = false,
        widthMm = 80f,
        heightMm = 45f,
        columns = 1,
        rows = 1,
        marginHorizontalMm = 3f,
        marginVerticalMm = 3f,
        spacingHorizontalMm = 0f,
        spacingVerticalMm = 0f
    ),

    // Multi-label Sheets (A4: 210 × 297 mm)
    A4_24_LABELS(
        id = "A4_24_LABELS",
        displayName = "A4 Sheet — 24 Labels (3 × 8)",
        isSheet = true,
        widthMm = 210f,
        heightMm = 297f,
        columns = 3,
        rows = 8,
        marginHorizontalMm = 7.2f,
        marginVerticalMm = 13f,
        spacingHorizontalMm = 2.5f,
        spacingVerticalMm = 0f
    ),
    A4_30_LABELS(
        id = "A4_30_LABELS",
        displayName = "A4 Sheet — 30 Labels (3 × 10)",
        isSheet = true,
        widthMm = 210f,
        heightMm = 297f,
        columns = 3,
        rows = 10,
        marginHorizontalMm = 0f,
        marginVerticalMm = 0f,
        spacingHorizontalMm = 0f,
        spacingVerticalMm = 0f
    ),
    A4_40_LABELS(
        id = "A4_40_LABELS",
        displayName = "A4 Sheet — 40 Labels (4 × 10)",
        isSheet = true,
        widthMm = 210f,
        heightMm = 297f,
        columns = 4,
        rows = 10,
        marginHorizontalMm = 0f,
        marginVerticalMm = 0f,
        spacingHorizontalMm = 0f,
        spacingVerticalMm = 0f
    ),
    A4_65_LABELS(
        id = "A4_65_LABELS",
        displayName = "A4 Sheet — 65 Labels (5 × 13)",
        isSheet = true,
        widthMm = 210f,
        heightMm = 297f,
        columns = 5,
        rows = 13,
        marginHorizontalMm = 4f,
        marginVerticalMm = 10.7f,
        spacingHorizontalMm = 2f,
        spacingVerticalMm = 0f
    );

    val labelsPerPage: Int get() = columns * rows

    /**
     * Calculates the width and height of an individual label in mm
     */
    val singleLabelWidthMm: Float
        get() = if (!isSheet) widthMm else {
            val availableW = widthMm - (marginHorizontalMm * 2) - ((columns - 1) * spacingHorizontalMm)
            availableW / columns
        }

    val singleLabelHeightMm: Float
        get() = if (!isSheet) heightMm else {
            val availableH = heightMm - (marginVerticalMm * 2) - ((rows - 1) * spacingVerticalMm)
            availableH / rows
        }

    companion object {
        fun fromId(id: String?): LabelPaperType {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: THERMAL_50X25
        }
    }
}

enum class LabelFontSize(val label: String, val scale: Float) {
    COMPACT("Small", 0.85f),
    MEDIUM("Medium", 1.0f),
    LARGE("Large", 1.15f)
}

enum class LabelBarcodeHeight(val label: String, val heightPercent: Float) {
    COMPACT("Compact (25%)", 0.25f),
    STANDARD("Standard (35%)", 0.35f),
    TALL("Tall (45%)", 0.45f)
}

data class LabelPrintOptions(
    val paperType: LabelPaperType = LabelPaperType.THERMAL_50X25,
    val showStoreName: Boolean = true,
    val showStoreLogo: Boolean = false,
    val showProductName: Boolean = true,
    val showPrice: Boolean = true,
    val showBarcodeText: Boolean = true,
    val showSku: Boolean = true,
    val showUnit: Boolean = false,
    val showCutBorder: Boolean = true,
    val fontSize: LabelFontSize = LabelFontSize.MEDIUM,
    val barcodeHeight: LabelBarcodeHeight = LabelBarcodeHeight.STANDARD,
    val customStoreName: String = "",
    val customCurrencySymbol: String = ""
)

data class ProductLabelItem(
    val product: Product,
    var copies: Int = 1,
    var customBarcode: String = product.barcode,
    var barcodeType: BarcodeType = BarcodeGenerator.detectBarcodeType(product.barcode)
)
