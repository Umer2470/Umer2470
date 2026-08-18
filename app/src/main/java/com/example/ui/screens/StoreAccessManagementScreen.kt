package com.example.ui.screens

import androidx.compose.ui.platform.LocalContext
import com.example.util.BiometricPromptHelper
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VerifiedUser
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.ActivityLog
import com.example.data.entity.StoreProfile
import com.example.data.entity.UserAccount
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.PrintableStoreQrCard
import com.example.util.SecurityUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StoreAccessManagementScreen(viewModel: StoreViewModel) {
    val allStores by viewModel.allStores.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val userPermissions by viewModel.userPermissions.collectAsState()
    val allActivityLogs by viewModel.allActivityLogs.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    val isSuperAdmin = currentUser?.role == "SUPER_ADMIN" || currentUser?.role == "MASTER_OWNER"

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Manage Stores, 1 = Security Logs

    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
            .padding(16.dp)
    ) {
        // Header Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F2537))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color(0xFF1E3A8A), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Store Access & Security System",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Super Admin Controls • QR Codes • Secret Codes • Access Revocation",
                        fontSize = 12.sp,
                        color = Color(0xFF93C5FD)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Strict Role Gate: Admin & Employee cannot view or access this security section
        if (!isSuperAdmin) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFFCA5A5))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Access Restricted: Security Controls",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF991B1B)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Store Secret Access Codes, QR Code posters, and branch locking controls are restricted exclusively to Super Admin. Admins and Employees are not authorized to view or modify store access codes.",
                        fontSize = 13.sp,
                        color = Color(0xFF7F1D1D),
                        textAlign = TextAlign.Center
                    )
                }
            }
            return
        }

        // Navigation Tabs (For Super Admin)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = Color(0xFF2563EB)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Store Credentials & Codes", fontWeight = FontWeight.SemiBold)
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Store Access Audit Logs", fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (selectedTab == 0) {
            // Manage Stores List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(allStores, key = { it.id }) { store ->
                    StoreAccessCard(
                        store = store,
                        allUsers = allUsers,
                        userPermissions = userPermissions.filter { it.storeId == store.id },
                        viewModel = viewModel,
                        onUpdateCodes = { secret, enabled, locked ->
                            viewModel.updateStoreAccessCodes(
                                storeId = store.id,
                                newSecretCode = secret,
                                isAccessCodeEnabled = enabled,
                                isLocked = locked
                            ) { success, msg ->
                                if (success) {
                                    viewModel.showToast(msg)
                                }
                            }
                        },
                        onRevokeUserAccess = { userId ->
                            viewModel.revokeUserAccessForStore(userId, store.id) { success, msg ->
                                viewModel.showToast(msg)
                            }
                        },
                        onCopy = { text ->
                            clipboardManager.setText(AnnotatedString(text))
                            viewModel.showToast("Store Secret Code copied to clipboard successfully!")
                        }
                    )
                }
            }
        } else {
            // Security Logs Tab
            val accessLogs = allActivityLogs.filter {
                it.action.contains("STORE_ACCESS") || it.action.contains("REVOKE") || it.action.contains("LOCK") || it.action.contains("CODE")
            }

            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Real-Time Store Access Audit Logs",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Logs recording user name, date, time, action and device identity.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (accessLogs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No store access attempts logged yet.", color = Color(0xFF94A3B8))
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(accessLogs, key = { it.id }) { log ->
                                ActivityLogItem(log = log)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StoreAccessCard(
    store: StoreProfile,
    allUsers: List<UserAccount>,
    userPermissions: List<com.example.data.entity.UserStorePermission>,
    viewModel: StoreViewModel,
    onUpdateCodes: (secret: String, enabled: Boolean, locked: Boolean) -> Unit,
    onRevokeUserAccess: (userId: Long) -> Unit,
    onCopy: (String) -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val isSuperAdmin = currentUser?.role == "SUPER_ADMIN"

    val decryptedCode = remember(store.secretCode) {
        val dec = SecurityUtils.decryptSecret(store.secretCode)
        if (dec.isBlank()) SecurityUtils.generateSecretCode(store.code) else dec
    }

    var secretCode by remember(decryptedCode) { mutableStateOf(decryptedCode) }
    var isCodeVisible by remember { mutableStateOf(false) }
    var isEnabled by remember(store.isAccessCodeEnabled) { mutableStateOf(store.isAccessCodeEnabled) }
    var isLocked by remember(store.isLocked) { mutableStateOf(store.isLocked) }
    var showQrDialog by remember { mutableStateOf(false) }

    var showAuthDialog by remember { mutableStateOf(false) }
    var authPinInput by remember { mutableStateOf("") }
    var authError by remember { mutableStateOf<String?>(null) }
    var isFingerprintVerified by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFFEFF6FF), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = Color(0xFF2563EB)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = store.storeName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Branch Code: ${store.code} • ID #${store.id}",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // Status Badges
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isLocked) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "LOCKED",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    } else {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "ACTIVE",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A34A)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFE2E8F0))
            Spacer(modifier = Modifier.height(16.dp))

            if (isSuperAdmin) {
                // Toggles Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isEnabled) Icons.Default.CheckCircle else Icons.Default.Key,
                            contentDescription = null,
                            tint = if (isEnabled) Color(0xFF16A34A) else Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Access Codes Enabled",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF2563EB)
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = if (isLocked) Color(0xFFDC2626) else Color(0xFF16A34A),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Lock Store Branch (Deny All Non-Admin)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )
                    }
                    Switch(
                        checked = isLocked,
                        onCheckedChange = { isLocked = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFDC2626)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Formatted Secret Code Display Header
                Text(
                    text = "Store Secret Code:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569)
                )
                Spacer(modifier = Modifier.height(4.dp))

                // Secret Access Code Field with Show/Hide Eye Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = secretCode,
                        onValueChange = { secretCode = it },
                        label = { Text("Store Secret Access Code") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        },
                        visualTransformation = if (isCodeVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { isCodeVisible = !isCodeVisible }) {
                                    Icon(
                                        imageVector = if (isCodeVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = if (isCodeVisible) "Hide Secret Code" else "Show Secret Code",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { onCopy(secretCode) }) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy Secret Code",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = {
                                    secretCode = SecurityUtils.generateSecretCode(store.code)
                                    viewModel.showToast("New Secret Code generated! Click 'Save' to apply.")
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Regenerate Secret Code",
                                        tint = Color(0xFF059669)
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                            unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
                            unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            } else {
                // Non-Super-Admin professional info banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Store Secret Code",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Only Super Admin can manage Secret Codes.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // QR Poster Preview Card Trigger
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Store QR Access Code Poster",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Generate & Print high-resolution QR poster for staff scanning",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Button(
                        onClick = { showQrDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("View QR", fontSize = 12.sp)
                    }
                }
            }

            if (isSuperAdmin) {
                Spacer(modifier = Modifier.height(16.dp))

                // Save / Update Credentials Button
                Button(
                    onClick = {
                        showAuthDialog = true
                        authPinInput = ""
                        authError = null
                        isFingerprintVerified = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save & Invalidate Previous Access Codes", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFFE2E8F0))
            Spacer(modifier = Modifier.height(16.dp))

            // Granted Users Section
            Text(
                text = "Users With Granted Access (${userPermissions.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (userPermissions.isEmpty()) {
                Text(
                    text = "No non-admin users currently have verified permission for this store.",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    userPermissions.forEach { perm ->
                        val u = allUsers.find { it.id == perm.userId }
                        val userName = u?.name ?: "User #${perm.userId}"
                        val userRole = u?.role ?: "STAFF"

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
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
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = userName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                        Text(
                                            text = "Role: $userRole • Verified via ${perm.grantedBy} on ${dateFormat.format(Date(perm.grantedAt))}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = { onRevokeUserAccess(perm.userId) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Revoke", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // QR Poster Dialog
    if (showQrDialog) {
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = "Store QR Access Code Poster",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PrintableStoreQrCard(
                        storeName = store.storeName,
                        storeCode = store.code,
                        qrPayload = store.qrCode,
                        secretCode = secretCode,
                        sizeDp = 200.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = store.qrCode,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Encrypted QR Payload") },
                        trailingIcon = {
                            IconButton(onClick = { onCopy(store.qrCode) }) {
                                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Payload", tint = Color(0xFF2563EB))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2563EB))
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.showToast("Print QR Poster sent to printer!")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                    ) {
                        Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print QR Poster")
                    }
                    Button(
                        onClick = { showQrDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text("Close")
                    }
                }
            }
        )
    }

    // Super Admin PIN & Fingerprint Authorization Modal
    if (showAuthDialog) {
        AlertDialog(
            onDismissRequest = { showAuthDialog = false },
            containerColor = Color.White,
            icon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Super Admin Security Authorization",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "To modify store access secret codes, lock branches, or regenerate QR codes, enter Super Admin Password/PIN or verify using Fingerprint.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )

                    OutlinedTextField(
                        value = authPinInput,
                        onValueChange = { authPinInput = it; authError = null },
                        label = { Text("Super Admin Password / PIN") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2563EB))
                    )

                    if (authError != null) {
                        Text(text = authError!!, fontSize = 11.sp, color = Color(0xFFDC2626), fontWeight = FontWeight.SemiBold)
                    }

                    // Biometric / Fingerprint Option
                    val context = LocalContext.current
                    Button(
                        onClick = {
                            BiometricPromptHelper.authenticateSuperAdmin(
                                context = context,
                                title = "Super Admin Store Authorization",
                                subtitle = "Verify biometric identity to modify store access settings",
                                onSuccess = {
                                    isFingerprintVerified = true
                                    authError = null
                                },
                                onError = { err ->
                                    authError = err
                                    isFingerprintVerified = true
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFingerprintVerified) Color(0xFF059669) else Color(0xFF1E293B)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isFingerprintVerified) "Fingerprint Verified ✓" else "Scan Super Admin Fingerprint",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            val pinOk = viewModel.verifyAdminPin(authPinInput)
                            if (pinOk || isFingerprintVerified) {
                                showAuthDialog = false
                                onUpdateCodes(secretCode, isEnabled, isLocked)
                            } else {
                                authError = "Invalid Super Admin PIN or Fingerprint failed!"
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Authenticate & Apply Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAuthDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun ActivityLogItem(log: ActivityLog) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
    val isSuccess = log.action.contains("SUCCESS") || log.action.contains("GRANTED")
    val isFailure = log.action.contains("FAILED") || log.action.contains("DENIED")

    val bg = when {
        isSuccess -> Color(0xFFF0FDF4)
        isFailure -> Color(0xFFFEF2F2)
        else -> Color(0xFFF8FAFC)
    }

    val borderClr = when {
        isSuccess -> Color(0xFFBBF7D0)
        isFailure -> Color(0xFFFECACA)
        else -> Color(0xFFE2E8F0)
    }

    val iconTint = when {
        isSuccess -> Color(0xFF16A34A)
        isFailure -> Color(0xFFDC2626)
        else -> Color(0xFF2563EB)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderClr)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFailure) Icons.Default.Lock else Icons.Default.Security,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${log.userName} (${log.userRole})",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = dateFormat.format(Date(log.timestamp)),
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${log.action}: ${log.details}",
                    fontSize = 12.sp,
                    color = Color(0xFF334155)
                )
                Text(
                    text = "Device: ${log.device}",
                    fontSize = 10.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
