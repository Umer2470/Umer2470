package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.entity.User
import com.example.ui.components.AppHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel

@Composable
fun UserManagementScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val users by viewModel.users.collectAsState()

    Scaffold(
        topBar = {
            AppHeader(
                title = "Staff & Cashier Users",
                subtitle = "${users.size} Active Operators",
                onBackClick = onNavigateBack
            )
        },
        containerColor = Slate50
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(users, key = { it.id }) { user ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                                Text(
                                    text = user.fullName.ifBlank { user.username },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Navy900
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Username: ${user.username} • Role: ${user.role}",
                                    fontSize = 12.sp,
                                    color = Navy500
                                )
                            }
                            StatusBadge(
                                text = user.role,
                                backgroundColor = if (user.role == "Admin") Emerald100 else Blue100,
                                textColor = if (user.role == "Admin") Emerald600 else Blue600
                            )
                        }
                    }
                }
            }
        }
    }
}
