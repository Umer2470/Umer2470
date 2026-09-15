package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.backup.AutoBackupFrequency
import com.example.data.backup.BackupOpState
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BackupRecoverySettingsCard(
    viewModel: StoreViewModel,
    onNavigateToHub: () -> Unit
) {
    val connectedAccount by viewModel.backupConnectedAccount.collectAsState()
    val autoFrequency by viewModel.backupAutoFrequency.collectAsState()
    val opState by viewModel.backupOpState.collectAsState()
    val lastBackupTs by viewModel.lastBackupTimestamp.collectAsState()
    val lastSafetyTs by viewModel.lastSafetyBackupTimestamp.collectAsState()
    val history by viewModel.backupHistory.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("settings_backup_recovery_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (connectedAccount != null) Emerald100 else Slate100),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = null,
                            tint = if (connectedAccount != null) Emerald700 else Navy700,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Google Drive Backup & Recovery",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Navy900
                        )
                        Text(
                            text = if (connectedAccount != null) "Connected: ${connectedAccount!!.email}" else "Not connected to Google Drive",
                            fontSize = 11.sp,
                            color = if (connectedAccount != null) Emerald700 else Slate500
                        )
                    }
                }

                Surface(
                    color = if (connectedAccount != null) Emerald50 else Slate100,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (connectedAccount != null) "Cloud Active" else "Offline Only",
                        color = if (connectedAccount != null) Emerald700 else Slate600,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Operation State Feedback (if active)
            when (val state = opState) {
                is BackupOpState.InProgress -> {
                    Surface(
                        color = Slate100,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Navy900)
                            Text(state.message, fontSize = 11.sp, color = Navy900, fontWeight = FontWeight.Medium)
                        }
                    }
                }
                is BackupOpState.Success -> {
                    Surface(
                        color = Emerald50,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Emerald300),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald600, modifier = Modifier.size(16.dp))
                            Text(state.message, fontSize = 11.sp, color = Emerald800, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                is BackupOpState.WaitingForNetwork -> {
                    Surface(
                        color = Amber50,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CloudQueue, contentDescription = null, tint = Amber700, modifier = Modifier.size(16.dp))
                            Text(state.message, fontSize = 11.sp, color = Amber900, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                is BackupOpState.Error -> {
                    Surface(
                        color = Rose50,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Rose600, modifier = Modifier.size(16.dp))
                            Text(state.errorMessage, fontSize = 11.sp, color = Rose800, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                BackupOpState.Idle -> {}
            }

            // Quick Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Last Cloud Sync", fontSize = 10.sp, color = Slate500)
                    Text(
                        text = if (lastBackupTs > 0) SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(lastBackupTs)) else "Never",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Navy900
                    )
                }
                Column {
                    Text("Snapshots Stored", fontSize = 10.sp, color = Slate500)
                    Text(
                        text = "${history.size} Backups",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Navy900
                    )
                }
                Column {
                    Text("Encryption", fontSize = 10.sp, color = Slate500)
                    Text(
                        text = "AES-256-GCM",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald700
                    )
                }
            }

            // Automatic Backup Schedule Chips
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Automatic Backup Schedule:", fontSize = 11.sp, color = Slate600, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AutoBackupFrequency.values().forEach { freq ->
                        val isSelected = autoFrequency == freq
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setBackupAutoFrequency(freq) },
                            label = {
                                Text(
                                    text = freq.displayName,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }

            // Primary Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.performCloudBackup() },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("btn_settings_backup_now")
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Backup Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onNavigateToHub,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).testTag("btn_settings_open_backup_hub")
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp), tint = Navy900)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restore & Hub", fontSize = 12.sp, color = Navy900, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
