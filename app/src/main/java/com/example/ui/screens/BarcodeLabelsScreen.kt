package com.example.ui.screens

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.Product
import com.example.data.entity.StoreSettings
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel
import com.example.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeLabelsScreen(
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val products by viewModel.products.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()
    val effectiveSettings = storeSettings ?: StoreSettings()

    // Filtering State
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedBarcodeStatus by remember { mutableStateOf("All") } // "All", "Has Barcode", "Missing Barcode"

    // Selection & Quantity State
    var selectedProductIds by remember { mutableStateOf(setOf<Long>()) }
    val productCopiesMap = remember { mutableStateMapOf<Long, Int>() }
    var bulkQuantityText by remember { mutableStateOf("1") }

    // Print Configuration State
    var printOptions by remember {
        mutableStateOf(
            LabelPrintOptions(
                paperType = LabelPaperType.THERMAL_50X25,
                showStoreName = true,
                showStoreLogo = false,
                showProductName = true,
                showPrice = true,
                showBarcodeText = true,
                showSku = true,
                showUnit = false,
                showCutBorder = true,
                fontSize = LabelFontSize.MEDIUM,
                barcodeHeight = LabelBarcodeHeight.STANDARD
            )
        )
    }

    // Dialogs
    var showPrintPreviewDialog by remember { mutableStateOf(false) }
    var productForBarcodeDialog by remember { mutableStateOf<Product?>(null) }
    var showBulkGenerateDialog by remember { mutableStateOf(false) }

    // Categories list derived from database
    val categories = remember(products) {
        listOf("All") + products.map { it.category.ifBlank { "General" } }.distinct().sorted()
    }

    // Filtered Products (Database source of truth)
    val filteredProducts = remember(products, searchQuery, selectedCategory, selectedBarcodeStatus) {
        products.filter { p ->
            val matchesSearch = searchQuery.isBlank() ||
                    p.name.contains(searchQuery, ignoreCase = true) ||
                    p.barcode.contains(searchQuery, ignoreCase = true) ||
                    p.description.contains(searchQuery, ignoreCase = true) ||
                    p.batchNumber.contains(searchQuery, ignoreCase = true)

            val matchesCategory = selectedCategory == "All" ||
                    p.category.equals(selectedCategory, ignoreCase = true)

            val matchesStatus = when (selectedBarcodeStatus) {
                "Has Barcode" -> p.barcode.isNotBlank()
                "Missing Barcode" -> p.barcode.isBlank()
                else -> true
            }

            matchesSearch && matchesCategory && matchesStatus
        }
    }

    // Total Selected Counts
    val selectedCount = selectedProductIds.size
    val totalLabelsToPrint = remember(selectedProductIds, productCopiesMap) {
        selectedProductIds.sumOf { id -> productCopiesMap[id] ?: 1 }
    }
    val missingBarcodeCount = remember(products) {
        products.count { it.barcode.isBlank() }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("barcode_labels_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = Emerald500,
                                modifier = Modifier.size(22.dp).padding(end = 6.dp)
                            )
                            Text(
                                text = "Barcode & Label Printing",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = effectiveSettings.storeName.ifBlank { "SENTRY STORE" },
                            fontSize = 11.sp,
                            color = Slate300
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_barcode_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Preview & Print Action Button
                    Button(
                        onClick = {
                            if (selectedProductIds.isEmpty()) {
                                Toast.makeText(context, "Please select at least 1 product to print", Toast.LENGTH_SHORT).show()
                            } else {
                                showPrintPreviewDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedProductIds.isNotEmpty()) Emerald600 else Slate600
                        ),
                        modifier = Modifier.padding(end = 8.dp).testTag("btn_open_print_preview")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp).padding(end = 4.dp)
                        )
                        Text(
                            text = if (totalLabelsToPrint > 0) "Print ($totalLabelsToPrint)" else "Print Labels",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Navy900
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Slate50)
        ) {
            // Stats & Quick Actions Banner
            Surface(
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Stat badges
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Blue50,
                                modifier = Modifier.testTag("stat_total_products")
                            ) {
                                Text(
                                    text = "Total: ${products.size} Products",
                                    color = Blue700,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            if (missingBarcodeCount > 0) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Gold50,
                                    modifier = Modifier.testTag("stat_missing_barcodes")
                                ) {
                                    Text(
                                        text = "⚠️ $missingBarcodeCount Missing Barcode",
                                        color = Gold700,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        // Bulk Barcode Generation Button
                        if (missingBarcodeCount > 0) {
                            OutlinedButton(
                                onClick = { showBulkGenerateDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Emerald700),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("btn_bulk_generate_barcodes")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoFixHigh,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp).padding(end = 4.dp)
                                )
                                Text("Auto-Generate All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Search Input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_barcode_search"),
                        placeholder = { Text("Search by Product Name, SKU, Barcode...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = Slate400)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = Slate400)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Slate100,
                            focusedContainerColor = Color.White
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filters & Controls Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Category Dropdown
                        var categoryExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { categoryExpanded = true },
                                modifier = Modifier.fillMaxWidth().testTag("dropdown_barcode_category"),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Category: $selectedCategory",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 12.sp,
                                    color = Navy900
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            DropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false }
                            ) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = {
                                            selectedCategory = cat
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Status Filter
                        var statusExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { statusExpanded = true },
                                modifier = Modifier.fillMaxWidth().testTag("dropdown_barcode_status"),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Status: $selectedBarcodeStatus",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 12.sp,
                                    color = Navy900
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            DropdownMenu(
                                expanded = statusExpanded,
                                onDismissRequest = { statusExpanded = false }
                            ) {
                                listOf("All", "Has Barcode", "Missing Barcode").forEach { st ->
                                    DropdownMenuItem(
                                        text = { Text(st) },
                                        onClick = {
                                            selectedBarcodeStatus = st
                                            statusExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Batch Selection & Bulk Quantity Bar
                    Surface(
                        color = Slate100,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Select All / Deselect All across COMPLETE product list
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val allFilteredSelected = filteredProducts.isNotEmpty() &&
                                        filteredProducts.all { selectedProductIds.contains(it.id) }

                                Checkbox(
                                    checked = allFilteredSelected,
                                    onCheckedChange = { checked ->
                                        selectedProductIds = if (checked) {
                                            selectedProductIds + filteredProducts.map { it.id }
                                        } else {
                                            selectedProductIds - filteredProducts.map { it.id }.toSet()
                                        }
                                    },
                                    modifier = Modifier.size(24.dp).testTag("checkbox_select_all")
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (allFilteredSelected) "Deselect All" else "Select All (${filteredProducts.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Navy900,
                                    modifier = Modifier.clickable {
                                        selectedProductIds = if (allFilteredSelected) {
                                            selectedProductIds - filteredProducts.map { it.id }.toSet()
                                        } else {
                                            selectedProductIds + filteredProducts.map { it.id }
                                        }
                                    }
                                )
                            }

                            // Bulk quantity applier
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("Qty:", fontSize = 12.sp, color = Slate600)
                                OutlinedTextField(
                                    value = bulkQuantityText,
                                    onValueChange = { newVal ->
                                        bulkQuantityText = newVal.filter { ch: Char -> ch.isDigit() }.take(3)
                                    },
                                    modifier = Modifier
                                        .width(58.dp)
                                        .height(48.dp)
                                        .testTag("input_bulk_quantity"),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp, textAlign = TextAlign.Center),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                Button(
                                    onClick = {
                                        val qty = bulkQuantityText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                                        selectedProductIds.forEach { id ->
                                            productCopiesMap[id] = qty
                                        }
                                        Toast.makeText(context, "Applied $qty label copies to all selected products", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Navy800),
                                    modifier = Modifier.testTag("btn_apply_bulk_qty")
                                ) {
                                    Text("Apply", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // Selected summary banner
                    if (selectedCount > 0) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Selected: $selectedCount Products  •  Total: $totalLabelsToPrint Labels",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald700,
                                modifier = Modifier.testTag("txt_selection_summary")
                            )

                            TextButton(
                                onClick = {
                                    // Option: Set quantity from current stock
                                    selectedProductIds.forEach { id ->
                                        val p = products.find { it.id == id }
                                        if (p != null && p.stockQuantity > 0) {
                                            productCopiesMap[id] = p.stockQuantity.toInt().coerceIn(1, 100)
                                        }
                                    }
                                    Toast.makeText(context, "Matched label quantities with in-stock counts", Toast.LENGTH_SHORT).show()
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Set Qty from Stock", fontSize = 11.sp, color = Blue700)
                            }
                        }
                    }
                }
            }

            // Products List
            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = Slate400,
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No products found",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate600
                        )
                        Text(
                            text = "Try adjusting your search query or filters",
                            fontSize = 12.sp,
                            color = Slate400
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("list_barcode_products"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        val isSelected = selectedProductIds.contains(product.id)
                        val copies = productCopiesMap[product.id] ?: 1

                        ProductBarcodeCard(
                            product = product,
                            currencySymbol = effectiveSettings.currencySymbol.ifBlank { "Rs." },
                            isSelected = isSelected,
                            copies = copies,
                            onToggleSelection = {
                                selectedProductIds = if (isSelected) {
                                    selectedProductIds - product.id
                                } else {
                                    selectedProductIds + product.id
                                }
                            },
                            onCopiesChanged = { newCopies ->
                                productCopiesMap[product.id] = newCopies.coerceIn(1, 999)
                            },
                            onEditBarcode = {
                                productForBarcodeDialog = product
                            },
                            onPrintSingle = {
                                val singleItem = ProductLabelItem(
                                    product = product,
                                    copies = copies,
                                    customBarcode = product.barcode,
                                    barcodeType = BarcodeGenerator.detectBarcodeType(product.barcode)
                                )
                                coroutineScope.launch(Dispatchers.IO) {
                                    val pdfFile = BarcodeLabelPdfGenerator.generateLabelsPdf(
                                        context = context,
                                        labelItems = listOf(singleItem),
                                        settings = effectiveSettings,
                                        options = printOptions
                                    )
                                    withContext(Dispatchers.Main) {
                                        if (pdfFile != null) {
                                            BarcodeLabelPdfGenerator.printPdf(context, pdfFile, "Label_${product.name}")
                                        } else {
                                            Toast.makeText(context, "Failed to generate label PDF", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Single Product Barcode Generator / Editor Dialog
    if (productForBarcodeDialog != null) {
        BarcodeEditorDialog(
            product = productForBarcodeDialog!!,
            existingBarcodes = products.map { it.barcode }.toSet(),
            onDismiss = { productForBarcodeDialog = null },
            onBarcodeSaved = { updatedProduct ->
                viewModel.saveProduct(updatedProduct) {
                    Toast.makeText(context, "Barcode saved for ${updatedProduct.name}", Toast.LENGTH_SHORT).show()
                }
                productForBarcodeDialog = null
            }
        )
    }

    // Bulk Barcode Generation Dialog
    if (showBulkGenerateDialog) {
        BulkBarcodeGenerateDialog(
            missingProducts = products.filter { it.barcode.isBlank() },
            existingBarcodes = products.map { it.barcode }.toSet(),
            onDismiss = { showBulkGenerateDialog = false },
            onGenerateComplete = { generatedProducts ->
                generatedProducts.forEach { p ->
                    viewModel.saveProduct(p)
                }
                Toast.makeText(context, "Successfully generated ${generatedProducts.size} barcodes!", Toast.LENGTH_LONG).show()
                showBulkGenerateDialog = false
            }
        )
    }

    // Print & Preview Sheet / Dialog
    if (showPrintPreviewDialog) {
        val selectedItems = remember(selectedProductIds, productCopiesMap, products) {
            selectedProductIds.mapNotNull { id ->
                val p = products.find { it.id == id } ?: return@mapNotNull null
                ProductLabelItem(
                    product = p,
                    copies = productCopiesMap[id] ?: 1,
                    customBarcode = p.barcode,
                    barcodeType = BarcodeGenerator.detectBarcodeType(p.barcode)
                )
            }
        }

        LabelPrintPreviewDialog(
            items = selectedItems,
            settings = effectiveSettings,
            initialOptions = printOptions,
            onDismiss = { showPrintPreviewDialog = false },
            onOptionsChanged = { printOptions = it },
            onPrint = { optionsToUse ->
                coroutineScope.launch(Dispatchers.IO) {
                    val pdfFile = BarcodeLabelPdfGenerator.generateLabelsPdf(
                        context = context,
                        labelItems = selectedItems,
                        settings = effectiveSettings,
                        options = optionsToUse
                    )
                    withContext(Dispatchers.Main) {
                        if (pdfFile != null) {
                            BarcodeLabelPdfGenerator.printPdf(context, pdfFile, "BatchBarcodeLabels")
                            showPrintPreviewDialog = false
                        } else {
                            Toast.makeText(context, "Failed to generate printable PDF", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onShare = { optionsToUse ->
                coroutineScope.launch(Dispatchers.IO) {
                    val pdfFile = BarcodeLabelPdfGenerator.generateLabelsPdf(
                        context = context,
                        labelItems = selectedItems,
                        settings = effectiveSettings,
                        options = optionsToUse
                    )
                    withContext(Dispatchers.Main) {
                        if (pdfFile != null) {
                            BarcodeLabelPdfGenerator.sharePdf(context, pdfFile)
                        } else {
                            Toast.makeText(context, "Failed to generate PDF for sharing", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }
}

/**
 * Individual Product Card in Barcode & Labels list
 */
@Composable
fun ProductBarcodeCard(
    product: Product,
    currencySymbol: String,
    isSelected: Boolean,
    copies: Int,
    onToggleSelection: () -> Unit,
    onCopiesChanged: (Int) -> Unit,
    onEditBarcode: () -> Unit,
    onPrintSingle: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Blue50.copy(alpha = 0.6f) else Color.White
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) Blue600 else Slate200
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_barcode_card_${product.id}")
            .clickable { onToggleSelection() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() },
                    modifier = Modifier.size(24.dp).testTag("checkbox_product_${product.id}")
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Navy900,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Price: $currencySymbol ${if (product.salePrice % 1.0 == 0.0) product.salePrice.toLong() else product.salePrice}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Emerald700
                        )
                        Text(text = "•", color = Slate400, fontSize = 10.sp)
                        Text(
                            text = "Stock: ${product.stockQuantity} ${product.unit}",
                            fontSize = 11.sp,
                            color = Slate600
                        )
                    }

                    val skuText = product.description.ifBlank { product.batchNumber.ifBlank { "ID: ${product.id}" } }
                    Text(
                        text = "SKU: $skuText  •  Cat: ${product.category}",
                        fontSize = 11.sp,
                        color = Slate500
                    )
                }

                // Quick Single Print
                IconButton(
                    onClick = onPrintSingle,
                    modifier = Modifier.testTag("btn_print_single_${product.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Print,
                        contentDescription = "Print Label",
                        tint = Navy700
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Slate200, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Barcode Status & Quantity Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Barcode Chip
                if (product.barcode.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Emerald50,
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Emerald200),
                        modifier = Modifier
                            .testTag("chip_barcode_active_${product.id}")
                            .clickable { onEditBarcode() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = Emerald700,
                                modifier = Modifier.size(16.dp).padding(end = 4.dp)
                            )
                            Text(
                                text = product.barcode,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Emerald800
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Barcode",
                                tint = Emerald600,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Gold50,
                        border = androidx.compose.foundation.BorderStroke(0.8.dp, Gold400),
                        modifier = Modifier
                            .testTag("chip_barcode_missing_${product.id}")
                            .clickable { onEditBarcode() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "+ Generate Barcode",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Gold800
                            )
                        }
                    }
                }

                // Label Copies Stepper
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "Copies:", fontSize = 11.sp, color = Slate600)

                    IconButton(
                        onClick = { onCopiesChanged(max(1, copies - 1)) },
                        modifier = Modifier.size(28.dp).testTag("btn_minus_copies_${product.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RemoveCircleOutline,
                            contentDescription = "Decrease",
                            tint = Slate700,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = "$copies",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Navy900,
                        modifier = Modifier.widthIn(min = 22.dp),
                        textAlign = TextAlign.Center
                    )

                    IconButton(
                        onClick = { onCopiesChanged(copies + 1) },
                        modifier = Modifier.size(28.dp).testTag("btn_plus_copies_${product.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = "Increase",
                            tint = Emerald600,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Barcode Editor Dialog for generating or assigning a barcode to a product
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeEditorDialog(
    product: Product,
    existingBarcodes: Set<String>,
    onDismiss: () -> Unit,
    onBarcodeSaved: (Product) -> Unit
) {
    var selectedType by remember {
        mutableStateOf(
            if (product.barcode.isNotBlank()) BarcodeGenerator.detectBarcodeType(product.barcode)
            else BarcodeType.CODE_128
        )
    }

    var barcodeInput by remember { mutableStateOf(product.barcode) }
    val validationResult = remember(barcodeInput, selectedType) {
        BarcodeGenerator.validate(barcodeInput, selectedType)
    }

    // Live rendered bitmap
    val previewBitmap = remember(barcodeInput, selectedType, validationResult) {
        if (barcodeInput.isNotBlank()) {
            BarcodeGenerator.generateBarcodeBitmap(barcodeInput, selectedType, 400, 140)
        } else null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 500.dp)
                .testTag("dialog_barcode_editor")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Barcode Generator & Editor",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )
                        Text(
                            text = product.name,
                            fontSize = 12.sp,
                            color = Slate600
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate500)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Barcode Format Dropdown
                Text(text = "Select Barcode Standard:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate700)
                Spacer(modifier = Modifier.height(4.dp))
                var formatExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { formatExpanded = true },
                        modifier = Modifier.fillMaxWidth().testTag("dropdown_select_barcode_type"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("${selectedType.displayName} — ${selectedType.description}", fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = formatExpanded,
                        onDismissRequest = { formatExpanded = false }
                    ) {
                        BarcodeType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(type.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(type.description, fontSize = 11.sp, color = Slate500)
                                    }
                                },
                                onClick = {
                                    selectedType = type
                                    formatExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Barcode Input
                OutlinedTextField(
                    value = barcodeInput,
                    onValueChange = { barcodeInput = it.trim() },
                    label = { Text("Barcode Number / Data") },
                    placeholder = { Text(selectedType.samplePlaceholder) },
                    modifier = Modifier.fillMaxWidth().testTag("input_custom_barcode"),
                    singleLine = true,
                    isError = barcodeInput.isNotBlank() && !validationResult.isValid,
                    trailingIcon = {
                        if (validationResult.isValid && barcodeInput.isNotBlank()) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Valid", tint = Emerald600)
                        }
                    }
                )

                if (barcodeInput.isNotBlank() && !validationResult.isValid) {
                    Text(
                        text = validationResult.errorMessage ?: "Invalid format for ${selectedType.displayName}",
                        color = Rose600,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Auto-generate button
                Button(
                    onClick = {
                        barcodeInput = BarcodeGenerator.autoGenerateBarcode(selectedType, product.id, existingBarcodes)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Slate100, contentColor = Navy800),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("btn_auto_generate_code")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).padding(end = 6.dp)
                    )
                    Text("Auto-Generate Unique Code", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Visual Preview Box
                Text(text = "Visual Barcode Preview:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate700)
                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate300),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    if (previewBitmap != null) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Image(
                                bitmap = previewBitmap.asImageBitmap(),
                                contentDescription = "Barcode Preview",
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(65.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (validationResult.isValid) validationResult.normalizedValue else barcodeInput,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                        }
                    } else {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text("Enter or generate a valid barcode to view preview", fontSize = 11.sp, color = Slate400)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).testTag("btn_cancel_barcode_editor")
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val finalCode = if (validationResult.isValid) validationResult.normalizedValue else barcodeInput.trim()
                            if (finalCode.isBlank()) return@Button

                            val updated = product.copy(
                                barcode = finalCode,
                                updatedAt = System.currentTimeMillis()
                            )
                            onBarcodeSaved(updated)
                        },
                        enabled = validationResult.isValid && barcodeInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        modifier = Modifier.weight(1f).testTag("btn_save_product_barcode")
                    ) {
                        Text("Save & Update", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Bulk Barcode Generator Dialog for generating barcodes for all products missing barcodes
 */
@Composable
fun BulkBarcodeGenerateDialog(
    missingProducts: List<Product>,
    existingBarcodes: Set<String>,
    onDismiss: () -> Unit,
    onGenerateComplete: (List<Product>) -> Unit
) {
    var selectedType by remember { mutableStateOf(BarcodeType.CODE_128) }
    var isProcessing by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth(0.92f).padding(16.dp).testTag("dialog_bulk_generate")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Bulk Barcode Generator",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Navy900
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Found ${missingProducts.size} products currently missing barcodes. Automatically generate unique, compliant barcodes for all of them.",
                    fontSize = 12.sp,
                    color = Slate600
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(text = "Choose Barcode Standard:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate700)
                Spacer(modifier = Modifier.height(6.dp))

                listOf(BarcodeType.CODE_128, BarcodeType.EAN_13).forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedType = type }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedType == type,
                            onClick = { selectedType = type }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(type.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(type.description, fontSize = 11.sp, color = Slate500)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            isProcessing = true
                            val generatedBarcodes = existingBarcodes.toMutableSet()
                            val updatedList = missingProducts.map { prod ->
                                val code = BarcodeGenerator.autoGenerateBarcode(selectedType, prod.id, generatedBarcodes)
                                generatedBarcodes.add(code)
                                prod.copy(barcode = code, updatedAt = System.currentTimeMillis())
                            }
                            isProcessing = false
                            onGenerateComplete(updatedList)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        enabled = !isProcessing,
                        modifier = Modifier.weight(1f).testTag("btn_confirm_bulk_generate")
                    ) {
                        Text("Generate ${missingProducts.size}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Print & Preview Modal showing real-time Single Label and Sheet layout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelPrintPreviewDialog(
    items: List<ProductLabelItem>,
    settings: StoreSettings,
    initialOptions: LabelPrintOptions,
    onDismiss: () -> Unit,
    onOptionsChanged: (LabelPrintOptions) -> Unit,
    onPrint: (LabelPrintOptions) -> Unit,
    onShare: (LabelPrintOptions) -> Unit
) {
    val context = LocalContext.current
    var options by remember { mutableStateOf(initialOptions) }
    var previewMode by remember { mutableStateOf("SINGLE") } // "SINGLE" or "SHEET"
    var previewPage by remember { mutableStateOf(1) }

    val totalLabels = remember(items) { items.sumOf { max(1, it.copies) } }
    val labelsPerPage = options.paperType.labelsPerPage
    val totalSheetPages = remember(totalLabels, labelsPerPage, options.paperType.isSheet) {
        if (!options.paperType.isSheet) totalLabels
        else max(1, ceil(totalLabels.toDouble() / labelsPerPage).toInt())
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .testTag("dialog_label_print_preview")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Navy900)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Print Preview & Layout Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${items.size} Products • $totalLabels Total Labels",
                            fontSize = 11.sp,
                            color = Slate300
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                // Mode Tabs
                TabRow(
                    selectedTabIndex = if (previewMode == "SINGLE") 0 else 1,
                    containerColor = Slate100,
                    contentColor = Navy900
                ) {
                    Tab(
                        selected = previewMode == "SINGLE",
                        onClick = { previewMode = "SINGLE" },
                        text = { Text("Single Label Preview", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                    Tab(
                        selected = previewMode == "SHEET",
                        onClick = { previewMode = "SHEET" },
                        text = {
                            Text(
                                if (options.paperType.isSheet) "Full Sheet Layout (${options.paperType.displayName})"
                                else "Roll Sequence (${options.paperType.displayName})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    )
                }

                // Main Content (Split between Preview Canvas and Settings controls)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    // Preview Pane (Left)
                    Box(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight()
                            .background(Slate200)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (previewMode == "SINGLE") {
                            // Single Label Realistic Preview
                            val firstItem = items.firstOrNull()
                            if (firstItem != null) {
                                SingleLabelVisualCard(
                                    item = firstItem,
                                    settings = settings,
                                    options = options
                                )
                            }
                        } else {
                            // Sheet / Roll Grid Preview
                            SheetLayoutVisualPreview(
                                items = items,
                                settings = settings,
                                options = options,
                                currentPage = previewPage,
                                totalPages = totalSheetPages,
                                onPageChange = { previewPage = it }
                            )
                        }
                    }

                    // Settings Controls Pane (Right)
                    Column(
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxHeight()
                            .background(Color.White)
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Print Format & Paper Size",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Navy900
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        // Paper Size Selector
                        var paperDropdownExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { paperDropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth().testTag("dropdown_select_paper_size"),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = options.paperType.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Navy900,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }

                            DropdownMenu(
                                expanded = paperDropdownExpanded,
                                onDismissRequest = { paperDropdownExpanded = false }
                            ) {
                                LabelPaperType.entries.forEach { pt ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(pt.displayName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text(
                                                    if (pt.isSheet) "${pt.columns} cols × ${pt.rows} rows (${pt.labelsPerPage} per page)"
                                                    else "Continuous / Roll sticker (${pt.widthMm} × ${pt.heightMm} mm)",
                                                    fontSize = 10.sp,
                                                    color = Slate500
                                                )
                                            }
                                        },
                                        onClick = {
                                            options = options.copy(paperType = pt)
                                            onOptionsChanged(options)
                                            previewPage = 1
                                            paperDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Slate200)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Label Content Options",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Navy900
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Toggles:
                        LabelToggleRow("Show Store Name", options.showStoreName) {
                            options = options.copy(showStoreName = it)
                            onOptionsChanged(options)
                        }

                        // Logo Toggle (Only store logo, product photo is FORBIDDEN)
                        LabelToggleRow("Show Store Logo (If Configured)", options.showStoreLogo) {
                            options = options.copy(showStoreLogo = it)
                            onOptionsChanged(options)
                        }

                        LabelToggleRow("Show Product Name", options.showProductName) {
                            options = options.copy(showProductName = it)
                            onOptionsChanged(options)
                        }

                        LabelToggleRow("Show Selling Price", options.showPrice) {
                            options = options.copy(showPrice = it)
                            onOptionsChanged(options)
                        }

                        LabelToggleRow("Show Barcode Text", options.showBarcodeText) {
                            options = options.copy(showBarcodeText = it)
                            onOptionsChanged(options)
                        }

                        LabelToggleRow("Show SKU", options.showSku) {
                            options = options.copy(showSku = it)
                            onOptionsChanged(options)
                        }

                        LabelToggleRow("Show Unit", options.showUnit) {
                            options = options.copy(showUnit = it)
                            onOptionsChanged(options)
                        }

                        LabelToggleRow("Show Cut Border / Outline", options.showCutBorder) {
                            options = options.copy(showCutBorder = it)
                            onOptionsChanged(options)
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Slate200)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Font scale
                        Text(text = "Font Sizing:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate700)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LabelFontSize.entries.forEach { size ->
                                FilterChip(
                                    selected = options.fontSize == size,
                                    onClick = {
                                        options = options.copy(fontSize = size)
                                        onOptionsChanged(options)
                                    },
                                    label = { Text(size.label, fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Barcode Height
                        Text(text = "Barcode Height:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Slate700)
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LabelBarcodeHeight.entries.forEach { bh ->
                                FilterChip(
                                    selected = options.barcodeHeight == bh,
                                    onClick = {
                                        options = options.copy(barcodeHeight = bh)
                                        onOptionsChanged(options)
                                    },
                                    label = { Text(bh.label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                // Bottom Action Bar
                Surface(
                    color = Slate100,
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onShare(options) },
                            modifier = Modifier.testTag("btn_share_labels_pdf")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                            Text("Share PDF")
                        }

                        Button(
                            onClick = { onPrint(options) },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                            modifier = Modifier.testTag("btn_confirm_print_labels")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp).padding(end = 6.dp))
                            Text("Print via Android Print ($totalLabels Labels)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LabelToggleRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 12.sp, color = Navy900)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.height(28.dp)
        )
    }
}

/**
 * Realistic Single Label Preview Card rendering exactly how the sticker looks
 */
@Composable
fun SingleLabelVisualCard(
    item: ProductLabelItem,
    settings: StoreSettings,
    options: LabelPrintOptions
) {
    val barcodeText = item.customBarcode.ifBlank { item.product.barcode }
    val barcodeBmp = remember(barcodeText, item.barcodeType, options.barcodeHeight) {
        val effective = barcodeText.ifBlank { "8901234567890" }
        BarcodeGenerator.generateBarcodeBitmap(effective, item.barcodeType, 400, 140)
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .widthIn(min = 240.dp, max = 340.dp)
            .testTag("single_label_preview_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cut outline preview
            if (options.showCutBorder) {
                Surface(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(4.dp),
                    border = androidx.compose.foundation.BorderStroke(0.8.dp, Slate300),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                ) {
                    Box(modifier = Modifier.padding(2.dp), contentAlignment = Alignment.Center) {
                        Text("--- CUT LINE / LABEL MARGIN ---", fontSize = 8.sp, color = Slate400)
                    }
                }
            }

            // Store Name
            if (options.showStoreName) {
                Text(
                    text = settings.storeName.ifBlank { "SENTRY STORE" }.uppercase(Locale.US),
                    fontSize = (12 * options.fontSize.scale).sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy900,
                    textAlign = TextAlign.Center
                )
            }

            // Product Name (NO PRODUCT IMAGE!)
            if (options.showProductName) {
                Text(
                    text = item.product.name,
                    fontSize = (13 * options.fontSize.scale).sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Price
            if (options.showPrice) {
                val symbol = settings.currencySymbol.ifBlank { "Rs." }
                val priceStr = if (item.product.salePrice % 1.0 == 0.0) item.product.salePrice.toLong() else item.product.salePrice
                Text(
                    text = "$symbol $priceStr",
                    fontSize = (14 * options.fontSize.scale).sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Barcode Bitmap
            if (barcodeBmp != null) {
                Image(
                    bitmap = barcodeBmp.asImageBitmap(),
                    contentDescription = "Barcode",
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(
                            when (options.barcodeHeight) {
                                LabelBarcodeHeight.COMPACT -> 45.dp
                                LabelBarcodeHeight.STANDARD -> 65.dp
                                LabelBarcodeHeight.TALL -> 85.dp
                            }
                        )
                )
            } else {
                Surface(
                    color = Slate100,
                    modifier = Modifier.fillMaxWidth(0.9f).height(50.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("|||||||||||||||||||||||||||||||||", fontFamily = FontFamily.Monospace, fontSize = 16.sp)
                    }
                }
            }

            // Barcode Number
            if (options.showBarcodeText) {
                Text(
                    text = barcodeText.ifBlank { "8901234567890" },
                    fontSize = (10 * options.fontSize.scale).sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )
            }

            // SKU & Unit
            if (options.showSku || options.showUnit) {
                val parts = mutableListOf<String>()
                if (options.showSku) {
                    val sku = item.product.description.ifBlank { item.product.batchNumber.ifBlank { barcodeText } }
                    parts.add("SKU: $sku")
                }
                if (options.showUnit && item.product.unit.isNotBlank()) {
                    parts.add("Unit: ${item.product.unit}")
                }

                Text(
                    text = parts.joinToString(" • "),
                    fontSize = (9 * options.fontSize.scale).sp,
                    color = Slate600,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Visual multi-item sheet preview showing sheet layout and pagination
 */
@Composable
fun SheetLayoutVisualPreview(
    items: List<ProductLabelItem>,
    settings: StoreSettings,
    options: LabelPrintOptions,
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit
) {
    val expanded = remember(items) {
        val list = mutableListOf<ProductLabelItem>()
        items.forEach { item ->
            repeat(max(1, item.copies)) { list.add(item) }
        }
        list
    }

    val paper = options.paperType
    val perPage = paper.labelsPerPage
    val startIndex = (currentPage - 1) * perPage
    val pageItems = expanded.drop(startIndex).take(perPage)

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Page Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { if (currentPage > 1) onPageChange(currentPage - 1) },
                enabled = currentPage > 1
            ) {
                Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Previous Page")
            }

            Text(
                text = "Page $currentPage of $totalPages",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Navy900,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            IconButton(
                onClick = { if (currentPage < totalPages) onPageChange(currentPage + 1) },
                enabled = currentPage < totalPages
            ) {
                Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Next Page")
            }
        }

        // Sheet Canvas Preview
        Card(
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "${paper.displayName} — Page $currentPage (${pageItems.size} labels)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate500,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                val cols = paper.columns
                val rows = (pageItems.size + cols - 1) / cols

                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (c in 0 until cols) {
                            val idx = r * cols + c
                            if (idx < pageItems.size) {
                                val item = pageItems[idx]
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(0.5.dp, if (options.showCutBorder) Slate400 else Color.Transparent)
                                        .background(Color.White)
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        if (options.showStoreName) {
                                            Text(
                                                settings.storeName.ifBlank { "SENTRY STORE" },
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }
                                        Text(
                                            item.product.name,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (options.showPrice) {
                                            Text("Rs. ${item.product.salePrice.toLong()}", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text("||||||||||||", fontFamily = FontFamily.Monospace, fontSize = 9.sp)
                                        if (options.showBarcodeText) {
                                            Text(item.customBarcode.ifBlank { item.product.barcode }, fontSize = 6.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
