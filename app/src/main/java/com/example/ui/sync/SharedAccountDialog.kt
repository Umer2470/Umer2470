package com.example.ui.sync

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.viewmodel.StoreViewModel

@Composable
fun SharedAccountDialog(
    viewModel: StoreViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) } // 0: Status/Sync, 1: Login, 2: Register, 3: Recover

    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var storeNameInput by remember { mutableStateOf("") }
    var recoveryPinInput by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }

    val syncStatus by viewModel.syncStatus.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isUrdu by viewModel.isUrdu.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isUrdu) "مشترکہ کلاؤڈ اکاؤنٹ اور ہم آہنگی" else "Cloud Account & Sync",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tabs
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        label = { Text("Status", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        label = { Text("Sign In", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        label = { Text("Register", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = activeTab == 3,
                        onClick = { activeTab = 3 },
                        label = { Text("Recover", fontSize = 12.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                when (activeTab) {
                    0 -> { // Status & Sync Tab
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text("Current Active Account:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(currentUser ?: "Offline Local Store", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Synchronization State:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(syncStatus, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                isProcessing = true
                                viewModel.triggerSync { success, msg ->
                                    isProcessing = false
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isProcessing
                        ) {
                            Text(if (isProcessing) "Syncing with Cloud..." else "🔄 Force Sync Now (ابھی سنک کریں)")
                        }
                    }

                    1 -> { // Sign In Tab
                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                if (usernameInput.isBlank() || passwordInput.isBlank()) {
                                    Toast.makeText(context, "Fill in all fields", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isProcessing = true
                                viewModel.loginSharedAccount(usernameInput, passwordInput) { success, msg ->
                                    isProcessing = false
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    if (success) activeTab = 0
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isProcessing
                        ) {
                            Text("Sign In with Shared Account")
                        }
                    }

                    2 -> { // Register Tab
                        OutlinedTextField(
                            value = storeNameInput,
                            onValueChange = { storeNameInput = it },
                            label = { Text("Store / Branch Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = recoveryPinInput,
                            onValueChange = { recoveryPinInput = it },
                            label = { Text("Recovery PIN (e.g. 7860)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                if (usernameInput.isBlank() || passwordInput.isBlank() || storeNameInput.isBlank()) {
                                    Toast.makeText(context, "Fill in all fields", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isProcessing = true
                                viewModel.registerSharedAccount(usernameInput, passwordInput, storeNameInput, recoveryPinInput.ifBlank { "1234" }) { success, msg ->
                                    isProcessing = false
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    if (success) activeTab = 0
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isProcessing
                        ) {
                            Text("Create Shared Production Account")
                        }
                    }

                    3 -> { // Recover Tab
                        OutlinedTextField(
                            value = usernameInput,
                            onValueChange = { usernameInput = it },
                            label = { Text("Username") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = recoveryPinInput,
                            onValueChange = { recoveryPinInput = it },
                            label = { Text("Recovery PIN") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("New Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                if (usernameInput.isBlank() || recoveryPinInput.isBlank() || passwordInput.isBlank()) {
                                    Toast.makeText(context, "Fill in all fields", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isProcessing = true
                                viewModel.recoverAccount(usernameInput, recoveryPinInput, passwordInput) { success, msg ->
                                    isProcessing = false
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    if (success) activeTab = 1
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            enabled = !isProcessing
                        ) {
                            Text("Reset Password with PIN")
                        }
                    }
                }
            }
        }
    }
}
