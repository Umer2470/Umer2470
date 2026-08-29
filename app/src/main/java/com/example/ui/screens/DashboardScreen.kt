package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.R
import com.example.data.api.network.ConnectionState
import com.example.data.api.security.AppActivationManager
import com.example.ui.components.AnalyticsChartsWidget
import com.example.ui.components.KpiCard
import com.example.ui.components.LiveClockBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.ShopLogoAvatar
import com.example.ui.components.SidebarNavigationDrawerContent
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel

data class QuickActionItem(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: StoreViewModel,
    onNavigate: (String) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val sales by viewModel.sales.collectAsState()
    val products by viewModel.products.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val storeSettings by viewModel.storeSettings.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val activationState by viewModel.activationState.collectAsState()
    val salesTrend by viewModel.salesTrend.collectAsState()
    val topCategories by viewModel.topCategories.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val allUsers by viewModel.users.collectAsState()

    val currency = storeSettings?.currencySymbol ?: "Rs"
    val totalRevenue = remember(sales) { sales.sumOf { it.netAmount } }
    val totalDue = remember(sales) { sales.sumOf { it.dueAmount } }
    val lowStockCount = remember(products) { products.count { it.stockQuantity <= it.minStockAlert } }

    val quickActions = listOf(
        QuickActionItem("Sales POS", Icons.Default.PointOfSale, Navy900, "pos"),
        QuickActionItem("Invoices", Icons.Default.ReceiptLong, Blue600, "invoice"),
        QuickActionItem("Inventory", Icons.Default.Inventory2, Emerald600, "inventory"),
        QuickActionItem("Purchases", Icons.Default.ShoppingBag, Gold600, "purchases"),
        QuickActionItem("Customers", Icons.Default.People, Purple600, "customers"),
        QuickActionItem("Suppliers", Icons.Default.LocalShipping, Navy600, "suppliers"),
        QuickActionItem("Reports", Icons.Default.Assessment, Rose600, "reports"),
        QuickActionItem("Attendance", Icons.Default.Badge, Emerald500, "attendance"),
        QuickActionItem("Daily Closing", Icons.Default.LockClock, Navy800, "closing"),
        QuickActionItem("Staff & Users", Icons.Default.ManageAccounts, Purple600, "users"),
        QuickActionItem("Settings", Icons.Default.Settings, Teal600, "settings"),
        QuickActionItem("Activation", Icons.Default.VpnKey, Blue600, "activation")
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
                    .testTag("main_navigation_drawer"),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                SidebarNavigationDrawerContent(
                    activeUser = activeUser,
                    storeSettings = storeSettings,
                    onNavigate = onNavigate,
                    onCloseDrawer = {
                        coroutineScope.launch { drawerState.close() }
                    }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ShopLogoAvatar(
                                logoUri = storeSettings?.logoUri,
                                size = 38.dp,
                                shape = RoundedCornerShape(8.dp),
                                borderColor = Gold400,
                                borderWidth = 1.dp
                            )
                            Column {
                                Text(
                                    text = storeSettings?.appDisplayName?.ifBlank { storeSettings?.storeName } ?: "SENTRY STORE",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = (storeSettings?.tagline ?: "Professional Retail & Business Management") + " • ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Gold400
                                    )
                                    Text(
                                        text = if (connectionState == ConnectionState.CONNECTED) "Online" else "Offline Safe",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (connectionState == ConnectionState.CONNECTED) Emerald500 else Gold400
                                    )
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { coroutineScope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("dashboard_menu_button")
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Navigation Menu", tint = Color.White)
                        }
                    },
                    actions = {
                        LiveClockBadge(compact = true)
                        IconButton(
                            onClick = { onNavigate("settings") },
                            modifier = Modifier.testTag("dashboard_settings_button")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Navy900,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            containerColor = Slate50
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // License status banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate("activation") }
                        .testTag("dashboard_license_banner"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (activationState == AppActivationManager.STATUS_ACTIVATED || activationState == AppActivationManager.STATUS_OFFLINE_ACTIVATED) Navy900 else Rose600
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (activationState == AppActivationManager.STATUS_ACTIVATED) Icons.Default.Verified else Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (activationState == AppActivationManager.STATUS_ACTIVATED) "Licensed Commercial POS" else "Activation Pending",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Device: ${viewModel.identityManager.getInstallationId()} • SentryStore.pk",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // VIP POS Commercial Showcase Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vip_pos_showcase_banner"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Navy900),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.sentry_store_banner_1787989285469),
                                contentDescription = "SENTRY STORE Commercial Solution",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                            )
                            // Elegant Dark-Gold Gradient Overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Navy900.copy(alpha = 0.65f),
                                                Navy900.copy(alpha = 0.95f)
                                            )
                                        )
                                    )
                            )
                            // Overlay Badges & Tagline
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ShopLogoAvatar(
                                    logoUri = storeSettings?.logoUri,
                                    size = 46.dp,
                                    shape = RoundedCornerShape(8.dp),
                                    borderColor = Gold400,
                                    borderWidth = 1.5.dp
                                )
                                Column {
                                    Surface(
                                        color = Gold500,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "${storeSettings?.posBrandName?.ifBlank { "SENTRY STORE POS" } ?: "SENTRY STORE POS"} • ${storeSettings?.tagline?.ifBlank { "Professional Retail" } ?: "Professional Retail"}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            color = Navy900,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = storeSettings?.brandDescription?.ifBlank { "Hardware, Paint & Multi-Category Retail POS" } ?: "Hardware, Paint & Multi-Category Retail POS",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        // Action shortcut row under banner
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Navy900)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "High-Speed Billing & Inventory",
                                fontSize = 11.5.sp,
                                color = Slate300,
                                fontWeight = FontWeight.Medium
                            )

                            Button(
                                onClick = { onNavigate("pos") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Gold500,
                                    contentColor = Navy900
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp).testTag("quick_pos_banner_btn")
                            ) {
                                Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Open POS", fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. STORE MODULE / STORE MALL (Directly below Header / License status)
                SectionHeader(
                    title = "STORE MODULE / STORE MALL",
                    subtitle = "Select store module & management terminal"
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.testTag("dashboard_store_module_section")
                ) {
                    quickActions.chunked(3).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { action ->
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { onNavigate(action.route) }
                                        .testTag("action_${action.route}"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Surface(
                                            color = action.color.copy(alpha = 0.1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = action.icon,
                                                contentDescription = action.title,
                                                tint = action.color,
                                                modifier = Modifier
                                                    .padding(8.dp)
                                                    .size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = action.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Navy900,
                                            fontSize = 11.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            if (rowItems.size < 3) {
                                Spacer(modifier = Modifier.weight(3f - rowItems.size))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 2. BUSINESS OVERVIEW (Real-time financial & inventory KPIs)
                SectionHeader(
                    title = "Business Overview",
                    subtitle = "Real-time sales & ledger summary"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        title = "Total Sales",
                        value = "$currency %.0f".format(totalRevenue),
                        icon = Icons.Default.TrendingUp,
                        color = Emerald600,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Receivables (Due)",
                        value = "$currency %.0f".format(totalDue),
                        icon = Icons.Default.AccountBalanceWallet,
                        color = Rose600,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    KpiCard(
                        title = "Total Products",
                        value = "${products.size}",
                        icon = Icons.Default.Category,
                        color = Navy900,
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        title = "Low Stock Alert",
                        value = "$lowStockCount Items",
                        icon = Icons.Default.WarningAmber,
                        color = if (lowStockCount > 0) Gold600 else Emerald600,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 3. BUSINESS ANALYTICS (Daily Sales Trend & Category Analytics Chart)
                SectionHeader(
                    title = "Business Analytics",
                    subtitle = "Sales trends & category performance"
                )

                AnalyticsChartsWidget(
                    salesTrend = salesTrend,
                    topCategories = topCategories,
                    currencySymbol = currency
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 4. RECENT SALES & ACTIVITY
                SectionHeader(
                    title = "Recent Sales & Activity",
                    subtitle = "Latest terminal transactions"
                )

                if (sales.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = Slate300,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No sales recorded yet today",
                                fontSize = 13.sp,
                                color = Navy500,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Start selling by opening Sales POS above",
                                fontSize = 11.sp,
                                color = Navy400
                            )
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("dashboard_recent_sales_list")
                    ) {
                        sales.take(5).forEach { sale ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigate("invoice") },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            color = Emerald50,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Receipt,
                                                contentDescription = null,
                                                tint = Emerald600,
                                                modifier = Modifier.padding(6.dp).size(20.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = sale.invoiceNumber,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = Navy900
                                            )
                                            Text(
                                                text = "${sale.customerName} • ${sale.paymentType}",
                                                fontSize = 11.sp,
                                                color = Navy500
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "$currency %.2f".format(sale.netAmount),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Emerald700
                                        )
                                        val formattedDate = java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(sale.createdAt))
                                        Text(
                                            text = formattedDate,
                                            fontSize = 10.sp,
                                            color = Navy400
                                        )
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

@Composable
private fun DrawerSectionHeading(
    title: String,
    badgeColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(14.dp)
                .background(badgeColor, RoundedCornerShape(2.dp))
        )
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = 0.6.sp
        )
    }
}

@Composable
private fun DrawerItemRow(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = iconBg,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 16.sp
                )
                Text(
                    text = subtitle,
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 13.sp,
                    maxLines = 1
                )
            }
        }
    }
}

