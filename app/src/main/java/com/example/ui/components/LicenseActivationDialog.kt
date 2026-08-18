package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.StoreViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LicenseStatusCard(
    viewModel: StoreViewModel,
    onOpenLicenseDialog: () -> Unit
) {
    val currentTenant by viewModel.currentTenant.collectAsState()
    val isLicenseValid by viewModel.isLicenseValid.collectAsState()
    val isMaster by viewModel.isMasterOwner.collectAsState()

    val tenant = currentTenant ?: return
    if (isMaster) return

    val daysRemaining = tenant.getDaysRemaining()
    val isWarning = daysRemaining <= 15

    val cardBg = when {
        !isLicenseValid -> Color(0xFFFEF2F2)
        isWarning -> Color(0xFFFFFBEB)
        else -> Color(0xFFECFDF5)
    }

    val textColor = when {
        !isLicenseValid -> Color(0xFFDC2626)
        isWarning -> Color(0xFFD97706)
        else -> Color(0xFF059669)
    }

    val icon = when {
        !isLicenseValid -> Icons.Default.Lock
        isWarning -> Icons.Default.Warning
        else -> Icons.Default.CheckCircle
    }

    Surface(
        color = cardBg,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (!isLicenseValid) "COMMERCIAL LICENSE INACTIVE" else "License: ${tenant.planType} Plan ($daysRemaining days left)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = textColor
                    )
                    Text(
                        text = "Key: ${tenant.licenseKey}",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.Gray
                    )
                }
            }

            Button(
                onClick = onOpenLicenseDialog,
                colors = ButtonDefaults.buttonColors(containerColor = textColor),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(if (!isLicenseValid) "Activate Key" else "License Info", fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun LicenseActivationDialog(
    viewModel: StoreViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val currentTenant by viewModel.currentTenant.collectAsState()
    var inputKey by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val tenant = currentTenant ?: return
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val expiryString = remember(tenant.licenseExpiryDate) { dateFormat.format(Date(tenant.licenseExpiryDate)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF2563EB))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Commercial SaaS Licensing")
            }
        },
        text = {
            Column {
                Text("Business Name: ${tenant.businessName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Tenant Code: ${tenant.tenantCode} • Plan: ${tenant.planType}", fontSize = 12.sp, color = Color.Gray)
                Text("Expiry Date: $expiryString (${tenant.getDaysRemaining()} days remaining)", fontSize = 12.sp, color = Color.Gray)

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Current License Key:", fontSize = 10.sp, color = Color.Gray)
                            Text(
                                tenant.licenseKey,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Color(0xFF0F172A)
                            )
                        }
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(tenant.licenseKey))
                            Toast.makeText(context, "Key copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Enter New Commercial License Key:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = inputKey,
                    onValueChange = { inputKey = it },
                    placeholder = { Text("e.g. SAAS-ALKH-AN-2026-X9A2-7B4K") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (inputKey.isNotBlank()) {
                        isSubmitting = true
                        viewModel.activateLicenseWithKey(inputKey) { success, msg ->
                            isSubmitting = false
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            if (success) {
                                onDismiss()
                            }
                        }
                    }
                },
                enabled = inputKey.isNotBlank() && !isSubmitting
            ) {
                Text(if (isSubmitting) "Verifying..." else "Activate Key")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
