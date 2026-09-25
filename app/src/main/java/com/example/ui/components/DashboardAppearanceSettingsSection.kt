package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.entity.StoreSettings
import com.example.ui.theme.*
import com.example.util.BrandingImageHelper
import java.io.File

@Composable
fun DashboardAppearanceSettingsSection(
    storeSettings: StoreSettings?,
    onSaveSettings: (StoreSettings) -> Unit
) {
    val context = LocalContext.current

    var stagedBannerUri by remember(storeSettings?.dashboardBannerUri) {
        mutableStateOf(storeSettings?.dashboardBannerUri)
    }
    var stagedSmallImageUri by remember(storeSettings?.dashboardSmallImageUri) {
        mutableStateOf(storeSettings?.dashboardSmallImageUri)
    }
    var stagedBgColor by remember(storeSettings?.dashboardBannerBgColor) {
        mutableStateOf(storeSettings?.dashboardBannerBgColor ?: "Navy")
    }
    var stagedShowText by remember(storeSettings?.showDashboardBannerText) {
        mutableStateOf(storeSettings?.showDashboardBannerText ?: true)
    }
    var stagedShowSmallImg by remember(storeSettings?.showDashboardSmallImage) {
        mutableStateOf(storeSettings?.showDashboardSmallImage ?: true)
    }
    var stagedHeading by remember(storeSettings?.dashboardBannerHeading) {
        mutableStateOf(storeSettings?.dashboardBannerHeading ?: "")
    }
    var stagedSubtitle by remember(storeSettings?.dashboardBannerSubtitle) {
        mutableStateOf(storeSettings?.dashboardBannerSubtitle ?: "")
    }
    var stagedDescription by remember(storeSettings?.dashboardBannerDescription) {
        mutableStateOf(storeSettings?.dashboardBannerDescription ?: "")
    }
    var stagedActionText by remember(storeSettings?.dashboardBannerActionText) {
        mutableStateOf(storeSettings?.dashboardBannerActionText ?: "High-Speed Billing & Inventory")
    }

    var customHexInput by remember { mutableStateOf("") }
    var showCustomHexField by remember { mutableStateOf(false) }
    var showSavedMessage by remember { mutableStateOf(false) }

    // Banner Image Picker Launcher
    val bannerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = BrandingImageHelper.saveCustomBannerFromUri(context, uri, stagedBannerUri)
            if (savedPath != null) {
                stagedBannerUri = savedPath
            }
        }
    }

    // Small Foreground Image Picker Launcher
    val smallImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = BrandingImageHelper.saveCustomSmallImageFromUri(context, uri, stagedSmallImageUri)
            if (savedPath != null) {
                stagedSmallImageUri = savedPath
            }
        }
    }

    // Resolve parsed background Color
    val resolvedBgColor = remember(stagedBgColor) {
        when (stagedBgColor.lowercase().trim()) {
            "black" -> Color.Black
            "white" -> Color.White
            "light gray", "gray", "lightgray" -> Color(0xFFF1F5F9)
            "", "navy", "dark navy" -> Navy900
            else -> try {
                Color(android.graphics.Color.parseColor(stagedBgColor))
            } catch (e: Exception) {
                Navy900
            }
        }
    }

    // Resolve banner bitmap
    val liveBannerBitmap = remember(stagedBannerUri) {
        if (!stagedBannerUri.isNullOrBlank()) {
            try {
                val f = File(stagedBannerUri!!)
                if (f.exists() && f.length() > 0) {
                    BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap()
                } else null
            } catch (e: Exception) {
                null
            }
        } else null
    }

    // Resolve small image bitmap
    val liveSmallBitmap = remember(stagedSmallImageUri) {
        if (!stagedSmallImageUri.isNullOrBlank()) {
            try {
                val f = File(stagedSmallImageUri!!)
                if (f.exists() && f.length() > 0) {
                    BitmapFactory.decodeFile(f.absolutePath)?.asImageBitmap()
                } else null
            } catch (e: Exception) {
                null
            }
        } else null
    }

    SectionHeader(
        title = "Dashboard Appearance",
        subtitle = "Dashboard Banner & Images Customization"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dashboard_appearance_settings_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Header & description
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Palette,
                    contentDescription = null,
                    tint = Navy900,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "Dashboard Banner & Promotional Showcase",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Navy900
                )
            }

            Text(
                text = "Customize the promotional showcase card displayed on the POS Dashboard. Upload shop banners, change the foreground badge, choose background colors, or toggle text overlay.",
                fontSize = 12.sp,
                color = Slate600,
                lineHeight = 16.sp
            )

            // ==========================================
            // LIVE BANNER PREVIEW
            // ==========================================
            Text(
                text = "Live Real-Time Banner Preview",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Navy900
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_banner_live_preview_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = resolvedBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(resolvedBgColor)
                    ) {
                        // Banner background image
                        if (liveBannerBitmap != null) {
                            Image(
                                bitmap = liveBannerBitmap,
                                contentDescription = "Dashboard Custom Banner Preview",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.sentry_store_banner_1787989285469),
                                contentDescription = "Default SENTRY STORE Banner",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                            )
                        }

                        // Gradient overlay for readability when text is shown
                        if (stagedShowText) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                resolvedBgColor.copy(alpha = 0.65f),
                                                resolvedBgColor.copy(alpha = 0.95f)
                                            )
                                        )
                                    )
                            )
                        }

                        // Badges & Tagline overlay
                        if (stagedShowText || stagedShowSmallImg) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                if (stagedShowSmallImg) {
                                    if (liveSmallBitmap != null) {
                                        Image(
                                            bitmap = liveSmallBitmap,
                                            contentDescription = "Custom Small Brand Image",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.5.dp, Gold400, RoundedCornerShape(8.dp))
                                        )
                                    } else if (!storeSettings?.logoUri.isNullOrBlank()) {
                                        ShopLogoAvatar(
                                            logoUri = storeSettings?.logoUri,
                                            size = 46.dp,
                                            shape = RoundedCornerShape(8.dp),
                                            borderColor = Gold400,
                                            borderWidth = 1.5.dp
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(id = R.drawable.sentry_store_logo_1787989266987),
                                            contentDescription = "Store Logo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.5.dp, Gold400, RoundedCornerShape(8.dp))
                                        )
                                    }
                                }

                                if (stagedShowText) {
                                    val heading = stagedHeading.ifBlank {
                                        storeSettings?.posBrandName?.ifBlank { "SENTRY STORE POS" } ?: "SENTRY STORE POS"
                                    }
                                    val sub = stagedSubtitle.ifBlank {
                                        storeSettings?.tagline?.ifBlank { "Professional Retail" } ?: "Professional Retail"
                                    }
                                    val desc = stagedDescription.ifBlank {
                                        storeSettings?.brandDescription?.ifBlank { "Hardware, Paint & Multi-Category Retail POS" }
                                            ?: "Hardware, Paint & Multi-Category Retail POS"
                                    }

                                    Column {
                                        Surface(
                                            color = Gold500,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "$heading • $sub",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp,
                                                color = Navy900,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = desc,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (resolvedBgColor == Color.White) Navy900 else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Action shortcut row under banner
                    val isLightBg = resolvedBgColor == Color.White || resolvedBgColor == Color(0xFFF1F5F9)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(resolvedBgColor)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stagedActionText.ifBlank { "High-Speed Billing & Inventory" },
                            fontSize = 11.5.sp,
                            color = if (isLightBg) Navy900 else Slate300,
                            fontWeight = FontWeight.Medium
                        )

                        Surface(
                            color = Gold500,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PointOfSale, contentDescription = null, tint = Navy900, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open POS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Navy900)
                            }
                        }
                    }
                }
            }

            Divider(color = Slate200)

            // ==========================================
            // 1. MAIN BACKGROUND / BANNER IMAGE
            // ==========================================
            Text(
                text = "1. Main Background / Banner Image",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Navy900
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Banner Thumbnail
                Box(
                    modifier = Modifier
                        .size(width = 90.dp, height = 56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.5.dp, if (stagedBannerUri != null) Emerald500 else Gold500, RoundedCornerShape(8.dp))
                ) {
                    if (liveBannerBitmap != null) {
                        Image(
                            bitmap = liveBannerBitmap,
                            contentDescription = "Current Banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.sentry_store_banner_1787989285469),
                            contentDescription = "Default SENTRY STORE Banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = if (stagedBannerUri != null) Emerald100 else Slate100,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (stagedBannerUri != null) "✓ Custom Banner Active" else "Default SENTRY STORE Banner Active",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (stagedBannerUri != null) Emerald800 else Slate700,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { bannerPickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("change_main_banner_button")
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (stagedBannerUri != null) "Replace Banner Image" else "Change Main Banner", fontSize = 11.sp)
                        }

                        if (stagedBannerUri != null) {
                            OutlinedButton(
                                onClick = {
                                    BrandingImageHelper.deleteOldBanner(stagedBannerUri)
                                    stagedBannerUri = null
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose600),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("remove_banner_image_button")
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Remove / Restore Default", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Divider(color = Slate200)

            // ==========================================
            // 2. SMALL FOREGROUND IMAGE
            // ==========================================
            Text(
                text = "2. Small Foreground Image / Badge",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Navy900
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Small image Thumbnail
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.5.dp, if (stagedSmallImageUri != null) Emerald500 else Gold500, RoundedCornerShape(8.dp))
                ) {
                    if (liveSmallBitmap != null) {
                        Image(
                            bitmap = liveSmallBitmap,
                            contentDescription = "Current Small Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (!storeSettings?.logoUri.isNullOrBlank()) {
                        ShopLogoAvatar(
                            logoUri = storeSettings?.logoUri,
                            size = 52.dp,
                            shape = RoundedCornerShape(8.dp)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.sentry_store_logo_1787989266987),
                            contentDescription = "Default Small Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = if (stagedSmallImageUri != null) Emerald100 else Slate100,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (stagedSmallImageUri != null) "✓ Custom Small Image" else "Default Store Logo / Badge",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (stagedSmallImageUri != null) Emerald800 else Slate700,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { smallImagePickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("change_small_image_button")
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (stagedSmallImageUri != null) "Replace Small Image" else "Change Small Image", fontSize = 11.sp)
                        }

                        if (stagedSmallImageUri != null) {
                            OutlinedButton(
                                onClick = {
                                    BrandingImageHelper.deleteOldSmallImage(stagedSmallImageUri)
                                    stagedSmallImageUri = null
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose600),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("remove_small_image_button")
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Remove / Restore Default", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Display Small Image on Banner",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Navy900
                    )
                    Text(
                        text = "Show or hide the square badge in front of the banner",
                        fontSize = 11.sp,
                        color = Slate500
                    )
                }
                Switch(
                    checked = stagedShowSmallImg,
                    onCheckedChange = { stagedShowSmallImg = it },
                    modifier = Modifier.testTag("toggle_small_image_switch")
                )
            }

            Divider(color = Slate200)

            // ==========================================
            // 3. DARK / BLACK BACKGROUND CUSTOMIZATION
            // ==========================================
            Text(
                text = "3. Banner Background Color",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Navy900
            )

            Text(
                text = "Applies only to the main promotional/banner card and its action row:",
                fontSize = 12.sp,
                color = Slate600
            )

            val colorOptions = listOf(
                Triple("Dark Navy", "Navy", Navy900),
                Triple("Black", "Black", Color.Black),
                Triple("White", "White", Color.White),
                Triple("Light Gray", "Light Gray", Color(0xFFF1F5F9))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                colorOptions.forEach { (label, key, color) ->
                    val isSelected = stagedBgColor.equals(key, ignoreCase = true)
                    Surface(
                        color = color,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) Gold500 else Slate300
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clickable {
                                stagedBgColor = key
                                showCustomHexField = false
                            }
                            .testTag("banner_bg_color_$key")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            val textColor = if (color == Color.White || color == Color(0xFFF1F5F9)) Navy900 else Color.White
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Gold500, modifier = Modifier.size(14.dp))
                                }
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }

            // Custom color button & field
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = { showCustomHexField = !showCustomHexField }
                ) {
                    Icon(Icons.Default.ColorLens, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (showCustomHexField) "Hide Custom Hex" else "Custom Hex Color (#RRGGBB)", fontSize = 12.sp)
                }

                if (stagedBgColor !in listOf("Navy", "Black", "White", "Light Gray")) {
                    Surface(
                        color = resolvedBgColor,
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Gold500),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stagedBgColor,
                            color = if (resolvedBgColor == Color.White) Navy900 else Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (showCustomHexField) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customHexInput,
                        onValueChange = { customHexInput = it },
                        label = { Text("Custom Hex Color") },
                        placeholder = { Text("#0F172A") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            val cleanHex = if (customHexInput.startsWith("#")) customHexInput else "#$customHexInput"
                            try {
                                android.graphics.Color.parseColor(cleanHex)
                                stagedBgColor = cleanHex
                            } catch (e: Exception) {
                                // Invalid hex, ignore
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Apply Hex")
                    }
                }
            }

            Divider(color = Slate200)

            // ==========================================
            // 4. BANNER TEXT CUSTOMIZATION
            // ==========================================
            Text(
                text = "4. Banner Text Customization",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Navy900
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Show Separately Rendered Banner Text",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Navy900
                    )
                    Text(
                        text = "Turn off if your custom uploaded banner already contains its own embedded text & graphics",
                        fontSize = 11.sp,
                        color = Slate500,
                        lineHeight = 15.sp
                    )
                }
                Switch(
                    checked = stagedShowText,
                    onCheckedChange = { stagedShowText = it },
                    modifier = Modifier.testTag("toggle_banner_text_switch")
                )
            }

            if (stagedShowText) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = stagedHeading,
                        onValueChange = { stagedHeading = it },
                        label = { Text("Main Heading") },
                        placeholder = { Text(storeSettings?.posBrandName?.ifBlank { "SENTRY STORE POS" } ?: "SENTRY STORE POS") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("banner_main_heading_field")
                    )

                    OutlinedTextField(
                        value = stagedSubtitle,
                        onValueChange = { stagedSubtitle = it },
                        label = { Text("Subtitle / Tagline") },
                        placeholder = { Text(storeSettings?.tagline?.ifBlank { "Professional Retail" } ?: "Professional Retail") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("banner_subtitle_field")
                    )

                    OutlinedTextField(
                        value = stagedDescription,
                        onValueChange = { stagedDescription = it },
                        label = { Text("Description") },
                        placeholder = { Text(storeSettings?.brandDescription?.ifBlank { "Hardware, Paint & Multi-Category Retail POS" } ?: "Hardware, Paint & Multi-Category Retail POS") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("banner_description_field")
                    )

                    OutlinedTextField(
                        value = stagedActionText,
                        onValueChange = { stagedActionText = it },
                        label = { Text("Action Bar Promotional Text") },
                        placeholder = { Text("High-Speed Billing & Inventory") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("banner_action_text_field")
                    )
                }
            }

            Divider(color = Slate200)

            // ==========================================
            // ACTION BUTTONS: SAVE & RESTORE
            // ==========================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val current = storeSettings ?: StoreSettings()
                        val updated = current.copy(
                            dashboardBannerUri = stagedBannerUri,
                            dashboardSmallImageUri = stagedSmallImageUri,
                            dashboardBannerBgColor = stagedBgColor,
                            showDashboardBannerText = stagedShowText,
                            showDashboardSmallImage = stagedShowSmallImg,
                            dashboardBannerHeading = stagedHeading,
                            dashboardBannerSubtitle = stagedSubtitle,
                            dashboardBannerDescription = stagedDescription,
                            dashboardBannerActionText = stagedActionText
                        )
                        onSaveSettings(updated)
                        showSavedMessage = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(44.dp)
                        .testTag("save_dashboard_banner_settings_button")
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        BrandingImageHelper.deleteOldBanner(stagedBannerUri)
                        BrandingImageHelper.deleteOldSmallImage(stagedSmallImageUri)
                        stagedBannerUri = null
                        stagedSmallImageUri = null
                        stagedBgColor = "Navy"
                        stagedShowText = true
                        stagedShowSmallImg = true
                        stagedHeading = ""
                        stagedSubtitle = ""
                        stagedDescription = ""
                        stagedActionText = "High-Speed Billing & Inventory"

                        val current = storeSettings ?: StoreSettings()
                        val updated = current.copy(
                            dashboardBannerUri = null,
                            dashboardSmallImageUri = null,
                            dashboardBannerBgColor = "Navy",
                            showDashboardBannerText = true,
                            showDashboardSmallImage = true,
                            dashboardBannerHeading = "",
                            dashboardBannerSubtitle = "",
                            dashboardBannerDescription = "",
                            dashboardBannerActionText = "High-Speed Billing & Inventory"
                        )
                        onSaveSettings(updated)
                        showSavedMessage = true
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Rose600),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("restore_dashboard_banner_defaults_button")
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Restore Defaults", fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp)
                }
            }

            if (showSavedMessage) {
                Surface(
                    color = Emerald50,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Emerald300),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald600, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Dashboard appearance settings saved to local database!",
                            color = Emerald800,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
