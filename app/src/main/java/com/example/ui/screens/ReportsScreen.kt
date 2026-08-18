package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.MoneyOff
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.InvoiceReceiptDialog
import com.example.ui.components.KpiCard
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Rose100
import com.example.ui.theme.Rose600
import com.example.ui.viewmodel.StoreViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(viewModel: StoreViewModel) {
    val context = LocalContext.current
    val allSales by viewModel.allSales.collectAsState()
    val allSaleItems by viewModel.allSaleItems.collectAsState()
    val allExpenses by viewModel.allExpenses.collectAsState()
    val lowStockItems by viewModel.lowStockProducts.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val showInvoiceDialog by viewModel.showInvoiceDialog.collectAsState()
    val lastCompletedSale by viewModel.lastCompletedSale.collectAsState()
    val lastCompletedSaleItems by viewModel.lastCompletedSaleItems.collectAsState()

    var activeReportTab by remember { mutableStateOf(0) } // 0: Daily Sales Report, 1: P&L Overview, 2: Invoices, 3: Expenses, 4: Low Stock
    var showAddExpenseModal by remember { mutableStateOf(false) }
    var expenseTitle by remember { mutableStateOf("") }
    var expenseAmount by remember { mutableStateOf("") }
    var expenseCategory by remember { mutableStateOf("General") }
    var expenseNote by remember { mutableStateOf("") }
    var saleToDelete by remember { mutableStateOf<com.example.data.entity.Sale?>(null) }

    // Date selection for Daily Sales Report
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }

    val calSelectedStart = (selectedDate.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val dayStartMs = calSelectedStart.timeInMillis
    val dayEndMs = dayStartMs + (24 * 60 * 60 * 1000) - 1

    val isSelectedToday = remember(dayStartMs) {
        val calNow = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        calNow.timeInMillis == dayStartMs
    }

    val datePickerDialog = remember(selectedDate) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                selectedDate = newCal
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        )
    }

    // Single Day Metrics
    val dailySales = allSales.filter { it.timestamp in dayStartMs..dayEndMs }
    val dailyExpenses = allExpenses.filter { it.timestamp in dayStartMs..dayEndMs }

    val dailyTransactionCount = dailySales.size
    val dailyGrossAmount = dailySales.sumOf { it.totalAmount }
    val dailyDiscountsTotal = dailySales.sumOf { it.discount }
    val dailyNetRevenue = dailySales.sumOf { it.netAmount }
    val dailyPaidTotal = dailySales.sumOf { it.paidAmount }
    val dailyDueTotal = dailySales.sumOf { it.dueAmount }
    val dailyExpensesTotal = dailyExpenses.sumOf { it.amount }

    val dailySaleIds = dailySales.map { it.id }.toSet()
    val dailySaleItemsList = allSaleItems.filter { it.saleId in dailySaleIds }

    val dailyCogs = dailySaleItemsList.sumOf { it.quantity * it.purchasePrice }
    val dailyItemsGrossProfit = dailySaleItemsList.sumOf { it.totalPrice - (it.quantity * it.purchasePrice) }
    val dailyNetSalesProfit = dailyItemsGrossProfit - dailyDiscountsTotal
    val dailyOperatingProfit = dailyNetSalesProfit - dailyExpensesTotal

    val profitMarginPercent = if (dailyNetRevenue > 0) (dailyNetSalesProfit / dailyNetRevenue) * 100 else 0.0
    val avgOrderValue = if (dailyTransactionCount > 0) dailyNetRevenue / dailyTransactionCount else 0.0

    val cashSalesTotal = dailySales.filter { it.paymentType.contains("Cash", ignoreCase = true) }.sumOf { it.netAmount }
    val udhaarSalesTotal = dailySales.filter { it.paymentType.contains("Udhaar", ignoreCase = true) || it.paymentType.contains("Credit", ignoreCase = true) || it.dueAmount > 0 }.sumOf { it.netAmount }
    val cardSalesTotal = dailySales.filter { it.paymentType.contains("Card", ignoreCase = true) || it.paymentType.contains("Online", ignoreCase = true) }.sumOf { it.netAmount }

    // Monthly time periods for P&L Overview tab
    val calMonth = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    val monthMs = calMonth.timeInMillis

    val monthSales = allSales.filter { it.timestamp >= monthMs }
    val monthExpenses = allExpenses.filter { it.timestamp >= monthMs }

    val monthTotalRevenue = monthSales.sumOf { it.netAmount }
    val monthTotalDiscounts = monthSales.sumOf { it.discount }
    val monthTotalExpenses = monthExpenses.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScrollableTabRow(selectedTabIndex = activeReportTab, edgePadding = 0.dp) {
            Tab(selected = activeReportTab == 0, onClick = { activeReportTab = 0 }, text = { Text("Daily Sales") })
            Tab(selected = activeReportTab == 1, onClick = { activeReportTab = 1 }, text = { Text("P&L Overview") })
            Tab(selected = activeReportTab == 2, onClick = { activeReportTab = 2 }, text = { Text("Invoices") })
            Tab(selected = activeReportTab == 3, onClick = { activeReportTab = 3 }, text = { Text("Expenses") })
            Tab(selected = activeReportTab == 4, onClick = { activeReportTab = 4 }, text = { Text("Low Stock") })
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (activeReportTab == 0) {
            // DAILY SALES REPORT VIEW
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Date Selector Navigation
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                val newCal = (selectedDate.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
                                selectedDate = newCal
                            }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Day")
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { datePickerDialog.show() }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Pick Date",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val formattedDate = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()).format(calSelectedStart.time)
                                    Text(
                                        text = if (isSelectedToday) "$formattedDate (Today)" else formattedDate,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Tap to choose date",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!isSelectedToday) {
                                    IconButton(onClick = { selectedDate = Calendar.getInstance() }) {
                                        Icon(Icons.Default.Today, contentDescription = "Jump to Today", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                IconButton(onClick = {
                                    val newCal = (selectedDate.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }
                                    selectedDate = newCal
                                }) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Day")
                                }
                            }
                        }
                    }
                }

                // Export Daily Sales PDF Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "Export Daily Sales PDF",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Save accounting report to device storage",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row {
                                IconButton(onClick = {
                                    val titleStr = "DAILY SALES REPORT - ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(calSelectedStart.time)}"
                                    viewModel.exportSalesReportPdf(context, dailySales, titleStr, action = "open")
                                }) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = {
                                    val titleStr = "DAILY SALES REPORT - ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(calSelectedStart.time)}"
                                    viewModel.exportSalesReportPdf(context, dailySales, titleStr, action = "whatsapp")
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = "Share Report", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = {
                                    val titleStr = "DAILY SALES REPORT - ${SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(calSelectedStart.time)}"
                                    viewModel.exportSalesReportPdf(context, dailySales, titleStr, action = "print")
                                }) {
                                    Icon(Icons.Default.Print, contentDescription = "Print Report", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                // Primary Daily KPI Cards Grid
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiCard(
                                title = "Daily Revenue",
                                value = "${settings.currencySymbol} ${dailyNetRevenue.toInt()}",
                                subtitle = "Gross: ${dailyGrossAmount.toInt()} | Disc: ${dailyDiscountsTotal.toInt()}",
                                icon = Icons.Default.TrendingUp,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )

                            KpiCard(
                                title = "Daily Sales Profit",
                                value = "${settings.currencySymbol} ${dailyNetSalesProfit.toInt()}",
                                subtitle = "${String.format(Locale.US, "%.1f", profitMarginPercent)}% Margin",
                                icon = Icons.Default.MonetizationOn,
                                containerColor = if (dailyNetSalesProfit >= 0) Emerald100 else Rose100,
                                contentColor = if (dailyNetSalesProfit >= 0) Emerald600 else Rose600,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiCard(
                                title = "Transactions",
                                value = "$dailyTransactionCount Sales",
                                subtitle = "Avg: ${settings.currencySymbol} ${avgOrderValue.toInt()} / order",
                                icon = Icons.Default.Receipt,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.weight(1f)
                            )

                            KpiCard(
                                title = "Collected vs Due",
                                value = "${settings.currencySymbol} ${dailyPaidTotal.toInt()}",
                                subtitle = "Udhaar Due: ${settings.currencySymbol} ${dailyDueTotal.toInt()}",
                                icon = Icons.Default.AccountBalanceWallet,
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Daily Profit & Loss Cost Structure
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Assessment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Daily Financial & Profit Analysis", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Gross Product Sales Value:", style = MaterialTheme.typography.bodyMedium)
                                Text("${settings.currencySymbol} ${dailyGrossAmount.toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Cost of Goods Sold (COGS):", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                                Text("- ${settings.currencySymbol} ${dailyCogs.toInt()}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Discounts Given:", style = MaterialTheme.typography.bodyMedium, color = Rose600)
                                Text("- ${settings.currencySymbol} ${dailyDiscountsTotal.toInt()}", style = MaterialTheme.typography.bodyMedium, color = Rose600)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Gross Profit from Sales:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${settings.currencySymbol} ${dailyNetSalesProfit.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = if (dailyNetSalesProfit >= 0) Emerald600 else Rose600)
                            }

                            if (dailyExpensesTotal > 0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Daily Operating Expenses:", style = MaterialTheme.typography.bodyMedium, color = Rose600)
                                    Text("- ${settings.currencySymbol} ${dailyExpensesTotal.toInt()}", style = MaterialTheme.typography.bodyMedium, color = Rose600, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Net Operating Profit:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("${settings.currencySymbol} ${dailyOperatingProfit.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = if (dailyOperatingProfit >= 0) Emerald600 else Rose600)
                                }
                            }
                        }
                    }
                }

                // Payment Mode Breakdown
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("Payment Mode Breakdown", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Cash Sales:", style = MaterialTheme.typography.bodySmall)
                                Text("${settings.currencySymbol} ${cashSalesTotal.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Udhaar / Credit Sales:", style = MaterialTheme.typography.bodySmall, color = Rose600)
                                Text("${settings.currencySymbol} ${udhaarSalesTotal.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Rose600)
                            }

                            if (cardSalesTotal > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Card / Digital Sales:", style = MaterialTheme.typography.bodySmall)
                                    Text("${settings.currencySymbol} ${cardSalesTotal.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Single Day Transactions Section Header
                item {
                    val dateFormattedStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(calSelectedStart.time)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Transactions for $dateFormattedStr ($dailyTransactionCount)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Transactions List for the Day
                if (dailySales.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No sales transactions recorded for this date.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                } else {
                    items(dailySales) { sale ->
                        val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(sale.timestamp))
                        val saleItemsList = allSaleItems.filter { it.saleId == sale.id }
                        val saleProfit = saleItemsList.sumOf { it.totalPrice - (it.quantity * it.purchasePrice) } - sale.discount

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.viewInvoice(sale) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(sale.invoiceNumber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (sale.paymentType.contains("Cash", ignoreCase = true)) Emerald100 else Rose100)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                sale.paymentType,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (sale.paymentType.contains("Cash", ignoreCase = true)) Emerald600 else Rose600,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("${sale.customerName} • $timeStr", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    if (saleItemsList.isNotEmpty()) {
                                        Text("${saleItemsList.sumOf { it.quantity.toInt() }} Items sold", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "${settings.currencySymbol} ${sale.netAmount.toInt()}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "Profit: +${settings.currencySymbol} ${saleProfit.toInt()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (saleProfit >= 0) Emerald600 else Rose600,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else if (activeReportTab == 1) {
            // P&L OVERVIEW & STATS
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text("Monthly Store Performance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "Export Complete Accounting PDF",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "Save all-time sales & P&L report to storage",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row {
                                IconButton(onClick = {
                                    viewModel.exportSalesReportPdf(context, title = "COMPLETE FINANCIAL ACCOUNTING REPORT", action = "open")
                                }) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = {
                                    viewModel.exportSalesReportPdf(context, title = "COMPLETE FINANCIAL ACCOUNTING REPORT", action = "whatsapp")
                                }) {
                                    Icon(Icons.Default.Share, contentDescription = "Share Report", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = {
                                    viewModel.exportSalesReportPdf(context, title = "COMPLETE FINANCIAL ACCOUNTING REPORT", action = "print")
                                }) {
                                    Icon(Icons.Default.Print, contentDescription = "Print Report", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        KpiCard(
                            title = "This Month Sales",
                            value = "${settings.currencySymbol} ${monthTotalRevenue.toInt()}",
                            subtitle = "${monthSales.size} Total transactions",
                            icon = Icons.Default.DateRange,
                            containerColor = Emerald100,
                            contentColor = Emerald600,
                            modifier = Modifier.weight(1f)
                        )

                        KpiCard(
                            title = "Expenses",
                            value = "${settings.currencySymbol} ${monthTotalExpenses.toInt()}",
                            subtitle = "${monthExpenses.size} Ledger records",
                            icon = Icons.Default.MoneyOff,
                            containerColor = Rose100,
                            contentColor = Rose600,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.MonetizationOn, contentDescription = null, tint = Emerald600)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Monthly Sales & Discount Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Gross Monthly Revenue:", style = MaterialTheme.typography.bodyMedium)
                                Text("${settings.currencySymbol} ${(monthTotalRevenue + monthTotalDiscounts).toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Discounts Given:", style = MaterialTheme.typography.bodyMedium, color = Rose600)
                                Text("- ${settings.currencySymbol} ${monthTotalDiscounts.toInt()}", style = MaterialTheme.typography.bodyMedium, color = Rose600)
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Monthly Store Expenses:", style = MaterialTheme.typography.bodyMedium, color = Rose600)
                                Text("- ${settings.currencySymbol} ${monthTotalExpenses.toInt()}", style = MaterialTheme.typography.bodyMedium, color = Rose600, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Net Collected Revenue:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${settings.currencySymbol} ${monthTotalRevenue.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Net Operating Profit:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                val netProfit = monthTotalRevenue - monthTotalExpenses
                                Text(
                                    "${settings.currencySymbol} ${netProfit.toInt()}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (netProfit >= 0) Emerald600 else Rose600
                                )
                            }
                        }
                    }
                }
            }
        } else if (activeReportTab == 2) {
            // ALL INVOICES HISTORY
            if (allSales.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No sales invoices generated yet.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(allSales) { sale ->
                        val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(sale.timestamp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.viewInvoice(sale) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sale.invoiceNumber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("${sale.customerName} • $dateStr", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("${settings.currencySymbol} ${sale.netAmount.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                        Text(sale.paymentType, style = MaterialTheme.typography.labelSmall, color = if (sale.dueAmount > 0) Rose600 else Emerald600)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(onClick = { saleToDelete = sale }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Invoice", tint = Color(0xFFDC2626))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (activeReportTab == 3) {
            // STORE EXPENSES TAB
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Store Expense Ledger", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Recorded Offline in Local SQLite", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                    Button(onClick = { showAddExpenseModal = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Expense")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (allExpenses.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No expenses recorded yet.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allExpenses) { exp ->
                            val dateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(exp.timestamp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(exp.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Text("${exp.category} • $dateStr", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        if (exp.note.isNotBlank()) {
                                            Text(exp.note, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "- ${settings.currencySymbol} ${exp.amount.toInt()}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Rose600
                                        )
                                        IconButton(onClick = { viewModel.deleteExpense(exp) }) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Expense", tint = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // LOW STOCK REORDER REPORT
            if (lowStockItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("All product stock levels are healthy! No low stock alerts.", color = Emerald600, fontWeight = FontWeight.Bold)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Rose100, contentColor = Rose600)
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Warning, contentDescription = null)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("${lowStockItems.size} Items requiring stock reorder", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    items(lowStockItems) { p ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Category: ${p.category} • Min Threshold: ${p.minStockLevel.toInt()} ${p.unit}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }

                                Box(
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Rose100).padding(8.dp)
                                ) {
                                    Text("${p.stockQuantity.toInt()} ${p.unit} Left", color = Rose600, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddExpenseModal) {
        AlertDialog(
            onDismissRequest = { showAddExpenseModal = false },
            title = { Text("Record Store Expense") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = expenseTitle,
                        onValueChange = { expenseTitle = it },
                        label = { Text("Expense Title (e.g. Shop Electricity)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = expenseAmount,
                        onValueChange = { expenseAmount = it },
                        label = { Text("Amount (${settings.currencySymbol})") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = expenseCategory,
                        onValueChange = { expenseCategory = it },
                        label = { Text("Category (Utilities, Rent, Transport, Salary)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = expenseNote,
                        onValueChange = { expenseNote = it },
                        label = { Text("Note / Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = expenseAmount.toDoubleOrNull() ?: 0.0
                        if (expenseTitle.isNotBlank() && amt > 0) {
                            viewModel.addExpense(expenseTitle.trim(), expenseCategory.trim(), amt, expenseNote.trim())
                            showAddExpenseModal = false
                            expenseTitle = ""
                            expenseAmount = ""
                            expenseNote = ""
                        } else {
                            viewModel.showToast("Please enter valid expense title and amount!")
                        }
                    }
                ) {
                    Text("Save Expense")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExpenseModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (saleToDelete != null) {
        AlertDialog(
            onDismissRequest = { saleToDelete = null },
            title = { Text("Delete Invoice", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this invoice?") },
            confirmButton = {
                Button(
                    onClick = {
                        val s = saleToDelete
                        saleToDelete = null
                        if (s != null) {
                            viewModel.deleteInvoice(s)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { saleToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showInvoiceDialog && lastCompletedSale != null) {
        InvoiceReceiptDialog(
            sale = lastCompletedSale!!,
            items = lastCompletedSaleItems,
            settings = settings,
            onDismiss = { viewModel.dismissInvoiceDialog() },
            onDeleteInvoice = { viewModel.deleteInvoice(it) }
        )
    }
}

