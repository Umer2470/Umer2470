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
import com.example.data.entity.Customer
import com.example.ui.components.AppHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val customers by viewModel.customers.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()
    val currency = storeSettings?.currencySymbol ?: "Rs"

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCustomer by remember { mutableStateOf<Customer?>(null) }

    val filteredCustomers = remember(customers, searchQuery) {
        if (searchQuery.isBlank()) customers
        else {
            customers.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.phone.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    if (showAddDialog) {
        var name by remember { mutableStateOf(editingCustomer?.name ?: "") }
        var phone by remember { mutableStateOf(editingCustomer?.phone ?: "") }
        var address by remember { mutableStateOf(editingCustomer?.address ?: "") }
        var balance by remember { mutableStateOf(editingCustomer?.balance?.toString() ?: "0.0") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (editingCustomer == null) "Add Customer" else "Edit Customer") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Customer Name") },
                        modifier = Modifier.fillMaxWidth().testTag("customer_name_input")
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = balance,
                        onValueChange = { balance = it },
                        label = { Text("Opening Balance ($currency)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val c = (editingCustomer ?: Customer()).copy(
                                name = name,
                                phone = phone,
                                address = address,
                                balance = balance.toDoubleOrNull() ?: 0.0
                            )
                            viewModel.saveCustomer(c) {
                                showAddDialog = false
                                editingCustomer = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    modifier = Modifier.testTag("save_customer_button")
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
                title = "Customer Accounts",
                subtitle = "${customers.size} Registered Customers",
                onBackClick = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingCustomer = null
                    showAddDialog = true
                },
                containerColor = Navy900,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_customer_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Customer")
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
                placeholder = { Text("Search customer name or phone...") },
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

            if (filteredCustomers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No customers found.", color = Navy500)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredCustomers, key = { it.id }) { customer ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editingCustomer = customer
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
                                        text = customer.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Navy900
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Phone: ${customer.phone.ifBlank { "N/A" }}",
                                        fontSize = 12.sp,
                                        color = Navy500
                                    )
                                    if (customer.address.isNotBlank()) {
                                        Text(
                                            text = "Address: ${customer.address}",
                                            fontSize = 11.sp,
                                            color = Navy600
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    StatusBadge(
                                        text = if (customer.balance <= 0) "Balance: 0" else "Due: $currency %.0f".format(customer.balance),
                                        backgroundColor = if (customer.balance <= 0) Emerald100 else Rose100,
                                        textColor = if (customer.balance <= 0) Emerald600 else Rose600
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    IconButton(
                                        onClick = { viewModel.softDeleteCustomer(customer.id) },
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
