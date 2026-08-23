package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppHeader
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel

@Composable
fun StoreAccessManagementScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            AppHeader(
                title = "Store Access Control",
                subtitle = "Security & PIN Authentication",
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
                .padding(16.dp)
        ) {
            SectionHeader(title = "Access Privileges", subtitle = "Role based permissions")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Admin Access: Full system settings, database management & licensing", fontSize = 13.sp, color = Navy800)
                    Divider()
                    Text("Manager Access: Inventory, purchases, customer ledgers & reports", fontSize = 13.sp, color = Navy800)
                    Divider()
                    Text("Cashier Access: POS sales counter and receipt printing only", fontSize = 13.sp, color = Navy800)
                }
            }
        }
    }
}
