package com.example.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.AttendanceMachineConfig
import com.example.data.entity.AttendanceRecord
import com.example.data.entity.EmployeeSalaryConfig
import com.example.data.entity.PayrollRecord
import com.example.ui.components.AppHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Attendance", "Payroll & Salary", "Biometric Machine")

    val attendanceList by viewModel.attendanceRecords.collectAsState()
    val users by viewModel.users.collectAsState()
    val payrollRecords by viewModel.payrollRecords.collectAsState()
    val salaryConfigs by viewModel.employeeSalaryConfigs.collectAsState()
    val machineConfig by viewModel.attendanceMachineConfig.collectAsState()

    var statusMessage by remember { mutableStateOf<String?>(null) }

    // Dialog States
    var showMarkDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<AttendanceRecord?>(null) }
    var showSalaryConfigDialog by remember { mutableStateOf(false) }
    var showDisburseDialog by remember { mutableStateOf<PayrollRecord?>(null) }
    var showPayslipDialog by remember { mutableStateOf<PayrollRecord?>(null) }
    var showMachineImportDialog by remember { mutableStateOf(false) }

    // Current Month Selection for Payroll
    val currentMonthDefault = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()) }
    var selectedMonthYear by remember { mutableStateOf(currentMonthDefault) }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Staff Attendance & Payroll",
                subtitle = "Attendance, Salary Calculation & Device Control",
                onBackClick = onNavigateBack
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showMarkDialog = true },
                    containerColor = Navy900,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("mark_attendance_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Mark Attendance")
                }
            }
        },
        containerColor = Slate50
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = Navy900
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            statusMessage?.let { msg ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { statusMessage = null }) {
                            Text("Dismiss", color = Color.White)
                        }
                    },
                    containerColor = Navy900,
                    contentColor = Color.White
                ) {
                    Text(msg)
                }
            }

            when (selectedTab) {
                0 -> AttendanceTabContent(
                    attendanceList = attendanceList,
                    onEdit = { showEditDialog = it },
                    onDelete = {
                        viewModel.deleteAttendanceRecord(it.id) {
                            statusMessage = "Attendance record deleted."
                        }
                    }
                )
                1 -> PayrollTabContent(
                    monthYear = selectedMonthYear,
                    onMonthChange = { selectedMonthYear = it },
                    payrollRecords = payrollRecords.filter { it.monthYear == selectedMonthYear },
                    onGeneratePayroll = {
                        viewModel.generateMonthlyPayroll(selectedMonthYear) { count ->
                            statusMessage = "Generated payroll for $count staff member(s)."
                        }
                    },
                    onOpenSalaryConfig = { showSalaryConfigDialog = true },
                    onDisburse = { showDisburseDialog = it },
                    onViewSlip = { showPayslipDialog = it }
                )
                2 -> MachineTabContent(
                    config = machineConfig ?: AttendanceMachineConfig(),
                    onTestConnection = { ip, port ->
                        viewModel.testMachineConnection(ip, port) { success, msg ->
                            statusMessage = msg
                        }
                    },
                    onSyncLogs = { ip, port ->
                        viewModel.syncAttendanceFromMachine(ip, port) { success, msg, _ ->
                            statusMessage = msg
                        }
                    },
                    onSaveConfig = { newConfig ->
                        viewModel.saveMachineConfig(newConfig) {
                            statusMessage = "Machine configuration saved."
                        }
                    },
                    onOpenImportDialog = { showMachineImportDialog = true }
                )
            }
        }
    }

    // Mark Attendance Dialog
    if (showMarkDialog) {
        MarkAttendanceDialog(
            userList = users.map { it.fullName.ifBlank { it.username } },
            onDismiss = { showMarkDialog = false },
            onSave = { empName, desig, date, status, notes, hours ->
                viewModel.recordAttendance(
                    employeeName = empName,
                    designation = desig,
                    dateString = date,
                    status = status,
                    workingHours = hours,
                    notes = notes,
                    onSuccess = {
                        showMarkDialog = false
                        statusMessage = "Attendance recorded for $empName."
                    }
                )
            }
        )
    }

    // Edit Attendance Dialog
    showEditDialog?.let { record ->
        EditAttendanceDialog(
            record = record,
            onDismiss = { showEditDialog = null },
            onSave = { updated ->
                viewModel.updateAttendanceRecord(updated) {
                    showEditDialog = null
                    statusMessage = "Attendance record updated."
                }
            }
        )
    }

    // Salary Configuration Dialog
    if (showSalaryConfigDialog) {
        SalaryConfigDialog(
            users = users.map { it.fullName.ifBlank { it.username } to it.role },
            existingConfigs = salaryConfigs,
            onDismiss = { showSalaryConfigDialog = false },
            onSave = { config ->
                viewModel.saveEmployeeSalaryConfig(config) {
                    showSalaryConfigDialog = false
                    statusMessage = "Salary configuration updated."
                }
            }
        )
    }

    // Disburse Payment Dialog
    showDisburseDialog?.let { record ->
        DisbursePaymentDialog(
            record = record,
            onDismiss = { showDisburseDialog = null },
            onDisburse = { amount, method, ref ->
                viewModel.recordPayrollPayment(
                    payrollId = record.id,
                    amount = amount,
                    paymentMethod = method,
                    reference = ref,
                    authorizedBy = "Owner / Admin",
                    onComplete = { success ->
                        showDisburseDialog = null
                        statusMessage = if (success) "Payment of Rs $amount disbursed." else "Payment failed."
                    }
                )
            }
        )
    }

    // View Payslip Dialog
    showPayslipDialog?.let { record ->
        PayslipDialog(
            record = record,
            onDismiss = { showPayslipDialog = null }
        )
    }

    // Machine Punch Log Import Dialog
    if (showMachineImportDialog) {
        MachineLogImportDialog(
            onDismiss = { showMachineImportDialog = false },
            onImport = { rawText ->
                viewModel.importAttendanceLogs(rawText) { success, msg, count ->
                    showMachineImportDialog = false
                    statusMessage = msg
                }
            }
        )
    }
}

