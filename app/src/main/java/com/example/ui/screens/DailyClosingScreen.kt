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
import com.example.ui.components.AppHeader
import com.example.ui.components.KpiCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DailyClosingScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val sales by viewModel.sales.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()
    val currency = storeSettings?.currencySymbol ?: "Rs"

    val todaySales = remember(sales) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfDay = calendar.timeInMillis
        sales.filter { it.createdAt >= startOfDay }
    }

    val cashInHand = remember(todaySales) { todaySales.sumOf { it.paidAmount } }
    val todayNet = remember(todaySales) { todaySales.sumOf { it.netAmount } }
    val todayDue = remember(todaySales) { todaySales.sumOf { it.dueAmount } }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Daily Register Closing",
                subtitle = "Shift Summary & Cash Reconciliation",
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
            SectionHeader(title = "Today's Register Totals", subtitle = "Transactions executed today")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    title = "Cash Collected",
                    value = "$currency %.0f".format(cashInHand),
                    icon = Icons.Default.Payments,
                    color = Emerald600,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "Today's Sales",
                    value = "$currency %.0f".format(todayNet),
                    icon = Icons.Default.Receipt,
                    color = Navy900,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            KpiCard(
                title = "Today's Credit (Due)",
                value = "$currency %.0f".format(todayDue),
                icon = Icons.Default.CreditCardOff,
                color = Rose600,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

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
                    Text("Register Reconciliation", fontWeight = FontWeight.Bold, color = Navy900)
                    Text("Total Receipts Today: ${todaySales.size} Invoices", fontSize = 13.sp, color = Navy600)
                    Text("Cash in Drawer: $currency %.2f".format(cashInHand), fontSize = 13.sp, color = Emerald600, fontWeight = FontWeight.SemiBold)

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { /* Close register */ },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("close_register_button")
                    ) {
                        Icon(Icons.Default.LockClock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print & Finalize Daily Shift Closing")
                    }
                }
            }
        }
    }
}
