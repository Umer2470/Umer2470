package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.AppHeader
import com.example.ui.components.settings.ReceiptSettingsSection
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.PosSettingsManager

@Composable
fun ReceiptSettingsScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val storeSettings by viewModel.storeSettings.collectAsState()
    val settingsManager = remember(context) { PosSettingsManager.getInstance(context) }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Thermal Receipt Settings",
                subtitle = "Paper width, headers, footers & auto-print",
                onBackClick = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.testTag("receipt_settings_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ReceiptSettingsSection(
                settingsManager = settingsManager,
                storeSettings = storeSettings,
                onSaveStoreSettings = { updated ->
                    viewModel.updateStoreSettings(updated)
                }
            )
        }
    }
}
