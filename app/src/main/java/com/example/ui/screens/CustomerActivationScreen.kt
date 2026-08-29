package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.security.AppActivationManager
import com.example.ui.components.AppHeader
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel

@Composable
fun CustomerActivationScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val activationState by viewModel.activationState.collectAsState()
    val installationId = remember { viewModel.identityManager.getInstallationId() }
    val isActivated = activationState == AppActivationManager.STATUS_ACTIVATED || activationState == AppActivationManager.STATUS_OFFLINE_ACTIVATED

    var activationCodeInput by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessStatus by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Device Activation",
                subtitle = "Commercial License & Security",
                onBackClick = onNavigateBack
            )
        },
        containerColor = Slate50
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Installation ID Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Your Unique Device Installation ID", style = MaterialTheme.typography.bodySmall, color = Navy500)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = installationId,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Navy900
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Installation ID", installationId)
                                clipboard.setPrimaryClip(clip)
                                statusMessage = "Installation ID copied to clipboard."
                                isSuccessStatus = true
                            },
                            modifier = Modifier.testTag("copy_installation_id_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy ID", tint = Navy900)
                        }
                    }
                    Text("Provide this ID to CH UMER (03080018035) to receive your activation key.", fontSize = 11.sp, color = Navy500)
                }
            }

            // Current Activation Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isActivated) Emerald100 else Rose100
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isActivated) "Status: ACTIVATED" else "Status: NOT ACTIVATED",
                            fontWeight = FontWeight.Bold,
                            color = if (isActivated) Emerald600 else Rose600,
                            fontSize = 15.sp
                        )
                        Text(
                            text = if (isActivated) "Full Commercial POS Features Unlocked" else "Enter 16-character code below",
                            color = Navy700,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = if (isActivated) Icons.Default.CheckCircle else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isActivated) Emerald600 else Rose600,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Code Entry Form
            SectionHeader(title = "Enter Activation Code", subtitle = "Format: ACTV-XXXX-XXXX-XXXX-XXXX")

            OutlinedTextField(
                value = activationCodeInput,
                onValueChange = { activationCodeInput = it },
                label = { Text("Activation Code") },
                placeholder = { Text("e.g. ACTV-A1B2-C3D4-E5F6-7890") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("activation_code_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Navy900,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )

            if (statusMessage != null) {
                Surface(
                    color = if (isSuccessStatus) Emerald100 else Rose100,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusMessage!!,
                        color = if (isSuccessStatus) Emerald600 else Rose600,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Button(
                onClick = {
                    if (activationCodeInput.isNotBlank()) {
                        isLoading = true
                        viewModel.activateApp(activationCodeInput) { status, msg, success ->
                            isLoading = false
                            statusMessage = msg
                            isSuccessStatus = success
                        }
                    }
                },
                enabled = !isLoading && activationCodeInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("activate_submit_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                } else {
                    Text("Activate License Now")
                }
            }

            // Customer License Support Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Slate100)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.HeadsetMic,
                        contentDescription = "Customer Support",
                        tint = Navy700,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Commercial License Support",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Navy900
                        )
                        Text(
                            text = "For store activation assistance, reach official support at 03080018035.",
                            fontSize = 11.sp,
                            color = Navy600
                        )
                    }
                }
            }
        }
    }
}
