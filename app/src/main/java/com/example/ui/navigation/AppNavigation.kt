package com.example.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.UserRole
import com.example.ui.attendance.AttendanceScreen
import com.example.ui.components.AppBottomNavigationBar
import com.example.ui.invoice.InvoiceScreen
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.StoreViewModel

sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Pos : Screen("pos")
    object Invoice : Screen("invoice")
    object Attendance : Screen("attendance")
    object Inventory : Screen("inventory")
    object Customers : Screen("customers")
    object Suppliers : Screen("suppliers")
    object Purchases : Screen("purchases")
    object Reports : Screen("reports")
    object Closing : Screen("closing")
    object Users : Screen("users")
    object CashierManagement : Screen("cashier_management")
    object StoreManagement : Screen("store_management")
    object OwnerControlCenter : Screen("owner_control_center")
    object AccessManagement : Screen("access_management")
    object Setup : Screen("setup")
    object Activation : Screen("activation")
    object RecycleBin : Screen("recycle_bin")
    object Logs : Screen("logs")
    object Settings : Screen("settings")
    object DeveloperHub : Screen("developer_hub")
}

@Composable
fun GuardedScreen(
    route: String,
    viewModel: StoreViewModel,
    onNavigateBack: () -> Unit,
    content: @Composable () -> Unit
) {
    val currentRole by viewModel.currentRole.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val isAllowed = remember(currentRole, route) {
        UserRole.isAllowed(currentRole, route)
    }

    if (isAllowed) {
        content()
    } else {
        var showOverrideDialog by remember { mutableStateOf(false) }
        var overrideUsername by remember { mutableStateOf("admin") }
        var overridePin by remember { mutableStateOf("") }
        var overrideError by remember { mutableStateOf<String?>(null) }
        var isOverrideSuccess by remember { mutableStateOf(false) }

        if (isOverrideSuccess) {
            content()
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Slate100)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 480.dp)
                        .testTag("access_restricted_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            color = Rose100,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LockPerson,
                                    contentDescription = "Restricted",
                                    tint = Rose600,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Text(
                            text = "Access Restricted",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Navy900
                        )

                        Text(
                            text = "Your current active role (${currentRole.displayName}) does not have permission to access the '$route' module.",
                            fontSize = 13.sp,
                            color = Navy600,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Surface(
                            color = Slate100,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Active User:", fontSize = 11.sp, color = Slate500)
                                    Text(
                                        text = activeUser?.fullName?.ifBlank { activeUser?.username } ?: "Cashier",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Navy900
                                    )
                                }
                                Surface(
                                    color = Blue100,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = currentRole.displayName,
                                        color = Blue700,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onNavigateBack,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("access_restricted_back_button")
                            ) {
                                Text("Go Back")
                            }

                            Button(
                                onClick = { showOverrideDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("access_restricted_override_button")
                            ) {
                                Text("Supervisor PIN")
                            }
                        }
                    }
                }

                if (showOverrideDialog) {
                    AlertDialog(
                        onDismissRequest = { showOverrideDialog = false },
                        title = {
                            Text(
                                text = "Supervisor / Admin Override",
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                        },
                        text = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Enter authorized PIN to temporarily unlock this module:",
                                    fontSize = 12.sp,
                                    color = Navy600
                                )
                                OutlinedTextField(
                                    value = overridePin,
                                    onValueChange = {
                                        overridePin = it
                                        overrideError = null
                                    },
                                    label = { Text("Supervisor / Admin PIN") },
                                    placeholder = { Text("Enter credential") },
                                    visualTransformation = PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("override_pin_input")
                                )
                                if (overrideError != null) {
                                    Text(
                                        text = overrideError!!,
                                        color = Rose600,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (overridePin.isBlank()) {
                                        overrideError = "Please enter credential."
                                        return@Button
                                    }
                                    viewModel.loginWithSingleCredential(overridePin) { success, msg, user ->
                                        if (success && user != null) {
                                            val overrideRole = UserRole.fromString(user.role)
                                            if (UserRole.isAllowed(overrideRole, route)) {
                                                isOverrideSuccess = true
                                                showOverrideDialog = false
                                            } else {
                                                overrideError = "User '${user.username}' (${overrideRole.displayName}) does not have permission for this module."
                                            }
                                        } else {
                                            overrideError = msg
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                                modifier = Modifier.testTag("override_confirm_button")
                            ) {
                                Text("Authorize")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showOverrideDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    viewModel: StoreViewModel,
    navController: NavHostController = rememberNavController()
) {
    val isActivated by viewModel.isActivatedFlow.collectAsState()
    val isAppLocked by viewModel.isAppLocked.collectAsState()
    val activeUser by viewModel.activeUser.collectAsState()
    val currentRole by viewModel.currentRole.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val cart by viewModel.cart.collectAsState()
    val cartItemCount = cart.sumOf { it.quantity }.toInt()

    if (!isActivated) {
        CustomerActivationScreen(
            viewModel = viewModel,
            onNavigateBack = null
        )
    } else if (isAppLocked || activeUser == null) {
        LoginScreen(
            viewModel = viewModel,
            onLoginSuccess = { viewModel.unlockTerminal() }
        )
    } else {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                AppBottomNavigationBar(
                    currentRoute = currentRoute,
                    cartItemCount = cartItemCount,
                    userRole = currentRole,
                    onNavigate = { targetRoute ->
                        if (currentRoute != targetRoute) {
                            navController.navigate(targetRoute) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = if (currentRole == UserRole.CASHIER) Screen.Pos.route else Screen.Dashboard.route,
                modifier = Modifier
                    .padding(innerPadding)
                    .imePadding()
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigate = { route -> navController.navigate(route) }
                    )
                }
                composable(Screen.Pos.route) {
                    GuardedScreen(
                        route = Screen.Pos.route,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    ) {
                        SalesPosScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
                composable(Screen.Invoice.route) {
                    GuardedScreen(
                        route = Screen.Invoice.route,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    ) {
                        InvoiceScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
                composable(Screen.Attendance.route) {
                    GuardedScreen(
                        route = Screen.Attendance.route,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    ) {
                        AttendanceScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
                composable(Screen.Inventory.route) {
                    GuardedScreen(
                        route = Screen.Inventory.route,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    ) {
                        InventoryScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
                composable(Screen.Customers.route) {
                    GuardedScreen(
                        route = Screen.Customers.route,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    ) {
                        CustomerScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
                composable(Screen.Suppliers.route) {
                    GuardedScreen(
                        route = Screen.Suppliers.route,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    ) {
                        SupplierScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
                composable(Screen.Purchases.route) {
                    GuardedScreen(
                        route = Screen.Purchases.route,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    ) {
                        PurchaseScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
                composable(Screen.Reports.route) {
                    GuardedScreen(
                        route = Screen.Reports.route,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    ) {
                        ReportsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
                composable(Screen.Closing.route) {
                    GuardedScreen(
                        route = Screen.Closing.route,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    ) {
                        DailyClosingScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
                composable(Screen.Users.route) {
                    GuardedScreen(
                        route = Screen.Users.route,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    ) {
                        UserManagementScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
                composable(Screen.CashierManagement.route) {
                    GuardedScreen(
                        route = Screen.CashierManagement.route,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    ) {
                        CashierManagementScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
                composable(Screen.StoreManagement.route) {
                    GuardedScreen(
                        route = Screen.StoreManagement.route,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    ) {
                        StoreManagementCenterScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
                composable(Screen.OwnerControlCenter.route) {
                    OwnerControlCenterScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToDeveloperHub = { navController.navigate(Screen.DeveloperHub.route) }
                    )
                }
                composable(Screen.AccessManagement.route) {
                    GuardedScreen(
                        route = Screen.AccessManagement.route,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    ) {
                        StoreAccessManagementScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
                composable(Screen.Setup.route) {
                    GuardedScreen(
                        route = Screen.Setup.route,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    ) {
                        BusinessSetupWizardScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
                composable(Screen.Activation.route) {
                    GuardedScreen(
                        route = Screen.Activation.route,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    ) {
                        CustomerActivationScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
                composable(Screen.RecycleBin.route) {
                    GuardedScreen(
                        route = Screen.RecycleBin.route,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    ) {
                        RecycleBinScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
                composable(Screen.Logs.route) {
                    GuardedScreen(
                        route = Screen.Logs.route,
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    ) {
                        ActivityLogsScreen(
                            viewModel = viewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        viewModel = viewModel,
                        onNavigate = { route -> navController.navigate(route) },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.DeveloperHub.route) {
                    DeveloperControlHubScreen(
                        viewModel = viewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
