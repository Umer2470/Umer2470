package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.ui.components.SuperAdminRecoveryDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.data.entity.UserAccount
import com.example.ui.components.ShopLogoAvatar
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.BiometricPromptHelper
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LoginScreen(viewModel: StoreViewModel) {
    val context = LocalContext.current
    val pinInput by viewModel.pinInput.collectAsState()
    val pinError by viewModel.pinError.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var currentTimeText by remember { mutableStateOf("") }
    var detectedUser by remember { mutableStateOf<UserAccount?>(null) }
    var showRecoveryDialog by remember { mutableStateOf(false) }

    val triggerBiometricAuth = {
        BiometricPromptHelper.authenticateSuperAdmin(
            context = context,
            title = "Biometric Security Unlock",
            subtitle = "Verify fingerprint or face unlock to access store POS and financial data",
            onSuccess = {
                viewModel.loginWithFingerprint()
            },
            onError = { err ->
                viewModel.showToast(err)
            }
        )
    }

    // Auto-prompt Biometric unlock if enabled in settings and hardware available
    LaunchedEffect(settings.isBiometricEnabled) {
        if (settings.isBiometricEnabled) {
            val (available, _) = BiometricPromptHelper.isBiometricAvailable(context)
            if (available) {
                delay(300L)
                triggerBiometricAuth()
            }
        }
    }

    // Live Clock Effect
    LaunchedEffect(Unit) {
        val formatter = SimpleDateFormat("EEEE, dd MMM yyyy • hh:mm:ss a", Locale.getDefault())
        while (true) {
            currentTimeText = formatter.format(Date())
            delay(1000L)
        }
    }

    // Auto-detect user role as PIN is entered
    LaunchedEffect(pinInput) {
        if (pinInput.isNotBlank()) {
            detectedUser = viewModel.detectUserByPin(pinInput)
        } else {
            detectedUser = null
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Real-time Date and Time Pill Header
            if (currentTimeText.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    tonalElevation = 2.dp,
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Current Time",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentTimeText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Header Store Brand Logo & Name
            ShopLogoAvatar(
                logoUri = settings.logoUri,
                size = 88.dp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = settings.storeName.ifBlank { "Store Management System" },
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = settings.ownerName.ifBlank { "Point of Sale & Business Control" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main PIN Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Enter Security PIN",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Role / User Auto-Detection Chip Banner
                    if (detectedUser != null) {
                        val roleBadge = when (detectedUser!!.role) {
                            "SUPER_ADMIN" -> "Super Admin"
                            "ADMIN" -> "Store Admin"
                            "EMPLOYEE" -> "Employee"
                            else -> detectedUser!!.role
                        }
                        val badgeBg = when (detectedUser!!.role) {
                            "SUPER_ADMIN" -> Color(0xFFEFF6FF)
                            "ADMIN" -> Color(0xFFF0FDF4)
                            else -> Color(0xFFFEF3C7)
                        }
                        val badgeFg = when (detectedUser!!.role) {
                            "SUPER_ADMIN" -> Color(0xFF1D4ED8)
                            "ADMIN" -> Color(0xFF15803D)
                            else -> Color(0xFFB45309)
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = badgeBg,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = badgeFg,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${detectedUser!!.name} • $roleBadge",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeFg
                                )
                            }
                        }
                    }

                    // PIN Dots (4 to 6 digits)
                    val dotCount = maxOf(4, pinInput.length)
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until dotCount) {
                            val isFilled = i < pinInput.length
                            Box(
                                modifier = Modifier
                                    .padding(5.dp)
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isFilled) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                            )
                        }
                    }

                    if (pinError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = pinError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Keypad (Layout preserved: 1-9, FP, 0, DEL)
                    val buttons = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("FP", "0", "DEL")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        for (row in buttons) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (key in row) {
                                    if (key.isEmpty()) {
                                        Spacer(modifier = Modifier.size(64.dp))
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (key) {
                                                        "DEL" -> MaterialTheme.colorScheme.errorContainer
                                                        "FP" -> {
                                                            if (detectedUser == null || detectedUser?.role == "SUPER_ADMIN") {
                                                                MaterialTheme.colorScheme.primaryContainer
                                                            } else {
                                                                Color.Transparent
                                                            }
                                                        }
                                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                                    }
                                                )
                                                .clickable {
                                                    when (key) {
                                                        "DEL" -> viewModel.onPinDelete()
                                                        "FP" -> {
                                                            val isSuperAdminFp = detectedUser == null || detectedUser?.role == "SUPER_ADMIN"
                                                            if (isSuperAdminFp) {
                                                                triggerBiometricAuth()
                                                            } else {
                                                                viewModel.showToast("Fingerprint Login is available only for Super Admin.")
                                                            }
                                                        }
                                                        else -> viewModel.onPinDigit(key)
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            when (key) {
                                                "DEL" -> {
                                                    Icon(
                                                        imageVector = Icons.Default.Backspace,
                                                        contentDescription = "Delete",
                                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                                    )
                                                }
                                                "FP" -> {
                                                    val isSuperAdminFp = detectedUser == null || detectedUser?.role == "SUPER_ADMIN"
                                                    if (isSuperAdminFp) {
                                                        Icon(
                                                            imageVector = Icons.Default.Fingerprint,
                                                            contentDescription = "Fingerprint Login",
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                }
                                                else -> {
                                                    Text(
                                                        text = key,
                                                        style = MaterialTheme.typography.titleLarge.copy(
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 22.sp
                                                        ),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Fingerprint Biometric Quick Login Button - Visible ONLY for Super Admin or default state
                    val showFingerprint = detectedUser == null || detectedUser?.role == "SUPER_ADMIN"
                    if (showFingerprint) {
                        Surface(
                            onClick = { triggerBiometricAuth() },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Use Fingerprint / Biometric Login",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Super Admin 3-Level Emergency Recovery Button
                    TextButton(
                        onClick = { showRecoveryDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Forgot PIN? Super Admin Emergency Recovery",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    if (showRecoveryDialog) {
        SuperAdminRecoveryDialog(
            viewModel = viewModel,
            onDismiss = { showRecoveryDialog = false }
        )
    }
}
