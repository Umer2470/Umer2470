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

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(InvoiceFilter.ALL) }
    var selectedSaleForReceipt by remember { mutableStateOf<Pair<Sale, List<SaleItem>>?>(null) }
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
                subtitle = "${sales.size} Total Invoices • PDF Export & Digital Records",
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
            // Metrics Summary with PDF Batch Export
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
                        Text(
                            text = "Digital PDF Audit Archives",
                            fontSize = 11.sp,
                            color = Gold400,
                            fontWeight = FontWeight.Medium
                        )

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
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
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

                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "$currency %.2f".format(sale.netAmount),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = Navy900
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Quick Share PDF
                                        FilledIconButton(
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
                                            colors = IconButtonDefaults.filledIconButtonColors(
                                                containerColor = Navy900,
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier
                                                .size(32.dp)
                                                .testTag("quick_pdf_share_${sale.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "Share PDF",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Receipt Dialog Button
                                        Surface(
                                            color = Navy100,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ReceiptLong,
                                                    contentDescription = "View",
                                                    tint = Navy900,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Receipt",
                                                    fontSize = 11.sp,
                                                    color = Navy900,
                                                    fontWeight = FontWeight.Bold
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
}
