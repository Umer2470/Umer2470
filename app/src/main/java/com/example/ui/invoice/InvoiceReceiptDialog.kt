package com.example.ui.invoice

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Receipt
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

enum class InvoiceViewFormat {
    THERMAL_RECEIPT,
    A4_TAX_INVOICE
}

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

    var selectedFormat by remember { mutableStateOf(InvoiceViewFormat.THERMAL_RECEIPT) }
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
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.94f)
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
                    Column {
                        Text(
                            text = if (selectedFormat == InvoiceViewFormat.THERMAL_RECEIPT) "Thermal 80mm Receipt" else "Official A4 Tax Invoice",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Invoice #${printableInvoice.meta.invoiceNumber}",
                            fontSize = 12.sp,
                            color = Gold400
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                }

                // Format Switcher Bar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Navy800
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFormat == InvoiceViewFormat.THERMAL_RECEIPT,
                            onClick = { selectedFormat = InvoiceViewFormat.THERMAL_RECEIPT },
                            label = { Text("Thermal Receipt (POS)") },
                            leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Gold500,
                                selectedLabelColor = Navy900,
                                selectedLeadingIconColor = Navy900,
                                containerColor = Navy700,
                                labelColor = Color.White,
                                iconColor = Color.White
                            )
                        )
                        FilterChip(
                            selected = selectedFormat == InvoiceViewFormat.A4_TAX_INVOICE,
                            onClick = { selectedFormat = InvoiceViewFormat.A4_TAX_INVOICE },
                            label = { Text("A4 Tax Invoice") },
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Gold500,
                                selectedLabelColor = Navy900,
                                selectedLeadingIconColor = Navy900,
                                containerColor = Navy700,
                                labelColor = Color.White,
                                iconColor = Color.White
                            )
                        )
                    }
                }

                // Printable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    if (selectedFormat == InvoiceViewFormat.THERMAL_RECEIPT) {
                        // 80mm Thermal Receipt Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            shape = RoundedCornerShape(8.dp)
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
                                    fontWeight = FontWeight.Black,
                                    color = Navy900,
                                    textAlign = TextAlign.Center
                                )
                                if (printableInvoice.header.phone.isNotBlank()) {
                                    Text(
                                        text = "Tel: ${printableInvoice.header.phone}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = Navy600
                                    )
                                }
                                if (printableInvoice.header.address.isNotBlank()) {
                                    Text(
                                        text = printableInvoice.header.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Navy600,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                                // Invoice Details
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Invoice: ${printableInvoice.meta.invoiceNumber}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Navy900)
                                    Text(printableInvoice.meta.formattedDate, fontSize = 12.sp, color = Navy700)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Customer: ${printableInvoice.customer.name}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Navy900)
                                    Text("Cashier: ${printableInvoice.meta.cashierName}", fontSize = 12.sp, color = Navy700)
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                                // Items Table
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Slate100, RoundedCornerShape(4.dp))
                                        .padding(vertical = 6.dp, horizontal = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Item", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Navy900, modifier = Modifier.weight(2.2f))
                                    Text("Qty", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Navy900, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                                    Text("Rate", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Navy900, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                    Text("Total", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Navy900, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                                }

                                printableInvoice.items.forEach { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp, horizontal = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(item.productName, fontSize = 12.sp, color = Navy900, modifier = Modifier.weight(2.2f))
                                        Text("%.1f %s".format(item.quantity, item.unit), fontSize = 12.sp, color = Navy800, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                                        Text("%.2f".format(item.unitPrice), fontSize = 12.sp, color = Navy800, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                        Text("%.2f".format(item.totalPrice), fontSize = 12.sp, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, color = Navy900)
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                                // Totals
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text("Subtotal: ${printableInvoice.totals.currencySymbol} %.2f".format(printableInvoice.totals.subtotal), fontSize = 12.sp, color = Navy800)
                                    if (printableInvoice.totals.discount > 0) {
                                        Text("Discount: -${printableInvoice.totals.currencySymbol} %.2f".format(printableInvoice.totals.discount), fontSize = 12.sp, color = Rose600, fontWeight = FontWeight.Bold)
                                    }
                                    if (printableInvoice.totals.taxAmount > 0) {
                                        Text("Tax: +${printableInvoice.totals.currencySymbol} %.2f".format(printableInvoice.totals.taxAmount), fontSize = 12.sp, color = Navy800)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "NET TOTAL: ${printableInvoice.totals.currencySymbol} %.2f".format(printableInvoice.totals.netAmount),
                                        fontWeight = FontWeight.Black,
                                        fontSize = 17.sp,
                                        color = Navy900
                                    )
                                    Text("Paid: ${printableInvoice.totals.currencySymbol} %.2f".format(printableInvoice.totals.paidAmount), fontSize = 13.sp, color = Emerald700, fontWeight = FontWeight.Bold)
                                    if (printableInvoice.totals.dueAmount > 0) {
                                        Text("Due Balance: ${printableInvoice.totals.currencySymbol} %.2f".format(printableInvoice.totals.dueAmount), fontSize = 13.sp, color = Rose600, fontWeight = FontWeight.Black)
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                                // QR Code
                                val qrImage = remember(printableInvoice.qrPayload) {
                                    QrCodeRenderer.generateQrImageBitmap(printableInvoice.qrPayload, 160, 160)
                                }
                                if (qrImage != null) {
                                    Image(
                                        bitmap = qrImage,
                                        contentDescription = "Invoice QR Verification",
                                        modifier = Modifier.size(110.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Scan QR to verify invoice authenticity",
                                        fontSize = 10.sp,
                                        color = Navy600
                                    )
                                }

                                if (printableInvoice.footerText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = printableInvoice.footerText,
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        color = Navy700
                                    )
                                }
                            }
                        }
                    } else {
                        // Official A4 Tax Invoice Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp)
                            ) {
                                // A4 Top Header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = printableInvoice.header.storeName,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Black,
                                            color = Navy900
                                        )
                                        Text(
                                            text = "Wholesale & Retail Sanitary Hardware",
                                            fontSize = 12.sp,
                                            color = Navy600
                                        )
                                        Text(
                                            text = printableInvoice.header.address,
                                            fontSize = 11.sp,
                                            color = Navy600
                                        )
                                        Text(
                                            text = "Phone/WhatsApp: ${printableInvoice.header.phone}",
                                            fontSize = 11.sp,
                                            color = Navy600
                                        )
                                    }

                                    Surface(
                                        color = Navy900,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "TAX INVOICE",
                                                fontWeight = FontWeight.Black,
                                                color = Gold400,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = printableInvoice.meta.invoiceNumber,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                                // Bill To & Meta Section
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("BILL TO / CUSTOMER:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Navy700)
                                        Text(printableInvoice.customer.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Navy900)
                                        if (printableInvoice.customer.phone.isNotBlank()) {
                                            Text("Phone: ${printableInvoice.customer.phone}", fontSize = 11.sp, color = Navy700)
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Date: ${printableInvoice.meta.formattedDate}", fontSize = 11.sp, color = Navy800)
                                        Text("Payment: ${printableInvoice.meta.paymentType}", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Emerald700)
                                        Text("Cashier: ${printableInvoice.meta.cashierName}", fontSize = 11.sp, color = Navy800)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // A4 Items Table
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Navy900, RoundedCornerShape(4.dp))
                                        .padding(vertical = 8.dp, horizontal = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("#", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White, modifier = Modifier.width(24.dp))
                                    Text("Item Description", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White, modifier = Modifier.weight(2f))
                                    Text("Qty", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                                    Text("Unit Price", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                    Text("Total Amount", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                                }

                                printableInvoice.items.forEachIndexed { index, item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (index % 2 == 0) Color.White else Slate50)
                                            .padding(vertical = 6.dp, horizontal = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${index + 1}", fontSize = 11.sp, color = Navy700, modifier = Modifier.width(24.dp))
                                        Text(item.productName, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Navy900, modifier = Modifier.weight(2f))
                                        Text("%.1f %s".format(item.quantity, item.unit), fontSize = 11.sp, color = Navy800, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                                        Text("%.2f".format(item.unitPrice), fontSize = 11.sp, color = Navy800, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                                        Text("%.2f".format(item.totalPrice), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Navy900, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                                // A4 Financial Breakdown
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Terms & Declarations:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Navy700)
                                        Text(
                                            text = printableInvoice.footerText,
                                            fontSize = 10.sp,
                                            color = Navy600
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("________________________", color = Navy400)
                                        Text("Authorized Signature", fontSize = 11.sp, color = Navy700, fontWeight = FontWeight.SemiBold)
                                    }

                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        modifier = Modifier.width(180.dp)
                                    ) {
                                        Text("Subtotal: ${printableInvoice.totals.currencySymbol} %.2f".format(printableInvoice.totals.subtotal), fontSize = 12.sp, color = Navy800)
                                        if (printableInvoice.totals.discount > 0) {
                                            Text("Discount: -${printableInvoice.totals.currencySymbol} %.2f".format(printableInvoice.totals.discount), fontSize = 12.sp, color = Rose600, fontWeight = FontWeight.Bold)
                                        }
                                        if (printableInvoice.totals.taxAmount > 0) {
                                            Text("Tax / GST: +${printableInvoice.totals.currencySymbol} %.2f".format(printableInvoice.totals.taxAmount), fontSize = 12.sp, color = Navy800)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Surface(
                                            color = Navy900,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "GRAND TOTAL: ${printableInvoice.totals.currencySymbol} %.2f".format(printableInvoice.totals.netAmount),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Paid: ${printableInvoice.totals.currencySymbol} %.2f".format(printableInvoice.totals.paidAmount), fontSize = 12.sp, color = Emerald700, fontWeight = FontWeight.Bold)
                                        if (printableInvoice.totals.dueAmount > 0) {
                                            Text("Balance Due: ${printableInvoice.totals.currencySymbol} %.2f".format(printableInvoice.totals.dueAmount), fontSize = 12.sp, color = Rose600, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showQrVerification = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("verify_qr_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
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
                                .weight(1.3f)
                                .height(46.dp)
                                .testTag("share_pdf_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export / Share PDF")
                        }
                    }
                }
            }
        }
    }
}
