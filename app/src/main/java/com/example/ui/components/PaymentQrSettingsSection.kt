package com.example.ui.components

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.api.security.OwnerSecurityManager
import com.example.data.entity.PaymentQrConfig
import com.example.data.entity.StoreSettings
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.PaymentQrImageHelper

/**
 * Scan to Pay & Payment QR Configuration Section for Store Settings.
 *
 * Provides complete administrative management for:
 * - Scan to Pay ON/OFF toggle on printed/digital invoices
 * - Custom header label (e.g. "SCAN TO PAY", "PAY VIA QR")
 * - Multi-QR profiles (Easypaisa, JazzCash, Raast, Bank, Custom)
 * - QR Image upload, replacement, preview, and deletion
 * - Owner Security verification before applying changes
 */
@Composable
fun PaymentQrSettingsSection(
    viewModel: StoreViewModel,
    storeSettings: StoreSettings?,
    modifier: Modifier = Modifier,
    isPreUnlocked: Boolean = false
) {
    val context = LocalContext.current
    val paymentQrConfigs by viewModel.paymentQrConfigs.collectAsState()
    val activePaymentQr by viewModel.activePaymentQr.collectAsState()

    val ownerSecurityManager = remember(context) { OwnerSecurityManager.getInstance(context) }
    var isOwnerAuthenticated by remember(isPreUnlocked) { mutableStateOf(isPreUnlocked) }
    var showOwnerAuthDialog by remember { mutableStateOf(false) }
    var pendingOwnerAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingQr by remember { mutableStateOf<PaymentQrConfig?>(null) }
    var qrToDelete by remember { mutableStateOf<PaymentQrConfig?>(null) }

    val isScanToPayEnabled = storeSettings?.isScanToPayEnabled ?: false
    var customLabelText by remember(storeSettings?.scanToPayLabel) {
        mutableStateOf(storeSettings?.scanToPayLabel ?: "SCAN TO PAY")
    }

    // Helper to guard administrative actions with Owner Security
    fun requireOwnerAuth(action: () -> Unit) {
        if (isOwnerAuthenticated) {
            action()
        } else {
            pendingOwnerAction = action
            showOwnerAuthDialog = true
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(
            title = "Payment Settings & Scan-to-Pay QR",
            subtitle = "Customer payment QR & barcode accounts on printed/digital receipts (Easypaisa, JazzCash, Raast, Bank)"
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("scan_to_pay_settings_card"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Enable / Disable Scan to Pay Switch Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isScanToPayEnabled) Blue50 else Slate100,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = "Scan to Pay QR",
                                    tint = if (isScanToPayEnabled) Blue600 else Slate500,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Enable Scan to Pay on Invoices",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Navy900
                            )
                            Text(
                                text = "Prints payment QR & account details on customer receipts",
                                fontSize = 12.sp,
                                color = Slate600
                            )
                        }
                    }

                    Switch(
                        checked = isScanToPayEnabled,
                        onCheckedChange = { targetState ->
                            requireOwnerAuth {
                                if (targetState) {
                                    val validQr = paymentQrConfigs.any {
                                        it.isEnabled && it.imagePath.isNotBlank() && PaymentQrImageHelper.isImageValid(it.imagePath)
                                    }
                                    if (!validQr) {
                                        Toast.makeText(
                                            context,
                                            "Please upload a Payment QR image first",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        editingQr = null
                                        showAddEditDialog = true
                                    } else {
                                        viewModel.setScanToPayEnabled(true) { success ->
                                            if (success) {
                                                Toast.makeText(context, "Scan to Pay enabled on invoices", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                } else {
                                    viewModel.setScanToPayEnabled(false) {
                                        Toast.makeText(context, "Scan to Pay disabled on invoices", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Blue600
                        ),
                        modifier = Modifier.testTag("scan_to_pay_toggle")
                    )
                }

                HorizontalDivider(color = Slate200)

                // 2. Custom Header Label on Invoices
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customLabelText,
                        onValueChange = { customLabelText = it },
                        label = { Text("Invoice QR Header Label") },
                        placeholder = { Text("e.g. SCAN TO PAY, PAY VIA QR") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("scan_to_pay_label_input"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Button(
                        onClick = {
                            requireOwnerAuth {
                                viewModel.updateScanToPayDetails(
                                    enabled = isScanToPayEnabled,
                                    label = customLabelText.ifBlank { "SCAN TO PAY" },
                                    activeQrId = activePaymentQr?.id
                                ) {
                                    Toast.makeText(context, "Label updated", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(54.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }

                // 3. Active QR Banner & Quick Preview
                if (activePaymentQr != null && activePaymentQr!!.imagePath.isNotBlank()) {
                    val activeQrBitmap = remember(activePaymentQr!!.imagePath) {
                        PaymentQrImageHelper.getQrBitmap(activePaymentQr!!.imagePath)
                    }

                    Surface(
                        color = Blue50,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Blue100),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (activeQrBitmap != null) {
                                Image(
                                    bitmap = activeQrBitmap.asImageBitmap(),
                                    contentDescription = "Active Payment QR Preview",
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .border(1.dp, Slate300, RoundedCornerShape(6.dp))
                                        .background(Color.White)
                                        .padding(4.dp)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .background(Slate200, RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.BrokenImage, contentDescription = null, tint = Slate500)
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        color = Emerald100,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "ACTIVE ON RECEIPT",
                                            color = Emerald800,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = activePaymentQr!!.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Navy900
                                    )
                                }

                                if (activePaymentQr!!.accountNumber.isNotBlank()) {
                                    Text(
                                        text = "A/C: ${activePaymentQr!!.accountNumber}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Navy800
                                    )
                                }
                                if (activePaymentQr!!.accountTitle.isNotBlank()) {
                                    Text(
                                        text = "Title: ${activePaymentQr!!.accountTitle}",
                                        fontSize = 11.sp,
                                        color = Slate600
                                    )
                                }
                                if (activePaymentQr!!.instructions.isNotBlank()) {
                                    Text(
                                        text = activePaymentQr!!.instructions,
                                        fontSize = 11.sp,
                                        color = Slate500
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    requireOwnerAuth {
                                        editingQr = activePaymentQr
                                        showAddEditDialog = true
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Active QR", tint = Blue700)
                            }
                        }
                    }
                } else if (paymentQrConfigs.isEmpty()) {
                    Surface(
                        color = Slate50,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = null, tint = Slate400, modifier = Modifier.size(36.dp))
                            Text(
                                text = "No Payment QR Configured",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Navy900
                            )
                            Text(
                                text = "Upload your shop's Easypaisa, JazzCash, Raast, or Bank payment QR code to appear on customer receipts.",
                                fontSize = 12.sp,
                                color = Slate600,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = {
                                    requireOwnerAuth {
                                        editingQr = null
                                        showAddEditDialog = true
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("upload_first_qr_button")
                            ) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload Payment QR Image")
                            }
                        }
                    }
                }

                // 4. Configured Payment Profiles List (Multi-QR)
                if (paymentQrConfigs.isNotEmpty()) {
                    Text(
                        text = "Configured Payment Methods (${paymentQrConfigs.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Navy900
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        paymentQrConfigs.forEach { qr ->
                            val isItemActive = (storeSettings?.activePaymentQrId == qr.id) || (storeSettings?.activePaymentQrId == null && qr.isDefault)
                            val itemBitmap = remember(qr.imagePath) {
                                PaymentQrImageHelper.getQrBitmap(qr.imagePath)
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isItemActive) Blue50.copy(alpha = 0.5f) else Slate50
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isItemActive) Blue100 else Slate200
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    if (itemBitmap != null) {
                                        Image(
                                            bitmap = itemBitmap.asImageBitmap(),
                                            contentDescription = qr.name,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .border(1.dp, Slate300, RoundedCornerShape(4.dp))
                                                .background(Color.White)
                                                .padding(2.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(Slate200, RoundedCornerShape(4.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.QrCode, contentDescription = null, tint = Slate500, modifier = Modifier.size(24.dp))
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = qr.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Navy900
                                            )
                                            if (isItemActive) {
                                                Surface(color = Emerald100, shape = RoundedCornerShape(4.dp)) {
                                                    Text(
                                                        "ACTIVE",
                                                        color = Emerald800,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Black,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${qr.paymentType} • ${if (qr.accountNumber.isNotBlank()) qr.accountNumber else "No Account #"}",
                                            fontSize = 11.sp,
                                            color = Slate600
                                        )
                                    }

                                    if (!isItemActive) {
                                        TextButton(
                                            onClick = {
                                                requireOwnerAuth {
                                                    viewModel.updateScanToPayDetails(
                                                        enabled = isScanToPayEnabled,
                                                        label = customLabelText,
                                                        activeQrId = qr.id
                                                    ) {
                                                        Toast.makeText(context, "${qr.name} set as active", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        ) {
                                            Text("Set Active", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Blue600)
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            requireOwnerAuth {
                                                editingQr = qr
                                                showAddEditDialog = true
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Slate700, modifier = Modifier.size(18.dp))
                                    }

                                    IconButton(
                                        onClick = {
                                            requireOwnerAuth {
                                                qrToDelete = qr
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Rose600, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            requireOwnerAuth {
                                editingQr = null
                                showAddEditDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate100, contentColor = Navy900),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_another_qr_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Add Another Payment QR Profile", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // ----------------------------------------------------
    // ADD / EDIT PAYMENT QR DIALOG
    // ----------------------------------------------------
    if (showAddEditDialog) {
        AddEditPaymentQrDialog(
            existingQr = editingQr,
            isInitialFirstQr = paymentQrConfigs.isEmpty(),
            onDismiss = {
                showAddEditDialog = false
                editingQr = null
            },
            onSave = { updatedQr, makeActive ->
                viewModel.saveOrUpdatePaymentQr(updatedQr, setAsActive = makeActive) { savedId ->
                    Toast.makeText(context, "Payment QR saved successfully", Toast.LENGTH_SHORT).show()
                    showAddEditDialog = false
                    editingQr = null
                }
            }
        )
    }

    // ----------------------------------------------------
    // DELETE CONFIRMATION DIALOG
    // ----------------------------------------------------
    if (qrToDelete != null) {
        AlertDialog(
            onDismissRequest = { qrToDelete = null },
            title = { Text("Delete Payment QR Profile?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete ${qrToDelete!!.name}? The image file will be safely removed from local storage.") },
            confirmButton = {
                Button(
                    onClick = {
                        val toRemove = qrToDelete!!
                        qrToDelete = null
                        viewModel.deletePaymentQr(context, toRemove) {
                            Toast.makeText(context, "Payment QR deleted", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose600)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { qrToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ----------------------------------------------------
    // OWNER SECURITY VERIFICATION DIALOG
    // ----------------------------------------------------
    if (showOwnerAuthDialog) {
        OwnerAuthVerificationDialog(
            ownerSecurityManager = ownerSecurityManager,
            onDismiss = {
                showOwnerAuthDialog = false
                pendingOwnerAction = null
            },
            onVerified = {
                isOwnerAuthenticated = true
                showOwnerAuthDialog = false
                pendingOwnerAction?.invoke()
                pendingOwnerAction = null
            }
        )
    }
}

/**
 * Add / Edit Payment QR Profile Dialog with File Picker and Live Preview.
 */
@Composable
fun AddEditPaymentQrDialog(
    existingQr: PaymentQrConfig?,
    isInitialFirstQr: Boolean,
    onDismiss: () -> Unit,
    onSave: (PaymentQrConfig, Boolean) -> Unit
) {
    val context = LocalContext.current
    val paymentPresets = listOf("Easypaisa", "JazzCash", "Raast", "Meezan Bank", "Bank Alfalah", "HBL", "Custom")

    var selectedType by remember { mutableStateOf(existingQr?.paymentType ?: "Easypaisa") }
    var profileName by remember { mutableStateOf(existingQr?.name ?: "Easypaisa") }
    var accountTitle by remember { mutableStateOf(existingQr?.accountTitle ?: "") }
    var accountNumber by remember { mutableStateOf(existingQr?.accountNumber ?: "") }
    var instructions by remember { mutableStateOf(existingQr?.instructions ?: "Scan QR to pay directly") }
    var imagePath by remember { mutableStateOf(existingQr?.imagePath ?: "") }
    var makeActiveOnInvoice by remember { mutableStateOf(existingQr?.isDefault ?: isInitialFirstQr) }
    var isEnabled by remember { mutableStateOf(existingQr?.isEnabled ?: true) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val qrImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedLocalPath = PaymentQrImageHelper.savePaymentQrFromUri(context, uri, existingQr?.imagePath)
            if (savedLocalPath != null) {
                imagePath = savedLocalPath
                validationError = null
            } else {
                validationError = "Failed to copy QR image to app storage"
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 16.dp)
                .testTag("add_edit_qr_dialog"),
            shape = RoundedCornerShape(16.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (existingQr == null) "Add Payment QR / Barcode" else "Edit Payment QR Profile",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Navy900
                )

                Text(
                    text = "Select payment type, enter account info, and upload a clear QR code or payment barcode image.",
                    fontSize = 12.sp,
                    color = Slate600
                )

                // Presets
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(paymentPresets) { preset ->
                        val isSelected = (selectedType == preset)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedType = preset
                                if (profileName.isBlank() || paymentPresets.contains(profileName)) {
                                    profileName = preset
                                }
                            },
                            label = { Text(preset, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                        )
                    }
                }

                // Image Upload / Preview Area
                Surface(
                    color = Slate50,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (validationError != null) Rose500 else Slate300),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val previewBitmap = remember(imagePath) {
                            if (imagePath.isNotBlank()) PaymentQrImageHelper.getQrBitmap(imagePath) else null
                        }

                        if (previewBitmap != null) {
                            Image(
                                bitmap = previewBitmap.asImageBitmap(),
                                contentDescription = "Selected QR Preview",
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .border(1.dp, Slate300, RoundedCornerShape(6.dp))
                                    .background(Color.White)
                                    .padding(4.dp)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { qrImagePicker.launch("image/*") },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Replace Image", fontSize = 11.sp)
                                }

                                TextButton(
                                    onClick = { imagePath = "" },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Rose600)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Remove", fontSize = 11.sp)
                                }
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = Blue600,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Upload QR Code or Barcode Image",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Navy900
                            )
                            Text(
                                text = "Tap below to select image from device gallery / downloads (PNG, JPG)",
                                fontSize = 11.sp,
                                color = Slate500,
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { qrImagePicker.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.testTag("select_qr_image_button")
                            ) {
                                Text("Choose Image", fontSize = 12.sp)
                            }
                        }
                    }
                }

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = Rose600,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Profile Name
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("Display Name") },
                    placeholder = { Text("e.g. Easypaisa Shop Account") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Account Title & Account Number
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = accountTitle,
                        onValueChange = { accountTitle = it },
                        label = { Text("Account Title (Optional)") },
                        placeholder = { Text("CH UMER") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it },
                        label = { Text("Account # / IBAN") },
                        placeholder = { Text("0308-0018035") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                // Short Instructions
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Short Instructions on Receipt") },
                    placeholder = { Text("Scan QR to pay directly") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                )

                // Make Active on Invoices Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { makeActiveOnInvoice = !makeActiveOnInvoice },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = makeActiveOnInvoice,
                        onCheckedChange = { makeActiveOnInvoice = it }
                    )
                    Column {
                        Text(
                            text = "Set as Active Payment QR on Invoices",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Navy900
                        )
                        Text(
                            text = "This QR will be immediately rendered on customer receipts",
                            fontSize = 11.sp,
                            color = Slate600
                        )
                    }
                }

                // Dialog Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (profileName.isBlank()) {
                                validationError = "Please provide a profile name"
                                return@Button
                            }
                            if (imagePath.isBlank() || !PaymentQrImageHelper.isImageValid(imagePath)) {
                                validationError = "Please select and upload a valid QR code image"
                                return@Button
                            }

                            val configToSave = PaymentQrConfig(
                                id = existingQr?.id ?: 0L,
                                name = profileName.trim(),
                                paymentType = selectedType.trim(),
                                imagePath = imagePath,
                                accountTitle = accountTitle.trim(),
                                accountNumber = accountNumber.trim(),
                                instructions = instructions.trim(),
                                isEnabled = isEnabled,
                                isDefault = makeActiveOnInvoice,
                                createdAt = existingQr?.createdAt ?: System.currentTimeMillis()
                            )
                            onSave(configToSave, makeActiveOnInvoice)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("save_qr_profile_button")
                    ) {
                        Text("Save Profile", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Owner Security Verification Dialog.
 *
 * Dedicated security enforcement matching the Owner / Developer Security system.
 * Unauthorized backdoors (9999, phone numbers) are strictly rejected.
 */
@Composable
fun OwnerAuthVerificationDialog(
    ownerSecurityManager: OwnerSecurityManager,
    onDismiss: () -> Unit,
    onVerified: () -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = Blue600)
                Text("Owner Security Verification", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Payment QR & Scan-to-Pay settings require Dedicated Owner Authorization.",
                    fontSize = 13.sp,
                    color = Slate700
                )
                Text(
                    text = "Enter your Dedicated Owner Security Password / PIN or Security Key:",
                    fontSize = 12.sp,
                    color = Slate600
                )

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = {
                        pinInput = it
                        errorMessage = null
                    },
                    label = { Text("Owner Password / PIN / Key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("owner_auth_pin_input"),
                    shape = RoundedCornerShape(8.dp)
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Rose600,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pinInput.isBlank()) {
                        errorMessage = "Please enter Owner Credential"
                        return@Button
                    }
                    val isAuthorized = ownerSecurityManager.verifyCredential(pinInput)
                    if (isAuthorized) {
                        onVerified()
                    } else {
                        errorMessage = "Invalid Owner Credential. Access Denied."
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                modifier = Modifier.testTag("verify_owner_auth_button")
            ) {
                Text("Verify", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
