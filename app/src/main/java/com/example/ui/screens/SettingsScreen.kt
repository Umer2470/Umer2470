package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.StoreSettings
import com.example.ui.components.AppHeader
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.BiometricPromptHelper

@Composable
fun SettingsScreen(
    viewModel: StoreViewModel,
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val storeSettings by viewModel.storeSettings.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val accentColorIndex by viewModel.accentColorIndex.collectAsState()
    val fontSizeScale by viewModel.fontSizeScale.collectAsState()
    val fontFamilyChoice by viewModel.fontFamilyChoice.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricAuthEnabled.collectAsState()

    val context = LocalContext.current
    val (isBioHardwareAvailable, bioStatusText) = remember(context) { BiometricPromptHelper.isBiometricAvailable(context) }
    var biometricTestMessage by remember { mutableStateOf<String?>(null) }
    var isBiometricTestSuccess by remember { mutableStateOf(false) }

    var storeName by remember(storeSettings) { mutableStateOf(storeSettings?.storeName ?: "CH UMER POS.03080018035") }
    var ownerName by remember(storeSettings) { mutableStateOf(storeSettings?.ownerName ?: "CH UMER") }
    var phone by remember(storeSettings) { mutableStateOf(storeSettings?.phone ?: "03080018035") }
    var address by remember(storeSettings) { mutableStateOf(storeSettings?.address ?: "Main Market, Store #1") }
    var currency by remember(storeSettings) { mutableStateOf(storeSettings?.currencySymbol ?: "Rs") }
    var taxRate by remember(storeSettings) { mutableStateOf((storeSettings?.taxRatePercent ?: 0.0).toString()) }
    var footerText by remember(storeSettings) { mutableStateOf(storeSettings?.invoiceFooterText ?: "Thank you for shopping with us! No return without receipt.") }
    var paperWidth by remember(storeSettings) { mutableStateOf(storeSettings?.paperWidthMm ?: 80) }
    var soundEnabled by remember(storeSettings) { mutableStateOf(storeSettings?.enableSoundEffects ?: true) }
    var backupEnabled by remember(storeSettings) { mutableStateOf(storeSettings?.enableCloudBackup ?: true) }

    var showSaveToast by remember { mutableStateOf(false) }
    var showEmergencyDialog by remember { mutableStateOf(false) }

    val accentPaletteColors = listOf(
        Pair("Royal Blue", Color(0xFF1D4ED8)),
        Pair("Emerald", Color(0xFF047857)),
        Pair("Deep Purple", Color(0xFF6D28D9)),
        Pair("Warm Orange", Color(0xFFC2410C)),
        Pair("Crimson Red", Color(0xFFBE123C)),
        Pair("Vibrant Teal", Color(0xFF0F766E))
    )

    Scaffold(
        topBar = {
            AppHeader(
                title = "Application Settings",
                subtitle = "Appearance, Store Config & Security",
                onBackClick = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 0. Strict Offline-First Status Banner
            Card(
                modifier = Modifier.fillMaxWidth().testTag("offline_status_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Emerald50),
                border = androidx.compose.foundation.BorderStroke(1.dp, Emerald300)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = Emerald600,
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Strict Offline-First System Active",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Emerald900
                        )
                        Text(
                            text = "POS terminal, local invoices, inventory, barcodes, and reports operate 100% offline. Internet is strictly optional for Cloud Backups & Activation.",
                            fontSize = 11.sp,
                            color = Emerald800,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // 1. Appearance & Theme Settings
            SectionHeader(title = "Appearance & Theme Customization", subtitle = "Select theme modes, accent palettes and styling")

            Card(
                modifier = Modifier.fillMaxWidth().testTag("settings_appearance_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Theme Mode", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Light", "Dark", "Black", "System").forEach { mode ->
                            FilterChip(
                                selected = themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                                label = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        when (mode) {
                                            "Light" -> Icon(Icons.Default.LightMode, contentDescription = null, modifier = Modifier.size(14.dp))
                                            "Dark" -> Icon(Icons.Default.DarkMode, contentDescription = null, modifier = Modifier.size(14.dp))
                                            "Black" -> Icon(Icons.Default.Contrast, contentDescription = null, modifier = Modifier.size(14.dp))
                                            "System" -> Icon(Icons.Default.SettingsBrightness, contentDescription = null, modifier = Modifier.size(14.dp))
                                        }
                                        Text(mode, fontWeight = FontWeight.SemiBold)
                                    }
                                },
                                modifier = Modifier.testTag("theme_chip_$mode")
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Text("Custom Primary / Accent Color", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("Selected color applies to buttons, navigation highlights, badges and active states:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        accentPaletteColors.forEachIndexed { index, (name, color) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(color, CircleShape)
                                        .clickable { viewModel.setAccentColor(index) }
                                        .border(
                                            width = if (accentColorIndex == index) 3.dp else 1.dp,
                                            color = if (accentColorIndex == index) Gold500 else Color.LightGray,
                                            shape = CircleShape
                                        )
                                        .testTag("accent_color_$index"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (accentColorIndex == index) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = name,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = name.split(" ").first(),
                                    fontSize = 10.sp,
                                    fontWeight = if (accentColorIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 2. Text & Typography Settings
            SectionHeader(title = "Text & Font Customization", subtitle = "Typography scaling, font families and preview")

            Card(
                modifier = Modifier.fillMaxWidth().testTag("settings_typography_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Text Size Scale", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple("Small (85%)", 0.85f, "small"),
                            Triple("Medium (100%)", 1.0f, "medium"),
                            Triple("Large (115%)", 1.15f, "large"),
                            Triple("Extra Large (130%)", 1.30f, "xlarge")
                        ).forEach { (label, scale, tag) ->
                            FilterChip(
                                selected = (fontSizeScale - scale).let { it in -0.02f..0.02f },
                                onClick = { viewModel.setFontSizeScale(scale) },
                                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                modifier = Modifier.testTag("font_scale_$tag")
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Text("Font Family Style", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Default", "SansSerif", "Serif", "Monospace").forEach { family ->
                            FilterChip(
                                selected = fontFamilyChoice == family,
                                onClick = { viewModel.setFontFamily(family) },
                                label = { Text(family, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                modifier = Modifier.testTag("font_family_$family")
                            )
                        }
                    }

                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Live Typography Preview Box
                    Text("Live Typography Preview:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "CH UMER POS Terminal #1",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Item: PPRC Pipe 25mm • Qty: 5 @ Rs 450.00 = Rs 2,250.00",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Status: PAID • Method: CASH",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Emerald700,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Grand Total: Rs 2,250.00",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 3. Store Configuration Settings
            SectionHeader(title = "Store & Business Configuration", subtitle = "Branding, Tax & Invoicing")

            Card(
                modifier = Modifier.fillMaxWidth().testTag("settings_store_config_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("Store Name") },
                        modifier = Modifier.fillMaxWidth().testTag("settings_store_name")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = ownerName,
                            onValueChange = { ownerName = it },
                            label = { Text("Owner Name") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Contact Phone") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Store Address") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = currency,
                            onValueChange = { currency = it },
                            label = { Text("Currency") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = taxRate,
                            onValueChange = { taxRate = it },
                            label = { Text("Tax Rate (%)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = footerText,
                        onValueChange = { footerText = it },
                        label = { Text("Invoice Receipt Note") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Thermal Receipt Paper Width", fontSize = 13.sp, color = Navy900)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = paperWidth == 58,
                                onClick = { paperWidth = 58 },
                                label = { Text("58mm") }
                            )
                            FilterChip(
                                selected = paperWidth == 80,
                                onClick = { paperWidth = 80 },
                                label = { Text("80mm") }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sound Effects & Audio Beeps", fontSize = 13.sp, color = Navy900)
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = { soundEnabled = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Automatic Local Cloud Sync", fontSize = 13.sp, color = Navy900)
                        Switch(
                            checked = backupEnabled,
                            onCheckedChange = { backupEnabled = it }
                        )
                    }

                    Button(
                        onClick = {
                            val updated = (storeSettings ?: StoreSettings()).copy(
                                storeName = storeName,
                                ownerName = ownerName,
                                phone = phone,
                                address = address,
                                currencySymbol = currency,
                                taxRatePercent = taxRate.toDoubleOrNull() ?: 0.0,
                                invoiceFooterText = footerText,
                                paperWidthMm = paperWidth,
                                enableSoundEffects = soundEnabled,
                                enableCloudBackup = backupEnabled
                            )
                            viewModel.updateStoreSettings(updated)
                            showSaveToast = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                        modifier = Modifier.fillMaxWidth().testTag("save_store_settings_button")
                    ) {
                        Text("Save Store Settings")
                    }

                    if (showSaveToast) {
                        Text(
                            text = "Settings successfully saved to database!",
                            color = Emerald600,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // 4. Biometric Security & Disaster Recovery
            SectionHeader(title = "Biometric Security & Authentication", subtitle = "Fingerprint & Face unlock for terminal access")

            Card(
                modifier = Modifier.fillMaxWidth().testTag("settings_biometric_card"),
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = if (isBiometricEnabled) Emerald100 else Slate100,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = "Biometric Icon",
                                    tint = if (isBiometricEnabled) Emerald700 else Navy500,
                                    modifier = Modifier.padding(8.dp).size(24.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Biometric Terminal Unlock",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Navy900
                                )
                                Text(
                                    text = "Fingerprint or Face Unlock on Login",
                                    fontSize = 12.sp,
                                    color = Navy500
                                )
                            }
                        }
                        Switch(
                            checked = isBiometricEnabled,
                            onCheckedChange = { viewModel.setBiometricAuthEnabled(it) },
                            modifier = Modifier.testTag("biometric_toggle_switch")
                        )
                    }

                    Surface(
                        color = if (isBioHardwareAvailable) Emerald50 else Amber50,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isBioHardwareAvailable) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (isBioHardwareAvailable) Emerald700 else Amber700,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = bioStatusText,
                                fontSize = 12.sp,
                                color = if (isBioHardwareAvailable) Emerald800 else Amber800,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    if (isBiometricEnabled) {
                        OutlinedButton(
                            onClick = {
                                BiometricPromptHelper.authenticateUser(
                                    context = context,
                                    title = "Test Biometric Sensor",
                                    subtitle = "Authenticate to verify biometric sensor status",
                                    negativeButtonText = "Cancel",
                                    onSuccess = {
                                        isBiometricTestSuccess = true
                                        biometricTestMessage = "Biometric authentication verified successfully!"
                                    },
                                    onError = { error ->
                                        isBiometricTestSuccess = false
                                        biometricTestMessage = error
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth().testTag("test_biometric_button")
                        ) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = Emerald600)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Test Biometric Sensor", color = Navy900)
                        }

                        if (biometricTestMessage != null) {
                            Text(
                                text = biometricTestMessage!!,
                                color = if (isBiometricTestSuccess) Emerald700 else Rose600,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 5. Emergency Recovery Credentials
            SectionHeader(title = "Disaster Recovery & Master Passphrase", subtitle = "Master access codes and emergency recovery keys")

            Card(
                modifier = Modifier.fillMaxWidth().testTag("settings_security_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Emergency Recovery Credentials",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Navy900
                    )
                    Text(
                        text = "Use these credentials in case you forget your Admin PIN or need offline terminal recovery.",
                        fontSize = 12.sp,
                        color = Navy500
                    )

                    OutlinedButton(
                        onClick = { showEmergencyDialog = true },
                        modifier = Modifier.fillMaxWidth().testTag("view_emergency_codes_button")
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Gold600)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Emergency Recovery Passphrase & Key", color = Navy900)
                    }
                }
            }

            // 6. Administration & Management Modules
            SectionHeader(title = "Administration & System Modules", subtitle = "Direct access to management hubs")

            listOf(
                Triple("Store Management Center", "Multi-branch and outlet configurations", "store_management"),
                Triple("Store Access & Roles", "Role permissions & security rules", "access_management"),
                Triple("Business Profile Setup", "Store name, address, receipt headers", "setup"),
                Triple("Device Activation", "Commercial license key status", "activation"),
                Triple("SaaS Master Control", "Tenant quotas, stores, license generator", "master_saas"),
                Triple("Developer & API Panel", "Server diagnostics & health checks", "dev_panel"),
                Triple("Staff & Users", "Manage operators and PINs", "users"),
                Triple("Recycle Bin", "Restore or permanently delete items", "recycle_bin"),
                Triple("Audit Activity Logs", "Inspect recent terminal actions", "logs")
            ).forEach { (title, subtitle, route) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(route) }
                        .testTag("settings_item_$route"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(title, fontWeight = FontWeight.Bold, color = Navy900)
                            Text(subtitle, fontSize = 12.sp, color = Navy500)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Navy500)
                    }
                }
            }
        }

        if (showEmergencyDialog) {
            val recoveryCode = remember { viewModel.getEmergencyRecoveryCode() }
            val recoveryPhrase = remember { viewModel.getRecoveryPassphrase() }

            AlertDialog(
                onDismissRequest = { showEmergencyDialog = false },
                title = {
                    Text(
                        text = "Emergency Recovery Keys",
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Keep these recovery keys safe. They allow master terminal unlock without an active internet connection.",
                            fontSize = 12.sp,
                            color = Navy600
                        )

                        Text("20-Character Emergency Code:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Navy900)
                        Surface(
                            color = Slate100,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = recoveryCode,
                                fontWeight = FontWeight.Bold,
                                color = Navy900,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Text("12-Word Passphrase:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Navy900)
                        Surface(
                            color = Slate100,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = recoveryPhrase,
                                fontWeight = FontWeight.Medium,
                                color = Navy800,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showEmergencyDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy900)
                    ) {
                        Text("Done")
                    }
                }
            )
        }
    }
}

