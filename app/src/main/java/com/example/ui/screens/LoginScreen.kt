package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel

@Composable
fun LoginScreen(
    viewModel: StoreViewModel,
    onLoginSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("admin") }
    var pin by remember { mutableStateOf("1234") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showRecoveryDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy900)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    color = Navy900,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = Gold500,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(36.dp)
                    )
                }

                Text(
                    text = "CH UMER POS",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Navy900
                )
                Text(
                    text = "Enterprise POS • 03080018035",
                    fontSize = 12.sp,
                    color = Navy500
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("login_username")
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("PIN / Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("login_pin")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = Rose600,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Button(
                    onClick = {
                        if (username.isNotBlank() && pin.isNotBlank()) {
                            onLoginSuccess()
                        } else {
                            errorMessage = "Please enter valid credentials."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_submit_button")
                ) {
                    Text("Unlock POS Terminal")
                }

                TextButton(
                    onClick = { showRecoveryDialog = true },
                    modifier = Modifier.testTag("login_forgot_password_button")
                ) {
                    Text(
                        text = "Forgot PIN / Emergency Password Recovery",
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
