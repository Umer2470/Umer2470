package com.example.ui.invoice

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.data.entity.StoreSettings
import com.example.ui.theme.*
import com.example.util.EscPosThermalPrinterService
import com.example.util.InvoiceFormattingService
import com.example.util.PdfGenerator
import com.example.util.PrintableInvoice
import com.example.util.QrCodeRenderer
import kotlinx.coroutines.launch
import java.io.File

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
    var lastExportedFile by remember { mutableStateOf<File?>(null) }
    var exportSuccessMessage by remember { mutableStateOf<String?>(null) }
    var isQuickPrinting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

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
                .fillMaxHeight(0.95f)
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
                        // Header Quick Print Button
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val result = EscPosThermalPrinterService.quickRePrintReceipt(
                                        context = context,
                                        sale = sale,
                                        items = items,
                                        settings = effectiveSettings
                                    )
                                    Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("receipt_print_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = "Quick Print",
                                tint = Color.White
                            )
                        }

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

                // Export Success Banner if triggered
                exportSuccessMessage?.let { msg ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        color = Emerald50,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Emerald200)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald600, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(msg, fontSize = 12.sp, color = Emerald800, fontWeight = FontWeight.Medium)
                            }
                            if (lastExportedFile != null) {
                                TextButton(
                                    onClick = { PdfGenerator.openPdfFile(context, lastExportedFile!!) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Emerald700)
                                ) {
                                    Text("OPEN", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
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
                                    Text("Date: ${printableInvoice.meta.saleDate.ifBlank { printableInvoice.meta.formattedDate }}", fontSize = 12.sp, color = Navy700)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Customer: ${printableInvoice.customer.name}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Navy900)
                                    Text("Time: ${printableInvoice.meta.saleTime}", fontSize = 12.sp, color = Navy700)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Pay Mode: ${printableInvoice.meta.paymentType}", fontSize = 11.sp, color = Navy700)
                                    Text("Cashier: ${printableInvoice.meta.cashierName}", fontSize = 11.sp, color = Navy700)
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
                                    QrCodeRenderer.generateQrImageBitmap(printableInvoice.qrPayload, 512, 512)
                                }
                                if (qrImage != null) {
                                    Image(
                                        bitmap = qrImage,
                                        contentDescription = "Invoice QR Verification",
                                        filterQuality = androidx.compose.ui.graphics.FilterQuality.High,
                                        modifier = Modifier.size(120.dp)
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Scan QR to verify invoice authenticity",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Navy700
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
                                        Text("Date: ${printableInvoice.meta.saleDate.ifBlank { printableInvoice.meta.formattedDate }}", fontSize = 11.sp, color = Navy800, fontWeight = FontWeight.SemiBold)
                                        if (printableInvoice.meta.saleTime.isNotBlank()) {
                                            Text("Time: ${printableInvoice.meta.saleTime}", fontSize = 11.sp, color = Navy800)
                                        }
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Quick Thermal Re-Print Button
                            Button(
                                onClick = {
                                    scope.launch {
                                        isQuickPrinting = true
                                        try {
                                            val result = EscPosThermalPrinterService.quickRePrintReceipt(
                                                context = context,
                                                sale = sale,
                                                items = items,
                                                settings = effectiveSettings
                                            )
                                            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Print error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isQuickPrinting = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Gold500,
                                    contentColor = Navy900
                                ),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(44.dp)
                                    .testTag("dialog_quick_print_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isQuickPrinting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Navy900,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Printing...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Quick Print", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // QR Verification Action
                            OutlinedButton(
                                onClick = { showQrVerification = true },
                                modifier = Modifier
                                    .weight(0.7f)
                                    .height(44.dp)
                                    .testTag("verify_qr_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("QR", fontSize = 12.sp)
                            }

                            // Share PDF Button
                            Button(
                                onClick = {
                                    val pdfFormat = if (selectedFormat == InvoiceViewFormat.THERMAL_RECEIPT) {
                                        PdfGenerator.ReceiptFormat.THERMAL_80MM
                                    } else {
                                        PdfGenerator.ReceiptFormat.A4
                                    }
                                    val pdfFile = PdfGenerator.generateInvoicePdf(context, printableInvoice, pdfFormat)
                                    if (pdfFile != null) {
                                        lastExportedFile = pdfFile
                                        PdfGenerator.sharePdfFile(context, pdfFile, "Share Invoice ${printableInvoice.meta.invoiceNumber}")
                                    } else {
                                        Toast.makeText(context, "Failed to export PDF", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(44.dp)
                                    .testTag("share_pdf_button"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
