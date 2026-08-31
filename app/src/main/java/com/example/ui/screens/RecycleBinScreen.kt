package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Customer
import com.example.data.entity.Product
import com.example.data.entity.Sale
import com.example.ui.components.AppHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class TrashCategory {
    INVOICES,
    PRODUCTS,
    CUSTOMERS
}

@Composable
fun RecycleBinScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val deletedSales by viewModel.recycleBinSales.collectAsState()
    val deletedProducts by viewModel.recycleBinProducts.collectAsState()
    val deletedCustomers by viewModel.recycleBinCustomers.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()
    val currency = storeSettings?.currencySymbol ?: "Rs"

    var selectedTab by remember { mutableStateOf(TrashCategory.INVOICES) }
    val totalDeleted = deletedSales.size + deletedProducts.size + deletedCustomers.size

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Recycle Bin & Trash",
                subtitle = "$totalDeleted Deleted Records • Invoices, Products & Customers",
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
            // Category Tabs
            TabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = Color.White,
                contentColor = Navy900,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == TrashCategory.INVOICES,
                    onClick = { selectedTab = TrashCategory.INVOICES },
                    text = { Text("Deleted Invoices (${deletedSales.size})", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("trash_tab_invoices")
                )
                Tab(
                    selected = selectedTab == TrashCategory.PRODUCTS,
                    onClick = { selectedTab = TrashCategory.PRODUCTS },
                    text = { Text("Products (${deletedProducts.size})", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("trash_tab_products")
                )
                Tab(
                    selected = selectedTab == TrashCategory.CUSTOMERS,
                    onClick = { selectedTab = TrashCategory.CUSTOMERS },
                    text = { Text("Customers (${deletedCustomers.size})", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.testTag("trash_tab_customers")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (selectedTab) {
                TrashCategory.INVOICES -> {
                    if (deletedSales.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No deleted invoices in recycle bin.", color = Navy500)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(deletedSales, key = { it.id }) { sale ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("deleted_sale_${sale.id}"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = sale.invoiceNumber,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Navy900,
                                                        fontSize = 15.sp
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    StatusBadge(
                                                        text = "TRASHED",
                                                        backgroundColor = Rose100,
                                                        textColor = Rose700
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = "Customer: ${sale.customerName} • Cashier: ${sale.cashierName.ifBlank { "Muhammad Umer" }}",
                                                    fontSize = 12.sp,
                                                    color = Navy700
                                                )
                                                Text(
                                                    text = "Created: ${dateFormat.format(Date(sale.createdAt))} • Pay: ${sale.paymentType}",
                                                    fontSize = 11.sp,
                                                    color = Navy500
                                                )
                                            }

                                            Text(
                                                text = "$currency %.2f".format(sale.netAmount),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 15.sp,
                                                color = Navy900
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        HorizontalDivider(color = Slate100)
                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.restoreSale(sale.id, reDeductStock = true) {
                                                        Toast.makeText(context, "Invoice #${sale.invoiceNumber} restored!", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Emerald700),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(34.dp).testTag("restore_invoice_${sale.id}")
                                            ) {
                                                Icon(Icons.Default.Restore, contentDescription = "Restore", modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Restore Invoice", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Spacer(modifier = Modifier.width(8.dp))

                                            Button(
                                                onClick = {
                                                    viewModel.hardDeleteSale(sale.id) {
                                                        Toast.makeText(context, "Invoice permanently deleted", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(34.dp).testTag("hard_delete_invoice_${sale.id}")
                                            ) {
                                                Icon(Icons.Default.DeleteForever, contentDescription = "Purge", modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Purge Forever", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                TrashCategory.PRODUCTS -> {
                    if (deletedProducts.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No deleted products found.", color = Navy500)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(deletedProducts, key = { it.id }) { product ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(product.name, fontWeight = FontWeight.Bold, color = Navy900)
                                            Text("Category: ${product.category} • Price: $currency %.2f".format(product.salePrice), fontSize = 12.sp, color = Navy500)
                                        }
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    viewModel.restoreProduct(product.id)
                                                    Toast.makeText(context, "${product.name} restored", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.testTag("restore_product_${product.id}")
                                            ) {
                                                Icon(Icons.Default.Restore, contentDescription = "Restore", tint = Emerald600)
                                            }
                                            IconButton(
                                                onClick = {
                                                    viewModel.hardDeleteProduct(product.id)
                                                    Toast.makeText(context, "${product.name} purged", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Icon(Icons.Default.DeleteForever, contentDescription = "Permanent Delete", tint = Rose600)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                TrashCategory.CUSTOMERS -> {
                    if (deletedCustomers.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No deleted customers found.", color = Navy500)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(deletedCustomers, key = { it.id }) { customer ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(customer.name, fontWeight = FontWeight.Bold, color = Navy900)
                                            Text("Phone: ${customer.phone.ifBlank { "N/A" }} • Balance: $currency %.2f".format(customer.balance), fontSize = 12.sp, color = Navy500)
                                        }
                                        IconButton(
                                            onClick = {
                                                viewModel.restoreCustomer(customer.id)
                                                Toast.makeText(context, "${customer.name} restored", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(Icons.Default.Restore, contentDescription = "Restore", tint = Emerald600)
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
}
