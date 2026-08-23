package com.example.ui.screens

import androidx.compose.foundation.background
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
import com.example.data.entity.Product
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.ui.components.AppHeader
import com.example.ui.components.BarCodeScannerDialog
import com.example.ui.invoice.InvoiceReceiptDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesPosScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val products by viewModel.products.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val discountAmount by viewModel.discountAmount.collectAsState()
    val taxRatePercent by viewModel.taxRatePercent.collectAsState()
    val receivedAmount by viewModel.receivedAmount.collectAsState()
    val paymentType by viewModel.paymentType.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showScannerDialog by remember { mutableStateOf(false) }
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf(false) }
    var lastSale by remember { mutableStateOf<Sale?>(null) }
    var lastSaleItems by remember { mutableStateOf<List<SaleItem>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val currency = storeSettings?.currencySymbol ?: "Rs"
    val subtotal = remember(cart) { cart.sumOf { it.totalPrice } }
    val taxAmount = remember(subtotal, discountAmount, taxRatePercent) {
        ((subtotal - discountAmount).coerceAtLeast(0.0) * taxRatePercent) / 100.0
    }
    val netAmount = remember(subtotal, discountAmount, taxAmount) {
        (subtotal - discountAmount + taxAmount).coerceAtLeast(0.0)
    }

    val filteredProducts = remember(products, searchQuery) {
        if (searchQuery.isBlank()) products
        else {
            products.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.barcode.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    if (showScannerDialog) {
        BarCodeScannerDialog(
            onBarcodeScanned = { barcode ->
                val matched = products.find { it.barcode.equals(barcode, ignoreCase = true) }
                if (matched != null) {
                    viewModel.addToCart(matched)
                } else {
                    errorMessage = "No product found with barcode: $barcode"
                }
            },
            onDismiss = { showScannerDialog = false }
        )
    }

    if (showReceiptDialog && lastSale != null) {
        InvoiceReceiptDialog(
            sale = lastSale!!,
            items = lastSaleItems,
            settings = storeSettings,
            onDismiss = { showReceiptDialog = false }
        )
    }

    if (showCheckoutDialog) {
        var discInput by remember { mutableStateOf(if (discountAmount > 0) discountAmount.toString() else "") }
        var recInput by remember { mutableStateOf(if (receivedAmount > 0) receivedAmount.toString() else netAmount.toString()) }

        AlertDialog(
            onDismissRequest = { showCheckoutDialog = false },
            title = { Text("Complete Sale Checkout") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Net Payable: $currency %.2f".format(netAmount),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Navy900
                    )

                    OutlinedTextField(
                        value = discInput,
                        onValueChange = {
                            discInput = it
                            viewModel.setDiscount(it.toDoubleOrNull() ?: 0.0)
                        },
                        label = { Text("Discount Amount ($currency)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = recInput,
                        onValueChange = {
                            recInput = it
                            viewModel.setReceivedAmount(it.toDoubleOrNull() ?: 0.0)
                        },
                        label = { Text("Cash Received ($currency)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Cash", "Bank", "Credit").forEach { mode ->
                            FilterChip(
                                selected = paymentType == mode,
                                onClick = { viewModel.setPaymentType(mode) },
                                label = { Text(mode) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.completeSale(
                            onSuccess = { sale, items ->
                                lastSale = sale
                                lastSaleItems = items
                                showCheckoutDialog = false
                                showReceiptDialog = true
                            },
                            onError = { err ->
                                errorMessage = err
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    modifier = Modifier.testTag("confirm_checkout_button")
                ) {
                    Text("Charge $currency %.2f".format(netAmount))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCheckoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Sales Counter POS",
                subtitle = "Active Cart: ${cart.size} Items",
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = { showScannerDialog = true },
                        modifier = Modifier.testTag("pos_scanner_button")
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan", tint = Color.White)
                    }
                }
            )
        },
        containerColor = Slate50
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp)
        ) {
            if (errorMessage != null) {
                Surface(
                    color = Rose100,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = Rose600,
                            modifier = Modifier.weight(1f),
                            fontSize = 12.sp
                        )
                        IconButton(onClick = { errorMessage = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Rose600)
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search product name or barcode...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pos_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Navy900,
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Main View: Top Cart Summary, Bottom Product Catalog
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Products Catalog Column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = "Catalog (${filteredProducts.size})",
                        fontWeight = FontWeight.Bold,
                        color = Navy900,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredProducts, key = { it.id }) { product ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.addToCart(product) }
                                    .testTag("product_card_${product.id}"),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = product.name,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = Navy900
                                        )
                                        Text(
                                            text = "$currency %.2f • Stock: %.0f %s".format(product.salePrice, product.stockQuantity, product.unit),
                                            fontSize = 11.sp,
                                            color = Navy500
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.addToCart(product) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.AddShoppingCart, contentDescription = "Add", tint = Navy900)
                                    }
                                }
                            }
                        }
                    }
                }

                // Active Cart Column
                Card(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cart (${cart.size})",
                                fontWeight = FontWeight.Bold,
                                color = Navy900,
                                fontSize = 14.sp
                            )
                            if (cart.isNotEmpty()) {
                                TextButton(
                                    onClick = { viewModel.clearCart() },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Clear", color = Rose600, fontSize = 11.sp)
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 6.dp))

                        if (cart.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Cart is empty", color = Navy500, fontSize = 12.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(cart, key = { it.product.id }) { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Slate50, RoundedCornerShape(6.dp))
                                            .padding(6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.product.name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                            Text("$currency %.2f x %.1f".format(item.unitPrice, item.quantity), fontSize = 10.sp, color = Navy500)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { viewModel.updateCartItemQuantity(item.product.id, item.quantity - 1) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(14.dp))
                                            }
                                            Text("%.0f".format(item.quantity), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                            IconButton(
                                                onClick = { viewModel.updateCartItemQuantity(item.product.id, item.quantity + 1) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 6.dp))

                        // Totals & Checkout Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Net Total:", fontWeight = FontWeight.Bold, color = Navy900, fontSize = 14.sp)
                            Text("$currency %.2f".format(netAmount), fontWeight = FontWeight.Bold, color = Emerald600, fontSize = 16.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { showCheckoutDialog = true },
                            enabled = cart.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("checkout_button")
                        ) {
                            Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Checkout ($currency %.0f)".format(netAmount))
                        }
                    }
                }
            }
        }
    }
}
