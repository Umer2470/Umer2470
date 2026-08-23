package com.example.ui.invoice

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.ui.components.AppHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InvoiceScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val sales by viewModel.sales.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
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

    val filteredSales = remember(sales, searchQuery) {
        if (searchQuery.isBlank()) sales
        else {
            sales.filter {
                it.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                it.customerName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Invoices & Receipts",
                subtitle = "${sales.size} Total Transactions",
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
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search by Invoice # or Customer") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("invoice_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Navy900,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredSales.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "No invoices created yet." else "No invoices match your search.",
                        color = Navy500,
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
                        val currency = storeSettings?.currencySymbol ?: "Rs"

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
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
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
                                            text = if (sale.dueAmount <= 0) "Paid" else "Due: $currency %.0f".format(sale.dueAmount),
                                            backgroundColor = if (sale.dueAmount <= 0) Emerald100 else Rose100,
                                            textColor = if (sale.dueAmount <= 0) Emerald600 else Rose600
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Customer: ${sale.customerName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Navy600
                                    )
                                    Text(
                                        text = "$formattedDate • Cashier: ${sale.cashierName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Navy500
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$currency %.2f".format(sale.netAmount),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Navy900
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ReceiptLong,
                                            contentDescription = "View Receipt",
                                            tint = Navy600,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "View",
                                            fontSize = 12.sp,
                                            color = Navy600,
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
