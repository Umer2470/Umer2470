package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.config.ApiConfig
import com.example.data.api.license.LicenseStateCache
import com.example.data.api.model.ApiResult
import com.example.data.api.network.ApiClient
import com.example.data.api.network.ConnectionState
import com.example.data.api.network.DetailedConnectionStatus
import com.example.data.api.network.NetworkConnectivityMonitor
import com.example.data.api.repository.DeveloperApiRepository
import com.example.data.api.security.AppActivationManager
import com.example.data.api.security.SecureIdentityManager
import com.example.data.api.security.SecureTokenManager
import com.example.data.api.sync.SyncLogLevel
import com.example.data.api.sync.SyncLogItem
import com.example.data.api.sync.SyncManager
import com.example.data.api.sync.SyncState
import com.example.ui.viewmodel.StoreViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class InstallationDeviceRecord(
    val installationId: String,
    val customerId: String,
    val storeId: Long,
    val appVersion: String,
    val activationStatus: String,
    val activatedAt: Long,
    val activatedBy: String,
    val lastSeen: Long,
    val deviceModel: String,
    val androidVersion: String,
    val licensePlan: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperPanelScreen(
    viewModel: StoreViewModel,
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val identityManager = remember { SecureIdentityManager.getInstance(context) }
    val tokenManager = remember { SecureTokenManager.getInstance(context) }
    val licenseCache = remember { LicenseStateCache.getInstance(context) }
    val syncManager = remember { SyncManager.getInstance(context) }
    val activationManager = remember { AppActivationManager.getInstance(context) }
    val devRepository = remember { DeveloperApiRepository(context) }
    val networkMonitor = remember { NetworkConnectivityMonitor.getInstance(context) }

    val connectionStatus by networkMonitor.connectionStatus.collectAsState()
    val syncLogs by syncManager.syncLogs.collectAsState()
    val pendingSyncCount by syncManager.pendingSyncCount.collectAsState()
    val lastSyncError by syncManager.lastSyncError.collectAsState()
    val isSyncing by syncManager.isSyncing.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Installations", "Code Generator", "API Config & Tests", "License", "Sync & Diagnostics")

    // State indicators
    var currentBaseUrl by remember { mutableStateOf(ApiConfig.getBaseUrl()) }
    var showEditUrlDialog by remember { mutableStateOf(false) }
    var customUrlInput by remember { mutableStateOf(currentBaseUrl) }

    var isCheckingConnection by remember { mutableStateOf(false) }

    var activeTestEndpoint by remember { mutableStateOf<String?>(null) }
    var endpointTestResults by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isRunningTest by remember { mutableStateOf(false) }

    var cachedLicenseStatus by remember { mutableStateOf(licenseCache.getCachedLicenseStatus()) }
    var isTestingLicense by remember { mutableStateOf(false) }

    var showClearCacheDialog by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()) }

    // Initial connection ping check
    LaunchedEffect(Unit) {
        networkMonitor.checkConnectionNow()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Top Header Banner
        Surface(
            color = Color(0xFF0F2537),
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onNavigateBack != null) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Icon(
                            imageVector = Icons.Default.DeveloperMode,
                            contentDescription = "Developer Mode",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "CH UMER POS.03080018035",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "OFFLINE-FIRST POS • ONLINE DEVELOPER & SYNC",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFFD700)
                            )
                        }
                    }

                    // Live 3-State Server Reachability Badge
                    AssistChip(
                        onClick = {
                            scope.launch {
                                isCheckingConnection = true
                                val res = networkMonitor.pingServer()
                                isCheckingConnection = false
                                Toast.makeText(
                                    context,
                                    res.statusMessage,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isCheckingConnection) Color.Yellow
                                            else when (connectionStatus.state) {
                                                ConnectionState.ONLINE_CONNECTED -> Color(0xFF10B981)
                                                ConnectionState.ONLINE_UNREACHABLE -> Color(0xFFF59E0B)
                                                ConnectionState.OFFLINE -> Color(0xFFEF4444)
                                            }
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isCheckingConnection) "PINGING..."
                                    else when (connectionStatus.state) {
                                        ConnectionState.ONLINE_CONNECTED -> "ONLINE (${connectionStatus.latencyMs}ms)"
                                        ConnectionState.ONLINE_UNREACHABLE -> "SERVER UNREACHABLE"
                                        ConnectionState.OFFLINE -> "OFFLINE"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = when (connectionStatus.state) {
                                ConnectionState.ONLINE_CONNECTED -> Color(0xFF064E3B)
                                ConnectionState.ONLINE_UNREACHABLE -> Color(0xFF78350F)
                                ConnectionState.OFFLINE -> Color(0xFF450A0A)
                            },
                            labelColor = Color.White
                        )
                    )
                }
            }
        }

        // Tab Navigation
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF1E293B),
            contentColor = Color.White,
            edgePadding = 12.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTab == index) Color(0xFFFFD700) else Color.LightGray,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (selectedTab) {
                0 -> DeveloperOverviewTab(
                    identityManager = identityManager,
                    tokenManager = tokenManager,
                    licenseCache = licenseCache,
                    activationManager = activationManager,
                    networkMonitor = networkMonitor,
                    syncManager = syncManager,
                    dateFormat = dateFormat,
                    onCopyInstallationId = {
                        clipboardManager.setText(AnnotatedString(identityManager.getInstallationId()))
                        Toast.makeText(context, "Installation ID copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )
                1 -> DeveloperInstallationsTab(
                    context = context,
                    identityManager = identityManager,
                    activationManager = activationManager,
                    licenseCache = licenseCache,
                    dateFormat = dateFormat,
                    viewModel = viewModel
                )
                2 -> DeveloperCodeGeneratorTab(
                    identityManager = identityManager,
                    activationManager = activationManager,
                    viewModel = viewModel,
                    onCopyCode = { code ->
                        clipboardManager.setText(AnnotatedString(code))
                        Toast.makeText(context, "Activation Code copied to clipboard", Toast.LENGTH_SHORT).show()
                    }
                )
                3 -> DeveloperApiConfigAndTestsTab(
                    currentBaseUrl = currentBaseUrl,
                    isRunningTest = isRunningTest,
                    activeTestEndpoint = activeTestEndpoint,
                    endpointTestResults = endpointTestResults,
                    connectionStatus = connectionStatus,
                    onEditUrlClick = {
                        customUrlInput = currentBaseUrl
                        showEditUrlDialog = true
                    },
                    onResetUrlClick = {
                        ApiConfig.resetToDefault(context)
                        ApiClient.getInstance(context).notifyBaseUrlChanged()
                        currentBaseUrl = ApiConfig.getBaseUrl()
                        Toast.makeText(context, "Reset to default production URL", Toast.LENGTH_SHORT).show()
                    },
                    onRunPingTest = {
                        scope.launch {
                            val res = networkMonitor.pingServer()
                            Toast.makeText(context, res.statusMessage, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onRunEndpointTest = { endpoint ->
                        scope.launch {
                            isRunningTest = true
                            activeTestEndpoint = endpoint
                            val startTime = System.currentTimeMillis()

                            val resultString = when (endpoint) {
                                "GET /health" -> {
                                    when (val res = devRepository.checkHealth()) {
                                        is ApiResult.Success -> "HTTP 200 OK (${System.currentTimeMillis() - startTime}ms)\nStatus: ${res.data.status}\nMessage: ${res.data.message ?: "Healthy"}"
                                        is ApiResult.Offline -> "Offline Mode: Server unreachable or no network connection"
                                        is ApiResult.Error -> "Error: ${res.message} (HTTP ${res.code})"
                                    }
                                }
                                "GET /config" -> {
                                    when (val res = devRepository.getServerConfig()) {
                                        is ApiResult.Success -> "HTTP 200 OK (${System.currentTimeMillis() - startTime}ms)\nAPI Version: ${res.data.apiVersion}\nStatus: ${res.data.status}"
                                        is ApiResult.Offline -> "Offline Mode: Using local fallback configuration"
                                        is ApiResult.Error -> "Error: ${res.message} (HTTP ${res.code})"
                                    }
                                }
                                "POST /installation/register" -> {
                                    when (val res = devRepository.registerInstallation()) {
                                        is ApiResult.Success -> "HTTP 200 OK (${System.currentTimeMillis() - startTime}ms)\nRegistered: ${res.data.installationId}\nToken: ${if (res.data.accessToken != null) "Received & Cached in KeyStore" else "None"}"
                                        is ApiResult.Offline -> "Offline Mode: Installation registered locally (${identityManager.getInstallationId()})"
                                        is ApiResult.Error -> "Error: ${res.message} (HTTP ${res.code})"
                                    }
                                }
                                "POST /installation/activate" -> {
                                    val testCode = AppActivationManager.generateActivationCode(identityManager.getInstallationId())
                                    when (val res = devRepository.activateInstallation(testCode)) {
                                        is ApiResult.Success -> "HTTP 200 OK (${System.currentTimeMillis() - startTime}ms)\nStatus: ${res.data.status}\nMessage: ${res.data.message}\nPlan: ${res.data.planType}"
                                        is ApiResult.Offline -> "Offline Mode: Server unreachable. Cryptographic verification fallback active."
                                        is ApiResult.Error -> "Error: ${res.message} (HTTP ${res.code})"
                                    }
                                }
                                "POST /license/validate" -> {
                                    when (val res = devRepository.validateLicense("COMMERCE-PRO-KEY-2026")) {
                                        is ApiResult.Success -> "HTTP 200 OK (${System.currentTimeMillis() - startTime}ms)\nLicense: ${res.data.status.uppercase()}\nPlan: ${res.data.planType}\nMax Shops: ${res.data.maxShops}"
                                        is ApiResult.Offline -> "Offline Mode: Operating with cached active commercial license"
                                        is ApiResult.Error -> "Error: ${res.message} (HTTP ${res.code})"
                                    }
                                }
                                "POST /license/heartbeat" -> {
                                    when (val res = devRepository.sendHeartbeat()) {
                                        is ApiResult.Success -> "HTTP 200 OK (${System.currentTimeMillis() - startTime}ms)\nHeartbeat: ${res.data.message}\nStatus: ${res.data.status}"
                                        is ApiResult.Offline -> "Offline Mode: Heartbeat queued for next online connection"
                                        is ApiResult.Error -> "Error: ${res.message} (HTTP ${res.code})"
                                    }
                                }
                                "POST /app/version" -> {
                                    when (val res = devRepository.checkAppVersion()) {
                                        is ApiResult.Success -> "HTTP 200 OK (${System.currentTimeMillis() - startTime}ms)\nLatest Version: ${res.data.latestVersion}\nUpdate Required: ${res.data.isUpdateRequired}"
                                        is ApiResult.Offline -> "Offline Mode: Current App Version ${identityManager.getAppVersion()}"
                                        is ApiResult.Error -> "Error: ${res.message} (HTTP ${res.code})"
                                    }
                                }
                                else -> "Unknown endpoint"
                            }

                            endpointTestResults = endpointTestResults.toMutableMap().apply {
                                put(endpoint, resultString)
                            }
                            isRunningTest = false
                        }
                    }
                )
                4 -> DeveloperLicenseTab(
                    cachedLicenseStatus = cachedLicenseStatus,
                    isTestingLicense = isTestingLicense,
                    licenseCache = licenseCache,
                    dateFormat = dateFormat,
                    onValidateLicenseClick = {
                        scope.launch {
                            isTestingLicense = true
                            val res = devRepository.validateLicense("COMMERCE-PRO-KEY-2026")
                            isTestingLicense = false
                            cachedLicenseStatus = licenseCache.getCachedLicenseStatus()
                            Toast.makeText(
                                context,
                                when (res) {
                                    is ApiResult.Success -> "License validated: ${res.data.status}"
                                    is ApiResult.Offline -> "Offline Mode: Using cached active license"
                                    is ApiResult.Error -> "License check: ${res.message}"
                                },
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
                5 -> DeveloperSyncAndDiagnosticsTab(
                    context = context,
                    syncManager = syncManager,
                    isManualSyncing = isSyncing,
                    pendingSyncCount = pendingSyncCount,
                    lastSyncError = lastSyncError,
                    syncLogs = syncLogs,
                    dateFormat = dateFormat,
                    onTriggerSyncClick = {
                        scope.launch {
                            val success = syncManager.performManualSync()
                            Toast.makeText(
                                context,
                                if (success) "Cloud Sync completed successfully!" else "Sync deferred (Offline Mode). Local data safe.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onTriggerCloudBackupClick = {
                        scope.launch {
                            val backupJson = "{\"backupTimestamp\":${System.currentTimeMillis()},\"app\":\"CH_UMER_POS\"}"
                            val success = syncManager.performCloudBackup(backupJson)
                            Toast.makeText(
                                context,
                                if (success) "Cloud Backup Snapshot saved to server!" else "Cloud Backup deferred: Server unreachable. Local data safe.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onLocalSnapshotClick = {
                        syncManager.recordLocalBackup()
                        Toast.makeText(context, "Local encrypted backup snapshot saved.", Toast.LENGTH_SHORT).show()
                    },
                    onClearCacheClick = { showClearCacheDialog = true }
                )
            }
        }
    }

    // Dialog: Edit Base URL
    if (showEditUrlDialog) {
        AlertDialog(
            onDismissRequest = { showEditUrlDialog = false },
            title = { Text("Configure Developer Server URL") },
            text = {
                Column {
                    Text("Specify HTTPS URL for CH UMER POS.03080018035 Control Center:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customUrlInput,
                        onValueChange = { customUrlInput = it },
                        label = { Text("Base URL") },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Must start with https:// (or http://) and end with /",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customUrlInput.isNotBlank() && (customUrlInput.startsWith("http://") || customUrlInput.startsWith("https://"))) {
                            ApiConfig.setBaseUrl(context, customUrlInput)
                            ApiClient.getInstance(context).notifyBaseUrlChanged()
                            currentBaseUrl = ApiConfig.getBaseUrl()
                            showEditUrlDialog = false
                            Toast.makeText(context, "API Base URL updated and persisted", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Invalid URL format (must begin with http:// or https://)", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save & Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditUrlDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Clear Cache Confirmation
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear Temporary Cache") },
            text = { Text("This will clear temporary network response logs and cache files. Local SQLite Room database records will NOT be touched.") },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            context.cacheDir.deleteRecursively()
                        } catch (e: Exception) { }
                        showClearCacheDialog = false
                        Toast.makeText(context, "Temporary network cache cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Clear Cache", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DeveloperOverviewTab(
    identityManager: SecureIdentityManager,
    tokenManager: SecureTokenManager,
    licenseCache: LicenseStateCache,
    activationManager: AppActivationManager,
    networkMonitor: NetworkConnectivityMonitor,
    syncManager: SyncManager,
    dateFormat: SimpleDateFormat,
    onCopyInstallationId: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val activationStatus by activationManager.activationStateFlow.collectAsState()
    val connectionStatus by networkMonitor.connectionStatus.collectAsState()
    val pendingSyncCount by syncManager.pendingSyncCount.collectAsState()
    var isPinging by remember { mutableStateOf(false) }
    var pingResult by remember { mutableStateOf<String?>(null) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // CARD 1: Developer Server & API Telemetry Card
        item {
            DeveloperCard(title = "Developer API & Server Status", icon = Icons.Default.CloudSync) {
                InfoRow("API Base URL", ApiConfig.getBaseUrl())
                InfoRow(
                    "Server Status",
                    when (connectionStatus.state) {
                        ConnectionState.ONLINE_CONNECTED -> "🟢 ONLINE (Server Connected)"
                        ConnectionState.ONLINE_UNREACHABLE -> "🟡 ONLINE (Server Unreachable)"
                        ConnectionState.OFFLINE -> "🔴 OFFLINE (No Internet)"
                    }
                )
                InfoRow("Connection Latency", if (connectionStatus.latencyMs > 0) "${connectionStatus.latencyMs} ms" else "N/A")
                InfoRow("HTTP Status Code", if (connectionStatus.httpCode > 0) connectionStatus.httpCode.toString() else "N/A")
                InfoRow("Status Message", connectionStatus.statusMessage)

                val lastHeartbeatTs = licenseCache.getLastValidatedTimestamp()
                InfoRow("Last Heartbeat", if (lastHeartbeatTs > 0) dateFormat.format(Date(lastHeartbeatTs)) else "None Recorded")
                InfoRow("Last Successful Sync", syncManager.getFormattedLastSync())
                InfoRow("Pending Sync Queue", "$pendingSyncCount items queued")

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        scope.launch {
                            isPinging = true
                            val result = networkMonitor.pingServer()
                            isPinging = false
                            pingResult = "HTTP ${result.httpCode} • Latency: ${result.latencyMs}ms • ${result.statusMessage}"
                            Toast.makeText(context, pingResult, Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isPinging
                ) {
                    if (isPinging) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pinging Server...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test API Ping", fontWeight = FontWeight.Bold)
                    }
                }

                if (pingResult != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = pingResult ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // CARD 2: Identity & Activation State
        item {
            DeveloperCard(title = "Installation ID & Activation Binding", icon = Icons.Default.Verified) {
                val fullId = identityManager.getInstallationId()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Persistent Installation ID", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        IconButton(onClick = onCopyInstallationId, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy ID", tint = Color(0xFFFFD700), modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(
                        text = fullId,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                }
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                InfoRow("Activation Status", activationStatus)
                InfoRow("License Status", licenseCache.getCachedLicenseStatus().uppercase())
                InfoRow("Activated At", activationManager.getActivatedAtFormatted())
                InfoRow("Authorized By", activationManager.getActivatedBy())
                InfoRow("Is POS Unlocked", if (activationManager.isActivated()) "YES (Active)" else "NO (Locked Until Activated)")
                InfoRow("Customer ID", identityManager.getCustomerId())
                InfoRow("Active Store ID", identityManager.getStoreId().toString())
            }
        }

        // CARD 3: POS Application Profile
        item {
            DeveloperCard(title = "POS Application Profile", icon = Icons.Default.Info) {
                InfoRow("Application Name", "CH UMER POS.03080018035")
                InfoRow("Architecture", "Offline-First POS • Online Developer & Sync")
                InfoRow("App Version", "${identityManager.getAppVersion()} (Production Build)")
                InfoRow("Database Engine", "SQLite Local Database (Room Primary Source)")
                InfoRow("Android Runtime", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                InfoRow("Device Model", "${Build.MANUFACTURER.uppercase()} ${Build.MODEL}")
            }
        }

        // CARD 4: System Security & Offline Resilience
        item {
            DeveloperCard(title = "System Security & Resilience", icon = Icons.Default.Security) {
                InfoRow("Token Storage", "KeyStore Hardware Encrypted")
                InfoRow("Access Token Status", if (tokenManager.getAccessToken().isNotBlank()) "Secured in KeyStore" else "Offline Default Ready")
                InfoRow("Offline Resilience", "Local Sales, Cashier, Invoice & Inventory 100% Functional")
                InfoRow("Database Lock Safety", "POS Data is never blocked by network or cloud sync checks")
            }
        }
    }
}

@Composable
fun DeveloperInstallationsTab(
    context: Context,
    identityManager: SecureIdentityManager,
    activationManager: AppActivationManager,
    licenseCache: LicenseStateCache,
    dateFormat: SimpleDateFormat,
    viewModel: StoreViewModel
) {
    val scope = rememberCoroutineScope()
    val activationStatus by activationManager.activationStateFlow.collectAsState()
    var showActionDialog by remember { mutableStateOf<String?>(null) } // "ACTIVATE", "SUSPEND", "REVOKE", "RESET"

    val currentInstallation = remember(activationStatus) {
        InstallationDeviceRecord(
            installationId = identityManager.getInstallationId(),
            customerId = identityManager.getCustomerId(),
            storeId = identityManager.getStoreId(),
            appVersion = identityManager.getAppVersion(),
            activationStatus = activationStatus,
            activatedAt = activationManager.getActivatedAt(),
            activatedBy = activationManager.getActivatedBy(),
            lastSeen = System.currentTimeMillis(),
            deviceModel = "${Build.MANUFACTURER.uppercase()} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            licensePlan = licenseCache.getCachedPlanType()
        )
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            DeveloperCard(title = "Registered App Installations Control", icon = Icons.Default.Devices) {
                Text(
                    text = "Manage POS installations, activation states, hardware bindings, and remote control policies.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Color(0xFFFFD700))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Current Device Installation",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }

                        AssistChip(
                            onClick = {},
                            label = { Text(currentInstallation.activationStatus, fontWeight = FontWeight.Bold) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = when (currentInstallation.activationStatus) {
                                    AppActivationManager.STATUS_ACTIVATED -> Color(0xFF064E3B)
                                    AppActivationManager.STATUS_SUSPENDED -> Color(0xFF78350F)
                                    AppActivationManager.STATUS_REVOKED -> Color(0xFF7F1D1D)
                                    else -> Color(0xFF1E3A8A)
                                },
                                labelColor = Color.White
                            )
                        )
                    }

                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                    InfoRow("Installation ID", currentInstallation.installationId)
                    InfoRow("Customer / Store", "${currentInstallation.customerId} (Store #${currentInstallation.storeId})")
                    InfoRow("Hardware Model", currentInstallation.deviceModel)
                    InfoRow("Android OS", currentInstallation.androidVersion)
                    InfoRow("App Version", currentInstallation.appVersion)
                    InfoRow("License Plan", currentInstallation.licensePlan)
                    InfoRow("Activated At", activationManager.getActivatedAtFormatted())
                    InfoRow("Authorized By", currentInstallation.activatedBy)

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Developer Control Actions:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val code = AppActivationManager.generateActivationCode(currentInstallation.installationId)
                                scope.launch {
                                    activationManager.activateWithCode(code) { status, msg, success ->
                                        viewModel.refreshActivationState()
                                        viewModel.logActivity("DEV_ACTION_ACTIVATE", "Developer activated device ${currentInstallation.installationId}")
                                        Toast.makeText(context, "Device Activated: $msg", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Activate", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                activationManager.suspendInstallation()
                                viewModel.refreshActivationState()
                                viewModel.logActivity("DEV_ACTION_SUSPEND", "Developer suspended device ${currentInstallation.installationId}")
                                Toast.makeText(context, "Installation Suspended", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Suspend", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                activationManager.revokeInstallation()
                                viewModel.refreshActivationState()
                                viewModel.logActivity("DEV_ACTION_REVOKE", "Developer revoked device ${currentInstallation.installationId}")
                                Toast.makeText(context, "Installation Revoked", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Revoke", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            activationManager.resetActivation()
                            viewModel.refreshActivationState()
                            viewModel.logActivity("DEV_ACTION_RESET", "Reset activation state on device ${currentInstallation.installationId}")
                            Toast.makeText(context, "Activation reset to unactivated (First Install Test Mode)", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF87171)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset to Unactivated (Test First-Time Launch)")
                    }
                }
            }
        }
    }
}

@Composable
fun DeveloperCodeGeneratorTab(
    identityManager: SecureIdentityManager,
    activationManager: AppActivationManager,
    viewModel: StoreViewModel,
    onCopyCode: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var targetInstallationId by remember { mutableStateOf(identityManager.getInstallationId()) }
    var targetCustomerId by remember { mutableStateOf(identityManager.getCustomerId()) }
    var selectedPlan by remember { mutableStateOf("COMMERCIAL") }
    var saltInput by remember { mutableStateOf("CH_UMER_DEV_2026") }

    var generatedCode by remember {
        mutableStateOf(
            AppActivationManager.generateActivationCode(
                installationId = targetInstallationId,
                customerId = targetCustomerId,
                planType = selectedPlan,
                salt = saltInput
            )
        )
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            DeveloperCard(title = "Authoritative Activation Code Generator", icon = Icons.Default.Key) {
                Text(
                    text = "Generate cryptographically signed activation codes bound to a specific Installation ID and Customer ID.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = targetInstallationId,
                    onValueChange = { targetInstallationId = it },
                    label = { Text("Target Installation ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { targetInstallationId = identityManager.getInstallationId() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Use This Device", tint = Color(0xFFFFD700))
                        }
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = targetCustomerId,
                    onValueChange = { targetCustomerId = it },
                    label = { Text("Customer ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("COMMERCIAL", "ENTERPRISE", "ANNUAL", "LIFETIME").forEach { plan ->
                        FilterChip(
                            selected = selectedPlan == plan,
                            onClick = { selectedPlan = plan },
                            label = { Text(plan, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        generatedCode = AppActivationManager.generateActivationCode(
                            installationId = targetInstallationId,
                            customerId = targetCustomerId,
                            planType = selectedPlan,
                            salt = saltInput
                        )
                        viewModel.logActivity("DEV_CODE_GENERATED", "Generated activation code for $targetInstallationId ($selectedPlan)")
                        Toast.makeText(context, "New Activation Code Generated", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GENERATE ACTIVATION CODE", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Generated Code Box
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Generated Activation Code:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                            IconButton(onClick = { onCopyCode(generatedCode) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color(0xFFFFD700))
                            }
                        }

                        Text(
                            text = generatedCode,
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700),
                            modifier = Modifier.padding(vertical = 6.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                scope.launch {
                                    activationManager.activateWithCode(generatedCode) { status, msg, success ->
                                        viewModel.refreshActivationState()
                                        Toast.makeText(context, "Applied to Device: $msg", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Apply Code Directly to this Device")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeveloperApiConfigAndTestsTab(
    currentBaseUrl: String,
    isRunningTest: Boolean,
    activeTestEndpoint: String?,
    endpointTestResults: Map<String, String>,
    connectionStatus: DetailedConnectionStatus,
    onEditUrlClick: () -> Unit,
    onResetUrlClick: () -> Unit,
    onRunPingTest: () -> Unit,
    onRunEndpointTest: (String) -> Unit
) {
    val endpoints = listOf(
        "GET /health" to "Checks server health and responsiveness",
        "GET /config" to "Fetches server feature flags and heartbeat interval",
        "POST /installation/register" to "Registers persistent Installation ID and fetches token",
        "POST /installation/activate" to "Authorizes installation using Developer Activation Code",
        "POST /license/validate" to "Validates commercial license with developer server",
        "POST /license/heartbeat" to "Sends periodic heartbeat ping to server",
        "POST /app/version" to "Checks for new production releases"
    )

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            DeveloperCard(title = "Centralized API Server Configuration", icon = Icons.Default.Cloud) {
                InfoRow("Active Base URL", currentBaseUrl)
                InfoRow("API Version", ApiConfig.API_VERSION)
                InfoRow("Connect Timeout", "${ApiConfig.CONNECT_TIMEOUT_SECONDS} Seconds")
                InfoRow("Read Timeout", "${ApiConfig.READ_TIMEOUT_SECONDS} Seconds")
                InfoRow("Max Retry Attempts", ApiConfig.MAX_RETRY_ATTEMPTS.toString())
                InfoRow("Live Status", "${connectionStatus.state.name} (${if (connectionStatus.latencyMs > 0) "${connectionStatus.latencyMs}ms" else "N/A"})")

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onEditUrlClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Configure URL")
                    }

                    OutlinedButton(
                        onClick = onResetUrlClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Default")
                    }

                    Button(
                        onClick = onRunPingTest,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ping")
                    }
                }
            }
        }

        item {
            DeveloperCard(title = "Developer API Endpoints Integration Test", icon = Icons.Default.Api) {
                Text(
                    text = "Tap any endpoint below to test connection with CH UMER POS.03080018035 server:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )

                Spacer(modifier = Modifier.height(10.dp))

                endpoints.forEach { (endpoint, description) ->
                    val isTestingThis = isRunningTest && activeTestEndpoint == endpoint
                    val result = endpointTestResults[endpoint]

                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (result != null) (if (result.contains("HTTP 200") || result.contains("OK")) Color(0xFF10B981) else Color(0xFFF59E0B)) else Color.White.copy(alpha = 0.1f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = endpoint,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFFFFD700)
                                    )
                                    Text(
                                        text = description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.LightGray
                                    )
                                }

                                Button(
                                    onClick = { onRunEndpointTest(endpoint) },
                                    enabled = !isRunningTest,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    if (isTestingThis) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Text("Test Ping", fontSize = 11.sp)
                                    }
                                }
                            }

                            if (result != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    color = Color(0xFF0F172A),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = result,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = if (result.contains("HTTP 200") || result.contains("OK")) Color(0xFF10B981) else Color(0xFFF59E0B),
                                        modifier = Modifier.padding(8.dp)
                                    )
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
fun DeveloperLicenseTab(
    cachedLicenseStatus: String,
    isTestingLicense: Boolean,
    licenseCache: LicenseStateCache,
    dateFormat: SimpleDateFormat,
    onValidateLicenseClick: () -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            DeveloperCard(title = "License State & Server Validation", icon = Icons.Default.VerifiedUser) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Current License State", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    AssistChip(
                        onClick = {},
                        label = { Text(cachedLicenseStatus.uppercase(), fontWeight = FontWeight.Bold) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = when (cachedLicenseStatus) {
                                "active" -> Color(0xFF064E3B)
                                "suspended" -> Color(0xFF7F1D1D)
                                else -> Color(0xFF78350F)
                            },
                            labelColor = Color.White
                        )
                    )
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))

                InfoRow("Plan Tier", licenseCache.getCachedPlanType())
                InfoRow("Max Store Units", licenseCache.getCachedMaxShops().toString())
                InfoRow("Max Staff Users", licenseCache.getCachedMaxUsers().toString())
                InfoRow("License Expiration", "Perpetual / Lifetime Commercial License")
                InfoRow("Last Server Check", if (licenseCache.getLastValidatedTimestamp() > 0) dateFormat.format(Date(licenseCache.getLastValidatedTimestamp())) else "Offline Initialized")

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onValidateLicenseClick,
                    enabled = !isTestingLicense,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    if (isTestingLicense) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Force Validate License with Server")
                    }
                }
            }
        }
    }
}

@Composable
fun DeveloperSyncAndDiagnosticsTab(
    context: Context,
    syncManager: SyncManager,
    isManualSyncing: Boolean,
    pendingSyncCount: Int,
    lastSyncError: String?,
    syncLogs: List<com.example.data.api.sync.SyncLogItem>,
    dateFormat: SimpleDateFormat,
    onTriggerSyncClick: () -> Unit,
    onTriggerCloudBackupClick: () -> Unit,
    onLocalSnapshotClick: () -> Unit,
    onClearCacheClick: () -> Unit
) {
    val lastSyncTs = syncManager.lastSyncTimestamp.collectAsState().value
    val lastSyncText = if (lastSyncTs > 0) dateFormat.format(Date(lastSyncTs)) else "Never (Pending first connection)"

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            DeveloperCard(title = "Cloud Synchronization Engine", icon = Icons.Default.Sync) {
                InfoRow("Sync Engine Status", if (isManualSyncing) "RUNNING..." else "STANDBY")
                InfoRow("Pending Queue Items", "$pendingSyncCount change(s) queued")
                InfoRow("Last Sync Execution", lastSyncText)
                if (lastSyncError != null) {
                    InfoRow("Last Notice", lastSyncError)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onTriggerSyncClick,
                    enabled = !isManualSyncing,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    if (isManualSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Execute Manual Cloud Sync (${pendingSyncCount} queued)")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onTriggerCloudBackupClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cloud Backup", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onLocalSnapshotClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Local Snapshot", fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            DeveloperCard(title = "Recent Synchronization & Network Logs", icon = Icons.Default.ReceiptLong) {
                if (syncLogs.isEmpty()) {
                    Text(
                        text = "No sync activity recorded yet. Sync runs automatically when online.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        syncLogs.take(8).forEach { log ->
                            Surface(
                                color = Color(0xFF0F172A),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = log.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = when (log.level) {
                                                SyncLogLevel.SUCCESS -> Color(0xFF10B981)
                                                SyncLogLevel.WARNING -> Color(0xFFF59E0B)
                                                SyncLogLevel.ERROR -> Color(0xFFEF4444)
                                                SyncLogLevel.INFO -> Color.White
                                            }
                                        )
                                        Text(
                                            text = dateFormat.format(Date(log.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            DeveloperCard(title = "Local SQLite Storage & Diagnostic Actions", icon = Icons.Default.Build) {
                InfoRow("SQLite Database Name", "store_manager_pos.db")
                InfoRow("Primary Source of Truth", "Local Room SQLite (Offline Native)")
                InfoRow("Auto Schema Migrations", "Enabled")

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onClearCacheClick,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear Temporary Cache & Network Logs")
                }
            }
        }
    }
}

@Composable
fun DeveloperCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
            content()
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = Color.White)
    }
}
