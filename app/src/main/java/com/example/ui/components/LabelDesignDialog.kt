package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Product
import com.example.data.entity.StoreSettings
import com.example.ui.theme.BrandGold
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.BrandSlate
import com.example.ui.theme.BrandWhite
import com.example.util.QrCodeRenderer

data class LabelTemplate(
    val name: String,
    val widthMm: Int,
    val heightMm: Int,
    val description: String
)

data class PrintedLabelBatch(
    val id: String,
    val productName: String,
    val barcode: String,
    val copies: Int,
    val timestamp: Long,
    val templateName: String
)

@Composable
fun LabelDesignDialog(
    products: List<Product>,
    settings: StoreSettings,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val templates = remember {
        listOf(
            LabelTemplate("Standard Product", 50, 30, "50 × 30 mm - Retail Shelf & Box"),
            LabelTemplate("Jewelry / Optical", 38, 25, "38 × 25 mm - Small Accessories"),
            LabelTemplate("Shelf Price Tag", 70, 40, "70 × 40 mm - Large Store Shelves"),
            LabelTemplate("Compact Sticker", 40, 20, "40 × 20 mm - Barcode Only")
        )
    }

    var selectedTemplateIndex by remember { mutableStateOf(0) }
    var selectedProduct by remember { mutableStateOf(products.firstOrNull()) }
    var customWidthMm by remember { mutableStateOf("50") }
    var customHeightMm by remember { mutableStateOf("30") }
    var labelLayoutType by remember { mutableStateOf(0) } // 0: Barcode Code128, 1: QR Code, 2: Dual
    var copiesCount by remember { mutableStateOf(1) }
    var printerType by remember { mutableStateOf("58mm Thermal (ESC/POS)") }
    var showReprintHistory by remember { mutableStateOf(false) }

    val recentPrintBatches = remember {
        mutableStateListOf(
            PrintedLabelBatch("B-101", "Cement 50kg Falcon", "89012345001", 10, System.currentTimeMillis() - 3600000, "Standard Product"),
            PrintedLabelBatch("B-102", "Dulux Paint White 4L", "89012345002", 5, System.currentTimeMillis() - 7200000, "Shelf Price Tag")
        )
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
                    Icon(
                        imageVector = Icons.Default.Label,
                        contentDescription = "Label Design",
                        tint = BrandGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Label & Barcode Design",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BrandNavy
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFDCFCE7),
                    border = BorderStroke(1.dp, Color(0xFF16A34A).copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "100% Free • No Fees",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Product Selection
                Text(
                    text = "1. SELECT PRODUCT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandSlate
                )

                if (products.isEmpty()) {
                    Text("No products in inventory. Showing sample template.", fontSize = 12.sp, color = Color.Gray)
                } else {
                    ScrollableTabRow(
                        selectedTabIndex = products.indexOf(selectedProduct).coerceAtLeast(0),
                        edgePadding = 0.dp
                    ) {
                        products.take(8).forEach { prod ->
                            Tab(
                                selected = selectedProduct?.id == prod.id,
                                onClick = { selectedProduct = prod },
                                text = { Text(prod.name.take(14), fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Template Selection
                Text(
                    text = "2. LABEL TEMPLATE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandSlate
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    templates.forEachIndexed { idx, tmpl ->
                        FilterChip(
                            selected = selectedTemplateIndex == idx,
                            onClick = {
                                selectedTemplateIndex = idx
                                customWidthMm = tmpl.widthMm.toString()
                                customHeightMm = tmpl.heightMm.toString()
                            },
                            label = { Text(tmpl.name, fontSize = 10.sp) }
                        )
                    }
                }

                // Layout Format (Barcode vs QR)
                Text(
                    text = "3. CODE LAYOUT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandSlate
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { labelLayoutType = 0 },
                        colors = if (labelLayoutType == 0) ButtonDefaults.outlinedButtonColors(containerColor = BrandGold.copy(alpha = 0.2f)) else ButtonDefaults.outlinedButtonColors(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Barcode 128", fontSize = 11.sp, fontWeight = if (labelLayoutType == 0) FontWeight.Bold else FontWeight.Normal)
                    }
                    OutlinedButton(
                        onClick = { labelLayoutType = 1 },
                        colors = if (labelLayoutType == 1) ButtonDefaults.outlinedButtonColors(containerColor = BrandGold.copy(alpha = 0.2f)) else ButtonDefaults.outlinedButtonColors(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("QR Code", fontSize = 11.sp, fontWeight = if (labelLayoutType == 1) FontWeight.Bold else FontWeight.Normal)
                    }
                    OutlinedButton(
                        onClick = { labelLayoutType = 2 },
                        colors = if (labelLayoutType == 2) ButtonDefaults.outlinedButtonColors(containerColor = BrandGold.copy(alpha = 0.2f)) else ButtonDefaults.outlinedButtonColors(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Dual Code", fontSize = 11.sp, fontWeight = if (labelLayoutType == 2) FontWeight.Bold else FontWeight.Normal)
                    }
                }

                // Dimensions Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = customWidthMm,
                        onValueChange = { customWidthMm = it },
                        label = { Text("Width (mm)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customHeightMm,
                        onValueChange = { customHeightMm = it },
                        label = { Text("Height (mm)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Live Print Preview Card
                Text(
                    text = "4. LIVE PRINT PREVIEW (${customWidthMm}mm × ${customHeightMm}mm)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandSlate
                )

                val displayProduct = selectedProduct
                val prodName = displayProduct?.name ?: "Sample Hardware Tool 10mm"
                val prodPrice = displayProduct?.salePrice ?: 450.0
                val prodBarcode = displayProduct?.barcode?.ifBlank { "890123456789" } ?: "890123456789"
                val storeBrand = settings.storeName.ifBlank { "M AZLAN UMER • POS" }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.5.dp, Color(0xFFCBD5E1))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Store brand header
                        Text(
                            text = storeBrand,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandNavy,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))

                        // Product Title
                        Text(
                            text = prodName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Barcode stripes or QR preview
                        if (labelLayoutType == 0 || labelLayoutType == 2) {
                            // Simulated Barcode 128 stripes
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .height(34.dp)
                            ) {
                                val barCount = 42
                                val barWidth = size.width / barCount
                                for (i in 0 until barCount) {
                                    val isBar = (i * 7 + 3) % 5 != 0
                                    if (isBar) {
                                        drawRect(
                                            color = Color.Black,
                                            topLeft = Offset(i * barWidth, 0f),
                                            size = Size(barWidth * 0.75f, size.height)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = prodBarcode,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp,
                                color = Color.DarkGray
                            )
                        }

                        if (labelLayoutType == 1 || labelLayoutType == 2) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                modifier = Modifier.size(60.dp),
                                shape = RoundedCornerShape(4.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color.LightGray)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode,
                                        contentDescription = "QR Code",
                                        tint = Color.Black,
                                        modifier = Modifier.size(52.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Price and Currency
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "PRICE: ",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Text(
                                text = "${settings.currencySymbol} ${prodPrice.toInt()}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = BrandNavy
                            )
                        }
                    }
                }

                // 5. Copies Counter & Printer Config
                Text(
                    text = "5. COPIES & PRINTER CONFIG",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandSlate
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Copies to Print:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (copiesCount > 1) copiesCount-- },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Decrease")
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            Text(
                                text = "$copiesCount",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }

                        IconButton(
                            onClick = { if (copiesCount < 100) copiesCount++ },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Increase")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = printerType == "58mm Thermal (ESC/POS)",
                        onClick = { printerType = "58mm Thermal (ESC/POS)" },
                        label = { Text("58mm Thermal", fontSize = 10.sp) }
                    )
                    FilterChip(
                        selected = printerType == "80mm Thermal (ESC/POS)",
                        onClick = { printerType = "80mm Thermal (ESC/POS)" },
                        label = { Text("80mm Thermal", fontSize = 10.sp) }
                    )
                    FilterChip(
                        selected = printerType == "A4 Sticker Sheet",
                        onClick = { printerType = "A4 Sticker Sheet" },
                        label = { Text("A4 Sheet", fontSize = 10.sp) }
                    )
                }

                // Reprint History Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showReprintHistory = !showReprintHistory }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recently Printed Batches (${recentPrintBatches.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandNavy
                    )
                    Text(
                        text = if (showReprintHistory) "Hide" else "Show",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                if (showReprintHistory) {
                    recentPrintBatches.forEach { batch ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(batch.productName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("${batch.templateName} • ${batch.copies} copies", fontSize = 10.sp, color = Color.Gray)
                                }
                                Button(
                                    onClick = {
                                        Toast.makeText(context, "Reprinting ${batch.copies} labels for ${batch.productName} via $printerType", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandNavy),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Reprint", fontSize = 10.sp, color = BrandWhite)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pName = selectedProduct?.name ?: "Sample Product"
                    val pBarcode = selectedProduct?.barcode?.ifBlank { "890123456789" } ?: "890123456789"
                    recentPrintBatches.add(
                        0,
                        PrintedLabelBatch(
                            id = "B-${System.currentTimeMillis() % 1000}",
                            productName = pName,
                            barcode = pBarcode,
                            copies = copiesCount,
                            timestamp = System.currentTimeMillis(),
                            templateName = templates[selectedTemplateIndex].name
                        )
                    )
                    Toast.makeText(context, "Printed $copiesCount label(s) for $pName on $printerType (No Fees)", Toast.LENGTH_LONG).show()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandNavy)
            ) {
                Icon(Icons.Default.Print, contentDescription = null, tint = BrandGold, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Print $copiesCount Label(s)", color = BrandWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.Gray)
            }
        }
    )
}
