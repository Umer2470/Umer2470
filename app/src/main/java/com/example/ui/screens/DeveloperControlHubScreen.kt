package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.*
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
import com.example.data.api.platform.*
import com.example.ui.components.AppHeader
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class DevPlatformTab {
    APPS,
    CUSTOMERS,
    INSTALLATIONS,
    LICENSES,
    GENERATOR,
    AUDIT_LOGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperControlHubScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    val platformEngine = remember { UniversalLicensePlatformEngine.getInstance(context) }

    val applications by platformEngine.applicationsFlow.collectAsState()
    val customers by platformEngine.customersFlow.collectAsState()
    val installations by platformEngine.installationsFlow.collectAsState()
    val licenses by platformEngine.licensesFlow.collectAsState()
    val auditLogs by platformEngine.auditLogsFlow.collectAsState()

    // Security Gate State
    var isDeveloperAuthenticated by remember { mutableStateOf(false) }
    var enteredMasterKey by remember { mutableStateOf("") }
    var masterKeyError by remember { mutableStateOf<String?>(null) }

    // Active Tab
    var selectedTab by remember { mutableStateOf(DevPlatformTab.APPS) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    // Dialog States
    var showAddAppDialog by remember { mutableStateOf(false) }
    var newAppId by remember { mutableStateOf("") }
    var newAppName by remember { mutableStateOf("") }
    var newAppPackage by remember { mutableStateOf("") }
    var newAppVersion by remember { mutableStateOf("1.0.0") }
    var newAppEmoji by remember { mutableStateOf("🚀") }

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var newCustBusiness by remember { mutableStateOf("") }
    var newCustContact by remember { mutableStateOf("") }
    var newCustPhone by remember { mutableStateOf("") }
    var newCustEmail by remember { mutableStateOf("") }
    var newCustAddress by remember { mutableStateOf("") }

    var showTransferDialog by remember { mutableStateOf<UniversalLicense?>(null) }
    var transferNewInstallationId by remember { mutableStateOf("") }

    var showSuspendDialog by remember { mutableStateOf<UniversalLicense?>(null) }
    var suspendReason by remember { mutableStateOf("Policy violation or contract termination") }

    // Generator State
    var genSelectedAppId by remember { mutableStateOf(UniversalLicensePlatformEngine.APP_SENTRY_STORE_POS) }
    var genSelectedCustomerId by remember { mutableStateOf(customers.firstOrNull()?.customerId ?: "CUST-1001") }
    var genInstallationId by remember { mutableStateOf(viewModel.identityManager.getInstallationId()) }
    var genPlanDuration by remember { mutableStateOf(30) } // 30, 90, 180, 365, 0 (Lifetime), -1 (Custom)
    var genCustomDaysInput by remember { mutableStateOf("60") }
    var lastGeneratedLicense by remember { mutableStateOf<UniversalLicense?>(null) }

    val currentLocalInstallationId = remember { viewModel.identityManager.getInstallationId() }

    Scaffold(
        topBar = {
            Surface(
                color = Navy900,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Gold500),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.VpnKey, contentDescription = null, tint = Navy900, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Universal Developer Control", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                Text("Central License & Multi-App Platform", fontSize = 11.sp, color = Gold400)
                            }
                        }

                        if (isDeveloperAuthenticated) {
                            IconButton(
                                onClick = { isDeveloperAuthenticated = false },
                                modifier = Modifier.testTag("btn_lock_dev_platform")
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = "Lock", tint = Gold400)
                            }
                        }
                    }
                }
            }
        },
        containerColor = Slate50
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!isDeveloperAuthenticated) {
                // ----------------------------------------------------
                // DEVELOPER MASTER SECURITY AUTHENTICATION GATE
                // ----------------------------------------------------
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dev_master_auth_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Navy900),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = Gold400,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Text(
                                text = "Developer Master Security Gate",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Navy900,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "Enter Master Developer Secret Key to access central multi-app licensing, customer databases, device transfers, and platform telemetry.",
                                fontSize = 12.sp,
                                color = Navy500,
                                textAlign = TextAlign.Center
                            )

                            OutlinedTextField(
                                value = enteredMasterKey,
                                onValueChange = {
                                    enteredMasterKey = it
                                    masterKeyError = null
                                },
                                label = { Text("Developer Master Key / PIN") },
                                placeholder = { Text("Enter Developer Key or Admin PIN") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                isError = masterKeyError != null,
                                supportingText = {
                                    if (masterKeyError != null) {
                                        Text(masterKeyError!!, color = Rose600)
                                    } else {
                                        Text("Developer Auth: Super Admin PIN or Emergency Recovery Code", fontSize = 11.sp, color = Navy400)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dev_master_key_input")
                            )

                            Button(
                                onClick = {
                                    val clean = enteredMasterKey.trim()
                                    if (viewModel.verifyDeveloperAuth(clean)) {
                                        isDeveloperAuthenticated = true
                                        enteredMasterKey = ""
                                        masterKeyError = null
                                    } else {
                                        masterKeyError = "Invalid Developer Master Key. Access Denied."
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_unlock_dev_platform")
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Authenticate Developer Console", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // ----------------------------------------------------
                // UNLOCKED DEVELOPER CONTROL PLATFORM
                // ----------------------------------------------------
                Column(modifier = Modifier.fillMaxSize()) {
                    // Tab Bar
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        containerColor = Color.White,
                        contentColor = Navy900,
                        edgePadding = 8.dp
                    ) {
                        Tab(
                            selected = selectedTab == DevPlatformTab.APPS,
                            onClick = { selectedTab = DevPlatformTab.APPS },
                            text = { Text("Apps (${applications.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("tab_dev_apps")
                        )
                        Tab(
                            selected = selectedTab == DevPlatformTab.GENERATOR,
                            onClick = { selectedTab = DevPlatformTab.GENERATOR },
                            text = { Text("Key Generator", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("tab_dev_generator")
                        )
                        Tab(
                            selected = selectedTab == DevPlatformTab.LICENSES,
                            onClick = { selectedTab = DevPlatformTab.LICENSES },
                            text = { Text("Licenses (${licenses.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("tab_dev_licenses")
                        )
                        Tab(
                            selected = selectedTab == DevPlatformTab.CUSTOMERS,
                            onClick = { selectedTab = DevPlatformTab.CUSTOMERS },
                            text = { Text("Customers (${customers.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("tab_dev_customers")
                        )
                        Tab(
                            selected = selectedTab == DevPlatformTab.INSTALLATIONS,
                            onClick = { selectedTab = DevPlatformTab.INSTALLATIONS },
                            text = { Text("Installations (${installations.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Devices, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("tab_dev_installations")
                        )
                        Tab(
                            selected = selectedTab == DevPlatformTab.AUDIT_LOGS,
                            onClick = { selectedTab = DevPlatformTab.AUDIT_LOGS },
                            text = { Text("Audit Trail", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("tab_dev_audit_logs")
                        )
                    }

                    // Toast Notification
                    if (toastMessage != null) {
                        Surface(
                            color = Emerald100,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald600, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(toastMessage!!, color = Emerald800, fontSize = 12.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = { toastMessage = null }, modifier = Modifier.size(20.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Emerald700, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    // Main Tab Content
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        when (selectedTab) {
                            DevPlatformTab.APPS -> ApplicationsTabView(
                                applications = applications,
                                onAddApp = { showAddAppDialog = true },
                                onToggleStatus = { appId, status ->
                                    platformEngine.updateApplicationStatus(appId, status)
                                    toastMessage = "Application $appId status updated to $status"
                                }
                            )

                            DevPlatformTab.GENERATOR -> KeyGeneratorTabView(
                                applications = applications,
                                customers = customers,
                                currentTerminalId = currentLocalInstallationId,
                                selectedAppId = genSelectedAppId,
                                onAppSelected = { genSelectedAppId = it },
                                selectedCustomerId = genSelectedCustomerId,
                                onCustomerSelected = { genSelectedCustomerId = it },
                                installationId = genInstallationId,
                                onInstallationIdChange = { genInstallationId = it },
                                planDuration = genPlanDuration,
                                onPlanDurationChange = { genPlanDuration = it },
                                customDaysInput = genCustomDaysInput,
                                onCustomDaysChange = { genCustomDaysInput = it },
                                lastGeneratedLicense = lastGeneratedLicense,
                                onGenerate = {
                                    val effectiveDays = if (genPlanDuration == -1) {
                                        genCustomDaysInput.toIntOrNull() ?: 30
                                    } else {
                                        genPlanDuration
                                    }
                                    val generated = platformEngine.generateLicense(
                                        appId = genSelectedAppId,
                                        customerId = genSelectedCustomerId,
                                        installationId = genInstallationId,
                                        durationDays = effectiveDays,
                                        createdBy = "Master Developer"
                                    )
                                    lastGeneratedLicense = generated
                                    toastMessage = "Cryptographic license generated for ${genSelectedAppId}!"
                                },
                                onCopyCode = { code ->
                                    clipboardManager.setPrimaryClip(ClipData.newPlainText("License Code", code))
                                    toastMessage = "License key copied to clipboard!"
                                },
                                onApplyLocally = { code ->
                                    viewModel.activateApp(code) { _, msg, _ ->
                                        toastMessage = msg
                                    }
                                }
                            )

                            DevPlatformTab.LICENSES -> LicensesTabView(
                                licenses = licenses,
                                onExtend = { lic, days ->
                                    platformEngine.extendLicense(lic.licenseId, days)
                                    toastMessage = "License extended by +$days days!"
                                },
                                onRenew = { lic, days, name ->
                                    platformEngine.renewLicense(lic.licenseId, days, name)
                                    toastMessage = "License renewed for $name!"
                                },
                                onSuspend = { lic -> showSuspendDialog = lic },
                                onReactivate = { lic ->
                                    platformEngine.reactivateLicense(lic.licenseId)
                                    toastMessage = "License reactivated!"
                                },
                                onDeactivate = { lic ->
                                    platformEngine.deactivateLicense(lic.licenseId)
                                    toastMessage = "License deactivated/revoked!"
                                },
                                onTransfer = { lic -> showTransferDialog = lic },
                                onCopy = { text ->
                                    clipboardManager.setPrimaryClip(ClipData.newPlainText("License Info", text))
                                    toastMessage = "Copied to clipboard!"
                                }
                            )

                            DevPlatformTab.CUSTOMERS -> CustomersTabView(
                                customers = customers,
                                onAddCustomer = { showAddCustomerDialog = true }
                            )

                            DevPlatformTab.INSTALLATIONS -> InstallationsTabView(
                                installations = installations
                            )

                            DevPlatformTab.AUDIT_LOGS -> AuditLogsTabView(
                                logs = auditLogs
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog: Add Application
    if (showAddAppDialog) {
        AlertDialog(
            onDismissRequest = { showAddAppDialog = false },
            title = { Text("Register New Application", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newAppId,
                        onValueChange = { newAppId = it },
                        label = { Text("Application ID (e.g. WORKOUT-APP)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newAppName,
                        onValueChange = { newAppName = it },
                        label = { Text("Application Name (e.g. FitPulse Pro)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newAppPackage,
                        onValueChange = { newAppPackage = it },
                        label = { Text("Package Identifier") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newAppVersion,
                            onValueChange = { newAppVersion = it },
                            label = { Text("Version") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newAppEmoji,
                            onValueChange = { newAppEmoji = it },
                            label = { Text("Icon") },
                            singleLine = true,
                            modifier = Modifier.width(70.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newAppId.isNotBlank() && newAppName.isNotBlank()) {
                            platformEngine.registerApplication(
                                appId = newAppId,
                                appName = newAppName,
                                packageIdentifier = newAppPackage.ifBlank { "com.app.${newAppId.lowercase()}" },
                                version = newAppVersion,
                                iconEmoji = newAppEmoji
                            )
                            toastMessage = "Application '$newAppName' registered in central platform!"
                            showAddAppDialog = false
                            newAppId = ""
                            newAppName = ""
                            newAppPackage = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900)
                ) {
                    Text("Register App")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAppDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Dialog: Add Customer
    if (showAddCustomerDialog) {
        AlertDialog(
            onDismissRequest = { showAddCustomerDialog = false },
            title = { Text("Create Customer Account", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newCustBusiness,
                        onValueChange = { newCustBusiness = it },
                        label = { Text("Business / Shop Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCustContact,
                        onValueChange = { newCustContact = it },
                        label = { Text("Contact Person") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCustPhone,
                        onValueChange = { newCustPhone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCustEmail,
                        onValueChange = { newCustEmail = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCustAddress,
                        onValueChange = { newCustAddress = it },
                        label = { Text("Store Address / City") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCustBusiness.isNotBlank()) {
                            platformEngine.createCustomer(
                                businessName = newCustBusiness,
                                contactPerson = newCustContact,
                                phone = newCustPhone,
                                email = newCustEmail,
                                address = newCustAddress
                            )
                            toastMessage = "Customer '$newCustBusiness' created!"
                            showAddCustomerDialog = false
                            newCustBusiness = ""
                            newCustContact = ""
                            newCustPhone = ""
                            newCustEmail = ""
                            newCustAddress = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900)
                ) {
                    Text("Create Customer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCustomerDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Dialog: Transfer Device
    if (showTransferDialog != null) {
        val targetLic = showTransferDialog!!
        AlertDialog(
            onDismissRequest = { showTransferDialog = null },
            title = { Text("Transfer License to New Device", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("App: ${targetLic.appId}", fontSize = 12.sp, color = Navy700, fontWeight = FontWeight.Bold)
                    Text("Current Terminal: ${targetLic.installationId}", fontSize = 12.sp, color = Navy500)
                    Text("Enter the New Device's Installation ID to transfer binding and re-sign license token:", fontSize = 12.sp)
                    OutlinedTextField(
                        value = transferNewInstallationId,
                        onValueChange = { transferNewInstallationId = it },
                        label = { Text("New Device Installation ID") },
                        placeholder = { Text("e.g. APP-XXXXXXXXXXXX") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (transferNewInstallationId.isNotBlank()) {
                            val transferred = platformEngine.transferDevice(
                                licenseId = targetLic.licenseId,
                                newInstallationId = transferNewInstallationId,
                                performedBy = "Master Developer"
                            )
                            if (transferred != null) {
                                toastMessage = "Device transferred! New activation code: ${transferred.licenseCode}"
                            }
                            showTransferDialog = null
                            transferNewInstallationId = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900)
                ) {
                    Text("Authorize Transfer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTransferDialog = null }) { Text("Cancel") }
            }
        )
    }

    // Dialog: Suspend License
    if (showSuspendDialog != null) {
        val targetLic = showSuspendDialog!!
        AlertDialog(
            onDismissRequest = { showSuspendDialog = null },
            title = { Text("Suspend License", fontWeight = FontWeight.Bold, color = Rose600) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Suspend license ${targetLic.licenseId} for ${targetLic.appId}?", fontSize = 13.sp)
                    OutlinedTextField(
                        value = suspendReason,
                        onValueChange = { suspendReason = it },
                        label = { Text("Suspension Reason") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        platformEngine.suspendLicense(targetLic.licenseId, suspendReason)
                        toastMessage = "License suspended"
                        showSuspendDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose600)
                ) {
                    Text("Suspend License")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSuspendDialog = null }) { Text("Cancel") }
            }
        )
    }
}

// ----------------------------------------------------
// SUB-VIEWS FOR EACH TAB
// ----------------------------------------------------

@Composable
fun ApplicationsTabView(
    applications: List<RegisteredApplication>,
    onAddApp: () -> Unit,
    onToggleStatus: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(
                    title = "Registered Platform Applications",
                    subtitle = "${applications.size} Managed Ecosystem Apps"
                )
                Button(
                    onClick = onAddApp,
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("btn_register_new_app")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add App", fontSize = 12.sp)
                }
            }
        }

        items(applications, key = { it.appId }) { app ->
            Card(
                modifier = Modifier.fillMaxWidth().testTag("app_card_${app.appId}"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(app.iconEmoji, fontSize = 22.sp)
                            Column {
                                Text(app.appName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Navy900)
                                Text("ID: ${app.appId} • v${app.version}", fontSize = 12.sp, color = Navy500, fontFamily = FontFamily.Monospace)
                            }
                        }
                        StatusBadge(
                            text = app.status,
                            backgroundColor = if (app.status == "ACTIVE") Emerald100 else Rose100,
                            textColor = if (app.status == "ACTIVE") Emerald600 else Rose600
                        )
                    }

                    HorizontalDivider(color = Slate200)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Package: ${app.packageIdentifier}", fontSize = 11.sp, color = Navy600)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (app.status == "ACTIVE") {
                                OutlinedButton(
                                    onClick = { onToggleStatus(app.appId, "MAINTENANCE") },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Pause", fontSize = 10.sp)
                                }
                            } else {
                                Button(
                                    onClick = { onToggleStatus(app.appId, "ACTIVE") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Activate", fontSize = 10.sp)
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
fun KeyGeneratorTabView(
    applications: List<RegisteredApplication>,
    customers: List<CustomerAccount>,
    currentTerminalId: String,
    selectedAppId: String,
    onAppSelected: (String) -> Unit,
    selectedCustomerId: String,
    onCustomerSelected: (String) -> Unit,
    installationId: String,
    onInstallationIdChange: (String) -> Unit,
    planDuration: Int,
    onPlanDurationChange: (Int) -> Unit,
    customDaysInput: String,
    onCustomDaysChange: (String) -> Unit,
    lastGeneratedLicense: UniversalLicense?,
    onGenerate: () -> Unit,
    onCopyCode: (String) -> Unit,
    onApplyLocally: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionHeader(
            title = "Universal Cryptographic Key Generator",
            subtitle = "Generate application-isolated and device-bound license keys"
        )

        Card(
            modifier = Modifier.fillMaxWidth().testTag("universal_generator_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 1. Select Application
                Text("1. Target Application:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy800)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    applications.forEach { app ->
                        FilterChip(
                            selected = selectedAppId == app.appId,
                            onClick = { onAppSelected(app.appId) },
                            label = { Text("${app.iconEmoji} ${app.appId.take(8)}", fontSize = 11.sp) },
                            modifier = Modifier.testTag("gen_app_chip_${app.appId}")
                        )
                    }
                }

                // 2. Select Customer
                Text("2. Target Customer Account:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy800)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    customers.take(4).forEach { cust ->
                        FilterChip(
                            selected = selectedCustomerId == cust.customerId,
                            onClick = { onCustomerSelected(cust.customerId) },
                            label = { Text(cust.businessName.take(12), fontSize = 11.sp) },
                            modifier = Modifier.testTag("gen_cust_chip_${cust.customerId}")
                        )
                    }
                }

                // 3. Target Installation ID
                Text("3. Target Device Installation ID:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy800)
                OutlinedTextField(
                    value = installationId,
                    onValueChange = onInstallationIdChange,
                    label = { Text("Installation ID") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("gen_input_installation_id")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { onInstallationIdChange(currentTerminalId) }) {
                        Icon(Icons.Default.Smartphone, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Use This Device ID ($currentTerminalId)", fontSize = 11.sp)
                    }
                }

                // 4. Membership Duration
                Text("4. Select Membership Duration:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy800)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        30 to "1 Mo",
                        90 to "3 Mo",
                        180 to "6 Mo",
                        365 to "1 Yr",
                        0 to "Lifetime"
                    ).forEach { (days, label) ->
                        FilterChip(
                            selected = planDuration == days,
                            onClick = { onPlanDurationChange(days) },
                            label = { Text(label, fontSize = 11.sp) },
                            modifier = Modifier.testTag("gen_duration_chip_$days")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = planDuration == -1,
                        onClick = { onPlanDurationChange(-1) },
                        label = { Text("Custom Days", fontSize = 11.sp) }
                    )
                    if (planDuration == -1) {
                        OutlinedTextField(
                            value = customDaysInput,
                            onValueChange = onCustomDaysChange,
                            label = { Text("Days (e.g. 60)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Generate Button
                Button(
                    onClick = onGenerate,
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("btn_generate_universal_key")
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate Cryptographic Key", fontWeight = FontWeight.Bold)
                }

                // Result Box
                if (lastGeneratedLicense != null) {
                    val lic = lastGeneratedLicense
                    Surface(
                        color = Gold50,
                        border = ButtonDefaults.outlinedButtonBorder,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("generated_key_result_box")
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("License for ${lic.appId}:", fontSize = 11.sp, color = Gold700, fontWeight = FontWeight.Bold)
                                    Text(lic.planName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Navy900)
                                }
                                IconButton(onClick = { onCopyCode(lic.licenseCode) }, modifier = Modifier.testTag("btn_copy_license_code")) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Navy900)
                                }
                            }

                            Text(
                                text = lic.licenseCode,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = Navy900
                            )

                            Text("Token: ${lic.signatureToken}", fontSize = 10.sp, color = Navy500, fontFamily = FontFamily.Monospace)

                            if (lic.appId == UniversalLicensePlatformEngine.APP_SENTRY_STORE_POS && lic.installationId == currentTerminalId) {
                                Button(
                                    onClick = { onApplyLocally(lic.licenseCode) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("btn_apply_generated_key")
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Activate This Device Now")
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
fun LicensesTabView(
    licenses: List<UniversalLicense>,
    onExtend: (UniversalLicense, Int) -> Unit,
    onRenew: (UniversalLicense, Int, String) -> Unit,
    onSuspend: (UniversalLicense) -> Unit,
    onReactivate: (UniversalLicense) -> Unit,
    onDeactivate: (UniversalLicense) -> Unit,
    onTransfer: (UniversalLicense) -> Unit,
    onCopy: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionHeader(
                title = "Managed Application Licenses",
                subtitle = "${licenses.size} Issued Multi-Tenant Licenses"
            )
        }

        if (licenses.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No licenses generated yet. Use the Key Generator tab.", color = Navy500, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(licenses, key = { it.licenseId }) { lic ->
                val isValid = lic.isCurrentlyValid()
                val daysRemaining = lic.getDaysRemaining()
                val expiryFormatted = if (lic.expiryDate <= 0L) "Lifetime (Permanent)" else {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(lic.expiryDate))
                }

                Card(
                    modifier = Modifier.fillMaxWidth().testTag("license_card_${lic.licenseId}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(lic.appId, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Navy900)
                                    Text("• ${lic.licenseId}", fontSize = 11.sp, color = Navy500)
                                }
                                Text(lic.planName, fontSize = 12.sp, color = Navy700)
                            }
                            StatusBadge(
                                text = lic.status,
                                backgroundColor = when (lic.status) {
                                    "ACTIVE" -> Emerald100
                                    "SUSPENDED" -> Gold100
                                    else -> Rose100
                                },
                                textColor = when (lic.status) {
                                    "ACTIVE" -> Emerald600
                                    "SUSPENDED" -> Gold600
                                    else -> Rose600
                                }
                            )
                        }

                        Surface(
                            color = Slate100,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth().clickable { onCopy(lic.licenseCode) }
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(lic.licenseCode, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy900)
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Navy600, modifier = Modifier.size(14.dp))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Bound: ${lic.installationId}", fontSize = 11.sp, color = Navy500, fontFamily = FontFamily.Monospace)
                            Text("Expiry: $expiryFormatted" + if (daysRemaining >= 0) " ($daysRemaining d)" else "", fontSize = 11.sp, color = Navy700)
                        }

                        HorizontalDivider(color = Slate200)

                        // Action Buttons Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onExtend(lic, 30) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+30d", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { onRenew(lic, 365, "1 Year Enterprise") },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Renew 1Y", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { onTransfer(lic) },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Transfer", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }

                            if (lic.status == "ACTIVE") {
                                OutlinedButton(
                                    onClick = { onSuspend(lic) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold600),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Suspend", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            } else if (lic.status == "SUSPENDED") {
                                Button(
                                    onClick = { onReactivate(lic) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Resume", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            IconButton(onClick = { onDeactivate(lic) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, contentDescription = "Revoke", tint = Rose600, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomersTabView(
    customers: List<CustomerAccount>,
    onAddCustomer: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(
                    title = "Customer Accounts & Tenancy",
                    subtitle = "${customers.size} Registered Accounts"
                )
                Button(
                    onClick = onAddCustomer,
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Customer", fontSize = 12.sp)
                }
            }
        }

        items(customers, key = { it.customerId }) { cust ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(cust.businessName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Navy900)
                        StatusBadge(text = cust.status, backgroundColor = Emerald100, textColor = Emerald600)
                    }
                    Text("Contact: ${cust.contactPerson} • ${cust.phone}", fontSize = 12.sp, color = Navy600)
                    if (cust.address.isNotBlank()) {
                        Text("Address: ${cust.address}", fontSize = 11.sp, color = Navy500)
                    }
                }
            }
        }
    }
}

@Composable
fun InstallationsTabView(
    installations: List<DeviceInstallation>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionHeader(
                title = "Device Bindings & Hardware Telemetry",
                subtitle = "${installations.size} Registered Installations"
            )
        }

        if (installations.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No device installations registered yet.", color = Navy500, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(installations, key = { "${it.appId}_${it.installationId}" }) { inst ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(inst.installationId, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Navy900)
                            StatusBadge(text = inst.status, backgroundColor = Emerald100, textColor = Emerald600)
                        }
                        Text("App: ${inst.appId} • Customer: ${inst.customerId}", fontSize = 12.sp, color = Navy700)
                        Text("Device: ${inst.deviceModel}", fontSize = 11.sp, color = Navy500)
                    }
                }
            }
        }
    }
}

@Composable
fun AuditLogsTabView(
    logs: List<LicenseAuditLog>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            SectionHeader(
                title = "Immutable License Audit Trail",
                subtitle = "${logs.size} Logged Administrative Events"
            )
        }

        items(logs, key = { it.id }) { log ->
            val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(log.action, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Navy900)
                        Text(dateFormatted, fontSize = 10.sp, color = Navy400)
                    }
                    Text(log.details, fontSize = 11.sp, color = Navy600)
                    Text("By: ${log.performedBy} • App: ${log.appId} • Device: ${log.installationId}", fontSize = 10.sp, color = Navy400)
                }
            }
        }
    }
}
