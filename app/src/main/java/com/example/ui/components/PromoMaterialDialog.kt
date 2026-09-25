package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.entity.StoreSettings
import com.example.ui.theme.*
import com.example.util.*
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromoMaterialPreviewDialog(
    items: List<ProductLabelItem>,
    settings: StoreSettings,
    initialOptions: PromoPrintOptions = PromoPrintOptions(),
    onDismiss: () -> Unit,
    onOptionsChanged: (PromoPrintOptions) -> Unit = {},
    onPrint: (PromoPrintOptions) -> Unit,
    onShare: (PromoPrintOptions) -> Unit
) {
    var options by remember { mutableStateOf(initialOptions) }
    var currentItemIndex by remember { mutableStateOf(0) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Preview, 1: Layout & Style Settings

    val activeItem = if (items.isNotEmpty()) items[currentItemIndex.coerceIn(0, items.size - 1)] else null
    val effectiveCurrency = options.customCurrencySymbol.ifBlank { settings.currencySymbol.ifBlank { "Rs." } }
    val effectiveStoreName = options.customStoreName.ifBlank { settings.storeName.ifBlank { "SENTRY STORE" } }

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
                .testTag("dialog_promo_preview")
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header
                Surface(
                    color = Navy900,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = Emerald400,
                                modifier = Modifier.size(24.dp).padding(end = 8.dp)
                            )
                            Column {
                                Text(
                                    text = "Promotional Material & Shelf Signs",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "${items.size} Product(s) Selected • Permanent Master Barcode",
                                    color = Slate300,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                // Tab Switcher between Preview and Settings
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Slate100,
                    contentColor = Navy900
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Live Preview", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Design & Format", fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                // Main Content Body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(Slate50)
                        .padding(12.dp)
                ) {
                    if (selectedTab == 0) {
                        // Live Visual Preview
                        if (activeItem != null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Multi-item pager if more than 1 item
                                if (items.size > 1) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(
                                            onClick = { if (currentItemIndex > 0) currentItemIndex-- },
                                            enabled = currentItemIndex > 0
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = "Previous")
                                        }

                                        Text(
                                            text = "Item ${currentItemIndex + 1} of ${items.size}: ${activeItem.product.name}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Slate700,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.Center
                                        )

                                        IconButton(
                                            onClick = { if (currentItemIndex < items.size - 1) currentItemIndex++ },
                                            enabled = currentItemIndex < items.size - 1
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = "Next")
                                        }
                                    }
                                }

                                // Interactive Promo Material Card Preview
                                PromoCardVisualPreview(
                                    item = activeItem,
                                    options = options,
                                    currencySymbol = effectiveCurrency,
                                    storeName = effectiveStoreName
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Quick Format info banner
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Blue50,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = Blue700, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Format: ${options.materialType.displayName}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Blue700
                                            )
                                            Text(
                                                text = options.materialType.description,
                                                fontSize = 11.sp,
                                                color = Blue600
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No items selected for promotion", color = Slate500)
                            }
                        }
                    } else {
                        // Settings & Customization Tab
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // 1. Promotional Material Format Selector
                            Text("1. Select Format & Size", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Navy900)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                PromoMaterialType.entries.forEach { format ->
                                    val isSelected = options.materialType == format
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) Blue50 else Color.White
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) Blue600 else Slate200
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                options = options.copy(materialType = format)
                                                onOptionsChanged(options)
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = isSelected,
                                                onClick = {
                                                    options = options.copy(materialType = format)
                                                    onOptionsChanged(options)
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(format.displayName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Navy900)
                                                Text(format.description, fontSize = 11.sp, color = Slate500)
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = Slate200)

                            // 2. Promotional Theme
                            Text("2. Promotional Theme & Badge", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Navy900)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(PromoTheme.entries) { theme ->
                                    val isThemeSelected = options.theme == theme
                                    FilterChip(
                                        selected = isThemeSelected,
                                        onClick = {
                                            options = options.copy(theme = theme)
                                            onOptionsChanged(options)
                                        },
                                        label = { Text(theme.title, fontWeight = FontWeight.SemiBold) },
                                        leadingIcon = {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .background(
                                                        color = try {
                                                            Color(android.graphics.Color.parseColor(theme.primaryColorHex))
                                                        } catch (_: Exception) { Color.Red },
                                                        shape = CircleShape
                                                    )
                                            )
                                        }
                                    )
                                }
                            }

                            // Custom Badge Text Field
                            OutlinedTextField(
                                value = options.customBadgeText,
                                onValueChange = {
                                    options = options.copy(customBadgeText = it)
                                    onOptionsChanged(options)
                                },
                                label = { Text("Custom Badge Text (Optional)") },
                                placeholder = { Text("e.g. SPECIAL OFFER, CLEARANCE SALE") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            HorizontalDivider(color = Slate200)

                            // 3. Price Display Options
                            Text("3. Price Display Options", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Navy900)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Show Was/Regular Price (Strikethrough)", fontSize = 13.sp)
                                Switch(
                                    checked = options.showRegularPrice,
                                    onCheckedChange = {
                                        options = options.copy(showRegularPrice = it)
                                        onOptionsChanged(options)
                                    }
                                )
                            }

                            if (options.showRegularPrice) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Display Discount: ${options.discountPercent}%", fontSize = 12.sp, color = Slate600)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Slider(
                                        value = options.discountPercent.toFloat(),
                                        onValueChange = {
                                            options = options.copy(discountPercent = it.toInt())
                                            onOptionsChanged(options)
                                        },
                                        valueRange = 5f..50f,
                                        steps = 8,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            HorizontalDivider(color = Slate200)

                            // 4. Content Elements
                            Text("4. Content & Barcode Options", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Navy900)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Print Master Barcode on Material", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Switch(
                                    checked = options.showMasterBarcode,
                                    onCheckedChange = {
                                        options = options.copy(showMasterBarcode = it)
                                        onOptionsChanged(options)
                                    }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Show Numeric Barcode Text", fontSize = 13.sp)
                                Switch(
                                    checked = options.showBarcodeNumber,
                                    onCheckedChange = {
                                        options = options.copy(showBarcodeNumber = it)
                                        onOptionsChanged(options)
                                    }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Show Store Name & Branding", fontSize = 13.sp)
                                Switch(
                                    checked = options.showStoreName,
                                    onCheckedChange = {
                                        options = options.copy(showStoreName = it)
                                        onOptionsChanged(options)
                                    }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Show Category & Unit", fontSize = 13.sp)
                                Switch(
                                    checked = options.showCategory,
                                    onCheckedChange = {
                                        options = options.copy(showCategory = it)
                                        onOptionsChanged(options)
                                    }
                                )
                            }
                        }
                    }
                }

                // Bottom Action Bar
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }

                        OutlinedButton(
                            onClick = { onShare(options) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Blue700),
                            modifier = Modifier.weight(1.1f).testTag("btn_share_promo_pdf")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share PDF")
                        }

                        Button(
                            onClick = { onPrint(options) },
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                            modifier = Modifier.weight(1.3f).testTag("btn_print_promo_material")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Print (${items.size})", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * High-fidelity visual preview of a Promotional Material card
 */
@Composable
fun PromoCardVisualPreview(
    item: ProductLabelItem,
    options: PromoPrintOptions,
    currencySymbol: String,
    storeName: String
) {
    val themeColor = try {
        Color(android.graphics.Color.parseColor(options.theme.primaryColorHex))
    } catch (_: Exception) {
        Color(0xFFDC2626)
    }

    val masterBarcode = item.customBarcode.ifBlank { item.product.barcode }.trim()
    val barcodeBitmap = remember(masterBarcode) {
        if (masterBarcode.isNotBlank()) {
            BarcodeGenerator.generateBarcodeBitmap(
                content = masterBarcode,
                type = BarcodeGenerator.detectBarcodeType(masterBarcode),
                width = 460,
                height = 110
            )
        } else null
    }

    val salePrice = item.product.salePrice
    val discountRate = max(options.discountPercent, 5)
    val originalPrice = salePrice * (1.0 + discountRate / 100.0)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, themeColor.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .testTag("promo_card_preview")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Promotional Banner Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(themeColor)
                    .padding(vertical = 10.dp, horizontal = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = options.customBadgeText.ifBlank { options.theme.defaultBadge },
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
            }

            // Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Store Name
                if (options.showStoreName) {
                    Text(
                        text = storeName.uppercase(Locale.US),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate500,
                        letterSpacing = 1.5.sp
                    )
                }

                // Product Title
                Text(
                    text = item.product.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Navy900,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Category & Unit Subtitle
                if (options.showCategory || options.showUnit) {
                    val sub = buildList {
                        if (options.showCategory && item.product.category.isNotBlank()) add(item.product.category)
                        if (options.showUnit && item.product.unit.isNotBlank()) add("Unit: ${item.product.unit}")
                    }.joinToString(" • ")

                    if (sub.isNotBlank()) {
                        Text(
                            text = sub,
                            fontSize = 12.sp,
                            color = Slate600
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Price Section
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (options.showRegularPrice) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Was $currencySymbol ${if (originalPrice % 1.0 == 0.0) originalPrice.toLong() else String.format(Locale.US, "%.1f", originalPrice)}",
                                fontSize = 13.sp,
                                color = Color.Red,
                                textDecoration = TextDecoration.LineThrough
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFFEE2E2)
                            ) {
                                Text(
                                    text = "SAVE ${options.discountPercent}%",
                                    color = Color(0xFFB91C1C),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "$currencySymbol ${if (salePrice % 1.0 == 0.0) salePrice.toLong() else salePrice}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = themeColor
                    )
                }

                // MASTER BARCODE
                if (options.showMasterBarcode && masterBarcode.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate50),
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "MASTER BARCODE • SCAN AT CHECKOUT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate500,
                                letterSpacing = 0.5.sp
                            )

                            if (barcodeBitmap != null) {
                                Image(
                                    bitmap = barcodeBitmap.asImageBitmap(),
                                    contentDescription = "Master barcode image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(55.dp)
                                )
                            }

                            if (options.showBarcodeNumber) {
                                Text(
                                    text = masterBarcode,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Navy900,
                                    letterSpacing = 1.5.sp
                                )
                            }
                        }
                    }
                }

                // Footer
                if (options.footerText.isNotBlank()) {
                    Text(
                        text = options.footerText,
                        fontSize = 10.sp,
                        color = Slate400,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
