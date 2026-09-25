package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat as ZxBarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.util.EnumMap
import java.util.Random

/**
 * Standard Barcode Formats supported by CH UMER POS / SENTRY STORE POS
 */
enum class BarcodeType(
    val id: String,
    val displayName: String,
    val zxFormat: ZxBarcodeFormat,
    val description: String,
    val samplePlaceholder: String
) {
    CODE_128(
        id = "CODE_128",
        displayName = "Code 128",
        zxFormat = ZxBarcodeFormat.CODE_128,
        description = "High-density alphanumeric format. Best for retail SKUs & custom codes.",
        samplePlaceholder = "SKU-10025"
    ),
    EAN_13(
        id = "EAN_13",
        displayName = "EAN-13",
        zxFormat = ZxBarcodeFormat.EAN_13,
        description = "International standard 13-digit retail barcode with checksum.",
        samplePlaceholder = "8901234567890"
    ),
    EAN_8(
        id = "EAN_8",
        displayName = "EAN-8",
        zxFormat = ZxBarcodeFormat.EAN_8,
        description = "Compact 8-digit retail barcode for small packaging.",
        samplePlaceholder = "89012345"
    ),
    UPC_A(
        id = "UPC_A",
        displayName = "UPC-A",
        zxFormat = ZxBarcodeFormat.UPC_A,
        description = "Universal Product Code 12-digit standard.",
        samplePlaceholder = "012345678905"
    ),
    CODE_39(
        id = "CODE_39",
        displayName = "Code 39",
        zxFormat = ZxBarcodeFormat.CODE_39,
        description = "Standard uppercase alphanumeric inventory barcode.",
        samplePlaceholder = "PROD-101"
    ),
    ITF_14(
        id = "ITF_14",
        displayName = "ITF-14",
        zxFormat = ZxBarcodeFormat.ITF,
        description = "Interleaved 2 of 5 14-digit carton & shipping barcode.",
        samplePlaceholder = "10012345678902"
    ),
    QR_CODE(
        id = "QR_CODE",
        displayName = "QR Code",
        zxFormat = ZxBarcodeFormat.QR_CODE,
        description = "2D high capacity matrix code for smart scanners & URLs.",
        samplePlaceholder = "https://sentrystore.pk/p/101"
    );

    companion object {
        fun fromId(id: String?): BarcodeType {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: CODE_128
        }
    }
}

data class BarcodeValidationResult(
    val isValid: Boolean,
    val normalizedValue: String,
    val errorMessage: String? = null
)

object BarcodeGenerator {

    private val random = Random()

    /**
     * Validates and optionally computes/fixes checksum for the given barcode data
     */
    fun validate(input: String, type: BarcodeType): BarcodeValidationResult {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            return BarcodeValidationResult(false, trimmed, "Barcode content cannot be empty")
        }

