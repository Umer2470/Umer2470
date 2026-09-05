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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.security.AppActivationManager
import com.example.data.entity.StoreBranch
import com.example.data.entity.User
import com.example.ui.components.AppHeader
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel

enum class OwnerTab {
    LICENSE,
    BRANCHES,
    STAFF,
    SECURITY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerControlCenterScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDeveloperHub: () -> Unit = {}
) {
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    val storeSettings by viewModel.storeSettings.collectAsState()
    val branches by viewModel.branches.collectAsState()
    val users by viewModel.users.collectAsState()
    val activationState by viewModel.activationState.collectAsState()
    val isActivated = activationState == "COMMERCIAL_ACTIVATED" || activationState == "ACTIVATED"

    // Security Gate State
    var isUnlocked by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var showRegenerateKeyDialog by remember { mutableStateOf(false) }

    // Tab State
    var currentTab by remember { mutableStateOf(OwnerTab.LICENSE) }

    // Toast Message
    var toastMessage by remember { mutableStateOf<String?>(null) }

    // License Generator State
    val currentTerminalId = remember { viewModel.identityManager.getInstallationId() }
    var targetInstallationId by remember { mutableStateOf(currentTerminalId) }
    var selectedPlanDays by remember { mutableStateOf(30) } // 30 (1 Mo), 90 (3 Mo), 180 (6 Mo), 365 (1 Yr), 0 (Lifetime), -1 (Custom)
    var customPlanDaysInput by remember { mutableStateOf("45") }
    var generatedActivationCode by remember { mutableStateOf("") }
    var showDeactivateConfirmDialog by remember { mutableStateOf(false) }

    // Change Owner PIN Dialog
    var showChangePinDialog by remember { mutableStateOf(false) }
    var newOwnerPin by remember { mutableStateOf("") }
    var confirmOwnerPin by remember { mutableStateOf("") }

    // Add Branch Dialog
    var showAddBranchDialog by remember { mutableStateOf(false) }
    var branchName by remember { mutableStateOf("") }
    var branchLocation by remember { mutableStateOf("") }
    var branchManager by remember { mutableStateOf("") }
    var branchPhone by remember { mutableStateOf("") }
    var branchIsHq by remember { mutableStateOf(false) }

    // Reset Staff PIN Dialog
    var resetPinUser by remember { mutableStateOf<User?>(null) }
    var staffNewPin by remember { mutableStateOf("") }

    // Emergency Keys Dialog
    var showEmergencyKeysDialog by remember { mutableStateOf(false) }

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
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = Navy900, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Owner Control Center", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                Text("Store Proprietor & Terminal Master Hub", fontSize = 11.sp, color = Gold400)
                            }
                        }

                        if (isUnlocked) {
                            IconButton(
                                onClick = { isUnlocked = false },
                                modifier = Modifier.testTag("btn_lock_owner_center")
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
            if (!isUnlocked) {
                // ----------------------------------------------------
                // OWNER SECURITY CODE / PIN AUTHENTICATION GATE
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
                            .testTag("owner_auth_card"),
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
                                    .background(Gold100),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Gold600,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Text(
                                text = "Owner Security Authorization",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Navy900,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "Dedicated Owner / Developer Control Center access. Authenticate using your Dedicated Owner Security Password/PIN or Owner Security Key.",
                                fontSize = 12.sp,
                                color = Navy500,
                                textAlign = TextAlign.Center
                            )

                            OutlinedTextField(
                                value = enteredPin,
                                onValueChange = {
                                    enteredPin = it
                                    pinError = null
                                },
                                label = { Text("Owner Password/PIN or Security Key") },
                                placeholder = { Text("Enter Owner PIN or Owner Security Key") },
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                        Icon(
                                            if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = if (isPasswordVisible) "Hide credential" else "Show credential"
                                        )
                                    }
                                },
                                singleLine = true,
                                isError = pinError != null,
                                supportingText = {
                                    if (pinError != null) {
                                        Text(pinError!!, color = Rose600)
                                    } else {
                                        Text("Dedicated Owner Credential Only (Cashier & Admin PINs are not accepted)", fontSize = 11.sp, color = Navy400)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("owner_pin_input")
                            )

                            Button(
                                onClick = {
                                    if (viewModel.verifyOwnerSecurityCode(enteredPin)) {
                                        isUnlocked = true
                                        enteredPin = ""
                                        pinError = null
                                    } else {
                                        pinError = "Invalid Owner Security Credential. Access Denied."
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("btn_unlock_owner_center")
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Unlock Owner Control Center", fontWeight = FontWeight.Bold)
                            }

                            Surface(
                                color = Slate100,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = Navy700,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Biometric authentication is completely excluded. Access is permitted strictly via Dedicated Owner Password/PIN or Security Key.",
                                        fontSize = 11.sp,
                                        color = Navy700,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // ----------------------------------------------------
                // UNLOCKED OWNER CONTROL CENTER
                // ----------------------------------------------------
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Navigation Tabs Strip
                    TabRow(
                        selectedTabIndex = currentTab.ordinal,
                        containerColor = Color.White,
                        contentColor = Navy900
                    ) {
                        Tab(
                            selected = currentTab == OwnerTab.LICENSE,
                            onClick = { currentTab = OwnerTab.LICENSE },
                            text = { Text("License & Keys", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("tab_owner_license")
                        )
                        Tab(
                            selected = currentTab == OwnerTab.BRANCHES,
                            onClick = { currentTab = OwnerTab.BRANCHES },
                            text = { Text("Branches", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("tab_owner_branches")
                        )
                        Tab(
                            selected = currentTab == OwnerTab.STAFF,
                            onClick = { currentTab = OwnerTab.STAFF },
                            text = { Text("Staff & PINs", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("tab_owner_staff")
                        )
                        Tab(
                            selected = currentTab == OwnerTab.SECURITY,
                            onClick = { currentTab = OwnerTab.SECURITY },
                            text = { Text("Security", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.testTag("tab_owner_security")
                        )
                    }

                    // Toast Banner
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

                    // Tab Content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        when (currentTab) {
                            OwnerTab.LICENSE -> {
                                val planName = viewModel.getLicensePlanName()
                                val expiryTimestamp = viewModel.getLicenseExpiryTimestamp()
                                val daysRemaining = viewModel.getLicenseDaysRemaining()
                                val expiryFormatted = if (expiryTimestamp <= 0L) "Permanent (Lifetime)" else {
                                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(expiryTimestamp))
                                }

                                // License Status Banner
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("owner_license_status_card"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (isActivated) Emerald50 else Rose100),
                                    border = ButtonDefaults.outlinedButtonBorder
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = if (isActivated) planName else "Terminal Unactivated",
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isActivated) Emerald800 else Rose600,
                                                    fontSize = 15.sp
                                                )
                                                Text(
                                                    text = if (isActivated) {
                                                        "Expiry: $expiryFormatted" + if (daysRemaining >= 0) " ($daysRemaining days remaining)" else " (No Expiry)"
                                                    } else {
                                                        "SENTRY STORE POS Multi-Terminal Commercial Edition"
                                                    },
                                                    fontSize = 11.sp,
                                                    color = Navy600
                                                )
                                            }
                                            StatusBadge(
                                                text = if (isActivated) "VERIFIED" else "PENDING",
                                                backgroundColor = if (isActivated) Emerald600 else Rose600,
                                                textColor = Color.White
                                            )
                                        }

                                        if (isActivated) {
                                            HorizontalDivider(color = Slate200)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                OutlinedButton(
                                                    onClick = {
                                                        viewModel.extendCurrentLicense(30)
                                                        toastMessage = "License extended by +30 Days!"
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f).testTag("btn_extend_license_30")
                                                ) {
                                                    Text("+30 Days", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        viewModel.renewCurrentLicense(365, "1 Year Enterprise")
                                                        toastMessage = "License renewed for 1 Year!"
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f).testTag("btn_renew_license_1yr")
                                                ) {
                                                    Text("Renew 1 Yr", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }

                                                Button(
                                                    onClick = { showDeactivateConfirmDialog = true },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f).testTag("btn_deactivate_license")
                                                ) {
                                                    Text("Deactivate", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Current Device Hardware ID
                                SectionHeader(title = "This Device Hardware Identity", subtitle = "Unique cryptographic installation signature")

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("Installation ID:", fontSize = 12.sp, color = Navy500)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = currentTerminalId,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Navy900
                                            )
                                            IconButton(
                                                onClick = {
                                                    clipboardManager.setPrimaryClip(ClipData.newPlainText("Installation ID", currentTerminalId))
                                                    toastMessage = "Installation ID copied to clipboard!"
                                                },
                                                modifier = Modifier.testTag("btn_copy_terminal_id")
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Navy700)
                                            }
                                        }
                                    }
                                }

                                // Universal Developer Platform Hub Entry Card
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigateToDeveloperHub() }
                                        .testTag("btn_open_universal_developer_hub"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Navy900),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(CircleShape)
                                                    .background(Gold500),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Hub, contentDescription = null, tint = Navy900, modifier = Modifier.size(22.dp))
                                            }
                                            Column {
                                                Text("🚀 Universal Developer License Hub", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                                Text("Multi-App Ecosystem, Customer Installations & Device Transfers", fontSize = 11.sp, color = Gold300)
                                            }
                                        }
                                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Gold400)
                                    }
                                }

                                // Master Offline Activation Generator Tool
                                SectionHeader(
                                    title = "Master Activation Key Generator",
                                    subtitle = "Generate authentic offline license keys with membership duration"
                                )

                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("activation_generator_card"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "Enter any Cashier Terminal's Installation ID and select membership duration:",
                                            fontSize = 12.sp,
                                            color = Navy600
                                        )

                                        OutlinedTextField(
                                            value = targetInstallationId,
                                            onValueChange = { targetInstallationId = it },
                                            label = { Text("Target Installation ID") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("generator_input_terminal_id")
                                        )

                                        // Membership Plan Duration Selector
                                        Text("Select License Membership Plan:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Navy800)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            listOf(
                                                30 to "1 Month",
                                                90 to "3 Months",
                                                180 to "6 Months",
                                                365 to "1 Year",
                                                0 to "Lifetime"
                                            ).forEach { (days, label) ->
                                                FilterChip(
                                                    selected = selectedPlanDays == days,
                                                    onClick = { selectedPlanDays = days },
                                                    label = { Text(label, fontSize = 10.sp) },
                                                    modifier = Modifier.testTag("plan_chip_$days")
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            FilterChip(
                                                selected = selectedPlanDays == -1,
                                                onClick = { selectedPlanDays = -1 },
                                                label = { Text("Custom Days", fontSize = 10.sp) },
                                                modifier = Modifier.testTag("plan_chip_custom")
                                            )
                                            if (selectedPlanDays == -1) {
                                                OutlinedTextField(
                                                    value = customPlanDaysInput,
                                                    onValueChange = { customPlanDaysInput = it },
                                                    label = { Text("Days (e.g. 45)") },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f).testTag("input_custom_plan_days")
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    if (targetInstallationId.isNotBlank()) {
                                                        val effectiveDays = if (selectedPlanDays == -1) {
                                                            customPlanDaysInput.toIntOrNull() ?: 30
                                                        } else {
                                                            selectedPlanDays
                                                        }
                                                        generatedActivationCode = viewModel.generateOfflineActivationCode(targetInstallationId, effectiveDays)
                                                        toastMessage = "Activation code generated successfully!"
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f).testTag("btn_generate_activation_code")
                                            ) {
                                                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Generate Code")
                                            }

                                            OutlinedButton(
                                                onClick = { targetInstallationId = currentTerminalId },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Use This Device")
                                            }
                                        }

                                        if (generatedActivationCode.isNotBlank()) {
                                            Surface(
                                                color = Gold50,
                                                border = ButtonDefaults.outlinedButtonBorder,
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column {
                                                        Text("Generated License Code:", fontSize = 11.sp, color = Gold700, fontWeight = FontWeight.Bold)
                                                        Text(
                                                            text = generatedActivationCode,
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            fontSize = 15.sp,
                                                            color = Navy900
                                                        )
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            clipboardManager.setPrimaryClip(ClipData.newPlainText("Activation Code", generatedActivationCode))
                                                            toastMessage = "Activation code copied to clipboard!"
                                                        },
                                                        modifier = Modifier.testTag("btn_copy_generated_code")
                                                    ) {
                                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Navy900)
                                                    }
                                                }
                                            }

                                            // Quick Apply button if generating for this device
                                            if (targetInstallationId.trim() == currentTerminalId.trim()) {
                                                Button(
                                                    onClick = {
                                                        viewModel.activateApp(generatedActivationCode) { _, msg, isSuccess ->
                                                            toastMessage = msg
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.fillMaxWidth().testTag("btn_apply_code_locally")
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Activate This Device Now")
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            OwnerTab.BRANCHES -> {
                                SectionHeader(title = "Store Outlets & Branch Network", subtitle = "${branches.size} Configured Outlets")

                                Button(
                                    onClick = { showAddBranchDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("btn_add_branch_owner")
                                ) {
                                    Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Add New Branch Outlet")
                                }

                                if (branches.isEmpty()) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White)
                                    ) {
                                        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                            Text("No additional branches configured yet.", color = Navy500, fontSize = 13.sp)
                                        }
                                    }
                                } else {
                                    branches.forEach { branch ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth().testTag("owner_branch_${branch.id}"),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color.White),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        Text(branch.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Navy900)
                                                        if (branch.isHeadquarters) {
                                                            StatusBadge(text = "HQ", backgroundColor = Gold100, textColor = Gold600)
                                                        }
                                                    }
                                                    Text("Location: ${branch.location}", fontSize = 12.sp, color = Navy600)
                                                    if (branch.managerName.isNotBlank()) {
                                                        Text("Manager: ${branch.managerName} • ${branch.phone}", fontSize = 11.sp, color = Navy500)
                                                    }
                                                }

                                                IconButton(
                                                    onClick = {
                                                        viewModel.deleteBranch(branch.id)
                                                        toastMessage = "Branch '${branch.name}' removed"
                                                    }
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Rose600)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            OwnerTab.STAFF -> {
                                SectionHeader(title = "Cashier & Staff Security PINs", subtitle = "Master PIN reset and operator control")

                                users.forEach { user ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth().testTag("staff_control_card_${user.id}"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(user.fullName.ifBlank { user.username }, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Navy900)
                                                Text("@${user.username} • Role: ${user.role}", fontSize = 11.sp, color = Navy500)
                                                Surface(
                                                    color = if (user.isActive) Emerald100 else Rose100,
                                                    shape = RoundedCornerShape(4.dp),
                                                    modifier = Modifier.padding(top = 4.dp)
                                                ) {
                                                    Text(
                                                        text = if (user.isActive) "ACTIVE OPERATOR" else "DISABLED",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (user.isActive) Emerald700 else Rose600,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                OutlinedButton(
                                                    onClick = {
                                                        resetPinUser = user
                                                        staffNewPin = ""
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.testTag("btn_reset_pin_${user.id}")
                                                ) {
                                                    Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Reset PIN", fontSize = 11.sp)
                                                }

                                                IconButton(
                                                    onClick = {
                                                        viewModel.toggleUserStatus(user) {
                                                            toastMessage = "Updated status for ${user.username}"
                                                        }
                                                    }
                                                ) {
                                                    Icon(
                                                        if (user.isActive) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                                                        contentDescription = "Toggle Status",
                                                        tint = if (user.isActive) Emerald600 else Slate400,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            OwnerTab.SECURITY -> {
                                SectionHeader(title = "Proprietor Security & Safeguards", subtitle = "Master access codes, dedicated security key, and safeguards")

                                // Change Owner PIN Card
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Owner Security Password / PIN", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Navy900)
                                            Text("Dedicated authentication password/PIN for Owner & Developer access", fontSize = 11.sp, color = Navy500)
                                        }
                                        Button(
                                            onClick = { showChangePinDialog = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.testTag("btn_change_owner_pin")
                                        ) {
                                            Text("Change PIN", fontSize = 12.sp)
                                        }
                                    }
                                }

                                // Dedicated Owner Security Key Card
                                val currentSecurityKey = remember(showRegenerateKeyDialog, toastMessage) { viewModel.getOwnerSecurityKey() }
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("owner_security_key_card"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
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
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Dedicated Owner Security Key", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Navy900)
                                                Text("Independent cryptographic master key for owner authorization", fontSize = 11.sp, color = Navy500)
                                            }
                                            Surface(
                                                color = Emerald100,
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "Active",
                                                    color = Emerald700,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        Surface(
                                            color = Slate100,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = currentSecurityKey.ifBlank { "OWNER-KEY-ENCRYPTED" },
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Navy900,
                                                modifier = Modifier.padding(12.dp)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    val clip = ClipData.newPlainText("Owner Security Key", currentSecurityKey)
                                                    clipboardManager.setPrimaryClip(clip)
                                                    toastMessage = "Owner Security Key copied to clipboard!"
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f).testTag("btn_copy_owner_security_key")
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Copy Key", fontSize = 12.sp)
                                            }

                                            Button(
                                                onClick = { showRegenerateKeyDialog = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.weight(1f).testTag("btn_regenerate_owner_key")
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Regenerate", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }

                                // Biometric Security Policy Status Card
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("biometric_policy_card"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Slate50)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.size(40.dp).clip(CircleShape).background(Rose100),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Block, contentDescription = null, tint = Rose600, modifier = Modifier.size(22.dp))
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Biometric Authentication: Completely Removed",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Navy900
                                            )
                                            Text(
                                                text = "No fingerprint or face unlock is permitted. Access is strictly granted via Dedicated Owner Password/PIN or Owner Security Key only.",
                                                fontSize = 11.sp,
                                                color = Navy600,
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                }

                                // Emergency Recovery Keys Card
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Disaster Recovery Passphrase", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Navy900)
                                            Text("12-word cryptographic seed & 20-char recovery key", fontSize = 11.sp, color = Navy500)
                                        }
                                        OutlinedButton(
                                            onClick = { showEmergencyKeysDialog = true },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.testTag("btn_view_recovery_keys")
                                        ) {
                                            Text("View Keys", fontSize = 12.sp)
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

    // ----------------------------------------------------
    // DIALOGS
    // ----------------------------------------------------

    // Change Owner PIN Dialog
    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Change Owner Security PIN", fontWeight = FontWeight.Bold, color = Navy900) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newOwnerPin,
                        onValueChange = { if (it.length <= 11) newOwnerPin = it },
                        label = { Text("New Security PIN (min 4 digits)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_new_owner_pin")
                    )
                    OutlinedTextField(
                        value = confirmOwnerPin,
                        onValueChange = { if (it.length <= 11) confirmOwnerPin = it },
                        label = { Text("Confirm New PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_confirm_owner_pin")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newOwnerPin.length >= 4 && newOwnerPin == confirmOwnerPin) {
                            viewModel.setOwnerSecurityCode(newOwnerPin)
                            showChangePinDialog = false
                            newOwnerPin = ""
                            confirmOwnerPin = ""
                            toastMessage = "Owner Security PIN updated successfully!"
                        }
                    },
                    enabled = newOwnerPin.length >= 4 && newOwnerPin == confirmOwnerPin,
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900)
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Regenerate Owner Security Key Confirmation Dialog
    if (showRegenerateKeyDialog) {
        AlertDialog(
            onDismissRequest = { showRegenerateKeyDialog = false },
            icon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = Navy900) },
            title = { Text("Regenerate Owner Security Key", fontWeight = FontWeight.Bold, color = Navy900) },
            text = {
                Text(
                    "Are you sure you want to generate a new Owner Security Key? Any previously saved or printed security keys will become invalid immediately.",
                    fontSize = 13.sp,
                    color = Navy700
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newKey = viewModel.regenerateOwnerSecurityKey()
                        showRegenerateKeyDialog = false
                        toastMessage = "New Owner Security Key generated!"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    modifier = Modifier.testTag("btn_confirm_regenerate_key")
                ) {
                    Text("Regenerate Key")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegenerateKeyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Branch Dialog
    if (showAddBranchDialog) {
        AlertDialog(
            onDismissRequest = { showAddBranchDialog = false },
            title = { Text("Add Store Branch Outlet", fontWeight = FontWeight.Bold, color = Navy900) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = branchName,
                        onValueChange = { branchName = it },
                        label = { Text("Branch Name *") },
                        placeholder = { Text("e.g. City Mall Branch") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = branchLocation,
                        onValueChange = { branchLocation = it },
                        label = { Text("Location / Address *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = branchManager,
                        onValueChange = { branchManager = it },
                        label = { Text("Branch Manager") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = branchPhone,
                        onValueChange = { branchPhone = it },
                        label = { Text("Contact Phone") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (branchName.isNotBlank() && branchLocation.isNotBlank()) {
                            viewModel.addBranch(
                                name = branchName,
                                location = branchLocation,
                                managerName = branchManager,
                                phone = branchPhone,
                                isHq = branchIsHq
                            )
                            showAddBranchDialog = false
                            branchName = ""
                            branchLocation = ""
                            branchManager = ""
                            branchPhone = ""
                            toastMessage = "Branch created successfully!"
                        }
                    },
                    enabled = branchName.isNotBlank() && branchLocation.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900)
                ) {
                    Text("Create Branch")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBranchDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reset Staff PIN Dialog
    resetPinUser?.let { staff ->
        AlertDialog(
            onDismissRequest = { resetPinUser = null },
            title = { Text("Reset PIN for @${staff.username}", fontWeight = FontWeight.Bold, color = Navy900) },
            text = {
                OutlinedTextField(
                    value = staffNewPin,
                    onValueChange = { if (it.length <= 8) staffNewPin = it },
                    label = { Text("New PIN") },
                    placeholder = { Text("Enter 4-8 digits") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_staff_new_pin")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (staffNewPin.isNotBlank()) {
                            viewModel.addOrUpdateCashier(
                                fullName = staff.fullName,
                                username = staff.username,
                                pin = staffNewPin,
                                role = staff.role,
                                userId = staff.id,
                                isActive = staff.isActive
                            ) {
                                resetPinUser = null
                                toastMessage = "PIN updated for @${staff.username}"
                            }
                        }
                    },
                    enabled = staffNewPin.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900)
                ) {
                    Text("Update PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { resetPinUser = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Emergency Keys Dialog
    if (showEmergencyKeysDialog) {
        val code = remember { viewModel.getEmergencyRecoveryCode() }
        val phrase = remember { viewModel.getRecoveryPassphrase() }

        AlertDialog(
            onDismissRequest = { showEmergencyKeysDialog = false },
            title = { Text("Emergency Recovery Credentials", fontWeight = FontWeight.Bold, color = Navy900) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Keep these credentials in a secure offline vault for terminal recovery.", fontSize = 12.sp, color = Navy600)
                    Text("20-Character Recovery Key:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Navy800)
                    Surface(color = Slate100, shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(code, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp), fontSize = 12.sp)
                    }
                    Text("12-Word Master Passphrase:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Navy800)
                    Surface(color = Slate100, shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(phrase, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, modifier = Modifier.padding(8.dp), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showEmergencyKeysDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Deactivation Confirmation Dialog
    if (showDeactivateConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeactivateConfirmDialog = false },
            title = { Text("Deactivate Terminal License?", fontWeight = FontWeight.Bold, color = Rose600) },
            text = {
                Text("This will reset the activation on this device and return it to unactivated status. You will need an authentic activation key to reactivate it.", fontSize = 13.sp, color = Navy700)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deactivateLicense()
                        showDeactivateConfirmDialog = false
                        toastMessage = "Terminal license deactivated."
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                    modifier = Modifier.testTag("confirm_deactivate_button")
                ) {
                    Text("Yes, Deactivate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeactivateConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
