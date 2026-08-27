package com.example.util

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.min

/**
 * Pure Kotlin QR Code Matrix Generator (Zero External Dependencies).
 * Generates ISO/IEC 18004 compliant QR code bitmaps for invoice verification,
 * thermal printing, and PDF embedding without requiring ZXing or external scanning libraries.
 */
object QrCodeRenderer {

    fun generateQrBitmap(
        content: String,
        width: Int = 512,
        height: Int = 512
    ): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val qr = QrEncoder.encodeText(content, QrErrorCorrectionLevel.MEDIUM)
            renderQrToBitmap(qr, width, height)
        } catch (e: Exception) {
            null
        }
    }

    fun generateQrImageBitmap(
        content: String,
        width: Int = 512,
        height: Int = 512
    ): ImageBitmap? {
        return generateQrBitmap(content, width, height)?.asImageBitmap()
    }

    private fun renderQrToBitmap(qr: QrCodeData, width: Int, height: Int): Bitmap {
        val quietZone = 4
        val rawSize = qr.size + (quietZone * 2)
        val scale = min(width / rawSize, height / rawSize).coerceAtLeast(1)
        val actualW = rawSize * scale
        val actualH = rawSize * scale

        val bitmap = Bitmap.createBitmap(actualW, actualH, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(actualW * actualH)

        for (y in 0 until actualH) {
            val qrY = (y / scale) - quietZone
            val rowOffset = y * actualW
            for (x in 0 until actualW) {
                val qrX = (x / scale) - quietZone
                val isDark = if (qrX in 0 until qr.size && qrY in 0 until qr.size) {
                    qr.getModule(qrX, qrY)
                } else {
                    false
                }
                pixels[rowOffset + x] = if (isDark) Color.BLACK else Color.WHITE
            }
        }
        bitmap.setPixels(pixels, 0, actualW, 0, 0, actualW, actualH)
        return bitmap
    }
}

enum class QrErrorCorrectionLevel(val ordinalBits: Int, val formatBits: Int) {
    LOW(1, 1),
    MEDIUM(0, 0),
    QUARTILE(3, 3),
    HIGH(2, 2)
}

data class QrCodeData(val size: Int, val modules: Array<BooleanArray>) {
    fun getModule(x: Int, y: Int): Boolean = modules[y][x]
}

object QrEncoder {
    fun encodeText(text: String, ecl: QrErrorCorrectionLevel): QrCodeData {
        val dataBytes = text.toByteArray(Charsets.UTF_8)
        val version = getMinVersion(dataBytes.size, ecl)
        val numDataCodewords = getNumDataCodewords(version, ecl)
        val totalCodewords = getTotalCodewords(version)

        val bitBuffer = mutableListOf<Boolean>()
        // Byte mode indicator: 0100
        appendBits(bitBuffer, 0x4, 4)
        // Character count indicator (8 bits for versions 1-9)
        appendBits(bitBuffer, dataBytes.size, if (version < 10) 8 else 16)
        // Data bytes
        for (b in dataBytes) {
            appendBits(bitBuffer, b.toInt() and 0xFF, 8)
        }
        // Terminator (up to 4 zeros)
        val bitLimit = numDataCodewords * 8
        val terminatorLen = min(4, bitLimit - bitBuffer.size)
        appendBits(bitBuffer, 0, terminatorLen)
        // Pad to byte boundary
        while (bitBuffer.size % 8 != 0) {
            bitBuffer.add(false)
        }
        // Pad bytes (0xEC, 0x11)
        val padByte = intArrayOf(0xEC, 0x11)
        var padIdx = 0
        while (bitBuffer.size < bitLimit) {
            appendBits(bitBuffer, padByte[padIdx % 2], 8)
            padIdx++
        }

        // Convert bitBuffer to data codewords
        val dataCodewords = IntArray(numDataCodewords)
        for (i in 0 until numDataCodewords) {
            var byteVal = 0
            for (j in 0 until 8) {
                if (bitBuffer[i * 8 + j]) {
                    byteVal = byteVal or (1 shl (7 - j))
                }
            }
            dataCodewords[i] = byteVal
        }

        // Generate error correction codewords via Reed-Solomon
        val ecCodewordsPerBlock = getEcCodewordsPerBlock(version, ecl)
        val numBlocks = getNumBlocks(version, ecl)
        val allCodewords = generateCodewordsWithEc(dataCodewords, version, ecl, ecCodewordsPerBlock, numBlocks, totalCodewords)

        // Build QR Matrix
        val size = version * 4 + 17
        val matrix = Array(size) { BooleanArray(size) }
        val isFunction = Array(size) { BooleanArray(size) }

        placeFinderPatterns(matrix, isFunction, size)
        placeTimingPatterns(matrix, isFunction, size)
        placeAlignmentPatterns(matrix, isFunction, version)
        placeFormatBits(matrix, isFunction, ecl, mask = 0, size)

        // Fill data bits with mask pattern 0: (x + y) % 2 == 0
        var codewordIdx = 0
        var bitIdx = 7
        var up = true
        var right = size - 1

        while (right > 0) {
            if (right == 6) right-- // Skip vertical timing column
            for (vert in 0 until size) {
                val y = if (up) size - 1 - vert else vert
                for (j in 0..1) {
                    val x = right - j
                    if (!isFunction[y][x]) {
                        var bit = false
                        if (codewordIdx < allCodewords.size) {
                            bit = ((allCodewords[codewordIdx] ushr bitIdx) and 1) != 0
                            bitIdx--
                            if (bitIdx < 0) {
                                bitIdx = 7
                                codewordIdx++
                            }
                        }
                        val mask = (x + y) % 2 == 0
                        matrix[y][x] = if (mask) !bit else bit
                    }
                }
            }
            up = !up
            right -= 2
        }

        return QrCodeData(size, matrix)
    }

