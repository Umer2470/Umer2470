package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.entity.CashMovement
import com.example.data.entity.RegisterShift
import com.example.data.entity.Sale
import com.example.ui.components.AppHeader
import com.example.ui.components.KpiCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyClosingScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val activeShift by viewModel.activeShift.collectAsState()
    val allShifts by viewModel.allShifts.collectAsState()
    val allMovements by viewModel.allCashMovements.collectAsState()
    val sales by viewModel.sales.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()

    val currency = storeSettings?.currencySymbol ?: "Rs"

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Active Shift / Closing, 1 = History
    var showOpenShiftDialog by remember { mutableStateOf(false) }
    var showCashMovementDialog by remember { mutableStateOf<String?>(null) } // "CASH_IN" or "CASH_OUT"
    var showCloseConfirmDialog by remember { mutableStateOf(false) }
    var showReopenDialog by remember { mutableStateOf<RegisterShift?>(null) }
    var showReportSlipDialog by remember { mutableStateOf<RegisterShift?>(null) }

    // Denomination states for current counting
    var d5000 by remember { mutableStateOf("0") }
    var d1000 by remember { mutableStateOf("0") }
    var d500 by remember { mutableStateOf("0") }
    var d100 by remember { mutableStateOf("0") }
    var d50 by remember { mutableStateOf("0") }
    var d20 by remember { mutableStateOf("0") }
    var d10 by remember { mutableStateOf("0") }
    var dCoins by remember { mutableStateOf("0") }

    val denominationTotal = remember(d5000, d1000, d500, d100, d50, d20, d10, dCoins) {
        val n5000 = (d5000.toIntOrNull() ?: 0) * 5000.0
        val n1000 = (d1000.toIntOrNull() ?: 0) * 1000.0
        val n500 = (d500.toIntOrNull() ?: 0) * 500.0
        val n100 = (d100.toIntOrNull() ?: 0) * 100.0
        val n50 = (d50.toIntOrNull() ?: 0) * 50.0
        val n20 = (d20.toIntOrNull() ?: 0) * 20.0
        val n10 = (d10.toIntOrNull() ?: 0) * 10.0
        val coins = dCoins.toDoubleOrNull() ?: 0.0
        n5000 + n1000 + n500 + n100 + n50 + n20 + n10 + coins
    }

    var manualActualCash by remember { mutableStateOf("") }
    var useManualActualCash by remember { mutableStateOf(false) }

    val finalActualCash = if (useManualActualCash) {
        manualActualCash.toDoubleOrNull() ?: 0.0
    } else {
        denominationTotal
    }

    // Active shift calculations
    val currentShiftSales = remember(sales, activeShift) {
        if (activeShift == null) emptyList()
        else sales.filter { it.createdAt >= activeShift!!.openedAt && !it.isDeleted }
    }

    val cashSales = remember(currentShiftSales) {
        currentShiftSales.filter { it.paymentType.equals("Cash", ignoreCase = true) }.sumOf { it.paidAmount }
    }
    val creditSales = remember(currentShiftSales) {
        currentShiftSales.sumOf { it.dueAmount }
    }
    val digitalSales = remember(currentShiftSales) {
        currentShiftSales.filter { !it.paymentType.equals("Cash", ignoreCase = true) && !it.paymentType.equals("Credit", ignoreCase = true) }
            .sumOf { it.paidAmount }
    }
    val totalNetSales = remember(currentShiftSales) { currentShiftSales.sumOf { it.netAmount } }
    val totalDiscounts = remember(currentShiftSales) { currentShiftSales.sumOf { it.discount } }
    val totalGrossSales = remember(currentShiftSales) { currentShiftSales.sumOf { it.totalAmount } }

    val currentShiftMovements = remember(allMovements, activeShift) {
        if (activeShift == null) emptyList()
        else allMovements.filter { it.shiftId == activeShift!!.id }
    }

    val totalCashIn = remember(currentShiftMovements) {
        currentShiftMovements.filter { it.type == "CASH_IN" }.sumOf { it.amount }
    }
    val totalCashOut = remember(currentShiftMovements) {
        currentShiftMovements.filter { it.type == "CASH_OUT" }.sumOf { it.amount }
    }

    val openingCash = activeShift?.openingCash ?: 0.0
    val expectedCash = (openingCash + cashSales + totalCashIn - totalCashOut).coerceAtLeast(0.0)
    val discrepancy = finalActualCash - expectedCash

    Scaffold(
        topBar = {
            AppHeader(
                title = "Daily Register Closing",
                subtitle = "Shift Settlement & Drawer Reconciliation",
                onBackClick = onNavigateBack
            )
        },
        containerColor = Slate50
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Top Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Navy900
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                if (activeShift != null) Icons.Default.LockClock else Icons.Default.LockOpen,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                if (activeShift != null) "Active Register (Open)" else "Register Closing",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_active_closing")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(
                                "Closing History (${allShifts.size})",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    modifier = Modifier.testTag("tab_closing_history")
                )
            }

            if (selectedTab == 0) {
                // ACTIVE SHIFT VIEW
                if (activeShift == null) {
                    // No shift is open: prompt to open register
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 500.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(Amber100, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.PointOfSale,
                                        contentDescription = null,
                                        tint = Amber600,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }

                                Text(
                                    text = "Register is Currently Closed",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Navy900
                                )

                                Text(
                                    text = "Start a new business shift and count your initial drawer float (Opening Cash) to begin recording sales.",
                                    fontSize = 13.sp,
                                    color = Navy600,
                                    textAlign = TextAlign.Center
                                )

                                Button(
                                    onClick = { showOpenShiftDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("open_register_shift_btn")
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Open Shift / Start Register")
                                }
                            }
                        }
                    }
                } else {
                    // Active shift open
                    val shift = activeShift!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Shift Active Header Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Navy900)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "SHIFT ACTIVE #${shift.shiftNumber}",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                        Text(
                                            text = "Cashier: ${shift.cashierName} • Started: ${SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(shift.openedAt))}",
                                            color = Slate300,
                                            fontSize = 12.sp
                                        )
                                    }
                                    StatusBadge(
                                        text = "LIVE REGISTER",
                                        backgroundColor = Emerald100,
                                        textColor = Emerald700
                                    )
                                }

                                if (shift.openingNotes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Notes: ${shift.openingNotes}",
                                        color = Slate200,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // Cash Reconciliation Formula Breakdown
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Drawer Cash Formula",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Navy900
                                    )
                                    Text(
                                        "Opening + Cash Sales + In - Out",
                                        fontSize = 11.sp,
                                        color = Navy500
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FlowStatItem(
                                        label = "Opening Cash",
                                        amount = "+$currency %.0f".format(openingCash),
                                        color = Slate700,
                                        modifier = Modifier.weight(1f)
                                    )
                                    FlowStatItem(
                                        label = "Cash Sales",
                                        amount = "+$currency %.0f".format(cashSales),
                                        color = Emerald600,
                                        modifier = Modifier.weight(1f)
                                    )
                                    FlowStatItem(
                                        label = "Cash In",
                                        amount = "+$currency %.0f".format(totalCashIn),
                                        color = Blue600,
                                        modifier = Modifier.weight(1f)
                                    )
                                    FlowStatItem(
                                        label = "Cash Out",
                                        amount = "-$currency %.0f".format(totalCashOut),
                                        color = Rose600,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 12.dp),
                                    color = Slate200
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "Expected Drawer Cash",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Navy900
                                        )
                                        Text(
                                            "Theoretical cash in till right now",
                                            fontSize = 11.sp,
                                            color = Navy500
                                        )
                                    }
                                    Text(
                                        "$currency %.2f".format(expectedCash),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = Navy900
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Notice regarding Credit Sales
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Amber100.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "ℹ Note: Credit Sales ($currency %.0f) are non-cash receivables and excluded from cash in drawer.".format(creditSales),
                                        fontSize = 11.sp,
                                        color = Amber700
                                    )
                                }
                            }
                        }

                        // Quick Cash Movements Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showCashMovementDialog = "CASH_IN" },
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_cash_in")
                            ) {
                                Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cash In (Add)", fontSize = 13.sp)
                            }

                            Button(
                                onClick = { showCashMovementDialog = "CASH_OUT" },
                                colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_cash_out")
                            ) {
                                Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cash Out (Expense)", fontSize = 13.sp)
                            }
                        }

                        // Cash Denominations Counter Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
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
                                            "Physical Cash Denominations",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Navy900
                                        )
                                        Text(
                                            "Count drawer bills & coins",
                                            fontSize = 11.sp,
                                            color = Navy500
                                        )
                                    }
                                    Text(
                                        "Counted: $currency %.0f".format(denominationTotal),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Emerald600
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Denominations Grid
                                DenominationRow(label = "Rs. 5,000", multiplier = 5000, value = d5000, onValueChange = { d5000 = it })
                                DenominationRow(label = "Rs. 1,000", multiplier = 1000, value = d1000, onValueChange = { d1000 = it })
                                DenominationRow(label = "Rs. 500", multiplier = 500, value = d500, onValueChange = { d500 = it })
                                DenominationRow(label = "Rs. 100", multiplier = 100, value = d100, onValueChange = { d100 = it })
                                DenominationRow(label = "Rs. 50", multiplier = 50, value = d50, onValueChange = { d50 = it })
                                DenominationRow(label = "Rs. 20", multiplier = 20, value = d20, onValueChange = { d20 = it })
                                DenominationRow(label = "Rs. 10", multiplier = 10, value = d10, onValueChange = { d10 = it })

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Coins & Small Change:", fontSize = 13.sp, color = Navy700)
                                    OutlinedTextField(
                                        value = dCoins,
                                        onValueChange = { dCoins = it },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.width(110.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            unfocusedBorderColor = Slate300,
                                            focusedBorderColor = Navy900
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = useManualActualCash,
                                        onCheckedChange = { useManualActualCash = it },
                                        modifier = Modifier.testTag("check_manual_actual_cash")
                                    )
                                    Text(
                                        "Override with manual lump-sum count",
                                        fontSize = 12.sp,
                                        color = Navy700
                                    )
                                }

                                if (useManualActualCash) {
                                    OutlinedTextField(
                                        value = manualActualCash,
                                        onValueChange = { manualActualCash = it },
                                        label = { Text("Manual Actual Cash Amount ($currency)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        // Discrepancy & Variance Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    discrepancy == 0.0 -> Emerald100.copy(alpha = 0.4f)
                                    discrepancy > 0.0 -> Blue100.copy(alpha = 0.4f)
                                    else -> Rose100.copy(alpha = 0.4f)
                                }
                            ),
                            border = BorderStroke(
                                1.dp,
                                when {
                                    discrepancy == 0.0 -> Emerald600
                                    discrepancy > 0.0 -> Blue600
                                    else -> Rose600
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = when {
                                                discrepancy == 0.0 -> "✓ Register Perfectly Balanced"
                                                discrepancy > 0.0 -> "▲ Cash Surplus (Over)"
                                                else -> "▼ Cash Shortage (Short)"
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = when {
                                                discrepancy == 0.0 -> Emerald700
                                                discrepancy > 0.0 -> Blue700
                                                else -> Rose700
                                            }
                                        )
                                        Text(
                                            text = "Actual ($currency %.2f) - Expected ($currency %.2f)".format(finalActualCash, expectedCash),
                                            fontSize = 11.sp,
                                            color = Navy600
                                        )
                                    }
                                    Text(
                                        text = (if (discrepancy > 0) "+$currency " else if (discrepancy < 0) "-$currency " else "$currency ") +
                                                "%.2f".format(Math.abs(discrepancy)),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = when {
                                            discrepancy == 0.0 -> Emerald700
                                            discrepancy > 0.0 -> Blue700
                                            else -> Rose700
                                        }
                                    )
                                }
                            }
                        }

                        // Shift Performance Summary
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Shift Sales & Units Performance",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Navy900
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SummaryMiniBadge("Invoices", "${currentShiftSales.size}", Navy900, Modifier.weight(1f))
                                    SummaryMiniBadge("Gross Sales", "$currency %.0f".format(totalGrossSales), Navy900, Modifier.weight(1f))
                                    SummaryMiniBadge("Discounts", "$currency %.0f".format(totalDiscounts), Amber700, Modifier.weight(1f))
                                    SummaryMiniBadge("Net Sales", "$currency %.0f".format(totalNetSales), Emerald600, Modifier.weight(1f))
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    "Payment Method Breakdown",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = Navy600
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Cash Received:", fontSize = 12.sp, color = Navy700)
                                    Text("$currency %.2f".format(cashSales), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Emerald600)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Digital / Bank / Raast / Cards:", fontSize = 12.sp, color = Navy700)
                                    Text("$currency %.2f".format(digitalSales), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Blue600)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Credit / Udhaar (Unpaid):", fontSize = 12.sp, color = Navy700)
                                    Text("$currency %.2f".format(creditSales), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Rose600)
                                }
                            }
                        }

                        // Finalize and Close Shift Button
                        Button(
                            onClick = { showCloseConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("finalize_close_shift_button")
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Finalize & Close Day Shift", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            } else {
                // HISTORY TAB
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (allShifts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No closed shifts recorded yet.", color = Navy500)
                            }
                        }
                    } else {
                        items(allShifts, key = { it.id }) { s ->
                            ShiftHistoryCard(
                                shift = s,
                                currency = currency,
                                onPrintClick = { showReportSlipDialog = s },
                                onReopenClick = { showReopenDialog = s }
                            )
                        }
                    }
                }
            }
        }
    }

    // DIALOG: OPEN SHIFT
    if (showOpenShiftDialog) {
        var opCashier by remember { mutableStateOf(activeUser?.fullName ?: storeSettings?.defaultCashierName ?: "Muhammad Umer") }
        var opAmount by remember { mutableStateOf("0") }
        var opNotes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showOpenShiftDialog = false },
            title = { Text("Open Register Shift", fontWeight = FontWeight.Bold, color = Navy900) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = opCashier,
                        onValueChange = { opCashier = it },
                        label = { Text("Cashier Name") },
                        modifier = Modifier.fillMaxWidth().testTag("open_cashier_input")
                    )
                    OutlinedTextField(
                        value = opAmount,
                        onValueChange = { opAmount = it },
                        label = { Text("Initial Drawer Cash Float ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("open_cash_input")
                    )
                    OutlinedTextField(
                        value = opNotes,
                        onValueChange = { opNotes = it },
                        label = { Text("Opening Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = opAmount.toDoubleOrNull() ?: 0.0
                        viewModel.openShift(
                            openingCash = amount,
                            cashierName = opCashier,
                            openingNotes = opNotes
                        ) {
                            showOpenShiftDialog = false
                            Toast.makeText(context, "Shift opened successfully!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    modifier = Modifier.testTag("confirm_open_shift_btn")
                ) {
                    Text("Start Shift")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOpenShiftDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // DIALOG: CASH MOVEMENT (IN / OUT)
    if (showCashMovementDialog != null) {
        val type = showCashMovementDialog!!
        var mvAmount by remember { mutableStateOf("") }
        var mvReason by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCashMovementDialog = null },
            title = {
                Text(
                    if (type == "CASH_IN") "Record Cash In (Deposit / Collection)" else "Record Cash Out (Petty Expense / Withdrawal)",
                    fontWeight = FontWeight.Bold,
                    color = if (type == "CASH_IN") Emerald600 else Rose600
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = mvAmount,
                        onValueChange = { mvAmount = it },
                        label = { Text("Amount ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("cash_movement_amount_input")
                    )
                    OutlinedTextField(
                        value = mvReason,
                        onValueChange = { mvReason = it },
                        label = { Text(if (type == "CASH_IN") "Reason (e.g. Customer collection, Add float)" else "Reason (e.g. Tea, Rent, Vendor payment, Owner withdrawal)") },
                        modifier = Modifier.fillMaxWidth().testTag("cash_movement_reason_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = mvAmount.toDoubleOrNull() ?: 0.0
                        if (amt <= 0.0) {
                            Toast.makeText(context, "Please enter a valid amount.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (mvReason.isBlank()) {
                            Toast.makeText(context, "Please enter a reason.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.recordCashMovement(
                            type = type,
                            amount = amt,
                            reason = mvReason,
                            cashierName = activeShift?.cashierName ?: ""
                        ) {
                            showCashMovementDialog = null
                            Toast.makeText(context, "Cash movement recorded.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == "CASH_IN") Emerald600 else Rose600
                    ),
                    modifier = Modifier.testTag("confirm_cash_movement_btn")
                ) {
                    Text("Save Movement")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCashMovementDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // DIALOG: FINALIZE & CLOSE SHIFT CONFIRMATION
    if (showCloseConfirmDialog) {
        var clNotes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCloseConfirmDialog = false },
            title = {
                Text("Finalize & Settle Register Closing", fontWeight = FontWeight.Bold, color = Navy900)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Expected Cash in Drawer: $currency %.2f".format(expectedCash), fontSize = 13.sp, color = Navy700)
                    Text("Physical Cash Counted: $currency %.2f".format(finalActualCash), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Emerald600)
                    Text(
                        "Variance (Discrepancy): $currency %.2f".format(discrepancy),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (discrepancy == 0.0) Emerald600 else if (discrepancy > 0) Blue600 else Rose600
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Closing this shift locks all financial movements for the day. Once closed, admin authorization is required to reopen.",
                        fontSize = 11.sp,
                        color = Navy500
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = clNotes,
                        onValueChange = { clNotes = it },
                        label = { Text("Closing Notes / Remarks") },
                        modifier = Modifier.fillMaxWidth().testTag("closing_notes_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.closeShift(
                            actualCash = finalActualCash,
                            closingNotes = clNotes,
                            den5000 = d5000.toIntOrNull() ?: 0,
                            den1000 = d1000.toIntOrNull() ?: 0,
                            den500 = d500.toIntOrNull() ?: 0,
                            den100 = d100.toIntOrNull() ?: 0,
                            den50 = d50.toIntOrNull() ?: 0,
                            den20 = d20.toIntOrNull() ?: 0,
                            den10 = d10.toIntOrNull() ?: 0,
                            denCoins = dCoins.toDoubleOrNull() ?: 0.0,
                            closedBy = activeUser?.fullName ?: activeShift?.cashierName ?: "Cashier",
                            onComplete = { closedShift ->
                                showCloseConfirmDialog = false
                                showReportSlipDialog = closedShift
                                Toast.makeText(context, "Shift closed successfully!", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    modifier = Modifier.testTag("confirm_finalize_close_btn")
                ) {
                    Text("Confirm & Close")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // DIALOG: REOPEN CLOSED SHIFT (ADMIN/OWNER ONLY)
    if (showReopenDialog != null) {
        val s = showReopenDialog!!
        var roPin by remember { mutableStateOf("") }
        var roReason by remember { mutableStateOf("") }
        var roError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showReopenDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Rose600)
                    Text("Re-Open Closed Shift", fontWeight = FontWeight.Bold, color = Navy900)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Reopening shift #${s.shiftNumber} unlocks financial records for this register session. This action is permanently logged to the system audit trail.",
                        fontSize = 12.sp,
                        color = Navy600
                    )
                    OutlinedTextField(
                        value = roPin,
                        onValueChange = {
                            roPin = it
                            roError = null
                        },
                        label = { Text("Admin / Owner PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("reopen_pin_input")
                    )
                    OutlinedTextField(
                        value = roReason,
                        onValueChange = {
                            roReason = it
                            roError = null
                        },
                        label = { Text("Reason for Reopening") },
                        modifier = Modifier.fillMaxWidth().testTag("reopen_reason_input")
                    )
                    if (roError != null) {
                        Text(roError!!, color = Rose600, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (roPin.isBlank()) {
                            roError = "PIN is required."
                            return@Button
                        }
                        if (roReason.isBlank()) {
                            roError = "Reason is required for audit trail."
                            return@Button
                        }
                        viewModel.reopenShift(
                            shiftId = s.id,
                            credentialPin = roPin,
                            reason = roReason
                        ) { success, msg ->
                            if (success) {
                                showReopenDialog = null
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            } else {
                                roError = msg
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                    modifier = Modifier.testTag("confirm_reopen_shift_btn")
                ) {
                    Text("Authorize & Reopen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReopenDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // DIALOG: PRINT / VIEW CLOSING SLIP
    if (showReportSlipDialog != null) {
        val s = showReportSlipDialog!!
        val slipText = remember(s) {
            buildClosingSlipText(s, storeSettings?.storeName ?: "SENTRY STORE", currency)
        }

        Dialog(onDismissRequest = { showReportSlipDialog = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Daily Shift Closing Report",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Navy900
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp)
                            .background(Slate100, RoundedCornerShape(8.dp))
                            .border(1.dp, Slate300, RoundedCornerShape(8.dp))
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Text(
                            text = slipText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Navy900
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, slipText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Daily Closing Slip"))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share")
                        }

                        Button(
                            onClick = {
                                Toast.makeText(context, "Printing Closing Slip...", Toast.LENGTH_SHORT).show()
                                showReportSlipDialog = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Print")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DenominationRow(
    label: String,
    multiplier: Int,
    value: String,
    onValueChange: (String) -> Unit
) {
    val count = value.toIntOrNull() ?: 0
    val subtotal = count * multiplier

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Navy900)
            Text("Subtotal: Rs $subtotal", fontSize = 11.sp, color = Navy500)
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = { if (count > 0) onValueChange((count - 1).toString()) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Decrease", tint = Navy700)
            }

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.width(64.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Slate300,
                    focusedBorderColor = Navy900
                )
            )

            IconButton(
                onClick = { onValueChange((count + 1).toString()) },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.AddCircleOutline, contentDescription = "Increase", tint = Navy700)
            }
        }
    }
}

@Composable
private fun FlowStatItem(
    label: String,
    amount: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Slate100, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column {
            Text(label, fontSize = 10.sp, color = Navy600, maxLines = 1)
            Text(amount, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = color, maxLines = 1)
        }
    }
}

@Composable
private fun SummaryMiniBadge(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Slate100, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column {
            Text(title, fontSize = 10.sp, color = Navy600)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color)
        }
    }
}

@Composable
private fun ShiftHistoryCard(
    shift: RegisterShift,
    currency: String,
    onPrintClick: () -> Unit,
    onReopenClick: () -> Unit
) {
    val isClosed = shift.status == "CLOSED"
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Shift #${shift.shiftNumber}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Navy900
                    )
                    Text(
                        text = "Cashier: ${shift.cashierName} • ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(shift.openedAt))}",
                        fontSize = 11.sp,
                        color = Navy500
                    )
                }

                StatusBadge(
                    text = if (isClosed) "CLOSED" else "OPEN",
                    backgroundColor = if (isClosed) Slate200 else Emerald100,
                    textColor = if (isClosed) Slate700 else Emerald700
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Sales:", fontSize = 11.sp, color = Navy600)
                    Text("$currency %.0f".format(shift.totalSales), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Navy900)
                }
                Column {
                    Text("Expected Cash:", fontSize = 11.sp, color = Navy600)
                    Text("$currency %.0f".format(shift.expectedCash), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Navy900)
                }
                Column {
                    Text("Actual Counted:", fontSize = 11.sp, color = Navy600)
                    Text("$currency %.0f".format(shift.actualCash), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Emerald600)
                }
                Column {
                    Text("Variance:", fontSize = 11.sp, color = Navy600)
                    Text(
                        "$currency %.0f".format(shift.discrepancy),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (shift.discrepancy == 0.0) Emerald600 else if (shift.discrepancy > 0) Blue600 else Rose600
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isClosed) {
                    TextButton(onClick = onReopenClick) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(14.dp), tint = Amber700)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reopen", color = Amber700, fontSize = 12.sp)
                    }
                }

                Button(
                    onClick = onPrintClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Closing Slip", fontSize = 12.sp)
                }
            }
        }
    }
}

private fun buildClosingSlipText(shift: RegisterShift, storeName: String, currency: String): String {
    val df = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
    val openedStr = df.format(Date(shift.openedAt))
    val closedStr = shift.closedAt?.let { df.format(Date(it)) } ?: "STILL OPEN"

    return """
========================================
           $storeName
     DAILY REGISTER CLOSING SLIP
========================================
Shift Ref: ${shift.shiftNumber}
Cashier  : ${shift.cashierName}
Opened At: $openedStr
Closed At: $closedStr
Status   : ${shift.status}
----------------------------------------
FINANCIAL RECONCILIATION
----------------------------------------
(+) Opening Float     : $currency ${"%.2f".format(shift.openingCash)}
(+) Cash Sales        : $currency ${"%.2f".format(shift.cashSales)}
(+) Cash In / Deposits: $currency ${"%.2f".format(shift.cashIn)}
(-) Cash Out / Expense: $currency ${"%.2f".format(shift.cashOut)}
----------------------------------------
(=) EXPECTED CASH     : $currency ${"%.2f".format(shift.expectedCash)}
(=) ACTUAL COUNTED    : $currency ${"%.2f".format(shift.actualCash)}
----------------------------------------
VARIANCE / DIFFERENCE : $currency ${"%.2f".format(shift.discrepancy)}
RESULT                : ${if (shift.discrepancy == 0.0) "BALANCED" else if (shift.discrepancy > 0) "SURPLUS (+)" else "SHORTAGE (-)"}
----------------------------------------
OTHER REVENUES
Card / Digital Sales  : $currency ${"%.2f".format(shift.cardSales)}
Credit Sales (Due)    : $currency ${"%.2f".format(shift.creditSales)}
Total Net Sales       : $currency ${"%.2f".format(shift.totalSales)}
Total Receipts        : ${shift.totalInvoices} Invoices
----------------------------------------
DENOMINATIONS COUNTED:
5000 x ${shift.denomination5000} = $currency ${shift.denomination5000 * 5000}
1000 x ${shift.denomination1000} = $currency ${shift.denomination1000 * 1000}
 500 x ${shift.denomination500} = $currency ${shift.denomination500 * 500}
 100 x ${shift.denomination100} = $currency ${shift.denomination100 * 100}
  50 x ${shift.denomination50} = $currency ${shift.denomination50 * 50}
  20 x ${shift.denomination20} = $currency ${shift.denomination20 * 20}
  10 x ${shift.denomination10} = $currency ${shift.denomination10 * 10}
Coins / Change       = $currency ${"%.2f".format(shift.denominationCoins)}
----------------------------------------
Closing Notes: ${shift.closingNotes.ifBlank { "N/A" }}
Closed By    : ${shift.closedBy.ifBlank { shift.cashierName }}

Cashier Signature: ____________________

Supervisor Sign  : ____________________
========================================
""".trimIndent()
}
