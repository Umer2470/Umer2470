package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.data.api.network.ConnectionState
import com.example.data.api.security.AppActivationManager
import com.example.ui.components.AnalyticsChartsWidget
import com.example.ui.components.KpiCard
import com.example.ui.components.SectionHeader
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
        QuickActionItem("SaaS Master", Icons.Default.AdminPanelSettings, Gold500, "master_saas"),
        QuickActionItem("Dev Panel", Icons.Default.DeveloperMode, Navy700, "dev_panel"),
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Drawer Header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Navy900)
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    color = Gold500,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storefront,
                                        contentDescription = null,
                                        tint = Navy900,
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .size(28.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = storeSettings?.storeName ?: "CH UMER POS",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Tel: 03080018035",
                                        fontSize = 12.sp,
                                        color = Gold400
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Current User & Role
                            Surface(
                                color = Navy800,
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
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(
                                            imageVector = Icons.Default.AccountCircle,
                                            contentDescription = null,
                                            tint = Gold400,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = activeUser?.fullName ?: "Super Administrator",
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Surface(
                                        color = Gold500,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = activeUser?.role ?: "SUPER_ADMIN",
                                            fontSize = 10.sp,
                                            color = Navy900,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Drawer Items
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // 1. STORE & BUSINESS
                        DrawerSectionHeading(
                            title = "STORE & BUSINESS",
                            badgeColor = Blue600
                        )

                        DrawerItemRow(
                            icon = Icons.Default.Store,
                            iconTint = Blue600,
                            iconBg = Blue50,
                            title = "Store Information & Profile",
                            subtitle = "Name, contact, NTN & tax settings",
                            testTag = "drawer_item_store_info",
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onNavigate("setup")
                            }
                        )

                        DrawerItemRow(
                            icon = Icons.Default.Business,
                            iconTint = Blue600,
                            iconBg = Blue50,
                            title = "Business & Branch Management",
                            subtitle = "Multi-branch & store identity",
                            testTag = "drawer_item_business_management",
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onNavigate("store_management")
                            }
                        )

                        DrawerItemRow(
                            icon = Icons.Default.Security,
                            iconTint = Gold600,
                            iconBg = Gold50,
                            title = "Store Access & Privileges",
                            subtitle = "Granular cashier & manager access",
                            testTag = "drawer_item_store_access",
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onNavigate("access_management")
                            }
                        )

                        Divider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // 2. MANAGEMENT & OPERATIONS
                        DrawerSectionHeading(
                            title = "MANAGEMENT & OPERATIONS",
                            badgeColor = Emerald600
                        )

                        DrawerItemRow(
                            icon = Icons.Default.PointOfSale,
                            iconTint = Blue600,
                            iconBg = Blue50,
                            title = "Sales POS Terminal",
                            subtitle = "Fast barcode & manual checkout",
                            testTag = "drawer_item_pos",
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onNavigate("pos")
                            }
                        )

                        DrawerItemRow(
                            icon = Icons.Default.ReceiptLong,
                            iconTint = Purple600,
                            iconBg = Purple100.copy(alpha = 0.5f),
                            title = "Invoices & Receipts",
                            subtitle = "Sales history, search & thermal print",
                            testTag = "drawer_item_invoices",
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onNavigate("invoice")
                            }
                        )

                        DrawerItemRow(
                            icon = Icons.Default.Inventory2,
                            iconTint = Emerald600,
                            iconBg = Emerald50,
                            title = "Inventory & Products",
                            subtitle = "Stock tracking, SKU & pricing",
                            testTag = "drawer_item_inventory",
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onNavigate("inventory")
                            }
                        )

                        DrawerItemRow(
                            icon = Icons.Default.ShoppingBag,
                            iconTint = Amber600,
                            iconBg = Amber100.copy(alpha = 0.5f),
                            title = "Purchases & Stock In",
                            subtitle = "Supplier bills & inventory receipts",
                            testTag = "drawer_item_purchases",
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onNavigate("purchases")
                            }
                        )

                        DrawerItemRow(
                            icon = Icons.Default.People,
                            iconTint = Purple600,
                            iconBg = Purple100.copy(alpha = 0.5f),
                            title = "Customer Ledgers",
                            subtitle = "Khata balances & payment records",
                            testTag = "drawer_item_customers",
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onNavigate("customers")
                            }
                        )

                        DrawerItemRow(
                            icon = Icons.Default.LocalShipping,
                            iconTint = Blue600,
                            iconBg = Blue50,
                            title = "Supplier Accounts",
                            subtitle = "Vendor balances & purchase orders",
                            testTag = "drawer_item_suppliers",
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onNavigate("suppliers")
                            }
                        )

                        DrawerItemRow(
                            icon = Icons.Default.Badge,
                            iconTint = Emerald600,
                            iconBg = Emerald50,
                            title = "Staff Attendance",
                            subtitle = "Check-in/out, logs & shifts",
                            testTag = "drawer_item_attendance",
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onNavigate("attendance")
                            }
                        )

                        DrawerItemRow(
                            icon = Icons.Default.Assessment,
                            iconTint = Rose600,
                            iconBg = Rose100.copy(alpha = 0.5f),
                            title = "Financial Reports & P&L",
                            subtitle = "Sales revenue, profit & analytics",
                            testTag = "drawer_item_reports",
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onNavigate("reports")
                            }
                        )

                        DrawerItemRow(
                            icon = Icons.Default.LockClock,
                            iconTint = BentoIndigo,
                            iconBg = BentoIndigoLight.copy(alpha = 0.5f),
                            title = "Daily Cash Closing",
                            subtitle = "Drawer balancing & session summary",
                            testTag = "drawer_item_closing",
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onNavigate("closing")
                            }
                        )

                        Divider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // 3. ADMINISTRATION
                        DrawerSectionHeading(
                            title = "ADMINISTRATION",
                            badgeColor = Amber600
                        )

                        DrawerItemRow(
                            icon = Icons.Default.ManageAccounts,
                            iconTint = Blue600,
                            iconBg = Blue50,
                            title = "Staff & User Roles",
                            subtitle = "Cashiers, managers & credentials",
                            testTag = "drawer_item_staff_users",
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onNavigate("users")
                            }
                        )

                        DrawerItemRow(
                            icon = Icons.Default.AdminPanelSettings,
                            iconTint = Gold600,
                            iconBg = Gold50,
                            title = "SaaS Master Control",
                            subtitle = "Store activation & instance locks",
                            testTag = "drawer_item_saas_master",
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onNavigate("master_saas")
                            }
                        )

                        DrawerItemRow(
                            icon = Icons.Default.VpnKey,
                            iconTint = Emerald600,
                            iconBg = Emerald50,
                            title = "License Activation",
                            subtitle = "Hardware key & license verification",
                            testTag = "drawer_item_activation",
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onNavigate("activation")
                            }
                        )

                        DrawerItemRow(
                            icon = Icons.Default.DeveloperMode,
                            iconTint = BentoIndigo,
                            iconBg = BentoIndigoLight.copy(alpha = 0.5f),
                            title = "Developer & Diagnostics",
                            subtitle = "Room DB integrity & device logs",
                            testTag = "drawer_item_dev_panel",
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onNavigate("dev_panel")
                            }
                        )

                        DrawerItemRow(
                            icon = Icons.Default.History,
                            iconTint = Teal600,
                            iconBg = Teal50,
                            title = "Audit Activity Logs",
                            subtitle = "Immutable security actions log",
                            testTag = "drawer_item_logs",
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onNavigate("logs")
                            }
                        )

                        DrawerItemRow(
                            icon = Icons.Default.DeleteSweep,
                            iconTint = Rose600,
                            iconBg = Rose100.copy(alpha = 0.5f),
                            title = "Recycle Bin",
                            subtitle = "Soft-deleted records recovery",
                            testTag = "drawer_item_recycle_bin",
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onNavigate("recycle_bin")
                            }
                        )

                        Divider(
                            modifier = Modifier.padding(vertical = 6.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // 4. SETTINGS & CUSTOMIZATION
                        DrawerSectionHeading(
                            title = "SETTINGS & CUSTOMIZATION",
                            badgeColor = Purple600
                        )

                        DrawerItemRow(
                            icon = Icons.Default.Palette,
                            iconTint = Purple600,
                            iconBg = Purple100.copy(alpha = 0.5f),
                            title = "Appearance & Theme",
                            subtitle = "Light, Dark, Black & Accent colors",
                            testTag = "drawer_item_appearance",
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onNavigate("settings")
                            }
                        )

                        DrawerItemRow(
                            icon = Icons.Default.TextFields,
                            iconTint = Blue600,
                            iconBg = Blue50,
                            title = "Text & Font Scaling",
                            subtitle = "Adjust size (85%-130%) & fonts",
                            testTag = "drawer_item_text_font",
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onNavigate("settings")
                            }
                        )

                        DrawerItemRow(
                            icon = Icons.Default.Settings,
                            iconTint = Navy800,
                            iconBg = Slate100,
                            title = "Store & General Settings",
                            subtitle = "Backup, receipts & emergency wipe",
                            testTag = "drawer_item_settings",
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                onNavigate("settings")
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Offline Status Footer
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Emerald50,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Emerald300)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Emerald700,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Offline Mode: 100% Ready",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald900
                                )
                                Text(
                                    text = "All POS data is safely saved locally",
                                    fontSize = 10.sp,
                                    color = Emerald800
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = storeSettings?.storeName ?: "CH UMER POS.03080018035",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Tel: 03080018035 • ",
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

                Spacer(modifier = Modifier.height(16.dp))

                // Financial KPIs
                SectionHeader(title = "Business Overview", subtitle = "Real-time ledger summary")

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

                Spacer(modifier = Modifier.height(16.dp))

                // Daily Sales Trend & Category Analytics Chart
                AnalyticsChartsWidget(
                    salesTrend = salesTrend,
                    topCategories = topCategories,
                    currencySymbol = currency
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Quick Actions Hub
                SectionHeader(title = "Modules & Management", subtitle = "Select module to operate")

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