    private fun appendBits(buffer: MutableList<Boolean>, value: Int, count: Int) {
        for (i in count - 1 downTo 0) {
            buffer.add(((value ushr i) and 1) != 0)
        }
    }

    private fun placeFinderPatterns(matrix: Array<BooleanArray>, isFunc: Array<BooleanArray>, size: Int) {
        val origins = listOf(0 to 0, size - 7 to 0, 0 to size - 7)
        for ((ox, oy) in origins) {
            for (y in 0 until 7) {
                for (x in 0 until 7) {
                    val isBorder = x == 0 || x == 6 || y == 0 || y == 6
                    val isCenter = x in 2..4 && y in 2..4
                    matrix[oy + y][ox + x] = isBorder || isCenter
                    isFunc[oy + y][ox + x] = true
                }
            }
            // Separator quiet ring around finders
            for (y in -1..7) {
                for (x in -1..7) {
                    val px = ox + x
                    val py = oy + y
                    if (px in 0 until size && py in 0 until size && !isFunc[py][px]) {
                        matrix[py][px] = false
                        isFunc[py][px] = true
                    }
                }
            }
        }
    }

    private fun placeTimingPatterns(matrix: Array<BooleanArray>, isFunc: Array<BooleanArray>, size: Int) {
        for (i in 8 until size - 8) {
            if (!isFunc[6][i]) {
                matrix[6][i] = (i % 2 == 0)
                isFunc[6][i] = true
            }
            if (!isFunc[i][6]) {
                matrix[i][6] = (i % 2 == 0)
                isFunc[i][6] = true
            }
        }
    }

    private fun placeAlignmentPatterns(matrix: Array<BooleanArray>, isFunc: Array<BooleanArray>, version: Int) {
        if (version < 2) return
        val pos = getAlignmentPatternPositions(version)
        for (y in pos) {
            for (x in pos) {
                if (isFunc[y][x]) continue
                for (dy in -2..2) {
                    for (dx in -2..2) {
                        val px = x + dx
                        val py = y + dy
                        matrix[py][px] = (dx == -2 || dx == 2 || dy == -2 || dy == 2 || (dx == 0 && dy == 0))
                        isFunc[py][px] = true
                    }
                }
            }
        }
    }

