package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.entity.StoreBranch
import com.example.data.entity.StoreSettings
import com.example.ui.components.AppHeader
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel

@Composable
fun StoreManagementScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val storeSettings by viewModel.storeSettings.collectAsState()
    val branches by viewModel.branches.collectAsState()

    var showAddBranchDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Business & Branch Management",
                subtitle = "Stores, Outlets & Multi-Location Sync",
                onBackClick = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddBranchDialog = true },
                containerColor = Navy900,
                contentColor = Gold500,
                modifier = Modifier.testTag("add_branch_fab")
            ) {
                Icon(Icons.Default.AddBusiness, contentDescription = "Add Branch")
            }
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
            // Main Head Office / Primary Store
            SectionHeader(title = "Primary Store Information", subtitle = "Headquarters details")

            Card(
                modifier = Modifier.fillMaxWidth().testTag("primary_store_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = storeSettings?.storeName ?: "CH UMER POS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Navy900
                        )
                        StatusBadge(
                            text = "Main Outlet",
                            backgroundColor = Emerald100,
                            textColor = Emerald600
                        )
                    }

                    Text("Proprietor: ${storeSettings?.ownerName ?: "CH UMER"}", fontSize = 13.sp, color = Navy700)
                    Text("Official Contact: ${storeSettings?.phone ?: "03080018035"}", fontSize = 13.sp, color = Navy700)
                    Text("Address: ${storeSettings?.address ?: "Main Market, Store #1"}", fontSize = 13.sp, color = Navy500)
                    Text("Tax System: ${storeSettings?.taxRatePercent ?: 0.0}% Standard Rate", fontSize = 13.sp, color = Navy500)
                }
            }

            // Branch Outlets
            SectionHeader(title = "Branch Outlets & Locations", subtitle = "${branches.size} Registered Branch Locations")

            if (branches.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No additional branches yet. Tap '+' to create a branch outlet.",
                            fontSize = 13.sp,
                            color = Navy500
                        )
                    }
                }
            } else {
                branches.forEach { branch ->
                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("branch_card_${branch.id}"),
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
                                    text = branch.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Navy900
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Location: ${branch.location} • Manager: ${branch.managerName.ifBlank { "N/A" }}",
                                    fontSize = 12.sp,
                                    color = Navy500
                                )
                                Text(
                                    text = "Phone: ${branch.phone.ifBlank { "03080018035" }}",
                                    fontSize = 12.sp,
                                    color = Navy500
                                )
                            }
                            StatusBadge(
                                text = if (branch.isActive) "Active" else "Offline",
                                backgroundColor = if (branch.isActive) Emerald100 else Slate100,
                                textColor = if (branch.isActive) Emerald600 else Navy600
                            )
                        }
                    }
                }
            }
        }

        if (showAddBranchDialog) {
            var branchName by remember { mutableStateOf("") }
            var branchCode by remember { mutableStateOf("BR-${(branches.size + 1).toString().padStart(3, '0')}") }
            var managerName by remember { mutableStateOf("") }
            var phone by remember { mutableStateOf("03080018035") }
            var address by remember { mutableStateOf("") }
            var errorMessage by remember { mutableStateOf<String?>(null) }

            AlertDialog(
                onDismissRequest = { showAddBranchDialog = false },
                title = {
                    Text("Add Branch Outlet", fontWeight = FontWeight.Bold, color = Navy900)
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = branchName,
                            onValueChange = { branchName = it },
                            label = { Text("Branch Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("branch_name_input")
                        )
                        OutlinedTextField(
                            value = branchCode,
                            onValueChange = { branchCode = it },
                            label = { Text("Branch Code") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("branch_code_input")
                        )
                        OutlinedTextField(
                            value = managerName,
                            onValueChange = { managerName = it },
                            label = { Text("Manager Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("branch_manager_input")
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Contact Phone") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("branch_phone_input")
                        )
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Branch Address") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("branch_address_input")
                        )
                        if (errorMessage != null) {
                            Text(text = errorMessage!!, color = Rose600, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (branchName.isBlank()) {
                                errorMessage = "Please enter a valid branch name."
                            } else {
                                viewModel.addStoreBranch(
                                    name = branchName.trim(),
                                    location = address.ifBlank { "BR-$branchCode" }.trim(),
                                    phone = phone.trim(),
                                    manager = managerName.trim()
                                ) {
                                    showAddBranchDialog = false
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                        modifier = Modifier.testTag("save_branch_button")
                    ) {
                        Text("Save Branch")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddBranchDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
