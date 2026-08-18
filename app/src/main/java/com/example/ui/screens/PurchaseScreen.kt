package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.entity.Supplier
import com.example.ui.theme.Rose600
import com.example.ui.viewmodel.StoreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseScreen(viewModel: StoreViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val purchaseCart by viewModel.purchaseCart.collectAsState()
    val suppliers by viewModel.allSuppliers.collectAsState()
    val selectedSupplier by viewModel.selectedSupplier.collectAsState()
    val purchasePaidInput by viewModel.purchasePaidInput.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var showAddSupplierDialog by remember { mutableStateOf(false) }
    var supplierDropdownExpanded by remember { mutableStateOf(false) }

    val filteredProducts = allProducts.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true)
    }

    val totalPurchaseVal = purchaseCart.sumOf { it.total }
    val paidVal = purchasePaidInput.toDoubleOrNull() ?: totalPurchaseVal
    val dueVal = (totalPurchaseVal - paidVal).coerceAtLeast(0.0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Select Items to Stock") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Purchase Entry (${purchaseCart.size})") })
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (selectedTab == 0) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search product to replenish stock...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredProducts) { product ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.addToPurchaseCart(product, 10.0) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Current Stock: ${product.stockQuantity.toInt()} ${product.unit} • Cost: ${settings.currencySymbol} ${product.purchasePrice.toInt()}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }

                            Button(
                                onClick = { viewModel.addToPurchaseCart(product, 10.0) },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add +10")
                            }
                        }
                    }
                }
            }

            if (purchaseCart.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { selectedTab = 1 },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Review Purchase Entry (${settings.currencySymbol} ${totalPurchaseVal.toInt()}) →", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            if (purchaseCart.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No items selected for purchase.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Items Being Purchased", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { viewModel.clearPurchaseCart() }) { Text("Clear All", color = Rose600) }
                        }
                    }

                    items(purchaseCart) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(item.product.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                    Text("Total: ${settings.currencySymbol} ${item.total.toInt()}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    OutlinedTextField(
                                        value = item.costPrice.toInt().toString(),
                                        onValueChange = { viewModel.updatePurchaseCartCost(item.product.id, it.toDoubleOrNull() ?: 0.0) },
                                        label = { Text("Cost Price") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedTextField(
                                        value = item.quantity.toInt().toString(),
                                        onValueChange = { viewModel.updatePurchaseCartQty(item.product.id, it.toDoubleOrNull() ?: 0.0) },
                                        label = { Text("Qty Purchased") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                    IconButton(onClick = { viewModel.removeFromPurchaseCart(item.product.id) }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Rose600)
                                    }
                                }
                            }
                        }
                    }

                    item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

                    item {
                        Column {
                            Text("Supplier / Vendor Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ExposedDropdownMenuBox(
                                    expanded = supplierDropdownExpanded,
                                    onExpandedChange = { supplierDropdownExpanded = !supplierDropdownExpanded },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = selectedSupplier?.let { "${it.name} (${it.company})" } ?: "General Vendor",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Select Supplier") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = supplierDropdownExpanded) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = supplierDropdownExpanded,
                                        onDismissRequest = { supplierDropdownExpanded = false }
                                    ) {
                                        DropdownMenuItem(text = { Text("General Vendor") }, onClick = { viewModel.setSelectedSupplier(null); supplierDropdownExpanded = false })
                                        suppliers.forEach { supp ->
                                            DropdownMenuItem(text = { Text("${supp.name} (${supp.company})") }, onClick = { viewModel.setSelectedSupplier(supp); supplierDropdownExpanded = false })
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = { showAddSupplierDialog = true },
                                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.secondaryContainer)
                                ) {
                                    Icon(imageVector = Icons.Default.AddBusiness, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = purchasePaidInput,
                                onValueChange = { viewModel.setPurchasePaidInput(it) },
                                label = { Text("Paid Amount to Supplier") },
                                placeholder = { Text("${totalPurchaseVal.toInt()}") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Total Bill:", style = MaterialTheme.typography.bodyMedium)
                                        Text("${settings.currencySymbol} ${totalPurchaseVal.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                    if (dueVal > 0) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Added to Supplier Payable Due:", style = MaterialTheme.typography.bodySmall, color = Rose600, fontWeight = FontWeight.Bold)
                                            Text("${settings.currencySymbol} ${dueVal.toInt()}", style = MaterialTheme.typography.bodySmall, color = Rose600, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { viewModel.processPurchase() },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Record Purchase & Update Stock")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSupplierDialog) {
        AddSupplierDialog(
            onSave = { name, comp, phone, addr ->
                viewModel.saveSupplier(Supplier(name = name, company = comp, phone = phone, address = addr))
                showAddSupplierDialog = false
            },
            onDismiss = { showAddSupplierDialog = false }
        )
    }
}

@Composable
fun AddSupplierDialog(
    onSave: (String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Supplier / Vendor") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Supplier Contact Name *") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("Company / Distributor Name") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone Number") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address / City") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onSave(name.trim(), company.trim(), phone.trim(), address.trim()) }) { Text("Save Supplier") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
