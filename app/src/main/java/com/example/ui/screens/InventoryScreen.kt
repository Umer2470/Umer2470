package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.sp
import com.example.data.entity.Product
import com.example.ui.components.BarCodeScannerDialog
import com.example.ui.components.BatchImportModal
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald600
import com.example.ui.theme.Rose100
import com.example.ui.theme.Rose600
import com.example.ui.viewmodel.StoreViewModel

val STANDARD_HARDWARE_CATEGORIES = listOf(
    "All",
    "Building Materials",
    "Sanitary & Sentry",
    "Paint & Finishes",
    "Tools",
    "Plumbing & Pipes",
    "Hardware & Fittings",
    "Roofing & Insulation",
    "Adhesives & Chemicals",
    "General"
)

val UNITS = listOf("Pcs", "Ft", "Meter", "Bag", "Box", "Set", "Roll", "Coil", "Ltr", "Pack")

@Composable
fun InventoryScreen(viewModel: StoreViewModel) {
    val context = LocalContext.current
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val allProducts by viewModel.allProducts.collectAsState()
    val dbCategories by viewModel.allCategories.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()
    val settings by viewModel.settings.collectAsState()

    // Combine DB categories and product categories with standard hardware categories
    val categoryList = remember(dbCategories, allProducts) {
        val combined = mutableListOf(
            "All",
            "Building Materials",
            "Sanitary & Sentry",
            "Paint & Finishes",
            "Tools",
            "Plumbing & Pipes",
            "Hardware & Fittings",
            "Roofing & Insulation",
            "Adhesives & Chemicals"
        )
        dbCategories.map { it.name }.forEach { if (it.isNotBlank() && !combined.contains(it)) combined.add(it) }
        allProducts.map { it.category }.forEach { if (it.isNotBlank() && !combined.contains(it)) combined.add(it) }
        combined
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var showBarcodeDialog by remember { mutableStateOf(false) }
    var showBatchImportDialog by remember { mutableStateOf(false) }
    var filterLowStockOnly by remember { mutableStateOf(false) }

    // Filter products
    val filteredProducts = allProducts.filter { p ->
        val matchesSearch = p.name.contains(searchQuery, ignoreCase = true) ||
                p.barcode.contains(searchQuery, ignoreCase = true) ||
                p.category.contains(searchQuery, ignoreCase = true)

        val matchesCategory = when {
            selectedCategory == "All" -> true
            p.category.equals(selectedCategory, ignoreCase = true) -> true
            selectedCategory.contains("Building Material", ignoreCase = true) && p.category.contains("Building Material", ignoreCase = true) -> true
            (selectedCategory.contains("Sanitary", ignoreCase = true) || selectedCategory.contains("Sentry", ignoreCase = true)) &&
                    (p.category.contains("Sanitary", ignoreCase = true) || p.category.contains("Sentry", ignoreCase = true)) -> true
            selectedCategory.contains("Paint", ignoreCase = true) && p.category.contains("Paint", ignoreCase = true) -> true
            selectedCategory.contains("Tool", ignoreCase = true) && p.category.contains("Tool", ignoreCase = true) -> true
            selectedCategory.contains("Plumbing", ignoreCase = true) && p.category.contains("Plumbing", ignoreCase = true) -> true
            else -> p.category.contains(selectedCategory, ignoreCase = true)
        }

        val matchesLowStock = !filterLowStockOnly || p.stockQuantity <= p.minStockLevel

        matchesSearch && matchesCategory && matchesLowStock
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    productToEdit = null
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Search & Barcode Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search by item name, barcode...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { showBarcodeDialog = true },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Scan Barcode",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = { showBatchImportDialog = true },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = "Batch Import Shipment",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = { viewModel.exportInventoryReportPdf(context) },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = "Export Inventory PDF Report",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dedicated Product Category Filter Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Category Filter:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (selectedCategory != "All" || filterLowStockOnly) {
                    TextButton(
                        onClick = {
                            viewModel.setSelectedCategory("All")
                            filterLowStockOnly = false
                        }
                    ) {
                        Text("Clear Filters", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Category & Low Stock Filter Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    FilterChip(
                        selected = filterLowStockOnly,
                        onClick = { filterLowStockOnly = !filterLowStockOnly },
                        label = { Text("⚠️ Low Stock (${lowStockProducts.size})") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (filterLowStockOnly) Color.White else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }

                items(categoryList) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { viewModel.setSelectedCategory(cat) },
                        label = { Text(cat) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats Subheader
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Inventory List (${filteredProducts.size} items)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Total Stock Value: ${settings.currencySymbol} ${allProducts.sumOf { it.stockQuantity * it.purchasePrice }.toInt()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Product List
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No products found matching criteria.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredProducts) { product ->
                        ProductItemCard(
                            product = product,
                            currencySymbol = settings.currencySymbol,
                            onEdit = {
                                productToEdit = product
                                showAddDialog = true
                            },
                            onDelete = {
                                productToDelete = product
                            },
                            onStockUpdate = { change ->
                                viewModel.saveProduct(product.copy(stockQuantity = (product.stockQuantity + change).coerceAtLeast(0.0)))
                            }
                        )
                    }
                }
            }
        }
    }

    // Barcode scanner modal
    if (showBarcodeDialog) {
        BarCodeScannerDialog(
            productsList = allProducts,
            onBarcodeScanned = { code ->
                viewModel.setSearchQuery(code)
                showBarcodeDialog = false
            },
            onDismiss = { showBarcodeDialog = false }
        )
    }

    // Batch Import Modal
    if (showBatchImportDialog) {
        BatchImportModal(
            viewModel = viewModel,
            onDismiss = { showBatchImportDialog = false }
        )
    }

    // Add or Edit Dialog
    if (showAddDialog) {
        AddEditProductDialog(
            product = productToEdit,
            categories = categoryList,
            onSave = { product ->
                viewModel.saveProduct(product)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // Delete Confirmation
    if (productToDelete != null) {
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text("Delete Product") },
            text = { Text("Are you sure you want to delete '${productToDelete!!.name}' from inventory?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(productToDelete!!)
                        productToDelete = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ProductItemCard(
    product: Product,
    currencySymbol: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onStockUpdate: (Double) -> Unit
) {
    val isLowStock = product.stockQuantity <= product.minStockLevel
    val margin = product.salePrice - product.purchasePrice

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${product.category} • Barcode: ${if (product.barcode.isBlank()) "N/A" else product.barcode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                // Stock status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isLowStock) Rose100 else Emerald100)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isLowStock) "LOW STOCK (${product.stockQuantity.toInt()} ${product.unit})" else "Stock: ${product.stockQuantity.toInt()} ${product.unit}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isLowStock) Rose600 else Emerald600
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pricing details & Margin
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Sale Price: $currencySymbol ${product.salePrice.toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Cost Price: $currencySymbol ${product.purchasePrice.toInt()} | Margin: $currencySymbol ${margin.toInt()}", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                }

                // Quick Stock adjustment buttons & Edit/Delete
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onStockUpdate(-1.0) },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text("-", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${product.stockQuantity.toInt()}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = { onStockUpdate(1.0) },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Text("+", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Rose600)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductDialog(
    product: Product?,
    categories: List<String> = STANDARD_HARDWARE_CATEGORIES,
    onSave: (Product) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var barcode by remember { mutableStateOf(product?.barcode ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "General") }
    var purchasePrice by remember { mutableStateOf(product?.purchasePrice?.toInt()?.toString() ?: "") }
    var salePrice by remember { mutableStateOf(product?.salePrice?.toInt()?.toString() ?: "") }
    var stockQuantity by remember { mutableStateOf(product?.stockQuantity?.toInt()?.toString() ?: "10") }
    var minStockLevel by remember { mutableStateOf(product?.minStockLevel?.toInt()?.toString() ?: "5") }
    var unit by remember { mutableStateOf(product?.unit ?: "Pcs") }

    var categoryExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (product == null) "Add Hardware / Sanitary Item" else "Edit Product Details")
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (errorMsg != null) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Barcode / Code (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.filterNot { it == "All" }.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    category = cat
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = purchasePrice,
                        onValueChange = { purchasePrice = it },
                        label = { Text("Cost Price *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = salePrice,
                        onValueChange = { salePrice = it },
                        label = { Text("Sale Price *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockQuantity,
                        onValueChange = { stockQuantity = it },
                        label = { Text("Stock Qty *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    ExposedDropdownMenuBox(
                        expanded = unitExpanded,
                        onExpandedChange = { unitExpanded = !unitExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = unit,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Unit") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                            modifier = Modifier.menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }
                        ) {
                            UNITS.forEach { u ->
                                DropdownMenuItem(
                                    text = { Text(u) },
                                    onClick = {
                                        unit = u
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = minStockLevel,
                    onValueChange = { minStockLevel = it },
                    label = { Text("Min Low-Stock Alert Threshold") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMsg = "Please enter product name."
                        return@Button
                    }
                    val cost = purchasePrice.toDoubleOrNull()
                    val sale = salePrice.toDoubleOrNull()
                    val qty = stockQuantity.toDoubleOrNull()
                    if (cost == null || sale == null || qty == null) {
                        errorMsg = "Please enter valid numeric prices and stock."
                        return@Button
                    }

                    onSave(
                        Product(
                            id = product?.id ?: 0L,
                            barcode = barcode.trim(),
                            name = name.trim(),
                            category = category,
                            purchasePrice = cost,
                            salePrice = sale,
                            stockQuantity = qty,
                            minStockLevel = minStockLevel.toDoubleOrNull() ?: 5.0,
                            unit = unit
                        )
                    )
                }
            ) { Text("Save Item") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
