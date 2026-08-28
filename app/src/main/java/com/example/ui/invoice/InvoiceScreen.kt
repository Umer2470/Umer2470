package com.example.ui.invoice

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.data.entity.StoreSettings
import com.example.ui.components.AppHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.EscPosThermalPrinterService
import com.example.util.PdfGenerator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class InvoiceFilter {
    ALL,
    PAID,
    DUE,
    CASH,
    CREDIT
}

@Composable
fun InvoiceScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val sales by viewModel.sales.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()
    val currency = storeSettings?.currencySymbol ?: "Rs"

    val lastPrinterConfig = remember(storeSettings) {
        EscPosThermalPrinterService.getLastPrinterConfig(context, storeSettings?.paperWidthMm ?: 80)
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(InvoiceFilter.ALL) }
    var selectedSaleForReceipt by remember { mutableStateOf<Pair<Sale, List<SaleItem>>?>(null) }
    var printingSaleId by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()

    if (selectedSaleForReceipt != null) {
        val (sale, items) = selectedSaleForReceipt!!
        InvoiceReceiptDialog(
            sale = sale,
            items = items,
            settings = storeSettings,
            onDismiss = { selectedSaleForReceipt = null }
        )
    }

    val totalRevenue = remember(sales) { sales.sumOf { it.netAmount } }
    val totalCollected = remember(sales) { sales.sumOf { it.paidAmount } }
    val totalDue = remember(sales) { sales.sumOf { it.dueAmount } }

    val filteredSales = remember(sales, searchQuery, selectedFilter) {
        sales.filter { sale ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                sale.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                sale.customerName.contains(searchQuery, ignoreCase = true) ||
                sale.cashierName.contains(searchQuery, ignoreCase = true)
            }
            val matchesFilter = when (selectedFilter) {
                InvoiceFilter.ALL -> true
                InvoiceFilter.PAID -> sale.dueAmount <= 0
                InvoiceFilter.DUE -> sale.dueAmount > 0
                InvoiceFilter.CASH -> sale.paymentType.equals("Cash", ignoreCase = true)
                InvoiceFilter.CREDIT -> sale.paymentType.equals("Credit", ignoreCase = true) || sale.dueAmount > 0
            }
            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Invoices & Receipts",
                subtitle = "${sales.size} Total Invoices • One-Tap Thermal Quick Print & PDF",
                onBackClick = onNavigateBack
            )
        },
        containerColor = Slate50
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Metrics Summary with PDF Batch Export & Printer Status Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Navy900)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Gross Sales", fontSize = 11.sp, color = Slate300)
                            Text(
                                "$currency %.0f".format(totalRevenue),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        VerticalDivider(
                            modifier = Modifier
                                .height(30.dp)
                                .padding(horizontal = 8.dp),
                            color = Navy700
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Collected", fontSize = 11.sp, color = Slate300)
                            Text(
                                "$currency %.0f".format(totalCollected),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald400
                            )
                        }
                        VerticalDivider(
                            modifier = Modifier
                                .height(30.dp)
                                .padding(horizontal = 8.dp),
                            color = Navy700
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pending Due", fontSize = 11.sp, color = Slate300)
                            Text(
                                "$currency %.0f".format(totalDue),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Rose400
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = Navy800)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Print,
                                contentDescription = null,
                                tint = Gold400,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Thermal Re-Print: ${lastPrinterConfig.paperWidthMm}mm ESC/POS ${if (!lastPrinterConfig.deviceName.isNullOrBlank()) "(${lastPrinterConfig.deviceName})" else "(Ready)"}",
                                fontSize = 11.sp,
                                color = Gold400,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                if (filteredSales.isEmpty()) {
                                    Toast.makeText(context, "No invoices to export", Toast.LENGTH_SHORT).show()
                                } else {
                                    val auditPdf = PdfGenerator.generateBatchInvoicesSummaryPdf(
                                        context,
                                        filteredSales,
                                        storeSettings ?: StoreSettings()
                                    )
                                    if (auditPdf != null) {
                                        Toast.makeText(context, "Audit Report PDF Saved: ${auditPdf.name}", Toast.LENGTH_LONG).show()
                                        PdfGenerator.sharePdfFile(context, auditPdf, "Share Invoices Audit Summary PDF")
                                    } else {
                                        Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Gold500,
                                contentColor = Navy900
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("export_audit_pdf_button")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export Audit PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Invoice # or Customer...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Navy700) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("invoice_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Navy900,
                    unfocusedBorderColor = Slate300,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == InvoiceFilter.ALL,
                    onClick = { selectedFilter = InvoiceFilter.ALL },
                    label = { Text("All (${sales.size})", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = selectedFilter == InvoiceFilter.PAID,
                    onClick = { selectedFilter = InvoiceFilter.PAID },
                    label = { Text("Paid", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = selectedFilter == InvoiceFilter.DUE,
                    onClick = { selectedFilter = InvoiceFilter.DUE },
                    label = { Text("Pending Due", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = selectedFilter == InvoiceFilter.CREDIT,
                    onClick = { selectedFilter = InvoiceFilter.CREDIT },
                    label = { Text("Credit", fontSize = 12.sp) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredSales.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "No invoices match the selected filter." else "No invoices match '$searchQuery'",
                        color = Navy600,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredSales, key = { it.id }) { sale ->
                        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                        val formattedDate = dateFormat.format(Date(sale.createdAt))
                        val isPrinting = printingSaleId == sale.id

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch {
                                        val (s, items) = viewModel.getSaleDetails(sale.id)
                                        if (s != null) {
                                            selectedSaleForReceipt = Pair(s, items)
                                        }
                                    }
                                }
                                .testTag("invoice_item_${sale.id}"),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = sale.invoiceNumber,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Navy900
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            StatusBadge(
                                                text = if (sale.dueAmount <= 0) "PAID" else "DUE: $currency %.0f".format(sale.dueAmount),
                                                backgroundColor = if (sale.dueAmount <= 0) Emerald100 else Rose100,
                                                textColor = if (sale.dueAmount <= 0) Emerald700 else Rose600
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Customer: ${sale.customerName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Navy800
                                        )
                                        Text(
                                            text = "$formattedDate • Pay: ${sale.paymentType} • Cashier: ${sale.cashierName.ifBlank { "Muhammad Umer" }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Navy600
                                        )
                                    }

                                    Text(
                                        text = "$currency %.2f".format(sale.netAmount),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = Navy900
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = Slate100)
                                Spacer(modifier = Modifier.height(8.dp))

                                // Quick Action Row with dedicated 'Quick Print' button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Pay: ${sale.paymentType}",
                                        fontSize = 11.sp,
                                        color = Slate500,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 1. Quick Print Button (One-Tap Thermal Re-print)
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    printingSaleId = sale.id
                                                    try {
                                                        val (s, items) = viewModel.getSaleDetails(sale.id)
                                                        if (s != null) {
                                                            val result = EscPosThermalPrinterService.quickRePrintReceipt(
                                                                context = context,
                                                                sale = s,
                                                                items = items,
                                                                settings = storeSettings
                                                            )
                                                            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            Toast.makeText(context, "Failed to load invoice items", Toast.LENGTH_SHORT).show()
                                                        }
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Print error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    } finally {
                                                        printingSaleId = null
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Gold500,
                                                contentColor = Navy900
                                            ),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .height(32.dp)
                                                .testTag("quick_print_${sale.id}")
                                        ) {
                                            if (isPrinting) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(12.dp),
                                                    color = Navy900,
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Printing...", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.Print,
                                                    contentDescription = "Quick Print",
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Quick Print", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // 2. Quick Share PDF
                                        FilledTonalIconButton(
                                            onClick = {
                                                scope.launch {
                                                    val (s, items) = viewModel.getSaleDetails(sale.id)
                                                    if (s != null) {
                                                        val pdfFile = PdfGenerator.generatePrintablePdfInvoice(
                                                            context,
                                                            s,
                                                            items,
                                                            storeSettings ?: StoreSettings(),
                                                            PdfGenerator.ReceiptFormat.A4
                                                        )
                                                        if (pdfFile != null) {
                                                            PdfGenerator.sharePdfFile(context, pdfFile, "Share ${s.invoiceNumber} PDF")
                                                        } else {
                                                            Toast.makeText(context, "Error creating PDF", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            },
                                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                containerColor = Navy100,
                                                contentColor = Navy900
                                            ),
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("quick_pdf_share_${sale.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "Share PDF",
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }

                                        // 3. Receipt Dialog Button
                                        OutlinedButton(
                                            onClick = {
                                                scope.launch {
                                                    val (s, items) = viewModel.getSaleDetails(sale.id)
                                                    if (s != null) {
                                                        selectedSaleForReceipt = Pair(s, items)
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp).testTag("view_receipt_${sale.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ReceiptLong,
                                                contentDescription = "View",
                                                tint = Navy800,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "View",
                                                fontSize = 11.sp,
                                                color = Navy800,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

