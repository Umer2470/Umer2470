package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.invoice.ScannedInvoiceVerificationDialog
import com.example.ui.theme.Navy900

@Composable
fun BarCodeScannerDialog(
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var manualInput by remember { mutableStateOf("") }
    var scannedVerificationData by remember { mutableStateOf<String?>(null) }

    if (scannedVerificationData != null) {
        ScannedInvoiceVerificationDialog(
            scannedData = scannedVerificationData!!,
            onDismiss = {
                scannedVerificationData = null
                onDismiss()
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .testTag("barcode_scanner_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scanner",
                            tint = Navy900
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Barcode / QR Scanner",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = manualInput,
                    onValueChange = { manualInput = it },
                    label = { Text("Scan or Enter Barcode / QR") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("barcode_manual_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            if (manualInput.isNotBlank()) {
                                if (manualInput.startsWith("Store:") || manualInput.contains("Invoice #:")) {
                                    scannedVerificationData = manualInput
                                } else {
                                    onBarcodeScanned(manualInput.trim())
                                    onDismiss()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("submit_barcode_button")
                    ) {
                        Text("Submit")
                    }
                }
            }
        }
    }
}
