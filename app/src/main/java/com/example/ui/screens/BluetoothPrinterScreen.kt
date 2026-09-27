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
import com.example.ui.components.settings.BluetoothPrinterSection
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.PosSettingsManager

@Composable
fun BluetoothPrinterScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val settingsManager = remember(context) { PosSettingsManager.getInstance(context) }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Bluetooth Thermal Printer",
                subtitle = "Wireless ESC/POS connection, scanner & test print",
                onBackClick = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier.testTag("bluetooth_printer_screen")
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BluetoothPrinterSection(
                settingsManager = settingsManager
            )
        }
    }
}
