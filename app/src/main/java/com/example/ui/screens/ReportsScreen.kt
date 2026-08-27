package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.ui.components.AppHeader
import com.example.ui.components.KpiCard
import com.example.ui.components.SectionHeader
import com.example.ui.invoice.InvoiceReceiptDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel

data class CashierPerformanceStat(
    val cashierName: String,
    val invoiceCount: Int,
    val netTotal: Double,
    val collectedTotal: Double
)

@Composable
fun ReportsScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val sales by viewModel.sales.collectAsState()
    val purchases by viewModel.purchases.collectAsState()
    val products by viewModel.products.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()

    val currency = storeSettings?.currencySymbol ?: "Rs"
    val totalRevenue = remember(sales) { sales.sumOf { it.netAmount } }
    val totalPurchases = remember(purchases) { purchases.sumOf { it.totalAmount } }
    val totalDiscounts = remember(sales) { sales.sumOf { it.discount } }
    val totalCustomerDue = remember(customers) { customers.sumOf { it.balance } }
    val estimatedProfit = remember(totalRevenue, totalPurchases) { (totalRevenue - totalPurchases).coerceAtLeast(0.0) }

    val cashierBreakdown = remember(sales) {
        val grouped = sales.groupBy { it.cashierName.ifBlank { "Muhammad Umer" } }
        grouped.map { (name, saleList) ->
            CashierPerformanceStat(
                cashierName = name,
                invoiceCount = saleList.size,
                netTotal = saleList.sumOf { it.netAmount },
                collectedTotal = saleList.sumOf { it.paidAmount }
            )
        }.sortedByDescending { it.netTotal }
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Financial & Sales Reports",
                subtitle = "Ledgers & P&L Analytics",
                onBackClick = onNavigateBack
            )
        },
        containerColor = Slate50
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SectionHeader(title = "Profit & Loss Summary", subtitle = "Cumulative business analytics")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    title = "Total Gross Sales",
                    value = "$currency %.0f".format(totalRevenue),
                    icon = Icons.Default.TrendingUp,
                    color = Emerald600,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Total Purchases (COGS)",
                    value = "$currency %.0f".format(totalPurchases),
                    icon = Icons.Default.ShoppingBag,
                    color = Navy800,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    title = "Estimated Gross Profit",
                    value = "$currency %.0f".format(estimatedProfit),
                    icon = Icons.Default.Savings,
                    color = Emerald600,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Customer Receivables",
                    value = "$currency %.0f".format(totalCustomerDue),
                    icon = Icons.Default.AccountBalanceWallet,
                    color = Rose600,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionHeader(title = "Cashier Performance Breakdown", subtitle = "Sales volume & invoice count per cashier")

            if (cashierBreakdown.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Text("No sales records available yet.", color = Slate400, fontSize = 12.sp)
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        cashierBreakdown.forEachIndexed { index, stat ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Navy100
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = Navy800,
                                            modifier = Modifier.padding(6.dp).size(16.dp)
                                        )
                                    }
                                    Column {
                                        Text(stat.cashierName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Navy900)
                                        Text("${stat.invoiceCount} Invoices • Collected: $currency %.0f".format(stat.collectedTotal), fontSize = 11.sp, color = Slate500)
                                    }
                                }

                                Text(
                                    text = "$currency %.2f".format(stat.netTotal),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = Emerald700
                                )
                            }
                            if (index < cashierBreakdown.size - 1) {
                                HorizontalDivider(color = Slate100)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SectionHeader(title = "Tax & Discount Metrics", subtitle = "Breakdown of margins")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Sales Completed:", color = Navy600, fontSize = 13.sp)
                        Text("${sales.size} Invoices", fontWeight = FontWeight.Bold, color = Navy900, fontSize = 13.sp)
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Discounts Awarded:", color = Navy600, fontSize = 13.sp)
                        Text("$currency %.2f".format(totalDiscounts), fontWeight = FontWeight.Bold, color = Rose600, fontSize = 13.sp)
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Inventory Stock Items:", color = Navy600, fontSize = 13.sp)
                        Text("${products.size} Items", fontWeight = FontWeight.Bold, color = Navy900, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
