package com.example.ui.invoice

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.data.entity.StoreSettings
import com.example.ui.theme.*
import com.example.util.InvoiceFormattingService
import com.example.util.PdfGenerator
import com.example.util.PrintableInvoice
import com.example.util.QrCodeRenderer

@Composable
fun InvoiceReceiptDialog(
    sale: Sale,
    items: List<SaleItem>,
    settings: StoreSettings?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val effectiveSettings = settings ?: StoreSettings()
    val printableInvoice = remember(sale, items, settings) {
        InvoiceFormattingService.formatSaleTransaction(sale, items, effectiveSettings)
    }

    var showQrVerification by remember { mutableStateOf(false) }

    if (showQrVerification) {
        InvoiceQrVerificationDialog(
            invoice = printableInvoice,
            onDismiss = { showQrVerification = false }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.9f)
                .testTag("invoice_receipt_dialog"),
            shape = RoundedCornerShape(16.dp),
            color = Slate50
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Navy900)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tax Invoice / Receipt",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_receipt_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                // Printable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = printableInvoice.header.storeName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Navy900,
                                textAlign = TextAlign.Center
                            )
                            if (printableInvoice.header.phone.isNotBlank()) {
                                Text(
                                    text = "Tel: ${printableInvoice.header.phone}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Navy500
                                )
                            }
                            if (printableInvoice.header.address.isNotBlank()) {
                                Text(
                                    text = printableInvoice.header.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Navy500,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Divider(modifier = Modifier.padding(vertical = 12.dp))

                            // Meta Info
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Invoice #: ${printableInvoice.meta.invoiceNumber}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text(printableInvoice.meta.formattedDate, fontSize = 12.sp, color = Navy500)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Customer: ${printableInvoice.customer.name}", fontSize = 12.sp)
                                Text("Cashier: ${printableInvoice.meta.cashierName}", fontSize = 12.sp, color = Navy500)
                            }

                            Divider(modifier = Modifier.padding(vertical = 12.dp))

                            // Items Table
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Slate100)
                                    .padding(vertical = 4.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Item", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(2f))
                                Text("Qty", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                Text("Rate", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                Text("Total", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                            }

                            printableInvoice.items.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.productName, fontSize = 12.sp, modifier = Modifier.weight(2f))
                                    Text("%.1f %s".format(item.quantity, item.unit), fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                                    Text("%.2f".format(item.unitPrice), fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                    Text("%.2f".format(item.totalPrice), fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 12.dp))

                            // Totals
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text("Subtotal: ${printableInvoice.totals.currencySymbol} %.2f".format(printableInvoice.totals.subtotal), fontSize = 12.sp)
                                if (printableInvoice.totals.discount > 0) {
                                    Text("Discount: -${printableInvoice.totals.currencySymbol} %.2f".format(printableInvoice.totals.discount), fontSize = 12.sp, color = Rose600)
                                }
                                if (printableInvoice.totals.taxAmount > 0) {
                                    Text("Tax: +${printableInvoice.totals.currencySymbol} %.2f".format(printableInvoice.totals.taxAmount), fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "NET TOTAL: ${printableInvoice.totals.currencySymbol} %.2f".format(printableInvoice.totals.netAmount),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Navy900
                                )
                                Text("Paid: ${printableInvoice.totals.currencySymbol} %.2f".format(printableInvoice.totals.paidAmount), fontSize = 12.sp, color = Emerald600)
                                if (printableInvoice.totals.dueAmount > 0) {
                                    Text("Due: ${printableInvoice.totals.currencySymbol} %.2f".format(printableInvoice.totals.dueAmount), fontSize = 12.sp, color = Rose600, fontWeight = FontWeight.Bold)
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 12.dp))

                            // QR Code
                            val qrImage = remember(printableInvoice.qrPayload) {
                                QrCodeRenderer.generateQrImageBitmap(printableInvoice.qrPayload, 160, 160)
                            }
                            if (qrImage != null) {
                                Image(
                                    bitmap = qrImage,
                                    contentDescription = "Invoice QR Verification",
                                    modifier = Modifier.size(120.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Scan QR to verify invoice authenticity",
                                    fontSize = 10.sp,
                                    color = Navy500
                                )
                            }

                            if (printableInvoice.footerText.isNotBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = printableInvoice.footerText,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = Navy600
                                )
                            }
                        }
                    }
                }

                // Actions Footer
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showQrVerification = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("verify_qr_button")
                        ) {
                            Text("QR Code")
                        }

                        Button(
                            onClick = {
                                val pdfFile = PdfGenerator.generateInvoicePdf(context, printableInvoice)
                                if (pdfFile != null) {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        pdfFile
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Invoice PDF"))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("share_pdf_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share PDF")
                        }
                    }
                }
            }
        }
    }
}