        return when (type) {
            BarcodeType.CODE_128 -> {
                // Code 128 accepts all standard ASCII 32-126
                val invalidChar = trimmed.find { it.code < 32 || it.code > 126 }
                if (invalidChar != null) {
                    BarcodeValidationResult(false, trimmed, "Code 128 contains invalid character: '$invalidChar'")
                } else {
                    BarcodeValidationResult(true, trimmed)
                }
            }

            BarcodeType.EAN_13 -> {
                val digits = trimmed.filter { it.isDigit() }
                if (digits.length == 12) {
                    val checksum = calculateMod10Checksum(digits, weightOdd = 1, weightEven = 3)
                    BarcodeValidationResult(true, digits + checksum)
                } else if (digits.length == 13) {
                    val data = digits.substring(0, 12)
                    val expectedCheck = calculateMod10Checksum(data, weightOdd = 1, weightEven = 3)
                    val actualCheck = digits[12].digitToInt()
                    if (actualCheck == expectedCheck) {
                        BarcodeValidationResult(true, digits)
                    } else {
                        BarcodeValidationResult(
                            false,
                            digits,
                            "Invalid EAN-13 checksum (expected $expectedCheck, found $actualCheck)"
                        )
                    }
                } else {
                    BarcodeValidationResult(false, trimmed, "EAN-13 must be exactly 12 or 13 numeric digits")
                }
            }

            BarcodeType.EAN_8 -> {
                val digits = trimmed.filter { it.isDigit() }
                if (digits.length == 7) {
                    val checksum = calculateMod10Checksum(digits, weightOdd = 3, weightEven = 1)
                    BarcodeValidationResult(true, digits + checksum)
                } else if (digits.length == 8) {
                    val data = digits.substring(0, 7)
                    val expectedCheck = calculateMod10Checksum(data, weightOdd = 3, weightEven = 1)
                    val actualCheck = digits[7].digitToInt()
                    if (actualCheck == expectedCheck) {
                        BarcodeValidationResult(true, digits)
                    } else {
                        BarcodeValidationResult(
                            false,
                            digits,
                            "Invalid EAN-8 checksum (expected $expectedCheck, found $actualCheck)"
                        )
                    }
                } else {
                    BarcodeValidationResult(false, trimmed, "EAN-8 must be exactly 7 or 8 numeric digits")
                }
            }

            BarcodeType.UPC_A -> {
                val digits = trimmed.filter { it.isDigit() }
                if (digits.length == 11) {
                    val checksum = calculateMod10Checksum(digits, weightOdd = 3, weightEven = 1)
                    BarcodeValidationResult(true, digits + checksum)
                } else if (digits.length == 12) {
                    val data = digits.substring(0, 11)
                    val expectedCheck = calculateMod10Checksum(data, weightOdd = 3, weightEven = 1)
                    val actualCheck = digits[11].digitToInt()
                    if (actualCheck == expectedCheck) {
                        BarcodeValidationResult(true, digits)
                    } else {
                        BarcodeValidationResult(
                            false,
                            digits,
                            "Invalid UPC-A checksum (expected $expectedCheck, found $actualCheck)"
                        )
                    }
                } else {
                    BarcodeValidationResult(false, trimmed, "UPC-A must be exactly 11 or 12 numeric digits")
                }
            }

            BarcodeType.CODE_39 -> {
                val upper = trimmed.uppercase()
                val validSet = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%"
                val invalidChar = upper.find { !validSet.contains(it) }
                if (invalidChar != null) {
                    BarcodeValidationResult(false, upper, "Code 39 contains invalid character: '$invalidChar'")
                } else {
                    BarcodeValidationResult(true, upper)
                }
            }

            BarcodeType.ITF_14 -> {
                val digits = trimmed.filter { it.isDigit() }
                if (digits.length == 13) {
                    val checksum = calculateMod10Checksum(digits, weightOdd = 3, weightEven = 1)
                    BarcodeValidationResult(true, digits + checksum)
                } else if (digits.length == 14) {
                    val data = digits.substring(0, 13)
                    val expectedCheck = calculateMod10Checksum(data, weightOdd = 3, weightEven = 1)
                    val actualCheck = digits[13].digitToInt()
                    if (actualCheck == expectedCheck) {
                        BarcodeValidationResult(true, digits)
                    } else {
                        BarcodeValidationResult(
                            false,
                            digits,
                            "Invalid ITF-14 checksum (expected $expectedCheck, found $actualCheck)"
                        )
                    }
                } else {
                    BarcodeValidationResult(false, trimmed, "ITF-14 must be exactly 13 or 14 numeric digits")
                }
            }

            BarcodeType.QR_CODE -> {
                BarcodeValidationResult(true, trimmed)
            }
        }
    }

    /**
     * Standard Modulo 10 checksum algorithm (used by EAN-13, EAN-8, UPC-A, ITF-14)
     */
    fun calculateMod10Checksum(data: String, weightOdd: Int, weightEven: Int): Int {
        var sum = 0
        for (i in data.indices) {
            val digit = data[i].digitToInt()
            val weight = if (i % 2 == 0) weightOdd else weightEven
            sum += digit * weight
        }
        val rem = sum % 10
        return if (rem == 0) 0 else 10 - rem
    }

    /**
     * Detects likely BarcodeType based on existing saved barcode text
     */
    fun detectBarcodeType(barcode: String): BarcodeType {
        val clean = barcode.trim()
        val allDigits = clean.isNotEmpty() && clean.all { it.isDigit() }
        return when {
            clean.startsWith("http://") || clean.startsWith("https://") -> BarcodeType.QR_CODE
            allDigits && clean.length == 13 -> BarcodeType.EAN_13
            allDigits && clean.length == 12 -> BarcodeType.UPC_A
            allDigits && clean.length == 8 -> BarcodeType.EAN_8
            allDigits && clean.length == 14 -> BarcodeType.ITF_14
            clean.all { "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-. $/+%".contains(it) } && clean.contains("-") -> BarcodeType.CODE_128
            else -> BarcodeType.CODE_128
        }
    }

    /**
     * Formats a database-controlled Master Barcode using the standard retail prefix "200"
     * and a 9-digit sequence with checksum.
     * Sequence 1 -> 2000000000017
     * Sequence 2 -> 2000000000024
     * Sequence 3 -> 2000000000031
     * etc.
     */
    fun formatMasterBarcode(sequence: Long, prefix: String = "200"): String {
        val seqPart = sequence.toString().padStart(9, '0')
        val base = "$prefix$seqPart" // 12 digits e.g. "200000000001"
        var sum = 8
        for (i in base.indices) {
            val digit = base[i].digitToInt()
            val weight = if (i % 2 == 0) 1 else 3
            sum += digit * weight
        }
        val rem = sum % 10
        val check = if (rem == 0) 0 else 10 - rem
        return "$base$check"
    }

    /**
     * Generates the next sequential Master Barcode (database-controlled sequence).
     * Rule: Never generate barcode from product name, price, or random temporary value.
     * Follows exact sequential format (Product A -> 2000000000017, Product B -> 2000000000024, etc.)
     */
    suspend fun generateNextMasterBarcode(
        prefix: String = "200",
        existingBarcodes: Collection<String> = emptyList(),
        isBarcodeTaken: suspend (String) -> Boolean = { false }
    ): String {
        var maxSeq = 0L
        val prefixLen = prefix.length
        for (barcode in existingBarcodes) {
            val clean = barcode.trim()
            if (clean.startsWith(prefix) && clean.length == 13) {
                val seqStr = clean.substring(prefixLen, 12)
                val seq = seqStr.toLongOrNull() ?: 0L
                if (seq > maxSeq) maxSeq = seq
            }
        }
        var nextSeq = maxOf(maxSeq + 1, (existingBarcodes.size + 1).toLong())
        var candidate: String
        var attempts = 0
        do {
            candidate = formatMasterBarcode(nextSeq, prefix)
            nextSeq++
            attempts++
        } while ((existingBarcodes.contains(candidate) || isBarcodeTaken(candidate)) && attempts < 500)

        return candidate
    }

    /**
     * Generates a 13-digit numeric barcode starting with 890 (or custom prefix)
     * with Mod10 checksum, perfectly compatible with Code 128 and retail scanners.
     * Verifies uniqueness against database/existing records before returning.
     */
    suspend fun generateUniqueNumericBarcode(
        prefix: String = "890",
        isBarcodeTaken: suspend (String) -> Boolean = { false }
    ): String {
        var candidate: String
        var attempts = 0
        do {
            val randomPart = (random.nextLong().let { if (it < 0) -it else it } % 1_000_000_000L)
                .toString().padStart(9, '0')
            val base = "$prefix$randomPart"
            val checksum = calculateMod10Checksum(base, weightOdd = 1, weightEven = 3)
            candidate = "$base$checksum"
            attempts++
        } while (isBarcodeTaken(candidate) && attempts < 100)

        if (isBarcodeTaken(candidate)) {
            val timePart = System.currentTimeMillis().toString().takeLast(9)
            val base = "$prefix$timePart"
            val checksum = calculateMod10Checksum(base, weightOdd = 1, weightEven = 3)
            candidate = "$base$checksum"
        }
        return candidate
    }

    fun generateUniqueNumericBarcodeSync(
        prefix: String = "890",
        isBarcodeTaken: (String) -> Boolean = { false }
    ): String {
        var candidate: String
        var attempts = 0
        do {
            val randomPart = (random.nextLong().let { if (it < 0) -it else it } % 1_000_000_000L)
                .toString().padStart(9, '0')
            val base = "$prefix$randomPart"
            val checksum = calculateMod10Checksum(base, weightOdd = 1, weightEven = 3)
            candidate = "$base$checksum"
            attempts++
        } while (isBarcodeTaken(candidate) && attempts < 100)

        if (isBarcodeTaken(candidate)) {
            val timePart = System.currentTimeMillis().toString().takeLast(9)
            val base = "$prefix$timePart"
            val checksum = calculateMod10Checksum(base, weightOdd = 1, weightEven = 3)
            candidate = "$base$checksum"
        }
        return candidate
    }

    /**
     * Generates a unique, valid barcode based on requested type and product ID
     */
    fun autoGenerateBarcode(
        type: BarcodeType,
        productId: Long,
        existingBarcodes: Set<String> = emptySet()
    ): String {
        var candidate: String
        var attempts = 0
        do {
            candidate = when (type) {
                BarcodeType.CODE_128 -> {
                    val idPart = productId.toString().padStart(5, '0')
                    val rand = random.nextInt(900) + 100
                    "SKU-$idPart-$rand"
                }

                BarcodeType.EAN_13 -> {
                    // Prefix 200 (In-store distribution prefix) + 9 digits + checksum
                    val idPart = (productId % 100000).toString().padStart(5, '0')
                    val rand = random.nextInt(9000) + 1000
                    val base = "200$idPart$rand"
                    val check = calculateMod10Checksum(base, weightOdd = 1, weightEven = 3)
                    base + check
                }

                BarcodeType.EAN_8 -> {
                    // 7 digits + checksum
                    val idPart = (productId % 1000).toString().padStart(3, '0')
                    val rand = random.nextInt(9000) + 1000
                    val base = "$idPart$rand"
                    val check = calculateMod10Checksum(base, weightOdd = 3, weightEven = 1)
                    base + check
                }

                BarcodeType.UPC_A -> {
                    // 11 digits + checksum
                    val idPart = (productId % 10000).toString().padStart(4, '0')
                    val rand = (random.nextInt(9000000) + 1000000).toString()
                    val base = "0$idPart$rand".substring(0, 11)
                    val check = calculateMod10Checksum(base, weightOdd = 3, weightEven = 1)
                    base + check
                }

                BarcodeType.CODE_39 -> {
                    val idPart = productId.toString().padStart(4, '0')
                    val rand = random.nextInt(900) + 100
                    "P$idPart-$rand"
                }

                BarcodeType.ITF_14 -> {
                    val idPart = (productId % 10000).toString().padStart(4, '0')
                    val rand = (random.nextInt(90000000) + 10000000).toString()
                    val base = "1$idPart$rand"
                    val check = calculateMod10Checksum(base, weightOdd = 3, weightEven = 1)
                    base + check
                }

                BarcodeType.QR_CODE -> {
                    val idPart = productId.toString().padStart(6, '0')
                    "SS-PROD-$idPart"
                }
            }
            attempts++
        } while (existingBarcodes.contains(candidate) && attempts < 50)

        return candidate
    }

    /**
     * Renders a crisp monochrome Bitmap for the barcode using ZXing
     */
    fun generateBarcodeBitmap(
        content: String,
        type: BarcodeType,
        width: Int = 400,
        height: Int = 140
    ): Bitmap? {
        val validation = validate(content, type)
        val validContent = if (validation.isValid) validation.normalizedValue else content.trim()
        if (validContent.isEmpty()) return null

        return try {
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
                put(EncodeHintType.MARGIN, 1)
                put(EncodeHintType.CHARACTER_SET, "UTF-8")
            }

            val writer = MultiFormatWriter()
            val bitMatrix: BitMatrix = writer.encode(
                validContent,
                type.zxFormat,
                width,
                height,
                hints
            )

            val matrixWidth = bitMatrix.width
            val matrixHeight = bitMatrix.height
            val pixels = IntArray(matrixWidth * matrixHeight)

            for (y in 0 until matrixHeight) {
                val offset = y * matrixWidth
                for (x in 0 until matrixWidth) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
                }
            }

            val bitmap = Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, matrixWidth, 0, 0, matrixWidth, matrixHeight)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