@Composable
fun AttendanceTabContent(
    attendanceList: List<AttendanceRecord>,
    onEdit: (AttendanceRecord) -> Unit,
    onDelete: (AttendanceRecord) -> Unit
) {
    val presentCount = attendanceList.count { it.status.equals("Present", ignoreCase = true) }
    val lateCount = attendanceList.count { it.status.equals("Late", ignoreCase = true) }
    val absentCount = attendanceList.count { it.status.equals("Absent", ignoreCase = true) }
    val halfDayCount = attendanceList.count { it.status.equals("Half-Day", ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryStatCard("Present", presentCount.toString(), Emerald600, Emerald100, Modifier.weight(1f))
            SummaryStatCard("Late", lateCount.toString(), Gold600, Gold100, Modifier.weight(1f))
            SummaryStatCard("Absent", absentCount.toString(), Rose600, Rose100, Modifier.weight(1f))
            SummaryStatCard("Half-Day", halfDayCount.toString(), Blue600, Blue100, Modifier.weight(1f))
        }

        if (attendanceList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No attendance records yet. Tap + to mark attendance or import logs.",
                    color = Navy500,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(attendanceList, key = { it.id }) { record ->
                    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    val inStr = if (record.checkInTime > 0) timeFormat.format(Date(record.checkInTime)) else "--"
                    val outStr = if (record.checkOutTime > 0) timeFormat.format(Date(record.checkOutTime)) else "--"

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp)
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
                                        text = record.employeeName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Navy900
                                    )
                                    if (record.designation.isNotBlank()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "(${record.designation})",
                                            fontSize = 12.sp,
                                            color = Navy500
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Date: ${record.dateString} • In: $inStr • Out: $outStr",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Navy500
                                )
                                Text(
                                    text = "Hours: ${String.format(Locale.getDefault(), "%.1f", record.workingHours)} hrs" +
                                            if (record.notes.isNotBlank()) " • ${record.notes}" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Navy600
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                StatusBadge(
                                    text = record.status,
                                    backgroundColor = when (record.status) {
                                        "Present" -> Emerald100
                                        "Late" -> Gold100
                                        "Half-Day" -> Blue100
                                        else -> Rose100
                                    },
                                    textColor = when (record.status) {
                                        "Present" -> Emerald600
                                        "Late" -> Gold600
                                        "Half-Day" -> Blue600
                                        else -> Rose600
                                    }
                                )
                                IconButton(onClick = { onEdit(record) }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Navy600)
                                }
                                IconButton(onClick = { onDelete(record) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Rose600)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryStatCard(label: String, value: String, textColor: Color, bgColor: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 11.sp, color = textColor, fontWeight = FontWeight.SemiBold)
            Text(value, fontSize = 18.sp, color = textColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PayrollTabContent(
    monthYear: String,
    onMonthChange: (String) -> Unit,
    payrollRecords: List<PayrollRecord>,
    onGeneratePayroll: () -> Unit,
    onOpenSalaryConfig: () -> Unit,
    onDisburse: (PayrollRecord) -> Unit,
    onViewSlip: (PayrollRecord) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Controls Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = monthYear,
                onValueChange = onMonthChange,
                label = { Text("Month (yyyy-MM)") },
                singleLine = true,
                modifier = Modifier.width(160.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenSalaryConfig) {
                    Text("Salary Rules", fontSize = 12.sp)
                }
                Button(
                    onClick = onGeneratePayroll,
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Compute", fontSize = 12.sp)
                }
            }
        }

        if (payrollRecords.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No payroll records for $monthYear.\nTap 'Compute' to auto-calculate salaries from attendance.",
                    color = Navy500,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(payrollRecords, key = { it.id }) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = record.employeeName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Navy900
                                    )
                                    Text(
                                        text = record.designation,
                                        fontSize = 12.sp,
                                        color = Navy500
                                    )
                                }
                                StatusBadge(
                                    text = record.paymentStatus,
                                    backgroundColor = if (record.paymentStatus == "PAID") Emerald100 else Gold100,
                                    textColor = if (record.paymentStatus == "PAID") Emerald600 else Gold600
                                )
                            }

                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = Slate200)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Days: P:${record.presentDays} | A:${record.absentDays} | L:${record.lateDays}", fontSize = 12.sp, color = Navy600)
                                    Text("Basic: Rs ${String.format(Locale.getDefault(), "%.0f", record.basicSalary)}", fontSize = 12.sp, color = Navy600)
                                    if (record.deductions > 0) {
                                        Text("Deductions: -Rs ${String.format(Locale.getDefault(), "%.0f", record.deductions)} (${record.deductionReason})", fontSize = 11.sp, color = Rose600)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Net Payable", fontSize = 11.sp, color = Navy500)
                                    Text("Rs ${String.format(Locale.getDefault(), "%.0f", record.netSalary)}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Navy900)
                                    if (record.paidAmount > 0) {
                                        Text("Paid: Rs ${String.format(Locale.getDefault(), "%.0f", record.paidAmount)}", fontSize = 11.sp, color = Emerald600)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { onViewSlip(record) }) {
                                    Text("Payslip Voucher", fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { onDisburse(record) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Navy900)
                                ) {
                                    Text(if (record.paymentStatus == "PAID") "Add Disbursal" else "Disburse Salary", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MachineTabContent(
    config: AttendanceMachineConfig,
    onTestConnection: (String, Int) -> Unit,
    onSyncLogs: (String, Int) -> Unit,
    onSaveConfig: (AttendanceMachineConfig) -> Unit,
    onOpenImportDialog: () -> Unit
) {
    var ip by remember(config) { mutableStateOf(config.ipAddress) }
    var portStr by remember(config) { mutableStateOf(config.port.toString()) }
    var deviceName by remember(config) { mutableStateOf(config.deviceName) }
    var autoSync by remember(config) { mutableStateOf(config.isAutoSyncEnabled) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Device Status", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Navy900)
                        StatusBadge(
                            text = if (config.isConnected) "CONNECTED" else "NOT CONNECTED",
                            backgroundColor = if (config.isConnected) Emerald100 else Slate200,
                            textColor = if (config.isConnected) Emerald600 else Slate600
                        )
                    }

                    Text(
                        text = "Real machine socket connection on local network. Status reflects genuine hardware reachability.",
                        fontSize = 12.sp,
                        color = Navy500
                    )

                    if (config.lastSyncTime > 0) {
                        val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
                        Text("Last Sync: ${sdf.format(Date(config.lastSyncTime))}", fontSize = 12.sp, color = Navy600)
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Machine Configuration", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Navy900)

                    OutlinedTextField(
                        value = ip,
                        onValueChange = { ip = it },
                        label = { Text("Device IP Address (LAN)") },
                        placeholder = { Text("e.g. 192.168.1.201") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = portStr,
                        onValueChange = { portStr = it },
                        label = { Text("Port (Standard: 4370)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = deviceName,
                        onValueChange = { deviceName = it },
                        label = { Text("Device Model / Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto-Sync on App Launch", fontSize = 13.sp, color = Navy800)
                        Switch(checked = autoSync, onCheckedChange = { autoSync = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val p = portStr.toIntOrNull() ?: 4370
                                onTestConnection(ip, p)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test Connection", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                val p = portStr.toIntOrNull() ?: 4370
                                onSaveConfig(
                                    config.copy(
                                        ipAddress = ip.trim(),
                                        port = p,
                                        deviceName = deviceName.trim(),
                                        isAutoSyncEnabled = autoSync
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save Config", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Machine Punch Logs & Import", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Navy900)
                    Text(
                        "Sync live logs directly from the biometric device or import CSV/text punch files. Duplicate punches are automatically detected and safely skipped.",
                        fontSize = 12.sp,
                        color = Navy500
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val p = portStr.toIntOrNull() ?: 4370
                                onSyncLogs(ip, p)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync Machine", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = onOpenImportDialog,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Import Logs", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkAttendanceDialog(
    userList: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, Double) -> Unit
) {
    var name by remember { mutableStateOf(userList.firstOrNull() ?: "") }
    var designation by remember { mutableStateOf("Staff") }
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var date by remember { mutableStateOf(today) }
    var status by remember { mutableStateOf("Present") }
    var hoursStr by remember { mutableStateOf("8.0") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mark Staff Attendance") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Employee Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = designation,
                        onValueChange = { designation = it },
                        label = { Text("Designation") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date (yyyy-MM-dd)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Present", "Late", "Half-Day", "Absent", "Leave").forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = {
                                status = s
                                hoursStr = when (s) {
                                    "Present", "Late" -> "8.0"
                                    "Half-Day" -> "4.0"
                                    else -> "0.0"
                                }
                            },
                            label = { Text(s, fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = hoursStr,
                    onValueChange = { hoursStr = it },
                    label = { Text("Working Hours") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val hours = hoursStr.toDoubleOrNull() ?: 8.0
                        onSave(name.trim(), designation.trim(), date.trim(), status, notes.trim(), hours)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Navy900)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EditAttendanceDialog(
    record: AttendanceRecord,
    onDismiss: () -> Unit,
    onSave: (AttendanceRecord) -> Unit
) {
    var status by remember { mutableStateOf(record.status) }
    var hoursStr by remember { mutableStateOf(record.workingHours.toString()) }
    var notes by remember { mutableStateOf(record.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Attendance: ${record.employeeName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Date: ${record.dateString}", fontSize = 12.sp, color = Navy600)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Present", "Late", "Half-Day", "Absent", "Leave").forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { Text(s, fontSize = 10.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = hoursStr,
                    onValueChange = { hoursStr = it },
                    label = { Text("Working Hours") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hours = hoursStr.toDoubleOrNull() ?: record.workingHours
                    onSave(record.copy(status = status, workingHours = hours, notes = notes.trim()))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Navy900)
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SalaryConfigDialog(
    users: List<Pair<String, String>>,
    existingConfigs: List<EmployeeSalaryConfig>,
    onDismiss: () -> Unit,
    onSave: (EmployeeSalaryConfig) -> Unit
) {
    var selectedIndex by remember { mutableStateOf(0) }
    val currentUser = users.getOrNull(selectedIndex) ?: ("Staff" to "Staff")
    val existing = existingConfigs.firstOrNull { it.employeeName.equals(currentUser.first, ignoreCase = true) }

    var basicSalary by remember(selectedIndex) { mutableStateOf((existing?.basicSalary ?: 25000.0).toString()) }
    var allowances by remember(selectedIndex) { mutableStateOf((existing?.monthlyAllowances ?: 1000.0).toString()) }
    var overtimeRate by remember(selectedIndex) { mutableStateOf((existing?.overtimeHourlyRate ?: 150.0).toString()) }
    var lateDeduction by remember(selectedIndex) { mutableStateOf((existing?.lateDeductionPerDay ?: 300.0).toString()) }
    var absentDeduction by remember(selectedIndex) { mutableStateOf((existing?.absentDeductionPerDay ?: 800.0).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Salary Configuration Rules") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Select Employee:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy800)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    users.forEachIndexed { idx, pair ->
                        FilterChip(
                            selected = selectedIndex == idx,
                            onClick = { selectedIndex = idx },
                            label = { Text(pair.first, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = basicSalary,
                    onValueChange = { basicSalary = it },
                    label = { Text("Monthly Basic Salary (Rs)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = allowances,
                    onValueChange = { allowances = it },
                    label = { Text("Monthly Allowances (Rs)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = overtimeRate,
                    onValueChange = { overtimeRate = it },
                    label = { Text("Overtime Rate (Rs / Hour)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = lateDeduction,
                        onValueChange = { lateDeduction = it },
                        label = { Text("Late Cut / Day") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = absentDeduction,
                        onValueChange = { absentDeduction = it },
                        label = { Text("Absent Cut / Day") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val config = EmployeeSalaryConfig(
                        employeeId = existing?.employeeId ?: (selectedIndex + 1).toLong(),
                        employeeName = currentUser.first,
                        designation = currentUser.second,
                        basicSalary = basicSalary.toDoubleOrNull() ?: 25000.0,
                        monthlyAllowances = allowances.toDoubleOrNull() ?: 1000.0,
                        overtimeHourlyRate = overtimeRate.toDoubleOrNull() ?: 150.0,
                        lateDeductionPerDay = lateDeduction.toDoubleOrNull() ?: 300.0,
                        absentDeductionPerDay = absentDeduction.toDoubleOrNull() ?: 800.0
                    )
                    onSave(config)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Navy900)
            ) {
                Text("Save Rules")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun DisbursePaymentDialog(
    record: PayrollRecord,
    onDismiss: () -> Unit,
    onDisburse: (Double, String, String) -> Unit
) {
    val remaining = (record.netSalary - record.paidAmount).coerceAtLeast(0.0)
    var amountStr by remember { mutableStateOf(remaining.toString()) }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var refNumber by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Disburse Salary: ${record.employeeName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Month: ${record.monthYear} • Net Payable: Rs ${String.format(Locale.getDefault(), "%.0f", record.netSalary)}", fontSize = 12.sp, color = Navy600)
                if (record.paidAmount > 0) {
                    Text("Already Paid: Rs ${String.format(Locale.getDefault(), "%.0f", record.paidAmount)}", fontSize = 12.sp, color = Emerald600)
                }

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Amount to Pay (Rs)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Cash", "Bank Transfer", "Cheque").forEach { method ->
                        FilterChip(
                            selected = paymentMethod == method,
                            onClick = { paymentMethod = method },
                            label = { Text(method, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = refNumber,
                    onValueChange = { refNumber = it },
                    label = { Text("Reference / Receipt # (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0) onDisburse(amt, paymentMethod, refNumber.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = Navy900)
            ) {
                Text("Confirm Disbursal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PayslipDialog(
    record: PayrollRecord,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("SALARY SLIP VOUCHER", fontWeight = FontWeight.Bold, color = Navy900) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Employee:", fontWeight = FontWeight.SemiBold, color = Navy800)
                    Text("${record.employeeName} (${record.designation})", color = Navy900)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Pay Period:", fontWeight = FontWeight.SemiBold, color = Navy800)
                    Text(record.monthYear, color = Navy900)
                }
                Divider(color = Slate200)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Present Days:")
                    Text("${record.presentDays} days")
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Absent / Late:")
                    Text("${record.absentDays} absent / ${record.lateDays} late")
                }
                if (record.overtimeHours > 0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Overtime:")
                        Text("${String.format(Locale.getDefault(), "%.1f", record.overtimeHours)} hrs (+Rs ${String.format(Locale.getDefault(), "%.0f", record.overtimeAmount)})")
                    }
                }
                Divider(color = Slate200)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Basic Salary:")
                    Text("Rs ${String.format(Locale.getDefault(), "%.0f", record.basicSalary)}")
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Allowances:")
                    Text("+ Rs ${String.format(Locale.getDefault(), "%.0f", record.allowances)}")
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Deductions:")
                    Text("- Rs ${String.format(Locale.getDefault(), "%.0f", record.deductions)}", color = Rose600)
                }
                if (record.deductionReason.isNotBlank()) {
                    Text("Reason: ${record.deductionReason}", fontSize = 11.sp, color = Navy500)
                }
                Divider(color = Slate300)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("NET SALARY:", fontWeight = FontWeight.Bold, color = Navy900)
                    Text("Rs ${String.format(Locale.getDefault(), "%.0f", record.netSalary)}", fontWeight = FontWeight.Bold, color = Navy900)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Status / Disbursed:", color = Navy600)
                    Text("${record.paymentStatus} (Rs ${String.format(Locale.getDefault(), "%.0f", record.paidAmount)})", fontWeight = FontWeight.SemiBold, color = Emerald600)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Navy900)
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
fun MachineLogImportDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var text by remember {
        mutableStateOf(
            "# Format: Name, Date(yyyy-MM-dd), CheckIn(HH:mm), CheckOut(HH:mm), Status, Designation\n" +
            "Ali Khan, 2026-09-22, 09:00, 18:00, Present, Cashier\n" +
            "Zainab Bibi, 2026-09-22, 09:30, 18:00, Late, Supervisor\n" +
            "Usman Raza, 2026-09-22, 00:00, 00:00, Absent, Staff"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Machine Punch Logs") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Paste raw machine punch records below. Duplicate punches will be automatically identified and skipped.",
                    fontSize = 12.sp,
                    color = Navy600
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("CSV / Text Log Data") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(text) },
                colors = ButtonDefaults.buttonColors(containerColor = Navy900)
            ) {
                Text("Process & Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
