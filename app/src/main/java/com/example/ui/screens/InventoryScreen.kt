package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Product
import com.example.data.entity.StockMovement
import com.example.ui.components.AppHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel
import java.text.SimpleDateFormat
import java.util.*

enum class InventoryTab {
    ALL_PRODUCTS,
    STOCK_ADJUSTMENT,
    MOVEMENT_HISTORY,
    LOW_STOCK_EXPIRY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToBarcodeLabels: () -> Unit = {}
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsState()
    val stockMovements by viewModel.stockMovements.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val currency = storeSettings?.currencySymbol ?: "Rs"

    var selectedTab by remember { mutableStateOf(InventoryTab.ALL_PRODUCTS) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddEditDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var adjustingProduct by remember { mutableStateOf<Product?>(null) }

    val filteredProducts = remember(products, searchQuery) {
        if (searchQuery.isBlank()) products
        else {
            products.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.barcode.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true) ||
                it.batchNumber.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val lowStockProducts = remember(products) {
        products.filter { it.stockQuantity <= it.minStockAlert }
    }

    val nowMillis = System.currentTimeMillis()
    val thirtyDaysMillis = 30L * 24 * 3600 * 1000
    val expiringProducts = remember(products) {
        products.filter { it.expiryDate != null && it.expiryDate > 0 }
    }

    // Add / Edit Product Dialog
    if (showAddEditDialog) {
        var name by remember { mutableStateOf(editingProduct?.name ?: "") }
        var category by remember { mutableStateOf(editingProduct?.category ?: "General") }
        var barcode by remember { mutableStateOf(editingProduct?.barcode ?: "") }
        var purchasePrice by remember { mutableStateOf(editingProduct?.purchasePrice?.toString() ?: "") }
        var salePrice by remember { mutableStateOf(editingProduct?.salePrice?.toString() ?: "") }
        var stockQuantity by remember { mutableStateOf(editingProduct?.stockQuantity?.toString() ?: "") }
        var unit by remember { mutableStateOf(editingProduct?.unit ?: "Pcs") }
        var minStockAlert by remember { mutableStateOf(editingProduct?.minStockAlert?.toString() ?: "5") }
        var batchNumber by remember { mutableStateOf(editingProduct?.batchNumber ?: "") }
        var secondaryUnit by remember { mutableStateOf(editingProduct?.secondaryUnit ?: "") }
        var unitConversionRate by remember { mutableStateOf(editingProduct?.unitConversionRate?.toString() ?: "1.0") }
        var expiryDateStr by remember {
            mutableStateOf(
                if (editingProduct?.expiryDate != null && editingProduct!!.expiryDate!! > 0) {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(editingProduct!!.expiryDate!!))
                } else ""
            )
        }
        var isTaxExempt by remember { mutableStateOf(editingProduct?.isTaxExempt ?: false) }
        var customTaxRate by remember { mutableStateOf(editingProduct?.customTaxRate?.toString() ?: "0.0") }

        AlertDialog(
            onDismissRequest = { showAddEditDialog = false },
            title = {
                Text(
                    text = if (editingProduct == null) "Add New Product" else "Edit Product",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Product Name *") },
                        modifier = Modifier.fillMaxWidth().testTag("product_name_input")
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Category") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = barcode,
                            onValueChange = { barcode = it },
                            label = { Text("Barcode / SKU") },
                            modifier = Modifier.weight(1f).testTag("product_barcode_input")
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = purchasePrice,
                            onValueChange = { purchasePrice = it },
                            label = { Text("Cost Price") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = salePrice,
                            onValueChange = { salePrice = it },
                            label = { Text("Sale Price *") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f).testTag("product_sale_price_input")
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = stockQuantity,
                            onValueChange = { stockQuantity = it },
                            label = { Text("Stock Quantity") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Primary Unit (Pcs/Kg)") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = minStockAlert,
                            onValueChange = { minStockAlert = it },
                            label = { Text("Min Stock Alert") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = batchNumber,
                            onValueChange = { batchNumber = it },
                            label = { Text("Batch / Lot #") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Multi-Unit Conversions
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = secondaryUnit,
                            onValueChange = { secondaryUnit = it },
                            label = { Text("Secondary Unit (Box/Pack)") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = unitConversionRate,
                            onValueChange = { unitConversionRate = it },
                            label = { Text("Units per Box") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Expiry Date
                    OutlinedTextField(
                        value = expiryDateStr,
                        onValueChange = { expiryDateStr = it },
                        label = { Text("Expiry Date (YYYY-MM-DD)") },
                        placeholder = { Text("e.g. 2026-12-31") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Tax Exemption Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tax Exempt Product", fontSize = 13.sp, color = Navy900)
                        Switch(checked = isTaxExempt, onCheckedChange = { isTaxExempt = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            var parsedExpiry: Long? = null
                            if (expiryDateStr.isNotBlank()) {
                                try {
                                    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(expiryDateStr.trim())
                                    parsedExpiry = date?.time
                                } catch (_: Exception) {}
                            }

                            val p = (editingProduct ?: Product()).copy(
                                name = name.trim(),
                                category = category.trim().ifBlank { "General" },
                                barcode = barcode.trim(),
                                purchasePrice = purchasePrice.toDoubleOrNull() ?: 0.0,
                                salePrice = salePrice.toDoubleOrNull() ?: 0.0,
                                stockQuantity = stockQuantity.toDoubleOrNull() ?: 0.0,
                                unit = unit.trim().ifBlank { "Pcs" },
                                minStockAlert = minStockAlert.toDoubleOrNull() ?: 5.0,
                                batchNumber = batchNumber.trim(),
                                secondaryUnit = secondaryUnit.trim(),
                                unitConversionRate = unitConversionRate.toDoubleOrNull() ?: 1.0,
                                expiryDate = parsedExpiry,
                                isTaxExempt = isTaxExempt,
                                customTaxRate = customTaxRate.toDoubleOrNull() ?: 0.0
                            )
                            viewModel.saveProduct(p) {
                                showAddEditDialog = false
                                editingProduct = null
                                Toast.makeText(context, "Product saved successfully", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    modifier = Modifier.testTag("save_product_button")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Stock Audit & Adjustment Dialog
    if (adjustingProduct != null) {
        val prod = adjustingProduct!!
        var newStockText by remember { mutableStateOf(prod.stockQuantity.toString()) }
        var selectedReason by remember { mutableStateOf("RECOUNT") }
        var notesText by remember { mutableStateOf("") }
        val reasons = listOf("RECOUNT", "DAMAGE", "EXPIRED", "LOSS", "SUPPLIER_RETURN", "FOUND")

        AlertDialog(
            onDismissRequest = { adjustingProduct = null },
            title = {
                Text("Stock Adjustment & Audit", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = prod.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Navy900
                    )
                    Text(
                        text = "Current Physical Stock: %.2f %s".format(prod.stockQuantity, prod.unit),
                        fontSize = 12.sp,
                        color = Slate600
                    )

                    HorizontalDivider(color = Slate200)

                    OutlinedTextField(
                        value = newStockText,
                        onValueChange = { newStockText = it },
                        label = { Text("New Counted Stock Quantity *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("adjusted_stock_input")
                    )

                    Text("Adjustment Reason", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Navy800)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        reasons.chunked(3).forEach { rowReasons ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowReasons.forEach { r ->
                                    FilterChip(
                                        selected = selectedReason == r,
                                        onClick = { selectedReason = r },
                                        label = { Text(r, fontSize = 10.sp) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Audit Notes / Remarks") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedStock = newStockText.toDoubleOrNull()
                        if (parsedStock != null) {
                            viewModel.adjustStock(
                                productId = prod.id,
                                newStock = parsedStock,
                                movementType = selectedReason,
                                reason = notesText.ifBlank { "Manual Audit Adjustment ($selectedReason)" },
                                performedBy = activeUser?.fullName ?: "Store Manager"
                            ) {
                                Toast.makeText(context, "Stock adjusted to $parsedStock ${prod.unit}", Toast.LENGTH_SHORT).show()
                                adjustingProduct = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    modifier = Modifier.testTag("confirm_adjust_stock_button")
                ) {
                    Text("Apply Adjustment")
                }
            },
            dismissButton = {
                TextButton(onClick = { adjustingProduct = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            AppHeader(
                title = "Inventory & Stock Audit",
                subtitle = "${products.size} Products • ${lowStockProducts.size} Low Stock",
                onBackClick = onNavigateBack
            )
        },
        floatingActionButton = {
            if (selectedTab == InventoryTab.ALL_PRODUCTS) {
                FloatingActionButton(
                    onClick = {
                        editingProduct = null
                        showAddEditDialog = true
                    },
                    containerColor = Navy900,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_product_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Product")
                }
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
            // Tab Navigation Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                edgePadding = 0.dp,
                containerColor = Color.White,
                contentColor = Navy900,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == InventoryTab.ALL_PRODUCTS,
                    onClick = { selectedTab = InventoryTab.ALL_PRODUCTS },
                    text = { Text("Products (${products.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == InventoryTab.LOW_STOCK_EXPIRY,
                    onClick = { selectedTab = InventoryTab.LOW_STOCK_EXPIRY },
                    text = { Text("Low / Expiry (${lowStockProducts.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == InventoryTab.MOVEMENT_HISTORY,
                    onClick = { selectedTab = InventoryTab.MOVEMENT_HISTORY },
                    text = { Text("Audit Movements (${stockMovements.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                InventoryTab.ALL_PRODUCTS -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${products.size} Products Registered",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )

                        Button(
                            onClick = onNavigateToBarcodeLabels,
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_inventory_to_barcode_labels")
                        ) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp).padding(end = 4.dp)
                            )
                            Text("Barcode & Labels", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search by name, barcode, SKU, batch...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Navy500)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Navy900,
                            unfocusedContainerColor = Color.White,
                            focusedContainerColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (filteredProducts.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No products found.", color = Navy500)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredProducts, key = { it.id }) { product ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = product.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = Navy900
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Category: ${product.category} • SKU: ${product.barcode.ifBlank { "N/A" }}",
                                                    fontSize = 11.sp,
                                                    color = Navy500
                                                )
                                                if (product.batchNumber.isNotBlank()) {
                                                    Text(
                                                        text = "Batch: ${product.batchNumber}",
                                                        fontSize = 10.sp,
                                                        color = Slate500
                                                    )
                                                }
                                                Text(
                                                    text = "Cost: $currency %.2f  |  Sale: $currency %.2f".format(product.purchasePrice, product.salePrice),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Navy800
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                StatusBadge(
                                                    text = "Stock: %.0f %s".format(product.stockQuantity, product.unit),
                                                    backgroundColor = if (product.stockQuantity <= product.minStockAlert) Rose100 else Emerald100,
                                                    textColor = if (product.stockQuantity <= product.minStockAlert) Rose600 else Emerald600
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        HorizontalDivider(color = Slate100)
                                        Spacer(modifier = Modifier.height(6.dp))

                                        // Action Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Audit / Adjust Stock Button
                                            OutlinedButton(
                                                onClick = { adjustingProduct = product },
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp), tint = Navy800)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Audit / Adjust", fontSize = 11.sp, color = Navy800)
                                            }

                                            Spacer(modifier = Modifier.width(6.dp))

                                            // Edit Button
                                            IconButton(
                                                onClick = {
                                                    editingProduct = product
                                                    showAddEditDialog = true
                                                },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Gold600, modifier = Modifier.size(16.dp))
                                            }

                                            // Delete Button
                                            IconButton(
                                                onClick = { viewModel.softDeleteProduct(product.id) },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Rose600, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                InventoryTab.LOW_STOCK_EXPIRY -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text("Critical Stock & Expiry Alerts", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Navy900)
                        }

                        if (lowStockProducts.isEmpty() && expiringProducts.isEmpty()) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Emerald50
                                ) {
                                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald600)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("All products have healthy stock levels and valid dates.", color = Emerald800, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        items(lowStockProducts, key = { "low_${it.id}" }) { product ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Rose200)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(product.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Navy900)
                                        Text("Current: %.0f %s | Minimum Alert: %.0f".format(product.stockQuantity, product.unit, product.minStockAlert), fontSize = 11.sp, color = Rose600)
                                    }
                                    Button(
                                        onClick = { adjustingProduct = product },
                                        colors = ButtonDefaults.buttonColors(containerColor = Rose600),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("Restock / Audit", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                InventoryTab.MOVEMENT_HISTORY -> {
                    if (stockMovements.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No stock movement history recorded yet.", color = Navy500)
                        }
                    } else {
                        val dateFormat = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(stockMovements, key = { it.id }) { movement ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = movement.productName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Navy900
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                StatusBadge(
                                                    text = movement.movementType,
                                                    backgroundColor = when (movement.movementType) {
                                                        "SALE" -> Navy100
                                                        "PURCHASE" -> Emerald100
                                                        "RETURN" -> Amber100
                                                        "DAMAGE" -> Rose100
                                                        else -> Purple100
                                                    },
                                                    textColor = when (movement.movementType) {
                                                        "SALE" -> Navy800
                                                        "PURCHASE" -> Emerald700
                                                        "RETURN" -> Amber700
                                                        "DAMAGE" -> Rose700
                                                        else -> Purple600
                                                    }
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${movement.reason} • By: ${movement.performedBy}",
                                                fontSize = 11.sp,
                                                color = Slate500
                                            )
                                            Text(
                                                text = dateFormat.format(Date(movement.timestamp)),
                                                fontSize = 10.sp,
                                                color = Slate400
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = if (movement.quantityDelta > 0) "+%.0f".format(movement.quantityDelta) else "%.0f".format(movement.quantityDelta),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 14.sp,
                                                color = if (movement.quantityDelta >= 0) Emerald700 else Rose600
                                            )
                                            Text(
                                                text = "New: %.0f".format(movement.stockAfter),
                                                fontSize = 10.sp,
                                                color = Slate500
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {}
            }
        }
    }
}
