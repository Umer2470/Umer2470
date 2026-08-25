package com.example.ui.screens

import androidx.compose.foundation.background
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
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel

@Composable
fun StoreAccessManagementScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val activeUser by viewModel.activeUser.collectAsState()
    val allUsers by viewModel.users.collectAsState()

    val permissionMatrix = listOf(
        Pair("POS Sales & Checkout", listOf("SUPER_ADMIN", "ADMIN", "CASHIER", "EMPLOYEE")),
        Pair("Invoices & Receipt Reprinting", listOf("SUPER_ADMIN", "ADMIN", "CASHIER")),
        Pair("Inventory & Stock Management", listOf("SUPER_ADMIN", "ADMIN")),
        Pair("Purchases & Supplier Payables", listOf("SUPER_ADMIN", "ADMIN")),
        Pair("Financial Reports & P&L", listOf("SUPER_ADMIN", "ADMIN")),
        Pair("Staff Attendance Tracking", listOf("SUPER_ADMIN", "ADMIN", "CASHIER", "EMPLOYEE")),
        Pair("Database & Settings Config", listOf("SUPER_ADMIN", "ADMIN")),
        Pair("Device License Activation", listOf("SUPER_ADMIN", "ADMIN")),
        Pair("SaaS Multi-Tenant Management", listOf("SUPER_ADMIN"))
    )

    Scaffold(
        topBar = {
            AppHeader(
                title = "Store Access & Permissions",
                subtitle = "Role Access Control & Security Policies",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Active Session & Operator Switcher
            SectionHeader(title = "Current Terminal Operator", subtitle = "Active session context")

            Card(
                modifier = Modifier.fillMaxWidth().testTag("active_operator_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = activeUser?.fullName ?: "Super Administrator",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Navy900
                            )
                            Text(
                                text = "Username: @${activeUser?.username ?: "admin"}",
                                fontSize = 12.sp,
                                color = Navy500
                            )
                        }
                        StatusBadge(
                            text = activeUser?.role ?: "SUPER_ADMIN",
                            backgroundColor = Gold100,
                            textColor = Gold600
                        )
                    }

                    if (allUsers.size > 1) {
                        Divider()
                        Text("Switch Active Session User:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Navy800)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            allUsers.forEach { user ->
                                FilterChip(
                                    selected = activeUser?.id == user.id,
                                    onClick = { viewModel.setActiveUser(user) },
                                    label = { Text(user.username) },
                                    modifier = Modifier.testTag("switch_user_${user.username}")
                                )
                            }
                        }
                    }
                }
            }

            // Role Permission Matrix
            SectionHeader(title = "Role Permission Matrix", subtitle = "Enforced access privileges")

            Card(
                modifier = Modifier.fillMaxWidth().testTag("permission_matrix_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    permissionMatrix.forEachIndexed { index, (feature, allowedRoles) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = feature,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Navy900,
                                modifier = Modifier.weight(1f)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                allowedRoles.forEach { role ->
                                    Surface(
                                        color = when (role) {
                                            "SUPER_ADMIN" -> Gold100
                                            "ADMIN" -> Emerald100
                                            "CASHIER" -> Blue100
                                            else -> Slate100
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = role.replace("_", " "),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (role) {
                                                "SUPER_ADMIN" -> Gold600
                                                "ADMIN" -> Emerald600
                                                "CASHIER" -> Blue600
                                                else -> Navy600
                                            },
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (index < permissionMatrix.size - 1) {
                            Divider(color = Slate100)
                        }
                    }
                }
            }

            // Terminal Protection Policies
            SectionHeader(title = "Security & Master Safeguards", subtitle = "Offline recovery policies")

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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = Gold600)
                        Column {
                            Text("Offline Cryptographic Recovery", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Navy900)
                            Text("20-character emergency reset key active in preferences", fontSize = 11.sp, color = Navy500)
                        }
                    }
                    Divider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = Emerald600)
                        Column {
                            Text("Hardware Identity Binding", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Navy900)
                            Text("Bound to: ${viewModel.identityManager.getInstallationId()}", fontSize = 11.sp, color = Navy500)
                        }
                    }
                }
            }
        }
    }
}

