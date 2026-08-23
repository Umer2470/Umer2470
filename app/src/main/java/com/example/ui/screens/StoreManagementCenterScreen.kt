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
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel

@Composable
fun StoreManagementCenterScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val storeSettings by viewModel.storeSettings.collectAsState()
    val branches by viewModel.branches.collectAsState()

    Scaffold(
        topBar = {
            AppHeader(
                title = "Store Management Center",
                subtitle = "Branch & Multi-Store Hub",
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
            SectionHeader(title = "Branches & Outlets", subtitle = "${branches.size} Active Branches")

            branches.forEach { branch ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(branch.name, fontWeight = FontWeight.Bold, color = Navy900)
                            Text("Location: ${branch.location}", fontSize = 12.sp, color = Navy500)
                        }
                        if (branch.isHeadquarters) {
                            Text("Headquarters", color = Gold600, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
