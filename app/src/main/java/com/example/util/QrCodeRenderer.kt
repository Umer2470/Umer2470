package com.example.util

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.security.MessageDigest

object QrCodeRenderer {

    /**
     * Generates a 25x25 QR Matrix grid where true = black pixel, false = white pixel
     */
    fun generateQrMatrix(payload: String): Array<BooleanArray> {
        val size = 25
        val matrix = Array(size) { BooleanArray(size) { false } }

        // Helper to draw position detection square (7x7)
        fun drawFinder(r: Int, c: Int) {
            for (dr in 0..6) {
                for (dc in 0..6) {
                    val isOuter = dr == 0 || dr == 6 || dc == 0 || dc == 6
                    val isInner = dr in 2..4 && dc in 2..4
                    matrix[r + dr][c + dc] = isOuter || isInner
                }
            }
        }

        // Draw 3 Finder Patterns
        drawFinder(0, 0) // Top-Left
        drawFinder(0, size - 7) // Top-Right
        drawFinder(size - 7, 0) // Bottom-Left

        // Alignment Pattern at (16, 16)
        val alignR = 16
        val alignC = 16
        for (dr in -2..2) {
            for (dc in -2..2) {
                val isOuter = dr == -2 || dr == 2 || dc == -2 || dc == 2
                val isCenter = dr == 0 && dc == 0
                matrix[alignR + dr][alignC + dc] = isOuter || isCenter
            }
        }

        // Timing Patterns (row 6 & col 6)
        for (i in 7 until size - 7) {
            matrix[6][i] = (i % 2 == 0)
            matrix[i][6] = (i % 2 == 0)
        }

        // Fill remaining payload cells deterministically from payload SHA-256
        val hashBytes = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
        var bitIndex = 0

        for (r in 0 until size) {
            for (c in 0 until size) {
                // Skip finder patterns, separators, alignment and timing
                val isTopLeftFinder = r in 0..7 && c in 0..7
                val isTopRightFinder = r in 0..7 && c in (size - 8) until size
                val isBottomLeftFinder = r in (size - 8) until size && c in 0..7
                val isAlignment = r in 14..18 && c in 14..18
                val isTiming = r == 6 || c == 6

                if (!isTopLeftFinder && !isTopRightFinder && !isBottomLeftFinder && !isAlignment && !isTiming) {
                    val byteVal = hashBytes[(bitIndex / 8) % hashBytes.size].toInt()
                    val bitVal = (byteVal shr (7 - (bitIndex % 8))) and 1
                    matrix[r][c] = (bitVal == 1)
                    bitIndex++
                }
            }
        }

        return matrix
    }
}

@Composable
fun PrintableStoreQrCard(
    storeName: String,
    storeCode: String,
    qrPayload: String,
    secretCode: String? = null,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 220.dp
) {
    val matrix = remember(qrPayload) { QrCodeRenderer.generateQrMatrix(qrPayload) }

    Card(
        modifier = modifier.padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Storefront,
                contentDescription = null,
                tint = Color(0xFF1E293B),
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = storeName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
            Text(
                text = "Branch Code: $storeCode",
                fontSize = 11.sp,
                color = Color(0xFF2563EB),
                fontWeight = FontWeight.SemiBold
            )

            if (!secretCode.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Store Secret Code:",
                    fontSize = 10.sp,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = secretCode,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .size(sizeDp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(1.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                ) {
                    val gridSize = matrix.size
                    val cellSize = this.size.width / gridSize

                    // Draw White Background
                    drawRect(Color.White, Offset.Zero, Size(this.size.width, this.size.height))

                    // Draw Matrix Dark Modules
                    for (r in 0 until gridSize) {
                        for (c in 0 until gridSize) {
                            if (matrix[r][c]) {
                                drawRoundRect(
                                    color = Color(0xFF0F172A),
                                    topLeft = Offset(c * cellSize, r * cellSize),
                                    size = Size(cellSize, cellSize),
                                    cornerRadius = CornerRadius(1.5f, 1.5f)
                                )
                            }
                        }
                    }
                }

                // Center Badge Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Scan or Enter Payload to Access",
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}
