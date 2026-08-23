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
import com.example.data.entity.Purchase
import com.example.data.entity.PurchaseItem
import com.example.ui.components.AppHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val purchases by viewModel.purchases.collectAsState()
    val suppliers by viewModel.suppliers.collectAsState()
    val products by viewModel.products.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()
    val currency = storeSettings?.currencySymbol ?: "Rs"

    var showAddDialog by remember { mutableStateOf(false) }

    if (showAddDialog) {
        var billNo by remember { mutableStateOf("BILL-${System.currentTimeMillis() % 100000}") }
        var selectedSupplierId by remember { mutableStateOf(suppliers.firstOrNull()?.id ?: 0L) }
        var selectedProductId by remember { mutableStateOf(products.firstOrNull()?.id ?: 0L) }
        var quantity by remember { mutableStateOf("10") }
        var unitCost by remember { mutableStateOf("100") }
        var paidAmount by remember { mutableStateOf("1000") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Record Stock Purchase / Inward") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = billNo,
                        onValueChange = { billNo = it },
                        label = { Text("Bill / Inward Invoice #") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("Quantity") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = unitCost,
                        onValueChange = { unitCost = it },
                        label = { Text("Cost Per Unit ($currency)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = paidAmount,
                        onValueChange = { paidAmount = it },
                        label = { Text("Amount Paid ($currency)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = quantity.toDoubleOrNull() ?: 1.0
                        val cost = unitCost.toDoubleOrNull() ?: 0.0
                        val total = qty * cost
                        val paid = paidAmount.toDoubleOrNull() ?: total
                        val due = (total - paid).coerceAtLeast(0.0)

                        val prod = products.find { it.id == selectedProductId } ?: products.firstOrNull()
                        val supp = suppliers.find { it.id == selectedSupplierId } ?: suppliers.firstOrNull()

                        val purchase = Purchase(
                            billNumber = billNo,
                            supplierId = supp?.id ?: 0L,
                            supplierName = supp?.name ?: "General Vendor",
                            totalAmount = total,
                            paidAmount = paid,
                            dueAmount = due
                        )

                        val item = PurchaseItem(
                            productId = prod?.id ?: 1L,
                            productName = prod?.name ?: "Stock Item",
                            quantity = qty,
                            unit = prod?.unit ?: "Pcs",
                            unitCost = cost,
                            totalCost = total
                        )

                        viewModel.recordPurchase(purchase, listOf(item)) {
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    modifier = Modifier.testTag("save_purchase_button")
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
                title = "Purchases & Inward Bills",
                subtitle = "${purchases.size} Purchase Invoices",
                onBackClick = onNavigateBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Navy900,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_purchase_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Purchase")
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
            if (purchases.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No purchase bills recorded. Tap + to record stock inward.", color = Navy500)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(purchases, key = { it.id }) { purchase ->
                        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                        val formattedDate = dateFormat.format(Date(purchase.createdAt))

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
                                        text = purchase.billNumber,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Navy900
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Supplier: ${purchase.supplierName} • $formattedDate",
                                        fontSize = 12.sp,
                                        color = Navy500
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$currency %.2f".format(purchase.totalAmount),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Navy900
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    StatusBadge(
                                        text = if (purchase.dueAmount <= 0) "Paid" else "Due: $currency %.0f".format(purchase.dueAmount),
                                        backgroundColor = if (purchase.dueAmount <= 0) Emerald100 else Rose100,
                                        textColor = if (purchase.dueAmount <= 0) Emerald600 else Rose600
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
