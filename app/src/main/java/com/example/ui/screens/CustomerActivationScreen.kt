package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.network.ConnectionState
import com.example.data.api.security.AppActivationManager
import com.example.ui.viewmodel.StoreViewModel
import kotlinx.coroutines.launch

/**
 * Mandatory First-Time Installation Activation Screen for CH UMER SENTRY STORE POS.
 * Shows unique Installation ID, Activation Code input, and handles online/offline cryptographic authorization.
 */
@Composable
fun CustomerActivationScreen(
    viewModel: StoreViewModel,
    onActivated: () -> Unit,
    onOpenDeveloperPortal: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val activationManager = remember { AppActivationManager.getInstance(context) }
    val connectionStatus by viewModel.connectionStatus.collectAsState()

    val installationId = remember { activationManager.getInstallationId() }
    var activationCodeInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccessStatus by remember { mutableStateOf<Boolean?>(null) }

    // Hidden Developer Mode Access (10 taps on shield logo)
    var showDevPasscodeDialog by remember { mutableStateOf(false) }
    var devPasscodeInput by remember { mutableStateOf("") }
    var logoTapCount by remember { mutableStateOf(0) }
    var lastTapTime by remember { mutableStateOf(0L) }

    val handleLogoTap = {
        val now = System.currentTimeMillis()
        if (now - lastTapTime > 2000) {
            logoTapCount = 1
        } else {
            logoTapCount++
        }
        lastTapTime = now

        if (logoTapCount >= 10) {
            logoTapCount = 0
            showDevPasscodeDialog = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Dark slate
                        Color(0xFF0F2537), // Navy
                        Color(0xFF0A1120)  // Deep Navy
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Shield Logo (10-tap Developer Trigger)
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF0F2537))
                        )
                    )
                    .clickable { handleLogoTap() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Application Security",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(46.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Activate CH UMER POS",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = 1.2.sp
            )

            Text(
                text = "Application Activation Required",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF38BDF8)
            )

            Text(
                text = "First-Time Installation Authorization via Developer Server",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Live Server Connectivity Pill
            Surface(
                onClick = { viewModel.pingServerNow() },
                shape = RoundedCornerShape(20.dp),
                color = when (connectionStatus.state) {
                    ConnectionState.ONLINE_CONNECTED -> Color(0xFF064E3B)
                    ConnectionState.ONLINE_UNREACHABLE -> Color(0xFF78350F)
                    ConnectionState.OFFLINE -> Color(0xFF450A0A)
                },
                border = BorderStroke(
                    1.dp,
                    when (connectionStatus.state) {
                        ConnectionState.ONLINE_CONNECTED -> Color(0xFF10B981)
                        ConnectionState.ONLINE_UNREACHABLE -> Color(0xFFF59E0B)
                        ConnectionState.OFFLINE -> Color(0xFFEF4444)
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (connectionStatus.state) {
                                    ConnectionState.ONLINE_CONNECTED -> Color(0xFF34D399)
                                    ConnectionState.ONLINE_UNREACHABLE -> Color(0xFFFBBF24)
                                    ConnectionState.OFFLINE -> Color(0xFFF87171)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (connectionStatus.state) {
                            ConnectionState.ONLINE_CONNECTED -> "Server: Connected (${connectionStatus.latencyMs}ms)"
                            ConnectionState.ONLINE_UNREACHABLE -> "Server: Unreachable (Online)"
                            ConnectionState.OFFLINE -> "Server: Offline (No Internet)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Activation Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Installation ID Header Box
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Installation ID:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(installationId))
                                Toast.makeText(context, "Installation ID copied!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy ID",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Text(
                            text = installationId,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Activation Code Input
                    Text(
                        text = "Activation Code:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = activationCodeInput,
                        onValueChange = { activationCodeInput = it.uppercase() },
                        placeholder = {
                            Text(
                                "e.g. ACTV-XXXX-XXXX-XXXX-XXXX",
                                color = Color(0xFF64748B),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Key,
                                contentDescription = null,
                                tint = Color(0xFFFFD700)
                            )
                        },
                        trailingIcon = {
                            if (activationCodeInput.isNotEmpty()) {
                                IconButton(onClick = { activationCodeInput = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF475569)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Unique code issued by CH Umer Developer Admin for this installation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Status Message Feedback Banner
                    AnimatedVisibility(
                        visible = statusMessage != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        statusMessage?.let { msg ->
                            val isSuccess = isSuccessStatus == true
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSuccess) Color(0xFF064E3B) else Color(0xFF450A0A),
                                border = BorderStroke(1.dp, if (isSuccess) Color(0xFF10B981) else Color(0xFFEF4444))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (isSuccess) Color(0xFF34D399) else Color(0xFFF87171),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSuccess) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
                                    )
                                }
                            }
                        }
                    }

                    // Activate Button
                    Button(
                        onClick = {
                            val code = activationCodeInput.trim()
                            if (code.isBlank()) {
                                statusMessage = "Please enter your activation code."
                                isSuccessStatus = false
                                return@Button
                            }

                            isSubmitting = true
                            statusMessage = "Activating application with Developer Server..."
                            isSuccessStatus = null

                            scope.launch {
                                activationManager.activateWithCode(code) { status, message, isSuccess ->
                                    isSubmitting = false
                                    isSuccessStatus = isSuccess
                                    statusMessage = message

                                    if (isSuccess) {
                                        viewModel.logActivity(
                                            "APP_ACTIVATION_SUCCESS",
                                            "Application successfully activated on Installation ID: $installationId"
                                        )
                                        Toast.makeText(context, "Activation Successful! Welcome to POS.", Toast.LENGTH_SHORT).show()
                                        onActivated()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2563EB),
                            disabledContainerColor = Color(0xFF1E3A8A)
                        )
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Activating...",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        } else {
                            Icon(
                                Icons.Default.LockOpen,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "ACTIVATE APPLICATION",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer Contact Developer Notice
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A).copy(alpha = 0.8f)),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Need an Activation Code?",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Contact Developer/Admin if you do not have an activation code. Provide your unique Installation ID shown above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }

    // Hidden Developer Access Passcode Dialog
    if (showDevPasscodeDialog) {
        AlertDialog(
            onDismissRequest = { showDevPasscodeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeveloperMode, contentDescription = null, tint = Color(0xFFFFD700))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Developer Portal Access", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        "Enter Developer Passcode to access Developer Control Center:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = devPasscodeInput,
                        onValueChange = { devPasscodeInput = it },
                        label = { Text("Developer Passcode") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val validPasscodes = listOf("7860", "9999", "8888", "1234")
                        if (devPasscodeInput.trim() in validPasscodes) {
                            showDevPasscodeDialog = false
                            devPasscodeInput = ""
                            onOpenDeveloperPortal()
                        } else {
                            Toast.makeText(context, "Incorrect Developer Passcode", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Unlock Developer Mode")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDevPasscodeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
