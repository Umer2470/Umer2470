package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.AttendanceRecord
import com.example.data.entity.UserAccount
import com.example.ui.viewmodel.StoreViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val allAttendance by viewModel.allAttendanceRecords.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Today's Sheet, 1: Staff Roster & Wage Rates, 2: Payroll & Gross Pay, 3: History Logs
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }

    var showMarkDialog by remember { mutableStateOf<UserAccount?>(null) }
    var showEditWageDialog by remember { mutableStateOf<UserAccount?>(null) }
    var showEditPayDialog by remember { mutableStateOf<AttendanceRecord?>(null) }
    var showPayslipDialog by remember { mutableStateOf<UserAccount?>(null) }

    val currencySymbol = settings.currencySymbol.ifBlank { "Rs." }
    val todayDateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val todayDisplayDate = remember { SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date()) }

    // Filtered users for search
    val filteredUsers = remember(allUsers, searchQuery) {
        if (searchQuery.isBlank()) allUsers
        else allUsers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.username.contains(searchQuery, ignoreCase = true) ||
                    it.role.contains(searchQuery, ignoreCase = true)
        }
    }

    // Today's attendance records map
    val todayRecordsMap = remember(allAttendance, todayDateStr) {
        allAttendance.filter { it.dateStr == todayDateStr }.associateBy { it.userId }
    }

    // Stats calculations for Today
    val totalStaffCount = allUsers.size
    val presentCount = todayRecordsMap.values.count { it.status == "PRESENT" || it.status == "LATE" }
    val absentCount = todayRecordsMap.values.count { it.status == "ABSENT" }
    val leaveCount = todayRecordsMap.values.count { it.status == "LEAVE" }
    val unmarkedCount = (totalStaffCount - (presentCount + absentCount + leaveCount)).coerceAtLeast(0)
    val todayTotalGrossPay = todayRecordsMap.values.sumOf { it.totalDailyPay }

    val currentMyRecord = todayRecordsMap[currentUser?.id ?: -1L]

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Employee Attendance & Payroll",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color.White
                        )
                        Text(
                            text = todayDisplayDate,
                            fontSize = 12.sp,
                            color = GoldLight
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showToast("Attendance sheet synced") }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = GoldAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyDark)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // MY QUICK IN / OUT PUNCH BAR
            currentUser?.let { user ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = NavyDark,
                    border = BorderStroke(1.dp, GoldAccent)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(GoldAccent.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Badge, contentDescription = null, tint = GoldLight)
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = user.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Role: ${user.role.replace("_", " ")}",
                                        fontSize = 11.sp,
                                        color = GoldLight
                                    )
                                }
                            }

                            val myStatus = currentMyRecord?.status ?: "NOT_CHECKED_IN"
                            val isOnBreak = currentMyRecord?.breakStartTime != null && currentMyRecord.breakEndTime == null
                            val statusBg = when {
                                isOnBreak -> Color(0xFFD97706)
                                myStatus == "PRESENT" || myStatus == "LATE" -> Color(0xFF16A34A)
                                myStatus == "ABSENT" -> Color(0xFFDC2626)
                                myStatus == "LEAVE" -> Color(0xFF2563EB)
                                else -> Color(0xFF64748B)
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = statusBg
                            ) {
                                Text(
                                    text = when {
                                        isOnBreak -> "On Break ☕"
                                        myStatus == "NOT_CHECKED_IN" -> "Not Checked In"
                                        else -> myStatus
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Time & Pay metrics row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val inTimeStr = currentMyRecord?.checkInTime?.let {
                                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(it))
                            } ?: "--:--"

                            val outTimeStr = currentMyRecord?.checkOutTime?.let {
                                SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(it))
                            } ?: "Active"

                            val workedMins = currentMyRecord?.totalWorkingMinutes ?: 0L
                            val workedHrsStr = "${workedMins / 60}h ${workedMins % 60}m"
                            val todayPay = currentMyRecord?.totalDailyPay ?: 0.0

                            Column {
                                Text("Check In", fontSize = 10.sp, color = Color.Gray)
                                Text(inTimeStr, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column {
                                Text("Check Out", fontSize = 10.sp, color = Color.Gray)
                                Text(outTimeStr, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Column {
                                Text("Worked", fontSize = 10.sp, color = Color.Gray)
                                Text(workedHrsStr, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldLight)
                            }
                            Column {
                                Text("Gross Pay", fontSize = 10.sp, color = Color.Gray)
                                Text("$currencySymbol ${todayPay.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4ADE80))
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick Punch Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.checkInEmployee(user.id, user.name, user.role)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("In", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    viewModel.checkOutEmployee(user.id)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Out", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    if (currentMyRecord?.breakStartTime == null || currentMyRecord.breakEndTime != null) {
                                        viewModel.startBreakEmployee(user.id)
                                    } else {
                                        viewModel.endBreakEmployee(user.id)
                                    }
                                },
                                modifier = Modifier.weight(1.2f),
                                border = BorderStroke(1.dp, GoldAccent),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldLight)
                            ) {
                                Icon(Icons.Default.FreeBreakfast, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (currentMyRecord?.breakStartTime != null && currentMyRecord.breakEndTime == null) "End Break" else "Break In/Out",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // SUMMARY CARDS
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AttendanceStatCard("Present", presentCount.toString(), Color(0xFF16A34A), Modifier.weight(1f))
                AttendanceStatCard("Absent", absentCount.toString(), Color(0xFFDC2626), Modifier.weight(1f))
                AttendanceStatCard("Leave", leaveCount.toString(), Color(0xFFD97706), Modifier.weight(1f))
                AttendanceStatCard("Unmarked", unmarkedCount.toString(), Color(0xFF64748B), Modifier.weight(1f))
                AttendanceStatCard("Gross Pay", "$currencySymbol ${todayTotalGrossPay.toInt()}", Color(0xFF2563EB), Modifier.weight(1.3f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // TAB SELECTOR (Scrollable or TabRow)
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = NavyDark,
                edgePadding = 12.dp
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Today's Sheet", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Staff Wage Rates", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Monthly Payroll", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Shift History Logs", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                    icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            // SEARCH BAR
            if (selectedTab != 2) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    placeholder = { Text("Search staff name, username, or role...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )
            }

            // CONTENT BODY
            when (selectedTab) {
                0 -> {
                    // TODAY'S ATTENDANCE LIST & STATUS FILTER
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("ALL", "PRESENT", "ABSENT", "LEAVE", "LATE").forEach { statusKey ->
                                FilterChip(
                                    selected = selectedStatusFilter == statusKey,
                                    onClick = { selectedStatusFilter = statusKey },
                                    label = { Text(statusKey, fontSize = 10.sp) }
                                )
                            }
                        }

                        val displayedUsers = remember(filteredUsers, todayRecordsMap, selectedStatusFilter) {
                            if (selectedStatusFilter == "ALL") filteredUsers
                            else filteredUsers.filter { u ->
                                val rec = todayRecordsMap[u.id]
                                rec?.status == selectedStatusFilter
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            items(displayedUsers, key = { it.id }) { user ->
                                val record = todayRecordsMap[user.id]
                                StaffAttendanceCardRow(
                                    user = user,
                                    record = record,
                                    currencySymbol = currencySymbol,
                                    onMarkClick = { showMarkDialog = user },
                                    onEditPayClick = {
                                        if (record != null) {
                                            showEditPayDialog = record
                                        } else {
                                            viewModel.showToast("Please check in or mark attendance first.")
                                        }
                                    },
                                    onQuickCheckIn = {
                                        viewModel.checkInEmployee(user.id, user.name, user.role)
                                    },
                                    onQuickCheckOut = {
                                        viewModel.checkOutEmployee(user.id)
                                    },
                                    onQuickBreak = {
                                        if (record?.breakStartTime == null || record.breakEndTime != null) {
                                            viewModel.startBreakEmployee(user.id)
                                        } else {
                                            viewModel.endBreakEmployee(user.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // STAFF ROSTER & WAGE CONFIGURATION
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        items(filteredUsers, key = { it.id }) { user ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(NavyDark.copy(alpha = 0.1f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = user.name.take(1).uppercase(),
                                                    fontWeight = FontWeight.Bold,
                                                    color = NavyDark,
                                                    fontSize = 16.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(user.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                                Text("Role: ${user.role} • Username: ${user.username}", fontSize = 11.sp, color = Color.Gray)
                                                if (user.phone.isNotBlank()) {
                                                    Text("Phone: ${user.phone}", fontSize = 11.sp, color = NavyLight)
                                                }
                                            }
                                        }

                                        Button(
                                            onClick = { showEditWageDialog = user },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = NavyDark)
                                        ) {
                                            Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("Configure Pay", fontSize = 11.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = Color(0xFFF1F5F9))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Display configured rates
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Daily Rate", fontSize = 10.sp, color = Color.Gray)
                                            Text("$currencySymbol ${user.dailyWageRate.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                                        }
                                        Column {
                                            Text("Hourly Rate", fontSize = 10.sp, color = Color.Gray)
                                            Text("$currencySymbol ${user.hourlyWageRate.toInt()}/hr", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                                        }
                                        Column {
                                            Text("Overtime Rate", fontSize = 10.sp, color = Color.Gray)
                                            Text("$currencySymbol ${user.overtimeHourlyRate.toInt()}/hr", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                        }
                                        Column {
                                            Text("Monthly Base", fontSize = 10.sp, color = Color.Gray)
                                            Text("$currencySymbol ${user.monthlyBaseSalary.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // MONTHLY PAYROLL & GROSS PAY SUMMARY
                    val calendar = Calendar.getInstance()
                    val currentMonthStr = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time) }

                    // Group attendance records by user for current month
                    val monthAttendance = remember(allAttendance, currentMonthStr) {
                        allAttendance.filter { it.dateStr.startsWith(currentMonthStr) }
                    }

                    val totalStoreGrossPay = monthAttendance.sumOf { it.totalDailyPay }
                    val totalStoreHours = monthAttendance.sumOf { it.totalWorkingMinutes } / 60.0
                    val totalStoreOvertimeHours = monthAttendance.sumOf { it.overtimeMinutes } / 60.0

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = NavyDark)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("MONTHLY STORE PAYROLL", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GoldLight)
                                            Text("Month: $currentMonthStr", fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = Color(0xFF16A34A)
                                        ) {
                                            Text(
                                                "Active Period",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Total Store Payroll", fontSize = 10.sp, color = Color.LightGray)
                                            Text("$currencySymbol ${totalStoreGrossPay.toInt()}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF4ADE80))
                                        }
                                        Column {
                                            Text("Total Regular Hours", fontSize = 10.sp, color = Color.LightGray)
                                            Text(String.format(Locale.US, "%.1f hrs", totalStoreHours), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                        Column {
                                            Text("Total Overtime", fontSize = 10.sp, color = Color.LightGray)
                                            Text(String.format(Locale.US, "%.1f hrs", totalStoreOvertimeHours), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GoldLight)
                                        }
                                    }
                                }
                            }
                        }

                        // Employee Breakdown Cards
                        items(allUsers, key = { it.id }) { user ->
                            val userRecords = monthAttendance.filter { it.userId == user.id }
                            val daysPresent = userRecords.count { it.status == "PRESENT" || it.status == "LATE" }
                            val daysAbsent = userRecords.count { it.status == "ABSENT" }
                            val daysLeave = userRecords.count { it.status == "LEAVE" }
                            val totalWorkingMins = userRecords.sumOf { it.totalWorkingMinutes }
                            val totalOvertimeMins = userRecords.sumOf { it.overtimeMinutes }
                            val grossBasePay = userRecords.sumOf { it.dailyGrossPay }
                            val overtimePayTotal = userRecords.sumOf { it.overtimePay }
                            val deductionsTotal = userRecords.sumOf { it.deductions }
                            val allowancesTotal = userRecords.sumOf { it.allowances }

                            val netSalary = if (user.monthlyBaseSalary > 0) {
                                (user.monthlyBaseSalary + overtimePayTotal + allowancesTotal - deductionsTotal).coerceAtLeast(0.0)
                            } else {
                                (grossBasePay + overtimePayTotal + allowancesTotal - deductionsTotal).coerceAtLeast(0.0)
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(NavyDark.copy(alpha = 0.1f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(user.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = NavyDark)
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(user.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text("Role: ${user.role.replace("_", " ")}", fontSize = 11.sp, color = Color.Gray)
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Net Payable Salary", fontSize = 10.sp, color = Color.Gray)
                                            Text("$currencySymbol ${netSalary.toInt()}", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF16A34A))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Attendance Breakdown Chips Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFDCFCE7), modifier = Modifier.weight(1f)) {
                                            Text("Present: $daysPresent", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D), textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 4.dp))
                                        }
                                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFFEE2E2), modifier = Modifier.weight(1f)) {
                                            Text("Absent: $daysAbsent", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB91C1C), textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 4.dp))
                                        }
                                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFFEF3C7), modifier = Modifier.weight(1f)) {
                                            Text("Leave: $daysLeave", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309), textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 4.dp))
                                        }
                                        Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE0F2FE), modifier = Modifier.weight(1.2f)) {
                                            Text("${totalWorkingMins / 60}h Worked", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1), textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 4.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Detailed Pay calculation row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Base Pay", fontSize = 10.sp, color = Color.Gray)
                                            Text("$currencySymbol ${grossBasePay.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Column {
                                            Text("Overtime (${totalOvertimeMins / 60}h)", fontSize = 10.sp, color = Color.Gray)
                                            Text("$currencySymbol ${overtimePayTotal.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                        }
                                        Column {
                                            Text("Allowances", fontSize = 10.sp, color = Color.Gray)
                                            Text("$currencySymbol ${allowancesTotal.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                                        }
                                        Column {
                                            Text("Deductions", fontSize = 10.sp, color = Color.Gray)
                                            Text("-$currencySymbol ${deductionsTotal.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = { showPayslipDialog = user },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = NavyDark),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Generate Employee Payslip", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // ATTENDANCE SHIFT LOGS
                    val filteredLogs = remember(allAttendance, searchQuery) {
                        if (searchQuery.isBlank()) allAttendance
                        else allAttendance.filter {
                            it.userName.contains(searchQuery, ignoreCase = true) ||
                                    it.dateStr.contains(searchQuery, ignoreCase = true) ||
                                    it.status.contains(searchQuery, ignoreCase = true)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 20.dp)
                    ) {
                        if (filteredLogs.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No attendance records found.", color = Color.Gray)
                                }
                            }
                        } else {
                            items(filteredLogs, key = { it.id }) { rec ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(rec.userName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = when (rec.status) {
                                                        "PRESENT", "LATE" -> Color(0xFFDCFCE7)
                                                        "ABSENT" -> Color(0xFFFEE2E2)
                                                        else -> Color(0xFFFEF3C7)
                                                    }
                                                ) {
                                                    Text(
                                                        rec.status,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = when (rec.status) {
                                                            "PRESENT", "LATE" -> Color(0xFF15803D)
                                                            "ABSENT" -> Color(0xFFB91C1C)
                                                            else -> Color(0xFFB45309)
                                                        },
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Text(rec.dateStr, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GoldAccent)
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        val inTimeStr = rec.checkInTime?.let { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(it)) } ?: "--"
                                        val outTimeStr = rec.checkOutTime?.let { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(it)) } ?: "--"
                                        val workedHrsStr = "${rec.totalWorkingMinutes / 60}h ${rec.totalWorkingMinutes % 60}m"

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("In: $inTimeStr  |  Out: $outTimeStr", fontSize = 11.sp, color = Color.Gray)
                                            Text("Worked: $workedHrsStr", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NavyDark)
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Gross Pay: $currencySymbol ${rec.totalDailyPay.toInt()} (Regular: ${rec.dailyGrossPay.toInt()}, OT: ${rec.overtimePay.toInt()})",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF16A34A)
                                            )

                                            Row {
                                                IconButton(
                                                    onClick = { showEditPayDialog = rec },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Edit, contentDescription = "Edit Pay", tint = NavyDark, modifier = Modifier.size(16.dp))
                                                }
                                                IconButton(
                                                    onClick = { viewModel.deleteAttendanceRecord(rec) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }

                                        if (rec.notes.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text("Note: ${rec.notes}", fontSize = 11.sp, color = Color.Gray)
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

    // MARK ATTENDANCE STATUS DIALOG
    showMarkDialog?.let { user ->
        var selectedStatus by remember { mutableStateOf("PRESENT") }
        var noteInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showMarkDialog = null },
            title = {
                Text("Mark Attendance - ${user.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select attendance status for today:", fontSize = 13.sp, color = Color.Gray)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedStatus == "PRESENT",
                            onClick = { selectedStatus = "PRESENT" },
                            label = { Text("Present") }
                        )
                        FilterChip(
                            selected = selectedStatus == "ABSENT",
                            onClick = { selectedStatus = "ABSENT" },
                            label = { Text("Absent") }
                        )
                        FilterChip(
                            selected = selectedStatus == "LEAVE",
                            onClick = { selectedStatus = "LEAVE" },
                            label = { Text("Leave") }
                        )
                        FilterChip(
                            selected = selectedStatus == "LATE",
                            onClick = { selectedStatus = "LATE" },
                            label = { Text("Late") }
                        )
                    }

                    OutlinedTextField(
                        value = noteInput,
                        onValueChange = { noteInput = it },
                        label = { Text("Optional Notes / Reason") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.markEmployeeAttendanceStatus(
                            userId = user.id,
                            userName = user.name,
                            userRole = user.role,
                            status = selectedStatus,
                            notes = noteInput
                        )
                        showMarkDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyDark)
                ) {
                    Text("Save Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMarkDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // EDIT WAGE & SALARY RATES DIALOG
    showEditWageDialog?.let { user ->
        var dailyWageStr by remember { mutableStateOf(user.dailyWageRate.toString()) }
        var hourlyWageStr by remember { mutableStateOf(user.hourlyWageRate.toString()) }
        var overtimeWageStr by remember { mutableStateOf(user.overtimeHourlyRate.toString()) }
        var monthlyBaseStr by remember { mutableStateOf(user.monthlyBaseSalary.toString()) }

        AlertDialog(
            onDismissRequest = { showEditWageDialog = null },
            title = {
                Text("Configure Salary & Pay Rates - ${user.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Set wage rates for automated gross pay calculations:", fontSize = 12.sp, color = Color.Gray)

                    OutlinedTextField(
                        value = dailyWageStr,
                        onValueChange = { dailyWageStr = it },
                        label = { Text("Daily Wage Rate ($currencySymbol)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = hourlyWageStr,
                        onValueChange = { hourlyWageStr = it },
                        label = { Text("Hourly Wage Rate ($currencySymbol)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = overtimeWageStr,
                        onValueChange = { overtimeWageStr = it },
                        label = { Text("Overtime Hourly Rate ($currencySymbol)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = monthlyBaseStr,
                        onValueChange = { monthlyBaseStr = it },
                        label = { Text("Monthly Base Salary ($currencySymbol)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateUserWageRates(
                            userId = user.id,
                            dailyWage = dailyWageStr.toDoubleOrNull() ?: 0.0,
                            hourlyWage = hourlyWageStr.toDoubleOrNull() ?: 0.0,
                            overtimeWage = overtimeWageStr.toDoubleOrNull() ?: 0.0,
                            monthlyBase = monthlyBaseStr.toDoubleOrNull() ?: 0.0
                        )
                        showEditWageDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyDark)
                ) {
                    Text("Save Rates")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditWageDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // EDIT ATTENDANCE PAY & ADJUSTMENTS DIALOG
    showEditPayDialog?.let { record ->
        var grossPayStr by remember { mutableStateOf(record.dailyGrossPay.toString()) }
        var overtimePayStr by remember { mutableStateOf(record.overtimePay.toString()) }
        var deductionsStr by remember { mutableStateOf(record.deductions.toString()) }
        var allowancesStr by remember { mutableStateOf(record.allowances.toString()) }
        var notesStr by remember { mutableStateOf(record.notes) }

        AlertDialog(
            onDismissRequest = { showEditPayDialog = null },
            title = {
                Text("Adjust Pay - ${record.userName} (${record.dateStr})", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = grossPayStr,
                        onValueChange = { grossPayStr = it },
                        label = { Text("Regular Gross Pay ($currencySymbol)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = overtimePayStr,
                        onValueChange = { overtimePayStr = it },
                        label = { Text("Overtime Pay ($currencySymbol)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = allowancesStr,
                        onValueChange = { allowancesStr = it },
                        label = { Text("Allowances / Bonus ($currencySymbol)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = deductionsStr,
                        onValueChange = { deductionsStr = it },
                        label = { Text("Deductions / Advance ($currencySymbol)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = notesStr,
                        onValueChange = { notesStr = it },
                        label = { Text("Notes / Reason") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateAttendancePayRecord(
                            recordId = record.id,
                            dailyGrossPay = grossPayStr.toDoubleOrNull() ?: 0.0,
                            overtimePay = overtimePayStr.toDoubleOrNull() ?: 0.0,
                            deductions = deductionsStr.toDoubleOrNull() ?: 0.0,
                            allowances = allowancesStr.toDoubleOrNull() ?: 0.0,
                            notes = notesStr
                        )
                        showEditPayDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyDark)
                ) {
                    Text("Save Adjustment")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditPayDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // PAYSLIP SUMMARY DIALOG
    showPayslipDialog?.let { user ->
        val calendar = Calendar.getInstance()
        val currentMonthStr = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time) }
        val userRecords = remember(allAttendance, user.id, currentMonthStr) {
            allAttendance.filter { it.userId == user.id && it.dateStr.startsWith(currentMonthStr) }
        }

        val daysPresent = userRecords.count { it.status == "PRESENT" || it.status == "LATE" }
        val daysAbsent = userRecords.count { it.status == "ABSENT" }
        val daysLeave = userRecords.count { it.status == "LEAVE" }
        val totalWorkingMins = userRecords.sumOf { it.totalWorkingMinutes }
        val totalOvertimeMins = userRecords.sumOf { it.overtimeMinutes }
        val grossBasePay = userRecords.sumOf { it.dailyGrossPay }
        val overtimePayTotal = userRecords.sumOf { it.overtimePay }
        val deductionsTotal = userRecords.sumOf { it.deductions }
        val allowancesTotal = userRecords.sumOf { it.allowances }

        val netSalary = if (user.monthlyBaseSalary > 0) {
            (user.monthlyBaseSalary + overtimePayTotal + allowancesTotal - deductionsTotal).coerceAtLeast(0.0)
        } else {
            (grossBasePay + overtimePayTotal + allowancesTotal - deductionsTotal).coerceAtLeast(0.0)
        }

        AlertDialog(
            onDismissRequest = { showPayslipDialog = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(settings.storeName.ifBlank { "CH UMAIR SENTRY STORE" }, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = NavyDark)
                        Text("Official Employee Salary Payslip", fontSize = 11.sp, color = Color.Gray)
                    }
                    IconButton(onClick = { showPayslipDialog = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    // Employee Info
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NavyDark, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(user.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("Role: ${user.role} • Month: $currentMonthStr", fontSize = 11.sp, color = GoldLight)
                        }
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF16A34A), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("VERIFIED PAYROLL", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    @Composable
                    fun PayslipRow(label: String, value: String, isBold: Boolean = false, color: Color = Color(0xFF1E293B)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, fontSize = 11.sp, color = Color(0xFF64748B))
                            Text(value, fontSize = 11.sp, fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold, color = color)
                        }
                    }

                    PayslipRow("Days Present / Worked:", "$daysPresent Days", isBold = true)
                    PayslipRow("Days Absent:", "$daysAbsent Days")
                    PayslipRow("Approved Leave:", "$daysLeave Days")
                    PayslipRow("Total Hours Worked:", String.format(Locale.US, "%.1f hrs", totalWorkingMins / 60.0))
                    PayslipRow("Total Overtime Hours:", String.format(Locale.US, "%.1f hrs", totalOvertimeMins / 60.0))

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Spacer(modifier = Modifier.height(8.dp))

                    PayslipRow("Base Gross Salary:", "$currencySymbol ${grossBasePay.toInt()}", isBold = true)
                    PayslipRow("Overtime Pay:", "$currencySymbol ${overtimePayTotal.toInt()}", color = Color(0xFFD97706))
                    PayslipRow("Allowances & Bonuses:", "+$currencySymbol ${allowancesTotal.toInt()}", color = Color(0xFF16A34A))
                    PayslipRow("Deductions & Advances:", "-$currencySymbol ${deductionsTotal.toInt()}", color = Color(0xFFDC2626))

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFFCBD5E1))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("NET PAYABLE SALARY", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = NavyDark)
                        Text("$currencySymbol ${netSalary.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Color(0xFF16A34A))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.showToast("Payslip generated for ${user.name}")
                        showPayslipDialog = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyDark),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Print / Share Payslip")
                }
            }
        )
    }
}

@Composable
private fun AttendanceStatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = color)
            Text(title, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = NavyDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StaffAttendanceCardRow(
    user: UserAccount,
    record: AttendanceRecord?,
    currencySymbol: String,
    onMarkClick: () -> Unit,
    onEditPayClick: () -> Unit,
    onQuickCheckIn: () -> Unit,
    onQuickCheckOut: () -> Unit,
    onQuickBreak: () -> Unit
) {
    val status = record?.status ?: "NOT_MARKED"
    val isOnBreak = record?.breakStartTime != null && record.breakEndTime == null
    val (statusText, statusBg) = when {
        isOnBreak -> "On Break" to Color(0xFFD97706)
        status == "PRESENT" -> "Present" to Color(0xFF16A34A)
        status == "ABSENT" -> "Absent" to Color(0xFFDC2626)
        status == "LEAVE" -> "On Leave" to Color(0xFF2563EB)
        status == "LATE" -> "Late" to Color(0xFF9333EA)
        else -> "Unmarked" to Color(0xFF64748B)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NavyDark.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.name.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = NavyDark
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(user.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(user.role.replace("_", " "), fontSize = 11.sp, color = Color.Gray)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = statusBg.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, statusBg.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusBg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val inTimeStr = record?.checkInTime?.let { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(it)) } ?: "Not Checked In"
            val outTimeStr = record?.checkOutTime?.let { SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(it)) } ?: "Active"
            val workedMins = record?.totalWorkingMinutes ?: 0L
            val grossPay = record?.totalDailyPay ?: 0.0

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("In: $inTimeStr | Out: $outTimeStr", fontSize = 11.sp, color = Color.Gray)
                    Text("Worked: ${workedMins / 60}h ${workedMins % 60}m | Pay: $currencySymbol ${grossPay.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onQuickCheckIn, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Login, contentDescription = "Check In", tint = Color(0xFF16A34A), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onQuickCheckOut, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Logout, contentDescription = "Check Out", tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onQuickBreak, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.FreeBreakfast, contentDescription = "Break", tint = GoldAccent, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onMarkClick, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Status", tint = NavyDark, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onEditPayClick, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Payments, contentDescription = "Adjust Pay", tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
