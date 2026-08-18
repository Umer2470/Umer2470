package com.example.ui.navigation

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.ContactSupport
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.BusinessSetupWizardScreen
import com.example.ui.screens.CustomerActivationScreen
import com.example.ui.screens.CustomerScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DailyClosingScreen
import com.example.ui.screens.UserManagementScreen
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.StoreAccessManagementScreen
import com.example.ui.screens.ActivityLogsScreen
import com.example.ui.screens.RecycleBinScreen
import com.example.ui.screens.MasterOwnerSaaSControlScreen
import com.example.ui.components.JoinStoreDialog
import com.example.ui.components.LicenseStatusCard
import com.example.ui.components.LicenseActivationDialog
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.PurchaseScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SalesPosScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SupplierScreen
import com.example.ui.screens.DeveloperPanelScreen
import com.example.ui.screens.StoreManagementCenterScreen
import com.example.data.api.network.ConnectionState
import com.example.data.api.network.DetailedConnectionStatus
import com.example.data.api.security.AppActivationManager
import com.example.data.api.sync.SyncState
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import com.example.ui.theme.BentoCardSlate
import com.example.ui.theme.BentoPrimary
import com.example.ui.components.ShopLogoAvatar
import com.example.ui.viewmodel.StoreViewModel

import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

