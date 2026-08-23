package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppHeader
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel

@Composable
fun SettingsScreen(
    viewModel: StoreViewModel,
    onNavigate: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            AppHeader(
                title = "Application Settings",
                subtitle = "Preferences & Diagnostics",
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SectionHeader(title = "Configuration Modules", subtitle = "System preferences")

            listOf(
                Triple("Business Profile", "Store name, address, receipt headers", "setup"),
                Triple("Device Activation", "Commercial license key status", "activation"),
                Triple("Developer & API Panel", "Server diagnostics & health checks", "dev_panel"),
                Triple("Staff & Users", "Manage operators and PINs", "users"),
                Triple("Recycle Bin", "Restore or permanently delete items", "recycle_bin"),
                Triple("Audit Activity Logs", "Inspect recent terminal actions", "logs")
            ).forEach { (title, subtitle, route) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(route) }
                        .testTag("settings_item_$route"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(title, fontWeight = FontWeight.Bold, color = Navy900)
                            Text(subtitle, fontSize = 12.sp, color = Navy500)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Navy500)
                    }
                }
            }
        }
    }
}