    private fun placeFormatBits(
        matrix: Array<BooleanArray>,
        isFunc: Array<BooleanArray>,
        ecl: QrErrorCorrectionLevel,
        mask: Int,
        size: Int
    ) {
        // Format info bits: 5 data bits (2 ECL + 3 mask), BCH (15,5) error code XORed with 0x5412
        var data = (ecl.formatBits shl 3) or mask
        var rem = data shl 10
        val poly = 0x537
        for (i in 4 downTo 0) {
            if ((rem and (1 shl (i + 10))) != 0) {
                rem = rem xor (poly shl i)
            }
        }
        val formatBits = ((data shl 10) or rem) xor 0x5412

        // Write around top-left finder and split on top-right / bottom-left
        for (i in 0 until 15) {
            val bit = ((formatBits ushr i) and 1) != 0

            // Top-left
            val (x1, y1) = when {
                i <= 5 -> (8 to i)
                i == 6 -> (8 to 7)
                i == 7 -> (8 to 8)
                i == 8 -> (7 to 8)
                else -> (14 - i to 8)
            }
            matrix[y1][x1] = bit
            isFunc[y1][x1] = true

            // Split across other corners
            val (x2, y2) = when {
                i < 8 -> (size - 1 - i to 8)
                else -> (8 to size - 15 + i)
            }
            matrix[y2][x2] = bit
            isFunc[y2][x2] = true
        }

        // Dark module
        matrix[size - 8][8] = true
        isFunc[size - 8][8] = true
    }

    private fun generateCodewordsWithEc(
        data: IntArray,
        version: Int,
        ecl: QrErrorCorrectionLevel,
        ecLen: Int,
        numBlocks: Int,
        totalCodewords: Int
    ): IntArray {
        val rsPoly = computeReedSolomonGenerator(ecLen)
        val shortBlockLen = data.size / numBlocks
        val numShortBlocks = numBlocks - (data.size % numBlocks)
        val blocks = Array(numBlocks) { IntArray(0) }
        val ecBlocks = Array(numBlocks) { IntArray(ecLen) }

        var offset = 0
        for (b in 0 until numBlocks) {
            val blkLen = if (b < numShortBlocks) shortBlockLen else shortBlockLen + 1
            val blkData = IntArray(blkLen)
            System.arraycopy(data, offset, blkData, 0, blkLen)
            offset += blkLen
            blocks[b] = blkData
            ecBlocks[b] = computeReedSolomonRemainder(blkData, rsPoly, ecLen)
        }

        // Interleave data and EC codewords
        val result = IntArray(totalCodewords)
        var idx = 0
        val maxDataLen = if (numShortBlocks == numBlocks) shortBlockLen else shortBlockLen + 1
        for (i in 0 until maxDataLen) {
            for (b in 0 until numBlocks) {
                if (i < blocks[b].size) {
                    result[idx++] = blocks[b][i]
                }
            }
        }
        for (i in 0 until ecLen) {
            for (b in 0 until numBlocks) {
                result[idx++] = ecBlocks[b][i]
            }
        }
        return result
    }

    private fun computeReedSolomonGenerator(degree: Int): IntArray {
        var poly = intArrayOf(1)
        for (i in 0 until degree) {
            val root = gfExp[i]
            val next = IntArray(poly.size + 1)
            for (j in poly.indices) {
                next[j] = next[j] xor gfMultiply(poly[j], root)
                next[j + 1] = next[j + 1] xor poly[j]
            }
            poly = next
        }
        return poly
    }

    private fun computeReedSolomonRemainder(data: IntArray, generator: IntArray, ecLen: Int): IntArray {
        val rem = IntArray(ecLen)
        for (b in data) {
            val factor = b xor rem[0]
            System.arraycopy(rem, 1, rem, 0, ecLen - 1)
            rem[ecLen - 1] = 0
            for (i in 0 until ecLen) {
                rem[i] = rem[i] xor gfMultiply(generator[generator.size - 2 - i], factor)
            }
        }
        return rem
    }

    // Galois Field GF(256) Math
    private val gfExp = IntArray(512)
    private val gfLog = IntArray(256)

