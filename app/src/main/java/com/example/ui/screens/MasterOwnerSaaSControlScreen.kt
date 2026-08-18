package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.TenantAccount
import com.example.data.entity.SubscriptionPlan
import com.example.ui.viewmodel.StoreViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterOwnerSaaSControlScreen(viewModel: StoreViewModel) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val allTenants by viewModel.allTenants.collectAsState()
    val plans by viewModel.allSubscriptionPlans.collectAsState()
    val currentTenant by viewModel.currentTenant.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusFilter by remember { mutableStateOf("ALL") }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showNoticeDialog by remember { mutableStateOf(false) }
    var tenantToRenew by remember { mutableStateOf<TenantAccount?>(null) }
    var tenantToDelete by remember { mutableStateOf<TenantAccount?>(null) }
    var createdLicenseKeyDialog by remember { mutableStateOf<String?>(null) }

    val filteredTenants = remember(allTenants, searchQuery, selectedStatusFilter) {
        allTenants.filter { tenant ->
            !tenant.isMasterOwnerAccount &&
            (searchQuery.isBlank() ||
             tenant.businessName.contains(searchQuery, ignoreCase = true) ||
             tenant.ownerName.contains(searchQuery, ignoreCase = true) ||
             tenant.tenantCode.contains(searchQuery, ignoreCase = true) ||
             tenant.licenseKey.contains(searchQuery, ignoreCase = true)) &&
            (selectedStatusFilter == "ALL" || tenant.status == selectedStatusFilter)
        }
    }

    val activeCount = allTenants.count { !it.isMasterOwnerAccount && it.status == "ACTIVE" && it.isLicenseActive() }
    val suspendedCount = allTenants.count { !it.isMasterOwnerAccount && (it.status == "SUSPENDED" || it.status == "DEACTIVATED") }
    val expiredCount = allTenants.count { !it.isMasterOwnerAccount && !it.isLicenseActive() && it.status == "ACTIVE" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0F172A)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeveloperMode,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Master Developer SaaS Control",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "Multi-Tenant Commercial Licensing Center",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                },
                actions = {
                    Button(
                        onClick = { showNoticeDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Broadcast Notice", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Customer Account", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8FAFC))
        ) {
            // Metrics Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Total Customers",
                    value = "${allTenants.count { !it.isMasterOwnerAccount }}",
                    subtext = "Active SaaS Tenants",
                    icon = Icons.Default.Business,
                    color = Color(0xFF2563EB),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Active Licenses",
                    value = "$activeCount",
                    subtext = "Valid Subscriptions",
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Suspended / Expired",
                    value = "${suspendedCount + expiredCount}",
                    subtext = "Requires Attention",
                    icon = Icons.Default.Warning,
                    color = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f)
                )
            }

            // Search and Status Filters
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search by customer name, code, owner, or license key...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Status Filter:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        listOf("ALL", "ACTIVE", "SUSPENDED", "DEACTIVATED").forEach { status ->
                            FilterChip(
                                selected = selectedStatusFilter == status,
                                onClick = { selectedStatusFilter = status },
                                label = { Text(status, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tenant Cards List
            if (filteredTenants.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No customer tenant accounts found", color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { showCreateDialog = true }) {
                            Text("Create First Customer Account")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredTenants) { tenant ->
                        TenantAccountCard(
                            tenant = tenant,
                            isCurrentlyActiveTenant = tenant.id == currentTenant?.id,
                            onSwitchWorkspace = {
                                viewModel.switchTenant(tenant.id)
                            },
                            onCopyKey = {
                                clipboardManager.setText(AnnotatedString(tenant.licenseKey))
                                Toast.makeText(context, "License Key copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            onRenew = { tenantToRenew = tenant },
                            onResetKey = {
                                viewModel.resetTenantLicense(tenant.id)
                            },
                            onToggleStatus = {
                                val nextStatus = if (tenant.status == "ACTIVE") "SUSPENDED" else "ACTIVE"
                                viewModel.updateTenantStatus(tenant.id, nextStatus)
                            },
                            onDelete = { tenantToDelete = tenant },
                            onBlockToggle = {
                                if (tenant.isBlocked) viewModel.unblockTenantCopy(tenant.id) else viewModel.blockTenantCopy(tenant.id)
                            },
                            onForceLogout = {
                                viewModel.forceLogoutTenantDevices(tenant.id)
                            },
                            onResetBinding = {
                                viewModel.resetTenantActivation(tenant.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal: Create New Customer Tenant
    if (showCreateDialog) {
        CreateCustomerTenantDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { code, bName, oName, email, phone, plan, shops, users, customDays ->
                viewModel.createCustomerTenant(
                    tenantCode = code,
                    businessName = bName,
                    ownerName = oName,
                    email = email,
                    phone = phone,
                    planType = plan,
                    maxShops = shops,
                    maxUsers = users,
                    customExpiryDays = customDays
                ) { success, msg, key ->
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    if (success) {
                        showCreateDialog = false
                        createdLicenseKeyDialog = key
                    }
                }
            }
        )
    }

    // Modal: Generated License Key Confirmation
    createdLicenseKeyDialog?.let { key ->
        AlertDialog(
            onDismissRequest = { createdLicenseKeyDialog = null },
            title = { Text("Commercial License Key Generated!") },
            text = {
                Column {
                    Text("New Commercial SaaS License Key for customer:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = key,
                            modifier = Modifier.padding(12.dp),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF0F172A),
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Provide this license key to the customer to activate their application.", fontSize = 12.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(onClick = {
                    clipboardManager.setText(AnnotatedString(key))
                    Toast.makeText(context, "Key copied!", Toast.LENGTH_SHORT).show()
                    createdLicenseKeyDialog = null
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Key & Close")
                }
            }
        )
    }

    // Modal: Renew / Extend License
    tenantToRenew?.let { tenant ->
        var daysInput by remember { mutableStateOf("365") }
        AlertDialog(
            onDismissRequest = { tenantToRenew = null },
            title = { Text("Extend Customer License") },
            text = {
                Column {
                    Text("Extend license validity for ${tenant.businessName}:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = daysInput,
                        onValueChange = { daysInput = it },
                        label = { Text("Add Duration in Days") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { daysInput = "30" }, modifier = Modifier.weight(1f)) { Text("+30 Days") }
                        Button(onClick = { daysInput = "365" }, modifier = Modifier.weight(1f)) { Text("+1 Year") }
                        Button(onClick = { daysInput = "36500" }, modifier = Modifier.weight(1f)) { Text("Lifetime") }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val days = daysInput.toIntOrNull() ?: 365
                    viewModel.renewTenantLicense(tenant.id, days)
                    tenantToRenew = null
                }) {
                    Text("Confirm Extension")
                }
            },
            dismissButton = {
                TextButton(onClick = { tenantToRenew = null }) { Text("Cancel") }
            }
        )
    }

    // Modal: Delete Customer Confirmation
    tenantToDelete?.let { tenant ->
        AlertDialog(
            onDismissRequest = { tenantToDelete = null },
            title = { Text("Delete Customer Tenant Account?") },
            text = {
                Text("Are you sure you want to delete '${tenant.businessName}' (${tenant.tenantCode})? All isolated store data, products, and user accounts for this customer will be permanently removed.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomerTenant(tenant.id)
                        tenantToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { tenantToDelete = null }) { Text("Cancel") }
            }
        )
    }

    // Modal: Broadcast Notice
    if (showNoticeDialog) {
        var noticeText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNoticeDialog = false },
            title = { Text("Push System Announcement / App Update") },
            text = {
                Column {
                    Text("Broadcast notification message to all customer tenant dashboards:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = noticeText,
                        onValueChange = { noticeText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. System maintenance scheduled for tonight at 2 AM or New POS feature update released!") },
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (noticeText.isNotBlank()) {
                        viewModel.pushGlobalSystemNotice(noticeText)
                        showNoticeDialog = false
                    }
                }) {
                    Text("Push Announcement")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoticeDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                Text(subtext, fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun TenantAccountCard(
    tenant: TenantAccount,
    isCurrentlyActiveTenant: Boolean,
    onSwitchWorkspace: () -> Unit,
    onCopyKey: () -> Unit,
    onRenew: () -> Unit,
    onResetKey: () -> Unit,
    onToggleStatus: () -> Unit,
    onDelete: () -> Unit,
    onBlockToggle: () -> Unit = {},
    onForceLogout: () -> Unit = {},
    onResetBinding: () -> Unit = {}
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val expiryString = remember(tenant.licenseExpiryDate) { dateFormat.format(Date(tenant.licenseExpiryDate)) }
    val daysRemaining = tenant.getDaysRemaining()
    val isExpired = !tenant.isLicenseActive()

    val statusBg = when {
        tenant.status != "ACTIVE" -> Color(0xFFFEF2F2)
        isExpired -> Color(0xFFFFFBEB)
        else -> Color(0xFFECFDF5)
    }

    val statusColor = when {
        tenant.status != "ACTIVE" -> Color(0xFFEF4444)
        isExpired -> Color(0xFFD97706)
        else -> Color(0xFF10B981)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentlyActiveTenant) Color(0xFFF0F9FF) else Color.White
        ),
        border = BorderStroke(
            1.5.dp,
            if (isCurrentlyActiveTenant) Color(0xFF0284C7) else Color(0xFFE2E8F0)
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF2563EB).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Store, contentDescription = null, tint = Color(0xFF2563EB))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = tenant.businessName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Color(0xFFE2E8F0),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = tenant.tenantCode,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF334155)
                                )
                            }
                        }
                        Text(
                            text = "Owner: ${tenant.ownerName} • ${tenant.phone}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (isExpired) "EXPIRED" else tenant.status,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // License Key Container
            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tenant.licenseKey,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF1E293B)
                        )
                    }

                    IconButton(onClick = onCopyKey, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Meta Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Plan: ${tenant.planType} • Payment: ${tenant.paymentStatus} • Max Shops: ${tenant.maxShops} • Max Users: ${tenant.maxUsers}",
                    fontSize = 11.sp,
                    color = Color(0xFF475569)
                )
                Text(
                    text = "Expires: $expiryString ($daysRemaining days left)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isExpired || tenant.isBlocked) Color.Red else Color(0xFF0F172A)
                )
            }

            if (tenant.isBlocked) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⛔ BLOCKED ILLEGAL COPY / STOLEN KEY",
                        color = Color(0xFFDC2626),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Divider(color = Color(0xFFF1F5F9))

            Spacer(modifier = Modifier.height(8.dp))

            // Developer Control Actions
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onSwitchWorkspace,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isCurrentlyActiveTenant) Color(0xFF0284C7) else Color(0xFF0F172A)
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(if (isCurrentlyActiveTenant) "Active Workspace" else "Inspect Customer Workspace", fontSize = 11.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = onRenew) {
                            Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Renew", fontSize = 11.sp)
                        }

                        TextButton(onClick = onResetKey) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset Key", fontSize = 11.sp)
                        }

                        TextButton(onClick = onToggleStatus) {
                            Icon(
                                imageVector = if (tenant.status == "ACTIVE") Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (tenant.status == "ACTIVE") Color(0xFFD97706) else Color(0xFF10B981)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (tenant.status == "ACTIVE") "Suspend" else "Activate", fontSize = 11.sp)
                        }

                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Advanced SaaS Security Controls Row
                val blockBtnColor = if (tenant.isBlocked) Color(0xFF10B981) else Color(0xFFDC2626)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = onBlockToggle,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = blockBtnColor
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(if (tenant.isBlocked) "Unblock" else "Block Copy", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onForceLogout,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Force Logout", fontSize = 10.sp)
                    }

                    OutlinedButton(
                        onClick = onResetBinding,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Reset Device", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CreateCustomerTenantDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String, String, String, String, Int, Int, Int?) -> Unit
) {
    var tenantCode by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var planType by remember { mutableStateOf("ANNUAL") }
    var maxShops by remember { mutableStateOf("10") }
    var maxUsers by remember { mutableStateOf("25") }
    var customDays by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Customer SaaS Tenant Account") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = tenantCode,
                    onValueChange = { tenantCode = it },
                    label = { Text("Tenant Unique Code (e.g. BAHRIA_HARDWARE)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = businessName,
                    onValueChange = { businessName = it },
                    label = { Text("Business / Store Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Customer Owner Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = maxShops,
                        onValueChange = { maxShops = it },
                        label = { Text("Max Shops") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = maxUsers,
                        onValueChange = { maxUsers = it },
                        label = { Text("Max Users") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text("Subscription Plan:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("TRIAL", "MONTHLY", "ANNUAL", "LIFETIME").forEach { plan ->
                        FilterChip(
                            selected = planType == plan,
                            onClick = { planType = plan },
                            label = { Text(plan, fontSize = 10.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (tenantCode.isNotBlank() && businessName.isNotBlank() && ownerName.isNotBlank()) {
                        onCreate(
                            tenantCode,
                            businessName,
                            ownerName,
                            email,
                            phone,
                            planType,
                            maxShops.toIntOrNull() ?: 10,
                            maxUsers.toIntOrNull() ?: 25,
                            customDays.toIntOrNull()
                        )
                    }
                }
            ) {
                Text("Provision Account & Generate Key")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
