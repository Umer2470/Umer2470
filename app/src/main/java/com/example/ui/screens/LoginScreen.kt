package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.components.ShopLogoAvatar
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.BiometricPromptHelper

@Composable
fun LoginScreen(
    viewModel: StoreViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val storeSettings by viewModel.storeSettings.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricAuthEnabled.collectAsState()
    val users by viewModel.users.collectAsState()

    var username by remember { mutableStateOf("admin") }
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showRecoveryDialog by remember { mutableStateOf(false) }

    // Quick role presets
    val rolePresets = listOf(
        Triple("admin", "👑 Super Admin", UserRole.SUPER_ADMIN),
        Triple("manager", "👨‍💼 Supervisor", UserRole.SUPERVISOR),
        Triple("umer", "👤 Cashier", UserRole.CASHIER)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy900)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ShopLogoAvatar(
                    logoUri = storeSettings?.logoUri,
                    size = 56.dp,
                    shape = RoundedCornerShape(12.dp),
                    borderColor = Gold500,
                    borderWidth = 2.dp
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = storeSettings?.appDisplayName?.ifBlank { storeSettings?.storeName } ?: "SENTRY STORE POS",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Navy900
                    )
                    Text(
                        text = "Application-Level Role Authentication",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Navy500
                    )
                }

                // Quick User / Role Selector Chips
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "SELECT USER / ROLE ACCOUNT",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        letterSpacing = 0.5.sp
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("login_role_chips_row")
                    ) {
                        val availableUsers = if (users.isNotEmpty()) users.filter { it.isActive } else emptyList()
                        if (availableUsers.isNotEmpty()) {
                            items(availableUsers) { u ->
                                val isSelected = username.equals(u.username, ignoreCase = true)
                                val role = UserRole.fromString(u.role)
                                Surface(
                                    onClick = {
                                        username = u.username
                                        errorMessage = null
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Navy900 else Slate100,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) Gold500 else Slate300
                                    ),
                                    modifier = Modifier.testTag("user_chip_${u.username}")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = role.displayName,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Gold400 else Navy800
                                        )
                                        Text(
                                            text = "@${u.username}",
                                            fontSize = 10.5.sp,
                                            color = if (isSelected) Color.White.copy(alpha = 0.8f) else Slate500
                                        )
                                    }
                                }
                            }
                        } else {
                            items(rolePresets) { (presetUsername, label, _) ->
                                val isSelected = username.equals(presetUsername, ignoreCase = true)
                                Surface(
                                    onClick = {
                                        username = presetUsername
                                        errorMessage = null
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) Navy900 else Slate100,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) Gold500 else Slate300
                                    ),
                                    modifier = Modifier.testTag("preset_chip_$presetUsername")
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Gold400 else Navy800,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Biometric Quick Unlock Option (if enabled)
                if (isBiometricEnabled) {
                    OutlinedButton(
                        onClick = {
                            errorMessage = null
                            BiometricPromptHelper.authenticateUser(
                                context = context,
                                title = "SENTRY STORE POS Unlock",
                                subtitle = "Verify fingerprint or biometric ID to access terminal",
                                negativeButtonText = "Use Role PIN",
                                onSuccess = {
                                    errorMessage = null
                                    viewModel.unlockTerminal()
                                    onLoginSuccess()
                                },
                                onError = { error ->
                                    errorMessage = error
                                }
                            )
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Emerald700,
                            containerColor = Emerald50
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Emerald600),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("biometric_unlock_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Biometric Unlock",
                            tint = Emerald600,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Quick Biometric Unlock",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Slate200)
                        Text(
                            text = "  OR AUTHENTICATE WITH CREDENTIALS  ",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy400
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Slate200)
                    }
                }

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        errorMessage = null
                    },
                    label = { Text("Username / Account ID") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Navy600)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("login_username")
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = it
                        errorMessage = null
                    },
                    label = { Text("PIN / Password") },
                    placeholder = { Text("Enter account PIN") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Navy600)
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("login_pin")
                )

                if (errorMessage != null) {
                    Surface(
                        color = Rose50,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Rose300),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Rose600, modifier = Modifier.size(18.dp))
                            Text(
                                text = errorMessage!!,
                                color = Rose700,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (username.isBlank() || pin.isBlank()) {
                            errorMessage = "Please enter both username and PIN."
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null
                        viewModel.login(username, pin) { success, message ->
                            isLoading = false
                            if (success) {
                                onLoginSuccess()
                            } else {
                                errorMessage = message
                            }
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("login_submit_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Gold400, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Authenticate & Unlock POS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                TextButton(
                    onClick = { showRecoveryDialog = true },
                    modifier = Modifier.testTag("login_forgot_password_button")
                ) {
                    Text(
                        text = "Forgot PIN / Emergency Recovery",
                        fontSize = 12.sp,
                        color = Gold600,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (showRecoveryDialog) {
            var recoveryUsername by remember { mutableStateOf(username) }
            var recoveryKey by remember { mutableStateOf("") }
            var newPin by remember { mutableStateOf("") }
            var recoveryStatus by remember { mutableStateOf<String?>(null) }
            var recoverySuccess by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showRecoveryDialog = false },
                title = {
                    Text(
                        text = "Security Password Recovery",
                        fontWeight = FontWeight.Bold,
                        color = Navy900
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Enter your username, 20-character emergency code, 12-word passphrase, or Master Admin Key to reset your PIN.",
                            fontSize = 12.sp,
                            color = Navy600
                        )

                        OutlinedTextField(
                            value = recoveryUsername,
                            onValueChange = { recoveryUsername = it },
                            label = { Text("Username") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("recovery_username_input")
                        )

                        OutlinedTextField(
                            value = recoveryKey,
                            onValueChange = { recoveryKey = it },
                            label = { Text("Recovery Key / Passphrase / Master PIN") },
                            placeholder = { Text("e.g. 03080018035 or 20-char code") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("recovery_key_input")
                        )

                        OutlinedTextField(
                            value = newPin,
                            onValueChange = { newPin = it },
                            label = { Text("New PIN / Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("recovery_new_pin_input")
                        )

                        if (recoveryStatus != null) {
                            Text(
                                text = recoveryStatus!!,
                                color = if (recoverySuccess) Emerald600 else Rose600,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (recoveryUsername.isBlank() || recoveryKey.isBlank() || newPin.isBlank()) {
                                recoveryStatus = "Please fill in all recovery fields."
                                recoverySuccess = false
                            } else {
                                viewModel.resetUserPin(recoveryUsername, newPin, recoveryKey) { success, msg ->
                                    recoverySuccess = success
                                    recoveryStatus = msg
                                    if (success) {
                                        pin = newPin
                                        username = recoveryUsername
                                        errorMessage = null
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                        modifier = Modifier.testTag("recovery_confirm_button")
                    ) {
                        Text("Reset PIN")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRecoveryDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}