    init {
        var x = 1
        for (i in 0 until 255) {
            gfExp[i] = x
            gfExp[i + 255] = x
            gfLog[x] = i
            x = (x shl 1)
            if ((x and 0x100) != 0) {
                x = x xor 0x11D
            }
        }
        gfLog[0] = 0
    }

    private fun gfMultiply(a: Int, b: Int): Int {
        if (a == 0 || b == 0) return 0
        return gfExp[(gfLog[a] + gfLog[b]) % 255]
    }

    private fun getMinVersion(byteCount: Int, ecl: QrErrorCorrectionLevel): Int {
        for (v in 1..10) {
            val cap = getNumDataCodewords(v, ecl) - 2 // overhead for mode + length
            if (byteCount <= cap) return v
        }
        return 10
    }

    private fun getNumDataCodewords(v: Int, ecl: QrErrorCorrectionLevel): Int {
        val total = getTotalCodewords(v)
        val ecTotal = getEcCodewordsPerBlock(v, ecl) * getNumBlocks(v, ecl)
        return total - ecTotal
    }

    private fun getTotalCodewords(v: Int): Int {
        return when (v) {
            1 -> 26
            2 -> 44
            3 -> 70
            4 -> 100
            5 -> 134
            6 -> 172
            7 -> 196
            8 -> 242
            9 -> 292
            10 -> 346
            else -> 346
        }
    }

    private fun getEcCodewordsPerBlock(v: Int, ecl: QrErrorCorrectionLevel): Int {
        return when (ecl) {
            QrErrorCorrectionLevel.LOW -> when (v) {
                1 -> 7; 2 -> 10; 3 -> 15; 4 -> 20; 5 -> 26; 6 -> 18; 7 -> 20; 8 -> 24; 9 -> 30; else -> 18
            }
            QrErrorCorrectionLevel.MEDIUM -> when (v) {
                1 -> 10; 2 -> 16; 3 -> 26; 4 -> 18; 5 -> 24; 6 -> 16; 7 -> 18; 8 -> 22; 9 -> 22; else -> 26
            }
            QrErrorCorrectionLevel.QUARTILE -> when (v) {
                1 -> 13; 2 -> 22; 3 -> 18; 4 -> 26; 5 -> 18; 6 -> 24; 7 -> 18; 8 -> 22; 9 -> 20; else -> 24
            }
            QrErrorCorrectionLevel.HIGH -> when (v) {
                1 -> 17; 2 -> 28; 3 -> 22; 4 -> 16; 5 -> 22; 6 -> 28; 7 -> 26; 8 -> 26; 9 -> 24; else -> 28
            }
        }
    }

    private fun getNumBlocks(v: Int, ecl: QrErrorCorrectionLevel): Int {
        return when (ecl) {
            QrErrorCorrectionLevel.LOW -> if (v in 6..8) 2 else 1
            QrErrorCorrectionLevel.MEDIUM -> when (v) {
                in 1..3 -> 1; 4 -> 2; 5 -> 2; 6 -> 4; 7 -> 4; 8 -> 4; 9 -> 5; else -> 5
            }
            QrErrorCorrectionLevel.QUARTILE -> when (v) {
                in 1..2 -> 1; in 3..4 -> 2; 5 -> 4; 6 -> 4; 7 -> 6; 8 -> 6; 9 -> 8; else -> 8
            }
            QrErrorCorrectionLevel.HIGH -> when (v) {
                1 -> 1; 2 -> 1; 3 -> 2; 4 -> 4; 5 -> 4; 6 -> 4; 7 -> 5; 8 -> 6; 9 -> 8; else -> 8
            }
        }
    }

    private fun getAlignmentPatternPositions(version: Int): IntArray {
        return when (version) {
            2 -> intArrayOf(6, 18)
            3 -> intArrayOf(6, 22)
            4 -> intArrayOf(6, 26)
            5 -> intArrayOf(6, 30)
            6 -> intArrayOf(6, 34)
            7 -> intArrayOf(6, 22, 38)
            8 -> intArrayOf(6, 24, 42)
            9 -> intArrayOf(6, 26, 46)
            10 -> intArrayOf(6, 28, 50)
            else -> intArrayOf(6, 28, 50)
        }
    }
}
