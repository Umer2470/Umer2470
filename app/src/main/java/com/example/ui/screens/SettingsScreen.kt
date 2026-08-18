package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Storefront
import com.example.data.entity.StoreProfile
import com.example.ui.components.ShopLogoAvatar
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.DatabaseBackupInfo
import com.example.util.DatabaseBackupManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(viewModel: StoreViewModel) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    var storeName by remember(settings) { mutableStateOf(settings.storeName) }
    var ownerName by remember(settings) { mutableStateOf(settings.ownerName) }
    var phone by remember(settings) { mutableStateOf(settings.phone) }
    var whatsappNumber by remember(settings) { mutableStateOf(settings.whatsappNumber) }
    var address by remember(settings) { mutableStateOf(settings.address) }
    var businessType by remember(settings) { mutableStateOf(settings.businessType) }
    var currency by remember(settings) { mutableStateOf(settings.currencySymbol) }
    var logoUri by remember(settings) { mutableStateOf(settings.logoUri) }
    var lowStockThresholdInput by remember(settings) { mutableStateOf(settings.defaultLowStockThreshold.toInt().toString()) }

    var cashierNameInput by remember(settings) { mutableStateOf(settings.defaultCashierName) }
    var cashierEmpIdInput by remember(settings) { mutableStateOf(settings.defaultCashierEmployeeId) }
    var cashierPhoneInput by remember(settings) { mutableStateOf(settings.defaultCashierPhone) }
    var cashierDesignationInput by remember(settings) { mutableStateOf(settings.defaultCashierDesignation.ifBlank { "Cashier" }) }

    val allUsers by viewModel.allUsers.collectAsState()

    var showPinChangeModal by remember { mutableStateOf(false) }
    var showBackupModal by remember { mutableStateOf(false) }
    var showRestoreModal by remember { mutableStateOf(false) }
    var showFactoryResetDialog by remember { mutableStateOf(false) }
    var factoryResetPinInput by remember { mutableStateOf("") }
    var factoryResetError by remember { mutableStateOf<String?>(null) }
    var exportedJsonText by remember { mutableStateOf("") }
    var restoreJsonInput by remember { mutableStateOf("") }

    val localBackups by viewModel.databaseBackups.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadLocalDatabaseBackups(context)
    }

    var selectedUriForImport by remember { mutableStateOf<Uri?>(null) }
    var selectedFileForImport by remember { mutableStateOf<File?>(null) }
    var showImportConfirmDialog by remember { mutableStateOf(false) }

    // System File Chooser to Export SQLite .db
    val exportDbLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        uri?.let { viewModel.exportDatabaseToUri(context, it) }
    }

    // System File Chooser to Import SQLite .db
    val importDbLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            selectedUriForImport = it
            selectedFileForImport = null
            showImportConfirmDialog = true
        }
    }

    // Shop Logo Picker Launcher
    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.saveShopLogo(context, it) }
    }

    val allStores by viewModel.allStores.collectAsState()
    val activeStore by viewModel.activeStore.collectAsState()
    var showAddStoreModal by remember { mutableStateOf(false) }

    var showSuperAdminRecoveryModal by remember { mutableStateOf(false) }
    var recoveryPinVerified by remember { mutableStateOf(false) }
    var recoveryPinInput by remember { mutableStateOf("") }
    var recoveryPinError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // STORE PROFILE SECTION
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Store Profile Info & Logo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Shop Logo Preview & Controls
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ShopLogoAvatar(logoUri = settings.logoUri, size = 88.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { logoPickerLauncher.launch("image/*") },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Change Logo")
                            }
                            if (settings.logoUri.isNotBlank()) {
                                OutlinedButton(
                                    onClick = { viewModel.removeShopLogo() },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Remove Logo")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(value = storeName, onValueChange = { storeName = it }, label = { Text("Store / Business Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = ownerName, onValueChange = { ownerName = it }, label = { Text("Proprietor / Owner Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Contact Phone") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                    OutlinedTextField(value = whatsappNumber, onValueChange = { whatsappNumber = it }, label = { Text("WhatsApp Number") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = businessType, onValueChange = { businessType = it }, label = { Text("Business Type") }, modifier = Modifier.weight(1.5f), singleLine = true)
                    OutlinedTextField(value = currency, onValueChange = { currency = it }, label = { Text("Currency") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Store Address") }, modifier = Modifier.fillMaxWidth())

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val thresh = lowStockThresholdInput.toDoubleOrNull() ?: 5.0
                        viewModel.updateStoreInfo(
                            name = storeName,
                            owner = ownerName,
                            phone = phone,
                            whatsappNumber = whatsappNumber,
                            address = address,
                            businessType = businessType,
                            currency = currency,
                            logoUri = logoUri,
                            lowStockThreshold = thresh
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Store Info & Settings")
                }
            }
        }

        // CASHIER / OPERATOR PROFILE SECTION
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Cashier / Operator Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Configure operator details printed on invoices & receipts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Active Cashier Preview Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Column {
                            Text(
                                text = "Active Cashier on Invoices:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (cashierNameInput.isNotBlank()) "$cashierNameInput (${cashierDesignationInput.ifBlank { "Cashier" }})" else "Not Assigned",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            if (cashierEmpIdInput.isNotBlank() || cashierPhoneInput.isNotBlank()) {
                                val details = listOfNotNull(
                                    if (cashierEmpIdInput.isNotBlank()) "ID: $cashierEmpIdInput" else null,
                                    if (cashierPhoneInput.isNotBlank()) "Ph: $cashierPhoneInput" else null
                                ).joinToString(" • ")
                                Text(
                                    text = details,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = cashierNameInput,
                    onValueChange = { cashierNameInput = it },
                    label = { Text("Cashier / Operator Name") },
                    placeholder = { Text("e.g. Ali Raza or Muhammad Usman") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cashierEmpIdInput,
                        onValueChange = { cashierEmpIdInput = it },
                        label = { Text("Employee ID") },
                        placeholder = { Text("e.g. EMP-001") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = cashierDesignationInput,
                        onValueChange = { cashierDesignationInput = it },
                        label = { Text("Designation") },
                        placeholder = { Text("Cashier") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = cashierPhoneInput,
                    onValueChange = { cashierPhoneInput = it },
                    label = { Text("Cashier Phone (Optional)") },
                    placeholder = { Text("e.g. 0300-1234567") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) }
                )

                // Quick Select From Registered Staff
                if (allUsers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Quick Select from Registered Staff:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allUsers.take(4).forEach { user ->
                            OutlinedButton(
                                onClick = {
                                    cashierNameInput = user.name
                                    cashierEmpIdInput = if (user.id > 0) user.id.toString() else ""
                                    cashierPhoneInput = user.phone
                                    cashierDesignationInput = if (user.role == "EMPLOYEE") "Cashier" else if (user.role == "ADMIN") "Store Admin" else user.role
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(user.name.take(14), fontSize = 11.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (cashierNameInput.isNotBlank() || cashierEmpIdInput.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                cashierNameInput = ""
                                cashierEmpIdInput = ""
                                cashierPhoneInput = ""
                                cashierDesignationInput = "Cashier"
                                viewModel.clearCashierProfile()
                            },
                            modifier = Modifier.weight(0.35f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Reset")
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.updateCashierProfile(
                                name = cashierNameInput,
                                employeeId = cashierEmpIdInput,
                                phone = cashierPhoneInput,
                                designation = cashierDesignationInput
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Cashier Profile")
                    }
                }
            }
        }

        // MULTI-STORE MANAGEMENT SECTION (FOR SUPER ADMIN)
        if (viewModel.isSuperAdmin()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Multi-Store Management", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Manage multiple shop branches & switch active store context", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    allStores.forEach { store ->
                        val isSelected = activeStore?.id == store.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    ShopLogoAvatar(logoUri = store.logoUri, size = 44.dp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(store.storeName, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Text("${store.ownerName} • Ph: ${store.phone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                    }
                                }
                                if (isSelected) {
                                    Text("Active Store", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                } else {
                                    OutlinedButton(
                                        onClick = { viewModel.selectStore(store.id) },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Switch")
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { showAddStoreModal = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add New Store Branch")
                    }
                }
            }
        }

        // APP THEME & ACCENT COLOR USER SETTINGS SECTION (SUPER ADMIN)
        val currentUserSettings by viewModel.currentUser.collectAsState()
        val isSuperAdminThemeControl = currentUserSettings?.role == "SUPER_ADMIN" || viewModel.isSuperAdmin()

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "App Theme & Accent Colors",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Customize light/dark mode and brand accent color",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }

                    if (isSuperAdminThemeControl) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Super Admin",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Light / Dark Mode Toggle Section
                Text(
                    text = "Appearance Mode",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val currentMode = settings.themeMode

                    ThemeOptionCard(
                        title = "Light Mode",
                        subtitle = "Bright Store",
                        icon = Icons.Default.LightMode,
                        isSelected = currentMode == "LIGHT",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isSuperAdminThemeControl) {
                                viewModel.updateThemeMode("LIGHT")
                            } else {
                                viewModel.showToast("Only Super Admin can change app appearance settings.")
                            }
                        }
                    )

                    ThemeOptionCard(
                        title = "Dark Mode",
                        subtitle = "Night / Low Light",
                        icon = Icons.Default.DarkMode,
                        isSelected = currentMode == "DARK",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isSuperAdminThemeControl) {
                                viewModel.updateThemeMode("DARK")
                            } else {
                                viewModel.showToast("Only Super Admin can change app appearance settings.")
                            }
                        }
                    )

                    ThemeOptionCard(
                        title = "System",
                        subtitle = "Auto Sync",
                        icon = Icons.Default.BrightnessAuto,
                        isSelected = currentMode == "SYSTEM",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (isSuperAdminThemeControl) {
                                viewModel.updateThemeMode("SYSTEM")
                            } else {
                                viewModel.showToast("Only Super Admin can change app appearance settings.")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Custom Accent Color Selection Section
                Text(
                    text = "Custom UI Accent Color",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                val currentAccent = settings.accentColor

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val accentRows = com.example.ui.theme.AccentColors.chunked(4)
                    for (row in accentRows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (option in row) {
                                val isSelected = option.key.equals(currentAccent, ignoreCase = true)
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (isSuperAdminThemeControl) {
                                                viewModel.updateAccentColor(option.key)
                                            } else {
                                                viewModel.showToast("Only Super Admin can change UI accent color.")
                                            }
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) option.previewColor.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = if (isSelected) BorderStroke(2.dp, option.previewColor) else null
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(option.previewColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = option.name,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                            fontSize = 11.sp,
                                            textAlign = TextAlign.Center,
                                            color = if (isSelected) option.previewColor else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                            for (i in 0 until (4 - row.size)) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // INVENTORY STOCK ALERT THRESHOLD SECTION
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Low Stock Alert Threshold", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Flag items when stock drops below threshold", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = lowStockThresholdInput,
                    onValueChange = { lowStockThresholdInput = it },
                    label = { Text("Default Low Stock Threshold (Units)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val thresh = lowStockThresholdInput.toDoubleOrNull() ?: 5.0
                            viewModel.updateLowStockThreshold(thresh, applyToAllProducts = false)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Set Default")
                    }

                    OutlinedButton(
                        onClick = {
                            val thresh = lowStockThresholdInput.toDoubleOrNull() ?: 5.0
                            viewModel.updateLowStockThreshold(thresh, applyToAllProducts = true)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Apply to All Items")
                    }
                }
            }
        }

        // SECURITY SECTION
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Security & PIN Lock", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (viewModel.isSuperAdmin()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Enable App PIN Lock", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text("Require 4-digit PIN on store app start", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }

                        Switch(
                            checked = settings.isPinEnabled,
                            onCheckedChange = { viewModel.togglePinSecurity(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Biometric / Face / Fingerprint Unlock", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            }
                            Text("Unlock app and sensitive POS data via biometrics on launch", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }

                        Switch(
                            checked = settings.isBiometricEnabled,
                            onCheckedChange = { viewModel.toggleBiometricSecurity(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showPinChangeModal = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Change 4-Digit Security PIN")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            viewModel.initSuperAdminRecoveryIfMissing()
                            showSuperAdminRecoveryModal = true
                            recoveryPinVerified = false
                            recoveryPinInput = ""
                            recoveryPinError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                    ) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = Color(0xFF38BDF8))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("3-Level Emergency Recovery Settings", color = Color.White)
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF475569))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Security Settings Managed by Super Admin", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                Text("Password, PIN, and security credentials can only be changed or reset by Super Admin / Owner.", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                        }
                    }
                }
            }
        }

        // SQLITE DATABASE BACKUP & RECOVERY SECTION
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("SQLite Database Backup & Restore", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Export & import local .db store file", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                    }

                    IconButton(onClick = { viewModel.loadLocalDatabaseBackups(context) }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Active DB stats
                val dbInfo = remember(localBackups) { DatabaseBackupManager.getCurrentDatabaseInfo(context) }
                dbInfo?.let { info ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Active Store DB: ${info.fileName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("Size: ${info.formattedSize}  •  Modified: ${info.formattedDate}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("OFFLINE DB", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Database Export & Import Buttons
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                exportDbLauncher.launch("alkhair_store_db_$timeStamp.db")
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export .db File")
                        }

                        OutlinedButton(
                            onClick = {
                                importDbLauncher.launch(arrayOf("*/*", "application/x-sqlite3", "application/octet-stream"))
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import .db File")
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.createLocalDatabaseBackup(context) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Quick Local Backup Copy")
                    }
                }

                // Local Saved Backups List
                if (localBackups.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Saved Local DB Backups (${localBackups.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        localBackups.take(5).forEach { backup ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(backup.fileName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                        Text("${backup.formattedSize} • ${backup.formattedDate}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        IconButton(
                                            onClick = { viewModel.shareDatabaseBackup(context, File(backup.path)) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        }

                                        TextButton(
                                            onClick = {
                                                selectedFileForImport = File(backup.path)
                                                selectedUriForImport = null
                                                showImportConfirmDialog = true
                                            }
                                        ) {
                                            Text("Restore", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Collapsible JSON Backup option
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("JSON Text Format Backup", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    TextButton(
                        onClick = {
                            exportedJsonText = viewModel.exportDataToJson(context)
                            showBackupModal = true
                        }
                    ) {
                        Text("Copy JSON", fontSize = 11.sp)
                    }
                }
            }
        }

        // FACTORY RESET BUSINESS SECTION (Super Admin Only)
        val currentUser by viewModel.currentUser.collectAsState()
        if (currentUser?.role == "SUPER_ADMIN" || viewModel.isSuperAdmin()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                border = BorderStroke(1.dp, Color(0xFFFECACA))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEE2E2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFDC2626)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Factory Reset Business",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                            Text(
                                "Erase all store data & return to Business Setup Wizard",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB91C1C)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            factoryResetPinInput = ""
                            factoryResetError = null
                            showFactoryResetDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFDC2626)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reset Business & Start Setup Wizard", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // LOGOUT LOCK BUTTON
        Button(
            onClick = { viewModel.logout() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
        ) {
            Icon(imageVector = Icons.Default.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Lock App & Logout")
        }
    }

    // Factory Reset Confirmation Modal
    if (showFactoryResetDialog) {
        AlertDialog(
            onDismissRequest = { showFactoryResetDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color(0xFFDC2626))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Factory Reset Business Data")
                }
            },
            text = {
                Column {
                    Text(
                        "WARNING: This action will permanently delete all products, sales history, customers, suppliers, staff accounts, and store settings. The app will return to the initial Business Setup Wizard.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Enter Super Admin PIN to confirm:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (factoryResetError != null) {
                        Text(factoryResetError!!, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    OutlinedTextField(
                        value = factoryResetPinInput,
                        onValueChange = { if (it.length <= 6) factoryResetPinInput = it },
                        label = { Text("Super Admin PIN") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.factoryResetBusiness(factoryResetPinInput) { success, msg ->
                            if (success) {
                                showFactoryResetDialog = false
                            } else {
                                factoryResetError = msg
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Erase & Reset Business")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFactoryResetDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Change PIN Modal
    if (showPinChangeModal) {
        if (!viewModel.isSuperAdmin()) {
            showPinChangeModal = false
            viewModel.showToast("Permission Denied: Only Super Admin can change PIN/Password credentials.")
        } else {
            var newPin by remember { mutableStateOf("") }
            var confirmPin by remember { mutableStateOf("") }
            var pinErr by remember { mutableStateOf<String?>(null) }

            AlertDialog(
            onDismissRequest = { showPinChangeModal = false },
            title = { Text("Change 4-Digit Security PIN") },
            text = {
                Column {
                    if (pinErr != null) {
                        Text(pinErr!!, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 4) newPin = it },
                        label = { Text("New 4-Digit PIN") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = { if (it.length <= 4) confirmPin = it },
                        label = { Text("Confirm New PIN") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPin.length != 4) {
                            pinErr = "PIN must be exactly 4 digits!"
                            return@Button
                        }
                        if (newPin != confirmPin) {
                            pinErr = "PINs do not match!"
                            return@Button
                        }
                        viewModel.updatePinCode(newPin)
                        showPinChangeModal = false
                    }
                ) { Text("Update PIN") }
            },
            dismissButton = { TextButton(onClick = { showPinChangeModal = false }) { Text("Cancel") } }
        )
        }
    }

    // Export Backup Modal
    if (showBackupModal) {
        AlertDialog(
            onDismissRequest = { showBackupModal = false },
            title = { Text("Store Backup Data JSON") },
            text = {
                Column {
                    Text("Copy or save this JSON backup text to restore store items, customers, & suppliers later:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportedJsonText,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        readOnly = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showBackupModal = false }) { Text("Done") }
            }
        )
    }

    // Restore Backup Modal
    if (showRestoreModal) {
        AlertDialog(
            onDismissRequest = { showRestoreModal = false },
            title = { Text("Restore Store Data from JSON") },
            text = {
                Column {
                    Text("Paste JSON backup data text below:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = restoreJsonInput,
                        onValueChange = { restoreJsonInput = it },
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        placeholder = { Text("Paste JSON string...") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreJsonInput.isNotBlank()) {
                            viewModel.restoreDataFromJson(restoreJsonInput.trim())
                            showRestoreModal = false
                        }
                    }
                ) { Text("Restore Data") }
            },
            dismissButton = { TextButton(onClick = { showRestoreModal = false }) { Text("Cancel") } }
        )
    }

    // Add New Store Branch Modal
    if (showAddStoreModal) {
        var newStoreName by remember { mutableStateOf("") }
        var newOwnerName by remember { mutableStateOf("") }
        var newPhone by remember { mutableStateOf("") }
        var newAddress by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddStoreModal = false },
            title = { Text("Add New Store Branch") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Create a separate branch store profile with isolated inventory, sales, customers, & expenses.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = newStoreName,
                        onValueChange = { newStoreName = it },
                        label = { Text("Store / Branch Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newOwnerName,
                        onValueChange = { newOwnerName = it },
                        label = { Text("Owner / Manager Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text("Contact Phone") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    OutlinedTextField(
                        value = newAddress,
                        onValueChange = { newAddress = it },
                        label = { Text("Address / Location") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newStoreName.isNotBlank()) {
                            val store = StoreProfile(
                                storeName = newStoreName,
                                ownerName = newOwnerName,
                                phone = newPhone,
                                address = newAddress
                            )
                            viewModel.saveStoreProfile(store)
                            showAddStoreModal = false
                        }
                    }
                ) {
                    Text("Create Branch Store")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStoreModal = false }) { Text("Cancel") }
            }
        )
    }

    // Database (.db) Import Confirmation Dialog
    if (showImportConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showImportConfirmDialog = false
                selectedUriForImport = null
                selectedFileForImport = null
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Overwrite Store Database?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        "Restoring this SQLite database (.db) file will overwrite all current store records (products, inventory, customers, ledgers, sales, and purchases).",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Ensure you have created a backup copy of your current database if you need to keep it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (selectedUriForImport != null) {
                            viewModel.importDatabaseFromUri(context, selectedUriForImport!!)
                        } else if (selectedFileForImport != null) {
                            viewModel.importDatabaseFromFile(context, selectedFileForImport!!)
                        }
                        showImportConfirmDialog = false
                        selectedUriForImport = null
                        selectedFileForImport = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Restore Database")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showImportConfirmDialog = false
                        selectedUriForImport = null
                        selectedFileForImport = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Super Admin 3-Level Emergency Recovery Credentials Modal
    if (showSuperAdminRecoveryModal) {
        val recoveryState by viewModel.superAdminRecovery.collectAsState()
        val recovery = recoveryState ?: com.example.data.entity.SuperAdminRecovery()
        val clipboardManager = LocalClipboardManager.current
        val coroutineScope = rememberCoroutineScope()

        var editEmail by remember(recovery) { mutableStateOf(recovery.recoveryEmail) }
        var editMobile by remember(recovery) { mutableStateOf(recovery.recoveryMobile) }

        AlertDialog(
            onDismissRequest = { showSuperAdminRecoveryModal = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Shield, contentDescription = null, tint = Color(0xFF0284C7))
                    Text("3-Level Emergency Recovery System", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!recoveryPinVerified) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF2F2),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                        ) {
                            Text(
                                "🔒 Authentication Required: Enter Super Admin PIN to view or modify emergency recovery credentials.",
                                fontSize = 11.sp,
                                color = Color(0xFF991B1B),
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        OutlinedTextField(
                            value = recoveryPinInput,
                            onValueChange = { if (it.length <= 4) recoveryPinInput = it },
                            label = { Text("Super Admin 4-Digit PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (recoveryPinError != null) {
                            Text(recoveryPinError!!, color = Color.Red, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val isOk = viewModel.verifyAdminPin(recoveryPinInput)
                                    if (isOk) {
                                        recoveryPinVerified = true
                                        recoveryPinError = null
                                    } else {
                                        recoveryPinError = "Invalid PIN Code!"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                        ) {
                            Text("Authenticate & Unlock View")
                        }
                    } else {
                        // VERIFIED -> SHOW CREDENTIALS
                        Text("12-Word Passphrase (Level 1 - Offline)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0284C7))
                        
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = recovery.passphraseWordsCsv.ifBlank { "Not generated yet" },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(recovery.passphraseWordsCsv))
                                    viewModel.showToast("12-Word Passphrase copied to clipboard!")
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy 12 Words", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text("Emergency Recovery Code (Level 2 - Offline)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF16A34A))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = recovery.emergencyCode.ifBlank { "Not generated yet" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF14532D),
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(recovery.emergencyCode))
                                    viewModel.showToast("Emergency Recovery Code copied!")
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Code", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text("Optional OTP Recovery Contact (Level 3)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF9333EA))

                        OutlinedTextField(
                            value = editEmail,
                            onValueChange = { editEmail = it },
                            label = { Text("Recovery Email") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = editMobile,
                            onValueChange = { editMobile = it },
                            label = { Text("Recovery Mobile") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.updateRecoveryContact(editEmail, editMobile) { ok, msg ->
                                        viewModel.showToast(msg)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA))
                            ) {
                                Text("Save Contacts", fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.regenerateRecoveryCredentials(recoveryPinInput) { ok, msg ->
                                        viewModel.showToast(msg)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Regenerate All", fontSize = 11.sp, color = Color.Red)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSuperAdminRecoveryModal = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun ThemeOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface

    Card(
        onClick = onClick,
        modifier = modifier.border(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor,
            shape = RoundedCornerShape(12.dp)
        ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = Color.Gray
            )
        }
    }
}
