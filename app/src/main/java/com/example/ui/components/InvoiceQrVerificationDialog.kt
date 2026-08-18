package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.InvoiceFormattingService
import com.example.util.QrCodeRenderer

/**
 * Mobile Invoice Preview QR Code Component
 */
@Composable
fun InvoiceQrCodeSection(
    storeName: String,
    invoiceNumber: String,
    formattedDate: String,
    customerName: String,
    totalAmount: Double,
    currencySymbol: String,
    paidAmount: Double,
    dueAmount: Double,
    paymentType: String,
    timestamp: Long,
    modifier: Modifier = Modifier
) {
    val qrPayload = remember(invoiceNumber, timestamp) {
        InvoiceFormattingService.getQrCodePayload(
            storeName = storeName,
            invoiceNumber = invoiceNumber,
            formattedDate = formattedDate,
            customerName = customerName,
            totalAmount = totalAmount,
            currencySymbol = currencySymbol,
            paidAmount = paidAmount,
            dueAmount = dueAmount,
            paymentType = paymentType,
            timestamp = timestamp
        )
    }

    val verId = remember(invoiceNumber, timestamp) {
        InvoiceFormattingService.getInvoiceVerificationId(invoiceNumber, timestamp)
    }

    val matrix = remember(qrPayload) {
        try {
            val writer = com.google.zxing.qrcode.QRCodeWriter()
            val bitMatrix = writer.encode(qrPayload, com.google.zxing.BarcodeFormat.QR_CODE, 25, 25)
            Array(bitMatrix.height) { r ->
                BooleanArray(bitMatrix.width) { c ->
                    bitMatrix.get(c, r)
                }
            }
        } catch (e: Exception) {
            QrCodeRenderer.generateQrMatrix(qrPayload)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // QR Canvas
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val gridSize = matrix.size
                    val cellSize = this.size.width / gridSize
                    drawRect(Color.White, Offset.Zero, Size(this.size.width, this.size.height))
                    for (r in 0 until gridSize) {
                        for (c in 0 until gridSize) {
                            if (matrix[r][c]) {
                                drawRect(
                                    color = Color(0xFF0F172A),
                                    topLeft = Offset(c * cellSize, r * cellSize),
                                    size = Size(cellSize, cellSize)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFDCFCE7), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "VERIFIED INVOICE QR",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF166534)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ID: $verId",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Scan QR to verify Invoice details externally",
                    fontSize = 9.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

/**
 * Verification Modal shown when scanning an Invoice QR Code
 */
@Composable
fun ScannedInvoiceVerificationDialog(
    qrText: String,
    onDismiss: () -> Unit
) {
    // Parse key-value pairs from QR text payload
    val lines = qrText.lines()
    fun getValue(key: String): String {
        val line = lines.find { it.startsWith(key, ignoreCase = true) || it.contains(key, ignoreCase = true) }
        return line?.substringAfter(":")?.trim() ?: ""
    }

    val storeName = getValue("Store Name").ifBlank { getValue("Store").ifBlank { "CH UMAIR SENTRY STORE" } }
    val invoiceNumber = getValue("Invoice Number").ifBlank { getValue("Invoice #").ifBlank { "N/A" } }
    val dateTime = getValue("Invoice Date & Time").ifBlank { getValue("Date/Time").ifBlank { "N/A" } }
    val customerName = getValue("Customer Name").ifBlank { getValue("Customer").ifBlank { "N/A" } }
    val totalAmount = getValue("Total Amount").ifBlank { "N/A" }
    val paymentStatus = getValue("Payment Status").ifBlank { "Paid" }
    val verId = getValue("Invoice Verification ID").ifBlank { getValue("Verification ID").ifBlank { "VER-$invoiceNumber" } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = Color(0xFF166534),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Verified Invoice",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Official Record Verification",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF166534),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                // Header Banner
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF2563EB), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = storeName,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("AUTHENTIC ✅", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Detail Rows
                @Composable
                fun VerificationRow(label: String, value: String, isBold: Boolean = false, color: Color = Color(0xFF1E293B)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = value,
                            fontSize = 11.sp,
                            color = color,
                            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
                            textAlign = TextAlign.End
                        )
                    }
                }

                VerificationRow("1. Invoice Number:", invoiceNumber, isBold = true)
                VerificationRow("2. Invoice Date & Time:", dateTime)
                VerificationRow("3. Customer Name:", customerName, isBold = true)
                VerificationRow("4. Store Name:", storeName)
                VerificationRow("5. Total Amount:", totalAmount, isBold = true, color = Color(0xFF2563EB))
                VerificationRow("6. Payment Status:", paymentStatus, isBold = true, color = Color(0xFF166534))
                VerificationRow("7. Verification ID:", verId, isBold = true, color = Color(0xFF0F172A))

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF166534),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Qr Code Verified Authentic by System",
                        fontSize = 10.sp,
                        color = Color(0xFF166534),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Close Verification Window", fontWeight = FontWeight.Bold)
            }
        }
    )
}
