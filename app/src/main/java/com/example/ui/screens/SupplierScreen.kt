package com.example.ui.screens

import androidx.compose.foundation.clickable
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
import com.example.data.entity.Supplier
import com.example.ui.components.AppHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupplierScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val suppliers by viewModel.suppliers.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()
    val currency = storeSettings?.currencySymbol ?: "Rs"

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingSupplier by remember { mutableStateOf<Supplier?>(null) }

    val filteredSuppliers = remember(suppliers, searchQuery) {
        if (searchQuery.isBlank()) suppliers
        else {
            suppliers.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.companyName.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf(editingSupplier?.name ?: "") }
        var company by remember { mutableStateOf(editingSupplier?.companyName ?: "") }
        var phone by remember { mutableStateOf(editingSupplier?.phone ?: "") }
        var balance by remember { mutableStateOf(editingSupplier?.balance?.toString() ?: "0.0") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (editingSupplier == null) "Add Supplier" else "Edit Supplier") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Contact Person") },
                        modifier = Modifier.fillMaxWidth().testTag("supplier_name_input")
                    )
                    OutlinedTextField(
                        value = company,
                        onValueChange = { company = it },
                        label = { Text("Company / Vendor Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = balance,
                        onValueChange = { balance = it },
                        label = { Text("Current Payable ($currency)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val s = (editingSupplier ?: Supplier()).copy(
                                name = name,
                                companyName = company,
                                phone = phone,
                                balance = balance.toDoubleOrNull() ?: 0.0
                            )
                            viewModel.saveSupplier(s) {
                                showAddDialog = false
                                editingSupplier = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    modifier = Modifier.testTag("save_supplier_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Suppliers & Vendors",
                subtitle = "${suppliers.size} Registered Suppliers",
                onBackClick = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingSupplier = null
                    showAddDialog = true
                },
                containerColor = Navy900,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_supplier_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Supplier")
            }
        },
        containerColor = Slate50
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search supplier by name or company...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Navy900,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredSuppliers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No suppliers registered.", color = Navy500)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredSuppliers, key = { it.id }) { supplier ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editingSupplier = supplier
                                    showAddDialog = true
                                },
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = supplier.companyName.ifBlank { supplier.name },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Navy900
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Contact: ${supplier.name} • Phone: ${supplier.phone.ifBlank { "N/A" }}",
                                        fontSize = 12.sp,
                                        color = Navy500
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    StatusBadge(
                                        text = if (supplier.balance <= 0) "Clear" else "Payable: $currency %.0f".format(supplier.balance),
                                        backgroundColor = if (supplier.balance <= 0) Emerald100 else Rose100,
                                        textColor = if (supplier.balance <= 0) Emerald600 else Rose600
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    IconButton(
                                        onClick = { viewModel.softDeleteSupplier(supplier.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Rose600)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