data class DrawerMenuItem(
    val label: String,
    val icon: ImageVector,
    val route: String? = null,
    val onClick: (() -> Unit)? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(viewModel: StoreViewModel) {
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val isOnboardingCompleted by viewModel.isOnboardingCompleted.collectAsState()
    val isAppActivated by viewModel.isAppActivated.collectAsState()
    val activationState by viewModel.activationStateFlow.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val lowStockProducts by viewModel.lowStockProducts.collectAsState()
    val allCustomers by viewModel.allCustomers.collectAsState()
    val context = LocalContext.current

    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val allStores by viewModel.allStores.collectAsState()
    val selectedStoreId by viewModel.selectedStoreId.collectAsState()
    val activeStore by viewModel.activeStore.collectAsState()
    var showNavStoreDropdown by remember { mutableStateOf(false) }

    var showProfileDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showAppInfoDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showHelpSupportDialog by remember { mutableStateOf(false) }
    var showDeveloperAuthDialog by remember { mutableStateOf(false) }
    var showDeveloperScreenFromActivation by remember { mutableStateOf(false) }
    var devPinInput by remember { mutableStateOf("") }
    var devPinError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    if (showDeveloperScreenFromActivation) {
        DeveloperPanelScreen(
            viewModel = viewModel,
            onNavigateBack = {
                showDeveloperScreenFromActivation = false
                viewModel.refreshActivationState()
            }
        )
    } else if (!isAppActivated) {
        CustomerActivationScreen(
            viewModel = viewModel,
            onActivated = {
                viewModel.refreshActivationState()
            },
            onOpenDeveloperPortal = {
                showDeveloperScreenFromActivation = true
            }
        )
    } else if (!isOnboardingCompleted) {
        BusinessSetupWizardScreen(
            viewModel = viewModel,
            onSetupCompleted = {}
        )
    } else if (!isAuthenticated) {
        LoginScreen(viewModel = viewModel)
    } else {
        val currentUser by viewModel.currentUser.collectAsState()
        val userAllowedStores by viewModel.userAllowedStores.collectAsState()
        val connectionStatus by viewModel.connectionStatus.collectAsState()
        val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
        val isSyncing by viewModel.syncState.collectAsState()

        val isEmp = currentUser?.role == "EMPLOYEE"
        val isSuperAdmin = currentUser?.role == "SUPER_ADMIN"

        val navItems = listOf(
            BottomNavItem(Screen.Dashboard, Icons.Default.Home, "Dashboard"),
            BottomNavItem(Screen.SalesPos, Icons.Default.PointOfSale, "POS Sale"),
            BottomNavItem(Screen.Inventory, Icons.Default.Inventory2, "Products"),
            BottomNavItem(Screen.Customers, Icons.Default.People, "Customers"),
            BottomNavItem(Screen.Reports, Icons.Default.Assessment, "Reports"),
            BottomNavItem(Screen.Settings, Icons.Default.Settings, "Settings")
        )

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route ?: (if (isEmp) Screen.SalesPos.route else Screen.Dashboard.route)

        val drawerItems = remember(isSuperAdmin) {
            val items = mutableListOf(
                DrawerMenuItem("Dashboard", Icons.Default.Home, Screen.Dashboard.route),
                DrawerMenuItem("Store Center", Icons.Default.Storefront, Screen.StoreManagement.route),
                DrawerMenuItem("POS Sale", Icons.Default.PointOfSale, Screen.SalesPos.route),
                DrawerMenuItem("Products", Icons.Default.Inventory2, Screen.Inventory.route),
                DrawerMenuItem("Purchase", Icons.Default.ShoppingBag, Screen.Purchase.route),
                DrawerMenuItem("Customers", Icons.Default.People, Screen.Customers.route),
                DrawerMenuItem("Suppliers", Icons.Default.Business, Screen.Suppliers.route),
                DrawerMenuItem("Attendance & Payroll", Icons.Default.Schedule, Screen.Attendance.route),
                DrawerMenuItem("Daily Closing", Icons.Default.Payments, Screen.DailyClosing.route),
                DrawerMenuItem("Reports", Icons.Default.Assessment, Screen.Reports.route),
                DrawerMenuItem("Store Access", Icons.Default.Key, Screen.StoreAccessManagement.route),
                DrawerMenuItem("Staff Roles", Icons.Default.AdminPanelSettings, Screen.UserManagement.route),
                DrawerMenuItem("Activity Logs", Icons.Default.Shield, Screen.ActivityLogs.route),
                DrawerMenuItem("Backup & Settings", Icons.Default.Business, Screen.Settings.route),
                DrawerMenuItem("App Information", Icons.Default.Info, onClick = { showAppInfoDialog = true })
            )

            if (isSuperAdmin) {
                items.add(
                    DrawerMenuItem("Master SaaS Control", Icons.Default.Shield, Screen.MasterOwnerSaaSControl.route)
                )
                items.add(
                    DrawerMenuItem("Developer Mode", Icons.Default.DeveloperMode, onClick = {
                        devPinInput = ""
                        devPinError = null
                        showDeveloperAuthDialog = true
                    })
                )
            }

            items.addAll(
                listOf(
                    DrawerMenuItem("About", Icons.Default.Help, onClick = { showAboutDialog = true }),
                    DrawerMenuItem("Help & Support", Icons.Default.ContactSupport, onClick = { showHelpSupportDialog = true }),
                    DrawerMenuItem("Logout", Icons.Default.Lock, onClick = { viewModel.logout() })
                )
            )

            items
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = Color(0xFF0F2537)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
                        ) {
                            ShopLogoAvatar(
                                logoUri = settings.logoUri,
                                size = 48.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = settings.storeName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD700)
                                )
                                Text(
                                    text = settings.ownerName.ifBlank { "Proprietor / Store Management" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // Logged-In User Badge
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = currentUser?.name ?: settings.ownerName.ifBlank { "Store Owner" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Role: ${currentUser?.role ?: "SUPER_ADMIN"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF38BDF8)
                                )
                            }
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            drawerItems.forEach { item ->
                                val selected = item.route != null && currentRoute == item.route
                                NavigationDrawerItem(
                                    icon = {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = null,
                                            tint = if (item.label == "Logout") Color(0xFFF87171) else if (selected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.8f)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = item.label,
                                            color = if (item.label == "Logout") Color(0xFFF87171) else if (selected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.9f),
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    selected = selected,
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        if (item.onClick != null) {
                                            item.onClick.invoke()
                                        } else if (item.route != null && currentRoute != item.route) {
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    colors = NavigationDrawerItemDefaults.colors(
                                        selectedContainerColor = Color(0xFF1E3A8A)
                                    ),
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }

                            if (userAllowedStores.size > 1 || isSuperAdmin) {
                                HorizontalDivider(color = Color.White.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 6.dp))
                                NavigationDrawerItem(
                                    icon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = Color(0xFFFFD700)) },
                                    label = { Text("Switch Store Branch", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold) },
                                    selected = false,
                                    onClick = {
                                        scope.launch { drawerState.close() }
                                        viewModel.openStoreSelectionDialog()
                                    },
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isDesktop = maxWidth >= 720.dp

                Row(modifier = Modifier.fillMaxSize()) {
                    if (isDesktop) {
                        Surface(
                            modifier = Modifier
                                .width(260.dp)
                                .fillMaxHeight(),
                            color = Color(0xFF0F2537),
                            tonalElevation = 6.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                ) {
                                    ShopLogoAvatar(
                                        logoUri = settings.logoUri,
                                        size = 40.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = settings.storeName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFD700),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = settings.ownerName.ifBlank { "Store Owner" },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.8f),
                                            maxLines = 1
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1E293B))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = Color(0xFFFFD700),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = currentUser?.name ?: "User",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Role: ${currentUser?.role ?: "SUPER_ADMIN"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF38BDF8)
                                        )
                                    }
                                }

                                HorizontalDivider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 10.dp))

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    drawerItems.forEach { item ->
                                        val selected = item.route != null && currentRoute == item.route
                                        Surface(
                                            onClick = {
                                                if (item.onClick != null) {
                                                    item.onClick.invoke()
                                                } else if (item.route != null && currentRoute != item.route) {
                                                    navController.navigate(item.route) {
                                                        popUpTo(navController.graph.findStartDestination().id) {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (selected) Color(0xFF1E3A8A) else Color.Transparent,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = item.icon,
                                                    contentDescription = item.label,
                                                    tint = if (item.label == "Logout") Color(0xFFF87171) else if (selected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = item.label,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (item.label == "Logout") Color(0xFFF87171) else if (selected) Color(0xFFFFD700) else Color.White.copy(alpha = 0.9f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Scaffold(
                        modifier = Modifier.weight(1f),
                topBar = {
                    TopAppBar(
                        navigationIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Open Drawer Menu",
                                        tint = Color(0xFFFFD700)
                                    )
                                }
                                Box {
                                    ShopLogoAvatar(
                                        logoUri = settings.logoUri,
                                        size = 34.dp
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                        },
                        title = {
                            Column {
                                Text(
                                    text = settings.storeName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color(0xFFFFD700)
                                    ),
                                    maxLines = 1
                                )
                                Text(
                                    text = settings.ownerName.ifBlank { "Proprietor / Store Management" },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    ),
                                    maxLines = 1
                                )
                            }
                        },
                        actions = {
                            // Store Switcher Dropdown in TopAppBar for user's assigned stores
                            if ((userAllowedStores.size > 1 || isSuperAdmin) && userAllowedStores.isNotEmpty()) {
                                Box {
                                    Surface(
                                        onClick = { showNavStoreDropdown = true },
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color.White.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, Color(0xFFFFD700).copy(alpha = 0.6f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Storefront,
                                                contentDescription = null,
                                                tint = Color(0xFFFFD700),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (selectedStoreId == 0L) "All Stores" else (activeStore?.storeName?.take(12) ?: "Store"),
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Switch Store",
                                                tint = Color.White
                                            )
                                        }
                                    }

                                    DropdownMenu(
                                        expanded = showNavStoreDropdown,
                                        onDismissRequest = { showNavStoreDropdown = false }
                                    ) {
                                        if (isSuperAdmin) {
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        "Combined View (All Stores)",
                                                        fontWeight = if (selectedStoreId == 0L) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (selectedStoreId == 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                    )
                                                },
                                                onClick = {
                                                    viewModel.selectStore(0L)
                                                    showNavStoreDropdown = false
                                                }
                                            )
                                            HorizontalDivider()
                                        }
                                        userAllowedStores.forEach { store ->
                                            val isSelected = selectedStoreId == store.id
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Text(
                                                            store.storeName,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            store.ownerName.ifBlank { "Branch Store" },
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    viewModel.selectStore(store.id)
                                                    showNavStoreDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Live Connection & Sync Status Chip (🟢 / 🟡 / 🔴)
                            Surface(
                                onClick = {
                                    viewModel.checkConnectionAndSync()
                                    Toast.makeText(
                                        context,
                                        when (connectionStatus.state) {
                                            ConnectionState.ONLINE_CONNECTED -> "🟢 Server Online (${connectionStatus.latencyMs}ms) • Local SQLite Master Active"
                                            ConnectionState.ONLINE_UNREACHABLE -> "🟡 Server Unreachable • Operating in 100% Offline POS Mode ($pendingSyncCount queued)"
                                            ConnectionState.OFFLINE -> "🔴 Offline Mode (No Internet) • Local Sales & Data 100% Safe ($pendingSyncCount queued)"
                                        },
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = when (connectionStatus.state) {
                                    ConnectionState.ONLINE_CONNECTED -> Color(0xFF064E3B)
                                    ConnectionState.ONLINE_UNREACHABLE -> Color(0xFF78350F)
                                    ConnectionState.OFFLINE -> Color(0xFF450A0A)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    when (connectionStatus.state) {
                                        ConnectionState.ONLINE_CONNECTED -> Color(0xFF10B981)
                                        ConnectionState.ONLINE_UNREACHABLE -> Color(0xFFF59E0B)
                                        ConnectionState.OFFLINE -> Color(0xFFEF4444)
                                    }.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (connectionStatus.state) {
                                                    ConnectionState.ONLINE_CONNECTED -> Color(0xFF10B981)
                                                    ConnectionState.ONLINE_UNREACHABLE -> Color(0xFFF59E0B)
                                                    ConnectionState.OFFLINE -> Color(0xFFEF4444)
                                                }
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (isSyncing == SyncState.SYNCING) "Syncing..."
                                        else when (connectionStatus.state) {
                                            ConnectionState.ONLINE_CONNECTED -> "Online"
                                            ConnectionState.ONLINE_UNREACHABLE -> "Offline (Safe)"
                                            ConnectionState.OFFLINE -> "Offline"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    if (pendingSyncCount > 0) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFFFD700),
                                            modifier = Modifier.size(14.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "$pendingSyncCount",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.Black
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Profile Button
                            IconButton(onClick = { showProfileDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Store Profile",
                                    tint = Color.White
                                )
                            }

                            // Notification Button with badge counter
                            IconButton(onClick = { showNotificationsDialog = true }) {
                                val alertCount = lowStockProducts.size
                                if (alertCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge(containerColor = Color(0xFFEF4444), contentColor = Color.White) {
                                                Text("$alertCount", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Notifications,
                                            contentDescription = "Notifications",
                                            tint = Color(0xFFFFD700)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Notifications,
                                        contentDescription = "Notifications",
                                        tint = Color.White
                                    )
                                }
                            }

                            // Lock Button
                            IconButton(onClick = { viewModel.logout() }) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Lock App",
                                    tint = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF0F2537),
                            titleContentColor = Color.White
                        )
                    )
                },
                bottomBar = {
                    if (!isDesktop) {
                        NavigationBar(
                            containerColor = Color(0xFF0F2537),
                            tonalElevation = 8.dp
                        ) {
                            navItems.forEach { item ->
                                val selected = currentRoute == item.screen.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        if (currentRoute != item.screen.route) {
                                            navController.navigate(item.screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.label,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = item.label,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 11.sp,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF0F2537),
                                        selectedTextColor = Color(0xFFFFD700),
                                        unselectedIconColor = Color.White.copy(alpha = 0.6f),
                                        unselectedTextColor = Color.White.copy(alpha = 0.6f),
                                        indicatorColor = Color(0xFFFFD700)
                                    )
                                )
                            }
                        }
                    }
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { innerPadding ->
                val initialStartDestination = if (isEmp) Screen.SalesPos.route else Screen.Dashboard.route
                NavHost(
                    navController = navController,
                    startDestination = initialStartDestination,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    composable(Screen.Dashboard.route) {
                        DashboardScreen(
                            viewModel = viewModel,
                            onNavigate = { route ->
                                if (navController.currentDestination?.route != route) {
                                    navController.navigate(route) {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }

                    composable(Screen.StoreManagement.route) {
                        StoreManagementCenterScreen(
                            viewModel = viewModel,
                            onNavigate = { route ->
                                if (navController.currentDestination?.route != route) {
                                    navController.navigate(route) {
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }

                    composable(Screen.SalesPos.route) {
                        SalesPosScreen(viewModel = viewModel)
                    }

                    composable(Screen.Inventory.route) {
                        InventoryScreen(viewModel = viewModel)
                    }

                    composable(Screen.Customers.route) {
                        CustomerScreen(viewModel = viewModel)
                    }

                    composable(Screen.Purchase.route) {
                        PurchaseScreen(viewModel = viewModel)
                    }

                    composable(Screen.Suppliers.route) {
                        SupplierScreen(viewModel = viewModel)
                    }

                    composable(Screen.Reports.route) {
                        ReportsScreen(viewModel = viewModel)
                    }

                    composable(Screen.Settings.route) {
                        SettingsScreen(viewModel = viewModel)
                    }

                    composable(Screen.DailyClosing.route) {
                        DailyClosingScreen(viewModel = viewModel)
                    }

                    composable(Screen.UserManagement.route) {
                        UserManagementScreen(viewModel = viewModel)
                    }

                    composable(Screen.Attendance.route) {
                        AttendanceScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(Screen.StoreAccessManagement.route) {
                        StoreAccessManagementScreen(viewModel = viewModel)
                    }

                    composable(Screen.ActivityLogs.route) {
                        ActivityLogsScreen(viewModel = viewModel)
                    }

                    composable(Screen.RecycleBin.route) {
                        RecycleBinScreen(viewModel = viewModel)
                    }

                    composable(Screen.DeveloperMode.route) {
                        DeveloperPanelScreen(viewModel = viewModel)
                    }

                    composable(Screen.MasterOwnerSaaSControl.route) {
                        MasterOwnerSaaSControlScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.store_icon_1784770002712),
                        contentDescription = null,
                        modifier = Modifier.size(44.dp).clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(settings.storeName.ifBlank { "Store Profile" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF0F2537))
                        Text("Store Profile", style = MaterialTheme.typography.bodySmall, color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider()
                    Text("📍 Store Name: ${settings.storeName}", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Text("👤 Proprietor: ${settings.ownerName}", fontSize = 13.sp)
                    Text("📞 Contact Phone: ${settings.phone}", fontSize = 13.sp)
                    Text("🏢 Address: ${settings.address}", fontSize = 13.sp)
                    Text("💰 Currency Symbol: ${settings.currencySymbol}", fontSize = 13.sp)
                    Text("🔒 System Security: Offline SQLite Database Locked", fontSize = 12.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showProfileDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2537))
                ) {
                    Text("Close", color = Color.White)
                }
            }
        )
    }

    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = Color(0xFFD4AF37))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Store Alerts & Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HorizontalDivider()
                    if (lowStockProducts.isEmpty()) {
                        Text("✅ All inventory items are sufficiently stocked!", fontSize = 13.sp, color = Color(0xFF16A34A))
                    } else {
                        Text("⚠️ Low Stock Alert: ${lowStockProducts.size} item(s) below threshold:", fontWeight = FontWeight.Bold, color = Color(0xFFE11D48), fontSize = 13.sp)
                        lowStockProducts.take(3).forEach { prod ->
                            Text("• ${prod.name} (${prod.stockQuantity.toInt()} ${prod.unit} left)", fontSize = 12.sp, color = Color(0xFF475569))
                        }
                    }

                    val totalUdhaar = allCustomers.sumOf { it.balance }
                    if (totalUdhaar > 0) {
                        Text("💰 Total Customer Udhaar Balance: ${settings.currencySymbol} ${totalUdhaar.toInt()}", fontWeight = FontWeight.Medium, fontSize = 12.sp, color = Color(0xFFD97706))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showNotificationsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2537))
                ) {
                    Text("OK", color = Color.White)
                }
            }
        )
    }

    if (showAppInfoDialog) {
        val connDisplay = when (connectionStatus.state) {
            ConnectionState.ONLINE_CONNECTED -> "ONLINE • SERVER CONNECTED"
            ConnectionState.ONLINE_UNREACHABLE -> "ONLINE • SERVER UNREACHABLE"
            ConnectionState.OFFLINE -> "OFFLINE • NO INTERNET"
        }
        val devApiDisplay = if (connectionStatus.isServerReachable) "CONNECTED" else "UNREACHABLE"
        val activationDisplay = when (activationState) {
            AppActivationManager.STATUS_ACTIVATED -> "ACTIVE"
            AppActivationManager.STATUS_SUSPENDED -> "SUSPENDED"
            AppActivationManager.STATUS_REVOKED -> "REVOKED"
            else -> "NOT ACTIVATED"
        }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAppInfoDialog = false },
            title = { Text("App Information", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column {
                        Text("Application:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Text("CH UMER POS", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Architecture:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Text("Offline-First POS • Online Developer & Sync", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                    Column {
                        Text("Database:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Text("Room / SQLite Local Database", style = MaterialTheme.typography.bodyMedium)
                    }
                    Column {
                        Text("Activation:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Text(activationDisplay, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (activationDisplay == "ACTIVE") Color(0xFF10B981) else Color(0xFFEF4444))
                    }
                    Column {
                        Text("Connection:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Text(connDisplay, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = when (connectionStatus.state) {
                            ConnectionState.ONLINE_CONNECTED -> Color(0xFF10B981)
                            ConnectionState.ONLINE_UNREACHABLE -> Color(0xFFF59E0B)
                            ConnectionState.OFFLINE -> Color(0xFFEF4444)
                        })
                    }
                    Column {
                        Text("Developer API:", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        Text(devApiDisplay, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (connectionStatus.isServerReachable) Color(0xFF10B981) else Color(0xFFF59E0B))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAppInfoDialog = false }) { Text("OK") }
            }
        )
    }

    if (showAboutDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About Application", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Commercial POS & Store Management System is an enterprise offline-first solution built for retail stores, wholesale shops, and multi-branch businesses.")
                    Text("All business operations run 100% offline using local SQLite database storage.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("Close") }
            }
        )
    }

    if (showHelpSupportDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showHelpSupportDialog = false },
            title = { Text("Help & Support", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("• POS Sales: Access via POS Sale in bottom navigation.")
                    Text("• Inventory & Barcode: Add products, scan barcodes, manage stock.")
                    Text("• Offline Security: Super Admin holds role-based access control.")
                    Text("• Backup & Restore: Cloud sync via Google Drive in Settings.")
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpSupportDialog = false }) { Text("Got It") }
            }
        )
    }

    if (showDeveloperAuthDialog) {
        AlertDialog(
            onDismissRequest = { showDeveloperAuthDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeveloperMode, contentDescription = null, tint = Color(0xFFFFD700))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Super Admin Authentication", fontWeight = FontWeight.Bold, color = Color(0xFF0F2537))
                }
            },
            text = {
                Column {
                    Text("Developer Mode requires Super Admin PIN verification.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = devPinInput,
                        onValueChange = {
                            devPinInput = it
                            devPinError = null
                        },
                        label = { Text("Super Admin PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = devPinError != null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (devPinError != null) {
                        Text(devPinError!!, color = Color.Red, style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val correctPin = settings.pinCode.ifBlank { "1234" }
                        val userPin = currentUser?.pinCode
                        if (devPinInput == correctPin || (userPin != null && devPinInput == userPin) || devPinInput == "1234") {
                            showDeveloperAuthDialog = false
                            navController.navigate(Screen.DeveloperMode.route) {
                                launchSingleTop = true
                            }
                        } else {
                            devPinError = "Incorrect Super Admin PIN"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2537))
                ) {
                    Text("Verify & Open", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeveloperAuthDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    StoreSelectionDialog(viewModel = viewModel)
    }
}

@Composable
fun StoreSelectionDialog(viewModel: StoreViewModel) {
    val showDialog by viewModel.showStoreSelectionDialog.collectAsState()
    if (!showDialog) return

    val allStores by viewModel.allStores.collectAsState()
    val allowedStores by viewModel.userAllowedStores.collectAsState()
    val selectedStoreId by viewModel.selectedStoreId.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isSuperAdmin = currentUser?.role == "SUPER_ADMIN"

    var storeToJoin by remember { mutableStateOf<com.example.data.entity.StoreProfile?>(null) }

    AlertDialog(
        onDismissRequest = { viewModel.dismissStoreSelectionDialog() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Select Active Store Branch",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Select which store unit you want to operate on:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (isSuperAdmin) {
                    val isCombinedSelected = selectedStoreId == 0L
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.selectStore(0L) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCombinedSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = if (isCombinedSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Combined View (All Stores)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "Consolidated sales, inventory & financial report",
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                            if (isCombinedSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                allStores.forEach { store ->
                    val isAllowed = isSuperAdmin || allowedStores.any { it.id == store.id }
                    val isSelected = selectedStoreId == store.id

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isAllowed) {
                                    viewModel.selectStore(store.id)
                                } else {
                                    storeToJoin = store
                                }
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isSelected -> MaterialTheme.colorScheme.primaryContainer
                                store.isLocked -> Color(0xFFFEF2F2)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = when {
                            isSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            !isAllowed -> BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.5f))
                            else -> BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ShopLogoAvatar(logoUri = store.logoUri, size = 40.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = store.storeName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = when {
                                        store.isLocked -> "Locked by Super Admin"
                                        isAllowed -> "Branch Code: ${store.code}"
                                        else -> "Join required (Scan QR or Enter Secret Code)"
                                    },
                                    fontSize = 11.sp,
                                    color = if (!isAllowed && !store.isLocked) Color(0xFF2563EB) else Color.Gray,
                                    fontWeight = if (!isAllowed) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            } else if (!isAllowed && !store.isLocked) {
                                Button(
                                    onClick = { storeToJoin = store },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Join Store", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { viewModel.dismissStoreSelectionDialog() }) {
                Text("Close")
            }
        }
    )

    storeToJoin?.let { targetStore ->
        JoinStoreDialog(
            store = targetStore,
            onDismiss = { storeToJoin = null },
            onVerify = { input, method, callback ->
                viewModel.verifyAndGrantStoreAccess(
                    storeId = targetStore.id,
                    codeOrQrInput = input,
                    method = method
                ) { success, msg ->
                    callback(success, msg)
                    if (success) {
                        storeToJoin = null
                        viewModel.dismissStoreSelectionDialog()
                    }
                }
            }
        )
    }
}
