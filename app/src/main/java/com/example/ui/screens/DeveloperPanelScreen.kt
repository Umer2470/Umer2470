package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import com.example.data.api.config.ApiConfig
import com.example.data.api.model.ApiResult
import com.example.data.api.security.AppActivationManager
import com.example.ui.components.AppHeader
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel
import kotlinx.coroutines.launch

@Composable
fun DeveloperPanelScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var targetInstallationId by remember { mutableStateOf(viewModel.identityManager.getInstallationId()) }
    var generatedCode by remember { mutableStateOf("") }
    var serverHealthResult by remember { mutableStateOf<String?>(null) }
    var isCheckingHealth by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Developer & SaaS Control Panel",
                subtitle = "API Diagnostics & Activation Key Generator",
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
            SectionHeader(title = "Server Connection & Diagnostics", subtitle = ApiConfig.DEFAULT_DEVELOPER_SERVER_URL)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("API Base URL: ${ApiConfig.DEFAULT_DEVELOPER_SERVER_URL}", fontSize = 12.sp, color = Navy600)
                    if (serverHealthResult != null) {
                        Text(
                            text = serverHealthResult!!,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (serverHealthResult!!.contains("OK") || serverHealthResult!!.contains("Success")) Emerald600 else Rose600
                        )
                    }

                    Button(
                        onClick = {
                            isCheckingHealth = true
                            scope.launch {
                                val res = viewModel.developerApiRepository.checkHealth()
                                isCheckingHealth = false
                                serverHealthResult = when (res) {
                                    is ApiResult.Success -> "Server Health: OK (${res.data.message ?: "Healthy"})"
                                    is ApiResult.Error -> "Health Check Failed: ${res.message}"
                                    is ApiResult.Offline -> "Health Check: Device is currently offline."
                                }
                            }
                        },
                        enabled = !isCheckingHealth,
                        colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                        modifier = Modifier.fillMaxWidth().testTag("check_health_button")
                    ) {
                        if (isCheckingHealth) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Text("Test Developer Server Health (GET /health)")
                        }
                    }
                }
            }

            SectionHeader(title = "Master Activation Key Generator", subtitle = "Generate authentic license keys for clients")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = targetInstallationId,
                        onValueChange = { targetInstallationId = it },
                        label = { Text("Client Installation ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("client_installation_id_input")
                    )

                    Button(
                        onClick = {
                            if (targetInstallationId.isNotBlank()) {
                                generatedCode = AppActivationManager.generateActivationCode(targetInstallationId.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Gold600),
                        modifier = Modifier.fillMaxWidth().testTag("generate_code_button")
                    ) {
                        Text("Generate Valid Cryptographic Code")
                    }

                    if (generatedCode.isNotBlank()) {
                        Divider()
                        Text("Generated Activation Code:", fontSize = 12.sp, color = Navy500)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = generatedCode,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Emerald600
                            )
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Generated Key", generatedCode)
                                    clipboard.setPrimaryClip(clip)
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                            }
                        }
                    }
                }
            }
        }
    }
}
