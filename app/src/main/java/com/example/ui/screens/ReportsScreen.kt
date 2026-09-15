package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import com.example.ui.components.AppHeader
import com.example.ui.components.KpiCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class ReportPeriod {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    ALL_TIME
}

data class CashierPerformanceStat(
    val cashierName: String,
    val invoiceCount: Int,
    val netTotal: Double,
    val collectedTotal: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val sales by viewModel.sales.collectAsState()
    val purchases by viewModel.purchases.collectAsState()
    val products by viewModel.products.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val cashMovements by viewModel.allCashMovements.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()

    val currency = storeSettings?.currencySymbol ?: "Rs"
    var selectedPeriod by remember { mutableStateOf(ReportPeriod.TODAY) }

    val periodRange = remember(selectedPeriod) {
        val cal = Calendar.getInstance()

        when (selectedPeriod) {
            ReportPeriod.TODAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            ReportPeriod.THIS_WEEK -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_WEEK, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            ReportPeriod.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                val end = cal.timeInMillis
                Pair(start, end)
            }
            ReportPeriod.ALL_TIME -> Pair(0L, Long.MAX_VALUE)
        }
    }

    val (startTime, endTime) = periodRange

    val filteredSales = remember(sales, startTime, endTime) {
        sales.filter { it.createdAt in startTime..endTime }
    }
    val filteredPurchases = remember(purchases, startTime, endTime) {
        purchases.filter { it.createdAt in startTime..endTime }
    }
    val filteredCashOut = remember(cashMovements, startTime, endTime) {
        cashMovements.filter { it.type == "CASH_OUT" && it.timestamp in startTime..endTime }
    }

    val totalRevenue = remember(filteredSales) { filteredSales.sumOf { it.netAmount } }
    val totalPurchases = remember(filteredPurchases) { filteredPurchases.sumOf { it.totalAmount } }
    val totalOperatingExpenses = remember(filteredCashOut) { filteredCashOut.sumOf { it.amount } }
    val totalDiscounts = remember(filteredSales) { filteredSales.sumOf { it.discount } }
    val totalCustomerDue = remember(customers) { customers.sumOf { it.balance } }

    // Accurate Net Profit = Revenue - Purchases (COGS) - Operating Expenses
    val netProfit = remember(totalRevenue, totalPurchases, totalOperatingExpenses) {
        totalRevenue - totalPurchases - totalOperatingExpenses
    }
    val profitMargin = remember(totalRevenue, netProfit) {
        if (totalRevenue > 0) (netProfit / totalRevenue) * 100 else 0.0
    }

    // Inventory Valuation
    val totalInventoryCost = remember(products) { products.sumOf { it.stockQuantity * it.purchasePrice } }
    val totalInventoryRetail = remember(products) { products.sumOf { it.stockQuantity * it.salePrice } }
    val potentialInventoryProfit = remember(totalInventoryCost, totalInventoryRetail) { (totalInventoryRetail - totalInventoryCost).coerceAtLeast(0.0) }

    val cashierBreakdown = remember(filteredSales) {
        val grouped = filteredSales.groupBy { it.cashierName.ifBlank { "Muhammad Umer" } }
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
                title = "Financial & Performance Reports",
                subtitle = "Comprehensive P&L, Ledgers & Operating Analytics",
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
            // Period Filter Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedPeriod == ReportPeriod.TODAY,
                    onClick = { selectedPeriod = ReportPeriod.TODAY },
                    label = { Text("Daily (Today)") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedPeriod == ReportPeriod.THIS_WEEK,
                    onClick = { selectedPeriod = ReportPeriod.THIS_WEEK },
                    label = { Text("This Week") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedPeriod == ReportPeriod.THIS_MONTH,
                    onClick = { selectedPeriod = ReportPeriod.THIS_MONTH },
                    label = { Text("Monthly") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedPeriod == ReportPeriod.ALL_TIME,
                    onClick = { selectedPeriod = ReportPeriod.ALL_TIME },
                    label = { Text("All Time") },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            SectionHeader(
                title = "Profit & Loss Summary",
                subtitle = "Revenue, COGS, Operating Expenses & True Net Margin"
            )

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
                    title = "Purchases (COGS)",
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
                    title = "Operating Expenses",
                    value = "$currency %.0f".format(totalOperatingExpenses),
                    icon = Icons.Default.ReceiptLong,
                    color = Rose600,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Accurate Net Profit",
                    value = "$currency %.0f".format(netProfit),
                    icon = Icons.Default.Savings,
                    color = if (netProfit >= 0) Emerald600 else Rose600,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Profit Margin & Customer Due Banner
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (netProfit >= 0) Emerald50 else Rose50,
                border = BorderStroke(1.dp, if (netProfit >= 0) Emerald200 else Rose200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Net Profit Margin: %.1f%%".format(profitMargin),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (netProfit >= 0) Emerald800 else Rose800
                        )
                        Text(
                            text = "Customer Receivables: $currency %.0f".format(totalCustomerDue),
                            fontSize = 11.sp,
                            color = Slate600
                        )
                    }
                    StatusBadge(
                        text = if (netProfit >= 0) "PROFITABLE" else "LOSS DEFICIT",
                        backgroundColor = if (netProfit >= 0) Emerald100 else Rose100,
                        textColor = if (netProfit >= 0) Emerald700 else Rose700
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Inventory Valuation Card
            SectionHeader(
                title = "Inventory Valuation & Assets",
                subtitle = "Total current warehouse capital valuation"
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Stock Valuation at Cost (CP):", fontSize = 13.sp, color = Navy700)
                        Text("$currency %.2f".format(totalInventoryCost), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Navy900)
                    }
                    HorizontalDivider(color = Slate100)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Stock Valuation at Retail (SP):", fontSize = 13.sp, color = Navy700)
                        Text("$currency %.2f".format(totalInventoryRetail), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Emerald700)
                    }
                    HorizontalDivider(color = Slate100)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Potential Unrealized Stock Profit:", fontSize = 13.sp, color = Navy700)
                        Text("$currency %.2f".format(potentialInventoryProfit), fontWeight = FontWeight.Black, fontSize = 13.sp, color = Emerald600)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Recent Cash Expenses Breakdown
            SectionHeader(
                title = "Operating Expense Ledger",
                subtitle = "Petty cash, utility bills, salaries & vendor payments"
            )

            if (filteredCashOut.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Text("No operating expenses recorded for selected period.", color = Slate400, fontSize = 12.sp)
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
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredCashOut.take(10).forEachIndexed { index, exp ->
                            val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(exp.reason.ifBlank { "Operating Expense" }, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Navy900)
                                    Text("${exp.cashierName} • ${dateFormat.format(Date(exp.timestamp))}", fontSize = 11.sp, color = Slate500)
                                }
                                Text("- $currency %.2f".format(exp.amount), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Rose600)
                            }
                            if (index < filteredCashOut.take(10).size - 1) {
                                HorizontalDivider(color = Slate100)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Cashier Performance
            SectionHeader(title = "Cashier Performance Breakdown", subtitle = "Sales volume & invoice count per cashier")

            if (cashierBreakdown.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Text("No sales records available for selected period.", color = Slate400, fontSize = 12.sp)
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

            SectionHeader(title = "Discounts & Invoice Activity", subtitle = "Breakdown of volume & allowances")

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
                        Text("Sales Invoices Processed:", color = Navy600, fontSize = 13.sp)
                        Text("${filteredSales.size} Invoices", fontWeight = FontWeight.Bold, color = Navy900, fontSize = 13.sp)
                    }
                    HorizontalDivider(color = Slate100)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Discounts Conceded:", color = Navy600, fontSize = 13.sp)
                        Text("$currency %.2f".format(totalDiscounts), fontWeight = FontWeight.Bold, color = Rose600, fontSize = 13.sp)
                    }
                    HorizontalDivider(color = Slate100)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Active Inventory SKUs:", color = Navy600, fontSize = 13.sp)
                        Text("${products.size} Products", fontWeight = FontWeight.Bold, color = Navy900, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
