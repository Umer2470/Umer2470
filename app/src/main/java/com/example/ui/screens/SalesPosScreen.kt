package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.network.ConnectionState
import com.example.data.api.sync.SyncState
import com.example.data.entity.Customer
import com.example.data.entity.Product
import com.example.ui.components.BarCodeScannerDialog
import com.example.ui.components.InvoiceReceiptDialog
import com.example.ui.theme.Rose600
import com.example.ui.viewmodel.CartItem
import com.example.ui.viewmodel.StoreViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesPosScreen(viewModel: StoreViewModel) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val customers by viewModel.allCustomers.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val discountAmount by viewModel.discountAmount.collectAsState()
    val paidAmountInput by viewModel.paidAmountInput.collectAsState()
    val paymentType by viewModel.paymentType.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val activeCashier by viewModel.activeCashierProfile.collectAsState()

    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val isSyncing by viewModel.syncState.collectAsState()

    val showInvoiceDialog by viewModel.showInvoiceDialog.collectAsState()
    val lastCompletedSale by viewModel.lastCompletedSale.collectAsState()
    val lastCompletedSaleItems by viewModel.lastCompletedSaleItems.collectAsState()

    var showBarcodeScanner by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Select Products, 1: Cart Checkout
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var customerDropdownExpanded by remember { mutableStateOf(false) }

    // Filter products for POS list
    val posProducts = allProducts.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.barcode.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true)
    }

    val cartSubtotal = cart.sumOf { it.total }
    val totalItemsCount = cart.sumOf { it.quantity.toInt() }
    val cartNetTotal = (cartSubtotal - discountAmount).coerceAtLeast(0.0)
    val paidVal = paidAmountInput.toDoubleOrNull() ?: cartNetTotal
    val changeToReturn = (paidVal - cartNetTotal).coerceAtLeast(0.0)
    val remainingDue = (cartNetTotal - paidVal).coerceAtLeast(0.0)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val isWideScreen = maxWidth >= 600.dp

        if (isWideScreen) {
            // Tablet / Desktop Wide View: Split 2-Column POS Layout
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Catalog & Barcode Scanner
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                ) {
                    PosOfflineSyncBanner(
                        connectionState = connectionStatus.state,
                        latencyMs = connectionStatus.latencyMs,
                        pendingSyncCount = pendingSyncCount,
                        isSyncing = isSyncing == SyncState.SYNCING,
                        onSyncClick = { viewModel.checkConnectionAndSync() }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    PosActiveCashierBadge(
                        cashierName = activeCashier.name,
                        designation = activeCashier.designation,
                        employeeId = activeCashier.employeeId
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    PosCatalogHeader(
                        searchQuery = searchQuery,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onScanBarcodeClick = { showBarcodeScanner = true }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    PosProductsList(
                        products = posProducts,
                        cart = cart,
                        currencySymbol = settings.currencySymbol,
                        onAddToCart = { product -> viewModel.addToCart(product, 1.0) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Right Column: Live Cart & Invoice Calculation
                Column(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                ) {
                    PosCartAndCheckoutSection(
                        cart = cart,
                        settingsCurrency = settings.currencySymbol,
                        selectedCustomer = selectedCustomer,
                        customers = customers,
                        customerDropdownExpanded = customerDropdownExpanded,
                        onCustomerDropdownExpandedChange = { customerDropdownExpanded = it },
                        onSelectCustomer = { viewModel.setSelectedCustomer(it) },
                        onAddCustomerClick = { showAddCustomerDialog = true },
                        paymentType = paymentType,
                        onPaymentTypeChange = { viewModel.setPaymentType(it) },
                        discountAmount = discountAmount,
                        onDiscountAmountChange = { viewModel.setDiscountAmount(it) },
                        paidAmountInput = paidAmountInput,
                        onPaidAmountInputChange = { viewModel.setPaidAmountInput(it) },
                        cartSubtotal = cartSubtotal,
                        cartNetTotal = cartNetTotal,
                        totalItemsCount = totalItemsCount,
                        changeToReturn = changeToReturn,
                        remainingDue = remainingDue,
                        onUpdateCartQty = { id, qty -> viewModel.updateCartQty(id, qty) },
                        onClearCart = { viewModel.clearCart() },
                        onProcessSale = { viewModel.processSale() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            // Mobile Vertical Screen: Dual Tab Switcher
            Column(modifier = Modifier.fillMaxSize()) {
                PosOfflineSyncBanner(
                    connectionState = connectionStatus.state,
                    latencyMs = connectionStatus.latencyMs,
                    pendingSyncCount = pendingSyncCount,
                    isSyncing = isSyncing == SyncState.SYNCING,
                    onSyncClick = { viewModel.checkConnectionAndSync() }
                )

                Spacer(modifier = Modifier.height(6.dp))

                PosActiveCashierBadge(
                    cashierName = activeCashier.name,
                    designation = activeCashier.designation,
                    employeeId = activeCashier.employeeId
                )

                Spacer(modifier = Modifier.height(6.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.testTag("pos_tab_items"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Select Items")
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.testTag("pos_tab_cart"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cart ($totalItemsCount)")
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    // CATALOG TAB
                    Column(modifier = Modifier.fillMaxSize()) {
                        PosCatalogHeader(
                            searchQuery = searchQuery,
                            onSearchChange = { viewModel.setSearchQuery(it) },
                            onScanBarcodeClick = { showBarcodeScanner = true }
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        PosProductsList(
                            products = posProducts,
                            cart = cart,
                            currencySymbol = settings.currencySymbol,
                            onAddToCart = { product -> viewModel.addToCart(product, 1.0) },
                            modifier = Modifier.weight(1f)
                        )

                        if (cart.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { selectedTab = 1 },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("pos_goto_checkout_btn"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("$totalItemsCount Items in Cart", fontWeight = FontWeight.Bold)
                                    Text("Proceed to Checkout (${settings.currencySymbol} ${cartSubtotal.toInt()}) →", fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                } else {
                    // CART CHECKOUT TAB
                    PosCartAndCheckoutSection(
                        cart = cart,
                        settingsCurrency = settings.currencySymbol,
                        selectedCustomer = selectedCustomer,
                        customers = customers,
                        customerDropdownExpanded = customerDropdownExpanded,
                        onCustomerDropdownExpandedChange = { customerDropdownExpanded = it },
                        onSelectCustomer = { viewModel.setSelectedCustomer(it) },
                        onAddCustomerClick = { showAddCustomerDialog = true },
                        paymentType = paymentType,
                        onPaymentTypeChange = { viewModel.setPaymentType(it) },
                        discountAmount = discountAmount,
                        onDiscountAmountChange = { viewModel.setDiscountAmount(it) },
                        paidAmountInput = paidAmountInput,
                        onPaidAmountInputChange = { viewModel.setPaidAmountInput(it) },
                        cartSubtotal = cartSubtotal,
                        cartNetTotal = cartNetTotal,
                        totalItemsCount = totalItemsCount,
                        changeToReturn = changeToReturn,
                        remainingDue = remainingDue,
                        onUpdateCartQty = { id, qty -> viewModel.updateCartQty(id, qty) },
                        onClearCart = { viewModel.clearCart() },
                        onProcessSale = { viewModel.processSale() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    // Barcode Scanner Dialog
    if (showBarcodeScanner) {
        BarCodeScannerDialog(
            productsList = allProducts,
            onBarcodeScanned = { barcode ->
                val p = allProducts.find { it.barcode == barcode }
                if (p != null) {
                    viewModel.addToCart(p, 1.0)
                    viewModel.showToast("Added '${p.name}' to cart!")
                } else {
                    viewModel.setSearchQuery(barcode)
                }
                showBarcodeScanner = false
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }

    // Add Customer Quick Modal
    if (showAddCustomerDialog) {
        AddCustomerDialog(
            onSave = { name, phone, address ->
                viewModel.saveCustomer(Customer(name = name, phone = phone, address = address))
                showAddCustomerDialog = false
            },
            onDismiss = { showAddCustomerDialog = false }
        )
    }

    // Invoice Receipt Dialog
    if (showInvoiceDialog && lastCompletedSale != null) {
        InvoiceReceiptDialog(
            sale = lastCompletedSale!!,
            items = lastCompletedSaleItems,
            settings = settings,
            onDismiss = { viewModel.dismissInvoiceDialog() },
            onDeleteInvoice = { viewModel.deleteInvoice(it) }
        )
    }
}

@Composable
private fun PosCatalogHeader(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onScanBarcodeClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .weight(1f)
                .testTag("pos_search_input"),
            placeholder = { Text("Search product name, category or barcode...") },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(16.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onScanBarcodeClick,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .testTag("pos_barcode_scan_btn")
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = "Scan Barcode",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun PosProductsList(
    products: List<Product>,
    cart: List<CartItem>,
    currencySymbol: String,
    onAddToCart: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    if (products.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text("No products found matching search.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(products) { product ->
                val inCartItem = cart.find { it.product.id == product.id }
                val qtyInCart = inCartItem?.quantity?.toInt() ?: 0

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pos_product_card_${product.id}")
                        .clickable { onAddToCart(product) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (qtyInCart > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${product.category} • Stock: ${product.stockQuantity.toInt()} ${product.unit}",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (product.stockQuantity <= product.minStockLevel) Rose600 else Color.Gray
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$currencySymbol ${product.salePrice.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Button(
                                onClick = { onAddToCart(product) },
                                modifier = Modifier.testTag("pos_add_to_cart_btn_${product.id}"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (qtyInCart > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Icon(imageVector = Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                                if (qtyInCart > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("$qtyInCart")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PosCartAndCheckoutSection(
    cart: List<CartItem>,
    settingsCurrency: String,
    selectedCustomer: Customer?,
    customers: List<Customer>,
    customerDropdownExpanded: Boolean,
    onCustomerDropdownExpandedChange: (Boolean) -> Unit,
    onSelectCustomer: (Customer?) -> Unit,
    onAddCustomerClick: () -> Unit,
    paymentType: String,
    onPaymentTypeChange: (String) -> Unit,
    discountAmount: Double,
    onDiscountAmountChange: (Double) -> Unit,
    paidAmountInput: String,
    onPaidAmountInputChange: (String) -> Unit,
    cartSubtotal: Double,
    cartNetTotal: Double,
    totalItemsCount: Int,
    changeToReturn: Double,
    remainingDue: Double,
    onUpdateCartQty: (Long, Double) -> Unit,
    onClearCart: () -> Unit,
    onProcessSale: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (cart.isEmpty()) {
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(56.dp), tint = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Text("POS Cart is empty", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                Text("Select items from catalog or scan product barcode.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    } else {
        LazyColumn(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cart Items ($totalItemsCount)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(
                        onClick = onClearCart,
                        modifier = Modifier.testTag("pos_clear_cart_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = Rose600, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Cart", color = Rose600)
                    }
                }
            }

            // Cart Items List
            items(cart) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pos_cart_item_${item.product.id}"),
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
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text(item.product.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text("@ $settingsCurrency ${item.customPrice.toInt()} / ${item.product.unit}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onUpdateCartQty(item.product.id, item.quantity - 1) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(imageVector = Icons.Default.Remove, contentDescription = null)
                            }

                            Text(
                                text = "${item.quantity.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            IconButton(
                                onClick = { onUpdateCartQty(item.product.id, item.quantity + 1) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Text(
                                text = "$settingsCurrency ${item.total.toInt()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // Customer Selector & Payment Mode
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Customer & Payment Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Customer Dropdown
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        ExposedDropdownMenuBox(
                            expanded = customerDropdownExpanded,
                            onExpandedChange = { onCustomerDropdownExpandedChange(!customerDropdownExpanded) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pos_customer_dropdown")
                        ) {
                            OutlinedTextField(
                                value = selectedCustomer?.let { "${it.name} (${if (it.balance > 0) "Udhaar: $settingsCurrency ${it.balance.toInt()}" else "No Due"})" } ?: "Cash Customer (Default)",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Customer") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )

                            ExposedDropdownMenu(
                                expanded = customerDropdownExpanded,
                                onDismissRequest = { onCustomerDropdownExpandedChange(false) }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Cash Customer (Default)") },
                                    onClick = {
                                        onSelectCustomer(null)
                                        onCustomerDropdownExpandedChange(false)
                                    }
                                )
                                customers.forEach { cust ->
                                    DropdownMenuItem(
                                        text = { Text("${cust.name} ${if (cust.balance > 0) "- Udhaar: $settingsCurrency ${cust.balance.toInt()}" else ""}") },
                                        onClick = {
                                            onSelectCustomer(cust)
                                            onCustomerDropdownExpandedChange(false)
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = onAddCustomerClick,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .testTag("pos_add_customer_btn")
                        ) {
                            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "Add Customer", tint = MaterialTheme.colorScheme.secondary)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Payment Type Chips
                    Text("Payment Mode", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    val paymentModes = listOf("Cash", "Udhaar / Credit", "Card / Online")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        paymentModes.forEach { mode ->
                            FilterChip(
                                selected = paymentType == mode,
                                onClick = { onPaymentTypeChange(mode) },
                                label = { Text(mode) },
                                modifier = Modifier.testTag("pos_payment_chip_$mode")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Discount & Paid Inputs
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = if (discountAmount == 0.0) "" else discountAmount.toInt().toString(),
                            onValueChange = { onDiscountAmountChange(it.toDoubleOrNull() ?: 0.0) },
                            label = { Text("Discount ($settingsCurrency)") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pos_discount_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        OutlinedTextField(
                            value = paidAmountInput,
                            onValueChange = onPaidAmountInputChange,
                            label = { Text("Paid Amount") },
                            placeholder = { Text("${cartNetTotal.toInt()}") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pos_paid_input"),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Financial Summary Breakdown Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pos_subtotal_summary_card"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Gross Subtotal ($totalItemsCount items):", style = MaterialTheme.typography.bodyMedium)
                                Text("$settingsCurrency ${cartSubtotal.toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                            if (discountAmount > 0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Discount:", style = MaterialTheme.typography.bodyMedium, color = Rose600)
                                    Text("- $settingsCurrency ${discountAmount.toInt()}", style = MaterialTheme.typography.bodyMedium, color = Rose600, fontWeight = FontWeight.Bold)
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Net Payable Total:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("$settingsCurrency ${cartNetTotal.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            }
                            if (changeToReturn > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Change to Return:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text("$settingsCurrency ${changeToReturn.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (remainingDue > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Remaining Udhaar / Due:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Rose600)
                                    Text("$settingsCurrency ${remainingDue.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Rose600)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Checkout Button
                    Button(
                        onClick = onProcessSale,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("pos_checkout_btn"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Complete Sale & Generate Invoice ($settingsCurrency ${cartNetTotal.toInt()})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AddCustomerDialog(
    onSave: (String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Customer") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer Name *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_customer_name_input")
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_customer_phone_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address / Location") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_customer_address_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onSave(name.trim(), phone.trim(), address.trim())
                },
                modifier = Modifier.testTag("save_customer_btn")
            ) { Text("Save Customer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PosOfflineSyncBanner(
    connectionState: ConnectionState,
    latencyMs: Long,
    pendingSyncCount: Int,
    isSyncing: Boolean,
    onSyncClick: () -> Unit
) {
    Surface(
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            1.dp,
            when (connectionState) {
                ConnectionState.ONLINE_CONNECTED -> Color(0xFF10B981).copy(alpha = 0.4f)
                ConnectionState.ONLINE_UNREACHABLE -> Color(0xFFF59E0B).copy(alpha = 0.4f)
                ConnectionState.OFFLINE -> Color(0xFF38BDF8).copy(alpha = 0.3f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pos_offline_sync_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when (connectionState) {
                                ConnectionState.ONLINE_CONNECTED -> Color(0xFF10B981)
                                ConnectionState.ONLINE_UNREACHABLE -> Color(0xFFF59E0B)
                                ConnectionState.OFFLINE -> Color(0xFFEF4444)
                            }
                        )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when (connectionState) {
                                ConnectionState.ONLINE_CONNECTED -> "Server Online"
                                ConnectionState.ONLINE_UNREACHABLE -> "Server Unreachable"
                                ConnectionState.OFFLINE -> "Offline Mode"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = when (connectionState) {
                                ConnectionState.ONLINE_CONNECTED -> Color(0xFF10B981)
                                ConnectionState.ONLINE_UNREACHABLE -> Color(0xFFF59E0B)
                                ConnectionState.OFFLINE -> Color(0xFF38BDF8)
                            }
                        )

                        Text(
                            text = " • Local SQLite Master (100% Safe)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.LightGray
                        )
                    }

                    Text(
                        text = if (isSyncing) "Synchronizing changes with developer cloud..."
                        else if (pendingSyncCount > 0) "$pendingSyncCount offline change(s) queued for sync"
                        else if (connectionState == ConnectionState.ONLINE_CONNECTED) "Cloud sync active & standby (${if (latencyMs > 0) "${latencyMs}ms" else "OK"})"
                        else "All POS sales, scans, and invoices saved locally",
                        fontSize = 10.sp,
                        color = if (pendingSyncCount > 0) Color(0xFFFFD700) else Color.Gray
                    )
                }
            }

            if (isSyncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = Color(0xFF38BDF8),
                    strokeWidth = 2.dp
                )
            } else {
                Surface(
                    onClick = onSyncClick,
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (pendingSyncCount > 0) "Sync ($pendingSyncCount)" else "Check",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PosActiveCashierBadge(
    cashierName: String,
    designation: String,
    employeeId: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("pos_active_cashier_badge")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Icon(
                    imageVector = Icons.Default.Badge,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Cashier: ",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = cashierName.ifBlank { "Not Assigned" },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (cashierName.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray
                )
                if (designation.isNotBlank() && cashierName.isNotBlank()) {
                    Text(
                        text = " • $designation",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (employeeId.isNotBlank()) {
                Text(
                    text = "ID: $employeeId",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
