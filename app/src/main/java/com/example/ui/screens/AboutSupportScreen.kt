package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.AppHeader
import com.example.ui.components.settings.AboutAndSupportSection
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.PosSettingsManager

@Composable
fun AboutSupportScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember(context) { PosSettingsManager.getInstance(context) }

    Scaffold(
        topBar = {
            AppHeader(
                title = "About & Support",
                subtitle = "App details, licenses, support channels & portal",
                onBackClick = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.testTag("about_support_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AboutAndSupportSection(
                settingsManager = settingsManager
            )
        }
    }
}
