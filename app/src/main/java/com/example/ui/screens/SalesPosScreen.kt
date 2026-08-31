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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.example.ui.components.CameraBarcodeScannerView
import com.example.ui.components.LiveClockHeaderWidget
import com.example.ui.components.ShopLogoAvatar
import com.example.ui.invoice.InvoiceReceiptDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.CartItem
import com.example.ui.viewmodel.HeldCart
import com.example.ui.viewmodel.StoreViewModel

enum class PosTab {
    CATALOG,
    CART
}

enum class PosViewMode {
    GRID,
    LIST
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesPosScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit
) {
    val products by viewModel.products.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val discountAmount by viewModel.discountAmount.collectAsState()
    val selectedCustomer by viewModel.selectedCustomer.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val heldCarts by viewModel.heldCarts.collectAsState()
    val paymentType by viewModel.paymentType.collectAsState()
    val receivedAmount by viewModel.receivedAmount.collectAsState()
    val activeCashierName by viewModel.activeCashierName.collectAsState()
    val users by viewModel.users.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()

    val subtotal = remember(cart) { cart.sumOf { it.totalPrice } }
    val netAmount = remember(subtotal, discountAmount) { (subtotal - discountAmount).coerceAtLeast(0.0) }

    val currency = storeSettings?.currencySymbol ?: "Rs"
    val storeDisplayName = storeSettings?.storeName?.ifBlank { "SENTRY STORE" } ?: "SENTRY STORE"
    val branchDisplayName = "Main Branch"

    val isCameraScannerEnabled by viewModel.cameraScannerEnabled.collectAsState()

    // Default view is CATALOG (Product List First as per required POS flow)
    var currentTab by remember { mutableStateOf(PosTab.CATALOG) }
    var viewMode by remember { mutableStateOf(PosViewMode.GRID) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }

    // Dialog States
    var showCameraScannerDialog by remember { mutableStateOf(false) }
    var showCustomerDialog by remember { mutableStateOf(false) }
    var showCashierSelectorDialog by remember { mutableStateOf(false) }
    var showHeldCartsDialog by remember { mutableStateOf(false) }
    var showCustomItemDialog by remember { mutableStateOf(false) }
    var showDiscountDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var editingCartItem by remember { mutableStateOf<CartItem?>(null) }

    // Sequential Checkout & Payment Screen Dialog (Only displayed after user clicks Proceed to Checkout)
    var showCheckoutDialog by remember { mutableStateOf(false) }

    // Invoice Receipt Dialog
    var showReceiptDialog by remember { mutableStateOf(false) }
    var lastSale by remember { mutableStateOf<Sale?>(null) }
    var lastSaleItems by remember { mutableStateOf<List<SaleItem>>(emptyList()) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successToast by remember { mutableStateOf<String?>(null) }

    // Extract dynamic categories from existing products
    val categories = remember(products) {
        val list = mutableListOf("All")
        list.addAll(products.map { it.category }.filter { it.isNotBlank() }.distinct().sorted())
        list
    }

    // Filter products based on search query (Manual Barcode, Name, Category, Description)
    val filteredProducts = remember(products, searchQuery, selectedCategory) {
        products.filter { p ->
            val matchesCategory = selectedCategory == "All" || p.category.equals(selectedCategory, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    p.barcode.contains(searchQuery, ignoreCase = true) ||
                    p.category.contains(searchQuery, ignoreCase = true) ||
                    p.description.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    val totalUnits = remember(cart) { cart.sumOf { it.quantity } }

    // ----------------------------------------------------
    // CAMERA BARCODE SCANNER MODAL
    // ----------------------------------------------------
    if (showCameraScannerDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showCameraScannerDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                CameraBarcodeScannerView(
                    onBarcodeScanned = { scannedCode ->
                        val trimmed = scannedCode.trim()
                        val foundProduct = products.find {
                            it.barcode.trim().equals(trimmed, ignoreCase = true) ||
                            it.name.trim().equals(trimmed, ignoreCase = true)
                        }
                        if (foundProduct != null) {
                            viewModel.addToCart(foundProduct)
                            successToast = "Scanned & Added: ${foundProduct.name} ($trimmed)"
                        } else {
                            errorMessage = "Barcode '$trimmed' not found in store catalog"
                        }
                        showCameraScannerDialog = false
                    },
                    onClose = { showCameraScannerDialog = false },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // ----------------------------------------------------
    // INVOICE RECEIPT DIALOG (Thermal / PDF)
    // ----------------------------------------------------
    if (showReceiptDialog && lastSale != null) {
        InvoiceReceiptDialog(
            sale = lastSale!!,
            items = lastSaleItems,
            settings = storeSettings,
            onDismiss = {
                showReceiptDialog = false
                lastSale = null
                lastSaleItems = emptyList()
            }
        )
    }

    // ----------------------------------------------------
    // CUSTOMER SELECTOR DIALOG
    // ----------------------------------------------------
    if (showCustomerDialog) {
        var custSearch by remember { mutableStateOf("") }
        var showNewCustForm by remember { mutableStateOf(false) }
        var newName by remember { mutableStateOf("") }
        var newPhone by remember { mutableStateOf("") }
        var newAddress by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCustomerDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Customer / Account", fontWeight = FontWeight.Bold, color = Navy900, fontSize = 16.sp)
                    IconButton(onClick = { showCustomerDialog = false }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Navy500)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!showNewCustForm) {
                        OutlinedTextField(
                            value = custSearch,
                            onValueChange = { custSearch = it },
                            placeholder = { Text("Search customer by name or phone...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Navy500) },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("customer_search_input")
                        )

                        // Walk-in Customer Selection
                        Surface(
                            onClick = {
                                viewModel.setSelectedCustomer(null)
                                showCustomerDialog = false
                            },
                            color = if (selectedCustomer == null) Blue50 else Slate50,
                            shape = RoundedCornerShape(8.dp),
                            border = if (selectedCustomer == null) ButtonDefaults.outlinedButtonBorder else null,
                            modifier = Modifier.fillMaxWidth().testTag("select_walkin_customer")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.PersonOutline, contentDescription = null, tint = Navy700)
                                Column {
                                    Text("Walk-in Customer", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Navy900)
                                    Text("Cash Sale • No ledger balance", fontSize = 11.sp, color = Navy500)
                                }
                            }
                        }

                        // Customer List
                        val filteredCusts = customers.filter {
                            custSearch.isBlank() ||
                                    it.name.contains(custSearch, ignoreCase = true) ||
                                    it.phone.contains(custSearch, ignoreCase = true)
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredCusts, key = { it.id }) { cust ->
                                val isSelected = selectedCustomer?.id == cust.id
                                Surface(
                                    onClick = {
                                        viewModel.setSelectedCustomer(cust)
                                        showCustomerDialog = false
                                    },
                                    color = if (isSelected) Blue50 else Color.White,
                                    shape = RoundedCornerShape(8.dp),
                                    border = ButtonDefaults.outlinedButtonBorder,
                                    modifier = Modifier.fillMaxWidth().testTag("cust_item_${cust.id}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = Navy900)
                                            if (cust.phone.isNotBlank()) {
                                                Text(cust.phone, fontSize = 11.sp, color = Navy500)
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Bal: $currency %.0f".format(cust.balance),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (cust.balance > 0) Rose600 else Emerald700
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { showNewCustForm = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Quick Add New Customer")
                        }
                    } else {
                        // Add Customer Form
                        Text("Add New Customer", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Navy900)
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("Full Name *") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newPhone,
                            onValueChange = { newPhone = it },
                            label = { Text("Phone Number") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newAddress,
                            onValueChange = { newAddress = it },
                            label = { Text("Address / City") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showNewCustForm = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Back")
                            }
                            Button(
                                onClick = {
                                    if (newName.isNotBlank()) {
                                        viewModel.quickAddCustomer(
                                            name = newName,
                                            phone = newPhone,
                                            address = newAddress,
                                            onSuccess = {
                                                showNewCustForm = false
                                                showCustomerDialog = false
                                                successToast = "Customer '${it.name}' added & selected"
                                            }
                                        )
                                    }
                                },
                                enabled = newName.isNotBlank(),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Save")
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // ----------------------------------------------------
    // CASHIER SELECTOR DIALOG
    // ----------------------------------------------------
    if (showCashierSelectorDialog) {
        val activeStaff = users.filter { it.isActive }

        AlertDialog(
            onDismissRequest = { showCashierSelectorDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Active Cashier", fontWeight = FontWeight.Bold, color = Navy900, fontSize = 16.sp)
                    IconButton(onClick = { showCashierSelectorDialog = false }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Navy500)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Current Active Cashier: $activeCashierName",
                        fontSize = 12.sp,
                        color = Navy600,
                        fontWeight = FontWeight.Medium
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(activeStaff, key = { it.id }) { cashier ->
                            val isCurrent = activeCashierName.equals(cashier.fullName, ignoreCase = true) ||
                                    activeCashierName.equals(cashier.username, ignoreCase = true)
                            Surface(
                                onClick = {
                                    viewModel.setActiveCashier(cashier.fullName.ifBlank { cashier.username })
                                    showCashierSelectorDialog = false
                                    successToast = "Active cashier set to ${cashier.fullName}"
                                },
                                color = if (isCurrent) Emerald50 else Color.White,
                                shape = RoundedCornerShape(8.dp),
                                border = ButtonDefaults.outlinedButtonBorder,
                                modifier = Modifier.fillMaxWidth().testTag("select_cashier_${cashier.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(if (isCurrent) Emerald600 else Navy900),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = cashier.fullName.take(1).uppercase().ifBlank { "C" },
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Column {
                                            Text(cashier.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Navy900)
                                            Text(cashier.role, fontSize = 11.sp, color = Navy500)
                                        }
                                    }

                                    if (isCurrent) {
                                        Surface(color = Emerald600, shape = RoundedCornerShape(4.dp)) {
                                            Text("ACTIVE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCashierSelectorDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ----------------------------------------------------
    // CUSTOM LINE ITEM DIALOG
    // ----------------------------------------------------
    if (showCustomItemDialog) {
        var customName by remember { mutableStateOf("") }
        var customPrice by remember { mutableStateOf("") }
        var customQty by remember { mutableStateOf("1") }
        var customUnit by remember { mutableStateOf("Unit") }

        AlertDialog(
            onDismissRequest = { showCustomItemDialog = false },
            title = { Text("Add Custom Line Item", fontWeight = FontWeight.Bold, color = Navy900, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Item Name / Description *") },
                        placeholder = { Text("e.g. Labor Charge, Custom Mix") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("custom_item_name_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = customPrice,
                            onValueChange = { customPrice = it },
                            label = { Text("Price ($currency) *") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.2f).testTag("custom_item_price_input")
                        )
                        OutlinedTextField(
                            value = customQty,
                            onValueChange = { customQty = it },
                            label = { Text("Quantity") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(0.8f).testTag("custom_item_qty_input")
                        )
                    }

                    OutlinedTextField(
                        value = customUnit,
                        onValueChange = { customUnit = it },
                        label = { Text("Unit") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = customPrice.toDoubleOrNull() ?: 0.0
                        val q = customQty.toDoubleOrNull() ?: 1.0
                        if (customName.isNotBlank() && p > 0) {
                            viewModel.addCustomItemToCart(
                                name = customName,
                                price = p,
                                quantity = q,
                                unit = customUnit.ifBlank { "Unit" }
                            )
                            showCustomItemDialog = false
                            currentTab = PosTab.CART
                            successToast = "Custom item added to bill"
                        }
                    },
                    enabled = customName.isNotBlank() && (customPrice.toDoubleOrNull() ?: 0.0) > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("save_custom_item_button")
                ) {
                    Text("Add to Bill")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomItemDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ----------------------------------------------------
    // EDIT CART ITEM DIALOG
    // ----------------------------------------------------
    editingCartItem?.let { item ->
        var editQty by remember { mutableStateOf(item.quantity.toString()) }
        var editPrice by remember { mutableStateOf(item.unitPrice.toString()) }
        var editDisc by remember { mutableStateOf(if (item.itemDiscount > 0) item.itemDiscount.toString() else "") }
        var editVariation by remember { mutableStateOf(item.customVariation) }

        AlertDialog(
            onDismissRequest = { editingCartItem = null },
            title = {
                Column {
                    Text("Edit Line Item", fontWeight = FontWeight.Bold, color = Navy900, fontSize = 16.sp)
                    Text(item.product.name, fontSize = 12.sp, color = Navy600, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Original Price: $currency %.2f / %s".format(item.product.salePrice, item.product.unit), fontSize = 12.sp, color = Navy600)

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = editQty,
                            onValueChange = { editQty = it },
                            label = { Text("Quantity (${item.product.unit})") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("edit_item_qty_input")
                        )
                        OutlinedTextField(
                            value = editPrice,
                            onValueChange = { editPrice = it },
                            label = { Text("Unit Price ($currency)") },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("edit_item_price_input")
                        )
                    }

                    OutlinedTextField(
                        value = editDisc,
                        onValueChange = { editDisc = it },
                        label = { Text("Line Item Discount ($currency)") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editVariation,
                        onValueChange = { editVariation = it },
                        label = { Text("Variation / Color Code / Batch") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val q = editQty.toDoubleOrNull() ?: item.quantity
                        val p = editPrice.toDoubleOrNull() ?: item.unitPrice
                        val d = editDisc.toDoubleOrNull() ?: 0.0
                        viewModel.updateCartItemFull(
                            productId = item.product.id,
                            quantity = q,
                            unitPrice = p,
                            itemDiscount = d,
                            customVariation = editVariation
                        )
                        editingCartItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingCartItem = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // ----------------------------------------------------
    // HELD CARTS DIALOG
    // ----------------------------------------------------
    if (showHeldCartsDialog) {
        AlertDialog(
            onDismissRequest = { showHeldCartsDialog = false },
            title = {
                Text("Held POS Sales (${heldCarts.size})", fontWeight = FontWeight.Bold, color = Navy900, fontSize = 16.sp)
            },
            text = {
                if (heldCarts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No held sales in temporary memory.", color = Navy500, fontSize = 13.sp)
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

    // ----------------------------------------------------
    // GLOBAL DISCOUNT DIALOG
    // ----------------------------------------------------
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
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("discount_modal_input")
                    )
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

    // ----------------------------------------------------
    // CLEAR CART CONFIRMATION DIALOG
    // ----------------------------------------------------
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

    // ----------------------------------------------------
    // PAYMENT SCREEN / CHECKOUT DIALOG (ONLY AFTER "PROCEED TO CHECKOUT")
    // ----------------------------------------------------
    if (showCheckoutDialog) {
        var recInput by remember {
            mutableStateOf(if (paymentType == "Credit") "0.00" else "%.2f".format(if (receivedAmount > 0) receivedAmount else netAmount))
        }

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
                        Text("Payment & Complete Sale", fontWeight = FontWeight.Bold, color = Navy900, fontSize = 17.sp)
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

                    // Cashier Info Row
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
                    Text("Select Payment Method:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Navy800)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("Cash", "Card", "Bank", "Credit").forEach { mode ->
                            FilterChip(
                                selected = paymentType == mode,
                                onClick = {
                                    viewModel.setPaymentType(mode)
                                    if (mode == "Credit") {
                                        recInput = "0.00"
                                        viewModel.setReceivedAmount(0.0)
                                    } else if (recVal == 0.0) {
                                        recInput = "%.2f".format(netAmount)
                                        viewModel.setReceivedAmount(netAmount)
                                    }
                                },
                                label = {
                                    Text(
                                        text = when(mode) {
                                            "Cash" -> "💵 Cash"
                                            "Card" -> "💳 Card"
                                            "Bank" -> "🏦 Transfer"
                                            else -> "📒 Credit"
                                        },
                                        fontSize = 11.sp
                                    )
                                },
                                modifier = Modifier.weight(1f).testTag("payment_chip_$mode")
                            )
                        }
                    }

                    // Amount Received / Tendered Input
                    OutlinedTextField(
                        value = recInput,
                        onValueChange = {
                            recInput = it
                            viewModel.setReceivedAmount(it.toDoubleOrNull() ?: 0.0)
                        },
                        label = { Text("Amount Received ($currency)") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("received_amount_input")
                    )

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
                                successToast = "Sale completed! Invoice #${sale.invoiceNumber}"
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
                    Text("Complete Sale ($currency %.2f)".format(netAmount), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCheckoutDialog = false }) {
                    Text("Back to Cart")
                }
            }
        )
    }

    // ----------------------------------------------------
    // MAIN POS SCREEN SCAFFOLD
    // ----------------------------------------------------
    Scaffold(
        topBar = {
            // PROFESSIONAL SENTRY STORE POS HEADER
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
                        // Left: Back button + Store Logo + Store Name & Cashier
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1.3f)
                        ) {
                            IconButton(
                                onClick = onNavigateBack,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }

                            ShopLogoAvatar(
                                logoUri = storeSettings?.logoUri,
                                size = 36.dp
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
                                        text = "$branchDisplayName | Cashier: $activeCashierName",
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

                        // Right: Live Clock & Offline Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LiveClockHeaderWidget(showSeconds = true)

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
            // PERSISTENT BOTTOM BAR ON PRODUCT LIST / CATALOG SCREEN
            if (currentTab == PosTab.CATALOG) {
                Surface(
                    color = Navy900,
                    shadowElevation = 12.dp,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (cart.isNotEmpty()) Emerald600 else Slate700),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ShoppingCart,
                                    contentDescription = "Cart",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = if (cart.isNotEmpty()) "${cart.size} Items (%.0f Units)".format(totalUnits) else "0 Items (Cart Empty)",
                                    fontSize = 12.sp,
                                    color = if (cart.isNotEmpty()) Slate200 else Slate400,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "$currency %.2f".format(netAmount),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = if (cart.isNotEmpty()) Gold400 else Slate300
                                )
                            }
                        }

                        Button(
                            onClick = { currentTab = PosTab.CART },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Emerald600,
                                disabledContainerColor = Slate700
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            modifier = Modifier
                                .testTag("btn_view_cart_bottom_bar")
                                .testTag("view_cart_button")
                        ) {
                            Text("VIEW CART", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            } else if (currentTab == PosTab.CART) {
                // Fixed Bottom Cart / Bill Summary and Sequential Checkout Action
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
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Summary Strip: Subtotal, Discount & Grand Total
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Total Amount:", fontSize = 12.sp, color = Navy500)
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

                            // Action Buttons: Hold, Clear, and Proceed to Checkout
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

                                // Primary "Proceed to Checkout ->" Action (Strict sequential checkout)
                                Button(
                                    onClick = {
                                        viewModel.setReceivedAmount(netAmount)
                                        showCheckoutDialog = true
                                    },
                                    enabled = cart.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Emerald600,
                                        disabledContainerColor = Slate200
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                    modifier = Modifier
                                        .testTag("proceed_to_checkout_button")
                                        .testTag("checkout_button")
                                ) {
                                    Text(
                                        text = "Proceed to Checkout",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
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
            // Notification Banners
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
                                text = if (selectedCustomer != null) "Balance: $currency %.0f • Tap to change".format(selectedCustomer!!.balance) else "Tap to select customer or contractor",
                                fontSize = 10.sp,
                                color = Navy500
                            )
                        }
                    }

                    // Quick Discount & Held Badge & Custom Item
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Camera Scanner Button
                        Surface(
                            onClick = { showCameraScannerDialog = true },
                            color = Emerald50,
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Emerald300),
                            modifier = Modifier.testTag("btn_camera_scanner")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = "Camera Scanner", tint = Emerald700, modifier = Modifier.size(13.dp))
                                Text(
                                    text = "Scan",
                                    color = Emerald800,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

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

                        Surface(
                            onClick = { showCustomItemDialog = true },
                            color = Slate100,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "+ Custom",
                                color = Navy700,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // POS Top Navigation Tabs (Product Catalog / List First vs Current Bill)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Catalog Tab (Default first view)
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
                            text = "Product Catalog (${products.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (currentTab == PosTab.CATALOG) Color.White else Navy800
                        )
                    }
                }

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
            }

            Spacer(modifier = Modifier.height(6.dp))

            // WORKSPACE CONTENT
            if (currentTab == PosTab.CATALOG) {
                // PRODUCT CATALOG VIEW (SEARCH + FILTER + PRODUCT LIST/GRID)
                // Manual Barcode & Name Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search product name, barcode, category...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Navy500, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Navy500, modifier = Modifier.size(16.dp))
                                }
                            }
                            IconButton(
                                onClick = { showCameraScannerDialog = true },
                                modifier = Modifier.size(36.dp).testTag("camera_scan_button")
                            ) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = "Camera Barcode Scanner",
                                    tint = Emerald600,
                                    modifier = Modifier.size(20.dp)
                                )
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

                Spacer(modifier = Modifier.height(6.dp))

                // Category Filter Pills & Grid/List toggle
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
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

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(
                            onClick = { viewMode = PosViewMode.GRID },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.GridView,
                                contentDescription = "Grid",
                                tint = if (viewMode == PosViewMode.GRID) Navy900 else Slate400,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewMode = PosViewMode.LIST },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.ViewList,
                                contentDescription = "List",
                                tint = if (viewMode == PosViewMode.LIST) Navy900 else Slate400,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // PRODUCT LIST / GRID
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    if (filteredProducts.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (searchQuery.isBlank()) "No products in store." else "No products found matching '$searchQuery'",
                                    fontSize = 13.sp,
                                    color = Navy500
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { showCustomItemDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Navy900)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add as Custom Item")
                                }
                            }
                        }
                    } else if (viewMode == PosViewMode.GRID) {
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
                                                        text = "x%.0f in bill".format(inCartItem.quantity),
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

                                        if (product.barcode.isNotBlank()) {
                                            Text(
                                                text = "Code: ${product.barcode}",
                                                fontSize = 10.sp,
                                                color = Navy500,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

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

                                            Button(
                                                onClick = {
                                                    viewModel.addToCart(product)
                                                    successToast = "+1 '${product.name}'"
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Navy900,
                                                    contentColor = Color.White
                                                ),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier
                                                    .height(32.dp)
                                                    .testTag("add_to_cart_${product.id}")
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text("ADD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // LIST VIEW
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredProducts, key = { it.id }) { product ->
                                val inCartItem = cart.find { it.product.id == product.id }
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.addToCart(product)
                                            successToast = "+1 '${product.name}'"
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (inCartItem != null) Blue50 else Color.White
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(product.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Navy900)
                                            Text(
                                                text = "${product.category} • $currency %.2f / %s • Stock: %.0f".format(product.salePrice, product.unit, product.stockQuantity),
                                                fontSize = 11.sp,
                                                color = Navy600
                                            )
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (inCartItem != null) {
                                                Surface(
                                                    color = Emerald100,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text("x%.0f".format(inCartItem.quantity), color = Emerald700, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                }
                                            }
                                            Button(
                                                onClick = {
                                                    viewModel.addToCart(product)
                                                    successToast = "+1 '${product.name}'"
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier
                                                    .height(32.dp)
                                                    .testTag("add_to_cart_${product.id}")
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text("ADD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // CURRENT BILL / CART VIEW
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
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
                                    text = "Add products from the catalog or search by name / barcode.",
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp,
                                    color = Navy500
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { currentTab = PosTab.CATALOG },
                                        colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Browse Product Catalog")
                                    }
                                    OutlinedButton(
                                        onClick = { showCustomItemDialog = true },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Custom Item")
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(cart, key = { "${it.product.id}_${it.customVariation}" }) { item ->
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
                                        // Product Details with editable tap
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { editingCartItem = item }
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(
                                                    text = item.product.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Navy900,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Navy400, modifier = Modifier.size(12.dp))
                                            }
                                            if (item.customVariation.isNotBlank()) {
                                                Text(
                                                    text = "Variation: ${item.customVariation}",
                                                    fontSize = 11.sp,
                                                    color = Gold700,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
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
                                                if (item.itemDiscount > 0) {
                                                    Surface(color = Rose100, shape = RoundedCornerShape(4.dp)) {
                                                        Text(
                                                            text = "-$currency %.0f".format(item.itemDiscount),
                                                            fontSize = 9.sp,
                                                            color = Rose600,
                                                            fontWeight = FontWeight.Bold,
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

                                        // Stepper Quantity Controls
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
                                                onClick = { editingCartItem = item },
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
                }
            }
        }
    }
}
