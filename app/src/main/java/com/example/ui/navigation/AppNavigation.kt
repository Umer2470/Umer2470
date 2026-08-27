package com.example.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.attendance.AttendanceScreen
import com.example.ui.components.AppBottomNavigationBar
import com.example.ui.invoice.InvoiceScreen
import com.example.ui.screens.*
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
    object AccessManagement : Screen("access_management")
    object Setup : Screen("setup")
    object Activation : Screen("activation")
    object DevPanel : Screen("dev_panel")
    object MasterSaas : Screen("master_saas")
    object RecycleBin : Screen("recycle_bin")
    object Logs : Screen("logs")
    object Settings : Screen("settings")
}

@Composable
fun AppNavigation(
    viewModel: StoreViewModel,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val cart by viewModel.cart.collectAsState()
    val cartItemCount = cart.sumOf { it.quantity }.toInt()

    Scaffold(
        bottomBar = {
            AppBottomNavigationBar(
                currentRoute = currentRoute,
                cartItemCount = cartItemCount,
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
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigate = { route -> navController.navigate(route) }
                )
            }
            composable(Screen.Pos.route) {
                SalesPosScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Invoice.route) {
                InvoiceScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Attendance.route) {
                AttendanceScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Inventory.route) {
                InventoryScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Customers.route) {
                CustomerScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Suppliers.route) {
                SupplierScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Purchases.route) {
                PurchaseScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Reports.route) {
                ReportsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Closing.route) {
                DailyClosingScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Users.route) {
                UserManagementScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.CashierManagement.route) {
                CashierManagementScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.StoreManagement.route) {
                StoreManagementCenterScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.AccessManagement.route) {
                StoreAccessManagementScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Setup.route) {
                BusinessSetupWizardScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Activation.route) {
                CustomerActivationScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.DevPanel.route) {
                DeveloperPanelScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.MasterSaas.route) {
                MasterOwnerSaaSControlScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.RecycleBin.route) {
                RecycleBinScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Logs.route) {
                ActivityLogsScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigate = { route -> navController.navigate(route) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
