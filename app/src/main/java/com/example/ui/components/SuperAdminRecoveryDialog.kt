package com.example.ui.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.SuperAdminRecovery
import com.example.ui.viewmodel.StoreViewModel
import kotlinx.coroutines.delay

@Composable
fun SuperAdminRecoveryDialog(
    viewModel: StoreViewModel,
    onDismiss: () -> Unit
) {
    val recoveryState by viewModel.superAdminRecovery.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Passphrase, 1: Emergency Code, 2: OTP Email/Mobile

    var passphraseInput by remember { mutableStateOf("") }
    var emergencyCodeInput by remember { mutableStateOf("") }
    var contactInput by remember { mutableStateOf("") }
    var otpInput by remember { mutableStateOf("") }
    var expectedOtp by remember { mutableStateOf<String?>(null) }

    var isOtpSent by remember { mutableStateOf(false) }
    var otpTimerSeconds by remember { mutableIntStateOf(0) }

    var recoveryVerified by remember { mutableStateOf(false) }
    var verifiedMethod by remember { mutableStateOf("") }

    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    var remainingLockoutSec by remember { mutableStateOf(0L) }

    val recovery = recoveryState ?: SuperAdminRecovery(isConfigured = false)

    // Pre-fill email or phone if available
    LaunchedEffect(recoveryState) {
        if (contactInput.isBlank()) {
            if (recovery.recoveryEmail.isNotBlank()) {
                contactInput = recovery.recoveryEmail
            } else if (recovery.recoveryMobile.isNotBlank()) {
                contactInput = recovery.recoveryMobile
            }
        }
    }

    // Live lockout countdown effect
    LaunchedEffect(recoveryState?.lockoutEndTimeMs, recoveryState?.failedAttemptsCount) {
        while (true) {
            val rec = viewModel.getSuperAdminRecoverySync()
            if (rec != null && rec.isLockedOut()) {
                remainingLockoutSec = rec.getRemainingLockoutSeconds()
            } else {
                remainingLockoutSec = 0L
            }
            delay(1000L)
        }
    }

    // OTP Resend Timer
    LaunchedEffect(isOtpSent, otpTimerSeconds) {
        if (isOtpSent && otpTimerSeconds > 0) {
            delay(1000L)
            otpTimerSeconds -= 1
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0F172A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Super Admin Recovery",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "3-Level Emergency Access System",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Lockout Warning Card
                if (remainingLockoutSec > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockClock,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Recovery Locked",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF991B1B)
                                )
                                val mins = remainingLockoutSec / 60
                                val secs = remainingLockoutSec % 60
                                Text(
                                    text = "5 failed attempts reached. Try again in ${mins}m ${secs}s.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB91C1C)
                                )
                            }
                        }
                    }
                }

                if (!recoveryVerified) {
                    // STEP 1: VERIFICATION METHOD TABS
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFFF8FAFC),
                        contentColor = Color(0xFF0284C7)
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = {
                                selectedTab = 0
                                errorMessage = null
                            },
                            text = { Text("12-Word Pass", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = {
                                selectedTab = 1
                                errorMessage = null
                            },
                            text = { Text("Emergency Code", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = {
                                selectedTab = 2
                                errorMessage = null
                            },
                            text = { Text("Email/Mobile OTP", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    // TAB CONTENT
                    when (selectedTab) {
                        // TAB 0: 12-Word Passphrase
                        0 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF0F9FF),
                                    border = BorderStroke(1.dp, Color(0xFFBAE6FD))
                                ) {
                                    Text(
                                        text = "• Level 1 Passphrase works 100% Offline.\n• Enter all 12 words in order separated by spaces.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF0369A1),
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }

                                OutlinedTextField(
                                    value = passphraseInput,
                                    onValueChange = { passphraseInput = it },
                                    label = { Text("Enter 12-Word Passphrase") },
                                    placeholder = { Text("e.g. anchor castle dragon forest ...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3,
                                    maxLines = 4,
                                    enabled = remainingLockoutSec <= 0 && !isLoading,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF0284C7)
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        onClick = {
                                            val clipText = clipboardManager.getText()?.text ?: ""
                                            if (clipText.isNotBlank()) {
                                                passphraseInput = clipText
                                            }
                                        },
                                        enabled = remainingLockoutSec <= 0
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Paste Passphrase", fontSize = 11.sp)
                                    }
                                }

                                Button(
                                    onClick = {
                                        isLoading = true
                                        errorMessage = null
                                        viewModel.verifyPassphrase(passphraseInput) { success, msg ->
                                            isLoading = false
                                            if (success) {
                                                recoveryVerified = true
                                                verifiedMethod = "12-Word Recovery Passphrase"
                                                successMessage = msg
                                            } else {
                                                errorMessage = msg
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = passphraseInput.isNotBlank() && remainingLockoutSec <= 0 && !isLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                                    } else {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Verify Passphrase & Reset PIN")
                                    }
                                }
                            }
                        }

                        // TAB 1: Emergency Code
                        1 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF0FDF4),
                                    border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                                ) {
                                    Text(
                                        text = "• Level 2 Emergency Code works 100% Offline.\n• Enter your 20-character code (formatted e.g. ABCD-1234-EFGH-5678-JK90).",
                                        fontSize = 11.sp,
                                        color = Color(0xFF15803D),
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }

                                OutlinedTextField(
                                    value = emergencyCodeInput,
                                    onValueChange = { emergencyCodeInput = it.uppercase() },
                                    label = { Text("20-Character Emergency Code") },
                                    placeholder = { Text("XXXX-XXXX-XXXX-XXXX-XXXX") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = remainingLockoutSec <= 0 && !isLoading,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF16A34A)
                                    )
                                )

                                Button(
                                    onClick = {
                                        isLoading = true
                                        errorMessage = null
                                        viewModel.verifyEmergencyCode(emergencyCodeInput) { success, msg ->
                                            isLoading = false
                                            if (success) {
                                                recoveryVerified = true
                                                verifiedMethod = "Emergency Recovery Code"
                                                successMessage = msg
                                            } else {
                                                errorMessage = msg
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = emergencyCodeInput.isNotBlank() && remainingLockoutSec <= 0 && !isLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                                    } else {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Verify Code & Reset PIN")
                                    }
                                }
                            }
                        }

                        // TAB 2: Email / Mobile OTP
                        2 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFAF5FF),
                                    border = BorderStroke(1.dp, Color(0xFFE9D5FF))
                                ) {
                                    Text(
                                        text = "• Level 3 OTP Verification.\n• Sends a 6-digit OTP code to registered Super Admin Email or Mobile.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF7E22CE),
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }

                                OutlinedTextField(
                                    value = contactInput,
                                    onValueChange = { contactInput = it },
                                    label = { Text("Registered Email or Mobile") },
                                    placeholder = { Text("admin@store.com or +923001234567") },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = remainingLockoutSec <= 0 && !isLoading,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF9333EA)
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Button(
                                        onClick = {
                                            isLoading = true
                                            errorMessage = null
                                            viewModel.requestRecoveryOtp(contactInput, true) { success, msg, generatedOtp ->
                                                isLoading = false
                                                if (success) {
                                                    isOtpSent = true
                                                    otpTimerSeconds = 60
                                                    expectedOtp = generatedOtp
                                                    successMessage = msg
                                                } else {
                                                    errorMessage = msg
                                                }
                                            }
                                        },
                                        enabled = contactInput.isNotBlank() && remainingLockoutSec <= 0 && otpTimerSeconds <= 0 && !isLoading,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA))
                                    ) {
                                        if (otpTimerSeconds > 0) {
                                            Text("Resend in ${otpTimerSeconds}s", fontSize = 11.sp)
                                        } else {
                                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isOtpSent) "Resend OTP" else "Send OTP Code", fontSize = 11.sp)
                                        }
                                    }
                                }

                                if (isOtpSent) {
                                    OutlinedTextField(
                                        value = otpInput,
                                        onValueChange = { if (it.length <= 6) otpInput = it },
                                        label = { Text("Enter 6-Digit OTP Code") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = remainingLockoutSec <= 0 && !isLoading,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF9333EA)
                                        )
                                    )

                                    Button(
                                        onClick = {
                                            isLoading = true
                                            errorMessage = null
                                            viewModel.verifyRecoveryOtp(contactInput, otpInput, expectedOtp ?: "") { success, msg ->
                                                isLoading = false
                                                if (success) {
                                                    recoveryVerified = true
                                                    verifiedMethod = "OTP Email/Mobile Verification"
                                                    successMessage = msg
                                                } else {
                                                    errorMessage = msg
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = otpInput.length == 6 && remainingLockoutSec <= 0 && !isLoading,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9333EA))
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                                        } else {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Verify OTP & Reset PIN")
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // STEP 2: RECOVERY VERIFIED -> SET NEW PIN
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(24.dp))
                                Column {
                                    Text("Identity Verified!", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF14532D))
                                    Text("Verified via $verifiedMethod. Please enter a new 4-digit PIN for Super Admin.", fontSize = 11.sp, color = Color(0xFF166534))
                                }
                            }
                        }

                        OutlinedTextField(
                            value = newPinInput,
                            onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) newPinInput = it },
                            label = { Text("New 4-Digit PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0284C7))
                        )

                        OutlinedTextField(
                            value = confirmPinInput,
                            onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) confirmPinInput = it },
                            label = { Text("Confirm New 4-Digit PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF0284C7))
                        )

                        Button(
                            onClick = {
                                if (newPinInput.length != 4) {
                                    errorMessage = "PIN must be exactly 4 digits."
                                    return@Button
                                }
                                if (newPinInput != confirmPinInput) {
                                    errorMessage = "New PIN and Confirm PIN do not match!"
                                    return@Button
                                }

                                isLoading = true
                                errorMessage = null
                                viewModel.resetSuperAdminPinWithRecovery(newPinInput, verifiedMethod) { success, msg ->
                                    isLoading = false
                                    if (success) {
                                        viewModel.showToast("Super Admin PIN reset successfully!")
                                        onDismiss()
                                    } else {
                                        errorMessage = msg
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = newPinInput.length == 4 && confirmPinInput.length == 4 && !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                            } else {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save New PIN & Authenticate")
                            }
                        }
                    }
                }

                // Error Message Display
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Success Message Display
                if (successMessage != null && errorMessage == null) {
                    Text(
                        text = successMessage!!,
                        color = Color(0xFF16A34A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
