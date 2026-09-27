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
import com.example.ui.components.settings.InvoicePdfSettingsSection
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.PosSettingsManager

@Composable
fun InvoicePdfSettingsScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val storeSettings by viewModel.storeSettings.collectAsState()
    val settingsManager = remember(context) { PosSettingsManager.getInstance(context) }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Invoice PDF Settings",
                subtitle = "Tagline, accent styling, footer & tax registration",
                onBackClick = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.testTag("invoice_pdf_settings_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InvoicePdfSettingsSection(
                settingsManager = settingsManager,
                storeSettings = storeSettings
            )
        }
    }
}
