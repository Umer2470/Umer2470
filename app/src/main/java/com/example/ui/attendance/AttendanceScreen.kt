package com.example.ui.attendance

import androidx.compose.foundation.background
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
    val attendanceList by viewModel.attendanceRecords.collectAsState()
    val users by viewModel.users.collectAsState()

    var showMarkDialog by remember { mutableStateOf(false) }
    var selectedEmployeeName by remember { mutableStateOf("") }
    var selectedStatus by remember { mutableStateOf("Present") }
    var attendanceNotes by remember { mutableStateOf("") }

    if (showMarkDialog) {
        AlertDialog(
            onDismissRequest = { showMarkDialog = false },
            title = { Text("Mark Employee Attendance") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = selectedEmployeeName,
                        onValueChange = { selectedEmployeeName = it },
                        label = { Text("Employee Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("employee_name_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Present", "Late", "Half-Day", "Absent").forEach { status ->
                            FilterChip(
                                selected = selectedStatus == status,
                                onClick = { selectedStatus = status },
                                label = { Text(status, fontSize = 11.sp) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = attendanceNotes,
                        onValueChange = { attendanceNotes = it },
                        label = { Text("Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedEmployeeName.isNotBlank()) {
                            viewModel.recordAttendance(
                                employeeName = selectedEmployeeName,
                                status = selectedStatus,
                                notes = attendanceNotes,
                                onSuccess = {
                                    showMarkDialog = false
                                    selectedEmployeeName = ""
                                    attendanceNotes = ""
                                }
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    modifier = Modifier.testTag("submit_attendance_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMarkDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Staff Attendance",
                subtitle = "Daily Check-in & Punch Logs",
                onBackClick = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showMarkDialog = true },
                containerColor = Navy900,
                contentColor = Color.White,
                modifier = Modifier.testTag("mark_attendance_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Mark Attendance")
            }
        },
        containerColor = Slate50
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (attendanceList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No attendance records yet. Tap + to mark attendance.",
                        color = Navy500,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(attendanceList, key = { it.id }) { record ->
                        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                        val timeStr = timeFormat.format(Date(record.checkInTime))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
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
                                Column {
                                    Text(
                                        text = record.employeeName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Navy900
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Date: ${record.dateString} at $timeStr",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Navy500
                                    )
                                    if (record.notes.isNotBlank()) {
                                        Text(
                                            text = "Note: ${record.notes}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Navy600
                                        )
                                    }
                                }

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
                            }
                        }
                    }
                }
            }
        }
    }
}
