package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.*
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
import androidx.core.content.FileProvider
import com.example.data.backup.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRecoveryScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    val connectedAccount by viewModel.backupConnectedAccount.collectAsState()
    val autoFrequency by viewModel.backupAutoFrequency.collectAsState()
    val opState by viewModel.backupOpState.collectAsState()
    val history by viewModel.backupHistory.collectAsState()
    val lastBackupTs by viewModel.lastBackupTimestamp.collectAsState()
    val lastSafetyTs by viewModel.lastSafetyBackupTimestamp.collectAsState()

    // Local dialog states
    var showConnectDialog by remember { mutableStateOf(false) }
    var connectEmailInput by remember { mutableStateOf("") }
    var connectNameInput by remember { mutableStateOf("") }
    var connectError by remember { mutableStateOf<String?>(null) }

    var selectedBackupForRestore by remember { mutableStateOf<BackupHistoryItem?>(null) }
    var restoreOwnerPin by remember { mutableStateOf("") }
    var restorePinError by remember { mutableStateOf<String?>(null) }
    var isRestoring by remember { mutableStateOf(false) }

    var selectedBackupForDelete by remember { mutableStateOf<BackupHistoryItem?>(null) }
    var deleteOwnerPin by remember { mutableStateOf("") }
    var deletePinError by remember { mutableStateOf<String?>(null) }

    var userToastMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Cloud Backup & Recovery",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Google Drive Sync & AES-256 Cryptographic Snapshots",
                            fontSize = 11.sp,
                            color = Gold400
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_back_backup_screen")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Navy900)
            )
        },
        containerColor = Slate50
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("backup_recovery_scrollable"),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Operation State Banner
            item {
                when (val state = opState) {
                    is BackupOpState.InProgress -> {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("backup_op_in_progress"),
                            colors = CardDefaults.cardColors(containerColor = Navy900),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = Gold400,
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = state.message,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                }
                                if (state.progressPercent in 0..100) {
                                    LinearProgressIndicator(
                                        progress = { state.progressPercent / 100f },
                                        color = Gold400,
                                        trackColor = Navy700,
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                    )
                                }
                            }
                        }
                    }
                    is BackupOpState.Success -> {
                        Surface(
                            color = Emerald50,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Emerald300),
                            modifier = Modifier.fillMaxWidth().testTag("backup_op_success")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald600, modifier = Modifier.size(20.dp))
                                    Text(state.message, color = Emerald800, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                IconButton(
                                    onClick = { viewModel.dismissBackupOpState() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Emerald700, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    is BackupOpState.WaitingForNetwork -> {
                        Surface(
                            color = Amber50,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Amber500),
                            modifier = Modifier.fillMaxWidth().testTag("backup_op_waiting_network")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.CloudQueue, contentDescription = null, tint = Amber700, modifier = Modifier.size(20.dp))
                                    Text(state.message, color = Amber900, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                IconButton(
                                    onClick = { viewModel.dismissBackupOpState() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Amber800, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    is BackupOpState.Error -> {
                        Surface(
                            color = Rose50,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Rose300),
                            modifier = Modifier.fillMaxWidth().testTag("backup_op_error")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Rose600, modifier = Modifier.size(20.dp))
                                    Text(state.errorMessage, color = Rose800, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                IconButton(
                                    onClick = { viewModel.dismissBackupOpState() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Rose700, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    BackupOpState.Idle -> {}
                }
            }

            // 2. Google Drive Account Connection Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("card_google_account"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
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
                                        tint = if (connectedAccount != null) Emerald700 else Navy600,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Google Drive Cloud Account",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Navy900
                                    )
                                    Text(
                                        text = if (connectedAccount != null) "Connected: ${connectedAccount!!.email}" else "Not Connected (Local backups only)",
                                        fontSize = 11.sp,
                                        color = if (connectedAccount != null) Emerald700 else Slate500
                                    )
                                }
                            }

                            if (connectedAccount != null) {
                                OutlinedButton(
                                    onClick = { viewModel.disconnectGoogleDrive() },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose600),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("btn_disconnect_google")
                                ) {
                                    Text("Disconnect", fontSize = 11.sp)
                                }
                            } else {
                                Button(
                                    onClick = {
                                        connectEmailInput = ""
                                        connectNameInput = ""
                                        connectError = null
                                        showConnectDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("btn_connect_google")
                                ) {
                                    Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Connect Account", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Status info row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Last Cloud Backup:", fontSize = 10.sp, color = Slate500)
                                Text(
                                    text = if (lastBackupTs > 0) SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(lastBackupTs)) else "Never",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = Navy800
                                )
                            }
                            Column {
                                Text("Last Safety Snapshot:", fontSize = 10.sp, color = Slate500)
                                Text(
                                    text = if (lastSafetyTs > 0) SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(lastSafetyTs)) else "None",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = Navy800
                                )
                            }
                            Column {
                                Text("Encryption Standard:", fontSize = 10.sp, color = Slate500)
                                Text(
                                    text = "AES-256-GCM",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Emerald700
                                )
                            }
                        }
                    }
                }
            }

            // 3. Primary Actions: Backup Now & Local Export
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("card_backup_actions"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Execute Database Snapshots",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Navy900
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.performCloudBackup() },
                                colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).testTag("btn_backup_now_cloud")
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Backup Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { viewModel.performLocalExport() },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).testTag("btn_backup_local_export")
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp), tint = Navy900)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Local Export", fontSize = 12.sp, color = Navy900, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            color = Slate50,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "• Cloud Backup uploads encrypted snapshots to Google Drive folder: SENTRY STORE POS/Backups\n• Local Export stores offline .sentrybackup files securely on the terminal.",
                                fontSize = 10.sp,
                                color = Slate600,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }

            // 4. Automatic Backup Schedule
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("card_auto_backup_schedule"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Automatic Scheduled Backup",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Navy900
                                )
                                Text(
                                    text = "Automated encrypted snapshots triggered in background",
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AutoBackupFrequency.values().forEach { freq ->
                                val isSelected = autoFrequency == freq
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.setBackupAutoFrequency(freq) },
                                    label = {
                                        Text(
                                            text = freq.displayName,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    modifier = Modifier.testTag("chip_freq_${freq.name}")
                                )
                            }
                        }
                    }
                }
            }

            // 5. Backup History & Restore List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Backup Snapshots & Recovery (${history.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Navy900
                    )
                    Text(
                        text = "Owner PIN Protected",
                        fontSize = 11.sp,
                        color = Gold700,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (history.isEmpty()) {
                item {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.History, contentDescription = null, tint = Slate400, modifier = Modifier.size(36.dp))
                            Text("No backup snapshots recorded yet", fontWeight = FontWeight.Bold, color = Slate600, fontSize = 13.sp)
                            Text("Tap 'Backup Now' or 'Local Export' to create your first encrypted database snapshot.", color = Slate400, fontSize = 11.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else {
                items(history) { item ->
                    BackupHistoryCard(
                        item = item,
                        onRestoreClick = {
                            selectedBackupForRestore = item
                            restoreOwnerPin = ""
                            restorePinError = null
                        },
                        onDeleteClick = {
                            selectedBackupForDelete = item
                            deleteOwnerPin = ""
                            deletePinError = null
                        }
                    )
                }
            }
        }
    }

    // ----------------------------------------------------
    // DIALOG: Connect Google Account
    // ----------------------------------------------------
    if (showConnectDialog) {
        AlertDialog(
            onDismissRequest = { showConnectDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CloudSync, contentDescription = null, tint = Navy900)
                    Text("Connect Google Account", fontWeight = FontWeight.Bold, color = Navy900, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Connect your Google Drive account for automated off-site database disaster recovery. Your Google password is NEVER requested or stored.",
                        fontSize = 12.sp,
                        color = Slate600
                    )

                    OutlinedTextField(
                        value = connectEmailInput,
                        onValueChange = {
                            connectEmailInput = it
                            connectError = null
                        },
                        label = { Text("Google Account Email") },
                        placeholder = { Text("e.g. proprietor@gmail.com") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_connect_google_email")
                    )

                    OutlinedTextField(
                        value = connectNameInput,
                        onValueChange = { connectNameInput = it },
                        label = { Text("Account / Store Display Name (Optional)") },
                        placeholder = { Text("e.g. SENTRY STORE OWNER") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_connect_google_name")
                    )

                    if (connectError != null) {
                        Text(connectError!!, color = Rose600, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val email = connectEmailInput.trim()
                        if (email.isBlank() || !email.contains("@")) {
                            connectError = "Please enter a valid Google Account email."
                        } else {
                            val success = viewModel.connectGoogleDrive(email, connectNameInput)
                            if (success) {
                                showConnectDialog = false
                            } else {
                                connectError = "Failed to connect Google Account."
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    modifier = Modifier.testTag("btn_confirm_connect_google")
                ) {
                    Text("Connect Account")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConnectDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ----------------------------------------------------
    // DIALOG: Authorize & Restore Database
    // ----------------------------------------------------
    if (selectedBackupForRestore != null) {
        val targetItem = selectedBackupForRestore!!
        AlertDialog(
            onDismissRequest = {
                if (!isRestoring) selectedBackupForRestore = null
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Rose600)
                    Text("Authorize Database Restore", fontWeight = FontWeight.Bold, color = Navy900, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = Amber50,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Amber500),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("⚠️ RESTORE WARNING:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Amber900)
                            Text("Restoring will replace current business tables with records from:", fontSize = 11.sp, color = Amber900)
                            Text("• Snapshot: ${targetItem.filename}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Navy900)
                            Text("• Date: ${targetItem.formattedDate}", fontSize = 11.sp, color = Slate700)
                            Text("• Records: ${targetItem.totalRecords}", fontSize = 11.sp, color = Slate700)
                            Text("An automatic Pre-Restore Safety Snapshot will be created immediately before data is replaced.", fontSize = 10.sp, color = Emerald700, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Text(
                        text = "Owner Verification Required. Only the dedicated Owner PIN / Password can authorize recovery (Cashier PINs and Owner Key cannot unlock restore).",
                        fontSize = 11.sp,
                        color = Slate600
                    )

                    OutlinedTextField(
                        value = restoreOwnerPin,
                        onValueChange = {
                            restoreOwnerPin = it
                            restorePinError = null
                        },
                        label = { Text("Owner Security PIN / Password") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        isError = restorePinError != null,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_restore_owner_pin")
                    )

                    if (restorePinError != null) {
                        Text(restorePinError!!, color = Rose600, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanPin = restoreOwnerPin.trim()
                        if (cleanPin.isBlank()) {
                            restorePinError = "Please enter your dedicated Owner Security PIN."
                            return@Button
                        }
                        if (cleanPin.startsWith("OWNER-KEY-", ignoreCase = true) || cleanPin == viewModel.getOwnerSecurityKey()) {
                            restorePinError = "Owner Key cannot authorize recovery. Enter your private Owner PIN."
                            return@Button
                        }
                        if (!viewModel.verifyOwnerSecurityCode(cleanPin)) {
                            restorePinError = "Invalid Owner PIN. Access Denied."
                            return@Button
                        }

                        val targetFile = if (targetItem.localFilePath != null) {
                            File(targetItem.localFilePath)
                        } else {
                            File(context.filesDir, "backups/${targetItem.filename}")
                        }

                        isRestoring = true
                        viewModel.performRestore(targetFile, cleanPin) { result ->
                            isRestoring = false
                            if (result.isSuccess) {
                                selectedBackupForRestore = null
                                userToastMessage = "Database recovered successfully (${result.getOrNull()} records)."
                            } else {
                                restorePinError = result.exceptionOrNull()?.localizedMessage ?: "Restore failed."
                            }
                        }
                    },
                    enabled = !isRestoring,
                    colors = ButtonDefaults.buttonColors(containerColor = Rose700),
                    modifier = Modifier.testTag("btn_confirm_restore_database")
                ) {
                    if (isRestoring) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restoring...")
                    } else {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Authorize & Restore")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { selectedBackupForRestore = null },
                    enabled = !isRestoring
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // ----------------------------------------------------
    // DIALOG: Delete Backup
    // ----------------------------------------------------
    if (selectedBackupForDelete != null) {
        val targetItem = selectedBackupForDelete!!
        AlertDialog(
            onDismissRequest = { selectedBackupForDelete = null },
            title = { Text("Delete Backup Snapshot?", fontWeight = FontWeight.Bold, color = Navy900) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Are you sure you want to delete ${targetItem.filename}? This action requires Owner PIN verification.", fontSize = 12.sp, color = Slate600)
                    OutlinedTextField(
                        value = deleteOwnerPin,
                        onValueChange = {
                            deleteOwnerPin = it
                            deletePinError = null
                        },
                        label = { Text("Owner PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        isError = deletePinError != null,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("input_delete_backup_pin")
                    )
                    if (deletePinError != null) {
                        Text(deletePinError!!, color = Rose600, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val success = viewModel.deleteBackup(targetItem.id, deleteOwnerPin)
                        if (success) {
                            selectedBackupForDelete = null
                            userToastMessage = "Backup snapshot deleted."
                        } else {
                            deletePinError = "Invalid Owner PIN."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose700),
                    modifier = Modifier.testTag("btn_confirm_delete_backup")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedBackupForDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BackupHistoryCard(
    item: BackupHistoryItem,
    onRestoreClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().testTag("backup_item_${item.id}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val (icon, tint) = when (item.destination) {
                        BackupDestination.GOOGLE_DRIVE -> Icons.Default.CloudDone to Emerald600
                        BackupDestination.LOCAL_STORAGE -> Icons.Default.Folder to Navy700
                        BackupDestination.SAFETY_BACKUP -> Icons.Default.Shield to Gold600
                    }
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))

                    Text(
                        text = item.filename,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Navy900,
                        maxLines = 1
                    )
                }

                Surface(
                    color = when (item.status) {
                        BackupStatusType.SUCCESS -> Emerald50
                        BackupStatusType.QUEUED_OFFLINE -> Amber50
                        BackupStatusType.IN_PROGRESS -> Slate100
                        BackupStatusType.FAILED -> Rose50
                    },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = item.status.name,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (item.status) {
                            BackupStatusType.SUCCESS -> Emerald700
                            BackupStatusType.QUEUED_OFFLINE -> Amber700
                            BackupStatusType.IN_PROGRESS -> Slate600
                            BackupStatusType.FAILED -> Rose700
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.formattedDate} • ${item.formattedSize} • ${item.totalRecords} records",
                    fontSize = 11.sp,
                    color = Slate500
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onRestoreClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("btn_restore_${item.id}")
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restore", fontSize = 11.sp)
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(28.dp).testTag("btn_delete_${item.id}")
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Slate400, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
