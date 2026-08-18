package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.DailyClosing
import com.example.ui.viewmodel.StoreViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DailyClosingScreen(viewModel: StoreViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val allSales by viewModel.allSales.collectAsState()
    val allExpenses by viewModel.allExpenses.collectAsState()
    val allPurchases by viewModel.allPurchases.collectAsState()
    val allDailyClosings by viewModel.allDailyClosings.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }

    // Form inputs
    var openingCashInput by remember { mutableStateOf("0") }
    var closingCashInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    val todayStart = cal.timeInMillis

    val todaySales = allSales.filter { it.timestamp >= todayStart }
    val totalSalesAmt = todaySales.sumOf { it.netAmount }
    val totalCashSales = todaySales.filter { it.paymentType.contains("Cash", ignoreCase = true) }.sumOf { it.paidAmount }
    val totalCreditSales = todaySales.sumOf { it.dueAmount }

    val todayExpensesAmt = allExpenses.filter { it.timestamp >= todayStart }.sumOf { it.amount }
    val todayPurchasesAmt = allPurchases.filter { it.timestamp >= todayStart }.sumOf { it.paidAmount }

    val openingCash = openingCashInput.toDoubleOrNull() ?: 0.0
    val closingCash = closingCashInput.toDoubleOrNull() ?: 0.0

    val expectedCash = openingCash + totalCashSales - todayExpensesAmt - todayPurchasesAmt
    val difference = closingCash - expectedCash

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TabRow(selectedTabIndex = selectedTabIndex) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Submit Daily Closing", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.Payments, contentDescription = null) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Closing History (${allDailyClosings.size})", fontWeight = FontWeight.Bold) },
                icon = { Icon(Icons.Default.History, contentDescription = null) }
            )
        }

        if (selectedTabIndex == 0) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2537)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Column {
                                    Text(
                                        text = "Staff: ${currentUser?.name ?: settings.ownerName}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Role: ${currentUser?.role ?: "SUPER_ADMIN"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Today's Register Totals",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Total Sales", fontSize = 11.sp, color = Color(0xFF166534))
                                Text(
                                    "${settings.currencySymbol} ${totalSalesAmt.toInt()}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF15803D)
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFDBEAFE))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Cash Received", fontSize = 11.sp, color = Color(0xFF1E40AF))
                                Text(
                                    "${settings.currencySymbol} ${totalCashSales.toInt()}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = Color(0xFF1D4ED8)
                                )
                            }
                        }

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Udhaar / Credit", fontSize = 11.sp, color = Color(0xFF92400E))
                                Text(
                                    "${settings.currencySymbol} ${totalCreditSales.toInt()}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = Color(0xFFB45309)
                                )
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Register Cash Count", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                            OutlinedTextField(
                                value = openingCashInput,
                                onValueChange = { openingCashInput = it },
                                label = { Text("Shift Opening Cash (${settings.currencySymbol})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = closingCashInput,
                                onValueChange = { closingCashInput = it },
                                label = { Text("Actual Cash In Drawer (${settings.currencySymbol})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = notesInput,
                                onValueChange = { notesInput = it },
                                label = { Text("Notes / Observations (Optional)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = false
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Expected Cash Balance", fontSize = 12.sp, color = Color.Gray)
                                    Text(
                                        "${settings.currencySymbol} ${expectedCash.toInt()}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Difference / Status", fontSize = 12.sp, color = Color.Gray)
                                    val statusColor = when {
                                        difference == 0.0 -> Color(0xFF16A34A)
                                        difference > 0 -> Color(0xFF0284C7)
                                        else -> Color(0xFFDC2626)
                                    }
                                    val diffText = if (difference >= 0) "+${difference.toInt()}" else "${difference.toInt()}"
                                    Text(
                                        "${settings.currencySymbol} $diffText",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = statusColor
                                    )
                                }
                            }

                            if (difference != 0.0 && closingCashInput.isNotBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (difference < 0) Color(0xFFFEE2E2) else Color(0xFFE0F2FE))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = if (difference < 0) Color(0xFFDC2626) else Color(0xFF0284C7),
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text(
                                        text = if (difference < 0) "Cash Shortage of ${settings.currencySymbol} ${Math.abs(difference).toInt()}" else "Cash Surplus of ${settings.currencySymbol} ${difference.toInt()}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (difference < 0) Color(0xFF991B1B) else Color(0xFF075985)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    viewModel.submitDailyClosing(
                                        openingCash = openingCash,
                                        closingCash = closingCash,
                                        notes = notesInput,
                                        onComplete = { success, msg ->
                                            viewModel.showToast(msg)
                                            if (success) {
                                                closingCashInput = ""
                                                notesInput = ""
                                            }
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2537)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Submit Shift Closing Record", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        } else {
            if (allDailyClosings.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No daily shift closing records found.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(allDailyClosings.sortedByDescending { it.closeTimestamp }) { item ->
                        DailyClosingCardItem(item = item, currencySymbol = settings.currencySymbol)
                    }
                }
            }
        }
    }
}

@Composable
fun DailyClosingCardItem(item: DailyClosing, currencySymbol: String) {
    val df = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val dateStr = df.format(Date(item.closeTimestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = item.userName,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Role: ${item.userRole} | $dateStr",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                val diffColor = when {
                    item.differenceAmount == 0.0 -> Color(0xFF16A34A)
                    item.differenceAmount > 0 -> Color(0xFF0284C7)
                    else -> Color(0xFFDC2626)
                }
                val diffLabel = if (item.differenceAmount >= 0) "+${item.differenceAmount.toInt()}" else "${item.differenceAmount.toInt()}"

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(diffColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Diff: $currencySymbol $diffLabel",
                        fontWeight = FontWeight.Bold,
                        color = diffColor,
                        fontSize = 12.sp
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Sales: $currencySymbol ${item.totalSales.toInt()}", fontSize = 12.sp)
                    Text("Cash Sales: $currencySymbol ${item.cashSales.toInt()}", fontSize = 12.sp)
                    Text("Udhaar Sales: $currencySymbol ${item.creditSales.toInt()}", fontSize = 12.sp)
                }
                Column {
                    Text("Opening Cash: $currencySymbol ${item.openingCash.toInt()}", fontSize = 12.sp)
                    Text("Closing Cash: $currencySymbol ${item.closingCash.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Expected: $currencySymbol ${item.expectedCash.toInt()}", fontSize = 12.sp, color = Color.Gray)
                }
            }

            if (item.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Notes: ${item.notes}", fontSize = 11.sp, color = Color.DarkGray)
            }
        }
    }
}
