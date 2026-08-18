package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.data.entity.StoreSettings
import com.example.ui.theme.BentoCardDark
import com.example.ui.theme.BentoEmerald
import com.example.ui.theme.BentoPrimary
import com.example.ui.theme.BentoPrimaryLight
import com.example.util.PdfGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.mutableStateOf
import android.widget.Toast
import com.example.util.EscPosThermalPrinterService

@Composable
fun InvoiceReceiptDialog(
    sale: Sale,
    items: List<SaleItem>,
    settings: StoreSettings,
    onDismiss: () -> Unit,
    onDeleteInvoice: ((Sale) -> Unit)? = null
) {
    val context = LocalContext.current
    val dateFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val formattedDate = dateFormatter.format(Date(sale.timestamp))
    val resolvedCashier = when {
        sale.cashierName.isNotBlank() -> sale.cashierName
        settings.defaultCashierName.isNotBlank() -> settings.defaultCashierName
        else -> "Not Assigned"
    }

    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showPrintPreviewDialog by remember { mutableStateOf(false) }

    // 0: 58mm Thermal, 1: 80mm Thermal, 2: A4 Standard Invoice
    var selectedFormatIndex by remember { mutableIntStateOf(0) }

    val currentFormat = when (selectedFormatIndex) {
        0 -> PdfGenerator.ReceiptFormat.THERMAL_58MM
        1 -> PdfGenerator.ReceiptFormat.THERMAL_80MM
        else -> PdfGenerator.ReceiptFormat.A4
    }

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
                            .background(BentoPrimaryLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = BentoPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Printable Sale Invoice",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BentoCardDark
                        )
                        Text(
                            text = "Inv #: ${sale.invoiceNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Receipt Format Selector Tabs
                TabRow(
                    selectedTabIndex = selectedFormatIndex,
                    containerColor = Color(0xFFF1F5F9),
                    contentColor = BentoPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .padding(2.dp)
                ) {
                    Tab(
                        selected = selectedFormatIndex == 0,
                        onClick = { selectedFormatIndex = 0 },
                        text = { Text("58mm Roll", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                    )
                    Tab(
                        selected = selectedFormatIndex == 1,
                        onClick = { selectedFormatIndex = 1 },
                        text = { Text("80mm Roll", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                    )
                    Tab(
                        selected = selectedFormatIndex == 2,
                        onClick = { selectedFormatIndex = 2 },
                        text = { Text("A4 PDF", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Receipt Preview Container
                if (selectedFormatIndex == 2) {
                    // A4 Modern Invoice Preview Layout
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE2E8F0), shape = RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = settings.storeName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = BentoPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "Prop: ${settings.ownerName} | Ph: ${settings.phone}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = settings.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Bill To: ${sale.customerName}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text("Payment: ${sale.paymentType}", style = MaterialTheme.typography.bodySmall, color = BentoPrimary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Invoice: ${sale.invoiceNumber}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text("Cashier: $resolvedCashier", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                                Text(formattedDate, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Item", modifier = Modifier.weight(2f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("Qty", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            Text("Price", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                            Text("Total", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        LazyColumn(modifier = Modifier.height(110.dp)) {
                            items(items) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.productName, modifier = Modifier.weight(2f), style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                                    Text("${item.quantity.toInt()} ${item.unit}", modifier = Modifier.weight(0.8f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                                    Text("${item.salePrice.toInt()}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
                                    Text("${item.totalPrice.toInt()}", modifier = Modifier.weight(1.2f), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal:", style = MaterialTheme.typography.bodySmall)
                            Text("${settings.currencySymbol} ${sale.totalAmount.toInt()}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (sale.discount > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Discount:", style = MaterialTheme.typography.bodySmall, color = Color.Red)
                                Text("- ${settings.currencySymbol} ${sale.discount.toInt()}", style = MaterialTheme.typography.bodySmall, color = Color.Red)
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Net Payable:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BentoPrimary)
                            Text("${settings.currencySymbol} ${sale.netAmount.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = BentoPrimary)
                        }
                        if (sale.dueAmount > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Remaining Udhaar:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.Red)
                                Text("${settings.currencySymbol} ${sale.dueAmount.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.Red)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // QR Verification Section (Mobile Preview)
                        InvoiceQrCodeSection(
                            storeName = settings.storeName,
                            invoiceNumber = sale.invoiceNumber,
                            formattedDate = formattedDate,
                            customerName = sale.customerName,
                            totalAmount = sale.netAmount,
                            currencySymbol = settings.currencySymbol,
                            paidAmount = sale.paidAmount,
                            dueAmount = sale.dueAmount,
                            paymentType = sale.paymentType,
                            timestamp = sale.timestamp
                        )
                    }
                } else {
                    // Thermal Roll Visual Receipt Preview (Monospace / Realistic Roll)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFEFCE8)) // Light thermal paper background
                            .border(1.dp, Color(0xFFEAB308), shape = RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = settings.storeName.uppercase(Locale.getDefault()),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "PH: ${settings.phone}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "- - - - - - - - - - - - - - - - - - -",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("INV: ${sale.invoiceNumber}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                            Text(sale.paymentType, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                        }
                        Text("CUST: ${sale.customerName}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                        Text("CASHIER: $resolvedCashier", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                        Text("DATE: $formattedDate", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))

                        Text(
                            text = "- - - - - - - - - - - - - - - - - - -",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        LazyColumn(modifier = Modifier.height(110.dp)) {
                            items(items) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.productName, style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), modifier = Modifier.weight(1.5f))
                                    Text("${item.quantity.toInt()}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), modifier = Modifier.weight(0.5f), textAlign = TextAlign.Center)
                                    Text("${item.totalPrice.toInt()}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace), modifier = Modifier.weight(0.8f), textAlign = TextAlign.End)
                                }
                            }
                        }

                        Text(
                            text = "- - - - - - - - - - - - - - - - - - -",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TOTAL:", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
                            Text("${settings.currencySymbol} ${sale.totalAmount.toInt()}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
                        }
                        if (sale.discount > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("DISCOUNT:", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                                Text("-${settings.currencySymbol} ${sale.discount.toInt()}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("NET PAYABLE:", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
                            Text("${settings.currencySymbol} ${sale.netAmount.toInt()}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("PAID:", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                            Text("${settings.currencySymbol} ${sale.paidAmount.toInt()}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace))
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        InvoiceQrCodeSection(
                            storeName = settings.storeName,
                            invoiceNumber = sale.invoiceNumber,
                            formattedDate = formattedDate,
                            customerName = sale.customerName,
                            totalAmount = sale.netAmount,
                            currencySymbol = settings.currencySymbol,
                            paidAmount = sale.paidAmount,
                            dueAmount = sale.dueAmount,
                            paymentType = sale.paymentType,
                            timestamp = sale.timestamp
                        )

                        Text(
                            text = "*** THANK YOU ***",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // WhatsApp Attachment & Share Button
                val whatsappGreen = Color(0xFF25D366)
                Button(
                    onClick = {
                        val pdfFile = PdfGenerator.generateAndGetFile(
                            context = context,
                            sale = sale,
                            items = items,
                            settings = settings,
                            format = currentFormat
                        )
                        val whatsappMsg = buildString {
                            appendLine("🧾 *INVOICE RECEIPT*")
                            appendLine("🏪 *${settings.storeName}*")
                            appendLine("--------------------------")
                            appendLine("Invoice #: *${sale.invoiceNumber}*")
                            appendLine("Customer: *${sale.customerName}*")
                            appendLine("Cashier: *${resolvedCashier}*")
                            appendLine("Date: $formattedDate")
                            appendLine("--------------------------")
                            for (item in items) {
                                appendLine("• ${item.productName} x${item.quantity.toInt()} = ${settings.currencySymbol}${item.totalPrice.toInt()}")
                            }
                            appendLine("--------------------------")
                            appendLine("Net Payable: *${settings.currencySymbol}${sale.netAmount.toInt()}*")
                            appendLine("Paid Amount: *${settings.currencySymbol}${sale.paidAmount.toInt()}*")
                            if (sale.dueAmount > 0) {
                                appendLine("Pending Udhaar: *${settings.currencySymbol}${sale.dueAmount.toInt()}*")
                            }
                            appendLine("--------------------------")
                            appendLine("Thank you for shopping with us! 🙏")
                        }
                        PdfGenerator.shareToWhatsApp(context, pdfFile, whatsappMsg)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = whatsappGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Attach & Share on WhatsApp", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // PDF Save & Print Primary Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val pdfFile = PdfGenerator.generateAndGetFile(
                                context = context,
                                sale = sale,
                                items = items,
                                settings = settings,
                                format = currentFormat
                            )
                            if (pdfFile != null) {
                                PdfGenerator.openOrSharePdf(context, pdfFile, action = "open")
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save PDF")
                    }

                    OutlinedButton(
                        onClick = {
                            val pdfFile = PdfGenerator.generateAndGetFile(
                                context = context,
                                sale = sale,
                                items = items,
                                settings = settings,
                                format = currentFormat
                            )
                            if (pdfFile != null) {
                                PdfGenerator.printPdf(context, pdfFile)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print Receipt")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Print Preview & Bluetooth Thermal Printer Primary Button
                Button(
                    onClick = { showPrintPreviewDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BentoPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PDF Print Preview & Bluetooth Printer", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ESC/POS Direct Thermal Print Row
                OutlinedButton(
                    onClick = {
                        val paperFmt = if (selectedFormatIndex == 1) EscPosThermalPrinterService.PaperFormat.FORMAT_80MM else EscPosThermalPrinterService.PaperFormat.FORMAT_58MM
                        val escPosBytes = EscPosThermalPrinterService.generateEscPosCommandBytes(sale, items, settings, paperFmt)
                        
                        // Attempt Bluetooth print first, fallback to USB
                        EscPosThermalPrinterService.printViaBluetooth(context, escPosBytes) { success, msg ->
                            if (!success && msg.contains("Bluetooth", ignoreCase = true)) {
                                EscPosThermalPrinterService.printViaUsb(context, escPosBytes) { usbSuccess, usbMsg ->
                                    Toast.makeText(context, if (usbSuccess) usbMsg else msg, Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp), tint = BentoPrimary)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("ESC/POS Direct Print (BT/USB)", color = BentoPrimary, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Share, Delete & Close Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            val pdfFile = PdfGenerator.generateAndGetFile(
                                context = context,
                                sale = sale,
                                items = items,
                                settings = settings,
                                format = currentFormat
                            )
                            if (pdfFile != null) {
                                PdfGenerator.openOrSharePdf(context, pdfFile, action = "share")
                            } else {
                                val shareBody = buildString {
                                    appendLine("${settings.storeName}")
                                    appendLine("Invoice: ${sale.invoiceNumber}")
                                    appendLine("Customer: ${sale.customerName}")
                                    appendLine("Cashier: $resolvedCashier")
                                    appendLine("Date: $formattedDate")
                                    appendLine("--------------------------")
                                    for (item in items) {
                                        appendLine("${item.productName} x ${item.quantity.toInt()} = ${item.totalPrice.toInt()}")
                                    }
                                    appendLine("--------------------------")
                                    appendLine("Total Payable: ${settings.currencySymbol} ${sale.netAmount.toInt()}")
                                    appendLine("Paid Amount: ${settings.currencySymbol} ${sale.paidAmount.toInt()}")
                                    if (sale.dueAmount > 0) {
                                        appendLine("Due Balance: ${settings.currencySymbol} ${sale.dueAmount.toInt()}")
                                    }
                                    appendLine("Thank you!")
                                }

                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareBody)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Receipt Bill"))
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }

                    if (onDeleteInvoice != null) {
                        OutlinedButton(
                            onClick = { showDeleteConfirmation = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFDC2626))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    )

    // Print Preview Dialog
    if (showPrintPreviewDialog) {
        PrintPreviewDialog(
            sale = sale,
            items = items,
            settings = settings,
            initialFormat = currentFormat,
            onDismiss = { showPrintPreviewDialog = false }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Invoice", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this invoice?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmation = false
                        onDeleteInvoice?.invoke(sale)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

