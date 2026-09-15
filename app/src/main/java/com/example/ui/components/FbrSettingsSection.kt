package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.StoreSettings
import com.example.data.fbr.FbrConnectionStatus
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FbrSettingsSection(
    viewModel: StoreViewModel,
    storeSettings: StoreSettings?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fbrRecords by viewModel.fbrRecords.collectAsState()

    var isFbrEnabled by remember(storeSettings) {
        mutableStateOf(storeSettings?.isFbrIntegrationEnabled ?: false)
    }
    var fbrPosId by remember(storeSettings) {
        mutableStateOf(storeSettings?.fbrPosId ?: "")
    }
    var fbrNtn by remember(storeSettings) {
        mutableStateOf(storeSettings?.fbrNtn ?: "")
    }
    var fbrStrn by remember(storeSettings) {
        mutableStateOf(storeSettings?.fbrStrn ?: "")
    }
    var fbrBusinessName by remember(storeSettings) {
        mutableStateOf(storeSettings?.fbrBusinessName ?: (storeSettings?.storeName ?: ""))
    }
    var fbrEnvironment by remember(storeSettings) {
        mutableStateOf(storeSettings?.fbrEnvironment ?: "Sandbox")
    }
    var fbrApiAuthToken by remember(storeSettings) {
        mutableStateOf(storeSettings?.fbrApiAuthToken ?: "")
    }
    var fbrDefaultTaxRate by remember(storeSettings) {
        mutableStateOf((storeSettings?.fbrDefaultTaxRate ?: 0.0).toString())
    }
    var fbrTaxMode by remember(storeSettings) {
        mutableStateOf(storeSettings?.fbrTaxMode ?: "Exclusive")
    }

    var isTokenVisible by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }
    var connectionStatus by remember { mutableStateOf<FbrConnectionStatus?>(null) }
    var connectionMessage by remember { mutableStateOf<String?>(null) }
    var showSavedMessage by remember { mutableStateOf(false) }

    val pendingCount = remember(fbrRecords) { fbrRecords.count { it.submissionStatus == "PENDING" || it.submissionStatus == "RETRY_REQUIRED" } }
    val acknowledgedCount = remember(fbrRecords) { fbrRecords.count { it.submissionStatus == "ACKNOWLEDGED" } }
    val failedCount = remember(fbrRecords) { fbrRecords.count { it.submissionStatus == "FAILED" } }

    SectionHeader(
        title = "FBR / Digital Tax Integration (Optional Master Module)",
        subtitle = "Official Real-Time Federal Board of Revenue Digital Invoicing Integration"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("fbr_settings_card"),
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
            // Master Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = if (isFbrEnabled) Emerald100 else Slate100,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = if (isFbrEnabled) Emerald700 else Navy500,
                            modifier = Modifier.padding(8.dp).size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Enable FBR Integration (Master Module)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Navy900
                        )
                        Text(
                            text = if (isFbrEnabled) "ACTIVE — Transactions validated and reported" else "OFF — Standard POS mode (Zero external dependency)",
                            fontSize = 11.sp,
                            color = if (isFbrEnabled) Emerald700 else Navy500
                        )
                    }
                }
                Switch(
                    checked = isFbrEnabled,
                    onCheckedChange = { isFbrEnabled = it },
                    modifier = Modifier.testTag("fbr_master_switch")
                )
            }

            if (isFbrEnabled) {
                HorizontalDivider(color = Slate100)

                // Environment Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("FBR Gateway Environment", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Navy900)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = fbrEnvironment.equals("Sandbox", ignoreCase = true),
                            onClick = { fbrEnvironment = "Sandbox" },
                            label = { Text("Sandbox (Testing)") }
                        )
                        FilterChip(
                            selected = fbrEnvironment.equals("Live", ignoreCase = true) || fbrEnvironment.equals("Production", ignoreCase = true),
                            onClick = { fbrEnvironment = "Live" },
                            label = { Text("Production (Live)") }
                        )
                    }
                }

                // POS ID
                OutlinedTextField(
                    value = fbrPosId,
                    onValueChange = { fbrPosId = it.filter { char -> char.isDigit() } },
                    label = { Text("FBR POS ID (e.g. 100012)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("fbr_pos_id_field")
                )

                // NTN & STRN Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = fbrNtn,
                        onValueChange = { fbrNtn = it },
                        label = { Text("NTN") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fbrStrn,
                        onValueChange = { fbrStrn = it },
                        label = { Text("STRN (Optional)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Business Name
                OutlinedTextField(
                    value = fbrBusinessName,
                    onValueChange = { fbrBusinessName = it },
                    label = { Text("Registered Business Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // API Bearer Token
                OutlinedTextField(
                    value = fbrApiAuthToken,
                    onValueChange = { fbrApiAuthToken = it },
                    label = { Text("FBR API Auth Token (Bearer Token)") },
                    visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
                            Icon(
                                imageVector = if (isTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isTokenVisible) "Hide Token" else "Show Token",
                                tint = Navy500
                            )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("fbr_token_field")
                )

                // Tax Configuration
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = fbrDefaultTaxRate,
                        onValueChange = { fbrDefaultTaxRate = it },
                        label = { Text("Default Tax Rate %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text("Tax Mode", fontSize = 11.sp, color = Navy700)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = fbrTaxMode.equals("Exclusive", ignoreCase = true),
                                onClick = { fbrTaxMode = "Exclusive" },
                                label = { Text("Exclusive", fontSize = 11.sp) }
                            )
                            FilterChip(
                                selected = fbrTaxMode.equals("Inclusive", ignoreCase = true),
                                onClick = { fbrTaxMode = "Inclusive" },
                                label = { Text("Inclusive", fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Connection Verification Section
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Slate50,
                    border = BorderStroke(1.dp, Slate200),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Gateway Connection Status", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Navy900)
                                if (connectionStatus != null) {
                                    Text(
                                        text = connectionStatus!!.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = when (connectionStatus) {
                                            FbrConnectionStatus.CONNECTED -> Emerald700
                                            FbrConnectionStatus.OFFLINE -> Amber700
                                            FbrConnectionStatus.AUTHENTICATION_FAILED -> Rose600
                                            else -> Navy700
                                        }
                                    )
                                } else {
                                    Text("Status: Not Verified", fontSize = 11.sp, color = Slate500)
                                }
                            }

                            Button(
                                onClick = {
                                    isTestingConnection = true
                                    viewModel.testFbrConnection { status, msg ->
                                        isTestingConnection = false
                                        connectionStatus = status
                                        connectionMessage = msg
                                    }
                                },
                                enabled = !isTestingConnection,
                                colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                if (isTestingConnection) {
                                    CircularProgressIndicator(modifier = Modifier.size(12.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Verifying...", fontSize = 11.sp)
                                } else {
                                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Test Connection", fontSize = 11.sp)
                                }
                            }
                        }

                        if (connectionMessage != null) {
                            Text(
                                text = connectionMessage!!,
                                fontSize = 11.sp,
                                color = if (connectionStatus == FbrConnectionStatus.CONNECTED) Emerald800 else Rose700
                            )
                        }
                    }
                }

                // Queue Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = Emerald50
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Acknowledged", fontSize = 10.sp, color = Emerald800)
                            Text("$acknowledgedCount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Emerald800)
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = Amber50
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Pending Queue", fontSize = 10.sp, color = Amber800)
                            Text("$pendingCount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Amber800)
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = Rose50
                    ) {
                        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Failed/Retry", fontSize = 10.sp, color = Rose800)
                            Text("$failedCount", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Rose800)
                        }
                    }
                }

                // Save Configuration Button
                Button(
                    onClick = {
                        val current = storeSettings ?: StoreSettings()
                        val updated = current.copy(
                            isFbrIntegrationEnabled = isFbrEnabled,
                            fbrPosId = fbrPosId.trim(),
                            fbrNtn = fbrNtn.trim(),
                            fbrStrn = fbrStrn.trim(),
                            fbrBusinessName = fbrBusinessName.trim(),
                            fbrEnvironment = fbrEnvironment,
                            fbrApiAuthToken = fbrApiAuthToken.trim(),
                            fbrDefaultTaxRate = fbrDefaultTaxRate.toDoubleOrNull() ?: 0.0,
                            fbrTaxMode = fbrTaxMode
                        )
                        viewModel.updateStoreSettings(updated)
                        showSavedMessage = true
                        Toast.makeText(context, "FBR Integration settings saved successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    modifier = Modifier.fillMaxWidth().testTag("save_fbr_settings_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save FBR Configuration", fontWeight = FontWeight.Bold)
                }

                if (showSavedMessage) {
                    Text(
                        text = "FBR configuration updated. All sales will comply with official FBR policies.",
                        fontSize = 11.sp,
                        color = Emerald700,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                // If disabled, provide quick save to persist disabled state
                Button(
                    onClick = {
                        val current = storeSettings ?: StoreSettings()
                        viewModel.updateStoreSettings(current.copy(isFbrIntegrationEnabled = false))
                        Toast.makeText(context, "FBR Integration turned OFF.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Slate200, contentColor = Navy900),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Confirm FBR Module Disabled")
                }
            }
        }
    }
}
