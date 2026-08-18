package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Product
import com.example.ui.theme.BentoRose
import com.example.ui.theme.Emerald100
import com.example.ui.theme.Emerald600
import com.example.ui.viewmodel.StoreViewModel

/**
 * Embedded Quick Sale Widget for the Dashboard, optimized for high-traffic counter speed.
 */
@Composable
fun QuickSaleDashboardWidget(
    viewModel: StoreViewModel,
    onOpenFullPos: () -> Unit
) {
    val allProducts by viewModel.allProducts.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val categories = remember(allProducts) {
        allProducts.map { it.category }.distinct().filter { it.isNotBlank() }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var showExpressModal by remember { mutableStateOf(false) }
    var selectedTenderedAmount by remember { mutableDoubleStateOf(0.0) } // 0.0 means exact

    // Filter products
    val filteredProducts = remember(allProducts, searchQuery, selectedCategory) {
        allProducts.filter { product ->
            val matchesSearch = product.name.contains(searchQuery, ignoreCase = true) ||
                    product.barcode.contains(searchQuery, ignoreCase = true) ||
                    product.category.contains(searchQuery, ignoreCase = true)
            val matchesCat = selectedCategory == "All" || product.category.equals(selectedCategory, ignoreCase = true)
            matchesSearch && matchesCat
        }.take(8) // Display top 8 matching items for quick access
    }

    val cartTotal = cart.sumOf { it.total }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Emerald100),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Emerald600,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Quick Sale Counter",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = Emerald100,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "1-TAP CHECKOUT",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Emerald600,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "High-traffic fast billing mode",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }

                Row {
                    if (cart.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearCart() }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear Cart",
                                tint = BentoRose,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Button(
                        onClick = { showExpressModal = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                    ) {
                        Icon(imageVector = Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Express Mode", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fast Product Search Bar with Barcode Icon
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Instant Product / Barcode Search...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    IconButton(onClick = { showBarcodeScanner = true }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == "All",
                        onClick = { selectedCategory = "All" },
                        label = { Text("All Products", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Emerald100, selectedLabelColor = Emerald600)
                    )
                }
                items(categories) { catName ->
                    FilterChip(
                        selected = selectedCategory == catName,
                        onClick = { selectedCategory = catName },
                        label = { Text(catName, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Emerald100, selectedLabelColor = Emerald600)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Products Grid for 1-Tap Quick Add
            Text("Tap item to add to bill:", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))

            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No matching products found.", fontSize = 12.sp, color = Color.Gray)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                ) {
                    items(filteredProducts) { product ->
                        val cartItem = cart.find { it.product.id == product.id }
                        val qtyInCart = cartItem?.quantity?.toInt() ?: 0

                        Card(
                            onClick = { viewModel.addToCart(product, 1.0) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (qtyInCart > 0) Emerald100.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = if (qtyInCart > 0) androidx.compose.foundation.BorderStroke(1.5.dp, Emerald600) else null
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
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${settings.currencySymbol}${product.salePrice.toInt()} / ${product.unit}",
                                        fontSize = 11.sp,
                                        color = Emerald600,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (qtyInCart > 0) {
                                    Surface(
                                        color = Emerald600,
                                        shape = CircleShape
                                    ) {
                                        Text(
                                            text = "$qtyInCart",
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Cart Summary & 1-TAP CASH SALE ACTION BAR
            AnimatedVisibility(
                visible = cart.isNotEmpty(),
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Cart Items List Quick View
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(cart) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.product.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { viewModel.updateCartQty(item.product.id, item.quantity - 1) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }

                                    Text(
                                        text = "${item.quantity.toInt()} ${item.product.unit}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )

                                    IconButton(
                                        onClick = { viewModel.updateCartQty(item.product.id, item.quantity + 1) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = "${settings.currencySymbol}${item.total.toInt()}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Emerald600
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tendered Note Buttons
                    Text("Cash Tendered / Note:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))

                    val notes = listOf(0.0, cartTotal, 500.0, 1000.0, 5000.0)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(notes) { noteVal ->
                            val isSelected = selectedTenderedAmount == noteVal
                            val label = when {
                                noteVal == 0.0 -> "Exact (${settings.currencySymbol}${cartTotal.toInt()})"
                                noteVal == cartTotal -> "Exact Note"
                                noteVal >= 1000 -> "${noteVal.toInt() / 1000}k Note"
                                else -> "Rs. ${noteVal.toInt()}"
                            }

                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTenderedAmount = noteVal },
                                label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Emerald600,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    // Change Calculation Info if note paid > total
                    val effectivePaid = if (selectedTenderedAmount > 0.0) selectedTenderedAmount else cartTotal
                    val changeAmount = (effectivePaid - cartTotal).coerceAtLeast(0.0)

                    if (changeAmount > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Return Change: ${settings.currencySymbol}${changeAmount.toInt()}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BentoRose
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // SINGLE TAP COMPLETE CASH SALE BUTTON
                    Button(
                        onClick = {
                            val paidToProcess = if (selectedTenderedAmount > 0) selectedTenderedAmount else cartTotal
                            viewModel.processQuickSale(customPaidAmount = paidToProcess, customPaymentType = "Cash")
                            selectedTenderedAmount = 0.0
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "COMPLETE CASH SALE (${settings.currencySymbol}${cartTotal.toInt()})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }

    // Barcode Scanner Dialog
    if (showBarcodeScanner) {
        BarCodeScannerDialog(
            productsList = allProducts,
            onBarcodeScanned = { scannedCode ->
                searchQuery = scannedCode
                showBarcodeScanner = false

                val matched = allProducts.find { it.barcode == scannedCode || it.name.equals(scannedCode, ignoreCase = true) }
                if (matched != null) {
                    viewModel.addToCart(matched, 1.0)
                    viewModel.showToast("Added '${matched.name}' to cart!")
                } else {
                    viewModel.showToast("No product found for barcode: $scannedCode")
                }
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }

    // Full Express Modal Mode for High-Traffic Peak Hours
    if (showExpressModal) {
        QuickSaleModal(
            viewModel = viewModel,
            onDismiss = { showExpressModal = false },
            onOpenFullPos = {
                showExpressModal = false
                onOpenFullPos()
            }
        )
    }
}

/**
 * Dedicated Full-Screen/Express Modal for peak rush counter billing.
 */
@Composable
fun QuickSaleModal(
    viewModel: StoreViewModel,
    onDismiss: () -> Unit,
    onOpenFullPos: () -> Unit
) {
    val allProducts by viewModel.allProducts.collectAsState()
    val cart by viewModel.cart.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val categories = remember(allProducts) {
        allProducts.map { it.category }.distinct().filter { it.isNotBlank() }
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var customTenderedInput by remember { mutableStateOf("") }

    val cartTotal = cart.sumOf { it.total }
    val cartCount = cart.sumOf { it.quantity }

    val filteredProducts = remember(allProducts, searchQuery, selectedCategory) {
        allProducts.filter { product ->
            val matchesSearch = product.name.contains(searchQuery, ignoreCase = true) ||
                    product.barcode.contains(searchQuery, ignoreCase = true) ||
                    product.category.contains(searchQuery, ignoreCase = true)
            val matchesCat = selectedCategory == "All" || product.category.equals(selectedCategory, ignoreCase = true)
            matchesSearch && matchesCat
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Emerald100),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = Emerald600)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Express Counter Mode", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Peak hour rapid billing", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Search & Scanner Header
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Scan or Search product...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = { showBarcodeScanner = true }) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Fast Category Selector
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == "All",
                            onClick = { selectedCategory = "All" },
                            label = { Text("All", fontSize = 11.sp) }
                        )
                    }
                    items(categories) { catName ->
                        FilterChip(
                            selected = selectedCategory == catName,
                            onClick = { selectedCategory = catName },
                            label = { Text(catName, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Products Quick Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                ) {
                    items(filteredProducts) { product ->
                        val cartItem = cart.find { it.product.id == product.id }
                        val qty = cartItem?.quantity?.toInt() ?: 0

                        Card(
                            onClick = { viewModel.addToCart(product, 1.0) },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (qty > 0) Emerald100 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(product.name, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${settings.currencySymbol}${product.salePrice.toInt()}", fontSize = 10.sp, color = Emerald600, fontWeight = FontWeight.Bold)
                                }
                                if (qty > 0) {
                                    Surface(color = Emerald600, shape = CircleShape) {
                                        Text("$qty", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                if (cart.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Selected Cart (${cartCount.toInt()} items):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(cart) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.product.name, fontSize = 11.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${item.quantity.toInt()} x ${settings.currencySymbol}${item.customPrice.toInt()} = ${settings.currencySymbol}${item.total.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tendered input or notes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val quickNotes = listOf(500.0, 1000.0, 5000.0)
                        quickNotes.forEach { note ->
                            OutlinedButton(
                                onClick = { customTenderedInput = note.toInt().toString() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Rs. ${note.toInt()}", fontSize = 10.sp)
                            }
                        }
                    }

                    val paidVal = customTenderedInput.toDoubleOrNull() ?: cartTotal
                    val changeVal = (paidVal - cartTotal).coerceAtLeast(0.0)

                    if (changeVal > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Change to Return: ${settings.currencySymbol}${changeVal.toInt()}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BentoRose
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (cart.isNotEmpty()) {
                Button(
                    onClick = {
                        val paidVal = customTenderedInput.toDoubleOrNull() ?: cartTotal
                        viewModel.processQuickSale(customPaidAmount = paidVal)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("1-TAP CASH SALE (${settings.currencySymbol}${cartTotal.toInt()})", fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick = onOpenFullPos,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open Full POS Billing")
                }
            }
        },
        dismissButton = null
    )

    if (showBarcodeScanner) {
        BarCodeScannerDialog(
            productsList = allProducts,
            onBarcodeScanned = { scannedCode ->
                searchQuery = scannedCode
                showBarcodeScanner = false

                val matched = allProducts.find { it.barcode == scannedCode || it.name.equals(scannedCode, ignoreCase = true) }
                if (matched != null) {
                    viewModel.addToCart(matched, 1.0)
                    viewModel.showToast("Added '${matched.name}'")
                }
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }
}
