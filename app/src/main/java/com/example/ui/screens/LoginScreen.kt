package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.User
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
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val storeSettings by viewModel.storeSettings.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricAuthEnabled.collectAsState()

    var pin by remember { mutableStateOf("") }
    var pinVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var identifiedRoleText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showRecoveryDialog by remember { mutableStateOf(false) }

    fun submitCredential(credentialToSubmit: String) {
        keyboardController?.hide()
        focusManager.clearFocus()
        if (credentialToSubmit.isBlank()) {
            errorMessage = "Please enter your credential."
            return
        }
        isLoading = true
        errorMessage = null
        identifiedRoleText = null

        viewModel.loginWithSingleCredential(credentialToSubmit) { success, message, user ->
            isLoading = false
            if (success && user != null) {
                val role = UserRole.fromString(user.role)
                identifiedRoleText = "Authorized: ${user.fullName.ifBlank { user.username }} (${role.displayName})"
                onLoginSuccess()
            } else {
                errorMessage = message
                identifiedRoleText = null
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy900)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .padding(vertical = 8.dp)
                .testTag("login_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // SENTRY STORE POS BRANDING HEADER
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ShopLogoAvatar(
                        logoUri = storeSettings?.logoUri,
                        size = 50.dp,
                        shape = RoundedCornerShape(12.dp),
                        borderColor = Gold500,
                        borderWidth = 2.dp
                    )
                    Column {
                        Text(
                            text = storeSettings?.appDisplayName?.ifBlank { storeSettings?.storeName } ?: "SENTRY STORE",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Navy900,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "APPLICATION LOGIN",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Gold600,
                            letterSpacing = 1.sp
                        )
                    }
                }

                HorizontalDivider(color = Slate200, thickness = 1.dp)

                Text(
                    text = "Enter your credential below. System will automatically detect and verify your authorized role.",
                    fontSize = 12.5.sp,
                    color = Navy600,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                // Single PIN / Password Entry Field
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = it
                        errorMessage = null
                        identifiedRoleText = null
                    },
                    label = { Text("PIN / Password") },
                    placeholder = { Text("Enter authorized credential") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Navy600)
                    },
                    trailingIcon = {
                        IconButton(onClick = { pinVisible = !pinVisible }) {
                            Icon(
                                imageVector = if (pinVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (pinVisible) "Hide credential" else "Show credential",
                                tint = Slate500
                            )
                        }
                    },
                    visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            submitCredential(pin)
                        }
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Navy900,
                        unfocusedBorderColor = Slate300,
                        focusedContainerColor = Slate50,
                        unfocusedContainerColor = Slate50
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("login_pin")
                )

                // Error Banner
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (errorMessage != null) {
                        Surface(
                            color = Rose50,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Rose300),
                            modifier = Modifier.fillMaxWidth().testTag("login_error_banner")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = Rose600,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = errorMessage!!,
                                    color = Rose700,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Role Identified Banner
                AnimatedVisibility(
                    visible = identifiedRoleText != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (identifiedRoleText != null) {
                        Surface(
                            color = Emerald50,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Emerald300),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Emerald600,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = identifiedRoleText!!,
                                    color = Emerald700,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Professional Numeric Touch Keypad for Fast Counter POS Input
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val keyRows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("C", "0", "⌫")
                    )

                    keyRows.forEach { rowKeys ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowKeys.forEach { key ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .clickable {
                                            keyboardController?.hide()
                                            focusManager.clearFocus()
                                            when (key) {
                                                "C" -> {
                                                    pin = ""
                                                    errorMessage = null
                                                }
                                                "⌫" -> {
                                                    if (pin.isNotEmpty()) {
                                                        pin = pin.dropLast(1)
                                                    }
                                                }
                                                else -> {
                                                    pin += key
                                                    errorMessage = null
                                                }
                                            }
                                        }
                                        .testTag("keypad_key_$key"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = when (key) {
                                        "C" -> Rose50
                                        "⌫" -> Amber50
                                        else -> Slate100
                                    },
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        when (key) {
                                            "C" -> Rose200
                                            "⌫" -> Gold200
                                            else -> Slate200
                                        }
                                    )
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = key,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 17.sp,
                                            color = when (key) {
                                                "C" -> Rose600
                                                "⌫" -> Amber700
                                                else -> Navy900
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Authorize & Unlock Button
                Button(
                    onClick = { submitCredential(pin) },
                    enabled = !isLoading && pin.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("login_submit_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = Gold400,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Authorize & Enter",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }

                // Biometric Unlock Option (if enabled on terminal)
                if (isBiometricEnabled) {
                    OutlinedButton(
                        onClick = {
                            errorMessage = null
                            BiometricPromptHelper.authenticateUser(
                                context = context,
                                title = "SENTRY STORE POS Unlock",
                                subtitle = "Verify biometric identity to enter session",
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
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
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
                            text = "Biometric Quick Unlock",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Emergency Recovery Option for Super Admin
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

        // Emergency Recovery Dialog
        if (showRecoveryDialog) {
            var recoveryUsername by remember { mutableStateOf("") }
            var recoveryKey by remember { mutableStateOf("") }
            var newPin by remember { mutableStateOf("") }
            var recoveryStatus by remember { mutableStateOf<String?>(null) }
            var recoverySuccess by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showRecoveryDialog = false },
                title = {
                    Text(
                        text = "Security Credential Recovery",
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
                            text = "Enter account username, emergency code or recovery passphrase to reset credential.",
                            fontSize = 12.sp,
                            color = Navy600
                        )

                        OutlinedTextField(
                            value = recoveryUsername,
                            onValueChange = { recoveryUsername = it },
                            label = { Text("Account Username") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth().testTag("recovery_username_input")
                        )

                        OutlinedTextField(
                            value = recoveryKey,
                            onValueChange = { recoveryKey = it },
                            label = { Text("Emergency Code / Passphrase") },
                            placeholder = { Text("Enter 20-character code or phrase") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
                            modifier = Modifier.fillMaxWidth().testTag("recovery_key_input")
                        )

                        OutlinedTextField(
                            value = newPin,
                            onValueChange = { newPin = it },
                            label = { Text("New PIN / Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
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
