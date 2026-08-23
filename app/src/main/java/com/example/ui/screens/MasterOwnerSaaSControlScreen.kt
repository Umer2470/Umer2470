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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppHeader
import com.example.ui.components.KpiCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel

@Composable
fun MasterOwnerSaaSControlScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val branches by viewModel.branches.collectAsState()
    val users by viewModel.users.collectAsState()
    val products by viewModel.products.collectAsState()

    Scaffold(
        topBar = {
            AppHeader(
                title = "Master Owner SaaS Control",
                subtitle = "CH UMER • 03080018035",
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
            SectionHeader(title = "Multi-Store Infrastructure", subtitle = "SaaS license tenant controls")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KpiCard(
                    title = "Active Branches",
                    value = "${branches.size}",
                    icon = Icons.Default.Storefront,
                    color = Navy900,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "System Users",
                    value = "${users.size}",
                    icon = Icons.Default.SupervisedUserCircle,
                    color = Emerald600,
                    modifier = Modifier.weight(1f)
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("SaaS Owner Privileges", fontWeight = FontWeight.Bold, color = Navy900)
                    Text("• Manage client tenant quotas and offline license keys", fontSize = 12.sp, color = Navy600)
                    Text("• Direct remote database backup & sync configuration", fontSize = 12.sp, color = Navy600)
                    Text("• Enterprise multi-shop consolidation", fontSize = 12.sp, color = Navy600)
                }
            }
        }
    }
}
