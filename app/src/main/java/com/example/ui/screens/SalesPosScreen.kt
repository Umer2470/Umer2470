package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Customer
import com.example.data.entity.Product
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.ui.components.ShopLogoAvatar
import com.example.ui.components.StatusBadge
import com.example.ui.invoice.InvoiceReceiptDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.CartItem
import com.example.ui.viewmodel.HeldCart
import com.example.ui.viewmodel.StoreViewModel

enum class PosTab {
    CART,
    CATALOG
}

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
    val businessProfile by viewModel.businessProfile.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val activeCashierName by viewModel.activeCashierName.collectAsState()
    val users by viewModel.users.collectAsState()
    val branches by viewModel.branches.collectAsState()
    val heldCarts by viewModel.heldCarts.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var currentTab by remember { mutableStateOf(PosTab.CART) }

    var showCheckoutDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf(false) }
    var showCustomerDialog by remember { mutableStateOf(false) }
    var showDiscountDialog by remember { mutableStateOf(false) }
    var showHeldCartsDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showCashierSelectorDialog by remember { mutableStateOf(false) }

    var lastSale by remember { mutableStateOf<Sale?>(null) }
    var lastSaleItems by remember { mutableStateOf<List<SaleItem>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successToast by remember { mutableStateOf<String?>(null) }

    val currency = storeSettings?.currencySymbol ?: "Rs"
    val storeDisplayName = storeSettings?.storeName?.ifBlank { null }
        ?: businessProfile?.businessName?.ifBlank { null }
        ?: "CH UMER POS"
    val branchDisplayName = branches.firstOrNull()?.name ?: "Main Outlet"

    // Calculations
    val subtotal = remember(cart) { cart.sumOf { it.totalPrice } }
    val totalUnits = remember(cart) { cart.sumOf { it.quantity } }
    val taxAmount = remember(subtotal, discountAmount, taxRatePercent) {
        ((subtotal - discountAmount).coerceAtLeast(0.0) * taxRatePercent) / 100.0
    }
    val netAmount = remember(subtotal, discountAmount, taxAmount) {
        (subtotal - discountAmount + taxAmount).coerceAtLeast(0.0)
    }

    // Dynamic Categories from Products
    val categories = remember(products) {
        val unique = products.map { if (it.category.isNotBlank()) it.category.trim() else "General" }.distinct()
        listOf("All") + unique
    }

    // Filtered Products
    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { p ->
            val matchesCategory = selectedCategory == "All" || p.category.equals(selectedCategory, ignoreCase = true)
            val matchesQuery = searchQuery.isBlank() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    p.barcode.contains(searchQuery, ignoreCase = true) ||
                    p.category.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    // Receipt Dialog
    if (showReceiptDialog && lastSale != null) {
        InvoiceReceiptDialog(
            sale = lastSale!!,
            items = lastSaleItems,
            settings = storeSettings,
            onDismiss = { showReceiptDialog = false }
        )
    }

    // Customer Selection Dialog
    if (showCustomerDialog) {
        var custSearch by remember { mutableStateOf("") }
        val filteredCusts = remember(customers, custSearch) {
            if (custSearch.isBlank()) customers
            else customers.filter {
                it.name.contains(custSearch, ignoreCase = true) ||
                it.phone.contains(custSearch, ignoreCase = true)
            }
        }

        AlertDialog(
            onDismissRequest = { showCustomerDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Customer", fontWeight = FontWeight.Bold, color = Navy900, fontSize = 16.sp)
                    IconButton(onClick = { showCustomerDialog = false }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Navy500)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = custSearch,
                        onValueChange = { custSearch = it },
                        placeholder = { Text("Search by name or phone...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Navy500) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("customer_search_input")
                    )

                    // Walk-in Customer Quick Option
                    Surface(
                        onClick = {
                            viewModel.setSelectedCustomer(null)
                            showCustomerDialog = false
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedCustomer == null) Emerald50 else Slate100,
                        border = if (selectedCustomer == null) ButtonDefaults.outlinedButtonBorder else null,
                        modifier = Modifier.fillMaxWidth().testTag("customer_walkin_option")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.PersonOutline, contentDescription = null, tint = if (selectedCustomer == null) Emerald600 else Navy500)
                                Column {
                                    Text("Walk-in Customer", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Navy900)
                                    Text("Default Cash Counter Sale", fontSize = 11.sp, color = Navy500)
                                }
                            }
                            if (selectedCustomer == null) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = Emerald600, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    Text("Registered Customers (${filteredCusts.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Navy700)

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filteredCusts, key = { it.id }) { cust ->
                            val isSelected = selectedCustomer?.id == cust.id
                            Surface(
                                onClick = {
                                    viewModel.setSelectedCustomer(cust)
                                    showCustomerDialog = false
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Blue50 else Color.White,
                                border = ButtonDefaults.outlinedButtonBorder,
                                modifier = Modifier.fillMaxWidth().testTag("customer_item_${cust.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Navy900)
                                        Text("📞 ${cust.phone.ifBlank { "No phone" }}", fontSize = 11.sp, color = Navy500)
                                    }
                                    if (cust.balance > 0) {
                                        StatusBadge(
                                            text = "Due: $currency %.0f".format(cust.balance),
                                            backgroundColor = Rose100,
                                            textColor = Rose600
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Cashier Selector Dialog
    if (showCashierSelectorDialog) {
        var customCashierName by remember { mutableStateOf("") }
        var cashierSearchQuery by remember { mutableStateOf("") }

        val activeCashiers = remember(users, cashierSearchQuery) {
            users.filter { it.isActive && (cashierSearchQuery.isBlank() ||
                    it.fullName.contains(cashierSearchQuery, ignoreCase = true) ||
                    it.username.contains(cashierSearchQuery, ignoreCase = true)) }
        }
        val inactiveCashiers = remember(users, cashierSearchQuery) {
            users.filter { !it.isActive && (cashierSearchQuery.isBlank() ||
                    it.fullName.contains(cashierSearchQuery, ignoreCase = true) ||
                    it.username.contains(cashierSearchQuery, ignoreCase = true)) }
        }

        AlertDialog(
            onDismissRequest = { showCashierSelectorDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Select Active Cashier", fontWeight = FontWeight.Bold, color = Navy900, fontSize = 16.sp)
                        Text("Assign operator for sales & invoices", fontSize = 11.sp, color = Navy500)
                    }
                    IconButton(onClick = { showCashierSelectorDialog = false }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Navy500)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Current Active Cashier Notice
                    Surface(
                        color = Navy900,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = Gold500, modifier = Modifier.size(18.dp))
                            Column {
                                Text("Current Active Cashier", fontSize = 10.sp, color = Slate300)
                                Text(activeCashierName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }

                    if (users.size > 3) {
                        OutlinedTextField(
                            value = cashierSearchQuery,
                            onValueChange = { cashierSearchQuery = it },
                            placeholder = { Text("Filter cashier list...", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("pos_cashier_search_input")
                        )
                    }

                    Text("Active Cashiers (${activeCashiers.size}):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Navy800)

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(activeCashiers, key = { it.id }) { u ->
                            val isSelected = activeCashierName.equals(u.fullName, ignoreCase = true) ||
                                    (u.fullName.isBlank() && activeCashierName.equals(u.username, ignoreCase = true))
                            Surface(
                                onClick = {
                                    val name = u.fullName.ifBlank { u.username }
                                    viewModel.setActiveCashierName(name, u)
                                    showCashierSelectorDialog = false
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Emerald50 else Slate50,
                                border = if (isSelected) ButtonDefaults.outlinedButtonBorder else null,
                                modifier = Modifier.fillMaxWidth().testTag("select_pos_cashier_${u.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = u.fullName.ifBlank { u.username },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Navy900
                                        )
                                        Text("Role: ${u.role} • @${u.username}", fontSize = 11.sp, color = Navy600)
                                    }
                                    if (isSelected) {
                                        StatusBadge(text = "CURRENT", backgroundColor = Emerald100, textColor = Emerald700)
                                    }
                                }
                            }
                        }

                        if (inactiveCashiers.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Inactive Staff (Disabled):", fontSize = 11.sp, color = Slate400, fontWeight = FontWeight.SemiBold)
                            }
                            items(inactiveCashiers, key = { "inactive_${it.id}" }) { inact ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Slate100,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = inact.fullName.ifBlank { inact.username },
                                            fontSize = 12.sp,
                                            color = Slate500
                                        )
                                        StatusBadge(text = "INACTIVE", backgroundColor = Rose100, textColor = Rose600)
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                    Text("Or Enter Custom Cashier Name:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Navy700)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customCashierName,
                            onValueChange = { customCashierName = it },
                            placeholder = { Text("e.g. Muhammad Umer", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("custom_cashier_input")
                        )
                        Button(
                            onClick = {
                                if (customCashierName.isNotBlank()) {
                                    viewModel.setActiveCashierName(customCashierName.trim())
                                    showCashierSelectorDialog = false
                                }
                            },
                            enabled = customCashierName.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("apply_custom_cashier_button")
                        ) {
                            Text("Set", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // Held Carts Dialog
    if (showHeldCartsDialog) {
        AlertDialog(
            onDismissRequest = { showHeldCartsDialog = false },
            title = {
                Text("Held POS Sales (${heldCarts.size})", fontWeight = FontWeight.Bold, color = Navy900, fontSize = 16.sp)
            },
            text = {
                if (heldCarts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No held sales at this time.", color = Navy500, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(heldCarts, key = { it.id }) { held ->
                            val heldTotal = held.items.sumOf { it.totalPrice }
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Slate50)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(held.note, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Navy900)
                                        Text(
                                            text = "${held.items.size} items • $currency %.2f".format(heldTotal),
                                            fontSize = 12.sp,
                                            color = Navy600
                                        )
                                        Text(
                                            text = "Customer: ${held.customer?.name ?: "Walk-in"}",
                                            fontSize = 11.sp,
                                            color = Navy500
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Button(
                                            onClick = {
                                                viewModel.restoreHeldCart(held)
                                                showHeldCartsDialog = false
                                                currentTab = PosTab.CART
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                            modifier = Modifier.testTag("restore_held_${held.id}")
                                        ) {
                                            Text("Recall", fontSize = 11.sp)
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteHeldCart(held.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Rose600, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHeldCartsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Quick Discount Dialog
    if (showDiscountDialog) {
        var discVal by remember { mutableStateOf(if (discountAmount > 0) discountAmount.toString() else "") }

        AlertDialog(
            onDismissRequest = { showDiscountDialog = false },
            title = { Text("Set Bill Discount ($currency)", fontWeight = FontWeight.Bold, color = Navy900) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Current Subtotal: $currency %.2f".format(subtotal), fontSize = 13.sp, color = Navy700)
                    OutlinedTextField(
                        value = discVal,
                        onValueChange = { discVal = it },
                        label = { Text("Discount Amount ($currency)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("discount_modal_input")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(50.0, 100.0, 200.0, 500.0).forEach { amt ->
                            OutlinedButton(
                                onClick = { discVal = amt.toString() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("$currency %.0f".format(amt), fontSize = 10.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val d = discVal.toDoubleOrNull() ?: 0.0
                        viewModel.setDiscount(d)
                        showDiscountDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    modifier = Modifier.testTag("apply_discount_button")
                ) {
                    Text("Apply Discount")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.setDiscount(0.0)
                    showDiscountDialog = false
                }) {
                    Text("Clear Discount", color = Rose600)
                }
            }
        )
    }

    // Clear Cart Confirm Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear Current Cart?", fontWeight = FontWeight.Bold, color = Navy900) },
            text = { Text("This will remove all ${cart.size} items from the current bill. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearCart()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                    modifier = Modifier.testTag("confirm_clear_cart_button")
                ) {
                    Text("Yes, Clear Cart")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Professional Checkout Dialog
    if (showCheckoutDialog) {
        var discInput by remember { mutableStateOf(if (discountAmount > 0) discountAmount.toString() else "") }
        var recInput by remember { mutableStateOf(if (receivedAmount > 0) receivedAmount.toString() else "%.2f".format(netAmount)) }

        val recVal = recInput.toDoubleOrNull() ?: 0.0
        val changeReturn = (recVal - netAmount).coerceAtLeast(0.0)
        val remainingDue = (netAmount - recVal).coerceAtLeast(0.0)

        AlertDialog(
            onDismissRequest = { showCheckoutDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Checkout & Payment", fontWeight = FontWeight.Bold, color = Navy900, fontSize = 17.sp)
                        Text("Customer: ${selectedCustomer?.name ?: "Walk-in Customer"}", fontSize = 12.sp, color = Navy500)
                    }
                    IconButton(onClick = { showCheckoutDialog = false }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Navy500)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Net Payable Banner
                    Surface(
                        color = Emerald50,
                        shape = RoundedCornerShape(10.dp),
                        border = ButtonDefaults.outlinedButtonBorder,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Total Amount Payable", fontSize = 11.sp, color = Navy600, fontWeight = FontWeight.Medium)
                                Text("${cart.size} Items • %.0f Units".format(totalUnits), fontSize = 10.sp, color = Navy500)
                            }
                            Text(
                                text = "$currency %.2f".format(netAmount),
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = Emerald700
                            )
                        }
                    }

                    // Active Cashier Row with Switch action
                    Surface(
                        color = Slate100,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Badge, contentDescription = null, tint = Navy700, modifier = Modifier.size(16.dp))
                                Text(
                                    text = "Cashier: $activeCashierName",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Navy900
                                )
                            }
                            TextButton(
                                onClick = { showCashierSelectorDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Switch", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Navy900)
                            }
                        }
                    }

                    // Payment Method Filter
                    Text("Payment Method:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Navy800)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Cash", "Bank", "Credit").forEach { mode ->
                            FilterChip(
                                selected = paymentType == mode,
                                onClick = {
                                    viewModel.setPaymentType(mode)
                                    if (mode == "Credit") {
                                        recInput = "0.00"
                                        viewModel.setReceivedAmount(0.0)
                                    } else if (mode == "Cash" && recVal == 0.0) {
                                        recInput = "%.2f".format(netAmount)
                                        viewModel.setReceivedAmount(netAmount)
                                    }
                                },
                                label = {
                                    Text(
                                        text = when(mode) {
                                            "Cash" -> "💵 Cash"
                                            "Bank" -> "💳 Bank / Card"
                                            else -> "📒 Credit (Udhar)"
                                        },
                                        fontSize = 11.sp
                                    )
                                },
                                modifier = Modifier.weight(1f).testTag("payment_chip_$mode")
                            )
                        }
                    }

                    // Cash Received / Tendered Input
                    OutlinedTextField(
                        value = recInput,
                        onValueChange = {
                            recInput = it
                            viewModel.setReceivedAmount(it.toDoubleOrNull() ?: 0.0)
                        },
                        label = { Text("Tendered / Received Amount ($currency)") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("received_amount_input")
                    )

                    // Quick Pay Tendered Buttons
                    if (paymentType == "Cash") {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("⚡ Quick Pay Tendered:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Navy700)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    onClick = {
                                        recInput = "%.2f".format(netAmount)
                                        viewModel.setReceivedAmount(netAmount)
                                    },
                                    color = if (recVal == netAmount) Gold500 else Gold100,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f).testTag("modal_quick_pay_exact")
                                ) {
                                    Text(
                                        text = "Exact",
                                        textAlign = TextAlign.Center,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (recVal == netAmount) Navy900 else Gold800,
                                        modifier = Modifier.padding(vertical = 7.dp)
                                    )
                                }

                                listOf(500.0, 1000.0, 2000.0, 5000.0).forEach { denom ->
                                    val isSelected = recVal == denom
                                    Surface(
                                        onClick = {
                                            recInput = denom.toString()
                                            viewModel.setReceivedAmount(denom)
                                        },
                                        color = if (isSelected) Navy900 else Slate100,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier.weight(1f).testTag("modal_quick_pay_${denom.toInt()}")
                                    ) {
                                        Text(
                                            text = "$currency ${denom.toInt()}",
                                            textAlign = TextAlign.Center,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) Color.White else Navy800,
                                            modifier = Modifier.padding(vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Change Return or Outstanding Balance calculation
                    if (paymentType != "Credit") {
                        Surface(
                            color = if (changeReturn > 0) Gold50 else Slate50,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (changeReturn > 0) "Change to Return:" else "Remaining Due:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Navy800
                                )
                                Text(
                                    text = if (changeReturn > 0) "$currency %.2f".format(changeReturn) else "$currency %.2f".format(remainingDue),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (changeReturn > 0) Gold700 else (if (remainingDue > 0) Rose600 else Emerald600)
                                )
                            }
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
                                successToast = "Sale recorded successfully! Invoice #${sale.invoiceNumber}"
                            },
                            onError = { err ->
                                errorMessage = err
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("confirm_checkout_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Charge & Issue Invoice ($currency %.2f)".format(netAmount), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCheckoutDialog = false }) {
                    Text("Back to Cart")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            // PROFESSIONAL POS HEADER
            Surface(
                color = Navy900,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Back button + Store Logo + Store Name & Branch
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }

                            ShopLogoAvatar(
                                logoUri = null,
                                size = 34.dp
                            )

                            Column(
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .clickable { showCashierSelectorDialog = true }
                            ) {
                                Text(
                                    text = storeDisplayName,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "$branchDisplayName • Cashier: $activeCashierName",
                                        color = Gold400,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Switch Cashier", tint = Gold400, modifier = Modifier.size(14.dp))
                                }
                            }
                        }

                        // Right: Status Badges (Secure POS / Offline Ready) + Barcode Scanner Trigger
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                color = Emerald900.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp),
                                border = ButtonDefaults.outlinedButtonBorder
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Emerald400)
                                    )
                                    Text(
                                        text = "Offline Ready",
                                        color = Emerald300,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // FIXED BOTTOM POS ACTION BAR
            Surface(
                color = Color.White,
                shadowElevation = 16.dp,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Quick Pay Row when cart is active
                    if (cart.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "⚡ Quick Pay:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy700
                            )

                            Surface(
                                onClick = {
                                    viewModel.setPaymentType("Cash")
                                    viewModel.setReceivedAmount(netAmount)
                                    showCheckoutDialog = true
                                },
                                color = Gold100,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.weight(1f).testTag("quick_pay_exact")
                            ) {
                                Text(
                                    text = "Exact",
                                    textAlign = TextAlign.Center,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Gold800,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            listOf(500.0, 1000.0, 2000.0, 5000.0).forEach { denom ->
                                Surface(
                                    onClick = {
                                        viewModel.setPaymentType("Cash")
                                        viewModel.setReceivedAmount(denom)
                                        showCheckoutDialog = true
                                    },
                                    color = Slate100,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.weight(1f).testTag("quick_pay_${denom.toInt()}")
                                ) {
                                    Text(
                                        text = "$currency ${denom.toInt()}",
                                        textAlign = TextAlign.Center,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Navy800,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        Divider(color = Slate200, thickness = 0.5.dp)
                    }

                    // Summary Strip: Subtotal, Discount & Grand Total
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Total Bill:", fontSize = 12.sp, color = Navy500)
                                if (discountAmount > 0) {
                                    Surface(color = Rose100, shape = RoundedCornerShape(4.dp)) {
                                        Text(
                                            text = "-$currency %.0f Disc".format(discountAmount),
                                            color = Rose600,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "$currency %.2f".format(netAmount),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = if (cart.isNotEmpty()) Emerald700 else Navy900
                            )
                        }

                        // Action Buttons: Hold, Clear, and Large Checkout
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Hold Button
                            OutlinedButton(
                                onClick = {
                                    if (cart.isNotEmpty()) {
                                        viewModel.holdCurrentCart("Hold #${heldCarts.size + 1}")
                                        successToast = "Order held safely. Next customer ready!"
                                    } else if (heldCarts.isNotEmpty()) {
                                        showHeldCartsDialog = true
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("hold_sale_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.PauseCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = Navy800)
                                    Text(
                                        text = if (cart.isNotEmpty()) "Hold" else "Held (${heldCarts.size})",
                                        fontSize = 12.sp,
                                        color = Navy800
                                    )
                                }
                            }

                            // Clear Cart Button (if cart has items)
                            if (cart.isNotEmpty()) {
                                IconButton(
                                    onClick = { showClearConfirmDialog = true },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .testTag("clear_cart_button")
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Clear", tint = Rose600)
                                }
                            }

                            // Primary Big Checkout Action
                            Button(
                                onClick = { showCheckoutDialog = true },
                                enabled = cart.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Emerald600,
                                    disabledContainerColor = Slate200
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                modifier = Modifier.testTag("checkout_button")
                            ) {
                                Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (cart.isNotEmpty()) "Charge $currency %.0f".format(netAmount) else "Checkout",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = Slate50
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Notification / Alert Banners
            if (errorMessage != null) {
                Surface(
                    color = Rose100,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Rose600, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = errorMessage!!,
                            color = Rose600,
                            modifier = Modifier.weight(1f),
                            fontSize = 12.sp
                        )
                        IconButton(onClick = { errorMessage = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Rose600, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            if (successToast != null) {
                Surface(
                    color = Emerald100,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald600, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = successToast!!,
                            color = Emerald800,
                            modifier = Modifier.weight(1f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        IconButton(onClick = { successToast = null }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Emerald700, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            // Customer Selector & Context Strip
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(10.dp),
                border = ButtonDefaults.outlinedButtonBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Customer Selector Chip
                    Row(
                        modifier = Modifier
                            .clickable { showCustomerDialog = true }
                            .padding(vertical = 4.dp)
                            .testTag("customer_select_chip"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = if (selectedCustomer != null) Blue600 else Navy500, modifier = Modifier.size(18.dp))
                        Column {
                            Text(
                                text = selectedCustomer?.name ?: "Walk-in Customer",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Navy900
                            )
                            Text(
                                text = if (selectedCustomer != null) "Tap to change customer" else "Tap to assign registered customer",
                                fontSize = 10.sp,
                                color = Navy500
                            )
                        }
                    }

                    // Quick Discount & Held Badge
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (heldCarts.isNotEmpty()) {
                            Surface(
                                onClick = { showHeldCartsDialog = true },
                                color = Gold100,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "⏸ ${heldCarts.size} Held",
                                    color = Gold800,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Surface(
                            onClick = { showDiscountDialog = true },
                            color = if (discountAmount > 0) Rose100 else Slate100,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (discountAmount > 0) "% -$currency %.0f".format(discountAmount) else "% Discount",
                                color = if (discountAmount > 0) Rose600 else Navy700,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Search Product & Barcode Area
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        if (it.isNotBlank()) {
                            currentTab = PosTab.CATALOG
                        }
                    },
                    placeholder = { Text("Search product name, category or barcode...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Navy500, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = Navy500, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Navy900,
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pos_search_input")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // POS Tab Switcher (Current Bill vs Quick Catalog)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Cart Tab
                Surface(
                    onClick = { currentTab = PosTab.CART },
                    shape = RoundedCornerShape(8.dp),
                    color = if (currentTab == PosTab.CART) Navy900 else Color.White,
                    border = if (currentTab != PosTab.CART) ButtonDefaults.outlinedButtonBorder else null,
                    modifier = Modifier.weight(1f).testTag("pos_tab_cart")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = if (currentTab == PosTab.CART) Gold500 else Navy700,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Current Bill (${cart.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (currentTab == PosTab.CART) Color.White else Navy800
                        )
                    }
                }

                // Catalog Tab
                Surface(
                    onClick = { currentTab = PosTab.CATALOG },
                    shape = RoundedCornerShape(8.dp),
                    color = if (currentTab == PosTab.CATALOG) Navy900 else Color.White,
                    border = if (currentTab != PosTab.CATALOG) ButtonDefaults.outlinedButtonBorder else null,
                    modifier = Modifier.weight(1f).testTag("pos_tab_catalog")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = if (currentTab == PosTab.CATALOG) Gold500 else Navy700,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Catalog (${products.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (currentTab == PosTab.CATALOG) Color.White else Navy800
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Category Filter Pills (Visible when in Catalog or search)
            if (currentTab == PosTab.CATALOG || searchQuery.isNotBlank()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            modifier = Modifier.testTag("category_chip_$cat")
                        )
                    }
                }
            }

            // MAIN WORKSPACE (CART or CATALOG)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (currentTab == PosTab.CART) {
                    // CURRENT CART / BILL VIEW
                    if (cart.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = ButtonDefaults.outlinedButtonBorder
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(Slate100),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.AddShoppingCart,
                                        contentDescription = null,
                                        tint = Navy500,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Current Bill is Empty",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Navy900
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Search by name, category or barcode to add items to bill.",
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp,
                                    color = Navy500
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { currentTab = PosTab.CATALOG },
                                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Browse Catalog")
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(cart, key = { it.product.id }) { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("cart_item_card_${item.product.id}"),
                                    shape = RoundedCornerShape(10.dp),
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
                                        // Product Details
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.product.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Navy900,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "$currency %.2f / %s".format(item.unitPrice, item.product.unit),
                                                    fontSize = 11.sp,
                                                    color = Navy600,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                if (item.product.category.isNotBlank()) {
                                                    Surface(color = Slate100, shape = RoundedCornerShape(4.dp)) {
                                                        Text(
                                                            text = item.product.category,
                                                            fontSize = 9.sp,
                                                            color = Navy500,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "Total: $currency %.2f".format(item.totalPrice),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Emerald700
                                            )
                                        }

                                        // Interactive Stepper Quantity Controls
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Surface(
                                                onClick = {
                                                    viewModel.updateCartItemQuantity(item.product.id, item.quantity - 1)
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                color = Slate100,
                                                modifier = Modifier.size(32.dp).testTag("dec_cart_${item.product.id}")
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(16.dp), tint = Navy800)
                                                }
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = Slate50,
                                                border = ButtonDefaults.outlinedButtonBorder,
                                                modifier = Modifier.height(32.dp).widthIn(min = 36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
                                                    Text(
                                                        text = if (item.quantity % 1.0 == 0.0) "%.0f".format(item.quantity) else "%.1f".format(item.quantity),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = Navy900
                                                    )
                                                }
                                            }

                                            Surface(
                                                onClick = {
                                                    viewModel.updateCartItemQuantity(item.product.id, item.quantity + 1)
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                color = Navy900,
                                                modifier = Modifier.size(32.dp).testTag("inc_cart_${item.product.id}")
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(16.dp), tint = Color.White)
                                                }
                                            }

                                            IconButton(
                                                onClick = { viewModel.removeFromCart(item.product.id) },
                                                modifier = Modifier.size(28.dp).testTag("remove_cart_${item.product.id}")
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Rose600, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // QUICK CATALOG GRID VIEW
                    if (filteredProducts.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No products found matching '$searchQuery'",
                                fontSize = 13.sp,
                                color = Navy500
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 160.dp),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredProducts, key = { it.id }) { product ->
                                val inCartItem = cart.find { it.product.id == product.id }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.addToCart(product)
                                            successToast = "+1 '${product.name}'"
                                        }
                                        .testTag("product_card_${product.id}"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (inCartItem != null) Blue50 else Color.White
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    border = if (inCartItem != null) ButtonDefaults.outlinedButtonBorder else null
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                color = Slate100,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = product.category.ifBlank { "General" },
                                                    fontSize = 9.sp,
                                                    color = Navy600,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }

                                            if (inCartItem != null) {
                                                Surface(
                                                    color = Emerald100,
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text(
                                                        text = "x%.0f in cart".format(inCartItem.quantity),
                                                        fontSize = 9.sp,
                                                        color = Emerald700,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Text(
                                            text = product.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Navy900,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "$currency %.2f".format(product.salePrice),
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 14.sp,
                                                    color = Emerald700
                                                )
                                                Text(
                                                    text = "Stock: %.0f %s".format(product.stockQuantity, product.unit),
                                                    fontSize = 10.sp,
                                                    color = if (product.stockQuantity <= product.minStockAlert) Rose600 else Navy500
                                                )
                                            }

                                            FilledIconButton(
                                                onClick = {
                                                    viewModel.addToCart(product)
                                                    successToast = "+1 '${product.name}'"
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                colors = IconButtonDefaults.filledIconButtonColors(
                                                    containerColor = Navy900,
                                                    contentColor = Color.White
                                                ),
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
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
}
